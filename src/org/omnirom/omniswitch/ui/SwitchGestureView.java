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
import org.omnirom.omniswitch.SwitchService;
import org.omnirom.omniswitch.showcase.ShowcaseView;
import org.omnirom.omniswitch.showcase.ShowcaseView.OnShowcaseEventListener;

import android.content.Context;
import android.content.Intent;
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
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class SwitchGestureView implements OnShowcaseEventListener {
    private final static String TAG = "SwitchGestureView";
    private final static String KEY_SHOWCASE_HANDLE = "showcase_handle_done";

    private Context mContext;
    private WindowManager mWindowManager;
    private ImageView mDragButton;
    private LinearLayout mView;
    private int mTriggerThreshholdX = 20;
    private int mTriggerThreshholdY = 20;

    private float[] mDownPoint = new float[2];
    private boolean mSwipeStarted;
    private boolean mShowStarted;
    private int mLocation = 0; // 0 = right 1 = left
    private boolean mShowing;
    private float mDensity;
    private int mStartY;
    private int mEndY;
    private int mColor;
    private Drawable mDragHandle;
    private Drawable mDragHandleOverlay;
    private int mScreenHeight;
    private boolean mShowDragHandle = true;
    private ShowcaseView mShowcaseView;
    private SharedPreferences mPrefs;
    private boolean mShowcaseDone;

    public SwitchGestureView(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
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

        mView = (LinearLayout) inflater.inflate(R.layout.gesture_view, null, false);

        mDragButton= new ImageView(mContext);
        mDragButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean defaultResult = v.onTouchEvent(event);

                int action = event.getAction();
                Log.d(TAG, "button onTouch");
                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (!mSwipeStarted) {
                        updateDragHandleImage(true);
                        mView.invalidate();

                        mDownPoint[0] = event.getX();
                        mDownPoint[1] = event.getY();
                        mSwipeStarted = true;
                        Log.d(TAG, "button down " + mDownPoint[0] + " "
                                + mDownPoint[1]);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    mSwipeStarted = false;
                    mShowStarted = false;
                    updateDragHandleImage(false);
                    mView.invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mSwipeStarted) {
                        final int historySize = event.getHistorySize();
                        for (int k = 0; k < historySize + 1; k++) {
                            float x = k < historySize ? event.getHistoricalX(k)
                                    : event.getX();
                            float y = k < historySize ? event.getHistoricalY(k)
                                    : event.getY();
                            float distanceY = Math.abs(mDownPoint[1] - y);
                            float distanceX = Math.abs(mDownPoint[0] - x);
                            Log.d(TAG, ""+distanceX + " " + distanceY);
                            if (distanceX > mTriggerThreshholdX
                                    //&& distanceY < mTriggerThreshholdY
                                    && !mShowStarted) {
                                Intent showIntent = new Intent(
                                        SwitchService.RecentsReceiver.ACTION_SHOW_OVERLAY);
                                mContext.sendBroadcast(showIntent);
                                mShowStarted = true;
                                mSwipeStarted = false;
                                break;
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (!mShowStarted){
                        mSwipeStarted = false;
                        updateDragHandleImage(false);
                        mView.invalidate();
                    }
                    break;
                default:
                    return defaultResult;
                }
                return true;
            }
        });
        
        mDragButton.setOnLongClickListener(new OnLongClickListener(){
            @Override
            public boolean onLongClick(View arg0) {
                if (!mShowStarted){
                    Log.d(TAG, "button long down");
                    Intent showIntent = new Intent(
                            SwitchService.RecentsReceiver.ACTION_SHOW_OVERLAY);
                    mContext.sendBroadcast(showIntent);
                }
                return true;
            }});
        updateLayout();
    }

    private int getAbsoluteGravity() {
        if (mLocation == 0) {
            return Gravity.RIGHT | Gravity.TOP;
        }
        if (mLocation == 1) {
            return Gravity.LEFT | Gravity.TOP;
        }

        return Gravity.RIGHT | Gravity.TOP;
    }

    public WindowManager.LayoutParams getGesturePanelLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        lp.gravity = getAbsoluteGravity();
        lp.y = mStartY;
        lp.height = (int)(mEndY - mStartY);
        lp.width = (int) (20 * mDensity + 0.5f);
        
        return lp;
    }

    private void updateLayout() {
        hide();
        mView.removeView(mDragButton);
        updateDragHandleImage(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        mView.addView(mDragButton, params);
        mView.invalidate();
        show();
    }
    
    private void updateDragHandleImage(boolean shown){
        Drawable d = shown ? mDragHandle : mDragHandleOverlay;

        if (mShowDragHandle){
            d = mDragHandle;
        }
        if (mLocation == 1) {
            d = rotate(mContext.getResources(), d, 180);
        }

        mDragButton.setScaleType(ImageView.ScaleType.FIT_XY);
        mDragButton.setImageDrawable(d);
        if (shown || mShowDragHandle){
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

    public void updatePrefs(SharedPreferences prefs, String key) {
        Log.d(TAG, "updatePrefs");
        int defaultHeight = (int) (100 * mDensity + 0.5);
        mStartY = prefs.getInt("handle_pos_start", mScreenHeight / 2 - defaultHeight / 2);
        mEndY = prefs.getInt("handle_pos_end", mScreenHeight / 2 + defaultHeight / 2);

        String location = prefs.getString(
                SettingsActivity.PREF_DRAG_HANDLE_LOCATION, "0");
        mLocation = Integer.valueOf(location);
        mColor = prefs
                .getInt(SettingsActivity.PREF_DRAG_HANDLE_COLOR, mContext.getResources().getColor(R.color.holo_blue_light));
        mShowDragHandle = prefs.getBoolean(SettingsActivity.PREF_SHOW_DRAG_HANDLE, true);
        updateLayout();
    }

    public void show() {
        if (mShowing) {
            return;
        }

        mWindowManager.addView(mView, getGesturePanelLayoutParams());
        if(!mShowcaseDone){
            mView.postDelayed(new Runnable(){
                @Override
                public void run() {
                    startShowcaseDragHandle();
                }}, 200);
        }
        mShowing = true;
    }

    public void hide() {
        if (!mShowing) {
            return;
        }

        mWindowManager.removeView(mView);
        mShowing = false;
    }

    public int getLocation() {
        return mLocation;
    }
    
    public void overlayShown() {
        mShowStarted = false;
        mSwipeStarted = false;
        updateDragHandleImage(false);
        mView.invalidate();
    }

    private boolean startShowcaseDragHandle() {
        if (!mPrefs.getBoolean(KEY_SHOWCASE_HANDLE, false)) {
            mPrefs.edit().putBoolean(KEY_SHOWCASE_HANDLE, true).commit();
            ShowcaseView.ConfigOptions co = new ShowcaseView.ConfigOptions();
            co.hideOnClickOutside = true;

            Point size = new Point();
            mWindowManager.getDefaultDisplay().getSize(size);

            mShowcaseView = ShowcaseView.insertShowcaseView(mLocation == 1 ? 0 : size.x, mStartY + (mEndY - mStartY)/2, mWindowManager, mContext,
                    R.string.sc_drag_handle_title, R.string.sc_drag_handle_body, co);

            mShowcaseView.animateGesture(size.x / 2, size.y * 2.0f / 3.0f,
                    size.x / 2, size.y / 2.0f);
            mShowcaseView.setOnShowcaseEventListener(this);
            mShowcaseDone = true;
            return true;
        }
        return false;
    }

    @Override
    public void onShowcaseViewHide(ShowcaseView showcaseView) {
    }

    @Override
    public void onShowcaseViewShow(ShowcaseView showcaseView) {
    }
}
