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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.omnirom.omniswitch.PackageManager;
import org.omnirom.omniswitch.TaskDescription;
import org.omnirom.omniswitch.Utils;
import org.omnirom.omniswitch.SwitchConfiguration;
import org.omnirom.omniswitch.SwitchManager;
import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.SettingsActivity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

public abstract class AbstractSwitchLayout implements ISwitchLayout {
    protected static final int FAVORITE_DURATION = 200;
    protected static final int SHOW_DURATION = 200;
    protected static final int SHOW_DURATION_FAST = 100;
    protected static final int HIDE_DURATION = 200;
    protected static final int HIDE_DURATION_FAST = 100;

    protected static final float ROTATE_0_DEGREE = 0f;
    protected static final float ROTATE_90_DEGREE = 90f;
    protected static final float ROTATE_180_DEGREE = 180f;
    protected static final float ROTATE_270_DEGREE = 270f;
    protected static final float ROTATE_360_DEGREE = 360f;
    protected static final String TAG = "SwitchLayout";
    protected static final boolean DEBUG = false;

    protected WindowManager mWindowManager;
    protected LayoutInflater mInflater;
    protected Context mContext;
    protected SwitchConfiguration mConfiguration;
    protected SharedPreferences mPrefs;
    protected SwitchManager mRecentsManager;
    protected View mLastAppButton;
    protected View mKillAllButton;
    protected View mKillOtherButton;
    protected View mHomeButton;
    protected View mSettingsButton;
    protected View mAllappsButton;
    protected View mBackButton;
    protected View mLockToAppButton;
    protected View mCloseButton;
    protected View mMenuButton;
    protected boolean mAutoClose = true;
    protected boolean mVirtualBackKey;
    protected boolean mVirtualMenuKey;
    protected TimeInterpolator mAccelerateInterpolator = new AccelerateInterpolator(1.5f);
    protected TimeInterpolator mDecelerateInterpolator = new DecelerateInterpolator(1.5f);
    protected LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    protected Handler mHandler = new Handler();
    protected int mSlop;
    protected List<View> mActionList;
    protected List<String> mFavoriteList;
    protected boolean mHasFavorites;
    protected boolean mButtonsVisible = true;
    protected boolean mShowAppDrawer;
    protected FavoriteListAdapter mFavoriteListAdapter;
    protected FrameLayout mPopupView;
    protected View mView;
    protected Animator mToggleOverlayAnim;
    protected float mCurrentSlideWidth;
    protected float mCurrentDistance;
    private float[] mDownPoint = new float[2];
    protected boolean mEnabled;
    private boolean mFlingEnable = true;
    private float mLastX;
    private boolean mMoveStarted;
    protected GestureDetector mGestureDetector;
    protected AppDrawerView mAppDrawer;
    private boolean mHandleRecentsUpdate;
    protected boolean mShowing;
    protected boolean mShowFavorites;
    protected boolean mTaskLoadDone;
    protected boolean mUpdateNoRecentsTasksDone;
    protected TextView mNoRecentApps;
    protected LinearLayout mButtonListItems;
    protected LinearLayout mButtonListContainer;
    protected LinearLayout mButtonListContainerTop;
    protected LinearLayout mButtonListContainerBottom;
    protected LinearLayout mRecents;
    protected View mOpenFavorite;
    protected AnimatorSet mShowFavAnim;
    private Typeface mLabelFont;

    protected GestureDetector.OnGestureListener mGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
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

    protected View.OnTouchListener mDragHandleListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            float xRaw = event.getRawX();
            float yRaw = event.getRawY();
            float distanceX = mDownPoint[0] - xRaw;

