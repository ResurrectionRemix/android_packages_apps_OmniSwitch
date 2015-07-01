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
    public static Drawable rotate(Resources resources, Drawable image, int deg) {
        if (!(image instanceof BitmapDrawable)) {
            return image;
        }
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));

        Bitmap b = ((BitmapDrawable) image).getBitmap();
        Bitmap bmResult = Bitmap.createBitmap(b.getWidth(), b.getHeight(),
                Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmResult);
        canvas.rotate(deg, b.getWidth() / 2, b.getHeight() / 2);
        canvas.drawBitmap(b, 0, 0, null);
        return new BitmapDrawable(resources, bmResult);
    }

    public static Drawable resize(Resources resources, Drawable image,
            int iconSize, int borderSize, float density) {
        int size = Math.round(iconSize * density);
        int border = Math.round(borderSize * density);
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));

        if (image instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) image).getBitmap();
            // create a border around the icon
            Bitmap bmResult = Bitmap.createBitmap(size + border, size + border,
                    Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bmResult);
            Bitmap bitmapResized = Bitmap.createScaledBitmap(b, size, size,
                    true);
            canvas.drawBitmap(bitmapResized, border / 2, border / 2, null);
            return new BitmapDrawable(resources, bmResult);
        } else if (image instanceof VectorDrawable) {
            // create a border around the icon
            Bitmap bmResult = Bitmap.createBitmap(size + border, size + border,
                    Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bmResult);
            Drawable d = image.mutate();
            d.setBounds(border / 2, border / 2, size, size);
            d.draw(canvas);
            return new BitmapDrawable(resources, bmResult);
        }
        return image;
    }

    public static Drawable resize(Resources resources, Drawable image,
            int iconSize, float density) {
        int size = Math.round(iconSize * density);
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));

        if (image instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) image).getBitmap();
            Bitmap bmResult = Bitmap.createBitmap(size, size,
                    Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bmResult);
            Bitmap bitmapResized = Bitmap.createScaledBitmap(b, size, size, true);
            canvas.drawBitmap(bitmapResized, 0, 0, null);
            return new BitmapDrawable(resources, bmResult);
        } else if (image instanceof VectorDrawable) {
            Bitmap bmResult = Bitmap.createBitmap(size, size,
                    Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bmResult);
            Drawable d = image.mutate();
            d.setBounds(0, 0, size, size);
            d.draw(canvas);
            return new BitmapDrawable(resources, bmResult);
        }
        return image;
    }

    public static Drawable resizeBitmap(Resources resources, Bitmap image,
            int width, int height) {
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));

        Bitmap bmResult = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmResult);
        Bitmap bitmapResized = Bitmap.createScaledBitmap(image, width, height, true);
        canvas.drawBitmap(bitmapResized, 0, 0, null);
        return new BitmapDrawable(resources, bmResult);
    }

    public static Drawable colorize(Resources resources, int color,
            Drawable image) {
        // remove any alpha
        color = color & ~0xff000000;
        color = color | 0xff000000;

        if (image instanceof BitmapDrawable || image instanceof VectorDrawable) {
            Drawable d = image.mutate();
            d.setColorFilter(color, Mode.SRC_ATOP);
            return d;
        }
        return image;
    }

    public static Drawable shadow(Resources resources, Drawable image) {
        if (!(image instanceof BitmapDrawable)) {
            return image;
        }
        Bitmap b = ((BitmapDrawable) image).getBitmap();

        BlurMaskFilter blurFilter = new BlurMaskFilter(5,
                BlurMaskFilter.Blur.OUTER);
        Paint shadowPaint = new Paint();
        shadowPaint.setMaskFilter(blurFilter);

        int[] offsetXY = new int[2];
        Bitmap b2 = b.extractAlpha(shadowPaint, offsetXY);

        Bitmap bmResult = Bitmap.createBitmap(b.getWidth(), b.getHeight(),
                Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));

        canvas.setBitmap(bmResult);
        canvas.drawBitmap(b2, 0, 0, null);
        canvas.drawBitmap(b, -offsetXY[0], -offsetXY[1], null);

        return new BitmapDrawable(resources, bmResult);
    }

    public static Drawable getDefaultActivityIcon(Context context) {
        return context.getResources().getDrawable(R.drawable.ic_default);
    }

    public static Drawable compose(Resources resources, Drawable icon, Context context, Drawable iconBack,
            Drawable iconMask, Drawable iconUpon, float scale, int iconSize, float density) {
        int size = Math.round(iconSize * density);
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));

        int width = 0, height = 0;
        if (icon instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) icon).getBitmap();
            width = b.getWidth();
            height = b.getHeight();
        } else if (icon instanceof VectorDrawable) {
            width = height = iconSize;
        }
        if (width <= 0 || height <= 0) {
            return icon;
        }

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
            ((BitmapDrawable) iconMask).getPaint().setXfermode(
                    new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            iconMask.draw(canvas);
        }
        if (iconBack != null) {
            iconBack.setBounds(icon.getBounds());
            ((BitmapDrawable) iconBack).getPaint().setXfermode(
                    new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
            iconBack.draw(canvas);
        }
        if (iconUpon != null) {
            iconUpon.setBounds(icon.getBounds());
            iconUpon.draw(canvas);
        }
        icon.setBounds(oldBounds);
        return new BitmapDrawable(resources, bitmap);
    }

    public static Drawable overlay(Resources resources, Bitmap b,
            Drawable icon, int width, int height, String label, float density,
            int iconSize, boolean bgStyle, boolean showLabel, boolean sideHeader) {
        final Canvas canvas = new Canvas();
        final int iconSizePx = Math.round(iconSize * density);
        final int borderPx = Math.round(5 * density);

        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));
        final Bitmap bmp = Bitmap.createBitmap(sideHeader ? width + iconSizePx : width,
                sideHeader ? height : height + iconSizePx,
                Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmp);
        final Bitmap bitmapResized = Bitmap.createScaledBitmap(b, width, height, true);
        canvas.drawBitmap(bitmapResized, sideHeader ? iconSizePx : 0, sideHeader ? 0 : iconSizePx, null);
        final Drawable iconResized = resize(resources, icon,  iconSize, density);

        final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Typeface font = Typeface.create("sans-serif", Typeface.NORMAL);
        textPaint.setTypeface(font);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        final int startTextPx = iconSizePx + borderPx;
        final int textSize = Math.round(14 * density);
        textPaint.setTextSize(textSize);
        label = TextUtils.ellipsize(label, textPaint, width - startTextPx - borderPx, TextUtils.TruncateAt.END).toString();

        if (bgStyle) {
            final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setStyle(Paint.Style.FILL);
            bgPaint.setColor(resources.getColor(R.color.button_bg_flat_color));
            if (sideHeader)  {
                canvas.drawRect(0, 0, iconSizePx, height, bgPaint);
            } else {
                canvas.drawRect(0, 0, width, iconSizePx, bgPaint);
            }
        } else {
            textPaint.setShadowLayer(5, 0, 0, Color.BLACK);
        }
        canvas.drawBitmap(((BitmapDrawable) iconResized).getBitmap(), 0, 0, null);
        if (showLabel) {
            if (sideHeader) {
                canvas.save();
                int xPos = (int) ((iconSizePx / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)) ; 
                int yPos = height - borderPx;
                canvas.rotate(270, xPos, yPos);
                canvas.drawText(label, xPos, yPos, textPaint);
                canvas.restore();
            } else {
                int yPos = (int) ((iconSizePx / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)) ; 
                int xPos = startTextPx;
                canvas.drawText(label, xPos, yPos, textPaint);
            }
        }
        return new BitmapDrawable(resources, bmp);
    }

    public static Drawable memImage(Resources resources, int size,
            float density, boolean horizontal, boolean bgStyle, String line1, String line2) {
        final Canvas canvas = new Canvas();
        final int borderPx = Math.round(5 * density);
        final int width = size;
        final int height = (int) (size * 2);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));
        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmp);

        final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Typeface font = Typeface.create("sans-serif", Typeface.NORMAL);
        textPaint.setTypeface(font);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        final int textSize = Math.round(14 * density);
        textPaint.setTextSize(textSize);
        if (!bgStyle) {
            textPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);
        }
        line1 = TextUtils.ellipsize(line1, textPaint, height, TextUtils.TruncateAt.END).toString();
        line2 = TextUtils.ellipsize(line2, textPaint, height, TextUtils.TruncateAt.END).toString();

        int xPos1 = width / 2 - borderPx;
        int xPos2 = width - borderPx;
        int yPos = height;

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
}
