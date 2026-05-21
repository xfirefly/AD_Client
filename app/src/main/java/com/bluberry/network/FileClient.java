package com.bluberry.network;

import com.bluberry.adclient.App;
import com.bluberry.adclient.CountingInputStream;
import com.bluberry.adclient.Msg;
import com.bluberry.common.IOUtil;
import com.bluberry.common.print;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cs.comm.Cmd;

public class FileClient implements Runnable {
    private static final String TAG = "FileClient";
    public String serverIP;
    private volatile boolean mRun = false;
    private FileType fileType;
    private long fileSize;

    public FileClient(String ip, FileType type, long len) {
        fileType = type;
        serverIP = ip;
        fileSize = len;
    }

    public static void Unzip(InputStream is, String targetDir, long isLen) {
        int BUFFER = 32 * 1024;
        String strEntry;
        int progress = 0;

        try {
            BufferedOutputStream dest = null;
            CountingInputStream cis = new CountingInputStream(is);
            ZipInputStream zis = new ZipInputStream(cis);
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                try {
                    print.d(TAG, "Unzip: " + entry);
                    int count;
                    byte[] data = new byte[BUFFER];
                    strEntry = entry.getName();

                    File entryFile = new File(targetDir + strEntry);
                    File entryDir = new File(entryFile.getParent());
                    if (!entryDir.exists()) {
                        entryDir.mkdirs();
                    }

                    FileOutputStream fos = new FileOutputStream(entryFile);
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);

                        if (isLen > 0) {
                            int progress_temp = (int) ((float) cis.getTotalBytesRead() / isLen * 100);
                            if (progress != progress_temp) {
                                progress = progress_temp;
                                App.sendMessage(Msg.MESSAGE_RECV_JSON_PROGRESS, progress);
                            }
                        }
                    }
                    dest.flush();
                    dest.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            zis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                final File to = new File(c.getAbsolutePath() + System.currentTimeMillis());
                c.renameTo(to);
                delete(to);
            }
        }
    }

    public synchronized void run() {
        mRun = true;
        Socket client = new Socket();
        InetSocketAddress isa = new InetSocketAddress(serverIP, Cmd.file_port);
        try {
            client.connect(isa, 10000);

            if (fileType == FileType.FILE_AD_ZIP) {
                App.sendMessage(Msg.MESSAGE_RECV_JSON_BEGIN, "");
                Thread.sleep(1000);

                String dir = App.getNextDir();
                Unzip(client.getInputStream(), dir, fileSize);
                client.close();

                App.sendMessage(Msg.MESSAGE_RECV_JSON_END, "");
                App.parseJson(dir, true);
            } else {
                print.d(TAG, "APK file: " + App.getApkFile());

                File f = new File(App.getApkFile());
                IOUtil.delete(f);
                IOUtil.writeStream(client.getInputStream(), new FileOutputStream(f));

                client.close();
                client = null;

                App.RunAsApp("pm install -r " + App.getApkFile());
            }

            print.d(TAG, "Done.");
        } catch (java.io.IOException e) {
            print.d(TAG, "Socket error: " + e.toString());
        } catch (InterruptedException e) {
            print.d(TAG, "Socket error: " + e.toString());
        }
    }

    public void stopClient() {
        mRun = false;
    }

    public enum FileType {
        FILE_AD_ZIP, FILE_APK,
    }
}
