/*
 *  Copyright (C) 2015-2016 The OmniROM Project
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
import android.graphics.drawable.Drawable;
import android.graphics.PixelFormat;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SwitchLayoutVertical extends AbstractSwitchLayout {
    private ListView mRecentList;
    private FavoriteViewVertical mFavoriteListView;
    private RecentListAdapter mRecentListAdapter;
    private ScrollView mButtonList;
    private boolean mShowThumbs;
    private Runnable mUpdateRamBarTask;
    private ImageView mRamDisplay;
    private View mRamDisplayContainer;
    private LinearLayout mRecentsOrAppDrawer;

    private class RecentListAdapter extends ArrayAdapter<TaskDescription> {

        public RecentListAdapter(Context context, int resource,
                List<TaskDescription> values) {
            super(context, R.layout.package_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TaskDescription ad = getItem(getTaskPosition(position));
            PackageTextView item = null;
            if (convertView == null) {
                item = getRecentItemTemplate();
            } else {
                item = (PackageTextView) convertView;
            }
            item.setTask(ad, true);

            // load thumb if not loaded so far
            if (mShowThumbs) {
                item.loadTaskThumb();
            }
            return item;
        }
    }

    private static final ViewOutlineProvider BUTTON_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
        }
    };

    public SwitchLayoutVertical(SwitchManager manager, Context context) {
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
                String usedMemStr = mContext.getResources()
                        .getString(R.string.service_foreground_processes,
                                sizeStr);
                sizeStr = Formatter.formatShortFileSize(mContext, availMem);
                String availMemStr = mContext.getResources()
                        .getString(R.string.service_background_processes,
                                sizeStr);
                mRamDisplay.setImageDrawable(BitmapUtils.memImage(mContext.getResources(),
                        mConfiguration.mMemDisplaySize, mConfiguration.mDensity,
                        mConfiguration.mLayoutStyle == 0, usedMemStr, availMemStr));
            }
        };
    }

    @Override
    protected synchronized void createView() {
        mView = mInflater.inflate(R.layout.recents_list_vertical, null, false);

        mRecents = (LinearLayout) mView.findViewById(R.id.recents);

        mRecentList = (ListView) mView
                .findViewById(R.id.recent_list);
        mRecentList.setVerticalScrollBarEnabled(false);
        final int listMargin = Math.round(2 * mConfiguration.mDensity);
        mRecentList.setDividerHeight(listMargin);
        mRecentList.setStackFromBottom(mConfiguration.mRevertRecents);

        mNoRecentApps = (TextView) mView.findViewById(R.id.no_recent_apps);

        mRecentList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                TaskDescription task = mRecentsManager.getTasks().get(getTaskPosition(position));
                mRecentsManager.switchTask(task, mAutoClose, false);
            }
        });

        mRecentList.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                TaskDescription task = mRecentsManager.getTasks().get(getTaskPosition(position));
                handleLongPressRecent(task, view);
                return true;
            }
        });

        SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(
                mRecentList,
                new SwipeDismissListViewTouchListener.DismissCallbacks() {
                    @Override
                    public void onDismiss(ListView listView,
                            int[] reverseSortedPositions) {
                        int position = getTaskPosition(reverseSortedPositions[0]);
                        if (DEBUG) {
                            Log.d(TAG, "onDismiss: "
                                    + mRecentsManager.getTasks().size() + ":"
                                    + position);
                        }
                        try {
                            TaskDescription ad = mRecentsManager.getTasks().get(position);
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

        mRecentList.setOnTouchListener(touchListener);
        mRecentList.setOnScrollListener(touchListener.makeScrollListener());
        mRecentList.setAdapter(mRecentListAdapter);

        mFavoriteListView = (FavoriteViewVertical) mView
                .findViewById(R.id.favorite_list);
        mFavoriteListView.setVerticalScrollBarEnabled(false);
        mFavoriteListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                String intent = mFavoriteList.get(position);
                mRecentsManager.startIntentFromtString(intent, true);
            }
        });
        mFavoriteListView.setAdapter(mFavoriteListAdapter);

        mFavoriteListView
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

        mAppDrawer = (AppDrawerView) mView.findViewById(R.id.app_drawer);
        mAppDrawer.setRecentsManager(mRecentsManager);

        mRecentsOrAppDrawer = (LinearLayout) mView.findViewById(R.id.recents_or_appdrawer);

        mPopupView = new FrameLayout(mContext);
        mPopupView.addView(mView);

        mView.setOnTouchListener(mDragHandleListener);
        mPopupView.setOnTouchListener(mDragHandleListener);
        mPopupView.setOnKeyListener(new PopupKeyListener());

        mButtonList = (ScrollView) mView
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
        if (mNoRecentApps == null || mRecentList == null) {
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
            if (!refresh) {
                resetRecentsPosition();
            }
            mRecentList.setVisibility(View.VISIBLE);
        } else {
            mNoRecentApps.setVisibility(View.VISIBLE);
            mRecentList.setVisibility(View.GONE);
        }
        mUpdateNoRecentsTasksDone = true;
    }

    @Override
    protected synchronized void initView() {
        mRecentsOrAppDrawer.removeView(mAppDrawer);

        mFavoriteListView.setLayoutParams(getListParams());
        mFavoriteListView.setSelection(0);
        mRecentList.setLayoutParams(getRecentListParams());
        mNoRecentApps.setLayoutParams(getRecentListParams());
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
        mShowThumbs = mConfiguration.mLoadThumbOnSwipe;
        enableOpenFavoriteButton(true);
        mOpenFavorite.setRotation(getExpandRotation());
        if (Utils.isLockToAppEnabled(mContext)) {
            updatePinAppButton();
        }
    }

    protected LinearLayout.LayoutParams getListParams() {
        return new LinearLayout.LayoutParams(mConfiguration.mMaxWidth + mConfiguration.mIconBorderHorizontalPx,
                LinearLayout.LayoutParams.MATCH_PARENT);
    }

    private LinearLayout.LayoutParams getRecentListParams() {
        return new LinearLayout.LayoutParams(getCurrentThumbWidth(),
                LinearLayout.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected LinearLayout.LayoutParams getListItemParams() {
        return new LinearLayout.LayoutParams(mConfiguration.mMaxWidth,
                mConfiguration.getItemMaxHeight());
    }

    private LinearLayout.LayoutParams getRecentListItemParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                getCurrentThumbWidth(),
                getCurrentThumbHeight());
        return params;
    }

    private int getAppDrawerColumns() {
        if (mConfiguration.mIconSizeDesc == SwitchConfiguration.IconSize.SMALL) {
            return 4;
        }
        if (mConfiguration.mIconSizeDesc == SwitchConfiguration.IconSize.NORMAL) {
            return 3;
        }
        return 2;
    }

    @Override
    protected LinearLayout.LayoutParams getAppDrawerParams() {
        return new LinearLayout.LayoutParams(getAppDrawerColumns()
                * (mConfiguration.mMaxWidth + mConfiguration.mIconBorderHorizontalPx),
                LinearLayout.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected WindowManager.LayoutParams getParams(float dimAmount) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.MATCH_PARENT,
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

        params.gravity = getHorizontalGravity();

        return params;
    }

    private int getHorizontalGravity() {
        if (mConfiguration.mLocation == 0) {
            return Gravity.RIGHT;
        } else {
            return Gravity.LEFT;
        }
    }

    @Override
    public void updatePrefs(SharedPreferences prefs, String key) {
        super.updatePrefs(prefs, key);
        if (DEBUG) {
            Log.d(TAG, "updatePrefs");
        }
        if (mRecentList != null) {
            mRecentList.setStackFromBottom(mConfiguration.mRevertRecents);
        }

        if (key != null && isPrefKeyForForceUpdate(key)) {
            if (mFavoriteListView != null) {
                mFavoriteListView.setAdapter(mFavoriteListAdapter);
            }
            if (mRecentList != null) {
                mRecentList.setAdapter(mRecentListAdapter);
            }
        }

        if (mFavoriteListView != null) {
            mFavoriteListView.updatePrefs(prefs, key);
        }
        buildButtonList();
        if (mConfiguration.mShowRambar) {
            if (mRamDisplay == null) {
                createMemoryDisplay();
            }
            addMemoryDisplay();
        }
        // must be recreated on dpi changes
        createOpenFavoriteButton();
        addOpenFavoriteButton();
        enableOpenFavoriteButton(!mShowAppDrawer);

        if (mView != null) {
            if (key.equals(SettingsActivity.PREF_BUTTON_POS)) {
                selectButtonContainer();
            }
            updateStyle();
        }
    }

    private PackageTextView getRecentItemTemplate() {
        PackageTextView item = new PackageTextView(mContext);
        if (mConfiguration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_LIGHT) {
            item.setTextColor(Color.BLACK);
            item.setShadowLayer(0, 0, 0, Color.BLACK);
        } else {
            item.setTextColor(Color.WHITE);
            item.setShadowLayer(5, 0, 0, Color.BLACK);
        }
        item.setTextSize(mConfiguration.mLabelFontSize);
        item.setEllipsize(TextUtils.TruncateAt.END);
        item.setGravity(Gravity.CENTER);
        item.setMaxLines(1);
        item.setCanSideHeader(true);
        item.setLayoutParams(getRecentListItemParams());
        item.setBackgroundResource(mConfiguration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_LIGHT ? R.drawable.ripple_dark
                : R.drawable.ripple_light);
        item.setThumbRatio(mConfiguration.mThumbRatio);
        return item;
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
            mFavoriteListView.setScaleX(0f);
            mFavoriteListView.setPivotX(0f);
            mFavoriteListView.setVisibility(View.VISIBLE);
            Animator expandAnimator = interpolator(
                    mLinearInterpolator,
                    ObjectAnimator.ofFloat(mFavoriteListView, View.SCALE_X, 0f, 1f));
            Animator rotateAnimator = interpolator(
                    mLinearInterpolator,
                    ObjectAnimator.ofFloat(mOpenFavorite, View.ROTATION,
                    mConfiguration.mLocation != 0 ? ROTATE_270_DEGREE : ROTATE_90_DEGREE,
                    mConfiguration.mLocation != 0 ? ROTATE_90_DEGREE : ROTATE_270_DEGREE));
            mShowFavAnim = new AnimatorSet();
            mShowFavAnim.playTogether(expandAnimator, rotateAnimator);
            mShowFavAnim.setDuration(FAVORITE_DURATION);
            mShowFavAnim.start();
        } else {
            mFavoriteListView.setScaleX(1f);
            mFavoriteListView.setPivotX(0f);
            Animator collapseAnimator = setVisibilityWhenDone(interpolator(
                    mLinearInterpolator,
                    ObjectAnimator.ofFloat(mFavoriteListView, View.SCALE_X, 1f, 0f)),
                    mFavoriteListView, View.GONE);
            Animator rotateAnimator = interpolator(
                    mLinearInterpolator,
                    ObjectAnimator.ofFloat(mOpenFavorite, View.ROTATION,
                    mConfiguration.mLocation != 0 ? ROTATE_90_DEGREE : ROTATE_270_DEGREE,
                    mConfiguration.mLocation != 0 ? ROTATE_270_DEGREE : ROTATE_90_DEGREE));
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

    private void addOpenFavoriteButton() {
        mActionList.add(mOpenFavorite);
    }

    private void updateStyle() {
        if (DEBUG) {
            Log.d(TAG, "updateStyle");
        }
        if (mConfiguration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_LIGHT) {
            mNoRecentApps.setTextColor(Color.BLACK);
            mNoRecentApps.setShadowLayer(0, 0, 0, Color.BLACK);
            updateOpenFavoriteButton(mContext.getResources().getDrawable(
                    R.drawable.ic_expand));
        } else {
            mNoRecentApps.setTextColor(Color.WHITE);
            mNoRecentApps.setShadowLayer(5, 0, 0, Color.BLACK);
            updateOpenFavoriteButton(BitmapUtils.shadow(
                    mContext.getResources(),
                    mContext.getResources().getDrawable(
                            R.drawable.ic_expand)));
        }
        if (mConfiguration.mBgStyle != SwitchConfiguration.BgStyle.TRANSPARENT) {
            mButtonListContainer.setBackground(mContext.getResources()
                    .getDrawable(R.drawable.overlay_bg_button_gradient));
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
                    R.drawable.overlay_bg_flat_dark_gradient));
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
        if (!mHasFavorites) {
            mShowFavorites = false;
        }
        mFavoriteListView.setVisibility(mShowFavorites ? View.VISIBLE
                : View.GONE);
        buildButtons();
        mButtonsVisible = isButtonVisible();
    }

    private float getExpandRotation() {
        if (mConfiguration.mLocation != 0) {
            return mShowFavorites ? ROTATE_90_DEGREE : ROTATE_270_DEGREE;
        }
        return mShowFavorites ? ROTATE_270_DEGREE : ROTATE_90_DEGREE;
    }

    @Override
    protected int getCurrentOverlayWidth() {
        if (mShowAppDrawer) {
            return getAppDrawerParams().width
                    + (isButtonVisible() ? mConfiguration.mActionSizePx : 0);
        }
        return getCurrentThumbWidth()
                + (mShowFavorites ? mConfiguration.mMaxWidth : 0)
                + (isButtonVisible() ? mConfiguration.mActionSizePx : 0);
    }

    private int getCurrentThumbWidth() {
        return (int)(mConfiguration.mThumbnailWidth * mConfiguration.mThumbRatio) +
               ( mConfiguration.mSideHeader ? mConfiguration.getOverlayHeaderWidth() : 0);
    }

    private int getCurrentThumbHeight() {
        return (int)(mConfiguration.mThumbnailHeight * mConfiguration.mThumbRatio) +
                (mConfiguration.mSideHeader ? 0 : mConfiguration.getOverlayHeaderWidth());
    }

    @Override
    protected void afterShowDone() {
        if (!mConfiguration.mLoadThumbOnSwipe) {
            mShowThumbs = true;
            mRecentListAdapter.notifyDataSetChanged();
        }
    }

    private void createMemoryDisplay() {
        mRamDisplayContainer = mInflater.inflate(R.layout.memory_display, null, false);
        mRamDisplay = (ImageView) mRamDisplayContainer.findViewById(R.id.memory_image);
        mRamDisplay.setImageDrawable(BitmapUtils.memImage(mContext.getResources(),
                mConfiguration.mMemDisplaySize, mConfiguration.mDensity,
                mConfiguration.mLayoutStyle == 0, "", ""));
    }

    private void addMemoryDisplay() {
        mActionList.add(mRamDisplayContainer);
    }

    @Override
    protected void updateRamDisplay() {
        if (mRamDisplay != null) {
            mHandler.post(mUpdateRamBarTask);
        }
    }

    @Override
    protected View getButtonList() {
        return mButtonList;
    }

    private int getTaskPosition(int position) {
        if (mConfiguration.mRevertRecents) {
            return mRecentsManager.getTasks().size() - 1 - position;
        } else {
            return position;
        }
    }

    private void resetRecentsPosition() {
        if (mConfiguration.mRevertRecents) {
            mRecentList.setSelection(mRecentsManager.getTasks().size() - 1);
        } else {
            mRecentList.setSelection(0);
        }
    }

    private void createOpenFavoriteButton() {
        mOpenFavorite = getActionButtonTemplate(mContext.getResources()
                .getDrawable(R.drawable.ic_expand));
        mOpenFavorite.setRotation(getExpandRotation());

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
    }

    private void updateOpenFavoriteButton(Drawable d) {
        if (mOpenFavorite != null) {
            ImageView openFavoriteImage = (ImageView) mOpenFavorite.findViewById(R.id.action_button_image);
            openFavoriteImage.setImageDrawable(d);
        }
    }
}
