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

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

public class SwitchConfiguration {
    private final static String TAG = "SwitchConfiguration";
    private static boolean DEBUG = false;

    public float mBackgroundOpacity = 0.7f;
    public boolean mDimBehind = true;
    public int mLocation = 0; // 0 = right 1 = left
    public boolean mAnimate = true;
    public int mIconSize = 60; // in dip
    public int mIconSizePx = 60;
    public int mQSActionSizePx = 60; // in px
    public int mActionSizePx = 48; // in px
    public int mOverlayIconSizeDp = 30;
    public int mOverlayIconSizePx = 30;
    public int mOverlayIconBorderDp = 2;
    public int mOverlayIconBorderPx = 2;
    public int mIconBorderDp = 4; // in dp
    public int mIconBorderPx = 4;
    public float mDensity;
    public int mDensityDpi;
    public int mMaxWidth;
    public boolean mShowRambar = true;
    public int mStartYRelative;
    public int mDragHandleHeight;
    public int mDragHandleWidth;
    public int mDefaultDragHandleWidth;
    public boolean mShowLabels = true;
    public int mDragHandleColor;
    public int mDefaultColor;
    public int mIconDpi;
    public boolean mAutoHide;
    public static final int AUTO_HIDE_DEFAULT = 3000; // 3s
    public boolean mDragHandleShow = true;
    public boolean mRestrictedMode;
    public int mLevelHeight; // in px
    public int mItemChangeWidthX; // in px - maximum value - can be lower if more items
    public int mThumbnailWidth; // in px
    public int mThumbnailHeight; // in px
    public Map<Integer, Boolean> mButtons;
    public boolean mLevelBackgroundColor = true;
    public boolean mLimitLevelChangeX = true;
    public Map<Integer, Boolean> mSpeedSwitchButtons;
    public int mLimitItemsX = 10;
    public float mLabelFontSize;
    public int mButtonPos = 1; // 0 = top 1 = bottom
    public List<String> mFavoriteList = new ArrayList<String>();
    public boolean mSpeedSwitcher = true;
    public boolean mFilterActive = true;
    public boolean mFilterBoot;
    public boolean mFilterRunning;
    public long mFilterTime;
    public boolean mSideHeader = true;
    private static SwitchConfiguration mInstance;
    private WindowManager mWindowManager;
    private int mDefaultHandleHeight;
    private int mLabelFontSizePx;
    public int mMaxHeight;
    public int mMemDisplaySize;
    public int mLayoutStyle;
    public float mThumbRatio = 1.0f;
    public IconSize mIconSizeDesc = IconSize.NORMAL;
    public int mIconBorderHorizontalDp = 8; // in dp
    public int mIconBorderHorizontalPx = 8; // in px
    public BgStyle mBgStyle = BgStyle.SOLID_LIGHT;
    public boolean mLaunchStatsEnabled;
    public boolean mRevertRecents;
    public int mIconSizeQuickPx = 100;
    // TODO do we need a setting for this
    public boolean mLoadThumbOnSwipe = true;
    public boolean mDimActionButton;
    public List<String> mLockedAppList = new ArrayList<String>();
    public boolean mTopSortLockedApps;

    // old pref slots
    private static final String PREF_DRAG_HANDLE_COLOR = "drag_handle_color";
    private static final String PREF_DRAG_HANDLE_OPACITY = "drag_handle_opacity";
    private static final String PREF_FLAT_STYLE = "flat_style";

    private List<OnSharedPreferenceChangeListener> mPrefsListeners = new ArrayList<OnSharedPreferenceChangeListener>();

    public enum IconSize {
        SMALL,
        NORMAL,
        LARGE
    }

    public enum BgStyle {
        TRANSPARENT,
        SOLID_LIGHT,
        SOLID_DARK
    }

