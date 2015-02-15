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
import java.util.List;
import java.io.IOException;

import org.omnirom.omniswitch.ui.BitmapUtils;
import org.omnirom.omniswitch.ui.ColorDrawableWithDimensions;
import org.omnirom.omniswitch.ui.IconPackHelper;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Process;
import android.os.ParcelFileDescriptor;
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
    private ActivityManager mActivityManager;
    private Drawable mDefaultThumbnailBackground;
    private PreloadTaskRunnable mPreloadTasksRunnable;
    private boolean mHasThumbPermissions;

    final static BitmapFactory.Options sBitmapOptions;

    static {
        sBitmapOptions = new BitmapFactory.Options();
        sBitmapOptions.inMutable = true;
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
        mLoadedTasks = new ArrayList<TaskDescription>();
        mActivityManager = (ActivityManager)
                mContext.getSystemService(Context.ACTIVITY_SERVICE);

        // Render the default thumbnail background
        int thumbnailWidth = (int) context.getResources().getDimensionPixelSize(
                R.dimen.thumbnail_width);
        int thumbnailHeight = (int) context.getResources()
                .getDimensionPixelSize(R.dimen.thumbnail_height);
        mDefaultThumbnailBackground = new ColorDrawableWithDimensions(
                Color.BLACK, thumbnailWidth, thumbnailHeight);
        mHasThumbPermissions = hasSystemPermission(context);
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
        // clear source bounds to find matching package intent
        baseIntent.setSourceBounds(null);
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

    private class PreloadTaskRunnable implements Runnable {
        @Override
        public void run() {
            if (DEBUG){
                Log.d(TAG, "preload start " + System.currentTimeMillis());
            }
            loadTasksInBackground();
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
        if (DEBUG){
            Log.d(TAG, "cancelLoadingTasks state = " + mState);
        }
        if (mTaskLoader != null) {
            mTaskLoader.cancel(false);
            mTaskLoader = null;
        }
        if (mPreloadTasksRunnable != null) {
            mHandler.removeCallbacks(mPreloadTasksRunnable);
            mPreloadTasksRunnable = null;
        }
        mLoadedTasks.clear();
        mPreloaded = false;
        mState = State.IDLE;
    }

    public void loadTasksInBackground() {
        if (DEBUG){
            Log.d(TAG, "loadTasksInBackground " + mSwitchManager + " start " + System.currentTimeMillis());
        }
        if (mPreloaded && mState != State.IDLE) {
            if (DEBUG){
                Log.d(TAG, "recents preloaded: waiting for done");
            }
            return;
        }
        if(mPreloaded && mSwitchManager != null){
            if (DEBUG){
                Log.d(TAG, "recents preloaded " + mLoadedTasks);
            }
            mSwitchManager.update(mLoadedTasks);
            return;
        }
        if (DEBUG){
            Log.d(TAG, "recents load");
        }
        mPreloaded = true;
        mState = State.LOADING;
        mLoadedTasks.clear();

        mTaskLoader = new AsyncTask<Void, List<TaskDescription>, Void>() {
            @Override
            protected void onProgressUpdate(
                    List<TaskDescription>... values) {
                if (!isCancelled()) {
                    if (mSwitchManager != null){
                        if (DEBUG){
                            Log.d(TAG, "recents loaded");
                        }
                        mSwitchManager.update(mLoadedTasks);
                    } else {
                        if (DEBUG){
                            Log.d(TAG, "recents preloaded");
                        }
                    }
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                final int origPri = Process.getThreadPriority(Process.myTid());
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                final PackageManager pm = mContext.getPackageManager();

                final List<ActivityManager.RecentTaskInfo> recentTasks = mActivityManager
                        .getRecentTasks(MAX_TASKS,
                                ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                                ActivityManager.RECENT_INCLUDE_PROFILES) ;

                int numTasks = recentTasks.size();
                ActivityInfo homeInfo = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME).resolveActivityInfo(
                                pm, 0);

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

                    // dont load AOSP recents - com.android.systemui/.recents.RecentsActivity
                    if (intent.getComponent().flattenToShortString().contains(".recents.RecentsActivity")) {
                        continue;
                    }

                    TaskDescription item = createTaskDescription(recentInfo.id,
                            recentInfo.persistentId, recentInfo.baseIntent,
                            recentInfo.origActivity, recentInfo.description);

                    if (item != null) {
                        item.setInitThumb(true);
                        item.setThumb(mDefaultThumbnailBackground);
                        mLoadedTasks.add(item);
                        loadTaskIcon(item);

                        if (mHasThumbPermissions && i < 3){
                            if (DEBUG){
                                Log.d(TAG, "load thumb " + item);
                            }

                            item.setInitThumb(false);
                            Bitmap b = getThumbnail(item.persistentTaskId);
                            if (b != null) {
                                item.setThumb(new BitmapDrawable(mContext.getResources(), b));
                            }
                        }
                    }
                }
                if (!isCancelled()) {
                    publishProgress(mLoadedTasks);
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

    void loadTaskIcon(TaskDescription td) {
        final PackageManager pm = mContext.getPackageManager();
        Drawable icon = getFullResIcon(td.resolveInfo, pm);

        synchronized (td) {
            if (icon == null) {
                icon = BitmapUtils.getDefaultActivityIcon(mContext);
            }
            td.setIcon(icon);
            td.setLoaded(true);
        }
    }

    private Drawable getFullResIcon(ResolveInfo info,
            PackageManager packageManager) {
        Resources resources;
        final Drawable defaultIcon = BitmapUtils.getDefaultActivityIcon(mContext);
        try {
            resources = packageManager
                    .getResourcesForApplication(info.activityInfo.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = 0;
            if (IconPackHelper.getInstance(mContext).isIconPackLoaded()){
                iconId = IconPackHelper.getInstance(mContext).getResourceIdForActivityIcon(info.activityInfo);
                if (iconId != 0) {
                    Drawable icon = IconPackHelper.getInstance(mContext).getIconPackResources().getDrawable(iconId);
                    if (!(icon instanceof BitmapDrawable)) {
                        icon = defaultIcon;
                    }
                    return icon;
                }
            }
            iconId = info.activityInfo.getIconResource();
            if (iconId != 0) {
                try {
                    Drawable icon = resources.getDrawable(iconId);
                    if (!(icon instanceof BitmapDrawable)) {
                        icon = defaultIcon;
                    }
                    return icon;
                } catch(Resources.NotFoundException e){
                    // ignore and use default below
                }
            }
        }
        return defaultIcon;
    }

    public void loadThumbnail(final TaskDescription td) {
        if (!mHasThumbPermissions) {
            td.setInitThumb(false);
            return;
        }
        Drawable d = td.getThumb();
        if (d == null || td.isInitThumb()){
            AsyncTask<Void, TaskDescription, Void> thumbnailLoader = new AsyncTask<Void, TaskDescription, Void>() {
                @Override
                protected void onProgressUpdate(TaskDescription... values) {
                }
                @Override
                protected Void doInBackground(Void... params) {
                    final int origPri = Process.getThreadPriority(Process.myTid());
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                    if (DEBUG){
                        Log.d(TAG, "load thumb " + td);
                    }

                    td.setInitThumb(false);
                    if (mHasThumbPermissions){
                        Bitmap b = getThumbnail(td.persistentTaskId);
                        if (b != null) {
                            td.setThumb(new BitmapDrawable(mContext.getResources(), b));
                        }
                    }

                    Process.setThreadPriority(origPri);
                    return null;
                }
            };
            thumbnailLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private boolean hasSystemPermission(Context context) {
        int result = context
                .checkCallingOrSelfPermission(android.Manifest.permission.READ_FRAME_BUFFER);
        return result == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
}
