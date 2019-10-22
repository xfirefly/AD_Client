package com.bluberry.adclient;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.bluberry.setup.ShowOptionMenu;
import com.bluberry.common.print;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import cs.ipc.Video;

public class RTKSourceInActivity extends Activity {

    /////////////////////////////////////////////////////////////////////
    public static RTKSourceInActivity thiz;
    private ADRender adRender;

    public void sendMessage(Msg m, Object obj) {
        adRender.sendMessage(m, obj);
    }

    public void captureHdmiIN() {
        mHandler.postDelayed(mScreenShot, 100);
    }

    public synchronized void parseJson(String dir, boolean add) {
        adRender.parseJson(dir, add);
    }

    public Video currHdmiIn;

    public void addHDMI(Video s) {
        if (!App.HDMI) {
            return;
        }

        //if (currHdmiIn != null)
        //	return;

        //m_HDMIRxPlayer = new HDMIRxPlayer(this, m_Root, s.x, s.y, s.w, s.h);
        currHdmiIn = s;
        //RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(s.w, s.h);
        //params.leftMargin = s.x;
        //params.topMargin = s.y;
        //m_Root.addView(iv, params);
        print.e(TAG, "add hdmi " + s.x + " " + s.y + " " + s.w + " " + s.h);
    }

    private ShowOptionMenu optionWindow = null; //显示 选源的UI类

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyShortcutEvent() called with: event = [" + event + "]");
        return super.dispatchKeyShortcutEvent(event);
    }

    boolean subEnable = true;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
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

    private Runnable closeshowSourceWindow = new Runnable() {
        @Override
        public void run() {
            closeOptionDialog();
        }
    };

    public void closeOptionDialog() {
        handimpl.removeCallbacks(closeshowSourceWindow);
        optionWindow.close();
    }

    public Handler handimpl = new Handler();

    /////////////////////////////////////////////////////////////////////

    private String TAG = "HDMIRxActivity";
    public ViewGroup m_Root;
    //private HDMIRxPlayer m_HDMIRxPlayer = null;
    private final Handler mHandler = new Handler();
    private byte[] mCapture;
    private static final long SCREENSHOT_SLOT = 200;
    private Toast mToast;
    private boolean mIsFullScreen = true;

    private final Runnable mScreenShot = new Runnable() {
        @Override
        public void run() {
/*			if (m_HDMIRxPlayer == null)
				return;
			if (m_HDMIRxPlayer.isPlaying() == false) {
				mHandler.postDelayed(this, SCREENSHOT_SLOT);
				return;
			}*/
            CaptureTask capTask = new CaptureTask(1, 0, 0, 1280, 720, 1280, 720);
            capTask.execute();
            //mHandler.postDelayed(this, SCREENSHOT_SLOT);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Make app to full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Hide both the navigation bar and the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        Log.d(TAG, "onCreate............");
        setContentView(R.layout.activity_hdmirx);
        m_Root = (ViewGroup) findViewById(R.id.root);

        //m_HDMIRxPlayer = new HDMIRxPlayer(this, m_Root, 500, 400);

        /////////////////////////////////////////////////////////////////////////
        thiz = this;
        adRender = new ADRender(this);
        adRender.init();
        //new Updater(this, new Handler());

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        scrWidth = displaymetrics.widthPixels;
        scrHeight = displaymetrics.heightPixels;
        optionWindow = new ShowOptionMenu(this, scrWidth, scrHeight);

        /////////////////////////////////////////////////////////////////////////

    }

    private int scrWidth, scrHeight;

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean retval = false;
/*		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			Log.d(TAG, "Key code = " + event.getKeyCode());
			if (m_HDMIRxPlayer != null)
				Log.d(TAG, "isPlaying() = " + m_HDMIRxPlayer.isPlaying());
		}

		if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
			if (event.getAction() == KeyEvent.ACTION_DOWN && m_HDMIRxPlayer != null && m_HDMIRxPlayer.isPlaying() == false) {
				m_HDMIRxPlayer.play();
			}
			retval = true;
		} else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_STOP) {
			if (event.getAction() == KeyEvent.ACTION_DOWN && m_HDMIRxPlayer != null && m_HDMIRxPlayer.isPlaying() == true) {
				m_HDMIRxPlayer.stop();
			}
			retval = true;
		} else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
			if (event.getAction() == KeyEvent.ACTION_DOWN && m_HDMIRxPlayer != null && m_HDMIRxPlayer.isPlaying() == true) {
				if (mIsFullScreen == true) {
					m_HDMIRxPlayer.setFixedSize(720, 480);
					mIsFullScreen = false;
				} else {
					m_HDMIRxPlayer.setFixedSize(1920, 1080);
					mIsFullScreen = true;
				}
			}
			retval = true;
		} else if (event.getKeyCode() == KeyEvent.KEYCODE_INFO) {
			if (event.getAction() == KeyEvent.ACTION_DOWN && m_HDMIRxPlayer != null && m_HDMIRxPlayer.isPlaying() == true) {
				if (mToast != null)
					mToast.cancel();
				if (m_HDMIRxPlayer.getHdmiInScanMode() == 1)
					mToast = Toast.makeText(this, "HDMI In ( " + m_HDMIRxPlayer.getHdmiInWidth() + "x" + m_HDMIRxPlayer.getHdmiInHeight() + ", " + m_HDMIRxPlayer.getHdmiInFps()
							+ "fps, Interlace )", Toast.LENGTH_LONG);
				else
					mToast = Toast.makeText(this, "HDMI In ( " + m_HDMIRxPlayer.getHdmiInWidth() + "x" + m_HDMIRxPlayer.getHdmiInHeight() + ", " + m_HDMIRxPlayer.getHdmiInFps()
							+ "fps, Progressive )", Toast.LENGTH_LONG);
				mToast.show();
			}
			retval = true;
		}*/
        if (retval == false) {
            retval = super.dispatchKeyEvent(event);
        }
        return retval;
    }

    @Override
    public void onResume() {
        super.onResume();
        //        mHandler.postDelayed(mScreenShot, SCREENSHOT_SLOT);
        Log.d(TAG, "------------OnResume--------");

        //if (currHdmiIn != null)
        //	addHDMI(currHdmiIn);

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
        //		mHandler.removeCallbacks(mScreenShot);
        //		if (m_HDMIRxPlayer.isPlaying() == true) {
        //			m_HDMIRxPlayer.stop();
        //		}
        //		m_HDMIRxPlayer.release();
        //		m_HDMIRxPlayer = null;

        stopHdmiIn();
    }

    public void stopHdmiIn() {
        mHandler.removeCallbacks(mScreenShot);
/*		if (m_HDMIRxPlayer != null) {
			if (m_HDMIRxPlayer.isPlaying() == true) {
				m_HDMIRxPlayer.stop();
			}
			m_HDMIRxPlayer.release();
			m_HDMIRxPlayer = null;
        }*/
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        super.onDestroy();

        adRender.unInit();
    }

    public class CaptureTask extends AsyncTask<Void, Void, byte[]> {
        int x = 0;
        int y = 0;
        int width = 0;
        int height = 0;
        int outWidth = 0;
        int outHeight = 0;
        int type = 0;

        public CaptureTask(int type, int x, int y, int width, int height, int outWidth, int outHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.outWidth = outWidth;
            this.outHeight = outHeight;
            this.type = type;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        private String nowTime() {
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss"); // HH:mm:ss
            String date = df.format(Calendar.getInstance().getTime());
            return date;
        }

        @Override
        protected byte[] doInBackground(Void... para) {
            byte[] data = new byte[0];
/*			if (m_HDMIRxPlayer == null || m_HDMIRxPlayer.isPlaying() == false)
				return null;

			byte[] data = m_HDMIRxPlayer.capture(type, x, y, width, height, outWidth, outHeight);*/
            Log.d(TAG, "datadata " + data.length);
            Bitmap one = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
            // Copy Bitmap to buffer
            ByteBuffer buffer = ByteBuffer.wrap(data);
            one.copyPixelsFromBuffer(buffer);
            //Canvas canvas = new Canvas(mutableBitmap); // now it should work ok
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + nowTime() + ".png");
                one.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
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

            return data;
        }

        @Override
        protected void onPostExecute(byte[] data) {
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d(TAG, "captureTask onCancelled ");
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }

    }

}
