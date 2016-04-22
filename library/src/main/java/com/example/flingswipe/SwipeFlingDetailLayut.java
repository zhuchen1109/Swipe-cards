package com.example.flingswipe;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ScrollView;

public class SwipeFlingDetailLayut extends ScrollView {

    private View mFirstView;
    private View mSecondView;
    private float mDensity, mScreenHeight;

    public SwipeFlingDetailLayut(Context context) {
        this(context, null);
    }

    public SwipeFlingDetailLayut(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mDensity = dm.density;
        mScreenHeight = dm.heightPixels;
    }

    public void setFirstAndSecondeView(View firstView, View secondView) {
        mFirstView = firstView;
        mSecondView = secondView;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

    }

    public void showDetailLayout(View startPosView) {
        int[] location = new int[2];
        startPosView.getLocationInWindow(location);
        int startWidth = startPosView.getWidth();
        int startHeight = startPosView.getHeight();
        float startX = location[0];
        float startY = location[1];

        mFirstView.getLocationInWindow(location);
        int endWidth = mFirstView.getWidth();
        int endHeight = mFirstView.getHeight();
        float endX = location[0];
        float endY = location[1];

        startShowDetailLayoutAnima(mFirstView, startWidth, startHeight, startX, startY, mSecondView, endWidth, endHeight, endX, endY);
    }

    public void dismissDetailLayout(View startPosView) {
        int[] location = new int[2];
        startPosView.getLocationInWindow(location);
        int startWidth = startPosView.getWidth();
        int startHeight = startPosView.getHeight();
        float startX = location[0];
        float startY = location[1];

        mFirstView.getLocationInWindow(location);
        int endWidth = mFirstView.getWidth();
        int endHeight = mFirstView.getHeight();
        float endX = location[0];
        float endY = location[1];
        startDismissDetailLayoutAnima(mFirstView, startWidth, startHeight, startX, startY, mSecondView, endWidth, endHeight, endX, endY);
    }

    private void startShowDetailLayoutAnima(final View startView, int startWidth, int startHeight, float startX, float startY, final View endView, int endWidth, int endHeight, float endX, float endY) {
        final float scaleX = (startWidth * 1.f / endWidth * 1.f);
        final float scaleY = (startHeight * 1.f / endHeight * 1.f);
        final float dx = startX + startWidth * .5f - (endX + endWidth * .5f);
        final float dy = startY + startHeight * .5f - (endY + endHeight * .5f);

        startView.setScaleX(scaleX);
        startView.setScaleY(scaleY);
        startView.setTranslationX(dx);
        startView.setTranslationY(dy);

        ValueAnimator viewPagerAnima = ValueAnimator.ofFloat(0.f, 1.f);
        viewPagerAnima.setInterpolator(sInterpolator);
        viewPagerAnima.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float frac = animation.getAnimatedFraction();
                startView.setScaleX(scaleX + (1.f - scaleX) * frac);
                startView.setScaleY(scaleY + (1.f - scaleY) * frac);
                startView.setTranslationX(dx * (1.f - frac));
                startView.setTranslationY(dy * (1.f - frac));
            }
        });
        viewPagerAnima.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }
        });
        viewPagerAnima.setDuration(500);

        endView.setTranslationY(mScreenHeight - endY - endHeight);
        endView.animate()
                .setInterpolator(sInterpolator)
                .setDuration(500)
                .translationY(0);

        viewPagerAnima.start();
        /*Log.d("xxxx", "startWidth:"+startWidth+"startHeight:"+startHeight+";startX:"+startX+";startY:"+startY+";endWidth:"+endWidth+";endHeight:"+endHeight+";endX:"+endX+";endY:"+endY
                +";scaleX:"+scaleX+";scaleY:"+scaleY+";dx:"+dx+";dy:"+dy+";"+(mScreenHeight - endY - endHeight));*/
    }

    private void startDismissDetailLayoutAnima(final View startView, int startWidth, int startHeight, float startX, float startY, final View endView, int endWidth, int endHeight, float endX, float endY) {
        final float scaleX = (startWidth * 1.f / endWidth * 1.f);
        final float scaleY = (startHeight * 1.f / endHeight * 1.f);
        final float dx = startX + startWidth * .5f - (endX + endWidth * .5f);
        final float dy = startY + startHeight * .5f - (endY + endHeight * .5f);

        ValueAnimator viewPagerAnima = ValueAnimator.ofFloat(0.f, 1.f);
        viewPagerAnima.setInterpolator(sInterpolator);
        viewPagerAnima.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float frac = 1 - animation.getAnimatedFraction();
                startView.setScaleX(scaleX + (1.f - scaleX) * frac);
                startView.setScaleY(scaleY + (1.f - scaleY) * frac);
                startView.setTranslationX(dx * (1.f - frac));
                startView.setTranslationY(dy * (1.f - frac));
            }
        });
        viewPagerAnima.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {
                onEnd(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                onEnd(animation);
            }

            private void onEnd(Animator animation) {
                setVisibility(View.INVISIBLE);
                startView.setScaleX(1.f);
                startView.setScaleY(1.f);
                startView.setTranslationX(0.f);
                startView.setTranslationY(0.f);
            }
        });
        viewPagerAnima.setDuration(500);

        endView.animate()
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(300)
                .translationY(mScreenHeight - endY - endHeight);

        viewPagerAnima.start();
    }

    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    public interface ISwipeFlingDetail {

    }

}
