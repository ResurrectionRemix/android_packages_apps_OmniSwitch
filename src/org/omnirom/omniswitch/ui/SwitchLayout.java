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

import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.omnirom.omniswitch.MemInfoReader;
import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.SettingsActivity;
import org.omnirom.omniswitch.SwitchManager;
import org.omnirom.omniswitch.SwitchService;
import org.omnirom.omniswitch.TaskDescription;

import android.app.ActivityManager;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

public class SwitchLayout extends LinearLayout {
    private static final String TAG = "SwitchLayout";
    private WindowManager mWindowManager;
    private LayoutInflater mInflater;
    private ListView mRecentList;
    private HorizontalListView mRecentListHorizontal;
    private HorizontalListView mFavoriteListHorizontal;
    private ImageButton mLastAppButton;
    private ImageButton mKillAllButton;
    private ImageButton mKillOtherButton;
    private ImageButton mHomeButton;
    private ImageButton mSettingsButton;
    private RecentListAdapter mRecentListAdapter;
    private FavoriteListAdapter mFavoriteListAdapter;
    private List<TaskDescription> mLoadedTasks;
    private Context mContext;
    private SwitchManager mRecentsManager;
    private FrameLayout mPopupView;
    private boolean mShowing;
    private PopupMenu mPopup;
    private float mBackgroundOpacity = 0.6f;
    private boolean mHorizontal = true;
    private View mView;
    private int mLocation = 0; // 0 = right 1 = left
    private boolean mAnimate = true;
    private int mIconSize = 60; // in dip
    private float mDensity;
    private int mHorizontalMaxWidth = mIconSize;
    private int mHorizontalScrollerHeight = mIconSize * 2;
    private LinearColorBar mRamUsageBar;
    private boolean mShowRambar;
    private TextView mBackgroundProcessText;
    private TextView mForegroundProcessText;
    private Handler mHandler = new Handler();
    private ActivityManager.MemoryInfo mMemInfo = new ActivityManager.MemoryInfo();
    private MemInfoReader mMemInfoReader = new MemInfoReader();
    private long mSecServerMem;
    private float mPosY = -1.0f;
    private List<String> mFavoriteList;
    private List<Drawable> mFavoriteIcons;
    private List<String> mFavoriteNames;
    private boolean mShowLabels = true;
    private boolean mShowFavorites;

    public class RecentListAdapter extends ArrayAdapter<TaskDescription> {

        public RecentListAdapter(Context context, int resource,
                List<TaskDescription> values) {
            super(context, mHorizontal ? R.layout.recent_item_horizontal
                    : R.layout.recent_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = null;
            TaskDescription ad = mLoadedTasks.get(position);

            if (mHorizontal) {
                rowView = mInflater.inflate(R.layout.recent_item_horizontal,
                        parent, false);
                final TextView item = (TextView) rowView
                        .findViewById(R.id.recent_item);
                if (mShowLabels) {
                    item.setText(ad.getLabel());
                }
                item.setMaxWidth(mHorizontalMaxWidth);
                item.setCompoundDrawablesWithIntrinsicBounds(null,
                        ad.getIcon(), null, null);
            } else {
                rowView = mInflater
                        .inflate(R.layout.recent_item, parent, false);
                final TextView item = (TextView) rowView
                        .findViewById(R.id.recent_item);
                if (mShowLabels) {
                    item.setText(ad.getLabel());
                }
                item.setCompoundDrawablesWithIntrinsicBounds(ad.getIcon(),
                        null, null, null);
            }
            return rowView;
        }
    }

    public class FavoriteListAdapter extends ArrayAdapter<String> {

        public FavoriteListAdapter(Context context, int resource,
                List<String> values) {
            super(context, R.layout.favorite_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = null;
            rowView = mInflater.inflate(R.layout.favorite_item, parent, false);
            final TextView item = (TextView) rowView
                    .findViewById(R.id.favorite_item);
            if (mShowLabels) {
                item.setText(mFavoriteNames.get(position));
            }
            item.setMaxWidth(mHorizontalMaxWidth);
            item.setCompoundDrawablesWithIntrinsicBounds(null,
                    mFavoriteIcons.get(position), null, null);
            return rowView;
        }
    }

