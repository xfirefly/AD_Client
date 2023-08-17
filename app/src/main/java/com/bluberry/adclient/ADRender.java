package com.bluberry.adclient;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bluberry.adclient.sp.SerialPortBiz;
import com.bluberry.common.IOUtil;
import com.bluberry.common.print;
import com.bluberry.network.CommandClient;
import com.bluberry.network.IpReceiver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cs.comm.AdProgram;
import cs.comm.Cmd;
import cs.comm.Command;
import cs.comm.Scene;
import cs.ipc.AdItemType;
import cs.ipc.Direction;
import cs.ipc.Picture;
import cs.ipc.Subtitle;
import cs.ipc.Video;

import static com.bluberry.adclient.Reversed.reversed;

public class ADRender {

    private static final String Timing_0000 = "00:00";
    private static MainActivity rtk;
    private ViewGroup m_Root;
    private String TAG = "ADR";
    private List<AdProgram> adList = new ArrayList<>();    // all ad
    private AdProgram currentAd = null;
    private Scene currentScene = null;

    private MyBroadcastReceiver usbReceiver;

    private GpioReadThread gpioThread;
    private SerialPortBiz spb;

    private List<ScrollTextView> subtitleList = new ArrayList<ScrollTextView>();
    private List<View> picList = new ArrayList<>();
    //private Thread backImageThread;
    //private Thread jsonParserThread;
    private Thread threadSceneUpdater; //切换上下场景时, 需要关闭此行程

    // app 相关线程, app 退出時全部关闭
    private List<Thread> appThreads = new ArrayList<Thread>();

    //ad program 相关线程, 收到新 AdProgram json 时需要全部关闭
    private List<Thread> adThreads = new ArrayList<Thread>();

    //场景显示 相关线程, 切换新场景时需要全部关闭
    private List<Thread> sceneThreads = new ArrayList<Thread>();

    private HashMap<String, Drawable> drawableMap = new HashMap<>();
    private TextView tvTips = null;
    public Handler adHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            List<Scene> scList = new ArrayList<>();
            List<AdProgram> scadList = new ArrayList<>();
            //HashMap<Scene, AdProgram> scMap = new HashMap<>();

