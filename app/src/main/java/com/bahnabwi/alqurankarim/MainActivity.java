package com.bahnabwi.alqurankarim;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.batoulapps.adhan.Prayer;
import com.batoulapps.adhan.PrayerTimes;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.icu.util.ULocale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private WebView webView;
    private SharedPreferences prefs;
    private HadithRepository hadithRepository;
    private QuranDataManager quranDataManager;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private MediaPlayer splashPlayer;

    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;
    private final float[] lastAccelerometer = new float[3], lastMagnetometer = new float[3];
    private boolean lastAccelerometerSet = false, lastMagnetometerSet = false;
    private float qiblaDegree = 0, currentAzimuth = 0, currentNeedle = 0;

    private final BroadcastReceiver audioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioService.UI_UPDATE_BROADCAST.equals(intent.getAction())) {
                try {
                    String status = intent.getStringExtra("status");
                    int surah = intent.getIntExtra("surah", 1), ayah = intent.getIntExtra("ayah", 1);
                    boolean repeat = intent.getBooleanExtra("repeat", false);
                    String reciter = intent.getStringExtra("reciterName");
                    int progress = intent.getIntExtra("progress", 0);
                    runJS(String.format(Locale.US, "if(window.onAudioUpdate) onAudioUpdate('%s',%d,%d,%b,'%s',%d)", status, surah, ayah, repeat, reciter, progress));
                } catch (Exception e) {
                    Log.e("AudioReceiver", "Error", e);
                }
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        setContentView(R.layout.activity_main);

        try {
            splashPlayer = MediaPlayer.create(this, R.raw.fool);
            if (splashPlayer != null) splashPlayer.start();
        } catch (Exception e) { Log.e("SplashAudio", "Error", e); }

        prefs = getSharedPreferences("quran_prefs", MODE_PRIVATE);
        hadithRepository = new HadithRepository(this);
        quranDataManager = new QuranDataManager(this, prefs, executorService);
        
        webView = findViewById(R.id.quranWebView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        WebBridge bridge = new WebBridge();
        webView.addJavascriptInterface(bridge, "Android");
        webView.addJavascriptInterface(bridge, "AndroidBridge");
        
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        checkLocationPermission();
        checkNotificationPermission();
        hadithRepository.initializeDefaultBooks();
        checkExactAlarmPermission();
        PrayerScheduler.scheduleNextPrayer(this);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            updateLocation();
        }
    }

    private void updateLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc != null) {
                prefs.edit().putFloat("last_lat", (float) loc.getLatitude()).putFloat("last_lng", (float) loc.getLongitude()).apply();
                qiblaDegree = (float) calculateQibla(loc.getLatitude(), loc.getLongitude());
                PrayerScheduler.scheduleNextPrayer(this);
            }
        } catch (SecurityException ignored) {}
    }

    private double calculateQibla(double lat, double lon) {
        double kaabaLat = Math.toRadians(21.4225), kaabaLon = Math.toRadians(39.8262);
        double myLat = Math.toRadians(lat), myLon = Math.toRadians(lon);
        double y = Math.sin(kaabaLon - myLon), x = Math.cos(myLat) * Math.tan(kaabaLat) - Math.sin(myLat) * Math.cos(kaabaLon - myLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    public void runJS(String js) {
        runOnUiThread(() -> { if(webView != null) webView.evaluateJavascript(js, null); });
    }

    class WebBridge {
        @JavascriptInterface
        public void showToast(String msg) { 
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show()); 
        }

        @JavascriptInterface
        public void openPrayerTimes() { runJS("if(window.loadPage) loadPage('prayer')"); }

        @JavascriptInterface
        public void openHadithLibrary() { startActivity(new Intent(MainActivity.this, HadithLibraryActivity.class)); }
        
        @JavascriptInterface
        public boolean isBookDownloaded(String bookId) { return hadithRepository.isBookDownloaded(bookId); }

        @JavascriptInterface
        public boolean isBookDownloading(String bookId) { return hadithRepository.isBookDownloading(bookId); }

        @JavascriptInterface
        public int getTotalDownloadedHadiths() { return hadithRepository.getTotalDownloadedHadiths(); }

        @JavascriptInterface
        public void downloadHadithBook(String bookId) {
            BookEntity b = hadithRepository.getBookStatus(bookId);
            if (b != null) hadithRepository.downloadBook(b);
        }

        @JavascriptInterface
        public void deleteHadithBook(String bookId) { hadithRepository.deleteBookData(bookId); }

        @JavascriptInterface
        public void openHadithBookNative(String bookId, String title) {
            Intent intent = new Intent(MainActivity.this, HadithSectionsActivity.class);
            intent.putExtra("book_id", bookId);
            intent.putExtra("book_title", title);
            startActivity(intent);
        }

        @JavascriptInterface
        public void getPrayerTimes(String callback) {
            executorService.execute(() -> {
                try {
                    double lat = prefs.getFloat("last_lat", 30.0444f), lng = prefs.getFloat("last_lng", 31.2357f);
                    PrayerTimes times = PrayerCalculator.getTimes(lat, lng);
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", new Locale("ar"));
                    JSONObject obj = new JSONObject();
                    obj.put("fajr", sdf.format(times.timeForPrayer(Prayer.FAJR)));
                    obj.put("shurooq", sdf.format(times.timeForPrayer(com.batoulapps.adhan.Prayer.SUNRISE)));
                    obj.put("dhuhr", sdf.format(times.timeForPrayer(com.batoulapps.adhan.Prayer.DHUHR)));
                    obj.put("asr", sdf.format(times.timeForPrayer(com.batoulapps.adhan.Prayer.ASR)));
                    obj.put("maghrib", sdf.format(times.timeForPrayer(com.batoulapps.adhan.Prayer.MAGHRIB)));
                    obj.put("isha", sdf.format(times.timeForPrayer(com.batoulapps.adhan.Prayer.ISHA)));
                    long nextTime = PrayerCalculator.getNextPrayerTimeMillis(lat, lng);
                    obj.put("next", PrayerCalculator.getPrayerName(nextTime, lat, lng));
                    obj.put("countdown", (nextTime - System.currentTimeMillis()) / 1000);
                    obj.put("activeKey", PrayerCalculator.getPrayerKey(nextTime, lat, lng));
                    obj.put("city", prefs.getString("last_city_name", "القاهرة، مصر"));
                    runJS("if(window." + callback + ") " + callback + "('" + obj.toString() + "')");
                } catch (Exception e) { Log.e("PrayerJS", "Error", e); }
            });
        }

        @JavascriptInterface
        public void getHijriDate(String callback) {
            executorService.execute(() -> {
                try {
                    android.icu.util.Calendar cal = android.icu.util.Calendar.getInstance(new ULocale("ar@calendar=islamic-umalqura"));
                    String monthName = new android.icu.text.SimpleDateFormat("MMMM", new ULocale("ar@calendar=islamic-umalqura")).format(cal);
                    String hijri = toArabicNumbers(cal.get(android.icu.util.Calendar.DAY_OF_MONTH)) + " " + monthName + " " + toArabicNumbers(cal.get(android.icu.util.Calendar.YEAR)) + " هـ";
                    runJS("if(window." + callback + ") " + callback + "('" + hijri + "')");
                } catch (Exception e) { Log.e("HijriJS", "Error", e); }
            });
        }

        private String toArabicNumbers(int n) {
            String s = String.valueOf(n); StringBuilder sb = new StringBuilder();
            for (char d : s.toCharArray()) sb.append("٠١٢٣٤٥٦٧٨٩".charAt(d - '0'));
            return sb.toString();
        }

        @JavascriptInterface
        public void startQiblaUpdate() {
            runOnUiThread(() -> {
                if (accelerometer != null) sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                if (magnetometer != null) sensorManager.registerListener(MainActivity.this, magnetometer, SensorManager.SENSOR_DELAY_UI);
            });
        }

        @JavascriptInterface
        public void stopQiblaUpdate() { runOnUiThread(() -> sensorManager.unregisterListener(MainActivity.this)); }

        @JavascriptInterface
        public void stopSplashAudio() {
            if (splashPlayer != null) {
                try {
                    if (splashPlayer.isPlaying()) splashPlayer.stop();
                    splashPlayer.release();
                    splashPlayer = null;
                } catch (Exception e) { Log.e("SplashAudio", "Error", e); }
            }
        }

        @JavascriptInterface
        public void getReciters(String callback) {
            executorService.execute(() -> {
                try {
                    String[] files = getAssets().list("reciters");
                    JSONArray array = new JSONArray();
                    if (files != null) {
                        for (String file : files) {
                            if (file.endsWith(".json") && !file.equals("reciters_list.json")) {
                                try (InputStream is = getAssets().open("reciters/" + file);
                                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                                    StringBuilder sb = new StringBuilder();
                                    String line; while ((line = reader.readLine()) != null) sb.append(line);
                                    array.put(new JSONObject(sb.toString()));
                                }
                            }
                        }
                    }
                    String b64 = Base64.encodeToString(array.toString().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                    runJS("if(window." + callback + ") " + callback + "('" + b64 + "')");
                } catch (Exception e) { Log.e("RecitersJS", "Error", e); }
            });
        }

        @JavascriptInterface
        public void setReciter(String id, String name, String type, String server) {
            prefs.edit().putString("lastReciter", id).putString("lastReciterName", name)
                .putString("reciter_type_" + id, type).putString("reciter_server_" + id, server).apply();
            Intent intent = new Intent(MainActivity.this, AudioService.class);
            intent.setAction(AudioService.ACTION_UPDATE_RECITER);
            intent.putExtra("reciter", id); intent.putExtra("reciterName", name);
            intent.putExtra("reciterType", type); intent.putExtra("reciterServer", server);
            startService(intent);
        }

        @JavascriptInterface
        public void fetchQuranData(String url, String callback) { quranDataManager.fetchQuranData(url, callback); }

        @JavascriptInterface
        public void fetchTafsir(int surah, int ayah, String callback) { quranDataManager.fetchTafsir(surah, ayah, callback); }

        @JavascriptInterface
        public void playAyah(int surah, int ayah) {
            Intent intent = new Intent(MainActivity.this, AudioService.class);
            intent.setAction(AudioService.ACTION_PLAY);
            intent.putExtra("surah", surah); intent.putExtra("ayah", ayah);
            startService(intent);
        }

        @JavascriptInterface
        public void togglePlay() {
            Intent intent = new Intent(MainActivity.this, AudioService.class);
            intent.setAction(AudioService.ACTION_TOGGLE);
            startService(intent);
        }

        @JavascriptInterface
        public void nextAyah() {
            Intent intent = new Intent(MainActivity.this, AudioService.class);
            intent.setAction(AudioService.ACTION_NEXT);
            startService(intent);
        }

        @JavascriptInterface
        public void prevAyah() {
            Intent intent = new Intent(MainActivity.this, AudioService.class);
            intent.setAction(AudioService.ACTION_PREV);
            startService(intent);
        }

        @JavascriptInterface
        public void requestAudioStatus() {
            Intent intent = new Intent(MainActivity.this, AudioService.class);
            intent.setAction(AudioService.ACTION_REQUEST_STATUS);
            startService(intent);
        }

        @JavascriptInterface
        public void toggleRepeat() {
            boolean current = prefs.getBoolean("audio_repeat", false);
            boolean newValue = !current;
            prefs.edit().putBoolean("audio_repeat", newValue).apply();
            Intent intent = new Intent(MainActivity.this, AudioService.class);
            intent.setAction(AudioService.ACTION_SET_REPEAT);
            intent.putExtra("enabled", newValue);
            startService(intent);
        }

        @JavascriptInterface
        public void downloadSurah(int surahId, String reciterId) { quranDataManager.downloadSurah(surahId, reciterId); }

        @JavascriptInterface
        public void deleteSurah(int surahId) { quranDataManager.deleteSurah(surahId); }

        @JavascriptInterface
        public void getDownloadedSurahs() {
            executorService.execute(() -> {
                List<Integer> ids = quranDataManager.getDownloadedSurahIds();
                String b64 = Base64.encodeToString(new JSONArray(ids).toString().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                runJS("if(window.onDownloadedSurahsReceived) onDownloadedSurahsReceived('" + b64 + "')");
            });
        }

        @JavascriptInterface
        public void getDetailedDownloads() {
            executorService.execute(() -> {
                String json = quranDataManager.getAllDownloadsDetailedJson();
                String b64 = Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                runJS("if(window.onDetailedDownloadsReceived) onDetailedDownloadsReceived('" + b64 + "')");
            });
        }

        @JavascriptInterface
        public void getLastRead() {
            int surah = prefs.getInt("lastSurah", 1), ayah = prefs.getInt("lastAyah", 1);
            try {
                JSONObject obj = new JSONObject(); obj.put("surah", surah); obj.put("ayah", ayah);
                String b64 = Base64.encodeToString(obj.toString().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                runJS("if(window.onLastRead) onLastRead('" + b64 + "')");
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void vibrate(int duration) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(duration);
                }
            }
        }

        @JavascriptInterface
        public void onSettingChanged(String key, Object value) {
            SharedPreferences.Editor editor = prefs.edit();
            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Number) {
                editor.putInt(key, ((Number) value).intValue());
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            }
            editor.commit(); // استخدام commit لضمان التزامن الفوري مع نظام التنبيهات

            if (key.startsWith("salat_nabi_")) {
                boolean enabled = prefs.getBoolean("salat_nabi_notify", false);
                int interval = prefs.getInt("salat_nabi_interval", 15);
                
                if (key.equals("salat_nabi_notify")) enabled = (Boolean) value;
                if (key.equals("salat_nabi_interval")) interval = ((Number) value).intValue();

                if (enabled) {
                    SalatReminderReceiver.scheduleReminder(MainActivity.this, interval);
                } else {
                    SalatReminderReceiver.cancelReminder(MainActivity.this);
                }
            }
        }

        @JavascriptInterface
        public void playSalatSoundSample(String soundId) {
            try {
                int resId = getResources().getIdentifier(soundId, "raw", getPackageName());
                if (resId != 0) {
                    MediaPlayer mp = MediaPlayer.create(MainActivity.this, resId);
                    if (mp != null) {
                        mp.setOnCompletionListener(MediaPlayer::release);
                        mp.start();
                    }
                }
            } catch (Exception e) { Log.e("SalatSample", "Error", e); }
        }

        @JavascriptInterface
        public void shareText(String text) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(intent, "مشاركة ورد الصلاة على النبي"));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) { lowPass(event.values, lastAccelerometer); lastAccelerometerSet = true; }
        else if (event.sensor == magnetometer) { lowPass(event.values, lastMagnetometer); lastMagnetometerSet = true; }
        if (lastAccelerometerSet && lastMagnetometerSet) {
            float[] R = new float[9], orientation = new float[3];
            if (SensorManager.getRotationMatrix(R, null, lastAccelerometer, lastMagnetometer)) {
                SensorManager.getOrientation(R, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]);
                GeomagneticField geoField = new GeomagneticField((float) prefs.getFloat("last_lat", 30.0444f), (float) prefs.getFloat("last_lng", 31.2357f), 0, System.currentTimeMillis());
                azimuth += geoField.getDeclination();
                azimuth = (azimuth + 360) % 360;
                float needleRotation = qiblaDegree - azimuth;
                currentAzimuth = smoothAngle(currentAzimuth, azimuth);
                currentNeedle = smoothAngle(currentNeedle, needleRotation);
                runJS("if(window.onQiblaUpdate) onQiblaUpdate(" + currentAzimuth + "," + currentNeedle + ")");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void lowPass(float[] input, float[] output) {
        float alpha = 0.05f;
        for (int i = 0; i < input.length; i++) output[i] = output[i] + alpha * (input[i] - output[i]);
    }

    private float smoothAngle(float oldA, float newA) {
        float diff = newA - oldA;
        while (diff < -180) diff += 360;
        while (diff > 180) diff -= 360;
        return oldA + diff * 0.1f;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(AudioService.UI_UPDATE_BROADCAST);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(audioReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else registerReceiver(audioReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(audioReceiver);
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (splashPlayer != null) { splashPlayer.release(); splashPlayer = null; }
    }
}
