package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import tw.nekomimi.nekogram.stickerstore.ProfileTabView;
import tw.nekomimi.nekogram.stickerstore.StickersTabView;

public class KuroThemeActivity extends BaseFragment {

    private static final int TAB_THEMES = 0;
    private static final int TAB_STICKERS = 1;
    private static final int TAB_PROFILE = 2;

    private FrameLayout contentFrame;
    private View[] tabContents = new View[3];
    private TabButton[] tabButtons = new TabButton[3];
    private int selectedTab = TAB_THEMES;

    private static class TabButton extends LinearLayout {
        ImageView icon;
        TextView label;

        TabButton(Context context) {
            super(context);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);

            icon = new ImageView(context);
            addView(icon, LayoutHelper.createLinear(24, 24, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 2));

            label = new TextView(context);
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);
            addView(label, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
        }

        void applySelected(boolean selected) {
            int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
            int tint = selected ? accent : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
            icon.setColorFilter(new PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN));
            label.setTextColor(tint);
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Kuro Theme");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout rootLayout = new FrameLayout(context);
        fragmentView = rootLayout;
        rootLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        contentFrame = new FrameLayout(context);
        rootLayout.addView(contentFrame, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 0, 0, 56));

        LinearLayout tabBar = new LinearLayout(context);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        View shadow = new View(context);
        shadow.setBackgroundColor(Theme.getColor(Theme.key_divider));
        FrameLayout tabBarWrap = new FrameLayout(context);
        tabBarWrap.addView(shadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.TOP));
        tabBarWrap.addView(tabBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56, Gravity.BOTTOM));
        rootLayout.addView(tabBarWrap, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56, Gravity.BOTTOM));

        addTabButton(context, tabBar, TAB_THEMES, R.drawable.msg_theme, "Themes");
        addTabButton(context, tabBar, TAB_STICKERS, R.drawable.msg_sticker, "Stickers");
        addTabButton(context, tabBar, TAB_PROFILE, R.drawable.msg_contacts, "Profile");

        selectTab(TAB_THEMES);

        return fragmentView;
    }

    private void addTabButton(Context context, LinearLayout tabBar, int index, int iconRes, String text) {
        TabButton button = new TabButton(context);
        button.icon.setImageResource(iconRes);
        button.label.setText(text);
        button.setOnClickListener(v -> selectTab(index));
        tabBar.addView(button, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f));
        tabButtons[index] = button;
    }

    private void selectTab(int index) {
        if (selectedTab != index || tabContents[index] == null) {
            selectedTab = index;
            showTabContent(index);
        }
        for (int i = 0; i < tabButtons.length; i++) {
            if (tabButtons[i] != null) {
                tabButtons[i].applySelected(i == index);
            }
        }
    }

    private void showTabContent(int index) {
        contentFrame.removeAllViews();
        if (tabContents[index] == null) {
            tabContents[index] = createTabContent(index);
        }
        if (tabContents[index].getParent() instanceof FrameLayout) {
            ((FrameLayout) tabContents[index].getParent()).removeView(tabContents[index]);
        }
        contentFrame.addView(tabContents[index], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    private View createTabContent(int index) {
        Context context = contentFrame.getContext();
        switch (index) {
            case TAB_STICKERS:
                return new StickersTabView(context, this);
            case TAB_PROFILE:
                return new ProfileTabView(context, currentAccount);
            case TAB_THEMES:
            default:
                return createComingSoonView(context);
        }
    }

    private View createComingSoonView(Context context) {
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        TextView textView = new TextView(context);
        textView.setText("Coming soon");
        textView.setTextSize(16);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        frameLayout.addView(textView, params);

        return frameLayout;
    }
}
