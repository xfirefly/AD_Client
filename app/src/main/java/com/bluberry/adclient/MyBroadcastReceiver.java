package com.bluberry.adclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import com.bluberry.common.FileUtil;
import com.bluberry.common.IOUtil;

import java.io.File;

public class MyBroadcastReceiver extends BroadcastReceiver {
    static final String TAG = "MyBroadcastReceiver";

    private IntentFilter mIntentFilter = null;

    public void registe() {

        mIntentFilter = new IntentFilter();
        //mIntentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        mIntentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        mIntentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        mIntentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        mIntentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        mIntentFilter.addDataScheme("file");
        App.getInstance().registerReceiver(this, mIntentFilter);
        mIntentFilter = new IntentFilter();
        //For Network
        //WiFi
        //mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        //mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        //Ethernet
        //mIntentFilter.addAction(EthernetManager.NETWORK_STATE_CHANGED_ACTION);
        //mIntentFilter.addAction(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);
        //Network
        //mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        //mIntentFilter.addAction(ConnectivityManager.INET_CONDITION_ACTION);
        //App.getInstance().registerReceiver(this, mIntentFilter);
    }

    public void unRegiste() {
        App.getInstance().unregisterReceiver(this);
    }

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final String Data = intent.getDataString();
        final String Type = intent.getType();
        final String DataScheme = intent.getScheme();

        Log.d(TAG, "<BroadcastReceiver> action:" + action);
        Log.d(TAG, "<BroadcastReceiver> Component:" + intent.getComponent());
        Log.d(TAG, "<BroadcastReceiver> Categories:" + intent.getCategories());
        Log.d(TAG, "<BroadcastReceiver> Data:" + Data);
        Log.d(TAG, "<BroadcastReceiver> Type:" + Type);
        Log.d(TAG, "<BroadcastReceiver> DataScheme:" + DataScheme);

        if (action.equals("android.intent.action.MEDIA_REMOVED") || action.equals("android.intent.action.MEDIA_BAD_REMOVAL")) {

        } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {

        } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            String path = Data.substring(7) + "/export_subtitles_tk.zip"; // skip file://

            File from = new File(path);
            if (from.exists()) {
                Toast.makeText(App.getInstance().getApplicationContext(), "找到场景文件", Toast.LENGTH_LONG).show();

                Thread copyAndShow = new Thread(new CopyAndShow(path));
                copyAndShow.start();
            } else {
                Toast.makeText(App.getInstance().getApplicationContext(), "没找到场景文件", Toast.LENGTH_LONG).show();
            }
        } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

        } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION) || action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {

        }

    }

    public class CopyAndShow implements Runnable {
        private String from = null;

        public CopyAndShow(String path) {
            from = path;
        }

        public void run() {
            try {
                MainActivity.thiz.sendMessage(Msg.MESSAGE_RECV_JSON_BEGIN, "");

                File f = new File(App.zipFile);
                IOUtil.delete(f);
                IOUtil.copyFile(from, App.zipFile);

                //RTKSourceInActivity.thiz.unzipAdFile();

                String dir = App.getNextDir();
                FileUtil.Unzip(App.zipFile, dir);

                MainActivity.thiz.parseJson(dir, true);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}