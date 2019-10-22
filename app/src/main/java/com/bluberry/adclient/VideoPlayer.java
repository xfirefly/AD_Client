package com.bluberry.adclient;

import android.content.Context;
import android.media.AudioManager;
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

    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mSurfaceHolder = null;
    private MediaPlayer mediaPlayer;
    private Context mContext;

    private String filePath;

    public VideoPlayer(Context context, ViewGroup parent, int x, int y, int width, int height) {
        mContext = context;

        // Create a new Media Player.
        mediaPlayer = new MediaPlayer();

		/*
		try {
		    Class<?> cMediaTimeProvider = Class.forName( "android.media.MediaTimeProvider" );
		    Class<?> cSubtitleController = Class.forName( "android.media.SubtitleController" );
		    Class<?> iSubtitleControllerAnchor = Class.forName( "android.media.SubtitleController$Anchor" );
		    Class<?> iSubtitleControllerListener = Class.forName( "android.media.SubtitleController$Listener" );

		    Constructor constructor = cSubtitleController.getConstructor(new Class[]{Context.class, cMediaTimeProvider, iSubtitleControllerListener});

		    Object subtitleInstance = constructor.newInstance(context, null, null);

		    Field f = cSubtitleController.getDeclaredField("mHandler");

		    f.setAccessible(true);
		    try {
		        f.set(subtitleInstance, new Handler());
		    }
		    catch (IllegalAccessException e) { }
		    finally {
		        f.setAccessible(false);
		    }

		    Method setsubtitleanchor = mediaPlayer.getClass().getMethod("setSubtitleAnchor", cSubtitleController, iSubtitleControllerAnchor);

		    setsubtitleanchor.invoke(mediaPlayer, subtitleInstance, null);
		    //Log.e("", "subtitle is setted :p");
		} catch (Exception e) {}
		 */

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(mPreparedListener);
        mediaPlayer.setOnCompletionListener(mCompletionListener);
        mediaPlayer.setOnErrorListener(mOnErrorListener);

        mSurfaceView = new SurfaceView(context);

        // Configure the Surface View.
        //mSurfaceView.setZOrderOnTop(false);
        mSurfaceView.setZOrderMediaOverlay(true);
        mSurfaceView.setKeepScreenOn(true);

        // Configure the Surface Holder and register the callback.
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        //mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.setFixedSize(width, height);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.leftMargin = x;
        params.topMargin = y;
        parent.addView(mSurfaceView, params);

        mSurfaceView.setVisibility(View.VISIBLE);

        /**
         * Listing 15-5: Controlling playback using the Media Controller
         */
		/*
		MediaController mediaController = new MediaController(mContext);
		mediaController.setMediaPlayer(new MediaPlayerControl() {
			public boolean canPause() {
				return true;
			}

			public boolean canSeekBackward() {
				return true;
			}

			public boolean canSeekForward() {
				return true;
			}

			public int getBufferPercentage() {
				return 0;
			}

			public int getCurrentPosition() {
				return mediaPlayer.getCurrentPosition();
			}

			public int getDuration() {
				return mediaPlayer.getDuration();
			}

			public boolean isPlaying() {
				return mediaPlayer.isPlaying();
			}

			public void pause() {
				mediaPlayer.pause();
			}

			public void seekTo(int pos) {
				mediaPlayer.seekTo(pos);
			}

			public void start() {
				mediaPlayer.start();
			}

			@Override
			public int getAudioSessionId() {
				// TODO Auto-generated method stub
				return 0;
			}
		});
		*/
    }

    private void play(String file) {
        filePath = file;
        try {
            mediaPlayer.setDataSource(filePath);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mediaPlayer.prepareAsync();
    }

    public void setSource(String path) {
        filePath = path;
    }

    public void destory() {
        if (mediaPlayer != null)
            mediaPlayer.release();
    }

    public void setZOrder(boolean z) {

        //mSurfaceView.setZOrderMediaOverlay(z);
        //mSurfaceView.setZOrderOnTop(z);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            Log.i("tt", "surfaceCreated");
            // When the surface is created, assign it as the
            // display surface and assign and prepare a data
            // source.

            mediaPlayer.setDisplay(holder);
            //mediaPlayer.setDataSource(filePath);
            //mediaPlayer.prepareAsync();
            play();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal Argument Exception", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Illegal State Exception", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception", e);
        } catch (Exception e) {
            Log.e(TAG, "IO Exception", e);
        }
    }

    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            Log.i("tt", "onPrepared");
            mp.setLooping(true);
            mp.start();
        }
    };

    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            Log.i("tt", "onCompletion");
            mp.reset();

            try {
                mediaPlayer.setDataSource(filePath);

                mediaPlayer.prepareAsync();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    };

    private OnErrorListener mOnErrorListener = new OnErrorListener() {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.d("tt", "on error");

            return false;
        }
    };

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("tt", "surfaceDestroyed");

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        //mediaPlayer.release();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("tt", "surfaceChanged");
    }

    public void stop() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }
}
