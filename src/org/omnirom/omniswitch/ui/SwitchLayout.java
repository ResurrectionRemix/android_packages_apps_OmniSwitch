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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.omnirom.omniswitch.PackageManager;
import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.SettingsActivity;
import org.omnirom.omniswitch.SwitchConfiguration;
import org.omnirom.omniswitch.SwitchManager;
import org.omnirom.omniswitch.TaskDescription;
import org.omnirom.omniswitch.Utils;
import org.omnirom.omniswitch.showcase.ShowcaseView;
import org.omnirom.omniswitch.showcase.ShowcaseView.OnShowcaseEventListener;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.ContextThemeWrapper;
import android.view.ViewAnimationUtils;
import android.view.ViewOutlineProvider;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;

public class SwitchLayout implements OnShowcaseEventListener {
    private static final String TAG = "SwitchLayout";
    private static final boolean DEBUG = false;
    private static final String KEY_SHOWCASE_FAVORITE = "showcase_favorite_done";

    private static final int FLIP_DURATION_DEFAULT = 200;
    private static final int FLIP_DURATION = 500;
    private static final int SHOW_DURATION = 200;
    private static final int SHOW_DURATION_FAST = 100;
    private static final int HIDE_DURATION = 150;
    private static final int HIDE_DURATION_FAST = 100;
    private static final int ROTATE_180_DEGREE = 180;

