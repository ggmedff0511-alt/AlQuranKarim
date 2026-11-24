package com.bahnabwi.alqurankarim;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import java.io.File;
import java.util.Locale;

/**
 * AudioService: النسخة الاحترافية لضمان عمل كافة المشايخ حقيقياً
 * تم التحديث لاستخدام HTTPS ودعم السيرفرات المباشرة والمحلية بكفاءة عالية.
 */
public class AudioService extends Service implements AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = "AudioService";
    private static final String CHANNEL_ID = "QuranAudioChannel_v2";
    public static final String UI_UPDATE_BROADCAST = "com.bahnabwi.alqurankarim.UI_UPDATE";
    public static final String ACTION_PLAY = "com.bahnabwi.alqurankarim.PLAY";
    public static final String ACTION_TOGGLE = "com.bahnabwi.alqurankarim.TOGGLE";
    public static final String ACTION_NEXT = "com.bahnabwi.alqurankarim.NEXT";
    public static final String ACTION_PREV = "com.bahnabwi.alqurankarim.PREV";
    public static final String ACTION_STOP = "com.bahnabwi.alqurankarim.STOP";
    public static final String ACTION_REQUEST_STATUS = "com.bahnabwi.alqurankarim.REQUEST_STATUS";
    public static final String ACTION_UPDATE_RECITER = "com.bahnabwi.alqurankarim.UPDATE_RECITER";
    public static final String ACTION_SET_REPEAT = "com.bahnabwi.alqurankarim.SET_REPEAT";
    public static final String ACTION_SET_SLEEP_TIMER = "com.bahnabwi.alqurankarim.SET_SLEEP_TIMER";

    private MediaPlayer mPlayer; 
    private boolean isRepeatEnabled = false;
    private PowerManager.WakeLock wakeLock;
    private SharedPreferences prefs;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPlayer != null && mPlayer.isPlaying()) {
                if ("surah".equals(currentReciterType)) estimateAyahFromPosition();
                else notifyUI("PLAYING");
                progressHandler.postDelayed(this, 300); 
            }
        }
    };

    private String currentReciter = "Maher_AlMuaiqly_64kbps", currentReciterName = "ماهر المعيقلي", currentReciterType = "ayah", currentReciterServer = "";
    private int currentSurah = 1, currentAyah = 1;
    private static final int[] AYAH_COUNTS = {0, 7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6};

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("quran_prefs", MODE_PRIVATE);
        isRepeatEnabled = prefs.getBoolean("audio_repeat", false);
        currentSurah = prefs.getInt("lastSurah", 1);
        currentAyah = prefs.getInt("lastAyah", 1);
        currentReciter = prefs.getString("lastReciter", "Maher_AlMuaiqly_64kbps");
        currentReciterName = prefs.getString("lastReciterName", "ماهر المعيقلي");
        currentReciterType = prefs.getString("reciter_type_" + currentReciter, "ayah");
        currentReciterServer = prefs.getString("reciter_server_" + currentReciter, "");

        createNotificationChannel();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlQuranKarim:AudioWakeLock");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY: 
                    currentSurah = intent.getIntExtra("surah", currentSurah);
                    currentAyah = intent.getIntExtra("ayah", currentAyah);
                    prefs.edit().putInt("lastSurah", currentSurah).putInt("lastAyah", currentAyah).apply();
                    playCurrent(); break;
                case ACTION_TOGGLE: 
                    if (mPlayer != null) {
                        if (mPlayer.isPlaying()) pauseAudio(); else resumeAudio();
                    } else { playCurrent(); }
                    break;
                case ACTION_NEXT: switchNext(); break;
                case ACTION_PREV: playPrev(); break;
                case ACTION_STOP: stopSelf(); break;
                case ACTION_REQUEST_STATUS: notifyUI(mPlayer != null && mPlayer.isPlaying() ? "PLAYING" : "PAUSED"); break;
                case ACTION_UPDATE_RECITER:
                    currentReciter = intent.getStringExtra("reciter");
                    currentReciterName = intent.getStringExtra("reciterName");
                    currentReciterType = intent.getStringExtra("reciterType");
                    currentReciterServer = intent.getStringExtra("reciterServer");
                    if (mPlayer != null && mPlayer.isPlaying()) playCurrent();
                    break;
                case ACTION_SET_REPEAT:
                    isRepeatEnabled = intent.getBooleanExtra("enabled", isRepeatEnabled);
                    break;
                case ACTION_SET_SLEEP_TIMER:
                    int m = intent.getIntExtra("minutes", 0);
                    if (m > 0) progressHandler.postDelayed(this::stopSelf, m * 60000L);
                    break;
            }
        }
        return START_STICKY;
    }

    private void playCurrent() {
        releasePlayers();
        notifyUI("BUFFERING");
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
            
            String url;
            File localFile = getLocalAudioFile();
            
            // تعليق جراحي: الأولوية للملف المحلي لضمان العمل بدون نت وسرعة الاستجابة
            if (localFile != null && localFile.exists() && localFile.length() > 5000) {
                url = localFile.getAbsolutePath();
                Log.d(TAG, "Playing local file: " + url);
            } else if (currentReciterServer != null && !currentReciterServer.isEmpty()) {
                String fileName = "ayah".equals(currentReciterType) ? 
                    String.format(Locale.US, "%03d%03d.mp3", currentSurah, currentAyah) :
                    String.format(Locale.US, "%03d.mp3", currentSurah);
                // تأكيد استخدام HTTPS وتصحيح الرابط
                url = currentReciterServer.replace("http://", "https://");
                url = url + (url.endsWith("/") ? "" : "/") + fileName;
            } else {
                // استخدام السيرفر العالمي EveryAyah مع دعم HTTPS
                url = String.format(Locale.US, "https://everyayah.com/data/%s/%03d%03d.mp3", currentReciter, currentSurah, currentAyah);
            }

            mPlayer.setDataSource(url);
            mPlayer.setOnPreparedListener(mp -> {
                if ("surah".equals(currentReciterType) && currentAyah > 1) {
                    int pos = (int)(((float)(currentAyah-1)/AYAH_COUNTS[currentSurah]) * mp.getDuration());
                    mp.seekTo(pos);
                }
                mp.start();
                if (wakeLock != null && !wakeLock.isHeld()) wakeLock.acquire(3600000L);
                notifyUI("PLAYING");
                progressHandler.post(progressRunnable);
                showNotification(true);
            });
            mPlayer.setOnCompletionListener(mp -> {
                if (isRepeatEnabled) playCurrent();
                else switchNext();
            });
            mPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Media Error for " + currentReciter + ": " + what + " / " + extra);
                // في حال فشل السيرفر الخاص، نحاول السيرفر العالمي كخيار احتياطي أخير
                if (currentReciterServer != null && !currentReciterServer.isEmpty()) {
                    currentReciterServer = ""; // تصفير السيرفر مؤقتاً للمحاولة التالية
                    playCurrent();
                } else {
                    switchNext();
                }
                return true;
            });
            mPlayer.prepareAsync();
        } catch (Exception e) { 
            Log.e(TAG, "Critical Play error", e);
            switchNext(); 
        }
    }

    private File getLocalAudioFile() {
        File base = getExternalFilesDir(null);
        if (base == null) return null;
        String path = "surah".equals(currentReciterType) ?
            String.format(Locale.US, "quran/audio/%s/%d/1.mp3", currentReciter, currentSurah) :
            String.format(Locale.US, "quran/audio/%s/%d/%d.mp3", currentReciter, currentSurah, currentAyah);
        return new File(base, path);
    }

    private void estimateAyahFromPosition() {
        if (mPlayer == null || mPlayer.getDuration() <= 0) return;
        int count = AYAH_COUNTS[currentSurah];
        int estimated = (int)(((float)mPlayer.getCurrentPosition()/mPlayer.getDuration()) * count) + 1;
        if (estimated > count) estimated = count;
        if (estimated != currentAyah) {
            currentAyah = estimated;
            prefs.edit().putInt("lastAyah", currentAyah).apply();
        }
        notifyUI("PLAYING");
    }

    private void switchNext() {
        if ("surah".equals(currentReciterType)) { currentSurah++; currentAyah = 1; }
        else { currentAyah++; if (currentAyah > AYAH_COUNTS[currentSurah]) { currentAyah = 1; currentSurah++; } }
        if (currentSurah > 114) { stopSelf(); return; }
        prefs.edit().putInt("lastSurah", currentSurah).putInt("lastAyah", currentAyah).apply();
        playCurrent();
    }

    private void playPrev() {
        currentAyah--; if (currentAyah < 1) { 
            currentSurah--; if (currentSurah < 1) currentSurah = 1; 
            currentAyah = AYAH_COUNTS[currentSurah]; 
        }
        prefs.edit().putInt("lastSurah", currentSurah).putInt("lastAyah", currentAyah).apply();
        playCurrent();
    }

    private void pauseAudio() { if (mPlayer != null) { mPlayer.pause(); notifyUI("PAUSED"); showNotification(false); } }
    
    private void resumeAudio() { 
        if (mPlayer != null) { 
            mPlayer.start(); 
            notifyUI("PLAYING"); 
            showNotification(true); 
            progressHandler.post(progressRunnable);
        } else { playCurrent(); }
    }
    
    private void releasePlayers() { 
        if (mPlayer != null) { 
            try { mPlayer.stop(); mPlayer.release(); } catch(Exception ignored) {} 
            mPlayer = null; 
        } 
        progressHandler.removeCallbacks(progressRunnable); 
    }

    private void notifyUI(String status) {
        Intent intent = new Intent(UI_UPDATE_BROADCAST);
        intent.putExtra("status", status); 
        intent.putExtra("surah", currentSurah); 
        intent.putExtra("ayah", currentAyah);
        intent.putExtra("repeat", isRepeatEnabled);
        intent.putExtra("reciterName", currentReciterName);
        if (mPlayer != null && mPlayer.getDuration() > 0) {
            float progress = (float) mPlayer.getCurrentPosition() / mPlayer.getDuration();
            intent.putExtra("progress", progress);
        }
        sendBroadcast(intent);
    }

    private void showNotification(boolean isPlaying) {
        String surahName = "سورة " + currentSurah;
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(surahName + " - آية " + currentAyah)
            .setContentText(currentReciterName)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setStyle(new MediaStyle())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .build();
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Quran Audio", NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override public void onAudioFocusChange(int f) {
        if (f == AudioManager.AUDIOFOCUS_LOSS || f == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            pauseAudio();
        }
    }
    
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
    
    @Override public void onDestroy() { 
        releasePlayers(); 
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); 
        super.onDestroy(); 
    }
}
