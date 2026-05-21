package com.bluberry.adclient;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.bluberry.common.print;
import com.bluberry.setup.ShowOptionMenu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cs.ipc.Video;

public class MainActivity extends Activity {

    private static final long SCREENSHOT_SLOT = 200;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService captureExecutor = Executors.newSingleThreadExecutor();
    private final Runnable mScreenShot = new Runnable() {
        @Override
        public void run() {
            captureExecutor.execute(new CaptureTask(1, 0, 0, 1280, 720, 1280, 720));
        }
    };
    public Video currHdmiIn;
    public Handler handimpl = new Handler(Looper.getMainLooper());
    public ViewGroup m_Root;
    boolean subEnable = true;
    private ADRender adRender;
    private ShowOptionMenu optionWindow = null;
    private Runnable closeshowSourceWindow = new Runnable() {
        @Override
        public void run() {
            closeOptionDialog();
        }
    };
    private String TAG = "HDMIRxActivity";
    private byte[] mCapture;
    private Toast mToast;
    private boolean mIsFullScreen = true;
    private int scrWidth, scrHeight;

    public void sendMessage(Msg m, Object obj) {
        adRender.sendMessage(m, obj);
    }

    public void captureHdmiIN() {
        mHandler.postDelayed(mScreenShot, 100);
    }

    public synchronized void parseJson(String dir, boolean add) {
        adRender.parseJson(dir, add);
    }

    public void addHDMI(Video s) {
        if (!App.HDMI) {
            return;
        }
        currHdmiIn = s;
        print.e(TAG, "add hdmi " + s.x + " " + s.y + " " + s.w + " " + s.h);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyShortcutEvent() called with: event = [" + event + "]");
        return super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown() called with: keyCode = [" + keyCode + "], event = [" + event + "]");

        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                View v = new View(this);
                optionWindow.showWindow(v);
                handimpl.removeCallbacks(closeshowSourceWindow);
                handimpl.postDelayed(closeshowSourceWindow, 30000L);
                break;

            case KeyEvent.KEYCODE_MEDIA_REWIND:
                subEnable = !subEnable;
                sendMessage(Msg.MESSAGE_CHANGE_SUBTITLE_VISABLE, subEnable);
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                sendMessage(Msg.MESSAGE_NEXT_SCENE, this);
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                sendMessage(Msg.MESSAGE_PREV_SCENE, this);
                break;

            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void closeOptionDialog() {
        handimpl.removeCallbacks(closeshowSourceWindow);
        optionWindow.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);

        Log.d(TAG, "onCreate............");
        setContentView(R.layout.activity_hdmirx);
        m_Root = (ViewGroup) findViewById(R.id.root);

        adRender = new ADRender(this);
        adRender.init();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        scrWidth = displaymetrics.widthPixels;
        scrHeight = displaymetrics.heightPixels;
        optionWindow = new ShowOptionMenu(this, scrWidth, scrHeight);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "------------OnResume--------");
        adRender.reDrawCurrentScene();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "Configuration Change :" + newConfig);
        Log.d(TAG, "Configuration Change keyboard: " + newConfig.keyboard);
        Log.d(TAG, "Configuration Change keyboardHidden: " + newConfig.keyboardHidden);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        stopHdmiIn();
    }

    public void stopHdmiIn() {
        mHandler.removeCallbacks(mScreenShot);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        captureExecutor.shutdownNow();
        adRender.unInit();
    }

    private class CaptureTask implements Runnable {
        int x, y, width, height, outWidth, outHeight, type;

        public CaptureTask(int type, int x, int y, int width, int height, int outWidth, int outHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.outWidth = outWidth;
            this.outHeight = outHeight;
            this.type = type;
        }

        private String nowTime() {
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            return df.format(Calendar.getInstance().getTime());
        }

        @Override
        public void run() {
            byte[] data = new byte[0];
            Log.d(TAG, "datadata " + data.length);
            Bitmap one = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
            ByteBuffer buffer = ByteBuffer.wrap(data);
            one.copyPixelsFromBuffer(buffer);

            FileOutputStream out = null;
            try {
                File extDir = getExternalFilesDir(null);
                String path = (extDir != null ? extDir.getAbsolutePath() : getFilesDir().getAbsolutePath()) + "/" + nowTime() + ".png";
                out = new FileOutputStream(path);
                one.compress(Bitmap.CompressFormat.JPEG, 100, out);
                one.recycle();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
