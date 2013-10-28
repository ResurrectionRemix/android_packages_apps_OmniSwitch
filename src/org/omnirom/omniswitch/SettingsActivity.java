package org.omnirom.omniswitch;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;


public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener {
	private static final String PREF_SERVICE_STATE = "toggle_service";
	private SwitchPreference mToggleService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.recents_settings);

        mToggleService = (SwitchPreference) findPreference(PREF_SERVICE_STATE);
        mToggleService.setChecked(RecentsService.isRunning());
        mToggleService.setOnPreferenceChangeListener(this);
    }

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mToggleService) {
        	boolean value = (Boolean) newValue;
        	
        	Intent svc = new Intent(this, RecentsService.class);
        	if (value){
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
        }
		return false;
	}
}
