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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.dslv.DragSortController;
import org.omnirom.omniswitch.dslv.DragSortListView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class CheckboxListDialog extends AlertDialog implements
        DialogInterface.OnClickListener {

    private String[] mListItems;
    private Drawable[] mListImages;
    private Map<Integer, Boolean> mCheckedItems;
    private DragSortListView mCheckboxListView;
    private LayoutInflater mInflater;
    private ArrayAdapter<String> mListAdapter;
    private ApplyRunnable mApplyRunnable;
    private String mTitle;

    ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);

    public interface ApplyRunnable {
        public void apply(Map<Integer, Boolean> buttons);
    };
    private class CheckboxListAdapter extends ArrayAdapter<String> {

        public CheckboxListAdapter(Context context, int resource, List<String> values) {
            super(context, R.layout.checkbox_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = mInflater.inflate(R.layout.checkbox_item, parent, false);

            final TextView item = (TextView)rowView.findViewById(R.id.item_text);
            int orderPosition = getPositionOfItem(position, mCheckedItems);
            item.setText(mListItems[orderPosition]);

            final CheckBox check = (CheckBox)rowView.findViewById(R.id.item_check);
            check.setChecked(mCheckedItems.get(orderPosition));

            final ImageView image = (ImageView)rowView.findViewById(R.id.item_image);
            image.setImageDrawable(mListImages[orderPosition]);

            return rowView;
        }   
    }

    private int getPositionOfItem(int position, Map<Integer, Boolean> buttons) {
        int i = 0;
        Iterator<Integer> nextKey = buttons.keySet().iterator();
        while(nextKey.hasNext()){
            Integer key = nextKey.next();
            if (i == position){
                return key;
            }
            i++;
        }
        return 0;
    }

    private void setValueAtPosition(int position, Map<Integer, Boolean> buttons, boolean value) {
        int i = 0;
        Iterator<Integer> nextKey = buttons.keySet().iterator();
        while(nextKey.hasNext()){
            Integer key = nextKey.next();
            if (i == position){
                buttons.put(key, value);
                break;
            }
            i++;
        }
    }

    private Boolean getValueAtPosition(int position, Map<Integer, Boolean> buttons) {
        int i = 0;
        Iterator<Integer> nextKey = buttons.keySet().iterator();
        while(nextKey.hasNext()){
            Integer key = nextKey.next();
            if (i == position){
                return buttons.get(key);
            }
            i++;
        }
        return null;
    }

    private class CheckboxListDragSortController extends DragSortController {

        public CheckboxListDragSortController() {
            super(mCheckboxListView, R.id.drag_handle,
                    DragSortController.ON_DOWN,
                    DragSortController.FLING_RIGHT_REMOVE);
            setRemoveEnabled(false);
            setSortEnabled(true);
            setBackgroundColor(0x363636);
        }

        @Override
        public void onDragFloatView(View floatView, Point floatPoint,
                Point touchPoint) {
            floatView.setLayoutParams(params);
            mCheckboxListView.setFloatAlpha(0.8f);
        }

        @Override
        public View onCreateFloatView(int position) {
            View v = mListAdapter.getView(position, null,
                    mCheckboxListView);
            v.setLayoutParams(params);
            return v;
        }

        @Override
        public void onDestroyFloatView(View floatView) {
        }
    }

    public CheckboxListDialog(Context context, String[] items, Drawable[] images, Map<Integer, Boolean> checked, ApplyRunnable applyRunnable, String title) {
        super(context);
        mTitle = title;
        mApplyRunnable = applyRunnable;
        mListItems = items;
        mListImages = images;
        mCheckedItems = checked;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private void applyChanges() {
        mApplyRunnable.apply(mCheckedItems);
    }
    
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            applyChanges();
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            cancel();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Context context = getContext();
        final View view = getLayoutInflater().inflate(
                R.layout.checkbox_list, null);
        setView(view);
        setTitle(mTitle);
        setCancelable(true);

        setButton(DialogInterface.BUTTON_POSITIVE,
                context.getString(android.R.string.ok), this);
        setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getString(android.R.string.cancel), this);

        super.onCreate(savedInstanceState);

        mCheckboxListView = (DragSortListView) view.findViewById(R.id.item_list);

        mListAdapter = new CheckboxListAdapter(getContext(),
                android.R.layout.simple_list_item_multiple_choice, Arrays.asList(mListItems));
        mCheckboxListView.setAdapter(mListAdapter);
        mCheckboxListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Boolean value = getValueAtPosition(position, mCheckedItems);
                if (value != null){
                    setValueAtPosition(position, mCheckedItems, !value);
                    mListAdapter.notifyDataSetChanged();
                }
            }
        });

        final DragSortController dragSortController = new CheckboxListDragSortController();
        mCheckboxListView.setFloatViewManager(dragSortController);
        mCheckboxListView
                .setDropListener(new DragSortListView.DropListener() {
                    @Override
                    public void drop(int from, int to) {
                        Map<Integer, Boolean> newItems = new LinkedHashMap<Integer, Boolean>();
                        Iterator<Integer> nextKey = mCheckedItems.keySet().iterator();
                        Integer fromKey = null;
                        Boolean fromValue = null;
                        int i = 0;
                        while(nextKey.hasNext()){
                            Integer key = nextKey.next();
                            Boolean value = mCheckedItems.get(key);
                            if (i == from){
                                fromKey = key;
                                fromValue = value;
                                break;
                            }
                            i++;
                        }
                        if (fromKey != null && fromValue != null){
                            nextKey = mCheckedItems.keySet().iterator();
                            i = 0;
                            boolean added = false;
                            while(nextKey.hasNext()){
                                Integer key = nextKey.next();
                                Boolean value = mCheckedItems.get(key);
                                if (i == to && to != mCheckedItems.size() - 1){
                                    if (to > from){
                                        newItems.put(key, value);
                                        newItems.put(fromKey, fromValue);
                                        added = true;
                                        i++;
                                        continue;
                                    }
                                    newItems.put(fromKey, fromValue);
                                    added = true;
                                } else if (i == from){
                                    i++;
                                    continue;
                                }
                                newItems.put(key, value);
                                i++;
                            }
                            // added at the end
                            if (!added){
                                newItems.put(fromKey, fromValue);
                            }
                            mCheckedItems.clear();
                            mCheckedItems.putAll(newItems);
                            mListAdapter.notifyDataSetChanged();
                        }
                    }
                });
        mCheckboxListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return dragSortController.onTouch(view, motionEvent);
            }
        });
    }
}
