package com.chirpy.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Alarm receiver to launch the tweet update service
 *
 * @author dhavalmotghare@gmail.com
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        Runnable runnable = new Runnable() {
            public void run() {
                Intent serviceIntent = new Intent(context, ChirpyService.class);
                serviceIntent.putExtra("ACTION", "UPDATE_FEEDS");
                context.startService(serviceIntent);
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }
}
