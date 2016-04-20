package com.example.flingswipe;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Scroller;


public class SwipeFlingCardListener implements View.OnTouchListener {

    private static final String TAG = SwipeFlingCardListener.class.getSimpleName();
    private static final int INVALID_POINTER_ID = -1;

    private final float objectX;
    private final float objectY;
    private final int objectH;
    private final int objectW;
    private final int parentWidth;
    private final FlingListener mFlingListener;
    private final Object dataObject;
    private final float halfWidth;
    private float BASE_ROTATION_DEGREES;

    private float aPosX;
    private float aPosY;
    private float aDownTouchX;
    private float aDownTouchY;
    private float mScrollPer;

    // The active pointer is the one currently moving our object.
    private int mActivePointerId = INVALID_POINTER_ID;
    private View frame = null;

    private final int TOUCH_ABOVE = 0;
    private final int TOUCH_BELOW = 1;
    private int touchPosition;
    private final Object obj = new Object();
    private boolean isAnimationRunning = false;
    private float MAX_COS = (float) Math.cos(Math.toRadians(45));
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mMinFlingVelocity, mMaxFlingVelocity;

    public SwipeFlingCardListener(View frame, Object itemAtPosition, FlingListener flingListener) {
        this(frame, itemAtPosition, 15f, flingListener);
    }

    public SwipeFlingCardListener(View frame, Object itemAtPosition, float rotation_degrees, FlingListener flingListener) {
        super();
        this.frame = frame;
        this.objectX = frame.getX();
        this.objectY = frame.getY();
        this.objectH = frame.getHeight();
        this.objectW = frame.getWidth();
        this.halfWidth = objectW / 2f;
        this.dataObject = itemAtPosition;
        this.parentWidth = ((ViewGroup) frame.getParent()).getWidth();
        this.BASE_ROTATION_DEGREES = rotation_degrees;
        this.mFlingListener = flingListener;

        this.mScroller = new Scroller(frame.getContext());
        ViewConfiguration config = ViewConfiguration.get(frame.getContext());
        mMinFlingVelocity = config.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = config.getScaledMaximumFlingVelocity();
        mVelocityTracker = VelocityTracker.obtain();
    }

    public boolean onTouch(View view, MotionEvent event) {

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

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
                    // Remember where we started
                    aDownTouchX = x;
                    aDownTouchY = y;
                    //to prevent an initial jump of the magnifier, aposX and aPosY must
                    //have the values from the magnifier frame
                    if (aPosX == 0) {
                        aPosX = frame.getX();
                    }
                    if (aPosY == 0) {
                        aPosY = frame.getY();
                    }

                    if (y < objectH / 2) {
                        touchPosition = TOUCH_ABOVE;
                    } else {
                        touchPosition = TOUCH_BELOW;
                    }
                }

                requestDisallowInterceptTouchEvent(view, true);
                break;

            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER_ID;
                resetCardViewOnStack();
                requestDisallowInterceptTouchEvent(view, false);
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
                final float dx = xMove - aDownTouchX;
                final float dy = yMove - aDownTouchY;

                // Move the frame
                aPosX += dx;
                aPosY += dy;



                // calculate the rotation degrees
                float distobjectX = aPosX - objectX;
                float rotation = BASE_ROTATION_DEGREES * 2.f * distobjectX / parentWidth;
                if (touchPosition == TOUCH_BELOW) {
                    rotation = -rotation;
                }

