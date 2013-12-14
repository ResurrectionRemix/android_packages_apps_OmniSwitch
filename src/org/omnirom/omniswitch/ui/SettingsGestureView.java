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
package org.omnirom.omniswitch.ui;

import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.SettingsActivity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class SettingsGestureView {
    private final static String TAG = "SwitchGestureView";

    private WindowManager mWindowManager;
    private ImageView mDragButton;
    private Button mOkButton;
    private Button mCancelButton;
    private LinearLayout mView;
    private LinearLayout mDragHandleViewLeft;
    private LinearLayout mDragHandleViewRight;
    private Context mContext;

    private int mSize = 1; // 0=small 1=normal 2=large
    private int mLocation = 0; // 0 = right 1 = left
    private boolean mShowing;
    private float mDensity;
    private float mPosY = -1.0f;
    private float mStartY = -1.0f;
    private float mEndY = -1.0f;
    private int mColor;
    private Drawable mDragHandle;
    private Drawable mDragHandleOverlay;
    private int mScreenHeight;
    private SharedPreferences mPrefs;

    public SettingsGestureView(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mDensity = mContext.getResources().getDisplayMetrics().density;
        Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);
        mScreenHeight = size.y;

        mDragHandle = mContext.getResources().getDrawable(
                R.drawable.drag_handle);
        mDragHandleOverlay = mContext.getResources().getDrawable(
                R.drawable.drag_handle_overlay);

        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mView = (LinearLayout) inflater.inflate(R.layout.settings_gesture_view, null, false);

        mDragHandleViewLeft = (LinearLayout)mView.findViewById(R.id.drag_handle_view_left);
        mDragHandleViewRight = (LinearLayout)mView.findViewById(R.id.drag_handle_view_right);

        mOkButton = (Button) mView.findViewById(R.id.ok_button);
        mCancelButton = (Button) mView.findViewById(R.id.cancel_button);
        
        mDragButton = new ImageView(mContext);
        updateLayout();

        mOkButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();

                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mPrefs.edit().putFloat("handle_pos_y", mPosY).commit();
                    hide();
                }
                return true;
            }
        });

        mCancelButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();

                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    hide();
                }
                return true;
            }
        });
        mDragHandleViewLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(mLocation != 1){
                    return false;
                }
                int action = event.getAction();
                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    float y = event.getY();
                    Log.d(TAG, "y " + y);
                    mPosY = y;
                    updateLayout();
                    break;
                }
                return true;
            }
        });
        mDragHandleViewRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(mLocation == 1){
                    return false;
                }
                int action = event.getAction();
                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    float y = event.getY();
                    Log.d(TAG, "y " + y);
                    mPosY = y;
                    updateLayout();
                    break;
                }
                return true;
            }
        });

    }

    public WindowManager.LayoutParams getGesturePanelLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.CENTER;
        lp.dimAmount = 0.8f;
        return lp;
    }

    private void updateLayout() {
        mDragHandleViewLeft.removeAllViews();
        mDragHandleViewRight.removeAllViews();
        
        updateDragHandleImage(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) (20 * mDensity + 0.5f), (int) (mEndY - mStartY));
        params.topMargin = (int) (mPosY - (mEndY - mStartY) / 2.0f);
        params.gravity = mLocation == 1 ? Gravity.LEFT : Gravity.RIGHT;
        if(mLocation == 1){
            mDragHandleViewLeft.addView(mDragButton, params);
            mDragHandleViewLeft.invalidate();
        } else {
            mDragHandleViewRight.addView(mDragButton, params);
            mDragHandleViewRight.invalidate();
        }
    }

    private void updateDragHandleImage(boolean shown) {
        Drawable d = shown ? mDragHandle : mDragHandleOverlay;

        if (mLocation == 1) {
            d = rotate(mContext.getResources(), d, 180);
        }

        mDragButton.setScaleType(ImageView.ScaleType.FIT_XY);
        mDragButton.setImageDrawable(d);
        if (shown) {
            mDragButton.getDrawable().setColorFilter(mColor, Mode.SRC_ATOP);
        }
    }

    private Drawable rotate(Resources resources, Drawable image, int deg) {
        Bitmap b = ((BitmapDrawable) image).getBitmap();
        Bitmap bmResult = Bitmap.createBitmap(b.getWidth(), b.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(bmResult);
        tempCanvas.rotate(deg, b.getWidth() / 2, b.getHeight() / 2);
        tempCanvas.drawBitmap(b, 0, 0, null);
        return new BitmapDrawable(resources, bmResult);
    }

    private void updateFromPrefs() {
        mPosY = mPrefs.getFloat("handle_pos_y", (float) (mScreenHeight / 2.0));
        String size = mPrefs.getString(SettingsActivity.PREF_DRAG_HANDLE_SIZE,
                "1");
        mSize = Integer.valueOf(size);
        if (mSize == 0) {
            mStartY = mPosY - (float) (20 * mDensity + 0.5);
            mEndY = mPosY + (float) (20 * mDensity + 0.5);
        } else if (mSize == 1) {
            mStartY = mPosY - (float) (50 * mDensity + 0.5);
            mEndY = mPosY + (float) (50 * mDensity + 0.5);
        } else if (mSize == 2) {
            mStartY = mPosY - (float) (80 * mDensity + 0.5);
            mEndY = mPosY + (float) (80 * mDensity + 0.5);
        }

        String location = mPrefs.getString(
                SettingsActivity.PREF_DRAG_HANDLE_LOCATION, "0");
        mLocation = Integer.valueOf(location);
        mColor = mPrefs.getInt(SettingsActivity.PREF_DRAG_HANDLE_COLOR,
                mContext.getResources().getColor(R.color.holo_blue_light));
    }

    public void show() {
        if (mShowing) {
            return;
        }
        updateFromPrefs();
        updateLayout();

        mWindowManager.addView(mView, getGesturePanelLayoutParams());
        mShowing = true;
    }

    public void hide() {
        if (!mShowing) {
            return;
        }

        mWindowManager.removeView(mView);
        mShowing = false;
    }
}
