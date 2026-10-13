package com.xcontrol.launcher

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var status: TextView
    private lateinit var speaker: TextToSpeech
    private val voiceRequest = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speaker = TextToSpeech(this, this)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(24, 24, 24, 24)
        layout.setBackgroundColor(Color.rgb(10, 20, 45))

        val title = TextView(this)
        title.text = "X-CONTROL"
        title.textSize = 32f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER

        status = TextView(this)
        status.text = "Your phone. Your commands."
        status.textSize = 17f
        status.setTextColor(Color.LTGRAY)
        status.gravity = Gravity.CENTER
        status.setPadding(0, 24, 0, 24)

        val voiceButton = Button(this)
        voiceButton.text = "🎙 SPEAK TO X-CONTROL"
        voiceButton.setOnClickListener { listen() }

        val batteryButton = Button(this)
        batteryButton.text = "CHECK BATTERY"
        batteryButton.setOnClickListener { checkBattery() }

        layout.addView(title)
        layout.addView(status)
        layout.addView(voiceButton)
        layout.addView(batteryButton)

        setContentView(layout)
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            speaker.language = Locale.US
            speaker.speak(
                "X Control is ready.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "welcome"
            )
        }
    }

    private fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        try {
            startActivityForResult(intent, voiceRequest)
        } catch (e: Exception) {
            status.text = "Voice recognition is unavailable."
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == voiceRequest &&
            resultCode == RESULT_OK &&
            data != null
        ) {
            val words = data.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )
            val command = words?.firstOrNull()?.lowercase(Locale.ROOT) ?: return
            handleCommand(command)
        }
    }

    private fun handleCommand(command: String) {
        when {
            command.contains("battery") -> checkBattery()
            command.startsWith("open ") -> openApp(command.removePrefix("open ").trim())
            command.contains("hello") -> respond("Hello! How can I help?")
            else -> respond("Sorry, I don't know that command yet.")
        }
    }

    private fun openApp(name: String) {
        val apps = packageManager.getInstalledApplications(
            PackageManager.GET_META_DATA
        )

        val match = apps.firstOrNull {
            packageManager.getApplicationLabel(it)
                .toString()
                .equals(name, ignoreCase = true)
        }

        val launchIntent = match?.let {
            packageManager.getLaunchIntentForPackage(it.packageName)
        }

        if (launchIntent != null) {
            startActivity(launchIntent)
            respond("Opening $name")
        } else {
            respond("I couldn't find $name")
        }
    }

    private fun checkBattery() {
        val battery = registerReceiver(
            null,
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = battery?.getIntExtra("level", -1) ?: -1
        respond("Your battery is at $level percent.")
    }

    private fun respond(message: String) {
        status.text = message
        if (::speaker.isInitialized) {
            speaker.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "response"
            )
        }
    }

    override fun onDestroy() {
        if (::speaker.isInitialized) {
            speaker.stop()
            speaker.shutdown()
        }
        super.onDestroy()
    }
}
