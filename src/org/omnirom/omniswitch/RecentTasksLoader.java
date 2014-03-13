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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.omnirom.omniswitch.ui.BitmapCache;
import org.omnirom.omniswitch.ui.IconPackHelper;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Process;
import android.util.Log;

public class RecentTasksLoader {
    private static final String TAG = "RecentTasksLoader";
    private static final boolean DEBUG = false;

    private static final int DISPLAY_TASKS = 20;
    private static final int MAX_TASKS = DISPLAY_TASKS + 1; // allow extra for
                                                            // non-apps

    private Context mContext;
    private AsyncTask<Void, List<TaskDescription>, Void> mTaskLoader;
    private Handler mHandler;
    private List<TaskDescription> mLoadedTasks;
    private boolean mPreloaded;
    private SwitchManager mSwitchManager;

    private enum State {
        LOADING, IDLE
    };

    private State mState = State.IDLE;

    private SwitchConfiguration mConfiguration;

    private static RecentTasksLoader sInstance;

    public static RecentTasksLoader getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RecentTasksLoader(context);
        }
        return sInstance;
    }

    public static void killInstance() {
        sInstance = null;
    }

    private RecentTasksLoader(Context context) {
        mContext = context;
        mHandler = new Handler();
        mLoadedTasks = new ArrayList<TaskDescription>();
        mConfiguration = SwitchConfiguration.getInstance(mContext);
    }

    public List<TaskDescription> getLoadedTasks() {
        return mLoadedTasks;
    }

    public void remove(TaskDescription td) {
        mLoadedTasks.remove(td);
    }

    private boolean isCurrentHomeActivity(ComponentName component,
            ActivityInfo homeInfo) {
        if (homeInfo == null) {
            final PackageManager pm = mContext.getPackageManager();
            homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(
                    Intent.CATEGORY_HOME).resolveActivityInfo(pm, 0);
        }
        return homeInfo != null
                && homeInfo.packageName.equals(component.getPackageName())
                && homeInfo.name.equals(component.getClassName());
    }

    // Create an TaskDescription, returning null if the title or icon is null
    TaskDescription createTaskDescription(int taskId, int persistentTaskId,
            Intent baseIntent, ComponentName origActivity,
            CharSequence description) {
        Intent intent = new Intent(baseIntent);
        if (origActivity != null) {
            intent.setComponent(origActivity);
        }
        final PackageManager pm = mContext.getPackageManager();
        intent.setFlags((intent.getFlags() & ~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        final ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
        if (resolveInfo != null) {
            final ActivityInfo info = resolveInfo.activityInfo;
            final String title = info.loadLabel(pm).toString();

            if (title != null && title.length() > 0) {
                if (DEBUG)
                    Log.v(TAG, "creating activity desc for id="
                            + persistentTaskId + ", label=" + title);

                TaskDescription item = new TaskDescription(taskId,
                        persistentTaskId, resolveInfo, baseIntent,
                        info.packageName, description);
                item.setLabel(title);

                return item;
            } else {
                if (DEBUG)
                    Log.v(TAG, "SKIPPING item " + persistentTaskId);
            }
        }
        return null;
    }

    Runnable mPreloadTasksRunnable = new Runnable() {
        public void run() {
            loadTasksInBackground(null);
        }
    };

    public void preloadTasks() {
        mHandler.post(mPreloadTasksRunnable);
    }

    public void cancelPreloadingTasks() {
        cancelLoadingTasks();
        mHandler.removeCallbacks(mPreloadTasksRunnable);
    }

    public void cancelLoadingTasks() {
        if (mTaskLoader != null) {
            mTaskLoader.cancel(false);
            mTaskLoader = null;
        }
        mLoadedTasks.clear();
        mPreloaded = false;
        mState = State.IDLE;
    }

    public void loadTasksInBackground(final SwitchManager manager) {
        if (DEBUG){
            Log.d(TAG, "loadTasksInBackground " + manager + " start " + System.currentTimeMillis());
        }
        mSwitchManager = manager;

        if(mPreloaded && mSwitchManager != null){
            if (DEBUG){
                Log.d(TAG, "recents preloaded");
            }
            mSwitchManager.update(mLoadedTasks);
            mPreloaded = false;
            return;
        }

        if (mState != State.IDLE) {
            return;
        }
        if (DEBUG){
            Log.d(TAG, "recents load");
        }
        mState = State.LOADING;
        mLoadedTasks.clear();

        mTaskLoader = new AsyncTask<Void, List<TaskDescription>, Void>() {
            @Override
            protected void onProgressUpdate(
                    List<TaskDescription>... values) {
                if (!isCancelled()) {
                    List<TaskDescription> newTasks = values[0];
                    mLoadedTasks.addAll(newTasks);
                    if (mSwitchManager != null){
                        if (DEBUG){
                            Log.d(TAG, "recents loaded");
                        }
                        mSwitchManager.update(mLoadedTasks);
                    } else {
                        mPreloaded = true;
                    }
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                final int origPri = Process.getThreadPriority(Process.myTid());
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                final PackageManager pm = mContext.getPackageManager();
                final ActivityManager am = (ActivityManager) mContext
                        .getSystemService(Context.ACTIVITY_SERVICE);

                final List<ActivityManager.RecentTaskInfo> recentTasks = am
                        .getRecentTasks(MAX_TASKS,
                                ActivityManager.RECENT_IGNORE_UNAVAILABLE);
                int numTasks = recentTasks.size();
                ActivityInfo homeInfo = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME).resolveActivityInfo(
                                pm, 0);

                List<TaskDescription> tasks = new ArrayList<TaskDescription>();

                for (int i = 0; i < numTasks; ++i) {
                    if (isCancelled()) {
                        break;
                    }
                    final ActivityManager.RecentTaskInfo recentInfo = recentTasks
                            .get(i);

                    Intent intent = new Intent(recentInfo.baseIntent);
                    if (recentInfo.origActivity != null) {
                        intent.setComponent(recentInfo.origActivity);
                    }

                    // Don't load the current home activity.
                    if (isCurrentHomeActivity(intent.getComponent(), homeInfo)) {
                        continue;
                    }

                    // Don't load ourselves
                    if (intent.getComponent().getPackageName()
                            .equals(mContext.getPackageName())) {
                        continue;
                    }

                    TaskDescription item = createTaskDescription(recentInfo.id,
                            recentInfo.persistentId, recentInfo.baseIntent,
                            recentInfo.origActivity, recentInfo.description);

                    if (item != null) {
                        // TODO we would not need to do that if already loaded
                        tasks.add(item);
                        loadTaskIcon(item);
                    }
                }
                if (!isCancelled()) {
                    publishProgress(tasks);
                }
                if (DEBUG){
                    Log.d(TAG, "loadTasksInBackground end " + System.currentTimeMillis());
                }
                mState = State.IDLE;
                Process.setThreadPriority(origPri);
                return null;
            }
        };
        mTaskLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    void loadTaskIcon(TaskDescription td) {
        final PackageManager pm = mContext.getPackageManager();
        Drawable icon = getFullResIcon(td.resolveInfo, pm);

        synchronized (td) {
            if (icon != null) {
                td.setIcon(icon);
            }
            td.setLoaded(true);
        }
    }

    private Drawable getFullResDefaultActivityIcon(ActivityInfo activityInfo) {
        return getFullResIcon(mContext.getResources(), R.drawable.ic_default, activityInfo);
    }

    private Drawable getFullResIcon(Resources resources, int iconId, ActivityInfo activityInfo) {
       return BitmapCache.getInstance(mContext).getResized(resources, activityInfo, iconId, mConfiguration);
    }

    private Drawable getFullResIcon(ResolveInfo info,
            PackageManager packageManager) {
        Resources resources;
        try {
            resources = packageManager
                    .getResourcesForApplication(info.activityInfo.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            if (IconPackHelper.getInstance(mContext).isIconPackLoaded()){
                int iconId = IconPackHelper.getInstance(mContext).getResourceIdForActivityIcon(info.activityInfo);
                if (iconId != 0) {
                    return getFullResIcon(IconPackHelper.getInstance(mContext).getIconPackResources(), iconId, info.activityInfo);
                }
            }
            return getFullResIcon(resources, info.activityInfo.getIconResource(), info.activityInfo);
        }
        return getFullResDefaultActivityIcon(info.activityInfo);
    }

//    public Bitmap loadThumbnail(TaskDescription td) {
//        final ActivityManager am = (ActivityManager)
//                mContext.getSystemService(Context.ACTIVITY_SERVICE);
//        final PackageManager pm = mContext.getPackageManager();
//        return am.getTaskTopThumbnail(td.persistentTaskId);
//    }
}
