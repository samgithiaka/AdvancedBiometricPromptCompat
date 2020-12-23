package dev.skomlach.biometric.compat

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import dev.skomlach.biometric.compat.engine.BiometricAuthentication
import dev.skomlach.biometric.compat.utils.BiometricErrorLockoutPermanentFix
import dev.skomlach.biometric.compat.utils.HardwareAccessImpl
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl
import dev.skomlach.common.misc.Utils
import org.ifaa.android.manager.IFAAManagerFactory

object BiometricManagerCompat {

    @JvmStatic
    fun isBiometricSensorPermanentlyLocked(
        api: BiometricAuthRequest = BiometricAuthRequest(
            BiometricApi.AUTO,
            BiometricType.BIOMETRIC_ANY
        )
    ): Boolean {
        check(BiometricPromptCompat.isInit) { "Please call BiometricPromptCompat.init(null);  first" }
        return BiometricErrorLockoutPermanentFix.INSTANCE.isBiometricSensorPermanentlyLocked(api.type)
    }
    @JvmStatic
    fun isHardwareDetected(
        api: BiometricAuthRequest = BiometricAuthRequest(
            BiometricApi.AUTO,
            BiometricType.BIOMETRIC_ANY
        )
    ): Boolean {
        check(BiometricPromptCompat.isInit) { "Please call BiometricPromptCompat.init(null);  first" }
        return HardwareAccessImpl.getInstance(api).isHardwareAvailable
    }
    @JvmStatic
    fun hasEnrolled(
        api: BiometricAuthRequest = BiometricAuthRequest(
            BiometricApi.AUTO,
            BiometricType.BIOMETRIC_ANY
        )
    ): Boolean {
        check(BiometricPromptCompat.isInit) { "Please call BiometricPromptCompat.init(null);  first" }
        return HardwareAccessImpl.getInstance(api).isBiometricEnrolled
    }
    @JvmStatic
    fun isLockOut(
        api: BiometricAuthRequest = BiometricAuthRequest(
            BiometricApi.AUTO,
            BiometricType.BIOMETRIC_ANY
        )
    ): Boolean {
        check(BiometricPromptCompat.isInit) { "Please call BiometricPromptCompat.init(null);  first" }
        return HardwareAccessImpl.getInstance(api).isLockedOut
    }
    @JvmStatic
    fun isNewBiometricApi(
        api: BiometricAuthRequest = BiometricAuthRequest(
            BiometricApi.AUTO,
            BiometricType.BIOMETRIC_ANY
        )
    ): Boolean {
        check(BiometricPromptCompat.isInit) { "Please call BiometricPromptCompat.init(null);  first" }
        return HardwareAccessImpl.getInstance(api).isNewBiometricApi
    }
    @JvmStatic
    fun openSettings(
        activity: Activity, api: BiometricAuthRequest = BiometricAuthRequest(
            BiometricApi.AUTO,
            BiometricType.BIOMETRIC_ANY
        )
    ) {
        check(BiometricPromptCompat.isInit) { "Please call BiometricPromptCompat.init(null);  first" }

        try {
            //https://git.aicp-rom.com/device_oneplus_oneplus3.git/tree/org.ifaa.android.manager/src/org/ifaa/android/manager/IFAAManagerFactory.java?h=refs/changes/03/28003/1
            //https://github.com/shivatejapeddi/android_device_xiaomi_sdm845-common/tree/10.x-vendor/org.ifaa.android.manager/src/org/ifaa/android/manager
            val authType = when (api.type) {
                BiometricType.BIOMETRIC_FINGERPRINT -> 1
                BiometricType.BIOMETRIC_IRIS -> 2
                else -> 0
            }
            val ifaamanager = IFAAManagerFactory.getIFAAManager(
                activity,
                authType
            )
            BiometricLoggerImpl.e("IFAA details: ${ifaamanager.deviceModel}/${ifaamanager.version}")

            if (ifaamanager.startBIOManager(activity, authType) == 0
            ) {
                return
            }
        } catch (ignore: Throwable) {
        }

        if (!HardwareAccessImpl.getInstance(api).isNewBiometricApi) {
            BiometricAuthentication.openSettings(activity)
        } else {
            //for unknown reasons on some devices happens SecurityException - "Permission.MANAGE_BIOMETRIC required" - but not should be
            if (Utils.startActivity(Intent("android.settings.BIOMETRIC_ENROLL"), activity)) {
                return
            }
            if (Utils.startActivity(Intent("android.settings.BIOMETRIC_SETTINGS"), activity)) {
                return
            }
            if (Utils.startActivity(
                    Intent().setComponent(
                        ComponentName(
                            "com.android.settings",
                            "com.android.settings.Settings\$BiometricsAndSecuritySettingsActivity"
                        )
                    ), activity
                )
            ) {
                return
            }
            if (Utils.startActivity(
                    Intent().setComponent(
                        ComponentName(
                            "com.android.settings",
                            "com.android.settings.Settings\$SecuritySettingsActivity"
                        )
                    ), activity
                )
            ) {
                return
            }
            Utils.startActivity(
                Intent(Settings.ACTION_SETTINGS), activity
            )
        }
    }

}