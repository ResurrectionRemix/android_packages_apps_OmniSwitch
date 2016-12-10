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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Rect;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuPopupHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.omnirom.omniswitch.launcher.Launcher;
import org.omnirom.omniswitch.DeepShortcutManager;
import org.omnirom.omniswitch.PackageManager;
import org.omnirom.omniswitch.SwitchConfiguration;
import org.omnirom.omniswitch.SwitchManager;
import org.omnirom.omniswitch.Utils;
import org.omnirom.omniswitch.R;

public class ContextMenuUtils {
    private static final int FIRST_SHORTCUT_MENU = Integer.MAX_VALUE - 100;

    public static void handleLongPressFavorite(Context context, PackageManager.PackageItem packageItem,
            View view, SwitchManager recentsManager, List favoriteList) {
        final SwitchConfiguration configuration = SwitchConfiguration.getInstance(context);
        final DeepShortcutManager shortcutManager = new DeepShortcutManager(context);
        final Context wrapper = new ContextThemeWrapper(context,
                configuration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_LIGHT
                ? R.style.PopupMenuLight : R.style.PopupMenuDark);
        final PopupMenu popup = new PopupMenu(wrapper, view);
        popup.getMenuInflater().inflate(R.menu.favorite_popup_menu,
                popup.getMenu());
        Map<Integer, ShortcutInfo> scMap = new HashMap<>();
        if (shortcutManager.hasHostPermission()) {
            List<ShortcutInfo> shortcuts = shortcutManager.queryForShortcutsContainer(
                    packageItem.getIntentRaw().getComponent(), null);
            if (shortcuts != null && shortcuts.size() != 0) {
                int i =0;
                for (ShortcutInfo sc : shortcuts) {
                    scMap.put(FIRST_SHORTCUT_MENU + i, sc);
                    MenuItem item = popup.getMenu().add(Menu.NONE, FIRST_SHORTCUT_MENU + i,
                            Menu.NONE, sc.getShortLabel());
                    item.setIcon(shortcutManager.getShortcutIconDrawable(sc, configuration.mDensityDpi));
                    i++;
                }
            }
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.package_inspect_item) {
                    final String packageName = packageItem.getActivityInfo().packageName;
                    if (recentsManager != null) {
                        recentsManager.startApplicationDetailsActivity(packageName);
                    } else {
                        SwitchManager.startApplicationDetailsActivity(context, packageName);
                    }
                } else if (item.getItemId() == R.id.package_remove_favorite) {
                    Utils.removeFromFavorites(context,
                            packageItem.getIntent(), favoriteList);
                } else {
                    ShortcutInfo sc = scMap.get(item.getItemId());
                    if (sc != null) {
                        if (recentsManager != null) {
                            recentsManager.hide(true);
                        }
                        shortcutManager.startShortcut(sc, new Rect(), null);
                    }
                }
                return true;
            }
        });
        popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
            public void onDismiss(PopupMenu menu) {
            }
        });
        if (scMap.size() != 0) {
            MenuPopupHelper menuHelper = new MenuPopupHelper(wrapper, (MenuBuilder) popup.getMenu(), view);
            menuHelper.setForceShowIcon(true);
            menuHelper.show();
        } else {
            popup.show();
        }
    }

    public static void handleLongPressAppDrawer(Context context, final
            PackageManager.PackageItem packageItem, SwitchManager recentsManager, View view) {
        final SwitchConfiguration configuration = SwitchConfiguration.getInstance(context);
        final DeepShortcutManager shortcutManager = new DeepShortcutManager(context);
        final Context wrapper = new ContextThemeWrapper(context,
                configuration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_LIGHT
                ? R.style.PopupMenuLight : R.style.PopupMenuDark);
        final PopupMenu popup = new PopupMenu(wrapper, view);
        popup.getMenuInflater().inflate(R.menu.package_popup_menu,
                popup.getMenu());
        final List<String> favoritList = new ArrayList<String>();
        Utils.updateFavoritesList(context, configuration, favoritList);
        boolean addFavEnabled = !favoritList.contains(packageItem.getIntent());
        if (!addFavEnabled) {
            popup.getMenu().removeItem(R.id.package_add_favorite);
        }

        Map<Integer, ShortcutInfo> scMap = new HashMap<>();
        if (shortcutManager.hasHostPermission()) {
            List<ShortcutInfo> shortcuts = shortcutManager.queryForShortcutsContainer(
                    packageItem.getIntentRaw().getComponent(), null);
            if (shortcuts != null && shortcuts.size() != 0) {
                int i =0;
                for (ShortcutInfo sc : shortcuts) {
                    scMap.put(FIRST_SHORTCUT_MENU + i, sc);
                    MenuItem item = popup.getMenu().add(Menu.NONE, FIRST_SHORTCUT_MENU + i,
                            Menu.NONE, sc.getShortLabel());
                    item.setIcon(shortcutManager.getShortcutIconDrawable(sc, configuration.mDensityDpi));
                    i++;
                }
            }
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.package_inspect_item) {
                    final String packageName = packageItem.getActivityInfo().packageName;
                    if (recentsManager != null) {
                        recentsManager.startApplicationDetailsActivity(packageName);
                    } else {
                        SwitchManager.startApplicationDetailsActivity(context, packageName);
                    }
                } else if (item.getItemId() == R.id.package_add_favorite) {
                    Utils.addToFavorites(context, packageItem.getIntent(),
                            favoritList);
                } else {
                    ShortcutInfo sc = scMap.get(item.getItemId());
                    if (sc != null) {
                        if (recentsManager != null) {
                            recentsManager.hide(true);
                        }
                        shortcutManager.startShortcut(sc, new Rect(), null);
                    }
                }
                return true;
            }
        });
        popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
            public void onDismiss(PopupMenu menu) {
            }
        });
        if (scMap.size() != 0) {
            MenuPopupHelper menuHelper = new MenuPopupHelper(wrapper, (MenuBuilder) popup.getMenu(), view);
            menuHelper.setForceShowIcon(true);
            menuHelper.show();
        } else {
            popup.show();
        }
    }
}
