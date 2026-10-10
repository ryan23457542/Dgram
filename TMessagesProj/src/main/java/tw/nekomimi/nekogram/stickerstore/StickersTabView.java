package tw.nekomimi.nekogram.stickerstore;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.StickersAlert;

import java.util.ArrayList;

public class StickersTabView extends FrameLayout {

    private final BaseFragment fragment;
    private final RecyclerListView listView;
    private final GridLayoutManager layoutManager;
    private final Adapter adapter;
    private final RadialProgressView progressView;
    private final TextView infoView;
    private final TextView sortPopular;
    private final TextView sortNewest;

    private final ArrayList<StickerPackInfo> packs = new ArrayList<>();
    private String currentSort = StickerStoreApi.SORT_NEWEST;
    private boolean loading;

    public StickersTabView(Context context, BaseFragment fragment) {
        super(context);
        this.fragment = fragment;
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        LinearLayout sortRow = new LinearLayout(context);
        sortRow.setOrientation(LinearLayout.HORIZONTAL);
        addView(sortRow, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 40, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0));

        sortPopular = createSortChip(context, "Popular");
        sortNewest = createSortChip(context, "Newest");
        sortRow.addView(sortPopular, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, 0, 0, 6, 0));
        sortRow.addView(sortNewest, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));

        sortPopular.setOnClickListener(v -> setSort(StickerStoreApi.SORT_POPULAR));
        sortNewest.setOnClickListener(v -> setSort(StickerStoreApi.SORT_NEWEST));

        layoutManager = new GridLayoutManager(context, 2);
        listView = new RecyclerListView(context);
        listView.setLayoutManager(layoutManager);
        listView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(56), AndroidUtilities.dp(8), AndroidUtilities.dp(16));
        listView.setClipToPadding(false);
        adapter = new Adapter();
        listView.setAdapter(adapter);
        addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position >= 0 && position < packs.size()) {
                openPack(packs.get(position));
            }
        });

        progressView = new RadialProgressView(context);
        addView(progressView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 28, 0, 0));

        infoView = new TextView(context);
        infoView.setTextSize(14);
        infoView.setGravity(Gravity.CENTER);
        infoView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        infoView.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);
        addView(infoView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 28, 0, 0));
        infoView.setVisibility(GONE);

        updateSortChips();
        loadPacks();
    }

    private TextView createSortChip(Context context, String text) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setTextSize(13);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
        chip.setTypeface(AndroidUtilities.bold());
        return chip;
    }

    private void styleChip(TextView chip, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(18));
        if (selected) {
            bg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            chip.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        } else {
            bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        }
        chip.setBackground(bg);
    }

    private void updateSortChips() {
        styleChip(sortPopular, StickerStoreApi.SORT_POPULAR.equals(currentSort));
        styleChip(sortNewest, StickerStoreApi.SORT_NEWEST.equals(currentSort));
    }

    private void setSort(String sort) {
        if (sort.equals(currentSort) || loading) {
            return;
        }
        currentSort = sort;
        updateSortChips();
        loadPacks();
    }

    private void loadPacks() {
        if (!StickerStoreApi.isConfigured()) {
            loading = false;
            progressView.setVisibility(GONE);
            listView.setVisibility(GONE);
            infoView.setVisibility(VISIBLE);
            infoView.setText("Sticker store isn't configured for this build.");
            return;
        }
        loading = true;
        progressView.setVisibility(VISIBLE);
        infoView.setVisibility(GONE);
        listView.setVisibility(GONE);
        StickerStoreApi.fetchPacks(null, currentSort, 1, 50, new StickerStoreApi.PacksCallback() {
            @Override
            public void onSuccess(ArrayList<StickerPackInfo> result) {
                loading = false;
                progressView.setVisibility(GONE);
                packs.clear();
                packs.addAll(result);
                adapter.notifyDataSetChanged();
                if (packs.isEmpty()) {
                    infoView.setVisibility(VISIBLE);
                    infoView.setText("No sticker packs yet.");
                    listView.setVisibility(GONE);
                } else {
                    listView.setVisibility(VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                loading = false;
                progressView.setVisibility(GONE);
                listView.setVisibility(GONE);
                infoView.setVisibility(VISIBLE);
                infoView.setText(message);
            }
        });
    }

    private void openPack(StickerPackInfo pack) {
        if (pack.shortName == null || pack.shortName.isEmpty() || fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        try {
            TLRPC.TL_inputStickerSetShortName inputStickerSet = new TLRPC.TL_inputStickerSetShortName();
            inputStickerSet.short_name = pack.shortName;
            StickersAlert alert = new StickersAlert(fragment.getParentActivity(), fragment, inputStickerSet, null, null, false);
            fragment.showDialog(alert);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private class Adapter extends RecyclerListView.SelectionAdapter {
        @Override
        public int getItemCount() {
            return packs.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            StickerPackGridCell cell = new StickerPackGridCell(parent.getContext());
            cell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(150)));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((StickerPackGridCell) holder.itemView).setPack(packs.get(position));
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            lp.leftMargin = AndroidUtilities.dp(4);
            lp.rightMargin = AndroidUtilities.dp(4);
            lp.topMargin = AndroidUtilities.dp(4);
            lp.bottomMargin = AndroidUtilities.dp(4);
            holder.itemView.setLayoutParams(lp);
        }
    }
}
