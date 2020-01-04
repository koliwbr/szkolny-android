package pl.szczodrzynski.edziennik.ui.modules.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.danielstone.materialaboutlibrary.ConvenienceBuilder;
import com.danielstone.materialaboutlibrary.MaterialAboutFragment;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionSwitchItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItemOnClickAction;
import com.danielstone.materialaboutlibrary.items.MaterialAboutProfileItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutSwitchItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;
import com.mikepenz.iconics.typeface.library.szkolny.font.SzkolnyFont;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.Notifier;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore;
import pl.szczodrzynski.edziennik.network.NetworkUtils;
import pl.szczodrzynski.edziennik.network.ServerRequest;
import pl.szczodrzynski.edziennik.receivers.BootReceiver;
import pl.szczodrzynski.edziennik.sync.SyncWorker;
import pl.szczodrzynski.edziennik.ui.dialogs.changelog.ChangelogDialog;
import pl.szczodrzynski.edziennik.ui.dialogs.settings.ProfileRemoveDialog;
import pl.szczodrzynski.edziennik.ui.modules.home.HomeFragmentOld;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.Utils;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;

import static android.app.Activity.RESULT_OK;
import static pl.szczodrzynski.edziennik.App.APP_URL;
import static pl.szczodrzynski.edziennik.ExtensionsKt.initDefaultLocale;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_DISABLED;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_ENABLED;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.YEAR_1_AVG_2_AVG;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.YEAR_1_AVG_2_SEM;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.YEAR_1_SEM_2_AVG;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.YEAR_1_SEM_2_SEM;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.YEAR_ALL_GRADES;
import static pl.szczodrzynski.edziennik.utils.Utils.d;
import static pl.szczodrzynski.edziennik.utils.Utils.getRealPathFromURI;
import static pl.szczodrzynski.edziennik.utils.Utils.getResizedBitmap;

public class SettingsNewFragment extends MaterialAboutFragment {

    private static final String TAG = "Settings";
    private App app = null;
    private MainActivity activity;

    private static final int CARD_PROFILE = 0;
    private static final int CARD_THEME = 1;
    private static final int CARD_SYNC = 2;
    private static final int CARD_REGISTER = 3;
    private static final int CARD_ABOUT = 4;
    private int iconColor = Color.WHITE;
    private int primaryTextOnPrimaryBg = -1;
    private int secondaryTextOnPrimaryBg = -1;
    private int iconSizeDp = 20;

    private MaterialAboutCard getCardWithItems(CharSequence title, ArrayList<MaterialAboutItem> items, boolean primaryColor) {
        MaterialAboutCard card = new MaterialAboutCard.Builder().title(title).cardColor(0xff1976D2).build();
        card.getItems().addAll(items);
        return card;
    }
    private MaterialAboutCard getCardWithItems(CharSequence title, ArrayList<MaterialAboutItem> items) {
        MaterialAboutCard card = new MaterialAboutCard.Builder().title(title).cardColor(Utils.getAttr(activity, R.attr.mal_card_background)).build();
        card.getItems().addAll(items);
        return card;
    }
    private void addCardItems(int cardIndex, ArrayList<MaterialAboutItem> itemsNew) {
        ArrayList<MaterialAboutItem> items = getList().getCards().get(cardIndex).getItems();
        items.remove(items.size() - 1);
        items.addAll(itemsNew);
        refreshMaterialAboutList();
    }
    private void addCardItem(int cardIndex, int itemIndex, MaterialAboutItem item) {
        ArrayList<MaterialAboutItem> items = getList().getCards().get(cardIndex).getItems();
        items.add(itemIndex, item);
        refreshMaterialAboutList();
    }
    private void removeCardItem(int cardIndex, int itemIndex) {
        ArrayList<MaterialAboutItem> items = getList().getCards().get(cardIndex).getItems();
        items.remove(itemIndex);
        refreshMaterialAboutList();
    }
    private MaterialAboutActionItem getMoreItem(MaterialAboutItemOnClickAction onClickAction) {
        return new MaterialAboutActionItem(
                getString(R.string.settings_more_text),
                null,
                new IconicsDrawable(activity)
                        .icon(CommunityMaterial.Icon.cmd_chevron_down)
                        .size(IconicsSize.dp(14))
                        .color(IconicsColor.colorInt(iconColor)),
                onClickAction
        );
    }


