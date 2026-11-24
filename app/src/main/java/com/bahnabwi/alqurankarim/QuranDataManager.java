package com.bahnabwi.alqurankarim;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * ميزة منفصلة: إدارة بيانات القرآن الكريم (تم التحديث لدعم التحميل والقراءة بدون نت)
 * وظيفتها: جلب الآيات من السيرفر أو الذاكرة المحلية وإدارة عملية التحميل.
 */
public class QuranDataManager {
    private static final String TAG = "QuranDataManager";
    private final MainActivity activity;
    private final SharedPreferences prefs;
    private final ExecutorService executor;
    private static final int[] AYAH_COUNTS = {0, 7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6};
    
    private final Map<String, String> reciterNamesCache = new HashMap<>();

    public QuranDataManager(MainActivity activity, SharedPreferences prefs, ExecutorService executor) {
        this.activity = activity;
        this.prefs = prefs;
        this.executor = executor;
        loadReciterNamesSync();
    }

    private void loadReciterNamesSync() {
        try {
            String[] files = activity.getAssets().list("reciters");
            if (files != null) {
                for (String fileName : files) {
                    if (!fileName.endsWith(".json")) continue;
                    try (InputStream is = activity.getAssets().open("reciters/" + fileName);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        JSONObject json = new JSONObject(sb.toString());
                        String id = json.optString("id");
                        String name = json.optString("name");
                        if (!id.isEmpty() && !name.isEmpty()) {
                            reciterNamesCache.put(id, name);
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) { Log.e(TAG, "LoadReciters error", e); }
    }

    public void fetchQuranData(final String urlStr, final String callback) {
        executor.execute(() -> {
            try {
                int surahId = -1;
                if (urlStr.contains("chapter_number=")) {
                    String part = urlStr.substring(urlStr.indexOf("chapter_number=") + 15);
                    if (part.contains("&")) part = part.substring(0, part.indexOf("&"));
                    try { surahId = Integer.parseInt(part); } catch (Exception ignored) {}
                }
                String localData = null;
                if (surahId != -1) localData = readLocalText(surahId);
                if (localData != null) {
                    final String b64 = Base64.encodeToString(localData.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                    activity.runOnUiThread(() -> activity.runJS("if(window." + callback + ") " + callback + "('" + b64 + "')"));
                    return;
                }
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) response.append(line).append("\n");
                }
                String result = response.toString();
                if (surahId != -1) result = applyDataPatches(surahId, result);
                if (surahId != -1 && !result.isEmpty()) saveLocalText(surahId, result);
                final String b64 = Base64.encodeToString(result.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                activity.runOnUiThread(() -> activity.runJS("if(window." + callback + ") " + callback + "('" + b64 + "')"));
            } catch (Exception e) {
                activity.runOnUiThread(() -> activity.runJS("if(window." + callback + ") " + callback + "(null)"));
            }
        });
    }

    public void fetchTafsir(int surah, int ayah, String callback) {
        executor.execute(() -> {
            try {
                // استخدام تفسير الميسر (معرف 16) كمثال افتراضي
                String urlStr = String.format(Locale.US, "https://api.quran.com/api/v4/tafsirs/16/by_ayah/%d:%d", surah, ayah);
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }
                
                JSONObject json = new JSONObject(response.toString());
                String tafsirText = "";
                if (json.has("tafsir")) {
                    tafsirText = json.getJSONObject("tafsir").getString("text");
                }
                
                final String b64 = Base64.encodeToString(tafsirText.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                activity.runOnUiThread(() -> activity.runJS("if(window." + callback + ") " + callback + "('" + b64 + "')"));
            } catch (Exception e) {
                Log.e(TAG, "FetchTafsir error", e);
                activity.runOnUiThread(() -> activity.runJS("if(window." + callback + ") " + callback + "(null)"));
            }
        });
    }

    public void downloadSurah(int surahId, String reciterId) {
        executor.execute(() -> {
            try {
                // 1. تحميل النص
                String urlText = "https://api.quran.com/api/v4/quran/verses/uthmani?chapter_number=" + surahId;
                HttpURLConnection connText = (HttpURLConnection) new URL(urlText).openConnection();
                connText.setRequestProperty("User-Agent", "Mozilla/5.0");
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(connText.getInputStream(), StandardCharsets.UTF_8))) {
                    String l; while((l = r.readLine()) != null) sb.append(l);
                }
                String content = applyDataPatches(surahId, sb.toString());
                saveLocalText(surahId, content);

                // 2. تحميل الصوتيات
                String type = prefs.getString("reciter_type_" + reciterId, "ayah");
                String server = prefs.getString("reciter_server_" + reciterId, "");
                
                if ("surah".equals(type) && !server.isEmpty()) {
                    String url = server.replace("https://", "http://");
                    if (!url.endsWith("/")) url += "/";
                    String audioUrl = url + String.format(Locale.US, "%03d.mp3", surahId);
                    downloadFile(audioUrl, getAudioPath(reciterId, surahId, 1, true));
                } else {
                    int count = AYAH_COUNTS[surahId];
                    for (int i = 1; i <= count; i++) {
                        String audioUrl = String.format(Locale.US, "http://www.everyayah.com/data/%s/%03d%03d.mp3", reciterId, surahId, i);
                        try {
                            downloadFile(audioUrl, getAudioPath(reciterId, surahId, i, false));
                        } catch (Exception e) { Log.e(TAG, "Ayah " + i + " skip", e); }
                    }
                }

                // 3. تحديث الواجهة
                updateUI();
                activity.runOnUiThread(() -> activity.runJS("if(window.AndroidBridge) window.AndroidBridge.showToast('تم تحميل سورة بنجاح');"));
            } catch (Exception e) { 
                Log.e(TAG, "Download error", e);
                activity.runOnUiThread(() -> activity.runJS("if(window.AndroidBridge) window.AndroidBridge.showToast('فشل التحميل، تحقق من الاتصال');"));
            }
        });
    }

    public void deleteSurah(int surahId) {
        executor.execute(() -> {
            try {
                // حذف النص
                File textFile = new File(activity.getFilesDir(), "surah_" + surahId + ".json");
                if (textFile.exists()) textFile.delete();

                // حذف الصوتيات
                File base = activity.getExternalFilesDir(null);
                if (base != null) {
                    File audioDir = new File(base, "quran/audio");
                    if (audioDir.exists()) {
                        File[] reciters = audioDir.listFiles();
                        if (reciters != null) {
                            for (File r : reciters) {
                                File sDir = new File(r, String.valueOf(surahId));
                                if (sDir.exists()) deleteRecursive(sDir);
                            }
                        }
                    }
                }
                updateUI();
                activity.runOnUiThread(() -> activity.runJS("if(window.AndroidBridge) window.AndroidBridge.showToast('تم حذف السورة');"));
            } catch (Exception e) { Log.e(TAG, "Delete error", e); }
        });
    }

    private void updateUI() {
        activity.runOnUiThread(() -> {
            List<Integer> ids = getDownloadedSurahIds();
            String idsJson = new JSONArray(ids).toString();
            String idsB64 = Base64.encodeToString(idsJson.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            activity.runJS("if(window.onDownloadedSurahsReceived) onDownloadedSurahsReceived('" + idsB64 + "')");

            String detailedJson = getAllDownloadsDetailedJson();
            String detailedB64 = Base64.encodeToString(detailedJson.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            activity.runJS("if(window.onDetailedDownloadsReceived) onDetailedDownloadsReceived('" + detailedB64 + "')");
        });
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private void downloadFile(String urlStr, File target) throws Exception {
        if (target.exists() && target.length() > 2000) return;
        target.getParentFile().mkdirs();
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(15000);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) throw new Exception("HTTP " + conn.getResponseCode());

            try (InputStream is = conn.getInputStream(); FileOutputStream fos = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192]; int read;
                while ((read = is.read(buffer)) != -1) fos.write(buffer, 0, read);
            }
        } finally { if (conn != null) conn.disconnect(); }
    }

    private File getAudioPath(String reciter, int s, int a, boolean isFullSurah) {
        File base = activity.getExternalFilesDir(null);
        if (base == null) return null;
        if (isFullSurah) return new File(base, "quran/audio/" + reciter + "/" + s + "/1.mp3");
        return new File(base, "quran/audio/" + reciter + "/" + s + "/" + a + ".mp3");
    }

    private void saveLocalText(int id, String data) {
        try {
            File f = new File(activity.getFilesDir(), "surah_" + id + ".json");
            try (FileOutputStream fos = new FileOutputStream(f)) { fos.write(data.getBytes(StandardCharsets.UTF_8)); }
        } catch (Exception ignored) {}
    }

    private String readLocalText(int id) {
        try {
            File f = new File(activity.getFilesDir(), "surah_" + id + ".json");
            if (!f.exists()) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String l; while((l = r.readLine()) != null) sb.append(l);
            }
            return applyDataPatches(id, sb.toString());
        } catch (Exception e) { return null; }
    }

    public List<Integer> getDownloadedSurahIds() {
        List<Integer> ids = new ArrayList<>();
        try {
            File base = activity.getExternalFilesDir(null);
            if (base == null) return ids;
            File audioDir = new File(base, "quran/audio");
            if (!audioDir.exists()) return ids;
            File[] reciterDirs = audioDir.listFiles();
            if (reciterDirs != null) {
                for (File rDir : reciterDirs) {
                    if (!rDir.isDirectory()) continue;
                    File[] surahDirs = rDir.listFiles();
                    if (surahDirs != null) {
                        for (File sDir : surahDirs) {
                            if (sDir.isDirectory()) {
                                File[] mp3s = sDir.listFiles();
                                if (mp3s != null && mp3s.length > 0) {
                                    try {
                                        int id = Integer.parseInt(sDir.getName());
                                        if (!ids.contains(id)) ids.add(id);
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) { Log.e(TAG, "Error checking downloads", e); }
        return ids;
    }

    public String getAllDownloadsDetailedJson() {
        JSONArray root = new JSONArray();
        try {
            File base = activity.getExternalFilesDir(null);
            if (base == null) return "[]";
            File audioDir = new File(base, "quran/audio");
            if (!audioDir.exists()) return "[]";
            File[] reciterDirs = audioDir.listFiles();
            if (reciterDirs == null) return "[]";
            for (File recDir : reciterDirs) {
                if (!recDir.isDirectory()) continue;
                String recId = recDir.getName();
                String recName = reciterNamesCache.get(recId);
                if (recName == null) recName = recId.replace("_", " ");
                
                File[] surahDirs = recDir.listFiles();
                if (surahDirs == null) continue;
                for (File sDir : surahDirs) {
                    if (!sDir.isDirectory()) continue;
                    try {
                        int surahId = Integer.parseInt(sDir.getName());
                        JSONObject obj = new JSONObject();
                        obj.put("surahId", surahId);
                        obj.put("reciterId", recId);
                        obj.put("reciterName", recName);
                        root.put(obj);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) { Log.e(TAG, "Detailed downloads error", e); }
        return root.toString();
    }

    private String applyDataPatches(int surahId, String data) {
        if (data == null) return null;
        if (surahId == 3) {
            data = data.replaceAll("(أُولُواْ?|أُو۟لُوا۟?|أُوْلُواْ?)\\s+(أُولُواْ?|أُو۟لُوا۟?|أُوْلُواْ?)", "أُولُو");
            data = data.replace("أُو۟لُوا۟ ٱلْأَلْبَٰبِ", "أُولُو الْأَلْبَابِ")
                       .replace("أُو۟لُوا۟ الْأَلْبَٰبِ", "أُولُو الْأَلْبَابِ")
                       .replace("أُوْلُواْ ٱلْأَلْبَٰبِ", "أُولُو الْأَلْبَابِ")
                       .replace("أُوْلُواْ الْأَلْبَٰبِ", "أُولُو الْأَلْبَابِ")
                       .replace("الْأَلْبَٰبِ", "الْأَلْبَابِ");
        }
        return data;
    }
}
