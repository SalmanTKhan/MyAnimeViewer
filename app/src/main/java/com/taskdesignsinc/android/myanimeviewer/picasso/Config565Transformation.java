package com.taskdesignsinc.android.myanimeviewer.picasso;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.squareup.picasso.Transformation;

public class Config565Transformation implements Transformation {

    @Override
    public Bitmap transform(Bitmap source) {
        Bitmap resultBitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(source, 0, 0, paint);
        source.recycle();
        return resultBitmap;
    }

    @Override
    public String key() {
        return Config565Transformation.class.getSimpleName();
    }
}