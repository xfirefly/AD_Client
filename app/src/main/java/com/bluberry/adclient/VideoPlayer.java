package com.bluberry.adclient;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.io.IOException;

public class VideoPlayer implements SurfaceHolder.Callback {
    private final String TAG = "VideoPlayer";
    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            Log.i(TAG, "onPrepared");
            mp.setLooping(true);
            mp.start();
        }
    };
    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mSurfaceHolder = null;
    private MediaPlayer mediaPlayer;
    private Context mContext;
    private String filePath;
    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            Log.i(TAG, "onCompletion");
            mp.reset();

            try {
                mediaPlayer.setDataSource(filePath);
                mediaPlayer.prepareAsync();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private OnErrorListener mOnErrorListener = new OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.d(TAG, "on error");
            return false;
        }
    };

    public VideoPlayer(Context context, ViewGroup parent, int x, int y, int width, int height) {
        mContext = context;

        mediaPlayer = new MediaPlayer();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build();
        mediaPlayer.setAudioAttributes(audioAttributes);

        mediaPlayer.setOnPreparedListener(mPreparedListener);
        mediaPlayer.setOnCompletionListener(mCompletionListener);
        mediaPlayer.setOnErrorListener(mOnErrorListener);

        mSurfaceView = new SurfaceView(context);

        mSurfaceView.setZOrderMediaOverlay(true);
        mSurfaceView.setKeepScreenOn(true);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFixedSize(width, height);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.leftMargin = x;
        params.topMargin = y;
        parent.addView(mSurfaceView, params);

        mSurfaceView.setVisibility(View.VISIBLE);
    }

    private void play(String file) {
        filePath = file;
        try {
            mediaPlayer.setDataSource(filePath);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.prepareAsync();
    }

    public void play() {
        if (filePath == null) {
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.setDataSource(filePath);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.prepareAsync();
    }

    public void setSource(String path) {
        filePath = path;
    }

    public void destroy() {
        if (mediaPlayer != null)
            mediaPlayer.release();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            Log.i(TAG, "surfaceCreated");
            mediaPlayer.setDisplay(holder);
            play();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal Argument Exception", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Illegal State Exception", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
    }

    public void stop() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }
}
