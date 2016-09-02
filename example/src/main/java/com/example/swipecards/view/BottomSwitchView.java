package com.example.swipecards.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class BottomSwitchView extends ImageView {

    private final static float DRAG_MAX_SCALE = 1.1f;
    private final static float DRAG_MIN_SCALE = 1.f;
    private final static float DRAG_DEFAULT_SCALE = 1.f;
    private final static float DRAG_SCALE_RANGE = DRAG_MAX_SCALE - DRAG_MIN_SCALE;

    //private final static float CLICK_MAX_SCALE = 1.f;
    private final static float CLICK_MIN_SCALE = .8f;
    private final static float CLICK_DEFAULT_SCALE = 1.f;

    private ValueAnimator mDownAnimator, mUpAnimator, mTempAnimator;

    public BottomSwitchView(Context context) {
        this(context, null);
    }

    public BottomSwitchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomSwitchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
    }

    /**
     * 拖拽中
     * @param scrollProgressPercent
     */
    public void onDrag(float scrollProgressPercent) {
        float per = Math.abs(scrollProgressPercent);
        float scale = DRAG_MIN_SCALE + DRAG_SCALE_RANGE * per;
        setScale(scale);
    }

    /**
     * 拖拽结束
     */
    public void onDragEnded() {
        Animator anim = buildAnimator(getScaleX(), DRAG_DEFAULT_SCALE, 200);
        anim.start();
    }

    private void setScale(float frac) {
        setScaleX(frac);
        setScaleY(frac);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled() && isClickable()) {
            switch (event.getAction() & event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startDownAnimator(getScaleX(), CLICK_MIN_SCALE, 200);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    startUpAnimator(getScaleX(), CLICK_DEFAULT_SCALE, 150);
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    private void startDownAnimator(float startValue, float endValue, int duration) {
        mDownAnimator = buildAnimator(startValue, endValue, duration);
        mDownAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mTempAnimator != null && !mTempAnimator.isRunning()) {
                    mTempAnimator.setFloatValues(getScaleX(), CLICK_DEFAULT_SCALE);
                    mTempAnimator.start();
                    mTempAnimator = null;
                }
            }
        });
        mDownAnimator.start();
    }

    private void startUpAnimator(float startValue, float endValue, int duration) {
        if (mDownAnimator != null) {
            mUpAnimator = buildAnimator(startValue, endValue, duration);
            if (mDownAnimator.isRunning()) {
                mTempAnimator = mUpAnimator;
            } else {
                mUpAnimator.start();
            }
        }
    }

    private ValueAnimator buildAnimator(float startValue, float endValue, int duration) {
        ValueAnimator anim = ValueAnimator.ofFloat(startValue, endValue);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float frac = (float) animation.getAnimatedValue();
                setScale(frac);
            }
        });
        anim.setDuration(duration);
        return anim;
    }
}
