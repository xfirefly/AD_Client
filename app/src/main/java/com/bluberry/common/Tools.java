package com.bluberry.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ParseException;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class Tools {
    private static final String tag = "Tools";
    // ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String letterChar = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String numberChar = "0123456789";
    private static byte[] base64DecodeChars = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55,
            56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46,
            47, 48, 49, 50, 51, -1, -1, -1, -1, -1};

    /**
     * blue-base64 decode
     *
     * @param str
     * @return
     */
    public static String de(String str) {
        String r = str.substring(5);
        r = r.substring(0, 1) + String.valueOf(str.charAt(1)) + r.substring(1); // 1
        // =>
        // 1
        r = r.substring(0, 3) + String.valueOf(str.charAt(0)) + r.substring(3); // 3
        // =>
        // 0
        r = r.substring(0, 8) + String.valueOf(str.charAt(3)) + r.substring(8); // 8
        // =>
        // 3
        r = r.substring(0, str.length() - 7) + String.valueOf(str.charAt(2)) + r.substring(str.length() - 7); // 7
        // =>
        // 2
        r = r.substring(0, str.length() - 5) + String.valueOf(str.charAt(4)) + r.substring(str.length() - 5); // 5
        // =>
        // 4

        try {
            return new String(decodeOrig(r));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
    }

    private static byte[] decodeOrig(String str) throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        byte[] data = str.getBytes("US-ASCII");
        int len = data.length;
        int i = 0;
        int b1, b2, b3, b4;
        while (i < len) {
            /* b1 */
            do {
                b1 = base64DecodeChars[data[i++]];
            } while (i < len && b1 == -1);
            if (b1 == -1)
                break;
            /* b2 */
            do {
                b2 = base64DecodeChars[data[i++]];
            } while (i < len && b2 == -1);
            if (b2 == -1)
                break;
            sb.append((char) ((b1 << 2) | ((b2 & 0x30) >>> 4)));
            /* b3 */
            do {
                b3 = data[i++];
                if (b3 == 61)
                    return sb.toString().getBytes("iso8859-1");
                b3 = base64DecodeChars[b3];
            } while (i < len && b3 == -1);
            if (b3 == -1)
                break;
            sb.append((char) (((b2 & 0x0f) << 4) | ((b3 & 0x3c) >>> 2)));
            /* b4 */
            do {
                b4 = data[i++];
                if (b4 == 61)
                    return sb.toString().getBytes("iso8859-1");
                b4 = base64DecodeChars[b4];
            } while (i < len && b4 == -1);
            if (b4 == -1)
                break;
            sb.append((char) (((b3 & 0x03) << 6) | b4));
        }
        return sb.toString().getBytes("iso8859-1");
    }

    public static Calendar todayCalendar() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DAY_OF_MONTH);
        print.d(tag, "today cal::  " + zeroPadding(y) + "/" + zeroPadding(m) + "/" + zeroPadding(d));

        return c;
    }

    /**
     * 返回格式为10个字符, eg 2013/01/02
     *
     * @return
     */
    public static String today() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        Date curDate = new Date(System.currentTimeMillis()); // 获取当前时间
        String date = formatter.format(curDate);

        print.d(tag, "today::  " + date);
        return date;
    }

    /**
     * 返回格式为5个字符
     *
     * @return
     */
    public static String currTime() { // 06:40
        final Calendar c = todayCalendar();

        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);

        String time = zeroPadding(h) + ":" + zeroPadding(m);

        print.w(tag, "time " + time);

        return time;
    }

    private static String zeroPadding(int i) {
        return (i < 10) ? "0" + i : "" + i;
    }

    /**
     * 根据日期得到星期几
     *
     * @param strDate 日期格式 yyyy/MM/dd
     * @return
     */
    public static String getWeek(String strDate) {

        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");// 定义日期格式
        Date date = null;
        try {
            date = format.parse(strDate);// 将字符串转换为日期
        } catch (ParseException e) {
            System.out.println("输入的日期格式不合理！");
        } catch (java.text.ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String[] weeks = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int week_index = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (week_index < 0) {
            week_index = 0;
        }

        return weeks[week_index];
    }

    /**
     * 计算字符串的MD5
     *
     * @param input
     * @return
     */
    public static String stringMD5(String input) {
        try {
            // 拿到一个MD5转换器（如果想要SHA1参数换成”SHA1”）
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");

            // 输入的字符串转换成字节数组
            byte[] inputByteArray = input.getBytes();

            // inputByteArray是输入字符串转换得到的字节数组
            messageDigest.update(inputByteArray);

            // 转换并返回结果，也是字节数组，包含16个元素
            byte[] resultByteArray = messageDigest.digest();

            // 字符数组转换成字符串返回
            return byteArrayToHex(resultByteArray);
        } catch (NoSuchAlgorithmException e) {

            return null;
        }
    }

    /**
     * 将字节数组换成成16进制的字符串
     *
     * @param byteArray
     * @return
     */
    public static String byteArrayToHex(byte[] byteArray) {
        // 首先初始化一个字符数组，用来存放每个16进制字符
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））
        char[] resultCharArray = new char[byteArray.length * 2];

        // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }

        // 字符数组组合成字符串返回
        return new String(resultCharArray);
    }

    /**
     * 根据 文件路径 计算md5
     *
     * @param inputFile
     * @return
     * @throws IOException
     */
    public static String fileMD5(String inputFile) throws IOException {
        // 缓冲区大小（这个可以抽出一个参数）
        int bufferSize = 256 * 1024;

        FileInputStream fileInputStream = null;
        DigestInputStream digestInputStream = null;

        try {
            // 拿到一个MD5转换器（同样，这里可以换成SHA1）
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");

            // 使用DigestInputStream
            fileInputStream = new FileInputStream(inputFile);
            digestInputStream = new DigestInputStream(fileInputStream, messageDigest);

            // read的过程中进行MD5处理，直到读完文件
            byte[] buffer = new byte[bufferSize];
            while (digestInputStream.read(buffer) > 0)
                ;

            // 获取最终的MessageDigest
            messageDigest = digestInputStream.getMessageDigest();
            // 拿到结果，也是字节数组，包含16个元素
            byte[] resultByteArray = messageDigest.digest();
            // 同样，把字节数组转换成字符串
            return byteArrayToHex(resultByteArray);
        } catch (NoSuchAlgorithmException e) {
            return null;

        } finally {
            try {
                digestInputStream.close();
            } catch (Exception e) {

            }

            try {
                fileInputStream.close();
            } catch (Exception e) {

            }
        }
    }

    /**
     * return sdcard path
     *
     * @return
     */
    public static String getExternalStoragePath() {
        boolean exists = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);

        if (exists)
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        else
            return null;
    }

    /**
     * 产生随机字符串
     *
     * @param length
     * @return
     */
    public static String generateString(int length) {
        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(allChar.charAt(random.nextInt(allChar.length())));
        }
        return sb.toString();
    }

    @SuppressLint("NewApi")
    public static String getCPUInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("PRODUCT ").append(android.os.Build.PRODUCT).append("\n");
        sb.append("BOARD ").append(android.os.Build.BOARD).append("\n");
        sb.append("BOOTLOADER ").append(android.os.Build.BOOTLOADER).append("\n");
        sb.append("BRAND ").append(android.os.Build.BRAND).append("\n");
        sb.append("CPU_ABI ").append(android.os.Build.CPU_ABI).append("\n");
        sb.append("CPU_ABI2 ").append(android.os.Build.CPU_ABI2).append("\n");
        sb.append("DEVICE ").append(android.os.Build.DEVICE).append("\n");
        sb.append("DISPLAY ").append(android.os.Build.DISPLAY).append("\n");
        sb.append("FINGERPRINT ").append(android.os.Build.FINGERPRINT).append("\n");
        sb.append("HARDWARE ").append(android.os.Build.HARDWARE).append("\n");
        sb.append("HOST ").append(android.os.Build.HOST).append("\n");
        sb.append("ID ").append(android.os.Build.ID).append("\n");
        sb.append("MANUFACTURER ").append(android.os.Build.MANUFACTURER).append("\n");
        sb.append("MODEL ").append(android.os.Build.MODEL).append("\n");
        sb.append("PRODUCT ").append(android.os.Build.PRODUCT).append("\n");

        sb.append("SERIAL ").append(android.os.Build.SERIAL).append("\n");
        sb.append("TAGS ").append(android.os.Build.TAGS).append("\n");
        sb.append("TIME ").append(android.os.Build.TIME).append("\n");
        sb.append("TYPE ").append(android.os.Build.TYPE).append("\n");
        sb.append("USER ").append(android.os.Build.USER).append("\n");
        sb.append("--------------------\n");

        // sb.append("CPUID ").append( SystemProperties.get("ro.hardware.cpuid",
        // "0") ).append("\n");

        if (new File("/proc/cpuinfo").exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
                String aLine;
                while ((aLine = br.readLine()) != null) {
                    sb.append(aLine + "\n");
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.w(tag, sb.toString());
        return sb.toString();
    }

    @SuppressLint("NewApi")
    public static String checkSD(Context contect) {
        // print path for debug
        StringBuilder sbBuilder = new StringBuilder();
        sbBuilder.append("Environment.getExternalStorageState:" + Environment.getExternalStorageState() + "\n");
        sbBuilder.append("Environment.isExternalStorageEmulated:" + Environment.isExternalStorageEmulated() + "\n");
        sbBuilder.append("getDataDirectory:" + Environment.getDataDirectory().getAbsolutePath() + "\n");
        sbBuilder.append("Environment.getExternalStorageDirectory:" + Environment.getExternalStorageDirectory().getAbsolutePath() + "\n");

        sbBuilder.append("context.getExternalFilesDir:" + contect.getExternalFilesDir(null) + "\n");
        sbBuilder.append("context.getFiledir:" + contect.getFilesDir().getAbsolutePath() + "\n");
        sbBuilder.append("context.getCachedir:" + contect.getCacheDir().getAbsolutePath() + "\n");
        sbBuilder.append("dataDir:" + contect.getApplicationInfo().dataDir + "\n---\n");
        print.e("CheckStore", sbBuilder.toString());

        String notice;
        // File data = Environment.getDataDirectory(); // 获得data的路径
        StatFs data_stat = new StatFs(contect.getFilesDir().getAbsolutePath()); // 创建StatFs对象
        long data_blockSize = data_stat.getBlockSize(); // 获取block的size
        float data_totalBlocks = data_stat.getBlockCount();// 获取block的个数
        int data_sizeInMb = (int) (data_blockSize * data_totalBlocks) / 1024 / 1024;// 计算总容量
        long data_availableBlocks = data_stat.getAvailableBlocks(); // 获取可用block的个数
        float data_percent = (int) (data_blockSize * data_availableBlocks) / 1024 / 1024; // 计算可用容量
        notice = contect.getFilesDir().getAbsolutePath() + "\nFLASH使用情况：\n总容量：" + data_sizeInMb + "M.\n已用:"
                + (data_sizeInMb - data_percent + "\n可用:" + data_percent + "M.");

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File filePath = Environment.getExternalStorageDirectory(); // 获得sd
            // 卡的路径

            StatFs stat = new StatFs(filePath.getPath()); // 创建StatFs对象
            long blockSize = stat.getBlockSize(); // 获取block的size
            float totalBlocks = stat.getBlockCount(); // 获取block的个数
            int sizeInMb = (int) (blockSize * totalBlocks) / 1024 / 1024; // 计算总容量
            long availableBlocks = stat.getAvailableBlocks(); // 可用block的个数
            float percent = (int) (blockSize * availableBlocks) / 1024 / 1024; // 计算可用容量

            notice = notice + "\n" + filePath.getPath() + "\nSD卡使用情况：\n总容量：" + sizeInMb + "M.\n已用："
                    + (sizeInMb - percent + "M\n可用:" + percent + "M.");
            print.i("CheckStore", notice);
            return notice;
        } else {
            notice = notice + "\nSD卡使用情况：未插入SD卡";
            return notice;
        }
    }

}
