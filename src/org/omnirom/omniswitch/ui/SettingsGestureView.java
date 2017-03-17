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
package org.omnirom.omniswitch.ui;

import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.colorpicker.ColorPickerDialog;
import org.omnirom.omniswitch.SettingsActivity;
import org.omnirom.omniswitch.SwitchConfiguration;
import org.omnirom.omniswitch.SwitchService;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class SettingsGestureView implements DialogInterface.OnDismissListener {
    private WindowManager mWindowManager;
    private ImageView mDragButton;
    private ImageView mDragButtonStart;
    private ImageView mDragButtonEnd;
    private Button mOkButton;
    private Button mCancelButton;
    private Button mLocationButton;
    private Button mResetButton;
    private LinearLayout mView;
    private LinearLayout mDragHandleViewLeft;
    private LinearLayout mDragHandleViewRight;
    private Context mContext;
    private int mLocation = 0; // 0 = right 1 = left
    private boolean mShowing;
    private float mDensity;
    private int mStartY;
    private int mStartYRelative;
    private int mHandleHeight;
    private int mEndY;
    private int mColor;
    private Drawable mDragHandle;
    private Drawable mDragHandleStart;
    private Drawable mDragHandleEnd;
    private SharedPreferences mPrefs;
    private float mDownY;
    private float mDeltaY;
    private int mSlop;
    private int mDragHandleMinHeight;
    private int mDragHandleLimiterHeight;
    private SwitchConfiguration mConfiguration;
    private SeekBar mDragHandleWidthBar;
    private int mDragHandleWidth;
    private ImageView mDragHandleColorView;
    private View mDragHandleColorContainer;
    private Dialog mDialog;

    public SettingsGestureView(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mDensity = mContext.getResources().getDisplayMetrics().density;
        Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);
        ViewConfiguration vc = ViewConfiguration.get(mContext);
        mSlop = vc.getScaledTouchSlop();
        mConfiguration = SwitchConfiguration.getInstance(mContext);

        mDragHandleLimiterHeight = Math.round(20 * mDensity);
        mDragHandleMinHeight = Math.round(60 * mDensity);
        mDragHandleWidth = mConfiguration.mDefaultDragHandleWidth;

        mDragHandle = mContext.getResources().getDrawable(
                R.drawable.drag_handle);
        mDragHandleStart = mContext.getResources().getDrawable(
                R.drawable.drag_handle_marker);
        mDragHandleEnd = mContext.getResources().getDrawable(
                R.drawable.drag_handle_marker);

        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mView = (LinearLayout) inflater.inflate(R.layout.settings_gesture_view, null, false);

        mDragHandleViewLeft = (LinearLayout)mView.findViewById(R.id.drag_handle_view_left);
        mDragHandleViewRight = (LinearLayout)mView.findViewById(R.id.drag_handle_view_right);

        mOkButton = (Button) mView.findViewById(R.id.ok_button);
        mCancelButton = (Button) mView.findViewById(R.id.cancel_button);
        mLocationButton = (Button) mView.findViewById(R.id.location_button);
        mResetButton = (Button) mView.findViewById(R.id.reset_button);

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
                        if(((mEndY + deltaY) < getLowerHandleLimit())
                                && (mStartY + deltaY > getUpperHandleLimit())){
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
                                && (mStartY + deltaY - mDragHandleLimiterHeight > 0)){
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
                                && (mEndY + deltaY + mDragHandleLimiterHeight < getLowerHandleLimit())){
                            mDeltaY = deltaY;
                            mDragButtonEnd.setTranslationY(mDeltaY);
                        }
                    }
                    break;
                }
                return true;
            }
        });

        mDragHandleWidthBar = (SeekBar) mView.findViewById(R.id.drag_handle_width);
        double min = mConfiguration.mDefaultDragHandleWidth * 0.5f;
        double max = mConfiguration.mDefaultDragHandleWidth * 2.0f;
        double value = mConfiguration.mDragHandleWidth;
        double progressValue = scaleValue(value, min, max, 1f, 100f);
        mDragHandleWidthBar.setProgress((int) progressValue);
        mDragHandleWidthBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                double progressValue = seekBar.getProgress();
                // 20 = mConfiguration.mDefaultDragHandleWidth
                // max = 2,0 * mConfiguration.mDefaultDragHandleWidth
                // min = mConfiguration.mDefaultDragHandleWidth / 2
                // 1-100 -> 0.5-2.0 -> 10-40
                double scaleFactor= scaleValue(progressValue, 1f, 100f, 0.5f, 2.0f);
                mDragHandleWidth = (int) (mConfiguration.mDefaultDragHandleWidth * scaleFactor);
                updateDragHandleLayoutParams();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
            }
        });

        mOkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mDialog != null) {
                    mDialog.dismiss();
                }
                Editor edit = mPrefs.edit();
                edit.putInt(SettingsActivity.PREF_DRAG_HANDLE_LOCATION, mLocation);
                int relHeight = (int)(mStartY / (mConfiguration.getCurrentDisplayHeight() /100));
                edit.putInt(SettingsActivity.PREF_HANDLE_POS_START_RELATIVE, relHeight);
                edit.putInt(SettingsActivity.PREF_HANDLE_HEIGHT, mEndY - mStartY);
                edit.putInt(SettingsActivity.PREF_HANDLE_WIDTH, mDragHandleWidth);
                edit.putInt(SettingsActivity.PREF_DRAG_HANDLE_COLOR_NEW, mColor);
                edit.commit();
                hide();
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mDialog != null) {
                    mDialog.dismiss();
                }
                hide();
            }
        });

        mLocationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mLocation == 1){
                    mLocation = 0;
                    mLocationButton.setText(mContext.getResources().getString(R.string.location_left));
                } else {
                    mLocation = 1;
                    mLocationButton.setText(mContext.getResources().getString(R.string.location_right));
                }
                updateLayout();
            }
        });

        mResetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                resetPosition();
            }
        });

        mView.setFocusableInTouchMode(true);
        mView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction()==KeyEvent.ACTION_DOWN){
                    hide();
                    return true;
                }
                return false;
            }
        });

        mDragHandleColorView = (ImageView) mView.findViewById(R.id.drag_handle_color);
        mDragHandleColorContainer = mView.findViewById(R.id.drag_handle_color_view);
        mDragHandleColorContainer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mDialog != null && mDialog.isShowing()) return;
                mDialog = getColorDialog();
                mDialog.setOnDismissListener(SettingsGestureView.this);
                mDialog.show();
            }
        });
    }

    public WindowManager.LayoutParams getGesturePanelLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.CENTER;
        lp.dimAmount = 0.6f;
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                mDragHandleWidth,
                (int) (mEndY - mStartY));
        params.gravity = mLocation == 1 ? Gravity.LEFT : Gravity.RIGHT;
        mDragButton.setLayoutParams(params);

        params = new LinearLayout.LayoutParams(
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
        
        mStartYRelative = (int)(mStartY / (mConfiguration.getCurrentDisplayHeight() /100));
        mHandleHeight = mEndY - mStartY;
    }

    private void updateDragHandleImage() {
        Drawable d = mDragHandle;
        Drawable d1 = mDragHandleStart;
        Drawable d2 = mDragHandleEnd;

        mDragButton.setScaleType(ImageView.ScaleType.FIT_XY);
        mDragButton.setImageDrawable(BitmapUtils.colorize(mContext.getResources(), mColor, d));
        mDragButton.getDrawable().setColorFilter(mColor, Mode.SRC_ATOP);
        mDragButton.setRotation(mLocation == 1 ? 180 : 0);
        
        mDragButtonStart.setScaleType(ImageView.ScaleType.FIT_XY);
        mDragButtonStart.setImageDrawable(d1);
        mDragButtonStart.setRotation(mLocation == 1 ? 180 : 0);

        mDragButtonEnd.setScaleType(ImageView.ScaleType.FIT_XY);
        mDragButtonEnd.setImageDrawable(d2);
        mDragButtonEnd.setRotation(mLocation == 1 ? 180 : 0);
    }

    private void updateFromPrefs() {
        mStartY = mConfiguration.getCurrentOffsetStart();
        mEndY = mConfiguration.getCurrentOffsetEnd();
        mDragHandleWidth = mConfiguration.mDragHandleWidth;

        mLocation = mConfiguration.mLocation;
        if (mLocation == 1){
            mLocationButton.setText(mContext.getResources().getString(R.string.location_right));
        } else {
            mLocationButton.setText(mContext.getResources().getString(R.string.location_left));
        }
        mColor = mConfiguration.mDragHandleColor;
        updateColorRect();
    }

    public void show() {
        if (mShowing) {
            return;
        }
        updateFromPrefs();
        updateLayout();

        mWindowManager.addView(mView, getGesturePanelLayoutParams());
        mShowing = true;

        Intent intent = new Intent(
                SwitchService.RecentsReceiver.ACTION_HANDLE_HIDE);
        mContext.sendBroadcast(intent);
    }

    public void hide() {
        if (!mShowing) {
            return;
        }

        mWindowManager.removeView(mView);
        mShowing = false;

        Intent intent = new Intent(
                SwitchService.RecentsReceiver.ACTION_HANDLE_SHOW);
        mContext.sendBroadcast(intent);
    }

    public void resetPosition() {
        mStartY = mConfiguration.getDefaultOffsetStart();
        mEndY = mConfiguration.getDefaultOffsetEnd();
        mDragHandleWidth = mConfiguration.mDefaultDragHandleWidth;
        mDragHandleWidthBar.setProgress(40);
        mColor = mConfiguration.mDefaultColor;
        updateColorRect();
        updateLayout();
    }

    public boolean isShowing() {
        return mShowing;
    }

    public void handleRotation(){
        mStartY = mConfiguration.getCustomOffsetStart(mStartYRelative);
        mEndY = mConfiguration.getCustomOffsetEnd(mStartYRelative, mHandleHeight);
        updateLayout();
    }

    private int getLowerHandleLimit() {
        return mConfiguration.getCurrentDisplayHeight() - mConfiguration.mLevelHeight;
    }

    private int getUpperHandleLimit() {
        return mConfiguration.mLevelHeight / 2;
    }

    private double scaleValue(double value, double oldMin, double oldMax, double newMin, double newMax) {
        return ( (value - oldMin) / (oldMax - oldMin) ) * (newMax - newMin) + newMin;
    }

    private static ShapeDrawable createRectShape(int width, int height, int color) {
        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.setIntrinsicHeight(height);
        shape.setIntrinsicWidth(width);
        shape.getPaint().setColor(color);
        return shape;
    }

    private void updateColorRect() {
        final int width = (int) mContext.getResources().getDimension(R.dimen.color_button_width);
        final int height = (int) mContext.getResources().getDimension(R.dimen.color_button_height);
        mDragHandleColorView.setImageDrawable(createRectShape(width, height, mColor));
    }

    private Dialog getColorDialog() {
        final ColorPickerDialog d = new ColorPickerDialog(mContext, mColor, true);
        d.setButton(AlertDialog.BUTTON_POSITIVE,
                mContext.getResources().getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mColor = d.getColor();
                        updateColorRect();
                        updateDragHandleImage();
                    }
                });
        d.setButton(AlertDialog.BUTTON_NEUTRAL,
                mContext.getResources().getString(R.string.reset),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mColor = mConfiguration.mDefaultColor;
                        updateColorRect();
                        updateDragHandleImage();
                        d.dismiss();
                    }
                });
        d.setButton(AlertDialog.BUTTON_NEGATIVE,
                mContext.getResources().getString(R.string.cancel),
                (DialogInterface.OnClickListener) null);
        return d;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mDialog = null;
    }
}
