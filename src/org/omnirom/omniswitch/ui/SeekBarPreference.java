package org.omnirom.omniswitch.ui;

import org.omnirom.omniswitch.R;
import org.omnirom.omniswitch.R.id;
import org.omnirom.omniswitch.R.layout;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends Preference implements
        OnSeekBarChangeListener {

    public static int maximum = 100;
    public static int interval = 5;

    private TextView monitorBox;
    private SeekBar bar;

    int currentValue = 60;

    private OnPreferenceChangeListener changer;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {

        View layout = View.inflate(getContext(), R.layout.slider_preference,
                null);

        monitorBox = (TextView) layout.findViewById(R.id.monitor_box);
        bar = (SeekBar) layout.findViewById(R.id.seek_bar);
        bar.setProgress(currentValue);
        monitorBox.setText(currentValue + "%");
        bar.setOnSeekBarChangeListener(this);

        return layout;
    }

    public void setInitValue(int progress) {
        currentValue = progress;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // TODO Auto-generated method stub
        return super.onGetDefaultValue(a, index);
    }

    @Override
    public void setOnPreferenceChangeListener(
            OnPreferenceChangeListener onPreferenceChangeListener) {
        changer = onPreferenceChangeListener;
        super.setOnPreferenceChangeListener(onPreferenceChangeListener);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {

        progress = Math.round(((float) progress) / interval) * interval;
        currentValue = progress;
        monitorBox.setText(progress + "%");
        changer.onPreferenceChange(this, Integer.toString(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
