/*
 * Copyright (C) 2011 The Android Open Source Project
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
import java.util.List;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Process;
import android.util.Log;

public class RecentTasksLoader {
    static final String TAG = "RecentTasksLoader";
    static final boolean DEBUG = true;

    private static final int DISPLAY_TASKS = 20;
    private static final int MAX_TASKS = DISPLAY_TASKS + 1; // allow extra for non-apps

    private RecentsManager mRecentsManager;
    private Context mContext;

    private AsyncTask<Void, ArrayList<TaskDescription>, Void> mTaskLoader;
    private Handler mHandler;

    private ArrayList<TaskDescription> mLoadedTasks;

    private enum State { LOADING, LOADED, CANCELLED };
    private State mState = State.CANCELLED;

    private int mIconDpi;

    private static RecentTasksLoader sInstance;
    public static RecentTasksLoader getInstance(Context context, RecentsManager manager) {
        if (sInstance == null) {
            sInstance = new RecentTasksLoader(context, manager);
        }
        return sInstance;
    }

    public static void killInstance(){
    	sInstance = null;
    }

    private RecentTasksLoader(Context context, RecentsManager manager) {
        mContext = context;
        mRecentsManager = manager;
        mHandler = new Handler();

        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mIconDpi = activityManager.getLauncherLargeIconDensity();
    }

    public ArrayList<TaskDescription> getLoadedTasks() {
        return mLoadedTasks;
    }

    public void remove(TaskDescription td) {
        mLoadedTasks.remove(td);
    }

    private boolean isCurrentHomeActivity(ComponentName component, ActivityInfo homeInfo) {
        if (homeInfo == null) {
            final PackageManager pm = mContext.getPackageManager();
            homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .resolveActivityInfo(pm, 0);
        }
        return homeInfo != null
            && homeInfo.packageName.equals(component.getPackageName())
            && homeInfo.name.equals(component.getClassName());
    }

    // Create an TaskDescription, returning null if the title or icon is null
    TaskDescription createTaskDescription(int taskId, int persistentTaskId, Intent baseIntent,
            ComponentName origActivity, CharSequence description) {
        Intent intent = new Intent(baseIntent);
        if (origActivity != null) {
            intent.setComponent(origActivity);
        }
        final PackageManager pm = mContext.getPackageManager();
        intent.setFlags((intent.getFlags()&~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        final ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
        if (resolveInfo != null) {
            final ActivityInfo info = resolveInfo.activityInfo;
            final String title = info.loadLabel(pm).toString();

            if (title != null && title.length() > 0) {
                if (DEBUG) Log.v(TAG, "creating activity desc for id="
                        + persistentTaskId + ", label=" + title);

                TaskDescription item = new TaskDescription(taskId,
                        persistentTaskId, resolveInfo, baseIntent, info.packageName,
                        description);
                item.setLabel(title);

                return item;
            } else {
                if (DEBUG) Log.v(TAG, "SKIPPING item " + persistentTaskId);
            }
        }
        return null;
    }

    Runnable mPreloadTasksRunnable = new Runnable() {
            public void run() {
                loadTasksInBackground();
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
        mLoadedTasks = null;
        mState = State.CANCELLED;
    }

    public void loadTasksInBackground() {
        loadTasksInBackground(false);
    }
    public void loadTasksInBackground(final boolean zeroeth) {
        if (mState != State.CANCELLED) {
            return;
        }
        mState = State.LOADING;

        mTaskLoader = new AsyncTask<Void, ArrayList<TaskDescription>, Void>() {
            @Override
            protected void onProgressUpdate(ArrayList<TaskDescription>... values) {
                if (!isCancelled()) {
                    ArrayList<TaskDescription> newTasks = values[0];
                    // do a callback to RecentsPanelView to let it know we have more values
                    // how do we let it know we're all done? just always call back twice
                    if (mLoadedTasks == null) {
                        mLoadedTasks = new ArrayList<TaskDescription>();
                    }
                    mLoadedTasks.addAll(newTasks);
                    mRecentsManager.update(mLoadedTasks);
                }
            }
            @Override
            protected Void doInBackground(Void... params) {
                // We load in two stages: first, we update progress with just the first screenful
                // of items. Then, we update with the rest of the items
                final int origPri = Process.getThreadPriority(Process.myTid());
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                final PackageManager pm = mContext.getPackageManager();
                final ActivityManager am = (ActivityManager)
                mContext.getSystemService(Context.ACTIVITY_SERVICE);

                final List<ActivityManager.RecentTaskInfo> recentTasks =
                        am.getRecentTasks(MAX_TASKS, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
                int numTasks = recentTasks.size();
                ActivityInfo homeInfo = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME).resolveActivityInfo(pm, 0);

                ArrayList<TaskDescription> tasks = new ArrayList<TaskDescription>();

                // skip the first task - assume it's either the home screen or the current activity.
                final int first = 0;
                for (int i = first, index = 0; i < numTasks && (index < MAX_TASKS); ++i) {
                    if (isCancelled()) {
                        break;
                    }
                    final ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(i);

                    Intent intent = new Intent(recentInfo.baseIntent);
                    if (recentInfo.origActivity != null) {
                        intent.setComponent(recentInfo.origActivity);
                    }

                    // Don't load the current home activity.
                    if (isCurrentHomeActivity(intent.getComponent(), homeInfo)) {
                        continue;
                    }

                    // Don't load ourselves
                    if (intent.getComponent().getPackageName().equals(mContext.getPackageName())) {
                        continue;
                    }

                    TaskDescription item = createTaskDescription(recentInfo.id,
                            recentInfo.persistentId, recentInfo.baseIntent,
                            recentInfo.origActivity, recentInfo.description);

                    if (item != null) {
                        tasks.add(item);
                        loadTaskIcon(item);
                        ++index;
                    }
                }
                if (!isCancelled()) {
                    publishProgress(tasks);
                }

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

    Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(),
                R.drawable.ic_launcher);
    }

    Drawable getFullResIcon(Resources resources, int iconId) {
        try {
            return resources.getDrawableForDensity(iconId, mIconDpi);
        } catch (Resources.NotFoundException e) {
            return getFullResDefaultActivityIcon();
        }
    }

    private Drawable getFullResIcon(ResolveInfo info, PackageManager packageManager) {
        Resources resources;
        try {
            resources = packageManager.getResourcesForApplication(
                    info.activityInfo.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.activityInfo.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }
    
    /*void loadTaskThumbnail(TaskDescription td) {
        final ActivityManager am = (ActivityManager)
                mContext.getSystemService(Context.ACTIVITY_SERVICE);
        Bitmap thumbnail = am.getTaskTopThumbnail(td.persistentTaskId);

        synchronized (td) {
            if (thumbnail != null) {
                td.setThumbnail(thumbnail);
            }
        }
    }*/
}
