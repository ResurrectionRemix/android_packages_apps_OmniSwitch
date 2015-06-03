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
package org.omnirom.omniswitch.ui;

import org.omnirom.omniswitch.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class LinearColorBar extends LinearLayout {
    private float mRedRatio;
    private float mYellowRatio;
    private float mGreenRatio;
    private boolean mShowingGreen;
    private int mLeftColor;
    private int mMiddleColor;
    private int mRightColor;

    final Rect mRect = new Rect();
    final Paint mPaint = new Paint();

    public LinearColorBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        mPaint.setStyle(Paint.Style.FILL);
        mLeftColor = context.getResources()
                .getColor(R.color.rambar_left_color);
        mMiddleColor = context.getResources()
                .getColor(R.color.rambar_middle_color);
        mRightColor = context.getResources()
                .getColor(R.color.rambar_right_color);
    }

    public void setRatios(float red, float yellow, float green) {
        mRedRatio = red;
        mYellowRatio = yellow;
        mGreenRatio = green;
        invalidate();
    }

    public void setShowingGreen(boolean showingGreen) {
        if (mShowingGreen != showingGreen) {
            mShowingGreen = showingGreen;
            updateIndicator();
            invalidate();
        }
    }

    private void updateIndicator() {
        int off = getPaddingTop() - getPaddingBottom();
        if (off < 0) off = 0;
        mRect.top = off;
        mRect.bottom = getHeight();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateIndicator();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();

        int left = 0;

        int right = left + (int)(width*mRedRatio);
        int right2 = right + (int)(width*mYellowRatio);
        int right3 = right2 + (int)(width*mGreenRatio);

        if (left < right) {
            mRect.left = left;
            mRect.right = right;
            mPaint.setColor(mLeftColor);
            canvas.drawRect(mRect, mPaint);
            width -= (right-left);
            left = right;
        }

        right = right2;

        if (left < right) {
            mRect.left = left;
            mRect.right = right;
            mPaint.setColor(mMiddleColor);
            canvas.drawRect(mRect, mPaint);
            width -= (right-left);
            left = right;
        }


        right = left + width;
        if (left < right) {
            mRect.left = left;
            mRect.right = right;
            mPaint.setColor(mRightColor);
            canvas.drawRect(mRect, mPaint);
        }
    }
}

