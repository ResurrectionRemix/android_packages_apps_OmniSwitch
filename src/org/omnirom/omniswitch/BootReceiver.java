package org.omnirom.omniswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "RecentsBootReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        try {           
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean("start_on_boot", false)) {
                Intent loadavg = new Intent(context, RecentsService.class);
                context.startService(loadavg);
            }

        } catch (Exception e) {
            Log.e(TAG, "Can't start load average service", e);
        }
    }
}
