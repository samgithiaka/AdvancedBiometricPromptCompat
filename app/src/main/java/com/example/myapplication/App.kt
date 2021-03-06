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

package com.example.myapplication

import androidx.multidex.MultiDexApplication
import dev.skomlach.biometric.compat.BiometricAuthRequest
import dev.skomlach.biometric.compat.BiometricPromptCompat
import java.util.*

class App : MultiDexApplication() {
    companion object {
        @JvmStatic
        val authRequestList = ArrayList<BiometricAuthRequest>()

        @JvmStatic
        val onInitListeners = ArrayList<OnInitFinished>()

        @JvmStatic
        var isReady = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        LogCat.instance.setLog2ViewCallback(object : LogCat.Log2ViewCallback{
            override fun log(string: String?) {
                LogCat.instance.setLog2ViewCallback(null)
                BiometricPromptCompat.logging(true)
                BiometricPromptCompat.init {
                    authRequestList.addAll(BiometricPromptCompat.availableAuthRequests)
                    for (listener in onInitListeners) {
                        listener.onFinished()
                    }
                    onInitListeners.clear()
                    isReady = true
                }
            }
        })

        LogCat.instance.start()
    }

    interface OnInitFinished {
        fun onFinished()
    }
}