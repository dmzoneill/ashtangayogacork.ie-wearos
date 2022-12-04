package ie.ayc.wearos

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class AycRequest {
    companion object {
        lateinit var instance: AycRequest

        fun GetInstance(): AycRequest{
            if(!::instance.isInitialized) {
                instance = AycRequest()
            }
            return instance
        }
    }

    private var loggedIn = false
    private lateinit var bookings: JSONArray
    private var bookingsLastUpdated = 0;

    fun IsLoggedIn(): Boolean {
        return loggedIn
    }

    fun DoToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(MainActivity.instance, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun request(url: String, postfields: String = "", debug: Boolean = false): String {
        try {
            val ayccm = AycCookieManager.instance

            val endpoint = URL(url)
            val urlConnection: HttpURLConnection = endpoint.openConnection() as HttpURLConnection

            urlConnection.useCaches = true
            if (postfields != "") {
                urlConnection.doInput = true
            }
            urlConnection.doOutput = true
            urlConnection.instanceFollowRedirects = true
            urlConnection.connectTimeout = 4000
            if (postfields != "") {
                urlConnection.requestMethod = "POST"
            } else {
                urlConnection.requestMethod = "GET"
            }
            urlConnection.addRequestProperty("Cookie", ayccm?.cookieValue ?: "");
            urlConnection.setRequestProperty("Host", "ashtangayoga.ie")
            urlConnection.setRequestProperty("authority", "ashtangayoga.ie")
            urlConnection.setRequestProperty(
                "accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
            )
            urlConnection.setRequestProperty("accept-language", "en-GB,en-US;q=0.9,en;q=0.8")
            urlConnection.setRequestProperty("cache-control", "no-cache")
            urlConnection.setRequestProperty("dnt", "1")
            urlConnection.setRequestProperty("origin", "https://ashtangayoga.ie")
            urlConnection.setRequestProperty("pragma", "no-cache")
            urlConnection.setRequestProperty("sec-ch-ua-mobile", "?0")
            urlConnection.setRequestProperty("sec-ch-ua-platform", "Linux")
            urlConnection.setRequestProperty("sec-fetch-dest", "document")
            urlConnection.setRequestProperty("sec-fetch-mode", "navigate")
            urlConnection.setRequestProperty("sec-fetch-site", "same-origin")
            urlConnection.setRequestProperty("sec-fetch-user", "?1")
            urlConnection.setRequestProperty("upgrade-insecure-requests", "1")
            urlConnection.setRequestProperty(
                "user-agent",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
            )
            urlConnection.setRequestProperty("Connection", "keep-alive")
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            urlConnection.setRequestProperty("charset", "utf-8")

            if (postfields != "") {
                urlConnection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )
                urlConnection.requestMethod = "POST"
                Log.i("ayc", " post fields length: " + postfields.length.toString())
                urlConnection.setRequestProperty("Content-Length", postfields.length.toString())

                // Send post request
                val outputStreamWriter = OutputStreamWriter(urlConnection.outputStream)
                outputStreamWriter.write(postfields)
                Log.i("ayc", " post data: " + postfields)
                outputStreamWriter.flush()
            }

            // Check if the connection is successful
            val responseCode = urlConnection.responseCode
            if (ayccm != null) {
                ayccm.addCookies(urlConnection.getHeaderFields().get("Set-Cookie"))
            };
            val response = urlConnection.inputStream.bufferedReader().use { it.readText() }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                if(debug) {
                    Log.i("ayc", response)
                }
                return response
            } else {
                Log.e("HTTPURLCONNECTION_ERROR", responseCode.toString())
                Log.i("ayc", " Failed to open the door")
                return ""
            }

        } catch (e: Exception) {
            Log.i("ayc", e.toString())
            return ""
        }
    }

    fun OpenDoor(id: String) {
        try {
            Log.i("ayc", " Open door")
            var opendoor = this.request("https://ashtangayoga.ie/json/?a=open_door&id=" + id)

            if (opendoor.indexOf("successful") > -1) {
                Log.i("ayc", opendoor)
            } else {
                Log.i("ayc", " Failed to open the door")
            }
        } catch (e: Exception) {
            Log.i("ayc", e.toString())
        }
    }

    fun IsDoorAvailable(): Int {
        if(!::bookings.isInitialized) {
            return -1;
        }
        Log.v("ayc-classes", "bookings:" + bookings.length());

        for (t in 1..bookings.length()) {
            try {
                var booking = bookings.getJSONObject(t);

                var cdate = booking.getString("date");
                var ctime = booking.getString("start_time");
                var classDate = SimpleDateFormat("yyyy-MM-dd HH:mm").parse(cdate + " " + ctime);
                var nowDate = Date();

                var doorArmedBeforeSeconds =
                    Integer.parseInt(booking.getString("doorArmedBeforeMins")) * 60;
                var doorDisarmedAfterSeconds =
                    Integer.parseInt(booking.getString("doorDisarmedAfterMins")) * 60;
                var start = ((classDate?.getTime() ?: 1000) / 1000) - doorArmedBeforeSeconds;
                var stop = ((classDate?.getTime() ?: 1000) / 1000) + doorDisarmedAfterSeconds;
                var now = (nowDate.time / 1000);

                if (now > start && now < stop) {
                    //door_button.setVisibility(View.VISIBLE);
                    Log.i("ayc", "available")
                    return booking.getString("class_id").toInt()
                } else {
                    //door_button.setVisibility(View.INVISIBLE);
                    Log.i("ayc", "not available")
                    return -1
                }
            } catch (e: Exception) {
                Log.i("ayc", e.toString())
                return -1
            }
        }
        return -1
    }

    fun GetNextClassMessage(): String {
        var msg = "No booking today"

        if(!::bookings.isInitialized) {
            return msg;
        }

        for (t in bookings.length() -1 downTo 0) {
            try {
                var booking = bookings.getJSONObject(t);

                var cdate = booking.getString("date");
                var ctime = booking.getString("start_time");
                var classDate = SimpleDateFormat("yyyy-MM-dd HH:mm").parse(cdate + " " + ctime);
                var nowDate = Date();

                if(cdate != SimpleDateFormat("yyyy-MM-dd").format(Date())) {
                    continue;
                }

                var doorArmedBeforeSeconds =
                    Integer.parseInt(booking.getString("doorArmedBeforeMins")) * 60;
                var start = ((classDate?.getTime() ?: 1000) / 1000) - doorArmedBeforeSeconds;
                var now = (nowDate.getTime() / 1000);

                if (now < start) {
                    var totalSecs = start -now;
                    var hours = totalSecs / 3600;
                    var minutes = (totalSecs % 3600) / 60;
                    var seconds = totalSecs % 60;
                    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
                }
            } catch (e: Exception) {
                Log.i("ayc", e.toString())
            }
        }

        return msg;
    }

    fun GetBookings() {
        try {
            Log.i("ayc", " Getting bookings")
            if(bookingsLastUpdated < (System.currentTimeMillis()/1000).toInt() - 1800) {
                var json = this.request("https://ashtangayoga.ie/json/?a=get_bookings")
                val reader = JSONObject(json)
                bookings = reader.getJSONArray("result");
                bookingsLastUpdated = (System.currentTimeMillis()/1000).toInt()
                Log.i("ayc", json)
            } else {
                Log.i("ayc", "Not ready to update")
            }
        } catch (e: Exception) {
            Log.i("ayc", e.toString())
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun login() {
        try {
            DoToast("Logging in..")

            var un = MainActivity.pref.getString("username", null)
            var pw = MainActivity.pref.getString("password", null)
            var other =
                "&wp-submit=Log In&redirect_to=https://ashtangayoga.ie/profile/&testcookie=1"
            val postfields = "log=" + (un?.trim() ?: "") + "&pwd=" + (pw?.trim() ?: "") + other

            var rHandler = AycRequest()
            var logininfo = rHandler.request("https://ashtangayoga.ie/wp-login.php", postfields)

            if (logininfo.contains("Incorrect username or password") == false) {
                DoToast("Login Successfull")
                loggedIn = true;
                bookingsLastUpdated = 0;
                GlobalScope.launch {
                    GetBookings()
                }
            } else {
                Log.i("ayc", "login failed")
                DoToast("Login Failed")
            }
        } catch (e: Exception) {
            Log.i("ayc", e.toString())
        }
    }
}