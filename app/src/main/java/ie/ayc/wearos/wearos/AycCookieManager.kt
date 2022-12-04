package ie.ayc.wearos

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.lang.Exception
import java.lang.RuntimeException
import java.net.*

class AycCookieManager private constructor() {
    private var mCookieManager: CookieManager? = null

    init {
        mCookieManager = CookieManager()
        mCookieManager!!.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(mCookieManager)
        loadCookies()
    }

    val cookies: List<HttpCookie>?
        get() = mCookieManager?.cookieStore?.cookies

    fun addCookies(list: List<String>?) {
        try {
            if (list == null) {
                Log.v("ayc-cookie-add", "empty list")
                return
            }
            if (list.size == 0) {
                Log.v("ayc-cookie-add", "empty list")
                return
            }
            for (cookie in list) {
                val allparts = cookie.split(";").toTypedArray()
                val cookie_parts = allparts[0].split("=").toTypedArray()
                if (cookie_parts.size > 1) {
                    val thecookie = HttpCookie(cookie_parts[0], cookie_parts[1])
                    mCookieManager!!.cookieStore.add(URI("https://ashtangayoga.ie"), thecookie)
                    Log.v("ayc-cookie-update", "updated cookie store with: $cookie")
                } else {
                    Log.v("ayc-cookie-single", cookie)
                }
                saveCookies()
            }
        } catch (e: Exception) {
            Log.v("ayc-cookie-add", "failed to update cookie store")
            Log.v("ayc-cookie-add", e.message!!)
            var i = 0
            while (i < e.stackTrace.size) {
                val x = e.stackTrace[i]
                Log.v(
                    "ayc-cookie-add",
                    x.fileName + ": " + x.methodName + ": " + x.lineNumber + ": " + x.toString()
                )
                i++
            }
        }
    }

    fun clearCookies() {
        if (mCookieManager != null) {
            mCookieManager!!.cookieStore.removeAll()
            Log.v("ayc-cookie-clear", "clear cookies")
            saveCookies()
        }
    }

    fun saveCookies() {
        try {
            val outputStream =
                MainActivity.instance.openFileOutput(COOKIES_FILE, Context.MODE_PRIVATE)
            val imploded = implode(cookies)
            Log.v("ayc-cookie-save", "imploded: $imploded")
            outputStream.write(imploded.toByteArray())
            outputStream.close()
            Log.v("ayc-cookie-save", "saved cookies to file")
        } catch (e: Exception) {
            Log.v("ayc-cookie-save", "failed ot save cookies to file")
        }
    }

    fun loadCookies() {
        try {
            val inputStream: InputStream = MainActivity.instance.openFileInput(COOKIES_FILE)
            val buf = BufferedReader(InputStreamReader(inputStream))
            var line = buf.readLine()
            while (line != null) {
                //sb.append(line);
                val cookie_parts = line.split("=").toTypedArray()
                mCookieManager!!.cookieStore.add(
                    URI("https://ashtangayoga.ie"), HttpCookie(
                        cookie_parts[0], cookie_parts[1]
                    )
                )
                Log.v("ayc-cookie-load", "load cookie: $line")
                line = buf.readLine()
            }
            Log.v("ayc-cookie-load", "Done loading cookies")
        } catch (e: Exception) {
            Log.v("ayc-cookie-load", "failed to load cookies from file")
        }
    }

    val isCookieManagerEmpty: Boolean
        get() = mCookieManager?.cookieStore?.cookies?.isEmpty() ?: true

    val cookieValue: String
        get() {
            var cookieValue = ""
            if (!isCookieManagerEmpty) {
                for (eachCookie in cookies!!) {
                    Log.v(
                        "ayc-cookie-get",
                        String.format("%s=%s; ", eachCookie.name, eachCookie.value)
                    )
                    cookieValue =
                        cookieValue + String.format("%s=%s; ", eachCookie.name, eachCookie.value)
                }
            }
            return cookieValue
        }

    companion object {
        private const val COOKIES_FILE = "cookies"
        private const val COOKIES_DELIMETER = "\n"
        var instance: AycCookieManager? = null
            get() {
                if (field == null) {
                    field = AycCookieManager()
                }
                return field
            }
            private set

        fun encodeValue(value: String?): String {
            return try {
                URLEncoder.encode(value, "UTF-8")
            } catch (ex: UnsupportedEncodingException) {
                throw RuntimeException(ex.cause)
            }
        }

        private fun implode(list: List<HttpCookie>?): String {
            Log.v("ayc-cookie-save", "imploding: " + list!!.size + " cookies")
            if (list.size == 0) return ""
            if (list.size == 1) return list[0].name + "=" + list[0].value
            var imploded = ""
            var y = 0
            while (y < list.size - 1) {
                imploded += list[y].name + "=" + list[y].value + COOKIES_DELIMETER
                y++
            }
            imploded += list[y].name + "=" + list[y].value
            return imploded
        }
    }
}