package com.mayuri.ai.core.actions

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings

/** Executes native Android actions requested by the AI intent. */
class DeviceActionHandler(private val app: Application) {

    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun toggleFlashlight(on: Boolean): ActionResponse = try {
        val cameraId = cameraManager.cameraIdList.firstOrNull()
        if (cameraId == null) {
            ActionResponse.Failure("Is device par flashlight nahi mila.")
        } else {
            cameraManager.setTorchMode(cameraId, on)
            ActionResponse.Success(if (on) "Flashlight on kar di." else "Flashlight off kar di.")
        }
    } catch (e: Exception) {
        ActionResponse.Failure(e.localizedMessage ?: "Flashlight control nahi ho paya.")
    }

    fun adjustVolume(increase: Boolean): ActionResponse = try {
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        ActionResponse.Success(if (increase) "Volume badha diya." else "Volume kam kar diya.")
    } catch (e: Exception) {
        ActionResponse.Failure(e.localizedMessage ?: "Volume control nahi ho paya.")
    }

    fun openSettings(target: String): ActionResponse {
        val action = when (target.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return launch(Intent(action))
    }

    fun openApplication(name: String): ActionResponse {
        val pm = app.packageManager

        pm.getLaunchIntentForPackage(name.lowercase())?.let { return launch(it) }

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val installed = pm.queryIntentActivities(launcherIntent, 0)
        val match = installed.firstOrNull {
            it.loadLabel(pm).toString().equals(name, ignoreCase = true)
        } ?: installed.firstOrNull {
            it.loadLabel(pm).toString().contains(name, ignoreCase = true)
        }

        val packageName = match?.activityInfo?.packageName
        val intent = packageName?.let { pm.getLaunchIntentForPackage(it) }
        return if (intent != null) {
            launch(intent)
        } else {
            ActionResponse.Failure("$name naam ki koi app mujhe nahi mili.")
        }
    }

    fun initiateCall(contact: String, onReady: (Intent) -> Unit): ActionResponse {
        val trimmed = contact.trim()
        val number = if (trimmed.matches(Regex("^[+0-9][0-9\\s-]{2,}$"))) {
            trimmed
        } else {
            lookupContactNumber(trimmed)
        }

        if (number.isNullOrBlank()) {
            return ActionResponse.Failure("$trimmed ka number mujhe nahi mila.")
        }

        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(number)))
        onReady(callIntent)
        return ActionResponse.NeedConfirmation("Ji, main $trimmed ko call mila rahi hoon.")
    }

    fun shareSocialContent(text: String, packageName: String?): ActionResponse {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (!packageName.isNullOrBlank()) setPackage(packageName)
        }
        return launch(sendIntent)
    }

    private fun lookupContactNumber(name: String): String? = try {
        val uri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
            Uri.encode(name)
        )
        app.contentResolver.query(
            uri,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }

    private fun launch(intent: Intent): ActionResponse = try {
        app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        ActionResponse.Success("Theek hai, kar diya.")
    } catch (e: Exception) {
        ActionResponse.Failure(e.localizedMessage ?: "Ye action nahi ho paya.")
    }
}
