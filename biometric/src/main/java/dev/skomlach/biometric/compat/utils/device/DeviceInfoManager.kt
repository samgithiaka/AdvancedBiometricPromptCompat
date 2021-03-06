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

package dev.skomlach.biometric.compat.utils.device

import android.content.Context
import android.net.ConnectivityManager
import android.os.Looper
import android.text.TextUtils
import androidx.annotation.WorkerThread
import dev.skomlach.biometric.compat.utils.device.DeviceModel.getNames
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl
import dev.skomlach.common.contextprovider.AndroidContext.appContext
import dev.skomlach.common.cryptostorage.SharedPreferenceProvider.getCryptoPreferences
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.net.ssl.SSLHandshakeException

class DeviceInfoManager private constructor() {
    private val pattern = Pattern.compile("\\((.*?)\\)+")
    fun hasFingerprint(deviceInfo: DeviceInfo?): Boolean {
        if (deviceInfo?.sensors == null) return false
        for (str in deviceInfo.sensors) {
            val s = str.toLowerCase(Locale.US)
            if (s.contains("fingerprint")) {
                return true
            }
        }
        return false
    }

    fun hasUnderDisplayFingerprint(deviceInfo: DeviceInfo?): Boolean {
        if (deviceInfo?.sensors == null) return false
        for (str in deviceInfo.sensors) {
            val s = str.toLowerCase(Locale.US)
            if (s.contains("fingerprint") && s.contains("under display")) {
                return true
            }
        }
        return false
    }

    fun hasIrisScanner(deviceInfo: DeviceInfo?): Boolean {
        if (deviceInfo?.sensors == null) return false
        for (str in deviceInfo.sensors) {
            val s = str.toLowerCase(Locale.US)
            if (s.contains(" id") || s.contains(" recognition") || s.contains(" unlock") || s.contains(
                    " auth"
                )
            ) {
                if (s.contains("iris")) {
                    return true
                }
            }
        }
        return false
    }

    fun hasFaceID(deviceInfo: DeviceInfo?): Boolean {
        if (deviceInfo?.sensors == null) return false
        for (str in deviceInfo.sensors) {
            val s = str.toLowerCase(Locale.US)
            if (s.contains(" id") || s.contains(" recognition") || s.contains(" unlock") || s.contains(
                    " auth"
                )
            ) {
                if (s.contains("face")) {
                    return true
                }
            }
        }
        return false
    }

    @WorkerThread
    fun getDeviceInfo(onDeviceInfoListener: OnDeviceInfoListener) {
        if (Looper.getMainLooper().thread === Thread.currentThread()) throw IllegalThreadStateException(
            "Worker thread required"
        )
        var deviceInfo = cachedDeviceInfo
        if (deviceInfo != null) {
            onDeviceInfoListener.onReady(deviceInfo)
            return
        }
        val strings = getNames()
        for (m in strings) {
            deviceInfo = loadDeviceInfo(m)
            if (deviceInfo?.sensors != null) {
                BiometricLoggerImpl.e("DeviceInfoManager: " + deviceInfo.model + " -> " + deviceInfo)
                setCachedDeviceInfo(deviceInfo)
                onDeviceInfoListener.onReady(deviceInfo)
                return
            }
        }
        if (deviceInfo != null) {
            BiometricLoggerImpl.e("DeviceInfoManager: " + deviceInfo.model + " -> " + deviceInfo)
            setCachedDeviceInfo(deviceInfo)
        }
        onDeviceInfoListener.onReady(deviceInfo)
    }

    private val cachedDeviceInfo: DeviceInfo?
        get() {
            val sharedPreferences = getCryptoPreferences("StoredDeviceInfo")
            if (sharedPreferences.getBoolean("checked", false)) {
                val model = sharedPreferences.getString("model", null) ?: return null
                val sensors = sharedPreferences.getStringSet("sensors", null) ?: return null
                return DeviceInfo(model, sensors)
            }
            return null
        }

    private fun setCachedDeviceInfo(deviceInfo: DeviceInfo) {
        val sharedPreferences = getCryptoPreferences("StoredDeviceInfo")
            .edit()
        sharedPreferences
            .putStringSet("sensors", deviceInfo.sensors)
            .putString("model", deviceInfo.model)
            .putBoolean("checked", true)
            .apply()
    }