    /*    _____            __ _ _
         |  __ \          / _(_) |
         | |__) | __ ___ | |_ _| | ___
         |  ___/ '__/ _ \|  _| | |/ _ \
         | |   | | | (_) | | | | |  __/
         |_|   |_|  \___/|_| |_|_|\__*/
    private Drawable getProfileDrawable() {
        return app.profile.getImageDrawable(activity);

        /*Bitmap profileImage = null;
        if (app.profile.getImage() != null && !app.profile.getImage().equals("")) {
            profileImage = BitmapFactory.decodeFile(app.profile.getImage());
            RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(getResources(), profileImage);
            roundDrawable.setCircular(true);
            return roundDrawable;
        }
        return getDrawableFromRes(getContext(), R.drawable.profile);*/

        /*if (profileImage == null) {
            profileImage = BitmapFactory.decodeResource(getResources(), R.drawable.profile);
        }
        profileImage = ThumbnailUtils.extractThumbnail(profileImage, Math.min(profileImage.getWidth(), profileImage.getHeight()), Math.min(profileImage.getWidth(), profileImage.getHeight()));
        RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(getResources(), profileImage);
        roundDrawable.setCircular(true);
        return roundDrawable;*/
    }
    private MaterialAboutProfileItem profileCardTitleItem;
    private ArrayList<MaterialAboutItem> getProfileCard(boolean expandedOnly) {
        ArrayList<MaterialAboutItem> items = new ArrayList<>();
        if (!expandedOnly) {

            profileCardTitleItem = new MaterialAboutProfileItem(
                    app.profile.getName(),
                    getString(R.string.settings_profile_subtitle_format, app.profile.getSubname()),
                    getProfileDrawable()
            );
            profileCardTitleItem.setOnClickAction(() -> {
                new MaterialDialog.Builder(activity)
                        .title(R.string.settings_profile_change_title)
                        .items(
                                getString(R.string.settings_profile_change_name),
                                getString(R.string.settings_profile_change_image),
                                app.profile.getImage() == null ? null : getString(R.string.settings_profile_remove_image)
                        )
                        .itemsCallback((dialog, itemView, position, text) -> {
                            switch (position) {
                                case 0:
                                    new MaterialDialog.Builder(activity)
                                            .title(getString(R.string.settings_profile_change_name_dialog_title))
                                            .input(getString(R.string.settings_profile_change_name_dialog_text), app.profile.getName(), (dialog1, input) -> {
                                                app.profile.setName(input.toString());
                                                profileCardTitleItem.setText(input);
                                                profileCardTitleItem.setIcon(getProfileDrawable());
                                                refreshMaterialAboutList();
                                                app.profileSaveAsync();
                                            })
                                            .positiveText(R.string.ok)
                                            .negativeText(R.string.cancel)
                                            .show();
                                    break;
                                case 1:
                                    startActivityForResult(CropImage.getPickImageChooserIntent(activity), 21);
                                    break;
                                case 2:
                                    app.profile.setImage(null);
                                    profileCardTitleItem.setIcon(getProfileDrawable());
                                    refreshMaterialAboutList();
                                    app.profileSaveAsync();
                                    break;
                            }
                        })
                        .negativeText(R.string.cancel)
                        .show();
            });
            items.add(profileCardTitleItem);

            /*items.add(
                    new MaterialAboutActionItem(
                            getString(R.string.settings_profile_change_password_text),
                            getString(R.string.settings_profile_change_password_subtext),
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon2.cmd_key_variant)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setOnClickAction(() -> {

                    })
            );*/

            items.add(
                    new MaterialAboutSwitchItem(
                            getString(R.string.settings_profile_sync_text),
                            getString(R.string.settings_profile_sync_subtext),
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon.cmd_account_convert)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setChecked(app.profile.getSyncEnabled())
                    .setOnChangeAction(((isChecked, tag) -> {
                        app.profile.setSyncEnabled(isChecked);
                        app.profileSaveAsync();
                        return true;
                    }))
            );

            items.add(getMoreItem(() -> addCardItems(CARD_PROFILE, getProfileCard(true))));
        }
        else {

            /*items.add(
                    new MaterialAboutSwitchItem(
                            getString(R.string.settings_profile_notify_text),
                            getString(R.string.settings_profile_notify_subtext),
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon.cmd_bell_ring)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setChecked(app.profile.getSyncNotifications())
                    .setOnChangeAction(((isChecked, tag) -> {
                        app.profile.setSyncNotifications(isChecked);
                        app.profileSaveAsync();
                        return true;
                    }))
            );*/

            items.add(
                    new MaterialAboutActionItem(
                            getString(R.string.settings_profile_remove_text),
                            getString(R.string.settings_profile_remove_subtext),
                            new IconicsDrawable(activity)
                                    .icon(SzkolnyFont.Icon.szf_delete_empty_outline)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setOnClickAction(() -> {
                        new ProfileRemoveDialog(activity, app.profile.getId(), app.profile.getName());
                    })
            );

        }
        return items;
    }

    /*    _______ _
         |__   __| |
            | |  | |__   ___ _ __ ___   ___
            | |  | '_ \ / _ \ '_ ` _ \ / _ \
            | |  | | | |  __/ | | | | |  __/
            |_|  |_| |_|\___|_| |_| |_|\__*/
    private ArrayList<MaterialAboutItem> getThemeCard(boolean expandedOnly) {
        ArrayList<MaterialAboutItem> items = new ArrayList<>();
        if (!expandedOnly) {

            Date today = Date.getToday();
            if (today.month == 12 || today.month == 1) {
                items.add(
                        new MaterialAboutSwitchItem(
                                getString(R.string.settings_theme_snowfall_text),
                                getString(R.string.settings_theme_snowfall_subtext),
                                new IconicsDrawable(activity)
                                        .icon(CommunityMaterial.Icon2.cmd_snowflake)
                                        .size(IconicsSize.dp(iconSizeDp))
                                        .color(IconicsColor.colorInt(iconColor))
                        )
                        .setChecked(app.config.getUi().getSnowfall())
                        .setOnChangeAction((isChecked, tag) -> {
                            app.config.getUi().setSnowfall(isChecked);
                            activity.recreate();
                            return true;
                        })
                );
            }

            items.add(
                    new MaterialAboutActionItem(
                            getString(R.string.settings_theme_theme_text),
                            Themes.INSTANCE.getThemeName(activity),
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon2.cmd_palette_outline)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setOnClickAction(() -> {
                        new MaterialDialog.Builder(activity)
                                .title(R.string.settings_theme_theme_text)
                                .items(Themes.INSTANCE.getThemeNames(activity))
                                .itemsCallbackSingleChoice(app.config.getUi().getTheme(), (dialog, itemView, which, text) -> {
                                    if (app.config.getUi().getTheme() == which)
                                        return true;
                                    app.config.getUi().setTheme(which);
                                    Themes.INSTANCE.setThemeInt(app.config.getUi().getTheme());
                                    activity.recreate();
                                    return true;
                                })
                                .show();
                    })
            );

            items.add(
                    new MaterialAboutSwitchItem(
                            getString(R.string.settings_theme_mini_drawer_text),
                            getString(R.string.settings_theme_mini_drawer_subtext),
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon.cmd_dots_vertical)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setChecked(app.config.getUi().getMiniMenuVisible())
                    .setOnChangeAction((isChecked, tag) -> {
                        // 0,1  1
                        // 0,0  0
                        // 1,1  0
                        // 1,0  1
                        app.config.getUi().setMiniMenuVisible(isChecked);
                        activity.getNavView().drawer.setMiniDrawerVisiblePortrait(isChecked);
                        return true;
                    })
            );

            items.add(getMoreItem(() -> addCardItems(CARD_THEME, getThemeCard(true))));
        }
        else {

            items.add(
                    new MaterialAboutActionItem(
                            getString(R.string.settings_theme_mini_drawer_buttons_text),
                            null,
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon.cmd_format_list_checks)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setOnClickAction(() -> {
                        List<Integer> buttonIds = new ArrayList<>();
                        buttonIds.add(MainActivity.DRAWER_ITEM_HOME);
                        buttonIds.add(MainActivity.DRAWER_ITEM_TIMETABLE);
                        buttonIds.add(MainActivity.DRAWER_ITEM_AGENDA);
                        buttonIds.add(MainActivity.DRAWER_ITEM_GRADES);
                        buttonIds.add(MainActivity.DRAWER_ITEM_MESSAGES);
                        buttonIds.add(MainActivity.DRAWER_ITEM_HOMEWORK);
                        buttonIds.add(MainActivity.DRAWER_ITEM_BEHAVIOUR);
                        buttonIds.add(MainActivity.DRAWER_ITEM_ATTENDANCE);
                        buttonIds.add(MainActivity.DRAWER_ITEM_ANNOUNCEMENTS);
                        buttonIds.add(MainActivity.DRAWER_ITEM_NOTIFICATIONS);
                        buttonIds.add(MainActivity.DRAWER_ITEM_SETTINGS);
                        //buttonIds.add(MainActivity.DRAWER_ITEM_DEBUG);
                        List<String> buttonCaptions = new ArrayList<>();
                        buttonCaptions.add(getString(R.string.menu_home_page));
                        buttonCaptions.add(getString(R.string.menu_timetable));
                        buttonCaptions.add(getString(R.string.menu_agenda));
                        buttonCaptions.add(getString(R.string.menu_grades));
                        buttonCaptions.add(getString(R.string.menu_messages));
                        buttonCaptions.add(getString(R.string.menu_homework));
                        buttonCaptions.add(getString(R.string.menu_notices));
                        buttonCaptions.add(getString(R.string.menu_attendance));
                        buttonCaptions.add(getString(R.string.menu_announcements));
                        buttonCaptions.add(getString(R.string.menu_notifications));
                        buttonCaptions.add(getString(R.string.menu_settings));
                        //buttonCaptions.add(getString(R.string.title_debugging));
                        List<Integer> selectedIds = new ArrayList<>();
                        for (int id: app.config.getUi().getMiniMenuButtons()) {
                            selectedIds.add(buttonIds.indexOf(id));
                        }
                        new MaterialDialog.Builder(activity)
                                .title(R.string.settings_theme_mini_drawer_buttons_dialog_title)
                                .content(getString(R.string.settings_theme_mini_drawer_buttons_dialog_text))
                                .items(buttonCaptions)
                                .itemsCallbackMultiChoice(selectedIds.toArray(new Integer[0]), (dialog, which, text) -> {
                                    List<Integer> list = new ArrayList<>();
                                    for (int index: which) {
                                        if (index == -1)
                                            continue;
                                        // wtf

                                        int id = buttonIds.get(index);
                                        list.add(id);
                                    }
                                    app.config.getUi().setMiniMenuButtons(list);
                                    activity.setDrawerItems();
                                    activity.getDrawer().updateBadges();
                                    return true;
                                })
                                .positiveText(R.string.ok)
                                .show();
                    })
            );

            items.add(
                    new MaterialAboutActionItem(
                            getString(R.string.settings_theme_drawer_header_text),
                            null,
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon2.cmd_image_outline)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setOnClickAction(() -> {
                        if (app.config.getUi().getHeaderBackground() != null) {
                            new MaterialDialog.Builder(activity)
                                    .title(R.string.what_do_you_want_to_do)
                                    .items(getString(R.string.settings_theme_drawer_header_dialog_set), getString(R.string.settings_theme_drawer_header_dialog_restore))
                                    .itemsCallback((dialog, itemView, position, text) -> {
                                        if (position == 0) {
                                            startActivityForResult(CropImage.getPickImageChooserIntent(activity), 22);
                                        } else {
                                            MainActivity ac = (MainActivity) getActivity();
                                            app.config.getUi().setHeaderBackground(null);
                                            if (ac != null) {
                                                ac.getDrawer().setAccountHeaderBackground(null);
                                                ac.getDrawer().open();
                                            }
                                        }
                                    })
                                    .show();
                        }
                        else {
                            startActivityForResult(CropImage.getPickImageChooserIntent(activity), 22);
                        }
                    })
            );

            items.add(
                    new MaterialAboutActionItem(
                            getString(R.string.settings_theme_app_background_text),
                            getString(R.string.settings_theme_app_background_subtext),
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon2.cmd_image_filter_hdr)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setOnClickAction(() -> {
                        if (app.config.getUi().getAppBackground() != null) {
                            new MaterialDialog.Builder(activity)
                                    .title(R.string.what_do_you_want_to_do)
                                    .items(getString(R.string.settings_theme_app_background_dialog_set), getString(R.string.settings_theme_app_background_dialog_restore))
                                    .itemsCallback((dialog, itemView, position, text) -> {
                                        if (position == 0) {
                                            startActivityForResult(CropImage.getPickImageChooserIntent(activity), 23);
                                        } else {
                                            app.config.getUi().setAppBackground(null);
                                            activity.recreate();
                                        }
                                    })
                                    .show();
                        }
                        else {
                            startActivityForResult(CropImage.getPickImageChooserIntent(activity), 23);
                        }
                    })
            );

            items.add(
                    new MaterialAboutSwitchItem(
                            getString(R.string.settings_theme_open_drawer_on_back_pressed_text),
                            null,
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon2.cmd_menu_open)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                            .setChecked(app.config.getUi().getOpenDrawerOnBackPressed())
                            .setOnChangeAction((isChecked, tag) -> {
                                app.config.getUi().setOpenDrawerOnBackPressed(isChecked);
                                return true;
                            })
            );
        }
        return items;
    }

    /*     _____
          / ____|
         | (___  _   _ _ __   ___
          \___ \| | | | '_ \ / __|
          ____) | |_| | | | | (__
         |_____/ \__, |_| |_|\___|
                  __/ |
                 |__*/
    private String getSyncCardIntervalSubText() {

        if (app.config.getSync().getInterval() < 60 * 60)
            return getString(
                    R.string.settings_sync_sync_interval_subtext_format,
                    HomeFragmentOld.plural(activity, R.plurals.time_till_minutes, app.config.getSync().getInterval() / 60)
            );
        return getString(
                R.string.settings_sync_sync_interval_subtext_format,
                HomeFragmentOld.plural(activity, R.plurals.time_till_hours, app.config.getSync().getInterval() / 60 / 60) +
                (app.config.getSync().getInterval() / 60 % 60 == 0 ?
                        "" :
                        " " + HomeFragmentOld.plural(activity, R.plurals.time_till_minutes, app.config.getSync().getInterval() / 60 % 60)
                )
        );
    }
    private String getSyncCardQuietHoursSubText() {
        return getString(
                app.config.getSync().getQuietHoursStart() >= app.config.getSync().getQuietHoursEnd() ? R.string.settings_sync_quiet_hours_subtext_next_day_format : R.string.settings_sync_quiet_hours_subtext_format,
                Time.fromMillis(Math.abs(app.config.getSync().getQuietHoursStart())).getStringHM(),
                Time.fromMillis(app.config.getSync().getQuietHoursEnd()).getStringHM()
        );
    }
    private MaterialAboutItem getSyncCardWifiItem() {
        return new MaterialAboutSwitchItem(
                getString(R.string.settings_sync_wifi_text),
                getString(R.string.settings_sync_wifi_subtext),
                new IconicsDrawable(activity)
                        .icon(CommunityMaterial.Icon2.cmd_wifi_strength_2)
                        .size(IconicsSize.dp(iconSizeDp))
                        .color(IconicsColor.colorInt(iconColor))
        )
        .setChecked(app.config.getSync().getOnlyWifi())
        .setOnChangeAction((isChecked, tag) -> {
            app.config.getSync().setOnlyWifi(isChecked);
            SyncWorker.Companion.rescheduleNext(app);
            return true;
        });
    }
    private MaterialAboutActionSwitchItem syncCardIntervalItem;
    private MaterialAboutActionSwitchItem syncCardQuietHoursItem;
    private ArrayList<MaterialAboutItem> getSyncCard(boolean expandedOnly) {
        ArrayList<MaterialAboutItem> items = new ArrayList<>();
        if (!expandedOnly) {

            syncCardIntervalItem = new MaterialAboutActionSwitchItem(
                    getString(R.string.settings_sync_sync_interval_text),
                    getString(R.string.settings_sync_sync_interval_subtext_disabled),
                    new IconicsDrawable(activity)
                            .icon(CommunityMaterial.Icon.cmd_download_outline)
                            .size(IconicsSize.dp(iconSizeDp))
                            .color(IconicsColor.colorInt(iconColor))
            );
            syncCardIntervalItem.setSubTextChecked(getSyncCardIntervalSubText());
            syncCardIntervalItem.setChecked(app.config.getSync().getEnabled());
            syncCardIntervalItem.setOnClickAction(() -> {
                List<CharSequence> intervalNames = new ArrayList<>();
                if (App.devMode && false) {
                    intervalNames.add(HomeFragmentOld.plural(activity, R.plurals.time_till_seconds, 30));
                    intervalNames.add(HomeFragmentOld.plural(activity, R.plurals.time_till_minutes, 2));
                }
                intervalNames.add(HomeFragmentOld.plural(activity, R.plurals.time_till_minutes, 30));
                intervalNames.add(HomeFragmentOld.plural(activity, R.plurals.time_till_minutes, 45));
                intervalNames.add(HomeFragmentOld.plural(activity, R.plurals.time_till_hours, 1));
                intervalNames.add(HomeFragmentOld.plural(activity, R.plurals.time_till_hours, 1)+" "+ HomeFragmentOld.plural(activity, R.plurals.time_till_minutes, 30));
                intervalNames.add(HomeFragmentOld.plural(activity, R.plurals.time_till_hours, 2));
                intervalNames.add(HomeFragmentOld.plural(activity, R.plurals.time_till_hours, 3));
                intervalNames.add(HomeFragmentOld.plural(activity, R.plurals.time_till_hours, 4));
                List<Integer> intervals = new ArrayList<>();
                if (App.devMode && false) {
                    intervals.add(30);
                    intervals.add(2 * 60);
                }
                intervals.add(30 * 60);
                intervals.add(45 * 60);
                intervals.add(60 * 60);
                intervals.add(90 * 60);
                intervals.add(120 * 60);
                intervals.add(180 * 60);
                intervals.add(240 * 60);
                new MaterialDialog.Builder(activity)
                        .title(getString(R.string.settings_sync_sync_interval_dialog_title))
                        .content(getString(R.string.settings_sync_sync_interval_dialog_text))
                        .items(intervalNames)
                        .itemsCallbackSingleChoice(intervals.indexOf(app.config.getSync().getInterval()), (dialog, itemView, which, text) -> {
                            app.config.getSync().setInterval(intervals.get(which));
                            syncCardIntervalItem.setSubTextChecked(getSyncCardIntervalSubText());
                            syncCardIntervalItem.setChecked(true);
                            if (!app.config.getSync().getEnabled()) {
                                addCardItem(CARD_SYNC, 1, getSyncCardWifiItem());
                            }
                            else {
                                refreshMaterialAboutList();
                            }
                            app.config.getSync().setEnabled(true);
                            // app.appConfig modifications have to surround syncCardIntervalItem and those ifs
                            SyncWorker.Companion.rescheduleNext(app);
                            return true;
                        })
                        .show();
            });
            syncCardIntervalItem.setOnChangeAction((isChecked, tag) -> {
                if (isChecked && !app.config.getSync().getEnabled()) {
                    addCardItem(CARD_SYNC, 1, getSyncCardWifiItem());
                }
                else if (!isChecked) {
                    removeCardItem(CARD_SYNC, 1);
                }
                app.config.getSync().setEnabled(isChecked);
                SyncWorker.Companion.rescheduleNext(app);
                return true;
            });
            items.add(syncCardIntervalItem);

            if (app.config.getSync().getEnabled()) {
                items.add(getSyncCardWifiItem());
            }

           /* items.add(
                    new MaterialAboutSwitchItem(
                            "Cisza na lekcjach",
                            "Nie odtwarzaj dźwięku powiadomień podczas lekcji",
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon2.cmd_volume_off)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setChecked(app.appConfig.quietDuringLessons)
                    .setOnChangeAction((isChecked) -> {
                        app.appConfig.quietDuringLessons = isChecked;
                        app.saveConfig("quietDuringLessons");
                        return true;
                    })
            );*/


            syncCardQuietHoursItem = new MaterialAboutActionSwitchItem(
                    getString(R.string.settings_sync_quiet_hours_text),
                    getString(R.string.settings_sync_quiet_hours_subtext_disabled),
                    new IconicsDrawable(activity)
                            .icon(CommunityMaterial.Icon.cmd_bell_sleep_outline)
                            .size(IconicsSize.dp(iconSizeDp))
                            .color(IconicsColor.colorInt(iconColor))
            );
            syncCardQuietHoursItem.setChecked(app.config.getSync().getQuietHoursStart() > 0);
            syncCardQuietHoursItem.setSubTextChecked(getSyncCardQuietHoursSubText());
            syncCardQuietHoursItem.setOnClickAction(() -> {
                new MaterialDialog.Builder(activity)
                        .title(R.string.settings_sync_quiet_hours_dialog_title)
                        .items(
                                getString(R.string.settings_sync_quiet_hours_set_beginning),
                                getString(R.string.settings_sync_quiet_hours_set_end)
                        )
                        .itemsCallback((dialog, itemView, position, text) -> {
                            if (position == 0) {
                                // set beginning
                                Time time = Time.fromMillis(Math.abs(app.config.getSync().getQuietHoursStart()));
                                TimePickerDialog.newInstance((v2, hourOfDay, minute, second) -> {
                                    // if it's disabled, it'll be enabled automatically
                                    app.config.getSync().setQuietHoursStart(new Time(hourOfDay, minute, second).getInMillis());
                                    syncCardQuietHoursItem.setChecked(true);
                                    syncCardQuietHoursItem.setSubTextChecked(getSyncCardQuietHoursSubText());
                                    refreshMaterialAboutList();
                                }, time.hour, time.minute, 0, true).show(activity.getSupportFragmentManager(), "TimePickerDialog");
                            }
                            else {
                                // set end
                                Time time = Time.fromMillis(app.config.getSync().getQuietHoursEnd());
                                TimePickerDialog.newInstance((v2, hourOfDay, minute, second) -> {
                                    if (app.config.getSync().getQuietHoursStart() < 0) {
                                        // if it's disabled, enable
                                        app.config.getSync().setQuietHoursStart(-1 * app.config.getSync().getQuietHoursStart());
                                    }
                                    app.config.getSync().setQuietHoursEnd(new Time(hourOfDay, minute, second).getInMillis());
                                    syncCardQuietHoursItem.setChecked(true);
                                    syncCardQuietHoursItem.setSubTextChecked(getSyncCardQuietHoursSubText());
                                    refreshMaterialAboutList();
                                }, time.hour, time.minute, 0, true).show(activity.getSupportFragmentManager(), "TimePickerDialog");
                            }
                        })
                        .show();
            });
            syncCardQuietHoursItem.setOnChangeAction((isChecked, tag) -> {
                if (isChecked && app.config.getSync().getQuietHoursStart() < 0) {
                    app.config.getSync().setQuietHoursStart(app.config.getSync().getQuietHoursStart() * -1);
                }
                else if (!isChecked && app.config.getSync().getQuietHoursStart() > 0) {
                    app.config.getSync().setQuietHoursStart(app.config.getSync().getQuietHoursStart() * -1);
                }
                else if (isChecked && app.config.getSync().getQuietHoursStart() == 0) {
                    app.config.getSync().setQuietHoursStart(new Time(22, 30, 0).getInMillis());
                    app.config.getSync().setQuietHoursEnd(new Time(5, 30, 0).getInMillis());
                    syncCardQuietHoursItem.setSubTextChecked(getSyncCardQuietHoursSubText());
                    refreshMaterialAboutList();
                }
                return true;
            });
            items.add(syncCardQuietHoursItem);

            items.add(
                    new MaterialAboutActionItem(
                            getString(R.string.settings_sync_web_push_text),
                            getString(R.string.settings_sync_web_push_subtext),
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon2.cmd_laptop)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setOnClickAction(() -> {
                        activity.loadTarget(MainActivity.TARGET_WEB_PUSH, null);
                    })
            );

            items.add(getMoreItem(() -> addCardItems(CARD_SYNC, getSyncCard(true))));
        }
        else {
            // TODO: 2019-04-27 add notification sound options: szkolny.eu, system default, custom
            /*items.add(
                    new MaterialAboutActionItem(
                            "Dźwięk powiadomień",
                            "Szkolny.eu",
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon2.cmd_volume_high)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setOnClickAction(() -> {

                    })
            );*/

            items.add(
                    new MaterialAboutSwitchItem(
                            getString(R.string.settings_sync_updates_text),
                            null,
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon.cmd_cellphone_arrow_down)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setChecked(app.config.getSync().getNotifyAboutUpdates())
                    .setOnChangeAction((isChecked, tag) -> {
                        app.config.getSync().setNotifyAboutUpdates(isChecked);
                        return true;
                    })
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                items.add(
                        new MaterialAboutActionItem(
                                getString(R.string.settings_sync_notifications_settings_text),
                                getString(R.string.settings_sync_notifications_settings_subtext),
                                new IconicsDrawable(activity)
                                        .icon(CommunityMaterial.Icon2.cmd_settings_outline)
                                        .size(IconicsSize.dp(iconSizeDp))
                                        .color(IconicsColor.colorInt(iconColor))
                        )
                        .setOnClickAction(() -> {
                            String channel = Notifier.GROUP_KEY_NOTIFICATIONS;
                            Intent intent = new Intent();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                                if (channel != null) {
                                    intent.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                                    intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel);
                                } else {
                                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                                }
                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                if (channel != null) {
                                    intent.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                                    intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel);
                                } else {
                                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                                }
                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                            } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                            } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                                intent.putExtra("app_package", activity.getPackageName());
                                intent.putExtra("app_uid", activity.getApplicationInfo().uid);
                            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                            }
                            activity.startActivity(intent);
                        })
                );
            }
        }
        return items;
    }

    /*    _____            _     _
         |  __ \          (_)   | |
         | |__) |___  __ _ _ ___| |_ ___ _ __
         |  _  // _ \/ _` | / __| __/ _ \ '__|
         | | \ \  __/ (_| | \__ \ ||  __/ |
         |_|  \_\___|\__, |_|___/\__\___|_|
                      __/ |
                     |__*/
    private String getRegisterCardAverageModeSubText() {
        switch (App.getConfig().forProfile().getGrades().getYearAverageMode()) {
            default:
            case YEAR_1_AVG_2_AVG:
                return getString(R.string.settings_register_avg_mode_0_short);
            case YEAR_1_SEM_2_AVG:
                return getString(R.string.settings_register_avg_mode_1_short);
            case YEAR_1_AVG_2_SEM:
                return getString(R.string.settings_register_avg_mode_2_short);
            case YEAR_1_SEM_2_SEM:
                return getString(R.string.settings_register_avg_mode_3_short);
            case YEAR_ALL_GRADES:
                return getString(R.string.settings_register_avg_mode_4_short);
        }
    }
    private String getRegisterCardBellSyncSubText() {
        if (app.config.getTimetable().getBellSyncDiff() == null)
            return getString(R.string.settings_register_bell_sync_subtext_disabled);
        return getString(
                R.string.settings_register_bell_sync_subtext_format,
                (app.config.getTimetable().getBellSyncMultiplier() == -1 ? "-" : "+") + app.config.getTimetable().getBellSyncDiff().getStringHMS()
        );
    }
    private MaterialAboutItem getRegisterCardSharedEventsItem() {
        return new MaterialAboutSwitchItem(
                getString(R.string.settings_register_shared_events_text),
                getString(R.string.settings_register_shared_events_subtext),
                new IconicsDrawable(activity)
                        .icon(CommunityMaterial.Icon2.cmd_share_outline)
                        .size(IconicsSize.dp(iconSizeDp))
                        .color(IconicsColor.colorInt(iconColor))
        )
        .setChecked(app.profile.getEnableSharedEvents())
        .setOnChangeAction((isChecked, tag) -> {
            app.profile.setEnableSharedEvents(isChecked);
            app.profileSaveAsync();
            if (isChecked) new MaterialDialog.Builder(activity)
                    .title(R.string.event_sharing)
                    .content(getString(R.string.settings_register_shared_events_dialog_enabled_text))
                    .positiveText(R.string.ok)
                    .show();
            else new MaterialDialog.Builder(activity)
                    .title(R.string.event_sharing)
                    .content(getString(R.string.settings_register_shared_events_dialog_disabled_text))
                    .positiveText(R.string.ok)
                    .show();
            return true;
        });
    }
    private MaterialAboutActionItem registerCardAverageModeItem;
    private MaterialAboutSwitchItem registerCardAllowRegistrationItem;
    private MaterialAboutActionItem registerCardBellSyncItem;
    private ArrayList<MaterialAboutItem> getRegisterCard(boolean expandedOnly) {
        ArrayList<MaterialAboutItem> items = new ArrayList<>();
        if (!expandedOnly) {

            registerCardAverageModeItem = new MaterialAboutActionItem(
                    getString(R.string.settings_register_avg_mode_text),
                    getRegisterCardAverageModeSubText(),
                    new IconicsDrawable(activity)
                            .icon(CommunityMaterial.Icon2.cmd_scale_balance)
                            .size(IconicsSize.dp(iconSizeDp))
                            .color(IconicsColor.colorInt(iconColor))
            );
            registerCardAverageModeItem.setOnClickAction(() -> {
                List<CharSequence> modeNames = new ArrayList<>();
                modeNames.add(getString(R.string.settings_register_avg_mode_4));
                modeNames.add(getString(R.string.settings_register_avg_mode_0));
                modeNames.add(getString(R.string.settings_register_avg_mode_1));
                modeNames.add(getString(R.string.settings_register_avg_mode_2));
                modeNames.add(getString(R.string.settings_register_avg_mode_3));
                List<Integer> modeIds = new ArrayList<>();
                modeIds.add(YEAR_ALL_GRADES);
                modeIds.add(YEAR_1_AVG_2_AVG);
                modeIds.add(YEAR_1_SEM_2_AVG);
                modeIds.add(YEAR_1_AVG_2_SEM);
                modeIds.add(YEAR_1_SEM_2_SEM);
                new MaterialDialog.Builder(activity)
                        .title(getString(R.string.settings_register_avg_mode_dialog_title))
                        .content(getString(R.string.settings_register_avg_mode_dialog_text))
                        .items(modeNames)
                        .itemsCallbackSingleChoice(modeIds.indexOf(App.getConfig().forProfile().getGrades().getYearAverageMode()), (dialog, itemView, which, text) -> {
                            App.getConfig().forProfile().getGrades().setYearAverageMode(modeIds.get(which));
                            registerCardAverageModeItem.setSubText(getRegisterCardAverageModeSubText());
                            refreshMaterialAboutList();
                            return true;
                        })
                        .show();
            });
            items.add(registerCardAverageModeItem);

            registerCardAllowRegistrationItem = new MaterialAboutSwitchItem(
                    getString(R.string.settings_register_allow_registration_text),
                    getString(R.string.settings_register_allow_registration_subtext),
                    new IconicsDrawable(activity)
                            .icon(CommunityMaterial.Icon.cmd_account_circle_outline)
                            .size(IconicsSize.dp(iconSizeDp))
                            .color(IconicsColor.colorInt(iconColor))
            );
            registerCardAllowRegistrationItem.setChecked(app.profile.getRegistration() == REGISTRATION_ENABLED);
            registerCardAllowRegistrationItem.setOnChangeAction((isChecked, tag) -> {
                if (isChecked) new MaterialDialog.Builder(activity)
                            .title(getString(R.string.settings_register_allow_registration_dialog_enabled_title))
                            .content(getString(R.string.settings_register_allow_registration_dialog_enabled_text))
                            .positiveText(R.string.i_agree)
                            .negativeText(R.string.not_now)
                            .onPositive(((dialog, which) -> {
                                registerCardAllowRegistrationItem.setChecked(true);
                                app.profile.setRegistration(REGISTRATION_ENABLED);
                                app.profileSaveAsync();
                                addCardItem(CARD_REGISTER, 2, getRegisterCardSharedEventsItem());
                                refreshMaterialAboutList();
                            }))
                            .show();
                else new MaterialDialog.Builder(activity)
                            .title(getString(R.string.settings_register_allow_registration_dialog_disabled_title))
                            .content(getString(R.string.settings_register_allow_registration_dialog_disabled_text))
                            .positiveText(R.string.ok)
                            .negativeText(R.string.cancel)
                            .onPositive(((dialog, which) -> {
                                registerCardAllowRegistrationItem.setChecked(false);
                                app.profile.setRegistration(REGISTRATION_DISABLED);
                                app.profileSaveAsync();
                                removeCardItem(CARD_REGISTER, 2);
                                refreshMaterialAboutList();
                                MaterialDialog progressDialog = new MaterialDialog.Builder(activity)
                                        .title(getString(R.string.settings_register_allow_registration_dialog_disabling_title))
                                        .content(getString(R.string.settings_register_allow_registration_dialog_disabling_text))
                                        .positiveText(R.string.ok)
                                        .negativeText(R.string.abort)
                                        .show();
                                new ServerRequest(app, app.requestScheme + APP_URL + "main.php?unregister", "Edziennik/UREG", app.profile)
                                        .withUsername(app.profile.getUserCode())
                                        .run((e, result) -> {
                                            progressDialog.dismiss();
                                            if (result == null || !result.get("success").getAsString().equals("true")) {
                                                new MaterialDialog.Builder(activity)
                                                        .title(R.string.error)
                                                        .content(getString(R.string.settings_register_allow_registration_dialog_disabling_error_text))
                                                        .positiveText(R.string.ok)
                                                        .show();
                                            }
                                            else {
                                                Toast.makeText(activity, getString(R.string.settings_register_allow_registration_dialog_disabling_finished), Toast.LENGTH_SHORT).show();
                                            }
                                        });

                            }))
                            .show();
                return false;
            });
            items.add(registerCardAllowRegistrationItem);

            if (app.profile.getRegistration() == REGISTRATION_ENABLED) {
                items.add(getRegisterCardSharedEventsItem());
            }

            items.add(getMoreItem(() -> addCardItems(CARD_REGISTER, getRegisterCard(true))));
        }
        else {

            registerCardBellSyncItem = new MaterialAboutActionItem(
                    getString(R.string.settings_register_bell_sync_text),
                    getRegisterCardBellSyncSubText(),
                    new IconicsDrawable(activity)
                            .icon(SzkolnyFont.Icon.szf_alarm_bell_outline)
                            .size(IconicsSize.dp(iconSizeDp))
                            .color(IconicsColor.colorInt(iconColor))
            );
            registerCardBellSyncItem.setOnClickAction(() -> {
                new MaterialDialog.Builder(activity)
                        .title(R.string.bell_sync_title)
                        .content(R.string.bell_sync_adjust_content)
                        .positiveText(R.string.ok)
                        .negativeText(R.string.cancel)
                        .neutralText(R.string.reset)
                        .inputRangeRes(8, 8, R.color.md_red_500)
                        .input("±H:MM:SS",
                                (app.config.getTimetable().getBellSyncDiff() != null
                                        ? (app.config.getTimetable().getBellSyncMultiplier() == -1 ? "-" : "+") + app.config.getTimetable().getBellSyncDiff().getStringHMS()
                                        : ""), (dialog, input) -> {
                                    if (input == null)
                                        return;
                                    if (input.length() < 8) {
                                        Toast.makeText(activity, R.string.bell_sync_adjust_error, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (input.charAt(2) != ':' || input.charAt(5) != ':') {
                                        Toast.makeText(activity, R.string.bell_sync_adjust_error, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (input.charAt(0) == '+') {
                                        app.config.getTimetable().setBellSyncMultiplier(1);
                                    }
                                    else if (input.charAt(0) == '-') {
                                        app.config.getTimetable().setBellSyncMultiplier(-1);
                                    }
                                    else {
                                        Toast.makeText(activity, R.string.bell_sync_adjust_error, Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    int hour;
                                    int minute;
                                    int second;
                                    try {
                                        hour = Integer.parseInt(input.charAt(1) + "");
                                        minute = Integer.parseInt(input.charAt(3) + "" + input.charAt(4));
                                        second = Integer.parseInt(input.charAt(6) + "" + input.charAt(7));
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                        Toast.makeText(activity, R.string.bell_sync_adjust_error, Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    app.config.getTimetable().setBellSyncDiff(new Time(hour, minute, second));
                                    registerCardBellSyncItem.setSubText(getRegisterCardBellSyncSubText());
                                    refreshMaterialAboutList();
                                })
                        .onNeutral(((dialog, which) -> {
                            app.config.getTimetable().setBellSyncDiff(null);
                            app.config.getTimetable().setBellSyncMultiplier(0);
                            registerCardBellSyncItem.setSubText(getRegisterCardBellSyncSubText());
                            refreshMaterialAboutList();
                        }))
                        .show();
            });
            items.add(registerCardBellSyncItem);

            items.add(
                    new MaterialAboutSwitchItem(
                            getString(R.string.settings_register_dont_count_zero_text),
                            null,
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon2.cmd_numeric_0_box_outline)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                            .setChecked(!app.config.getFor(App.profileId).getGrades().getCountZeroToAvg())
                            .setOnChangeAction((isChecked, tag) -> {
                                app.config.getFor(App.profileId).getGrades().setCountZeroToAvg(!isChecked);
                                app.saveConfig("dontCountZeroToAverage");
                                return true;
                            })
            );

            items.add(
                    new MaterialAboutSwitchItem(
                            getString(R.string.settings_register_count_in_seconds_text),
                            getString(R.string.settings_register_count_in_seconds_subtext),
                            new IconicsDrawable(activity)
                                    .icon(CommunityMaterial.Icon2.cmd_timer)
                                    .size(IconicsSize.dp(iconSizeDp))
                                    .color(IconicsColor.colorInt(iconColor))
                    )
                    .setChecked(app.config.getTimetable().getCountInSeconds())
                    .setOnChangeAction((isChecked, tag) -> {
                        app.config.getTimetable().setCountInSeconds(isChecked);
                        return true;
                    })
            );

            if (app.profile.getLoginStoreType() == LoginStore.LOGIN_TYPE_LIBRUS) {
                items.add(
                        new MaterialAboutSwitchItem(
                                getString(R.string.settings_register_show_teacher_absences_text),
                                null,
                                new IconicsDrawable(activity)
                                        .icon(CommunityMaterial.Icon.cmd_account_arrow_right_outline)
                                        .size(IconicsSize.dp(iconSizeDp))
                                        .color(IconicsColor.colorInt(iconColor))
                        )
                        .setChecked(app.profile.getStudentData("showTeacherAbsences", true))
                        .setOnChangeAction((isChecked, tag) -> {
                            app.profile.putStudentData("showTeacherAbsences", isChecked);
                            app.profileSaveAsync();
                            return true;
                        })
                );
            }

        }
        return items;
    }

    /*             _                 _
             /\   | |               | |
            /  \  | |__   ___  _   _| |_
           / /\ \ | '_ \ / _ \| | | | __|
          / ____ \| |_) | (_) | |_| | |_
         /_/    \_\_.__/ \___/ \__,_|\_*/
    private MaterialAboutActionItem pref_about_version;
    private ArrayList<MaterialAboutItem> getAboutCard(boolean expandedOnly) {
        primaryTextOnPrimaryBg = 0xffffffff;//getColorFromAttr(activity, R.attr.colorOnPrimary);
        secondaryTextOnPrimaryBg = 0xd0ffffff;//activity.getResources().getColor(R.color.secondaryTextLight);

        ArrayList<MaterialAboutItem> items = new ArrayList<>();
        if (!expandedOnly) {
            items.add(new MaterialAboutTitleItem.Builder()
                    .text(R.string.app_name)
                    .desc(R.string.settings_about_title_subtext)
                    .textColor(primaryTextOnPrimaryBg)
                    .descColor(secondaryTextOnPrimaryBg)
                    .icon(R.mipmap.ic_splash)
                    .build());

            pref_about_version = new MaterialAboutActionItem.Builder()
                    .text(R.string.settings_about_version_text)
                    .textColor(primaryTextOnPrimaryBg)
                    .subTextColor(secondaryTextOnPrimaryBg)
                    .subText(BuildConfig.VERSION_NAME + ", " + BuildConfig.BUILD_TYPE)
                    .icon(new IconicsDrawable(activity)
                            .icon(CommunityMaterial.Icon2.cmd_information_outline)
                            .color(IconicsColor.colorInt(primaryTextOnPrimaryBg))
                            .size(IconicsSize.dp(iconSizeDp)))
                    .build();
            final int[] clickCounter = {0};
            pref_about_version.setOnClickAction(() -> {
                if (6 - clickCounter[0] != 0) {
                    Toast.makeText(activity, ("\ud83d\ude02"), Toast.LENGTH_SHORT).show();
                }
                refreshMaterialAboutList();
                clickCounter[0] = clickCounter[0] + 1;
                if (clickCounter[0] > 6) {
                    final MediaPlayer mp = MediaPlayer.create(activity, R.raw.ogarnij_sie);
                    mp.start();
                    clickCounter[0] = 0;
                }
            });
            items.add(pref_about_version);

            items.add(new MaterialAboutActionItem.Builder()
                    .text(R.string.settings_about_privacy_policy_text)
                    .textColor(primaryTextOnPrimaryBg)
                    .subTextColor(secondaryTextOnPrimaryBg)
                    .icon(new IconicsDrawable(activity)
                            .icon(CommunityMaterial.Icon2.cmd_shield_outline)
                            .color(IconicsColor.colorInt(primaryTextOnPrimaryBg))
                            .size(IconicsSize.dp(iconSizeDp)))
                    .setOnClickAction(ConvenienceBuilder.createWebsiteOnClickAction(activity, Uri.parse("https://szkolny.eu/privacy-policy")))
                    .build());

            items.add(new MaterialAboutActionItem.Builder()
                    .text(R.string.settings_about_discord_text)
                    .textColor(primaryTextOnPrimaryBg)
                    .subTextColor(secondaryTextOnPrimaryBg)
                    .subText(R.string.settings_about_discord_subtext)
                    .icon(new IconicsDrawable(activity)
                            .icon(SzkolnyFont.Icon.szf_discord_outline)
                            .color(IconicsColor.colorInt(primaryTextOnPrimaryBg))
                            .size(IconicsSize.dp(iconSizeDp)))
                    .setOnClickAction(ConvenienceBuilder.createWebsiteOnClickAction(activity, Uri.parse("https://discord.gg/n9e8pWr")))
                    .build());

            items.add(new MaterialAboutActionItem.Builder()
                    .text(R.string.settings_about_language_text)
                    .textColor(primaryTextOnPrimaryBg)
                    .subTextColor(secondaryTextOnPrimaryBg)
                    .subText(R.string.settings_about_language_subtext)
                    .icon(new IconicsDrawable(activity)
                            .icon(CommunityMaterial.Icon2.cmd_translate)
                            .color(IconicsColor.colorInt(primaryTextOnPrimaryBg))
                            .size(IconicsSize.dp(iconSizeDp)))
                    .setOnClickAction(() -> {
                        new MaterialDialog.Builder(activity)
                                .title(getString(R.string.settings_about_language_dialog_title))
                                .content(getString(R.string.settings_about_language_dialog_text))
                                .items(getString(R.string.language_system), getString(R.string.language_polish), getString(R.string.language_english))
                                .itemsCallbackSingleChoice(app.config.getUi().getLanguage() == null ? 0 : app.config.getUi().getLanguage().equals("pl") ? 1 : 2, (dialog, itemView, which, text) -> {
                                    switch (which) {
                                        case 0:
                                            app.config.getUi().setLanguage(null);
                                            initDefaultLocale();
                                            break;
                                        case 1:
                                            app.config.getUi().setLanguage("pl");
                                            break;
                                        case 2:
                                            app.config.getUi().setLanguage("en");
                                            break;
                                    }
                                    app.saveConfig("language");
                                    activity.recreate(MainActivity.DRAWER_ITEM_SETTINGS);
                                    return true;
                                })
                                .show();
                    })
                    .build());

            items.add(getMoreItem(() -> addCardItems(CARD_ABOUT, getAboutCard(true))));
        }
        else {
            items.add(new MaterialAboutActionItem.Builder()
                    .text(R.string.settings_about_update_text)
                    .subText(R.string.settings_about_update_subtext)
                    .textColor(primaryTextOnPrimaryBg)
                    .subTextColor(secondaryTextOnPrimaryBg)
                    .icon(new IconicsDrawable(activity)
                            .icon(CommunityMaterial.Icon2.cmd_update)
                            .color(IconicsColor.colorInt(primaryTextOnPrimaryBg))
                            .size(IconicsSize.dp(iconSizeDp)))
                    .setOnClickAction(() -> {
                        //open browser or intent here
                        NetworkUtils net = new NetworkUtils(app);
                        if (!net.isOnline()) {
                            new MaterialDialog.Builder(activity)
                                    .title(R.string.you_are_offline_title)
                                    .content(R.string.you_are_offline_text)
                                    .positiveText(R.string.ok)
                                    .show();
                        } else {
                            BootReceiver br = new BootReceiver();
                            Intent i = new Intent();
                            i.putExtra("UserChecked", true);
                            br.onReceive(getContext(), i);
                        }
                    })
                    .build());

            items.add(new MaterialAboutActionItem.Builder()
                    .text(R.string.settings_about_changelog_text)
                    .textColor(primaryTextOnPrimaryBg)
                    .subTextColor(secondaryTextOnPrimaryBg)
                    .icon(new IconicsDrawable(activity)
                            .icon(CommunityMaterial.Icon2.cmd_radar)
                            .color(IconicsColor.colorInt(primaryTextOnPrimaryBg))
                            .size(IconicsSize.dp(iconSizeDp)))
                    .setOnClickAction(() -> new ChangelogDialog(activity, null, null))
                    .build());

            items.add(new MaterialAboutActionItem.Builder()
                    .text(R.string.settings_about_licenses_text)
                    .textColor(primaryTextOnPrimaryBg)
                    .subTextColor(secondaryTextOnPrimaryBg)
                    .icon(new IconicsDrawable(activity)
                            .icon(CommunityMaterial.Icon.cmd_code_braces)
                            .color(IconicsColor.colorInt(primaryTextOnPrimaryBg))
                            .size(IconicsSize.dp(iconSizeDp)))
                    .setOnClickAction(() -> {
                        Intent intent = new Intent(activity, SettingsLicenseActivity.class);
                        startActivity(intent);
                    })
                    .build());

        /*items.add(new MaterialAboutActionItem.Builder()
                .text(R.string.settings_about_intro_text)
                .icon(new IconicsDrawable(activity)
                        .icon(CommunityMaterial.Icon2.cmd_projector_screen)
                        .color(IconicsColor.colorInt(iconColor))
                        .size(IconicsSize.dp(iconSizeDp)))
                .setOnClickAction(() -> {
                    if (tryingToDevMode[0]) {
                        if (getParentFragment() instanceof SettingsGeneralFragment) {
                            ((SettingsGeneralFragment) getParentFragment()).showDevSettings();
                        }
                        tryingToDevMode[0] = false;
                        return;
                    }
                    IntroConnectionFragment.connectionOk = true;
                    IntroConnectionFragment.httpsOk = (app.requestScheme.equals("https"));
                    Intent intent = new Intent(activity, MainIntroActivity.class);
                    IntroSplashFragment.skipAnimation = false;
                    startActivity(intent);
                })
                .build());*/

            if (App.devMode) {
                items.add(new MaterialAboutActionItem.Builder()
                        .text(R.string.settings_about_crash_text)
                        .subText(R.string.settings_about_crash_subtext)
                        .textColor(primaryTextOnPrimaryBg)
                        .subTextColor(secondaryTextOnPrimaryBg)
                        .icon(new IconicsDrawable(activity)
                                .icon(CommunityMaterial.Icon.cmd_bug_outline)
                                .color(IconicsColor.colorInt(primaryTextOnPrimaryBg))
                                .size(IconicsSize.dp(iconSizeDp)))
                        .setOnClickAction(() -> {
                            throw new RuntimeException("MANUAL CRASH");
                        })
                        .build());
            }
        }
        return items;
    }

    @Override
    protected MaterialAboutList getMaterialAboutList(Context activityContext) {
        if (getActivity() == null || getContext() == null || !isAdded())
            return null;
        this.activity = (MainActivity) activityContext;
        this.app = (App) activity.getApplication();
        iconColor = Themes.INSTANCE.getPrimaryTextColor(activityContext);
        if (app.profile == null)
            return new MaterialAboutList.Builder().build();

        MaterialAboutList materialAboutList = new MaterialAboutList();
        materialAboutList.addCard(getCardWithItems(null, getProfileCard(false)));
        materialAboutList.addCard(getCardWithItems(getString(R.string.settings_theme_title_text), getThemeCard(false)));
        materialAboutList.addCard(getCardWithItems(getString(R.string.settings_sync_title_text), getSyncCard(false)));
        materialAboutList.addCard(getCardWithItems(getString(R.string.settings_about_register_title_text), getRegisterCard(false)));
        //if (configurableEndpoints != null)
        //    materialAboutList.addCard(getCardWithItems(getString(R.string.settings_sync_customize_title_text), getSyncCustomizeCard(false)));
        materialAboutList.addCard(getCardWithItems(null, getAboutCard(false), true));

        return materialAboutList;
    }

    @Override
    protected int getTheme() {
        return Themes.INSTANCE.getAppTheme();
    }

    /*     _____          _                    ____             _                                   _
          / ____|        | |                  |  _ \           | |                                 | |
         | |    _   _ ___| |_ ___  _ __ ___   | |_) | __ _  ___| | ____ _ _ __ ___  _   _ _ __   __| |___
         | |   | | | / __| __/ _ \| '_ ` _ \  |  _ < / _` |/ __| |/ / _` | '__/ _ \| | | | '_ \ / _` / __|
         | |___| |_| \__ \ || (_) | | | | | | | |_) | (_| | (__|   < (_| | | | (_) | |_| | | | | (_| \__ \
          \_____\__,_|___/\__\___/|_| |_| |_| |____/ \__,_|\___|_|\_\__, |_|  \___/ \__,_|_| |_|\__,_|___/
                                                                     __/ |
                                                                    |__*/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            Toast.makeText(app, "Wystąpił błąd. Spróbuj ponownie", Toast.LENGTH_SHORT).show();
            return;
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            Uri uri = result.getUri();

            File photoFile = new File(uri.getPath());
            File destFile = new File(getContext().getFilesDir(),"profile"+ app.profile.getId() +".jpg");
            if (destFile.exists()) {
                destFile.delete();
                destFile = new File(getContext().getFilesDir(),"profile"+ app.profile.getId() +".jpg");
            }

            d(TAG, "Source file: "+photoFile.getAbsolutePath());
            d(TAG, "Dest file: "+destFile.getAbsolutePath());

            if (result.getCropRect().width() > 512 || true) {
                Bitmap bigImage = BitmapFactory.decodeFile(uri.getPath());
                Bitmap smallImage = getResizedBitmap(bigImage, 512, 512);
                try (FileOutputStream out = new FileOutputStream(destFile)) {
                    smallImage.compress(Bitmap.CompressFormat.JPEG, 80, out); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                    app.profile.setImage(destFile.getAbsolutePath());
                    app.profileSaveAsync();
                    profileCardTitleItem.setIcon(getProfileDrawable());
                    refreshMaterialAboutList();
                    if (photoFile.exists()) {
                        photoFile.delete();
                    }
                    //((MainActivity)getActivity()).recreateWithTransition(); // TODO somehow update miniDrawer profile picture
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                if (photoFile.renameTo(destFile)) {
                    // success
                    app.profile.setImage(destFile.getAbsolutePath());
                    app.profileSaveAsync();
                    profileCardTitleItem.setIcon(getProfileDrawable());
                    refreshMaterialAboutList();
                    //((MainActivity)getActivity()).recreateWithTransition(); // TODO somehow update miniDrawer profile picture
                }
                else {
                    // not this time
                    Toast.makeText(app, R.string.error_occured, Toast.LENGTH_LONG).show();
                }
            }
        }
        else if (requestCode == 21 && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                String path = getRealPathFromURI(activity, uri);
                if (path.toLowerCase().endsWith(".gif")) {
                    app.profile.setImage(path);
                    app.profileSaveAsync();
                    profileCardTitleItem.setIcon(getProfileDrawable());
                    refreshMaterialAboutList();
                }
                else {
                    CropImage.activity(data.getData())
                            .setAspectRatio(1, 1)
                            //.setMaxCropResultSize(512, 512)
                            .setCropShape(CropImageView.CropShape.OVAL)
                            .setGuidelines(CropImageView.Guidelines.ON)
                            .start(activity, this);
                }
            }

        }
        else if (requestCode == 22 && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                app.config.getUi().setHeaderBackground(getRealPathFromURI(getContext(), uri));
                if (activity != null) {
                    activity.getDrawer().setAccountHeaderBackground(app.config.getUi().getHeaderBackground());
                    activity.getDrawer().open();
                }
            }
        }
        else if (requestCode == 23 && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                app.config.getUi().setAppBackground(getRealPathFromURI(getContext(), uri));
                if (activity != null) {
                    activity.recreate();
                }
            }
        }

    }
}
