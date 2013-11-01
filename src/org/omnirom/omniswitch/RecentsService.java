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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class RecentsService extends Service {
	private final static String TAG = "RecentsService";

	private RecentsGestureView mGesturePanel;
	private RecentsReceiver mReceiver;
	private RecentsManager mManager;
	private SharedPreferences mPrefs;
	private SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener;

	private static boolean mIsRunning;

	public static boolean isRunning() {
		return mIsRunning;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mGesturePanel = new RecentsGestureView(this, null);
		Log.d(TAG, "started RecentsService");

		mManager = new RecentsManager(this);

		mReceiver = new RecentsReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(RecentsReceiver.ACTION_SHOW_RECENTS);
		filter.addAction(RecentsReceiver.ACTION_SHOW_RECENTS2);
		filter.addAction(RecentsReceiver.ACTION_HIDE_RECENTS);
		filter.addAction(RecentsReceiver.ACTION_KILL_RECENTS);

		registerReceiver(mReceiver, filter);
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		updatePrefs(mPrefs, null);

		mPrefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				updatePrefs(prefs, key);
			}
		};

		mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);

		mGesturePanel.show();

		mIsRunning = true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "stopped RecentsService");

		mGesturePanel.hide();
		mGesturePanel = null;

		unregisterReceiver(mReceiver);
		mManager.killManager();
		mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);

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
				if (!mManager.isShowing()) {
					Intent mainActivity = new Intent(context,
							MainActivity.class);
					mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

					startActivity(mainActivity);
				}
			} else if (ACTION_SHOW_RECENTS2.equals(action)) {
				if (!mManager.isShowing()) {
					mManager.show();
				}
			} else if (ACTION_HIDE_RECENTS.equals(action)) {
				if (mManager.isReady() && mManager.isShowing()) {
					Intent finishActivity = new Intent(
							MainActivity.ActivityReceiver.ACTION_FINISH);
					sendBroadcast(finishActivity);
					mManager.hide();
				}
			} else if (ACTION_KILL_RECENTS.equals(action)) {
				if (mManager.isShowing()) {
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
	public void updatePrefs(SharedPreferences prefs, String key){
		mManager.updatePrefs(prefs, key);
		mGesturePanel.updatePrefs(prefs, key);
	}
}
