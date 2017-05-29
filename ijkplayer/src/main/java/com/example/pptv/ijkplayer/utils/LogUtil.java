package com.example.pptv.ijkplayer.utils;

import android.util.Log;

/**
 * Created by wzhx on 2017/5/28.
 */

public class LogUtil {
    private static final String TAG = "ijkMediaPlayer";

    public static void e(String log) {
        Log.e(TAG, log);
    }

    public static void d(String log) {
        Log.d(TAG, log);
    }
}
