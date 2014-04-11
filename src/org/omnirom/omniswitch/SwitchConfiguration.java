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

import org.omnirom.omniswitch.ui.ColorDrawableWithDimensions;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class SwitchConfiguration {
    public float mBackgroundOpacity = 0.5f;
    public boolean mDimBehind;
    public int mLocation = 0; // 0 = right 1 = left
    public boolean mAnimate = true;
    public int mIconSize = 60; // in dip
    public int mBigIconSizePx = 100; // in px
    public int mSmallIconSizePx = 60; // in px
    public int mActionIconSize = 60; // in dp
    public int mActionIconSizePx = 60; // in px
    public int mIconBorder = 8; // in dp
    public float mDensity;
    public int mMaxWidth;
    public int mMaxHeight;
    public boolean mShowRambar;
    public int mStartYRelative;
    public int mDragHandleHeight;
    public int mDragHandleWidth;
    public boolean mShowLabels = true;
    public int mDragHandleColor;
    public float mDragHandleOpacity;
    public int mGlowColor;
    public int mDefaultColor;
    public int mIconDpi;
    public boolean mAutoHide;
    public static final int AUTO_HIDE_DEFAULT = 3000; // 3s
    public boolean mDragHandleShow = true;
    public int mGravity;
    public boolean mRestrictedMode;
    public int mLevelHeight; // in px
    public int mItemChangeWidthX; // in px - maximum value - can be lower if more items
    public Drawable mDefaultThumbnailBackground;
    public int mThumbnailWidth; // in px
    public int mThumbnailHeight; // in px
    public Map<Integer, Boolean> mButtons;
    public int mLevelChangeWidthX; // in px
    public boolean mLevelBackgroundColor = true;
    public boolean mLimitLevelChangeX = true;
    public Map<Integer, Boolean> mSpeedSwitchButtons;
    public int mLimitItemsX = 10;

    public static SwitchConfiguration mInstance;
    private WindowManager mWindowManager;
    private int mDefaultHandleHeight;
    private int mHorizontalMargin;

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
                .getColor(R.color.holo_blue_light);
        mGlowColor = context.getResources().getColor(R.color.glow_color);
        mDefaultHandleHeight = Math.round(100 * mDensity);
        mHorizontalMargin = Math.round(5 * mDensity);
        mRestrictedMode = !hasSystemPermission(context);
        mLevelHeight = Math.round(80 * mDensity);
        mItemChangeWidthX = Math.round(40 * mDensity);
        mBigIconSizePx = Math.round(mBigIconSizePx * mDensity);
        mSmallIconSizePx = Math.round(mSmallIconSizePx * mDensity);
        mActionIconSizePx = Math.round(mActionIconSize * mDensity);
        mLevelChangeWidthX = Math.round(60 * mDensity);

        // Render the default thumbnail background
        mThumbnailWidth = (int) context.getResources().getDimensionPixelSize(
                R.dimen.thumbnail_width);
        mThumbnailHeight = (int) context.getResources()
                .getDimensionPixelSize(R.dimen.thumbnail_height);
        mDefaultThumbnailBackground = new ColorDrawableWithDimensions(
                Color.BLACK, mThumbnailWidth, mThumbnailHeight);

        updatePrefs(PreferenceManager.getDefaultSharedPreferences(context), "");
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        mLocation = prefs.getInt(SettingsActivity.PREF_DRAG_HANDLE_LOCATION, 0);
        int opacity = prefs.getInt(SettingsActivity.PREF_OPACITY, 50);
        mBackgroundOpacity = (float) opacity / 100.0f;
        mAnimate = prefs.getBoolean(SettingsActivity.PREF_ANIMATE, true);
        String iconSize = prefs
                .getString(SettingsActivity.PREF_ICON_SIZE, "60");
        mIconSize = Integer.valueOf(iconSize);
        mShowRambar = prefs
                .getBoolean(SettingsActivity.PREF_SHOW_RAMBAR, false);
        mShowLabels = prefs.getBoolean(SettingsActivity.PREF_SHOW_LABELS, true);

        int relHeightStart = (int) (getDefaultOffsetStart() / (getCurrentDisplayHeight() / 100));

        mStartYRelative = prefs
                .getInt(SettingsActivity.PREF_HANDLE_POS_START_RELATIVE,
                        relHeightStart);
        mDragHandleHeight = prefs.getInt(SettingsActivity.PREF_HANDLE_HEIGHT,
                mDefaultHandleHeight);

        mMaxWidth = Math.round((mIconSize + mIconBorder) * mDensity);
        mMaxHeight = Math.round((mIconSize + 3 * mIconBorder) * mDensity);

        mDragHandleColor = prefs.getInt(
                SettingsActivity.PREF_DRAG_HANDLE_COLOR, mDefaultColor);
        opacity = prefs.getInt(SettingsActivity.PREF_DRAG_HANDLE_OPACITY, 100);
        mDragHandleOpacity = (float) opacity / 100.0f;
        mAutoHide = prefs.getBoolean(SettingsActivity.PREF_AUTO_HIDE_HANDLE,
                false);
        mDragHandleShow = prefs.getBoolean(
                SettingsActivity.PREF_DRAG_HANDLE_ENABLE, true);
        mDimBehind = prefs.getBoolean(SettingsActivity.PREF_DIM_BEHIND, false);
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
    }

    // includes rotation
    public int getCurrentDisplayHeight() {
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);
        int height = dm.heightPixels;
        return height;
    }

    public int getCurrentDisplayWidth() {
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        return width;
    }

    public boolean isLandscape() {
        return getCurrentDisplayWidth() > getCurrentDisplayHeight();
    }

    public int getCurrentOverlayWidth() {
        if (isLandscape()) {
            // landscape
            return Math.max((int) (getCurrentDisplayWidth() * 0.66f),
                    getCurrentDisplayHeight());
        }
        return getCurrentDisplayWidth() - mHorizontalMargin;
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
}
