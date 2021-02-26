/*
 *  Copyright (c) 2021 Sergey Komlach aka Salat-Cx65; Original project: https://github.com/Salat-Cx65/AdvancedBiometricPromptCompat
 *  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package dev.skomlach.biometric.compat.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
object SettingsHelper {
    @JvmStatic
    fun getInt(context: Context, secureSettingKey: String?, defaultValue: Int): Int {
        return getLong(context, secureSettingKey, defaultValue.toLong()).toInt()
    }
    @JvmStatic
    fun getLong(context: Context, secureSettingKey: String?, defaultValue: Long): Long {
        var result = getLongInternal(context, secureSettingKey, defaultValue)
        if (result == defaultValue) {
            result = getIntInternal(context, secureSettingKey, defaultValue.toInt()).toLong()
        }
        return result
    }
    @JvmStatic
    fun getString(context: Context, secureSettingKey: String?, defaultValue: String): String {
        try {
            val result = Settings.Secure.getString(context.contentResolver, secureSettingKey)
            if (defaultValue != result) return result
        } catch (e: Throwable) {
        }
        //fallback
        try {
            val result = Settings.System.getString(context.contentResolver, secureSettingKey)
            if (defaultValue != result) return result
        } catch (e: Throwable) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) try {
            val result = Settings.Global.getString(context.contentResolver, secureSettingKey)
            if (defaultValue != result) return result
        } catch (e: Throwable) {
        }
        return defaultValue
    }

    private fun getLongInternal(
        context: Context,
        secureSettingKey: String?,
        defaultValue: Long
    ): Long {
        try {
            val result = Settings.Secure.getLong(context.contentResolver, secureSettingKey)
            if (result != defaultValue) return result
        } catch (e: Throwable) {
        }
        //fallback
        try {
            val result = Settings.System.getLong(context.contentResolver, secureSettingKey)
            if (result != defaultValue) return result
        } catch (e: Throwable) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) try {
            val result = Settings.Global.getLong(context.contentResolver, secureSettingKey)
            if (result != defaultValue) return result
        } catch (e: Throwable) {
        }
        return defaultValue
    }

    private fun getIntInternal(
        context: Context,
        secureSettingKey: String?,
        defaultValue: Int
    ): Int {
        try {
            val result = Settings.Secure.getInt(context.contentResolver, secureSettingKey)
            if (result != defaultValue) return result
        } catch (e: Throwable) {
        }
        //fallback
        try {
            val result = Settings.System.getInt(context.contentResolver, secureSettingKey)
            if (result != defaultValue) return result
        } catch (e: Throwable) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) try {
            val result = Settings.Global.getInt(context.contentResolver, secureSettingKey)
            if (result != defaultValue) return result
        } catch (e: Throwable) {
        }
        return defaultValue
    }
}