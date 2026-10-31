package com.mayuri.ai.core.actions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat

sealed class ActionResponse {
    data class Success(val message: String) : ActionResponse()
    data class Failure(val reason: String) : ActionResponse()
    data class NeedConfirmation(val message: String, val onConfirmAction: () -> Unit) : ActionResponse()
}

class DeviceActionHandler(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // 1. Flashlight Control
    fun toggleFlashlight(enable: Boolean): ActionResponse {
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionResponse.Failure("Flashlight hardware nahi mila.")

            cameraManager.setTorchMode(cameraId, enable)
            ActionResponse.Success(if (enable) "Flashlight on kar di gayi hai." else "Flashlight off kar di hai.")
        } catch (e: CameraAccessException) {
            ActionResponse.Failure("Camera access error: ${e.localizedMessage}")
        }
    }

    // 2. Volume Control
    fun adjustVolume(increase: Boolean): ActionResponse {
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        return ActionResponse.Success(if (increase) "Volume badha diya gaya hai." else "Volume kam kar diya gaya hai.")
    }

    // 3. Launch Any Installed Application
    fun openApplication(appName: String): ActionResponse {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in packages) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(appName.lowercase())) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ActionResponse.Success("$label open kar rahi hoon.")
                }
            }
        }
        return ActionResponse.Failure("Aapke phone me $appName application nahi mila.")
    }

    // 4. Initiating Phone Calls safely
    fun initiateCall(query: String, onExecuteCall: (Intent) -> Unit): ActionResponse {
        val isNumber = query.replace("[^0-9+]".toRegex(), "").length >= 5
        if (isNumber) {
            val number = query.replace("[^0-9+]".toRegex(), "")
            return prepareCallIntent(number, onExecuteCall)
        }

        // Contact Lookup
        val contactNumber = queryContactNumber(query)
        return if (contactNumber != null) {
            prepareCallIntent(contactNumber, onExecuteCall)
        } else {
            ActionResponse.Failure("Contact book me '$query' nahi mila.")
        }
    }

    private fun prepareCallIntent(phoneNumber: String, onExecuteCall: (Intent) -> Unit): ActionResponse {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        
        return ActionResponse.NeedConfirmation("Kya aap $phoneNumber par call lagana chahte hain?") {
            onExecuteCall(intent)
        }
    }

    private fun queryContactNumber(name: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return cursor.getString(numberIndex)
            }
        }
        return null
    }

    // 5. Battery Status
    fun getBatteryLevel(): ActionResponse {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return ActionResponse.Success("Phone ki battery $level% hai.")
    }

    // 6. Alarms & Timers
    fun setAlarm(hour: Int, minutes: Int, message: String): ActionResponse {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResponse.Success("$hour bajke $minutes minute par alarm set kar diya gaya hai.")
        } catch (e: Exception) {
            ActionResponse.Failure("Alarm set karne me dikkat aayi.")
        }
    }

    // 7. System Settings Deep-links
    fun openSettings(type: String): ActionResponse {
        val action = when (type.lowercase()) {
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
        return ActionResponse.Success("$type settings open kar di gayi hai.")
    }

    // 8. Social Media Share Sheet / WhatsApp Direct Msg
    fun shareSocialContent(text: String, appPackage: String? = null): ActionResponse {
        val intent = Intent(Intent.ACTION_SEND).apply {
            this.type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            appPackage?.let { setPackage(it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResponse.Success("Ready! Content app me transfer kar diya gaya hai.")
        } catch (e: Exception) {
            ActionResponse.Failure("Application available nahi hai. Play Store se check karein.")
        }
    }
}