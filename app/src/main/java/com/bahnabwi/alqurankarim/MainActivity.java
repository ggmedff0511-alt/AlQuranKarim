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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
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
        checkBatteryOptimizations(); // جراحة: فحص تحسين البطارية لضمان عمل الأذان في الخلفية
        PrayerScheduler.scheduleNextPrayer(this);
    }

    /**
     * جراحة: طلب استثناء التطبيق من تحسين البطارية لضمان أن الأذان يعمل دائماً حتى لو كان التطبيق مغلقاً.
     */
    private void checkBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    @SuppressLint("BatteryLife")
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("BatteryOpt", "Could not request battery optimization exemption", e);
                }
            }
        }
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
        public void playSurah(int surahNum, String reciter) {
            Intent intent = new Intent(MainActivity.this, AudioService.class);
            intent.setAction("PLAY_SURAH");
            intent.putExtra("surah", surahNum);
            intent.putExtra("reciter", reciter);
            ContextCompat.startForegroundService(MainActivity.this, intent);
        }

        @JavascriptInterface
        public void toggleAudio() {
            Intent intent = new Intent(MainActivity.this, AudioService.class);
            intent.setAction("TOGGLE_PLAY");
            startService(intent);
        }

        @JavascriptInterface
        public void stopAudio() {
            Intent intent = new Intent(MainActivity.this, AudioService.class);
            intent.setAction("STOP_AUDIO");
            startService(intent);
        }

        @JavascriptInterface
        public void setRepeat(boolean repeat) {
            Intent intent = new Intent(MainActivity.this, AudioService.class);
            intent.setAction("SET_REPEAT");
            intent.putExtra("repeat", repeat);
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        if (magnetometer != null) sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        IntentFilter filter = new IntentFilter(AudioService.UI_UPDATE_BROADCAST);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(audioReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(audioReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        unregisterReceiver(audioReceiver);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            lastAccelerometerSet = true;
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            lastMagnetometerSet = true;
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            float[] R = new float[9], orientation = new float[3];
            if (SensorManager.getRotationMatrix(R, null, lastAccelerometer, lastMagnetometer)) {
                SensorManager.getOrientation(R, orientation);
                float azimuthInRadians = orientation[0];
                float azimuthInDegrees = (float) Math.toDegrees(azimuthInRadians);
                
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                try {
                    Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (loc != null) {
                        GeomagneticField geoField = new GeomagneticField((float) loc.getLatitude(), (float) loc.getLongitude(), (float) loc.getAltitude(), System.currentTimeMillis());
                        azimuthInDegrees += geoField.getDeclination();
                    }
                } catch (SecurityException ignored) {}

                azimuthInDegrees = (azimuthInDegrees + 360) % 360;
                float needleRotation = qiblaDegree - azimuthInDegrees;
                runJS(String.format(Locale.US, "if(window.onQiblaUpdate) onQiblaUpdate(%.2f, %.2f)", azimuthInDegrees, needleRotation));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onDestroy() {
        executorService.shutdown();
        if (splashPlayer != null) {
            splashPlayer.release();
            splashPlayer = null;
        }
        super.onDestroy();
    }
}
