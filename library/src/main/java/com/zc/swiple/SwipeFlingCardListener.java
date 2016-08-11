package com.zc.swiple;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.PointF;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import java.util.ArrayList;

/**
 * 侧滑事件辅助类
 *
 * @zc
 */
public class SwipeFlingCardListener implements View.OnTouchListener {

    private final static String TAG = SwipeFlingCardListener.class.getSimpleName();
    private final static boolean DEBUG = true;
    private final static int TOUCH_ABOVE = 0;
    private final static int TOUCH_BELOW = 1;
    private final static int STATE_NONE = -1;
    private final static int STATE_START_DOWN = 0;
    private final static int STATE_MOVE = 1;
    private final static int INVALID_POINTER_ID = -1;

    private final float mHalfWidth;
    private final float mOriginX;
    private final float mOriginY;
    private float mPosX;
    private float mPosY;
    private float mDownTouchX;
    private float mDownTouchY;
    private float mScrollPer;
    private float BASE_ROTATION_DEGREES;
    private float MAX_COS = (float) Math.cos(Math.toRadians(45));

    private final int mFrameHeight;
    private final int mFrameWidth;
    private final int mFrameParentWidth;
    private int mMinFlingVelocity, mMaxFlingVelocity, mMinTouchSlop, mTapTimeout, mFlingDistance;
    private float mMinFlingTouchSlop;//对事件采样的最小间隔 用于处理fling
    private int mTouchPosition;
    private int mActivePointerId = INVALID_POINTER_ID;// The active pointer is the one currently moving our object.
    private int mCurState = STATE_NONE;

    private boolean isAnimationRunning = false;
    private long mCurDownTime;

    private final FlingListener mFlingListener;
    private final Object dataObject;
    private View frame = null;
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;
    private ArrayList<Float> mTouchPosList = new ArrayList<>();

    public SwipeFlingCardListener(View frame, Object itemAtPosition, FlingListener flingListener) {
        this(frame, itemAtPosition, 15f, flingListener);
    }

