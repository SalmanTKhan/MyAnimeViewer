package com.taskdesignsinc.android.myanimeviewer.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WriteLog {
    public static final int VERBOSE = 0;
    public static final int DEBUG = 1;
    private static boolean mIsEnabled = false;
    public static boolean mSystemOut = true;
    public static boolean mPrintPhoneInfo = false;
    public static int mDebugLevel = 0;
    private static Context mContext;

    public static void setContext(Context context) {
        mContext = context;
    }

    public static void setEnabled(boolean isEnabled) {
        mIsEnabled = isEnabled;
        if (mIsEnabled)
            mDebugLevel = DEBUG;
    }

    public final static String getDateTime() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss");
        return df.format(new Date());
    }

    public static String getPhoneInfo() {
        mPrintPhoneInfo = true;
        String s = getDateTime() + " Phone Info:";
        s += "\n OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
        s += "\n OS API Level: " + android.os.Build.VERSION.SDK_INT;
        s += "\n Device: " + android.os.Build.DEVICE;
        s += "\n Model (and Product): " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")";
        s += "\n Screen Size Height: " + DisplayUtils.getScreenHeight(mContext) + "  Width: " + DisplayUtils.getScreenWidth(mContext);
        return s;
    }

    public static void appendLog(String... text) {
        if (text != null && text.length > 0) {
            String parsedText = text[0];
            for (int i = 0; i < text.length - 1; i++) {
                if (i + 1 >= text.length)
                    break;
                parsedText = parsedText.replaceAll("\\{" + i + "\\}", text[i + 1]);
            }
            appendLog(parsedText);
        }
    }

    public static void appendLog(String tag, String text) {
        if (mSystemOut)
            Log.d(tag, text);
        printToFile(tag + ": " + text);
    }

    public static void appendLog(String tag, String text, int level) {
        if (level >= mDebugLevel) {
            if (mSystemOut)
                Log.d(tag, text);
            printToFile(tag + ": " + text);
        }
    }

    private static void printToFile(String text) {
        if (mIsEnabled) {
            File logFile = new File(StorageUtils.getDataDirectory(), "Log.txt");
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try {
                //BufferedWriter for performance, true to set append to file flag
                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                if (!mPrintPhoneInfo)
                    buf.append(getPhoneInfo());
                buf.append("[" + getDateTime() + "] " + text);
                buf.newLine();
                buf.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void appendLog(String text) {
        if (mSystemOut) {
            System.out.println(text);
        }
        printToFile(text);
    }

    public static void appendLogIf(boolean condition, String text) {
        if (condition)
            appendLog(text);
    }

    public static void appendLogDetailedOnly(String text) {
        if (isDetailed())
            appendLog(text);
    }

    public static void appendLogDetailedOnly(String tag, String text) {
        if (isDetailed())
            appendLog(tag, text);
    }

    public static void appendLogException(String tag, String text, Exception exception) {
        WriteLog.appendLog(tag, text);
        WriteLog.appendLog(Log.getStackTraceString(exception));
    }

    public static void appendLogException(String text, Exception exception) {
        WriteLog.appendLog(text);
        WriteLog.appendLog(Log.getStackTraceString(exception));
    }

    public static boolean isDetailed() {
        return mDebugLevel > 1;
    }

    public static boolean IsEnabled() {
        return mIsEnabled;
    }
}
