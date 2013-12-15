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
import java.util.Iterator;
import java.util.List;

import org.omnirom.omniswitch.showcase.ShowcaseView;
import org.omnirom.omniswitch.showcase.ShowcaseView.OnShowcaseEventListener;
import org.omnirom.omniswitch.ui.FavoriteDialog;
import org.omnirom.omniswitch.ui.SeekBarPreference;
import org.omnirom.omniswitch.ui.SettingsGestureView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.WindowManager;

public class SettingsActivity extends PreferenceActivity implements
        OnPreferenceChangeListener /*, OnShowcaseEventListener*/ {
    private static final String TAG = "SettingsActivity";

    public static final String PREF_SERVICE_STATE = "toggle_service";
    public static final String PREF_OPACITY = "opacity";
    public static final String PREF_ANIMATE = "animate";
    public static final String PREF_START_ON_BOOT = "start_on_boot";
    public static final String PREF_ICON_SIZE = "icon_size";
    public static final String PREF_DRAG_HANDLE_LOCATION = "drag_handle_location";
    private static final String PREF_ADJUST_HANDLE = "adjust_handle";
    public static final String PREF_DRAG_HANDLE_COLOR = "drag_handle_color";
    public static final String PREF_SHOW_RAMBAR = "show_rambar";
    public static final String PREF_SHOW_LABELS = "show_labels";
    public static final String PREF_FAVORITE_APPS_CONFIG = "favorite_apps_config";
    public static final String PREF_SHOW_DRAG_HANDLE = "show_drag_handle";

    private final static int SHOWCASE_INDEX_ADJUST = 0;

    private final static String KEY_SHOWCASE_ADJUST = "SHOWCASE_ADJUST";

    private SwitchPreference mToggleService;
    private ListPreference mDragHandleLocation;
    private ListPreference mIconSize;
    private SeekBarPreference mOpacity;
    private Preference mFavoriteAppsConfig;
    private Preference mAdjustHandle;
    private static List<String> sFavoriteList = new ArrayList<String>();
    private static SharedPreferences sPrefs;
    private SettingsGestureView mGestureView;
    //private ShowcaseView mShowcaseView;
    //private int mShowCaseIndex;
    private FavoriteDialog mManageAppDialog;

    @Override
    public void onPause() {
        if (mGestureView != null) {
            mGestureView.hide();
        }
        if (mManageAppDialog != null) {
            mManageAppDialog.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        addPreferencesFromResource(R.xml.recents_settings);

        mToggleService = (SwitchPreference) findPreference(PREF_SERVICE_STATE);
        mToggleService.setChecked(SwitchService.isRunning());
        mToggleService.setOnPreferenceChangeListener(this);

        /*
         * mOrientation = (ListPreference) findPreference(PREF_ORIENTATION);
         * mOrientation.setOnPreferenceChangeListener(this); List<CharSequence>
         * values = Arrays .asList(mOrientation.getEntryValues()); int idx =
         * values.indexOf(mPrefs.getString("orientation",
         * mOrientation.getEntryValues()[0].toString()));
         * mOrientation.setValueIndex(idx);
         * mOrientation.setSummary(mOrientation.getEntries()[idx]);
         */

        mIconSize = (ListPreference) findPreference(PREF_ICON_SIZE);
        mIconSize.setOnPreferenceChangeListener(this);
        List<CharSequence> values = Arrays.asList(mIconSize.getEntryValues());
        int idx = values.indexOf(sPrefs.getString("icon_size",
                mIconSize.getEntryValues()[1].toString()));
        mIconSize.setValueIndex(idx);
        mIconSize.setSummary(mIconSize.getEntries()[idx]);

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

        mFavoriteAppsConfig = (Preference) findPreference(PREF_FAVORITE_APPS_CONFIG);

        String favoriteListString = sPrefs.getString("favorite_apps", "");
        sFavoriteList.clear();
        Utils.parseFavorites(favoriteListString, sFavoriteList);
        removeUninstalledFavorites(this);
        updateEnablement(false, null);
    }

    private void updateEnablement(boolean force, Boolean value) {
        boolean running = false;

        if (!force) {
            running = SwitchService.isRunning();
        } else if (value != null) {
            running = value.booleanValue();
        }
        mAdjustHandle.setEnabled(!running);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mAdjustHandle) {
            //if (!startShowcaseAdjust()) {
                mGestureView = new SettingsGestureView(this);
                mGestureView.show();
            //}
            return true;
        } else if (preference == mFavoriteAppsConfig) {
            showManageAppDialog();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mToggleService) {
            boolean value = (Boolean) newValue;

            Intent svc = new Intent(this, SwitchService.class);
            if (value) {
                Intent killRecent = new Intent(
                        SwitchService.RecentsReceiver.ACTION_KILL_ACTIVITY);
                sendBroadcast(killRecent);

                startService(svc);
            } else {
                Intent killRecent = new Intent(
                        SwitchService.RecentsReceiver.ACTION_KILL_ACTIVITY);
                sendBroadcast(killRecent);
            }
            updateEnablement(true, (Boolean) newValue);
            return true;
            /*
             * } else if (preference == mOrientation) { String value = (String)
             * newValue; List<CharSequence> values = Arrays.asList(mOrientation
             * .getEntryValues()); int idx = values.indexOf(value);
             * mOrientation.setSummary(mOrientation.getEntries()[idx]);
             * mOrientation.setValueIndex(idx); return true;
             */
        } else if (preference == mIconSize) {
            String value = (String) newValue;
            List<CharSequence> values = Arrays.asList(mIconSize
                    .getEntryValues());
            int idx = values.indexOf(value);
            mIconSize.setSummary(mIconSize.getEntries()[idx]);
            mIconSize.setValueIndex(idx);
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

    private void showManageAppDialog() {
        if (mManageAppDialog != null && mManageAppDialog.isShowing()) {
            return;
        }

        List<String> favoriteList = new ArrayList<String>();
        favoriteList.addAll(sFavoriteList);
        mManageAppDialog = new FavoriteDialog(this, favoriteList);
        mManageAppDialog.show();
    }

//    private boolean startShowcaseAdjust() {
//        if (!sPrefs.getBoolean(KEY_SHOWCASE_ADJUST, false)) {
//            sPrefs.edit().putBoolean(KEY_SHOWCASE_ADJUST, true).commit();
//            ShowcaseView.ConfigOptions co = new ShowcaseView.ConfigOptions();
//            co.hideOnClickOutside = true;
//
//            Point size = new Point();
//            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//            wm.getDefaultDisplay().getSize(size);
//
//            String location = sPrefs.getString(PREF_DRAG_HANDLE_LOCATION, "0");
//            boolean left = location.equals("1");
//            float x = left ? 0 : size.x;
//            float y = size.y / 2;
//            mShowcaseView = ShowcaseView.insertShowcaseView(x, y, this,
//                    R.string.sc_adjust_title, R.string.sc_adjust_body, co);
//
//            // Animate gesture
//            mShowCaseIndex = SHOWCASE_INDEX_ADJUST;
//            mShowcaseView.animateGesture(size.x / 2, size.y * 2.0f / 3.0f,
//                    size.x / 2, size.y / 2.0f);
//            mShowcaseView.setOnShowcaseEventListener(this);
//            return true;
//        }
//        return false;
//    }

//    @Override
//    public void onShowcaseViewHide(ShowcaseView showcaseView) {
//        if (mShowCaseIndex == SHOWCASE_INDEX_ADJUST) {
//            mGestureView = new SettingsGestureView(this);
//            mGestureView.show();
//        }
//    }

//    @Override
//    public void onShowcaseViewShow(ShowcaseView showcaseView) {
//        // TODO Auto-generated method stub
//
//    }

    public static void removeUninstalledFavorites(final Context context) {
        Log.d(TAG, "" + sFavoriteList);
        final PackageManager pm = context.getPackageManager();
        boolean changed = false;
        List<String> newFavoriteList = new ArrayList<String>();
        Iterator<String> nextFavorite = sFavoriteList.iterator();
        while (nextFavorite.hasNext()) {
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
        if (changed) {
            sFavoriteList.clear();
            sFavoriteList.addAll(newFavoriteList);
            sPrefs.edit()
                    .putString("favorite_apps", Utils.flattenFavorites(sFavoriteList))
                    .commit();
        }
    }
    
    public void applyChanges(List<String> favoriteList){
        sFavoriteList.clear();
        sFavoriteList.addAll(favoriteList);
        sPrefs.edit()
                .putString("favorite_apps",
                        Utils.flattenFavorites(sFavoriteList))
                .commit();
    }
}
