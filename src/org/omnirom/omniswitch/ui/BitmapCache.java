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

import java.util.HashMap;
import java.util.Map;

import org.omnirom.omniswitch.PackageManager;
import org.omnirom.omniswitch.SwitchConfiguration;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

// TODO: this is brain-dead simple but better then nothing for now
public class BitmapCache {

    private static BitmapCache sInstance;
    private Map<String, Drawable> mBitmaps;

    public static BitmapCache getInstance() {
        if (sInstance == null){
            sInstance = new BitmapCache();
        }
        return sInstance;
    }

    private BitmapCache() {
        mBitmaps = new HashMap<String, Drawable>();
    }

    public void clear() {
        mBitmaps.clear();
    }

    private String bitmapHash(String intent, SwitchConfiguration configuration) {
        return intent + configuration.mIconSize;
    }

    private String bitmapHash(String packageName, int iconId, SwitchConfiguration configuration) {
        return packageName + "_" + iconId + "_" + configuration.mIconSize;
    }

    public Drawable getResized(Resources resources, Drawable icon, String intent, SwitchConfiguration configuration) {
        String key = bitmapHash(intent, configuration);
        Drawable d = mBitmaps.get(key);
        if (d == null){
            d = BitmapUtils.resize(resources,
                    icon, configuration.mIconSize,
                    configuration.mIconBorder,
                    configuration.mDensity);
            mBitmaps.put(key, d);
        }
        return d;
    }

    public Drawable getResized(Resources resources, PackageManager.PackageItem packageItem, SwitchConfiguration configuration) {
        String key = bitmapHash(packageItem.getIntent(), configuration);
        Drawable d = mBitmaps.get(key);
        if (d == null){
            d = BitmapUtils.resize(resources,
                    packageItem.getIcon(), configuration.mIconSize,
                    configuration.mIconBorder,
                    configuration.mDensity);
            mBitmaps.put(key, d);
        }
        return d;
    }

    public Drawable getResized(Resources resources, String packageName, int iconId, SwitchConfiguration configuration) {
        String key = bitmapHash(packageName, iconId, configuration);
        Drawable d = mBitmaps.get(key);
        if (d == null){
            d = BitmapUtils.resize(resources,
                    resources.getDrawable(iconId), configuration.mIconSize,
                    configuration.mIconBorder,
                    configuration.mDensity);
            mBitmaps.put(key, d);
        }
        return d;
    }
}