            Msg usermsg = Msg.values()[msg.what];
            print.i(TAG, "handle msg " + usermsg.toString() + " ");
            switch (usermsg) {
                case MESSAGE_PARSE_AD_DONE:
                    break;

                case MESSAGE_DRAW_AD:
                    tvTips = null;

                    endDrawAd(); //stop prev ad
                    drawAdProgram((AdProgram) msg.obj);
                    startSceneUpdater();
                    break;

                case MESSAGE_DRAW_SCENE_EX:
                    tvTips = null;
				/*if (msg.arg1 >= currentAd.scene.size() )
					break;*/

                    endDrawAd(); //stop prev ad
                    drawAdProgram((AdProgram) msg.obj);
                    try {
                        drawScene(currentAd.scene.get(msg.arg1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;

                case MESSAGE_DRAW_SCENE:
                    drawScene((Scene) msg.obj);
                    break;

/*
			case MESSAGE_PARSE_AD_FINISH:
				tvTips = null;

				currentAd = (AdProgram) msg.obj;
				drawAdProgram();
				break;

			case MESSAGE_ADD_TIMING_SCENE:
				AdProgram prog = (AdProgram) msg.obj;
				if (currentAd == null) {
					print.e(TAG, "currentAd is null, ERROR");
					return;
				}

				for (Scene sne : prog.scene) {
					currentAd.scene.add(sne);
					print.i(TAG, "add new timing scene: " + sne.timing);
				}
				break; */

                case MESSAGE_DRAW_BG:
                    drawBackImage((Drawable) msg.obj);
                    break;

                case MESSAGE_DRAW_IMAGE:
                    PictureUpdater iu = (PictureUpdater) msg.obj;
                    drawPicture(iu.getImageView(), iu.getDrawableImg());
                    break;

                case MESSAGE_DRAW_VIDEO:
                    VideoUpdater vu = (VideoUpdater) msg.obj;
                    playVideo(vu.vPlayer, "");
                    break;

                case MESSAGE_TakeScreenshot:
                    Thread ipReceiverThread = new Thread(new TakeScreenshot((CommandClient) msg.obj));
                    ipReceiverThread.start();

                    //rtk.captureHdmiIN();
                    break;

                case MESSAGE_STOP_AND_RENAME:

                    break;
                case MESSAGE_RECV_JSON_BEGIN:
                    if (App.SERIAL_PORT)
                        break;

                    //endDrawAd();
                    //showTips(App.getInstance().getResources().getText(R.string.wait));
                    Toast.makeText(rtk, R.string.wait, Toast.LENGTH_LONG).show();
                    break;
                case MESSAGE_RECV_JSON_PROGRESS:
                    if (App.SERIAL_PORT)
                        break;

                    if (tvTips != null) {
                        tvTips.setText("接收文件, 请稍候...(" + msg.obj + "%)");
                    }
                    break;
                case MESSAGE_RECV_JSON_END:
                    if (App.SERIAL_PORT)
                        break;

                    if (tvTips != null) {
                        tvTips.setText("解析文件, 请稍候...");
                    }
                    break;

                case MESSAGE_NO_DATA:
                    endDrawAd();
                    showTips(App.getInstance().getResources().getText(R.string.no_data));
                    //Toast.makeText(rtk, R.string.no_data, Toast.LENGTH_LONG).show();

                    sendEmptyMessageDelayed(Msg.MESSAGE_ADD_HDMI.ordinal(), 10);
                    break;

                case MESSAGE_ADD_HDMI:

/*				RtkHDMIRxManager rxm = new RtkHDMIRxManager();
				HDMIRxStatus rxStatus = rxm.getHDMIRxStatus();
				if (rxStatus.status == HDMIRxStatus.STATUS_READY) {
					Video video = new Video();
					video.x = video.y = 0;
					video.w = 1920;
					video.h = 1080;
					rtk.addHDMI(video);
				}
				rxm.release();*/
                    break;

                case MESSAGE_PREV_SCENE:
//				for (int i = 0; i < adList.size(); i++) {
//					if (adList.get(i).scene.get(0).drawing) {
//						if (i <= 0) { //已经是播放第一个场景
//							break;
//						} else {
//							switchPrevNextScene(i - 1);
//							break;
//						}
//					}
//				}

                    for (int i = 0; i < adList.size(); i++) {
                        for (Scene sc : adList.get(i).scene) {
                            scList.add(sc);
                            scadList.add(adList.get(i));
                            //scMap.put(sc, adList.get(i));
                        }
                    }

                    for (int i = 0; i < scList.size(); i++) {
                        if (scList.get(i).drawing) {
                            if (i == 0) {
                                break;
                            } else {

                                Message msgx = adHandler.obtainMessage(Msg.MESSAGE_DRAW_SCENE_EX.ordinal(), scadList.get(i - 1));
                                msgx.arg1 = i - 1;
                                msgx.sendToTarget();
                                break;
                            }
                        }
                    }
                    break;
                case MESSAGE_NEXT_SCENE:
//				for (int i = 0; i < adList.size(); i++) {
//					if (adList.get(i).scene.get(0).drawing) {
//						if (i >= (adList.size() - 1)) { //已经是播放最后一个场景
//							break;
//						} else {
//							switchPrevNextScene(i + 1);
//							break;
//						}
//					}
//				}
                    for (int i = 0; i < adList.size(); i++) {
                        for (Scene sc : adList.get(i).scene) {
                            scList.add(sc);
                            scadList.add(adList.get(i));
                        }
                    }

                    for (int i = 0; i < scList.size(); i++) {
                        if (scList.get(i).drawing) {
                            if (i == (scList.size() - 1)) {
                                break;
                            } else {

                                Message msgx = adHandler.obtainMessage(Msg.MESSAGE_DRAW_SCENE_EX.ordinal(), scadList.get(i + 1));
                                msgx.arg1 = i + 1;
                                msgx.sendToTarget();
                                break;
                            }
                        }
                    }
                    break;

                case MESSAGE_CHANGE_SUBTITLE_VISABLE:
                    enableSubtitle((Boolean) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    public ADRender(MainActivity mainActivity) {
        rtk = mainActivity;
        m_Root = rtk.m_Root;
    }

    public void init() {
        //App.IpRecv.execute(IpReceiver.getInstance());
        //if (App.WAN)
        {
            //WanServer.getInstance().connectWanServerCmd();
        }
        //else
        {
            Thread ipReceiverThread = new Thread(IpReceiver.getInstance());
            ipReceiverThread.start();
            appThreads.add(ipReceiverThread);
        }

        Thread jsonParserThread = new Thread(new AdJsonParser());
        jsonParserThread.start();
        //sceneThreads.add(jsonParserThread);
        usbReceiver = new MyBroadcastReceiver();
        usbReceiver.registe();

        if (App.getString("name").equals("0")) {
            App.saveString("name", "unnamed");
        }

        if (App.getString("id").equals("0")) {
            App.saveString("id", "1");
            App.saveInt("sub_onoff", 1);
        }

        if (App.SERIAL_PORT) {
            spb = new SerialPortBiz();
            spb.init();

            gpioThread = new GpioReadThread();
            gpioThread.start();

/*			IntentFilter hdmiRxFilter = new IntentFilter(HDMIRxStatus.ACTION_HDMIRX_PLUGGED);
			rtk.registerReceiver(hdmiRxHotPlugReceiver, hdmiRxFilter);*/
        }


        Thread timingAdChecker = new Thread(new TimingAdChecker());
        timingAdChecker.start();
    }

    public void unInit() {
        App.Alive = false;

/*
		try {
			rtk.unregisterReceiver(hdmiRxHotPlugReceiver);
		} catch (Exception e) {
		}
*/

        if (spb != null) {
            spb.uninit();
        }

        usbReceiver.unRegiste();
        IpReceiver.getInstance().Close();

        endDrawAd();

        print.w(TAG, "appThreads join begin " + nowTime());
        joinThreads(appThreads);


        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 不管什么场景发过来, 都添加到List, 定时场景等待到时再播放,  轮播和主界面发送的单个场景直接播放
     * 定时场景一次只能发一个, 而且必须设置时间.
     * 轮播场景, 必须是>=2个场景, 不能设置定时, 必须设定轮播间隔
     * 单个场景, 不设置定时, 也不设置轮播间隔
     * <p>
     * 断点记忆, 最后场景(单条, 轮播), 重启再播放, 以前的定时场景不再处理
     * 可用上下场景切换
     * <p>
     * 网络 发送, 发单条场景, 取消轮播, 不取消定时 , 追加在后面
     * <p>
     * 主界面2个发送按钮 发单条场景
     * <p>
     * 场景追加在后面
     * <p>
     * 切换场景时关闭轮播
     * <p>
     * 底图改成背景图
     *
     * @param dir must end with /
     * @param add 通过网络或者串口发过来, 设为true
     * @return
     */
    public synchronized AdProgram parseJson(String dir, boolean add) {
        print.w(TAG, "parseJson: " + dir);
        AdProgram ad = null;
        String json = IOUtil.loadFileAsString(dir + "config.json");
        if (json == null) {
            sendMessage(Msg.MESSAGE_NO_DATA, "");
            return ad;
        }

        try {
            //byte[] buff = IOUtil.readStream(new FileInputStream(App.unzipDir + "config.json"));
            json = CryptLib.decode(json);
            if (json == null) {
                print.w(TAG, "decode ret null ");
                return ad;
            }

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(AdItemType.class, new AdItemTypeSerializer());
            gsonBuilder.registerTypeAdapter(Direction.class, new DirectionSerializer());
            Gson gson = gsonBuilder.create();

            ad = gson.fromJson(json, AdProgram.class);
            for (Scene sne : ad.scene) {
                sne.dir = dir;
            }

            //不管什么场景发过来, 都添加到List, 定时场景等待到时再播放,  轮播和主界面发送的单个场景直接播放
            //sendMessage(Msg.MESSAGE_PARSE_AD_DONE, ad);
            adList.add(ad);

            if (add && ad.scene.get(0).timing.equals(Timing_0000)) {
                sendMessage(Msg.MESSAGE_DRAW_AD, ad);
            }

/*
			if (ad.sceneIntval > 0 || ad.scene.get(0).timing.equals(Timing_0000)) { //轮播场景
				sendMessage(Msg.MESSAGE_PARSE_AD_FINISH, ad);

				if (add) { // 通过网络发送或者usb导入
					Thread.sleep(1000);
					removeOtherScene(dir);
				}
			} else { //定时播放
				sendMessage(Msg.MESSAGE_ADD_TIMING_SCENE, ad);
			}
*/
            //print.w(TAG, new GsonBuilder().setPrettyPrinting().create().toJson(ad));
        } catch (JsonParseException e) {
            e.printStackTrace();

        } catch (Exception e) {

            e.printStackTrace();
        }
        return ad;
    }

    private void removeOtherScene__(String dir) {
        print.i(TAG, "keep scene: " + dir);
        for (String str : App.getDirs(App.AdDir)) {
            String tmp = App.AdDir + str + "/";
            if (tmp.compareTo(dir) != 0) {
                IOUtil.delete(new File(tmp));
            }
        }
    }

    private void joinThreads(List<Thread> threads) {
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
        }

        threads.clear();
    }

    private Drawable getDrawable(String path) {
        if (!drawableMap.containsKey(path)) {
            print.v(TAG, "create : " + path);
            drawableMap.put(path, Drawable.createFromPath(path));
        }

        return drawableMap.get(path);
    }

    private VideoPlayer addVideo(Video s) {
        VideoPlayer vplayer = new VideoPlayer(rtk, m_Root, s.x, s.y, s.w, s.h);
        //vplayer.play(App.unzipDir + s.filelist.get(0));

        print.i(TAG, "add vid " + s.x + " " + s.y + " " + s.w + " " + s.h);
        return vplayer;
    }

    private void playVideo(VideoPlayer vplayer, String path) {
        print.i(TAG, "vplayer play " + vplayer);
        vplayer.play();
    }

    private ImageView addPicture(Picture s) {
        ImageView iv = new ImageView(rtk);
        iv.setScaleType(ScaleType.FIT_XY);

        //Drawable d = Drawable.createFromPath(App.unzipDir + s.filename );

        //iv.setImageDrawable(d);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(s.w, s.h);
        params.leftMargin = s.x;
        params.topMargin = s.y;
        m_Root.addView(iv, params);
        print.i(TAG, "add pic " + s.x + " " + s.y + " " + s.w + " " + s.h);

        picList.add(iv);
        return iv;
    }

    private void drawPicture(ImageView iv, Drawable d) {
        iv.setImageDrawable(d);
    }

    private void addSubtitle(Subtitle s) {
        //		Animation animationToLeft = new TranslateAnimation(400, -400, 0, 0);
        //		animationToLeft.setDuration(12000);
        //		animationToLeft.setRepeatMode(Animation.RESTART);
        //		animationToLeft.setRepeatCount(Animation.INFINITE);

        ScrollTextView view = new ScrollTextView(rtk);

        view.setDirection(s.direction);
        if (s.direction == Direction.Static) {
            view.setGravity(Gravity.CENTER);
            view.setText(s.text);
        } else {
            view.setGravity(Gravity.CENTER_VERTICAL);
            String ttmp = s.text + "               ";
            ttmp = new String(new char[20]).replace("\0", ttmp);

            view.setText(ttmp);
        }

        view.setSingleLine(true);

        Typeface tf = App.getInstance().getFont(s.fontname);
        //view.setTypeface(App.getInstance().getFont(s.fontname)); //设置字体
        if (s.bold && s.italic) {
            print.i(TAG, "set bi");
            view.setTypeface(tf, Typeface.BOLD_ITALIC);
        } else if (s.bold) {
            print.i(TAG, "set b ");
            view.setTypeface(tf, Typeface.BOLD);
        } else if (s.italic) {
            print.i(TAG, "set  i");
            view.setTypeface(tf, Typeface.ITALIC);
        } else {
            view.setTypeface(tf, Typeface.NORMAL);
        }

        view.setTextSize((float) (s.fontsize * 1.5));
        view.setTextColor(Color.parseColor(s.fontcolor));

        view.setSpeed(s.speed);
        if (s.transparent) {
            //The alpha value for full transparency is 00 and the alpha value for no transparency is FF
            view.setBackgroundColor(Color.parseColor("#00000000"));
        } else {
            view.setBackgroundColor(Color.parseColor(s.backcolor));
        }
        //		SpannableString spanString = new SpannableString(s.text);
        //		spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
        //		spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
        //		spanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spanString.length(), 0);

		/*
		public int getPaintFlags ()
		Returns
		the flags on the Paint being used to display the text.

		setPaintFlags(int flags)
		Sets flags on the Paint being used to display the text and
		reflows the text if they are different from the old flags.

		UNDERLINE_TEXT_FLAG
		Paint flag that applies an underline decoration to drawn text.
		*/

        // Set TextView text underline
        if (s.underline) {
            view.setPaintFlags(view.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(s.w, s.h);
        params.leftMargin = s.x;
        params.topMargin = s.y;
        m_Root.addView(view, params);
        print.i(TAG, "add sub " + s.x + " " + s.y + " " + s.w + " " + s.h);
        view.startScroll();

        view.setVisibility(App.getInt("sub_onoff") != 0 ? View.VISIBLE : View.INVISIBLE);
        //view.setAnimation(animationToLeft);
        //view.setEllipsize(TextUtils.TruncateAt.END);
        //view.setText(textLeft);

        subtitleList.add(view);
    }

    private void drawBackImage(Drawable d) {
        //		Bitmap bmImg = null;
        //		try {
        //			bmImg = BitmapFactory.decodeStream(new FileInputStream(path));
        //		} catch (FileNotFoundException e) {
        //			e.printStackTrace();
        //		}
        //		BitmapDrawable background = new BitmapDrawable(bmImg);
        //		m_Root.setBackground(background);

        m_Root.setBackground(d);
    }

    //private Scene currentScene = null;
    public void enableSubtitle(boolean enable) {
        int v = enable ? View.VISIBLE : View.INVISIBLE;
        App.saveInt("sub_onoff", enable ? 1 : 0);

        for (ScrollTextView view : subtitleList) {
            view.setVisibility(v);
        }
    }

    public void distorySubtitle() {
        for (ScrollTextView view : subtitleList) {
            view.setVisibility(View.GONE);
            m_Root.removeView(view);
            view.distory();
        }
        subtitleList.clear();

        for (View view : picList) {
            view.setVisibility(View.GONE);
            m_Root.removeView(view);
        }
        picList.clear();
    }

    private void drawScene(Scene scene) {
        Log.d(TAG, "drawScene() called with: scene = [" + scene + "]");
        //rtk.stopHdmiIn();
        //rtk.currHdmiIn = null;
        distorySubtitle();

        for (Scene _s : currentAd.scene) {
            _s.drawing = false;
        }
        scene.drawing = true;

        //m_Root.removeAllViews();


        print.w(TAG, "scene thread join begin  " + nowTime());
        joinThreads(sceneThreads);
        print.w(TAG, "scene thread join end  " + nowTime());

        drawableMap.clear();

        currentScene = scene;

        int i_vid = 0;
        int i_pic = 0;
        int i_sub = 0;
        for (AdItemType itemType : scene.layers) {
            switch (itemType) {
                case Video:
                    if (!App.SERIAL_PORT) {
                        Video v = scene.video.get(i_vid);
                        if (v.filelist.size() > 0) {
                            Thread thread = new Thread(new VideoUpdater(scene.dir, addVideo(v), v, i_vid));
                            thread.start();
                            sceneThreads.add(thread);
                        } else {
                            rtk.addHDMI(scene.video.get(i_vid));
                        }
                        i_vid++;
                    } else {
                        rtk.addHDMI(scene.video.get(i_vid));
                        i_vid++;
                    }
                    break;
                case Picture:
                    Picture p = scene.picture.get(i_pic);
                    Thread thread = new Thread(new PictureUpdater(scene.dir, addPicture(p), p));
                    thread.start();
                    sceneThreads.add(thread);
                    i_pic++;
                    break;
                case Subtitle:
                    addSubtitle(scene.subtitle.get(i_sub));
                    i_sub++;
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * 启动定时切换底图线程, 定时播放场景线程
     */
    private void drawAdProgram(AdProgram ad) {
        currentAd = ad;

        print.w(TAG, "ad thread join begin  " + nowTime());
        joinThreads(adThreads);
        print.w(TAG, "ad thread join end  " + nowTime());

//		Thread backImageThread = new Thread(new BackImageUpdater());
//		backImageThread.start();
//		adThreads.add(backImageThread);

        App.saveString("last_scene", currentAd.scene.get(0).dir);

    }

    private void startSceneUpdater() {
        //解析所有scene, 启动定时线程
        threadSceneUpdater = new Thread(new SceneUpdater(currentAd));
        threadSceneUpdater.start();
        adThreads.add(threadSceneUpdater);
    }

    private void endDrawAd() {
        Log.d(TAG, "endDrawAd() called");

        if (currentAd != null) {
            for (Scene sne : currentAd.scene) {
                sne.drawing = false;
            }
        }
        rtk.stopHdmiIn();
        rtk.currHdmiIn = null;

        distorySubtitle();

        drawableMap.clear();
        //m_Root.removeAllViews();


        print.w(TAG, "sceneThreads join begin " + nowTime());
        joinThreads(sceneThreads);

        print.w(TAG, "adThreads join begin " + nowTime());
        joinThreads(adThreads);

        currentAd = null;
        currentScene = null;
    }

    public boolean isBusy() {
        return currentAd != null;
    }

    private String nowTime() {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm"); // HH:mm:ss
        String date = df.format(Calendar.getInstance().getTime());

        return date;
    }

    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        rtk.startActivity(intent);
    }

    /**
     * 切换到index 指定的场景, 切换钱关闭 场景循环播放/定时播放
     *
     * @param index
     */
    private void switchPrevNextScene(int index) {
		/*print.w(TAG, "switchPrevNextScene start  " + nowTime());

		threadSceneUpdater.interrupt();
		try {
			threadSceneUpdater.join();
		} catch (InterruptedException e) {

			e.printStackTrace();
		}

		print.w(TAG, "switchPrevNextScene end  " + nowTime());
		drawScene(currentAd.scene.get(index));	*/

        sendMessage(Msg.MESSAGE_DRAW_AD, adList.get(index));
    }

    public void sendMessage(Msg m, Object obj) {
        Message msg = adHandler.obtainMessage(m.ordinal(), obj);
        msg.sendToTarget();
        //adHandler.sendMessage(msg);
    }

    private void showTips(CharSequence cs) {

        tvTips = new TextView(rtk);
        tvTips.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        tvTips.setTextColor(Color.rgb(255, 255, 255));
        tvTips.setText(cs);
        RelativeLayout.LayoutParams paramss = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramss.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        paramss.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        //params.leftMargin = s.x;
        //params.topMargin = s.y;
        m_Root.addView(tvTips, paramss);
    }

    public void reDrawCurrentScene() {
        if (rtk.currHdmiIn != null && currentScene != null) {    //re-draw scene
            //rtk.addHDMI(rtk.currHdmiIn);
            sendMessage(Msg.MESSAGE_DRAW_SCENE, currentScene);
        }
    }

    // read all scene, parse
    public class AdJsonParser implements Runnable {
        public void run() {
            print.i(TAG, "init parse: " + App.AdDir);
            String dirs[] = App.getDirs(App.AdDir);

            for (String str : dirs) {
                print.i(TAG, "init parse: " + App.AdDir + str);
                parseJson(App.AdDir + str + "/", false);
            }

            if (dirs.length == 0) {
                sendMessage(Msg.MESSAGE_NO_DATA, "");
                return;
            }

            // read last play ad
            String last = App.getString("last_scene");
            for (AdProgram ad : adList) {
                if (ad.scene.get(0).dir.equals(last)) {
                    sendMessage(Msg.MESSAGE_DRAW_AD, ad);
                    return;
                }
            }


            sendMessage(Msg.MESSAGE_DRAW_AD, adList.get(adList.size() - 1));
        }
    }

    class BackImageUpdater implements Runnable {
        public void run() {
            if (currentAd.backImage.size() == 0) {
                return;
            }

            int i = 0;
            boolean alive = true;

            while (alive) {
                try {
                    //Drawable d = Drawable.createFromPath("App.unzipDir" + currentAd.backImage.get(i));
                    Drawable d = getDrawable("App.unzipDir" + currentAd.backImage.get(i));

                    if (d != null) {
                        sendMessage(Msg.MESSAGE_DRAW_BG, d);
                    }

                    if (++i == currentAd.backImage.size()) {
                        i = 0;
                    }

                    if (currentAd.backImageIntval > 0 && currentAd.backImage.size() > 1) {
                        print.w(TAG, "bg sleep " + currentAd.backImageIntval);
                        Thread.sleep(currentAd.backImageIntval * 1000);
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {

                    e.printStackTrace();
                    alive = false; // 收到新的 AdProgram
                }

            }
        }
    }

    class PictureUpdater implements Runnable {
        private Picture picture;
        private ImageView imageView;
        private Drawable drawable;
        private String dir;

        public PictureUpdater(String d, ImageView iv, Picture p) {
            picture = p;
            imageView = iv;
            dir = d;
        }

        public ImageView getImageView() {
            return imageView;
        }

        public Drawable getDrawableImg() {
            return drawable;
        }

        public void run() {
            int i = 0;
            boolean alive = true;

            while (alive) {
                try {
                    drawable = getDrawable(dir + picture.filelist.get(i));
                    sendMessage(Msg.MESSAGE_DRAW_IMAGE, this);
                    if (++i == picture.filelist.size()) {
                        i = 0;
                    }

                    if (picture.intval > 0 && picture.filelist.size() > 1) {
                        print.w(TAG, "img sleep " + picture.intval);
                        Thread.sleep(picture.intval * 1000);
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    alive = false; //切换场景, 该线程被 cancel
                } finally {

                }
            }
        }
    }

    class VideoUpdater implements Runnable {
        public VideoPlayer vPlayer;
        public int index;
        private Video video;
        private String dir;

        public VideoUpdater(String d, VideoPlayer vp, Video v, int i) {
            dir = d;
            video = v;
            vPlayer = vp;

            index = i;
            print.w(TAG, "VideoUpdater  " + vp);
        }

        public void run() {
            int i = 0;
            boolean alive = true;

            while (alive) {
                try {
                    vPlayer.setSource(dir + video.filelist.get(i));
                    sendMessage(Msg.MESSAGE_DRAW_VIDEO, this);
                    if (++i == video.filelist.size()) {
                        i = 0;
                    }

                    if (video.intval > 0 && video.filelist.size() > 1) {
                        print.w(TAG, "video sleep " + video.intval);
                        Thread.sleep(video.intval * 1000);
                    } else {
                        Thread.sleep(9999999);
                        //break;
                    }
                } catch (InterruptedException e) {

                    vPlayer.stop();
                    e.printStackTrace();
                    alive = false; //切换场景, 该线程被 cancel
                } finally {

                }
            }
        }
    }

    class SceneUpdater implements Runnable {
        private AdProgram ad;
        /*
        public void run() {
            if (ad.scene.size() == 1) { // 只有一个场景
                sendMessage(Msg.MESSAGE_DRAW_SCENE, ad.scene.get(0));
                return;
            }

            boolean timingOff = false;
            //			for (Scene scene : ad.scene) {
            //				timingOff |= (scene.timing.compareTo(Timing_0000) == 0);
            //			}

            boolean alive = true;
            while (alive) {
                try {
                    if (ad.sceneIntval > 0) { //循环播放优先级最高
                        loopPlayback();
                    } else {
                        if (timingOff) { //没有设置定时播放, 也没有设置循环播放, 所以播放第一个
                            print.e(TAG, "ERR  没有设置定时播放, 也没有设置循环播放, 所以播放第一个 ");
                            sendMessage(Msg.MESSAGE_DRAW_SCENE, ad.scene.get(0));
                            return;
                        }

                        if (App.tuke) { //只对时间点做 字符串比较
                            timingPlayback_tk();
                        } else { //比较当前时间点是否在2个时间点之间
                            timingPlayback();
                        }

                    }
                } catch (InterruptedException e) {

                    e.printStackTrace();
                    alive = false; // 收到新的 AdProgram
                }
            }
        }*/
        private int sleepTime = -1;
        private int sceneIndex = 0; // 轮播场景时, 下一个需要播放场景的索引
        public SceneUpdater(AdProgram ad) {
            this.ad = ad;
        }

        private void loopPlayback_tk() {
            if (sceneIndex < ad.scene.size()) {
                if (!ad.scene.get(sceneIndex).drawing) {
                    sendMessage(Msg.MESSAGE_DRAW_SCENE, ad.scene.get(sceneIndex));
                }

                print.v(TAG, "loopPlayback sleep:  " + ad.sceneIntval + " => " + sleepTime);
                sceneIndex++;
                if (sceneIndex >= ad.scene.size()) {
                    sceneIndex = 0;
                }
            }
        }

        public void run() {
            if (ad.scene.size() == 1) { // 只有一个场景
                print.v(TAG, "只有一个场景 ");
                sendMessage(Msg.MESSAGE_DRAW_SCENE, ad.scene.get(0));
            }


            boolean alive = true;
            while (alive) {
                try {
                    //print.v(TAG, "ad.sceneIntval   " + ad.sceneIntval   + " sleepTime " + sleepTime);
                    if (ad.sceneIntval > 0) { //有场景需要轮播
                        if ((sleepTime >= ad.sceneIntval) || (sleepTime == -1)) {
                            loopPlayback_tk();
                            sleepTime = 0;
                        }
                    }


                    Thread.sleep(1000);
                    sleepTime++;
                } catch (InterruptedException e) {

                    e.printStackTrace();
                    alive = false; // 收到新的 AdProgram
                }
            }
        }

        private void loopPlayback() throws InterruptedException {
            for (Scene scene : ad.scene) {
                if (!scene.drawing) {
                    sendMessage(Msg.MESSAGE_DRAW_SCENE, scene);
                }

                print.v(TAG, "loopPlayback sleep:  " + ad.sceneIntval);
                Thread.sleep(ad.sceneIntval * 1000);
            }
        }


        private void timingPlayback_tk() {
            String d = nowTime();
            for (int i = 0; i < ad.scene.size(); i++) {
                print.w(TAG, "nowTime :   " + d + " = " + ad.scene.get(i).timing);
                if ((!ad.scene.get(i).drawing) && (d.equals(ad.scene.get(i).timing))) {
                    sendMessage(Msg.MESSAGE_DRAW_SCENE, ad.scene.get(i));
                }
            }
        }

        private void timingPlayback() throws InterruptedException {
			/*
			LocalTime start = null;
			LocalTime end = null;

			//所有 scene 的 timing 必须按 00:00 - 24:00 顺序排列
			for (int i = 0; i < ad.scene.size(); i++) {
				start = timeParse(ad.scene.get(i).timing);
				print.v(TAG, "srt name:  " + ad.scene.get(i).name);

				if (i + 1 == ad.scene.size()) {
					end = timeParse("23:59");
				} else {
					end = timeParse(ad.scene.get(i + 1).timing);
					print.v(TAG, "end name:  " + ad.scene.get(i + 1).name);
				}
				boolean isWithin = isWithinInterval(start, end, timeParse(nowTime()));
				if (isWithin == true && ad.scene.get(i).drawing == false) {
					print.i(TAG, "match time, draw:  " + ad.scene.get(i).name);
					sendMessage(Msg.MESSAGE_DRAW_SCENE, ad.scene.get(i));
					break;
				}
			}

			Thread.sleep(30000);
			*/
        }

		/*
		// http://stackoverflow.com/questions/22310329/jodatime-check-if-localtime-is-after-now-and-now-before-another-localtime
		private boolean isWithinInterval(LocalTime start, LocalTime end, LocalTime time) {
			print.i(TAG, "srt-end " + start.toString() + " " + end.toString());
			if (start.isAfter(end)) {
				//return !isWithinInterval(end, start, time);
				print.i(TAG, "start after end, ERRRRRRRRRRR ");
				return false;
			}
			// This assumes you want an inclusive start and an exclusive end point.
			boolean b = start.compareTo(time) <= 0 && time.compareTo(end) < 0;

			print.i(TAG, "cur-time between " + time.toString() + " is " + b);

			return b;
		}*/
    }

    //定时播放任务检查
    class TimingAdChecker implements Runnable {
        public void run() {
            boolean alive = true;
            while (App.Alive) {
                try {
                    timingPlayback_tk();

                    Thread.sleep(5000);
                    //sleepTime++;
                } catch (InterruptedException e) {

                    e.printStackTrace();
                    alive = false;
                }
            }
        }

        private void timingPlayback_tk() throws InterruptedException {
            String d = nowTime();
            for (AdProgram ad : reversed(adList)) {        //逆序, 优先检查最后发送的
                for (int i = 0; i < ad.scene.size(); i++) {
                    //print.w(TAG, "nowTime :   " + d + " = " + ad.scene.get(i).timing);
                    if ((!ad.scene.get(i).drawing) && (d.equals(ad.scene.get(i).timing))) {
                        // stop loop playback thread
                        // TODO:

                        // sendMessage(Msg.MESSAGE_DRAW_SCENE_EX, ad);
                        Message msgx = adHandler.obtainMessage(Msg.MESSAGE_DRAW_SCENE_EX.ordinal(), ad);
                        msgx.arg1 = i;
                        msgx.sendToTarget();
                        Thread.sleep(30000);    //等待ad draw done
                        return;
                    }
                }
            }
        }

    }

    private class GpioReadThread extends Thread {
        boolean subEnable = true;

        @SuppressLint("NewApi")
        @Override
        public void run() {
            int i = 0;

            while (App.Alive) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader("/sys/kernel/debug/gpio"), 512);
                    String line = null;
                    i = 0;

                    while ((line = br.readLine()) != null) {
                        i++;
                        //System.out.println(line);
                        if (i == 5 && line.contains("32") && line.contains("lo")) {
                            subEnable = !subEnable;
                            sendMessage(Msg.MESSAGE_CHANGE_SUBTITLE_VISABLE, subEnable);
                            break;
                        } else if (i == 6 && line.contains("33") && line.contains("lo")) {
                            sendMessage(Msg.MESSAGE_PREV_SCENE, this);
                            break;
                        } else if (i == 7 && line.contains("34")) {
                            if (line.contains("lo")) {
                                sendMessage(Msg.MESSAGE_NEXT_SCENE, this);
                            }
                            break;
                        }
                    }
                    br.close();
                    Thread.sleep(80);
                } catch (FileNotFoundException e) {

                    e.printStackTrace();
                } catch (IOException e) {

                    e.printStackTrace();
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
            }
        }
    }

    public class TakeScreenshot implements Runnable {
        CommandClient cc;

        public TakeScreenshot(CommandClient obj) {
            cc = obj;
        }

        public void run() {
            takeScreenshot();
        }

        private void takeScreenshot() {
            try {

                // create bitmap screen capture
                View v1 = rtk.getWindow().getDecorView().getRootView();
                v1.setDrawingCacheEnabled(true);
                Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
                v1.setDrawingCacheEnabled(false);

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                //Base64OutputStream bout=new Base64OutputStream(bos);
                //FileOutputStream outputStream = new FileOutputStream(imageFile);
                int quality = 80;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bout);
                bitmap.recycle();

                String encodedImage = Base64.encodeToString(bout.toByteArray(), Base64.DEFAULT | Base64.NO_WRAP);
                bout.close();

                cc.sendCommand(Cmd.create(Command.screen_image, encodedImage));
                //openScreenshot(imageFile);
            } catch (Throwable e) {
                // Several error may come out with file handling or OOM
                e.printStackTrace();
            }
        }

        private void takeScreenshot3() {
            Date now = new Date();
            android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

            try {
                // image naming and path  to include sd card  appending name you choose for file
                String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

                // create bitmap screen capture
                View v1 = rtk.getWindow().getDecorView().getRootView();
                v1.setDrawingCacheEnabled(true);
                Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
                v1.setDrawingCacheEnabled(false);

                File imageFile = new File(mPath);

                FileOutputStream outputStream = new FileOutputStream(imageFile);
                int quality = 100;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                outputStream.flush();
                outputStream.close();

                openScreenshot(imageFile);
            } catch (Throwable e) {
                // Several error may come out with file handling or OOM
                e.printStackTrace();
            }
        }
    }

}