    public static SwitchConfiguration getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SwitchConfiguration(context);
        }
        return mInstance;
    }

    private SwitchConfiguration(Context context) {
        mWindowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);

        mDefaultColor = context.getResources()
                .getColor(R.color.default_drag_handle_color);
        setDensityConfiguration(context);
        updatePrefs(PreferenceManager.getDefaultSharedPreferences(context), "");
    }

    public boolean onConfigurationChanged(Context context) {
        final float newDensity = context.getResources().getDisplayMetrics().density;
        if(DEBUG){
            Log.d(TAG, "onConfigurationChanged " + mDensity + " " + newDensity);
        }
        if (newDensity != mDensity) {
            setDensityConfiguration(context);
            return true;
        }
        return false;
    }

    private void setDensityConfiguration(Context context) {
        mDensity = context.getResources().getDisplayMetrics().density;
        mDensityDpi =  context.getResources().getDisplayMetrics().densityDpi;
        if(DEBUG){
            Log.d(TAG, "setDensityConfiguration " + mDensity);
        }

        mDefaultHandleHeight = Math.round(100 * mDensity);
        mRestrictedMode = !hasSystemPermission(context);
        mLevelHeight = Math.round(80 * mDensity);
        mItemChangeWidthX = Math.round(40 * mDensity);
        mActionSizePx = Math.round(48 * mDensity);
        mQSActionSizePx = Math.round(60 * mDensity);
        mOverlayIconSizePx = Math.round(mOverlayIconSizeDp * mDensity);
        mOverlayIconBorderPx =  Math.round(mOverlayIconBorderDp * mDensity);
        mIconBorderHorizontalPx = Math.round(mIconBorderHorizontalDp * mDensity);
        mIconSizeQuickPx = Math.round(100 * mDensity);
        mIconBorderPx = Math.round(mIconBorderDp * mDensity);
        // Render the default thumbnail background
        mThumbnailWidth = (int) context.getResources().getDimensionPixelSize(
                R.dimen.thumbnail_width);
        mThumbnailHeight = (int) context.getResources()
                .getDimensionPixelSize(R.dimen.thumbnail_height);
        mMemDisplaySize = (int) context.getResources().getDimensionPixelSize(
                R.dimen.ram_display_size);
    }

    public void initDefaults(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains(SettingsActivity.PREF_BG_STYLE) &&
                prefs.contains(PREF_FLAT_STYLE)) {
            boolean flatStyle = prefs.getBoolean(PREF_FLAT_STYLE, true);
            prefs.edit().putString(SettingsActivity.PREF_BG_STYLE, flatStyle ? "0" : "1").commit();
        }
        if (!prefs.contains(SettingsActivity.PREF_DRAG_HANDLE_COLOR_NEW) &&
                prefs.contains(PREF_DRAG_HANDLE_COLOR)) {
            int dragHandleColor = prefs.getInt(PREF_DRAG_HANDLE_COLOR, mDefaultColor);
            int opacity = prefs.getInt(PREF_DRAG_HANDLE_OPACITY, 100);
            dragHandleColor = (dragHandleColor & 0x00FFFFFF) + (opacity << 24);
            prefs.edit().putInt(SettingsActivity.PREF_DRAG_HANDLE_COLOR_NEW, dragHandleColor).commit();
        }
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        if(DEBUG){
            Log.d(TAG, "updatePrefs");
        }
        mLocation = prefs.getInt(SettingsActivity.PREF_DRAG_HANDLE_LOCATION, 0);
        int opacity = prefs.getInt(SettingsActivity.PREF_OPACITY, 70);
        mBackgroundOpacity = (float) opacity / 100.0f;
        mAnimate = prefs.getBoolean(SettingsActivity.PREF_ANIMATE, true);
        String iconSize = prefs
                .getString(SettingsActivity.PREF_ICON_SIZE, String.valueOf(mIconSize));
        mIconSize = Integer.valueOf(iconSize);
        if (mIconSize == 60) {
            mIconSizeDesc = IconSize.NORMAL;
            mIconSize = 52;
        } else if (mIconSize == 80) {
            mIconSizeDesc = IconSize.LARGE;
            mIconSize = 70;
        } else {
            mIconSizeDesc = IconSize.SMALL;
        }
        mShowRambar = prefs.getBoolean(SettingsActivity.PREF_SHOW_RAMBAR, true);
        mShowLabels = prefs.getBoolean(SettingsActivity.PREF_SHOW_LABELS, true);

        int relHeightStart = (int) (getDefaultOffsetStart() / (getCurrentDisplayHeight() / 100));

        mStartYRelative = prefs
                .getInt(SettingsActivity.PREF_HANDLE_POS_START_RELATIVE,
                        relHeightStart);
        mDragHandleHeight = prefs.getInt(SettingsActivity.PREF_HANDLE_HEIGHT,
                mDefaultHandleHeight);

        mIconSizePx = Math.round(mIconSize * mDensity);
        mMaxWidth = Math.round((mIconSize + mIconBorderDp) * mDensity);
        mMaxHeight = Math.round((mIconSize + mIconBorderDp) * mDensity);
        mLabelFontSize = 14f;
        // add a small gap
        mLabelFontSizePx = Math.round((mLabelFontSize + mIconBorderDp) * mDensity);

        mDragHandleColor = prefs.getInt(
                    SettingsActivity.PREF_DRAG_HANDLE_COLOR_NEW, mDefaultColor);
        mAutoHide = prefs.getBoolean(SettingsActivity.PREF_AUTO_HIDE_HANDLE,
                false);
        mDragHandleShow = prefs.getBoolean(
                SettingsActivity.PREF_DRAG_HANDLE_ENABLE, true);
        mDimBehind = prefs.getBoolean(SettingsActivity.PREF_DIM_BEHIND, false);
        mDefaultDragHandleWidth = Math.round(20 * mDensity);
        mDragHandleWidth = prefs.getInt(
                    SettingsActivity.PREF_HANDLE_WIDTH, mDefaultDragHandleWidth);
        mButtons = Utils.buttonStringToMap(prefs.getString(SettingsActivity.PREF_BUTTONS_NEW,
                SettingsActivity.PREF_BUTTON_DEFAULT_NEW), SettingsActivity.PREF_BUTTON_DEFAULT_NEW);
        mLevelBackgroundColor = prefs.getBoolean(SettingsActivity.PREF_SPEED_SWITCHER_COLOR, true);
        mLimitLevelChangeX = prefs.getBoolean(SettingsActivity.PREF_SPEED_SWITCHER_LIMIT, true);
        mSpeedSwitchButtons = Utils.buttonStringToMap(prefs.getString(SettingsActivity.PREF_SPEED_SWITCHER_BUTTON_NEW,
                SettingsActivity.PREF_SPEED_SWITCHER_BUTTON_DEFAULT_NEW), SettingsActivity.PREF_SPEED_SWITCHER_BUTTON_DEFAULT_NEW);
        mLimitItemsX = prefs.getInt(SettingsActivity.PREF_SPEED_SWITCHER_ITEMS, 10);
        String buttonPos = prefs.getString(SettingsActivity.PREF_BUTTON_POS, "1");
        mButtonPos = Integer.valueOf(buttonPos);

        String bgStyle = prefs.getString(SettingsActivity.PREF_BG_STYLE, "0");
        int bgStyleInt = Integer.valueOf(bgStyle);
        if (bgStyleInt == 0) {
            mBgStyle = BgStyle.SOLID_LIGHT;
        } else if(bgStyleInt == 1) {
            mBgStyle = BgStyle.TRANSPARENT;
        } else {
            mBgStyle = BgStyle.SOLID_DARK;
        }

        mFavoriteList.clear();
        String favoriteListString = prefs.getString(SettingsActivity.PREF_FAVORITE_APPS, "");
        Utils.parseFavorites(favoriteListString, mFavoriteList);
        mSpeedSwitcher = prefs.getBoolean(SettingsActivity.PREF_SPEED_SWITCHER, true);
        mFilterBoot = prefs.getBoolean(SettingsActivity.PREF_APP_FILTER_BOOT, false);
        String filterTimeString = prefs.getString(SettingsActivity.PREF_APP_FILTER_TIME, "0");
        mFilterTime = Integer.valueOf(filterTimeString);
        if (mFilterTime != 0) {
            // value is in hours but we need millisecs
            mFilterTime = mFilterTime * 3600 * 1000;
        }
        String layoutStyle = prefs.getString(SettingsActivity.PREF_LAYOUT_STYLE, "1");
        mLayoutStyle = Integer.valueOf(layoutStyle);
        String thumbSize = prefs.getString(SettingsActivity.PREF_THUMB_SIZE, "1.0");
        mThumbRatio = Float.valueOf(thumbSize);
        mFilterRunning = prefs.getBoolean(SettingsActivity.PREF_APP_FILTER_RUNNING, false);
        mLaunchStatsEnabled = prefs.getBoolean(SettingsActivity.PREF_LAUNCH_STATS, false);
        mRevertRecents = prefs.getBoolean(SettingsActivity.PREF_REVERT_RECENTS, false);
        mLoadThumbOnSwipe = prefs.getBoolean(SettingsActivity.PREF_SWIPE_THUMB_UPDATE, true);
        mDimActionButton = prefs.getBoolean(SettingsActivity.PREF_DIM_ACTION_BUTTON, false);
        mLockedAppList.clear();
        String lockedAppsListString = prefs.getString(SettingsActivity.PREF_LOCKED_APPS_LIST, "");
        Utils.parseLockedApps(lockedAppsListString, mLockedAppList);
        mTopSortLockedApps = prefs.getBoolean(SettingsActivity.PREF_LOCKED_APPS_SORT, false);

        for(OnSharedPreferenceChangeListener listener : mPrefsListeners) {
            if(DEBUG){
                Log.d(TAG, "onSharedPreferenceChanged " + listener.getClass().getName());
            }
            listener.onSharedPreferenceChanged(prefs, key);
        }
    }

    public void resetDefaults(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
    }

    // includes rotation
    public int getCurrentDisplayHeight() {
        Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);
        int height = size.y;
        return height;
    }

    public int getCurrentDisplayWidth() {
        Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);
        int width = size.x;
        return width;
    }

    public boolean isLandscape() {
        return getCurrentDisplayWidth() > getCurrentDisplayHeight();
    }

    public int getCurrentOverlayWidth() {
        if (isLandscape()) {
            // landscape
            return Math.max(mMaxWidth * 6,
                (int) (getCurrentDisplayWidth() * 0.66f));
        }
        return getCurrentDisplayWidth();
    }

    public int getCurrentOffsetStart() {
        return (getCurrentDisplayHeight() / 100) * mStartYRelative;
    }

    public int getCustomOffsetStart(int startYRelative) {
        return (getCurrentDisplayHeight() / 100) * startYRelative;
    }

    public int getDefaultOffsetStart() {
        return ((getCurrentDisplayHeight() / 2) - mDefaultHandleHeight / 2);
    }

    public int getDefaultHeightRelative() {
        return mDefaultHandleHeight / (getCurrentDisplayHeight() / 100);
    }

    public int getCurrentOffsetEnd() {
        return getCurrentOffsetStart() + mDragHandleHeight;
    }

    public int getCustomOffsetEnd(int startYRelative, int handleHeight) {
        return getCustomOffsetStart(startYRelative) + handleHeight;
    }

    public int getDefaultOffsetEnd() {
        return getDefaultOffsetStart() + mDefaultHandleHeight;
    }

    private boolean hasSystemPermission(Context context) {
        int result = context
                .checkCallingOrSelfPermission(android.Manifest.permission.REMOVE_TASKS);
        return result == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    public int calcHorizontalDivider(boolean fullscreen) {
        int horizontalDividerWidth = 0;
        int width = fullscreen ? getCurrentDisplayWidth() : getCurrentOverlayWidth();
        int columnWidth = mMaxWidth + mIconBorderHorizontalPx;
        int numColumns = width / columnWidth;
        if (numColumns > 1) {
            int equalWidth = width / numColumns;
            if (equalWidth > columnWidth) {
                horizontalDividerWidth = equalWidth - columnWidth;
            }
        }
        return horizontalDividerWidth;
    }

    public int calcVerticalDivider(int height) {
        int verticalDividerHeight = 0;
        int numRows = height / getItemMaxHeight();
        if (numRows > 1) {
            int equalHeight = height / numRows;
            if (equalHeight > getItemMaxHeight()) {
                verticalDividerHeight = equalHeight - getItemMaxHeight();
            }
        }
        return verticalDividerHeight;
    }

    public int getItemMaxHeight() {
        return mShowLabels ? mMaxHeight + mLabelFontSizePx :  mMaxHeight;
    }

    public int getOverlayHeaderWidth() {
        return mOverlayIconSizePx + 2 * mOverlayIconBorderPx;
    }

    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener prefsListener) {
        mPrefsListeners.add(prefsListener);
    }

    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener prefsListener) {
        mPrefsListeners.remove(prefsListener);
    }

    public int getLauncherViewWidth() {
        if (isLandscape()) {
            return (int) (getCurrentDisplayWidth() * 0.75f);
        }
        return getCurrentDisplayWidth();
    }
}
