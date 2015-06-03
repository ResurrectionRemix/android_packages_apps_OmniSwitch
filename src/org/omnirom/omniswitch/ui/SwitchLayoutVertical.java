/*
 *  Copyright (C) 2015 The OmniROM Project
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

public class SwitchLayoutVertical extends AbstractSwitchLayout {
    private ListView mRecentList;
    private ListView mFavoriteListView;
    private RecentListAdapter mRecentListAdapter;
    private ScrollView mButtonList;
    private boolean mShowThumbs;
    private Runnable mUpdateRamBarTask;
    private ImageView mRamDisplay;

    private class RecentListAdapter extends ArrayAdapter<TaskDescription> {

        public RecentListAdapter(Context context, int resource,
                List<TaskDescription> values) {
            super(context, R.layout.package_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TaskDescription ad = mRecentsManager.getTasks().get(position);

            PackageTextView item = null;
            if (convertView == null) {
                item = getRecentItemTemplate();
            } else {
                item = (PackageTextView) convertView;
            }
            item.setLabel(ad.getLabel());
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
                Log.d(TAG, usedMemStr + " " + availMemStr);
                mRamDisplay.setImageDrawable(BitmapUtils.memImage(mContext.getResources(),
                        mConfiguration.mMemDisplaySize, mConfiguration.mDensity,
                        mConfiguration.mLayoutStyle == 0, mConfiguration.mBgStyle == 0, usedMemStr, availMemStr));
            }
        };
    }

    @Override
    protected synchronized void createView() {
        mView = mInflater.inflate(R.layout.recents_list_vertical, null, false);

        mRecents = (LinearLayout) mView.findViewById(R.id.recents);

        mRecentList = (ListView) mView
                .findViewById(R.id.recent_list_horizontal);
        mRecentList.setVerticalScrollBarEnabled(false);
        final int listMargin = Math.round(2 * mConfiguration.mDensity);
        mRecentList.setDividerHeight(listMargin);

        mNoRecentApps = (TextView) mView.findViewById(R.id.no_recent_apps);

        mRecentList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                TaskDescription task = mRecentsManager.getTasks().get(position);
                mRecentsManager.switchTask(task, mAutoClose, false);
            }
        });

        mRecentList.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                TaskDescription task = mRecentsManager.getTasks().get(position);
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

        mRecentList.setOnTouchListener(touchListener);
        mRecentList.setOnScrollListener(touchListener.makeScrollListener());
        mRecentList.setAdapter(mRecentListAdapter);

        mFavoriteListView = (ListView) mView
                .findViewById(R.id.favorite_list_horizontal);
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
            mRecentList.setVisibility(View.VISIBLE);
        } else {
            mNoRecentApps.setVisibility(View.VISIBLE);
            mRecentList.setVisibility(View.GONE);
        }
        mUpdateNoRecentsTasksDone = true;
    }

    @Override
    protected synchronized void initView() {
        if (mButtonListContainer != null) {
            mButtonListContainer.setVisibility(View.GONE);
        }
        mButtonList = (ScrollView) mView
                .findViewById(mConfiguration.mButtonPos == 0 ? R.id.button_list_top
                        : R.id.button_list_bottom);
        mButtonList.setVerticalScrollBarEnabled(false);
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

        mFavoriteListView.setLayoutParams(getListParams());
        mFavoriteListView.setSelection(0);

        mRecentList.setLayoutParams(getRecentListParams());
        mRecentList.setSelection(0);

        mNoRecentApps.setLayoutParams(getRecentListParams());
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

        mFavoriteListView.setVisibility(mShowFavorites ? View.VISIBLE
                : View.GONE);
        enableOpenFavoriteButton();
        mOpenFavorite.setRotation(getExpandRotation());

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
        mShowThumbs = false;
    }

    protected LinearLayout.LayoutParams getListParams() {
        return new LinearLayout.LayoutParams(mConfiguration.mMaxWidth,
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

    // TODO dont use real icon size values in code
    private int getAppDrawerColumns() {
        if (mConfiguration.mIconSize == 40) {
            return 4;
        }
        if (mConfiguration.mIconSize == 60) {
            return 3;
        }
        return 2;
    }

    private LinearLayout.LayoutParams getAppDrawerParams() {
        return new LinearLayout.LayoutParams(getAppDrawerColumns()
                * mConfiguration.mMaxWidth,
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
        mFavoriteListAdapter.notifyDataSetChanged();
        if (mFavoriteListView != null) {
            mFavoriteListView.setAdapter(mFavoriteListAdapter);
        }
        mRecentListAdapter.notifyDataSetChanged();
        if (mRecentList != null) {
            mRecentList.setAdapter(mRecentListAdapter);
        }
        mAppDrawerListAdapter.notifyDataSetChanged();
        if (mAppDrawer != null) {
            mAppDrawer.setAdapter(mAppDrawerListAdapter);
        }
        createOpenFavoriteButton();
        buildButtonList();
        if (mConfiguration.mShowRambar) {
            addMemoryDisplay();
        }
        addOpenFavoriteButton();
    }

    @Override
    public void updateLayout() {
        try {
            if (mShowing) {
                mFavoriteListAdapter.notifyDataSetChanged();
                mFavoriteListView.setAdapter(mFavoriteListAdapter);

                mRecentListAdapter.notifyDataSetChanged();
                mRecentList.setAdapter(mRecentListAdapter);

                mWindowManager.updateViewLayout(mPopupView,
                        getParams(mConfiguration.mBackgroundOpacity));
                mAppDrawer.requestLayout();
            }
        } catch (Exception e) {
            // ignored
        }
    }

    private PackageTextView getRecentItemTemplate() {
        PackageTextView item = new PackageTextView(mContext);
        if (mConfiguration.mBgStyle == 0) {
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
        item.setBackgroundResource(mConfiguration.mBgStyle == 0 ? R.drawable.ripple_dark
                : R.drawable.ripple_light);
        item.setThumbRatio(mConfiguration.mThumbRatio);
        return item;
    }

    @Override
    protected void flipToAppDrawerNew() {
        mRecents.setVisibility(View.GONE);
        mAppDrawer.setVisibility(View.VISIBLE);
        enableOpenFavoriteButton();
    }

    @Override
    protected void flipToRecentsNew() {
        mAppDrawer.setVisibility(View.GONE);
        mRecents.setVisibility(View.VISIBLE);
        enableOpenFavoriteButton();
    }

    @Override
    protected void toggleFavorites() {
        mShowFavorites = !mShowFavorites;

        if (mShowFavAnim != null) {
            mShowFavAnim.cancel();
        }

        if (mShowFavorites) {
            mFavoriteListView.setVisibility(View.VISIBLE);
            mFavoriteListView.setScaleX(0f);
            mFavoriteListView.setPivotX(0f);
            mShowFavAnim = start(interpolator(
                    mLinearInterpolator,
                    ObjectAnimator.ofFloat(mFavoriteListView, View.SCALE_X, 0f,
                            1f)).setDuration(FAVORITE_DURATION));
        } else {
            mFavoriteListView.setScaleX(1f);
            mFavoriteListView.setPivotX(0f);
            mShowFavAnim = start(setVisibilityWhenDone(
                    interpolator(
                            mLinearInterpolator,
                            ObjectAnimator.ofFloat(mFavoriteListView,
                                    View.SCALE_X, 1f, 0f)).setDuration(
                            FAVORITE_DURATION), mFavoriteListView,
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

    private void addOpenFavoriteButton() {
        mActionList.add(mOpenFavorite);
    }

    @Override
    protected LinearLayout.LayoutParams getButtonListItemParams() {
        int buttonMargin = Math.round(2 * mConfiguration.mDensity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, buttonMargin, 0, buttonMargin);
        return params;
    }

    private void updateStyle() {
        if (mConfiguration.mBgStyle == 0) {
            mButtonListContainer.setBackground(mContext.getResources()
                    .getDrawable(R.drawable.overlay_bg_button_flat));
            mNoRecentApps.setTextColor(Color.BLACK);
            mNoRecentApps.setShadowLayer(0, 0, 0, Color.BLACK);
            mOpenFavorite.setImageDrawable(mContext.getResources().getDrawable(
                    R.drawable.ic_expand_down));
            mButtonListContainer.setOutlineProvider(BUTTON_OUTLINE_PROVIDER);
        } else {
            mButtonListContainer.setBackground(null);
            mNoRecentApps.setTextColor(Color.WHITE);
            mNoRecentApps.setShadowLayer(5, 0, 0, Color.BLACK);
            mOpenFavorite.setImageDrawable(BitmapUtils.shadow(
                    mContext.getResources(),
                    mContext.getResources().getDrawable(
                            R.drawable.ic_expand_down)));
            mButtonListContainer.setOutlineProvider(null);
        }
    }

    private int getExpandRotation() {
        if (mConfiguration.mLocation != 0) {
            return mShowFavorites ? ROTATE_90_DEGREE : ROTATE_270_DEGREE;
        }
        return mShowFavorites ? ROTATE_270_DEGREE : ROTATE_90_DEGREE;
    }

    @Override
    protected int getCurrentOverlayWidth() {
        return getCurrentThumbWidth()
                + (mShowFavorites ? mConfiguration.mMaxWidth : 0)
                + (isButtonVisible() ? mConfiguration.mActionIconSizePx : 0);
    }

    private int getCurrentThumbWidth() {
        return (int)(mConfiguration.mThumbnailWidth * mConfiguration.mThumbRatio) +
               ( mConfiguration.mSideHeader ? mConfiguration.mOverlayIconSizePx : 0);
    }

    private int getCurrentThumbHeight() {
        return (int)(mConfiguration.mThumbnailHeight * mConfiguration.mThumbRatio) +
                (mConfiguration.mSideHeader ? 0 : mConfiguration.mOverlayIconSizePx);
    }

    @Override
    protected void afterShowDone() {
        mShowThumbs = true;
        mRecentListAdapter.notifyDataSetChanged();
    }

    private void addMemoryDisplay() {
        mRamDisplay = new ImageView(mContext);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        mRamDisplay.setLayoutParams(params);
        mRamDisplay.setImageDrawable(BitmapUtils.memImage(mContext.getResources(),
                mConfiguration.mMemDisplaySize, mConfiguration.mDensity,
                mConfiguration.mLayoutStyle == 0, mConfiguration.mBgStyle == 0, "", ""));
        mActionList.add(mRamDisplay);
    }

    @Override
    protected void updateRamDisplay() {
        if (mRamDisplay != null) {
            mHandler.post(mUpdateRamBarTask);
        }
    }
}
