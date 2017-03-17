/*
 *  Copyright (C) 2014-2016 The OmniROM Project
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
package org.omnirom.omniswitch.ui;

import org.omnirom.omniswitch.PackageManager;
import org.omnirom.omniswitch.SwitchConfiguration;
import org.omnirom.omniswitch.TaskDescription;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.util.Log;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

public class BitmapCache {
    private static final String TAG = "OmniSwitch:BitmapCache";
    private static final boolean DEBUG = false;
    private static BitmapCache sInstance;
    private Context mContext;
    private LruCache<String, Drawable> mMemoryCache;
    private HashMap<String, Drawable> mThumbnailMap;

    public static BitmapCache getInstance(Context context) {
        if (sInstance == null){
            sInstance = new BitmapCache();
        }
        sInstance.setContext(context);
        return sInstance;
    }

    private BitmapCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/4rd of the available memory for this memory cache.
        int cacheSize = maxMemory / 4;
        if (DEBUG) Log.d(TAG, "maxMemory = " + maxMemory +" cacheSize = " + cacheSize);

        mMemoryCache = new LruCache<String, Drawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, Drawable bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                if (bitmap instanceof BitmapDrawable){
                    return ((BitmapDrawable)bitmap).getBitmap().getByteCount() / 1024;
                } else {
                    return 1;
                }
            }
            @Override
            protected void entryRemoved(boolean evicted, String key, Drawable oldValue, Drawable newValue){
            }
        };
        mThumbnailMap = new HashMap<String, Drawable>(25);
    }

    private void setContext(Context context) {
        mContext = context;
    }

    public void clear() {
        if (DEBUG) Log.d(TAG, "clear");
        mMemoryCache.evictAll();
    }

    public void clearThumbs() {
        mThumbnailMap.clear();
    }

    private String bitmapHash(Intent intent, int iconSize) {
        // unique identifier
        String key = intent.getComponent().flattenToString();
        return key + "_" + iconSize;
    }

    private IconPackHelper getIconPackHelper() {
        return IconPackHelper.getInstance(mContext);
    }

    public Drawable getPackageIconCached(Resources resources, PackageManager.PackageItem packageItem, SwitchConfiguration configuration) {
        String key = bitmapHash(packageItem.getIntentRaw(), configuration.mIconSize);
        Drawable d = getBitmapFromMemCache(key);
        if (d == null){
            if (DEBUG) Log.d(TAG, "addToCache = " + key);
            d = getPackageIconUncached(resources, packageItem, configuration, configuration.mIconSize);
            addBitmapToMemoryCache(key, d);
        }
        return d;
    }

    public Drawable getPackageIconUncached(Resources resources, PackageManager.PackageItem packageItem, SwitchConfiguration configuration, int iconSize) {
        Drawable icon = PackageManager.getInstance(mContext).getPackageIcon(packageItem);
        if (getIconPackHelper().isIconPackLoaded() && (getIconPackHelper()
                .getResourceIdForActivityIcon(packageItem.getActivityInfo()) == 0)) {
            icon = BitmapUtils.compose(resources,
                    icon, mContext, getIconPackHelper().getIconBackFor(packageItem.getTitle()),
                    getIconPackHelper().getIconMask(), getIconPackHelper().getIconUpon(),
                    getIconPackHelper().getIconScale(), iconSize, configuration.mDensity);
        }
        return icon;
    }

    public void addBitmapToMemoryCache(String key, Drawable bitmap) {
        mMemoryCache.put(key, bitmap);
    }

    public Iterator<String> keyIterator() {
        return mMemoryCache.snapshot().keySet().iterator();
    }

    // remove all entries with this package name
    public void removeBitmapToMemoryCache(String packageName) {
        Iterator<String> nextKey = keyIterator();
        while (nextKey.hasNext()) {
            String key = nextKey.next();
            if (key.startsWith(packageName)) {
                Drawable removed = mMemoryCache.remove(key);
                if (removed != null) {
                    if (DEBUG) Log.d(TAG, "removedFromCache = " + key);
                }
            }
        }
    }

    public Drawable getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    public Drawable getSharedThumbnail(TaskDescription ad) {
        String key = String.valueOf(ad.getPersistentTaskId());
        return mThumbnailMap.get(key);
    }

    public void putSharedThumbnail(Resources resources, TaskDescription ad, Drawable thumb) {
        String key = String.valueOf(ad.getPersistentTaskId());
        mThumbnailMap.put(key, thumb);
    }
}
