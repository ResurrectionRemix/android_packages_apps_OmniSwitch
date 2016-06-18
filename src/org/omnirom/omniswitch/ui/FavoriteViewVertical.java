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
package org.omnirom.omniswitch.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.widget.ListView;

import org.omnirom.omniswitch.SwitchConfiguration;

public class FavoriteViewVertical extends ListView {

    private SwitchConfiguration mConfiguration;

    public FavoriteViewVertical(Context context, AttributeSet attrs) {
        super(context, attrs);
        mConfiguration = SwitchConfiguration.getInstance(mContext);
    }

    @Override
    public void onSizeChanged (int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int dividerHeight = mConfiguration.calcVerticalDivider(h);
        setDividerHeight(dividerHeight);
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        int dividerHeight = mConfiguration.calcVerticalDivider(getHeight());
        setDividerHeight(dividerHeight);
        requestLayout();
    }
}
