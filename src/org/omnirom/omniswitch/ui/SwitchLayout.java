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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SwitchLayout implements OnShowcaseEventListener {
    private static final String TAG = "SwitchLayout";
    private static final boolean DEBUG = false;
    private static final String KEY_SHOWCASE_FAVORITE = "showcase_favorite_done";

    private static final int FLIP_DURATION_OUT = 125;
    private static final int FLIP_DURATION_IN = 225;
    private static final int FLIP_DURATION_DEFAULT = 200;
    private static final int SHOW_DURATION = 200;
    private static final int HIDE_DURATION = 100;

    private WindowManager mWindowManager;
    private LayoutInflater mInflater;
    private HorizontalListView mRecentListHorizontal;
    private HorizontalListView mFavoriteListHorizontal;
    private PackageTextView mLastAppButton;
    private PackageTextView mKillAllButton;
    private PackageTextView mKillOtherButton;
    private PackageTextView mHomeButton;
    private PackageTextView mSettingsButton;
    private PackageTextView mAllappsButton;
    private PackageTextView mBackButton;
    private PackageTextView mImmersiveModeButton;
    private RecentListAdapter mRecentListAdapter;
    private FavoriteListAdapter mFavoriteListAdapter;
    private List<TaskDescription> mLoadedTasks;
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
    private ImageButton mOpenFavorite;
    private boolean mHasFavorites;
    private TextView mNoRecentApps;
    private boolean mTaskLoadDone;
    private boolean mUpdateNoRecentsTasksDone;
    private boolean mButtonsVisible = true;
    private Drawable[] mFavoriteDrawableDefault = new Drawable[2]; // 0 is down 1 is up
    private Drawable[] mFavoriteDrawableGlow = new Drawable[2]; // 0 is down 1 is up
    private boolean mAutoClose = true;
    private boolean mShowAppDrawer;
    private GridView mAppDrawer;
    private AppDrawerListAdapter mAppDrawerListAdapter;
    private View mCurrentSelection;
    private LinearLayout mRamUsageBarContainer;
    private GlowSelector mPendingGlowSelector;
    private LinearLayout mRecents;
    private Animator mAppDrawerAnim;
    private Animator mRecentsAnim;
    private TimeInterpolator mAccelerateInterpolator = new AccelerateInterpolator();
    private TimeInterpolator mDecelerateInterpolator = new DecelerateInterpolator();
    private Animator mShowFavAnim;
    private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private Animator mToggleOverlayAnim;
    private boolean mVirtualBackKey;
    private ButtonScrollView mButtonList;
    private LinearLayout mButtonListItems;
    private List<PackageTextView> mActionList;

    private class RecentListAdapter extends ArrayAdapter<TaskDescription> {

        public RecentListAdapter(Context context, int resource,
                List<TaskDescription> values) {
            super(context, R.layout.package_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TaskDescription ad = mLoadedTasks.get(position);
            final PackageTextView item = getPackageItemTemplate();

            if (mConfiguration.mShowLabels) {
                item.setText(ad.getLabel());
            }

            Drawable d = BitmapCache.getInstance(mContext).getResized(mContext.getResources(), ad, ad.getIcon(), mConfiguration, mConfiguration.mIconSize);
            item.setOriginalImage(d);
            item.setBackground(d);

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
            final PackageTextView item = getPackageItemTemplate();
            String intent = mFavoriteList.get(position);

            PackageManager.PackageItem packageItem = PackageManager.getInstance(mContext).getPackageItem(intent);
            item.setIntent(packageItem.getIntent());
            if (mConfiguration.mShowLabels) {
                item.setText(packageItem.getTitle());
            }
            Drawable d = BitmapCache.getInstance(mContext).getResized(mContext.getResources(), packageItem, mConfiguration, mConfiguration.mIconSize);
            item.setOriginalImage(d);
            item.setBackground(d);
            return item;
        }
    }

    private class AppDrawerListAdapter extends ArrayAdapter<PackageManager.PackageItem> implements OnTouchListener {

        public AppDrawerListAdapter(Context context, int resource,
                List<PackageManager.PackageItem> values) {
            super(context, R.layout.package_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final PackageTextView item = getPackageItemTemplate();

            PackageManager.PackageItem packageItem = PackageManager.getInstance(mContext).getPackageList().get(position);
            item.setIntent(packageItem.getIntent());
            if (mConfiguration.mShowLabels) {
                item.setText(packageItem.getTitle());
            }
            Drawable d = BitmapCache.getInstance(mContext).getResized(mContext.getResources(), packageItem, mConfiguration, mConfiguration.mIconSize);
            item.setOriginalImage(d);
            item.setBackground(d);
            item.setOnTouchListener(this);
            return item;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                mSelectionGlowListener.onItemSelected(v, true);
            } else {
                mSelectionGlowListener.onItemSelected(v, false);
            }
            return false;
        }
    }

    private HorizontalListView.SelectionListener mSelectionGlowListener = new HorizontalListView.SelectionListener() {
        @Override
        public void onItemSelected(View view, boolean selected) {
            PackageTextView textView = (PackageTextView)view;
            mCurrentSelection = textView;

            if(selected){
                if (mPendingGlowSelector == null) {
                    mPendingGlowSelector = new GlowSelector();
                }
                // TODO
                //int tapTimeout = ViewConfiguration.getTapTimeout() / 2;
                mView.postDelayed(mPendingGlowSelector, 50);
            } else {
                mView.removeCallbacks(mPendingGlowSelector);
                textView.setBackground(textView.getOriginalImage());
            }
        }
    };

    final class GlowSelector implements Runnable {

        @Override
        public void run() {
            PackageTextView textView = (PackageTextView)mCurrentSelection;

            textView.setBackground(BitmapUtils.glow(mContext.getResources(),
                            mConfiguration.mGlowColor,
                            textView.getBackground()));
        }
    };

    public void setRecentsManager(SwitchManager manager) {
        mRecentsManager = manager;
    }

    public SwitchLayout(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mConfiguration = SwitchConfiguration.getInstance(mContext);

        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLoadedTasks = new ArrayList<TaskDescription>();
        mRecentListAdapter = new RecentListAdapter(mContext,
                android.R.layout.simple_list_item_single_choice,
                mLoadedTasks);
        mFavoriteList = new ArrayList<String>();
        mFavoriteListAdapter = new FavoriteListAdapter(mContext,
                android.R.layout.simple_list_item_single_choice,
                mFavoriteList);
        mAppDrawerListAdapter = new AppDrawerListAdapter(mContext,
                android.R.layout.simple_list_item_single_choice,
                PackageManager.getInstance(mContext).getPackageList());
        // default on first start
        mShowFavorites = mPrefs.getBoolean(SettingsActivity.PREF_SHOW_FAVORITE, false);
        mActionList = new ArrayList<PackageTextView>();
    }

    private synchronized void createView() {
        mView = mInflater.inflate(R.layout.recents_list_horizontal, null, false);
        mView.setBackground(mContext.getResources().getDrawable(R.drawable.overlay_bg));

        mRecents = (LinearLayout) mView.findViewById(R.id.recents);

        mRecentListHorizontal = (HorizontalListView) mView
                .findViewById(R.id.recent_list_horizontal);
        mRecentListHorizontal.setDividerWidth(10);
        
        mNoRecentApps = (TextView) mView
                .findViewById(R.id.no_recent_apps);

        mButtonList = (ButtonScrollView) mView.findViewById(R.id.button_list);
        mButtonList.setHorizontalScrollBarEnabled(false);
        mButtonListItems = (LinearLayout) mView.findViewById(R.id.button_list_items);
        mRecentListHorizontal.setSelectionListener(mSelectionGlowListener);

        mRecentListHorizontal
        .setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,
                    View view, int position, long id) {
                TaskDescription task = mLoadedTasks.get(position);
                mRecentsManager.switchTask(task, mAutoClose);
            }
        });

        mRecentListHorizontal
        .setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent,
                    View view, int position, long id) {
                TaskDescription task = mLoadedTasks.get(position);
                handleLongPress(task, view);
                return true;
            }
        });

        SwipeDismissHorizontalListViewTouchListener touchListener = new SwipeDismissHorizontalListViewTouchListener(
                mRecentListHorizontal,
                new SwipeDismissHorizontalListViewTouchListener.DismissCallbacks() {
                    public void onDismiss(HorizontalListView listView,
                            int[] reverseSortedPositions) {
                        Log.d(TAG, "onDismiss: " + mLoadedTasks.size() + ":" + reverseSortedPositions[0]);
                        // TODO
                        try {
                            TaskDescription ad = mLoadedTasks.get(reverseSortedPositions[0]);
                            mRecentsManager.killTask(ad);
                        } catch(IndexOutOfBoundsException e){
                            // ignored
                        }
                    }

                    @Override
                    public boolean canDismiss(int position) {
                        return position < mLoadedTasks.size();
                    }
                });

        mRecentListHorizontal.setSwipeListener(touchListener);
        mRecentListHorizontal.setAdapter(mRecentListAdapter);

        mOpenFavorite = (ImageButton) mView
                .findViewById(R.id.openFavorites);

        mFavoriteDrawableDefault[0] = BitmapUtils.shadow(mContext.getResources(), mContext.getResources().getDrawable(R.drawable.arrow_up));
        mFavoriteDrawableDefault[1] = BitmapUtils.shadow(mContext.getResources(), mContext.getResources().getDrawable(R.drawable.arrow_down));
        mFavoriteDrawableGlow[0] = BitmapUtils.glow(mContext.getResources(),
                mConfiguration.mGlowColor,
                mContext.getResources().getDrawable(R.drawable.arrow_up));
        mFavoriteDrawableGlow[1] = BitmapUtils.glow(mContext.getResources(),
                mConfiguration.mGlowColor,
                mContext.getResources().getDrawable(R.drawable.arrow_down));

        mOpenFavorite.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN){
                    setButtonGlow(mOpenFavorite,
                            mShowFavorites ? 0 : 1,
                                    true);

                    toggleFavorites();
                }
                return true;
            }});

        mFavoriteListHorizontal = (HorizontalListView) mView
                  .findViewById(R.id.favorite_list_horizontal);
        mFavoriteListHorizontal.setDividerWidth(10);

        mFavoriteListHorizontal.setSelectionListener(mSelectionGlowListener);

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
                handleLongPress(packageItem, view);
                return true;
            }
        });

        // touches inside the overlay should not hide it
        mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mRamUsageBarContainer = (LinearLayout) mView.findViewById(R.id.ram_usage_bar_container);
        mRamUsageBar = (LinearColorBar) mView.findViewById(R.id.ram_usage_bar);
        mForegroundProcessText = (TextView) mView
                .findViewById(R.id.foregroundText);
        mBackgroundProcessText = (TextView) mView
                .findViewById(R.id.backgroundText);

        mAppDrawer = (GridView) mView.findViewById(R.id.app_drawer);
        mAppDrawer.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                PackageManager.PackageItem packageItem = PackageManager.getInstance(mContext).getPackageList().get(position);
                mRecentsManager.startIntentFromtString(packageItem.getIntent(), true);
            }});
        mAppDrawer.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP){
                    if (mCurrentSelection != null){
                        mSelectionGlowListener.onItemSelected(mCurrentSelection, false);
                        mCurrentSelection = null;
                    }
                }
                return false;
            }});
        mAppDrawer.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (mCurrentSelection != null){
                    mSelectionGlowListener.onItemSelected(mCurrentSelection, false);
                    mCurrentSelection = null;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {
            }});

        mAppDrawer.setAdapter(mAppDrawerListAdapter);

        mAppDrawer
        .setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent,
                    View view, int position, long id) {
                PackageManager.PackageItem packageItem = PackageManager.getInstance(mContext).getPackageList().get(position);
                handleLongPress(packageItem, view);
                return true;
            }
        });

        mPopupView = new FrameLayout(mContext);
        mPopupView.addView(mView);

        mPopupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (mShowing) {
                        if (DEBUG) {
                            Log.d(TAG, "onTouch");
                        }
                        mRecentsManager.close();
                        return true;
                    }
                }
                return false;
            }
        });
        mPopupView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        event.getRepeatCount() == 0) {
                    if (mShowing) {
                        if (DEBUG) {
                            Log.d(TAG, "onKey");
                        }
                        mRecentsManager.close();
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private synchronized void updateRecentsAppsList(boolean force){
        if(!force && mUpdateNoRecentsTasksDone){
            return;
        }
        if(mNoRecentApps == null || mRecentListHorizontal == null){
            return;
        }

        if(!mTaskLoadDone){
            return;
        }
        if(DEBUG){
            Log.d(TAG, "updateRecentsAppsList " + System.currentTimeMillis());
        }
        mRecentListAdapter.notifyDataSetChanged();

        if(mLoadedTasks.size()!=0){
            mNoRecentApps.setVisibility(View.GONE);
            mRecentListHorizontal.setVisibility(View.VISIBLE);
        } else {
            mNoRecentApps.setVisibility(View.VISIBLE);
            mRecentListHorizontal.setVisibility(View.GONE);
        }
        mUpdateNoRecentsTasksDone = true;
    }

    private synchronized void initView(){
        if (!mConfiguration.mDimBehind){
            if (mConfiguration.mGravity == 0){
                mView.setBackground(mContext.getResources().getDrawable(R.drawable.overlay_bg));
            } else {
                if (mConfiguration.mLocation == 0){
                    mView.setBackground(mContext.getResources().getDrawable(R.drawable.overlay_bg_right));
                } else {
                    mView.setBackground(mContext.getResources().getDrawable(R.drawable.overlay_bg_left));
                }
            }
            mView.getBackground().setAlpha((int) (255 * mConfiguration.mBackgroundOpacity));
        } else {
            mView.getBackground().setAlpha(0);
        }
        mRamUsageBarContainer.setVisibility(mConfiguration.mShowRambar ? View.VISIBLE : View.GONE);
        mFavoriteListHorizontal.setLayoutParams(getListviewParams());
        mFavoriteListHorizontal.scrollTo(0);
        mRecentListHorizontal.setLayoutParams(getListviewParams());
        mRecentListHorizontal.scrollTo(0);
        mNoRecentApps.setLayoutParams(getListviewParams());
        // TODO
        mAppDrawer.setColumnWidth(mConfiguration.mMaxWidth);
        mAppDrawer.setLayoutParams(getAppDrawerParams());
        mAppDrawer.requestLayout();

        mAppDrawer.setScaleX(0f);
        mRecents.setScaleX(1f);
        mRecents.setVisibility(View.VISIBLE);
        mShowAppDrawer = false;
        mAppDrawer.setVisibility(View.GONE);
        mAppDrawer.setSelection(0);

        mView.setScaleX(1f);

        mOpenFavorite.setVisibility(mHasFavorites ? View.VISIBLE : View.GONE);
        if (!mHasFavorites){
            mShowFavorites = false;
        }
        mOpenFavorite.setImageDrawable(mShowFavorites ? mFavoriteDrawableDefault[0] : mFavoriteDrawableDefault[1]);
        mFavoriteListHorizontal.setVisibility(mShowFavorites ? View.VISIBLE : View.GONE);

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
        // reset any glow images
        resetButtonGlow();

        mButtonsVisible = isButtonVisible();

        mVirtualBackKey = false;
    }

    private boolean isButtonVisible(){
        return mButtonListItems.getChildCount() != 0;
    }

    public synchronized void show() {
        if (mShowing) {
            return;
        }

        if (mPopupView == null){
            createView();
        }
        
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
        

        if (mConfiguration.mAnimate) {
            toggleOverlay(true);
        } else {
            showDone();
        }
    }

    private synchronized void showDone(){
        if(DEBUG){
            Log.d(TAG, "showDone " + System.currentTimeMillis());
        }
        mPopupView.setFocusableInTouchMode(true);
        mHandler.post(updateRamBarTask);
        updateRecentsAppsList(false);

        mShowing = true;
        mRecentsManager.getSwitchGestureView().overlayShown();

        if(mHasFavorites && !mShowcaseDone){
            mPopupView.post(new Runnable(){
                @Override
                public void run() {
                    startShowcaseFavorite();
                }});
        }
    }

    private synchronized void hideDone() {
        if(DEBUG){
            Log.d(TAG, "hideDone " + System.currentTimeMillis());
        }

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
        mLoadedTasks.clear();
        mRecentListAdapter.notifyDataSetChanged();
        mAppDrawer.scrollTo(0, 0);

        mRecentsManager.getSwitchGestureView().overlayHidden();

        // run back trigger if required
        if(mVirtualBackKey && !mConfiguration.mRestrictedMode){
            Utils.triggerVirtualKeypress(mHandler, KeyEvent.KEYCODE_BACK);
        }
    }

    public synchronized void hide() {
        if (!mShowing) {
            return;
        }

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

        if (mConfiguration.mAnimate) {
            toggleOverlay(false);
        } else {
            hideDone();
        }
    }

    private LinearLayout.LayoutParams getListviewParams(){
        return new LinearLayout.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            mConfiguration.mMaxHeight);
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
            WindowManager.LayoutParams.MATCH_PARENT,
            getAppDrawerLines() * mConfiguration.mMaxHeight);
    }

    private WindowManager.LayoutParams getParams(float dimAmount) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                mConfiguration.getCurrentOverlayWidth(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                mConfiguration.mDimBehind ? WindowManager.LayoutParams.FLAG_DIM_BEHIND : 0,
                PixelFormat.TRANSLUCENT);

        if (mConfiguration.mDimBehind){
            params.dimAmount = dimAmount;
        }

        params.gravity = Gravity.TOP | getHorizontalGravity();
        params.y = mConfiguration.getCurrentOffsetStart() 
                + mConfiguration.mDragHandleHeight / 2
                - mConfiguration.mMaxHeight / 2
                - (mButtonsVisible ? mConfiguration.mMaxHeight : 0);

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

    public synchronized void update(List<TaskDescription> taskList) {
        if(DEBUG){
            Log.d(TAG, "update " + System.currentTimeMillis());
        }
        mLoadedTasks.clear();
        mLoadedTasks.addAll(taskList);

        mTaskLoadDone = true;
        updateRecentsAppsList(false);
    }

    public void refresh(List<TaskDescription> taskList) {
        if(DEBUG){
            Log.d(TAG, "refresh");
        }
        mLoadedTasks.clear();
        mLoadedTasks.addAll(taskList);

        mTaskLoadDone = true;
        updateRecentsAppsList(true);
    }

    private void handleLongPress(final TaskDescription ad, View view) {
        final PopupMenu popup = new PopupMenu(mContext, view);
        mPopup = popup;
        popup.getMenuInflater().inflate(R.menu.recent_popup_menu,
                popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.recent_remove_item) {
                    mRecentsManager.killTask(ad);
                } else if (item.getItemId() == R.id.recent_inspect_item) {
                    mRecentsManager.startApplicationDetailsActivity(ad.getPackageName());
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

    private void handleLongPress(final PackageManager.PackageItem packageItem, View view) {
        final PopupMenu popup = new PopupMenu(mContext, view);
        mPopup = popup;
        popup.getMenuInflater().inflate(R.menu.package_popup_menu,
                popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.package_inspect_item) {
                    mRecentsManager.startApplicationDetailsActivity(packageItem.getActivityInfo().packageName);
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
        String favoriteListString = prefs.getString(SettingsActivity.PREF_FAVORITE_APPS, "");
        Utils.parseFavorites(favoriteListString, mFavoriteList);

        mHasFavorites = mFavoriteList.size() != 0;
        mFavoriteListAdapter.notifyDataSetChanged();
        mRecentListAdapter.notifyDataSetChanged();
        mAppDrawerListAdapter.notifyDataSetChanged();
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
                mWindowManager.updateViewLayout(mPopupView, getParams(mConfiguration.mBackgroundOpacity));
                mAppDrawer.requestLayout();
            }
        } catch(Exception e){
            // ignored
        }
    }
    
    private void setButtonGlow(ImageButton button, int state, boolean enable){
        if(enable){
            if(state ==0){
                button.setImageDrawable(mFavoriteDrawableGlow[0]);
            } else {
                button.setImageDrawable(mFavoriteDrawableGlow[1]);
            }
        } else {
            if(state ==0){
                button.setImageDrawable(mFavoriteDrawableDefault[0]);
            } else {
                button.setImageDrawable(mFavoriteDrawableDefault[1]);
            }
        }
    }

    private void toggleAppdrawer() {
        mShowAppDrawer = !mShowAppDrawer;
        if (mShowAppDrawer){
            flipToAppDrawer();
        } else {
            flipToRecents();
        }
    }

    private PackageTextView getPackageItemTemplate() {
        PackageTextView item = new PackageTextView(mContext);
        item.setTextColor(Color.WHITE);
        item.setShadowLayer(5, 0, 0, Color.BLACK);
        item.setEllipsize(TextUtils.TruncateAt.END);
        item.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);
        item.setMaxHeight(mConfiguration.mMaxHeight);
        item.setMaxWidth(mConfiguration.mMaxWidth);
        item.setMaxLines(1);
        return item;
    }

    private PackageTextView getPackageItemActionTemplate() {
        PackageTextView item = new PackageTextView(mContext);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(10, 0, 10, 0);
        return item;
    }

    private void flipToAppDrawer() {
        if (mRecentsAnim != null){
            mRecentsAnim.cancel();
        }
        if (mAppDrawerAnim != null){
            mAppDrawerAnim.cancel();
        }
        mAppDrawer.setVisibility(View.VISIBLE);
        mAppDrawer.setScaleX(0f);
        mRecents.setScaleX(1f);

        mAppDrawerAnim = start(startDelay(
                FLIP_DURATION_OUT,
                interpolator(mDecelerateInterpolator,
                        ObjectAnimator.ofFloat(mAppDrawer, View.SCALE_X, 1f)
                                .setDuration(FLIP_DURATION_IN))));
        mRecentsAnim = start(setVisibilityWhenDone(
                interpolator(mAccelerateInterpolator,
                        ObjectAnimator.ofFloat(mRecents, View.SCALE_X, 0f))
                        .setDuration(FLIP_DURATION_OUT), mRecents, View.GONE));
    }

    private void flipToRecents() {
        if (mRecentsAnim != null){
            mRecentsAnim.cancel();
        }
        if (mAppDrawerAnim != null){
            mAppDrawerAnim.cancel();
        }
        mRecents.setVisibility(View.VISIBLE);
        mRecents.setScaleX(0f);
        mAppDrawer.setScaleX(1f);

        mRecentsAnim = start(startDelay(
                FLIP_DURATION_OUT,
                interpolator(mDecelerateInterpolator,
                        ObjectAnimator.ofFloat(mRecents, View.SCALE_X, 1f)
                                .setDuration(FLIP_DURATION_IN))));
        mAppDrawerAnim = start(setVisibilityWhenDone(
                interpolator(mAccelerateInterpolator,
                        ObjectAnimator.ofFloat(mAppDrawer, View.SCALE_X, 0f))
                        .setDuration(FLIP_DURATION_OUT), mAppDrawer, View.GONE));
    }

