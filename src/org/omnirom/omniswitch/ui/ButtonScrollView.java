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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class ButtonScrollView extends HorizontalScrollView {

    public ButtonScrollView(Context context) {
        super(context);
    }

    public ButtonScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        resetGlow();
        super.onScrollChanged(l, t, oldl, oldt);
    }

    private void resetGlow() {
        for(int i=0; i < getChildCount(); ++i) {
            View child = getChildAt(i);
            if(child instanceof LinearLayout){
                LinearLayout sub = (LinearLayout)child;
                for(int j=0; j < sub.getChildCount(); ++j) {
                    child = sub.getChildAt(j);
                    if(child instanceof PackageTextView){
                        ((PackageTextView)child).setBackground(((PackageTextView)child).getOriginalImage());
                    }
                }
            }
        }
    }
}
