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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.omnirom.omniswitch.PackageManager;
import org.omnirom.omniswitch.PackageManager.PackageItem;
import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.SettingsActivity;
import org.omnirom.omniswitch.dslv.DragSortController;
import org.omnirom.omniswitch.dslv.DragSortListView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FavoriteDialog extends AlertDialog implements
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private LayoutInflater mInflater;
    private List<String> mFavoriteList;
    private SettingsActivity mContext;
    private FavoriteListAdapter mFavoriteAdapter;
    private DragSortListView mFavoriteConfigList;
    private AlertDialog mAddFavoriteDialog;

    ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);

    public class FavoriteListAdapter extends ArrayAdapter<String> {

        public FavoriteListAdapter(Context context, int resource,
                List<String> values) {
            super(context, R.layout.favorite_app_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = null;
            rowView = mInflater.inflate(R.layout.favorite_app_item, parent,
                    false);
            String intent = mFavoriteList.get(position);
            PackageManager.PackageItem packageItem = PackageManager.getInstance(mContext).getPackageItem(intent);

            final TextView item = (TextView) rowView
                    .findViewById(R.id.app_item);
            item.setText(packageItem.getTitle());
            final ImageView image = (ImageView) rowView
                    .findViewById(R.id.app_icon);
            image.setImageDrawable(BitmapCache.getInstance(mContext).getPackageIcon(mContext.getResources(), packageItem));
            return rowView;
        }
    }

    private class FavoriteDragSortController extends DragSortController {

        public FavoriteDragSortController() {
            super(mFavoriteConfigList, R.id.drag_handle,
                    DragSortController.ON_DOWN,
                    DragSortController.FLING_RIGHT_REMOVE);
            setRemoveEnabled(true);
            setSortEnabled(true);
            setBackgroundColor(0x363636);
        }

        @Override
        public void onDragFloatView(View floatView, Point floatPoint,
                Point touchPoint) {
            floatView.setLayoutParams(params);
            mFavoriteConfigList.setFloatAlpha(0.8f);
        }

        @Override
        public View onCreateFloatView(int position) {
            View v = mFavoriteAdapter.getView(position, null,
                    mFavoriteConfigList);
            v.setLayoutParams(params);
            return v;
        }

        @Override
        public void onDestroyFloatView(View floatView) {
        }
    }

    public FavoriteDialog(SettingsActivity context, List<String> favoriteList) {
        super(context);
        mContext = context;
        mFavoriteList = favoriteList;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Context context = getContext();
        final View view = getLayoutInflater().inflate(R.layout.favorite_dialog,
                null);
        setView(view);
        setTitle(R.string.favorite_apps_config_title);
        setCancelable(true);

        setButton(DialogInterface.BUTTON_POSITIVE,
                context.getString(android.R.string.ok), this);
        setButton(DialogInterface.BUTTON_NEUTRAL,
                context.getString(R.string.favorite_add), this);
        setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getString(android.R.string.cancel), this);

        super.onCreate(savedInstanceState);

        mFavoriteConfigList = (DragSortListView) view
                .findViewById(R.id.favorite_apps);
        mFavoriteAdapter = new FavoriteListAdapter(mContext,
                android.R.layout.simple_list_item_single_choice, mFavoriteList);
        mFavoriteConfigList.setAdapter(mFavoriteAdapter);

        final DragSortController dragSortController = new FavoriteDragSortController();
        mFavoriteConfigList.setFloatViewManager(dragSortController);
        mFavoriteConfigList
                .setDropListener(new DragSortListView.DropListener() {
                    @Override
                    public void drop(int from, int to) {
                        String intent = mFavoriteList.remove(from);
                        mFavoriteList.add(to, intent);
                        mFavoriteAdapter.notifyDataSetChanged();
                    }
                });
        mFavoriteConfigList
                .setRemoveListener(new DragSortListView.RemoveListener() {
                    @Override
                    public void remove(int which) {
                        mFavoriteList.remove(which);
                        mFavoriteAdapter.notifyDataSetChanged();
                    }
                });
        mFavoriteConfigList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return dragSortController.onTouch(view, motionEvent);
            }
        });
        mFavoriteConfigList.setItemsCanFocus(false);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Button neutralButton = getButton(DialogInterface.BUTTON_NEUTRAL);
        neutralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddFavoriteDialog();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAddFavoriteDialog != null) {
            mAddFavoriteDialog.dismiss();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mAddFavoriteDialog != null) {
            mAddFavoriteDialog = null;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mContext.applyChanges(mFavoriteList);
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            cancel();
        }
    }

    private void showAddFavoriteDialog() {
        if (mAddFavoriteDialog != null && mAddFavoriteDialog.isShowing()) {
            return;
        }

        mAddFavoriteDialog = new AddFavoriteDialog(getContext());
        mAddFavoriteDialog.setOnDismissListener(this);
        mAddFavoriteDialog.show();
    }

    public void applyChanges(List<String> favoriteList) {
        mFavoriteList.clear();
        mFavoriteList.addAll(favoriteList);
        mFavoriteAdapter.notifyDataSetChanged();
    }

    private class AddFavoriteDialog extends AlertDialog implements
            DialogInterface.OnClickListener {

        private PackageAdapter mPackageAdapter;
        private List<String> mChangedFavoriteList;
        private ListView mListView;
        private List<PackageItem> mInstalledPackages;

        private class PackageAdapter extends BaseAdapter {

            private void reloadList() {
                mInstalledPackages = new LinkedList<PackageItem>();
                mInstalledPackages.addAll(PackageManager.getInstance(mContext).getPackageList());
                Collections.sort(mInstalledPackages);
            }

            public PackageAdapter() {
                reloadList();
            }

            @Override
            public int getCount() {
                return mInstalledPackages.size();
            }

            @Override
            public PackageItem getItem(int position) {
                return mInstalledPackages.get(position);
            }

            @Override
            public long getItemId(int position) {
                // intent is guaranteed to be unique in mInstalledPackages
                return mInstalledPackages.get(position).getIntent().hashCode();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                if (convertView != null) {
                    holder = (ViewHolder) convertView.getTag();
                } else {
                    convertView = getLayoutInflater().inflate(
                            R.layout.installed_app_item, parent, false);
                    holder = new ViewHolder();
                    convertView.setTag(holder);

                    holder.item = (TextView) convertView
                            .findViewById(R.id.app_item);
                    holder.check = (CheckBox) convertView
                            .findViewById(R.id.app_check);
                    holder.image = (ImageView) convertView
                            .findViewById(R.id.app_icon);
                }
                PackageItem applicationInfo = getItem(position);
                holder.item.setText(applicationInfo.getTitle());
                holder.image.setImageDrawable(BitmapCache.getInstance(mContext).getPackageIcon(mContext.getResources(), applicationInfo));
                holder.check.setChecked(mChangedFavoriteList
                        .contains(applicationInfo.getIntent()));

                return convertView;
            }
        }

        private class ViewHolder {
            TextView item;
            CheckBox check;
            ImageView image;
        }

        protected AddFavoriteDialog(Context context) {
            super(context);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                applyChanges(mChangedFavoriteList);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                cancel();
            }
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            final Context context = getContext();
            final View view = getLayoutInflater().inflate(
                    R.layout.installed_apps_dialog, null);
            setView(view);
            setTitle(R.string.favorite_apps_add_dialog_title);
            setCancelable(true);

            setButton(DialogInterface.BUTTON_POSITIVE,
                    context.getString(android.R.string.ok), this);
            setButton(DialogInterface.BUTTON_NEGATIVE,
                    context.getString(android.R.string.cancel), this);

            super.onCreate(savedInstanceState);
            mChangedFavoriteList = new ArrayList<String>();
            mChangedFavoriteList.addAll(mFavoriteList);

            mListView = (ListView) view.findViewById(R.id.installed_apps);
            mPackageAdapter = new PackageAdapter();
            mListView.setAdapter(mPackageAdapter);
            mListView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                        int position, long id) {
                    PackageItem info = (PackageItem) parent
                            .getItemAtPosition(position);
                    ViewHolder viewHolder = (ViewHolder) view.getTag();
                    viewHolder.check.setChecked(!viewHolder.check.isChecked());
                    if (viewHolder.check.isChecked()) {
                        if (!mChangedFavoriteList.contains(info.getIntent())) {
                            mChangedFavoriteList.add(info.getIntent());
                        }
                    } else {
                        mChangedFavoriteList.remove(info.getIntent());
                    }
                }
            });
        }
    }
}
