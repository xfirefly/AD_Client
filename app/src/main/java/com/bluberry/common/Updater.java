package com.bluberry.common;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.bluberry.config.PkgConfig;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class Updater {
    private static final String tag = "Updater";

    public static final int ACTION_UPDATE_APK = 1; // 更新本apk
    public static final int ACTION_INSTALL_APK = 2; // 安装其他apk

    private class PkgInfo {
        public String upgradeMode;
        public String product;
        public String newVerName;
        public int newCode;
        public String swUpgradeUrl;
        public String md5;
        public String pkgName;

        public ArrayList<String> textList;

        PkgInfo() {
            upgradeMode = null;
            swUpgradeUrl = null;
            newVerName = null;
            newCode = -1;
            md5 = null;
            pkgName = null;
            textList = new ArrayList<String>();
        }
    }

    private PkgInfo mServerPkg;
    private Context mContext;
    private Handler mHandler;
    private String mXmlURL = null;
    private String mXmlTmpPath = null;
    private String mApkTmpPath = null;

    private String msgTitle = null;
    private String msgOK = null;
    private String msgCancel = null;

    private int mAction;
    private int mThreadSleep = 5000;

    public Updater(Context context, Handler handler, int action) {
        mAction = action;
        mHandler = handler;
        mContext = context;

        String tmpApkName = "tempbb.apk";

        if (action == ACTION_UPDATE_APK) {
            msgTitle = "有最新版本,可以更新至:";
            msgOK = "更新";
            msgCancel = "暂不更新";

            tmpApkName = "tempbb.apk";
            mThreadSleep = 10000;
            mXmlURL = PkgConfig.getUpdateXml() + "?eth=";
            mXmlTmpPath = mContext.getFilesDir().getAbsolutePath() + "/tempbb.xml";
        } else if (action == ACTION_INSTALL_APK) {
            msgTitle = "安装新软件, 版本: ";
            msgOK = "安装";
            msgCancel = "暂不安装";

            tmpApkName = "tempcc.apk";
            mThreadSleep = 15000;
            mXmlURL = PkgConfig.getUpdateXml() + ".ins";
            mXmlTmpPath = mContext.getFilesDir().getAbsolutePath() + "/tempcc.xml";
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            mApkTmpPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + tmpApkName;
        } else {
            mApkTmpPath = mContext.getFilesDir().getAbsolutePath() + "/" + tmpApkName;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(mThreadSleep);
                    doDownload(mXmlURL, mXmlTmpPath, "info", null);
                } catch (Exception e) {
                    // null
                }
            }
        }).start();
    }

    public Updater(Context context, Handler handler) {
        this(context, handler, ACTION_UPDATE_APK);

        Log.v(tag, "cfg name:" + mContext.getPackageName());
        Log.v(tag, "cfg code:" + getVerCode());
        Log.v(tag, "cfg ver:" + getVerName());
        Log.v(tag, "cfg id:" + PkgConfig.customer);
        Log.v(tag, "cfg xml:" + mXmlURL);
    }

    private int getVerCode() {
        int verCode = -1;
        try {
            verCode = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {

        }

        return verCode;
    }

    private String getVerName() {
        String verName = "ver";
        try {
            verName = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {

        }

        return verName;
    }

    private boolean getNewPackageInfo() {
        mServerPkg = new PkgInfo();

        DocumentBuilderFactory factory = null;
        DocumentBuilder builder = null;
        Document document = null;
        InputStream inputStream = null;

        // 首先找到xml文件
        factory = DocumentBuilderFactory.newInstance();

        try {
            // 找到xml，并加载文档
            builder = factory.newDocumentBuilder();
            inputStream = new FileInputStream(new File(mXmlTmpPath));
            document = builder.parse(inputStream);

            if (null == document) {
                print.d(tag, "builder.parse ERROR");
                return false;
            }

            // 找到根Element plist
            Element root = document.getDocumentElement();
            if (null == root) {
                print.d(tag, "document.getDocumentElement ERROR");
                return false;
            }

            // script-version node
            NodeList dictNodeList = root.getElementsByTagName("script-version");
            Element elementUpgradeMode = (Element) dictNodeList.item(0);
            mServerPkg.upgradeMode = elementUpgradeMode.getAttribute("key");

            Element elementSubdict = (Element) root.getElementsByTagName("subdict").item(0);

            Element elementProduct = (Element) elementSubdict.getElementsByTagName("product").item(0);
            mServerPkg.product = elementProduct.getAttribute("string");

            Element elementNewCode = (Element) elementSubdict.getElementsByTagName("new-Version").item(0);
            mServerPkg.newCode = Integer.parseInt(elementNewCode.getAttribute("code"));
            // packageInfo.newVerName = elementNewCode.getAttribute("string");
            mServerPkg.newVerName = String.valueOf(mServerPkg.newCode);

            NodeList personNodes = elementSubdict.getElementsByTagName("text");

            if (null != personNodes) {
                int length = personNodes.getLength();
                for (int i = 0; i < length; i++) {
                    Element personElement = (Element) personNodes.item(i);
                    mServerPkg.textList.add(personElement.getAttribute("str"));
                }
            }

            Element elementSWURL = (Element) elementSubdict.getElementsByTagName("SWURL").item(0);
            mServerPkg.swUpgradeUrl = elementSWURL.getAttribute("string");

            Element elementMD5 = (Element) elementSubdict.getElementsByTagName("md5").item(0);
            if (elementMD5 != null) {
                mServerPkg.md5 = elementMD5.getAttribute("string");
            }

            Element elementPkgName = (Element) elementSubdict.getElementsByTagName("pkgName").item(0);
            if (elementPkgName != null) {
                mServerPkg.pkgName = elementPkgName.getAttribute("string");
            }

            // if ((getVerCode() < packageInfo.newCode) &&
            // (getVerName().contains(packageInfo.product))) {

            if (mAction == ACTION_UPDATE_APK && getVerCode() < mServerPkg.newCode) {
                Log.v(tag, "cfg this:" + getVerCode() + " server:" + mServerPkg.newCode);
                Log.v(tag, "cfg this:" + getVerName() + " server:" + mServerPkg.product);

                doUpgrade();
            }

            if (mAction == ACTION_INSTALL_APK && isPackageInstalled(mServerPkg.pkgName) == false) {
                doUpgrade();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean isPackageInstalled_old(String pkg) {
        PackageManager manager;
        List<ResolveInfo> apps;

        manager = mContext.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        // mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        apps = manager.queryIntentActivities(mainIntent, 0);

        if (apps != null && pkg != null) {
            Log.v(tag, "cfg find: " + pkg);

            for (int i = 0; i < apps.size(); i++) {
                ResolveInfo ri = apps.get(i);
                if (pkg.equals(ri.activityInfo.packageName)) {
                    Log.v(tag, "cfg found " + pkg);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isPackageInstalled(String packagename) {
        PackageManager pm = mContext.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            Log.v(tag, "cfg found " + packagename);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private void doUpgrade() {
        if (getNewPackageUpgradeMode().equals("force")) {
            mHandler.post(new Runnable() {
                public void run() {
                    startForceUpgrade();
                }
            });
        } else {
            mHandler.post(new Runnable() {
                public void run() {
                    startNormalUpgrade();
                }
            });
        }
    }

    private int getNewPackageVerCode() {
        return mServerPkg.newCode;
    }

    private String getNewPackageVerName() {
        return mServerPkg.newVerName;
    }

    private String getNewPackageSwUrl() {
        return mServerPkg.swUpgradeUrl;
    }

    private String getNewPackageMD5() {
        return mServerPkg.md5;
    }

    private String getNewPackageUpgradeMode() {
        // "normal"
        // "force"

        return mServerPkg.upgradeMode;
    }

    private final static int kSystemRootStateUnknow = -1;
    private final static int kSystemRootStateDisable = 0;
    private final static int kSystemRootStateEnable = 1;
    private static int systemRootState = kSystemRootStateUnknow;

    public static boolean isRootSystem() {
        if (systemRootState == kSystemRootStateEnable) {
            return true;
        } else if (systemRootState == kSystemRootStateDisable) {

            return false;
        }

        File f = null;
        final String kSuSearchPaths[] = {"/system/bin/", "/system/xbin/", "/system/sbin/", "/sbin/", "/vendor/bin/"};
        try {
            for (int i = 0; i < kSuSearchPaths.length; i++) {
                f = new File(kSuSearchPaths[i] + "su");
                if (f != null && f.exists()) {
                    systemRootState = kSystemRootStateEnable;
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        systemRootState = kSystemRootStateDisable;
        return false;
    }

    @SuppressLint("NewApi")
    private void startNormalUpgrade() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mServerPkg.textList.size(); i++) {
            sb.append((i + 1) + ":" + mServerPkg.textList.get(i) + "\n");
        }

        Dialog dialog = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_LIGHT)
                .setTitle(msgTitle + getNewPackageVerName()).setMessage(sb.toString())
                // 设置内容
                .setNegativeButton(msgOK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadApk();
                    }
                }).setPositiveButton(msgCancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                }).create();// 创建

        dialog.show();
    }

    @SuppressLint("NewApi")
    private void startForceUpgrade() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mServerPkg.textList.size(); i++) {
            sb.append((i + 1) + ":" + mServerPkg.textList.get(i) + "\n");
        }

        Dialog dialog = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_LIGHT)
                .setTitle(msgTitle + getNewPackageVerName()).setMessage(sb.toString())
                // 设置内容
                .setNegativeButton(msgOK,// 设置确定按钮
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                downloadApk();
                            }
                        }).setPositiveButton(msgCancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // 点击"取消"按钮之后退出程序
                        dialog.cancel();
                        if (getNewPackageUpgradeMode().equals("force")) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    }
                }).create();// 创建

        dialog.show();
    }

    private void downloadApk() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    doDownload(getNewPackageSwUrl(), mApkTmpPath, "app", getNewPackageMD5());
                } catch (Exception e) {
                }
            }
        }).start();
    }

    private void update() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(new File(mApkTmpPath)), "application/vnd.android.package-archive");
            mContext.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doDownload(String addr, String savePath, String flag, String md5) {

        try {
            print.v(tag, addr);
            print.v(tag, "==>" + savePath);

            if (addr != null && download(addr, savePath, flag)) {
                Thread.sleep(2000);

                print.v(tag, "finished");
                if (md5 != null) {
                    md5 = md5.toUpperCase();
                    String md5Tmp = Tools.fileMD5(savePath);
                    print.v(tag, "server " + md5);
                    print.v(tag, " local " + md5Tmp);
                    if (md5.equals(md5Tmp) == false)
                        return;
                    else
                        print.v(tag, "chk ok");
                }

                if (flag.equals("info")) {
                    mHandler.post(new Runnable() {
                        public void run() {
                            getNewPackageInfo();
                        }
                    });
                } else if (flag.equals("app")) {
                    mHandler.post(new Runnable() {
                        public void run() {
                            update();
                        }
                    });
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean download(String url, String filePath, String flag) {
        File file = new File(filePath);
        if (file.exists())
            file.delete();
        else
            file.getParentFile().mkdirs(); // 建立上级目录

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        HttpGet get = new HttpGet(url);
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpParams httpParameters = new BasicHttpParams();
        // set the timeout in milliseconds until a connection is established
        // the default value is zero, that means the timeout is not used
        int timeoutConnection = 10000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        // set the default socket timeout (SO_TIMEOUT) in milliseconds
        // which is the timeout for waiting for data
        int timeoutSocket = 9000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

        httpclient.setParams(httpParameters);

        try {
            HttpResponse httpResp = httpclient.execute(get);
            if (HttpStatus.SC_OK == httpResp.getStatusLine().getStatusCode()) {
                HttpEntity entity = httpResp.getEntity();
                if (flag.equals("app")) {
                    writeStream(entity.getContent(), fos, entity.getContentLength());
                } else {
                    writeStream(entity.getContent(), fos, -1);
                }
                // return something, maybe?
                return true;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
        return false;
    }

    private ProgressListener listener = null;

    public interface ProgressListener {
        void onProgress(long progress);
    }

    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    /**
     * InputStream => OutputStream
     *
     * @param is
     * @param os
     * @param isLen InputStream 长度, 可显示读取百分比
     * @return
     * @throws IOException
     */
    public boolean writeStream(InputStream is, OutputStream os, long isLen) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        BufferedInputStream bufIn = new BufferedInputStream(is);
        BufferedOutputStream bufOut = new BufferedOutputStream(os);

        long totalRead = 0;
        long progress = 0;

        int len = 0;
        while ((len = bufIn.read(buffer)) != -1) {
            bufOut.write(buffer, 0, len);

            // 显示读取百分比
            if (isLen > 0) {
                totalRead += len;
                long progress_temp = totalRead * 100 / isLen;
                if (progress_temp % 1 == 0 && progress != progress_temp) {
                    progress = progress_temp;
                    //print.v(tag, "progress: " + progress);

                    if (listener != null) {
                        listener.onProgress(progress);
                    }
                }
            }

        }

        bufIn.close();
        bufOut.flush();
        bufOut.close();

        return true;
    }

}
