package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Collections;

public class ContactChangesActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private TextView emptyView;

    private final ArrayList<LogEntry> logEntries = new ArrayList<>();

    private static class LogEntry {
        long userId;
        String name;
        String type; // "name" or "photo"
        String oldValue;
        String newValue;
        long time;
    }

    private SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("dgram_contact_changes", Context.MODE_PRIVATE);
    }

    @Override
    public boolean onFragmentCreate() {
        getNotificationCenter().addObserver(this, NotificationCenter.updateInterfaces);
        getNotificationCenter().addObserver(this, NotificationCenter.contactsDidLoad);
        loadLog();
        checkForChanges();
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.updateInterfaces);
        getNotificationCenter().removeObserver(this, NotificationCenter.contactsDidLoad);
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Contact Changes");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listAdapter = new ListAdapter(context);
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new TextView(context);
        emptyView.setText("No changes recorded yet.\nDgram will log name and photo changes of your contacts while the app is open.");
        emptyView.setTextSize(15);
        emptyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        emptyView.setGravity(android.view.Gravity.CENTER);
        emptyView.setPadding(60, 0, 60, 0);
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, android.view.Gravity.CENTER));

        updateEmptyView();

        return fragmentView;
    }

    private void updateEmptyView() {
        if (emptyView == null || listView == null) {
            return;
        }
        emptyView.setVisibility(logEntries.isEmpty() ? View.VISIBLE : View.GONE);
        listView.setVisibility(logEntries.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void loadLog() {
        logEntries.clear();
        String saved = prefs().getString("log", "[]");
        try {
            JSONArray arr = new JSONArray(saved);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                LogEntry entry = new LogEntry();
                entry.userId = obj.optLong("uid");
                entry.name = obj.optString("name");
                entry.type = obj.optString("type");
                entry.oldValue = obj.optString("old");
                entry.newValue = obj.optString("new");
                entry.time = obj.optLong("time");
                logEntries.add(entry);
            }
        } catch (JSONException ignored) {
        }
        Collections.reverse(logEntries);
    }

    private void saveLog() {
        JSONArray arr = new JSONArray();
        ArrayList<LogEntry> chronological = new ArrayList<>(logEntries);
        Collections.reverse(chronological);
        int start = Math.max(0, chronological.size() - 200);
        for (int i = start; i < chronological.size(); i++) {
            LogEntry entry = chronological.get(i);
            JSONObject obj = new JSONObject();
            try {
                obj.put("uid", entry.userId);
                obj.put("name", entry.name);
                obj.put("type", entry.type);
                obj.put("old", entry.oldValue);
                obj.put("new", entry.newValue);
                obj.put("time", entry.time);
                arr.put(obj);
            } catch (JSONException ignored) {
            }
        }
        prefs().edit().putString("log", arr.toString()).apply();
    }

    private JSONObject loadSnapshot() {
        String saved = prefs().getString("snapshot", "{}");
        try {
            return new JSONObject(saved);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private void saveSnapshot(JSONObject snapshot) {
        prefs().edit().putString("snapshot", snapshot.toString()).apply();
    }

    private void checkForChanges() {
        JSONObject snapshot = loadSnapshot();
        boolean changed = false;

        for (TLRPC.TL_contact contact : ContactsController.getInstance(currentAccount).contacts) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(contact.user_id);
            if (user == null || UserObject.isDeleted(user) || UserObject.isUserSelf(user)) {
                continue;
            }
            String key = String.valueOf(user.id);
            String currentName = ContactsController.formatName(user.first_name, user.last_name);
            long photoId = (user.photo != null) ? user.photo.photo_id : 0;

            JSONObject prevData = snapshot.optJSONObject(key);
            if (prevData == null) {
                JSONObject newData = new JSONObject();
                try {
                    newData.put("name", currentName);
                    newData.put("photoId", photoId);
                    snapshot.put(key, newData);
                } catch (JSONException ignored) {
                }
                changed = true;
                continue;
            }

            String prevName = prevData.optString("name");
            long prevPhotoId = prevData.optLong("photoId");

            if (!prevName.equals(currentName)) {
                addLogEntry(user.id, currentName, "name", prevName, currentName);
                try {
                    prevData.put("name", currentName);
                } catch (JSONException ignored) {
                }
                changed = true;
            }
            if (prevPhotoId != photoId) {
                addLogEntry(user.id, currentName, "photo", null, null);
                try {
                    prevData.put("photoId", photoId);
                } catch (JSONException ignored) {
                }
                changed = true;
            }
        }

        if (changed) {
            saveSnapshot(snapshot);
            saveLog();
            loadLog();
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            updateEmptyView();
        }
    }

    private void addLogEntry(long userId, String name, String type, String oldValue, String newValue) {
        LogEntry entry = new LogEntry();
        entry.userId = userId;
        entry.name = name;
        entry.type = type;
        entry.oldValue = oldValue;
        entry.newValue = newValue;
        entry.time = System.currentTimeMillis();
        logEntries.add(0, entry);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.updateInterfaces || id == NotificationCenter.contactsDidLoad) {
            checkForChanges();
        }
    }

    private String formatEntryText(LogEntry entry) {
        if ("photo".equals(entry.type)) {
            return entry.name + " changed their profile photo";
        } else {
            return entry.name + " changed name from \"" + entry.oldValue + "\" to \"" + entry.newValue + "\"";
        }
    }

    private String formatTime(long time) {
        long diff = System.currentTimeMillis() - time;
        long minutes = diff / 60000;
        if (minutes < 1) {
            return "just now";
        } else if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h ago";
        }
        long days = hours / 24;
        return days + "d ago";
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }

        @Override
        public int getItemCount() {
            return logEntries.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextInfoPrivacyCell cell = new TextInfoPrivacyCell(context);
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            LogEntry entry = logEntries.get(position);
            TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
            cell.setText(formatEntryText(entry) + "  \u2022  " + formatTime(entry.time));
            cell.setBackgroundDrawable(Theme.getThemedDrawable(context, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
        }
    }
}
