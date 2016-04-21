package com.example.flingswipe;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class SwipeFlingDetailLayut extends ScrollView {

    private View mFirstView;
    private View mSecondView;

    public SwipeFlingDetailLayut(Context context) {
        this(context, null);
    }

    public SwipeFlingDetailLayut(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {

    }

    public void setFirstAndSecondeView(View firstView, View secondView) {
        mFirstView = firstView;
        mSecondView = secondView;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

    }

    public interface ISwipeFlingDetail {

    }

}
