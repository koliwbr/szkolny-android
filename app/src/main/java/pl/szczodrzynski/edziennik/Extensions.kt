package pl.szczodrzynski.edziennik

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import im.wangchao.mhttp.callback.TextCallbackHandler
import im.wangchao.mhttp.internal.exception.ResponseFailException
import pl.szczodrzynski.edziennik.datamodels.Profile
import pl.szczodrzynski.edziennik.datamodels.Teacher
import pl.szczodrzynski.navlib.R
import pl.szczodrzynski.navlib.crc16
import pl.szczodrzynski.navlib.getColorFromRes
import kotlin.contracts.contract

fun List<Teacher>.byId(id: Long) = firstOrNull { it.id == id }
fun List<Teacher>.byNameFirstLast(nameFirstLast: String) = firstOrNull { it.name + " " + it.surname == nameFirstLast }
fun List<Teacher>.byNameLastFirst(nameLastFirst: String) = firstOrNull { it.surname + " " + it.name == nameLastFirst }
fun List<Teacher>.byNameFDotLast(nameFDotLast: String) = firstOrNull { it.name + "." + it.surname == nameFDotLast }
fun List<Teacher>.byNameFDotSpaceLast(nameFDotSpaceLast: String) = firstOrNull { it.name + ". " + it.surname == nameFDotSpaceLast }

fun JsonObject.getString(key: String): String? = get(key).let { if (it.isJsonNull) null else it.asString }
fun JsonObject.getInt(key: String): Int? = get(key).let { if (it.isJsonNull) null else it.asInt }
fun JsonObject.getLong(key: String): Long? = get(key).let { if (it.isJsonNull) null else it.asLong }
fun JsonObject.getJsonObject(key: String): JsonObject? = get(key).let { if (it.isJsonNull) null else it.asJsonObject }
fun JsonObject.getJsonArray(key: String): JsonArray? = get(key).let { if (it.isJsonNull) null else it.asJsonArray }

fun CharSequence?.isNotNullNorEmpty(): Boolean {
    return this != null && this.isNotEmpty()
}

fun currentTimeUnix() = System.currentTimeMillis() / 1000

fun Bundle?.getInt(key: String, defaultValue: Int): Int {
    return this?.getInt(key, defaultValue) ?: defaultValue
}
fun Bundle?.getLong(key: String, defaultValue: Long): Long {
    return this?.getLong(key, defaultValue) ?: defaultValue
}
fun Bundle?.getFloat(key: String, defaultValue: Float): Float {
    return this?.getFloat(key, defaultValue) ?: defaultValue
}
fun Bundle?.getString(key: String, defaultValue: String): String {
    return this?.getString(key, defaultValue) ?: defaultValue
}

fun colorFromName(context: Context, name: String?): Int {
    var crc = crc16(name ?: "")
    crc = (crc and 0xff) or (crc shr 8)
    crc %= 16
    val color = when (crc) {
        13 -> R.color.md_red_500
        4  -> R.color.md_pink_A400
        2  -> R.color.md_purple_A400
        9  -> R.color.md_deep_purple_A700
        5  -> R.color.md_indigo_500
        1  -> R.color.md_indigo_A700
        6  -> R.color.md_cyan_A200
        14 -> R.color.md_teal_400
        15 -> R.color.md_green_500
        7  -> R.color.md_yellow_A700
        3  -> R.color.md_deep_orange_A400
        8  -> R.color.md_deep_orange_A700
        10 -> R.color.md_brown_500
        12 -> R.color.md_grey_400
        11 -> R.color.md_blue_grey_400
        else -> R.color.md_light_green_A700
    }
    return context.getColorFromRes(color)
}

fun MutableList<out Profile>.filterOutArchived() {
    this.removeAll { it.archived }
}

fun Activity.isStoragePermissionGranted(): Boolean {
    return if (Build.VERSION.SDK_INT >= 23) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            false
        }
    } else {
        true
    }
}