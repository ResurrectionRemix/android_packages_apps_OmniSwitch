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
import android.view.WindowManager;

public class Configuration {
    public float mBackgroundOpacity = 0.8f;
    public int mLocation = 0; // 0 = right 1 = left
    public boolean mAnimate = true;
    public int mIconSize = 60; // in dip
    public float mDensity;
    public int mHorizontalMaxWidth = mIconSize;
    public int mHorizontalScrollerHeight = mIconSize * 2;
    public boolean mShowRambar;
    public int mStartY;
    public int mEndY;
    public boolean mShowLabels = true;
    public int mScreenHeight;
    public int mColor;
    public int mDefaultColor;
    public boolean mShowDragHandle = true;
    public int mIconDpi;

    public static Configuration mInstance;

    public static Configuration getInstance(Context context) {
        if(mInstance==null){
            mInstance = new Configuration(context);
        }
        return mInstance;
    }
    
    private Configuration(Context context){
        mDensity = context.getResources().getDisplayMetrics().density;
        mIconDpi = context.getResources().getDisplayMetrics().densityDpi;

        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);

        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        mScreenHeight = size.y;
        mDefaultColor = context.getResources().getColor(R.color.holo_blue_light);
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        mLocation = prefs.getInt(
                SettingsActivity.PREF_DRAG_HANDLE_LOCATION, 0);
        int opacity = prefs.getInt(SettingsActivity.PREF_OPACITY, 80);
        mBackgroundOpacity = (float) opacity / 100.0f;
        mAnimate = prefs.getBoolean(SettingsActivity.PREF_ANIMATE, true);
        String iconSize = prefs
                .getString(SettingsActivity.PREF_ICON_SIZE, "60");
        mIconSize = Integer.valueOf(iconSize);
        mShowRambar = prefs
                .getBoolean(SettingsActivity.PREF_SHOW_RAMBAR, false);
        mShowLabels = prefs.getBoolean(SettingsActivity.PREF_SHOW_LABELS, true);
        int defaultHeight = (int) (100 * mDensity + 0.5);
        mStartY = prefs.getInt(SettingsActivity.PREF_HANDLE_POS_START, mScreenHeight / 2 - defaultHeight / 2);
        mEndY = prefs.getInt(SettingsActivity.PREF_HANDLE_POS_END, mScreenHeight / 2 + defaultHeight / 2);

        mHorizontalMaxWidth = (int) ((mIconSize + 10) * mDensity + 0.5f);
        mHorizontalScrollerHeight = (int) ((mIconSize + (mShowLabels ? 40 : 10))
                * mDensity + 0.5f);
        mColor = prefs
                .getInt(SettingsActivity.PREF_DRAG_HANDLE_COLOR, mDefaultColor);
        mShowDragHandle = prefs.getBoolean(SettingsActivity.PREF_SHOW_DRAG_HANDLE, true);
    }
}
