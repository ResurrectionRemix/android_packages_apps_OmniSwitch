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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.omnirom.omniswitch.showcase.ShowcaseView;
import org.omnirom.omniswitch.showcase.ShowcaseView.OnShowcaseEventListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SettingsActivity extends PreferenceActivity implements
        OnPreferenceChangeListener, OnShowcaseEventListener {
    private static final String TAG = "SettingsActivity";

    public static final String PREF_SERVICE_STATE = "toggle_service";
    //public static final String PREF_ORIENTATION = "orientation";
    public static final String PREF_OPACITY = "opacity";
    public static final String PREF_ANIMATE = "animate";
    public static final String PREF_START_ON_BOOT = "start_on_boot";
    public static final String PREF_ICON_SIZE = "icon_size";
    public static final String PREF_DRAG_HANDLE_SIZE = "drag_handle_size";
    public static final String PREF_DRAG_HANDLE_LOCATION = "drag_handle_location";
    private static final String PREF_ADJUST_HANDLE = "adjust_handle";
    public static final String PREF_DRAG_HANDLE_COLOR = "drag_handle_color";
    public static final String PREF_SHOW_RAMBAR = "show_rambar";
    public static final String PREF_FAVORITE_APPS = "favorite_apps";
    public static final String PREF_SHOW_LABELS = "show_labels";

    private final static int SHOWCASE_INDEX_ADJUST = 0;

    private final static String KEY_SHOWCASE_ADJUST = "SHOWCASE_ADJUST";

    private static final int DIALOG_APPS = 0;

    private SwitchPreference mToggleService;
    //private ListPreference mOrientation;
    private ListPreference mDragHandleSize;
    private ListPreference mDragHandleLocation;
    private ListPreference mIconSize;
    private CheckBoxPreference mAnimate;
    private CheckBoxPreference mStartOnBoot;
    private SeekBarPreference mOpacity;
    private Preference mFavoriteApps;
    private Preference mAdjustHandle;
    private Preference mDragHandleColor;
    private PackageManager mPackageManager;
    private PackageAdapter mPackageAdapter;
    private static List<String> sFavoriteList = new ArrayList<String>();
    private List<String> mChangedFavoriteList;
    private static SharedPreferences sPrefs;
    private SettingsGestureView mGestureView;
    private ShowcaseView mShowcaseView;
    private int mShowCaseIndex;
    private CheckBoxPreference mShowRambar;
    private CheckBoxPreference mShowLabels;

    @Override
    public void onPause() {
        mGestureView.hide();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mGestureView.hide();
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        addPreferencesFromResource(R.xml.recents_settings);

        mToggleService = (SwitchPreference) findPreference(PREF_SERVICE_STATE);
        mToggleService.setChecked(RecentsService.isRunning());
        mToggleService.setOnPreferenceChangeListener(this);

        mAnimate = (CheckBoxPreference) findPreference(PREF_ANIMATE);
        mStartOnBoot = (CheckBoxPreference) findPreference(PREF_START_ON_BOOT);

        /*mOrientation = (ListPreference) findPreference(PREF_ORIENTATION);
        mOrientation.setOnPreferenceChangeListener(this);
        List<CharSequence> values = Arrays
                .asList(mOrientation.getEntryValues());
        int idx = values.indexOf(mPrefs.getString("orientation",
                mOrientation.getEntryValues()[0].toString()));
        mOrientation.setValueIndex(idx);
        mOrientation.setSummary(mOrientation.getEntries()[idx]);*/

        mIconSize = (ListPreference) findPreference(PREF_ICON_SIZE);
        mIconSize.setOnPreferenceChangeListener(this);
        List<CharSequence> values = Arrays.asList(mIconSize.getEntryValues());
        int idx = values.indexOf(sPrefs.getString("icon_size",
                mIconSize.getEntryValues()[1].toString()));
        mIconSize.setValueIndex(idx);
        mIconSize.setSummary(mIconSize.getEntries()[idx]);

        mDragHandleSize = (ListPreference) findPreference(PREF_DRAG_HANDLE_SIZE);
        mDragHandleSize.setOnPreferenceChangeListener(this);
        values = Arrays.asList(mDragHandleSize.getEntryValues());
        idx = values.indexOf(sPrefs.getString("drag_handle_size",
                mDragHandleSize.getEntryValues()[1].toString()));
        mDragHandleSize.setValueIndex(idx);
        mDragHandleSize.setSummary(mDragHandleSize.getEntries()[idx]);

        mDragHandleLocation = (ListPreference) findPreference(PREF_DRAG_HANDLE_LOCATION);
        mDragHandleLocation.setOnPreferenceChangeListener(this);
        values = Arrays.asList(mDragHandleLocation.getEntryValues());
        idx = values.indexOf(sPrefs.getString("drag_handle_location",
                mDragHandleLocation.getEntryValues()[0].toString()));
        mDragHandleLocation.setValueIndex(idx);
        mDragHandleLocation.setSummary(mDragHandleLocation.getEntries()[idx]);

        mOpacity = (SeekBarPreference) findPreference(PREF_OPACITY);
        mOpacity.setInitValue(sPrefs.getInt("opacity", 60));
        mOpacity.setOnPreferenceChangeListener(this);

        mAdjustHandle = (Preference) findPreference(PREF_ADJUST_HANDLE);

        mDragHandleColor = (Preference) findPreference(PREF_DRAG_HANDLE_COLOR);

        mFavoriteApps = (Preference) findPreference(PREF_FAVORITE_APPS);
        // Get launch-able applications
        mPackageManager = getPackageManager();
        mPackageAdapter = new PackageAdapter();
        
        String favoriteListString = sPrefs.getString("favorite_apps", "");
        sFavoriteList.clear();
        parseFavorites(favoriteListString, sFavoriteList);
        removeUninstalledFavorites(this);

        mGestureView = new SettingsGestureView(this, null);
        
        mShowRambar = (CheckBoxPreference) findPreference(PREF_SHOW_RAMBAR);
        mShowLabels = (CheckBoxPreference) findPreference(PREF_SHOW_LABELS);

        updateEnablement(false, null);
    }

    private void updateEnablement(boolean force, Boolean value) {
        boolean running = false;

        if (!force) {
            running = RecentsService.isRunning();
        } else if (value != null) {
            running = value.booleanValue();
        }
        mAdjustHandle.setEnabled(running);

        mOpacity.setEnabled(running);
        mOpacity.setInitValue(sPrefs.getInt("opacity", 60));

        mDragHandleLocation.setEnabled(running);
        mDragHandleSize.setEnabled(running);
        mIconSize.setEnabled(running);
        //mOrientation.setEnabled(running);
        mAnimate.setEnabled(running);
        mStartOnBoot.setEnabled(running);
        mDragHandleColor.setEnabled(running);
        mShowRambar.setEnabled(running);
        mFavoriteApps.setEnabled(running);
        mShowLabels.setEnabled(running);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mFavoriteApps) {
            showDialog(DIALOG_APPS);
            return true;
        } else if (preference == mAdjustHandle) {
            if (!startShowcaseAdjust()) {
                mGestureView.show();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mToggleService) {
            boolean value = (Boolean) newValue;

            Intent svc = new Intent(this, RecentsService.class);
            if (value) {
                Intent killRecent = new Intent(
                        RecentsService.RecentsReceiver.ACTION_KILL_RECENTS);
                sendBroadcast(killRecent);

                startService(svc);
            } else {
                Intent killRecent = new Intent(
                        RecentsService.RecentsReceiver.ACTION_KILL_RECENTS);
                sendBroadcast(killRecent);
            }
            updateEnablement(true, (Boolean) newValue);
            return true;
        /*} else if (preference == mOrientation) {
            String value = (String) newValue;
            List<CharSequence> values = Arrays.asList(mOrientation
                    .getEntryValues());
            int idx = values.indexOf(value);
            mOrientation.setSummary(mOrientation.getEntries()[idx]);
            mOrientation.setValueIndex(idx);
            return true;*/
        } else if (preference == mIconSize) {
            String value = (String) newValue;
            List<CharSequence> values = Arrays.asList(mIconSize
                    .getEntryValues());
            int idx = values.indexOf(value);
            mIconSize.setSummary(mIconSize.getEntries()[idx]);
            mIconSize.setValueIndex(idx);
            return true;
        } else if (preference == mDragHandleSize) {
            String value = (String) newValue;
            List<CharSequence> values = Arrays.asList(mDragHandleSize
                    .getEntryValues());
            int idx = values.indexOf(value);
            mDragHandleSize.setSummary(mDragHandleSize.getEntries()[idx]);
            mDragHandleSize.setValueIndex(idx);
            return true;
        } else if (preference == mDragHandleLocation) {
            String value = (String) newValue;
            List<CharSequence> values = Arrays.asList(mDragHandleLocation
                    .getEntryValues());
            int idx = values.indexOf(value);
            mDragHandleLocation
                    .setSummary(mDragHandleLocation.getEntries()[idx]);
            mDragHandleLocation.setValueIndex(idx);
            return true;
        } else if (preference == mOpacity) {
            float val = Float.parseFloat((String) newValue);
            sPrefs.edit().putInt(PREF_OPACITY, (int) val).commit();
            return true;
        }

        return false;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        Log.d(TAG, "xxx" + sFavoriteList);
        mChangedFavoriteList = new ArrayList<String>();
        mChangedFavoriteList.addAll(sFavoriteList);
        super.onPrepareDialog(id, dialog, args);
    }
    
    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Dialog dialog;
        switch (id) {
        case DIALOG_APPS:
            final ListView list = new ListView(this);
            list.setAdapter(mPackageAdapter);
            list.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                    ViewHolder viewHolder = (ViewHolder) view.getTag();
                    viewHolder.check.setChecked(!viewHolder.check.isChecked());
                    if(viewHolder.check.isChecked()){
                        if(!mChangedFavoriteList.contains(info.intent)){
                            mChangedFavoriteList.add(info.intent);
                        }
                    } else {
                        mChangedFavoriteList.remove(info.intent);
                    }
                }
            });
            builder.setView(list);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    sFavoriteList = mChangedFavoriteList;
                    sPrefs.edit().putString("favorite_apps", flattenFavorites(sFavoriteList)).commit();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            dialog = builder.create();
            break;
        default:
            dialog = null;
        }
        return dialog;
    }

    /**
     * AppItem class
     */
    private static class PackageItem implements Comparable<PackageItem> {
        CharSequence title;
        String packageName;
        Drawable icon;
        String intent;

        @Override
        public int compareTo(PackageItem another) {
            int result = title.toString().compareToIgnoreCase(
                    another.title.toString());
            return result != 0 ? result : packageName
                    .compareTo(another.packageName);
        }
    }

    /**
     * AppAdapter class
     */
    private class PackageAdapter extends BaseAdapter {
        private List<PackageItem> mInstalledPackages = new LinkedList<PackageItem>();

        private void reloadList() {
            final Handler handler = new Handler();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mInstalledPackages) {
                        mInstalledPackages.clear();
                    }

                    final Intent mainIntent = new Intent(Intent.ACTION_MAIN,
                            null);
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    List<ResolveInfo> installedAppsInfo = mPackageManager
                            .queryIntentActivities(mainIntent, 0);

                    for (ResolveInfo info : installedAppsInfo) {
                        ApplicationInfo appInfo = info.activityInfo.applicationInfo;

                        final PackageItem item = new PackageItem();
                        item.packageName = appInfo.packageName;
                        
                        ActivityInfo activity = info.activityInfo;
                        ComponentName name =
                                new ComponentName(activity.applicationInfo.packageName, activity.name);
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        intent.setComponent(name);
                        item.intent = intent.toUri(0);
                        try {
                            item.icon = mPackageManager.getActivityIcon(intent);
                        } catch (NameNotFoundException e) {
                            item.icon = appInfo.loadIcon(mPackageManager);
                        }
                        item.title = getActivityLabel(mPackageManager, intent);
                        if(item.title==null){
                            item.title = appInfo.loadLabel(mPackageManager);
                        }
                        mInstalledPackages.add(item);
                    }
                }
            }).start();
        }

        public PackageAdapter() {
            reloadList();
        }

        @Override
        public int getCount() {
            synchronized (mInstalledPackages) {
                return mInstalledPackages.size();
            }
        }

        @Override
        public PackageItem getItem(int position) {
            synchronized (mInstalledPackages) {
                return mInstalledPackages.get(position);
            }
        }

        @Override
        public long getItemId(int position) {
            synchronized (mInstalledPackages) {
                // intent is guaranteed to be unique in mInstalledPackages
                return mInstalledPackages.get(position).intent.hashCode();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView != null) {
                holder = (ViewHolder) convertView.getTag();
            } else {
                final LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.app_item, parent,
                        false);
                holder = new ViewHolder();
                convertView.setTag(holder);

                holder.item = (TextView) convertView
                        .findViewById(R.id.app_item);
                holder.check = (CheckBox) convertView
                        .findViewById(R.id.app_check);
                holder.image = (ImageView) convertView
                        .findViewById(R.id.app_icon);
            }
            PackageItem applicationInfo = getItem(position);
            holder.item.setText(applicationInfo.title);
            holder.image.setImageDrawable(applicationInfo.icon);
            holder.check.setChecked(mChangedFavoriteList.contains(applicationInfo.intent));

            return convertView;
        }
    }

    static class ViewHolder {
        TextView item;
        CheckBox check;
        ImageView image;
    }

    private boolean startShowcaseAdjust() {
        if (!sPrefs.getBoolean(KEY_SHOWCASE_ADJUST, false)) {
            sPrefs.edit().putBoolean(KEY_SHOWCASE_ADJUST, true).commit();
            ShowcaseView.ConfigOptions co = new ShowcaseView.ConfigOptions();
            co.hideOnClickOutside = true;

            Point size = new Point();
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getSize(size);

            String location = sPrefs.getString(PREF_DRAG_HANDLE_LOCATION, "0");
            boolean left = location.equals("1");
            float x = left ? 0 : size.x;
            float y = size.y / 2;
            mShowcaseView = ShowcaseView.insertShowcaseView(x, y, this,
                    R.string.sc_adjust_title, R.string.sc_adjust_body, co);

            // Animate gesture
            mShowCaseIndex = SHOWCASE_INDEX_ADJUST;
            mShowcaseView.animateGesture(size.x / 2, size.y * 2.0f / 3.0f,
                    size.x / 2, size.y / 2.0f);
            mShowcaseView.setOnShowcaseEventListener(this);
            return true;
        }
        return false;
    }

    @Override
    public void onShowcaseViewHide(ShowcaseView showcaseView) {
        if (mShowCaseIndex == SHOWCASE_INDEX_ADJUST) {
            mGestureView.show();
        }
    }

    @Override
    public void onShowcaseViewShow(ShowcaseView showcaseView) {
        // TODO Auto-generated method stub

    }
    
    public static void parseFavorites(String favoriteListString, List<String> favoriteList){
        String[] split = favoriteListString.split("##");
        for(int i = 0; i < split.length; i++){
            favoriteList.add(split[i]);
        }
    }
    
    public static String flattenFavorites(List<String> favoriteList) {
        Iterator<String> nextFavorite=favoriteList.iterator();
        StringBuffer buffer = new StringBuffer();
        while(nextFavorite.hasNext()){
            String favorite = nextFavorite.next();
            buffer.append(favorite + "##");
        }
        if(buffer.length()!=0){
            return buffer.substring(0, buffer.length()-2).toString();
        }
        return buffer.toString();
    }
    
    public static void removeUninstalledFavorites(final Context context) {
        Log.d(TAG, "" + sFavoriteList);
        final PackageManager pm = context.getPackageManager();
        boolean changed = false;
        List<String> newFavoriteList = new ArrayList<String>();
        Iterator<String> nextFavorite=sFavoriteList.iterator();
        while(nextFavorite.hasNext()){
            String favorite = nextFavorite.next();
            Intent intent = null;
            try {
                intent = Intent.parseUri(favorite, 0);
                pm.getActivityIcon(intent);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "NameNotFoundException: [" + favorite + "]");
                changed = true;
                continue;
            } catch (URISyntaxException e) {
                Log.e(TAG, "URISyntaxException: [" + favorite + "]");
                changed = true;
                continue;
            }
            newFavoriteList.add(favorite);
        }
        if(changed){
            sFavoriteList.clear();
            sFavoriteList.addAll(newFavoriteList);
            sPrefs.edit().putString("favorite_apps", flattenFavorites(sFavoriteList)).commit();
        }
    }
    
    public static String getActivityLabel(PackageManager pm, Intent intent){
        ActivityInfo ai = intent.resolveActivityInfo(pm, PackageManager.GET_ACTIVITIES);
        String label = null;

        if (ai != null) {
            label = ai.loadLabel(pm).toString();
            if (label == null) {
                label = ai.name;
            }
        }
        return label;
    }
}
