package com.taskdesignsinc.android.myanimeviewer.util;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.UrlConnectionDownloader;
import com.taskdesignsinc.android.myanimeviewer.adapter.AnimeRecyclerAdapter;
import com.taskdesignsinc.android.myanimeviewer.picasso.PaletteTransformation;
import com.taskdesignsinc.android.thememanager.ThemeManager;

public class ImageLoaderManager {

    private static final String mTag = ImageLoaderManager.class.getSimpleName();

    private static ImageLoaderManager mInstance;
    private Context mContext;

    private Handler mHandler;

    private SharedPreferences mPrefs;

    private int mTryCount = 0;
    private String mLastURL = "";

    private boolean mBlurryImageMessage = false;
    private Picasso mPicasso;

    public static ImageLoaderManager getInstance(Context ctx) {
        /**
         * use the application context as suggested by CommonsWare. this will
         * ensure that you dont accidentally leak an Activitys context (see this
         * article for more information:
         * http://developer.android.com/resources/articles
         * /avoiding-memory-leaks.html)
         */
        if (mInstance == null) {
            mInstance = new ImageLoaderManager(ctx.getApplicationContext());
        }
        return mInstance;
    }

    public static ImageLoaderManager getInstance() {
        return mInstance;
    }

    private ImageLoaderManager(Context pContext) {
        mContext = pContext;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mHandler = new Handler();
        initPicasso();
    }

    private void initPicasso() {
        mPicasso = new Picasso.Builder(mContext)
                .downloader(new UrlConnectionDownloader(mContext))
                .loggingEnabled(WriteLog.IsEnabled())
                .build();
    }

    public void loadImage(String url, ImageView imageView) {
        loadImage(url, imageView, null, null);
    }

    public void loadImage(String url, ImageView imageView, @DrawableRes int placeHolderRes, PaletteTransformation.PaletteCallback callback) {
        if (TextUtils.isEmpty(url)) {
            WriteLog.appendLog(mTag, "loadImage invalid url supplied " + url);
            return;
        } else if (imageView == null) {
            WriteLog.appendLog(mTag, "loadImage invalid ImageView " + url);
            return;
        }
        url = url.replace(" ", "%20");
        if (callback != null) {
            Picasso.with(mContext)
                    .load(url)
                    .transform(PaletteTransformation.instance())
                    .placeholder(placeHolderRes)
                    .error(android.R.drawable.ic_delete)
                    .into(imageView, callback);
        } else {
            Picasso.with(mContext)
                    .load(url)
                    .transform(PaletteTransformation.instance())
                    .placeholder(placeHolderRes)
                    .error(android.R.drawable.ic_delete)
                    .into(imageView);
        }
    }

    public void loadImage(final String url, final ImageView imageView, final PaletteTransformation.PaletteCallback callback, final AnimeRecyclerAdapter.ViewHolder viewHolder) {
        if (TextUtils.isEmpty(url)) {
            WriteLog.appendLog(mTag + ": loadImage invalid url supplied " + url);
            return;
        } else if (imageView == null) {
            WriteLog.appendLog(mTag + ": loadImage invalid ImageView " + url);
            return;
        }
        int loaderType = Integer.parseInt(mPrefs.getString(Constants.KEY_LOADER_TYPE, "0"));
        if (loaderType == 0) {
            Glide.with(mContext)
                    .load(url)
                    .asBitmap()
                    .into(new BitmapImageViewTarget(imageView) {
                        @Override
                        public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                            super.onResourceReady(bitmap, anim);
                            if (viewHolder != null) {
                                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(Palette palette) {
                                        int colorTemp = ThemeManager.getInstance().getBackgroundColor(mContext);
                                        if (viewHolder.mTitleView != null && palette != null) {
                                            viewHolder.mTitleView.setTextColor(ThemeManager.getInstance().getTextColor());
                                            if (palette.getVibrantSwatch() != null)
                                                colorTemp = palette.getVibrantSwatch().getRgb();
                                            else if (palette.getDarkMutedSwatch() != null)
                                                colorTemp = palette.getDarkMutedSwatch().getRgb();
                                        }
                                        Integer colorFrom = ThemeManager.getInstance().getBackgroundColor(mContext);
                                        Integer colorTo = colorTemp;
                                        viewHolder.mPaletteColor = colorTo;
                                        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                                        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                                            @Override
                                            public void onAnimationUpdate(ValueAnimator animator) {
                                                viewHolder.mTitleView.setBackgroundColor((Integer) animator.getAnimatedValue());
                                            }

                                        });
                                        colorAnimation.start();
                                    }
                                });
                            }
                        }
                    });
        } else {
            ViewTreeObserver vto = imageView.getViewTreeObserver();
            if (vto == null || !vto.isAlive()) return;
            vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                    final int height = imageView.getMeasuredHeight();
                    final int width = imageView.getMeasuredWidth();

                    // get the target image view width and height
                    final int heightDp = (int) DisplayUtils.dpFromPx(mContext, height);
                    final int widthDp = (int) DisplayUtils.dpFromPx(mContext, width);

                    if (mPrefs.getBoolean(Constants.KEY_LOADER_IMAGE_QUALITY_HIGH, false)) {
                        // download resized image
                        mPicasso
                                .load(url)
                                .resize(widthDp, heightDp)
                                .transform(PaletteTransformation.instance())
                                .error(android.R.drawable.ic_delete)
                                .into(imageView, callback);
                    } else {
                        // download resized image
                        mPicasso
                                .load(url)
                                .resize(widthDp, heightDp)
                                .config(Bitmap.Config.RGB_565)
                                .transform(PaletteTransformation.instance())
                                .error(android.R.drawable.ic_delete)
                                .into(imageView, callback);
                    }
                    return true;
                }
            });
        }
    }

    @Deprecated
    public void scrollListener(AbsListView gridView) {
    }

    @Deprecated
    public void onDestroy() {
    }
}
