package com.bahnabwi.alqurankarim;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.util.Log;
import java.util.concurrent.ExecutorService;

public class PrayerManager {
    private final MainActivity activity;
    private final SharedPreferences prefs;
    private final ExecutorService executor;
    private MediaPlayer adhanPlayer;

    public PrayerManager(MainActivity activity, SharedPreferences prefs, ExecutorService executor) {
        this.activity = activity;
        this.prefs = prefs;
        this.executor = executor;
    }

    public void playAdhanNow() {
        executor.execute(() -> {
            try {
                if (adhanPlayer != null) {
                    adhanPlayer.release();
                }
                // Using a default adhan sound from raw resources
                int resId = activity.getResources().getIdentifier("adhan_fajr", "raw", activity.getPackageName());
                if (resId == 0) resId = activity.getResources().getIdentifier("adhan", "raw", activity.getPackageName());
                
                if (resId != 0) {
                    adhanPlayer = MediaPlayer.create(activity, resId);
                    adhanPlayer.start();
                    adhanPlayer.setOnCompletionListener(MediaPlayer::release);
                }
            } catch (Exception e) {
                Log.e("PrayerManager", "Error playing adhan", e);
            }
        });
    }

    public void release() {
        if (adhanPlayer != null) {
            adhanPlayer.release();
            adhanPlayer = null;
        }
    }
}
