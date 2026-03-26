package com.numberniceic.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.SoundPool
import android.net.Uri
import android.util.Log
import com.numberniceic.R

object SoundManager {
    private var soundPool: SoundPool? = null
    private var chatId: Int = 0
    private var sendId: Int = 0
    private var cuteId: Int = 0
    private var isLoaded: Boolean = false

    fun init(context: Context) {
        // Disabled per user request
    }

    fun playChatSound(context: Context) {
        // Disabled per user request
    }

    fun playSendSound(context: Context) {
        // Disabled per user request
    }

    fun playCuteSound(context: Context) {
        // Disabled per user request
    }

    fun playSystemNotificationSound(context: Context) {
        // Disabled per user request
    }

    private fun vibrate(context: Context) {
        // Disabled per user request
    }

    private fun playSound(context: Context, soundId: Int, rawRes: Int, tag: String) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val mediaVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        Log.d("SoundManager", "Play $tag Sound Requested. isLoaded: $isLoaded, Media Volume: $mediaVol")

        try {
            if (soundPool == null) {
                init(context)
            }
            
            // 🚀 Try SoundPool first
            if (isLoaded && soundId != 0) {
                val streamId = soundPool?.play(soundId, 0.7f, 0.7f, 1, 0, 1f)
                Log.d("SoundManager", "SoundPool Successfully Played $tag StreamID: $streamId")
                
                if (streamId == 0) {
                    Log.w("SoundManager", "SoundPool failed (streamId 0), triggering MediaPlayer.")
                    triggerMediaPlayerFallback(context, rawRes)
                }
            } else {
                Log.d("SoundManager", "SoundPool not ready. Triggering MediaPlayer.")
                triggerMediaPlayerFallback(context, rawRes)
            }
            
            // 📳 Vibrate
            vibrate(context)
            
        } catch (e: Exception) {
            Log.e("SoundManager", "Error Playing $tag Sound", e)
            triggerMediaPlayerFallback(context, rawRes) // Last ditch effort
        }
    }

    private fun triggerMediaPlayerFallback(context: Context, rawRes: Int) {
        try {
            Log.d("SoundManager", "Starting MediaPlayer for resource $rawRes...")
            val mp = MediaPlayer.create(context.applicationContext, rawRes)
            if (mp != null) {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setVolume(0.7f, 0.7f)
                mp.setOnCompletionListener { 
                    Log.d("SoundManager", "MediaPlayer Playback Finished")
                    it.release() 
                }
                mp.start()
                Log.d("SoundManager", "MediaPlayer Started Successfully")
            } else {
                Log.e("SoundManager", "MediaPlayer.create returned null!")
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "MediaPlayer Failed", e)
        }
    }
}
