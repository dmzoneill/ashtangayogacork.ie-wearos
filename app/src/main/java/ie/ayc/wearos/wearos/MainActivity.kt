package ie.ayc.wearos

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import ie.ayc.wearos.R
import ie.ayc.wearos.databinding.ActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    companion object {
        lateinit var instance: MainActivity
        lateinit var pref: SharedPreferences
        var requester: AycRequest = AycRequest.GetInstance()
    }

    private lateinit var ticker: DoorAvailableTicker

    private lateinit var binding: ActivityMainBinding
    private lateinit var landingView: FrameLayout
    private lateinit var mainView: LinearLayout
    private lateinit var settingsView: LinearLayout

    private lateinit var doorImage: ImageView
    private lateinit var settingsImage: ImageView
    private lateinit var ganeshImage: ImageView
    private lateinit var backSaveImage: ImageView
    private lateinit var backImage: ImageView

    private lateinit var passwordInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var nextClass: TextView
    var flipFlop = true;

    fun DoToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(instance, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleView(Top: Int, Bottom: Int, Options: Int) {
        this.landingView = findViewById(R.id.landingView)
        this.mainView = findViewById(R.id.mainView)
        this.settingsView = findViewById(R.id.settingsView)
        this.mainView.visibility = Bottom
        this.landingView.visibility = Top
        this.settingsView.visibility = Options

        if(Top == View.VISIBLE) {
            ticker.stop()
        } else {
            ticker.start()
        }
    }

    fun loadFormData() {
        var username = pref.getString("username", null)
        var password = pref.getString("password", null)
        if (username != null) {
            this.usernameInput.setText(username.toString())
        }
        if (password != null) {
            this.passwordInput.setText(password.toString())
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("ayc", "fun onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.ganeshImage = findViewById(R.id.ganeshImage)
        this.doorImage = findViewById(R.id.doorImage)
        this.settingsImage = findViewById(R.id.settingsImage)
        this.backSaveImage = findViewById(R.id.backSaveImage)
        this.backImage = findViewById(R.id.backImage)
        this.passwordInput = findViewById(R.id.passwordInput)
        this.usernameInput = findViewById(R.id.usernameInput)
        this.nextClass = findViewById(R.id.nextclass)
        this.passwordInput.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        pref = applicationContext.getSharedPreferences("MyPref", 0)
        instance = this
        ticker = DoorAvailableTicker()
        DoorAvailableTicker.ui = this;
        this.loadFormData()

        GlobalScope.launch {
            requester.login()
            requester.GetBookings()
        }

        this.ganeshImage.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick(v: View?) {
                //DoToast("Double click")
                instance.toggleView(View.GONE, View.VISIBLE, View.GONE)
            }
        })

        this.settingsImage.setOnClickListener {
            Log.i("ayc", "view settings")
            instance.toggleView(View.GONE, View.GONE, View.VISIBLE)
        }

        this.backSaveImage.setOnClickListener {
            Log.i("ayc", "saving and return")
            this.passwordInput.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            val editor: SharedPreferences.Editor = pref.edit()
            editor.putString("username", this.usernameInput.text.toString())
            editor.putString("password", this.passwordInput.text.toString())
            editor.commit()
            instance.toggleView(View.GONE, View.VISIBLE, View.GONE)

            GlobalScope.launch {
                requester.login()
            }
        }

        this.backImage.setOnClickListener {
            Log.i("ayc", "saving and return")
            instance.toggleView(View.VISIBLE, View.GONE, View.GONE)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun toggleDoor(enabled: Boolean) {
        runOnUiThread {

            if (enabled) {
                this.doorImage.setImageResource(R.mipmap.door_green)
                this.doorImage.setOnClickListener(object : DoubleClickListener() {
                    override fun onDoubleClick(v: View?) {
                        Log.i("ayc", "door onDoubleClick")

                        var ayc = AycRequest.GetInstance()
                        if (ayc.IsLoggedIn()) {
                            var avail = ayc.IsDoorAvailable()
                            if (avail != -1) {
                                Handler(Looper.getMainLooper()).post {
                                    GlobalScope.launch {
                                        DoToast("Opening the door..")
                                        requester.OpenDoor(avail.toString())
                                    }
                                }
                            }
                        }
                    }
                })
            } else {
                if (flipFlop) {
                    this.doorImage.visibility = View.VISIBLE
                    this.nextClass.visibility = View.GONE
                    this.doorImage.setImageResource(R.mipmap.door_red)
                    this.doorImage.setOnClickListener {
                        Log.i("ayc", "Door not available")
                    }
                } else {
                    this.doorImage.visibility = View.GONE
                    this.nextClass.visibility = View.VISIBLE
                    this.nextClass.text = AycRequest.GetInstance().GetNextClassMessage();
                }
                flipFlop = !flipFlop
            }
        }
    }
}