package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.messenger.NotificationCenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class OnlineContactsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private TextView emptyView;
    private final ArrayList<TLRPC.User> sortedUsers = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        getNotificationCenter().addObserver(this, NotificationCenter.updateInterfaces);
        loadContacts();
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.updateInterfaces);
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Online Contacts");
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
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        listAdapter = new ListAdapter(context);
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= sortedUsers.size()) {
                return;
            }
            TLRPC.User user = sortedUsers.get(position);
            Bundle args = new Bundle();
            args.putLong("user_id", user.id);
            presentFragment(new ChatActivity(args));
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new TextView(context);
        emptyView.setText("No contacts found.");
        emptyView.setTextSize(16);
        emptyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        emptyView.setGravity(android.view.Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, android.view.Gravity.CENTER));

        updateEmptyView();

        return fragmentView;
    }

    private void loadContacts() {
        sortedUsers.clear();
        for (TLRPC.TL_contact contact : ContactsController.getInstance(currentAccount).contacts) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(contact.user_id);
            if (user != null && !UserObject.isDeleted(user) && !UserObject.isUserSelf(user) && isOnline(user)) {
                sortedUsers.add(user);
            }
        }
        Collections.sort(sortedUsers, new Comparator<TLRPC.User>() {
            @Override
            public int compare(TLRPC.User a, TLRPC.User b) {
                boolean aOnline = isOnline(a);
                boolean bOnline = isOnline(b);
                if (aOnline && !bOnline) {
                    return -1;
                } else if (!aOnline && bOnline) {
                    return 1;
                }
                return 0;
            }
        });
    }

    private boolean isOnline(TLRPC.User user) {
        if (user == null || user.status == null) {
            return false;
        }
        int currentTime = getConnectionsManager().getCurrentTime();
        if (user.status instanceof TLRPC.TL_userStatusOnline) {
            return user.status.expires > currentTime;
        }
        return false;
    }

    private void updateEmptyView() {
        if (emptyView == null) {
            return;
        }
        emptyView.setVisibility(sortedUsers.isEmpty() ? View.VISIBLE : View.GONE);
        listView.setVisibility(sortedUsers.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            loadContacts();
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            updateEmptyView();
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return sortedUsers.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            UserCell userCell = new UserCell(context, 8, 0, false);
            return new RecyclerListView.Holder(userCell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TLRPC.User user = sortedUsers.get(position);
            UserCell userCell = (UserCell) holder.itemView;
            boolean online = isOnline(user);
            String status;
            if (online) {
                status = LocaleController.getString(R.string.Online);
            } else {
                status = LocaleController.formatUserStatus(currentAccount, user);
            }
            userCell.setData(user, null, status, 0, position != sortedUsers.size() - 1);
        }
    }
}
