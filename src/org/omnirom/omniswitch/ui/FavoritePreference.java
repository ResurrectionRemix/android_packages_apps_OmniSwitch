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

import java.util.ArrayList;
import java.util.List;

import org.omnirom.omniswitch.SettingsActivity;
import org.omnirom.omniswitch.Utils;

import android.app.Dialog;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

public class FavoritePreference extends DialogPreference {

    /**
     * @param context
     * @param attrs
     */
    public FavoritePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
    }

    @Override
    protected Dialog createDialog() {
        String favoriteListString = getSharedPreferences().getString(SettingsActivity.PREF_FAVORITE_APPS, "");
        List<String> favoriteList = new ArrayList<String>();
        Utils.parseFavorites(favoriteListString, favoriteList);

        FavoriteDialog d = new FavoriteDialog((SettingsActivity)getContext(), favoriteList);
        return d;
    }
}
