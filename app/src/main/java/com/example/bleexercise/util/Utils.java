package com.example.bleexercise.util;

import android.app.Activity;
import android.widget.Toast;

public class Utils {

    private static Utils utils;

    public static Utils getInstance()
    {
        if(utils == null)
            utils = new Utils();

        return utils;
    }

    public void showToast(final Activity activity, final String msg, final boolean isLong)
    {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, msg, isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
            }
        });
    }
}
