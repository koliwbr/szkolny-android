package pl.szczodrzynski.edziennik

import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.*
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.szkolny.font.SzkolnyFont
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import pl.szczodrzynski.edziennik.datamodels.Metadata.*
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.navlib.NavView
import pl.szczodrzynski.navlib.SystemBarsUtil
import pl.szczodrzynski.navlib.SystemBarsUtil.Companion.COLOR_HALF_TRANSPARENT
import pl.szczodrzynski.navlib.bottomsheet.NavBottomSheet
import pl.szczodrzynski.navlib.drawer.NavDrawer
import pl.szczodrzynski.navlib.drawer.items.DrawerPrimaryItem
import pl.szczodrzynski.navlib.drawer.items.withAppTitle
import pl.szczodrzynski.navlib.getColorFromAttr
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.NavOptions
import com.danimahardhika.cafebar.CafeBar
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import pl.droidsonroids.gif.GifDrawable
import pl.szczodrzynski.edziennik.App.APP_URL
import pl.szczodrzynski.edziennik.api.AppError
import pl.szczodrzynski.edziennik.api.interfaces.EdziennikInterface.*
import pl.szczodrzynski.edziennik.api.interfaces.SyncCallback
import pl.szczodrzynski.edziennik.databinding.ActivitySzkolnyBinding
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile
import pl.szczodrzynski.edziennik.datamodels.ProfileFull
import pl.szczodrzynski.edziennik.dialogs.ChangelogDialog
import pl.szczodrzynski.edziennik.fragments.*
import pl.szczodrzynski.edziennik.login.LoginActivity
import pl.szczodrzynski.edziennik.messages.MessagesDetailsFragment
import pl.szczodrzynski.edziennik.messages.MessagesFragment
import pl.szczodrzynski.edziennik.models.NavTarget
import pl.szczodrzynski.edziennik.network.ServerRequest
import pl.szczodrzynski.edziennik.sync.SyncJob
import pl.szczodrzynski.edziennik.utils.SwipeRefreshLayoutNoTouch
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem
import java.io.File
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {

        var useOldMessages = false

        const val TAG = "MainActivity"

        const val REQUEST_LOGIN_ACTIVITY = 20222

        const val DRAWER_PROFILE_ADD_NEW = 200
        const val DRAWER_PROFILE_SYNC_ALL = 201
        const val DRAWER_PROFILE_EXPORT_DATA = 202
        const val DRAWER_PROFILE_MANAGE = 203
        const val DRAWER_ITEM_HOME = 1
        const val DRAWER_ITEM_TIMETABLE = 11
        const val DRAWER_ITEM_AGENDA = 12
        const val DRAWER_ITEM_GRADES = 13
        const val DRAWER_ITEM_MESSAGES = 17
        const val DRAWER_ITEM_HOMEWORKS = 14
        const val DRAWER_ITEM_NOTICES = 15
        const val DRAWER_ITEM_ATTENDANCES = 16
        const val DRAWER_ITEM_ANNOUNCEMENTS = 18
        const val DRAWER_ITEM_NOTIFICATIONS = 20
        const val DRAWER_ITEM_SETTINGS = 101
        const val DRAWER_ITEM_DEBUG = 102

        const val TARGET_GRADES_EDITOR = 501
        const val TARGET_HELP = 502
        const val TARGET_FEEDBACK = 120
        const val TARGET_MESSAGES_DETAILS = 503

        const val HOME_ID = DRAWER_ITEM_HOME

        val navTargetList: List<NavTarget> by lazy {
            val list: MutableList<NavTarget> = mutableListOf()

            // home item
            list += NavTarget(DRAWER_ITEM_HOME, R.string.menu_home_page, HomeFragment::class)
                    .withTitle(R.string.app_name)
                    .withIcon(CommunityMaterial.Icon2.cmd_home)
                    .isInDrawer(true)
                    .isStatic(true)
                    .withPopToHome(false)

            list += NavTarget(DRAWER_ITEM_TIMETABLE, R.string.menu_timetable, RegisterTimetableFragment::class)
                    .withIcon(CommunityMaterial.Icon2.cmd_timetable)
                    .withBadgeTypeId(TYPE_LESSON_CHANGE)
                    .isInDrawer(true)

            list += NavTarget(DRAWER_ITEM_AGENDA, R.string.menu_agenda, RegisterAgendaDefaultFragment::class)
                    .withIcon(CommunityMaterial.Icon.cmd_calendar)
                    .withBadgeTypeId(TYPE_EVENT)
                    .isInDrawer(true)

            list += NavTarget(DRAWER_ITEM_GRADES, R.string.menu_grades, RegisterGradesFragment::class)
                    .withIcon(CommunityMaterial.Icon2.cmd_numeric_5_box)
                    .withBadgeTypeId(TYPE_GRADE)
                    .isInDrawer(true)

            list += NavTarget(DRAWER_ITEM_MESSAGES, R.string.menu_messages, MessagesFragment::class)
                    .withIcon(CommunityMaterial.Icon.cmd_email)
                    .withBadgeTypeId(TYPE_MESSAGE)
                    .isInDrawer(true)

            list += NavTarget(DRAWER_ITEM_HOMEWORKS, R.string.menu_homework, RegisterHomeworksFragment::class)
                    .withIcon(SzkolnyFont.Icon.szf_file_document_edit)
                    .withBadgeTypeId(TYPE_HOMEWORK)
                    .isInDrawer(true)

            list += NavTarget(DRAWER_ITEM_NOTICES, R.string.menu_notices, RegisterNoticesFragment::class)
                    .withIcon(CommunityMaterial.Icon2.cmd_message_alert)
                    .withBadgeTypeId(TYPE_NOTICE)
                    .isInDrawer(true)

            list += NavTarget(DRAWER_ITEM_ATTENDANCES, R.string.menu_attendances, RegisterAttendancesFragment::class)
                    .withIcon(CommunityMaterial.Icon.cmd_calendar_remove)
                    .withBadgeTypeId(TYPE_ATTENDANCE)
                    .isInDrawer(true)

            list += NavTarget(DRAWER_ITEM_ANNOUNCEMENTS, R.string.menu_announcements, RegisterAnnouncementsFragment::class)
                    .withIcon(CommunityMaterial.Icon.cmd_bulletin_board)
                    .withBadgeTypeId(TYPE_ANNOUNCEMENT)
                    .isInDrawer(true)


            // static drawer items
            list += NavTarget(DRAWER_ITEM_NOTIFICATIONS, R.string.menu_notifications, RegisterNotificationsFragment::class)
                    .withIcon(CommunityMaterial.Icon.cmd_bell_ring)
                    .isInDrawer(true)
                    .isStatic(true)
                    .isBelowSeparator(true)

            list += NavTarget(DRAWER_ITEM_SETTINGS, R.string.menu_settings, SettingsNewFragment::class)
                    .withIcon(CommunityMaterial.Icon2.cmd_settings)
                    .isInDrawer(true)
                    .isStatic(true)
                    .isBelowSeparator(true)


            // profile settings items
            list += NavTarget(DRAWER_PROFILE_ADD_NEW, R.string.menu_add_new_profile, null)
                    .withIcon(CommunityMaterial.Icon2.cmd_plus)
                    .withDescription(R.string.drawer_add_new_profile_desc)
                    .isInProfileList(true)

            list += NavTarget(DRAWER_PROFILE_MANAGE, R.string.menu_manage_profiles, ProfileManagerFragment::class)
                    .withTitle(R.string.title_profile_manager)
                    .withIcon(CommunityMaterial.Icon.cmd_account_group)
                    .withDescription(R.string.drawer_manage_profiles_desc)
                    .isInProfileList(false)

            list += NavTarget(DRAWER_PROFILE_SYNC_ALL, R.string.menu_sync_all, null)
                    .withIcon(CommunityMaterial.Icon2.cmd_sync)
                    .isInProfileList(true)


            // other target items, not directly navigated
            list += NavTarget(TARGET_GRADES_EDITOR, R.string.menu_grades_editor, GradesEditorFragment::class)
            list += NavTarget(TARGET_HELP, R.string.menu_help, HelpFragment::class)
            list += NavTarget(TARGET_FEEDBACK, R.string.menu_feedback, FeedbackFragment::class)
            list += NavTarget(TARGET_MESSAGES_DETAILS, R.string.menu_message, MessagesDetailsFragment::class)
            list += NavTarget(DRAWER_ITEM_DEBUG, R.string.menu_debug, DebugFragment::class)

            list
        }
    }

    val b: ActivitySzkolnyBinding by lazy { ActivitySzkolnyBinding.inflate(layoutInflater) }
    val navView: NavView by lazy { b.navView }
    val drawer: NavDrawer by lazy { navView.drawer }
    val bottomSheet: NavBottomSheet by lazy { navView.bottomSheet }

    val swipeRefreshLayout: SwipeRefreshLayoutNoTouch by lazy { b.swipeRefreshLayout }

    val app: App by lazy {
        applicationContext as App
    }

    private val fragmentManager by lazy { supportFragmentManager }
    private lateinit var navTarget: NavTarget
    private val navTargetId
        get() = navTarget.id

    private val navBackStack = mutableListOf<NavTarget>()
    private var navLoading = true

    /*     ____           _____                _
          / __ \         / ____|              | |
         | |  | |_ __   | |     _ __ ___  __ _| |_ ___
         | |  | | '_ \  | |    | '__/ _ \/ _` | __/ _ \
         | |__| | | | | | |____| | |  __/ (_| | ||  __/
          \____/|_| |_|  \_____|_|  \___|\__,_|\__\__*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(Themes.appTheme)

        setContentView(b.root)

        navLoading = true

        b.navView.apply {
            drawer.init(this@MainActivity)

            SystemBarsUtil(this@MainActivity).run {
                paddingByKeyboard = b.navView
                appFullscreen = true
                statusBarColor = getColorFromAttr(context, android.R.attr.colorBackground)
                statusBarDarker = false
                statusBarFallbackLight = COLOR_HALF_TRANSPARENT
                statusBarFallbackGradient = COLOR_HALF_TRANSPARENT
                navigationBarTransparent = false

                b.navView.configSystemBarsUtil(this)

                commit()
            }

            toolbar.apply {
                subtitleFormat = R.string.toolbar_subtitle
                subtitleFormatWithUnread = R.plurals.toolbar_subtitle_with_unread
            }

            bottomBar.apply {
                fabEnable = false
                fabExtendable = true
                fabExtended = false
                fabGravity = Gravity.CENTER
            }

            bottomSheet.apply {
                removeAllItems()
                toggleGroupEnabled = false
                textInputEnabled = false
            }

            drawer.apply {
                setAccountHeaderBackground(app.appConfig.headerBackground)

                drawerProfileListEmptyListener = {
                    app.appConfig.loginFinished = false
                    app.saveConfig("loginFinished")
                    profileListEmptyListener()
                }
                drawerItemSelectedListener = { id, position, drawerItem ->
                    loadTarget(id)
                    true
                }
                drawerProfileSelectedListener = { id, profile, _, _ ->
                    loadProfile(id)
                    false
                }
                drawerProfileLongClickListener = { _, profile, _, view ->
                    if (profile is ProfileDrawerItem) {
                        showProfileContextMenu(profile, view)
                        true
                    }
                    else {
                        false
                    }
                }
                drawerProfileImageLongClickListener = drawerProfileLongClickListener
                drawerProfileSettingClickListener = this@MainActivity.profileSettingClickListener

                miniDrawerVisibleLandscape = null
                miniDrawerVisiblePortrait = app.appConfig.miniDrawerVisible
            }
        }

        navTarget = navTargetList[0]

        var profileListEmpty = drawer.profileListEmpty

        if (savedInstanceState != null) {
            intent?.putExtras(savedInstanceState)
            savedInstanceState.clear()
        }

        if (!profileListEmpty) {
            handleIntent(intent?.extras)
        }
        app.db.profileDao().getAllFull().observe(this, Observer { profiles ->
            // TODO fix weird -1 profiles ???
            profiles.removeAll { it.id < 0 }
            drawer.setProfileList(profiles)
            if (profileListEmpty) {
                profileListEmpty = false
                handleIntent(intent?.extras)
            }
            else if (app.profile != null) {
                drawer.currentProfile = app.profile.id
            }
        })

        // if null, getAllFull will load a profile and update drawerItems
        if (app.profile != null)
            setDrawerItems()

        app.db.metadataDao().getUnreadCounts().observe(this, Observer { unreadCounters ->
            unreadCounters.map {
                it.type = it.thingType
            }
            drawer.setUnreadCounterList(unreadCounters)
        })

        b.swipeRefreshLayout.isEnabled = true
        b.swipeRefreshLayout.setOnRefreshListener { this.syncCurrentFeature() }

        isStoragePermissionGranted()

        SyncJob.schedule(app)

        // APP BACKGROUND
        if (app.appConfig.appBackground != null) {
            try {
                var bg = app.appConfig.appBackground
                val bgDir = File(Environment.getExternalStoragePublicDirectory("Szkolny.eu"), "bg")
                if (bgDir.exists()) {
                    val files = bgDir.listFiles()
                    val r = Random()
                    val i = r.nextInt(files.size)
                    bg = files[i].toString()
                }
                val linearLayout = b.root
                if (bg.endsWith(".gif")) {
                    linearLayout.background = GifDrawable(bg)
                } else {
                    linearLayout.background = BitmapDrawable.createFromPath(bg)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // WHAT'S NEW DIALOG
        if (app.appConfig.lastAppVersion != BuildConfig.VERSION_CODE) {
            ServerRequest(app, app.requestScheme + APP_URL + "main.php?just_updated", "MainActivity/JU")
                    .run { e, result ->
                        Handler(Looper.getMainLooper()).post {
                            try {
                                ChangelogDialog().show(supportFragmentManager, "whats_new")
                            } catch (e2: Exception) {
                                e2.printStackTrace()
                            }
                        }
                    }
            if (app.appConfig.lastAppVersion < 170) {
                //Intent intent = new Intent(this, ChangelogIntroActivity.class);
                //startActivity(intent);
            } else {
                app.appConfig.lastAppVersion = BuildConfig.VERSION_CODE
                app.saveConfig("lastAppVersion")
            }
        }

        // RATE SNACKBAR
        if (app.appConfig.appRateSnackbarTime != 0L && app.appConfig.appRateSnackbarTime <= System.currentTimeMillis()) {
            navView.coordinator.postDelayed({
                CafeBar.builder(this)
                        .content(R.string.rate_snackbar_text)
                        .icon(IconicsDrawable(this).icon(CommunityMaterial.Icon2.cmd_star).size(IconicsSize.dp(20)).color(IconicsColor.colorInt(Themes.getPrimaryTextColor(this))))
                        .positiveText(R.string.rate_snackbar_positive)
                        .positiveColor(-0xb350b0)
                        .negativeText(R.string.rate_snackbar_negative)
                        .negativeColor(0xff666666.toInt())
                        .neutralText(R.string.rate_snackbar_neutral)
                        .neutralColor(0xff666666.toInt())
                        .onPositive { cafeBar ->
                            Utils.openGooglePlay(this)
                            cafeBar.dismiss()
                            app.appConfig.appRateSnackbarTime = 0
                            app.saveConfig("appRateSnackbarTime")
                        }
                        .onNegative { cafeBar ->
                            Toast.makeText(this, "Szkoda, opinie innych pomagają mi rozwijać aplikację.", Toast.LENGTH_LONG).show()
                            cafeBar.dismiss()
                            app.appConfig.appRateSnackbarTime = 0
                            app.saveConfig("appRateSnackbarTime")
                        }
                        .onNeutral { cafeBar ->
                            Toast.makeText(this, "OK", Toast.LENGTH_LONG).show()
                            cafeBar.dismiss()
                            app.appConfig.appRateSnackbarTime = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000
                            app.saveConfig("appRateSnackbarTime")
                        }
                        .autoDismiss(false)
                        .swipeToDismiss(true)
                        .floating(true)
                        .show()
            }, 10000)
        }

        // CONTEXT MENU ITEMS
        bottomSheet.removeAllItems()
        bottomSheet.appendItems(
                BottomSheetPrimaryItem(false)
                        .withTitle(R.string.menu_sync)
                        .withIcon(CommunityMaterial.Icon2.cmd_sync)
                        .withOnClickListener(View.OnClickListener {
                            bottomSheet.close()
                            app.apiEdziennik.guiSyncFeature(app, this, App.profileId, R.string.sync_dialog_title, R.string.sync_dialog_text, R.string.sync_done, fragmentToFeature(navTargetId))
                        }),
                BottomSheetSeparatorItem(false),
                BottomSheetPrimaryItem(false)
                        .withTitle(R.string.menu_settings)
                        .withIcon(CommunityMaterial.Icon2.cmd_settings)
                        .withOnClickListener(View.OnClickListener { loadTarget(DRAWER_ITEM_SETTINGS) }),
                BottomSheetPrimaryItem(false)
                        .withTitle(R.string.menu_feedback)
                        .withIcon(CommunityMaterial.Icon2.cmd_help_circle)
                        .withOnClickListener(View.OnClickListener { loadTarget(TARGET_FEEDBACK) })
        )
        if (App.devMode) {
            bottomSheet += BottomSheetPrimaryItem(false)
                    .withTitle(R.string.menu_debug)
                    .withIcon(CommunityMaterial.Icon.cmd_android_debug_bridge)
                    .withOnClickListener(View.OnClickListener { loadTarget(DRAWER_ITEM_DEBUG) })
        }
    }

    var profileListEmptyListener = {
        startActivityForResult(Intent(this, LoginActivity::class.java), REQUEST_LOGIN_ACTIVITY)
    }
    private var profileSettingClickListener = { id: Int, view: View? ->
        when (id) {
            DRAWER_PROFILE_ADD_NEW -> {
                LoginActivity.privacyPolicyAccepted = true
                // else it would try to navigateUp onBackPressed, which it can't do. There is no parent fragment
                LoginActivity.firstCompleted = false
                profileListEmptyListener()
            }
            DRAWER_PROFILE_SYNC_ALL -> {
                SyncJob.run(app)
            }
            else -> {
                loadTarget(id)
            }
        }
        false
    }

    /*     _____
          / ____|
         | (___  _   _ _ __   ___
          \___ \| | | | '_ \ / __|
          ____) | |_| | | | | (__
         |_____/ \__, |_| |_|\___|
                  __/ |
                 |__*/
    fun syncCurrentFeature() {
        swipeRefreshLayout.isRefreshing = true
        Toast.makeText(this, fragmentToSyncName(navTargetId), Toast.LENGTH_SHORT).show()
        val callback = object : SyncCallback {
            override fun onLoginFirst(profileList: List<Profile>, loginStore: LoginStore) {

            }

            override fun onSuccess(activityContext: Context, profileFull: ProfileFull) {
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onError(activityContext: Context, error: AppError) {
                swipeRefreshLayout.isRefreshing = false
                app.apiEdziennik.guiShowErrorSnackbar(this@MainActivity, error)
            }

            override fun onProgress(progressStep: Int) {

            }

            override fun onActionStarted(stringResId: Int) {

            }
        }
        val feature = fragmentToFeature(navTargetId)
        if (feature == FEATURE_ALL) {
            swipeRefreshLayout.isRefreshing = false
            app.apiEdziennik.guiSync(app, this, App.profileId, R.string.sync_dialog_title, R.string.sync_dialog_text, R.string.sync_done)
        } else {
            app.apiEdziennik.guiSyncSilent(app, this, App.profileId, callback, feature)
        }
    }
    private fun fragmentToFeature(currentFragment: Int): Int {
        return when (currentFragment) {
            DRAWER_ITEM_TIMETABLE -> FEATURE_TIMETABLE
            DRAWER_ITEM_AGENDA -> FEATURE_AGENDA
            DRAWER_ITEM_GRADES -> FEATURE_GRADES
            DRAWER_ITEM_HOMEWORKS -> FEATURE_HOMEWORKS
            DRAWER_ITEM_NOTICES -> FEATURE_NOTICES
            DRAWER_ITEM_ATTENDANCES -> FEATURE_ATTENDANCES
            DRAWER_ITEM_MESSAGES -> when (MessagesFragment.pageSelection) {
                1 -> FEATURE_MESSAGES_OUTBOX
                else -> FEATURE_MESSAGES_INBOX
            }
            DRAWER_ITEM_ANNOUNCEMENTS -> FEATURE_ANNOUNCEMENTS
            else -> FEATURE_ALL
        }
    }
    private fun fragmentToSyncName(currentFragment: Int): Int {
        return when (currentFragment) {
            DRAWER_ITEM_TIMETABLE -> R.string.sync_feature_timetable
            DRAWER_ITEM_AGENDA -> R.string.sync_feature_agenda
            DRAWER_ITEM_GRADES -> R.string.sync_feature_grades
            DRAWER_ITEM_HOMEWORKS -> R.string.sync_feature_homeworks
            DRAWER_ITEM_NOTICES -> R.string.sync_feature_notices
            DRAWER_ITEM_ATTENDANCES -> R.string.sync_feature_attendances
            DRAWER_ITEM_MESSAGES -> when (MessagesFragment.pageSelection) {
                1 -> R.string.sync_feature_messages_outbox
                else -> R.string.sync_feature_messages_inbox
            }
            DRAWER_ITEM_ANNOUNCEMENTS -> R.string.sync_feature_announcements
            else -> R.string.sync_feature_syncing_all
        }
    }

    /*    _____       _             _
         |_   _|     | |           | |
           | |  _ __ | |_ ___ _ __ | |_ ___
           | | | '_ \| __/ _ \ '_ \| __/ __|
          _| |_| | | | ||  __/ | | | |_\__ \
         |_____|_| |_|\__\___|_| |_|\__|__*/
    private val intentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleIntent(intent?.extras)
        }
    }
    private fun handleIntent(extras: Bundle?) {

        Log.d(TAG, "handleIntent() {")
        extras?.keySet()?.forEach { key ->
            Log.d(TAG, "    \"$key\": "+extras.get(key))
        }
        Log.d(TAG, "}")

        if (extras?.containsKey("reloadProfileId") == true) {
            val reloadProfileId = extras.getInt("reloadProfileId", -1)
            extras.remove("reloadProfileId")
            if (reloadProfileId == -1 || (app.profile != null && app.profile.id == reloadProfileId)) {
                reloadTarget()
                return
            }
        }

        var intentProfileId = -1
        var intentTargetId = -1

        if (extras?.containsKey("profileId") == true) {
            intentProfileId = extras.getInt("profileId", -1)
            extras.remove("profileId")
        }

        if (extras?.containsKey("fragmentId") == true) {
            intentTargetId = extras.getInt("fragmentId", -1)
            extras.remove("fragmentId")
        }

        /*if (intentTargetId == -1 && navController.currentDestination?.id == R.id.loadingFragment) {
            intentTargetId = navTarget.id
        }*/

        if (navLoading) {
            navLoading = false
            b.fragment.removeAllViews()
            if (intentTargetId == -1)
                intentTargetId = HOME_ID
        }

        when {
            app.profile == null -> {
                if (intentProfileId == -1)
                    intentProfileId = app.appSharedPrefs.getInt("current_profile_id", 1)
                loadProfile(intentProfileId, intentTargetId)
            }
            intentProfileId != -1 -> {
                loadProfile(intentProfileId, intentTargetId)
            }
            intentTargetId != -1 -> {
                drawer.currentProfile = app.profile.id
                loadTarget(intentTargetId, extras)
            }
            else -> {
                drawer.currentProfile = app.profile.id
            }
        }
    }

    override fun recreate() {
        recreate(navTargetId)
    }
    fun recreate(targetId: Int) {
        recreate(targetId, null)
    }
    fun recreate(targetId: Int? = null, arguments: Bundle? = null) {
        val intent = Intent(this, MainActivity::class.java)
        if (arguments != null)
            intent.putExtras(arguments)
        if (targetId != null) {
            intent.putExtra("fragmentId", targetId)
        }
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        startActivity(intent)
    }

    override fun onResume() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_MAIN)
        registerReceiver(intentReceiver, filter)
        super.onResume()
    }
    override fun onPause() {
        unregisterReceiver(intentReceiver)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("fragmentId", navTargetId)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent?.extras)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_LOGIN_ACTIVITY) {
            if (resultCode == Activity.RESULT_CANCELED && false) {
                finish()
            }
            else {
                if (!app.appConfig.loginFinished)
                    finish()
                else {
                    handleIntent(data?.extras)
                }
            }
        }
    }

    /*    _                     _                  _   _               _
         | |                   | |                | | | |             | |
         | |     ___   __ _  __| |  _ __ ___   ___| |_| |__   ___   __| |___
         | |    / _ \ / _` |/ _` | | '_ ` _ \ / _ \ __| '_ \ / _ \ / _` / __|
         | |___| (_) | (_| | (_| | | | | | | |  __/ |_| | | | (_) | (_| \__ \
         |______\___/ \__,_|\__,_| |_| |_| |_|\___|\__|_| |_|\___/ \__,_|__*/
    val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.task_open_enter) // new fragment enter
            .setExitAnim(R.anim.task_open_exit) // old fragment exit
            .setPopEnterAnim(R.anim.task_close_enter) // old fragment enter back
            .setPopExitAnim(R.anim.task_close_exit) // new fragment exit
            .build()

    fun loadProfile(id: Int) = loadProfile(id, navTargetId)
    fun loadProfile(id: Int, arguments: Bundle?) = loadProfile(id, navTargetId, arguments)
    fun loadProfile(id: Int, drawerSelection: Int, arguments: Bundle? = null) {
        Log.d("NavDebug", "loadProfile(id = $id, drawerSelection = $drawerSelection)")
        if (app.profile != null && App.profileId == id) {
            drawer.currentProfile = app.profile.id
            loadTarget(drawerSelection, arguments)
            return
        }
        AsyncTask.execute {
            app.profileLoadById(id)

            this.runOnUiThread {
                if (app.profile == null) {
                    LoginActivity.firstCompleted = false
                    if (app.appConfig.loginFinished) {
                        // this shouldn't run
                        profileListEmptyListener()
                    }
                } else {
                    setDrawerItems()
                    drawer.currentProfile = app.profile.id
                    loadTarget(drawerSelection, arguments)
                }
            }
        }
    }
    fun loadTarget(id: Int, arguments: Bundle? = null) {
        var loadId = id
        if (loadId == -1) {
            loadId = DRAWER_ITEM_HOME
        }
        val target = navTargetList
                .singleOrNull { it.id == loadId }
        if (target == null) {
            Toast.makeText(this, getString(R.string.error_invalid_fragment, id), Toast.LENGTH_LONG).show()
            loadTarget(navTargetList.first(), arguments)
        }
        else {
            loadTarget(target, arguments)
        }
    }
    private fun loadTarget(target: NavTarget, arguments: Bundle? = null) {
        Log.d("NavDebug", "loadItem(id = ${target.id})")

        bottomSheet.close()
        bottomSheet.removeAllContextual()
        bottomSheet.toggleGroupEnabled = false
        bottomSheet.onCloseListener = null
        drawer.close()
        drawer.setSelection(target.id, fireOnClick = false)
        navView.toolbar.setTitle(target.title ?: target.name)

        Log.d("NavDebug", "Navigating from ${navTarget.fragmentClass?.java?.simpleName} to ${target.fragmentClass?.java?.simpleName}")

        val fragment = target.fragmentClass?.java?.newInstance() ?: return
        fragment.arguments = arguments
        val transaction = fragmentManager.beginTransaction()

        if (navTarget == target) {
            // just reload the current target
            transaction.setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out
            )
        }
        else {
            navBackStack.lastIndexOf(target).let {
                if (it == -1)
                    return@let target
                // pop the back stack up until that target
                transaction.setCustomAnimations(
                        R.anim.task_close_enter,
                        R.anim.task_close_exit
                )

                // navigating grades_add -> grades
                // navTarget == grades_add
                // navBackStack = [home, grades, grades_editor]
                // it == 1
                //
                // navTarget = target
                // remove 1
                // remove 2
                val popCount = navBackStack.size - it
                for (i in 0 until popCount) {
                    navBackStack.removeAt(navBackStack.lastIndex)
                }
                navTarget = target

                return@let null
            }?.let {
                // target is neither current nor in the back stack
                // so navigate to it
                transaction.setCustomAnimations(
                        R.anim.task_open_enter,
                        R.anim.task_open_exit
                )
                navBackStack.add(navTarget)
                navTarget = target
            }
        }

        if (navTarget.popToHome) {
            // if the current has popToHome, let only home be in the back stack
            // probably `if (navTarget.popToHome)` in popBackStack() is not needed now
            val popCount = navBackStack.size - 1
            for (i in 0 until popCount) {
                navBackStack.removeAt(navBackStack.lastIndex)
            }
        }

        Log.d("NavDebug", "Current fragment ${navTarget.fragmentClass?.java?.simpleName}, pop to home ${navTarget.popToHome}, back stack:")
        navBackStack.forEachIndexed { index, target2 ->
            Log.d("NavDebug", " - $index: ${target2.fragmentClass?.java?.simpleName}")
        }

        transaction.replace(R.id.fragment, fragment)
        transaction.commitAllowingStateLoss()

        // TASK DESCRIPTION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val bm = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            val taskDesc = ActivityManager.TaskDescription(
                    if (target.id == HOME_ID) getString(R.string.app_name) else getString(R.string.app_task_format, getString(target.name)),
                    bm,
                    getColorFromAttr(this, R.attr.colorSurface)
            )
            setTaskDescription(taskDesc)
        }

    }
    fun reloadTarget() = loadTarget(navTarget)

    private fun popBackStack(): Boolean {
        if (navBackStack.size == 0) {
            return false
        }
        // TODO back stack argument support
        if (navTarget.popToHome) {
            loadTarget(HOME_ID)
        }
        else {
            loadTarget(navBackStack.last())
        }
        return true
    }
    fun navigateUp() {
        if (!popBackStack()) {
            super.onBackPressed()
        }
    }

    /**
     * Use the NavLib's menu button ripple to gain user attention
     * that something has changed in the bottom sheet.
     */
    fun gainAttention() {
        b.navView.postDelayed({
            navView.gainAttentionOnBottomBar()
        }, 1000)
    }

    /*    _____                                _ _
         |  __ \                              (_) |
         | |  | |_ __ __ ___      _____ _ __   _| |_ ___ _ __ ___  ___
         | |  | | '__/ _` \ \ /\ / / _ \ '__| | | __/ _ \ '_ ` _ \/ __|
         | |__| | | | (_| |\ V  V /  __/ |    | | ||  __/ | | | | \__ \
         |_____/|_|  \__,_| \_/\_/ \___|_|    |_|\__\___|_| |_| |_|__*/
    private fun createDrawerItem(target: NavTarget, level: Int = 1): IDrawerItem<*> {
        val item = DrawerPrimaryItem()
                .withIdentifier(target.id.toLong())
                .withName(target.name)
                .withHiddenInMiniDrawer(!app.appConfig.miniDrawerButtonIds.contains(target.id))
                .also { if (target.description != null) it.withDescription(target.description!!) }
                .also { if (target.icon != null) it.withIcon(target.icon!!) }
                .also { if (target.title != null) it.withAppTitle(getString(target.title!!)) }
                .also { if (target.badgeTypeId != null) it.withBadgeStyle(drawer.badgeStyle)}

        if (target.badgeTypeId != null)
            drawer.addUnreadCounterType(target.badgeTypeId!!, target.id)
        // TODO sub items
        /*
        if (target.subItems != null) {
            for (subItem in target.subItems!!) {
                item.subItems += createDrawerItem(subItem, level+1)
            }
        }*/

        return item
    }

    fun setDrawerItems() {
        Log.d("NavDebug", "setDrawerItems() app.profile = ${app.profile ?: "null"}")
        val drawerItems = arrayListOf<IDrawerItem<*>>()
        val drawerProfiles = arrayListOf<ProfileSettingDrawerItem>()

        val supportedFragments = if (app.profile == null) arrayListOf<Int>()
        else app.profile.supportedFragments

        targetPopToHomeList.clear()

        var separatorAdded = false

        for (target in navTargetList) {
            if (target.isInDrawer && target.isBelowSeparator && !separatorAdded) {
                separatorAdded = true
                drawerItems += DividerDrawerItem()
            }

            if (target.popToHome)
                targetPopToHomeList += target.id

            if (target.isInDrawer && (target.isStatic || supportedFragments.isEmpty() || supportedFragments.contains(target.id))) {
                drawerItems += createDrawerItem(target)
                if (target.id == 1) {
                    targetHomeId = target.id
                }
            }

            if (target.isInProfileList) {
                drawerProfiles += ProfileSettingDrawerItem()
                        .withIdentifier(target.id.toLong())
                        .withName(target.name)
                        .also { if (target.description != null) it.withDescription(target.description!!) }
                        .also { if (target.icon != null) it.withIcon(target.icon!!) }
            }
        }

        // seems that this cannot be open, because the itemAdapter has Profile items
        // instead of normal Drawer items...
        drawer.profileSelectionClose()

        drawer.setItems(*drawerItems.toTypedArray())
        drawer.removeAllProfileSettings()
        drawer.addProfileSettings(*drawerProfiles.toTypedArray())
    }

    private fun showProfileContextMenu(profile: IProfile<*>, view: View) {
        val profileId = profile.identifier.toInt()
        val popupMenu = PopupMenu(this, view)
        popupMenu.menu.add(0, 1, 1, R.string.profile_menu_open_settings)
        popupMenu.menu.add(0, 2, 2, R.string.profile_menu_remove)
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.itemId == 1) {
                if (profileId != app.profile.id) {
                    loadProfile(profileId, DRAWER_ITEM_SETTINGS)
                    return@setOnMenuItemClickListener true
                }
                loadTarget(DRAWER_ITEM_SETTINGS, null)
            } else if (item.itemId == 2) {
                app.apiEdziennik.guiRemoveProfile(this@MainActivity, profileId, profile.name?.getText(this).toString())
            }
            true
        }
        popupMenu.show()
    }

    private val targetPopToHomeList = arrayListOf<Int>()
    private var targetHomeId: Int = -1
    override fun onBackPressed() {
        if (!b.navView.onBackPressed()) {

            navigateUp()

            /*val currentDestinationId = navController.currentDestination?.id

            if (if (targetHomeId != -1 && targetPopToHomeList.contains(navController.currentDestination?.id)) {
                        if (!navController.popBackStack(targetHomeId, false)) {
                            navController.navigateUp()
                        }
                        true
                    } else {
                        navController.navigateUp()
                    }) {
                val currentId = navController.currentDestination?.id ?: -1
                val drawerSelection = navTargetList
                        .singleOrNull {
                            it.navGraphId == currentId
                        }?.also {
                            navView.toolbar.setTitle(it.title ?: it.name)
                        }?.id ?: -1
                drawer.setSelection(drawerSelection, false)
            } else {
                super.onBackPressed()
            }*/
        }
    }
}