    private WindowManager mWindowManager;
    private LayoutInflater mInflater;
    private HorizontalListView mRecentListHorizontal;
    private HorizontalListView mFavoriteListHorizontal;
    private ImageView mLastAppButton;
    private ImageView mKillAllButton;
    private ImageView mKillOtherButton;
    private ImageView mHomeButton;
    private ImageView mSettingsButton;
    private ImageView mAllappsButton;
    private ImageView mBackButton;
    private ImageView mLockToAppButton;
    private RecentListAdapter mRecentListAdapter;
    private FavoriteListAdapter mFavoriteListAdapter;
    private Context mContext;
    private SwitchManager mRecentsManager;
    private FrameLayout mPopupView;
    private boolean mShowing;
    private PopupMenu mPopup;
    private View mView;
    private LinearColorBar mRamUsageBar;
    private TextView mBackgroundProcessText;
    private TextView mForegroundProcessText;
    private Handler mHandler = new Handler();
    private List<String> mFavoriteList;
    private boolean mShowFavorites;
    private ShowcaseView mShowcaseView;
    private SharedPreferences mPrefs;
    private boolean mShowcaseDone;
    private float mOpenFavoriteY;
    private SwitchConfiguration mConfiguration;
    private ImageView mOpenFavorite;
    private boolean mHasFavorites;
    private TextView mNoRecentApps;
    private boolean mTaskLoadDone;
    private boolean mUpdateNoRecentsTasksDone;
    private boolean mButtonsVisible = true;
    private boolean mAutoClose = true;
    private boolean mShowAppDrawer;
    private GridView mAppDrawer;
    private AppDrawerListAdapter mAppDrawerListAdapter;
    private View mCurrentSelection;
    private LinearLayout mRamUsageBarContainer;
    private LinearLayout mRecents;
    private Animator mAppDrawerAnim;
    private Animator mRecentsAnim;
    private TimeInterpolator mAccelerateInterpolator = new AccelerateInterpolator();
    private TimeInterpolator mDecelerateInterpolator = new DecelerateInterpolator();
    private Animator mShowFavAnim;
    private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private Animator mToggleOverlayAnim;
    private boolean mVirtualBackKey;
    private HorizontalScrollView mButtonList;
    private LinearLayout mButtonListItems;
    private LinearLayout mButtonListContainer;
    private List<ImageView> mActionList;
    private boolean mHandleRecentsUpdate;
    private float mCurrentSlideWidth;
    private float mCurrentDistance;
    private float[] mDownPoint = new float[2];
    private boolean mEnabled;
    private int mSlop;
    private boolean mFlingEnable = true;
    private float mLastX;

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
            if (DEBUG) {
                Log.d(TAG, "onFling close");
            }
            finishOverlaySlide(false, true);
            return false;
        }
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    };

    private class RecentListAdapter extends ArrayAdapter<TaskDescription> {

        public RecentListAdapter(Context context, int resource,
                List<TaskDescription> values) {
            super(context, R.layout.package_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TaskDescription ad = mRecentsManager.getTasks().get(position);

            PackageTextView item = null;
            if (convertView == null){
                item = getPackageItemTemplate();
            } else {
                item = (PackageTextView) convertView;
            }
            item.setTask(ad, false);

            if (mConfiguration.mShowLabels) {
                item.setText(ad.getLabel());
            } else {
                item.setText("");
            }
            Drawable d = BitmapCache.getInstance(mContext).getResized(mContext.getResources(), ad, ad.getIcon(), mConfiguration, mConfiguration.mIconSize);
            item.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
            return item;
        }
    }

    private class FavoriteListAdapter extends ArrayAdapter<String> {

        public FavoriteListAdapter(Context context, int resource,
                List<String> values) {
            super(context, R.layout.package_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PackageTextView item = null;
            if (convertView == null){
                item = getPackageItemTemplate();
            } else {
                item = (PackageTextView) convertView;
            }
            String intent = mFavoriteList.get(position);

            PackageManager.PackageItem packageItem = PackageManager.getInstance(mContext).getPackageItem(intent);
            item.setIntent(packageItem.getIntent());
            if (mConfiguration.mShowLabels) {
                item.setText(packageItem.getTitle());
            } else {
                item.setText("");
            }
            Drawable d = BitmapCache.getInstance(mContext).getResized(mContext.getResources(), packageItem, mConfiguration, mConfiguration.mIconSize);
            item.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
            return item;
        }
    }

    private class AppDrawerListAdapter extends ArrayAdapter<PackageManager.PackageItem> {

        public AppDrawerListAdapter(Context context, int resource,
                List<PackageManager.PackageItem> values) {
            super(context, R.layout.package_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PackageTextView item = null;
            if (convertView == null){
                item = getPackageItemTemplate();
            } else {
                item = (PackageTextView) convertView;
            }
            PackageManager.PackageItem packageItem = PackageManager.getInstance(mContext).getPackageList().get(position);
            item.setIntent(packageItem.getIntent());
            if (mConfiguration.mShowLabels) {
                item.setText(packageItem.getTitle());
            } else {
                item.setText("");
            }
            Drawable d = BitmapCache.getInstance(mContext).getResized(mContext.getResources(), packageItem, mConfiguration, mConfiguration.mIconSize);
            item.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
            return item;
        }
    }

    private View.OnTouchListener mDragHandleListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            float xRaw = event.getRawX();
            float yRaw = event.getRawY();
            float distanceX = mDownPoint[0] - xRaw;

            if(DEBUG){
                Log.d(TAG, "mView onTouch " + action + ":" + (int)xRaw + ":" + (int)yRaw + " " + mEnabled + " " + mFlingEnable);
            }
            if (mFlingEnable) {
                mGestureDetector.onTouchEvent(event);
            }
            if (!mEnabled) {
                return true;
            }
            switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownPoint[0] = xRaw;
                mDownPoint[1] = yRaw;
                mEnabled = true;
                mFlingEnable = true;
                mLastX = xRaw;
                break;
            case MotionEvent.ACTION_CANCEL:
                mEnabled = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(distanceX) > mSlop) {
                    mFlingEnable = false;

                    if (mLastX > xRaw) {
                        // move left
                        if (mConfiguration.mLocation != 0) {
                            mFlingEnable = true;
                        }
                    } else {
                        // move right
                        if (mConfiguration.mLocation == 0) {
                            mFlingEnable = true;
                        }
                    }
                    if (distanceX > 0) {
                        // move left
                        if (mConfiguration.mLocation != 0) {
                            slideLayoutHide(-distanceX);
                        }
                    } else {
                        // move right
                        if (mConfiguration.mLocation == 0) {
                            slideLayoutHide(-distanceX);
                        }
                    }
                }
                mLastX = xRaw;
                break;
            case MotionEvent.ACTION_UP:
                mFlingEnable = true;
                if (Math.abs(distanceX) > mSlop) {
                    finishSlideLayoutHide();
                } else {
                    hide(false);
                }
                break;
            }
            return true;
        }
    };

    private static final ViewOutlineProvider BUTTON_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
        }
    };

    private static final ViewOutlineProvider BAR_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
        }
    };

    public SwitchLayout(SwitchManager manager, Context context) {
        mContext = context;
        mRecentsManager = manager;
        mWindowManager = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mConfiguration = SwitchConfiguration.getInstance(mContext);

        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRecentListAdapter = new RecentListAdapter(mContext,
                android.R.layout.simple_list_item_single_choice,
                mRecentsManager.getTasks());
        mFavoriteList = new ArrayList<String>();
        mFavoriteListAdapter = new FavoriteListAdapter(mContext,
                android.R.layout.simple_list_item_single_choice,
                mFavoriteList);
        mAppDrawerListAdapter = new AppDrawerListAdapter(mContext,
                android.R.layout.simple_list_item_single_choice,
                PackageManager.getInstance(mContext).getPackageList());
        // default on first start
        mShowFavorites = mPrefs.getBoolean(SettingsActivity.PREF_SHOW_FAVORITE, false);
        mActionList = new ArrayList<ImageView>();
        mGestureDetector = new GestureDetector(context, mGestureListener);
        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
    }

    private synchronized void createView() {
        mView = mInflater.inflate(R.layout.recents_list_horizontal, null, false);

        mRecents = (LinearLayout) mView.findViewById(R.id.recents);

        mRecentListHorizontal = (HorizontalListView) mView.findViewById(R.id.recent_list_horizontal);

        mNoRecentApps = (TextView) mView
                .findViewById(R.id.no_recent_apps);

        mRecentListHorizontal
        .setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,
                    View view, int position, long id) {
                TaskDescription task = mRecentsManager.getTasks().get(position);
                mRecentsManager.switchTask(task, mAutoClose, false);
            }
        });

        mRecentListHorizontal
        .setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent,
                    View view, int position, long id) {
                TaskDescription task = mRecentsManager.getTasks().get(position);
                handleLongPressRecent(task, view);
                return true;
            }
        });

        SwipeDismissHorizontalListViewTouchListener touchListener = new SwipeDismissHorizontalListViewTouchListener(
                mRecentListHorizontal,
                new SwipeDismissHorizontalListViewTouchListener.DismissCallbacks() {
                    public void onDismiss(HorizontalListView listView,
                            int[] reverseSortedPositions) {
                        Log.d(TAG, "onDismiss: " + mRecentsManager.getTasks().size() + ":" + reverseSortedPositions[0]);
                        // TODO
                        try {
                            TaskDescription ad = mRecentsManager.getTasks().get(reverseSortedPositions[0]);
                            mRecentsManager.killTask(ad);
                        } catch(IndexOutOfBoundsException e){
                            // ignored
                        }
                    }

                    @Override
                    public boolean canDismiss(int position) {
                        return position < mRecentsManager.getTasks().size();
                    }
                });

        mRecentListHorizontal.setSwipeListener(touchListener);
        mRecentListHorizontal.setAdapter(mRecentListAdapter);

        mOpenFavorite = (ImageView) mView
                .findViewById(R.id.openFavorites);

        mOpenFavorite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleFavorites();
            }
        });

        mOpenFavorite.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(mContext,
                        mContext.getResources().getString(R.string.open_favorite_help), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        mFavoriteListHorizontal = (HorizontalListView) mView
                  .findViewById(R.id.favorite_list_horizontal);

        mFavoriteListHorizontal
        .setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,
                    View view, int position, long id) {
                String intent = mFavoriteList.get(position);
                mRecentsManager.startIntentFromtString(intent, true);
            }
        });
        mFavoriteListHorizontal.setAdapter(mFavoriteListAdapter);

        mFavoriteListHorizontal
        .setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent,
                    View view, int position, long id) {
                String intent = mFavoriteList.get(position);
                PackageManager.PackageItem packageItem = PackageManager.getInstance(mContext).getPackageItem(intent);
                handleLongPressFavorite(packageItem, view);
                return true;
            }
        });

        mRamUsageBarContainer = (LinearLayout) mView.findViewById(R.id.ram_usage_bar_container);
        mRamUsageBar = (LinearColorBar) mView.findViewById(R.id.ram_usage_bar);
        mForegroundProcessText = (TextView) mView
                .findViewById(R.id.foregroundText);
        mBackgroundProcessText = (TextView) mView
                .findViewById(R.id.backgroundText);
        mForegroundProcessText.setTextColor(Color.WHITE);
        mBackgroundProcessText.setTextColor(Color.WHITE);

        mAppDrawer = (GridView) mView.findViewById(R.id.app_drawer);
        mAppDrawer.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                PackageManager.PackageItem packageItem = PackageManager.getInstance(mContext).getPackageList().get(position);
                mRecentsManager.startIntentFromtString(packageItem.getIntent(), true);
            }});

        mAppDrawer.setAdapter(mAppDrawerListAdapter);

        mAppDrawer
        .setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent,
                    View view, int position, long id) {
                PackageManager.PackageItem packageItem = PackageManager.getInstance(mContext).getPackageList().get(position);
                handleLongPressAppDrawer(packageItem, view);
                return true;
            }
        });

        mPopupView = new FrameLayout(mContext);
        mPopupView.addView(mView);

        mView.setOnTouchListener(mDragHandleListener);
        mPopupView.setOnTouchListener(mDragHandleListener);

        mPopupView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(DEBUG){
                    Log.d(TAG, "mPopupView onKey " + keyCode);
                }
                if (!mEnabled) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_BACK &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        event.getRepeatCount() == 0) {
                    if (mShowing) {
                        if (DEBUG) {
                            Log.d(TAG, "onKey");
                        }
                        hide(false);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private synchronized void updateRecentsAppsList(boolean force){
        if(DEBUG){
            Log.d(TAG, "updateRecentsAppsList " + System.currentTimeMillis());
        }
        if(!force && mUpdateNoRecentsTasksDone){
            if(DEBUG){
                Log.d(TAG, "!force && mUpdateNoRecentsTasksDone");
            }
            return;
        }
        if(mNoRecentApps == null || mRecentListHorizontal == null){
            if(DEBUG){
                Log.d(TAG, "mNoRecentApps == null || mRecentListHorizontal == null");
            }
            return;
        }

        if(!mTaskLoadDone){
            if(DEBUG){
                Log.d(TAG, "!mTaskLoadDone");
            }
            return;
        }
        if(DEBUG){
            Log.d(TAG, "updateRecentsAppsList2");
        }
        mRecentListAdapter.notifyDataSetChanged();

        if(mRecentsManager.getTasks().size()!=0){
            mNoRecentApps.setVisibility(View.GONE);
            mRecentListHorizontal.setVisibility(View.VISIBLE);
        } else {
            mNoRecentApps.setVisibility(View.VISIBLE);
            mRecentListHorizontal.setVisibility(View.GONE);
        }
        mUpdateNoRecentsTasksDone = true;
    }

    private synchronized void initView(){
        if (mButtonListContainer != null) {
            mButtonListContainer.setVisibility(View.GONE);
        }
        mButtonList = (HorizontalScrollView) mView.findViewById(mConfiguration.mButtonPos == 0 ?  R.id.button_list_top : R.id.button_list_bottom);
        mButtonList.setHorizontalScrollBarEnabled(false);
        mButtonListItems = (LinearLayout) mView.findViewById(mConfiguration.mButtonPos == 0 ? R.id.button_list_items_top : R.id.button_list_items_bottom);
        mButtonListContainer = (LinearLayout) mView.findViewById(mConfiguration.mButtonPos == 0 ? R.id.button_list_container_top : R.id.button_list_container_bottom);
        mButtonListContainer.setVisibility(View.VISIBLE);

        updateStyle();

        if (mConfiguration.mBgStyle == 0) {
            mView.setBackground(mContext.getResources().getDrawable(R.drawable.overlay_bg_flat));
        } else {
            mView.setBackground(mContext.getResources().getDrawable(R.drawable.overlay_bg));
            if (!mConfiguration.mDimBehind){
                mView.getBackground().setAlpha((int) (255 * mConfiguration.mBackgroundOpacity));
            } else {
                mView.getBackground().setAlpha(0);
            }
        }
        mRamUsageBarContainer.setVisibility(mConfiguration.mShowRambar ? View.VISIBLE : View.GONE);

        mConfiguration.calcHorizontalDivider();

        mFavoriteListHorizontal.setLayoutParams(getListParams());
        mFavoriteListHorizontal.scrollTo(0);
        mFavoriteListHorizontal.setDividerWidth(mConfiguration.mHorizontalDividerWidth);
        mFavoriteListHorizontal.setPadding(mConfiguration.mHorizontalDividerWidth / 2, 0, mConfiguration.mHorizontalDividerWidth / 2, 0);

        mRecentListHorizontal.setLayoutParams(getListParams());
        mRecentListHorizontal.scrollTo(0);
        mRecentListHorizontal.setDividerWidth(mConfiguration.mHorizontalDividerWidth);
        mRecentListHorizontal.setPadding(mConfiguration.mHorizontalDividerWidth / 2, 0, mConfiguration.mHorizontalDividerWidth / 2, 0);

        mNoRecentApps.setLayoutParams(getListParams());
        mAppDrawer.setColumnWidth(mConfiguration.mMaxWidth);
        mAppDrawer.setLayoutParams(getAppDrawerParams());
        mAppDrawer.requestLayout();
        mRecents.setVisibility(View.VISIBLE);
        mShowAppDrawer = false;
        mAppDrawer.setVisibility(View.GONE);
        mAppDrawer.setSelection(0);
        mView.setTranslationX(0);

        if (!mHasFavorites){
            mShowFavorites = false;
        }

        mFavoriteListHorizontal.setVisibility(mShowFavorites ? View.VISIBLE : View.GONE);
        mOpenFavorite.setVisibility(mHasFavorites ? View.VISIBLE : View.GONE);
        mOpenFavorite.setRotation(mShowFavorites ? ROTATE_180_DEGREE : 0);
        mOpenFavorite.setBackgroundResource(mConfiguration.mBgStyle == 0 ? R.drawable.ripple_dark : R.drawable.ripple_light);

        if(mHasFavorites && !mShowcaseDone){
            mOpenFavorite.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mOpenFavorite.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    int[] location = new int[2];
                    mOpenFavorite.getLocationOnScreen(location);
                    mOpenFavoriteY = location[1];
                }
            });
        }

        buildButtons();

        mButtonsVisible = isButtonVisible();

        mVirtualBackKey = false;
    }

    private boolean isButtonVisible(){
        return mButtonListItems.getChildCount() != 0;
    }

    private synchronized void preShow() {
        if (mPopupView == null){
            createView();
        }
        mRecentListAdapter.notifyDataSetChanged();
        initView();

        if(DEBUG){
            Log.d(TAG, "show " + System.currentTimeMillis());
        }

        try {
            mWindowManager.addView(mPopupView, getParams(mConfiguration.mBackgroundOpacity));
        } catch(Exception e){
            // something went wrong - try to recover here
            mWindowManager.removeView(mPopupView);
            mWindowManager.addView(mPopupView, getParams(mConfiguration.mBackgroundOpacity));
        }
    }

    public synchronized void show() {
        if (mShowing) {
            return;
        }

        preShow();

        if (mConfiguration.mAnimate) {
            toggleOverlay(true);
        } else {
            showDone();
        }
    }

    public synchronized void showHidden() {
        if (mShowing) {
            return;
        }

        preShow();

        int startValue = 0;
        if (mConfiguration.mLocation == 0) {
            startValue = mConfiguration.getCurrentOverlayWidth();
        } else {
            startValue = -mConfiguration.getCurrentOverlayWidth();
        }
        mView.setTranslationX(startValue);

        preShowDone();
    }

    private synchronized void showDone(){
        if(DEBUG){
            Log.d(TAG, "showDone " + System.currentTimeMillis());
        }
        preShowDone();
        postShowDone();
    }

    private synchronized void postShowDone() {
        if(DEBUG){
            Log.d(TAG, "postShowDone " + System.currentTimeMillis());
        }
        mRecentsManager.getSwitchGestureView().overlayShown();
        mCurrentDistance = 0;
        mCurrentSlideWidth = 0;
        mEnabled = true;

        if(mHasFavorites && !mShowcaseDone){
            mPopupView.post(new Runnable(){
                @Override
                public void run() {
                    startShowcaseFavorite();
                }});
        }
    }

    private synchronized void preShowDone(){
        if(DEBUG){
            Log.d(TAG, "preShowDone " + System.currentTimeMillis());
        }
        mPopupView.setFocusableInTouchMode(true);
        mHandler.post(updateRamBarTask);
        updateRecentsAppsList(false);

        mShowing = true;
    }

    private synchronized void hideDone() {
        if(DEBUG){
            Log.d(TAG, "hideDone " + System.currentTimeMillis());
        }

        setHandleRecentsUpdate(false);

        try {
            mWindowManager.removeView(mPopupView);
        } catch(Exception e){
            // ignored
        }

        // reset
        mNoRecentApps.setVisibility(View.GONE);
        mRecentListHorizontal.setVisibility(View.VISIBLE);
        mTaskLoadDone = false;
        mUpdateNoRecentsTasksDone = false;
        mAppDrawer.scrollTo(0, 0);

        mRecentsManager.getSwitchGestureView().overlayHidden();

        // run back trigger if required
        if(mVirtualBackKey && !mConfiguration.mRestrictedMode){
            Utils.triggerVirtualKeypress(mHandler, KeyEvent.KEYCODE_BACK);
        }
    }

    public synchronized void preHide() {
        // to prevent any reentering
        mShowing = false;

        if (mPopup != null) {
            mPopup.dismiss();
        }
        try {
            if (mConfiguration.mDimBehind){
                // TODO workaround for flicker on launcher screen
                mWindowManager.updateViewLayout(mPopupView, getParams(0));
            }
        } catch(Exception e){
            // ignored
        }
    }

    public synchronized void hide(boolean fast) {
        if (!mShowing) {
            return;
        }
        preHide();
        if (mConfiguration.mAnimate && !fast) {
            toggleOverlay(false);
        } else {
            hideDone();
        }
    }

    public synchronized void hideHidden() {
        if (!mShowing) {
            return;
        }
        preHide();
        hideDone();
    }

    private LinearLayout.LayoutParams getListParams(){
        return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            mConfiguration.getItemMaxHeight());
    }

    private LinearLayout.LayoutParams getListItemParams(){
        return new LinearLayout.LayoutParams(
            mConfiguration.mMaxWidth,
            mConfiguration.getItemMaxHeight());
    }

    // TODO dont use real icon size values in code
    private int getAppDrawerLines(){
        if (mConfiguration.mIconSize == 40){
            return 5;
        }
        if (mConfiguration.mIconSize == 60){
            return 4;
        }
        return 3;
    }

    private RelativeLayout.LayoutParams getAppDrawerParams(){
        return new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            getAppDrawerLines() * mConfiguration.getItemMaxHeight());
    }

    private WindowManager.LayoutParams getParams(float dimAmount) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                mConfiguration.getCurrentOverlayWidth(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                mConfiguration.mDimBehind ? WindowManager.LayoutParams.FLAG_DIM_BEHIND : 0,
                PixelFormat.TRANSLUCENT);

        // Turn on hardware acceleration for high end gfx devices.
        if (ActivityManager.isHighEndGfx()) {
            params.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
            params.privateFlags |=
                    WindowManager.LayoutParams.PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED;
        }
        if (mConfiguration.mDimBehind){
            params.dimAmount = dimAmount;
        }

        params.gravity = Gravity.TOP | getHorizontalGravity();
        params.y = mConfiguration.getCurrentOffsetStart()
                + mConfiguration.mDragHandleHeight / 2
                - mConfiguration.getItemMaxHeight() / 2
                - (mButtonsVisible ? mConfiguration.getItemMaxHeight() : 0);

        return params;
    }

    private int getHorizontalGravity(){
        if (mConfiguration.mGravity == 0){
            return Gravity.CENTER_HORIZONTAL;
        } else {
            if (mConfiguration.mLocation == 0){
                return Gravity.RIGHT;
            } else {
                return Gravity.LEFT;
            }
        }
    }

    public synchronized void update() {
        if(DEBUG){
            Log.d(TAG, "update " + System.currentTimeMillis() + " " + mRecentsManager.getTasks());
        }

        mTaskLoadDone = true;
        if(isHandleRecentsUpdate()){
            updateRecentsAppsList(true);
        }
    }


    public void refresh() {
        if(DEBUG){
            Log.d(TAG, "refresh");
        }

        mTaskLoadDone = true;
        updateRecentsAppsList(true);
    }

    private void handleLongPressRecent(final TaskDescription ad, View view) {
        final Context wrapper = new ContextThemeWrapper(mContext, R.style.PopupMenu);
        final PopupMenu popup = new PopupMenu(wrapper, view);
        mPopup = popup;
        popup.getMenuInflater().inflate(R.menu.recent_popup_menu,
                popup.getMenu());
        popup.getMenu().findItem(R.id.package_add_favorite).setEnabled(!mFavoriteList.contains(ad.getIntent().toUri(0)));
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.package_stop_task) {
                    mRecentsManager.killTask(ad);
                } else if (item.getItemId() == R.id.package_inspect_item) {
                    mRecentsManager.startApplicationDetailsActivity(ad.getPackageName());
                } else if (item.getItemId() == R.id.package_add_favorite) {
                    Intent intent = ad.getIntent();
                    String intentStr = intent.toUri(0);
                    PackageManager.PackageItem packageItem =
                            PackageManager.getInstance(mContext).getPackageItem(intentStr);
                    if (packageItem == null){
                        // find a matching available package by matching thing like
                        // package name
                        packageItem = PackageManager.getInstance(mContext).getPackageItemByComponent(intent);
                        if (packageItem == null){
                            Log.d(TAG, "failed to add " + intentStr);
                            return false;
                        }
                        intentStr = packageItem.getIntent();
                    }
                    Utils.addToFavorites(mContext, intentStr, mFavoriteList);
                } else {
                    return false;
                }
                return true;
            }
        });
        popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
            public void onDismiss(PopupMenu menu) {
                mPopup = null;
            }
        });
        popup.show();
    }

    private void handleLongPressFavorite(final PackageManager.PackageItem packageItem, View view) {
        final Context wrapper = new ContextThemeWrapper(mContext, R.style.PopupMenu);
        final PopupMenu popup = new PopupMenu(wrapper, view);
        mPopup = popup;
        popup.getMenuInflater().inflate(R.menu.favorite_popup_menu,
                popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.package_inspect_item) {
                    mRecentsManager.startApplicationDetailsActivity(packageItem.getActivityInfo().packageName);
                } else if (item.getItemId() == R.id.package_remove_favorite) {
                    Utils.removeFromFavorites(mContext, packageItem.getIntent(), mFavoriteList);
                } else {
                    return false;
                }
                return true;
            }
        });
        popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
            public void onDismiss(PopupMenu menu) {
                mPopup = null;
            }
        });
        popup.show();
    }

    private void handleLongPressAppDrawer(final PackageManager.PackageItem packageItem, View view) {
        final Context wrapper = new ContextThemeWrapper(mContext, R.style.PopupMenu);
        final PopupMenu popup = new PopupMenu(wrapper, view);
        mPopup = popup;
        popup.getMenuInflater().inflate(R.menu.package_popup_menu,
                popup.getMenu());
        popup.getMenu().findItem(R.id.package_add_favorite).setEnabled(!mFavoriteList.contains(packageItem.getIntent()));
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.package_inspect_item) {
                    mRecentsManager.startApplicationDetailsActivity(packageItem.getActivityInfo().packageName);
                } else if (item.getItemId() == R.id.package_add_favorite) {
                    Log.d(TAG, "add " + packageItem.getIntent());
                    Utils.addToFavorites(mContext, packageItem.getIntent(), mFavoriteList);
                } else {
                    return false;
                }
                return true;
            }
        });
        popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
            public void onDismiss(PopupMenu menu) {
                mPopup = null;
            }
        });
        popup.show();
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        if(DEBUG){
            Log.d(TAG, "updatePrefs");
        }
        mFavoriteList.clear();
        mFavoriteList.addAll(mConfiguration.mFavoriteList);

        List<String> favoriteList = new ArrayList<String>();
        favoriteList.addAll(mFavoriteList);
        Iterator<String> nextFavorite = favoriteList.iterator();
        while(nextFavorite.hasNext()){
            String intent = nextFavorite.next();
            PackageManager.PackageItem packageItem = PackageManager.getInstance(mContext).getPackageItem(intent);
            if (packageItem == null){
                Log.d(TAG, "failed to add " + intent);
                mFavoriteList.remove(intent);
            }
        }
        mHasFavorites = mFavoriteList.size() != 0;
        mFavoriteListAdapter.notifyDataSetChanged();
        if (mFavoriteListHorizontal != null){
            mFavoriteListHorizontal.setAdapter(mFavoriteListAdapter);
        }
        mRecentListAdapter.notifyDataSetChanged();
        if (mRecentListHorizontal != null) {
            mRecentListHorizontal.setAdapter(mRecentListAdapter);
        }
        mAppDrawerListAdapter.notifyDataSetChanged();
        if (mAppDrawer != null) {
            mAppDrawer.setAdapter(mAppDrawerListAdapter);
        }
        buildButtonList();
    }

    private final Runnable updateRamBarTask = new Runnable() {
        @Override
        public void run() {
            if (!mConfiguration.mShowRambar || mRamUsageBar == null) {
                return;
            }
            final ActivityManager am = (ActivityManager) mContext
                    .getSystemService(Context.ACTIVITY_SERVICE);
            MemoryInfo memInfo = new MemoryInfo();
            am.getMemoryInfo(memInfo);

            long availMem = memInfo.availMem;
            long totalMem = memInfo.totalMem;

            String sizeStr = Formatter.formatShortFileSize(mContext, totalMem
                    - availMem);
            mForegroundProcessText.setText(mContext.getResources().getString(
                    R.string.service_foreground_processes, sizeStr));
            sizeStr = Formatter.formatShortFileSize(mContext, availMem);
            mBackgroundProcessText.setText(mContext.getResources().getString(
                    R.string.service_background_processes, sizeStr));

            float fTotalMem = totalMem;
            float fAvailMem = availMem;
            mRamUsageBar.setRatios((fTotalMem - fAvailMem) / fTotalMem, 0, 0);
        }
    };

    public boolean isShowing() {
        return mShowing;
    }

    private boolean startShowcaseFavorite() {
        if (!mPrefs.getBoolean(KEY_SHOWCASE_FAVORITE, false)) {
            mPrefs.edit().putBoolean(KEY_SHOWCASE_FAVORITE, true).commit();
            ShowcaseView.ConfigOptions co = new ShowcaseView.ConfigOptions();
            co.hideOnClickOutside = true;

            Point size = new Point();
            mWindowManager.getDefaultDisplay().getSize(size);

            mShowcaseView = ShowcaseView.insertShowcaseView(size.x / 2, mOpenFavoriteY, mWindowManager, mContext,
                    R.string.sc_favorite_title, R.string.sc_favorite_body, co);

            mShowcaseView.setOnShowcaseEventListener(this);
            mShowcaseDone = true;
            return true;
        }
        mShowcaseDone = true;
        return false;
    }
    @Override
    public void onShowcaseViewHide(ShowcaseView showcaseView) {
    }

    @Override
    public void onShowcaseViewShow(ShowcaseView showcaseView) {
    }
    
    public void updateLayout() {
        try {
            if (mShowing){
                mConfiguration.calcHorizontalDivider();

                mFavoriteListHorizontal.setDividerWidth(mConfiguration.mHorizontalDividerWidth);
                mFavoriteListHorizontal.setPadding(mConfiguration.mHorizontalDividerWidth / 2, 0, mConfiguration.mHorizontalDividerWidth / 2, 0);

                mRecentListHorizontal.setDividerWidth(mConfiguration.mHorizontalDividerWidth);
                mRecentListHorizontal.setPadding(mConfiguration.mHorizontalDividerWidth / 2, 0, mConfiguration.mHorizontalDividerWidth / 2, 0);

                mFavoriteListAdapter.notifyDataSetChanged();
                mFavoriteListHorizontal.setAdapter(mFavoriteListAdapter);

                mRecentListAdapter.notifyDataSetChanged();
                mRecentListHorizontal.setAdapter(mRecentListAdapter);

                mWindowManager.updateViewLayout(mPopupView, getParams(mConfiguration.mBackgroundOpacity));
                mAppDrawer.requestLayout();
            }
        } catch(Exception e){
            // ignored
        }
    }

    private void toggleAppdrawer() {
        mShowAppDrawer = !mShowAppDrawer;
        if (mShowAppDrawer){
            flipToAppDrawerNew();
        } else {
            flipToRecentsNew();
        }
    }

    private PackageTextView getPackageItemTemplate() {
        PackageTextView item = new PackageTextView(mContext);
        if (mConfiguration.mBgStyle == 0){
            item.setTextColor(Color.BLACK);
            item.setShadowLayer(0, 0, 0, Color.BLACK);
        } else {
            item.setTextColor(Color.WHITE);
            item.setShadowLayer(5, 0, 0, Color.BLACK);
        }
        item.setTextSize(mConfiguration.mLabelFontSize);
        item.setEllipsize(TextUtils.TruncateAt.END);
        item.setGravity(Gravity.CENTER);
        item.setLayoutParams(getListItemParams());
        item.setMaxLines(1);
        item.setBackgroundResource(mConfiguration.mBgStyle == 0 ? R.drawable.ripple_dark : R.drawable.ripple_light);
        return item;
    }

    private ImageView getActionButtonTemplate(Drawable image) {
        ImageView item = (ImageView) mInflater.inflate(R.layout.action_button, null, false);
        item.setImageDrawable(image);
        return item;
    }

    private void flipToAppDrawerNew() {
        if (mRecentsAnim != null){
            mRecentsAnim.cancel();
        }
        if (mAppDrawerAnim != null){
            mAppDrawerAnim.cancel();
        }

        // center right
        int cx = (mAppDrawer.getLeft() + mAppDrawer.getRight() / 2);
        int cy = (mAppDrawer.getTop() + mAppDrawer.getBottom() / 2);

        // get the final radius for the clipping circle
        int finalRadius = Math.max(mAppDrawer.getWidth(), mAppDrawer.getHeight());

        // create the animator for this view (the start radius is zero)
        mAppDrawerAnim =
            ViewAnimationUtils.createCircularReveal(mAppDrawer, cx, cy, 0, finalRadius);
        mAppDrawerAnim.setDuration(FLIP_DURATION);
        mAppDrawer.setVisibility(View.VISIBLE);

        mRecents.animate().alpha(0f).setDuration(FLIP_DURATION / 2).withEndAction(new Runnable(){
            @Override
            public void run() {
                mRecents.setVisibility(View.GONE);
                mRecents.setAlpha(1f);
        }});

        mAppDrawerAnim.start();
    }

    private void flipToRecentsNew() {
        if (mRecentsAnim != null){
            mRecentsAnim.cancel();
        }
        if (mAppDrawerAnim != null){
            mAppDrawerAnim.cancel();
        }

        // center
        int cx = (mRecents.getLeft() + mRecents.getRight() / 2);
        int cy = (mRecents.getTop() + mRecents.getBottom() / 2);

        // get the final radius for the clipping circle
        int finalRadius = Math.max(mRecents.getWidth(), mRecents.getHeight());

        // create the animator for this view (the start radius is zero)
        mRecentsAnim =
            ViewAnimationUtils.createCircularReveal(mRecents, cx, cy, 0, finalRadius);
        mRecentsAnim.setDuration(FLIP_DURATION);
        mRecents.setVisibility(View.VISIBLE);

        mAppDrawer.animate().alpha(0f).setDuration(FLIP_DURATION / 2).withEndAction(new Runnable(){
            @Override
            public void run() {
                mAppDrawer.setVisibility(View.GONE);
                mAppDrawer.setAlpha(1f);
        }});

        mRecentsAnim.start();
    }

    private void toggleFavorites() {
        mShowFavorites = !mShowFavorites;

        if (mShowFavAnim != null){
            mShowFavAnim.cancel();
        }

        if (mShowFavorites){
            mFavoriteListHorizontal.setVisibility(View.VISIBLE);
            mFavoriteListHorizontal.setScaleY(0f);
            mFavoriteListHorizontal.setPivotY(0f);
            mShowFavAnim = start(interpolator(mLinearInterpolator,
                            ObjectAnimator.ofFloat(mFavoriteListHorizontal, View.SCALE_Y, 0f, 1f))
                            .setDuration(FLIP_DURATION_DEFAULT));
        } else {
            mFavoriteListHorizontal.setScaleY(1f);
            mFavoriteListHorizontal.setPivotY(0f);
            mShowFavAnim = start(setVisibilityWhenDone(
                    interpolator(mLinearInterpolator,
                            ObjectAnimator.ofFloat(mFavoriteListHorizontal, View.SCALE_Y, 1f, 0f))
                            .setDuration(FLIP_DURATION_DEFAULT), mFavoriteListHorizontal, View.GONE));
        }

        mShowFavAnim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mOpenFavorite.setRotation(mShowFavorites ? ROTATE_180_DEGREE : 0);
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

    private void toggleOverlay(final boolean show) {
        if (mToggleOverlayAnim != null){
            mToggleOverlayAnim.cancel();
        }

        if (show){
            int startValue = 0;
            if (mConfiguration.mLocation == 0) {
                startValue = mConfiguration.getCurrentOverlayWidth();
            } else {
                startValue = -mConfiguration.getCurrentOverlayWidth();
            }
            mView.setTranslationX(startValue);
            mToggleOverlayAnim = start(interpolator(mLinearInterpolator,
                    ObjectAnimator.ofFloat(mView, View.TRANSLATION_X, startValue, 0))
                    .setDuration(SHOW_DURATION));
        } else {
            int endValue = 0;
            if (mConfiguration.mLocation == 0) {
                endValue = mConfiguration.getCurrentOverlayWidth();
            } else {
                endValue = -mConfiguration.getCurrentOverlayWidth();
            }
            mView.setTranslationX(0);
            mToggleOverlayAnim = start(interpolator(mLinearInterpolator,
                    ObjectAnimator.ofFloat(mView, View.TRANSLATION_X, 0, endValue))
                    .setDuration(HIDE_DURATION));
        }

        mToggleOverlayAnim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!show){
                    hideDone();
                } else {
                    showDone();
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

    private void finishOverlaySlide(final boolean show, boolean fromFling) {
        if (DEBUG) {
            Log.d(TAG, "finishOverlaySlide " + show + " " + fromFling + " " + mCurrentSlideWidth);
        }
        if (mToggleOverlayAnim != null){
            mToggleOverlayAnim.cancel();
        }
        if (mCurrentDistance < mSlop) {
            return;
        }

        mEnabled = false;

        if (show){
            mToggleOverlayAnim = start(interpolator(mLinearInterpolator,
                    ObjectAnimator.ofFloat(mView, View.TRANSLATION_X, mCurrentSlideWidth, 0))
                    .setDuration(fromFling ? SHOW_DURATION_FAST : SHOW_DURATION));
        } else {
            int endValue = 0;
            if (mConfiguration.mLocation == 0) {
                endValue = mConfiguration.getCurrentOverlayWidth();
            } else {
                endValue = -mConfiguration.getCurrentOverlayWidth();
            }
            mToggleOverlayAnim = start(interpolator(mLinearInterpolator,
                    ObjectAnimator.ofFloat(mView, View.TRANSLATION_X, mCurrentSlideWidth, endValue))
                    .setDuration(fromFling ? HIDE_DURATION_FAST : HIDE_DURATION));
        }

        mToggleOverlayAnim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!show){
                    hideHidden();
                } else {
                    postShowDone();
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

    private Animator setVisibilityWhenDone(
            final Animator a, final View v, final int vis) {
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                v.setVisibility(vis);
            }
        });
        return a;
    }

    private Animator interpolator(TimeInterpolator ti, Animator a) {
        a.setInterpolator(ti);
        return a;
    }

    private Animator startDelay(int d, Animator a) {
        a.setStartDelay(d);
        return a;
    }

    private Animator start(Animator a) {
        a.start();
        return a;
    }

    public void shutdownService() {
        // remember on reboot
        mPrefs.edit().putBoolean(SettingsActivity.PREF_SHOW_FAVORITE, mShowFavorites).commit();
    }

    private ImageView getActionButton(int buttonId) {
        if (buttonId == SettingsActivity.BUTTON_HOME){
            mHomeButton = getActionButtonTemplate(mContext.getResources().getDrawable(R.drawable.ic_sysbar_home));
            mHomeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.goHome(mAutoClose);
                }
            });
            mHomeButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(mContext,
                            mContext.getResources().getString(R.string.home_help), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            return mHomeButton;
        }

        if (buttonId == SettingsActivity.BUTTON_TOGGLE_APP){
            mLastAppButton = getActionButtonTemplate(mContext.getResources().getDrawable(R.drawable.lastapp));
            mLastAppButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.toggleLastApp(mAutoClose);
                }
            });
            mLastAppButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(mContext,
                            mContext.getResources().getString(R.string.toggle_last_app_help), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            return mLastAppButton;
        }

        if (buttonId == SettingsActivity.BUTTON_KILL_ALL){
            mKillAllButton = getActionButtonTemplate(mContext.getResources().getDrawable(R.drawable.kill_all));
            mKillAllButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.killAll(mAutoClose);
                }
            });

            mKillAllButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(mContext,
                            mContext.getResources().getString(R.string.kill_all_apps_help), Toast.LENGTH_SHORT).show();                return true;
                }
            });
            return mKillAllButton;
        }

        if (buttonId == SettingsActivity.BUTTON_KILL_OTHER){
            mKillOtherButton = getActionButtonTemplate(mContext.getResources().getDrawable(R.drawable.kill_other));
            mKillOtherButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.killOther(mAutoClose);
                }
            });
            mKillOtherButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(mContext,
                            mContext.getResources().getString(R.string.kill_other_apps_help), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            return mKillOtherButton;
        }

        if (buttonId == SettingsActivity.BUTTON_SETTINGS){
            mSettingsButton = getActionButtonTemplate(mContext.getResources().getDrawable(R.drawable.settings));
            mSettingsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.startSettingssActivity();
                }
            });
            mSettingsButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mRecentsManager.startOmniSwitchSettingsActivity();
                    return true;
                }
            });
            return mSettingsButton;
        }

        if (buttonId == SettingsActivity.BUTTON_ALLAPPS){
            mAllappsButton = getActionButtonTemplate(mContext.getResources().getDrawable(R.drawable.ic_allapps));
            mAllappsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleAppdrawer();
                }
            });
            mAllappsButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(mContext,
                            mContext.getResources().getString(R.string.allapps_help), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            return mAllappsButton;
        }

        if (buttonId == SettingsActivity.BUTTON_BACK){
            mBackButton = getActionButtonTemplate(mContext.getResources().getDrawable(R.drawable.ic_sysbar_back));
            mBackButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mVirtualBackKey = true;
                    hide(false);
                }
            });
            mBackButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(mContext,
                            mContext.getResources().getString(R.string.back_help), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            return mBackButton;
        }

        if (buttonId == SettingsActivity.BUTTON_LOCK_APP){
            mLockToAppButton = getActionButtonTemplate(mContext.getResources().getDrawable(R.drawable.lock_app_pin));
            mLockToAppButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Utils.toggleLockModeOnCurrent(mContext);
                }
            });
            mLockToAppButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(mContext,
                            mContext.getResources().getString(R.string.lock_app_help), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            return mLockToAppButton;
        }
        return null;
    }

    private void buildButtonList(){
        mActionList.clear();
        Iterator<Integer> nextKey = mConfiguration.mButtons.keySet().iterator();
        while(nextKey.hasNext()){
            Integer key = nextKey.next();
            Boolean value = mConfiguration.mButtons.get(key);
            if (value){
                ImageView item = getActionButton(key);
                if (item != null){
                    mActionList.add(item);
                }
            }
        }
    }

    private void buildButtons(){
        mButtonListItems.removeAllViews();
        int buttonMargin = Math.round(5 * mConfiguration.mDensity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(buttonMargin, 0, buttonMargin, 0);
        Iterator<ImageView> nextButton = mActionList.iterator();
        while(nextButton.hasNext()){
            ImageView item = nextButton.next();
            mButtonListItems.addView(item, params);
        }
    }

    public void setHandleRecentsUpdate(boolean handleRecentsUpdate) {
        mHandleRecentsUpdate = handleRecentsUpdate;
    }

    public boolean isHandleRecentsUpdate() {
        return mHandleRecentsUpdate;
    }

    private void updateStyle() {
        if (mConfiguration.mBgStyle == 0){
            mButtonListContainer.setBackground(mContext.getResources().getDrawable(R.drawable.overlay_bg_button_flat));
            mForegroundProcessText.setShadowLayer(0, 0, 0, Color.BLACK);
            mBackgroundProcessText.setShadowLayer(0, 0, 0, Color.BLACK);
            mNoRecentApps.setTextColor(Color.BLACK);
            mNoRecentApps.setShadowLayer(0, 0, 0, Color.BLACK);
            mOpenFavorite.setImageDrawable(BitmapUtils.colorize(mContext.getResources(), mContext.getResources().getColor(R.color.button_bg_flat_color), mContext.getResources().getDrawable(R.drawable.ic_expand_down)));
            mRamUsageBarContainer.setOutlineProvider(BAR_OUTLINE_PROVIDER);
            mButtonListContainer.setOutlineProvider(BUTTON_OUTLINE_PROVIDER);
        } else {
            mButtonListContainer.setBackground(null);
            mForegroundProcessText.setShadowLayer(5, 0, 0, Color.BLACK);
            mBackgroundProcessText.setShadowLayer(5, 0, 0, Color.BLACK);
            mNoRecentApps.setTextColor(Color.WHITE);
            mNoRecentApps.setShadowLayer(5, 0, 0, Color.BLACK);
            mOpenFavorite.setImageDrawable(BitmapUtils.shadow(mContext.getResources(), mContext.getResources().getDrawable(R.drawable.ic_expand_down)));
            mRamUsageBarContainer.setOutlineProvider(null);
            mButtonListContainer.setOutlineProvider(null);
        }
    }

    public void finishSlideLayout() {
        if (DEBUG) {
            Log.d(TAG, "finishSlideLayout " + mCurrentDistance);
        }
        if (mCurrentDistance > mSlop) {
            if (mCurrentDistance > mConfiguration.getCurrentDisplayWidth() / 2) {
                finishOverlaySlide(true, false);
            } else {
                finishOverlaySlide(false, false);
            }
        }
    }

    public void finishSlideLayoutHide() {
        if (DEBUG) {
            Log.d(TAG, "finishSlideLayoutHide " + mCurrentDistance);
        }
        if (mCurrentDistance > mSlop) {
            if (mCurrentDistance > mConfiguration.getCurrentDisplayWidth() / 2) {
                finishOverlaySlide(false, false);
            } else {
                finishOverlaySlide(true, false);
            }
        }
    }

    public void openSlideLayout(boolean fromFling) {
        finishOverlaySlide(true, fromFling);
    }

    public void canceSlideLayout() {
        finishOverlaySlide(false, false);
    }

    public void slideLayout(float distanceX) {
        if (DEBUG) {
            Log.d(TAG, "slideLayout " + distanceX);
        }
        mCurrentDistance = Math.abs(distanceX);
        if (mConfiguration.mLocation == 0) {
            mCurrentSlideWidth = mConfiguration.getCurrentOverlayWidth() - distanceX;
        } else {
            mCurrentSlideWidth = -mConfiguration.getCurrentOverlayWidth() + distanceX;
        }
        mView.setTranslationX(mCurrentSlideWidth);
    }

    public void slideLayoutHide(float distanceX) {
        if (DEBUG) {
            Log.d(TAG, "slideLayoutHide " + distanceX);
        }
        mCurrentDistance = Math.abs(distanceX);
        mCurrentSlideWidth = distanceX;
        mView.setTranslationX(mCurrentSlideWidth);
    }
}
