package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.ContactsActivity;

import java.util.ArrayList;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SpecialContactsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private final int TYPE_ENABLE = 0;
    private final int TYPE_ENABLE_INFO = 1;
    private final int TYPE_BACKGROUND = 2;
    private final int TYPE_BACKGROUND_INFO = 3;
    private final int TYPE_ADD_HEADER = 4;
    private final int TYPE_ADD_BUTTON = 5;

    private final ArrayList<Integer> items = new ArrayList<>();

    private SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("dgram_special_contacts", Context.MODE_PRIVATE);
    }

    private boolean isFeatureEnabled() {
        return prefs().getBoolean("enabled", true);
    }

    private boolean isBackgroundEnabled() {
        return prefs().getBoolean("background", false);
    }

    private void updateRows() {
        items.clear();
        items.add(TYPE_ENABLE);
        items.add(TYPE_ENABLE_INFO);
        items.add(TYPE_BACKGROUND);
        items.add(TYPE_BACKGROUND_INFO);
        items.add(TYPE_ADD_BUTTON);
    }

    @Override
    public boolean onFragmentCreate() {
        updateRows();
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Special Contacts");
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

        listView.setOnItemClickListener((view, position) -> {
            int type = items.get(position);
            if (type == TYPE_ENABLE) {
                boolean newVal = !isFeatureEnabled();
                prefs().edit().putBoolean("enabled", newVal).apply();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(newVal);
                }
            } else if (type == TYPE_BACKGROUND) {
                boolean newVal = !isBackgroundEnabled();
                prefs().edit().putBoolean("background", newVal).apply();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(newVal);
                }
            } else if (type == TYPE_ADD_BUTTON) {
                presentFragment(new ContactsActivity(null));
            }
        });

        return fragmentView;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = items.get(holder.getAdapterPosition());
            return type == TYPE_ENABLE || type == TYPE_BACKGROUND || type == TYPE_ADD_BUTTON;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case TYPE_ENABLE:
                case TYPE_BACKGROUND:
                    view = new TextCheckCell(context);
                    break;
                case TYPE_ENABLE_INFO:
                case TYPE_BACKGROUND_INFO:
                    view = new TextInfoPrivacyCell(context);
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
            switch (type) {
                case TYPE_ENABLE:
                    ((TextCheckCell) holder.itemView).setTextAndCheck("Enable Special Contacts", isFeatureEnabled(), true);
                    break;
                case TYPE_ENABLE_INFO:
                    ((TextInfoPrivacyCell) holder.itemView).setText("This feature helps by notifying you when your selected contacts do some specific actions. Like reading messages, changing profile details or going online.");
                    ((TextInfoPrivacyCell) holder.itemView).setBackgroundDrawable(Theme.getThemedDrawable(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    break;
                case TYPE_BACKGROUND:
                    ((TextCheckCell) holder.itemView).setTextAndCheck("Enable for Background", isBackgroundEnabled(), false);
                    break;
                case TYPE_BACKGROUND_INFO:
                    ((TextInfoPrivacyCell) holder.itemView).setText("Use Special Contacts while app is closed.\nKeep in mind that this might increase your phone battery usage.");
                    ((TextInfoPrivacyCell) holder.itemView).setBackgroundDrawable(Theme.getThemedDrawable(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    break;
                case TYPE_ADD_BUTTON:
                    ((TextSettingsCell) holder.itemView).setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
                    ((TextSettingsCell) holder.itemView).setText("Add New Contact", false);
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position);
        }
    }
}
