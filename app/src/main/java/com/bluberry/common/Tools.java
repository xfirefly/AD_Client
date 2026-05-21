package com.bluberry.common;

import android.content.Context;
import android.os.Build;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class Tools {
    private static final String tag = "Tools";
    private static final String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static byte[] base64DecodeChars = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55,
            56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46,
            47, 48, 49, 50, 51, -1, -1, -1, -1, -1};

    public static String de(String str) {
        String r = str.substring(5);
        r = r.substring(0, 1) + String.valueOf(str.charAt(1)) + r.substring(1);
        r = r.substring(0, 3) + String.valueOf(str.charAt(0)) + r.substring(3);
        r = r.substring(0, 8) + String.valueOf(str.charAt(3)) + r.substring(8);
        r = r.substring(0, str.length() - 7) + String.valueOf(str.charAt(2)) + r.substring(str.length() - 7);
        r = r.substring(0, str.length() - 5) + String.valueOf(str.charAt(4)) + r.substring(str.length() - 5);

        try {
            return new String(decodeOrig(r));
        } catch (UnsupportedEncodingException e) {
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
            do {
                b1 = base64DecodeChars[data[i++]];
            } while (i < len && b1 == -1);
            if (b1 == -1) break;
            do {
                b2 = base64DecodeChars[data[i++]];
            } while (i < len && b2 == -1);
            if (b2 == -1) break;
            sb.append((char) ((b1 << 2) | ((b2 & 0x30) >>> 4)));
            do {
                b3 = data[i++];
                if (b3 == 61) return sb.toString().getBytes("iso8859-1");
                b3 = base64DecodeChars[b3];
            } while (i < len && b3 == -1);
            if (b3 == -1) break;
            sb.append((char) (((b2 & 0x0f) << 4) | ((b3 & 0x3c) >>> 2)));
            do {
                b4 = data[i++];
                if (b4 == 61) return sb.toString().getBytes("iso8859-1");
                b4 = base64DecodeChars[b4];
            } while (i < len && b4 == -1);
            if (b4 == -1) break;
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

    public static String today() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        Date curDate = new Date(System.currentTimeMillis());
        String date = formatter.format(curDate);
        print.d(tag, "today::  " + date);
        return date;
    }

    public static String currTime() {
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

    public static String getWeek(String strDate) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
        Date date = null;
        try {
            date = format.parse(strDate);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }

        String[] weeks = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int week_index = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (week_index < 0) {
            week_index = 0;
        }

        return weeks[week_index];
    }

    public static String stringMD5(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] inputByteArray = input.getBytes();
            messageDigest.update(inputByteArray);
            byte[] resultByteArray = messageDigest.digest();
            return byteArrayToHex(resultByteArray);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static String byteArrayToHex(byte[] byteArray) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] resultCharArray = new char[byteArray.length * 2];
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        return new String(resultCharArray);
    }

    public static String fileMD5(String inputFile) throws IOException {
        int bufferSize = 256 * 1024;
        FileInputStream fileInputStream = null;
        DigestInputStream digestInputStream = null;

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(inputFile);
            digestInputStream = new DigestInputStream(fileInputStream, messageDigest);

            byte[] buffer = new byte[bufferSize];
            while (digestInputStream.read(buffer) > 0);

            messageDigest = digestInputStream.getMessageDigest();
            byte[] resultByteArray = messageDigest.digest();
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

    public static String getExternalStoragePath() {
        boolean exists = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (exists)
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        else
            return null;
    }

    public static String generateString(int length) {
        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(allChar.charAt(random.nextInt(allChar.length())));
        }
        return sb.toString();
    }

    public static String getCPUInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("PRODUCT ").append(Build.PRODUCT).append("\n");
        sb.append("BOARD ").append(Build.BOARD).append("\n");
        sb.append("BOOTLOADER ").append(Build.BOOTLOADER).append("\n");
        sb.append("BRAND ").append(Build.BRAND).append("\n");
        sb.append("SUPPORTED_ABIS ").append(Arrays.toString(Build.SUPPORTED_ABIS)).append("\n");
        sb.append("DEVICE ").append(Build.DEVICE).append("\n");
        sb.append("DISPLAY ").append(Build.DISPLAY).append("\n");
        sb.append("FINGERPRINT ").append(Build.FINGERPRINT).append("\n");
        sb.append("HARDWARE ").append(Build.HARDWARE).append("\n");
        sb.append("HOST ").append(Build.HOST).append("\n");
        sb.append("ID ").append(Build.ID).append("\n");
        sb.append("MANUFACTURER ").append(Build.MANUFACTURER).append("\n");
        sb.append("MODEL ").append(Build.MODEL).append("\n");
        sb.append("PRODUCT ").append(Build.PRODUCT).append("\n");
        sb.append("FINGERPRINT ").append(Build.FINGERPRINT).append("\n");
        sb.append("TAGS ").append(Build.TAGS).append("\n");
        sb.append("TIME ").append(Build.TIME).append("\n");
        sb.append("TYPE ").append(Build.TYPE).append("\n");
        sb.append("USER ").append(Build.USER).append("\n");
        sb.append("--------------------\n");

        if (new File("/proc/cpuinfo").exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
                String aLine;
                while ((aLine = br.readLine()) != null) {
                    sb.append(aLine).append("\n");
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.w(tag, sb.toString());
        return sb.toString();
    }

    public static String checkSD(Context context) {
        StringBuilder sbBuilder = new StringBuilder();
        sbBuilder.append("Environment.getExternalStorageState:").append(Environment.getExternalStorageState()).append("\n");
        sbBuilder.append("Environment.isExternalStorageEmulated:").append(Environment.isExternalStorageEmulated()).append("\n");
        sbBuilder.append("getDataDirectory:").append(Environment.getDataDirectory().getAbsolutePath()).append("\n");
        sbBuilder.append("Environment.getExternalStorageDirectory:").append(Environment.getExternalStorageDirectory().getAbsolutePath()).append("\n");
        sbBuilder.append("context.getExternalFilesDir:").append(context.getExternalFilesDir(null)).append("\n");
        sbBuilder.append("context.getFiledir:").append(context.getFilesDir().getAbsolutePath()).append("\n");
        sbBuilder.append("context.getCachedir:").append(context.getCacheDir().getAbsolutePath()).append("\n");
        sbBuilder.append("dataDir:").append(context.getApplicationInfo().dataDir).append("\n---\n");
        print.e("CheckStore", sbBuilder.toString());

        String notice;
        StatFs data_stat = new StatFs(context.getFilesDir().getAbsolutePath());
        long data_blockSize = data_stat.getBlockSizeLong();
        long data_totalBlocks = data_stat.getBlockCountLong();
        long data_sizeInMb = (data_blockSize * data_totalBlocks) / 1024 / 1024;
        long data_availableBlocks = data_stat.getAvailableBlocksLong();
        long data_percent = (data_blockSize * data_availableBlocks) / 1024 / 1024;
        notice = context.getFilesDir().getAbsolutePath() + "\nFLASH: Total:" + data_sizeInMb + "M. Used:"
                + (data_sizeInMb - data_percent) + "M Available:" + data_percent + "M.";

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File filePath = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(filePath.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long sizeInMb = (blockSize * totalBlocks) / 1024 / 1024;
            long availableBlocks = stat.getAvailableBlocksLong();
            long percent = (blockSize * availableBlocks) / 1024 / 1024;

            notice = notice + "\n" + filePath.getPath() + "\nSD Card: Total:" + sizeInMb + "M. Used:"
                    + (sizeInMb - percent) + "M Available:" + percent + "M.";
            print.i("CheckStore", notice);
            return notice;
        } else {
            notice = notice + "\nSD Card: Not inserted";
            return notice;
        }
    }
}
