package com.bluberry.common;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class IOUtil {
    private static final String tag = "IOUtil";
    private static final int BUFFER = 8 * 1024;

    public static byte[] readStream(InputStream is) throws IOException {
        byte[] buffer = new byte[BUFFER];
        BufferedInputStream bufIn = new BufferedInputStream(is);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int len = 0;
        while ((len = bufIn.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }

        bufIn.close();
        bos.flush();
        bos.close();

        return bos.toByteArray();
    }

    public static boolean writeStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[BUFFER];
        BufferedInputStream bufIn = new BufferedInputStream(is);
        BufferedOutputStream bufOut = new BufferedOutputStream(os);

        int len = 0;
        while ((len = bufIn.read(buffer)) != -1) {
            bufOut.write(buffer, 0, len);
        }

        bufIn.close();
        bufOut.flush();
        bufOut.close();
        print.i(tag, "writeStream done");
        return true;
    }

    public static boolean writeStream(InputStream is, OutputStream os, long isLen) throws IOException {
        byte[] buffer = new byte[BUFFER];
        BufferedInputStream bufIn = new BufferedInputStream(is);
        BufferedOutputStream bufOut = new BufferedOutputStream(os);

        long totalRead = 0;
        long progress = 0;

        int len = 0;
        while ((len = bufIn.read(buffer)) != -1) {
            bufOut.write(buffer, 0, len);

            if (isLen > 0) {
                totalRead += len;
                long progress_temp = totalRead * 100 / isLen;
                if (progress_temp % 10 == 0 && progress != progress_temp) {
                    progress = progress_temp;
                    print.v(tag, "progress: " + progress);
                }
            }
        }

        bufIn.close();
        bufOut.flush();
        bufOut.close();

        return true;
    }

    public static boolean decompressGZIP(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[BUFFER];
        GZIPInputStream zis = new GZIPInputStream(new BufferedInputStream(is));
        BufferedOutputStream bufOut = new BufferedOutputStream(os);

        int len = 0;
        while ((len = zis.read(buffer)) != -1) {
            bufOut.write(buffer, 0, len);
        }

        zis.close();
        bufOut.flush();
        bufOut.close();

        return true;
    }

    public static String loadFileAsString(String filePath) {
        File file = new File(filePath);
        if (!file.exists())
            return null;

        StringBuffer fileData = new StringBuffer(1024 * 4);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            char[] buf = new char[BUFFER];
            int numRead = 0;

            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return fileData.toString();
    }

    public static boolean saveFile(String filePath, String data) {
        File file = new File(filePath);
        if (file.exists())
            file.delete();
        else
            file.getParentFile().mkdirs();

        boolean result = false;
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(data.getBytes());
            out.flush();
            out.close();
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void readFile(String filePath, ArrayList<String> list) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        BufferedReader br = new BufferedReader(isr);

        String line = "";
        while ((line = br.readLine()) != null) {
            list.add(line);
        }
        br.close();
        isr.close();
        fis.close();
    }

    public static void writeFile(String filePath, String str) throws IOException {
        FileOutputStream fos = new FileOutputStream(new File(filePath));
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter bw = new BufferedWriter(osw);

        bw.write(str);

        bw.close();
        osw.close();
        fos.close();
    }

    public static boolean copyFile(File from_file, File to_file) {
        if (!to_file.getParentFile().exists()) {
            to_file.getParentFile().mkdirs();
        }

        FileInputStream from = null;
        FileOutputStream to = null;
        boolean ret = false;

        try {
            from = new FileInputStream(from_file);
            to = new FileOutputStream(to_file);
            byte[] buffer = new byte[4096];
            int bytes_read;

            while ((bytes_read = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytes_read);
            }

            ret = true;
        } catch (Exception e) {
            ret = false;
            e.printStackTrace();
        } finally {
            if (from != null)
                try {
                    from.close();
                } catch (IOException e) {
                }
            if (to != null)
                try {
                    to.close();
                } catch (IOException e) {
                }
        }

        return ret;
    }

    public static String readStreamContent(InputStream is) {
        if (is == null) {
            return null;
        }

        String str = null;
        try {
            byte[] data = IOUtil.readStream(is);
            str = new String(data, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return str;
    }

    public static boolean copyFile(String from, String to) {
        return copyFile(new File(from), new File(to));
    }

    public static void copyAssetFile(Context context, String from, String to) {
        print.d(tag, "from::" + from);
        print.d(tag, "to::" + to);

        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open(from);
            FileOutputStream fos = new FileOutputStream(to);

            byte[] buffer = new byte[4096];
            int count = 0;
            while ((count = is.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
            }

            fos.flush();
            fos.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void unzipAssets(Context context, String assetName, String outputDirectory, boolean isReWrite) throws IOException {
        File file = new File(outputDirectory);
        if (!file.exists()) {
            file.mkdirs();
        }

        InputStream inputStream = context.getAssets().open(assetName);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        byte[] buffer = new byte[1024 * 1024];
        int count = 0;

        while (zipEntry != null) {
            if (zipEntry.isDirectory()) {
                file = new File(outputDirectory + File.separator + zipEntry.getName());
                if (isReWrite || !file.exists()) {
                    file.mkdir();
                }
            } else {
                file = new File(outputDirectory + File.separator + zipEntry.getName());
                if (isReWrite || !file.exists()) {
                    file.createNewFile();
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    while ((count = zipInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, count);
                    }
                    fileOutputStream.close();
                }
            }
            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();
    }

    public static void delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                final File to = new File(c.getAbsolutePath() + System.currentTimeMillis());
                c.renameTo(to);
                delete(to);
            }
        }
        if (!f.delete())
            print.e(tag, "Delete error: " + f.getName());
    }
}
