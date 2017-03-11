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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.omnirom.omniswitch.PackageManager;
import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.RecentTasksLoader;
import org.omnirom.omniswitch.SettingsActivity;
import org.omnirom.omniswitch.SwitchConfiguration;
import org.omnirom.omniswitch.SwitchManager;
import org.omnirom.omniswitch.TaskDescription;
import org.omnirom.omniswitch.Utils;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class SwitchGestureView {
    private final static String TAG = "SwitchGestureView";
    private static final boolean DEBUG = false;

    private static final int FLIP_DURATION_DEFAULT = 200;

    private Context mContext;
    private WindowManager mWindowManager;
    private ImageView mDragButton;
    private FrameLayout mView;
    private float[] mDownPoint = new float[2];
    private float[] mInitDownPoint = new float[2];
    private boolean mShowing;
    private boolean mEnabled = true;
    private RippleDrawable mDragHandleImage;
    private Drawable mDragHandleHiddenImage; // transparent image to detect touches for auto hide trigger
    private SharedPreferences mPrefs;
    private SwitchConfiguration mConfiguration;
    private boolean mHidden = true;
    private Handler mHandler;
    private SwitchManager mRecentsManager;
    private Runnable mAutoHideRunnable = new Runnable(){
        @Override
        public void run() {
            if(!mHidden){
                updateDragHandleImage(false);
            }
        }};
    private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private Animator mToggleDragHandleAnim;
    private List<PackageTextView> mRecentList;
    private boolean mHandleRecentsUpdate;
    private Runnable mLongPressRunnable = new Runnable(){
        @Override
        public void run() {
            if (DEBUG){
                Log.d(TAG, "mLongPressRunnable");
            }
            mRecentsManager.hideHidden();
            mLongPress = true;
            mHandleRecentsUpdate = true;
            RecentTasksLoader.getInstance(mContext).loadTasksInBackground(0, true, true);
        }};
    private PackageTextView[] mCurrentItemEnv= new PackageTextView[3];

    private int mCurrentRecentItemIndex;
    private boolean mLongPress;
    private int mLevel;
    private int mLockedLevel;
    private int[] mVerticalBorders = new int[4];
    private List<PackageTextView> mFavoriteList;
    private int mCurrentFavoriteItemIndex;
    private List<PackageTextView> mActionList;
    private int mCurrentActionItemIndex;
    private int mLevelX = -1;
    private boolean mVirtualBackKey;
    private boolean mVirtualMenuKey;
    private HorizontalScrollView[] mAllLists = new HorizontalScrollView[3];
    private View mLevelBorderIndicator;
    private FrameLayout mItemView;
    private boolean mShowIndicators;
    private boolean mShowingSpeedSwitcher;
    private int mSlop;
    private boolean mFlingEnable = true;
    private float mLastX;
    private float mThumbRatio = 1.2f;
    private boolean mMoveStarted;
    private PackageTextView mLockToAppButton;

    private GestureDetector mGestureDetector;
    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }
        @Override
        public void onShowPress(MotionEvent e) {
        }
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }
        @Override
        public void onLongPress(MotionEvent e) {
        }
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distanceX = Math.abs(mInitDownPoint[0] - e2.getRawX());
            if (distanceX > mSlop) {
                if (DEBUG) {
                    Log.d(TAG, "onFling open " + velocityX);
                }
                mEnabled = false;
                mHandler.removeCallbacks(mLongPressRunnable);
                mRecentsManager.openSlideLayout(true);
            }
            return false;
        }
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    };

    public SwitchGestureView(SwitchManager manager, Context context) {
        mContext = context;
        mRecentsManager = manager;
        mWindowManager = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mConfiguration = SwitchConfiguration.getInstance(mContext);
        mHandler = new Handler();
        mFavoriteList = new ArrayList<PackageTextView>();
        mRecentList = new ArrayList<PackageTextView>();
        mActionList = new ArrayList<PackageTextView>();
        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop() / 2;

        mGestureDetector = new GestureDetector(context, mGestureListener);
        mGestureDetector.setIsLongpressEnabled(false);
        HorizontalScrollView list = new HorizontalScrollView(mContext);
        list.setHorizontalScrollBarEnabled(false);
        LinearLayout listLayout = new LinearLayout(mContext);
        listLayout.setOrientation(LinearLayout.HORIZONTAL);
        list.addView(listLayout);
        mAllLists[0] = list;

        list = new HorizontalScrollView(mContext);
        list.setHorizontalScrollBarEnabled(false);
        listLayout = new LinearLayout(mContext);
        listLayout.setOrientation(LinearLayout.HORIZONTAL);
        list.addView(listLayout);
        mAllLists[1] = list;

        list = new HorizontalScrollView(mContext);
        list.setHorizontalScrollBarEnabled(false);
        listLayout = new LinearLayout(mContext);
        listLayout.setOrientation(LinearLayout.HORIZONTAL);
        list.addView(listLayout);
        mAllLists[2] = list;

        ColorStateList rippleColor =
                ColorStateList.valueOf(mContext.getResources().getColor(android.R.color.white));
        mDragHandleImage = new RippleDrawable(rippleColor, mContext.getResources().getDrawable(
                R.drawable.drag_handle), null);
        mDragHandleHiddenImage = mContext.getResources().getDrawable(
                R.drawable.drag_handle_overlay);

        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mItemView = new FrameLayout(mContext);

        mView = (FrameLayout) inflater.inflate(R.layout.gesture_view, null, false);
        mView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!mEnabled){
                    return false;
                }

                int action = event.getAction();
                float xRaw = event.getRawX();
                float yRaw = event.getRawY();

                if(DEBUG){
                    Log.d(TAG, "mView onTouch " + action + ":" + (int)xRaw + ":" + (int)yRaw);
                }

                switch (action) {
                case MotionEvent.ACTION_CANCEL:
                    resetView();
                    break;
                case MotionEvent.ACTION_UP:
                    if(mLongPress){
                        if (mCurrentItemEnv[1] != null){
                            if (mLevel == 0 ){
                                mRecentsManager.switchTask(mCurrentItemEnv[1].getTask(), false, false);
                            } else if (mLevel == 1){
                                mRecentsManager.startIntentFromtString(mCurrentItemEnv[1].getIntent(), false);
                            } else if (mLevel == -1){
                                mCurrentItemEnv[1].runAction();
                            }
                        }
                        resetView();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!mShowingSpeedSwitcher) {
                        break;
                    }
                    if (!isValidCoordinate((int)xRaw, (int)yRaw)){
                        if (mLevel != -2 && mLevel != 2){
                            clearViewBackground();
                            hideAllBorders();
                            hideAllLists();
                            resetEnvItems();
                            mLockedLevel = mLevel;
                            mLevel = -2;
                        }
                        break;
                    }
                    int newLevel = calcLevel((int)xRaw, (int)yRaw);
                    if (newLevel != mLevel){
                        int oldLevel = mLevel;
                        if (DEBUG){
                            Log.d(TAG, "yPos: " + yRaw + " oldLevel: " + oldLevel + " newLevel: " + newLevel);
                        }
                        mLevel = newLevel;
                        mLevelX = getHorizontalGridIndex((int)xRaw);
                        doLevelChange(oldLevel);
                        mDownPoint[0] = xRaw;
                    }
                    if (mLevelX == -1){
                        mLevelX = getHorizontalGridIndex((int)xRaw);
                    } else {
                        int levelX = getHorizontalGridIndex((int)xRaw);
                        if (mLevelX != levelX){
                            mLevelX = levelX;
                            switchItem();
                            mDownPoint[0] = xRaw;
                        }
                    }
                }
                return true;
            }
        });

        mDragButton= new ImageView(mContext);
        mDragButton.setScaleType(ImageView.ScaleType.FIT_XY);
        mDragButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                float xRaw = event.getRawX();
                float yRaw = event.getRawY();
                float distanceX = mInitDownPoint[0] - xRaw;

                if(DEBUG){
                    Log.d(TAG, "mDragButton onTouch " + action + ":" + (int)xRaw + ":" + (int)yRaw + " mEnabled=" + mEnabled +
                            " mFlingEnable=" + mFlingEnable +  " mMoveStarted=" + mMoveStarted + " mLongPress=" + mLongPress);
                    }
                if (mFlingEnable && !mHidden) {
                    mGestureDetector.onTouchEvent(event);
                }
                if (!mEnabled){
                    return true;
                }
                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    mShowingSpeedSwitcher = false;
                    mHandleRecentsUpdate = false;
                    mLongPress = false;
                    mFlingEnable = false;
                    mMoveStarted = false;

                    mRecentsManager.clearTasks();
                    RecentTasksLoader.getInstance(mContext).cancelLoadingTasks();
                    RecentTasksLoader.getInstance(mContext).setSwitchManager(mRecentsManager);
                    RecentTasksLoader.getInstance(mContext).preloadTasks();

                    mDownPoint[0] = xRaw;
                    mDownPoint[1] = yRaw;
                    mInitDownPoint[0] = xRaw;
                    mInitDownPoint[1] = yRaw;
                    mLastX = xRaw;
                    if(mConfiguration.mSpeedSwitcher && !mHidden){
                        mHandler.postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    mHandler.removeCallbacks(mLongPressRunnable);
                    mEnabled = true;
                    mFlingEnable = false;
                    mMoveStarted = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mHidden) {
                        return true;
                    }
                    v.setPressed(false);
                    mFlingEnable = false;
                    if (Math.abs(distanceX) > mSlop) {
                        mHandler.removeCallbacks(mLongPressRunnable);
                        if (mLastX > xRaw) {
                            // move left
                            if (mConfiguration.mLocation == 0) {
                                mFlingEnable = true;
                                mMoveStarted = true;
                                mRecentsManager.showHidden();
                            }
                        } else {
                            // move right
                            if (mConfiguration.mLocation != 0) {
                                mFlingEnable = true;
                                mMoveStarted = true;
                                mRecentsManager.showHidden();
                            }
                        }
                        if (mMoveStarted) {
                            mRecentsManager.slideLayout(distanceX);
                        }
                    }
                    mLastX = xRaw;
                    break;
                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    mFlingEnable = false;
                    mHandler.removeCallbacks(mLongPressRunnable);
                    if(mHidden && mConfiguration.mAutoHide){
                        updateDragHandleImage(true);
                        mHandler.postDelayed(mAutoHideRunnable, SwitchConfiguration.AUTO_HIDE_DEFAULT);
                        return true;
                    }

                    if (mMoveStarted) {
                        mRecentsManager.finishSlideLayout();
                    } else {
                        mRecentsManager.hideHidden();
                    }
                    mMoveStarted = false;
                    break;
                }
                return true;
            }
        });
        mView.addView(mDragButton, getDragHandleLayoutParamsSmall());
        updateButton(false);
    }

    private int getGravity() {
        if (mConfiguration.mLocation == 0) {
            return Gravity.RIGHT | Gravity.TOP;
        }
        if (mConfiguration.mLocation == 1) {
            return Gravity.LEFT | Gravity.TOP;
        }

        return Gravity.RIGHT | Gravity.TOP;
    }

    public WindowManager.LayoutParams getParamsSmall() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                mConfiguration.mDragHandleWidth,
                mConfiguration.mDragHandleHeight,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        lp.gravity = getGravity();
        lp.y = mConfiguration.getCurrentOffsetStart();
        return lp;
    }

    private FrameLayout.LayoutParams getDragHandleLayoutParamsSmall() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                mConfiguration.mDragHandleWidth,
                mConfiguration.mDragHandleHeight);
        params.gravity = Gravity.CENTER;
        return params;
    }

    public WindowManager.LayoutParams getParamsFull() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                mConfiguration.getCurrentDisplayWidth(),
                mConfiguration.getCurrentDisplayHeight(),
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.CENTER;
        lp.dimAmount = mConfiguration.mBackgroundOpacity;
        return lp;
    }

    private int getItemViewTopMargin() {
        return Math.max(0, (int)mInitDownPoint[1] - mConfiguration.mThumbnailHeight * 2);
    }

    private int getItemViewHeight() {
        return Math.round(300 * mConfiguration.mDensity);
    }

    public FrameLayout.LayoutParams getItemViewParams() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                mConfiguration.getCurrentDisplayWidth(),
                getItemViewHeight());
        lp.gravity = Gravity.CENTER_HORIZONTAL|Gravity.TOP;
        lp.topMargin = getItemViewTopMargin();
        return lp;
    }

    private void updateButton(boolean reload) {
        if(mConfiguration.mAutoHide){
            updateDragHandleImage(false);
        } else {
            if (reload) {
                // to catch location/rotation changes
                updateDragHandleImage(false);
            }
            updateDragHandleImage(true);
        }
    }

    private void colorizeDragHandleImage() {
        Drawable inner = ((RippleDrawable) mDragHandleImage).getDrawable(0);
        inner=BitmapUtils.colorize(mContext.getResources(), mConfiguration.mDragHandleColor & 0x00FFFFFF, inner);
        inner.setAlpha((mConfiguration.mDragHandleColor >> 24) & 0x000000FF);
        ((RippleDrawable) mDragHandleImage).setDrawable(0, inner);
    }

    private void updateDragHandleImage(boolean shown){
        if ((mHidden && !shown) || (!mHidden && shown)) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "updateDragHandleImage " + shown);
        }

        Drawable current = mDragHandleImage;

        mHidden = !shown;

        if(mConfiguration.mAutoHide){
            if(mHidden){
                current = mDragHandleHiddenImage;
            }
        } else {
            if(!shown){
                current = mDragHandleHiddenImage;
            }
        }
        toggleDragHandle(shown, current);
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        if(DEBUG){
            Log.d(TAG, "updatePrefs");
        }

        if (mConfiguration.mDragHandleShow) {
            colorizeDragHandleImage();
            updateButton(true);
        }

        if (mConfiguration.mSpeedSwitcher) {
            buildFavoriteItems(mConfiguration.mFavoriteList);
            buildActionList();
        }

        if(key == null || key.equals(SettingsActivity.PREF_DRAG_HANDLE_ENABLE)){
            if(mConfiguration.mDragHandleShow){
                show();
            } else {
                hide();
            }
        }
    }

    public synchronized void show() {
        if (mShowing) {
            return;
        }
        // should never happen but make sure were not triggering a crash here
        if (!canDrawOverlayViews()) {
            return;
        }
        if(DEBUG){
            Log.d(TAG, "show");
        }
        mDragButton.setLayoutParams(getDragHandleLayoutParamsSmall());
        mWindowManager.addView(mView, getParamsSmall());
        mShowing = true;
        mEnabled = true;
    }

    public synchronized void hide() {
        if (!mShowing) {
            return;
        }

        if(DEBUG){
            Log.d(TAG, "hide");
        }
        mWindowManager.removeView(mView);
        mShowing = false;
        mEnabled = false;
    }

    public void overlayShown() {
        if (DEBUG){
            Log.d(TAG, "overlayShown");
        }
        mHandler.removeCallbacks(mLongPressRunnable);
        mHandler.removeCallbacks(mAutoHideRunnable);
        updateDragHandleImage(false);
        mEnabled = false;
    }

    public void overlayHidden() {
        if (DEBUG){
            Log.d(TAG, "overlayHidden");
        }
        if(mConfiguration.mAutoHide){
            updateDragHandleImage(false);
        } else {
            updateDragHandleImage(true);
        }
        mEnabled = true;
    }

    public boolean isShowing() {
        return mShowing;
    }

    public void updateLayout() {
        if (mShowing){
            mWindowManager.updateViewLayout(mView, getParamsSmall());
        }
    }

    private Animator start(Animator a) {
        a.start();
        return a;
    }

    private Animator interpolator(TimeInterpolator ti, Animator a) {
        a.setInterpolator(ti);
        return a;
    }

    private void toggleDragHandle(final boolean show, final Drawable current) {
        if (mToggleDragHandleAnim != null){
            mToggleDragHandleAnim.cancel();
        }

        mDragButton.setRotation(mConfiguration.mLocation == 0 ? 0f : 180f);

        if (show){
            mDragButton.setTranslationX(mConfiguration.mLocation == 0 ? mConfiguration.mDragHandleWidth : -mConfiguration.mDragHandleWidth);
            mDragButton.setImageDrawable(current);
            mToggleDragHandleAnim = start(interpolator(mLinearInterpolator,
                            ObjectAnimator.ofFloat(mDragButton, View.TRANSLATION_X,
                            mConfiguration.mLocation == 0 ? mConfiguration.mDragHandleWidth :
                                    -mConfiguration.mDragHandleWidth,
                            0f))
                            .setDuration(FLIP_DURATION_DEFAULT));
        } else {
            mDragButton.setTranslationX(0f);
            mToggleDragHandleAnim = start(interpolator(mLinearInterpolator,
                            ObjectAnimator.ofFloat(mDragButton, View.TRANSLATION_X, 1f,
                            mConfiguration.mLocation == 0 ? mConfiguration.mDragHandleWidth :
                                    -mConfiguration.mDragHandleWidth))
                            .setDuration(FLIP_DURATION_DEFAULT));
        }

        mToggleDragHandleAnim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!show){
                    mDragButton.setImageDrawable(current);
                    mDragButton.setTranslationX(0f);
                }
            }
            @Override
            public void onAnimationStart(Animator animation) {
            }
            @Override
            public void onAnimationCancel(Animator animation) {
            }
            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    private synchronized void resetView() {
        if (DEBUG){
            Log.d(TAG, "resetView");
        }

        if (mShowingSpeedSwitcher){
            mWindowManager.removeView(mView);
            mItemView.removeAllViews();
            mView.removeAllViews();
            resetEnvItems();
            clearViewBackground();

            mDragButton.setImageDrawable(mDragHandleHiddenImage);
            mView.addView(mDragButton, getDragHandleLayoutParamsSmall());
            mWindowManager.addView(mView, getParamsSmall());

            if(!mConfiguration.mAutoHide){
                updateDragHandleImage(true);
            }
            mRecentsManager.getLayout().resetRecentsState();
            // run back trigger if required
            if(mVirtualBackKey && !mConfiguration.mRestrictedMode){
                Utils.triggerVirtualKeypress(mHandler, KeyEvent.KEYCODE_BACK);
            }
            mVirtualBackKey = false;
            if(mVirtualMenuKey && !mConfiguration.mRestrictedMode){
                Utils.triggerVirtualKeypress(mHandler, KeyEvent.KEYCODE_MENU);
            }
            mVirtualMenuKey = false;
        }
        mShowingSpeedSwitcher = false;
        mHandleRecentsUpdate = false;
        mLongPress = false;
    }

    public synchronized void update() {
        if (DEBUG){
            Log.d(TAG, "update " + System.currentTimeMillis() + " " + mRecentsManager.getTasks() + " mLevel = " + mLevel);
        }
        if(isHandleRecentsUpdate()){
            loadRecentItems();
        }
    }

    private PackageTextView getPackageItemTemplate() {
        PackageTextView item = new PackageTextView(mContext);
        item.setTextColor(Color.WHITE);
        item.setShadowLayer(5, 0, 0, Color.BLACK);
        item.setTextSize(20f);
        item.setTypeface(item.getTypeface(), Typeface.BOLD);
        item.setEllipsize(TextUtils.TruncateAt.END);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setMaxLines(1);
        return item;
    }

    private synchronized void loadRecentItems() {
        if (!mConfiguration.mSpeedSwitcher) {
            Log.d(TAG, "loadRecentItems: called with !mSpeedSwitcher");
            return;
        }
        if (!mLongPress) {
            Log.d(TAG, "loadRecentItems: called with !mLongPress");
            return;
        }
        if (DEBUG){
            Log.d(TAG, "loadRecentItems:");
        }
        mHandler.removeCallbacks(mAutoHideRunnable);
        mRecentList.clear();
        int i = 0;
        Iterator<TaskDescription> nextTask = mRecentsManager.getTasks().iterator();
        while(nextTask.hasNext() && i < mConfiguration.mLimitItemsX){
            TaskDescription ad = nextTask.next();
            PackageTextView item = getPackageItemTemplate();
            item.setThumbRatio(mThumbRatio);
            item.setTask(ad, true);
            item.loadTaskThumb();
            mRecentList.add(item);
            i++;
        }
        mCurrentRecentItemIndex = 0;
        mCurrentFavoriteItemIndex = 0;
        mCurrentActionItemIndex = 0;
        mLevelX = -1;
        mLevel = -2;
        mLockedLevel = mLevel;
        mShowingSpeedSwitcher = true;

        resetEnvItems();
        calcVerticalBorders();
        if (Utils.isLockToAppEnabled(mContext)) {
            updatePinAppButton();
        }

        fillList(0);
        fillList(1);
        fillList(2);

        if(mRecentList.size() > 0 || mFavoriteList.size() > 0 || mActionList.size() > 0){
            mHidden = true;
            mView.removeView(mDragButton);
            mWindowManager.updateViewLayout(mView, getParamsFull());
            try {
                mView.removeView(mItemView);
            } catch(Exception e) {
            }
            mView.addView(mItemView, getItemViewParams());
        }

        if(mRecentList.size() > 0){
            mLevel = 0;
            mLockedLevel = mLevel;
            setViewBackground();
            PackageTextView item = mRecentList.get(mCurrentRecentItemIndex);
            layoutTask(item);
        } else if(mFavoriteList.size() > 0){
            mLevel = 1;
            mLockedLevel = mLevel;
            setViewBackground();
            PackageTextView item = mFavoriteList.get(mCurrentFavoriteItemIndex);
            layoutFavorite(item);
        } else if(mActionList.size() > 0){
            mLevel = -1;
            mLockedLevel = mLevel;
            setViewBackground();
            PackageTextView item = mActionList.get(mCurrentActionItemIndex);
            layoutAction(item);
        }

        if(mRecentList.size() > 0 || mFavoriteList.size() > 0 || mActionList.size() > 0){
            initAlpha();
            final HorizontalScrollView actualView = mAllLists[levelToListLevel(mLevel)];
            actualView.scrollTo(0, 0);
            actualView.setVisibility(View.VISIBLE);
            drawCurrentLevelBorders();
        }
    }

    private void layoutTask(PackageTextView item){
        if (DEBUG){
            Log.d(TAG, "layoutTask:" + item.getLabel());
        }
        mCurrentItemEnv[1] = item;
        updateCurrentItemEnv();
    }

    private void buildFavoriteItems(List<String> favoriteList){
        mFavoriteList.clear();
        int i = 0;
        Iterator<String> nextFavorite = favoriteList.iterator();
        while(nextFavorite.hasNext() && i < mConfiguration.mLimitItemsX){
            String intent = nextFavorite.next();
            PackageTextView item = getPackageItemTemplate();
            PackageManager.PackageItem packageItem = PackageManager.getInstance(mContext).getPackageItem(intent);
            if (packageItem == null){
                Log.d(TAG, "failed to add " + intent);
                continue;
            }
            item.setIntent(packageItem.getIntent());
            item.setLabel(packageItem.getTitle().toString());
            Drawable d = BitmapCache.getInstance(mContext).getResizedUncached(mContext.getResources(),
                    packageItem, mConfiguration, 100);
            item.setOriginalImage(d);
            item.setCompoundDrawablesWithIntrinsicBounds(null, item.getOriginalImage(), null, null);
            if (mConfiguration.mShowLabels) {
                item.setText(item.getLabel());
            }
            mFavoriteList.add(item);
            i++;
        }
    }

    private void layoutFavorite(PackageTextView item){
        if (DEBUG){
            Log.d(TAG, "layoutFavorite:" + item.getLabel());
        }
        mCurrentItemEnv[1] = item;
        updateCurrentItemEnv();
    }

    private void setViewBackground(){
        GradientDrawable background = null;
        if (mConfiguration.mLevelBackgroundColor){
            if (mLevel == 0){
                int colors[] = { 0x00000000, 0xC033b5e5, 0x00000000 };
                background = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
            } else if (mLevel == 1){
                int colors[] = { 0x00000000, 0xC0A4C739, 0x00000000 };
                background = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
            } else if (mLevel == -1){
                int colors[] = { 0x00000000, 0xC0FF0000, 0x00000000 };
                background = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
            }
        } else {
            int colors[] = { 0x00000000, 0xC0000000, 0x00000000 };
            background = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        }
        if (background != null){
            mItemView.setBackground(background);
        }
    }

    private void clearViewBackground(){
        mItemView.setBackground(null);
    }

    private boolean isAllowedLevelChange() {
        if (mLevel == 0){
            if (mRecentList.size() > 0){
                return mCurrentRecentItemIndex == 0;
            }
        } else if (mLevel == 1){
            if (mFavoriteList.size() > 0){
                return mCurrentFavoriteItemIndex == 0;
            }
        } else if (mLevel == -1){
            if (mActionList.size() > 0){
                return mCurrentActionItemIndex == 0;
            }
        }
        return true;
    }

    private int calcLevel(int xPos, int yPos){
        boolean isLevelSwitchArea = isAllowedLevelChange();
        int oldLevel = mLevel;
        int newLevel = -2;
        if (yPos < mVerticalBorders[0]){
            newLevel = -2;
        } else if (yPos < mVerticalBorders[1]){
            if(mActionList.size() > 0){
                newLevel = -1;
            } else {
                newLevel = -2;
            }
        } else if (yPos >= mVerticalBorders[1] && yPos < mVerticalBorders[2]){
            if(mRecentList.size() > 0){
                newLevel = 0;
            } else if(mFavoriteList.size() > 0){
                newLevel = 1;
            } else if(mActionList.size() > 0){
                newLevel = -1;
            }
        } else if (yPos >= mVerticalBorders[2] && yPos < mVerticalBorders[3]){
            if(mFavoriteList.size() > 0){
                newLevel = 1;
            } else {
                newLevel = 2;
            }
        } else if (yPos >= mVerticalBorders[3]){
            newLevel = 2;
        }

        if (oldLevel != newLevel){
            if (mConfiguration.mLimitLevelChangeX){
                if (oldLevel == -2 || oldLevel == 2){
                    // coming from outside
                    return mLockedLevel;
                }
                if (newLevel == -2 || newLevel == 2){
                    // going outside
                    mLockedLevel = oldLevel;
                    return newLevel;
                }
                if (newLevel != -2 && newLevel != 2 && !isLevelSwitchArea){
                    // switch not possible
                    newLevel = oldLevel;
                }
            }
        }
        return newLevel;
    }

    private void doLevelChange(int oldLevel) {
        clearViewBackground();
        resetEnvItems();

        int idx = 0;
        PackageTextView item = null;
        HorizontalScrollView oldList = null;

        List<PackageTextView> currentList = getCurrentList(levelToListLevel(mLevel));
        if (currentList != null){
            idx = roundIndex(currentList, mLevelX);
            if (DEBUG){
                Log.d(TAG, "idx " + idx + " mLevelX " + mLevelX);
            }
            item = currentList.get(idx);
        }
        if (levelToListLevel(oldLevel) != -1){
            oldList = mAllLists[levelToListLevel(oldLevel)];
        }

        final HorizontalScrollView finalOldList = oldList;
        final int idxFinal = idx;

        if (mLevel == 0){
            if (mRecentList.size() != 0){
                mCurrentRecentItemIndex = idx;
                setViewBackground();
                layoutTask(item);
            } else if(mFavoriteList.size() > 0){
                mCurrentFavoriteItemIndex = idx;
                setViewBackground();
                layoutFavorite(item);
            } else if(mActionList.size() > 0){
                mCurrentActionItemIndex = idx;
                setViewBackground();
                layoutAction(item);
            }
        } else if (mLevel == 1){
            if (mFavoriteList.size() != 0){
                mCurrentFavoriteItemIndex = idx;
                setViewBackground();
                layoutFavorite(item);
            }
        } else if (mLevel == -1){
            if (mActionList.size() != 0){
                mCurrentActionItemIndex = idx;
                setViewBackground();
                layoutAction(item);
            }
        } else {
            if (finalOldList != null){
                finalOldList.animate().alpha(0f).setDuration(200).withEndAction(new Runnable(){
                    @Override
                    public void run() {
                        finalOldList.setVisibility(View.GONE);
                        finalOldList.setAlpha(1f);
                    }});
            }
            return;
        }

        final HorizontalScrollView actualView = mAllLists[levelToListLevel(mLevel)];
        actualView.setVisibility(View.VISIBLE);
        if (finalOldList != null){
            actualView.setScaleY(0f);
        } else {
            // coming from outside
            actualView.setAlpha(0f);
        }

        mHandler.post(new Runnable(){
            @Override
            public void run() {
                actualView.scrollTo(idxFinal * getListItemWidth(levelToListLevel(mLevel)), 0);
                initAlpha();
                if (finalOldList != null){
                    finalOldList.animate().scaleY(0f).setDuration(100).withEndAction(new Runnable(){
                        @Override
                        public void run() {
                            finalOldList.setVisibility(View.GONE);
                            finalOldList.setScaleY(1f);
                        }});
                    actualView.animate().scaleY(1f).setDuration(200);
                } else {
                    // coming from outside
                    actualView.animate().alpha(1f).setDuration(200);
                }
                drawCurrentLevelBorders();
            }});
    }

    private void calcVerticalBorders(){
        mVerticalBorders[0] = (int)mInitDownPoint[1] - mConfiguration.mLevelHeight / 2 - mConfiguration.mLevelHeight;
        mVerticalBorders[1] = (int)mInitDownPoint[1] - mConfiguration.mLevelHeight / 2;
        mVerticalBorders[2] = (int)mInitDownPoint[1] + mConfiguration.mLevelHeight / 2;
        mVerticalBorders[3] = (int)mInitDownPoint[1] + mConfiguration.mLevelHeight / 2 + mConfiguration.mLevelHeight;
    }

    public boolean isHandleRecentsUpdate() {
        return mConfiguration.mSpeedSwitcher && mHandleRecentsUpdate;
    }

    private boolean isValidCoordinate(int xPos, int yPos){
        int location = mConfiguration.mLocation;
        Rect r = new Rect(location == 1 ? 0 : 10, 10, mConfiguration.getCurrentDisplayWidth() - (location == 1 ? 20 : 0), mConfiguration.getCurrentDisplayHeight() - 20);
        return r.contains(xPos, yPos);
    }

    private int roundIndex(List<PackageTextView> list, int idx){
        if (idx < 0){
            idx = 0;
        } else if (idx > list.size() -1){
            idx = list.size() - 1;
        }
        return idx;
    }

    private void updateCurrentItemEnv(){
        int idx = 0;
        int leftIdx = 0;
        int rightIdx = 0;

        if (mLevel == 0){
            idx = mCurrentRecentItemIndex;
        } else if (mLevel == 1){
            idx = mCurrentFavoriteItemIndex;
        } else if (mLevel == -1){
            idx = mCurrentActionItemIndex;
        }

        List<PackageTextView> currentList = getCurrentList(levelToListLevel(mLevel));
        if (currentList == null){
            return;
        }

        if (mConfiguration.mLocation == 0){
            leftIdx = idx + 1;
            rightIdx = idx - 1;
        } else {
            leftIdx = idx - 1;
            rightIdx = idx + 1;
        }
        leftIdx = roundIndex(currentList, leftIdx);
        rightIdx = roundIndex(currentList, rightIdx);

        if (leftIdx != idx){
            mCurrentItemEnv[0] = currentList.get(leftIdx);
        } else {
            mCurrentItemEnv[0] = null;
        }
        if (rightIdx != idx){
            mCurrentItemEnv[2] = currentList.get(rightIdx);
        } else {
            mCurrentItemEnv[2] = null;
        }
        if (DEBUG){
            Log.d(TAG, "updateCurrentItemEnv:" + mCurrentItemEnv[0] + ":" + mCurrentItemEnv[1] + ":" + mCurrentItemEnv[2]);
        }
    }

    private void resetEnvItems(){
        mCurrentItemEnv[0] = null;
        mCurrentItemEnv[1] = null;
        mCurrentItemEnv[2] = null;
    }

    private void switchItem(){
        if (mLevel == 0 || mLevel == 1 || mLevel == -1){
            List<PackageTextView> currentList = getCurrentList(levelToListLevel(mLevel));
            if (currentList == null){
                return;
            }
            int idx = roundIndex(currentList, mLevelX);
            if (DEBUG){
                Log.d(TAG, "idx " + idx + " mLevelX " + mLevelX);
            }
            PackageTextView item = currentList.get(idx);
            final HorizontalScrollView actualView = mAllLists[levelToListLevel(mLevel)];

            if (mLevel == 0){
                if (mRecentList.size() > 0){
                    mCurrentRecentItemIndex = idx;
                    layoutTask(item);
                }
            } else if (mLevel == 1){
                if (mFavoriteList.size() > 0){
                    mCurrentFavoriteItemIndex = idx;
                    layoutFavorite(item);
                }
            } else if (mLevel == -1){
                if (mActionList.size() > 0){
                    mCurrentActionItemIndex = idx;
                    layoutAction(item);
                }
            }
            if (item != null){
                if (idx != -1){
                    actualView.smoothScrollTo(idx * getListItemWidth(levelToListLevel(mLevel)), 0);
                    initAlpha();
                    //animateAlpha();
                }
            }
        }
    }

    private void buildActionList(){
        mActionList.clear();
        Iterator<Integer> nextKey = mConfiguration.mSpeedSwitchButtons.keySet().iterator();
        while(nextKey.hasNext()){
            Integer key = nextKey.next();
            Boolean value = mConfiguration.mSpeedSwitchButtons.get(key);
            if (value){
                PackageTextView item = getActionButton(key);
                if (item != null){
                    mActionList.add(item);
                    item.setCompoundDrawablesWithIntrinsicBounds(null, item.getOriginalImage(), null, null);
                }
            }
        }
    }

    private PackageTextView getActionButton(int buttonId){
        PackageTextView item;
        Drawable d;

        if (buttonId == SettingsActivity.BUTTON_SPEED_SWITCH_HOME){
            item = getPackageItemTemplate();
            d = mContext.getResources().getDrawable(R.drawable.ic_sysbar_home);
            d = BitmapUtils.resize(mContext.getResources(),
                    d,
                    mConfiguration.mQSActionSize,
                    mConfiguration.mIconBorder,
                    mConfiguration.mDensity);
            item.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), d));

            item.setLabel(mContext.getResources().getString(R.string.home_help));
            item.setAction(new Runnable(){
                @Override
                public void run() {
                    mRecentsManager.goHome(false);
                }});
            return item;
        }
        if (buttonId == SettingsActivity.BUTTON_SPEED_SWITCH_BACK){
            item = getPackageItemTemplate();
            d = mContext.getResources().getDrawable(R.drawable.ic_sysbar_back);
            d = BitmapUtils.resize(mContext.getResources(),
                    d,
                    mConfiguration.mQSActionSize,
                    mConfiguration.mIconBorder,
                    mConfiguration.mDensity);
            item.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), d));

            item.setLabel(mContext.getResources().getString(R.string.back));
            item.setAction(new Runnable(){
                @Override
                public void run() {
                    mVirtualBackKey = true;
                }});
            return item;
        }
        if (buttonId == SettingsActivity.BUTTON_SPEED_SWITCH_KILL_CURRENT){
            item = getPackageItemTemplate();
            d = mContext.getResources().getDrawable(R.drawable.kill_current);
            d = BitmapUtils.resize(mContext.getResources(),
                    d,
                    mConfiguration.mQSActionSize,
                    mConfiguration.mIconBorder,
                    mConfiguration.mDensity);
            item.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), d));

            item.setLabel(mContext.getResources().getString(R.string.kill_current));
            item.setAction(new Runnable(){
                @Override
                public void run() {
                    mRecentsManager.killCurrent(false);
                }});
            return item;
        }
        if (buttonId == SettingsActivity.BUTTON_SPEED_SWITCH_KILL_ALL){
            item = getPackageItemTemplate();
            d = mContext.getResources().getDrawable(R.drawable.kill_all);
            d = BitmapUtils.resize(mContext.getResources(),
                    d,
                    mConfiguration.mQSActionSize,
                    mConfiguration.mIconBorder,
                    mConfiguration.mDensity);
            item.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), d));

            item.setLabel(mContext.getResources().getString(R.string.kill_all_apps));
            item.setAction(new Runnable(){
                @Override
                public void run() {
                    mRecentsManager.killAll(false);
                }});
            return item;
        }
        if (buttonId == SettingsActivity.BUTTON_SPEED_SWITCH_KILL_OTHER){
            item = getPackageItemTemplate();
            d = mContext.getResources().getDrawable(R.drawable.kill_other);
            d = BitmapUtils.resize(mContext.getResources(),
                    d,
                    mConfiguration.mQSActionSize,
                    mConfiguration.mIconBorder,
                    mConfiguration.mDensity);
            item.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), d));

            item.setLabel(mContext.getResources().getString(R.string.kill_other_apps));
            item.setAction(new Runnable(){
                @Override
                public void run() {
                    mRecentsManager.killOther(false);
                }});
            return item;
        }
        if (buttonId == SettingsActivity.BUTTON_SPEED_SWITCH_LOCK_APP){
            mLockToAppButton = getPackageItemTemplate();
            d = mContext.getResources().getDrawable(R.drawable.ic_pin);
            d = BitmapUtils.resize(mContext.getResources(),
                    d,
                    mConfiguration.mQSActionSize,
                    mConfiguration.mIconBorder,
                    mConfiguration.mDensity);
            mLockToAppButton.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), d));

            mLockToAppButton.setLabel(mContext.getResources().getString(R.string.lock_to_app));
            mLockToAppButton.setAction(new Runnable(){
                @Override
                public void run() {
                    if (!Utils.isLockToAppEnabled(mContext)) {
                        Toast.makeText(
                                mContext,
                                mContext.getResources().getString(
                                        R.string.lock_app_not_enabled),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mRecentsManager.toggleLockToApp(false);
                }});
            return mLockToAppButton;
        }
        if (buttonId == SettingsActivity.BUTTON_SPEED_SWITCH_TOGGLE_APP){
            item = getPackageItemTemplate();
            d = mContext.getResources().getDrawable(R.drawable.ic_lastapp);
            d = BitmapUtils.resize(mContext.getResources(),
                    d,
                    mConfiguration.mQSActionSize,
                    mConfiguration.mIconBorder,
                    mConfiguration.mDensity);
            item.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), d));

            item.setLabel(mContext.getResources().getString(R.string.toggle_last_app));
            item.setAction(new Runnable(){
                @Override
                public void run() {
                    mRecentsManager.toggleLastApp(false);
                }});
            return item;
        }
        if (buttonId == SettingsActivity.BUTTON_SPEED_SWITCH_MENU) {
            item = getPackageItemTemplate();
            d = mContext.getResources().getDrawable(R.drawable.ic_menu);
            d = BitmapUtils.resize(mContext.getResources(),
                    d,
                    mConfiguration.mQSActionSize,
                    mConfiguration.mIconBorder,
                    mConfiguration.mDensity);
            item.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), d));

            item.setLabel(mContext.getResources().getString(R.string.menu));
            item.setAction(new Runnable(){
                @Override
                public void run() {
                    mVirtualMenuKey = true;
                }});
            return item;
        }

        return null;
    }

    private void layoutAction(PackageTextView item){
        if (DEBUG){
            Log.d(TAG, "layoutAction:" + item.getLabel());
        }
        mCurrentItemEnv[1] = item;
        updateCurrentItemEnv();
    }

    private int getHorizontalGridIndex(int rawX){
        int width = 1;
        if (mLevel == 0){
            width = mConfiguration.getCurrentDisplayWidth() / mRecentList.size();
        } else if (mLevel == 1){
            width = mConfiguration.getCurrentDisplayWidth() / mFavoriteList.size();
        } else if (mLevel == -1){
            width = mConfiguration.getCurrentDisplayWidth() / mActionList.size();
        }
        if (width > mConfiguration.mItemChangeWidthX){
            width = mConfiguration.mItemChangeWidthX;
        }
        // count from left or right
        if (mConfiguration.mLocation == 0){
            return (mConfiguration.getCurrentDisplayWidth() - rawX) / width;
        } else {
            return rawX / width;
        }
    }

    private FrameLayout.LayoutParams getListViewParams(int level){
        int width = level == 1 ? (int)(mConfiguration.mThumbnailWidth * mThumbRatio * 3) : mConfiguration.getCurrentOverlayWidth();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                width,
                FrameLayout.LayoutParams.WRAP_CONTENT);

        params.gravity = Gravity.CENTER;
        return params;
    }

    private int getListItemWidth(int level) {
        if (level == 1){
            return (int)(mConfiguration.mThumbnailWidth * mThumbRatio) + mConfiguration.mIconBorder;
        }
        return mConfiguration.getCurrentOverlayWidth() / 3;
    }

    private LinearLayout.LayoutParams getListItemParams(int level){
        int width = getListItemWidth(level);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                width,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        return params;
    }

    private void hideAllLists(){
        HorizontalScrollView oldList = null;
        if (levelToListLevel(mLevel) != -1){
            oldList = mAllLists[levelToListLevel(mLevel)];
        }

        final HorizontalScrollView finalOldList = oldList;
        if (finalOldList != null){
            finalOldList.animate().alpha(0f).setDuration(200).withEndAction(new Runnable(){
            @Override
            public void run() {
                finalOldList.setAlpha(1f);
                mAllLists[0].setVisibility(View.GONE);
                mAllLists[1].setVisibility(View.GONE);
                mAllLists[2].setVisibility(View.GONE);
            }
        });
        } else {
            mAllLists[0].setVisibility(View.GONE);
            mAllLists[1].setVisibility(View.GONE);
            mAllLists[2].setVisibility(View.GONE);
        }
    }

    private int levelToListLevel(int level){
        if (level == -1){
            return 0;
        }
        if (level == 0){
            return 1;
        }
        if (level == 1){
            return 2;
        }
        return -1;
    }

    private List<PackageTextView> getCurrentList(int level){
        if (level == 0){
            return mActionList;
        }
        if (level == 1){
            return mRecentList;
        }
        if (level == 2){
            return mFavoriteList;
        }
        return null;
    }

    private void fillList(final int level) {
        if (level < 0 || level > 2){
            return;
        }
        LinearLayout listLayout = (LinearLayout)mAllLists[level].getChildAt(0);
        listLayout.removeAllViews();

        PackageTextView header = getPackageItemTemplate();
        listLayout.addView(header, getListItemParams(level));

        Iterator<PackageTextView> nextItem = getCurrentList(level).iterator();
        while(nextItem.hasNext()){
            PackageTextView item = nextItem.next();
            listLayout.addView(item, getListItemParams(level));
        }

        PackageTextView footer = getPackageItemTemplate();
        listLayout.addView(footer, getListItemParams(level));
        mAllLists[level].setVisibility(View.GONE);
        mAllLists[level].setLayoutParams(getListViewParams(level));

        try {
            mItemView.addView(mAllLists[level]);
        } catch(IllegalStateException e) {
            // something went wrong - try to recover here
        }
    }

