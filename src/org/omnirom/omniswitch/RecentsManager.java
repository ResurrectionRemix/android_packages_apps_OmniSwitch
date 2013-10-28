/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnirom.omniswitch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
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
    	if (mIsShowning){
    		Log.d(TAG, "hide");
    		mLayout.hide();
    		mIsShowning = false;
    	}
    }

    public void show() {
    	if (!mIsShowning){
    		Log.d(TAG, "show"); 
    		mIsReady = false;
    		mLayout.show();
    		reload();
    		mIsShowning = true;
    	}
    }

    public boolean isShowing(){
    	return mIsShowning;
    }

    private void init() {
        Log.d(TAG, "init");

    	mLoadedTasks = new ArrayList<TaskDescription>();

    	mLayout = new RecentsLayout(mContext, null, this);
    	mRecentTasksLoader = RecentTasksLoader.getInstance(mContext, this);
        mRecentTasksLoader.loadTasksInBackground();
    }
    
    public void killManager(){
    	RecentTasksLoader.killInstance();
    }
    
    public void update(List<TaskDescription> taskList){
    	Log.d(TAG, "update");
    	mLoadedTasks.clear();
    	mLoadedTasks.addAll(taskList);
    	mLayout.update(mLoadedTasks);
    	mIsReady = true;
    }
    
    public void reload(){
    	mRecentTasksLoader.cancelLoadingTasks();
    	mRecentTasksLoader.loadTasksInBackground();
    }
    
    public boolean isReady(){
    	return mIsReady;
    }
    
    public void switchTask(TaskDescription ad) {
    	if (ad.isKilled()){
    		return;
    	}
        final ActivityManager am = (ActivityManager)
        		mContext.getSystemService(Context.ACTIVITY_SERVICE);

    	Log.d(TAG, "switch to " + ad.getPackageName());

        if (ad.getTaskId() >= 0) {
            // This is an active task; it should just go to the foreground.
            // If that task was split viewed, a normal press wil resume it to
            // normal fullscreen view
            /*IWindowManager wm = (IWindowManager) WindowManagerGlobal.getWindowManagerService();
            try {
                if (DEBUG) Log.v(TAG, "Restoring window full screen after split, because of normal tap");
                wm.setTaskSplitView(ad.taskId, false);
            } catch (RemoteException e) {
                Log.e(TAG, "Could not setTaskSplitView to fullscreen", e);
            }*/
            am.moveTaskToFront(ad.getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
        } else {
            Intent intent = ad.getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
                    | Intent.FLAG_ACTIVITY_TASK_ON_HOME
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (DEBUG) Log.v(TAG, "Starting activity " + intent);
            try {
            	mContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
            } catch (SecurityException e) {
                Log.e(TAG, "Recents does not have the permission to launch " + intent, e);
            }
        }
    	Intent hideRecent = new Intent(
                RecentsService.RecentsReceiver.ACTION_HIDE_RECENTS);
        mContext.sendBroadcast(hideRecent);
    }
    
    public void killTask(TaskDescription ad) {
        final ActivityManager am = (ActivityManager)
        		mContext.getSystemService(Context.ACTIVITY_SERVICE);

        am.removeTask(ad.getPersistentTaskId(), ActivityManager.REMOVE_TASK_KILL_PROCESS);
       	Log.d(TAG, "kill " + ad.getPackageName());
	    ad.setKilled();
	    reload();
    }
    
    public void killAll(){
        final ActivityManager am = (ActivityManager)
        		mContext.getSystemService(Context.ACTIVITY_SERVICE);

        if (mLoadedTasks.size() == 0){
        	return;
        }

    	Iterator<TaskDescription> nextTask = mLoadedTasks.iterator();
    	while(nextTask.hasNext()){
    		TaskDescription ad = nextTask.next();
            am.removeTask(ad.getPersistentTaskId(), ActivityManager.REMOVE_TASK_KILL_PROCESS);
           	Log.d(TAG, "kill " + ad.getPackageName());
    		ad.setKilled();
    	}
    	dismissAndGoHome();
    }

    public void killOther(){
        final ActivityManager am = (ActivityManager)
        		mContext.getSystemService(Context.ACTIVITY_SERVICE);

        if (mLoadedTasks.size() == 0){
        	return;
        }
    	Iterator<TaskDescription> nextTask = mLoadedTasks.iterator();
    	// skip active task
    	nextTask.next();
    	while(nextTask.hasNext()){
    		TaskDescription ad = nextTask.next();
            am.removeTask(ad.getPersistentTaskId(), ActivityManager.REMOVE_TASK_KILL_PROCESS);
           	Log.d(TAG, "kill " + ad.getPackageName());
    		ad.setKilled();
    	}
    	reload();
    }

    
    public void dismissAndGoHome() {
    	Intent homeIntent = new Intent(Intent.ACTION_MAIN, null);
    	homeIntent.addCategory(Intent.CATEGORY_HOME);
    	homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
    			| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    	mContext.startActivityAsUser(homeIntent, new UserHandle(UserHandle.USER_CURRENT));
    	Intent hideRecent = new Intent(
    			RecentsService.RecentsReceiver.ACTION_HIDE_RECENTS);
        mContext.sendBroadcast(hideRecent);
    }
}
