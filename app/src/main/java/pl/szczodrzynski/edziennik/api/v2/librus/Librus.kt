package pl.szczodrzynski.edziennik.api.v2.librus

import android.content.Context
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.AppError
import pl.szczodrzynski.edziennik.api.AppError.*
import pl.szczodrzynski.edziennik.api.interfaces.*
import pl.szczodrzynski.edziennik.api.v2.LOGIN_MODE_LIBRUS_EMAIL
import pl.szczodrzynski.edziennik.api.v2.LOGIN_MODE_LIBRUS_JST
import pl.szczodrzynski.edziennik.api.v2.LOGIN_MODE_LIBRUS_SYNERGIA
import pl.szczodrzynski.edziennik.api.v2.librus.firstlogin.FirstLoginLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.firstlogin.FirstLoginSynergia
import pl.szczodrzynski.edziennik.api.v2.librus.login.LoginJst
import pl.szczodrzynski.edziennik.api.v2.librus.login.LoginLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.login.LoginSynergia
import pl.szczodrzynski.edziennik.api.v2.librus.login.SynergiaTokenExtractor
import pl.szczodrzynski.edziennik.api.v2.models.DataStore
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.MessageFull
import pl.szczodrzynski.edziennik.datamodels.Profile
import pl.szczodrzynski.edziennik.datamodels.ProfileFull
import pl.szczodrzynski.edziennik.messages.MessagesComposeInfo
import pl.szczodrzynski.edziennik.models.Endpoint
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.lang.Exception

class Librus(val app: App, val profile: Profile?, val loginStore: LoginStore) : EdziennikInterface {
    private val TAG = "librus.Librus"

    lateinit var syncCallback: SyncCallback
    lateinit var featureList: ArrayList<Int>
    lateinit var dataStore: DataStore
    var onLogin: (() -> Unit)? = null
    val internalErrorList = ArrayList<Int>()

    fun isError(error: AppError?): Boolean {
        if (error == null)
            return false
        syncCallback.onError(null, error)
        return true
    }

    /*    _      _ _
         | |    (_) |
         | |     _| |__  _ __ _   _ ___
         | |    | | '_ \| '__| | | / __|
         | |____| | |_) | |  | |_| \__ \
         |______|_|_.__/|_|   \__,_|__*/
    private fun loginLibrus() {
        LoginLibrus(app, loginStore, syncCallback) {
            if (profile == null) {
                firstLoginLibrus()
                return@LoginLibrus
            }
            synergiaTokenExtractor()
        }
    }
    private fun firstLoginLibrus() {
        FirstLoginLibrus(app, loginStore, syncCallback) { profileList ->
            syncCallback.onLoginFirst(profileList, loginStore)
        }
    }
    private fun synergiaTokenExtractor() {
        if (profile == null) {
            throw Exception("Profile may not be null")
        }
        SynergiaTokenExtractor(app, profile, loginStore, syncCallback) {
            d(TAG, "Profile $profile")
            d(TAG, "LoginStore $loginStore")
            onLogin?.invoke()
        }
    }
    /*     _____                            _
          / ____|                          (_)
         | (___  _   _ _ __   ___ _ __ __ _ _  __ _
          \___ \| | | | '_ \ / _ \ '__/ _` | |/ _` |
          ____) | |_| | | | |  __/ | | (_| | | (_| |
         |_____/ \__, |_| |_|\___|_|  \__, |_|\__,_|
                  __/ |                __/ |
                 |___/                |__*/
    private fun loginSynergia() {
        LoginSynergia(app, loginStore, syncCallback) {
            if (profile == null) {
                firstLoginSynergia()
                return@LoginSynergia
            }
            onLogin?.invoke()
        }
    }
    private fun firstLoginSynergia() {
        FirstLoginSynergia(app, loginStore, syncCallback) { profileList ->
            syncCallback.onLoginFirst(profileList, loginStore)
        }
    }
    /*         _  _____ _______
              | |/ ____|__   __|
              | | (___    | |
          _   | |\___ \   | |
         | |__| |____) |  | |
          \____/|_____/   |*/
    private fun loginJst() {
        LoginJst(app, loginStore, syncCallback) {
            if (profile == null) {
                firstLoginSynergia()
                return@LoginJst
            }
            onLogin?.invoke()
        }
    }

    private fun wrapCallback(callback: SyncCallback): SyncCallback {
        return object : SyncCallback {
            override fun onSuccess(activityContext: Context?, profileFull: ProfileFull?) {
                callback.onSuccess(activityContext, profileFull)
            }

            override fun onProgress(progressStep: Int) {
                callback.onProgress(progressStep)
            }

            override fun onActionStarted(stringResId: Int) {
                callback.onActionStarted(stringResId)
            }

            override fun onLoginFirst(profileList: MutableList<Profile>?, loginStore: LoginStore?) {
                callback.onLoginFirst(profileList, loginStore)
            }

            override fun onError(activityContext: Context?, error: AppError) {
                when (error.errorCode) {
                    in internalErrorList -> {
                        // finish immediately if the same error occurs twice during the same sync
                        callback.onError(activityContext, error)
                    }
                    CODE_INTERNAL_LIBRUS_ACCOUNT_410 -> {
                        internalErrorList.add(error.errorCode)
                        loginStore.removeLoginData("refreshToken") // force a clean login
                        loginLibrus()
                    }
                    else -> callback.onError(activityContext, error)
                }
            }
        }
    }

    fun login(callback: SyncCallback) {
        this.internalErrorList.clear()
        this.syncCallback = wrapCallback(callback)
        when (loginStore.mode) {
            LOGIN_MODE_LIBRUS_EMAIL -> {
                loginLibrus()
            }
            LOGIN_MODE_LIBRUS_SYNERGIA -> {

            }
            LOGIN_MODE_LIBRUS_JST -> {

            }
        }
    }

    fun getData() {

    }

    override fun sync(activityContext: Context, callback: SyncCallback, profileId: Int, profile: Profile?, loginStore: LoginStore) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun syncMessages(activityContext: Context, errorCallback: SyncCallback, profile: ProfileFull) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun syncFeature(activityContext: Context, callback: SyncCallback, profile: ProfileFull, vararg featureList: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMessage(activityContext: Context, errorCallback: SyncCallback, profile: ProfileFull, message: MessageFull, messageCallback: MessageGetCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAttachment(activityContext: Context, errorCallback: SyncCallback, profile: ProfileFull, message: MessageFull, attachmentId: Long, attachmentCallback: AttachmentGetCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRecipientList(activityContext: Context, errorCallback: SyncCallback, profile: ProfileFull, recipientListGetCallback: RecipientListGetCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getComposeInfo(profile: ProfileFull): MessagesComposeInfo {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getConfigurableEndpoints(profile: Profile?): MutableMap<String, Endpoint> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEndpointEnabled(profile: Profile?, defaultActive: Boolean, name: String?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}