    private fun loadDeviceInfo(model: String): DeviceInfo? {
        BiometricLoggerImpl.e("DeviceInfoManager: loadDeviceInfo for $model")
        return if (model.isNullOrEmpty()) null else try {
            val url = "https://m.gsmarena.com/res.php3?sSearch=" + URLEncoder.encode(model)
            var html: String? = getHtml(url) ?: return null
            val detailsLink = getDetailsLink(url, html, model)
                ?: return DeviceInfo(model, null)

            //not found
            BiometricLoggerImpl.e("DeviceInfoManager: Link: $detailsLink")
            html = getHtml(detailsLink)
            if (html == null) return null
            val l = getSensorDetails(html)
            BiometricLoggerImpl.e("DeviceInfoManager: Sensors: $l")
            DeviceInfo(model, l)
        } catch (e: Throwable) {
            null
        }
    }

    //parser
    private fun getSensorDetails(html: String?): Set<String> {
        val list: MutableSet<String> = HashSet()
        if (html != null) {
            val doc = Jsoup.parse(html)
            val body = doc.body().getElementById("content")
            val rElements = body.getElementsByAttribute("data-spec")
            for (i in rElements.indices) {
                val element = rElements[i]
                if (element.attr("data-spec") == "sensors") {
                    var name = element.text()
                    if (!name.isNullOrEmpty()) {
                        val matcher = pattern.matcher(name)
                        while (matcher.find()) {
                            val s = matcher.group()
                            name = name.replace(s, s.replace(",", ";"))
                        }
                        val split = name.split(",").toTypedArray()
                        for (s in split) {
                            list.add(capitalize(s.trim { it <= ' ' }))
                        }
                    }
                }
            }
        }
        return list
    }

    private fun getDetailsLink(url: String, html: String?, model: String): String? {
        if (html != null) {
            val doc = Jsoup.parse(html)
            val body = doc.body().getElementById("content")
            val rElements = body.getElementsByTag("a")
            for (i in rElements.indices) {
                val element = rElements[i]
                val name = element.text()
                if (name.isNullOrEmpty()) {
                    continue
                }
                if (name.equals(model, ignoreCase = true)) {
                    return Network.resolveUrl(url, element.attr("href"))
                }
            }
        }
        return null
    }

    //tools
    private fun getHtml(url: String): String? {
        try {
            var urlConnection: HttpURLConnection? = null
            val connectivityManager =
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (connectivityManager?.activeNetworkInfo?.isConnectedOrConnecting == true) {
                return try {
                    urlConnection = Network.createConnection(
                        url, TimeUnit.SECONDS.toMillis(30)
                            .toInt()
                    )
                    urlConnection.requestMethod = "GET"
                    urlConnection.setRequestProperty("Content-Language", "en-US")
                    urlConnection.setRequestProperty("Accept-Language", "en-US")
                    urlConnection.setRequestProperty(
                        "User-Agent",
                        agents[SecureRandom().nextInt(agents.size)]
                    )
                    urlConnection.connect()
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    var inputStream: InputStream? = null
                    inputStream = urlConnection.inputStream
                    if (inputStream == null) inputStream = urlConnection.errorStream
                    Network.fastCopy(inputStream, byteArrayOutputStream)
                    inputStream.close()
                    val data = byteArrayOutputStream.toByteArray()
                    byteArrayOutputStream.close()
                    String(data)
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect()
                        urlConnection = null
                    }
                }
            }
        } catch (e: Throwable) {
            //ignore - old device cannt resolve SSL connection
            if (e is SSLHandshakeException) {
                return "<html></html>"
            }
            BiometricLoggerImpl.e(e)
        }
        return null
    }

    interface OnDeviceInfoListener {
        fun onReady(deviceInfo: DeviceInfo?)
    }

    companion object {
        @JvmField var INSTANCE = DeviceInfoManager()
        val agents = arrayOf(
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/602.2.14 (KHTML, like Gecko) Version/10.0.1 Safari/602.2.14",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.98 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.98 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0"
        )

        private fun capitalize(s: String?): String {
            if (s.isNullOrEmpty()) {
                return ""
            }
            val first = s[0]
            return if (Character.isUpperCase(first)) {
                s
            } else {
                Character.toUpperCase(first).toString() + s.substring(1)
            }
        }
    }
}