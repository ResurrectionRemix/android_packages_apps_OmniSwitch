/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.ShortcutInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import org.omnirom.omniswitch.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Performs operations related to deep shortcuts, such as querying for them, pinning them, etc.
 */
public class DeepShortcutManager {
    private static final String TAG = "DeepShortcutManager";

    // TODO: Replace this with platform constants when the new sdk is available.
    public static final int FLAG_MATCH_DYNAMIC = 1 << 0;
    public static final int FLAG_MATCH_MANIFEST = 1 << 3;
    public static final int FLAG_MATCH_PINNED = 1 << 1;

    private static final int FLAG_GET_ALL =
            FLAG_MATCH_DYNAMIC | FLAG_MATCH_PINNED | FLAG_MATCH_MANIFEST;

    private final LauncherApps mLauncherApps;

    public DeepShortcutManager(Context context) {
        mLauncherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
    }

    /**
     * Queries for the shortcuts with the package name and provided ids.
     * <p>
     * This method is intended to get the full details for shortcuts when they are added or updated,
     * because we only get "key" fields in onShortcutsChanged().
     */
    public List<ShortcutInfo> queryForFullDetails(String packageName,
                                                        List<String> shortcutIds) {
        return query(FLAG_GET_ALL, packageName, null, shortcutIds);
    }

    /**
     * Gets all the manifest and dynamic shortcuts associated with the given package and user,
     * to be displayed in the shortcuts container on long press.
     */
    public List<ShortcutInfo> queryForShortcutsContainer(ComponentName activity,
                                                               List<String> ids) {
        return query(FLAG_MATCH_MANIFEST | FLAG_MATCH_DYNAMIC,
                activity.getPackageName(), activity, ids);
    }

    @TargetApi(25)
    public void startShortcut(ShortcutInfo shortcutInfo, Rect sourceBounds,
                              Bundle startActivityOptions) {
        if (Utils.isNycMR1OrAbove()) {
            try {
                mLauncherApps.startShortcut(shortcutInfo, sourceBounds,
                        startActivityOptions);
            } catch (SecurityException | IllegalStateException e) {
                Log.e(TAG, "Failed to start shortcut", e);
            }
        }
    }

    @TargetApi(25)
    public Drawable getShortcutIconDrawable(ShortcutInfo shortcutInfo, int density) {
        if (Utils.isNycMR1OrAbove()) {
            try {
                Drawable icon = mLauncherApps.getShortcutIconDrawable(
                        shortcutInfo, density);
                return icon;
            } catch (SecurityException | IllegalStateException e) {
                Log.e(TAG, "Failed to get shortcut icon", e);
            }
        }
        return null;
    }

    /**
     * Query the system server for all the shortcuts matching the given parameters.
     * If packageName == null, we query for all shortcuts with the passed flags, regardless of app.
     * <p>
     * TODO: Use the cache to optimize this so we don't make an RPC every time.
     */
    @TargetApi(25)
    private List<ShortcutInfo> query(int flags, String packageName,
                                           ComponentName activity, List<String> shortcutIds) {
        if (Utils.isNycMR1OrAbove()) {
            ShortcutQuery q = new ShortcutQuery();
            q.setQueryFlags(flags);
            if (packageName != null) {
                q.setPackage(packageName);
                q.setActivity(activity);
                q.setShortcutIds(shortcutIds);
            }
            List<ShortcutInfo> shortcutInfos = null;
            try {
                shortcutInfos = mLauncherApps.getShortcuts(q, android.os.Process.myUserHandle());
            } catch (SecurityException | IllegalStateException e) {
                Log.e(TAG, "Failed to query for shortcuts", e);
            }
            if (shortcutInfos == null) {
                return Collections.EMPTY_LIST;
            }
            return shortcutInfos;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    @TargetApi(25)
    public boolean hasHostPermission() {
        if (Utils.isNycMR1OrAbove()) {
            try {
                return mLauncherApps.hasShortcutHostPermission();
            } catch (SecurityException | IllegalStateException e) {
                Log.e(TAG, "Failed to make shortcut manager call", e);
            }
        }
        return false;
    }
}
