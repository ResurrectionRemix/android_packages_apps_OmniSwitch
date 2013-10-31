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

import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {
	private static final String PREF_SERVICE_STATE = "toggle_service";
	private static final String PREF_ORIENTATION = "orientation";
	private static final String PREF_DRAG_HANDLE_SIZE = "drag_handle_size";

	private SwitchPreference mToggleService;
	private ListPreference mOrientation;
	private ListPreference mDragHandleSize;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		addPreferencesFromResource(R.xml.recents_settings);

		mToggleService = (SwitchPreference) findPreference(PREF_SERVICE_STATE);
		mToggleService.setChecked(RecentsService.isRunning());
		mToggleService.setOnPreferenceChangeListener(this);
		
		mOrientation = (ListPreference) findPreference(PREF_ORIENTATION);
		mOrientation.setOnPreferenceChangeListener(this);
		List<CharSequence> values = Arrays.asList(mOrientation.getEntryValues());
		int idx = values.indexOf(prefs.getString("orientation", mOrientation.getEntryValues()[0].toString()));
		mOrientation.setValueIndex(idx);
		mOrientation.setSummary(mOrientation.getEntries()[idx]);

		mDragHandleSize = (ListPreference) findPreference(PREF_DRAG_HANDLE_SIZE);
		mDragHandleSize.setOnPreferenceChangeListener(this);
		values = Arrays.asList(mDragHandleSize.getEntryValues());
		idx = values.indexOf(prefs.getString("drag_handle_size", mDragHandleSize.getEntryValues()[1].toString()));
		mDragHandleSize.setValueIndex(idx);
		mDragHandleSize.setSummary(mDragHandleSize.getEntries()[idx]);
	}

	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference == mToggleService) {
			boolean value = (Boolean) newValue;

			Intent svc = new Intent(this, RecentsService.class);
			if (value) {
				Intent killRecent = new Intent(
						RecentsService.RecentsReceiver.ACTION_KILL_RECENTS);
				sendBroadcast(killRecent);

				startService(svc);
			} else {
				Intent killRecent = new Intent(
						RecentsService.RecentsReceiver.ACTION_KILL_RECENTS);
				sendBroadcast(killRecent);
			}
			return true;
		} else if (preference == mOrientation){
			String value = (String) newValue;
			List<CharSequence> values = Arrays.asList(mOrientation.getEntryValues());
			int idx = values.indexOf(value);
			mOrientation.setSummary(mOrientation.getEntries()[idx]);
			mOrientation.setValueIndex(idx);
		} else if (preference == mDragHandleSize){
			String value = (String) newValue;
			List<CharSequence> values = Arrays.asList(mDragHandleSize.getEntryValues());
			int idx = values.indexOf(value);
			mDragHandleSize.setSummary(mDragHandleSize.getEntries()[idx]);
			mDragHandleSize.setValueIndex(idx);
		}
		
		return false;
	}
}
