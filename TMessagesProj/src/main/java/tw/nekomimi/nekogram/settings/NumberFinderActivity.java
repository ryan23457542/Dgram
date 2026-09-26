package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.graphics.PorterDuff;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import android.os.Bundle;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

public class NumberFinderActivity extends BaseFragment {

    private EditText phoneInput;
    private LinearLayout resultContainer;
    private BackupImageView avatarImageView;
    private TextView nameTextView;
    private TextView usernameTextView;
    private TextView statusTextView;
    private FrameLayout openChatButton;
    private TLRPC.User foundUser;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Number Finder");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        fragmentView = rootLayout;

        // Input row
        LinearLayout inputRow = new LinearLayout(context);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        phoneInput = new EditText(context);
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setHint("Enter phone number (e.g. +959123456789)");
        phoneInput.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3));
        phoneInput.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        phoneInput.setTextSize(16);
        phoneInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        phoneInput.setSingleLine(true);
        phoneInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });
        inputRow.addView(phoneInput, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        TextView searchButton = new TextView(context);
        searchButton.setText("Search");
        searchButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        searchButton.setTextSize(16);
        searchButton.setPadding(AndroidUtilities.dp(12), 0, 0, 0);
        searchButton.setOnClickListener(v -> doSearch());
        inputRow.addView(searchButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        rootLayout.addView(inputRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        rootLayout.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1));

        // Result container (hidden until search)
        resultContainer = new LinearLayout(context);
        resultContainer.setOrientation(LinearLayout.VERTICAL);
        resultContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        resultContainer.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(32), AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        resultContainer.setVisibility(View.GONE);

        avatarImageView = new BackupImageView(context);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(40));
        resultContainer.addView(avatarImageView, LayoutHelper.createLinear(80, 80, Gravity.CENTER_HORIZONTAL));

        nameTextView = new TextView(context);
        nameTextView.setTextSize(18);
        nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameTextView.setGravity(Gravity.CENTER);
        nameTextView.setPadding(0, AndroidUtilities.dp(12), 0, 0);
        resultContainer.addView(nameTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        usernameTextView = new TextView(context);
        usernameTextView.setTextSize(14);
        usernameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        usernameTextView.setGravity(Gravity.CENTER);
        usernameTextView.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        resultContainer.addView(usernameTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        openChatButton = new FrameLayout(context);
        openChatButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        TextView openChatText = new TextView(context);
        openChatText.setText("Open Chat");
        openChatText.setTextColor(0xffffffff);
        openChatText.setTextSize(14);
        openChatText.setGravity(Gravity.CENTER);
        openChatButton.addView(openChatText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        openChatButton.setOnClickListener(v -> {
            if (foundUser != null) {
                Bundle args = new Bundle();
                args.putLong("user_id", foundUser.id);
                presentFragment(new ChatActivity(args));
            }
        });
        LinearLayout.LayoutParams btnParams = LayoutHelper.createLinear(160, 40, Gravity.CENTER_HORIZONTAL);
        btnParams.topMargin = AndroidUtilities.dp(16);
        resultContainer.addView(openChatButton, btnParams);

        statusTextView = new TextView(context);
        statusTextView.setTextSize(15);
        statusTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(32), AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        statusTextView.setVisibility(View.GONE);
        rootLayout.addView(statusTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        rootLayout.addView(resultContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        return fragmentView;
    }

    private void doSearch() {
        String raw = phoneInput.getText().toString();
        String normalized = raw.replaceAll("[^0-9]", "");
        AndroidUtilities.hideKeyboard(phoneInput);

        resultContainer.setVisibility(View.GONE);
        statusTextView.setVisibility(View.GONE);
        foundUser = null;

        if (TextUtils.isEmpty(normalized)) {
            statusTextView.setText("Please enter a phone number.");
            statusTextView.setVisibility(View.VISIBLE);
            return;
        }

        TLRPC.User matched = null;
        for (TLRPC.TL_contact contact : ContactsController.getInstance(currentAccount).contacts) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(contact.user_id);
            if (user != null && user.phone != null) {
                String userPhone = user.phone.replaceAll("[^0-9]", "");
                if (userPhone.endsWith(normalized) || normalized.endsWith(userPhone)) {
                    matched = user;
                    break;
                }
            }
        }

        if (matched != null) {
            foundUser = matched;
            String fullName = ContactsController.formatName(matched.first_name, matched.last_name);
            nameTextView.setText(fullName);
            if (matched.username != null) {
                usernameTextView.setText("@" + matched.username);
                usernameTextView.setVisibility(View.VISIBLE);
            } else {
                usernameTextView.setVisibility(View.GONE);
            }
            AvatarDrawable avatarDrawable = new AvatarDrawable(matched);
            avatarImageView.setForUserOrChat(matched, avatarDrawable);
            resultContainer.setVisibility(View.VISIBLE);
        } else {
            statusTextView.setText("No contact found with this number.\nMake sure the number is saved in your contacts.");
            statusTextView.setVisibility(View.VISIBLE);
        }
    }
}
