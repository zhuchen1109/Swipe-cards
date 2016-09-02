package com.zc.swiple;

import android.util.Log;
import android.view.View;

import static com.zc.swiple.SwipeFlingView.DEBUG;

/**
 * 拖拽事件回调处理
 *
 * @zc
 */
class SwipeFlingDragCallBack extends ViewDragHelper.Callback {

    private final static String TAG = SwipeFlingDragCallBack.class.getSimpleName();
    private SwipeFlingView mDragView;

    SwipeFlingDragCallBack(SwipeFlingView view) {
        this.mDragView = view;
    }

    @Override
    public void onViewPositionChanged(View changedView, int left, int top,
                                      int dx, int dy) {
        mDragView.onViewPositionChanged(changedView, left, top, dx, dy);
    }

    @Override
    public boolean tryCaptureView(View child, int pointerId) {
        if (DEBUG) {
            Log.d(TAG, "tryCaptureView");
        }
        return mDragView.tryCaptureView(child, pointerId);
    }

    @Override
    public void onViewCaptured(View capturedChild, int activePointerId) {
        if (DEBUG) {
            Log.d(TAG, "onViewCaptured");
        }
        mDragView.onViewCaptured(capturedChild, activePointerId);
    }

    @Override
    public void onViewDragStateChanged(int state) {
        if (DEBUG) {
            Log.d(TAG, "onViewDragStateChanged state:" + state);
        }
        super.onViewDragStateChanged(state);
    }


    @Override
    public int getViewHorizontalDragRange(View child) {
        // 这个用来控制拖拽过程中松手后，自动滑行的速度
        if (DEBUG) {
            //Log.d(TAG, "getViewHorizontalDragRange");
        }
        return 0;
    }

    @Override
    public int getViewVerticalDragRange(View child) {
        return super.getViewVerticalDragRange(child);
    }

    @Override
    public void onViewReleased(View releasedChild, float xvel, float yvel) {
        if (DEBUG) {
            Log.d(TAG, "onViewReleased");
        }
        mDragView.onViewReleased(releasedChild, xvel, yvel);
    }

    @Override
    public int clampViewPositionHorizontal(View child, int left, int dx) {
        return left;
    }

    @Override
    public int clampViewPositionVertical(View child, int top, int dy) {
        return top;
    }
}
