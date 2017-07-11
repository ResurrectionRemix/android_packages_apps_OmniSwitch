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
package org.omnirom.omniswitch;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.omnirom.omniswitch.ui.BitmapCache;
import org.omnirom.omniswitch.ui.BitmapUtils;
import org.omnirom.omniswitch.ui.IconPackHelper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class PackageManager {
    private static final boolean DEBUG = false;
    private static final String TAG = "PackageManager";

    private Map<String, PackageItem> mInstalledPackages;
    private List<PackageItem> mInstalledPackagesList;
    private Context mContext;
    private boolean mInitDone;
    private static PackageManager sInstance;

    public static final String PACKAGES_UPDATED_TAG = "PACKAGES_UPDATED";

    public static class PackageItem implements Comparable<PackageItem> {
        private CharSequence title;
        private String packageName;
        private Intent intent;
        private ActivityInfo activity;

        public Intent getIntentRaw() {
            return intent;
        }

        public String getIntent() {
            return intent.toUri(0);
        }

        public CharSequence getTitle() {
            return title;
        }

        public ActivityInfo getActivityInfo() {
            return activity;
        }

        @Override
        public int compareTo(PackageItem another) {
            int result = title.toString().compareToIgnoreCase(
                    another.title.toString());
            return result != 0 ? result : packageName
                    .compareTo(another.packageName);
        }

        @Override
        public String toString() {
            return getTitle().toString();
        }
    }

    public static PackageManager getInstance(Context context) {
        if (sInstance == null){
            sInstance = new PackageManager();
        }
        sInstance.setContext(context);
        return sInstance;
    }

    private PackageManager() {
        mInstalledPackages = new HashMap<String, PackageItem>();
        mInstalledPackagesList = new ArrayList<PackageItem>();
    }

    private void setContext(Context context) {
        mContext = context;
    }

    public synchronized List<PackageItem> getPackageList() {
        if(!mInitDone){
            updatePackageList();
        }
        return mInstalledPackagesList;
    }

    public synchronized Map<String, PackageItem> getPackageMap() {
        if(!mInitDone){
            updatePackageList();
        }
        return mInstalledPackages;
    }

    public synchronized void clearPackageList() {
        mInstalledPackages.clear();
        mInstalledPackagesList.clear();
        mInitDone = false;
    }

    public Drawable getPackageIcon(PackageItem item) {
        final android.content.pm.PackageManager pm = mContext.getPackageManager();

        Drawable icon = null;
        if (IconPackHelper.getInstance(mContext).isIconPackLoaded()){
            int iconId = IconPackHelper.getInstance(mContext).getResourceIdForActivityIcon(item.activity);
            if (iconId != 0) {
                icon = IconPackHelper.getInstance(mContext).getIconPackResources().getDrawable(iconId);
            }
        }
        if (icon == null || !IconPackHelper.getInstance(mContext).isIconPackLoaded()){
            try {
                icon = pm.getActivityIcon(item.intent);
            } catch (NameNotFoundException e) {
            }
        }
        if (icon == null) {
            icon = BitmapUtils.getDefaultActivityIcon(mContext);
        }
        return icon;
    }

    public synchronized void reloadPackageList() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean old = prefs.getBoolean(PackageManager.PACKAGES_UPDATED_TAG, false);
        updatePackageList();
        prefs.edit().putBoolean(PackageManager.PACKAGES_UPDATED_TAG, !old).commit();
    }

    public synchronized void updatePackageList() {
        if (DEBUG) Log.d(TAG, "updatePackageList");
        final android.content.pm.PackageManager pm = mContext.getPackageManager();
        Set<String> packageNameList = new HashSet<String>();

        mInstalledPackages.clear();
        mInstalledPackagesList.clear();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> installedAppsInfo = pm.queryIntentActivities(
                mainIntent, 0);

        for (ResolveInfo info : installedAppsInfo) {
            ApplicationInfo appInfo = info.activityInfo.applicationInfo;

            final PackageItem item = new PackageItem();
            item.packageName = appInfo.packageName;
            packageNameList.add(item.packageName);

            ActivityInfo activity = info.activityInfo;
            item.activity = activity;
            ComponentName name = new ComponentName(
                    activity.applicationInfo.packageName, activity.name);
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            intent.setComponent(name);
            item.intent = intent;

            item.title = Utils.getActivityLabel(pm, intent);
            if (item.title == null) {
                item.title = appInfo.loadLabel(pm);
            }
            mInstalledPackages.put(item.getIntent(), item);
            mInstalledPackagesList.add(item);
        }

        updateFavorites();
        updateLockedApps(packageNameList);

        Collections.sort(mInstalledPackagesList);
        mInitDone = true;
    }

    public synchronized void updatePackageIcons() {
        BitmapCache.getInstance(mContext).clear();
    }

    public synchronized CharSequence getTitle(String intent) {
        return getPackageMap().get(intent).getTitle();
    }

    public synchronized PackageItem getPackageItem(String intent) {
        return getPackageMap().get(intent);
    }

    public synchronized PackageItem getPackageItemByComponent(Intent intent) {
        String pkgName = intent.getComponent().getPackageName();

        Iterator<PackageItem> nextPackage = mInstalledPackagesList.iterator();
        while(nextPackage.hasNext()){
            PackageItem item = nextPackage.next();
            ComponentName name = item.getIntentRaw().getComponent();
            String pPkgName = name.getPackageName();
            if (pkgName.equals(pPkgName)){
                return item;
            }
        }
        return null;
    }

    public synchronized List<String> getPackageListForPackageName(String pkgName) {
        List<String> pkgList = new ArrayList<String>();
        Iterator<PackageItem> nextPackage = mInstalledPackagesList.iterator();
        while(nextPackage.hasNext()){
            PackageItem item = nextPackage.next();
            ComponentName name = item.getIntentRaw().getComponent();
            String pPkgName = name.getPackageName();
            if (pkgName.equals(pPkgName)){
                pkgList.add(item.getIntent());
            }
        }
        return pkgList;
    }

    public synchronized boolean contains(String intent) {
        return getPackageMap().containsKey(intent);
    }

    public void removePackageIconCache(String packageName) {
        BitmapCache.getInstance(mContext).removeBitmapToMemoryCache(packageName);
    }

    private synchronized void updateFavorites() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String favoriteListString = prefs.getString(SettingsActivity.PREF_FAVORITE_APPS, "");
        List<String> favoriteList = new ArrayList<String>();
        Utils.parseFavorites(favoriteListString, favoriteList);
        boolean changed = false;

        List<String> newFavoriteList = new ArrayList<String>();
        Iterator<String> nextFavorite = favoriteList.iterator();
        while (nextFavorite.hasNext()) {
            String favorite = nextFavorite.next();
            // DONT USE getPackageMap() here!
            if (!mInstalledPackages.containsKey(favorite)){
                changed = true;
                continue;
            }
            newFavoriteList.add(favorite);
        }
        if (changed) {
            prefs.edit()
                    .putString(SettingsActivity.PREF_FAVORITE_APPS, Utils.flattenFavorites(newFavoriteList))
                    .commit();
        }
    }

    private synchronized void updateLockedApps(Set<String> packageNameList) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String appListString = prefs.getString(SettingsActivity.PREF_LOCKED_APPS_LIST, "");
        List<String> appsList = new ArrayList<String>();
        Utils.parseLockedApps(appListString, appsList);
        boolean changed = false;

        List<String> newAppsList = new ArrayList<String>();
        Iterator<String> nextApp = appsList.iterator();
        while (nextApp.hasNext()) {
            String packageName = nextApp.next();
            if (!packageNameList.contains(packageName)){
                changed = true;
                continue;
            }
            newAppsList.add(packageName);
        }
        if (changed) {
            prefs.edit()
                    .putString(SettingsActivity.PREF_LOCKED_APPS_LIST, TextUtils.join(",", newAppsList))
                    .commit();
        }
    }
}
