/*
 *  Copyright (C) 2016 The OmniROM Project
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
package org.omnirom.omniswitch.launcher;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.TaskStackBuilder;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.PopupMenu;

import org.omnirom.omniswitch.IEditFavoriteActivity;
import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.PackageManager;
import org.omnirom.omniswitch.RecentTasksLoader;
import org.omnirom.omniswitch.SettingsActivity;
import org.omnirom.omniswitch.SwitchConfiguration;
import org.omnirom.omniswitch.SwitchManager;
import org.omnirom.omniswitch.SwitchService;
import org.omnirom.omniswitch.Utils;
import org.omnirom.omniswitch.ui.AppDrawerView;
import org.omnirom.omniswitch.ui.FavoriteDialog;
import org.omnirom.omniswitch.ui.FavoriteViewHorizontal;
import org.omnirom.omniswitch.ui.FavoriteView;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Launcher extends Activity implements IEditFavoriteActivity {
    private static final String TAG = "Launcher";
    private static final boolean DEBUG = false;
    public static final String WECLOME_SCREEN_DISMISSED = "weclome_screen_dismissed";
    // false= collapsed true = expanded
    public static final String STATE_ESSENTIALS_EXPANDED = "state_essentials_expanded";
    // 0 1=fav 2=app drawer
    public static final String STATE_PANEL_SHOWN = "state_panel_shown";

    private static final Intent PHONE_INTENT = new Intent(Intent.ACTION_DIAL);
    private static final float ROTATE_0_DEGREE = 0f;
    private static final float ROTATE_180_DEGREE = 180f;
    private static final float DIM_AMOUNT = 0.2f;

    private boolean mAttached;
    private View mRootView;
    private ViewGroup mAppDrawerPanel;
    private AppDrawerView mAppDrawer;
    private boolean mAppDrawerPanelVisibile;
    private SharedPreferences mPrefs;
    private List<String> mFavoriteList;
    private SwitchConfiguration mConfiguration;
    private Handler mHandler = new Handler();
    private float[] mInitDownPoint = new float[2];
    private boolean mFlingEnable;
    protected boolean mEnabled;
    private int mSlop;
    private float mLastX;
    private LauncherClings mWelcomeCling;
    private boolean mLongPress;
    private boolean mMoveStarted;
    private boolean mWrongMoveStarted;
    private ImageView mAppDrawerButton;
    private View mRootBottomView;
    private ImageView mFavoriteButton;
    private boolean mFavoritePanelVisibile;
    private ViewGroup mFavoritePanel;
    private ImageView mFavoriteEditButton;
    private View mFavoriteEditButtonSpace;
    private FavoriteView mFavoriteGrid;
    private ImageView mPhoneButton;
    private ViewGroup mEssentialsPanel;
    private ImageView mEssentialsButton;
    private boolean mEssentialsPanelVisibile;
    private ImageView mCameraButton;

    private Runnable mLongPressRunnable = new Runnable(){
    @Override
    public void run() {
        if (DEBUG){
            Log.d(TAG, "mLongPressRunnable");
        }
        if (getRecentsManager() != null) {
            getRecentsManager().hideHidden();
        }
        mLongPress = true;
    }};

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
                if (getRecentsManager() != null) {
                    getRecentsManager().openSlideLayout(true);
                }
            }
            return false;
        }
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    };

    private final OnSharedPreferenceChangeListener mPrefsListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (DEBUG) {
                Log.d(TAG, "updatePrefs " + key);
            }
            try {
                if (key != null && key.equals(SettingsActivity.PREF_FAVORITE_APPS)) {
                    updateFavoritesList();
                }
                mAppDrawer.updatePrefs(prefs, key);
                mFavoriteGrid.updatePrefs(prefs, key);
            } catch(Exception e) {
                Log.e(TAG, "updatePrefs", e);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFavoriteList = new ArrayList<String>();
        mConfiguration = SwitchConfiguration.getInstance(this);
        ViewConfiguration vc = ViewConfiguration.get(this);
        mSlop = vc.getScaledTouchSlop() / 2;
        mGestureDetector = new GestureDetector(this, mGestureListener);
        mGestureDetector.setIsLongpressEnabled(false);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mEssentialsPanelVisibile = isEssentialsExpanded();
        final int activePanel = getActivePanel();
        if (activePanel == 1) {
            mFavoritePanelVisibile = true;
        } else if (activePanel == 2) {
            mAppDrawerPanelVisibile = true;
        }
        initView();
        restoreState();

        updateFavoritesList();

        if (shouldShowIntroScreen()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getWindow().setDimAmount(DIM_AMOUNT);
            mWelcomeCling = new LauncherClings(this);
            mWelcomeCling.showWelcomeCling();
        }
    }

    private void initView() {
        setContentView(R.layout.launcher);
        mRootView = findViewById(R.id.root);
        mRootBottomView = findViewById(R.id.root_bottom);
        mRootBottomView.setAlpha(1f);

        mAppDrawerPanel = (ViewGroup) findViewById(R.id.app_drawer_panel);
        mAppDrawerPanel.setAlpha(1f);

        mFavoritePanel = (ViewGroup) findViewById(R.id.favorite_panel);
        mFavoritePanel.setAlpha(1f);

        mEssentialsPanel= (ViewGroup) findViewById(R.id.essentials_panel);
        mEssentialsPanel.setAlpha(1f);

        View wallpaperButton = findViewById(R.id.wallpaper_button);
        wallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WallpaperManager wpm = WallpaperManager.getInstance(Launcher.this);
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                Point size = new Point();
                wm.getDefaultDisplay().getSize(size);
                wpm.suggestDesiredDimensions(size.x, size.y);
                Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(pickWallpaper);
            }
        });
        wallpaperButton.setBackgroundResource(R.drawable.ripple_dark);

        View settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SwitchManager.startSettingsActivity(Launcher.this);
            }
        });
        settingsButton.setBackgroundResource(R.drawable.ripple_dark);

        View assistButton = findViewById(R.id.assist_button);
        assistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAssist();
            }
        });
        assistButton.setBackgroundResource(R.drawable.ripple_dark);

        View voiceAssistButton = findViewById(R.id.voice_assist_button);
        voiceAssistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchVoiceAssist();
            }
        });
        voiceAssistButton.setBackgroundResource(R.drawable.ripple_dark);

        mFavoriteEditButton = (ImageView)findViewById(R.id.edit_favorite_button);
        mFavoriteEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FavoriteDialog dialog = new FavoriteDialog(Launcher.this, Launcher.this, mFavoriteList);
                dialog.show();
            }
        });
        mFavoriteEditButton.setBackgroundResource(R.drawable.ripple_dark);

        mFavoriteEditButtonSpace = findViewById(R.id.edit_favorite_button_space);

        mAppDrawerButton = (ImageView)findViewById(R.id.app_drawer_button);
        mAppDrawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mAppDrawerPanelVisibile) {
                    showAppDrawerPanel();
                } else {
                    hideAppDrawerPanel(true, true);
                }
            }
        });
        mAppDrawerButton.setBackgroundResource(R.drawable.ripple_dark);

        mFavoriteButton = (ImageView)findViewById(R.id.favorite_button);
        mFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mFavoritePanelVisibile) {
                    showFavoritePanel();
                } else {
                    hideFavoritePanel(true, true);
                }
            }
        });
        mFavoriteButton.setBackgroundResource(R.drawable.ripple_dark);

        mEssentialsButton = (ImageView)findViewById(R.id.essentials_button);
        mEssentialsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mEssentialsPanelVisibile) {
                    showEssentialsPanel();
                } else {
                    hideEssentialsPanel();
                }
            }
        });
        mEssentialsButton.setBackgroundResource(R.drawable.ripple_dark);

        mPhoneButton = (ImageView)findViewById(R.id.phone_button);
        mPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPhone();
            }
        });
        mPhoneButton.setBackgroundResource(R.drawable.ripple_dark);

        ImageView cameraButton = (ImageView)findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCamera();
            }
        });
        cameraButton.setBackgroundResource(R.drawable.ripple_dark);

        if (isPhoneVisible()) {
            mPhoneButton.setVisibility(View.VISIBLE);
            View phoneSpace = findViewById(R.id.phone_button_space);
            phoneSpace.setVisibility(View.VISIBLE);
        }
        if (isDeviceProvisioned()) {
            if (canLaunchAssist()) {
                assistButton.setVisibility(View.VISIBLE);
                View assistSpace = findViewById(R.id.assist_button_space);
                assistSpace.setVisibility(View.VISIBLE);
            }
            if (canLaunchVoiceAssist()) {
                voiceAssistButton.setVisibility(View.VISIBLE);
                View voiceAssistSpace = findViewById(R.id.voice_assist_button_space);
                voiceAssistSpace.setVisibility(View.VISIBLE);
            }
        }

        mRootView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                float xRaw = event.getRawX();
                float yRaw = event.getRawY();
                float distanceX = mInitDownPoint[0] - xRaw;

                if (isShowingIntroScreen()) {
                    return true;
                }
                if (getRecentsManager() == null) {
                    return true;
                }
                if(DEBUG){
                    Log.d(TAG, "mRootView onTouch " + action + ":" + (int)xRaw + ":" + (int)yRaw + " mFlingEnable=" + mFlingEnable +
                            " mEnabled=" + mEnabled +  " mMoveStarted=" + mMoveStarted + " mLongPress=" + mLongPress +
                            " mWrongMoveStarted=" + mWrongMoveStarted);
                }
                if (mFlingEnable) {
                    mGestureDetector.onTouchEvent(event);
                }
                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    mFlingEnable = false;
                    mEnabled = true;
                    mLongPress = false;
                    mMoveStarted = false;
                    mWrongMoveStarted = false;

                    if (getRecentsManager() != null) {
                        getRecentsManager().clearTasks();
                        RecentTasksLoader.getInstance(Launcher.this).cancelLoadingTasks();
                        RecentTasksLoader.getInstance(Launcher.this).setSwitchManager(getRecentsManager());
                        RecentTasksLoader.getInstance(Launcher.this).preloadTasks();
                    }

                    mInitDownPoint[0] = xRaw;
                    mInitDownPoint[1] = yRaw;
                    mLastX = xRaw;
                    mHandler.postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
                    break;
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    mHandler.removeCallbacks(mLongPressRunnable);
                    mFlingEnable = false;
                    mEnabled = true;
                    mLongPress = false;
                    mMoveStarted = false;
                    mWrongMoveStarted = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!mEnabled){
                        return true;
                    }
                    v.setPressed(false);
                    mFlingEnable = false;
                    if (Math.abs(distanceX) > mSlop) {
                        mHandler.removeCallbacks(mLongPressRunnable);
                        if (mLastX > xRaw) {
                            // move left
                            if (mConfiguration.mLocation == 0 && !mWrongMoveStarted) {
                                mFlingEnable = true;
                                mMoveStarted = true;
                                if (getRecentsManager() != null) {
                                    getRecentsManager().showHidden();
                                }
                            } else {
                                mWrongMoveStarted = true;
                            }
                        } else {
                            // move right
                            if (mConfiguration.mLocation != 0 && !mWrongMoveStarted) {
                                mFlingEnable = true;
                                mMoveStarted = true;
                                if (getRecentsManager() != null) {
                                    getRecentsManager().showHidden();
                                }
                            } else {
                                mWrongMoveStarted = true;
                            }
                        }
                        if (mMoveStarted) {
                            if (getRecentsManager() != null) {
                                getRecentsManager().slideLayout(distanceX);
                            }
                        }
                    }
                    mLastX = xRaw;
                    break;
                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    mFlingEnable = false;
                    mHandler.removeCallbacks(mLongPressRunnable);

                    if (mEnabled && !mLongPress) {
                        if (mMoveStarted) {
                            if (getRecentsManager() != null) {
                                getRecentsManager().finishSlideLayout();
                            }
                            } else {
                            if (getRecentsManager() != null) {
                                getRecentsManager().hideHidden();
                            }
                        }
                    }
                    mEnabled = true;
                    mMoveStarted = false;
                    mWrongMoveStarted = false;
                    break;
                }
                return true;
            }
        });

        mAppDrawer = (AppDrawerView) findViewById(R.id.app_drawer);
        mAppDrawer.setTransparentMode(true);
        mAppDrawer.init();

        mFavoriteGrid = (FavoriteView) findViewById(R.id.favorite_grid);
        mFavoriteGrid.setTransparentMode(true);
        mFavoriteGrid.init();
    }

    private LinearLayout.LayoutParams getListParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                mConfiguration.getItemMaxHeight());
    }

    private void hideAppDrawerPanel(final boolean withDim, final boolean withAlpha) {
        if (mAppDrawerPanelVisibile) {
            if (withAlpha) {
                mAppDrawerPanel.animate().alpha(0f).setDuration(500).withEndAction(new Runnable(){
//                     @Override
                    public void run() {
                        mAppDrawerPanel.setVisibility(View.GONE);
                        mAppDrawerPanelVisibile = false;
                        setActivePanel(0);
                        if (withDim) {
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                        }
                        mAppDrawer.scrollTo(0, 0);
                        mAppDrawer.setSelection(0);
                    }
                });
            } else {
                mAppDrawerPanel.setVisibility(View.GONE);
                mAppDrawerPanelVisibile = false;
                setActivePanel(0);
                if (withDim) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                }
                mAppDrawer.scrollTo(0, 0);
                mAppDrawer.setSelection(0);
            }
        }
    }

    private void showAppDrawerPanel() {
        if (isShowingIntroScreen()) {
            return;
        }
        if (!mAppDrawerPanelVisibile) {
            mAppDrawerPanelVisibile = true;
            showWithFade(mAppDrawerPanel, new Runnable() {
                @Override
                public void run() {
                    hideFavoritePanel(false, false);
                    setActivePanel(2);
                }
            });
        }
    }

    private void hideFavoritePanel(final boolean withDim, final boolean withAlpha) {
        if (mFavoritePanelVisibile) {
            if (withAlpha) {
                mFavoritePanel.animate().alpha(0f).setDuration(500).withEndAction(new Runnable(){
                    @Override
                    public void run() {
                        mFavoritePanel.setVisibility(View.GONE);
                        mFavoritePanelVisibile = false;
                        setActivePanel(0);
                        if (withDim) {
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                        }
                        mFavoriteGrid.scrollTo(0, 0);
                        mFavoriteGrid.setSelection(0);

                        mFavoriteEditButton.setVisibility(View.GONE);
                        mFavoriteEditButtonSpace.setVisibility(View.GONE);
                    }
                });
            } else {
                mFavoritePanel.setVisibility(View.GONE);
                mFavoritePanelVisibile = false;
                setActivePanel(0);
                if (withDim) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                }
                mFavoriteGrid.scrollTo(0, 0);
                mFavoriteGrid.setSelection(0);

                mFavoriteEditButton.setVisibility(View.GONE);
                mFavoriteEditButtonSpace.setVisibility(View.GONE);
            }
        }
    }

    private void showFavoritePanel() {
        if (isShowingIntroScreen()) {
            return;
        }
        if (!mFavoritePanelVisibile) {
            mFavoritePanelVisibile = true;
            showWithFade(mFavoritePanel, new Runnable() {
                @Override
                public void run() {
                    hideAppDrawerPanel(false, false);
                    mFavoriteEditButton.setVisibility(View.VISIBLE);
                    mFavoriteEditButtonSpace.setVisibility(View.VISIBLE);
                    setActivePanel(1);
                }
            });
        }
    }

    private void hideEssentialsPanel() {
        if (mEssentialsPanelVisibile) {
            Animator rotateAnimator = ObjectAnimator.ofFloat(mEssentialsButton, View.ROTATION, ROTATE_0_DEGREE, ROTATE_180_DEGREE);
            rotateAnimator.setDuration(500);
            Animator fadeAnimator = ObjectAnimator.ofFloat(mEssentialsPanel, "alpha", 1f, 0f);
            fadeAnimator.setDuration(500);
            fadeAnimator.addListener(new AnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mEssentialsPanel.setVisibility(View.GONE);
                    setEssentialsExpanded(false);
                    mEssentialsPanelVisibile = false;
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
            AnimatorSet set = new AnimatorSet();
            set.playTogether(fadeAnimator, rotateAnimator);
            set.start();
        }
    }

    private void showEssentialsPanel() {
        if (isShowingIntroScreen()) {
            return;
        }
        if (!mEssentialsPanelVisibile) {
            mEssentialsPanelVisibile = true;
            setEssentialsExpanded(true);
            mEssentialsPanel.setAlpha(0f);
            mEssentialsPanel.setVisibility(View.VISIBLE);

            Animator rotateAnimator = ObjectAnimator.ofFloat(mEssentialsButton, View.ROTATION, ROTATE_180_DEGREE, ROTATE_0_DEGREE);
            rotateAnimator.setDuration(500);
            Animator fadeAnimator = ObjectAnimator.ofFloat(mEssentialsPanel, "alpha", 0f, 1f);
            fadeAnimator.setDuration(500);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(fadeAnimator, rotateAnimator);
            set.start();
        }
    }

    private void restoreFavoritePanel() {
        if (isShowingIntroScreen()) {
            return;
        }
        if (mFavoritePanelVisibile && mFavoritePanel.getVisibility() == View.GONE) {
            mFavoritePanel.setVisibility(View.VISIBLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getWindow().setDimAmount(DIM_AMOUNT);
            mFavoriteEditButton.setVisibility(View.VISIBLE);
            mFavoriteEditButtonSpace.setVisibility(View.VISIBLE);
        }
    }

    private void restoreAppDrawerPanel() {
        if (isShowingIntroScreen()) {
            return;
        }
        if (mAppDrawerPanelVisibile && mAppDrawerPanel.getVisibility() == View.GONE) {
            mAppDrawerPanel.setVisibility(View.VISIBLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getWindow().setDimAmount(DIM_AMOUNT);
        }
    }

    private void restoreEssentialsPanel() {
        if (isShowingIntroScreen()) {
            return;
        }
        if (mEssentialsPanelVisibile && mEssentialsPanel.getVisibility() == View.GONE) {
            mEssentialsPanel.setVisibility(View.VISIBLE);
            mEssentialsButton.setRotation(ROTATE_0_DEGREE);
        } else {
            mEssentialsButton.setRotation(ROTATE_180_DEGREE);
        }
    }

    @Override
    protected void onResume() {
        // make sure service is started whenever we use it in launcher mode
        if (!mPrefs.getBoolean(SettingsActivity.PREF_ENABLE, false)) {
            mPrefs.edit().putBoolean(SettingsActivity.PREF_ENABLE, true).commit();
            final Intent startIntent = new Intent(this, SwitchService.class);
            startService(startIntent);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        // DONT use listener to mPrefs cause it MUST be after mConfiguration update
        mConfiguration.registerOnSharedPreferenceChangeListener(mPrefsListener);
        mAttached = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mConfiguration.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
            mAttached = false;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initView();
        restoreState();
    }

    private void updateFavoritesList() {
        Utils.updateFavoritesList(this, mConfiguration, mFavoriteList);
        if (DEBUG) {
            Log.d(TAG, "updateFavoritesList " + mFavoriteList);
        }
    }

    @Override
    public void onBackPressed() {
        hideOverlays();
    }

    private boolean shouldShowIntroScreen() {
        return !mPrefs.getBoolean(WECLOME_SCREEN_DISMISSED, false);
    }

    public void dismissIntroScreen() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mPrefs.edit().putBoolean(WECLOME_SCREEN_DISMISSED, true).commit();
    }

    private boolean isShowingIntroScreen() {
        return mWelcomeCling != null && mWelcomeCling.isVisible();
    }

    private SwitchManager getRecentsManager() {
        return SwitchService.getRecentsManager();
    }

    private void showWithFade(final View view, final Runnable preShowActions) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        Animator anim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        anim.setDuration(500);
        anim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationStart(Animator animation) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                getWindow().setDimAmount(DIM_AMOUNT);
                if (preShowActions != null) {
                    preShowActions.run();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        anim.start();
    }

    @Override
    public void applyChanges(List<String> favoriteList){
        mPrefs.edit().putString(SettingsActivity.PREF_FAVORITE_APPS, Utils.flattenFavorites(favoriteList)).commit();
    }
    

    private void hideOverlays() {
        hideAppDrawerPanel(true, true);
        hideFavoritePanel(true, true);
    }

    private boolean isPhoneVisible() {
        android.content.pm.PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEPHONY)
                && pm.resolveActivity(PHONE_INTENT, 0) != null;
    }

    private void launchPhone() {
        final TelecomManager tm = TelecomManager.from(this);
        if (tm.isInCall()) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    tm.showInCallScreen(false /* showDialpad */);
                }
            });
        } else {
            final Intent phoneIntent = new Intent(PHONE_INTENT);
            phoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(phoneIntent);
        }
    }

    private void launchCamera() {
        final Intent cameraIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(cameraIntent);
    }

    private Intent getVoiceAssistIntent() {
        final Intent assistIntent = new Intent(Intent.ACTION_VOICE_ASSIST);
        assistIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                // that will always trigger the voice search page
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        return assistIntent;
    }

    private void launchVoiceAssist() {
        startActivity(getVoiceAssistIntent());
    }

    private boolean canLaunchVoiceAssist() {
        return Utils.canResolveIntent(this, getVoiceAssistIntent());
    }

    private Intent getAssistIntent() {
        final Intent assistIntent = new Intent(Intent.ACTION_ASSIST);
        assistIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        return assistIntent;
    }

    private void launchAssist() {
        startActivity(getAssistIntent());
    }

    private boolean canLaunchAssist() {
        return Utils.canResolveIntent(this, getAssistIntent());
    }

    private boolean isEssentialsExpanded() {
        return mPrefs.getBoolean(STATE_ESSENTIALS_EXPANDED, false);
    }

    private void setEssentialsExpanded(boolean value) {
        mPrefs.edit().putBoolean(STATE_ESSENTIALS_EXPANDED, value).commit();
    }

    private int getActivePanel() {
        return mPrefs.getInt(STATE_PANEL_SHOWN, 0);
    }

    private void setActivePanel(int value) {
        mPrefs.edit().putInt(STATE_PANEL_SHOWN, value).commit();
    }

    private void restoreState() {
        restoreFavoritePanel();
        restoreAppDrawerPanel();
        restoreEssentialsPanel();
    }

    private boolean isDeviceProvisioned() {
        return (Settings.Global.getInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 0) != 0);
    }
}

