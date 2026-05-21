package com.bluberry.adclient;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.io.IOException;

public class VideoPlayer implements SurfaceHolder.Callback {
    private final String TAG = "VideoPlayer";

    private SurfaceView mSurfaceView;
    private MediaPlayer mediaPlayer;
    private String filePath;

    // Set to true before any MediaPlayer.release() call so concurrent stop()
    // or surfaceDestroyed() invocations become safe no-ops.
    private volatile boolean released = false;

    // onPrepared: begin looping playback as soon as the decoder is ready.
    // • Single-video playlist: setLooping(true) keeps it running indefinitely;
    //   VideoUpdater sleeps forever and never calls play() again.
    // • Multi-video playlist: VideoUpdater wakes after intval seconds and calls
    //   play() with the next file, which reset()s the player — looping is
    //   interrupted cleanly and the next video starts fresh.
    private final MediaPlayer.OnPreparedListener mPreparedListener = mp -> {
        Log.i(TAG, "onPrepared");
        mp.setLooping(true);
        mp.start();
    };

    // NOTE: onCompletion is intentionally omitted.
    // setLooping(true) already handles single-video continuous play without
    // re-preparing. The old onCompletion did the same thing redundantly.

    // Log errors; return false so the player can attempt natural recovery.
    private final MediaPlayer.OnErrorListener mOnErrorListener = (mp, what, extra) -> {
        Log.e(TAG, "MediaPlayer error what=" + what + " extra=" + extra);
        return false;
    };

    public VideoPlayer(Context context, ViewGroup parent, int x, int y, int width, int height) {
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build());

        mediaPlayer.setOnPreparedListener(mPreparedListener);
        mediaPlayer.setOnErrorListener(mOnErrorListener);

        mSurfaceView = new SurfaceView(context);
        mSurfaceView.setZOrderMediaOverlay(true);
        mSurfaceView.setKeepScreenOn(true);

        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setFixedSize(width, height);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.leftMargin = x;
        params.topMargin = y;
        parent.addView(mSurfaceView, params);
        mSurfaceView.setVisibility(View.VISIBLE);
    }

    /** Sets the file path for the next play() call without starting playback. */
    public void setSource(String path) {
        filePath = path;
    }

    /** Returns the SurfaceView so the parent can remove it from its ViewGroup on cleanup. */
    public SurfaceView getSurfaceView() {
        return mSurfaceView;
    }

    /**
     * Starts (or restarts) playback of {@link #filePath}.
     * Safe to call from any thread.
     */
    public void play() {
        if (released || filePath == null) return;
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepareAsync();
        } catch (IllegalStateException | IOException | IllegalArgumentException | SecurityException e) {
            Log.e(TAG, "play() failed", e);
        }
    }

    /**
     * Stops ongoing playback. No-op if already stopped or {@link #destroy()} was called.
     * Called from VideoUpdater (background thread) and surfaceDestroyed (main thread).
     */
    public void stop() {
        if (released || mediaPlayer == null) return;
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "stop() failed", e);
        }
    }

    /**
     * Permanently releases MediaPlayer resources. Must be called when the player is
     * no longer needed. Sets {@link #released} before touching the MediaPlayer so
     * any concurrent stop() or surfaceDestroyed() calls become safe no-ops.
     */
    public void destroy() {
        released = true;         // guard first — stop() checks this flag
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // ── SurfaceHolder.Callback ────────────────────────────────────────────────

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (released) return;
        Log.i(TAG, "surfaceCreated");
        try {
            mediaPlayer.setDisplay(holder);
            play();
        } catch (Exception e) {
            Log.e(TAG, "surfaceCreated failed", e);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        // stop() is a no-op if destroy() already ran (released == true),
        // preventing double-stop / use-after-release.
        stop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
    }
}
