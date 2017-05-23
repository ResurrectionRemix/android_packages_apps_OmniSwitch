/*
 *  Copyright (C) 2013-2016 The OmniROM Project
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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.omnirom.omniswitch.ui.BitmapCache;
import org.omnirom.omniswitch.ui.BitmapUtils;
import org.omnirom.omniswitch.ui.IconPackHelper;

import android.app.ActivityManager;
import static android.app.ActivityManager.StackId.DOCKED_STACK_ID;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

public class RecentTasksLoader {
    private static final String TAG = "RecentTasksLoader";
    private static final boolean DEBUG = false;
    private static final int THUMB_INIT_LOAD = 6;
    private static final int TASK_INIT_LOAD = 8;

    private Context mContext;
    private AsyncTask<Void, List<TaskDescription>, Void> mTaskLoader;
    private AsyncTask<Void, Void, Void> mTaskInfoLoader;
    private AsyncTask<Void, TaskDescription, Void> mThumbnailLoader;
    private Handler mHandler;
    private List<TaskDescription> mLoadedTasks;
    private boolean mPreloaded;
    private SwitchManager mSwitchManager;
    private ActivityManager mActivityManager;
    private Bitmap mDefaultThumbnail;
    private PreloadTaskRunnable mPreloadTasksRunnable;
    private boolean mHasThumbPermissions;
    private SwitchConfiguration mConfiguration;
    private TaskDescription mDockedTask;
    private TaskDescription mTopHomeTask;
    private TaskDescription mPlaceholderTask;
    private PackageManager mPackageManager;
    private Drawable mDefaultAppIcon;

    final static BitmapFactory.Options sBitmapOptions;

    static {
        sBitmapOptions = new BitmapFactory.Options();
        sBitmapOptions.inMutable = true;
        sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
    }

    private enum State {
        LOADING, IDLE
    };

    private State mState = State.IDLE;

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
        mLoadedTasks = new CopyOnWriteArrayList<TaskDescription>();
        mActivityManager = (ActivityManager)
                mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mPackageManager = mContext.getPackageManager();
        mDefaultThumbnail = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        mDefaultThumbnail.setHasAlpha(true);
        mDefaultThumbnail.eraseColor(0x00ffffff);
        mHasThumbPermissions = hasSystemPermission(context);
        mConfiguration = SwitchConfiguration.getInstance(mContext);
        mDefaultAppIcon = BitmapUtils.getDefaultActivityIcon(mContext);
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
            homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(
                    Intent.CATEGORY_HOME).resolveActivityInfo(mPackageManager, 0);
        }
        return homeInfo != null
                && homeInfo.packageName.equals(component.getPackageName())
                && homeInfo.name.equals(component.getClassName());
    }

    // Create an TaskDescription, returning null if the title or icon is null
    TaskDescription createTaskDescription(int taskId, int persistentTaskId, int stackId,
            Intent baseIntent, ComponentName origActivity) {
        // clear source bounds to find matching package intent
        baseIntent.setSourceBounds(null);
        Intent intent = new Intent(baseIntent);
        if (origActivity != null) {
            intent.setComponent(origActivity);
        }
        intent.setFlags((intent.getFlags() & ~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        final ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
        if (resolveInfo != null) {
            if (DEBUG)
                Log.v(TAG, "creating activity desc for id=" + persistentTaskId);
            TaskDescription ad = new TaskDescription(taskId,
                    persistentTaskId, resolveInfo, baseIntent, stackId);
            return ad;
        }
        return null;
    }

    private class PreloadTaskRunnable implements Runnable {
        @Override
        public void run() {
            if (DEBUG) {
                Log.d(TAG, "preload start " + System.currentTimeMillis());
            }
            loadTasksInBackground(0, true, true);
        }
    }

    public void preloadTasks() {
        mPreloadTasksRunnable = new PreloadTaskRunnable();
        mHandler.post(mPreloadTasksRunnable);
    }

    public void setSwitchManager(final SwitchManager manager) {
        mSwitchManager = manager;
    }

    public void cancelLoadingTasks() {
        if (DEBUG) {
            Log.d(TAG, "cancelLoadingTasks state = " + mState);
        }
        if (mTaskLoader != null) {
            mTaskLoader.cancel(true);
            mTaskLoader = null;
        }
        if (mTaskInfoLoader != null) {
            mTaskInfoLoader.cancel(true);
            mTaskInfoLoader = null;
        }
        if (mThumbnailLoader != null) {
            mThumbnailLoader.cancel(true);
            mThumbnailLoader = null;
        }
        if (mPreloadTasksRunnable != null) {
            mHandler.removeCallbacks(mPreloadTasksRunnable);
            mPreloadTasksRunnable = null;
        }
        mLoadedTasks.clear();
        mDockedTask = null;
        mTopHomeTask = null;
        mPlaceholderTask = null;
        mPreloaded = false;
        mState = State.IDLE;
    }

    public void loadTasksInBackground(int maxNumTasks, boolean withIcons, boolean withThumbs) {
        if (mPreloaded && mState != State.IDLE) {
            if (DEBUG) {
                Log.d(TAG, "recents preloaded: waiting for done");
            }
            return;
        }
        if (mPreloaded && mSwitchManager != null) {
            if (DEBUG) {
                Log.d(TAG, "recents preloaded " + mLoadedTasks);
            }
            mSwitchManager.update(mLoadedTasks, mDockedTask, mTopHomeTask, mPlaceholderTask);
            loadMissingTaskInfo();
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "recents load");
        }
        mPreloaded = true;
        mState = State.LOADING;
        mLoadedTasks.clear();
        mDockedTask = null;
        mPlaceholderTask = null;
        mTopHomeTask = null;
        if (withThumbs) {
            BitmapCache.getInstance(mContext).clearThumbs();
        }

        final long currentTime = System.currentTimeMillis();
        final long bootTimeMillis = currentTime - SystemClock.elapsedRealtime();

        mTaskLoader = new AsyncTask<Void, List<TaskDescription>, Void>() {
            @Override
            protected void onProgressUpdate(
                    List<TaskDescription>... values) {
                if (!isCancelled()) {
                    if (mSwitchManager != null) {
                        if (DEBUG) {
                            Log.d(TAG, "recents loaded");
                        }
                        mSwitchManager.update(mLoadedTasks, mDockedTask, mTopHomeTask, mPlaceholderTask);
                        loadMissingTaskInfo();
                    } else {
                        if (DEBUG) {
                            Log.d(TAG, "recents preloaded");
                        }
                    }
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                long start = System.currentTimeMillis();
                if (DEBUG) {
                    Log.d(TAG, "loadTasksInBackground " + mSwitchManager + " start " + start);
                }

                final int origPri = Process.getThreadPriority(Process.myTid());
                Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

                final List<ActivityManager.RecentTaskInfo> recentTasks = mActivityManager
                        .getRecentTasks(maxNumTasks == 0 ? ActivityManager.getMaxRecentTasksStatic() : maxNumTasks,
                                ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                                ActivityManager.RECENT_INCLUDE_PROFILES |
                                ActivityManager.RECENT_WITH_EXCLUDED) ;

                int numTasks = recentTasks.size();
                final ActivityInfo homeInfo = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME).resolveActivityInfo(mPackageManager, 0);
                boolean isFirstValidTask = true;
                int preloadedThumbNum = 0;
                int preloadTaskNum = 0;
                final boolean withIconPack = IconPackHelper.getInstance(mContext).isIconPackLoaded();

                for (int i = 0; i < numTasks; ++i) {
                    if (isCancelled()) {
                        break;
                    }
                    final ActivityManager.RecentTaskInfo recentInfo = recentTasks
                            .get(i);

                    if (DEBUG) {
                        Log.d(TAG, "" + i + " recent item = " + recentInfo.baseIntent + " " + recentInfo.taskDescription.getLabel());
                    }
                    TaskDescription item = createTaskDescription(recentInfo.id,
                            recentInfo.persistentId, recentInfo.stackId,
                            recentInfo.baseIntent, recentInfo.origActivity);

                    Intent intent = new Intent(recentInfo.baseIntent);
                    if (recentInfo.origActivity != null) {
                        intent.setComponent(recentInfo.origActivity);
                    }

                    // Don't load the current home activity.
                    if (isCurrentHomeActivity(intent.getComponent(), homeInfo)) {
                        continue;
                    }

                    final String componentString = intent.getComponent().flattenToShortString();
                    if (componentString.contains(".recents.RecentsActivity") ||
                            componentString.contains("com.android.settings/.FallbackHome")) {
                        continue;
                    }

                    // always remember our own placeholder activity
                    // this is the fallback if no other top task is available
                    if (componentString.contains(getPlaceholderActivity())) {
                        if (DEBUG) {
                            Log.d(TAG, "mPlaceholderTask=" + recentInfo.baseIntent);
                        }
                        mPlaceholderTask = item;
                        continue;
                    }

                    // always add the docked task and put on first place
                    if (recentInfo.stackId == DOCKED_STACK_ID) {
                        mDockedTask = item;
                        item.setDocked();
                        mLoadedTasks.add(0, item);
                    }
                    boolean activeTask = true;
                    if (mConfiguration.mFilterActive) {
                        long lastActiveTime = recentInfo.lastActiveTime;
                        long firstActiveTime = recentInfo.firstActiveTime;
                        if (DEBUG) {
                            Log.d(TAG, intent.getComponent().getPackageName() + ": " + firstActiveTime + ":" + lastActiveTime + ":" + bootTimeMillis);
                        }
                        // only show active since boot
                        if (mConfiguration.mFilterBoot && lastActiveTime < bootTimeMillis) {
                            if (DEBUG) {
                                Log.d(TAG, intent.getComponent().getPackageName() + ": filter app not active since boot");
                            }
                            activeTask = false;
                        }
                        if (activeTask) {
                            // filter older then time
                            if (mConfiguration.mFilterTime != 0 && lastActiveTime < currentTime - mConfiguration.mFilterTime) {
                                if (DEBUG) {
                                    Log.d(TAG, intent.getComponent().getPackageName() + ": filter app not active since " + mConfiguration.mFilterTime);
                                }
                                activeTask = false;
                            }
                        }
                        if (activeTask) {
                            if (mConfiguration.mFilterRunning && recentInfo.id < 0) {
                                if (DEBUG) {
                                    Log.d(TAG, intent.getComponent().getPackageName() + ": filter app not running");
                                }
                                activeTask = false;
                            }
                        }
                    }

                    if (!activeTask) {
                        if (DEBUG) {
                            Log.d(TAG, "skip inactive task =" + recentInfo.baseIntent);
                        }
                        continue;
                    }

                    // this can include activities that have set exclude from recents
                    // but never the placeholder task
                    if (recentInfo.stackId != DOCKED_STACK_ID && mTopHomeTask == null
                            && !componentString.contains(getPlaceholderActivity())) {
                        if (DEBUG) {
                            Log.d(TAG, "mTopHomeTask=" + recentInfo.baseIntent);
                        }
                        mTopHomeTask = item;
                    }

                    // both activities live in the same task so we can also accept that as
                    // placeholder if needed
                    if (componentString.contains(getSettingsActivity()) && mPlaceholderTask == null) {
                        if (DEBUG) {
                            Log.d(TAG, "settings activity as mPlaceholderTask=" + recentInfo.baseIntent);
                        }
                        mPlaceholderTask = item;
                    }

                    // Check the first non-recents task, include this task even if it is marked as excluded
                    boolean isExcluded = (recentInfo.baseIntent.getFlags() & Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                            == Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;

                    if (isExcluded && !isFirstValidTask) {
                        continue;
                    }
                    isFirstValidTask = false;

                    if (recentInfo.stackId != DOCKED_STACK_ID) {
                        mLoadedTasks.add(item);
                    }
                    if (withIcons && preloadTaskNum < TASK_INIT_LOAD) {
                        String label = item.resolveInfo.loadLabel(mPackageManager).toString();
                        loadTaskIcon(item, withIconPack, label);
                        item.setLabel(label);
                        preloadTaskNum++;
                    } else {
                        item.setDefaultIcon(mDefaultAppIcon);
                    }
                    if (withThumbs && mHasThumbPermissions && preloadedThumbNum < THUMB_INIT_LOAD) {
                        Bitmap b = getThumbnail(item.persistentTaskId);
                        if (b != null) {
                            item.setThumbPreloaded(b);
                        }
                        preloadedThumbNum++;
                    }
                }
                if (!isCancelled()) {
                    publishProgress(mLoadedTasks);
                }
                if (DEBUG) {
                    Log.d(TAG, "loadTasksInBackground end " + (System.currentTimeMillis() - start));
                }
                mState = State.IDLE;
                Process.setThreadPriority(origPri);
                return null;
            }
        };
        mTaskLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Returns a task thumbnail from the activity manager
     */
    private Bitmap getThumbnail(int taskId) {
        ActivityManager.TaskThumbnail taskThumbnail = mActivityManager.getTaskThumbnail(taskId);
        if (taskThumbnail == null) return null;

        Bitmap thumbnail = taskThumbnail.mainThumbnail;
        ParcelFileDescriptor descriptor = taskThumbnail.thumbnailFileDescriptor;
        if (thumbnail == null && descriptor != null) {
            thumbnail = BitmapFactory.decodeFileDescriptor(descriptor.getFileDescriptor(),
                    null, sBitmapOptions);
        }
        if (descriptor != null) {
            try {
                descriptor.close();
            } catch (IOException e) {
            }
        }
        return thumbnail;
    }

    void loadTaskIcon(TaskDescription td, boolean withIconPack, String label) {
        Drawable icon = getFullResIcon(td.resolveInfo, withIconPack, label);
        if (icon == null) {
            icon = mDefaultAppIcon;
        }
        td.setIcon(icon);
    }

    private IconPackHelper getIconPackHelper() {
        return IconPackHelper.getInstance(mContext);
    }

    private Drawable getFullResIcon(ResolveInfo info, boolean withIconPack, String label) {
        Resources resources;
        try {
            resources = mPackageManager
                    .getResourcesForApplication(info.activityInfo.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = 0;
            if (withIconPack) {
                iconId = getIconPackHelper().getResourceIdForActivityIcon(info.activityInfo);
                if (iconId != 0) {
                    return IconPackHelper.getInstance(mContext).getIconPackResources().getDrawable(iconId);
                }
            }
            iconId = info.activityInfo.getIconResource();
            if (iconId != 0) {
                try {
                    Drawable d = resources.getDrawable(iconId, null);
                    if (withIconPack) {
                        d = BitmapUtils.compose(resources,
                                d, mContext, getIconPackHelper().getIconBackFor(label),
                                getIconPackHelper().getIconMask(), getIconPackHelper().getIconUpon(),
                                getIconPackHelper().getIconScale(), mConfiguration.mIconSize, mConfiguration.mDensity);
                    }
                    return d;
                } catch (Exception e) {
                }
            }
        }
        return null;
    }

    public void loadThumbnail(final TaskDescription td) {
        if (!mHasThumbPermissions) {
            return;
        }
        if (td.isThumbPreloaded()) {
            Bitmap b = td.getThumbPreloaded();
            if (b != null) {
                if (DEBUG) {
                    Log.d(TAG, "use preloaded thumb " + td + " " + td.persistentTaskId);
                }
                td.setThumb(b);
            }
            return;
        }
        if (td.isThumbLoading()) {
            return;
        }
        mThumbnailLoader = new AsyncTask<Void, TaskDescription, Void>() {
            @Override
            protected void onProgressUpdate(TaskDescription... values) {
            }
            @Override
            protected Void doInBackground(Void... params) {
                final int origPri = Process.getThreadPriority(Process.myTid());
                Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

                if (DEBUG) {
                    Log.d(TAG, "late load thumb " + td + " " + td.persistentTaskId);
                }
                td.setThumbLoading(true);
                Bitmap b = getThumbnail(td.persistentTaskId);
                if (b != null) {
                    td.setThumb(b);
                }

                Process.setThreadPriority(origPri);
                return null;
            }
        };
        mThumbnailLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void loadTaskInfo(final TaskDescription td) {
        synchronized(td) {
            if (!td.isPreloadedTask()) {
                String label = td.resolveInfo.loadLabel(mPackageManager).toString();
                final boolean withIconPack = IconPackHelper.getInstance(mContext).isIconPackLoaded();
                Drawable icon = getFullResIcon(td.resolveInfo, withIconPack, label);
                if (icon == null) {
                    icon = mDefaultAppIcon;
                }
                td.setIcon(icon);
                td.setLabel(label);
            }
        }
    }

    public void loadMissingTaskInfo() {
        mTaskInfoLoader = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onProgressUpdate(Void... values) {
            }
            @Override
            protected Void doInBackground(Void... params) {
                long start = System.currentTimeMillis();
                final int origPri = Process.getThreadPriority(Process.myTid());
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                for (TaskDescription td : mLoadedTasks) {
                    if (isCancelled()) {
                        break;
                    }
                    synchronized(td) {
                        if (!td.isPreloadedTask()) {
                            if (DEBUG) {
                                Log.d(TAG, "late load task info " + td + " " + td.persistentTaskId);
                            }
                            loadTaskInfo(td);
                        }
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "loadMissingTaskInfo end " + (System.currentTimeMillis() - start));
                }
                Process.setThreadPriority(origPri);
                return null;
            }
        };
        mTaskInfoLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private boolean hasSystemPermission(Context context) {
        int result = context
                .checkCallingOrSelfPermission(android.Manifest.permission.READ_FRAME_BUFFER);
        return result == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    public Bitmap getDefaultThumb() {
        return mDefaultThumbnail;
    }

    private String getPlaceholderActivity() {
        return mContext.getPackageName() + "/.PlaceholderActivity";
    }

    private String getSettingsActivity() {
        return mContext.getPackageName() + "/.SettingsActivity";
    }
}
