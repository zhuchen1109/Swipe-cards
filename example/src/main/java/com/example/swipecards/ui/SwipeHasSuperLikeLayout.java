package com.example.swipecards.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * 被superlike的卡片显示superlike的视图
 * @zc
 */
public class SwipeHasSuperLikeLayout extends FrameLayout {
    public SwipeHasSuperLikeLayout(Context context) {
        super(context);
    }

    public SwipeHasSuperLikeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void show() {
        if (getVisibility() == View.VISIBLE) {
            animate().alpha(1).setDuration(200);
        }
    }

    public void hide() {
        if (getVisibility() == View.VISIBLE) {
            animate().alpha(0).setDuration(100);
        }
    }
}
