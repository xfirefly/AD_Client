package com.bluberry.adclient;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
    public static final String TAG = "customTag";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            ComponentName comp = new ComponentName(context.getPackageName(), MainActivity.class.getName());

            context.startActivity(new Intent().setComponent(comp).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            Log.e(TAG, "Received unexpected intent " + intent.toString());
        }
    }

}