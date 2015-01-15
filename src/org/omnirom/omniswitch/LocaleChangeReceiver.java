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
package org.omnirom.omniswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import org.omnirom.omniswitch.SwitchConfiguration;

public class LocaleChangeReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = false;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (DEBUG) Log.d("LocaleChangeReceiver", "onReceive " + intent.getAction());
        if (SwitchService.isRunning()){
            PackageManager.getInstance(context).updatePackageList();

            // to force a reload of all adapters that show packages
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean value = prefs.getBoolean("LOCALE_CHANGED", false);
            prefs.edit().putBoolean("LOCALE_CHANGED", !value).commit();
        }
    }
}
