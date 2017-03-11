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

import java.util.List;

import org.omnirom.omniswitch.PackageManager;
import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.SettingsActivity;
import org.omnirom.omniswitch.SwitchConfiguration;
import org.omnirom.omniswitch.SwitchManager;
import org.omnirom.omniswitch.TaskDescription;
import org.omnirom.omniswitch.Utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
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
    private FavoriteViewHorizontal mFavoriteListHorizontal;
    private RecentListAdapter mRecentListAdapter;
    private LinearColorBar mRamUsageBar;
    private TextView mBackgroundProcessText;
    private TextView mForegroundProcessText;
    private LinearLayout mRamUsageBarContainer;
    private HorizontalScrollView mButtonList;
    protected Runnable mUpdateRamBarTask;
    private LinearLayout mRecentsOrAppDrawer;

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
            item.setTaskInfo(mConfiguration);
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

        mFavoriteListHorizontal = (FavoriteViewHorizontal) mView
                .findViewById(R.id.favorite_list_horizontal);
        mFavoriteListHorizontal.setRecentsManager(mRecentsManager);
        mFavoriteListHorizontal.init();

        mRamUsageBarContainer = (LinearLayout) mView
                .findViewById(R.id.ram_usage_bar_container);
        mRamUsageBar = (LinearColorBar) mView.findViewById(R.id.ram_usage_bar);
        mForegroundProcessText = (TextView) mView
                .findViewById(R.id.foregroundText);
        mBackgroundProcessText = (TextView) mView
                .findViewById(R.id.backgroundText);
        mForegroundProcessText.setTextColor(Color.WHITE);
        mBackgroundProcessText.setTextColor(Color.WHITE);

        mAppDrawer = (AppDrawerView) mView.findViewById(R.id.app_drawer);
        mAppDrawer.setRecentsManager(mRecentsManager);

        mRecentsOrAppDrawer = (LinearLayout) mView.findViewById(R.id.recents_or_appdrawer);

        mPopupView = new FrameLayout(mContext);
        mPopupView.addView(mView);

        mView.setOnTouchListener(mDragHandleListener);
        mPopupView.setOnTouchListener(mDragHandleListener);
        mPopupView.setOnKeyListener(new PopupKeyListener());

        mButtonList = (HorizontalScrollView) mView
                .findViewById(R.id.button_list_top);
        mButtonListItems = (LinearLayout) mView
                .findViewById(R.id.button_list_items_top);

        mButtonListContainerTop = (LinearLayout) mView
                .findViewById(R.id.button_list_container_top);
        mButtonListContainerBottom = (LinearLayout) mView
                .findViewById(R.id.button_list_container_bottom);
        selectButtonContainer();
        updateStyle();
    }

    @Override
    protected synchronized void updateRecentsAppsList(boolean force,  boolean refresh) {
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
        mRecentsOrAppDrawer.removeView(mAppDrawer);
        updateListLayout();

        mNoRecentApps.setLayoutParams(getListParams());
        mRecents.setVisibility(View.VISIBLE);
        mShowAppDrawer = false;
        mAppDrawer.setVisibility(View.GONE);
        mAppDrawer.post(new Runnable() {
            @Override
            public void run() {
                mAppDrawer.setSelection(0);
            }
        });
        mView.setTranslationX(0);
        mVirtualBackKey = false;
        enableOpenFavoriteButton(true);
        mOpenFavorite.setRotation(getExpandRotation());
        if (Utils.isLockToAppEnabled(mContext)) {
            updatePinAppButton();
        }
    }

    private void updateListLayout() {
        int dividerWith = mConfiguration.calcHorizontalDivider(false);

        mFavoriteListHorizontal.setLayoutParams(getListParams());
        mFavoriteListHorizontal.scrollTo(0);
        mFavoriteListHorizontal.setDividerWidth(dividerWith);
        mFavoriteListHorizontal.setPadding(dividerWith / 2, 0,
                dividerWith / 2, 0);

        mRecentListHorizontal.setLayoutParams(getListParams());
        mRecentListHorizontal.scrollTo(0);
        mRecentListHorizontal.setDividerWidth(dividerWith);
        mRecentListHorizontal.setPadding(dividerWith / 2, 0,
                dividerWith / 2, 0);
    }

    private LinearLayout.LayoutParams getListParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                mConfiguration.getItemMaxHeight());
    }

    @Override
    protected LinearLayout.LayoutParams getListItemParams() {
        return new LinearLayout.LayoutParams(mConfiguration.mMaxWidth + mConfiguration.mIconBorderHorizontal,
                mConfiguration.getItemMaxHeight());
    }

    private int getAppDrawerLines() {
        if (mConfiguration.mIconSizeDesc == SwitchConfiguration.IconSize.SMALL) {
            if (mConfiguration.isLandscape()) {
                return 4;
            } else {
                return 5;
            }
        }
        if (mConfiguration.mIconSizeDesc == SwitchConfiguration.IconSize.NORMAL) {
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

    @Override
    protected LinearLayout.LayoutParams getAppDrawerParams() {
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
                - (mButtonsVisible ? mConfiguration.mActionSizePx : 0);

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
        if (mFavoriteListHorizontal != null) {
            mFavoriteListHorizontal.updatePrefs(prefs, key);
        }
        if (key != null && isPrefKeyForForceUpdate(key)) {
            if (mRecentListHorizontal != null) {
                mRecentListHorizontal.setAdapter(mRecentListAdapter);
            }
        }
        buildButtonList();
        if (mView != null) {
            if (key.equals(SettingsActivity.PREF_BUTTON_POS)) {
                selectButtonContainer();
            }
            updateStyle();
        }
    }

    @Override
    public void updateLayout() {
        try {
            if (mShowing) {
                updateListLayout();
            }
        } catch (Exception e) {
            // ignored
        }
        super.updateLayout();
    }

    @Override
    protected void flipToAppDrawerNew() {
        mRecentsOrAppDrawer.addView(mAppDrawer);
        mAppDrawer.setLayoutParams(getAppDrawerParams());
        mAppDrawer.requestLayout();
        mRecents.setVisibility(View.GONE);
        mAppDrawer.setVisibility(View.VISIBLE);
        enableOpenFavoriteButton(false);
    }

    @Override
    protected void flipToRecentsNew() {
        mRecentsOrAppDrawer.removeView(mAppDrawer);
        mAppDrawer.setVisibility(View.GONE);
        mRecents.setVisibility(View.VISIBLE);
        enableOpenFavoriteButton(true);
    }

    @Override
    protected void toggleFavorites() {
        mShowFavorites = !mShowFavorites;
        storeExpandedFavoritesState();

        if (mShowFavAnim != null) {
            mShowFavAnim.cancel();
        }

        if (mShowFavorites) {
            mFavoriteListHorizontal.setVisibility(View.VISIBLE);
            mFavoriteListHorizontal.setScaleY(0f);
            mFavoriteListHorizontal.setPivotY(0f);
            Animator expandAnimator = interpolator(
                    mLinearInterpolator,
                    ObjectAnimator.ofFloat(mFavoriteListHorizontal, View.SCALE_Y, 0f, 1f));
            Animator rotateAnimator = interpolator(
                    mLinearInterpolator,
                    ObjectAnimator.ofFloat(mOpenFavorite, View.ROTATION, ROTATE_0_DEGREE, ROTATE_180_DEGREE));
            mShowFavAnim = new AnimatorSet();
            mShowFavAnim.playTogether(expandAnimator, rotateAnimator);
            mShowFavAnim.setDuration(FAVORITE_DURATION);
            mShowFavAnim.start();
        } else {
            mFavoriteListHorizontal.setScaleY(1f);
            mFavoriteListHorizontal.setPivotY(0f);
            Animator collapseAnimator = setVisibilityWhenDone(interpolator(
                    mLinearInterpolator,
                    ObjectAnimator.ofFloat(mFavoriteListHorizontal, View.SCALE_Y, 1f, 0f)),
                    mFavoriteListHorizontal, View.GONE);
            Animator rotateAnimator = interpolator(
                    mLinearInterpolator,
                    ObjectAnimator.ofFloat(mOpenFavorite, View.ROTATION, ROTATE_180_DEGREE, ROTATE_0_DEGREE));
            mShowFavAnim = new AnimatorSet();
            mShowFavAnim.playTogether(collapseAnimator, rotateAnimator);
            mShowFavAnim.setDuration(FAVORITE_DURATION);
            mShowFavAnim.start();
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

    private void updateStyle() {
        if (mConfiguration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_LIGHT) {
            mForegroundProcessText.setShadowLayer(0, 0, 0, Color.BLACK);
            mBackgroundProcessText.setShadowLayer(0, 0, 0, Color.BLACK);
            mNoRecentApps.setTextColor(Color.BLACK);
            mNoRecentApps.setShadowLayer(0, 0, 0, Color.BLACK);
            ((ImageView) mOpenFavorite).setImageDrawable(BitmapUtils.colorize(
                    mContext.getResources(),
                    mContext.getResources().getColor(
                            R.color.button_bg_flat_color),
                    mContext.getResources().getDrawable(
                            R.drawable.ic_expand)));
            mRamUsageBarContainer.setOutlineProvider(BAR_OUTLINE_PROVIDER);
        } else {
            mForegroundProcessText.setShadowLayer(5, 0, 0, Color.BLACK);
            mBackgroundProcessText.setShadowLayer(5, 0, 0, Color.BLACK);
            mNoRecentApps.setTextColor(Color.WHITE);
            mNoRecentApps.setShadowLayer(5, 0, 0, Color.BLACK);
            ((ImageView) mOpenFavorite).setImageDrawable(BitmapUtils.shadow(
                    mContext.getResources(),
                    mContext.getResources().getDrawable(
                            R.drawable.ic_expand)));
            mRamUsageBarContainer.setOutlineProvider(null);
        }
        if (mConfiguration.mBgStyle != SwitchConfiguration.BgStyle.TRANSPARENT) {
            mButtonListContainer.setBackground(mContext.getResources()
                    .getDrawable(R.drawable.overlay_bg_button_flat));
            mButtonListContainer.setOutlineProvider(BUTTON_OUTLINE_PROVIDER);
        } else {
            mButtonListContainer.setBackground(null);
            mButtonListContainer.setOutlineProvider(null);
        }
        if (mConfiguration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_LIGHT) {
            mView.setBackground(mContext.getResources().getDrawable(
                    R.drawable.overlay_bg_flat_gradient));
        } else if (mConfiguration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_DARK) {
            mView.setBackground(mContext.getResources().getDrawable(
                    R.drawable.overlay_bg_flat_dark));
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
        if (!mHasFavorites) {
            mShowFavorites = false;
        }
        mFavoriteListHorizontal.setVisibility(mShowFavorites ? View.VISIBLE
                : View.GONE);
        mOpenFavorite
                .setBackgroundResource(mConfiguration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_LIGHT
                        ? R.drawable.ripple_dark  : R.drawable.ripple_light);

        buildButtons();
        mButtonsVisible = isButtonVisible();
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

    private float getExpandRotation() {
        return mShowFavorites ? ROTATE_180_DEGREE : ROTATE_0_DEGREE;
    }

    @Override
    protected View getButtonList() {
        return mButtonList;
    }

    @Override
    protected View getActionButtonTemplate(Drawable image) {
        View v = mInflater.inflate(R.layout.action_button_horizontal, null, false);
        ImageView item = (ImageView) v.findViewById(R.id.action_button_image);
        item.setImageDrawable(image);
        return v;
    }
}
