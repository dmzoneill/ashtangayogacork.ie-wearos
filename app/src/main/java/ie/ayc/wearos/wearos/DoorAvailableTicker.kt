package ie.ayc.wearos

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.Thread.sleep

class DoorAvailableTicker : Runnable {
    companion object {
        var backgroundThread: Thread? = null
        lateinit var ui: MainActivity
    }


    fun start() {
        if (backgroundThread == null) {
            backgroundThread = Thread(this)
            backgroundThread!!.start()
        }
    }

    fun stop() {
        if (backgroundThread != null) {
            backgroundThread!!.interrupt()
        }
    }

    override fun run() {
        try {
            Log.i("AYC", "Thread starting.")
            while (!Thread.interrupted()) {
                var ayc = AycRequest.GetInstance()
                if (ayc.IsLoggedIn()) {
                    Log.i("AYC", "checking class ready....")
                    ayc.GetBookings()

                    if (ayc.IsDoorAvailable() != -1) {
                        Handler(Looper.getMainLooper()).post {
                            ui.toggleDoor(true)
                        }
                    } else {
                        ui.toggleDoor(false)
                    }
                }
                sleep(5000)
                Log.i("AYC", "Waiting....")
            }
            Log.i("AYC", "Thread stopping.")
        } catch (ex: InterruptedException) {
            Log.i("AYC", "Thread shutting down as it was requested to stop.")
        } finally {
            backgroundThread = null
        }
    }
}