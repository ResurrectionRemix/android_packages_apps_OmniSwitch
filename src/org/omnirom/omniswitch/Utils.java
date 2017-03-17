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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.omnirom.omniswitch.launcher.Launcher;

public class Utils {

    public static void parseFavorites(String favoriteListString,
            List<String> favoriteList) {
        if (favoriteListString.length() == 0){
            return;
        }

        if (favoriteListString.indexOf("##") == -1){
            favoriteList.add(favoriteListString);
            return;
        }
        String[] split = favoriteListString.split("##");
        for (int i = 0; i < split.length; i++) {
            favoriteList.add(split[i]);
        }
    }

    public static String flattenFavorites(List<String> favoriteList) {
        Iterator<String> nextFavorite = favoriteList.iterator();
        StringBuffer buffer = new StringBuffer();
        while (nextFavorite.hasNext()) {
            String favorite = nextFavorite.next();
            buffer.append(favorite + "##");
        }
        if (buffer.length() != 0) {
            return buffer.substring(0, buffer.length() - 2).toString();
        }
        return buffer.toString();
    }

    public static String getActivityLabel(android.content.pm.PackageManager pm, Intent intent) {
        ActivityInfo ai = intent.resolveActivityInfo(pm,
                android.content.pm.PackageManager.GET_ACTIVITIES);
        String label = null;

        if (ai != null) {
            label = ai.loadLabel(pm).toString();
            if (label == null) {
                label = ai.name;
            }
        }
        return label;
    }

    public static Map<Integer, Boolean> buttonStringToMap(String buttonString, String defaultButtonString){
        Map<Integer, Boolean> buttons = new LinkedHashMap<Integer, Boolean>();
        String[] splitParts = buttonString.split(",");
        for(int i = 0; i < splitParts.length; i++){
            String[] buttonParts = splitParts[i].split(":");
            Integer key = Integer.valueOf(buttonParts[0]);
            boolean value = buttonParts[1].equals("1") ? true : false;
            buttons.put(key, value);
        }
        // add any entries that are more in the default
        String[] splitPartsDefault = defaultButtonString.split(",");
        if (splitPartsDefault.length > splitParts.length){
            for(int i = splitParts.length; i < splitPartsDefault.length; i++){
                String[] buttonParts = splitPartsDefault[i].split(":");
                Integer key = Integer.valueOf(buttonParts[0]);
                boolean value = buttonParts[1].equals("1") ? true : false;
                buttons.put(key, value);
            }
        }
        return buttons;
    }

    public static String buttonMapToString(Map<Integer, Boolean> buttons){
        String buttonString = "";
        Iterator<Integer> nextBoolean = buttons.keySet().iterator();
        while(nextBoolean.hasNext()){
            Integer key = nextBoolean.next();
            boolean value = buttons.get(key);
            if (value){
                buttonString = buttonString + key +":1,";
            } else {
                buttonString = buttonString + key + ":0,";
            }
        }
        if(buttonString.length() > 0){
            buttonString = buttonString.substring(0, buttonString.length() - 1);
        }
        return buttonString;
    }

    public static void triggerVirtualKeypress(final Handler handler, final int keyCode) {
        final InputManager im = InputManager.getInstance();
        long now = SystemClock.uptimeMillis();

        final KeyEvent downEvent = new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
              keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
              KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY, InputDevice.SOURCE_CLASS_BUTTON);
        final KeyEvent upEvent = KeyEvent.changeAction(downEvent,
              KeyEvent.ACTION_UP);

        // add a small delay to make sure everything behind got focus
        handler.postDelayed(new Runnable(){
            @Override
            public void run() {
                im.injectInputEvent(downEvent,InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
            }}, 100);

        handler.postDelayed(new Runnable(){
            @Override
            public void run() {
                im.injectInputEvent(upEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
            }}, 150);
    }

    public static void toggleImmersiveMode(Context context) {
        /*boolean immersive = Settings.System.getInt(context.getContentResolver(),
                Settings.System.IMMERSIVE_MODE, 0) == 1;

        Settings.System.putInt(context.getContentResolver(),
                Settings.System.IMMERSIVE_MODE, !immersive ? 1 : 0);*/
    }

    public static void removeFromFavorites(Context context, String item, List<String> favoriteList) {
        if (favoriteList.contains(item)){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            favoriteList.remove(item);
            prefs.edit().putString(SettingsActivity.PREF_FAVORITE_APPS,
                    Utils.flattenFavorites(favoriteList)).commit();
        }
    }

    public static void addToFavorites(Context context, String item, List<String> favoriteList) {
        if (!favoriteList.contains(item)){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            favoriteList.add(item);
            prefs.edit().putString(SettingsActivity.PREF_FAVORITE_APPS,
                    Utils.flattenFavorites(favoriteList)).commit();
        }
    }

    public static boolean isLockToAppEnabled(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), Settings.System.LOCK_TO_APP_ENABLED)
                != 0;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

     public static boolean isInLockTaskMode() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch(RemoteException e) {
        }
        return false;
    }

    public static boolean isMultiStackEnabled() {
        return ActivityManager.supportsMultiWindow();
    }

    public static boolean isDockingActive() {
        if (isMultiStackEnabled()) {
            try {
                return WindowManagerGlobal.getWindowManagerService().getDockedStackSide() != WindowManager.DOCKED_INVALID;
            } catch (RemoteException e) {
            }
        }
        return false;
    }

    public static void updateFavoritesList(Context context, SwitchConfiguration config, List<String> favoriteList) {
        favoriteList.clear();
        favoriteList.addAll(config.mFavoriteList);

        List<String> fList = new ArrayList<String>();
        fList.addAll(favoriteList);
        Iterator<String> nextFavorite = fList.iterator();
        while (nextFavorite.hasNext()) {
            String intent = nextFavorite.next();
            PackageManager.PackageItem packageItem = PackageManager
                    .getInstance(context).getPackageItem(intent);
            if (packageItem == null) {
                favoriteList.remove(intent);
            }
        }
    }

    public static boolean isPrefKeyForForceUpdate(String key) {
        if (key.equals(SettingsActivity.PREF_BG_STYLE) ||
                key.equals(SettingsActivity.PREF_SHOW_LABELS) ||
                key.equals(SettingsActivity.PREF_ICON_SIZE) ||
                key.equals(SettingsActivity.PREF_ICONPACK) ||
                key.equals(SettingsActivity.PREF_THUMB_SIZE) ||
                key.equals(SwitchService.DPI_CHANGE)) {
            return true;
        }
        return false;
    }

    public static void enableLauncherMode(Context context, boolean value) {
        final android.content.pm.PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, Launcher.class),
                value ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
    }

    public static List<String> getFavoriteListFromStats(Context context, int count) {
        List<String> favoriteList = new ArrayList<String>();
        List<String> topLaunchList = SwitchStatistics.getInstance(context).getTopmostLaunches(count);
        for (String pkgName : topLaunchList) {
            List<String> pkgList = PackageManager.getInstance(context).getPackageListForPackageName(pkgName);
            if (pkgList.size() != 0) {
                favoriteList.addAll(pkgList);
            }
        }
        return favoriteList;
    }

    public static boolean isNycMR1OrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
    }

    public static boolean isNycOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }
}
