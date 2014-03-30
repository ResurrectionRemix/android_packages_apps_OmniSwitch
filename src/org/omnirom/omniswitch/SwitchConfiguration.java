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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class SwitchConfiguration {
    public float mBackgroundOpacity = 0.5f;
    public boolean mDimBehind;
    public int mLocation = 0; // 0 = right 1 = left
    public boolean mAnimate = true;
    public int mIconSize = 60; // in dip
    public int mIconBorder = 8; // in dip
    public float mDensity;
    public int mHorizontalMaxWidth ;
    public int mHorizontalMaxHeight;
    public boolean mShowRambar;
    public int mStartYRelative;
    public int mHandleHeight;
    public int mHandleWidth;
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

    public static SwitchConfiguration mInstance;
    private WindowManager mWindowManager;
    private int mDefaultHeight;
    private int mHorizontalMargin;

    public static SwitchConfiguration getInstance(Context context) {
        if(mInstance==null){
            mInstance = new SwitchConfiguration(context);
        }
        return mInstance;
    }
    
    private SwitchConfiguration(Context context){
        mDensity = context.getResources().getDisplayMetrics().density;

        mWindowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);

        Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);
        mDefaultColor = context.getResources().getColor(R.color.holo_blue_light);
        mGlowColor = context.getResources().getColor(R.color.glow_color);
        mDefaultHeight = Math.round(100 * mDensity);
        mHorizontalMargin = Math.round(5 * mDensity);
        updatePrefs(PreferenceManager.getDefaultSharedPreferences(context), "");
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        mLocation = prefs.getInt(
                SettingsActivity.PREF_DRAG_HANDLE_LOCATION, 0);
        int opacity = prefs.getInt(SettingsActivity.PREF_OPACITY, 50);
        mBackgroundOpacity = (float) opacity / 100.0f;
        mAnimate = prefs.getBoolean(SettingsActivity.PREF_ANIMATE, true);
        String iconSize = prefs
                .getString(SettingsActivity.PREF_ICON_SIZE, "60");
        mIconSize = Integer.valueOf(iconSize);
        mShowRambar = prefs
                .getBoolean(SettingsActivity.PREF_SHOW_RAMBAR, false);
        mShowLabels = prefs.getBoolean(SettingsActivity.PREF_SHOW_LABELS, true);

        int relHeightStart = (int)(getDefaultOffsetStart() / (getCurrentDisplayHeight() /100));

        mStartYRelative = prefs.getInt(SettingsActivity.PREF_HANDLE_POS_START_RELATIVE, relHeightStart);
        mHandleHeight = prefs.getInt(SettingsActivity.PREF_HANDLE_HEIGHT, mDefaultHeight);

        mHorizontalMaxWidth = Math.round((mIconSize + mIconBorder) * mDensity);
        mHorizontalMaxHeight = Math.round((mIconSize + 3 * mIconBorder) * mDensity);

        mDragHandleColor = prefs
                .getInt(SettingsActivity.PREF_DRAG_HANDLE_COLOR, mDefaultColor);
        opacity = prefs.getInt(SettingsActivity.PREF_DRAG_HANDLE_OPACITY, 100);
        mDragHandleOpacity = (float) opacity / 100.0f;
        mAutoHide= prefs.getBoolean(SettingsActivity.PREF_AUTO_HIDE_HANDLE, false);
        mDragHandleShow = prefs.getBoolean(SettingsActivity.PREF_DRAG_HANDLE_ENABLE, true);
        mDimBehind = prefs.getBoolean(SettingsActivity.PREF_DIM_BEHIND, false);
        String gravity = prefs.getString(SettingsActivity.PREF_GRAVITY, "0");
        mGravity = Integer.valueOf(gravity);
        mHandleWidth = Math.round(20 * mDensity);
    }
    
    // includes rotation                
    private int getCurrentDisplayHeight(){
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);
        int height = dm.heightPixels;
        return height;
    }
    
    public int getCurrentDisplayWidth(){
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        return width;
    }

    public boolean isLandscape() {
        return getCurrentDisplayWidth() > getCurrentDisplayHeight();
    }

    public int getCurrentOverlayWidth() {
        if (isLandscape()){
            // landscape
            return Math.max((int)(getCurrentDisplayWidth() * 0.66f), getCurrentDisplayHeight());
        }
        return getCurrentDisplayWidth() - mHorizontalMargin;
    }
    
    public int getCurrentOffsetStart(){
        return (getCurrentDisplayHeight() / 100) * mStartYRelative;
    }

    public int getCustomOffsetStart(int startYRelative){
        return (getCurrentDisplayHeight() / 100) * startYRelative;
    }
    
    public int getDefaultOffsetStart(){
        return ((getCurrentDisplayHeight() / 2) - mDefaultHeight /2);
    }

    public int getDefaultHeightRelative(){
        return mDefaultHeight / (getCurrentDisplayHeight() / 100);
    }

    public int getCurrentOffsetEnd(){
        return getCurrentOffsetStart() + mHandleHeight;
    }

    public int getCustomOffsetEnd(int startYRelative, int handleHeight){
        return getCustomOffsetStart(startYRelative) + handleHeight;
    }
    
    public int getDefaultOffsetEnd(){
        return getDefaultOffsetStart() + mDefaultHeight;
    }
}
