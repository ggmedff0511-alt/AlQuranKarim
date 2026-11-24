package com.bahnabwi.alqurankarim;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.batoulapps.adhan.Prayer;
import com.batoulapps.adhan.PrayerTimes;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrayerTimesActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "PrayerTimesActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ProgressBar progressBar;
    private TextView tvDate, tvCity;
    
    private TextView tvNextPrayerName, tvCountdown;
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;

    private ImageView ivCompassBase, ivCompassNeedle, ivKaabaIcon;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;
    private float qiblaDegree = 0f;

    private LocationManager locationManager;
    private double latitude = 30.0444; 
    private double longitude = 31.2357;
    private SharedPreferences prefs;
    private Vibrator vibrator;
    private boolean hasVibrated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer_times);

        prefs = getSharedPreferences("quran_prefs", MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        progressBar = findViewById(R.id.pbLoadingPrayer);
        tvDate = findViewById(R.id.tvDate);
        tvCity = findViewById(R.id.tvCity);
        tvNextPrayerName = findViewById(R.id.tvNextPrayerName);
        tvCountdown = findViewById(R.id.tvCountdown);
        ivKaabaIcon = findViewById(R.id.ivKaabaIcon);

        findViewById(R.id.layoutNextPrayer).setOnClickListener(v -> {
            long testTime = System.currentTimeMillis() + 10000;
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, PrayerAlarmReceiver.class);
            intent.setAction(PrayerAlarmReceiver.ACTION_PRAYER_ALARM);
            intent.putExtra("prayerName", "اختبار");
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            
            PendingIntent pi = PendingIntent.getBroadcast(this, 1002, intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    am.setAlarmClock(new AlarmManager.AlarmClockInfo(testTime, pi), pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, testTime, pi);
                }
            }
            Toast.makeText(this, "سيعمل الأذان بعد 10 ثوانٍ.. أغلق التطبيق الآن!", Toast.LENGTH_LONG).show();
        });

        ivCompassBase = findViewById(R.id.ivCompassBase);
        ivCompassNeedle = findViewById(R.id.ivCompassNeedle);
        
        MaterialButton btnBack = findViewById(R.id.btnBackPrayer);
        btnBack.setOnClickListener(v -> finish());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("ar"));
        tvDate.setText(sdf.format(new Date()));

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (location != null) {
                updateLocationData(location.getLatitude(), location.getLongitude());
            }
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location loc) {
                    updateLocationData(loc.getLatitude(), loc.getLongitude());
                }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(@NonNull String provider) {}
                @Override public void onProviderDisabled(@NonNull String provider) {}
            }, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location error", e);
        }
    }

    private void updateLocationData(double lat, double lng) {
        this.latitude = lat;
        this.longitude = lng;
        this.qiblaDegree = (float) calculateQibla(lat, lng);
        prefs.edit().putFloat("last_lat", (float) lat).putFloat("last_lng", (float) lng).apply();
        
        if (prefs.getBoolean("azan_notify", true)) {
            PrayerScheduler.scheduleNextPrayer(this);
        }
        updateCityName(lat, lng);
        displayOfflinePrayerTimes(lat, lng);
        fetchPrayerTimesOnline(lat, lng);
        startCountdownTimer();
    }

    private void startCountdownTimer() {
        if (countdownRunnable != null) countdownHandler.removeCallbacks(countdownRunnable);
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long nextTime = PrayerCalculator.getNextPrayerTimeMillis(latitude, longitude);
                String name = PrayerCalculator.getPrayerName(nextTime, latitude, longitude);
                long diff = nextTime - System.currentTimeMillis();
                if (diff > 0) {
                    long hours = (diff / (1000 * 60 * 60));
                    long mins = (diff / (1000 * 60)) % 60;
                    long secs = (diff / 1000) % 60;
                    tvNextPrayerName.setText(getString(R.string.next_prayer_countdown, name));
                    tvCountdown.setText(String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs));
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    tvCountdown.setText("00:00:00");
                    updateLocationData(latitude, longitude);
                }
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    private void displayOfflinePrayerTimes(double lat, double lng) {
        try {
            PrayerTimes times = PrayerCalculator.getTimes(lat, lng);
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", new Locale("ar"));
            timeFormat.setTimeZone(TimeZone.getDefault());
            setPrayerItem(findViewById(R.id.layoutFajr), getString(R.string.fajr), timeFormat.format(times.timeForPrayer(Prayer.FAJR)));
            setPrayerItem(findViewById(R.id.layoutSunrise), getString(R.string.sunrise), timeFormat.format(times.timeForPrayer(Prayer.SUNRISE)));
            setPrayerItem(findViewById(R.id.layoutDhuhr), getString(R.string.dhuhr), timeFormat.format(times.timeForPrayer(Prayer.DHUHR)));
            setPrayerItem(findViewById(R.id.layoutAsr), getString(R.string.asr), timeFormat.format(times.timeForPrayer(Prayer.ASR)));
            setPrayerItem(findViewById(R.id.layoutMaghrib), getString(R.string.maghrib), timeFormat.format(times.timeForPrayer(Prayer.MAGHRIB)));
            setPrayerItem(findViewById(R.id.layoutIsha), getString(R.string.isha), timeFormat.format(times.timeForPrayer(Prayer.ISHA)));
        } catch (Exception e) {
            Log.e(TAG, "Error calculating offline times", e);
        }
    }

    private double calculateQibla(double lat, double lon) {
        double kaabaLat = Math.toRadians(21.4225), kaabaLon = Math.toRadians(39.8262);
        double myLat = Math.toRadians(lat), myLon = Math.toRadians(lon);
        double y = Math.sin(kaabaLon - myLon), x = Math.cos(myLat) * Math.tan(kaabaLat) - Math.sin(myLat) * Math.cos(kaabaLon - myLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    private void updateCityName(double lat, double lng) {
        executorService.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("ar"));
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String city = addresses.get(0).getLocality(), country = addresses.get(0).getCountryName();
                    runOnUiThread(() -> tvCity.setText(city != null ? city + "، " + country : country));
                }
            } catch (Exception ignored) {}
        });
    }

    private void fetchPrayerTimesOnline(double lat, double lng) {
        executorService.execute(() -> {
            try {
                URL url = new URL("https://api.aladhan.com/v1/timings?latitude=" + lat + "&longitude=" + lng + "&method=5");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                JSONObject timings = new JSONObject(sb.toString()).getJSONObject("data").getJSONObject("timings");
                runOnUiThread(() -> updateUI(timings));
            } catch (Exception ignored) {}
        });
    }

    private void updateUI(JSONObject timings) {
        try {
            setPrayerItem(findViewById(R.id.layoutFajr), getString(R.string.fajr), formatTo12h(timings.getString("Fajr")));
            setPrayerItem(findViewById(R.id.layoutSunrise), getString(R.string.sunrise), formatTo12h(timings.getString("Sunrise")));
            setPrayerItem(findViewById(R.id.layoutDhuhr), getString(R.string.dhuhr), formatTo12h(timings.getString("Dhuhr")));
            setPrayerItem(findViewById(R.id.layoutAsr), getString(R.string.asr), formatTo12h(timings.getString("Asr")));
            setPrayerItem(findViewById(R.id.layoutMaghrib), getString(R.string.maghrib), formatTo12h(timings.getString("Maghrib")));
            setPrayerItem(findViewById(R.id.layoutIsha), getString(R.string.isha), formatTo12h(timings.getString("Isha")));
        } catch (Exception ignored) {}
    }

    private String formatTo12h(String time24) {
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.US);
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", new Locale("ar"));
            Date date = sdf24.parse(time24);
            return date != null ? sdf12.format(date) : time24;
        } catch (Exception e) { return time24; }
    }

    private void setPrayerItem(View view, String name, String time) {
        if (view == null) return;
        TextView tvName = view.findViewById(R.id.tvPrayerName), tvTime = view.findViewById(R.id.tvPrayerTime);
        if (tvName != null) tvName.setText(name);
        if (tvTime != null) tvTime.setText(time);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        if (magnetometer != null) sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        startCountdownTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (countdownRunnable != null) countdownHandler.removeCallbacks(countdownRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) { lowPass(event.values, lastAccelerometer); lastAccelerometerSet = true; }
        else if (event.sensor == magnetometer) { lowPass(event.values, lastMagnetometer); lastMagnetometerSet = true; }
        
        if (lastAccelerometerSet && lastMagnetometerSet) {
            float[] R = new float[9], orientation = new float[3];
            if (SensorManager.getRotationMatrix(R, null, lastAccelerometer, lastMagnetometer)) {
                SensorManager.getOrientation(R, orientation);
                float azimuthInDegrees = (float) Math.toDegrees(orientation[0]);
                GeomagneticField geoField = new GeomagneticField((float) latitude, (float) longitude, 0, System.currentTimeMillis());
                azimuthInDegrees += geoField.getDeclination();
                azimuthInDegrees = (azimuthInDegrees + 360) % 360;

                float baseTarget = -azimuthInDegrees;
                float needleTarget = qiblaDegree - azimuthInDegrees;

                applySmoothRotation(ivCompassBase, baseTarget);
                applySmoothRotation(ivCompassNeedle, needleTarget);

                // جراحة: تفعيل الاهتزاز (Haptic) وإظهار الكعبة عند محاذاة القبلة بدقة (+- 5 درجات)
                checkQiblaAlignment(needleTarget);
            }
        }
    }

    private void checkQiblaAlignment(float needleTarget) {
        float normalizedTarget = (needleTarget + 360) % 360;
        if (normalizedTarget > 355 || normalizedTarget < 5) {
            if (!hasVibrated) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else { vibrator.vibrate(50); }
                hasVibrated = true;
            }
            if (ivKaabaIcon != null) ivKaabaIcon.setVisibility(View.VISIBLE);
        } else {
            hasVibrated = false;
            if (ivKaabaIcon != null) ivKaabaIcon.setVisibility(View.GONE);
        }
    }

    private void lowPass(float[] input, float[] output) {
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + 0.15f * (input[i] - output[i]);
        }
    }

    private void applySmoothRotation(ImageView view, float target) {
        float current = view.getRotation();
        float diff = target - current;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        view.setRotation(current + diff * 0.18f);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override protected void onDestroy() { executorService.shutdown(); super.onDestroy(); }
}
