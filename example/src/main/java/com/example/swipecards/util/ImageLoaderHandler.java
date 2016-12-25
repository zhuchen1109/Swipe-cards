package com.example.swipecards.util;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.Target;

import java.util.WeakHashMap;

/**
 * 图片加载帮助类
 * 统一在此处理加载图片,避免多处修改加载规则容易导致不一致
 *
 * @zc
 */
public class ImageLoaderHandler {

    private static ImageLoaderHandler sInstance;

    private WeakHashMap<String, Target> mPreloadQueue = new WeakHashMap<>();

    private ImageLoaderHandler() {
    }

    public static ImageLoaderHandler get() {
        if (sInstance == null) {
            sInstance = new ImageLoaderHandler();
        }
        return sInstance;
    }

    public void loadCardImage(Activity activity, ImageView iv, final View loadingView, final String url, final boolean isShowLoadWhenStarted) {
        Glide.with(activity)
                .load(url)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(new ImageViewTarget<GlideDrawable>(iv) {

                    @Override
                    public void onLoadStarted(Drawable placeholder) {
                        super.onLoadStarted(placeholder);
                        if (isShowLoadWhenStarted && loadingView != null) {
                            loadingView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                        if (loadingView != null) {
                            loadingView.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                        super.onLoadCleared(placeholder);
                    }

                    @Override
                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                        super.onResourceReady(resource, glideAnimation);
                        if (loadingView != null) {
                            loadingView.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    protected void setResource(GlideDrawable resource) {
                        getView().setImageDrawable(resource);
                    }
                });
    }

}