    public void setRecentsManager(SwitchManager manager) {
        mRecentsManager = manager;
    }

    public SwitchLayout(Context context) {
        super(context);
        mContext = context;
        mWindowManager = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLoadedTasks = new ArrayList<TaskDescription>();
        mRecentListAdapter = new RecentListAdapter(mContext,
                android.R.layout.simple_list_item_multiple_choice, mLoadedTasks);
        mFavoriteList = new ArrayList<String>();
        mFavoriteListAdapter = new FavoriteListAdapter(mContext,
                android.R.layout.simple_list_item_multiple_choice,
                mFavoriteList);

        mDensity = mContext.getResources().getDisplayMetrics().density;

        final ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        am.getMemoryInfo(mMemInfo);
        String sClassName = "android.app.ActivityManager";
        try {
            Class classToInvestigate = Class.forName(sClassName);
            Class[] classes = classToInvestigate.getDeclaredClasses();
            for (int i = 0; i < classes.length; i++) {
                Class c = classes[i];
                if (c.getName()
                        .equals("android.app.ActivityManager$MemoryInfo")) {
                    String strNewFieldName = "secondaryServerThreshold";
                    Field field = c.getField(strNewFieldName);
                    mSecServerMem = field.getLong(mMemInfo);
                    break;
                }
            }
        } catch (ClassNotFoundException e) {
        } catch (NoSuchFieldException e) {
        } catch (Exception e) {
        }
    }

    private void createView() {
        mView = null;
        if (mHorizontal) {
            mView = mInflater.inflate(R.layout.recents_list_horizontal, this,
                    false);
            mRecentListHorizontal = (HorizontalListView) mView
                    .findViewById(R.id.recent_list_horizontal);
            LayoutParams params = new LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    mHorizontalScrollerHeight);

            mRecentListHorizontal.setLayoutParams(params);
            mRecentListHorizontal
                    .setOnItemClickListener(new OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent,
                                View view, int position, long id) {
                            Log.d(TAG, "onItemClick");
                            TaskDescription task = mLoadedTasks.get(position);
                            mRecentsManager.switchTask(task);
                            hide();
                        }
                    });

            mRecentListHorizontal
                    .setOnItemLongClickListener(new OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent,
                                View view, int position, long id) {
                            Log.d(TAG, "onItemLongClick");
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
                            for (int position : reverseSortedPositions) {
                                TaskDescription ad = mRecentListAdapter
                                        .getItem(position);
                                mRecentsManager.killTask(ad);
                                break;
                            }
                        }

