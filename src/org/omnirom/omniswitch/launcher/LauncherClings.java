/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.omnirom.omniswitch.launcher;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;

import org.omnirom.omniswitch.R;

class LauncherClings implements OnClickListener {
    private static final String TAG_CROP_TOP_AND_SIDES = "crop_bg_top_and_sides";

    private Launcher mLauncher;
    private LayoutInflater mInflater;
    private boolean mIsVisible;
    private View mClingView;
    private ViewGroup mRootView;

    public LauncherClings(Launcher launcher) {
        mLauncher = launcher;
        mInflater = LayoutInflater.from(mLauncher);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.cling_dismiss) {
            dismissCling();
        }
    }

    public void showWelcomeCling() {
        mIsVisible = true;
        mRootView = (ViewGroup) mLauncher.findViewById(R.id.root);
        mClingView = mInflater.inflate(R.layout.welcome_cling, null, false);
        final ViewGroup content = (ViewGroup) mClingView.findViewById(R.id.cling_content);
        mInflater.inflate(R.layout.welcome_cling_content, content);
        content.findViewById(R.id.cling_dismiss).setOnClickListener(this);

        if (TAG_CROP_TOP_AND_SIDES.equals(content.getTag())) {
            Drawable bg = new BorderCropDrawable(mLauncher.getResources().getDrawable(R.drawable.cling_bg),
                    true, true, true, false);
            content.setBackground(bg);
        }
        mRootView.addView(mClingView);
    }

    private void dismissCling() {
        if (mClingView != null) {
            mRootView.removeView(mClingView);
            mIsVisible = false;
            mClingView = null;
            mLauncher.dismissIntroScreen();
        }
    }

    public boolean isVisible() {
        return mIsVisible;
    }
}