//    private void partialFlip(float progress) {
//        if (mRecentsAnim != null){
//            mRecentsAnim.cancel();
//        }
//        if (mAppDrawerAnim != null){
//            mAppDrawerAnim.cancel();
//        }
//
//        progress = Math.min(Math.max(progress, -1f), 1f);
//        if (progress < 0f) { // recents side
//            mAppDrawer.setVisibility(View.GONE);
//            mAppDrawer.setScaleX(0f);
//            mRecents.setScaleX(-progress);
//            mRecents.setVisibility(View.VISIBLE);
//        } else { // app drawer side
//            mRecents.setVisibility(View.GONE);
//            mRecents.setScaleX(0f);
//            mAppDrawer.setScaleX(progress);
//            mAppDrawer.setVisibility(View.VISIBLE);
//        }
//    }

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
                setButtonGlow(mOpenFavorite,
                        mShowFavorites ? 0 : 1,
                        false);
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

        if (mConfiguration.mLocation == 0) {
            mView.setPivotX(mConfiguration.getCurrentOverlayWidth());
        } else {
            mView.setPivotX(0f);
        }

        if (show){
            mView.setScaleX(0f);
            mToggleOverlayAnim = start(interpolator(mLinearInterpolator,
                            ObjectAnimator.ofFloat(mView, View.SCALE_X, 0f, 1f))
                            .setDuration(SHOW_DURATION));
        } else {
            mView.setScaleX(1f);
            mToggleOverlayAnim = start(interpolator(mLinearInterpolator,
                            ObjectAnimator.ofFloat(mView, View.SCALE_X, 1f, 0f))
                            .setDuration(HIDE_DURATION));
        }

        mToggleOverlayAnim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!show){
                    // to avoid the "Attempting to destroy the window while drawing"
                    // error
                    mPopupView.post(new Runnable() {
                        @Override
                        public void run() {
                            hideDone();
                        }
                    });
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

    private PackageTextView getActionButton(int buttonId) {
        if (buttonId == SettingsActivity.BUTTON_HOME){
            mHomeButton = getPackageItemActionTemplate();
            mHomeButton.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), mContext.getResources().getDrawable(R.drawable.home)));
            mHomeButton.setGlowImage(BitmapUtils.glow(mContext.getResources(),
                    mConfiguration.mGlowColor,
                    mContext.getResources().getDrawable(R.drawable.home)));
            mHomeButton.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction()==MotionEvent.ACTION_DOWN){
                        mHomeButton.setBackground(mHomeButton.getGlowImage());
                    } else if(event.getAction()==MotionEvent.ACTION_UP){
                        mHomeButton.setBackground(mHomeButton.getOriginalImage());
                    } else if(event.getAction()==MotionEvent.ACTION_CANCEL){
                        mHomeButton.setBackground(mHomeButton.getOriginalImage());
                    }
                    v.onTouchEvent(event);
                    return true;
                }});
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
            mLastAppButton = getPackageItemActionTemplate();
            mLastAppButton.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), mContext.getResources().getDrawable(R.drawable.lastapp)));
            mLastAppButton.setGlowImage(BitmapUtils.glow(mContext.getResources(),
                    mConfiguration.mGlowColor,
                    mContext.getResources().getDrawable(R.drawable.lastapp)));
            mLastAppButton.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction()==MotionEvent.ACTION_DOWN){
                        mLastAppButton.setBackground(mLastAppButton.getGlowImage());
                    } else if(event.getAction()==MotionEvent.ACTION_UP){
                        mLastAppButton.setBackground(mLastAppButton.getOriginalImage());
                    } else if(event.getAction()==MotionEvent.ACTION_CANCEL){
                        mLastAppButton.setBackground(mLastAppButton.getOriginalImage());
                    }
                    v.onTouchEvent(event);
                    return true;
                }});

            mLastAppButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.toggleLastApp(mAutoClose);
                }
            });
            mLastAppButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(mContext,
                            mContext.getResources().getString(R.string.toogle_last_app_help), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            return mLastAppButton;
        }

        if (buttonId == SettingsActivity.BUTTON_KILL_ALL){
            mKillAllButton = getPackageItemActionTemplate();
            mKillAllButton.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), mContext.getResources().getDrawable(R.drawable.kill_all)));
            mKillAllButton.setGlowImage(BitmapUtils.glow(mContext.getResources(),
                    mConfiguration.mGlowColor,
                    mContext.getResources().getDrawable(R.drawable.kill_all)));
            mKillAllButton.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction()==MotionEvent.ACTION_DOWN){
                        mKillAllButton.setBackground(mKillAllButton.getGlowImage());
                    } else if(event.getAction()==MotionEvent.ACTION_UP){
                        mKillAllButton.setBackground(mKillAllButton.getOriginalImage());
                    } else if(event.getAction()==MotionEvent.ACTION_CANCEL){
                        mKillAllButton.setBackground(mKillAllButton.getOriginalImage());
                    }
                    v.onTouchEvent(event);
                    return true;
                }});
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
            mKillOtherButton = getPackageItemActionTemplate();
            mKillOtherButton.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), mContext.getResources().getDrawable(R.drawable.kill_other)));
            mKillOtherButton.setGlowImage(BitmapUtils.glow(mContext.getResources(),
                    mConfiguration.mGlowColor,
                    mContext.getResources().getDrawable(R.drawable.kill_other)));
            mKillOtherButton.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction()==MotionEvent.ACTION_DOWN){
                        mKillOtherButton.setBackground(mKillOtherButton.getGlowImage());
                    } else if(event.getAction()==MotionEvent.ACTION_UP){
                        mKillOtherButton.setBackground(mKillOtherButton.getOriginalImage());
                    } else if(event.getAction()==MotionEvent.ACTION_CANCEL){
                        mKillOtherButton.setBackground(mKillOtherButton.getOriginalImage());
                    }
                    v.onTouchEvent(event);
                    return true;
                }});

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
            mSettingsButton = getPackageItemActionTemplate();
            mSettingsButton.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), mContext.getResources().getDrawable(R.drawable.settings)));
            mSettingsButton.setGlowImage(BitmapUtils.glow(mContext.getResources(),
                    mConfiguration.mGlowColor,
                    mContext.getResources().getDrawable(R.drawable.settings)));

            mSettingsButton.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction()==MotionEvent.ACTION_DOWN){
                        mSettingsButton.setBackground(mSettingsButton.getGlowImage());
                    } else if(event.getAction()==MotionEvent.ACTION_UP){
                        mSettingsButton.setBackground(mSettingsButton.getOriginalImage());
                    } else if(event.getAction()==MotionEvent.ACTION_CANCEL){
                        mSettingsButton.setBackground(mSettingsButton.getOriginalImage());
                    }
                    v.onTouchEvent(event);
                    return true;
                }});

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
            mAllappsButton = getPackageItemActionTemplate();
            mAllappsButton.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), mContext.getResources().getDrawable(R.drawable.ic_allapps)));
            mAllappsButton.setGlowImage(BitmapUtils.glow(mContext.getResources(),
                    mConfiguration.mGlowColor,
                    mContext.getResources().getDrawable(R.drawable.ic_allapps)));

            mAllappsButton.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction()==MotionEvent.ACTION_DOWN){
                        mAllappsButton.setBackground(mAllappsButton.getGlowImage());
                    } else if(event.getAction()==MotionEvent.ACTION_UP){
                        mAllappsButton.setBackground(mAllappsButton.getOriginalImage());
                    } else if(event.getAction()==MotionEvent.ACTION_CANCEL){
                        mAllappsButton.setBackground(mAllappsButton.getOriginalImage());
                    }
                    v.onTouchEvent(event);
                    return true;
                }});

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
            mBackButton = getPackageItemActionTemplate();
            mBackButton.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), mContext.getResources().getDrawable(R.drawable.back)));
            mBackButton.setGlowImage(BitmapUtils.glow(mContext.getResources(),
                    mConfiguration.mGlowColor,
                    mContext.getResources().getDrawable(R.drawable.back)));

            mBackButton.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction()==MotionEvent.ACTION_DOWN){
                        mBackButton.setBackground(mBackButton.getGlowImage());
                    } else if(event.getAction()==MotionEvent.ACTION_UP){
                        mBackButton.setBackground(mBackButton.getOriginalImage());
                    } else if(event.getAction()==MotionEvent.ACTION_CANCEL){
                        mBackButton.setBackground(mBackButton.getOriginalImage());
                    }
                    v.onTouchEvent(event);
                    return true;
                }});

            mBackButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mVirtualBackKey = true;
                    mRecentsManager.close();
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

        if (buttonId == SettingsActivity.BUTTON_IMMERSIVE_MODE){
            mImmersiveModeButton = getPackageItemActionTemplate();
            mImmersiveModeButton.setOriginalImage(BitmapUtils.shadow(mContext.getResources(), mContext.getResources().getDrawable(R.drawable.immersive_mode)));
            mImmersiveModeButton.setGlowImage(BitmapUtils.glow(mContext.getResources(),
                    mConfiguration.mGlowColor,
                    mContext.getResources().getDrawable(R.drawable.immersive_mode)));

            mImmersiveModeButton.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction()==MotionEvent.ACTION_DOWN){
                        mImmersiveModeButton.setBackground(mImmersiveModeButton.getGlowImage());
                    } else if(event.getAction()==MotionEvent.ACTION_UP){
                        mImmersiveModeButton.setBackground(mImmersiveModeButton.getOriginalImage());
                    } else if(event.getAction()==MotionEvent.ACTION_CANCEL){
                        mImmersiveModeButton.setBackground(mImmersiveModeButton.getOriginalImage());
                    }
                    v.onTouchEvent(event);
                    return true;
                }});

            mImmersiveModeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Utils.toggleImmersiveMode(mContext);
                    if (mAutoClose){
                        mRecentsManager.close();
                    }
                }
            });
            mImmersiveModeButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(mContext,
                            mContext.getResources().getString(R.string.immersive_mode_help),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            return mImmersiveModeButton;
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
                PackageTextView item = getActionButton(key);
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
        Iterator<PackageTextView> nextButton = mActionList.iterator();
        while(nextButton.hasNext()){
            PackageTextView item = nextButton.next();
            mButtonListItems.addView(item, params);
        }
    }

    private void resetButtonGlow(){
        for (int i = 0; i < mButtonListItems.getChildCount(); i++){
            PackageTextView item = (PackageTextView) mButtonListItems.getChildAt(i);
            item.setBackground(item.getOriginalImage());
        }
    }
}
