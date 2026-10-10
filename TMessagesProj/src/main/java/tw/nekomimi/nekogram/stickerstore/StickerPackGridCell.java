package tw.nekomimi.nekogram.stickerstore;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import tw.nekomimi.nekogram.settings.GroupCardHelper;

public class StickerPackGridCell extends FrameLayout {

    private final FrameLayout iconWrap;
    private final TextView iconLetter;
    private final TextView titleView;
    private final TextView categoryView;
    private final TextView ratingView;
    private final TextView downloadsView;

    public StickerPackGridCell(Context context) {
        super(context);
        setBackground(GroupCardHelper.cardBackground(context, GroupCardHelper.POS_SINGLE));
        setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        addView(column, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        iconWrap = new FrameLayout(context);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.RECTANGLE);
        iconBg.setCornerRadius(AndroidUtilities.dp(14));
        iconBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        iconWrap.setBackground(iconBg);
        column.addView(iconWrap, LayoutHelper.createLinear(72, 72, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8));

        iconLetter = new TextView(context);
        iconLetter.setTextSize(26);
        iconLetter.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        iconLetter.setGravity(Gravity.CENTER);
        iconWrap.addView(iconLetter, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        titleView = new TextView(context);
        titleView.setTextSize(14);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleView.setMaxLines(1);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        column.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        categoryView = new TextView(context);
        categoryView.setTextSize(12);
        categoryView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        categoryView.setGravity(Gravity.CENTER_HORIZONTAL);
        categoryView.setMaxLines(1);
        categoryView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        column.addView(categoryView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 2, 0, 4));

        LinearLayout statsRow = new LinearLayout(context);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setGravity(Gravity.CENTER_HORIZONTAL);
        column.addView(statsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ratingView = new TextView(context);
        ratingView.setTextSize(12);
        ratingView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statsRow.addView(ratingView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8, 0));

        downloadsView = new TextView(context);
        downloadsView.setTextSize(12);
        downloadsView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statsRow.addView(downloadsView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        setWillNotDraw(false);
    }

    public void setPack(StickerPackInfo pack) {
        String title = pack.title != null ? pack.title : pack.shortName;
        titleView.setText(title);
        iconLetter.setText(title != null && !title.isEmpty() ? title.substring(0, 1).toUpperCase() : "?");
        categoryView.setText(pack.category != null ? pack.category : "");
        categoryView.setVisibility(pack.category != null && !pack.category.isEmpty() ? View.VISIBLE : View.GONE);
        ratingView.setText(pack.rating > 0 ? String.format(LocaleController.getInstance().getCurrentLocale(), "★ %.1f", pack.rating) : "");
        ratingView.setVisibility(pack.rating > 0 ? View.VISIBLE : View.GONE);
        downloadsView.setText(pack.downloads > 0 ? formatDownloads(pack.downloads) : "");
        downloadsView.setVisibility(pack.downloads > 0 ? View.VISIBLE : View.GONE);
    }

    private static String formatDownloads(int downloads) {
        if (downloads >= 1000) {
            return String.format(LocaleController.getInstance().getCurrentLocale(), "%.1fK ↓", downloads / 1000f);
        }
        return downloads + " ↓";
    }
}
