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

import java.util.List;

import org.omnirom.omniswitch.PackageManager;
import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.SettingsActivity;
import org.omnirom.omniswitch.SwitchManager;
import org.omnirom.omniswitch.TaskDescription;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SwitchLayout extends AbstractSwitchLayout {
    private HorizontalListView mRecentListHorizontal;
    private HorizontalListView mFavoriteListHorizontal;
    private RecentListAdapter mRecentListAdapter;
    private LinearColorBar mRamUsageBar;
    private TextView mBackgroundProcessText;
    private TextView mForegroundProcessText;
    private LinearLayout mRamUsageBarContainer;
    private Animator mAppDrawerAnim;
    private Animator mRecentsAnim;
    private HorizontalScrollView mButtonList;
    protected Runnable mUpdateRamBarTask;

    private class RecentListAdapter extends ArrayAdapter<TaskDescription> {

        public RecentListAdapter(Context context, int resource,
                List<TaskDescription> values) {
            super(context, R.layout.package_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TaskDescription ad = getItem(position);

            PackageTextView item = null;
            if (convertView == null) {
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
            Drawable d = BitmapCache.getInstance(mContext).getResized(
                    mContext.getResources(), ad, ad.getIcon(), mConfiguration,
                    mConfiguration.mIconSize);
            item.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
            return item;
        }
    }

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
        super(manager, context);
        mRecentListAdapter = new RecentListAdapter(mContext,
                android.R.layout.simple_list_item_single_choice,
                mRecentsManager.getTasks());
        // default on first start
        mShowFavorites = mPrefs.getBoolean(SettingsActivity.PREF_SHOW_FAVORITE,
                false);

        mUpdateRamBarTask = new Runnable() {
            @Override
            public void run() {
                final ActivityManager am = (ActivityManager) mContext
                        .getSystemService(Context.ACTIVITY_SERVICE);
                MemoryInfo memInfo = new MemoryInfo();
                am.getMemoryInfo(memInfo);

                long availMem = memInfo.availMem;
                long totalMem = memInfo.totalMem;

                String sizeStr = Formatter.formatShortFileSize(mContext,
                        totalMem - availMem);
                mForegroundProcessText.setText(mContext.getResources()
                        .getString(R.string.service_foreground_processes,
                                sizeStr));
                sizeStr = Formatter.formatShortFileSize(mContext, availMem);
                mBackgroundProcessText.setText(mContext.getResources()
                        .getString(R.string.service_background_processes,
                                sizeStr));

                float fTotalMem = totalMem;
                float fAvailMem = availMem;
                mRamUsageBar.setRatios((fTotalMem - fAvailMem) / fTotalMem, 0,
                        0);
            }
        };
    }

    @Override
    protected synchronized void createView() {
        mView = mInflater
                .inflate(R.layout.recents_list_horizontal, null, false);

        mRecents = (LinearLayout) mView.findViewById(R.id.recents);

        mRecentListHorizontal = (HorizontalListView) mView
                .findViewById(R.id.recent_list_horizontal);

        mNoRecentApps = (TextView) mView.findViewById(R.id.no_recent_apps);

        mRecentListHorizontal.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                TaskDescription task = mRecentsManager.getTasks().get(position);
                mRecentsManager.switchTask(task, mAutoClose, false);
            }
        });

        mRecentListHorizontal
                .setOnItemLongClickListener(new OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent,
                            View view, int position, long id) {
                        TaskDescription task = mRecentsManager.getTasks().get(
                                position);
                        handleLongPressRecent(task, view);
                        return true;
                    }
                });

        SwipeDismissHorizontalListViewTouchListener touchListener = new SwipeDismissHorizontalListViewTouchListener(
                mRecentListHorizontal,
                new SwipeDismissHorizontalListViewTouchListener.DismissCallbacks() {
                    public void onDismiss(HorizontalListView listView,
                            int[] reverseSortedPositions) {
                        Log.d(TAG, "onDismiss: "
                                + mRecentsManager.getTasks().size() + ":"
                                + reverseSortedPositions[0]);
                        try {
                            TaskDescription ad = mRecentsManager.getTasks()
                                    .get(reverseSortedPositions[0]);
                            mRecentsManager.killTask(ad);
                        } catch (IndexOutOfBoundsException e) {
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

        mOpenFavorite = (ImageView) mView.findViewById(R.id.openFavorites);

        mOpenFavorite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleFavorites();
            }
        });

        mOpenFavorite.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(
                        mContext,
                        mContext.getResources().getString(
                                R.string.open_favorite_help),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        mFavoriteListHorizontal = (HorizontalListView) mView
                .findViewById(R.id.favorite_list_horizontal);

        mFavoriteListHorizontal
                .setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
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
                        PackageManager.PackageItem packageItem = PackageManager
                                .getInstance(mContext).getPackageItem(intent);
                        handleLongPressFavorite(packageItem, view);
                        return true;
                    }
                });

        mRamUsageBarContainer = (LinearLayout) mView
                .findViewById(R.id.ram_usage_bar_container);
        mRamUsageBar = (LinearColorBar) mView.findViewById(R.id.ram_usage_bar);
        mForegroundProcessText = (TextView) mView
                .findViewById(R.id.foregroundText);
        mBackgroundProcessText = (TextView) mView
                .findViewById(R.id.backgroundText);
        mForegroundProcessText.setTextColor(Color.WHITE);
        mBackgroundProcessText.setTextColor(Color.WHITE);

        mAppDrawer = (GridView) mView.findViewById(R.id.app_drawer);
        mAppDrawer.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                PackageManager.PackageItem packageItem = PackageManager
                        .getInstance(mContext).getPackageList().get(position);
                mRecentsManager.startIntentFromtString(packageItem.getIntent(),
                        true);
            }
        });

        mAppDrawer.setAdapter(mAppDrawerListAdapter);

        mAppDrawer.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                PackageManager.PackageItem packageItem = PackageManager
                        .getInstance(mContext).getPackageList().get(position);
                handleLongPressAppDrawer(packageItem, view);
                return true;
            }
        });

        mPopupView = new FrameLayout(mContext);
        mPopupView.addView(mView);

        mView.setOnTouchListener(mDragHandleListener);
        mPopupView.setOnTouchListener(mDragHandleListener);
        mPopupView.setOnKeyListener(new PopupKeyListener());
    }

    @Override
    protected synchronized void updateRecentsAppsList(boolean force) {
        if (DEBUG) {
            Log.d(TAG, "updateRecentsAppsList " + System.currentTimeMillis());
        }
        if (!force && mUpdateNoRecentsTasksDone) {
            if (DEBUG) {
                Log.d(TAG, "!force && mUpdateNoRecentsTasksDone");
            }
            return;
        }
        if (mNoRecentApps == null || mRecentListHorizontal == null) {
            if (DEBUG) {
                Log.d(TAG,
                        "mNoRecentApps == null || mRecentListHorizontal == null");
            }
            return;
        }

        if (!mTaskLoadDone) {
            if (DEBUG) {
                Log.d(TAG, "!mTaskLoadDone");
            }
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "updateRecentsAppsList2");
        }
        mRecentListAdapter.notifyDataSetChanged();

        if (mRecentsManager.getTasks().size() != 0) {
            mNoRecentApps.setVisibility(View.GONE);
            mRecentListHorizontal.setVisibility(View.VISIBLE);
        } else {
            mNoRecentApps.setVisibility(View.VISIBLE);
            mRecentListHorizontal.setVisibility(View.GONE);
        }
        mUpdateNoRecentsTasksDone = true;
    }

    @Override
    protected synchronized void initView() {
        if (mButtonListContainer != null) {
            mButtonListContainer.setVisibility(View.GONE);
        }
        mButtonList = (HorizontalScrollView) mView
                .findViewById(mConfiguration.mButtonPos == 0 ? R.id.button_list_top
                        : R.id.button_list_bottom);
        mButtonList.setHorizontalScrollBarEnabled(false);
        mButtonListItems = (LinearLayout) mView
                .findViewById(mConfiguration.mButtonPos == 0 ? R.id.button_list_items_top
                        : R.id.button_list_items_bottom);
        mButtonListContainer = (LinearLayout) mView
                .findViewById(mConfiguration.mButtonPos == 0 ? R.id.button_list_container_top
                        : R.id.button_list_container_bottom);
        mButtonListContainer.setVisibility(View.VISIBLE);

        updateStyle();

        if (mConfiguration.mBgStyle == 0) {
            mView.setBackground(mContext.getResources().getDrawable(
                    R.drawable.overlay_bg_flat));
        } else {
            mView.setBackground(mContext.getResources().getDrawable(
                    R.drawable.overlay_bg));
            if (!mConfiguration.mDimBehind) {
                mView.getBackground().setAlpha(
                        (int) (255 * mConfiguration.mBackgroundOpacity));
            } else {
                mView.getBackground().setAlpha(0);
            }
        }
        mRamUsageBarContainer
                .setVisibility(mConfiguration.mShowRambar ? View.VISIBLE
                        : View.GONE);

        mConfiguration.calcHorizontalDivider();

        mFavoriteListHorizontal.setLayoutParams(getListParams());
        mFavoriteListHorizontal.scrollTo(0);
        mFavoriteListHorizontal
                .setDividerWidth(mConfiguration.mHorizontalDividerWidth);
        mFavoriteListHorizontal.setPadding(
                mConfiguration.mHorizontalDividerWidth / 2, 0,
                mConfiguration.mHorizontalDividerWidth / 2, 0);

        mRecentListHorizontal.setLayoutParams(getListParams());
        mRecentListHorizontal.scrollTo(0);
        mRecentListHorizontal
                .setDividerWidth(mConfiguration.mHorizontalDividerWidth);
        mRecentListHorizontal.setPadding(
                mConfiguration.mHorizontalDividerWidth / 2, 0,
                mConfiguration.mHorizontalDividerWidth / 2, 0);

        mNoRecentApps.setLayoutParams(getListParams());
        mAppDrawer.setColumnWidth(mConfiguration.mMaxWidth);
        mAppDrawer.setLayoutParams(getAppDrawerParams());
        mAppDrawer.requestLayout();
        mAppDrawer.scrollTo(0, 0);
        mRecents.setVisibility(View.VISIBLE);
        mShowAppDrawer = false;
        mAppDrawer.setVisibility(View.GONE);
        mAppDrawer.setSelection(0);
        mView.setTranslationX(0);

        if (!mHasFavorites) {
            mShowFavorites = false;
        }

        mFavoriteListHorizontal.setVisibility(mShowFavorites ? View.VISIBLE
                : View.GONE);
        enableOpenFavoriteButton();
        mOpenFavorite.setRotation(getExpandRotation());
        mOpenFavorite
                .setBackgroundResource(mConfiguration.mBgStyle == 0 ? R.drawable.ripple_dark
                        : R.drawable.ripple_light);

        if (mHasFavorites && !mShowcaseDone) {
            mOpenFavorite.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mOpenFavorite.getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(this);
                            int[] location = new int[2];
                            mOpenFavorite.getLocationOnScreen(location);
                            mOpenFavoriteX = location[0];
                            mOpenFavoriteY = location[1];
                        }
                    });
        }

        buildButtons();

        mButtonsVisible = isButtonVisible();

        mVirtualBackKey = false;
    }

    private LinearLayout.LayoutParams getListParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                mConfiguration.getItemMaxHeight());
    }

    @Override
    protected LinearLayout.LayoutParams getListItemParams() {
        return new LinearLayout.LayoutParams(mConfiguration.mMaxWidth,
                mConfiguration.getItemMaxHeight());
    }

    // TODO dont use real icon size values in code
    private int getAppDrawerLines() {
        if (mConfiguration.mIconSize == 40) {
            if (mConfiguration.isLandscape()) {
                return 4;
            } else {
                return 5;
            }
        }
        if (mConfiguration.mIconSize == 60) {
            if (mConfiguration.isLandscape()) {
                return 3;
            } else {
                return 4;
            }
        }
        if (mConfiguration.isLandscape()) {
            return 2;
        }
        return 3;
    }

    private LinearLayout.LayoutParams getAppDrawerParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, getAppDrawerLines()
                        * mConfiguration.getItemMaxHeight());
    }

    @Override
    protected WindowManager.LayoutParams getParams(float dimAmount) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                getCurrentOverlayWidth(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                mConfiguration.mDimBehind ? WindowManager.LayoutParams.FLAG_DIM_BEHIND
                        : 0, PixelFormat.TRANSLUCENT);

        // Turn on hardware acceleration for high end gfx devices.
        if (ActivityManager.isHighEndGfx()) {
            params.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
            params.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED;
        }
        if (mConfiguration.mDimBehind) {
            params.dimAmount = dimAmount;
        }

        params.gravity = Gravity.TOP | getHorizontalGravity();
        params.y = mConfiguration.getCurrentOffsetStart()
                + mConfiguration.mDragHandleHeight / 2
                - mConfiguration.getItemMaxHeight() / 2
                - (mButtonsVisible ? mConfiguration.mActionIconSizePx : 0);

        return params;
    }

    private int getHorizontalGravity() {
        if (mConfiguration.mGravity == 0) {
            return Gravity.CENTER_HORIZONTAL;
        } else {
            if (mConfiguration.mLocation == 0) {
                return Gravity.RIGHT;
            } else {
                return Gravity.LEFT;
            }
        }
    }

    @Override
    public void updatePrefs(SharedPreferences prefs, String key) {
        super.updatePrefs(prefs, key);
        if (DEBUG) {
            Log.d(TAG, "updatePrefs");
        }
        mFavoriteListAdapter.notifyDataSetChanged();
        if (mFavoriteListHorizontal != null) {
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

    @Override
    public void updateLayout() {
        try {
            if (mShowing) {
                mConfiguration.calcHorizontalDivider();

                mFavoriteListHorizontal
                        .setDividerWidth(mConfiguration.mHorizontalDividerWidth);
                mFavoriteListHorizontal.setPadding(
                        mConfiguration.mHorizontalDividerWidth / 2, 0,
                        mConfiguration.mHorizontalDividerWidth / 2, 0);

                mRecentListHorizontal
                        .setDividerWidth(mConfiguration.mHorizontalDividerWidth);
                mRecentListHorizontal.setPadding(
                        mConfiguration.mHorizontalDividerWidth / 2, 0,
                        mConfiguration.mHorizontalDividerWidth / 2, 0);

                mFavoriteListAdapter.notifyDataSetChanged();
                mFavoriteListHorizontal.setAdapter(mFavoriteListAdapter);

                mRecentListAdapter.notifyDataSetChanged();
                mRecentListHorizontal.setAdapter(mRecentListAdapter);

                mAppDrawer.setLayoutParams(getAppDrawerParams());
                mAppDrawer.requestLayout();

                mWindowManager.updateViewLayout(mPopupView,
                        getParams(mConfiguration.mBackgroundOpacity));
            }
        } catch (Exception e) {
            // ignored
        }
    }

    @Override
    protected void flipToAppDrawerNew() {
        if (mRecentsAnim != null) {
            mRecentsAnim.cancel();
        }
        if (mAppDrawerAnim != null) {
            mAppDrawerAnim.cancel();
        }

        // center right
        int cx = (mAppDrawer.getLeft() + mAppDrawer.getRight() / 2);
        int cy = (mAppDrawer.getTop() + mAppDrawer.getBottom() / 2);

        // get the final radius for the clipping circle
        int finalRadius = Math.max(mAppDrawer.getWidth(),
                mAppDrawer.getHeight());

        // create the animator for this view (the start radius is zero)
        mAppDrawerAnim = ViewAnimationUtils.createCircularReveal(mAppDrawer,
                cx, cy, 0, finalRadius);
        mAppDrawerAnim.setDuration(FLIP_DURATION);
        mAppDrawer.setVisibility(View.VISIBLE);

        mRecents.animate().alpha(0f).setDuration(FLIP_DURATION / 2)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mRecents.setVisibility(View.GONE);
                        mRecents.setAlpha(1f);
                        enableOpenFavoriteButton();
                    }
                });

        mAppDrawerAnim.start();
    }

    @Override
    protected void flipToRecentsNew() {
        if (mRecentsAnim != null) {
            mRecentsAnim.cancel();
        }
        if (mAppDrawerAnim != null) {
            mAppDrawerAnim.cancel();
        }

        // center
        int cx = (mRecents.getLeft() + mRecents.getRight() / 2);
        int cy = (mRecents.getTop() + mRecents.getBottom() / 2);

        // get the final radius for the clipping circle
        int finalRadius = Math.max(mRecents.getWidth(), mRecents.getHeight());

        // create the animator for this view (the start radius is zero)
        mRecentsAnim = ViewAnimationUtils.createCircularReveal(mRecents, cx,
                cy, 0, finalRadius);
        mRecentsAnim.setDuration(FLIP_DURATION);
        mRecents.setVisibility(View.VISIBLE);

        mAppDrawer.animate().alpha(0f).setDuration(FLIP_DURATION / 2)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mAppDrawer.setVisibility(View.GONE);
                        mAppDrawer.setAlpha(1f);
                        enableOpenFavoriteButton();
                    }
                });

        mRecentsAnim.start();
    }

    @Override
    protected void toggleFavorites() {
        mShowFavorites = !mShowFavorites;

        if (mShowFavAnim != null) {
            mShowFavAnim.cancel();
        }

        if (mShowFavorites) {
            mFavoriteListHorizontal.setVisibility(View.VISIBLE);
            mFavoriteListHorizontal.setScaleY(0f);
            mFavoriteListHorizontal.setPivotY(0f);
            mShowFavAnim = start(interpolator(
                    mLinearInterpolator,
                    ObjectAnimator.ofFloat(mFavoriteListHorizontal,
                            View.SCALE_Y, 0f, 1f)).setDuration(
                    FAVORITE_DURATION));
        } else {
            mFavoriteListHorizontal.setScaleY(1f);
            mFavoriteListHorizontal.setPivotY(0f);
            mShowFavAnim = start(setVisibilityWhenDone(
                    interpolator(
                            mLinearInterpolator,
                            ObjectAnimator.ofFloat(mFavoriteListHorizontal,
                                    View.SCALE_Y, 1f, 0f)).setDuration(
                            FAVORITE_DURATION), mFavoriteListHorizontal,
                    View.GONE));
        }

        mShowFavAnim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mOpenFavorite.setRotation(getExpandRotation());
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

    @Override
    protected LinearLayout.LayoutParams getButtonListItemParams() {
        int buttonMargin = Math.round(5 * mConfiguration.mDensity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(buttonMargin, 0, buttonMargin, 0);
        return params;
    }

    private void updateStyle() {
        if (mConfiguration.mBgStyle == 0) {
            mButtonListContainer.setBackground(mContext.getResources()
                    .getDrawable(R.drawable.overlay_bg_button_flat));
            mForegroundProcessText.setShadowLayer(0, 0, 0, Color.BLACK);
            mBackgroundProcessText.setShadowLayer(0, 0, 0, Color.BLACK);
            mNoRecentApps.setTextColor(Color.BLACK);
            mNoRecentApps.setShadowLayer(0, 0, 0, Color.BLACK);
            mOpenFavorite.setImageDrawable(BitmapUtils.colorize(
                    mContext.getResources(),
                    mContext.getResources().getColor(
                            R.color.button_bg_flat_color),
                    mContext.getResources().getDrawable(
                            R.drawable.ic_expand_down_small)));
            mRamUsageBarContainer.setOutlineProvider(BAR_OUTLINE_PROVIDER);
            mButtonListContainer.setOutlineProvider(BUTTON_OUTLINE_PROVIDER);
        } else {
            mButtonListContainer.setBackground(null);
            mForegroundProcessText.setShadowLayer(5, 0, 0, Color.BLACK);
            mBackgroundProcessText.setShadowLayer(5, 0, 0, Color.BLACK);
            mNoRecentApps.setTextColor(Color.WHITE);
            mNoRecentApps.setShadowLayer(5, 0, 0, Color.BLACK);
            mOpenFavorite.setImageDrawable(BitmapUtils.shadow(
                    mContext.getResources(),
                    mContext.getResources().getDrawable(
                            R.drawable.ic_expand_down_small)));
            mRamUsageBarContainer.setOutlineProvider(null);
            mButtonListContainer.setOutlineProvider(null);
        }
    }

    @Override
    protected int getCurrentOverlayWidth() {
        return mConfiguration.getCurrentOverlayWidth();
    }

    @Override
    protected void updateRamDisplay() {
        if (mUpdateRamBarTask != null) {
            mHandler.post(mUpdateRamBarTask);
        }
    }

    @Override
    protected void afterShowDone() {
    }

    private int getExpandRotation() {
        return mShowFavorites ? ROTATE_180_DEGREE : 0;
    }
}
