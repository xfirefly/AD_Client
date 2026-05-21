package com.bluberry.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.bluberry.config.PkgConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Updater {
    public static final int ACTION_UPDATE_APK = 1;
    public static final int ACTION_INSTALL_APK = 2;
    private static final String tag = "Updater";
    private final static int kSystemRootStateUnknow = -1;
    private final static int kSystemRootStateDisable = 0;
    private final static int kSystemRootStateEnable = 1;
    private static int systemRootState = kSystemRootStateUnknow;
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
    private ProgressListener listener = null;

    public Updater(Context context, Handler handler, int action) {
        mAction = action;
        mHandler = handler;
        mContext = context;

        String tmpApkName = "tempbb.apk";

        if (action == ACTION_UPDATE_APK) {
            msgTitle = "Update available: ";
            msgOK = "Update";
            msgCancel = "Later";
            tmpApkName = "tempbb.apk";
            mThreadSleep = 10000;
            mXmlURL = PkgConfig.getUpdateXml() + "?eth=";
            mXmlTmpPath = mContext.getFilesDir().getAbsolutePath() + "/tempbb.xml";
        } else if (action == ACTION_INSTALL_APK) {
            msgTitle = "Install new software, version: ";
            msgOK = "Install";
            msgCancel = "Later";
            tmpApkName = "tempcc.apk";
            mThreadSleep = 15000;
            mXmlURL = PkgConfig.getUpdateXml() + ".ins";
            mXmlTmpPath = mContext.getFilesDir().getAbsolutePath() + "/tempcc.xml";
        }

        mApkTmpPath = mContext.getExternalFilesDir(null).getAbsolutePath() + "/" + tmpApkName;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(mThreadSleep);
                    doDownload(mXmlURL, mXmlTmpPath, "info", null);
                } catch (Exception e) {
                    // ignored
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

    public static boolean isRootSystem() {
        if (systemRootState == kSystemRootStateEnable) {
            return true;
        } else if (systemRootState == kSystemRootStateDisable) {
            return false;
        }

        File f;
        final String[] kSuSearchPaths = {"/system/bin/", "/system/xbin/", "/system/sbin/", "/sbin/", "/vendor/bin/"};
        try {
            for (int i = 0; i < kSuSearchPaths.length; i++) {
                f = new File(kSuSearchPaths[i] + "su");
                if (f.exists()) {
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

    private int getVerCode() {
        int verCode = -1;
        try {
            verCode = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // ignored
        }
        return verCode;
    }

    private String getVerName() {
        String verName = "ver";
        try {
            verName = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // ignored
        }
        return verName;
    }

    private boolean getNewPackageInfo() {
        mServerPkg = new PkgInfo();
        InputStream inputStream = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            inputStream = new FileInputStream(new File(mXmlTmpPath));
            Document document = builder.parse(inputStream);

            if (document == null) {
                print.d(tag, "builder.parse ERROR");
                return false;
            }

            Element root = document.getDocumentElement();
            if (root == null) {
                print.d(tag, "document.getDocumentElement ERROR");
                return false;
            }

            NodeList dictNodeList = root.getElementsByTagName("script-version");
            Element elementUpgradeMode = (Element) dictNodeList.item(0);
            mServerPkg.upgradeMode = elementUpgradeMode.getAttribute("key");

            Element elementSubdict = (Element) root.getElementsByTagName("subdict").item(0);

            Element elementProduct = (Element) elementSubdict.getElementsByTagName("product").item(0);
            mServerPkg.product = elementProduct.getAttribute("string");

            Element elementNewCode = (Element) elementSubdict.getElementsByTagName("new-Version").item(0);
            mServerPkg.newCode = Integer.parseInt(elementNewCode.getAttribute("code"));
            mServerPkg.newVerName = String.valueOf(mServerPkg.newCode);

            NodeList personNodes = elementSubdict.getElementsByTagName("text");
            if (personNodes != null) {
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

            if (mAction == ACTION_UPDATE_APK && getVerCode() < mServerPkg.newCode) {
                Log.v(tag, "cfg this:" + getVerCode() + " server:" + mServerPkg.newCode);
                Log.v(tag, "cfg this:" + getVerName() + " server:" + mServerPkg.product);
                doUpgrade();
            }

            if (mAction == ACTION_INSTALL_APK && !isPackageInstalled(mServerPkg.pkgName)) {
                doUpgrade();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean isPackageInstalled(String packagename) {
        try {
            mContext.getPackageManager().getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            Log.v(tag, "cfg found " + packagename);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
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
        return mServerPkg.upgradeMode;
    }

    private void startNormalUpgrade() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mServerPkg.textList.size(); i++) {
            sb.append((i + 1) + ":" + mServerPkg.textList.get(i) + "\n");
        }

        Dialog dialog = new AlertDialog.Builder(mContext)
                .setTitle(msgTitle + getNewPackageVerName())
                .setMessage(sb.toString())
                .setNegativeButton(msgOK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadApk();
                    }
                })
                .setPositiveButton(msgCancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                })
                .create();
        dialog.show();
    }

    private void startForceUpgrade() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mServerPkg.textList.size(); i++) {
            sb.append((i + 1) + ":" + mServerPkg.textList.get(i) + "\n");
        }

        Dialog dialog = new AlertDialog.Builder(mContext)
                .setTitle(msgTitle + getNewPackageVerName())
                .setMessage(sb.toString())
                .setNegativeButton(msgOK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadApk();
                    }
                })
                .setPositiveButton(msgCancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                        if (getNewPackageUpgradeMode().equals("force")) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    }
                })
                .create();
        dialog.show();
    }

    private void downloadApk() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    doDownload(getNewPackageSwUrl(), mApkTmpPath, "app", getNewPackageMD5());
                } catch (Exception e) {
                    // ignored
                }
            }
        }).start();
    }

    private void update() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            File apkFile = new File(mApkTmpPath);
            Uri uri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".fileprovider", apkFile);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
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
                    if (!md5.equals(md5Tmp))
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
            file.getParentFile().mkdirs();

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(9000);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(file);
                if (flag.equals("app")) {
                    writeStream(is, fos, conn.getContentLengthLong());
                } else {
                    writeStream(is, fos, -1);
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }
        return false;
    }

    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    public boolean writeStream(InputStream is, OutputStream os, long isLen) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        BufferedInputStream bufIn = new BufferedInputStream(is);
        BufferedOutputStream bufOut = new BufferedOutputStream(os);

        long totalRead = 0;
        long progress = 0;

        int len;
        while ((len = bufIn.read(buffer)) != -1) {
            bufOut.write(buffer, 0, len);

            if (isLen > 0) {
                totalRead += len;
                long progress_temp = totalRead * 100 / isLen;
                if (progress_temp % 1 == 0 && progress != progress_temp) {
                    progress = progress_temp;
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

    public interface ProgressListener {
        void onProgress(long progress);
    }

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
            textList = new ArrayList<>();
        }
    }
}
