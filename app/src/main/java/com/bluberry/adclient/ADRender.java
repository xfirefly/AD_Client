package com.bluberry.adclient;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
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

import androidx.core.content.FileProvider;

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
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private MainActivity rtk;
    private ViewGroup m_Root;
    private String TAG = "ADR";
    private List<AdProgram> adList = new ArrayList<>();
    private AdProgram currentAd = null;
    private Scene currentScene = null;

    private MyBroadcastReceiver usbReceiver;

    private GpioReadThread gpioThread;

    private List<ScrollTextView> subtitleList = new ArrayList<>();
    private List<View> picList = new ArrayList<>();
    private Thread threadSceneUpdater;

    private List<Thread> appThreads = new ArrayList<>();
    private List<Thread> adThreads = new ArrayList<>();
    private List<Thread> sceneThreads = new ArrayList<>();

    private HashMap<String, Drawable> drawableMap = new HashMap<>();
    private TextView tvTips = null;

    public Handler adHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            List<Scene> scList = new ArrayList<>();
            List<AdProgram> scadList = new ArrayList<>();

            Msg usermsg = Msg.values()[msg.what];
            print.i(TAG, "handle msg " + usermsg.toString() + " ");
            switch (usermsg) {
                case MESSAGE_PARSE_AD_DONE:
                    break;

                case MESSAGE_DRAW_AD:
                    tvTips = null;
                    endDrawAd();
                    drawAdProgram((AdProgram) msg.obj);
                    startSceneUpdater();
                    break;

                case MESSAGE_DRAW_SCENE_EX:
                    tvTips = null;
                    endDrawAd();
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
                    break;

                case MESSAGE_STOP_AND_RENAME:
                    break;

                case MESSAGE_RECV_JSON_BEGIN:
                    Toast.makeText(rtk, R.string.wait, Toast.LENGTH_LONG).show();
                    break;

                case MESSAGE_RECV_JSON_PROGRESS:
                    if (tvTips != null) {
                        tvTips.setText("Receiving files, please wait...(" + msg.obj + "%)");
                    }
                    break;

                case MESSAGE_RECV_JSON_END:
                    if (tvTips != null) {
                        tvTips.setText("Parsing files, please wait...");
                    }
                    break;

                case MESSAGE_NO_DATA:
                    endDrawAd();
                    showTips(App.getInstance().getResources().getText(R.string.no_data));
                    sendEmptyMessageDelayed(Msg.MESSAGE_ADD_HDMI.ordinal(), 10);
                    break;

                case MESSAGE_ADD_HDMI:
                    break;

                case MESSAGE_PREV_SCENE:
                    for (int i = 0; i < adList.size(); i++) {
                        for (Scene sc : adList.get(i).scene) {
                            scList.add(sc);
                            scadList.add(adList.get(i));
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
        App.setMessageHandler(adHandler);
        App.setAdRender(this);

        Thread ipReceiverThread = new Thread(IpReceiver.getInstance());
        ipReceiverThread.start();
        appThreads.add(ipReceiverThread);

        Thread jsonParserThread = new Thread(new AdJsonParser());
        jsonParserThread.start();

        usbReceiver = new MyBroadcastReceiver();
        usbReceiver.registe();

        if (App.getString("name").equals("0")) {
            App.saveString("name", "unnamed");
        }

        if (App.getString("id").equals("0")) {
            App.saveString("id", "1");
            App.saveInt("sub_onoff", 1);
        }

        Thread timingAdChecker = new Thread(new TimingAdChecker());
        timingAdChecker.start();
    }

    public void unInit() {
        App.Alive = false;

        usbReceiver.unRegiste();
        IpReceiver.getInstance().Close();

        endDrawAd();

        print.w(TAG, "appThreads join begin " + nowTime());
        joinThreads(appThreads);

        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public synchronized AdProgram parseJson(String dir, boolean add) {
        print.w(TAG, "parseJson: " + dir);
        AdProgram ad = null;
        String json = IOUtil.loadFileAsString(dir + "config.json");
        if (json == null) {
            sendMessage(Msg.MESSAGE_NO_DATA, "");
            return ad;
        }

        try {
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

            adList.add(ad);

            if (add && ad.scene.get(0).timing.equals(Timing_0000)) {
                sendMessage(Msg.MESSAGE_DRAW_AD, ad);
            }

        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ad;
    }

    private void removeOtherScene__(String dir) {
        print.i(TAG, "keep scene: " + dir);
        for (String str : App.getDirs(App.getAdDir())) {
            String tmp = App.getAdDir() + str + "/";
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
            view.setBackgroundColor(Color.parseColor("#00000000"));
        } else {
            view.setBackgroundColor(Color.parseColor(s.backcolor));
        }

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

        subtitleList.add(view);
    }

    private void drawBackImage(Drawable d) {
        m_Root.setBackground(d);
    }

    public void enableSubtitle(boolean enable) {
        int v = enable ? View.VISIBLE : View.INVISIBLE;
        App.saveInt("sub_onoff", enable ? 1 : 0);

        for (ScrollTextView view : subtitleList) {
            view.setVisibility(v);
        }
    }

    public void destroySubtitle() {
        for (ScrollTextView view : subtitleList) {
            view.setVisibility(View.GONE);
            m_Root.removeView(view);
            view.destroy();
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
        destroySubtitle();

        for (Scene _s : currentAd.scene) {
            _s.drawing = false;
        }
        scene.drawing = true;

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
                    {
                        Video v = scene.video.get(i_vid);
                        if (v.filelist.size() > 0) {
                            Thread thread = new Thread(new VideoUpdater(scene.dir, addVideo(v), v, i_vid));
                            thread.start();
                            sceneThreads.add(thread);
                        } else {
                            rtk.addHDMI(scene.video.get(i_vid));
                        }
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

    private void drawAdProgram(AdProgram ad) {
        currentAd = ad;

        print.w(TAG, "ad thread join begin  " + nowTime());
        joinThreads(adThreads);
        print.w(TAG, "ad thread join end  " + nowTime());

        App.saveString("last_scene", currentAd.scene.get(0).dir);
    }

    private void startSceneUpdater() {
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

        destroySubtitle();

        drawableMap.clear();

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
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        return df.format(Calendar.getInstance().getTime());
    }

    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Uri uri = FileProvider.getUriForFile(rtk, rtk.getPackageName() + ".fileprovider", imageFile);
        intent.setDataAndType(uri, "image/*");
        rtk.startActivity(intent);
    }

    private void switchPrevNextScene(int index) {
        sendMessage(Msg.MESSAGE_DRAW_AD, adList.get(index));
    }

    public void sendMessage(Msg m, Object obj) {
        Message msg = adHandler.obtainMessage(m.ordinal(), obj);
        msg.sendToTarget();
    }

    private void showTips(CharSequence cs) {
        tvTips = new TextView(rtk);
        tvTips.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        tvTips.setTextColor(Color.rgb(255, 255, 255));
        tvTips.setText(cs);
        RelativeLayout.LayoutParams paramss = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramss.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        paramss.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        m_Root.addView(tvTips, paramss);
    }

    public void reDrawCurrentScene() {
        if (rtk.currHdmiIn != null && currentScene != null) {
            sendMessage(Msg.MESSAGE_DRAW_SCENE, currentScene);
        }
    }

    public class AdJsonParser implements Runnable {
        public void run() {
            print.i(TAG, "init parse: " + App.getAdDir());
            String[] dirs = App.getDirs(App.getAdDir());

            for (String str : dirs) {
                print.i(TAG, "init parse: " + App.getAdDir() + str);
                parseJson(App.getAdDir() + str + "/", false);
            }

            if (dirs.length == 0) {
                sendMessage(Msg.MESSAGE_NO_DATA, "");
                return;
            }

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
                    alive = false;
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
                    alive = false;
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
                    }
                } catch (InterruptedException e) {
                    vPlayer.stop();
                    e.printStackTrace();
                    alive = false;
                }
            }
        }
    }

    class SceneUpdater implements Runnable {
        private AdProgram ad;
        private int sleepTime = -1;
        private int sceneIndex = 0;

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
            if (ad.scene.size() == 1) {
                print.v(TAG, "Single scene");
                sendMessage(Msg.MESSAGE_DRAW_SCENE, ad.scene.get(0));
            }

            boolean alive = true;
            while (alive) {
                try {
                    if (ad.sceneIntval > 0) {
                        if ((sleepTime >= ad.sceneIntval) || (sleepTime == -1)) {
                            loopPlayback_tk();
                            sleepTime = 0;
                        }
                    }

                    Thread.sleep(1000);
                    sleepTime++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    alive = false;
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
            // Timing playback logic placeholder
        }
    }

    class TimingAdChecker implements Runnable {
        public void run() {
            boolean alive = true;
            while (App.Alive) {
                try {
                    timingPlayback_tk();
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    alive = false;
                }
            }
        }

        private void timingPlayback_tk() throws InterruptedException {
            String d = nowTime();
            for (AdProgram ad : reversed(adList)) {
                for (int i = 0; i < ad.scene.size(); i++) {
                    if ((!ad.scene.get(i).drawing) && (d.equals(ad.scene.get(i).timing))) {
                        Message msgx = adHandler.obtainMessage(Msg.MESSAGE_DRAW_SCENE_EX.ordinal(), ad);
                        msgx.arg1 = i;
                        msgx.sendToTarget();
                        Thread.sleep(30000);
                        return;
                    }
                }
            }
        }
    }

    private class GpioReadThread extends Thread {
        boolean subEnable = true;

        @Override
        public void run() {
            int i = 0;

            while (App.Alive) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader("/sys/kernel/debug/gpio"), 512);
                    String line;
                    i = 0;

                    while ((line = br.readLine()) != null) {
                        i++;
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
                View v1 = rtk.getWindow().getDecorView().getRootView();
                Bitmap bitmap = Bitmap.createBitmap(v1.getWidth(), v1.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                v1.draw(canvas);

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                int quality = 80;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bout);
                bitmap.recycle();

                String encodedImage = Base64.encodeToString(bout.toByteArray(), Base64.DEFAULT | Base64.NO_WRAP);
                bout.close();

                cc.sendCommand(Cmd.create(Command.screen_image, encodedImage));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
