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

package dev.skomlach.common.cryptostorage

import android.content.SharedPreferences
import dev.skomlach.common.contextprovider.AndroidContext.appContext

object SharedPreferenceProvider {
    private var dependencies: CryptoPreferencesProvider? = null

    @JvmStatic
    @Synchronized
    fun getCryptoPreferences(name: String): SharedPreferences {
        if (dependencies == null) {
            dependencies = EncryptedPreferencesProvider(appContext)
        }
        return (dependencies as CryptoPreferencesProvider).getCryptoPreferences(name)
    }
}