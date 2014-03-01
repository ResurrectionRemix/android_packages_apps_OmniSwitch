/*
 *  Copyright (C) 2014 The OmniROM Project
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import org.omnirom.omniswitch.ui.BitmapCache;

public class PackageManager {

    private Map<String, PackageItem> mInstalledPackages;
    private List<PackageItem> mInstalledPackagesList;
    private Context mContext;
    private boolean mInitDone;
    private static PackageManager sInstance;

    public static class PackageItem implements Comparable<PackageItem> {
        private CharSequence title;
        private String packageName;
        private Drawable icon;
        private String intent;

        public String getIntent() {
            return intent;
        }

        public CharSequence getTitle() {
            return title;
        }

        public Drawable getIcon() {
            return icon;
        }

        @Override
        public int compareTo(PackageItem another) {
            int result = title.toString().compareToIgnoreCase(
                    another.title.toString());
            return result != 0 ? result : packageName
                    .compareTo(another.packageName);
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

    public List<PackageItem> getPackageList() {
        if(!mInitDone){
            updatePackageList(false);
        }
        return mInstalledPackagesList;
    }

    public void updatePackageList(boolean removed) {
        final android.content.pm.PackageManager pm = mContext.getPackageManager();

        // TODO
        BitmapCache.getInstance().clear();

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

            ActivityInfo activity = info.activityInfo;
            ComponentName name = new ComponentName(
                    activity.applicationInfo.packageName, activity.name);
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            intent.setComponent(name);
            item.intent = intent.toUri(0);
            try {
                item.icon = pm.getActivityIcon(intent);
            } catch (NameNotFoundException e) {
                continue;
            }
            item.title = Utils.getActivityLabel(pm, intent);
            if (item.title == null) {
                item.title = appInfo.loadLabel(pm);
            }
            if (item.icon == null) {
                item.icon = getDefaultActivityIcon();
            }
            mInstalledPackages.put(item.intent, item);
            mInstalledPackagesList.add(item);
        }
        if (removed){
            updateFavorites();
        }
        Collections.sort(mInstalledPackagesList);
        mInitDone = true;
    }

    private Drawable getDefaultActivityIcon() {
        return mContext.getResources().getDrawable(R.drawable.ic_default);
    }

    public Drawable getIcon(String intent) {
        return mInstalledPackages.get(intent).getIcon();
    }

    public CharSequence getTitle(String intent) {
        return mInstalledPackages.get(intent).getTitle();
    }

    public boolean contains(String intent) {
        return mInstalledPackages.containsKey(intent);
    }

    private void updateFavorites() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String favoriteListString = prefs.getString(SettingsActivity.PREF_FAVORITE_APPS, "");
        List<String> favoriteList = new ArrayList<String>();
        Utils.parseFavorites(favoriteListString, favoriteList);
        boolean changed = false;

        List<String> newFavoriteList = new ArrayList<String>();
        Iterator<String> nextFavorite = favoriteList.iterator();
        while (nextFavorite.hasNext()) {
            String favorite = nextFavorite.next();
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
}
