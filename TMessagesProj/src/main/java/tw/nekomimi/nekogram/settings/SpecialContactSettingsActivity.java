package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class SpecialContactSettingsActivity extends BaseFragment {

    private long userId;
    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private final int TYPE_USER_HEADER = 0;
    private final int TYPE_HEADER = 1;
    private final int TYPE_CHECK = 2;
    private final int TYPE_TEXT = 3;

    private final ArrayList<Integer> items = new ArrayList<>();

    private static final String[] CHECK_KEYS = {
            "goingOnline", "goingOffline", "readingMessage", "sendingMessage",
            "changingProfilePicture", "changingUsername"
    };
    private static final String[] CHECK_LABELS = {
            "Going Online", "Going Offline", "Reading Message", "Sending Message",
            "Changing Profile Picture", "Changing Username"
    };
    private static final boolean[] CHECK_DEFAULTS = {
            true, false, true, false, true, true
    };

    public SpecialContactSettingsActivity(long userId) {
        super();
        this.userId = userId;
    }

    private SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("dgram_special_contacts", Context.MODE_PRIVATE);
    }

    private JSONObject loadSettings() {
        String saved = prefs().getString("settings_" + userId, null);
        if (saved != null) {
            try {
                return new JSONObject(saved);
            } catch (JSONException ignored) {
            }
        }
        JSONObject obj = new JSONObject();
        for (int i = 0; i < CHECK_KEYS.length; i++) {
            try {
                obj.put(CHECK_KEYS[i], CHECK_DEFAULTS[i]);
            } catch (JSONException ignored) {
            }
        }
        try {
            obj.put("actionsNotification", true);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    private void saveSettings(JSONObject obj) {
        prefs().edit().putString("settings_" + userId, obj.toString()).apply();
    }

    private void updateRows() {
        items.clear();
        items.add(TYPE_USER_HEADER);
        items.add(TYPE_HEADER); // Actions
        for (int i = 0; i < CHECK_KEYS.length; i++) {
            items.add(TYPE_CHECK);
        }
        items.add(TYPE_HEADER); // General
        items.add(TYPE_CHECK); // Actions Notification
        items.add(TYPE_TEXT); // Sound
        items.add(TYPE_TEXT); // Vibrate
        items.add(TYPE_TEXT); // Priority
    }

    @Override
    public boolean onFragmentCreate() {
        updateRows();
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Special Contact Settings");
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
        listView.setOnItemClickListener((view, position) -> {
            int type = items.get(position);
            if (type == TYPE_CHECK) {
                JSONObject settings = loadSettings();
                int checkIndex = getCheckIndexForPosition(position);
                boolean newVal;
                if (checkIndex >= 0) {
                    newVal = !settings.optBoolean(CHECK_KEYS[checkIndex], CHECK_DEFAULTS[checkIndex]);
                    try {
                        settings.put(CHECK_KEYS[checkIndex], newVal);
                    } catch (JSONException ignored) {
                    }
                } else {
                    newVal = !settings.optBoolean("actionsNotification", true);
                    try {
                        settings.put("actionsNotification", newVal);
                    } catch (JSONException ignored) {
                    }
                }
                saveSettings(settings);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(newVal);
                }
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    private int getCheckIndexForPosition(int position) {
        // positions: 0=header user, 1=header actions, 2..7 = checks, then general header etc
        int checkStart = 2;
        int idx = position - checkStart;
        if (idx >= 0 && idx < CHECK_KEYS.length) {
            return idx;
        }
        return -1;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = items.get(holder.getAdapterPosition());
            return type == TYPE_CHECK || type == TYPE_TEXT;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case TYPE_USER_HEADER:
                    view = new UserCell(context, 16, 0, false);
                    break;
                case TYPE_HEADER:
                    view = new HeaderCell(context);
                    break;
                case TYPE_CHECK:
                    view = new TextCheckCell(context);
                    break;
                default:
                    view = new TextSettingsCell(context);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int type = items.get(position);
            JSONObject settings = loadSettings();
            switch (type) {
                case TYPE_USER_HEADER: {
                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
                    String name = user != null ? ContactsController.formatName(user.first_name, user.last_name) : "Unknown";
                    String status = user != null ? LocaleController.formatUserStatus(currentAccount, user) : "";
                    ((UserCell) holder.itemView).setData(user, name, status, 0, false);
                    break;
                }
                case TYPE_HEADER: {
                    int headerIndex = 0;
                    int seen = 0;
                    for (int i = 0; i <= position; i++) {
                        if (items.get(i) == TYPE_HEADER) {
                            seen++;
                        }
                    }
                    ((HeaderCell) holder.itemView).setText(seen == 1 ? "Actions" : "General");
                    break;
                }
                case TYPE_CHECK: {
                    int checkIndex = getCheckIndexForPosition(position);
                    if (checkIndex >= 0) {
                        boolean val = settings.optBoolean(CHECK_KEYS[checkIndex], CHECK_DEFAULTS[checkIndex]);
                        ((TextCheckCell) holder.itemView).setTextAndCheck(CHECK_LABELS[checkIndex], val, true);
                    } else {
                        boolean val = settings.optBoolean("actionsNotification", true);
                        ((TextCheckCell) holder.itemView).setTextAndCheck("Actions Notification", val, true);
                    }
                    break;
                }
                case TYPE_TEXT: {
                    int textPos = getTextRowIndex(position);
                    String[] labels = {"Sound", "Vibrate", "Priority"};
                    String[] values = {"Default", "Default", "Same as in Settings"};
                    if (textPos >= 0 && textPos < labels.length) {
                        ((TextSettingsCell) holder.itemView).setTextAndValue(labels[textPos], values[textPos], true);
                    }
                    break;
                }
            }
        }

        private int getTextRowIndex(int position) {
            int count = -1;
            for (int i = 0; i <= position; i++) {
                if (items.get(i) == TYPE_TEXT) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position);
        }
    }
}
