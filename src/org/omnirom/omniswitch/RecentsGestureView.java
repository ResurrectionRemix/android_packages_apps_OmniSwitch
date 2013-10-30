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
import android.graphics.PixelFormat;
import android.graphics.Point;
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
	private int mScreenWidth;
	private int mScreenHeight;
	private int mSize = 1; // 0=small 1=normal 2=large
	private int mDragButtonOpacity = 255;
	private boolean mHorizontal = true;

	public RecentsGestureView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mDragButton = new ImageView(mContext);
		Point size = new Point();
		WindowManager wm = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getSize(size);
		mScreenHeight = size.y;
		mScreenWidth = size.x;
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
							float distance = mHorizontal ? distanceX : distanceY;
							if (distance > mTriggerThreshhold
									&& !mRecentsStarted) {
								Log.d(TAG, "ACTION_SHOW_RECENTS");

								Intent showRibbon = new Intent(
										RecentsService.RecentsReceiver.ACTION_SHOW_RECENTS);
								mContext.sendBroadcast(showRibbon);
								mRecentsStarted = true;
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

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}

	private int getGravity() {
		int gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
		return gravity;
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
		lp.gravity = getGravity();
		return lp;
	}

	private void updateLayout() {
		LinearLayout.LayoutParams dragParams;
		int dragHeight = 20;
		int dragWidth = 80;
		removeAllViews();

		if (mSize == 0){
			 dragHeight = 20;
			 dragWidth = 50;
		} else if (mSize == 2){
			 dragHeight = 20;
			 dragWidth = 110;			
		}
		float density = getResources().getDisplayMetrics().density;
		dragParams = new LinearLayout.LayoutParams((int)(dragHeight * density + 0.5f)
				, (int)(dragWidth * density + 0.5f));
		setOrientation(VERTICAL);

		mDragButton.setScaleType(ImageView.ScaleType.FIT_XY);
		mDragButton.setImageDrawable(mContext.getResources().getDrawable(
				R.drawable.drag_button_land));
        mDragButton.setImageAlpha(mDragButtonOpacity);

		addView(mDragButton, dragParams);
		invalidate();
	}

	public void updatePrefs(SharedPreferences prefs, String key){
		String size = prefs.getString("drag_handle_size", "1");
		mSize = Integer.valueOf(size);
		String opacity = prefs.getString("drag_handle_opacity", "255");
		mDragButtonOpacity = Integer.valueOf(opacity);
		updateLayout();
	}
	
	public void show(){
		final WindowManager wm = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		wm.addView(this, getGesturePanelLayoutParams());		
	}
	
	public void hide(){
		final WindowManager wm = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		wm.removeView(this);
	}
}
