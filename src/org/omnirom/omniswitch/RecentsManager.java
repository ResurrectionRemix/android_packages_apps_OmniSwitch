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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;
import android.os.UserHandle;
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.view.IWindowManager;
import android.os.RemoteException;

public class RecentsManager {
	private static final String TAG = "RecentsManager";
	private boolean DEBUG = true;
	private List<TaskDescription> mLoadedTasks;
	private RecentTasksLoader mRecentTasksLoader;
	private RecentsLayout mLayout;
	private Context mContext;
	private boolean mIsReady;
	private boolean mIsShowning;

	public RecentsManager(Context context) {
		mContext = context;
		init();
	}

	public void hide() {
		if (mIsShowning) {
			Log.d(TAG, "hide");
			mLayout.hide();
			mIsShowning = false;
		}
	}

	public void show() {
		if (!mIsShowning) {
			Log.d(TAG, "show");
			mIsReady = false;
			reload();
			mLayout.show();
			mIsShowning = true;
		}
	}

	public boolean isShowing() {
		return mIsShowning;
	}

	private void init() {
		Log.d(TAG, "init");

		mLoadedTasks = new ArrayList<TaskDescription>();

		mLayout = new RecentsLayout(mContext, null, this);
		mRecentTasksLoader = RecentTasksLoader.getInstance(mContext, this);
		mRecentTasksLoader.loadTasksInBackground();
	}

	public void killManager() {
		RecentTasksLoader.killInstance();
	}

	public void update(List<TaskDescription> taskList) {
		Log.d(TAG, "update");
		mLoadedTasks.clear();
		mLoadedTasks.addAll(taskList);
		mLayout.update(mLoadedTasks);
		mIsReady = true;
	}

	public void reload() {
		mRecentTasksLoader.cancelLoadingTasks();
		mRecentTasksLoader.loadTasksInBackground();
	}

	public boolean isReady() {
		return mIsReady;
	}

	public void switchTask(TaskDescription ad) {
		if (ad.isKilled()) {
			return;
		}
		final ActivityManager am = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);

		Log.d(TAG, "switch to " + ad.getPackageName());

		Intent hideRecent = new Intent(
				RecentsService.RecentsReceiver.ACTION_HIDE_RECENTS);
		mContext.sendBroadcast(hideRecent);

