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
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class SettingsGestureView extends LinearLayout {
    private final static String TAG = "SettingsGestureView";

    private Context mContext;
    private boolean mShowing;
    private float mPosY = -1.0f;

    public SettingsGestureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    float y = event.getY();
                    Log.d(TAG, "y " + y);
                    mPosY = y;
                    PreferenceManager.getDefaultSharedPreferences(mContext)
                            .edit().putFloat("handle_pos_y", mPosY).commit();
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    hide();
                    break;
                }
                return true;
            }
        });
        setBackgroundColor(Color.BLACK);
        getBackground().setAlpha(180);
    }

    public WindowManager.LayoutParams getParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        return lp;
    }

    public void show() {
        if (mShowing) {
            return;
        }
        final WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);

        wm.addView(this, getParams());
        mShowing = true;
    }

    public void hide() {
        if (!mShowing) {
            return;
        }
        final WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        wm.removeView(this);
        mShowing = false;
    }
}