                frame.setX(aPosX);
                frame.setY(aPosY);
                frame.setRotation(rotation);
                mScrollPer = getScrollProgressPercent();
                mFlingListener.onScroll(mScrollPer);
                break;

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                requestDisallowInterceptTouchEvent(view, false);
                break;
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
            float zeroToOneValue = (aPosX + halfWidth - leftBorder()) / (rightBorder() - leftBorder());
            return zeroToOneValue * 2f - 1f;
        }
    }

    private boolean resetCardViewOnStack() {
        if (movedBeyondLeftBorder()) {
            // Left Swipe
            onSelected(true, getExitPoint(-objectW), 300, true);
            mFlingListener.onScroll(-1.0f);
        } else if (movedBeyondRightBorder()) {
            // Right Swipe
            onSelected(false, getExitPoint(parentWidth), 300, true);
            mFlingListener.onScroll(1.0f);
        } else {
            float abslMoveDistance = Math.abs(aPosX - objectX);
            aPosX = 0;
            aPosY = 0;
            aDownTouchX = 0;
            aDownTouchY = 0;
            frame.animate()
                    .setDuration(400)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .x(objectX)
                    .y(objectY)
                    .rotation(0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            startCardViewAnimator(mScrollPer, 0.f, 300);
                        }
                    });
            mFlingListener.onScroll(0.0f);
            if (abslMoveDistance < 4.0) {
                mFlingListener.onClick(dataObject);
            }
        }
        return false;
    }

    private boolean movedBeyondLeftBorder() {
        return aPosX + halfWidth < leftBorder();
    }

    private boolean movedBeyondRightBorder() {
        return aPosX + halfWidth > rightBorder();
    }

    public float leftBorder() {
        return parentWidth / 4.f;
    }

    public float rightBorder() {
        return 3 * parentWidth / 4.f;
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

    public void onSelected(final boolean isLeft, float exitY, final int duration, final boolean triggerByTouchMove) {
        if (isAnimationRunning) {
            return;
        }
        isAnimationRunning = true;
        float exitX;
        if (isLeft) {
            exitX = -objectW - getRotationWidthOffset();
        } else {
            exitX = parentWidth + getRotationWidthOffset();
        }

        final VelocityTracker tracker = mVelocityTracker;
        tracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
        float vx = tracker.getXVelocity();
        float vy = tracker.getYVelocity();
        if (false && triggerByTouchMove && Math.abs(vx) > mMinFlingVelocity) {
            mScroller.setFriction(0.009F);
            mScroller.startScroll((int) aPosX, (int) aPosY, (int) (exitX - aPosX), (int) (exitY - aPosY), 1800);
            //mScroller.fling((int) aPosX, (int) aPosY, (int) vx, (int) vy, 0, (int) exitX, 0, (int) exitY);
            //mScroller.setFinalX((int) exitX);
            //mScroller.setFinalY((int) exitY);
            frame.invalidate();
            /*Log.d("xxxx", (int) aPosX+";"+(int) aPosY+";"+0+";"+(int) exitX+";"+(int) exitY + ";"+mScroller.getFinalX()+";"+mScroller.getFinalY()+":"+mScroller.getDuration()
                +";vx:"+vx+";vy:"+vy);*/
        } else {
            this.frame.animate()
                    .setDuration(duration)
                    .setInterpolator(sInterpolator)
                    .x(exitX)
                    .y(exitY)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            if (!triggerByTouchMove) {
                                startCardViewAnimator(0.f, 1.f, duration);
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
                            if (isLeft) {
                                mFlingListener.onCardExited();
                                mFlingListener.leftExit(frame, dataObject, triggerByTouchMove);
                            } else {
                                mFlingListener.onCardExited();
                                mFlingListener.rightExit(frame, dataObject, triggerByTouchMove);
                            }
                            if (frame != null) {
                                frame.animate().setListener(null);
                            }
                            isAnimationRunning = false;
                        }

                    })
                    .rotation(getExitRotation(isLeft));
        }
    }

    /**
     * Starts a default left exit animation.
     */
    public void selectLeft() {
        if (!isAnimationRunning)
            onSelected(true, objectY, 300, false);
    }

    public void selectRight() {
        if (!isAnimationRunning)
            onSelected(false, objectY, 300, false);
    }

    private float getExitPoint(int exitXPoint) {
        float[] x = new float[2];
        x[0] = objectX;
        x[1] = aPosX;

        float[] y = new float[2];
        y[0] = objectY;
        y[1] = aPosY;

        LinearRegression regression = new LinearRegression(x, y);
        return (float) regression.slope() * exitXPoint + (float) regression.intercept();
    }

    private void startCardViewAnimator(float startValue, float endValue, int duration) {
        ValueAnimator anim = ValueAnimator.ofFloat(startValue, endValue);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mFlingListener != null) {
                    mFlingListener.onScroll((float) animation.getAnimatedValue());
                }
            }
        });
        anim.setDuration(duration);
        anim.start();
    }

    private float getExitRotation(boolean isLeft) {
        float rotation = BASE_ROTATION_DEGREES * 2.f * (parentWidth - objectX) / parentWidth;
        if (touchPosition == TOUCH_BELOW) {
            rotation = -rotation;
        }
        if (isLeft) {
            rotation = -rotation;
        }
        return rotation;
    }

    private float getRotationWidthOffset() {
        return objectW / MAX_COS - objectW;
    }

    public void setRotationDegrees(float degrees) {
        this.BASE_ROTATION_DEGREES = degrees;
    }

    public boolean isTouching() {
        return this.mActivePointerId != INVALID_POINTER_ID;
    }

    public PointF getLastPoint() {
        return new PointF(this.aPosX, this.aPosY);
    }

    protected interface FlingListener {
        void onCardExited();

        void leftExit(View view, Object dataObject, boolean triggerByTouchMove);

        void rightExit(View view, Object dataObject, boolean triggerByTouchMove);

        void onClick(Object dataObject);

        void onScroll(float scrollProgressPercent);
    }

    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            /*t -= 1.0f;
            return t * t * t + 1.0f;*/
            //return (float)(1.0f - Math.pow((1.0f - t), 2));
            return t;
        }
    };

    static class ViscousFluidInterpolator implements Interpolator {
        /** Controls the viscous fluid effect (how much of it). */
        private static final float VISCOUS_FLUID_SCALE = 8.0f;

        private static final float VISCOUS_FLUID_NORMALIZE;
        private static final float VISCOUS_FLUID_OFFSET;

        static {

            // must be set to 1.0 (used in viscousFluid())
            VISCOUS_FLUID_NORMALIZE = 1.0f / viscousFluid(1.0f);
            // account for very small floating-point error
            VISCOUS_FLUID_OFFSET = 1.0f - VISCOUS_FLUID_NORMALIZE * viscousFluid(1.0f);
        }

        private static float viscousFluid(float x) {
            x *= VISCOUS_FLUID_SCALE;
            if (x < 1.0f) {
                x -= (1.0f - (float)Math.exp(-x));
            } else {
                float start = 0.36787944117f;   // 1/e == exp(-1)
                x = 1.0f - (float)Math.exp(1.0f - x);
                x = start + x * (1.0f - start);
            }
            return x;
        }

        @Override
        public float getInterpolation(float input) {
            final float interpolated = VISCOUS_FLUID_NORMALIZE * viscousFluid(input);
            if (interpolated > 0) {
                return interpolated + VISCOUS_FLUID_OFFSET;
            }
            return interpolated;
        }
    }

}





