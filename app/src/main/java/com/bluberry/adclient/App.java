package com.bluberry.adclient;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.StatFs;

import com.bluberry.common.print;

import android.os.Handler;
import android.os.Message;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executor;

public class App extends Application {
    public final static boolean HDMI = false;

    private final static String TAG = "App";
    public static volatile boolean Alive = false;
    public static Executor IpRecvrr;

    private static App instance;
    private static Handler messageHandler;
    private static ADRender adRenderInstance;
    private HashMap<String, Typeface> fontHashMap;
    private String imageCacheDir = null;
    private Activity currentActivity = null;

    public static String getAdDir() {
        return instance.getExternalFilesDir(null).getAbsolutePath() + "/ad_bluberry/";
    }

    public static String getZipFile() {
        return instance.getExternalFilesDir(null).getAbsolutePath() + "/ad_bluberry_z.zip";
    }

    public static String getApkFile() {
        return instance.getExternalFilesDir(null).getAbsolutePath() + "/ad_bluberry_z.apk";
    }

    public static String getNextDir() {
        String dir;
        int last = getInt("ad_dir_idx");
        while (true) {
            last++;
            dir = getAdDir() + last + "/";
            File f = new File(dir);
            if (!f.exists()) {
                f.mkdir();
                saveInt("ad_dir_idx", last);
                print.w(TAG, "use dir :" + dir);
                return dir;
            }
        }
    }

    public static String[] getDirs(String parent) {
        File file = new File(parent);
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        if (directories != null) {
            Arrays.sort(directories);
        } else {
            directories = new String[0];
        }
        return directories;
    }

    public static App getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    public static void saveString(String key, String value) {
        getInstance().savePref("adclient", key, value);
    }

    public static String getString(String key) {
        return getInstance().getPref("adclient", key);
    }

    public static void saveInt(String key, int value) {
        saveString(key, String.valueOf(value));
    }

    public static int getInt(String key) {
        return Integer.valueOf(getString(key));
    }

    public static void RunAsApp(String cmds) {
        print.w(TAG, "cmds " + cmds);
        try {
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes(cmds + "\n");
            outputStream.flush();
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void installApk(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            try {
                final String command = "pm install -r " + file.getAbsolutePath();
                Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
                proc.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Alive = true;

        fontHashMap = new HashMap<>();

        File d = new File(getAdDir());
        if (!d.exists()) {
            d.mkdir();
        }
    }

    public Typeface getFont(String ttfName) {
        Typeface tf = fontHashMap.get(ttfName);
        if (tf == null) {
            print.w(TAG, "create font " + ttfName);
            tf = Typeface.createFromAsset(getAssets(), "fonts/" + ttfName);
            fontHashMap.put(ttfName, tf);
        }
        return tf;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        print.w(TAG, "System is running low on memory");
    }

    public Resources getAppResources() {
        return instance.getResources();
    }

    public String getImageCacheDir() {
        if (imageCacheDir != null) {
            return imageCacheDir;
        }

        File extDir = getExternalFilesDir(null);
        if (extDir != null) {
            StatFs stat = new StatFs(extDir.getAbsolutePath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long sizeInMb = (blockSize * totalBlocks) / 1024 / 1024;

            if (sizeInMb == 0) {
                imageCacheDir = getFilesDir().getAbsolutePath();
            } else {
                imageCacheDir = extDir.getAbsolutePath();
            }
        } else {
            imageCacheDir = getFilesDir().getAbsolutePath();
        }

        return imageCacheDir;
    }

    public int getFreeSpaceInMB(String dirName) {
        StatFs stat = new StatFs(dirName);
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        int free = (int) ((blockSize * availableBlocks) / 1024 / 1024);
        print.e("getFreeSpaceInMB ", dirName + ":" + free);
        return free;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public void savePref(String prefName, String key, String value) {
        print.i(TAG, "set " + key + " : " + value);
        SharedPreferences.Editor editor = getSharedPreferences(prefName, Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getPref(String prefName, String key) {
        SharedPreferences sharedata = getSharedPreferences(prefName, Context.MODE_PRIVATE);
        String value = sharedata.getString(key, "0");
        print.i(TAG, "get " + key + " : " + value);
        return value;
    }

    public void deletePref(String prefName, String key) {
        SharedPreferences.Editor editor = getSharedPreferences(prefName, Context.MODE_PRIVATE).edit();
        editor.remove(key);
        editor.apply();
    }

    public String getOtherAppPref(String pkgName, String prefName, String key) {
        try {
            Context otherContext = createPackageContext(pkgName, CONTEXT_IGNORE_SECURITY);
            SharedPreferences preferences = otherContext.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            return preferences.getString(key, null);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String filesDir() {
        return instance.getFilesDir().getAbsolutePath();
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(Activity activity) {
        currentActivity = activity;
    }

    public static void setMessageHandler(Handler handler) {
        messageHandler = handler;
    }

    public static void sendMessage(Msg msg, Object obj) {
        if (messageHandler != null) {
            Message m = messageHandler.obtainMessage(msg.ordinal(), obj);
            m.sendToTarget();
        }
    }

    public static void setAdRender(ADRender render) {
        adRenderInstance = render;
    }

    public static void parseJson(String dir, boolean add) {
        if (adRenderInstance != null) {
            adRenderInstance.parseJson(dir, add);
        }
    }
}