                        @Override
                        public boolean canDismiss(int position) {
                            Log.d(TAG, "canDismiss " + position);
                            return true;
                        }
                    });

            mRecentListHorizontal.setSwipeListener(touchListener);

            mRecentListHorizontal.setAdapter(mRecentListAdapter);

            mOpenFavorite = (ImageButton) mView
                    .findViewById(R.id.openFavorites);
            mOpenFavorite.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mShowFavorites = !mShowFavorites;
                    mOpenFavorite.setImageDrawable(getResources().getDrawable(
                            mShowFavorites ? R.drawable.arrow_up
                                    : R.drawable.arrow_down));
                    if (mAnimate) {
                        mFavoriteListHorizontal
                                .startAnimation(mShowFavorites ? getShowFavoriteAnimation()
                                        : getHideFavoriteAnimation());
                    } else {
                        mFavoriteListHorizontal
                                .setVisibility(mShowFavorites ? View.VISIBLE
                                        : View.GONE);
                    }
                }
            });

            mFavoriteListHorizontal = (HorizontalListView) mView
                    .findViewById(R.id.favorite_list_horizontal);

            mFavoriteListHorizontal.setLayoutParams(params);
            mFavoriteListHorizontal
                    .setOnItemClickListener(new OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent,
                                View view, int position, long id) {
                            Log.d(TAG, "onItemClick");
                            String intent = mFavoriteList.get(position);
                            mRecentsManager.startIntentFromtString(intent);
                            hide();
                        }
                    });
            mFavoriteListHorizontal.setAdapter(mFavoriteListAdapter);

        } else {
            mView = mInflater.inflate(R.layout.recents_list, this, false);
            mRecentList = (ListView) mView.findViewById(R.id.recent_list);

            mRecentList.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                        int position, long id) {
                    Log.d(TAG, "onItemClick");
                    TaskDescription task = mLoadedTasks.get(position);
                    mRecentsManager.switchTask(task);
                    hide();
                }
            });

            mRecentList
                    .setOnItemLongClickListener(new OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent,
                                View view, int position, long id) {
                            Log.d(TAG, "onItemLongClick");
                            TaskDescription task = mLoadedTasks.get(position);
                            handleLongPress(task, view);
                            return true;
                        }
                    });

            SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(
                    mRecentList,
                    new SwipeDismissListViewTouchListener.DismissCallbacks() {
                        public void onDismiss(ListView listView,
                                int[] reverseSortedPositions) {
                            for (int position : reverseSortedPositions) {
                                TaskDescription ad = mRecentListAdapter
                                        .getItem(position);
                                mRecentsManager.killTask(ad);
                                break;
                            }
                        }

                        @Override
                        public boolean canDismiss(int position) {
                            Log.d(TAG, "canDismiss " + position);
                            return true;
                        }
                    });
            mRecentList.setOnTouchListener(touchListener);
            mRecentList.setOnScrollListener(touchListener.makeScrollListener());

            mRecentList.setAdapter(mRecentListAdapter);
        }

        mHomeButton = (ImageButton) mView.findViewById(R.id.home);
        mHomeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mRecentsManager.dismissAndGoHome();
            }
        });

        mLastAppButton = (ImageButton) mView.findViewById(R.id.lastApp);
        mLastAppButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mRecentsManager.toggleLastApp();
            }
        });

        mKillAllButton = (ImageButton) mView.findViewById(R.id.killAll);
        mKillAllButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mRecentsManager.killAll();
            }
        });

        mKillOtherButton = (ImageButton) mView.findViewById(R.id.killOther);
        mKillOtherButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mRecentsManager.killOther();
            }
        });

        mSettingsButton = (ImageButton) mView.findViewById(R.id.settings);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent hideRecent = new Intent(
                        SwitchService.RecentsReceiver.ACTION_HIDE_RECENTS);
                mContext.sendBroadcast(hideRecent);

                Intent mainActivity = new Intent(mContext,
                        SettingsActivity.class);
                mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                mContext.startActivity(mainActivity);
            }
        });

        mRamUsageBar = (LinearColorBar) mView.findViewById(R.id.ram_usage_bar);
        mForegroundProcessText = (TextView) mView
                .findViewById(R.id.foregroundText);
        mBackgroundProcessText = (TextView) mView
                .findViewById(R.id.backgroundText);

        if (!mShowRambar) {
            mRamUsageBar.setVisibility(View.GONE);
        }
        mPopupView = new FrameLayout(mContext);

        mPopupView.removeAllViews();

        mPopupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mShowing) {
                    Log.d(TAG, "onTouch");
                    Intent hideRecent = new Intent(
                            SwitchService.RecentsReceiver.ACTION_HIDE_RECENTS);
                    mContext.sendBroadcast(hideRecent);
                    // TODO workaround for flicker on launcher screen
                    mWindowManager.updateViewLayout(mPopupView, getParams(0));
                }
                return true;
            }
        });
        mPopupView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (mShowing) {
                    Log.d(TAG, "onKey");
                    Intent hideRecent = new Intent(
                            SwitchService.RecentsReceiver.ACTION_HIDE_RECENTS);
                    mContext.sendBroadcast(hideRecent);
                    // TODO workaround for flicker on launcher screen
                    mWindowManager.updateViewLayout(mPopupView, getParams(0));
                }
                return true;
            }
        });
    }

    public void show() {
        if (mShowing) {
            return;
        }

        createView();

        mWindowManager.addView(mPopupView, getParams(mBackgroundOpacity));

        mPopupView.addView(mView);

        if (mAnimate) {
            mView.startAnimation(getShowAnimation());
        } else {
            mPopupView.setFocusableInTouchMode(true);
            mShowing = true;
        }
    }

    private Animation getShowAnimation() {
        int animId = R.anim.slide_right_in;

        if (mLocation == 1) {
            animId = R.anim.slide_left_in;
        }
        Animation animation = AnimationUtils.loadAnimation(mContext, animId);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mPopupView.setFocusableInTouchMode(true);
                mShowing = true;
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        return animation;
    }

    private Animation getHideAnimation() {
        int animId = R.anim.slide_right_out;

        if (mLocation == 1) {
            animId = R.anim.slide_left_out;
        }

        Animation animation = AnimationUtils.loadAnimation(mContext, animId);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                // to avoid the "Attempting to destroy the window while drawing"
                // error
                Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mWindowManager.removeView(mPopupView);
                        mPopupView = null;
                    }
                });
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        return animation;
    }

    private Animation getShowFavoriteAnimation() {
        int animId = R.anim.slide_down;
        Animation animation = AnimationUtils.loadAnimation(mContext, animId);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
                mFavoriteListHorizontal.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        return animation;
    }

    private Animation getHideFavoriteAnimation() {
        int animId = R.anim.slide_up;
        Animation animation = AnimationUtils.loadAnimation(mContext, animId);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mFavoriteListHorizontal.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        return animation;
    }

    public void hide() {
        if (!mShowing) {
            return;
        }
        mShowing = false;

        if (mPopup != null) {
            mPopup.dismiss();
        }

        if (mAnimate) {
            mView.startAnimation(getHideAnimation());
        } else {
            mWindowManager.removeView(mPopupView);
            mPopupView = null;
        }
    }

    private WindowManager.LayoutParams getParams(float opacity) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                mHorizontal ? WindowManager.LayoutParams.MATCH_PARENT
                        : WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        params.dimAmount = opacity;

        if (mHorizontal && mPosY != -1.0f) {
            params.gravity = getAbsoluteGravity();
            params.y = (int) (mPosY - mHorizontalScrollerHeight / 2);
        } else {
            params.gravity = getDefaultGravity();
        }

        return params;
    }

    private int getDefaultGravity() {
        return Gravity.CENTER;
    }

    private int getAbsoluteGravity() {
        return Gravity.TOP;
    }

    public void update(List<TaskDescription> taskList) {
        Log.d(TAG, "update");
        mLoadedTasks.clear();
        mLoadedTasks.addAll(taskList);
        mRecentListAdapter.notifyDataSetChanged();
        mHandler.post(updateRamBarTask);
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
                    startApplicationDetailsActivity(ad.getPackageName());
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

    private void startApplicationDetailsActivity(String packageName) {
        Intent hideRecent = new Intent(
                SwitchService.RecentsReceiver.ACTION_HIDE_RECENTS);
        mContext.sendBroadcast(hideRecent);

        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(
                        "package", packageName, null));
        intent.setComponent(intent.resolveActivity(mContext.getPackageManager()));
        TaskStackBuilder.create(getContext())
                .addNextIntentWithParentStack(intent).startActivities();
    }

    public void handleSwipe(TaskDescription ad) {
        mRecentsManager.killTask(ad);
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        Log.d(TAG, "updatePrefs");
        // mHorizontal = prefs.getString(SettingsActivity.PREF_ORIENTATION,
        // "vertical").equals("horizontal");
        String location = prefs.getString(
                SettingsActivity.PREF_DRAG_HANDLE_LOCATION, "0");
        mLocation = Integer.valueOf(location);
        int opacity = prefs.getInt(SettingsActivity.PREF_OPACITY, 60);
        mBackgroundOpacity = (float) opacity / 100.0f;
        Log.d(TAG, "mBackgroundOpacity " + mBackgroundOpacity);
        mAnimate = prefs.getBoolean(SettingsActivity.PREF_ANIMATE, true);
        String iconSize = prefs
                .getString(SettingsActivity.PREF_ICON_SIZE, "60");
        mIconSize = Integer.valueOf(iconSize);
        mShowRambar = prefs
                .getBoolean(SettingsActivity.PREF_SHOW_RAMBAR, false);
        mShowLabels = prefs.getBoolean(SettingsActivity.PREF_SHOW_LABELS, true);
        mPosY = prefs.getFloat("handle_pos_y", -1.0f);

        mHorizontalMaxWidth = (int) ((mIconSize + 10) * mDensity + 0.5f);
        mHorizontalScrollerHeight = (int) ((mIconSize + (mShowLabels ? 40 : 10))
                * mDensity + 0.5f);

        mFavoriteList.clear();
        String favoriteListString = prefs.getString("favorite_apps", "");
        SettingsActivity.parseFavorites(favoriteListString, mFavoriteList);

        updateFavorites();
        mFavoriteListAdapter.notifyDataSetChanged();
    }

    private final Runnable updateRamBarTask = new Runnable() {
        @Override
        public void run() {
            if (!mShowRambar || mRamUsageBar == null) {
                return;
            }
            mMemInfoReader.readMemInfo();
            long availMem = mMemInfoReader.getFreeSize()
                    + mMemInfoReader.getCachedSize() - mSecServerMem;
            long totalMem = mMemInfoReader.getTotalSize();

            String sizeStr = Formatter.formatShortFileSize(mContext, totalMem
                    - availMem);
            mForegroundProcessText.setText(getResources().getString(
                    R.string.service_foreground_processes, sizeStr));
            sizeStr = Formatter.formatShortFileSize(mContext, availMem);
            mBackgroundProcessText.setText(getResources().getString(
                    R.string.service_background_processes, sizeStr));

            float fTotalMem = totalMem;
            float fAvailMem = availMem;
            mRamUsageBar.setRatios((fTotalMem - fAvailMem) / fTotalMem, 0, 0);
        }
    };
    private ImageButton mOpenFavorite;

    private void updateFavorites() {
        final PackageManager pm = mContext.getPackageManager();
        List<String> validFavorites = new ArrayList<String>();
        mFavoriteIcons = new ArrayList<Drawable>();
        mFavoriteNames = new ArrayList<String>();
        Iterator<String> nextFavorite = mFavoriteList.iterator();
        while (nextFavorite.hasNext()) {
            String favorite = nextFavorite.next();
            Intent intent = null;
            try {
                intent = Intent.parseUri(favorite, 0);
                mFavoriteIcons.add(resize(Resources.getSystem(),
                        pm.getActivityIcon(intent)));
            } catch (NameNotFoundException e) {
                Log.e(TAG, "NameNotFoundException: [" + favorite + "]");
                continue;
            } catch (URISyntaxException e) {
                Log.e(TAG, "URISyntaxException: [" + favorite + "]");
                continue;
            }
            validFavorites.add(favorite);
            String label = SettingsActivity.getActivityLabel(pm, intent);
            if (label == null) {
                label = favorite;
            }
            mFavoriteNames.add(label);
        }
        mFavoriteList.clear();
        mFavoriteList.addAll(validFavorites);
    }

    private Drawable resize(Resources resources, Drawable image) {
        // TODO
        int size = (int) (mIconSize * mDensity + 0.5f);
        Bitmap b = ((BitmapDrawable) image).getBitmap();
        int originalHeight = b.getHeight();
        int originalWidth = b.getWidth();

        int l = originalHeight > originalWidth ? originalHeight : originalWidth;
        float factor = (float) size / (float) l;

        int resizedHeight = (int) (originalHeight * factor);
        int resizedWidth = (int) (originalWidth * factor);

        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, resizedWidth,
                resizedHeight, false);
        return new BitmapDrawable(resources, bitmapResized);
    }
    
    public boolean isShowing() {
        return mShowing;
    }
}
