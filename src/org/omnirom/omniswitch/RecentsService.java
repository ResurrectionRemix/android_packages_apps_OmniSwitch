package org.omnirom.omniswitch;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Log;
import android.view.WindowManager;

public class RecentsService extends Service {
	private final static String TAG = "RecentsService";
	
	private RecentsGestureView mGesturePanel;
	private RecentsReceiver mReceiver;
	private RecentsManager mManager;
	private Handler mHandler;
	private static boolean mIsRunning;

	public static boolean isRunning() {
		return mIsRunning;
	}
	@Override
    public void onCreate() {
        super.onCreate();

        mGesturePanel = new RecentsGestureView(this, null);
        Log.d(TAG, "started RecentsService");

        WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        wm.addView(mGesturePanel, mGesturePanel.getGesturePanelLayoutParams());
        
        mManager = new RecentsManager(this);
        mHandler = new Handler();
        
        mReceiver = new RecentsReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(RecentsReceiver.ACTION_SHOW_RECENTS);
        filter.addAction(RecentsReceiver.ACTION_SHOW_RECENTS2);
        filter.addAction(RecentsReceiver.ACTION_HIDE_RECENTS);
        filter.addAction(RecentsReceiver.ACTION_KILL_RECENTS);

        registerReceiver(mReceiver, filter);
        mIsRunning = true;
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "stopped RecentsService");
        ((WindowManager)getSystemService(WINDOW_SERVICE)).removeView(mGesturePanel);
        unregisterReceiver(mReceiver);
        mManager.killManager();
        mIsRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    public class RecentsReceiver extends BroadcastReceiver {
        public static final String ACTION_SHOW_RECENTS = "org.omnirom.omniswitch.ACTION_SHOW_RECENTS";
        public static final String ACTION_SHOW_RECENTS2 = "org.omnirom.omniswitch.ACTION_SHOW_RECENTS2";
        public static final String ACTION_HIDE_RECENTS = "org.omnirom.omniswitch.ACTION_HIDE_RECENTS";
        public static final String ACTION_KILL_RECENTS = "org.omnirom.omniswitch.ACTION_KILL_RECENTS";

        @Override
        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive " + action);
            if (ACTION_SHOW_RECENTS.equals(action)) {
            	if (!mManager.isShowing()){
            		Intent mainActivity = new Intent(context, MainActivity.class);
            		mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

            		startActivity(mainActivity);
            		//mManager.show();
            	}
            } else if (ACTION_SHOW_RECENTS2.equals(action)) {
                if (!mManager.isShowing()){
                	mManager.show();
                }
            } else if (ACTION_HIDE_RECENTS.equals(action)) {
            	if (mManager.isReady() && mManager.isShowing()){
            		mManager.hide();
            	    Intent finishActivity = new Intent(
            			    MainActivity.ActivityReceiver.ACTION_FINISH);
            	    sendBroadcast(finishActivity);
            	}
            } else if (ACTION_KILL_RECENTS.equals(action)) {
            	if (mManager.isShowing()){
            		mManager.hide();
            	}
            	Intent finishActivity = new Intent(
            			MainActivity.ActivityReceiver.ACTION_FINISH);
            	sendBroadcast(finishActivity);
            	Intent svc = new Intent(context, RecentsService.class);
            	context.stopService(svc);
            }
        }
    }
}
