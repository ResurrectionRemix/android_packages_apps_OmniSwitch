package org.omnirom.omniswitch;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.util.Log;

public class RecentsGestureView extends LinearLayout{

    private Context mContext;
    private ImageView mDragButton;
    long mDowntime;
    int mTimeOut, mLocation, ribbonNumber;
    private int mButtonWeight = 50;
    private int mButtonHeight = 50;
    private int mGestureHeight = 30;
    private int mDragButtonOpacity = 0;

    private int mTriggerThreshhold = 20;
    private float[] mDownPoint = new float[2];
    private boolean mRibbonSwipeStarted = false;
    private boolean mRecentsStarted;
    private int mScreenWidth, mScreenHeight;

    final static String TAG = "RecentsGestureView";

    public RecentsGestureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mDragButton = new ImageView(mContext);
        mGestureHeight = 20;
        Point size = new Point();
        mGestureHeight = getResources().getDimensionPixelSize(R.dimen.drag_handle_height);
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(size);
        mScreenHeight = size.y;
        mScreenWidth = size.x;
        mDragButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                case  MotionEvent.ACTION_DOWN :
                    if (!mRibbonSwipeStarted) {
                        mDownPoint[0] = event.getX();
                        mDownPoint[1] = event.getY();
                        mRibbonSwipeStarted = true;
                    	Log.d(TAG, "start " + mDownPoint[0] + " " + mDownPoint[1]);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL :
                    mRibbonSwipeStarted = false;
                    break;
                case MotionEvent.ACTION_MOVE :
                	if (mRibbonSwipeStarted) {
                    	final int historySize = event.getHistorySize();
                        for (int k = 0; k < historySize + 1; k++) {
                            float x = k < historySize ? event.getHistoricalX(k) : event.getX();
                            float y = k < historySize ? event.getHistoricalY(k) : event.getY();
                            float distance = 0f;
                            //distance = mDownPoint[1] - y;
                            distance = mDownPoint[0] - x;

                            if (distance > mTriggerThreshhold && !mRecentsStarted) {
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
        WindowManager.LayoutParams lp  = new WindowManager.LayoutParams(
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
        float dragSize = 0;
        float dragHeight = (mGestureHeight * (mButtonHeight * 0.01f));
        removeAllViews();
        
        dragSize = ((mScreenHeight) * (mButtonWeight*0.05f)) / getResources().getDisplayMetrics().density;
        dragParams = new LinearLayout.LayoutParams((int) dragHeight, (int) dragSize);
        setOrientation(VERTICAL);

        mDragButton.setScaleType(ImageView.ScaleType.FIT_XY);
        mDragButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.drag_button_land));
        addView(mDragButton);
        invalidate();
    }
}
