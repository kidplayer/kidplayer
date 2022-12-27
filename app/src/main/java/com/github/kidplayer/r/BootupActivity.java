package com.github.kidplayer.r;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.github.kidplayer.MainActivity;
import com.github.kidplayer.comm.App;

public class BootupActivity extends BroadcastReceiver {
    private static final String TAG = "BootupActivity";

    private static final long INITIAL_DELAY = 5000;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "BootupActivity initiated");

        SharedPreferences sp = App.getInstance().getSharedPreferences("SP", Context.MODE_PRIVATE);
        String id = "_start_at_boot_";
        boolean startAtBoot = sp.getBoolean(id,true);

        if (startAtBoot && intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().toUpperCase().indexOf("QUICKBOOT_POWERON")>-1) {
            Intent mainActivityIntent = new Intent(context, MainActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             context.startActivity(mainActivityIntent);
           // scheduleRecommendationUpdate(context);
        }
    }

    private void scheduleRecommendationUpdate(Context context) {
        Log.d(TAG, "Scheduling recommendations update");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent recommendationIntent = new Intent(context, UpdateRecommendationsService.class);
        PendingIntent alarmIntent = PendingIntent.getService(context, 0, recommendationIntent, 0);

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                INITIAL_DELAY,
                AlarmManager.INTERVAL_HALF_HOUR,
                alarmIntent);
    }
}