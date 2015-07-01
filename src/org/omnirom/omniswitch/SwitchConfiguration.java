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

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.omnirom.omniswitch.ui.BitmapCache;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.view.WindowManager;

public class SwitchConfiguration {
    public float mBackgroundOpacity = 0.7f;
    public boolean mDimBehind = true;
    public int mLocation = 0; // 0 = right 1 = left
    public boolean mAnimate = true;
    public int mIconSize = 60; // in dip
    public int mIconSizeSettings = 60; // in dip
    public int mBigIconSizePx = 100; // in px
    public int mSmallIconSizePx = 60; // in px
    public int mActionIconSize = 60; // in dp
    public int mActionIconSizePx = 60; // in px
    public int mOverlayIconSizeDp = 30;
    public int mOverlayIconSizePx = 30;
    public int mIconBorder = 8; // in dp
    public float mDensity;
    public int mMaxWidth;
    public boolean mShowRambar = true;
    public int mStartYRelative;
    public int mDragHandleHeight;
    public int mDragHandleWidth;
    public boolean mShowLabels = true;
    public int mDragHandleColor;
    public float mDragHandleOpacity;
    public int mDefaultColor;
    public int mIconDpi;
    public boolean mAutoHide;
    public static final int AUTO_HIDE_DEFAULT = 3000; // 3s
    public boolean mDragHandleShow = true;
    public int mGravity;
    public boolean mRestrictedMode;
    public int mLevelHeight; // in px
    public int mItemChangeWidthX; // in px - maximum value - can be lower if more items
    public int mThumbnailWidth; // in px
    public int mThumbnailHeight; // in px
    public Map<Integer, Boolean> mButtons;
    public int mLevelChangeWidthX; // in px
    public boolean mLevelBackgroundColor = true;
    public boolean mLimitLevelChangeX = true;
    public Map<Integer, Boolean> mSpeedSwitchButtons;
    public int mLimitItemsX = 10;
    public int mHorizontalDividerWidth;
    public float mLabelFontSize;
    public int mButtonPos = 0; // 0 = top 1 = bottom
    public int mBgStyle = 0; // 0 = solid 1 = transparent
    public List<String> mFavoriteList = new ArrayList<String>();
    public boolean mSpeedSwitcher = true;
    public boolean mFilterActive = true;
    public boolean mFilterBoot = true;
    public long mFilterTime = 0;
    public boolean mSideHeader = true;
    public static SwitchConfiguration mInstance;
    private WindowManager mWindowManager;
    private int mDefaultHandleHeight;
    private int mLabelFontSizePx;
    public int mMaxHeight;
    public int mMemDisplaySize;
    public int mLayoutStyle;
    public float mThumbRatio = 1.0f;

