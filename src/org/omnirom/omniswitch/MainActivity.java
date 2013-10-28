package org.omnirom.omniswitch;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

public class MainActivity extends Activity {
	private static final String TAG = "RecentsMainActivity";
	
	private ActivityReceiver mReceiver;

    public class ActivityReceiver extends BroadcastReceiver {
        public static final String ACTION_FINISH = "org.omnirom.omniswitch.ACTION_FINISH_ACTIVITY";

        @Override
        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive " + action);
            if (ACTION_FINISH.equals(action)) {
            	finish();
            }
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onStop(){
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        
        mReceiver = new ActivityReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ActivityReceiver.ACTION_FINISH);

        registerReceiver(mReceiver, filter);

        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onPause(){
        Log.d(TAG, "onPause");
    	Intent hideRecent = new Intent(
                RecentsService.RecentsReceiver.ACTION_HIDE_RECENTS);
        sendBroadcast(hideRecent);
        super.onPause();
    }

    @Override
    public void onResume(){
        Log.d(TAG, "onResume");
    	Intent hideRecent = new Intent(
        		RecentsService.RecentsReceiver.ACTION_SHOW_RECENTS2);
        sendBroadcast(hideRecent);
        super.onResume();
    }
    
    @Override
    public void onDestroy(){
        Log.d(TAG, "onDestroy");
    	unregisterReceiver(mReceiver);
    	super.onDestroy();
    }
}
