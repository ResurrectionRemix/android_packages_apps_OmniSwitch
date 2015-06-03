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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.omnirom.omniswitch.PackageManager;
import org.omnirom.omniswitch.showcase.ShowcaseView;
import org.omnirom.omniswitch.showcase.ShowcaseView.OnShowcaseEventListener;
import org.omnirom.omniswitch.TaskDescription;
import org.omnirom.omniswitch.Utils;
import org.omnirom.omniswitch.SwitchConfiguration;
import org.omnirom.omniswitch.SwitchManager;
import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.SettingsActivity;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
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

public abstract class AbstractSwitchLayout implements ISwitchLayout,
        OnShowcaseEventListener {
    protected static final int FAVORITE_DURATION = 200;
    protected static final int FLIP_DURATION = 500;
    protected static final int SHOW_DURATION = 350;
    protected static final int SHOW_DURATION_FAST = 200;
    protected static final int HIDE_DURATION = 350;
    protected static final int HIDE_DURATION_FAST = 200;

    protected static final int ROTATE_90_DEGREE = 90;
    protected static final int ROTATE_180_DEGREE = 180;
    protected static final int ROTATE_270_DEGREE = 270;
    protected static final String TAG = "SwitchLayout";
    protected static final boolean DEBUG = false;
    protected static final String KEY_SHOWCASE_FAVORITE = "showcase_favorite_done";

    protected WindowManager mWindowManager;
    protected LayoutInflater mInflater;
    protected Context mContext;
    protected SwitchConfiguration mConfiguration;
    protected SharedPreferences mPrefs;
    protected SwitchManager mRecentsManager;
    protected ImageView mLastAppButton;
    protected ImageView mKillAllButton;
    protected ImageView mKillOtherButton;
    protected ImageView mHomeButton;
    protected ImageView mSettingsButton;
    protected ImageView mAllappsButton;
    protected ImageView mBackButton;
    protected ImageView mLockToAppButton;
    protected ImageView mCloseButton;
    protected boolean mAutoClose = true;
    protected boolean mVirtualBackKey;
    protected TimeInterpolator mAccelerateInterpolator = new AccelerateInterpolator(1.5f);
    protected TimeInterpolator mDecelerateInterpolator = new DecelerateInterpolator(1.5f);
    protected LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    protected Handler mHandler = new Handler();
    protected int mSlop;
    protected List<ImageView> mActionList;
    protected List<String> mFavoriteList;
    protected boolean mHasFavorites;
    protected boolean mButtonsVisible = true;
    protected boolean mShowAppDrawer;
    protected AppDrawerListAdapter mAppDrawerListAdapter;
    protected FavoriteListAdapter mFavoriteListAdapter;
    protected FrameLayout mPopupView;
    protected View mView;
    protected ShowcaseView mShowcaseView;
    protected boolean mShowcaseDone;
    protected float mOpenFavoriteX;
    protected float mOpenFavoriteY;
    protected Animator mToggleOverlayAnim;
    protected float mCurrentSlideWidth;
    protected float mCurrentDistance;
    private float[] mDownPoint = new float[2];
    protected boolean mEnabled;
    private boolean mFlingEnable = true;
    private float mLastX;
    private boolean mDragThreshold;
    protected GestureDetector mGestureDetector;
    protected GridView mAppDrawer;
    protected PopupMenu mPopup;
    private boolean mHandleRecentsUpdate;
    protected boolean mShowing;
    protected boolean mShowFavorites;
    protected boolean mTaskLoadDone;
    protected boolean mUpdateNoRecentsTasksDone;
    protected TextView mNoRecentApps;
    protected LinearLayout mButtonListItems;
    protected LinearLayout mButtonListContainer;
    protected LinearLayout mRecents;
    protected ImageView mOpenFavorite;
    protected Animator mShowFavAnim;

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
                mDragThreshold = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                mEnabled = true;
                mFlingEnable = false;
                mDragThreshold = false;
                break;
            case MotionEvent.ACTION_MOVE:
                mFlingEnable = false;
                if (Math.abs(distanceX) > mSlop) {
                    mDragThreshold = true;
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
                mLastX = xRaw;
                break;
            case MotionEvent.ACTION_UP:
                mFlingEnable = false;
                if (Math.abs(distanceX) > mSlop || mDragThreshold) {
                    finishSlideLayoutHide();
                } else {
                    hide(false);
                }
                mDragThreshold = false;
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

    public class AppDrawerListAdapter extends
            ArrayAdapter<PackageManager.PackageItem> {

        public AppDrawerListAdapter(Context context, int resource,
                List<PackageManager.PackageItem> values) {
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
            PackageManager.PackageItem packageItem = PackageManager
                    .getInstance(mContext).getPackageList().get(position);
            item.setIntent(packageItem.getIntent());
            if (mConfiguration.mShowLabels) {
                item.setText(packageItem.getTitle());
            } else {
                item.setText("");
            }
            Drawable d = BitmapCache.getInstance(mContext).getResized(
                    mContext.getResources(), packageItem, mConfiguration,
                    mConfiguration.mIconSize);
            item.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
            return item;
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
            String intent = mFavoriteList.get(position);

            PackageManager.PackageItem packageItem = PackageManager
                    .getInstance(mContext).getPackageItem(intent);
            item.setIntent(packageItem.getIntent());
            if (mConfiguration.mShowLabels) {
                item.setText(packageItem.getTitle());
            } else {
                item.setText("");
            }
            Drawable d = BitmapCache.getInstance(mContext).getResized(
                    mContext.getResources(), packageItem, mConfiguration,
                    mConfiguration.mIconSize);
            item.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
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
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
        mFavoriteList = new ArrayList<String>();
        mActionList = new ArrayList<ImageView>();
        mAppDrawerListAdapter = new AppDrawerListAdapter(mContext,
                android.R.layout.simple_list_item_single_choice, PackageManager
                        .getInstance(mContext).getPackageList());
        mFavoriteListAdapter = new FavoriteListAdapter(mContext,
                android.R.layout.simple_list_item_single_choice, mFavoriteList);
        mGestureDetector = new GestureDetector(context, mGestureListener);
    }

    @Override
    public abstract void updateLayout();

    protected ImageView getActionButtonTemplate(Drawable image) {
        ImageView item = (ImageView) mInflater.inflate(R.layout.action_button,
                null, false);
        item.setImageDrawable(image);
        return item;
    }

    protected ImageView getActionButton(int buttonId) {
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
                    .getDrawable(R.drawable.lastapp));
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
                    .getDrawable(R.drawable.settings));
            mSettingsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mRecentsManager.startSettingssActivity();
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
                    .getDrawable(R.drawable.ic_allapps));
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
                    .getDrawable(R.drawable.lock_app_pin));
            mLockToAppButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Utils.toggleLockModeOnCurrent(mContext);
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
        mFavoriteList.clear();
        mFavoriteList.addAll(mConfiguration.mFavoriteList);

        List<String> favoriteList = new ArrayList<String>();
        favoriteList.addAll(mFavoriteList);
        Iterator<String> nextFavorite = favoriteList.iterator();
        while (nextFavorite.hasNext()) {
            String intent = nextFavorite.next();
            PackageManager.PackageItem packageItem = PackageManager
                    .getInstance(mContext).getPackageItem(intent);
            if (packageItem == null) {
                Log.d(TAG, "failed to add " + intent);
                mFavoriteList.remove(intent);
            }
        }
        mHasFavorites = mFavoriteList.size() != 0;
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
        item.setLayoutParams(getListItemParams());
        item.setMaxLines(1);
        item.setBackgroundResource(mConfiguration.mBgStyle == 0 ? R.drawable.ripple_dark
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

    protected void handleLongPressRecent(final TaskDescription ad, View view) {
        final Context wrapper = new ContextThemeWrapper(mContext,
                R.style.PopupMenu);
        final PopupMenu popup = new PopupMenu(wrapper, view);
        mPopup = popup;
        popup.getMenuInflater().inflate(R.menu.recent_popup_menu,
                popup.getMenu());
        popup.getMenu().findItem(R.id.package_add_favorite)
                .setEnabled(!mFavoriteList.contains(ad.getIntent().toUri(0)));
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.package_stop_task) {
                    mRecentsManager.killTask(ad);
                } else if (item.getItemId() == R.id.package_inspect_item) {
                    mRecentsManager.startApplicationDetailsActivity(ad
                            .getPackageName());
                } else if (item.getItemId() == R.id.package_add_favorite) {
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

    protected void handleLongPressFavorite(
            final PackageManager.PackageItem packageItem, View view) {
        final Context wrapper = new ContextThemeWrapper(mContext,
                R.style.PopupMenu);
        final PopupMenu popup = new PopupMenu(wrapper, view);
        mPopup = popup;
        popup.getMenuInflater().inflate(R.menu.favorite_popup_menu,
                popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.package_inspect_item) {
                    mRecentsManager.startApplicationDetailsActivity(packageItem
                            .getActivityInfo().packageName);
                } else if (item.getItemId() == R.id.package_remove_favorite) {
                    Utils.removeFromFavorites(mContext,
                            packageItem.getIntent(), mFavoriteList);
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

    protected void handleLongPressAppDrawer(
            final PackageManager.PackageItem packageItem, View view) {
        final Context wrapper = new ContextThemeWrapper(mContext,
                R.style.PopupMenu);
        final PopupMenu popup = new PopupMenu(wrapper, view);
        mPopup = popup;
        popup.getMenuInflater().inflate(R.menu.package_popup_menu,
                popup.getMenu());
        popup.getMenu().findItem(R.id.package_add_favorite)
                .setEnabled(!mFavoriteList.contains(packageItem.getIntent()));
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.package_inspect_item) {
                    mRecentsManager.startApplicationDetailsActivity(packageItem
                            .getActivityInfo().packageName);
                } else if (item.getItemId() == R.id.package_add_favorite) {
                    Log.d(TAG, "add " + packageItem.getIntent());
                    Utils.addToFavorites(mContext, packageItem.getIntent(),
                            mFavoriteList);
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

    protected boolean startShowcaseFavorite() {
        if (!mPrefs.getBoolean(KEY_SHOWCASE_FAVORITE, false)) {
            mPrefs.edit().putBoolean(KEY_SHOWCASE_FAVORITE, true).commit();
            ShowcaseView.ConfigOptions co = new ShowcaseView.ConfigOptions();
            co.hideOnClickOutside = true;

            Point size = new Point();
            mWindowManager.getDefaultDisplay().getSize(size);

            mShowcaseView = ShowcaseView.insertShowcaseView(mOpenFavoriteX,
                    mOpenFavoriteY, mWindowManager, mContext,
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
        if (mConfiguration.mShowRambar) {
            updateRamDisplay();
        }
        updateRecentsAppsList(false);

        mShowing = true;
    }

    protected abstract void updateRamDisplay();
    protected abstract void updateRecentsAppsList(boolean force);

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
    }

    public synchronized void preHide() {
        // to prevent any reentering
        mShowing = false;

        if (mPopup != null) {
            mPopup.dismiss();
        }
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
        afterShowDone();

        if (mHasFavorites && !mShowcaseDone) {
            mPopupView.post(new Runnable() {
                @Override
                public void run() {
                    startShowcaseFavorite();
                }
            });
        }
    }

    @Override
    public synchronized void update() {
        if (DEBUG) {
            Log.d(TAG, "update " + System.currentTimeMillis() + " "
                    + mRecentsManager.getTasks());
        }

        mTaskLoadDone = true;
        if (isHandleRecentsUpdate()) {
            updateRecentsAppsList(true);
        }
    }

    @Override
    public void refresh() {
        if (DEBUG) {
            Log.d(TAG, "refresh");
        }

        mTaskLoadDone = true;
        updateRecentsAppsList(true);
    }

    @Override
    public void shutdownService() {
        // remember on reboot
        mPrefs.edit()
                .putBoolean(SettingsActivity.PREF_SHOW_FAVORITE, mShowFavorites)
                .commit();
    }

    protected abstract LinearLayout.LayoutParams getButtonListItemParams();

    protected void buildButtons() {
        mButtonListItems.removeAllViews();
        Iterator<ImageView> nextButton = mActionList.iterator();
        while (nextButton.hasNext()) {
            ImageView item = nextButton.next();
            mButtonListItems.addView(item, getButtonListItemParams());
        }
    }

    protected void buildButtonList() {
        mActionList.clear();
        Iterator<Integer> nextKey = mConfiguration.mButtons.keySet().iterator();
        while (nextKey.hasNext()) {
            Integer key = nextKey.next();
            Boolean value = mConfiguration.mButtons.get(key);
            if (value) {
                ImageView item = getActionButton(key);
                if (item != null) {
                    mActionList.add(item);
                }
            }
        }
    }

    protected abstract void toggleFavorites();

    protected void createOpenFavoriteButton() {
        mOpenFavorite = getActionButtonTemplate(mContext.getResources()
                .getDrawable(R.drawable.ic_expand_down));

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

    protected void enableOpenFavoriteButton() {
        mOpenFavorite.setVisibility((mHasFavorites && mAppDrawer
                .getVisibility() == View.GONE) ? View.VISIBLE : View.GONE);
    }
}