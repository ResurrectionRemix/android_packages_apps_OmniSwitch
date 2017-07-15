/*
 *  Copyright (C) 2017 The OmniROM Project
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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.text.TextPaint;
import android.text.TextUtils;

public class BitmapUtils {
    private static TextPaint sTextPaint;
    private static Paint sLockedAppsPaint;
    private static Paint sDockedAppsPaint;
    private static Paint sDefaultBgPaint;

    private static TextPaint getTextPaint() {
        if (sTextPaint == null) {
            sTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            Typeface font = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
            sTextPaint.setTypeface(font);
            sTextPaint.setColor(Color.WHITE);
            sTextPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);
            sTextPaint.setTextAlign(Paint.Align.LEFT);
        }
        return sTextPaint;
    }

    private static Paint getLockedAppsPaint(Resources resources) {
        if (sLockedAppsPaint == null) {
            sLockedAppsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            sLockedAppsPaint.setStyle(Paint.Style.FILL);
            sLockedAppsPaint.setColor(resources.getColor(R.color.locked_task_bg_color));
        }
        return sLockedAppsPaint;
    }

    private static Paint geDockedAppsPaint(Resources resources) {
        if (sDockedAppsPaint == null) {
            sDockedAppsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            sDockedAppsPaint.setStyle(Paint.Style.FILL);
            sDockedAppsPaint.setColor(resources.getColor(R.color.docked_task_bg_color));
        }
        return sDockedAppsPaint;
    }

    private static Paint getDefaultBgPaint(Resources resources) {
        if (sDefaultBgPaint == null) {
            sDefaultBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            sDefaultBgPaint.setStyle(Paint.Style.FILL);
            sDefaultBgPaint.setColor(resources.getColor(R.color.button_bg_flat_color));
        }
        return sDefaultBgPaint;
    }

    public static Drawable resize(Resources resources, Drawable image,
            int iconSize, int borderSize, float density) {
        int size = Math.round(iconSize * density);
        int border = Math.round(borderSize * density);
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));

        Bitmap bmResult = Bitmap.createBitmap(size + border, size + border,
                Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmResult);
        image.setBounds(border / 2, border / 2, size, size);
        image.draw(canvas);
        return new BitmapDrawable(resources, bmResult);
    }

    public static Drawable colorize(Resources resources, int color,
            Drawable image) {
        // remove any alpha
        color = color & ~0xff000000;
        color = color | 0xff000000;

        Drawable d = image.mutate();
        d.setColorFilter(color, Mode.SRC_ATOP);
        return d;
    }

    public static BitmapDrawable shadow(Resources resources, Drawable image) {
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));
        final int imageWidth = image.getIntrinsicWidth();
        final int imageHeight = image.getIntrinsicHeight();
        final Bitmap b = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(b);
        image.setBounds(0, 0, imageWidth, imageHeight);
        image.draw(canvas);

        BlurMaskFilter blurFilter = new BlurMaskFilter(5,
                BlurMaskFilter.Blur.OUTER);
        Paint shadowPaint = new Paint();
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setMaskFilter(blurFilter);

        int[] offsetXY = new int[2];
        Bitmap b2 = b.extractAlpha(shadowPaint, offsetXY);

        Bitmap bmResult = Bitmap.createBitmap(b.getWidth(), b.getHeight(),
                Bitmap.Config.ARGB_8888);

        canvas.setBitmap(bmResult);
        canvas.drawBitmap(b2, offsetXY[0], offsetXY[1], null);
        canvas.drawBitmap(b, 0, 0, null);

        return new BitmapDrawable(resources, bmResult);
    }

    public static Drawable getDefaultActivityIcon(Context context) {
        return context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
    }

    public static Drawable compose(Resources resources, Drawable icon, Context context, Drawable iconBack,
            Drawable iconMask, Drawable iconUpon, float scale, int iconSize, float density) {
        int size = Math.round(iconSize * density);
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));

        int width = size;
        int height = size;

        // TODO
        if (iconBack == null && iconMask == null && iconUpon == null){
            scale = 1.0f;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);

        Rect oldBounds = new Rect();
        oldBounds.set(icon.getBounds());
        icon.setBounds(0, 0, width, height);
        canvas.save();
        canvas.scale(scale, scale, width / 2, height/2);
        icon.draw(canvas);
        canvas.restore();
        if (iconMask != null) {
            iconMask.setBounds(icon.getBounds());
            BitmapDrawable  b = getBitmapDrawable(resources, iconMask);
            b.getPaint().setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            b.draw(canvas);
        }
        if (iconBack != null) {
            iconBack.setBounds(icon.getBounds());
            BitmapDrawable  b = getBitmapDrawable(resources, iconBack);
            b.getPaint().setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
            b.draw(canvas);
        }
        if (iconUpon != null) {
            iconUpon.setBounds(icon.getBounds());
            iconUpon.draw(canvas);
        }
        icon.setBounds(oldBounds);
        return new BitmapDrawable(resources, bitmap);
    }

    public static Drawable overlay(Resources resources, Bitmap b,
            Drawable iconResized, int width, int height, String label, float density,
            int iconSize, boolean bgStyle, boolean sideHeader,
            int iconBorderSizePx, boolean dockedTask, boolean lockedTask,
            float opacity) {
        final Canvas canvas = new Canvas();
        final int iconSizePx = Math.round(iconSize * density);
        final int textInsetPx = Math.round(5 * density);

        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));
        final Bitmap bmp = Bitmap.createBitmap(sideHeader ? width + iconBorderSizePx : width,
                sideHeader ? height : height + iconBorderSizePx,
                Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmp);
        int bSize = b.getWidth() > b.getHeight() ? b.getHeight() : b.getWidth();
        Rect src = new Rect(0, 0, bSize, bSize);
        Rect dest = new Rect(sideHeader ? iconBorderSizePx : 0, sideHeader ? 0 : iconBorderSizePx,
                width + (sideHeader ? iconBorderSizePx : 0), height + (sideHeader ? 0 : iconBorderSizePx));
        canvas.drawBitmap(b, src, dest, null);

        final TextPaint textPaint = getTextPaint();
        final int startTextPx = iconBorderSizePx + textInsetPx;
        final int textSize = Math.round(14 * density);
        textPaint.setTextSize(textSize);

        Paint bgPaint = null;
        if (dockedTask) {
            bgPaint = geDockedAppsPaint(resources);
        } else if (lockedTask) {
            bgPaint = getLockedAppsPaint(resources);
        } else {
            bgPaint = getDefaultBgPaint(resources);
        }
        if (bgPaint != null) {
            if (bgStyle) {
                bgPaint.setAlpha(255);
            } else {
                bgPaint.setAlpha((int) (255 * opacity));
            }
            if (sideHeader)  {
                canvas.drawRect(0, 0, iconBorderSizePx, height, bgPaint);
            } else {
                canvas.drawRect(0, 0, width, iconBorderSizePx, bgPaint);
            }
        }
        if (iconResized != null) {
            final int iconInset = (iconBorderSizePx - iconSizePx) / 2;
            iconResized.setBounds(iconInset, iconInset, iconSizePx + iconInset, iconSizePx + iconInset);
            iconResized.draw(canvas);
        }
        if (label != null) {
            label = TextUtils.ellipsize(label, textPaint, width - startTextPx - textInsetPx, TextUtils.TruncateAt.END).toString();
            if (sideHeader) {
                canvas.save();
                int xPos = (int) ((iconBorderSizePx / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)) ; 
                int yPos = height - textInsetPx;
                canvas.rotate(270, xPos, yPos);
                canvas.drawText(label, xPos, yPos, textPaint);
                canvas.restore();
            } else {
                int yPos = (int) ((iconBorderSizePx / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)) ; 
                int xPos = startTextPx;
                canvas.drawText(label, xPos, yPos, textPaint);
            }
        }
        return new BitmapDrawable(resources, bmp);
    }

    public static Drawable memImage(Resources resources, int size,
            float density, boolean horizontal, String line1, String line2) {
        final Canvas canvas = new Canvas();
        final int borderPx = Math.round(5 * density);
        final int width = size;
        final int height = (int) (size * 2);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));
        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmp);

        final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Typeface font = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        textPaint.setTypeface(font);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        final int textSize = Math.round(14 * density);
        textPaint.setTextSize(textSize);
        textPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);

        line1 = TextUtils.ellipsize(line1, textPaint, height, TextUtils.TruncateAt.END).toString();
        line2 = TextUtils.ellipsize(line2, textPaint, height, TextUtils.TruncateAt.END).toString();

        int xPos1 = width / 2 - borderPx / 2;
        int xPos2 = width - borderPx / 2;
        int yPos = height - borderPx;

        canvas.save();
        canvas.rotate(270, xPos1, yPos);
        canvas.drawText(line1, xPos1, yPos, textPaint);
        canvas.restore();

        canvas.save();
        canvas.rotate(270, xPos2, yPos);
        canvas.drawText(line2, xPos2, yPos, textPaint);
        canvas.restore();

        return new BitmapDrawable(resources, bmp);
    }

    public static BitmapDrawable getBitmapDrawable(Resources resources, Drawable image) {
        if (image instanceof BitmapDrawable) {
            return (BitmapDrawable) image;
        }
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));

        Bitmap bmResult = Bitmap.createBitmap(image.getIntrinsicWidth(), image.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmResult);
        image.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        image.draw(canvas);
        return new BitmapDrawable(resources, bmResult);
    }
}
