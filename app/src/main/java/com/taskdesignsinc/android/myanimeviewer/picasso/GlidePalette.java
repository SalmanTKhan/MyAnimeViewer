package com.taskdesignsinc.android.myanimeviewer.picasso;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

public class GlidePalette extends BitmapPalette {

    protected GlidePalette() {
    }

    public GlidePalette use(@Profile int paletteProfile) {
        super.use(paletteProfile);
        return this;
    }

    public GlidePalette intoBackground(View view) {
        return this.intoBackground(view, Swatch.RGB);
    }

    @Override
    public GlidePalette intoBackground(View view, @Swatch int paletteSwatch) {
        super.intoBackground(view, paletteSwatch);
        return this;
    }

    public GlidePalette intoTextColor(TextView textView) {
        return this.intoTextColor(textView, Swatch.TITLE_TEXT_COLOR);
    }

    @Override
    public GlidePalette intoTextColor(TextView textView, @Swatch int paletteSwatch) {
        super.intoTextColor(textView, paletteSwatch);
        return this;
    }

    @Override
    public GlidePalette crossfade(boolean crossfade) {
        super.crossfade(crossfade);
        return this;
    }

    @Override
    public GlidePalette crossfade(boolean crossfade, int crossfadeSpeed) {
        super.crossfade(crossfade, crossfadeSpeed);
        return this;
    }

    @Override
    public GlidePalette intoCallBack(GlidePalette.CallBack callBack) {
        super.intoCallBack(callBack);
        return this;
    }

    @Override
    public GlidePalette setPaletteBuilderInterceptor(PaletteBuilderInterceptor interceptor) {
        super.setPaletteBuilderInterceptor(interceptor);
        return this;
    }

    @Override
    public GlidePalette skipPaletteCache(boolean skipCache) {
        super.skipPaletteCache(skipCache);
        return this;
    }

    public boolean onResourceReady(Bitmap b) {
        if (b != null) {
            start(b);
            return true;
        }

        return false;
    }

    public interface BitmapHolder {
        @Nullable
        Bitmap getBitmap();
    }

}