    public static SwitchConfiguration getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SwitchConfiguration(context);
        }
        return mInstance;
    }

    private SwitchConfiguration(Context context) {
        mDensity = context.getResources().getDisplayMetrics().density;

        mWindowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);

        Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);
        mDefaultColor = context.getResources()
                .getColor(R.color.material_green);
        mDefaultHandleHeight = Math.round(100 * mDensity);
        mRestrictedMode = !hasSystemPermission(context);
        mLevelHeight = Math.round(80 * mDensity);
        mItemChangeWidthX = Math.round(40 * mDensity);
        mBigIconSizePx = Math.round(mBigIconSizePx * mDensity);
        mSmallIconSizePx = Math.round(mSmallIconSizePx * mDensity);
        mActionIconSizePx = Math.round(mActionIconSize * mDensity);
        mLevelChangeWidthX = Math.round(60 * mDensity);
        mOverlayIconSizePx = Math.round(mOverlayIconSizePx * mDensity);
        mHorizontalDividerWidth = 0;
        // Render the default thumbnail background
        mThumbnailWidth = (int) context.getResources().getDimensionPixelSize(
                R.dimen.thumbnail_width);
        mThumbnailHeight = (int) context.getResources()
                .getDimensionPixelSize(R.dimen.thumbnail_height);
        mMemDisplaySize = (int) context.getResources().getDimensionPixelSize(
                R.dimen.ram_display_size);
        updatePrefs(PreferenceManager.getDefaultSharedPreferences(context), "");
    }

    public void initDefaults(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains(SettingsActivity.PREF_BG_STYLE) &&
                prefs.contains(SettingsActivity.PREF_FLAT_STYLE)) {
            boolean flatStyle = prefs.getBoolean(SettingsActivity.PREF_FLAT_STYLE, true);
            prefs.edit().putString(SettingsActivity.PREF_BG_STYLE, flatStyle ? "0" : "1").commit();
        }
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        mLocation = prefs.getInt(SettingsActivity.PREF_DRAG_HANDLE_LOCATION, 0);
        int opacity = prefs.getInt(SettingsActivity.PREF_OPACITY, 70);
        mBackgroundOpacity = (float) opacity / 100.0f;
        mAnimate = prefs.getBoolean(SettingsActivity.PREF_ANIMATE, true);
        String iconSize = prefs
                .getString(SettingsActivity.PREF_ICON_SIZE, String.valueOf(mIconSize));
        mIconSize = Integer.valueOf(iconSize);
        mShowRambar = prefs
                .getBoolean(SettingsActivity.PREF_SHOW_RAMBAR, true);
        mShowLabels = prefs.getBoolean(SettingsActivity.PREF_SHOW_LABELS, true);

        int relHeightStart = (int) (getDefaultOffsetStart() / (getCurrentDisplayHeight() / 100));

        mStartYRelative = prefs
                .getInt(SettingsActivity.PREF_HANDLE_POS_START_RELATIVE,
                        relHeightStart);
        mDragHandleHeight = prefs.getInt(SettingsActivity.PREF_HANDLE_HEIGHT,
                mDefaultHandleHeight);

        mMaxWidth = Math.round((mIconSize + mIconBorder) * mDensity);
        mMaxHeight = Math.round((mIconSize + mIconBorder) * mDensity);
        mLabelFontSize = 15f;
        // add a small gap
        mLabelFontSizePx = Math.round((mLabelFontSize + mIconBorder) * mDensity);

        mDragHandleColor = prefs.getInt(
                SettingsActivity.PREF_DRAG_HANDLE_COLOR, mDefaultColor);
        opacity = prefs.getInt(SettingsActivity.PREF_DRAG_HANDLE_OPACITY, 100);
        mDragHandleOpacity = (float) opacity / 100.0f;
        mAutoHide = prefs.getBoolean(SettingsActivity.PREF_AUTO_HIDE_HANDLE,
                false);
        mDragHandleShow = prefs.getBoolean(
                SettingsActivity.PREF_DRAG_HANDLE_ENABLE, true);
        mDimBehind = prefs.getBoolean(SettingsActivity.PREF_DIM_BEHIND, true);
        String gravity = prefs.getString(SettingsActivity.PREF_GRAVITY, "0");
        mGravity = Integer.valueOf(gravity);
        mDragHandleWidth = Math.round(20 * mDensity);
        mButtons = Utils.buttonStringToMap(prefs.getString(SettingsActivity.PREF_BUTTONS_NEW,
                SettingsActivity.PREF_BUTTON_DEFAULT_NEW), SettingsActivity.PREF_BUTTON_DEFAULT_NEW);
        mLevelBackgroundColor = prefs.getBoolean(SettingsActivity.PREF_SPEED_SWITCHER_COLOR, true);
        mLimitLevelChangeX = prefs.getBoolean(SettingsActivity.PREF_SPEED_SWITCHER_LIMIT, true);
        mSpeedSwitchButtons = Utils.buttonStringToMap(prefs.getString(SettingsActivity.PREF_SPEED_SWITCHER_BUTTON_NEW,
                SettingsActivity.PREF_SPEED_SWITCHER_BUTTON_DEFAULT_NEW), SettingsActivity.PREF_SPEED_SWITCHER_BUTTON_DEFAULT_NEW);
        mLimitItemsX = prefs.getInt(SettingsActivity.PREF_SPEED_SWITCHER_ITEMS, 10);
        String buttonPos = prefs.getString(SettingsActivity.PREF_BUTTON_POS, "0");
        mButtonPos = Integer.valueOf(buttonPos);
        String bgStyle = prefs.getString(SettingsActivity.PREF_BG_STYLE, "0");
        mBgStyle = Integer.valueOf(bgStyle);

        mFavoriteList.clear();
        String favoriteListString = prefs.getString(SettingsActivity.PREF_FAVORITE_APPS, "");
        Utils.parseFavorites(favoriteListString, mFavoriteList);
        mSpeedSwitcher = prefs.getBoolean(SettingsActivity.PREF_SPEED_SWITCHER, true);
        mFilterBoot = prefs.getBoolean(SettingsActivity.PREF_APP_FILTER_BOOT, true);
        String filterTimeString = prefs.getString(SettingsActivity.PREF_APP_FILTER_TIME, "0");
        mFilterTime = Integer.valueOf(filterTimeString);
        if (mFilterTime != 0) {
            // value is in hours but we need millisecs
            mFilterTime = mFilterTime * 3600 * 1000;
        }
        String layoutStyle = prefs.getString(SettingsActivity.PREF_LAYOUT_STYLE, "0");
        mLayoutStyle = Integer.valueOf(layoutStyle);
        String thumbSize = prefs.getString(SettingsActivity.PREF_THUMB_SIZE, "1.0");
        mThumbRatio = Float.valueOf(thumbSize);
    }

    public void resetDefaults(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
    }

    // includes rotation
    public int getCurrentDisplayHeight() {
        Point size = new Point();
        mWindowManager.getDefaultDisplay().getRealSize(size);
        int height = size.y;
        return height;
    }

    public int getCurrentDisplayWidth() {
        Point size = new Point();
        mWindowManager.getDefaultDisplay().getRealSize(size);
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

    public void calcHorizontalDivider() {
        mHorizontalDividerWidth = 0;
        int numColumns = getCurrentOverlayWidth() / mMaxWidth;
        if (numColumns > 1) {
            int equalWidth = getCurrentOverlayWidth() / numColumns;
            if (equalWidth > mMaxWidth) {
                mHorizontalDividerWidth = equalWidth - mMaxWidth;
            }
        }
    }

    public int getItemMaxHeight() {
        return mShowLabels ? mMaxHeight + mLabelFontSizePx :  mMaxHeight;
    }
}
