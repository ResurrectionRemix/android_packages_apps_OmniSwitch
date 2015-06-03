/*
 *  Copyright (C) 2013 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omniswitch;

import org.omnirom.omniswitch.ui.BitmapCache;
import org.omnirom.omniswitch.ui.IconPackHelper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.util.Log;

public class SwitchService extends Service {
    private final static String TAG = "SwitchService";
    private static boolean DEBUG = false;

    private static final int START_SERVICE_ERROR_ID = 0;

    private RecentsReceiver mReceiver;
    private SwitchManager mManager;
    private SharedPreferences mPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener;
    private SwitchConfiguration mConfiguration;
    private int mUserId = -1;

    private static boolean mIsRunning;
    private static boolean mCommitSuicide;

    public static boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            mConfiguration = SwitchConfiguration.getInstance(this);
            mConfiguration.initDefaults(this);

            if(mConfiguration.mRestrictedMode){
                createErrorNotification();
            }
            mUserId = UserHandle.myUserId();
            Log.d(TAG, "started SwitchService " + mUserId);

            mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

            String layoutStyle = mPrefs.getString(SettingsActivity.PREF_LAYOUT_STYLE, "0");
            mManager = new SwitchManager(this, Integer.valueOf(layoutStyle));

            mReceiver = new RecentsReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(RecentsReceiver.ACTION_SHOW_OVERLAY);
            filter.addAction(RecentsReceiver.ACTION_HIDE_OVERLAY);
            filter.addAction(RecentsReceiver.ACTION_HANDLE_HIDE);
            filter.addAction(RecentsReceiver.ACTION_HANDLE_SHOW);
            filter.addAction(RecentsReceiver.ACTION_TOGGLE_OVERLAY);
            filter.addAction(Intent.ACTION_USER_SWITCHED);
            filter.addAction(Intent.ACTION_SHUTDOWN);

            registerReceiver(mReceiver, filter);
            PackageManager.getInstance(this).updatePackageList();

            updatePrefs(mPrefs, null);

            mPrefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs,
                        String key) {
                    try {
                        updatePrefs(prefs, key);
                    } catch(Exception e) {
                        Log.e(TAG, "updatePrefs", e);
                    }
                }
            };

            mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);

            mIsRunning = true;
        } catch(Exception e) {
            Log.e(TAG, "onCreate", e);
            commitSuicide();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "stopped SwitchService " + mUserId);

        try {
            unregisterReceiver(mReceiver);
            mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
        } catch(IllegalArgumentException e) {
            // ignored on purpose
        }
        if (mManager != null) {
            mManager.killManager();
            mManager.shutdownService();
        }

        mIsRunning = false;
        BitmapCache.getInstance(this).clear();

        if (mCommitSuicide) {
            mCommitSuicide = false;
            // to get the "app has stopped alert"
            throw new RuntimeException("Failed to start OmniSwitch");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public class RecentsReceiver extends BroadcastReceiver {
        public static final String ACTION_SHOW_OVERLAY = "org.omnirom.omniswitch.ACTION_SHOW_OVERLAY";
        public static final String ACTION_HIDE_OVERLAY = "org.omnirom.omniswitch.ACTION_HIDE_OVERLAY";
        public static final String ACTION_HANDLE_HIDE = "org.omnirom.omniswitch.ACTION_HANDLE_HIDE";
        public static final String ACTION_HANDLE_SHOW = "org.omnirom.omniswitch.ACTION_HANDLE_SHOW";
        public static final String ACTION_TOGGLE_OVERLAY = "org.omnirom.omniswitch.ACTION_TOGGLE_OVERLAY";

        private void show(Context context) {
            mManager.show();
        }

        private void hide() {
            mManager.hide(false);
        }

        @Override
        public void onReceive(final Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if(DEBUG){
                    Log.d(TAG, "onReceive " + action);
                }
                if (ACTION_SHOW_OVERLAY.equals(action)) {
                    if (!mManager.isShowing()) {
                        if (DEBUG){
                            Log.d(TAG, "ACTION_SHOW_OVERLAY " + System.currentTimeMillis());
                        }
                        show(context);
                    }
                } else if (ACTION_HIDE_OVERLAY.equals(action)) {
                    if (mManager.isShowing()) {
                        hide();
                    }
                } else if (ACTION_HANDLE_SHOW.equals(action)){
                    if (mConfiguration.mDragHandleShow){
                        mManager.getSwitchGestureView().show();
                    }
                } else if (ACTION_HANDLE_HIDE.equals(action)){
                    mManager.getSwitchGestureView().hide();
                } else if (ACTION_TOGGLE_OVERLAY.equals(action)) {
                    if (mManager.isShowing()) {
                        hide();
                    } else {
                        show(context);
                    }
                } else if (Intent.ACTION_USER_SWITCHED.equals(action)) {
                    int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
                    Log.d(TAG, "user switch " + mUserId + "->" + userId);
                    if (userId != mUserId){
                        mManager.getSwitchGestureView().hide();
                    } else {
                        if (mConfiguration.mDragHandleShow){
                            mManager.getSwitchGestureView().show();
                        }
                    }
                } else if (Intent.ACTION_SHUTDOWN.equals(action)) {
                    Log.d(TAG, "ACTION_SHUTDOWN");
                    mManager.shutdownService();
                }
            } catch(Exception e) {
                Log.e(TAG,"onReceive", e);
            }
        }
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        // MUST be before the rest
        IconPackHelper.getInstance(this).updatePrefs(prefs, key);
        mConfiguration.updatePrefs(prefs, key);
        mManager.updatePrefs(prefs, key);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            if (mIsRunning) {
                mManager.updateLayout();
            }
        } catch(Exception e) {
            Log.e(TAG, "onConfigurationChanged", e);
        }
    }

    private void createErrorNotification() {
        final NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        final Notification notifyDetails = new Notification.Builder(this)
                .setContentTitle("OmniSwitch restricted mode")
                .setContentText("Failed to gain system permissions")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .build();
        notificationManager.notify(START_SERVICE_ERROR_ID, notifyDetails);
    }

    private void commitSuicide() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(SettingsActivity.PREF_START_ON_BOOT, false).commit();
        prefs.edit().putBoolean(SettingsActivity.PREF_ENABLE, false).commit();
        mCommitSuicide = true;
        Intent stopIntent = new Intent(this, SwitchService.class);
        stopService(stopIntent);
    }
}
