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
package org.omnirom.omniswitch.ui;

import android.content.SharedPreferences;

public interface ISwitchLayout {

    void hide(boolean fast);

    void hideHidden();

    void setHandleRecentsUpdate(boolean handleRecentsUpdate);

    boolean isHandleRecentsUpdate();

    void showHidden();

    boolean isShowing();

    void show();

    void update();

    void refresh();

    void updatePrefs(SharedPreferences prefs, String key);

    void updateLayout();

    void slideLayout(float distanceX);

    void finishSlideLayout();

    void openSlideLayout(boolean fromFling);

    void canceSlideLayout();

    void shutdownService();
}