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
import java.util.Iterator;
import java.util.List;

import org.omnirom.omniswitch.ui.ISwitchLayout;
import org.omnirom.omniswitch.ui.SwitchGestureView;
import org.omnirom.omniswitch.ui.SwitchLayout;
import org.omnirom.omniswitch.ui.SwitchLayoutVertical;

import android.app.ActivityManager;
import android.app.ActivityOptions;
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
    private static final boolean DEBUG = false;
    private List<TaskDescription> mLoadedTasks;
    private List<TaskDescription> mActiveTasks;
    private ISwitchLayout mLayout;
    private SwitchGestureView mGestureView;
    private Context mContext;
    private SwitchConfiguration mConfiguration;
    private int mLayoutStyle;

    public SwitchManager(Context context, int layoutStyle) {
        mContext = context;
        mConfiguration = SwitchConfiguration.getInstance(mContext);
        mLayoutStyle = layoutStyle;
        init();
    }

    public void hide(boolean fast) {
        if (isShowing()) {
            if(DEBUG){
                Log.d(TAG, "hide");
            }
            mLayout.hide(fast);
        }
    }

    public void hideHidden() {
        if (isShowing()) {
            if(DEBUG){
                Log.d(TAG, "hideHidden");
            }
            mLayout.hideHidden();
        }
    }

    public void show() {
        if (!isShowing()) {
            if(DEBUG){
                Log.d(TAG, "show");
            }
            mLayout.setHandleRecentsUpdate(true);

            clearTasks();
            RecentTasksLoader.getInstance(mContext).cancelLoadingTasks();
            RecentTasksLoader.getInstance(mContext).setSwitchManager(this);
            RecentTasksLoader.getInstance(mContext).loadTasksInBackground();

            // show immediately
            mLayout.show();
        }
    }

    public void showHidden() {
        if (!isShowing()) {
            if(DEBUG){
                Log.d(TAG, "showHidden");
            }
            mLayout.setHandleRecentsUpdate(true);

            // show immediately
            mLayout.showHidden();
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
        mActiveTasks = new ArrayList<TaskDescription>();
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

    public void update(List<TaskDescription> taskList) {
        if(DEBUG){
            Log.d(TAG, "update");
        }
        mLoadedTasks.clear();
        mLoadedTasks.addAll(taskList);
        filterActiveTasks();
        mLayout.update();
        mGestureView.update();
    }

    public void switchTask(TaskDescription ad, boolean close, boolean customAnim) {
        if (ad.isKilled()) {
            return;
        }
        final ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);

        if(DEBUG){
            Log.d(TAG, "switch to " + ad.getPackageName());
        }

        if(close){
            hide(true);
        }

        if (ad.getTaskId() >= 0) {
            // This is an active task; it should just go to the foreground.
            if (customAnim) {
                final ActivityOptions opts = ActivityOptions.makeCustomAnimation(mContext,
                        R.anim.last_app_in,
                        R.anim.last_app_out);
                am.moveTaskToFront(ad.getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION, opts.toBundle());
            } else {
                am.moveTaskToFront(ad.getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
            }
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

        am.removeTask(ad.getPersistentTaskId());
        if(DEBUG){
            Log.d(TAG, "kill " + ad.getPackageName());
        }
        ad.setKilled();
        mLoadedTasks.remove(ad);
        mActiveTasks.remove(ad);
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
        final ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);

        if (mLoadedTasks.size() == 0) {
            if(close){
                hide(true);
            }
            return;
        }

        Iterator<TaskDescription> nextTask = mLoadedTasks.iterator();
        while (nextTask.hasNext()) {
            TaskDescription ad = nextTask.next();
            am.removeTask(ad.getPersistentTaskId());
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

        if (mActiveTasks.size() <= 1) {
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
            am.removeTask(ad.getPersistentTaskId());
            if(DEBUG){
                Log.d(TAG, "kill " + ad.getPackageName());
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
        final ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);

        if (mActiveTasks.size() == 0) {
            if(close){
                hide(true);
            }
            return;
        }

        if (mActiveTasks.size() >= 1){
            TaskDescription ad = mActiveTasks.get(0);
            am.removeTask(ad.getPersistentTaskId());
            if(DEBUG){
                Log.d(TAG, "kill " + ad.getPackageName());
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
            String layoutStyle = prefs.getString(SettingsActivity.PREF_LAYOUT_STYLE, "0");
            mLayoutStyle = Integer.valueOf(layoutStyle);
            switchLayout();
        }
        mLayout.updatePrefs(prefs, key);
        mGestureView.updatePrefs(prefs, key);
    }

    public void toggleLastApp(boolean close) {
        if (mActiveTasks.size() < 2) {
            if(close){
                hide(true);
            }
            return;
        }

        TaskDescription ad = mActiveTasks.get(1);
        switchTask(ad, close, true);
    }

    public void startIntentFromtString(String intent, boolean close) {
        if(close){
            hide(true);
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

    public void startApplicationDetailsActivity(String packageName) {
        hide(true);

        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(
                        "package", packageName, null));
        intent.setComponent(intent.resolveActivity(mContext.getPackageManager()));
        TaskStackBuilder.create(mContext)
                .addNextIntentWithParentStack(intent).startActivities();
    }

    public void startSettingssActivity() {
        hide(true);

        Intent intent = new Intent(Settings.ACTION_SETTINGS, null);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mContext.startActivity(intent);
    }

    public void startOmniSwitchSettingsActivity() {
        hide(true);

        Intent mainActivity = new Intent(mContext,
                SettingsActivity.class);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        mContext.startActivity(mainActivity);
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
        return mActiveTasks;
    }

    public void clearTasks() {
        mLoadedTasks.clear();
        mActiveTasks.clear();
    }

    private void filterActiveTasks() {
        mActiveTasks.clear();
        Iterator<TaskDescription> nextTask = mLoadedTasks.iterator();
        while(nextTask.hasNext()) {
            TaskDescription ad = nextTask.next();
            if (ad.isActive()) {
                mActiveTasks.add(ad);
            }
        }
    }
}