            if (DEBUG) {
                Log.d(TAG, "mView onTouch " + action + ":" + (int) xRaw + ":"
                        + (int) yRaw + " " + mEnabled + " " + mFlingEnable);
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
                mFlingEnable = false;
                mLastX = xRaw;
                mMoveStarted = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                mEnabled = true;
                mFlingEnable = false;
                mMoveStarted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                mFlingEnable = false;
                if (Math.abs(distanceX) > mSlop) {
                    if (mLastX > xRaw) {
                        // move left
                        if (mConfiguration.mLocation != 0) {
                            mFlingEnable = true;
                            mMoveStarted = true;
                        }
                    } else {
                        // move right
                        if (mConfiguration.mLocation == 0) {
                            mFlingEnable = true;
                            mMoveStarted = true;
                        }
                    }
                }
                if (mMoveStarted) {
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
                mFlingEnable = false;
                if (mMoveStarted) {
                    finishSlideLayoutHide();
                } else {
                    hide(false);
                }
                mMoveStarted = false;
                break;
            }
            return true;
        }
    };

    public class PopupKeyListener implements View.OnKeyListener {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (DEBUG) {
                Log.d(TAG, "mPopupView onKey " + keyCode);
            }
            if (!mEnabled) {
                return false;
            }
            if (keyCode == KeyEvent.KEYCODE_BACK
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getRepeatCount() == 0) {
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
    }

    public class FavoriteListAdapter extends ArrayAdapter<String> {

        public FavoriteListAdapter(Context context, int resource,
                List<String> values) {
            super(context, R.layout.package_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PackageTextView item = null;
            if (convertView == null) {
                item = getPackageItemTemplate();
            } else {
                item = (PackageTextView) convertView;
            }
            String intent = getItem(position);

            PackageManager.PackageItem packageItem = PackageManager
                    .getInstance(mContext).getPackageItem(intent);
            item.setIntent(packageItem.getIntent());
            if (mConfiguration.mShowLabels) {
                item.setText(packageItem.getTitle());
            } else {
                item.setText("");
            }
            Drawable d = BitmapCache.getInstance(mContext).getPackageIconCached(mContext.getResources(), packageItem, mConfiguration);
            d.setBounds(0, 0, mConfiguration.mIconSizePx, mConfiguration.mIconSizePx);
            item.setCompoundDrawables(null, d, null, null);
            return item;
        }
    }

    public AbstractSwitchLayout(SwitchManager manager, Context context) {
        mContext = context;
        mRecentsManager = manager;
        mWindowManager = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mConfiguration = SwitchConfiguration.getInstance(mContext);
        mLabelFont = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
        mFavoriteList = new ArrayList<String>();
        mActionList = new ArrayList<View>();
        mFavoriteListAdapter = new FavoriteListAdapter(mContext,
                android.R.layout.simple_list_item_single_choice, mFavoriteList);
        mGestureDetector = new GestureDetector(context, mGestureListener);
        mGestureDetector.setIsLongpressEnabled(false);
        updateFavoritesList();
    }

    @Override
    public void updateLayout() {
        try {
            if (mAppDrawer != null) {
                mAppDrawer.setLayoutParams(getAppDrawerParams());
            }
            if (mPopupView != null) {
                mWindowManager.updateViewLayout(mPopupView,
                        getParams(mConfiguration.mBackgroundOpacity));
            }
        } catch (Exception e) {
            // ignored
        }
    }

    protected View getActionButtonTemplate(Drawable image) {
        View v = mInflater.inflate(R.layout.action_button, null, false);
        ImageView item = (ImageView) v.findViewById(R.id.action_button_image);
        BitmapUtils.colorize(mContext.getResources(), mContext.getResources().getColor(R.color.text_color_dark), image);
        item.setImageDrawable(image);
        return v;
    }

    protected View getActionButton(int buttonId) {
        if (buttonId == SettingsActivity.BUTTON_HOME) {
            mHomeButton = getActionButtonTemplate(mContext.getResources()
                    .getDrawable(R.drawable.ic_sysbar_home));
            mHomeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.goHome(mAutoClose);
                }
            });
            mHomeButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(
                            mContext,
                            mContext.getResources().getString(
                                    R.string.home_help), Toast.LENGTH_SHORT)
                            .show();
                    return true;
                }
            });
            return mHomeButton;
        }

        if (buttonId == SettingsActivity.BUTTON_TOGGLE_APP) {
            mLastAppButton = getActionButtonTemplate(mContext.getResources()
                    .getDrawable(R.drawable.ic_lastapp));
            mLastAppButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.toggleLastApp(mAutoClose);
                }
            });
            mLastAppButton
                    .setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Toast.makeText(
                                    mContext,
                                    mContext.getResources().getString(
                                            R.string.toggle_last_app_help),
                                    Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
            return mLastAppButton;
        }

        if (buttonId == SettingsActivity.BUTTON_KILL_ALL) {
            mKillAllButton = getActionButtonTemplate(mContext.getResources()
                    .getDrawable(R.drawable.kill_all));
            mKillAllButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.killAll(mAutoClose);
                }
            });

            mKillAllButton
                    .setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Toast.makeText(
                                    mContext,
                                    mContext.getResources().getString(
                                            R.string.kill_all_apps_help),
                                    Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
            return mKillAllButton;
        }

        if (buttonId == SettingsActivity.BUTTON_KILL_OTHER) {
            mKillOtherButton = getActionButtonTemplate(mContext.getResources()
                    .getDrawable(R.drawable.kill_other));
            mKillOtherButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.killOther(mAutoClose);
                }
            });
            mKillOtherButton
                    .setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Toast.makeText(
                                    mContext,
                                    mContext.getResources().getString(
                                            R.string.kill_other_apps_help),
                                    Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
            return mKillOtherButton;
        }

        if (buttonId == SettingsActivity.BUTTON_SETTINGS) {
            mSettingsButton = getActionButtonTemplate(mContext.getResources()
                    .getDrawable(R.drawable.ic_settings_small));
            mSettingsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.startSettingsActivity();
                }
            });
            mSettingsButton
                    .setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            mRecentsManager.startOmniSwitchSettingsActivity();
                            return true;
                        }
                    });
            return mSettingsButton;
        }

        if (buttonId == SettingsActivity.BUTTON_ALLAPPS) {
            mAllappsButton = getActionButtonTemplate(mContext.getResources()
                    .getDrawable(R.drawable.ic_apps));
            mAllappsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleAppdrawer();
                }
            });
            mAllappsButton
                    .setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Toast.makeText(
                                    mContext,
                                    mContext.getResources().getString(
                                            R.string.allapps_help),
                                    Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
            return mAllappsButton;
        }

        if (buttonId == SettingsActivity.BUTTON_BACK) {
            mBackButton = getActionButtonTemplate(mContext.getResources()
                    .getDrawable(R.drawable.ic_sysbar_back));
            mBackButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mVirtualBackKey = true;
                    hide(false);
                }
            });
            mBackButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(
                            mContext,
                            mContext.getResources().getString(
                                    R.string.back_help), Toast.LENGTH_SHORT)
                            .show();
                    return true;
                }
            });
            return mBackButton;
        }

        if (buttonId == SettingsActivity.BUTTON_LOCK_APP) {
            mLockToAppButton = getActionButtonTemplate(mContext.getResources()
                    .getDrawable(R.drawable.ic_pin));
            mLockToAppButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!Utils.isLockToAppEnabled(mContext)) {
                        Toast.makeText(
                                mContext,
                                mContext.getResources().getString(
                                        R.string.lock_app_not_enabled),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mRecentsManager.toggleLockToApp(mAutoClose);
                }
            });
            mLockToAppButton
                    .setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Toast.makeText(
                                    mContext,
                                    mContext.getResources().getString(
                                            R.string.lock_app_help),
                                    Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
            return mLockToAppButton;
        }

        if (buttonId == SettingsActivity.BUTTON_CLOSE) {
            mCloseButton = getActionButtonTemplate(mContext.getResources()
                    .getDrawable(R.drawable.ic_close));
            mCloseButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.hide(false);
                }
            });
            mCloseButton
                    .setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Toast.makeText(
                                    mContext,
                                    mContext.getResources().getString(
                                            R.string.close_help),
                                    Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
            return mCloseButton;
        }
        if (buttonId == SettingsActivity.BUTTON_MENU) {
            mMenuButton = getActionButtonTemplate(mContext.getResources()
                    .getDrawable(R.drawable.ic_menu));
            mMenuButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mVirtualMenuKey = true;
                    hide(false);
                }
            });
            mMenuButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(
                            mContext,
                            mContext.getResources().getString(
                                    R.string.menu_help), Toast.LENGTH_SHORT)
                            .show();
                    return true;
                }
            });
            return mMenuButton;
        }

        return null;
    }

    protected Animator setVisibilityWhenDone(final Animator a, final View v,
            final int vis) {
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                v.setVisibility(vis);
            }
        });
        return a;
    }

    protected Animator interpolator(TimeInterpolator ti, Animator a) {
        a.setInterpolator(ti);
        return a;
    }

    protected Animator startDelay(int d, Animator a) {
        a.setStartDelay(d);
        return a;
    }

    protected Animator start(Animator a) {
        a.start();
        return a;
    }

    @Override
    public void updatePrefs(SharedPreferences prefs, String key) {
        if (DEBUG) {
            Log.d(TAG, "updatePrefs " + key);
        }

        if (key != null && key.equals(SettingsActivity.PREF_FAVORITE_APPS)) {
            updateFavoritesList();
        }

        if (mAppDrawer != null) {
            mAppDrawer.updatePrefs(prefs, key);
        }
    }

    protected abstract void flipToAppDrawerNew();

    protected abstract void flipToRecentsNew();

    protected void toggleAppdrawer() {
        mShowAppDrawer = !mShowAppDrawer;
        if (mShowAppDrawer) {
            flipToAppDrawerNew();
        } else {
            flipToRecentsNew();
        }
    }

    protected PackageTextView getPackageItemTemplate() {
        PackageTextView item = new PackageTextView(mContext);
        if (mConfiguration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_LIGHT) {
            item.setTextColor(mContext.getResources().getColor(R.color.text_color_light));
            item.setShadowLayer(0, 0, 0, Color.BLACK);
        } else {
            item.setTextColor(mContext.getResources().getColor(R.color.text_color_dark));
            item.setShadowLayer(5, 0, 0, Color.BLACK);
        }
        item.setTextSize(mConfiguration.mLabelFontSize);
        item.setEllipsize(TextUtils.TruncateAt.END);
        item.setGravity(Gravity.CENTER);
        item.setLayoutParams(getListItemParams());
        item.setPadding(0, mConfiguration.mIconBorderPx, 0, 0);
        item.setMaxLines(1);
        item.setTypeface(mLabelFont);
        item.setBackgroundResource(mConfiguration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_LIGHT ? R.drawable.ripple_dark
                : R.drawable.ripple_light);
        return item;
    }

    protected abstract LinearLayout.LayoutParams getListItemParams();

    public void slideLayoutHide(float distanceX) {
        if (DEBUG) {
            Log.d(TAG, "slideLayoutHide " + distanceX);
        }
        mCurrentDistance = Math.abs(distanceX);
        mCurrentSlideWidth = distanceX;
        mView.setTranslationX(mCurrentSlideWidth);
        if (DEBUG) {
            Log.d(TAG, "slideLayoutHide " + mCurrentSlideWidth);
        }
    }

    protected abstract int getCurrentOverlayWidth();

    public void finishSlideLayoutHide() {
        if (DEBUG) {
            Log.d(TAG, "finishSlideLayoutHide " + mCurrentDistance);
        }
        if (mCurrentDistance > getCurrentOverlayWidth() / 2) {
            finishOverlaySlide(false, false);
        } else {
            finishOverlaySlide(true, false);
        }
    }

    public void finishSlideLayout() {
        if (DEBUG) {
            Log.d(TAG, "finishSlideLayout " + mCurrentDistance);
        }
        if (mCurrentDistance > getCurrentOverlayWidth() / 2) {
            finishOverlaySlide(true, false);
        } else {
            finishOverlaySlide(false, false);
        }
    }

    @Override
    public void openSlideLayout(boolean fromFling) {
        finishOverlaySlide(true, fromFling);
    }

    @Override
    public void canceSlideLayout() {
        finishOverlaySlide(false, false);
    }

    public void slideLayout(float distanceX) {
        if (DEBUG) {
            Log.d(TAG, "slideLayout " + distanceX);
        }
        mCurrentDistance = Math.abs(distanceX);
        if (mConfiguration.mLocation == 0) {
            mCurrentSlideWidth = getCurrentOverlayWidth() - distanceX;
            if (mCurrentSlideWidth > 0) {
                mView.setTranslationX(mCurrentSlideWidth);
            } else {
                mCurrentSlideWidth = 0;
                mView.setTranslationX(mCurrentSlideWidth);
            }
        } else {
            mCurrentSlideWidth = -getCurrentOverlayWidth() + distanceX;
            if (mCurrentSlideWidth < 0) {
                mView.setTranslationX(mCurrentSlideWidth);
            } else {
                mCurrentSlideWidth = 0;
                mView.setTranslationX(mCurrentSlideWidth);
            }
        }
        if (DEBUG) {
            Log.d(TAG, "slideLayout " + mCurrentSlideWidth);
        }
    }

    protected abstract void afterShowDone();

    private void finishOverlaySlide(final boolean show, boolean fromFling) {
        if (DEBUG) {
            Log.d(TAG, "finishOverlaySlide " + show + " " + fromFling + " "
                    + mCurrentSlideWidth);
        }
        if (mToggleOverlayAnim != null) {
            mToggleOverlayAnim.cancel();
        }

        mEnabled = false;

        if (show) {
            mToggleOverlayAnim = start(interpolator(
                    mDecelerateInterpolator,
                    ObjectAnimator.ofFloat(mView, View.TRANSLATION_X,
                            mCurrentSlideWidth, 0)).setDuration(
                    fromFling ? SHOW_DURATION_FAST : SHOW_DURATION));
        } else {
            int endValue = 0;
            if (mConfiguration.mLocation == 0) {
                endValue = getCurrentOverlayWidth();
            } else {
                endValue = -getCurrentOverlayWidth();
            }
            mToggleOverlayAnim = start(interpolator(
                    mAccelerateInterpolator,
                    ObjectAnimator.ofFloat(mView, View.TRANSLATION_X,
                            mCurrentSlideWidth, endValue)).setDuration(
                    fromFling ? HIDE_DURATION_FAST : HIDE_DURATION));
        }

        mToggleOverlayAnim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!show) {
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

    protected void handleLongPressRecent(final TaskDescription ad, final View view) {
        final Context wrapper = new ContextThemeWrapper(mContext,
                mConfiguration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_LIGHT
                ? R.style.PopupMenuLight : R.style.PopupMenuDark);
        final String intentStr = getRecentsItemIntent(ad);
        final PopupMenu popup = new PopupMenu(wrapper, view);
        popup.getMenuInflater().inflate(R.menu.recent_popup_menu,
                popup.getMenu());
        boolean addFavEnabled = intentStr != null && !mFavoriteList.contains(intentStr);
        if (!addFavEnabled) {
            popup.getMenu().removeItem(R.id.package_add_favorite);
        }
        if (!Utils.isLockToAppEnabled(mContext)) {
            popup.getMenu().removeItem(R.id.package_lock_task);
        }
        if (!Utils.isMultiStackEnabled()) {
            popup.getMenu().removeItem(R.id.package_dock_task);
        } else {
            if (Utils.isDockingActive()) {
                if (mRecentsManager.isDockedTask(ad)) {
                    popup.getMenu().findItem(R.id.package_dock_task).setTitle(R.string.package_undock_task_title);
                }
            }
        }
        String packageName = ad.getPackageName();
        final boolean isLockedApp = ad.isLocked();
        if (isLockedApp) {
            popup.getMenu().findItem(R.id.package_lock_app).setTitle(R.string.package_unlock_app_title);
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.package_stop_task) {
                    mRecentsManager.killTask(ad, true);
                } else if (item.getItemId() == R.id.package_force_stop) {
                    mRecentsManager.forceStop(ad, true);
                } else if (item.getItemId() == R.id.package_inspect_item) {
                    mRecentsManager.startApplicationDetailsActivity(ad
                            .getPackageName());
                } else if (item.getItemId() == R.id.package_lock_app) {
                    mRecentsManager.toggleLockedApp(ad, isLockedApp, true);
                } else if (item.getItemId() == R.id.package_add_favorite) {
                    if (intentStr == null) {
                        Log.d(TAG, "failed to add " + ad.getIntent().toUri(0));
                        return false;
                    }
                    Utils.addToFavorites(mContext, intentStr, mFavoriteList);
                } else if (item.getItemId() == R.id.package_lock_task) {
                    if (!Utils.isLockToAppEnabled(mContext)) {
                        return false;
                    }
                    mRecentsManager.stopLockToApp(false);
                    mRecentsManager.lockToApp(ad, mAutoClose);
                } else if (item.getItemId() == R.id.package_dock_task) {
                    mRecentsManager.dockTask(ad, mAutoClose);
                } else {
                    return false;
                }
                return true;
            }
        });
        popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
            public void onDismiss(PopupMenu menu) {
            }
        });
        popup.show();
    }

    private String getRecentsItemIntent(final TaskDescription ad) {
        try {
            Intent intent = ad.getIntent();
            String intentStr = intent.toUri(0);
            PackageManager.PackageItem packageItem = PackageManager
                    .getInstance(mContext).getPackageItem(intentStr);
            if (packageItem == null) {
                // find a matching available package by matching thing
                // like
                // package name
                packageItem = PackageManager.getInstance(mContext)
                        .getPackageItemByComponent(intent);
                if (packageItem == null) {
                    return null;
                }
                intentStr = packageItem.getIntent();
            }
            return intentStr;
        } catch (Exception e) {
            // toUri can throw an exception
            return null;
        }
    }

    protected void handleLongPressFavorite(final PackageManager.PackageItem packageItem, View view) {
        ContextMenuUtils.handleLongPressFavorite(mContext, packageItem, view,
                mRecentsManager, mFavoriteList);
    }

    @Override
    public void setHandleRecentsUpdate(boolean handleRecentsUpdate) {
        mHandleRecentsUpdate = handleRecentsUpdate;
    }

    @Override
    public boolean isHandleRecentsUpdate() {
        return mHandleRecentsUpdate;
    }

    public boolean isShowing() {
        return mShowing;
    }

    protected abstract void initView();

    protected abstract void createView();

    protected abstract WindowManager.LayoutParams getParams(float dimAmount);

    protected void toggleOverlay(final boolean show) {
        if (mToggleOverlayAnim != null) {
            mToggleOverlayAnim.cancel();
        }

        if (show) {
            int startValue = 0;
            if (mConfiguration.mLocation == 0) {
                startValue = getCurrentOverlayWidth();
            } else {
                startValue = -getCurrentOverlayWidth();
            }
            mView.setTranslationX(startValue);
            mToggleOverlayAnim = start(interpolator(
                    mDecelerateInterpolator,
                    ObjectAnimator.ofFloat(mView, View.TRANSLATION_X,
                            startValue, 0)).setDuration(SHOW_DURATION));
        } else {
            int endValue = 0;
            if (mConfiguration.mLocation == 0) {
                endValue = getCurrentOverlayWidth();
            } else {
                endValue = -getCurrentOverlayWidth();
            }
            mView.setTranslationX(0);
            mToggleOverlayAnim = start(interpolator(
                    mAccelerateInterpolator,
                    ObjectAnimator.ofFloat(mView, View.TRANSLATION_X, 0,
                            endValue)).setDuration(HIDE_DURATION));
        }

        mToggleOverlayAnim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!show) {
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

    protected synchronized void showDone() {
        if (DEBUG) {
            Log.d(TAG, "showDone " + System.currentTimeMillis());
        }
        preShowDone();
        postShowDone();
    }

    protected synchronized void preShowDone() {
        if (DEBUG) {
            Log.d(TAG, "preShowDone " + System.currentTimeMillis());
        }
        mPopupView.setFocusableInTouchMode(true);
        updateRecentsAppsList(false, false);

        mShowing = true;
    }

    protected abstract void updateRamDisplay();
    protected abstract void updateRecentsAppsList(boolean force, boolean refresh);

    protected synchronized void hideDone() {
        if (DEBUG) {
            Log.d(TAG, "hideDone " + System.currentTimeMillis());
        }

        setHandleRecentsUpdate(false);

        try {
            mWindowManager.removeView(mPopupView);
        } catch (Exception e) {
            // ignored
        }

        // reset
        mTaskLoadDone = false;
        mUpdateNoRecentsTasksDone = false;

        mRecentsManager.getSwitchGestureView().overlayHidden();

        // run back trigger if required
        if (mVirtualBackKey && !mConfiguration.mRestrictedMode) {
            Utils.triggerVirtualKeypress(mHandler, KeyEvent.KEYCODE_BACK);
        }
        mVirtualBackKey = false;
        if (mVirtualMenuKey && !mConfiguration.mRestrictedMode) {
            Utils.triggerVirtualKeypress(mHandler, KeyEvent.KEYCODE_MENU);
        }
        mVirtualMenuKey = false;
    }

    public synchronized void preHide() {
        // to prevent any reentering
        mShowing = false;

        try {
            if (mConfiguration.mDimBehind) {
                // TODO workaround for flicker on launcher screen
                mWindowManager.updateViewLayout(mPopupView, getParams(0));
            }
        } catch (Exception e) {
            // ignored
        }
    }

    @Override
    public synchronized void hide(boolean fast) {
        if (!mShowing) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "hide " + fast);
        }
        preHide();
        if (mConfiguration.mAnimate && !fast) {
            toggleOverlay(false);
        } else {
            hideDone();
        }
    }

    @Override
    public synchronized void hideHidden() {
        if (!mShowing) {
            return;
        }
        preHide();
        hideDone();
    }

    protected boolean isButtonVisible() {
        return mButtonListItems.getChildCount() != 0;
    }

    protected synchronized void preShow() {
        if (mPopupView == null) {
            createView();
        }
        initView();

        if (DEBUG) {
            Log.d(TAG, "show " + System.currentTimeMillis());
        }

        try {
            mWindowManager.addView(mPopupView,
                    getParams(mConfiguration.mBackgroundOpacity));
        } catch (Exception e) {
            // something went wrong - try to recover here
            mWindowManager.removeView(mPopupView);
            mWindowManager.addView(mPopupView,
                    getParams(mConfiguration.mBackgroundOpacity));
        }
    }

    @Override
    public synchronized void showHidden() {
        if (mShowing) {
            return;
        }

        preShow();

        int startValue = 0;
        if (mConfiguration.mLocation == 0) {
            startValue = getCurrentOverlayWidth();
        } else {
            startValue = -getCurrentOverlayWidth();
        }
        mView.setTranslationX(startValue);

        preShowDone();
    }

    @Override
    public synchronized void show() {
        if (mShowing) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "show");
        }
        preShow();

        if (mConfiguration.mAnimate) {
            toggleOverlay(true);
        } else {
            showDone();
        }
    }

    protected synchronized void postShowDone() {
        if (DEBUG) {
            Log.d(TAG, "postShowDone " + System.currentTimeMillis());
        }
        mRecentsManager.getSwitchGestureView().overlayShown();
        mCurrentDistance = 0;
        mCurrentSlideWidth = 0;
        mEnabled = true;
        if (mConfiguration.mShowRambar) {
            updateRamDisplay();
        }
        afterShowDone();
    }

    @Override
    public synchronized void update() {
        if (DEBUG) {
            Log.d(TAG, "update " + System.currentTimeMillis() + " "
                    + mRecentsManager.getTasks());
        }

        mTaskLoadDone = true;
        if (isHandleRecentsUpdate()) {
            updateRecentsAppsList(true, false);
        }
    }

    @Override
    public void refresh() {
        if (DEBUG) {
            Log.d(TAG, "refresh");
        }

        mTaskLoadDone = true;
        updateRecentsAppsList(true, true);
    }

    @Override
    public void shutdownService() {
    }

    protected void storeExpandedFavoritesState() {
        mPrefs.edit()
                .putBoolean(SettingsActivity.PREF_SHOW_FAVORITE, mShowFavorites)
                .commit();
    }

    protected void buildButtons() {
        if (DEBUG) {
            Log.d(TAG, "buildButtons");
        }
        mButtonListItems.removeAllViews();
        Iterator<View> nextButton = mActionList.iterator();
        while (nextButton.hasNext()) {
            View item = nextButton.next();
            mButtonListItems.addView(item);
        }
        mButtonListContainer.setVisibility(mActionList.size() == 0 ? View.GONE : View.VISIBLE);
    }

    protected void buildButtonList() {
        mActionList.clear();
        Iterator<Integer> nextKey = mConfiguration.mButtons.keySet().iterator();
        while (nextKey.hasNext()) {
            Integer key = nextKey.next();
            Boolean value = mConfiguration.mButtons.get(key);
            if (value) {
                View item = getActionButton(key);
                if (item != null) {
                    mActionList.add(item);
                }
            }
        }
    }

    protected abstract void toggleFavorites();

    protected void enableOpenFavoriteButton(boolean visible) {
        mOpenFavorite.setVisibility(mHasFavorites && visible ? View.VISIBLE : View.GONE);
    }

    private void updateFavoritesList() {
        Utils.updateFavoritesList(mContext, mConfiguration, mFavoriteList);
        if (DEBUG) {
            Log.d(TAG, "updateFavoritesList " + mFavoriteList);
        }
        mFavoriteListAdapter.notifyDataSetChanged();
        mHasFavorites = mFavoriteList.size() != 0;
    }

    /**
     * changing one of those keys requires full update of contents
     * TODO I have not found a better seolution then calling setAdapter again
     */
    protected boolean isPrefKeyForForceUpdate(String key) {
        return Utils.isPrefKeyForForceUpdate(key);
    }

    protected abstract View getButtonList();

    protected void selectButtonContainer() {
        if (mConfiguration.mButtonPos == 0) {
            mButtonListContainerTop.removeAllViews();
            mButtonListContainerBottom.removeAllViews();
            mButtonListContainerBottom.setVisibility(View.GONE);
            mButtonListContainerTop.addView(getButtonList());
            mButtonListContainerTop.setVisibility(View.VISIBLE);
            mButtonListContainer = mButtonListContainerTop;
       } else {
            mButtonListContainerTop.removeAllViews();
            mButtonListContainerBottom.removeAllViews();
            mButtonListContainerTop.setVisibility(View.GONE);
            mButtonListContainerBottom.addView(getButtonList());
            mButtonListContainerBottom.setVisibility(View.VISIBLE);
            mButtonListContainer = mButtonListContainerBottom;
        }
    }

    /* if quick switcher was triggerd update() will be called
    but the values never reset since hideDone() is not called */
    public void resetRecentsState() {
        mTaskLoadDone = false;
        mUpdateNoRecentsTasksDone = false;
    }

    protected abstract LinearLayout.LayoutParams getAppDrawerParams();

    protected void updatePinAppButton() {
        if (mLockToAppButton != null) {
            ImageView lockAppButtonImage = (ImageView) mLockToAppButton.findViewById(R.id.action_button_image);
            Drawable image = null;
            if (Utils.isInLockTaskMode()) {
                image = mContext.getResources().getDrawable(R.drawable.ic_pin_off);
            } else {
                image = mContext.getResources().getDrawable(R.drawable.ic_pin);
            }
            BitmapUtils.colorize(mContext.getResources(), mContext.getResources().getColor(R.color.text_color_dark), image);
            lockAppButtonImage.setImageDrawable(image);
        }
    }

    protected int getHorizontalGravity() {
        if (mConfiguration.mLocation == 0) {
            return Gravity.RIGHT;
        } else {
            return Gravity.LEFT;
        }
    }
}
