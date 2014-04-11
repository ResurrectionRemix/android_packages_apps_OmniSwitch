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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.omnirom.omniswitch.ui.SwitchGestureView;
import org.omnirom.omniswitch.ui.SwitchLayout;

import android.app.ActivityManager;
import android.app.TaskStackBuilder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

public class SwitchManager {
    private static final String TAG = "SwitchManager";
    private static final boolean DEBUG = true;
    private List<TaskDescription> mLoadedTasks;
    private SwitchLayout mLayout;
    private SwitchGestureView mGestureView;
    private Context mContext;
    private SwitchConfiguration mConfiguration;

    public SwitchManager(Context context) {
        mContext = context;
        mConfiguration = SwitchConfiguration.getInstance(mContext);
        init();
    }

    public void hide() {
        if (isShowing()) {
            if(DEBUG){
                Log.d(TAG, "hide");
            }
            mGestureView.setHandleRecentsUpdate(true);
            mLayout.hide();
        }
    }

    public void show() {
        if (!isShowing()) {
            if(DEBUG){
                Log.d(TAG, "show");
            }
            mGestureView.setHandleRecentsUpdate(false);
            // update task list
            reload();

            // show immediately
            mLayout.show();
        }
    }

    public boolean isShowing() {
        return mLayout.isShowing();
    }

    private void init() {
        if(DEBUG){
            Log.d(TAG, "init");
        }

        mLoadedTasks = new ArrayList<TaskDescription>();

        mLayout = new SwitchLayout(mContext);
        mLayout.setRecentsManager(this);

        mGestureView = new SwitchGestureView(mContext);
        mGestureView.setRecentsManager(this);
    }

    public void killManager() {
        RecentTasksLoader.killInstance();
        mGestureView.hide();
    }

    public SwitchLayout getLayout() {
        return mLayout;
    }

    public SwitchGestureView getSwitchGestureView() {
        return mGestureView;
    }

    public void update(List<TaskDescription> taskList) {
        if(DEBUG){
            Log.d(TAG, "update");
        }
        mLoadedTasks.clear();
        mLoadedTasks.addAll(taskList);
        mLayout.update(mLoadedTasks);
        if(mGestureView.isHandleRecentsUpdate()){
            mGestureView.update(mLoadedTasks);
        }
    }

    public void reload() {
        RecentTasksLoader.getInstance(mContext).loadTasksInBackground(this);
    }

    public void switchTask(TaskDescription ad, boolean close) {
        if (ad.isKilled()) {
            return;
        }
        final ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);

        if(DEBUG){
            Log.d(TAG, "switch to " + ad.getPackageName());
        }

        if(close){
            close();
        }

        if (ad.getTaskId() >= 0) {
            // This is an active task; it should just go to the foreground.
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
                mContext.startActivity(intent);
            } catch (SecurityException e) {
                Log.e(TAG, "Recents does not have the permission to launch "
                        + intent, e);
            }
        }
    }

    public void killTask(TaskDescription ad) {
        if (mConfiguration.mRestrictedMode){
            return;
        }
        final ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);

        am.removeTask(ad.getPersistentTaskId(),
                ActivityManager.REMOVE_TASK_KILL_PROCESS);
        if(DEBUG){
            Log.d(TAG, "kill " + ad.getPackageName());
        }
        ad.setKilled();
        mLoadedTasks.remove(ad);
        mLayout.refresh(mLoadedTasks);
    }

    public void killAll(boolean close) {
        if (mConfiguration.mRestrictedMode){
            return;
        }
        final ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);

        if (mLoadedTasks.size() == 0) {
            if(close){
                close();
            }
            return;
        }

        Iterator<TaskDescription> nextTask = mLoadedTasks.iterator();
        while (nextTask.hasNext()) {
            TaskDescription ad = nextTask.next();
            am.removeTask(ad.getPersistentTaskId(),
                    ActivityManager.REMOVE_TASK_KILL_PROCESS);
            if(DEBUG){
                Log.d(TAG, "kill " + ad.getPackageName());
            }
            ad.setKilled();
        }
        goHome(close);
    }

    public void killOther(boolean close) {
        if (mConfiguration.mRestrictedMode){
            return;
        }
        final ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);

        if (mLoadedTasks.size() == 0) {
            if(close){
                close();
            }
            return;
        }
        Iterator<TaskDescription> nextTask = mLoadedTasks.iterator();
        // skip active task
        nextTask.next();
        while (nextTask.hasNext()) {
            TaskDescription ad = nextTask.next();
            am.removeTask(ad.getPersistentTaskId(),
                    ActivityManager.REMOVE_TASK_KILL_PROCESS);
            if(DEBUG){
                Log.d(TAG, "kill " + ad.getPackageName());
            }
            ad.setKilled();
        }
        if(close){
            close();
        }
    }

    public void killCurrent(boolean close) {
        if (mConfiguration.mRestrictedMode){
            return;
        }
        final ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);

        if (mLoadedTasks.size() == 0) {
            if(close){
                close();
            }
            return;
        }

        if (mLoadedTasks.size() >= 1){
            TaskDescription ad = mLoadedTasks.get(0);
            am.removeTask(ad.getPersistentTaskId(),
                    ActivityManager.REMOVE_TASK_KILL_PROCESS);
            if(DEBUG){
                Log.d(TAG, "kill " + ad.getPackageName());
            }
            ad.setKilled();
        }
        if(close){
            close();
        }
    }

    public void goHome(boolean close) {
        if(close){
            close();
        }

        Intent homeIntent = new Intent(Intent.ACTION_MAIN, null);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mContext.startActivity(homeIntent);
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        mLayout.updatePrefs(prefs, key);
        mGestureView.updatePrefs(prefs, key);
    }

    public void toggleLastApp(boolean close) {
        if (mLoadedTasks.size() < 2) {
            if(close){
                close();
            }
            return;
        }

        TaskDescription ad = mLoadedTasks.get(1);
        switchTask(ad, close);
    }
    
    public void startIntentFromtString(String intent, boolean close) {
        if(close){
            close();
        }

        try {
            Intent intentapp = Intent.parseUri(intent, 0);
            mContext.startActivity(intentapp);
        } catch (URISyntaxException e) {
            Log.e(TAG, "URISyntaxException: [" + intent + "]");
        } catch (ActivityNotFoundException e){
            Log.e(TAG, "ActivityNotFound: [" + intent + "]");
        }
    }
    
    public void updateLayout() {
        if (mLayout.isShowing()){
            mLayout.updateLayout();
        }
        if (mGestureView.isShowing()){
            mGestureView.updateLayout();
        }
    }

    public void close(){
        Intent hideRecent = new Intent(
                SwitchService.RecentsReceiver.ACTION_HIDE_OVERLAY);
        mContext.sendBroadcast(hideRecent);
    }

    public void startApplicationDetailsActivity(String packageName) {
        close();

        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(
                        "package", packageName, null));
        intent.setComponent(intent.resolveActivity(mContext.getPackageManager()));
        TaskStackBuilder.create(mContext)
                .addNextIntentWithParentStack(intent).startActivities();
    }

    public void startSettingssActivity() {
        close();

        Intent intent = new Intent(Settings.ACTION_SETTINGS, null);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mContext.startActivity(intent);
    }

    public void startOmniSwitchSettingsActivity() {
        close();

        Intent mainActivity = new Intent(mContext,
                SettingsActivity.class);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        mContext.startActivity(mainActivity);
    }

    public void shutdownService() {
        mLayout.shutdownService();
    }
}
