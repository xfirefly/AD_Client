package com.bluberry.common;

import android.util.Log;

/**
 * 日志记录模块
 */
public class print {

    /**
     * 调试日志类型
     */
    private static final int DEBUG = 111;
    /**
     * 错误日志类型
     */
    private static final int ERROR = 112;
    /**
     * 信息日志类型
     */
    private static final int INFO = 113;

    /**** 5中Log日志类型 *******/
    /**
     * 详细信息日志类型
     */
    private static final int VERBOSE = 114;
    /**
     * 警告调试日志类型
     */
    private static final int WARN = 115;
    // 锁，是否打开Log日志输出
    private static boolean LogOn = true;
    // 是否打开VERBOSE输出
    private static boolean LogOn_VERBOSE = true;
    // 是否打开debug输出
    private static boolean LogOn_DEBUG = true;

    /**
     * 显示，打印日志
     */
    private static void LogShow(int Style, String Tag, String msg) {
        if (LogOn) {
            switch (Style) {
                case DEBUG:
                    if (LogOn_DEBUG) {
                        Log.d(Tag, msg);
                    }
                    break;
                case ERROR:
                    Log.e(Tag, msg);
                    break;
                case INFO:
                    Log.i(Tag, msg);
                    break;
                case VERBOSE:
                    if (LogOn_VERBOSE) {
                        Log.v(Tag, msg);
                    }
                    break;
                case WARN:
                    Log.w(Tag, msg);
                    break;
            }
        }
    }

    public static void d(String tag, String msg) {

        if (null == msg) {
            LogShow(DEBUG, tag, "null");
        } else {
            LogShow(DEBUG, tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (null == msg) {
            LogShow(ERROR, tag, "null");
        } else {
            LogShow(ERROR, tag, msg);
        }

    }

    public static void i(String tag, String msg) {
        if (null == msg) {
            LogShow(INFO, tag, "null");
        } else {
            LogShow(INFO, tag, msg);
        }

    }

    public static void v(String tag, String msg) {
        if (null == msg) {
            LogShow(VERBOSE, tag, "null");
        } else {
            LogShow(VERBOSE, tag, msg);
        }

    }

    public static void w(String tag, String msg) {
        if (null == msg) {
            LogShow(WARN, tag, "null");
        } else {
            LogShow(WARN, tag, msg);
        }

    }

}
