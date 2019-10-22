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

    /**
     * 读取 InputStream, 返回 byte数组
     *
     * @param is
     * @return
     * @throws IOException
     * @throws Exception
     */
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

    /**
     * InputStream => OutputStream
     *
     * @param is
     * @param os
     * @return
     * @throws IOException
     */
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

    /**
     * InputStream => OutputStream
     *
     * @param is
     * @param os
     * @param isLen InputStream 长度, 可显示读取百分比
     * @return
     * @throws IOException
     */
    public static boolean writeStream(InputStream is, OutputStream os, long isLen) throws IOException {
        byte[] buffer = new byte[BUFFER];
        BufferedInputStream bufIn = new BufferedInputStream(is);
        BufferedOutputStream bufOut = new BufferedOutputStream(os);

        long totalRead = 0;
        long progress = 0;

        int len = 0;
        while ((len = bufIn.read(buffer)) != -1) {
            bufOut.write(buffer, 0, len);

            //显示读取百分比
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

    /**
     * 数据解压缩
     *
     * @param is
     * @param os
     * @return
     * @throws IOException
     */
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

    /**
     * Load file content to String
     *
     * @param filePath
     * @return
     */
    public static String loadFileAsString(String filePath) {
        File file = new File(filePath);
        if (!file.exists())
            return null;

        StringBuffer fileData = new StringBuffer(1024 * 4); // 16k buffer

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
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return fileData.toString();
    }

    /**
     * save String to file
     *
     * @param filePath
     * @param data
     * @return
     */
    public static boolean saveFile(String filePath, String data) {
        File file = new File(filePath);
        if (file.exists())
            file.delete();
        else
            file.getParentFile().mkdirs(); // 建立上级目录

        boolean result = false;
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));

            out.write(data.getBytes()); // TODO add encoding ???
            out.flush();
            out.close();

            result = true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 一行一行读取文件，解决读取中文字符时出现乱码
     * <p>
     * 流的关闭顺序：先打开的后关，后打开的先关， 否则有可能出现java.io.IOException: Stream closed异常
     *
     * @throws IOException
     */
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

    /**
     * 一行一行写入文件，解决写入中文字符时出现乱码
     * <p>
     * 流的关闭顺序：先打开的后关，后打开的先关， 否则有可能出现java.io.IOException: Stream closed异常
     *
     * @throws IOException
     */
    public static void writeFile(String filePath, String str) throws IOException {
        // 写入中文字符时解决中文乱码问题
        FileOutputStream fos = new FileOutputStream(new File(filePath));
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter bw = new BufferedWriter(osw);

        bw.write(str);

        // 注意关闭的先后顺序，先打开的后关闭，后打开的先关闭
        bw.close();
        osw.close();
        fos.close();
    }

    /**
     * copy file
     *
     * @param from_file
     * @param to_file
     * @return
     */
    public static boolean copyFile(File from_file, File to_file) {
        if (!to_file.getParentFile().exists()) {
            to_file.getParentFile().mkdirs();
        }

        FileInputStream from = null; // Stream to read from source
        FileOutputStream to = null; // Stream to write to destination
        boolean ret = false;

        try {
            from = new FileInputStream(from_file); // Create input stream
            to = new FileOutputStream(to_file); // Create output stream
            byte[] buffer = new byte[4096]; // To hold file contents
            int bytes_read; // How many bytes in buffer

            // Read a chunk of bytes into the buffer, then write them out,
            // looping until we reach the end of the file (when read() returns
            // -1). Note the combination of assignment and comparison in this
            // while loop. This is a common I/O programming idiom.
            while ((bytes_read = from.read(buffer)) != -1) {
                // Read until EOF
                to.write(buffer, 0, bytes_read); // write
            }

            ret = true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            ret = false;
            e.printStackTrace();
        }
        // Always close the streams, even if exceptions were thrown
        finally {
            if (from != null)
                try {
                    from.close();
                } catch (IOException e) {
                    ;
                }
            if (to != null)
                try {
                    to.close();
                } catch (IOException e) {
                    ;
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return str;
    }

    /**
     * copy file
     *
     * @param from_file
     * @param to_file
     * @return
     */
    public static boolean copyFile(String from, String to) {
        return copyFile(new File(from), new File(to));
    }

    public static void copyAssetFile(Context context, String from, String to) {
        System.out.println("from::" + from);
        System.out.println("to::" + from);

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 解压assets的zip压缩文件到指定目录
     *
     * @param context上下文对象
     * @param assetName压缩文件名
     * @param outputDirectory输出目录
     * @param isReWrite是否覆盖
     * @throws IOException
     */
    public static void unzipAssets(Context context, String assetName, String outputDirectory, boolean isReWrite) throws IOException {
        // 创建解压目标目录
        File file = new File(outputDirectory);
        // 如果目标目录不存在，则创建
        if (!file.exists()) {
            file.mkdirs();
        }
        // 打开压缩文件
        InputStream inputStream = context.getAssets().open(assetName);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        // 读取一个进入点
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        // 使用1Mbuffer
        byte[] buffer = new byte[1024 * 1024];
        // 解压时字节计数
        int count = 0;
        // 如果进入点为空说明已经遍历完所有压缩包中文件和目录
        while (zipEntry != null) {
            // 如果是一个目录
            if (zipEntry.isDirectory()) {
                file = new File(outputDirectory + File.separator + zipEntry.getName());
                // 文件需要覆盖或者是文件不存在
                if (isReWrite || !file.exists()) {
                    file.mkdir();
                }
            } else {
                // 如果是文件
                file = new File(outputDirectory + File.separator + zipEntry.getName());
                // 文件需要覆盖或者文件不存在，则解压文件
                if (isReWrite || !file.exists()) {
                    file.createNewFile();
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    while ((count = zipInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, count);
                    }
                    fileOutputStream.close();
                }
            }
            // 定位到下一个文件入口
            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();
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
        if (!f.delete())
            System.out.println("Delete error + " + f.getName()); // throw new FileNotFoundException("Failed to delete file: " + f);
    }
}
