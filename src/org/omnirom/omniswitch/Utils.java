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

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

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

      handler.post(new Runnable(){
          @Override
          public void run() {
              im.injectInputEvent(downEvent,InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
          }});

      handler.postDelayed(new Runnable(){
        @Override
        public void run() {
            im.injectInputEvent(upEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }}, 20);
  }
}
