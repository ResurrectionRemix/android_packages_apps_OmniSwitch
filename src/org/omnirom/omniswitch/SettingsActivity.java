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

import org.omnirom.omniswitch.ui.BitmapUtils;
import org.omnirom.omniswitch.ui.CheckboxListDialog;
import org.omnirom.omniswitch.ui.DragHandleColorPreference;
import org.omnirom.omniswitch.ui.FavoriteDialog;
import org.omnirom.omniswitch.ui.IconPackHelper;
import org.omnirom.omniswitch.ui.SeekBarPreference;
import org.omnirom.omniswitch.ui.SettingsGestureView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Switch;

public class SettingsActivity extends PreferenceActivity implements
        OnPreferenceChangeListener  {
    private static final String TAG = "SettingsActivity";

    public static final String PREF_OPACITY = "opacity";
    public static final String PREF_ANIMATE = "animate";
    public static final String PREF_START_ON_BOOT = "start_on_boot";
    public static final String PREF_ICON_SIZE = "icon_size";
    public static final String PREF_DRAG_HANDLE_LOCATION = "drag_handle_location_new";
    private static final String PREF_ADJUST_HANDLE = "adjust_handle";
    public static final String PREF_DRAG_HANDLE_COLOR = "drag_handle_color";
    public static final String PREF_DRAG_HANDLE_OPACITY = "drag_handle_opacity";
    public static final String PREF_SHOW_RAMBAR = "show_rambar";
    public static final String PREF_SHOW_LABELS = "show_labels";
    public static final String PREF_FAVORITE_APPS_CONFIG = "favorite_apps_config";
    public static final String PREF_FAVORITE_APPS = "favorite_apps";
    public static final String PREF_HANDLE_POS_START_RELATIVE = "handle_pos_start_relative";
    public static final String PREF_HANDLE_HEIGHT = "handle_height";
    public static final String PREF_BUTTON_CONFIG = "button_config";
    public static final String PREF_BUTTONS = "buttons";
    public static final String PREF_BUTTON_DEFAULT = "1,1,1,1,1,1";
    public static final String PREF_AUTO_HIDE_HANDLE = "auto_hide_handle";
    public static final String PREF_DRAG_HANDLE_ENABLE = "drag_handle_enable";
    public static final String PREF_ENABLE = "enable";
    public static final String PREF_DIM_BEHIND = "dim_behind";
    public static final String PREF_GRAVITY = "gravity";
    public static final String PREF_ICONPACK = "iconpack";

    public static int BUTTON_KILL_ALL = 0;
    public static int BUTTON_KILL_OTHER = 1;
    public static int BUTTON_TOGGLE_APP = 2;
    public static int BUTTON_HOME = 3;
    public static int BUTTON_SETTINGS = 4;
    public static int BUTTON_ALLAPPS = 5;

    public static int NUM_BUTTON = 6;

    private ListPreference mIconSize;
    private SeekBarPreference mOpacity;
    private Preference mFavoriteAppsConfig;
    private Preference mAdjustHandle;
    private SharedPreferences mPrefs;
    private SettingsGestureView mGestureView;
    private FavoriteDialog mManageAppDialog;
    private Preference mButtonConfig;
    private String[] mButtonEntries;
    private Drawable[] mButtonImages;
    private String mButtons;
    private SeekBarPreference mDragHandleOpacity;
    private SwitchPreference mDragHandleEnable;
    private CheckBoxPreference mDragHandleAutoHide;
    private DragHandleColorPreference mDragHandleColor;
    private ListPreference mGravity;
    private Preference mIconpack;
    private Switch mToggleServiceSwitch;
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener;

    @Override
    public void onPause() {
        if (mGestureView != null) {
            mGestureView.hide();
        }
        if (mManageAppDialog != null) {
            mManageAppDialog.dismiss();
        }
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
        super.onPause();
    }

    @Override
    public void onResume() {
        mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        addPreferencesFromResource(R.xml.recents_settings);

        mIconSize = (ListPreference) findPreference(PREF_ICON_SIZE);
        mIconSize.setOnPreferenceChangeListener(this);
        int idx = mIconSize.findIndexOfValue(mPrefs.getString(PREF_ICON_SIZE,
                mIconSize.getEntryValues()[1].toString()));
        mIconSize.setValueIndex(idx);
        mIconSize.setSummary(mIconSize.getEntries()[idx]);

        mOpacity = (SeekBarPreference) findPreference(PREF_OPACITY);
        mOpacity.setInitValue(mPrefs.getInt(PREF_OPACITY, 50));
        mOpacity.setOnPreferenceChangeListener(this);

        mDragHandleOpacity = (SeekBarPreference) findPreference(PREF_DRAG_HANDLE_OPACITY);
        mDragHandleOpacity.setInitValue(mPrefs.getInt(PREF_DRAG_HANDLE_OPACITY, 100));
        mDragHandleOpacity.setOnPreferenceChangeListener(this);

        mAdjustHandle = (Preference) findPreference(PREF_ADJUST_HANDLE);
        mButtonConfig = (Preference) findPreference(PREF_BUTTON_CONFIG);
        initButtons();
        mButtons = mPrefs.getString(PREF_BUTTONS, PREF_BUTTON_DEFAULT);
        
        mFavoriteAppsConfig = (Preference) findPreference(PREF_FAVORITE_APPS_CONFIG);
        
        mDragHandleAutoHide = (CheckBoxPreference) findPreference(PREF_AUTO_HIDE_HANDLE);
        mDragHandleEnable = (SwitchPreference) findPreference(PREF_DRAG_HANDLE_ENABLE);
        mDragHandleEnable.setOnPreferenceChangeListener(this);
        mDragHandleColor = (DragHandleColorPreference) findPreference(PREF_DRAG_HANDLE_COLOR);
        
        mGravity = (ListPreference) findPreference(PREF_GRAVITY);
        mGravity.setOnPreferenceChangeListener(this);
        idx = mGravity.findIndexOfValue(mPrefs.getString(PREF_GRAVITY,
                mGravity.getEntryValues()[0].toString()));
        mGravity.setValueIndex(idx);
        mGravity.setSummary(mGravity.getEntries()[idx]);

        mIconpack = (Preference) findPreference(PREF_ICONPACK);
        updateDragHandleEnablement(mDragHandleEnable.isChecked());

        mPrefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs,
                    String key) {
                updatePrefs(prefs, key);
            }
        };
        updatePrefs(mPrefs, null);
    }

    private void updateDragHandleEnablement(Boolean value) {
        boolean dragHandleEnable = value.booleanValue();
        mAdjustHandle.setEnabled(dragHandleEnable);
        mDragHandleOpacity.setEnabled(dragHandleEnable);
        mDragHandleAutoHide.setEnabled(dragHandleEnable);
        mDragHandleColor.setEnabled(dragHandleEnable);
    }

    private class ButtonsApplyRunnable implements CheckboxListDialog.ApplyRunnable {
        public void apply(boolean[] buttons) {
            mButtons = Utils.buttonArrayToString(buttons);
            mPrefs.edit().putString(PREF_BUTTONS, mButtons).commit();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mAdjustHandle) {
            mGestureView = new SettingsGestureView(this);
            mGestureView.show();
            return true;
        } else if (preference == mFavoriteAppsConfig) {
            showManageAppDialog();
            return true;
        } else if (preference == mButtonConfig){
            boolean[] buttons = Utils.getDefaultButtons();
            Utils.buttonStringToArry(mButtons, buttons);
            CheckboxListDialog dialog = new CheckboxListDialog(this,
                    mButtonEntries, mButtonImages, buttons, new ButtonsApplyRunnable(),
                    getResources().getString(R.string.buttons_title));
            dialog.show();
            return true;
        } else if (preference == mIconpack){
            IconPackHelper.pickIconPack(this);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mIconSize) {
            String value = (String) newValue;
            int idx = mIconSize.findIndexOfValue(value);
            mIconSize.setSummary(mIconSize.getEntries()[idx]);
            mIconSize.setValueIndex(idx);
            return true;
        } else if (preference == mOpacity) {
            float val = Float.parseFloat((String) newValue);
            mPrefs.edit().putInt(PREF_OPACITY, (int) val).commit();
            return true;
        } else if (preference == mDragHandleOpacity) {
            float val = Float.parseFloat((String) newValue);
            mPrefs.edit().putInt(PREF_DRAG_HANDLE_OPACITY, (int) val).commit();
            return true;
        } else if (preference == mDragHandleEnable) {
            updateDragHandleEnablement((Boolean) newValue);
            return true;
        } else if (preference == mGravity) {
            String value = (String) newValue;
            int idx = mGravity.findIndexOfValue(value);
            mGravity.setSummary(mGravity.getEntries()[idx]);
            mGravity.setValueIndex(idx);
            return true;
        }

        return false;
    }

    private void showManageAppDialog() {
        if (mManageAppDialog != null && mManageAppDialog.isShowing()) {
            return;
        }

        String favoriteListString = mPrefs.getString(PREF_FAVORITE_APPS, "");
        List<String> favoriteList = new ArrayList<String>();
        Utils.parseFavorites(favoriteListString, favoriteList);

        mManageAppDialog = new FavoriteDialog(this, favoriteList);
        mManageAppDialog.show();
    }

    public void applyChanges(List<String> favoriteList){
        mPrefs.edit()
                .putString(PREF_FAVORITE_APPS,
                        Utils.flattenFavorites(favoriteList))
                .commit();
    }
    
    private void initButtons(){
        mButtonEntries = getResources().getStringArray(R.array.button_entries);
        mButtonImages = new Drawable[mButtonEntries.length];
        mButtonImages[0]=BitmapUtils.colorize(getResources(), Color.GRAY, getResources().getDrawable(R.drawable.kill_all));
        mButtonImages[1]=BitmapUtils.colorize(getResources(), Color.GRAY, getResources().getDrawable(R.drawable.kill_other));
        mButtonImages[2]=BitmapUtils.colorize(getResources(), Color.GRAY, getResources().getDrawable(R.drawable.lastapp));
        mButtonImages[3]=BitmapUtils.colorize(getResources(), Color.GRAY, getResources().getDrawable(R.drawable.home));
        mButtonImages[4]=BitmapUtils.colorize(getResources(), Color.GRAY, getResources().getDrawable(R.drawable.settings));
        mButtonImages[5]=BitmapUtils.colorize(getResources(), Color.GRAY, getResources().getDrawable(R.drawable.ic_allapps));
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // dont restart activity on orientation changes
        if (mGestureView != null && mGestureView.isShowing()){
            mGestureView.handleRotation();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        boolean startOnBoot = mPrefs.getBoolean(SettingsActivity.PREF_START_ON_BOOT, false);
        mToggleServiceSwitch = (Switch) menu.findItem(R.id.toggle_service).getActionView().findViewById(R.id.switch_item);
        mToggleServiceSwitch.setChecked(SwitchService.isRunning() && mPrefs.getBoolean(SettingsActivity.PREF_ENABLE, startOnBoot));
        mToggleServiceSwitch.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                boolean value = ((Switch)v).isChecked();
                Intent svc = new Intent(SettingsActivity.this, SwitchService.class);
                Log.d(TAG, "toggle service " + value);
                if (value) {
                    if (SwitchService.isRunning()){
                        stopService(svc);
                    }
                    startService(svc);
                } else {
                    if (SwitchService.isRunning()){
                        stopService(svc);
                    }
                }
                mPrefs.edit().putBoolean(PREF_ENABLE, value).commit();
            }});
        return true;
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        if (!SwitchService.isRunning()){
            IconPackHelper.getInstance(SettingsActivity.this).updatePrefs(mPrefs, null);
//            PackageManager.getInstance(SettingsActivity.this).updatePrefs(mPrefs, null);
        }
    }
}
