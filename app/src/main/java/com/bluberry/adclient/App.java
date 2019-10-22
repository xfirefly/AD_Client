package com.bluberry.adclient;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.StatFs;

import com.bluberry.common.print;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executor;
//import com.realtek.hardware.RtkHDMIManager;

public class App extends Application {
    //============== config ==============
    //public final static boolean WAN = true;
    public final static boolean HDMI = false;
    public final static boolean SERIAL_PORT = false;


    //////////////////////////////////////////////////
    private final static String TAG = "App";
    private static App instance;
    public static volatile boolean Alive = false;

    private HashMap<String, Typeface> fontHashMap;

    public static Executor IpRecvrr;

    //所有ad zip 解压到本目录下, 按数字递增取文件夹名
    public static final String AdDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ad_bluberry/";

    //public static String unzipDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ad_bluberry/";
    //public static String unzipTmp = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ad_bluberry_tmp/";
    //public static String unzipDirEx = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ad_bluberry_ex/";

    public static String zipFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ad_bluberry_z.zip";
    public static String apkFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ad_bluberry_z.apk";

    // 获取可用文件夹名称
    public static String getNextDir() {
        String dir;
        int last = getInt("ad_dir_idx");
        while (true) {
            last++;
            dir = AdDir + last + "/";
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
        Arrays.sort(directories);
        System.out.println(Arrays.toString(directories));
        return directories;
    }

    /**
     * 不要执行太多代码,影响启动速度
     */
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Alive = true;

        fontHashMap = new HashMap<String, Typeface>();
        //		fontHashMap.put("simkai.ttf", simkai);
        //		fontHashMap.put("msyh.ttf", msyh);
        //		fontHashMap.put("simsun.ttf", simsun);
        //		fontHashMap.put("simhei.ttf", simhei);
        //		fontHashMap.put("pingfang.ttf", pingfang);

        //IpRecvrr = Executors.newSingleThreadExecutor();

        File d = new File(AdDir);
        if (!d.exists()) {
            d.mkdir();
        }

/*		RtkHDMIManager r = new RtkHDMIManager();
		if (r != null) {
			r.setTVSystem(9);   // 1080p 50 for tuke
		}*/
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

    /**
     * Called when the overall system is running low on memory
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();

        print.w(TAG, "System is running low on memory");
    }

    /**
     * @return The Application Instance
     */
    public static App getInstance() {
        return instance;
    }

    /**
     * @return the main resources from the App
     */
    public Resources getAppResources() {
        return instance.getResources();
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    private String imageCacheDir = null;

    public String getImageCacheDir() {
        if (imageCacheDir != null) {
            return imageCacheDir;
        }

        StatFs stat;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
            stat = new StatFs(dir); // 创建StatFs对象
            long blockSize = stat.getBlockSize(); // 获取block的size
            float totalBlocks = stat.getBlockCount(); // 获取block的个数
            int sizeInMb = (int) (blockSize * totalBlocks) / 1024 / 1024; // 计算总容量

            if (sizeInMb == 0) {
                print.e("sd card", "size 0, why why why why why why why why ???"); // xiaomi
                imageCacheDir = getFilesDir().getAbsolutePath();
            } else {
                imageCacheDir = dir;
            }
        } else {
            imageCacheDir = getFilesDir().getAbsolutePath();
        }

        return imageCacheDir;
    }

    public int getFreeSpaceInMB(String dirName) {
        int imageCacheDirFree = 0;

        StatFs stat = new StatFs(dirName);
        long data_blockSize = stat.getBlockSize(); // 获取block的size
        long data_availableBlocks = stat.getAvailableBlocks(); // 获取可用block的个数
        imageCacheDirFree = (int) (data_blockSize * data_availableBlocks) / 1024 / 1024; // MB可用容量

        print.e("getFreeSpaceInMB ", dirName + ":" + imageCacheDirFree);

        return imageCacheDirFree;
    }

    /**
     * true if Network connected
     *
     * @return
     */
    public boolean isNetworkAvailable() {
        Context context = getApplicationContext();
        ConnectivityManager connect = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connect == null) {
            return false;
        } else { // get all network info
            NetworkInfo[] info = connect.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    /**
     * save Preferences in current context
     *
     * @param prefName
     * @param key
     * @param value
     */
    public void savePref(String prefName, String key, String value) {
        print.i(TAG, "set " + key + " : " + value);

        SharedPreferences.Editor sharedatab = getSharedPreferences(prefName, Context.MODE_PRIVATE).edit();
        sharedatab.putString(key, value);
        sharedatab.apply();
    }

    /**
     * get Preferences in current context
     *
     * @param prefName
     * @param key
     * @return if key not found, return null
     */
    public String getPref(String prefName, String key) {
        SharedPreferences sharedata = getSharedPreferences(prefName, Context.MODE_PRIVATE);
        String value = sharedata.getString(key, "0");

        print.i(TAG, "get " + key + " : " + value);
        return value;
    }

    /**
     * delete Preferences in current context
     *
     * @param prefName
     * @param key
     */
    public void deletePref(String prefName, String key) {
        SharedPreferences.Editor sharedatab = getSharedPreferences(prefName, Context.MODE_PRIVATE).edit();

        sharedatab.remove(key);
        sharedatab.apply();
    }

    /**
     * 获取其他apk 的SharedPreferences 内容
     *
     * @param pkgName  "com.example.blueberrybackplay"
     * @param prefName "huibo"
     * @param key      "url"
     */
    public String getOtherAppPref(String pkgName, String prefName, String key) {
        Context huiboContext = null;
        try {
            huiboContext = createPackageContext(pkgName, CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        SharedPreferences preferences = huiboContext.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        String value = preferences.getString(key, null);

        return value;
    }

    public String filesDir() {
        return instance.getFilesDir().getAbsolutePath();
    }

    private Activity currentActivity = null;

    /**
     * for LanmeiHome
     *
     * @return
     */
    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(Activity activity) {
        currentActivity = activity;
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

}
