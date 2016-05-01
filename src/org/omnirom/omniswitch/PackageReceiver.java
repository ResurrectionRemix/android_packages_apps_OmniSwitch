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
package org.omnirom.omniswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class PackageReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = false;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (SwitchService.isRunning()){
            Uri data = intent.getData();
            String packageName = data.getEncodedSchemeSpecificPart();
            if (DEBUG) Log.d("OmniSwitch:PackageReceiver", "onReceive " + intent.getAction() + " " + packageName);
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
                PackageManager.getInstance(context).removePackageIconCache(packageName);
            }
            PackageManager.getInstance(context).reloadPackageList();
        }
    }
}