    public SwipeFlingCardListener(View frame, Object itemAtPosition, float rotation_degrees, FlingListener flingListener) {
        super();
        this.frame = frame;
        this.mOriginX = frame.getX();
        this.mOriginY = frame.getY();
        this.mFrameHeight = frame.getHeight();
        this.mFrameWidth = frame.getWidth();
        this.mHalfWidth = mFrameWidth / 2f;
        this.dataObject = itemAtPosition;
        this.mFrameParentWidth = ((ViewGroup) frame.getParent()).getWidth();
        this.BASE_ROTATION_DEGREES = rotation_degrees;
        this.mFlingListener = flingListener;

        this.mScroller = new Scroller(frame.getContext());
        ViewConfiguration config = ViewConfiguration.get(frame.getContext());
        float density = frame.getResources().getDisplayMetrics().density;
        mMinFlingVelocity = /*(int) (100 * Env.DENSITY);*/config.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = config.getScaledMaximumFlingVelocity();
        mFlingDistance = (int) (density * 25 + .5f);
        mMinTouchSlop = config.getScaledTouchSlop();
        mMinFlingTouchSlop = density * 3.3f;
        mTapTimeout = ViewConfiguration.getTapTimeout();
        mVelocityTracker = VelocityTracker.obtain();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        if (action != MotionEvent.ACTION_MOVE && mCurState != STATE_MOVE) {
            mVelocityTracker.addMovement(event);
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mCurDownTime = SystemClock.currentThreadTimeMillis();
                // from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Save the ID of this pointer

                mActivePointerId = event.getPointerId(0);
                float x = 0;
                float y = 0;
                boolean success = false;
                try {
                    x = event.getX(mActivePointerId);
                    y = event.getY(mActivePointerId);
                    success = true;
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Exception in onTouch(view, event) : " + mActivePointerId, e);
                }
                if (success) {
                    mFlingListener.onStart();
                    mCurState = STATE_START_DOWN;
                    // Remember where we started
                    mDownTouchX = x;
                    mDownTouchY = y;
                    //to prevent an initial jump of the magnifier, aposX and mPosY must
                    //have the values from the magnifier frame
                    if (mPosX == 0) {
                        mPosX = frame.getX();
                    }
                    if (mPosY == 0) {
                        mPosY = frame.getY();
                    }

                    if (y < mFrameHeight / 2) {
                        mTouchPosition = TOUCH_ABOVE;
                    } else {
                        mTouchPosition = TOUCH_BELOW;
                    }
                    mTouchPosList.clear();
                }
                requestDisallowInterceptTouchEvent(view, true);
                break;

            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER_ID;
                if (mCurState == STATE_MOVE) {
                    mFlingListener.onEndDragCard();
                    resetCardViewOnStack();
                } else if (mCurState == STATE_START_DOWN) {
                    float absMoveX = Math.abs(mPosX - mOriginX);
                    float absMoveY = Math.abs(mPosY - mOriginY);
                    mPosX = 0;
                    mPosY = 0;
                    mDownTouchX = 0;
                    mDownTouchY = 0;
                    if (absMoveX < mMinTouchSlop && absMoveY < mMinTouchSlop
                            && (SystemClock.currentThreadTimeMillis() - mCurDownTime) < mTapTimeout) {
                        mFlingListener.onClick(dataObject);
                    }
                    mFlingListener.onEnd();
                }
                requestDisallowInterceptTouchEvent(view, false);
                cancel();
                mCurState = STATE_NONE;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // Extract the index of the pointer that left the touch sensor
                final int pointerIndex = (event.getAction() &
                        MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = event.getPointerId(newPointerIndex);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // Find the index of the active pointer and fetch its position
                final int pointerIndexMove = event.findPointerIndex(mActivePointerId);
                final float xMove = event.getX(pointerIndexMove);
                final float yMove = event.getY(pointerIndexMove);
                //from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Calculate the distance moved
                final float dx = xMove - mDownTouchX;
                final float dy = yMove - mDownTouchY;
                if (mCurState == STATE_START_DOWN) {
                    if (Math.abs(dx) > mMinTouchSlop || Math.abs(dy) > mMinTouchSlop) {
                        mCurState = STATE_MOVE;
                        mFlingListener.onStartDragCard();
                        mDownTouchX = xMove;
                        mDownTouchY = yMove;
                    }
                } else if (mCurState == STATE_MOVE) {
                    // Move the frame
                    mPosX += dx;
                    mPosY += dy;
                    // calculate the rotation degrees
                    float distobjectX = mPosX - mOriginX;
                    float distobjectY = mPosY - mOriginY;
                    float rotation = BASE_ROTATION_DEGREES * 2.f * distobjectX / mFrameParentWidth;
                    if (mTouchPosition == TOUCH_BELOW) {
                        rotation = -rotation;
                    }
                    frame.setX(mPosX);
                    frame.setY(mPosY);
                    frame.setRotation(rotation);
                    mScrollPer = getScrollProgressPercent();
                    mFlingListener.onScroll(mScrollPer, true);

                    addTouchPos(distobjectX);
                    event.offsetLocation(distobjectX, distobjectY);
                    mVelocityTracker.addMovement(event);

                    if (DEBUG) {
                        /*mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                        float vx = mVelocityTracker.getXVelocity();
                        float vy = mVelocityTracker.getYVelocity();
                        boolean isFling = Math.abs(vx) > mMinFlingVelocity;
                        Log.d("xxxx", "isFling:move " + "vx:" + vx + ";dx:" + dx + ";xMove:" + xMove +
                                ",newXMove:" + (event.getX(pointerIndexMove)) + ";distobjectX:" + distobjectX +
                                ",distobjectY:" + distobjectY);*/
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                requestDisallowInterceptTouchEvent(view, false);
                cancel();
                mFlingListener.onEnd();
                break;
            }
        }
        return true;
    }

    private void addTouchPos(float posDx) {
        float lastPosDx = mTouchPosList.size() > 0 ? mTouchPosList.get(mTouchPosList.size() - 1) : 0.f;
        if (Math.abs(posDx - lastPosDx) > mMinFlingTouchSlop) {
            mTouchPosList.add(posDx);
        } else {
            if (DEBUG) {
                Log.d(TAG, "ignore add posDx,posDx=" + posDx + ";lastPosDx=" + lastPosDx);
            }
        }
    }

    /**
     * 若true:一次完整手势 触摸的事件点方向都一致
     *
     * @return
     */
    private boolean isTouchPosAgreement() {
        if (mTouchPosList.size() < 2) {
            return false;
        }
        boolean larger = false;
        for (int i = 1; i < mTouchPosList.size(); i++) {
            float first = mTouchPosList.get(i - 1);
            float second = mTouchPosList.get(i);
            boolean isBigger = second > first;
            if (i == 1) {
                larger = isBigger;
            } else {
                if (larger != isBigger) {
                    return false;
                }
            }
        }
        return true;
    }

    private void requestDisallowInterceptTouchEvent(View view, boolean isDisallow) {
        if (view.getParent() == null) {
            return;
        }
        view.getParent().requestDisallowInterceptTouchEvent(isDisallow);
    }

    private float getScrollProgressPercent() {
        if (movedBeyondLeftBorder()) {
            return -1f;
        } else if (movedBeyondRightBorder()) {
            return 1f;
        } else {
            float zeroToOneValue = (mPosX + mHalfWidth - leftBorder()) / (rightBorder() - leftBorder());
            return zeroToOneValue * 2f - 1f;
        }
    }

    private boolean resetCardViewOnStack() {
        final VelocityTracker tracker = mVelocityTracker;
        tracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
        float vx = tracker.getXVelocity();
        //float vy = tracker.getYVelocity();
        boolean isFling = Math.abs(vx) > mMinFlingVelocity;
        boolean isTouchPosAgreement = isTouchPosAgreement();
        if (DEBUG) {
            Log.d(TAG, "isFling:" + isFling + ";vx=" + vx + ";isTouchPosAgreement:" + isTouchPosAgreement);
        }

        if (canMovedLeft(vx, isFling, isTouchPosAgreement) && mFlingListener.canLeftExit()) {
            // Left Swipe
            onSelected(true, getExitPoint(-mFrameWidth), 300, true, true);
            mFlingListener.onScroll(-1.0f, true);
        } else if (canMovedRight(vx, isFling, isTouchPosAgreement) && mFlingListener.canRightExit()) {
            // Right Swipe
            onSelected(false, getExitPoint(mFrameParentWidth), 300, true, true);
            mFlingListener.onScroll(1.0f, true);
        } else {
            mPosX = 0;
            mPosY = 0;
            mDownTouchX = 0;
            mDownTouchY = 0;
            final int duration = 400;
            frame.animate()
                    .setDuration(duration)
                    .setInterpolator(sInterpolator)
                    .x(mOriginX)
                    .y(mOriginY)
                    .rotation(0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            startCardViewAnimator(mScrollPer, 0.f, (int) (duration * 0.8), true);
                        }
                    });
            mFlingListener.onEnd();
            //mFlingListener.onScroll(0.0f, true);
        }
        return false;
    }

