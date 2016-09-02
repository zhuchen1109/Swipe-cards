package com.example.swipecards.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * swipe fling view跟手滑动时，like/unlike的view
 *
 * @zc
 */
public class SwipeIndicatorView extends ImageView {

    private float mCurPercent = -100.f;//当前设置的alpha值

    public SwipeIndicatorView(Context context) {
        this(context, null);
    }

    public SwipeIndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

    }

    public void setProgressPercent(float scrollProgressPercent) {
        if (mCurPercent == scrollProgressPercent) {
            return;
        }
        this.mCurPercent = scrollProgressPercent;
        if (getVisibility() != View.VISIBLE) {
            setVisibility(View.VISIBLE);
        }

        /*int newImgRes = scrollProgressPercent < 0 ? mUnLikeRes : mLikeRes;
        if (newImgRes != mCurImgRes) {
            mCurImgRes = newImgRes;
            setImageResource(newImgRes);
        }*/

        float absPer = Math.abs(scrollProgressPercent);
        int alpha = (int) (0xFF * absPer);
        setImageViewAlpha(this, alpha);
    }

    public void reset() {
        setProgressPercent(0.f);
        setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected boolean onSetAlpha(int alpha) {
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void setImageViewAlpha(ImageView view, int alpha) {
        if (Build.VERSION.SDK_INT >= 16) {
            view.setImageAlpha(alpha);
        } else {
            view.setAlpha(alpha);
        }
    }
}