		if (ad.getTaskId() >= 0) {
			// This is an active task; it should just go to the foreground.
			// If that task was split viewed, a normal press wil resume it to
			// normal fullscreen view
			IWindowManager wm = (IWindowManager) WindowManagerGlobal
					.getWindowManagerService();
			try {
				if (DEBUG)
					Log.v(TAG,
							"Restoring window full screen after split, because of normal tap");
				wm.setTaskSplitView(ad.taskId, false);
			} catch (RemoteException e) {
				Log.e(TAG, "Could not setTaskSplitView to fullscreen", e);
			}
			am.moveTaskToFront(ad.getTaskId(),
					ActivityManager.MOVE_TASK_WITH_HOME);
		} else {
			Intent intent = ad.getIntent();
			intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
					| Intent.FLAG_ACTIVITY_TASK_ON_HOME
					| Intent.FLAG_ACTIVITY_NEW_TASK);
			if (DEBUG)
				Log.v(TAG, "Starting activity " + intent);
			try {
				mContext.startActivityAsUser(intent, new UserHandle(
						UserHandle.USER_CURRENT));
			} catch (SecurityException e) {
				Log.e(TAG, "Recents does not have the permission to launch "
						+ intent, e);
			}
		}
	}

	public void killTask(TaskDescription ad) {
		final ActivityManager am = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);

		am.removeTask(ad.getPersistentTaskId(),
				ActivityManager.REMOVE_TASK_KILL_PROCESS);
		Log.d(TAG, "kill " + ad.getPackageName());
		ad.setKilled();
		mLoadedTasks.remove(ad);
		mLayout.update(mLoadedTasks);
	}

	public void killAll() {
		final ActivityManager am = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);

		if (mLoadedTasks.size() == 0) {
			return;
		}

		Iterator<TaskDescription> nextTask = mLoadedTasks.iterator();
		while (nextTask.hasNext()) {
			TaskDescription ad = nextTask.next();
			am.removeTask(ad.getPersistentTaskId(),
					ActivityManager.REMOVE_TASK_KILL_PROCESS);
			Log.d(TAG, "kill " + ad.getPackageName());
			ad.setKilled();
		}
		dismissAndGoHome();
	}

	public void killOther() {
		final ActivityManager am = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);

		if (mLoadedTasks.size() == 0) {
			return;
		}
		Iterator<TaskDescription> nextTask = mLoadedTasks.iterator();
		// skip active task
		nextTask.next();
		while (nextTask.hasNext()) {
			TaskDescription ad = nextTask.next();
			am.removeTask(ad.getPersistentTaskId(),
					ActivityManager.REMOVE_TASK_KILL_PROCESS);
			Log.d(TAG, "kill " + ad.getPackageName());
			ad.setKilled();
		}
		reload();
	}

	/**
	 * Opens the task linked in the ViewHolder in split view mode.
	 * 
	 * @param holder
	 *            ViewHolder of a task thumbnail
	 * @param location
	 *            Where to put the split app (-1 for auto, 0 for top, 1 for
	 *            bottom (the reference is a phone in portrait))
	 */
	public void openInSplitView(TaskDescription ad, int location) {
		final ActivityManager am = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);

		final IWindowManager wm = (IWindowManager) WindowManagerGlobal
				.getWindowManagerService();

		Intent hideRecent = new Intent(
				RecentsService.RecentsReceiver.ACTION_HIDE_RECENTS);
		mContext.sendBroadcast(hideRecent);

		// If we weren't on the homescreen, resize the previous activity (if not
		// already split)
		final List<ActivityManager.RecentTaskInfo> recentTasks = am
				.getRecentTasks(20, ActivityManager.RECENT_IGNORE_UNAVAILABLE);

		if (recentTasks != null && recentTasks.size() > 0) {
			final PackageManager pm = mContext.getPackageManager();
			ActivityInfo homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(
					Intent.CATEGORY_HOME).resolveActivityInfo(pm, 0);
			int taskInt = 0;
			ActivityManager.RecentTaskInfo taskInfo = recentTasks.get(1);
			Log.e("XPLOD", "Resizing previous activity " + taskInfo.baseIntent);
			Intent intent = new Intent(taskInfo.baseIntent);
			if (taskInfo.origActivity != null) {
				intent.setComponent(taskInfo.origActivity);
			}

			ComponentName component = intent.getComponent();

			if (homeInfo == null
					|| !homeInfo.packageName.equals(component.getPackageName())
					|| !homeInfo.name.equals(component.getClassName())) {
				Log.e("XPLOD", "not home intent, splitting");
				// This is not the home activity, so split it
				try {
					wm.setTaskSplitView(taskInfo.persistentId, true);
				} catch (RemoteException e) {
					Log.e(TAG, "Could not set previous task to split view", e);
				}

				// We move this to front first, then our activity, so it updates
				am.moveTaskToFront(taskInfo.persistentId, 0, null);
			}
		}

		if (ad.getTaskId() >= 0) {
			// The task is already launched. The Activity will pull its split
			// information from WindowManagerService once it resumes, so we
			// set its state here.
			try {
				wm.setTaskSplitView(ad.getTaskId(), true);
			} catch (RemoteException e) {
				Log.e(TAG, "Could not setTaskSplitView", e);
			}
			am.moveTaskToFront(ad.taskId, 0, null);
		} else {
			// The app has been killed (we have no taskId for it), so we start
			// a new one with the SPLIT_VIEW flag
			Intent intent = ad.intent;
			intent.addFlags(Intent.FLAG_ACTIVITY_SPLIT_VIEW
					| Intent.FLAG_ACTIVITY_NEW_TASK);

			if (DEBUG)
				Log.v(TAG, "Starting split view activity " + intent);

			try {
				mContext.startActivityAsUser(intent, null, new UserHandle(
						UserHandle.USER_CURRENT));
			} catch (SecurityException e) {
				Log.e(TAG, "Recents does not have the permission to launch "
						+ intent, e);
			}
		}
	}

	public void dismissAndGoHome() {
		Intent hideRecent = new Intent(
				RecentsService.RecentsReceiver.ACTION_HIDE_RECENTS);
		mContext.sendBroadcast(hideRecent);

		Intent homeIntent = new Intent(Intent.ACTION_MAIN, null);
		homeIntent.addCategory(Intent.CATEGORY_HOME);
		homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		mContext.startActivityAsUser(homeIntent, new UserHandle(
				UserHandle.USER_CURRENT));
	}

	public void updatePrefs(SharedPreferences prefs, String key){
		mLayout.updatePrefs(prefs, key);
	}

	public void toggleLastApp() {
		if (mLoadedTasks.size() < 2) {
			return;
		}
		
		TaskDescription ad = mLoadedTasks.get(1);
		switchTask(ad);
	}
}
