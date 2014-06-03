/*
 *  Copyright (C) 2014 The OmniROM Project
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

import org.omnirom.omniswitch.TaskDescription;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

public class PackageTextView extends TextView {

    private String mIntent;
    private Drawable mOriginalImage;
    private Drawable mSmallImage;
    private Drawable mGlowImage;
    private TaskDescription mTask;
    private Drawable mThumbImage;
    private CharSequence mLabel;
    private boolean mThumbLoaded;
    private Runnable mAction;

    public PackageTextView(Context context) {
        super(context);
    }

    public PackageTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PackageTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setIntent(String intent) {
        mIntent = intent;
    }

    public String getIntent() {
        return mIntent;
    }

    public void setOriginalImage(Drawable image) {
        mOriginalImage = image;
    }

    public Drawable getOriginalImage() {
        return mOriginalImage;
    }

    public TaskDescription getTask() {
        return mTask;
    }

    public void setTask(TaskDescription task) {
        mTask = task;
    }

    public Drawable getThumb() {
        if (!mThumbLoaded) {
            return mOriginalImage;
        }
        return mThumbImage;
    }

    public void setThumb(Drawable thumb) {
        mThumbImage = thumb;
        mThumbLoaded = true;
    }

    public CharSequence getLabel() {
        return mLabel;
    }

    public void setLabel(CharSequence label) {
        mLabel = label;
    }

    public Drawable getSmallImage() {
        if (mSmallImage == null) {
            return mOriginalImage;
        }
        return mSmallImage;
    }

    public void setSmallImage(Drawable smallImage) {
        mSmallImage = smallImage;
    }

    public boolean isThumbLoaded() {
        return mThumbLoaded;
    }

    public void runAction() {
        if (mAction != null) {
            mAction.run();
        }
    }

    public void setAction(Runnable action) {
        mAction = action;
    }

    public boolean isAction() {
        return mAction != null;
    }

    public void setGlowImage(Drawable image) {
        mGlowImage = image;
    }

    public Drawable getGlowImage() {
        return mGlowImage;
    }

    @Override
    public String toString() {
        return getLabel().toString();
    }
}