    private boolean canMovedLeft(float velocityX, boolean isFling, boolean isTouchPosAgreement) {
        return canFling(true, velocityX, isFling, isTouchPosAgreement) || movedBeyondLeftBorder();
    }

    private boolean canMovedRight(float velocityX, boolean isFling, boolean isTouchPosAgreement) {
        return canFling(false, velocityX, isFling, isTouchPosAgreement) || movedBeyondRightBorder();
    }

    private boolean canFling(boolean fromLeft, float velocityX, boolean isFling, boolean isTouchPosAgreement) {
        if (!isFling) {
            return false;
        }
        if (isTouchPosAgreement) {
            if (mTouchPosList.size() >= 2) {
                float firstPos = mTouchPosList.get(mTouchPosList.size() - 2);
                float secondPos = mTouchPosList.get(mTouchPosList.size() - 1);
                if (secondPos == firstPos && mTouchPosList.size() >= 3) {
                    firstPos = mTouchPosList.get(mTouchPosList.size() - 3);
                }
                float dPos = secondPos - firstPos;
                if (fromLeft) {
                    return dPos < 0;
                } else {
                    return dPos > 0;
                }
            }
        }
        return false;
    }

    private boolean movedBeyondLeftBorder() {
        return mPosX + mHalfWidth < leftBorder();
    }

    private boolean movedBeyondRightBorder() {
        return mPosX + mHalfWidth > rightBorder();
    }

    public float leftBorder() {
        return mFrameParentWidth / 4.f;
    }

    public float rightBorder() {
        return 3 * mFrameParentWidth / 4.f;
    }

    public void computeScroll() {
        boolean b = mScroller.computeScrollOffset();
        if (b) {
            int curX = mScroller.getCurrX();
            int curY = mScroller.getCurrY();
            frame.setX(curX);
            frame.setY(curY);
            frame.postInvalidate();
        }
    }

    public void onSelected(final boolean isLeft, float exitY, final int duration, final boolean triggerByTouchMove, final boolean isCallbackForOnScroll) {
        this.onSelected(isLeft, false, exitY, duration, triggerByTouchMove, isCallbackForOnScroll);
    }

