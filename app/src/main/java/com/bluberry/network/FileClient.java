package com.bluberry.network;

import com.bluberry.adclient.App;
import com.bluberry.adclient.CountingInputStream;
import com.bluberry.adclient.Msg;
import com.bluberry.adclient.MainActivity;
import com.bluberry.common.IOUtil;

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
        String strEntry; //保存每个zip的条目名称

        int progress = 0;

        try {
            BufferedOutputStream dest = null; //缓冲输出流
            CountingInputStream cis = new CountingInputStream(is);
            ZipInputStream zis = new ZipInputStream(cis);
            ZipEntry entry; //每个zip条目的实例

            while ((entry = zis.getNextEntry()) != null) {
                try {
                    System.out.println("Unzip:  " + entry);
                    int count;
                    byte data[] = new byte[BUFFER];
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

                        //显示读取百分比
                        if (isLen > 0) {
                            int progress_temp = (int) ((float) cis.getTotalBytesRead() / isLen * 100);
                            if (progress != progress_temp) {
                                progress = progress_temp;
                                MainActivity.thiz.sendMessage(Msg.MESSAGE_RECV_JSON_PROGRESS, progress);
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
        } catch (Exception cwj) {
            cwj.printStackTrace();
        }
    }

    public static void delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                // fix : EBUSY (Device or resource busy)
                final File to = new File(c.getAbsolutePath() + System.currentTimeMillis());
                c.renameTo(to);
                delete(to);

                //delete(c);
            }

        }
    }

    public synchronized void run() {
        mRun = true;
        Socket client = new Socket();
        InetSocketAddress isa = new InetSocketAddress(serverIP, Cmd.file_port);
        try {
            client.connect(isa, 10000);
            //BufferedOutputStream out = new BufferedOutputStream(client.getOutputStream());
            // 送出字串
            //out.write("Send From Client ".getBytes());
            //out.flush();
            //out.close();
            //out = null;
            //File f = new File("C:/hellocc");

            //String fn = getUniqueFileName(zipFile, "zip");
            if (fileType == FileType.FILE_AD_ZIP) {
                //				File f = new File(App.zipFile);
                //				IOUtil.delete(f);
                //				IOUtil.writeStream(client.getInputStream(), new FileOutputStream(f));
                //				client.close();
                //				client = null;
                //				RTKSourceInActivity.thiz.unzipAdFile();

                MainActivity.thiz.sendMessage(Msg.MESSAGE_RECV_JSON_BEGIN, "");
                Thread.sleep(1000);    //wait player stop

                //File unzip = new File(App.unzipDir);
                //IOUtil.delete(new File(App.unzipTmp));
                //unzip.renameTo(new File(App.unzipTmp)); //当前使用的

                //Unzip(client.getInputStream(), App.unzipDir, fileSize);
                String dir = App.getNextDir();
                Unzip(client.getInputStream(), dir, fileSize);
                client.close();

                MainActivity.thiz.sendMessage(Msg.MESSAGE_RECV_JSON_END, "");
                MainActivity.thiz.parseJson(dir, true);
            } else {
                System.out.println(App.apkFile);

                File f = new File(App.apkFile);
                IOUtil.delete(f);
                IOUtil.writeStream(client.getInputStream(), new FileOutputStream(f));

                client.close();
                client = null;

                App.RunAsApp("pm install -r " + App.apkFile);
            }

            System.out.println("Done..");
        } catch (java.io.IOException e) {
            System.out.println("Socket連線有問題 !");
            System.out.println("IOException :" + e.toString());
        } catch (InterruptedException e) {
            System.out.println("Socket連線有問題 !");
            System.out.println("IOException :" + e.toString());
        }
    }

    public void stopClient() {
        mRun = false;
    }

    public enum FileType {
        FILE_AD_ZIP, FILE_APK,
    }
}
