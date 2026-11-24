package com.bahnabwi.alqurankarim;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.util.Log;
import java.util.concurrent.ExecutorService;

public class SalatReminderManager {
    private final MainActivity activity;
    private final SharedPreferences prefs;
    private final ExecutorService executor;
    private MediaPlayer previewPlayer;

    public SalatReminderManager(MainActivity activity, SharedPreferences prefs, ExecutorService executor) {
        this.activity = activity;
        this.prefs = prefs;
        this.executor = executor;
    }

    public void toggleReminder(boolean enabled, int interval) {
        prefs.edit()
                .putBoolean("salat_reminder_enabled", enabled)
                .putInt("salat_reminder_interval", interval)
                .apply();

        if (enabled) {
            SalatReminderReceiver.scheduleReminder(activity, interval);
        } else {
            SalatReminderReceiver.cancelReminder(activity);
        }
    }

    public void playSoundPreview(String soundName) {
        executor.execute(() -> {
            try {
                if (previewPlayer != null) {
                    previewPlayer.release();
                }
                int resId = activity.getResources().getIdentifier(soundName, "raw", activity.getPackageName());
                if (resId != 0) {
                    previewPlayer = MediaPlayer.create(activity, resId);
                    if (previewPlayer != null) {
                        previewPlayer.start();
                        previewPlayer.setOnCompletionListener(MediaPlayer::release);
                    }
                }
            } catch (Exception e) {
                Log.e("SalatReminderManager", "Error playing preview", e);
            }
        });
    }
}
