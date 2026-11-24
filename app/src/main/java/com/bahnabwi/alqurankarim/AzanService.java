package com.bahnabwi.alqurankarim;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * خدمة تشغيل الأذان الحقيقية - المصحف الكوني
 * تعليق جراحي: تم إضافة نظام AudioFocus لضمان انفراد الأذان بالصوت وإيقاف أي وسائط أخرى تلقائياً.
 */
public class AzanService extends Service implements AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = "AzanService";
    private static final String CHANNEL_ID = "AzanServiceChannel_v6";
    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlQuranKarim:AzanWakeLock");
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(10 * 60 * 1000L);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || "STOP_AZAN".equals(intent.getAction())) {
            stopAzan();
            stopSelf();
            return START_NOT_STICKY;
        }

        String prayerName = intent.getStringExtra("prayerName");
        if (prayerName == null) prayerName = "الصلاة";

        createNotificationChannel();

        Intent activityIntent = new Intent(this, AzanActivity.class);
        activityIntent.putExtra("prayerName", prayerName);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullScreenPI = PendingIntent.getActivity(this, 0, activityIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, AzanService.class);
        stopIntent.setAction("STOP_AZAN");
        PendingIntent stopPI = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("حان الآن موعد أذان " + prayerName)
                .setContentText("الله أكبر، الله أكبر.. حان وقت الصلاة")
                .setSmallIcon(R.drawable.ic_clock)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPI, true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف الأذان", stopPI)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(2001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(2001, notification);
        }

        requestAudioFocusAndPlay();

        return START_NOT_STICKY;
    }

    private void requestAudioFocusAndPlay() {
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            result = audioManager.requestAudioFocus(focusRequest);
        } else {
            result = audioManager.requestAudioFocus(this, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN);
        }

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            playAzan();
        } else {
            // إذا لم نحصل على التركيز، نشغل الأذان على أي حال لأنه تنبيه هام
            playAzan();
        }
    }

    private void playAzan() {
        try {
            stopAzan();
            
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            mediaPlayer = MediaPlayer.create(this, R.raw.azan, aa, 0);
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(1.0f, 1.0f);
                mediaPlayer.setLooping(false);
                mediaPlayer.setOnCompletionListener(mp -> stopSelf());
                mediaPlayer.start();
                Log.d(TAG, "Azan started successfully");
            } else {
                stopSelf();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing azan", e);
            stopSelf();
        }
    }

    private void stopAzan() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        abandonAudioFocus();
    }

    private void abandonAudioFocus() {
        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                audioManager.abandonAudioFocusRequest(focusRequest);
            } else {
                audioManager.abandonAudioFocus(this);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "تنبيهات الأذان الكبرى", NotificationManager.IMPORTANCE_HIGH
            );
            serviceChannel.setSound(null, null);
            serviceChannel.enableVibration(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            stopAzan();
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        stopAzan();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
