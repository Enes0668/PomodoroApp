package com.example.pomodoro

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var textViewTimer: TextView
    private lateinit var buttonStart: Button
    private lateinit var buttonReset: Button
    private lateinit var buttonResetPomodoroCount: Button
    private lateinit var textViewPomodoroCount: TextView
    private lateinit var textViewMode: TextView

    private var timer: CountDownTimer? = null
    private var isRunning = false
    private var timeLeftInMillis: Long = 0

    private var pomodoroCount = 0

    private val pomodoroDuration = 25 * 60 * 1000L    // 25 dakika
    private val shortBreakDuration = 5 * 60 * 1000L   // 5 dakika
    private val longBreakDuration = 15 * 60 * 1000L   // 15 dakika

    private lateinit var mediaPlayer: MediaPlayer

    private enum class TimerMode { POMODORO, SHORT_BREAK, LONG_BREAK }
    private var currentMode = TimerMode.POMODORO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewTimer = findViewById(R.id.textViewTimer)
        buttonStart = findViewById(R.id.buttonStart)
        buttonReset = findViewById(R.id.buttonReset)
        buttonResetPomodoroCount = findViewById(R.id.buttonResetPomodoroCount)
        textViewPomodoroCount = findViewById(R.id.textViewPomodoroCount)
        textViewMode = findViewById(R.id.textViewMode)

        val prefs = getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
        pomodoroCount = prefs.getInt("pomodoro_count", 0)
        updatePomodoroCountText()

        mediaPlayer = MediaPlayer.create(this, R.raw.bell_sound)

        buttonStart.setOnClickListener {
            if (!isRunning) {
                val duration = getCurrentDuration()
                startTimer(timeLeftInMillis.takeIf { it > 0 } ?: duration)
            } else {
                pauseTimer()
            }
        }

        buttonReset.setOnClickListener {
            resetTimer()
        }

        buttonResetPomodoroCount.setOnClickListener {
            pomodoroCount = 0
            savePomodoroCount()
            updatePomodoroCountText()
        }

        resetTimer()
    }

    private fun getCurrentDuration(): Long {
        return when (currentMode) {
            TimerMode.POMODORO -> pomodoroDuration
            TimerMode.SHORT_BREAK -> shortBreakDuration
            TimerMode.LONG_BREAK -> longBreakDuration
        }
    }

    private fun startTimer(duration: Long) {
        timeLeftInMillis = duration

        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText(timeLeftInMillis)
            }

            override fun onFinish() {
                playSound()
                when (currentMode) {
                    TimerMode.POMODORO -> {
                        pomodoroCount++
                        updatePomodoroCountText()
                        savePomodoroCount()
                        currentMode = if (pomodoroCount % 4 == 0) TimerMode.LONG_BREAK else TimerMode.SHORT_BREAK
                    }
                    TimerMode.SHORT_BREAK, TimerMode.LONG_BREAK -> {
                        currentMode = TimerMode.POMODORO
                    }
                }
                resetTimer()
            }
        }.start()

        isRunning = true
        buttonStart.text = "Duraklat"
        updateModeText()
    }

    private fun pauseTimer() {
        timer?.cancel()
        isRunning = false
        buttonStart.text = "Devam Et"
    }

    private fun resetTimer() {
        timer?.cancel()
        isRunning = false
        timeLeftInMillis = getCurrentDuration()
        updateTimerText(timeLeftInMillis)
        buttonStart.text = "Başlat"
        updateModeText()
    }

    private fun updateTimerText(millis: Long) {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        textViewTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updatePomodoroCountText() {
        textViewPomodoroCount.text = "Tamamlanan Pomodoro: $pomodoroCount"
    }

    private fun updateModeText() {
        val modeText = when (currentMode) {
            TimerMode.POMODORO -> "Pomodoro"
            TimerMode.SHORT_BREAK -> "Kısa Mola"
            TimerMode.LONG_BREAK -> "Uzun Mola"
        }
        textViewMode.text = "Mod: $modeText"
    }

    private fun savePomodoroCount() {
        val prefs = getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putInt("pomodoro_count", pomodoroCount)
            apply()
        }
    }

    private fun playSound() {
        mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}
