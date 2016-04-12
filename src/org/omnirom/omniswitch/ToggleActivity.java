/*
 * Copyright (C) 2016 The DirtyUnicorns Project
 *
 * @author Randall Rushing <randall.rushing@gmail.com>
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

import android.app.Activity;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;

public class ToggleActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String action = getIntent().getAction();
        if (TextUtils.equals(action, Intent.ACTION_CREATE_SHORTCUT)) {
            ShortcutIconResource icon = ShortcutIconResource.fromContext(this,
                    R.drawable.ic_launcher);
            Intent shortcutIntent = new Intent();
            shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(this,
                    ToggleActivity.class));
            shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.shortcut_label_toggle_overlay));
            shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
            setResult(RESULT_OK, shortcutIntent);
        } else {
            sendBroadcastAsUser(new Intent(SwitchService.RecentsReceiver.ACTION_TOGGLE_OVERLAY),
                    UserHandle.CURRENT_OR_SELF);
        }
        finish();
    }
}
