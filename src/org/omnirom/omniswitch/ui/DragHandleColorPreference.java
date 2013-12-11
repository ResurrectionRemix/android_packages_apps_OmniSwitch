/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnirom.omniswitch.ui;

import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.SettingsActivity;
import org.omnirom.omniswitch.R.dimen;
import org.omnirom.omniswitch.R.id;
import org.omnirom.omniswitch.R.layout;
import org.omnirom.omniswitch.R.string;
import org.omnirom.omniswitch.colorpicker.ColorPickerDialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class DragHandleColorPreference extends DialogPreference {

    public static final int DEFAULT_COLOR = 0xFFFFFFFF; // White

    private ImageView mLightColorView;
    private int mColorValue;
    private Resources mResources;
    private SharedPreferences mPrefs;

    /**
     * @param context
     * @param attrs
     */
    public DragHandleColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mColorValue = mPrefs.getInt(SettingsActivity.PREF_DRAG_HANDLE_COLOR,
                DEFAULT_COLOR);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_color);
        mResources = getContext().getResources();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mLightColorView = (ImageView) view.findViewById(R.id.light_color);
        updatePreferenceViews();
    }

    private void updatePreferenceViews() {
        final int width = (int) mResources
                .getDimension(R.dimen.color_button_width);
        final int height = (int) mResources
                .getDimension(R.dimen.color_button_height);

        if (mLightColorView != null) {
            mLightColorView.setEnabled(true);
            mLightColorView.setImageDrawable(createRectShape(width, height,
                    mColorValue));
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        final ColorPickerDialog d = new ColorPickerDialog(getContext(),
                mColorValue);
        d.setAlphaSliderVisible(true);

        d.setButton(AlertDialog.BUTTON_POSITIVE,
                mResources.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mColorValue = d.getColor();
                        updatePreferenceViews();
                        mPrefs.edit()
                                .putInt(SettingsActivity.PREF_DRAG_HANDLE_COLOR,
                                        mColorValue).commit();
                    }
                });
        d.setButton(AlertDialog.BUTTON_NEGATIVE,
                mResources.getString(R.string.cancel),
                (DialogInterface.OnClickListener) null);
        d.show();
    }

    public int getColor() {
        return mColorValue;
    }

    public void setColor(int color) {
        mColorValue = color;
        updatePreferenceViews();
    }

    private static ShapeDrawable createRectShape(int width, int height,
            int color) {
        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.setIntrinsicHeight(height);
        shape.setIntrinsicWidth(width);
        shape.getPaint().setColor(color);
        return shape;
    }
}
