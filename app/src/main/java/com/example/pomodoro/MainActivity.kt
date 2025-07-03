package com.example.pomodoro

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {

    private lateinit var textViewTimer: TextView
    private lateinit var buttonStart: Button
    private lateinit var buttonReset: Button
    private lateinit var textViewPomodoroCount: TextView

    private var timer: CountDownTimer? = null
    private var isRunning = false
    private var timeLeftInMillis: Long = 0

    private var pomodoroCount = 0
    private val pomodorosBeforeLongBreak = 4

    private val pomodoroDuration = 25 * 60 * 1000L  // gerçek süreyi kullanabilirsin
    private val shortBreakDuration = 5 * 60 * 1000L
    private val longBreakDuration = 15 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()

        textViewTimer = findViewById(R.id.textViewTimer)
        buttonStart = findViewById(R.id.buttonStart)
        buttonReset = findViewById(R.id.buttonReset)
        textViewPomodoroCount = findViewById(R.id.textViewPomodoroCount)

        buttonStart.setOnClickListener {
            if (!isRunning) {
                startTimer(timeLeftInMillis.takeIf { it > 0 } ?: pomodoroDuration)
            } else {
                pauseTimer()
            }
        }

        buttonReset.setOnClickListener {
            resetTimer()
        }

        // Başlangıçta timer 25:00 gösterir
        resetTimer()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "pomodoro_channel",
                "Pomodoro Bildirimleri",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Sayaç bildirimleri"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "pomodoro_channel")
            .setSmallIcon(R.drawable.ic_timer) // drawable içine bir simge eklemelisin
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    private fun startTimer(duration: Long) {
        timeLeftInMillis = duration

        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText(timeLeftInMillis)
            }

            override fun onFinish() {
                isRunning = false
                buttonStart.text = "Başlat"
                sendNotification("Zaman doldu!", "Mola zamanı 🍵")

                if (duration == pomodoroDuration) {
                    // Pomodoro bitti
                    pomodoroCount++
                    textViewPomodoroCount.text = "Tamamlanan Pomodoro: $pomodoroCount"

                    if (pomodoroCount % pomodorosBeforeLongBreak == 0) {
                        // Uzun mola başlat
                        startTimer(longBreakDuration)
                        sendNotification("Uzun mola başladı", "15 dakika dinlen")
                    } else {
                        // Kısa mola başlat
                        startTimer(shortBreakDuration)
                        sendNotification("Kısa mola başladı", "5 dakika dinlen")
                    }
                } else {
                    // Mola bitti, yeni pomodoro başlat
                    startTimer(pomodoroDuration)
                    sendNotification("Çalışma zamanı", "25 dakika çalış")
                }
            }
        }.start()

        isRunning = true
        buttonStart.text = "Duraklat"
    }

    private fun pauseTimer() {
        timer?.cancel()
        isRunning = false
        buttonStart.text = "Devam Et"
    }

    private fun resetTimer() {
        timer?.cancel()
        isRunning = false
        timeLeftInMillis = pomodoroDuration
        updateTimerText(timeLeftInMillis)
        pomodoroCount = 0
        textViewPomodoroCount.text = "Tamamlanan Pomodoro: $pomodoroCount"
        buttonStart.text = "Başlat"
    }

    private fun updateTimerText(millis: Long) {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        val formatted = String.format("%02d:%02d", minutes, seconds)
        textViewTimer.text = formatted
    }
}
