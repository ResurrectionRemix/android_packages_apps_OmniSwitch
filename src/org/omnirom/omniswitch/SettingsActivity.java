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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {
	private static final String TAG = "SettingsActivity";
	
	public static final String PREF_SERVICE_STATE = "toggle_service";
	public static final String PREF_ORIENTATION = "orientation";
	public static final String PREF_OPACITY = "opacity";
	public static final String PREF_ANIMATE = "animate";
	public static final String PREF_DRAG_HANDLE_SIZE = "drag_handle_size";
	public static final String PREF_DRAG_HANDLE_LOCATION = "drag_handle_location";
	public static final String PREF_DRAG_HANDLE_OPACITY = "drag_handle_opacity";

	private static final String PREF_FAVORITE_APPS = "favorite_apps";

	private static final int DIALOG_APPS = 0;
    
	private SwitchPreference mToggleService;
	private ListPreference mOrientation;
	private ListPreference mDragHandleSize;
	private ListPreference mDragHandleLocation;
	private SeekBarPreference mOpacity;
	private SeekBarPreference mDragHandleOpacity;
	private Preference mFavoriteApps;
    private PackageManager mPackageManager;
	private PackageAdapter mPackageAdapter;
    private String mPackageList;
    private Map<String, Package> mPackages;
    private SharedPreferences mPrefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		addPreferencesFromResource(R.xml.recents_settings);

		mToggleService = (SwitchPreference) findPreference(PREF_SERVICE_STATE);
		mToggleService.setChecked(RecentsService.isRunning());
		mToggleService.setOnPreferenceChangeListener(this);
		
		mOrientation = (ListPreference) findPreference(PREF_ORIENTATION);
		mOrientation.setOnPreferenceChangeListener(this);
		List<CharSequence> values = Arrays.asList(mOrientation.getEntryValues());
		int idx = values.indexOf(mPrefs.getString("orientation", mOrientation.getEntryValues()[0].toString()));
		mOrientation.setValueIndex(idx);
		mOrientation.setSummary(mOrientation.getEntries()[idx]);

		mDragHandleSize = (ListPreference) findPreference(PREF_DRAG_HANDLE_SIZE);
		mDragHandleSize.setOnPreferenceChangeListener(this);
		values = Arrays.asList(mDragHandleSize.getEntryValues());
		idx = values.indexOf(mPrefs.getString("drag_handle_size", mDragHandleSize.getEntryValues()[1].toString()));
		mDragHandleSize.setValueIndex(idx);
		mDragHandleSize.setSummary(mDragHandleSize.getEntries()[idx]);

		mDragHandleLocation = (ListPreference) findPreference(PREF_DRAG_HANDLE_LOCATION);
		mDragHandleLocation.setOnPreferenceChangeListener(this);
		values = Arrays.asList(mDragHandleLocation.getEntryValues());
		idx = values.indexOf(mPrefs.getString("drag_handle_location", mDragHandleLocation.getEntryValues()[0].toString()));
		mDragHandleLocation.setValueIndex(idx);
		mDragHandleLocation.setSummary(mDragHandleLocation.getEntries()[idx]);

		mOpacity = (SeekBarPreference) findPreference(PREF_OPACITY);
		mOpacity.setInitValue(mPrefs.getInt("opacity", 60));
		mOpacity.setOnPreferenceChangeListener(this);

		mDragHandleOpacity= (SeekBarPreference) findPreference(PREF_DRAG_HANDLE_OPACITY);
		mDragHandleOpacity.setInitValue(mPrefs.getInt("drag_handle_opacity", 60));
		mDragHandleOpacity.setOnPreferenceChangeListener(this);
		
		//mFavoriteApps = (Preference) findPreference(PREF_FAVORITE_APPS);
        // Get launch-able applications
        mPackageManager = getPackageManager();
        mPackageAdapter = new PackageAdapter();

        mPackages = new HashMap<String, Package>();

	}

	@Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mFavoriteApps){
        	showDialog(DIALOG_APPS);
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
			return true;
		} else if (preference == mOrientation){
			String value = (String) newValue;
			List<CharSequence> values = Arrays.asList(mOrientation.getEntryValues());
			int idx = values.indexOf(value);
			mOrientation.setSummary(mOrientation.getEntries()[idx]);
			mOrientation.setValueIndex(idx);
			return true;
		} else if (preference == mDragHandleSize){
			String value = (String) newValue;
			List<CharSequence> values = Arrays.asList(mDragHandleSize.getEntryValues());
			int idx = values.indexOf(value);
			mDragHandleSize.setSummary(mDragHandleSize.getEntries()[idx]);
			mDragHandleSize.setValueIndex(idx);
			return true;
		} else if (preference == mDragHandleLocation){
			String value = (String) newValue;
			List<CharSequence> values = Arrays.asList(mDragHandleLocation.getEntryValues());
			int idx = values.indexOf(value);
			mDragHandleLocation.setSummary(mDragHandleLocation.getEntries()[idx]);
			mDragHandleLocation.setValueIndex(idx);
			return true;
		} else if (preference == mOpacity){
			float val = Float.parseFloat((String) newValue);
			mPrefs.edit().putInt(PREF_OPACITY, (int)val).commit();
			return true;
		} else if (preference == mDragHandleOpacity){
			float val = Float.parseFloat((String) newValue);
			mPrefs.edit().putInt(PREF_DRAG_HANDLE_OPACITY, (int)val).commit();
			return true;
		}
		
		return false;
	}

    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Dialog dialog;
        switch (id) {
            case DIALOG_APPS:
                final ListView list = new ListView(this);
                list.setAdapter(mPackageAdapter);

                builder.setView(list);
                dialog = builder.create();

                list.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // Add empty application definition, the user will be able to edit it later
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        Log.d(TAG, "package " + info.packageName);
                        dialog.cancel();
                    }
                });
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
        TreeSet<CharSequence> activityTitles = new TreeSet<CharSequence>();
        String packageName;
        Drawable icon;

        @Override
        public int compareTo(PackageItem another) {
            int result = title.toString().compareToIgnoreCase(another.title.toString());
            return result != 0 ? result : packageName.compareTo(another.packageName);
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

                    final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    List<ResolveInfo> installedAppsInfo =
                            mPackageManager.queryIntentActivities(mainIntent, 0);

                    for (ResolveInfo info : installedAppsInfo) {
                        ApplicationInfo appInfo = info.activityInfo.applicationInfo;

                        final PackageItem item = new PackageItem();
                        item.title = appInfo.loadLabel(mPackageManager);
                        item.activityTitles.add(info.loadLabel(mPackageManager));
                        item.icon = appInfo.loadIcon(mPackageManager);
                        item.packageName = appInfo.packageName;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // NO synchronize here: We know that mInstalledApps.clear()
                                // was called and will never be called again.
                                // At this point the only thread modifying mInstalledApp is main
                                int index = Collections.binarySearch(mInstalledPackages, item);
                                if (index < 0) {
                                    mInstalledPackages.add(-index - 1, item);
                                } else {
                                    mInstalledPackages.get(index).activityTitles.addAll(item.activityTitles);
                                }
                                notifyDataSetChanged();
                            }
                        });
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
                // packageName is guaranteed to be unique in mInstalledPackages
                return mInstalledPackages.get(position).packageName.hashCode();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = layoutInflater.inflate(R.layout.app_item, parent, false);
            
            PackageItem applicationInfo = getItem(position);
			final TextView item = (TextView) rowView
					.findViewById(R.id.app_item);
			item.setText(applicationInfo.title);
			item.setCompoundDrawablesWithIntrinsicBounds(applicationInfo.icon, null , null, null);

            return rowView;
        }
    }
}