//    private void animateAlpha(){
//        if (mCurrentItemEnv[0] != null){
//            mCurrentItemEnv[0].animate().alpha(0.5f).setDuration(100);
//        }
//        if (mCurrentItemEnv[1] != null){
//            mCurrentItemEnv[1].animate().alpha(1f).setDuration(100);
//        }
//        if (mCurrentItemEnv[2] != null){
//            mCurrentItemEnv[2].animate().alpha(0.5f).setDuration(100);
//        }
//    }

    private void initAlpha(){
        if (mCurrentItemEnv[0] != null){
            mCurrentItemEnv[0].setAlpha(0.5f);
        }
        if (mCurrentItemEnv[1] != null){
            mCurrentItemEnv[1].setAlpha(1f);
        }
        if (mCurrentItemEnv[2] != null){
            mCurrentItemEnv[2].setAlpha(0.5f);
        }
    }

    private View getLevelStripeBorderView(int level) {
        int top = 0;
        if (level == -1){
            top = mVerticalBorders[0];
        } else if (level == 0){
            top = mVerticalBorders[1];
        } else if (level == 1){
            if(mRecentList.size() > 0){
                top = mVerticalBorders[2];
            } else {
                top = mVerticalBorders[1];
            }
        } else {
            return null;
        }
        View border = new View(mContext);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                mConfiguration.getCurrentDisplayWidth(),
                mConfiguration.mLevelHeight);
        params.topMargin = top;
        border.setLayoutParams(params);
        return border;
    }

    private void drawCurrentLevelBorders(){
        if (!mShowIndicators){
            return;
        }
        if (mLevelBorderIndicator != null){
            mView.removeView(mLevelBorderIndicator);
            mLevelBorderIndicator = null;
        }

        mLevelBorderIndicator = getLevelStripeBorderView(mLevel);
        if (mLevelBorderIndicator != null){
            mLevelBorderIndicator.setBackgroundColor(Color.BLACK);
            mLevelBorderIndicator.setAlpha(0.5f);
            mView.addView(mLevelBorderIndicator);
        }
    }

    private void hideAllBorders(){
        if (!mShowIndicators){
            return;
        }
        if (mLevelBorderIndicator != null){
            mView.removeView(mLevelBorderIndicator);
            mLevelBorderIndicator = null;
        }
    }

    private boolean canDrawOverlayViews() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(mContext);
    }

    private void updatePinAppButton() {
        if (mLockToAppButton != null) {
            Drawable d = null;
            if (Utils.isInLockTaskMode()) {
                d = mContext.getResources().getDrawable(R.drawable.ic_pin_off);
            } else {
                d = mContext.getResources().getDrawable(R.drawable.ic_pin);
            }
            d = BitmapUtils.resize(mContext.getResources(),
                    d,
                    mConfiguration.mQSActionSize,
                    mConfiguration.mIconBorder,
                    mConfiguration.mDensity);
            mLockToAppButton.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), d));
        }
    }
}
