package com.example.swipecards.util;

import android.util.Log;

import com.example.swipecards.BuildConfig;

/**
 * Log工具类.
 */
public class MyLog {

    public static void v(String tag, String msg) {
        if (BuildConfig.LOGV) {
            Log.v(tag, msg);
        }
    }

    public static void v(String tag, String msg, Throwable t) {
        if (BuildConfig.LOGV) {
            Log.v(tag, msg, t);
        }
    }

    public static void d(String tag, String msg) {
        if (BuildConfig.LOGD) {
            Log.d(tag, msg);
        }
    }

    public static void d(String tag, String msg, Throwable t) {
        if (BuildConfig.LOGD) {
            Log.d(tag, msg, t);
        }
    }

    public static void i(String tag, String msg) {
        if (BuildConfig.LOGI) {
            Log.i(tag, msg);
        }
    }

    public static void i(String tag, String msg, Throwable t) {
        if (BuildConfig.LOGI) {
            Log.i(tag, msg, t);
        }
    }


    public static void w(String tag, String msg) {
        if (BuildConfig.LOGW) {
            Log.w(tag, msg);
        }
    }

    public static void w(String tag, String msg, Throwable t) {
        if (BuildConfig.LOGW) {
            Log.w(tag, msg, t);
        }
    }

    public static void e(String tag, String msg) {
        if (BuildConfig.LOGE) {
            Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable t) {
        if (BuildConfig.LOGE) {
            Log.e(tag, msg, t);
        }
    }


    private static String className = "";

    private static String methodName;

    private static int lineNumber;

    private static String createLog(String log) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        buffer.append(methodName);
        buffer.append(":");
        buffer.append(lineNumber);
        buffer.append("]");
        buffer.append(log);

        return buffer.toString();
    }

    private static void getMethodNames(StackTraceElement[] sElements) {
        className = sElements[1].getFileName();
        methodName = sElements[1].getMethodName();
        lineNumber = sElements[1].getLineNumber();
    }

    public static void e(String message) {
        if (BuildConfig.LOGE) {
            // Throwable instance must be created before any methods
            getMethodNames(new Throwable().getStackTrace());
            e(className, createLog(message));
        }
    }

    public static void w(String message) {
        if (BuildConfig.LOGW) {
            getMethodNames(new Throwable().getStackTrace());
            w(className, createLog(message));
        }
    }

    public static void i(String message) {
        if (BuildConfig.LOGI) {
            getMethodNames(new Throwable().getStackTrace());
            i(className, createLog(message));
        }
    }

    public static void d(String message) {
        if (BuildConfig.LOGD) {
            getMethodNames(new Throwable().getStackTrace());
            d(className, createLog(message));
        }
    }

    public static void v(String message) {
        if (BuildConfig.LOGV) {
            getMethodNames(new Throwable().getStackTrace());
            v(className, createLog(message));
        }
    }

    public static void wtf(String message) {
        if (BuildConfig.LOGE) {
            getMethodNames(new Throwable().getStackTrace());
            Log.wtf(className, createLog(message));
        }
    }
}