    /**
     * @param isLeft
     * @param exitY
     * @param duration
     * @param triggerByTouchMove
     * @param isCallbackForOnScroll 配合triggerByTouchMove=true时使用，若isCallbackForOnScroll=true，那么会在动画时SwipeFlingView对外回调函数onScroll，反之不会
     */
    public void onSelected(final boolean isLeft, final boolean isSuperLike, float exitY, final int duration, final boolean triggerByTouchMove, final boolean isCallbackForOnScroll) {
        if (isAnimationRunning) {
            return;
        }
        isAnimationRunning = true;
        float exitX;
        if (isLeft) {
            exitX = -mFrameWidth - getRotationWidthOffset();
        } else {
            exitX = mFrameParentWidth + getRotationWidthOffset();
        }

        //final VelocityTracker tracker = mVelocityTracker;
        //tracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
        float vx = 0;
        float vy = 0;//tracker.getYVelocity();
        if (false && triggerByTouchMove && Math.abs(vx) > mMinFlingVelocity) {
            //mScroller.setFriction(0.009F);
            mScroller.startScroll((int) mPosX, (int) mPosY, (int) (exitX - mPosX), (int) (exitY - mPosY), 1800);
            //mScroller.fling((int) mPosX, (int) mPosY, (int) vx, (int) vy, 0, (int) exitX, 0, (int) exitY);
            //mScroller.setFinalX((int) exitX);
            //mScroller.setFinalY((int) exitY);
            frame.invalidate();
            /*Log.d("xxxx", (int) mPosX+";"+(int) mPosY+";"+0+";"+(int) exitX+";"+(int) exitY + ";"+mScroller.getFinalX()+";"+mScroller.getFinalY()+":"+mScroller.getDuration()
                +";vx:"+vx+";vy:"+vy);*/
        } else {
            mFlingListener.onPreCardExited();
            this.frame.animate()
                    .setDuration(duration)
                    .setInterpolator(null)
                    .x(exitX)
                    .y(exitY)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            if (!triggerByTouchMove) {
                                startCardViewAnimator(0.f, isLeft ? -1.f : 1.f, duration, isCallbackForOnScroll);
                            }
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            onEnd(animation);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            onEnd(animation);
                        }

                        private void onEnd(Animator animator) {
                            mFlingListener.onCardExited();
                            if (!isSuperLike) {
                                if (isLeft) {
                                    mFlingListener.leftExit(frame, dataObject, triggerByTouchMove);
                                } else {
                                    mFlingListener.rightExit(frame, dataObject, triggerByTouchMove);
                                }
                            } else {
                                mFlingListener.onSuperLike(frame, dataObject, triggerByTouchMove);
                            }
                            if (frame != null) {
                                frame.animate().setListener(null);
                            }
                            isAnimationRunning = false;
                            mFlingListener.onEnd();
                        }

                    })
                    .rotation(getExitRotation(isLeft));
        }
    }

    /**
     * Starts a default left exit animation.
     */
    public void selectLeft(boolean isCallbackForOnScroll) {
        if (!isAnimationRunning)
            onSelected(true, mOriginY, 300, false, isCallbackForOnScroll);
    }

    public void selectRight(boolean isCallbackForOnScroll) {
        if (!isAnimationRunning)
            onSelected(false, mOriginY, 300, false, isCallbackForOnScroll);
    }

    public void selectSuperLike(boolean isCallbackForOnScroll) {
        if (!isAnimationRunning)
            onSelected(false, true, mOriginY, 300, false, isCallbackForOnScroll);
    }

    private float getExitPoint(int exitXPoint) {
        float[] x = new float[2];
        x[0] = mOriginX;
        x[1] = mPosX;

        float[] y = new float[2];
        y[0] = mOriginY;
        y[1] = mPosY;

        LinearRegression regression = new LinearRegression(x, y);
        return (float) regression.slope() * exitXPoint + (float) regression.intercept();
    }

    private void startCardViewAnimator(float startValue, float endValue, int duration, final boolean isCallbackForOnScroll) {
        ValueAnimator anim = ValueAnimator.ofFloat(startValue, endValue);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mFlingListener != null) {
                    mFlingListener.onScroll((float) animation.getAnimatedValue(), isCallbackForOnScroll);
                }
            }
        });
        anim.setDuration(duration);
        anim.start();
    }

    private float getExitRotation(boolean isLeft) {
        float rotation = BASE_ROTATION_DEGREES * 2.f * (mFrameParentWidth - mOriginX) / mFrameParentWidth;
        if (mTouchPosition == TOUCH_BELOW) {
            rotation = -rotation;
        }
        if (isLeft) {
            rotation = -rotation;
        }
        return rotation;
    }

    private float getRotationWidthOffset() {
        return mFrameWidth / MAX_COS - mFrameWidth;
    }

    public void setRotationDegrees(float degrees) {
        this.BASE_ROTATION_DEGREES = degrees;
    }

    public boolean isTouching() {
        return this.mActivePointerId != INVALID_POINTER_ID;
    }

    public PointF getLastPoint() {
        return new PointF(this.mPosX, this.mPosY);
    }

    public boolean isAnimationRunning() {
        return isAnimationRunning;
    }

    public void recycle() {
        cancel();
    }

    private void cancel() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    protected interface FlingListener {

        void onStart();

        void onStartDragCard();

        void onPreCardExited();

        void onCardExited();

        boolean canLeftExit();

        void leftExit(View view, Object dataObject, boolean triggerByTouchMove);

        boolean canRightExit();

        void rightExit(View view, Object dataObject, boolean triggerByTouchMove);

        void onSuperLike(View view, Object dataObject, boolean triggerByTouchMove);

        void onClick(Object dataObject);

        void onScroll(float scrollProgressPercent, boolean isCallbackForOnScroll);

        void onEndDragCard();

        void onEnd();
    }

    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

}





