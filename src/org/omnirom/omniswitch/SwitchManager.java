/*
 *  Copyright (C) 2013-2015 The OmniROM Project
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.omnirom.omniswitch.ui.ISwitchLayout;
import org.omnirom.omniswitch.ui.SwitchGestureView;
import org.omnirom.omniswitch.ui.SwitchLayout;
import org.omnirom.omniswitch.ui.SwitchLayoutVertical;

import android.app.ActivityManager;
import static android.app.ActivityManager.StackId.DOCKED_STACK_ID;
import static android.app.ActivityManager.StackId.HOME_STACK_ID;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.TaskStackBuilder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

public class SwitchManager {
    private static final String TAG = "SwitchManager";
    private static final boolean DEBUG = false;
    private List<TaskDescription> mLoadedTasks;
    private ISwitchLayout mLayout;
    private SwitchGestureView mGestureView;
    private Context mContext;
    private SwitchConfiguration mConfiguration;
    private int mLayoutStyle;
    private final ActivityManager mAm;
    private TaskDescription mDockedTask;
    private TaskDescription mTopHomeTask;
    private boolean mRestoreStack;
    private TaskDescription mPlaceholderTask;

    public SwitchManager(Context context, int layoutStyle) {
        mContext = context;
        mConfiguration = SwitchConfiguration.getInstance(mContext);
        mLayoutStyle = layoutStyle;
        mAm = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        init();
    }

    public void hide(boolean fast) {
        if (isShowing()) {
            if (DEBUG){
                Log.d(TAG, "hide");
            }
            mLayout.hide(fast);
        }
    }

    public void hideHidden() {
        if (isShowing()) {
            if (DEBUG){
                Log.d(TAG, "hideHidden");
            }
            mLayout.hideHidden();
        }
    }

    public void show() {
        if (!isShowing()) {
            if (DEBUG){
                Log.d(TAG, "show");
            }
            mLayout.setHandleRecentsUpdate(true);
            mRestoreStack = false;

            clearTasks();
            RecentTasksLoader.getInstance(mContext).cancelLoadingTasks();
            RecentTasksLoader.getInstance(mContext).setSwitchManager(this);
            RecentTasksLoader.getInstance(mContext).loadTasksInBackground(0, true, true);

            // show immediately
            mLayout.show();
        }
    }

    public void showHidden() {
        if (!isShowing()) {
            if (DEBUG){
                Log.d(TAG, "showHidden");
            }
            mLayout.setHandleRecentsUpdate(true);
            mRestoreStack = false;

            // show immediately
            mLayout.showHidden();
        }
    }

    public boolean isShowing() {
        return mLayout.isShowing();
    }

    private void init() {
        if (DEBUG){
            Log.d(TAG, "init");
        }

        mLoadedTasks = new ArrayList<TaskDescription>();
        switchLayout();
        mGestureView = new SwitchGestureView(this, mContext);
    }

    public void killManager() {
        RecentTasksLoader.killInstance();
        mGestureView.hide();
    }

    public ISwitchLayout getLayout() {
        return mLayout;
    }

    private void switchLayout() {
        if (mLayout != null) {
            mLayout.shutdownService();
        }
        if (mLayoutStyle == 0) {
            mLayout = new SwitchLayout(this, mContext);
        } else {
            mLayout = new SwitchLayoutVertical(this, mContext);
        }

    }
    public SwitchGestureView getSwitchGestureView() {
        return mGestureView;
    }

    public void update(List<TaskDescription> taskList, TaskDescription dockedTask,
            TaskDescription topHomeTask, TaskDescription placeholderTask) {
        if (DEBUG){
            Log.d(TAG, "update mRestoreStack= " + mRestoreStack);
        }
        if (!mRestoreStack) {
            mLoadedTasks.clear();
            mLoadedTasks.addAll(taskList);
        }
        mDockedTask = dockedTask;
        mPlaceholderTask = placeholderTask;
        mTopHomeTask = topHomeTask;
        setFocusStack();

        if (!mRestoreStack) {
            mLayout.update();
            mGestureView.update();
        }
        mRestoreStack = false;
    }

    public void switchTask(TaskDescription ad, boolean close, boolean customAnim) {
        if (ad.isKilled()) {
            return;
        }
        if(close){
            hide(true);
        }

        try {
            ActivityOptions options = null;
            if (customAnim) {
                options = ActivityOptions.makeCustomAnimation(mContext, R.anim.last_app_in, R.anim.last_app_out);
            } else {
                options = ActivityOptions.makeBasic();
            }
            ActivityManagerNative.getDefault().startActivityFromRecents(
                        ad.getPersistentTaskId(),  options.toBundle());
            SwitchStatistics.getInstance(mContext).traceStartIntent(ad.getIntent());
            if (DEBUG){
                Log.d(TAG, "switch to " + ad.getLabel() + " " + ad.getStackId());
            }
        } catch (Exception e) {
        }
    }

    public void killTask(TaskDescription ad) {
        if (mConfiguration.mRestrictedMode){
            return;
        }

        mAm.removeTask(ad.getPersistentTaskId());
        if (DEBUG){
            Log.d(TAG, "kill " + ad.getLabel());
        }
        ad.setKilled();
        mLoadedTasks.remove(ad);
        // make sure we stay on the correct focus
        // and killing is not changing it - overlay stays open here
        restoreHomeStack();
        mLayout.refresh();
    }

    /**
     * killall will always remove all tasks - also those that are
     * filtered out (not active)
     * @param close
     */
    public void killAll(boolean close) {
        if (mConfiguration.mRestrictedMode){
            return;
        }

        if (mLoadedTasks.size() == 0) {
            if(close){
                hide(true);
            }
            return;
        }

        Iterator<TaskDescription> nextTask = mLoadedTasks.iterator();
        while (nextTask.hasNext()) {
            TaskDescription ad = nextTask.next();
            mAm.removeTask(ad.getPersistentTaskId());
            if (DEBUG){
                Log.d(TAG, "kill " + ad.getLabel());
            }
            ad.setKilled();
        }
        goHome(close);
    }

    public void killOther(boolean close) {
        if (mConfiguration.mRestrictedMode){
            return;
        }

        if (getTasks().size() <= 1) {
            if(close){
                hide(true);
            }
            return;
        }
        Iterator<TaskDescription> nextTask = mLoadedTasks.iterator();
        // skip active task
        nextTask.next();
        while (nextTask.hasNext()) {
            TaskDescription ad = nextTask.next();
            mAm.removeTask(ad.getPersistentTaskId());
            if (DEBUG){
                Log.d(TAG, "kill " + ad.getLabel());
            }
            ad.setKilled();
        }
        if(close){
            hide(true);
        }
    }

    public void killCurrent(boolean close) {
        if (mConfiguration.mRestrictedMode){
            return;
        }

        if (getTasks().size() == 0) {
            if(close){
                hide(true);
            }
            return;
        }

        if (getTasks().size() >= 1){
            TaskDescription ad = getTasks().get(0);
            mAm.removeTask(ad.getPersistentTaskId());
            if (DEBUG){
                Log.d(TAG, "kill " + ad.getLabel());
            }
            ad.setKilled();
        }
        if(close){
            hide(true);
        }
    }

    public void goHome(boolean close) {
        if(close){
            hide(true);
        }

        Intent homeIntent = new Intent(Intent.ACTION_MAIN, null);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mContext.startActivity(homeIntent);
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        if (key != null && key.equals(SettingsActivity.PREF_LAYOUT_STYLE)) {
            String layoutStyle = prefs.getString(SettingsActivity.PREF_LAYOUT_STYLE, "1");
            mLayoutStyle = Integer.valueOf(layoutStyle);
            switchLayout();
        }
        mLayout.updatePrefs(prefs, key);
        mGestureView.updatePrefs(prefs, key);
    }

    public void toggleLastApp(boolean close) {
        if (getTasks().size() < 2) {
            if(close){
                hide(true);
            }
            return;
        }

        TaskDescription ad = getTasks().get(1);
        switchTask(ad, close, true);
    }

    public void startIntentFromtString(String intent, boolean close) {
        if(close){
            hide(true);
        }
        startIntentFromtString(mContext, intent);
    }

    public static void startIntentFromtString(Context context, String intent) {
        try {
            Intent intentapp = Intent.parseUri(intent, 0);
            SwitchStatistics.getInstance(context).traceStartIntent(intentapp);
            context.startActivity(intentapp);
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

    public void startApplicationDetailsActivity(String packageName) {
        hide(true);
        startApplicationDetailsActivity(mContext, packageName);
    }

    public static void startApplicationDetailsActivity(Context context, String packageName) {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null));
        intent.setComponent(intent.resolveActivity(context.getPackageManager()));
        TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intent).startActivities();
    }

    public void startSettingsActivity() {
        hide(true);
        startSettingsActivity(mContext);
    }

    public static void startSettingsActivity(Context context) {
        Intent intent = new Intent(Settings.ACTION_SETTINGS, null);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);
    }

    public void startOmniSwitchSettingsActivity() {
        hide(true);
        startOmniSwitchSettingsActivity(mContext);
    }

    public static void startOmniSwitchSettingsActivity(Context context) {
        Intent mainActivity = new Intent(context,
                SettingsActivity.class);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(mainActivity);
    }

    public static void startPlaceholderActivity(Context context) {
        Intent mainActivity = new Intent(context,
                PlaceholderActivity.class);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(mainActivity);
    }

    public void shutdownService() {
        mLayout.shutdownService();
    }

    public void slideLayout(float distanceX) {
        mLayout.slideLayout(distanceX);
    }

    public void finishSlideLayout() {
        mLayout.finishSlideLayout();
    }

    public void openSlideLayout(boolean fromFling) {
        mLayout.openSlideLayout(fromFling);
    }

    public void canceSlideLayout() {
        mLayout.canceSlideLayout();
    }

    public List<TaskDescription> getTasks() {
	    return mLoadedTasks;
    }

    public void clearTasks() {
        mLoadedTasks.clear();
    }

    private TaskDescription getCurrentTopTask() {
        if (getTasks().size() >= 1) {
            TaskDescription ad = getTasks().get(0);
            return ad;
        } else {
            return null;
        }
    }

    private TaskDescription getUndockedTopTask(TaskDescription excludeTask) {
        for (TaskDescription ad : getTasks()) {
            if (ad.getStackId() != DOCKED_STACK_ID) {
                if (excludeTask == null) {
                    return ad;
                } else if (ad.getPersistentTaskId() != excludeTask.getPersistentTaskId()) {
                    return ad;
                }
            }
        }
        return null;
    }

    public TaskDescription getLastTask() {
        if (getTasks().size() < 2) {
            return null;
        }

        return getTasks().get(1);
    }

    public void lockToCurrentApp(boolean close) {
        TaskDescription ad = getCurrentTopTask();
        if (ad != null) {
            lockToApp(ad, close);
        }
    }

    public void lockToApp(TaskDescription ad, boolean close) {
        try {
            if (!ActivityManagerNative.getDefault().isInLockTaskMode()) {
                switchTask(ad, false, false);
                ActivityManagerNative.getDefault().startSystemLockTaskMode(ad.getPersistentTaskId());
                if (DEBUG){
                    Log.d(TAG, "lock app " + ad.getLabel() + " " + ad.getPersistentTaskId());
                }
            }
        } catch(RemoteException e) {
        }
        if(close){
            hide(true);
        }
    }

    public void stopLockToApp(boolean close) {
        try {
            if (ActivityManagerNative.getDefault().isInLockTaskMode()) {
                ActivityManagerNative.getDefault().stopLockTaskMode();
                if (DEBUG){
                    Log.d(TAG, "stop lock app");
                }
            }
        } catch(RemoteException e) {
        }
        if(close){
            hide(true);
        }
    }

    public void toggleLockToApp(boolean close) {
        try {
            if (ActivityManagerNative.getDefault().isInLockTaskMode()) {
                stopLockToApp(false);
            } else {
                lockToCurrentApp(false);
            }
        } catch(RemoteException e) {
        }
        if(close){
            hide(true);
        }
    }

    public boolean isDockedTask(TaskDescription ad) {
        return mDockedTask != null && ad.getPersistentTaskId() == mDockedTask.getPersistentTaskId();
    }

    /**
     * make sure we set the focus stack to home whenever we open
     * else we will open apps in the docked stack if last focus was there
     * focus cant change while we are open
     */
    public void setFocusStack() {
        if (DEBUG){
            Log.d(TAG, "setFocusStack " + Utils.isDockingActive());
        }
        if (Utils.isDockingActive()){
            if (mPlaceholderTask == null) {
                if (DEBUG){
                    Log.d(TAG, "start placeholder activity");
                }
                // make sure the placeholder activity gets activated if needed
                startPlaceholderActivity(mContext);
            }
            try {
                TaskDescription activateTask = null;
                if (mTopHomeTask != null) {
                    // we want to restore also activities that are excluded from recents
                    if (DEBUG) {
                        Log.d(TAG, "setFocusStack mTopHomeTask = " + mTopHomeTask.getIntent());
                    }
                    activateTask = mTopHomeTask;
                } else {
                    TaskDescription ad = getUndockedTopTask(null);
                    if (ad != null) {
                        if (DEBUG) {
                            Log.d(TAG, "setFocusStack topUndockedTask = " + ad.getIntent());
                        }
                        activateTask = ad;
                    } else if (mPlaceholderTask != null) {
                        if (DEBUG) {
                            Log.d(TAG, "setFocusStack mPlaceholderTask = " + mPlaceholderTask.getIntent());
                        }
                        activateTask = mPlaceholderTask;
                    }
                }
                if (activateTask != null) {
                    ActivityManagerNative.getDefault().startActivityFromRecents(
                            activateTask.getPersistentTaskId(), ActivityOptions.makeBasic().toBundle());
                }
            } catch (Exception e) {
            }
        }
    }

    public void dockTask(TaskDescription ad, boolean close) {
        if(close){
            hide(true);
        }
        try {
            if (isDockedTask(ad)) {
                // undock
                if (DEBUG){
                    Log.d(TAG, "undock task " + ad.getIntent());
                }
                ActivityManagerNative.getDefault().moveTasksToFullscreenStack(
                        DOCKED_STACK_ID, true /* onTop */);
                mDockedTask = null;
                return;
            }
            if (mDockedTask != null) {
                // undock old
                if (DEBUG){
                    Log.d(TAG, "undock task " + mDockedTask.getIntent());
                }
                ActivityManagerNative.getDefault().moveTasksToFullscreenStack(
                        DOCKED_STACK_ID, false /* onTop */);
            }
            // dock new
            if (DEBUG){
                Log.d(TAG, "dock task " + ad.getIntent());
            }
            // get it before changing anything
            TaskDescription topUndocked = getUndockedTopTask(ad);

            if (DEBUG){
                Log.d(TAG, "start placeholder activity");
            }
            // always start the placeholder activity to linger in the bg
            startPlaceholderActivity(mContext);

            int createMode = ActivityManager.DOCKED_STACK_CREATE_MODE_TOP_OR_LEFT;
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setDockCreateMode(createMode);
            options.setLaunchStackId(DOCKED_STACK_ID);
            ActivityManagerNative.getDefault().startActivityFromRecents(
                    ad.getPersistentTaskId(),  options.toBundle());
            // using this is faster then using onTop == true above
            if (mDockedTask != null) {
                // if we unddocked another task move that on top of the home stack
                // we simply assume that this task is alive at this point
                mAm.moveTaskToFront(mDockedTask.getPersistentTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
            } else {
                if (topUndocked != null) {
                    if (DEBUG){
                        Log.d(TAG, "top undocked task " + topUndocked.getIntent());
                    }
                    // can be a dead task so make sure it gets started
                    ActivityManagerNative.getDefault().startActivityFromRecents(
                            topUndocked.getPersistentTaskId(), ActivityOptions.makeBasic().toBundle());
                }
            }
            mDockedTask = ad;
        } catch (Exception e) {
        }
    }

    public void restoreHomeStack() {
        if (Utils.isDockingActive()){
            if (DEBUG) {
                Log.d(TAG, "restoreHomeStack");
            }
            mRestoreStack = true;
            RecentTasksLoader.getInstance(mContext).cancelLoadingTasks();
            RecentTasksLoader.getInstance(mContext).setSwitchManager(this);
            RecentTasksLoader.getInstance(mContext).loadTasksInBackground(10, false, false);
        }
    }
}
