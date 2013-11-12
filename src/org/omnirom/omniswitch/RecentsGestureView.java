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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class RecentsGestureView extends LinearLayout {
	private final static String TAG = "RecentsGestureView";

	private Context mContext;
	private ImageView mDragButton;

	private int mTriggerThreshhold = 20;
	private float[] mDownPoint = new float[2];
	private boolean mRibbonSwipeStarted = false;
	private boolean mRecentsStarted;
	private int mSize = 1; // 0=small 1=normal 2=large
	private int mDragButtonOpacity = 255;
	private int mLocation = 0; // 0 = right 1 = left 
	private boolean mShowing;
	private float mDensity;
	private float mPosY = -1.0f;

	private LinearLayout.LayoutParams mDragParams;

	public RecentsGestureView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mDensity = getResources().getDisplayMetrics().density;
		
		mDragButton = new ImageView(mContext);
		mDragButton.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch (action) {
				case MotionEvent.ACTION_DOWN:
					if (!mRibbonSwipeStarted) {
						mDownPoint[0] = event.getX();
						mDownPoint[1] = event.getY();
						mRibbonSwipeStarted = true;
						Log.d(TAG, "start " + mDownPoint[0] + " "
								+ mDownPoint[1]);
					}
					break;
				case MotionEvent.ACTION_CANCEL:
					mRibbonSwipeStarted = false;
					mRecentsStarted = false;
					break;
				case MotionEvent.ACTION_MOVE:
					if (mRibbonSwipeStarted) {
						final int historySize = event.getHistorySize();
						for (int k = 0; k < historySize + 1; k++) {
							float x = k < historySize ? event.getHistoricalX(k)
									: event.getX();
							float y = k < historySize ? event.getHistoricalY(k)
									: event.getY();
							float distanceY = mDownPoint[1] - y;
							float distanceX = mDownPoint[0] - x;
							float distance = Math.abs(distanceX);
							if (distance > mTriggerThreshhold
									&& !mRecentsStarted) {
								Log.d(TAG, "ACTION_SHOW_RECENTS");

								Intent showRibbon = new Intent(
										RecentsService.RecentsReceiver.ACTION_SHOW_RECENTS);
								mContext.sendBroadcast(showRibbon);
								mRecentsStarted = true;
								mRibbonSwipeStarted = false;
								break;
							}
						}
					}
					break;
				case MotionEvent.ACTION_UP:
					mRibbonSwipeStarted = false;
					mRecentsStarted = false;
					break;
				}
				return true;
			}
		});
		updateLayout();
	}

	private int getDefaultGravity() {
		if (mLocation == 0){
			return Gravity.RIGHT | Gravity.CENTER_VERTICAL;
		}
		if (mLocation == 1){
			return Gravity.LEFT | Gravity.CENTER_VERTICAL;
		}

		return Gravity.RIGHT | Gravity.CENTER_VERTICAL;
	}

	private int getAbsoluteGravity() {
		if (mLocation == 0){
			return Gravity.RIGHT | Gravity.TOP;
		}
		if (mLocation == 1){
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
		
		if (mPosY != -1.0f){
			lp.gravity = getAbsoluteGravity();
			lp.y = (int)(mPosY - mDragParams.height/2);
		} else {
			lp.gravity = getDefaultGravity();
		}
		return lp;
	}
	
	private void updateLayout() {
		int dragHeight = 20;
		int dragWidth = 80;
		
		hide();
		removeAllViews();

		if (mSize == 0){
			 dragHeight = 20;
			 dragWidth = 50;
		} else if (mSize == 2){
			 dragHeight = 20;
			 dragWidth = 110;			
		}
		mDragParams = new LinearLayout.LayoutParams((int)(dragHeight * mDensity + 0.5f)
				, (int)(dragWidth * mDensity + 0.5f));
		setOrientation(VERTICAL);

		Drawable d = mContext.getResources().getDrawable(
				R.drawable.drag_button_land);
        if (mLocation == 1){
        	d = rotate(mContext.getResources(), d, 180);
        }

		mDragButton.setScaleType(ImageView.ScaleType.FIT_XY);
		mDragButton.setImageDrawable(d);
        mDragButton.setImageAlpha(mDragButtonOpacity);

		addView(mDragButton, mDragParams);
		invalidate();
		show();
	}

	private Drawable rotate(Resources resources, Drawable image, int deg) {
	    Bitmap b = ((BitmapDrawable)image).getBitmap();
		Bitmap bmResult = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas tempCanvas = new Canvas(bmResult); 
		tempCanvas.rotate(deg, b.getWidth()/2, b.getHeight()/2);
		tempCanvas.drawBitmap(b, 0, 0, null);
	    return new BitmapDrawable(resources, bmResult);
	}
	
	public void updatePrefs(SharedPreferences prefs, String key){
		Log.d(TAG, "updatePrefs");
		mPosY = prefs.getFloat("handle_pos_y", -1.0f);
		String size = prefs.getString(SettingsActivity.PREF_DRAG_HANDLE_SIZE, "1");
		mSize = Integer.valueOf(size);
		int opacity = prefs.getInt(SettingsActivity.PREF_DRAG_HANDLE_OPACITY, 60);
		mDragButtonOpacity = (int)(255 * ((float)opacity/100.0f));
		String location = prefs.getString(SettingsActivity.PREF_DRAG_HANDLE_LOCATION, "0");
		mLocation = Integer.valueOf(location);

		updateLayout();
	}

	public void show(){
		if (mShowing){
			return;
		}
		final WindowManager wm = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		
		wm.addView(this, getGesturePanelLayoutParams());
		mShowing = true;
	}
	
	public void hide(){
		if (!mShowing){
			return;
		}
		final WindowManager wm = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		wm.removeView(this);
		mShowing = false;
	}
	
	public int getLocation(){
		return mLocation;
	}
}
