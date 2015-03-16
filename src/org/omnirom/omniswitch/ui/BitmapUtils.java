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
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.VectorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.List;
import java.util.Collections;

public class BitmapUtils {
    public static Drawable rotate(Resources resources, Drawable image, int deg) {
        if (!(image instanceof BitmapDrawable)) {
            return image;
        }
        Bitmap b = ((BitmapDrawable) image).getBitmap();
        Bitmap bmResult = Bitmap.createBitmap(b.getWidth(), b.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(bmResult);
        tempCanvas.rotate(deg, b.getWidth() / 2, b.getHeight() / 2);
        tempCanvas.drawBitmap(b, 0, 0, null);
        return new BitmapDrawable(resources, bmResult);
    }

    public static Drawable resize(Resources resources, Drawable image,
            int iconSize, int borderSize, float density) {
        int size = Math.round(iconSize * density);
        int border = Math.round(borderSize * density);

        if (image instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) image).getBitmap();
            // create a border around the icon
            Bitmap bmResult = Bitmap.createBitmap(size + border, size + border,
                    Bitmap.Config.ARGB_8888);
            Canvas tempCanvas = new Canvas(bmResult);
            Bitmap bitmapResized = Bitmap.createScaledBitmap(b, size, size,
                    true);
            tempCanvas.drawBitmap(bitmapResized, border / 2, border / 2, null);
            return new BitmapDrawable(resources, bmResult);
        } else if (image instanceof VectorDrawable) {
            // create a border around the icon
            Bitmap bmResult = Bitmap.createBitmap(size + border, size + border,
                    Bitmap.Config.ARGB_8888);
            Canvas tempCanvas = new Canvas(bmResult);
            Drawable d = ((VectorDrawable) image).mutate();
            ((VectorDrawable) d).setBounds(border / 2, border / 2, size, size);
            ((VectorDrawable) d).draw(tempCanvas);
            return new BitmapDrawable(resources, bmResult);
        }
        return image;
    }

    public static Drawable resize(Resources resources, Drawable image,
            int iconSize, float density) {
        int size = Math.round(iconSize * density);

        if (image instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) image).getBitmap();
            Bitmap bmResult = Bitmap.createBitmap(size, size,
                    Bitmap.Config.ARGB_8888);
            Canvas tempCanvas = new Canvas(bmResult);
            Bitmap bitmapResized = Bitmap.createScaledBitmap(b, size, size, true);
            tempCanvas.drawBitmap(bitmapResized, 0, 0, null);
            return new BitmapDrawable(resources, bmResult);
        } else if (image instanceof VectorDrawable) {
            Bitmap bmResult = Bitmap.createBitmap(size, size,
                    Bitmap.Config.ARGB_8888);
            Canvas tempCanvas = new Canvas(bmResult);
            Drawable d = ((VectorDrawable) image).mutate();
            ((VectorDrawable) d).setBounds(0, 0, size, size);
            ((VectorDrawable) d).draw(tempCanvas);
            return new BitmapDrawable(resources, bmResult);
        }
        return image;
    }

    public static Drawable colorize(Resources resources, int color,
            Drawable image) {
        // remove any alpha
        color = color & ~0xff000000;
        color = color | 0xff000000;

        if (image instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) image).getBitmap();
            BitmapDrawable b1 = new BitmapDrawable(resources, b);
            b1.setColorFilter(color, Mode.SRC_ATOP);
            return b1;
        } else if (image instanceof VectorDrawable) {
            Drawable d = ((VectorDrawable) image).mutate();
            ((VectorDrawable)d).setColorFilter(color, Mode.SRC_ATOP);
            return d;
        }
        return image;
    }

    public static Drawable shadow(Resources resources, Drawable image) {
        // convert to bitmap first
        if (image instanceof VectorDrawable) {
            return image;
            /*Bitmap bmResult = Bitmap.createBitmap(size, size,
                    Bitmap.Config.ARGB_8888);
            Canvas tempCanvas = new Canvas(bmResult);
            Drawable d = ((VectorDrawable) image).mutate();
            ((VectorDrawable) d).setBounds(0, 0, size, size);
            ((VectorDrawable) d).draw(tempCanvas);
            image = new BitmapDrawable(resources, bmResult);*/
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

        Canvas c = new Canvas(bmResult);
        c.drawBitmap(b2, 0, 0, null);
        c.drawBitmap(b, -offsetXY[0], -offsetXY[1], null);

        return new BitmapDrawable(resources, bmResult);
    }

    public static Drawable getDefaultActivityIcon(Context context) {
        return context.getResources().getDrawable(R.drawable.ic_default);
    }

    public static BitmapDrawable compose(Resources resources, Drawable icon, Context context, Drawable iconBack,
            Drawable iconMask, Drawable iconUpon, float scale, int size) {
        // convert to bitmap first
        if (icon instanceof VectorDrawable) {
            Bitmap bmResult = Bitmap.createBitmap(size, size,
                    Bitmap.Config.ARGB_8888);
            Canvas tempCanvas = new Canvas(bmResult);
            Drawable d = ((VectorDrawable) icon).mutate();
            ((VectorDrawable) d).setBounds(0, 0, size, size);
            ((VectorDrawable) d).draw(tempCanvas);
            icon = new BitmapDrawable(resources, bmResult);
        }

        Bitmap b = ((BitmapDrawable) icon).getBitmap();
        int width = b.getWidth();
        int height = b.getHeight();

        // TODO
        if (iconBack == null && iconMask == null && iconUpon == null){
            scale = 1.0f;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
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

    public static Drawable overlay(Resources resources, Drawable b,
            Drawable icon, int width, int height, int iconSize) {
        if (icon instanceof BitmapDrawable) {
            Bitmap bmp = Bitmap.createBitmap(width, height + 40,
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            b.setBounds(0, 40, width, height);
            b.draw(c);
            c.drawBitmap(((BitmapDrawable) icon).getBitmap(), 0, 0, null);
            return new BitmapDrawable(resources, bmp);
        } else if (icon instanceof VectorDrawable) {
            Bitmap bmp = Bitmap.createBitmap(width, height + 40,
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            b.setBounds(0, 40, width, height);
            b.draw(c);
            Drawable d = ((VectorDrawable) icon).mutate();
            ((VectorDrawable) d).setBounds(0, 0, iconSize, iconSize);
            ((VectorDrawable) d).draw(c);
            return new BitmapDrawable(resources, bmp);
        }
        return icon;
    }
}
