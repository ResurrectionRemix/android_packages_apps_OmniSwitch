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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class SettingsGestureView {
    private final static String TAG = "SwitchGestureView";

    private WindowManager mWindowManager;
    private ImageView mDragButton;
    private ImageView mDragButtonStart;
    private ImageView mDragButtonEnd;

    private Button mOkButton;
    private Button mCancelButton;
    private LinearLayout mView;
    private LinearLayout mDragHandleViewLeft;
    private LinearLayout mDragHandleViewRight;
    private Context mContext;

    private int mLocation = 0; // 0 = right 1 = left
    private boolean mShowing;
    private float mDensity;
    private int mStartY;
    private int mEndY;
    private int mColor;
    private Drawable mDragHandle;
    private Drawable mDragHandleStart;
    private Drawable mDragHandleEnd;
    private int mScreenHeight;
    private SharedPreferences mPrefs;
    private float mDownY;
    private float mDeltaY;
    private LinearLayout.LayoutParams mParams;
    private int mSlop;
    private int mDragHandleMinHeight;
    private int mDragHandleLimiterHeight;

    public SettingsGestureView(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mDensity = mContext.getResources().getDisplayMetrics().density;
        Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);
        mScreenHeight = size.y;
        ViewConfiguration vc = ViewConfiguration.get(mContext);
        mSlop = vc.getScaledTouchSlop();

        mDragHandleLimiterHeight = (int) (40 * mDensity + 0.5);
        mDragHandleMinHeight = (int) (60 * mDensity + 0.5);

        mDragHandle = mContext.getResources().getDrawable(
                R.drawable.drag_handle);
        mDragHandleStart = mContext.getResources().getDrawable(
                R.drawable.drag_handle_start);
        mDragHandleEnd = mContext.getResources().getDrawable(
                R.drawable.drag_handle_end);

        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mView = (LinearLayout) inflater.inflate(R.layout.settings_gesture_view, null, false);

        mDragHandleViewLeft = (LinearLayout)mView.findViewById(R.id.drag_handle_view_left);
        mDragHandleViewRight = (LinearLayout)mView.findViewById(R.id.drag_handle_view_right);

        mOkButton = (Button) mView.findViewById(R.id.ok_button);
        mCancelButton = (Button) mView.findViewById(R.id.cancel_button);
        
        mDragButton = new ImageView(mContext);
        mDragButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();

                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mDownY = event.getRawY();
                    mDeltaY = 0;
                    break;
                case MotionEvent.ACTION_UP:
                    if(mDeltaY != 0){
                        mStartY += mDeltaY;
                        mEndY += mDeltaY;
                        updateDragHandleLayoutParams();
                    }
                    mDragButton.setTranslationY(0);
                    mDragButtonStart.setTranslationY(0);
                    mDragButtonEnd.setTranslationY(0);
                    mDownY = 0;
                    break;
                case MotionEvent.ACTION_CANCEL:
                    mDownY = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaY = event.getRawY() - mDownY;
                    if(Math.abs(deltaY) > mSlop){
                        if(((mEndY + deltaY) < mScreenHeight)
                                && (mStartY + deltaY > 0)){
                            mDeltaY = deltaY;
                            mDragButton.setTranslationY(mDeltaY);
                            mDragButtonStart.setTranslationY(mDeltaY);
                            mDragButtonEnd.setTranslationY(mDeltaY);
                        }
                    }
                    break;
                }
                return true;
            }
        });

        mDragButtonStart = new ImageView(mContext);
        mDragButtonStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();

                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mDownY = event.getRawY();
                    mDeltaY = 0;
                    break;
                case MotionEvent.ACTION_UP:
                    if(mDeltaY != 0){
                        mStartY += mDeltaY;
                        updateDragHandleLayoutParams();
                    }
                    mDragButtonStart.setTranslationY(0);
                    mDownY = 0;
                    break;
                case MotionEvent.ACTION_CANCEL:
                    mDownY = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaY = event.getRawY() - mDownY;
                    if(Math.abs(deltaY) > mSlop){
                        if(((mStartY + deltaY) < (mEndY - mDragHandleMinHeight))
                                && (mStartY + deltaY > 0)){
                            mDeltaY = deltaY;
                            mDragButtonStart.setTranslationY(mDeltaY);
                        }
                    }
                    break;
                }
                return true;
            }
        });
        mDragButtonEnd = new ImageView(mContext);
        mDragButtonEnd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();

                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mDownY = event.getRawY();
                    mDeltaY = 0;
                    break;
                case MotionEvent.ACTION_UP:
                    if(mDeltaY != 0){
                        mEndY += mDeltaY;
                        updateDragHandleLayoutParams();
                    }
                    mDragButtonEnd.setTranslationY(0);
                    mDownY = 0;
                    break;
                case MotionEvent.ACTION_CANCEL:
                    mDownY = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaY = event.getRawY() - mDownY;
                    if(Math.abs(deltaY) > mSlop){
                        if(((mEndY + deltaY) > (mStartY + mDragHandleMinHeight))
                                && (mEndY + deltaY < mScreenHeight)){
                            mDeltaY = deltaY;
                            mDragButtonEnd.setTranslationY(mDeltaY);
                        }
                    }
                    break;
                }
                return true;
            }
        });

        
        mOkButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();

                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mPrefs.edit().putInt("handle_pos_start", mStartY).commit();
                    mPrefs.edit().putInt("handle_pos_end", mEndY).commit();
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

        updateDragHandleImage();
        updateDragHandleLayoutParams();

        getDragHandleContainer().addView(mDragButtonStart);
        getDragHandleContainer().addView(mDragButton);
        getDragHandleContainer().addView(mDragButtonEnd);
    }

    private LinearLayout getDragHandleContainer() {
        if(mLocation == 1){
            return mDragHandleViewLeft;
        } else {
            return mDragHandleViewRight;
        }
    }
    private void updateDragHandleLayoutParams() {
        mParams = new LinearLayout.LayoutParams(
                (int) (20 * mDensity + 0.5), (int) (mEndY - mStartY));
        mParams.gravity = mLocation == 1 ? Gravity.LEFT : Gravity.RIGHT;
        mDragButton.setLayoutParams(mParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                mDragHandleLimiterHeight );
        params.topMargin = mStartY - mDragHandleLimiterHeight;
        params.gravity = mLocation == 1 ? Gravity.LEFT : Gravity.RIGHT;
        mDragButtonStart.setLayoutParams(params);

        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                mDragHandleLimiterHeight);
        params.gravity = mLocation == 1 ? Gravity.LEFT : Gravity.RIGHT;
        mDragButtonEnd.setLayoutParams(params);
    }
    
    private void updateDragHandleImage() {
        Drawable d = mDragHandle;
        Drawable d1 = mDragHandleStart;
        Drawable d2 = mDragHandleEnd;

        if (mLocation == 1) {
            d = rotate(mContext.getResources(), d, 180);
            d1 = rotate(mContext.getResources(), d1, 180);
            d2 = rotate(mContext.getResources(), d2, 180);
        }

        mDragButton.setScaleType(ImageView.ScaleType.FIT_XY);
        mDragButton.setImageDrawable(d);
        mDragButton.getDrawable().setColorFilter(mColor, Mode.SRC_ATOP);
        
        mDragButtonStart.setScaleType(ImageView.ScaleType.FIT_XY);
        mDragButtonStart.setImageDrawable(d1);

        mDragButtonEnd.setScaleType(ImageView.ScaleType.FIT_XY);
        mDragButtonEnd.setImageDrawable(d2);
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
        int defaultHeight = (int) (100 * mDensity + 0.5);
        mStartY = mPrefs.getInt("handle_pos_start", mScreenHeight / 2 - defaultHeight / 2);
        mEndY = mPrefs.getInt("handle_pos_end", mScreenHeight / 2 + defaultHeight / 2);

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
