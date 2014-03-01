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
import java.util.List;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

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

    public static String getActivityLabel(PackageManager pm, Intent intent) {
        ActivityInfo ai = intent.resolveActivityInfo(pm,
                PackageManager.GET_ACTIVITIES);
        String label = null;

        if (ai != null) {
            label = ai.loadLabel(pm).toString();
            if (label == null) {
                label = ai.name;
            }
        }
        return label;
    }
    
    public static void buttonStringToArry(String buttonString, boolean[] buttons){
        String[] splitParts = buttonString.split(",");
        for(int i = 0; i < splitParts.length; i++){
            if (splitParts[i].equals("0")){
                buttons[i]=false;
            } else if (splitParts[i].equals("1")){
                buttons[i]=true;
            }
        }
    }
    
    public static String buttonArrayToString(boolean[] buttons){
        String buttonString = "";
        for(int i = 0; i < buttons.length; i++){
            boolean value = buttons[i];
            if (value){
                buttonString = buttonString + "1,";
            } else {
                buttonString = buttonString + "0,";
            }
        }
        if(buttonString.length() > 0){
            buttonString = buttonString.substring(0, buttonString.length() - 1);
        }
        return buttonString;
    }
    
    public static boolean[] getDefaultButtons(){
        boolean[] buttons = new boolean[SettingsActivity.NUM_BUTTON];
        for(int i = 0; i < buttons.length; i++){
            buttons[i] = true;
        }
        return buttons;
    }
}
