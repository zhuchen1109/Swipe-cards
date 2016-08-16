package com.zc.swiple;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.PointF;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.FrameLayout;

import com.lorentzos.flingswipe.R;

import java.util.ArrayList;

/**
 * 支持侧滑的叠加组件
 * @zc
 */
public class SwipeFlingViewNew extends AdapterView {

    protected final static boolean DEBUG = true;
    static final String TAG = SwipeFlingViewNew.class.getSimpleName();

    private static final int MIN_FLING_VELOCITY = 300; // dips
    private float MAX_COS = (float) Math.cos(Math.toRadians(45));
    private int MAX_VISIBLE = 4;
    private int MIN_ADAPTER_STACK = 3;
    private float ROTATION_DEGREES = 15.f;
    private float SCALE_STEP = 0.1f;
    private float[] CHILD_SCALE_BY_INDEX;
    private float[] CHILD_VERTICAL_OFFSET_BY_INDEX;
    private float mCardVerticalOffset = -1;
    private float mCardHeight = -1;
    private RecycleBin mRecycleBin;
    private int mCurPositon;
    private int mPositonByEmptyData;//命中卡片集合为空时 记录此时的position

    private int heightMeasureSpec;
    private int widthMeasureSpec;
    private Adapter mAdapter;
    private int LAST_OBJECT_IN_STACK = 0;
    private onSwipeListener mFlingListener;
    private AdapterDataSetObserver mDataSetObserver;
    private boolean mInLayout = false;
    private View mActiveCard = null;
    private OnItemClickListener mOnItemClickListener;
    private SwipeFlingCardListener flingCardListener;
    private PointF mLastTouchPoint;
    private boolean hasCardTouched = false;//因为现在设计的问题 在卡片划走 下一张卡片绑定事件有空挡期 会导致事件被父容器捕获

    private ViewDragHelper mViewDragHelper;

    private int mOriginTopViewX = 0, mOriginTopViewY = 0; //视图初始位置
    private int mCardWidth;
    private float mCardHalfWidth;
    private int mMinFlingVelocity, mMaxFlingVelocity, mMinTouchSlop, mTapTimeout;
    private ArrayList<View> mReleasedViewList = new ArrayList<>();

    public SwipeFlingViewNew(Context context) {
        this(context, null);
    }

    public SwipeFlingViewNew(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.SwipeFlingStyle);
    }

    public SwipeFlingViewNew(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwipeFlingView, defStyle, 0);
        MAX_VISIBLE = a.getInt(R.styleable.SwipeFlingView_max_visible, MAX_VISIBLE);
        MIN_ADAPTER_STACK = a.getInt(R.styleable.SwipeFlingView_min_adapter_stack, MIN_ADAPTER_STACK);
        ROTATION_DEGREES = a.getFloat(R.styleable.SwipeFlingView_rotation_degrees, ROTATION_DEGREES);
        a.recycle();

        init(context);
    }

    public void init(final Context context) {
        mViewDragHelper = ViewDragHelper.create(this, 10, new SwipeFlingDragCallBack(this));

        float density = context.getResources().getDisplayMetrics().density;
        mCardVerticalOffset = 6f * density;

        mRecycleBin = new RecycleBin();
        CHILD_SCALE_BY_INDEX = new float[MAX_VISIBLE];
        CHILD_VERTICAL_OFFSET_BY_INDEX = new float[MAX_VISIBLE];
        int index = 0;
        float tempScale = 1.f;
        float verticalOffset = 0;
        while (index < MAX_VISIBLE) {
            if (index != 0 && index != MAX_VISIBLE -1) {
                tempScale -= SCALE_STEP;
                verticalOffset += mCardVerticalOffset;
            }
            CHILD_SCALE_BY_INDEX[MAX_VISIBLE - 1 - index] = tempScale;
            CHILD_VERTICAL_OFFSET_BY_INDEX[MAX_VISIBLE - 1 - index] = verticalOffset;
            ++index;
        }
        if (DEBUG) {
            String log = "CHILD_SCALE_BY_INDEX:";
            for (float f : CHILD_SCALE_BY_INDEX) {
                log += f + ">>";
            }
            String offset = "CHILD_VERTICAL_OFFSET_BY_INDEX:";
            for (float f : CHILD_VERTICAL_OFFSET_BY_INDEX) {
                offset += f + ">>";
            }
            log(log + ";mCardVerticalOffset:" + mCardVerticalOffset + offset);
        }

        ViewConfiguration config = ViewConfiguration.get(getContext());
        mMinFlingVelocity = (int) (MIN_FLING_VELOCITY * density);
        mMaxFlingVelocity = config.getScaledMaximumFlingVelocity();
        mMinTouchSlop = config.getScaledTouchSlop();
        mTapTimeout = ViewConfiguration.getTapTimeout();
    }

    @Override
    public View getSelectedView() {
        return converChildView(mActiveCard);
    }

    public View getOriginSelectedView() {
        return mActiveCard;
    }

    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.widthMeasureSpec = widthMeasureSpec;
        this.heightMeasureSpec = heightMeasureSpec;
    }

    public int getWidthMeasureSpec() {
        return widthMeasureSpec;
    }

    public int getHeightMeasureSpec() {
        return heightMeasureSpec;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mAdapter == null) {
            return;
        }
        mInLayout = true;
        final int adapterCount = mAdapter.getCount();

        log("onLayout hasVaildBin:" + mRecycleBin.hasVaildBin() + ";mCurPositon:" + mCurPositon + ";adapterCount:" + adapterCount);
        if (adapterCount == 0 || mCurPositon >= adapterCount) {
            removeAllViewsInLayout();
        } else {
            if (mRecycleBin.hasVaildBin()) {
                int startingIndex = 0;
                if (adapterCount > MAX_VISIBLE) {
                    int curChildCount = getChildCount();
                    while (startingIndex < Math.min(adapterCount, MAX_VISIBLE)) {
                        //log("curChildCount:"+curChildCount);
                        //计算要复用view的index
                        int recycleActiveIndex = curChildCount >= MAX_VISIBLE -1 ?
                                startingIndex : MAX_VISIBLE - 1 - (curChildCount + startingIndex);
                        if (recycleActiveIndex > -1 && mRecycleBin.getActiveView(recycleActiveIndex) == null) {
                            int position = mCurPositon + startingIndex + curChildCount;
                            if (position >= adapterCount) {
                                break;
                            }
                            //log("RecycleBin startingIndex:" + startingIndex + ";recycleActiveIndex:" + recycleActiveIndex);
                            View recycleView = mRecycleBin.getAndResetRecycleView();
                            View newUnderChild = mAdapter.getView(position, converChildView(recycleView), this);
                            newUnderChild = makeAndAddView(0, newUnderChild);
                            mRecycleBin.addActiveView(newUnderChild, recycleActiveIndex);
                            scaleChildView(newUnderChild, recycleActiveIndex);
                        }
                        startingIndex++;
                    }
                }
                /*removeAllViewsInLayout();
                layoutChildren(0, adapterCount);*/
                LAST_OBJECT_IN_STACK = getChildCount() - 1;
                View topCard = getChildAt(LAST_OBJECT_IN_STACK);
                if (mActiveCard != null && topCard != null && topCard == mActiveCard) {
                    //nothing
                } else {
                    setTopView();
                }
            } else {
                View topCard = getChildAt(LAST_OBJECT_IN_STACK);
                if (mActiveCard != null && topCard != null && topCard == mActiveCard) {
                    if (this.flingCardListener.isTouching()) {
                        PointF lastPoint = this.flingCardListener.getLastPoint();
                        if (this.mLastTouchPoint == null || !this.mLastTouchPoint.equals(lastPoint)) {
                            this.mLastTouchPoint = lastPoint;
                            removeViewsInLayout(0, LAST_OBJECT_IN_STACK);
                            layoutChildren(1, adapterCount);
                        }
                    }
                } else {
                    removeAllViewsInLayout();
                    layoutChildren(0, adapterCount);
                    setTopView();
                }
            }
        }

        mInLayout = false;

        //-----
        /*View topCard = getChildAt(LAST_OBJECT_IN_STACK);
        if (topCard != null) {
            mOriginTopViewX = topCard.getLeft();
            mOriginTopViewY = topCard.getTop();
            mCardWidth = topCard.getWidth();
            mCardHalfWidth = mCardWidth * .5f;
            Log.d("xxxx", "mOriginTopViewX:"+mOriginTopViewX+";mOriginTopViewY:"+mOriginTopViewY);
        }*/
    }

    private void layoutChildren(int startingIndex, int adapterCount) {
        while (startingIndex < Math.min(adapterCount, MAX_VISIBLE)) {
            layoutChild(startingIndex, null);
            startingIndex++;
        }
    }

    private void layoutChild(int startingIndex, View convertView) {
        View newUnderChild = mAdapter.getView(mCurPositon + startingIndex, convertView, this);
        if (newUnderChild.getVisibility() != GONE) {
            newUnderChild = makeAndAddView(startingIndex, newUnderChild);
            mRecycleBin.addActiveView(newUnderChild, MAX_VISIBLE - 1 - startingIndex);
            LAST_OBJECT_IN_STACK = startingIndex;
            scaleChildView(newUnderChild, MAX_VISIBLE - 1 - startingIndex);
        }
    }

    private void computeCardHeight() {
        if (mCardHeight > 0) return;
        mCardHeight = getChildAt(0).getHeight();

    }

    boolean enableScale = true;

    private void scaleChildView(View newUnderChild, int startingIndex) {
        if (!enableScale) return;
        if (startingIndex >= MAX_VISIBLE || startingIndex < 0) return;
        computeCardHeight();
        float originHeight = newUnderChild.getHeight();
        float scale = CHILD_SCALE_BY_INDEX[startingIndex];
        float newHeight = originHeight * scale;
        newUnderChild.setScaleX(scale);
        newUnderChild.setScaleY(scale);
        newUnderChild.setTranslationY((originHeight - newHeight) * .5f + CHILD_VERTICAL_OFFSET_BY_INDEX[startingIndex]);
    }

    private void updateChildrenOffset(boolean isOffsetUp, float scrollProgressPercent) {
        if (!enableScale) return;
        float absPer = Math.abs(scrollProgressPercent);
        int maxIndex = Math.min(getChildCount() - 1, MAX_VISIBLE - 1);
        int curIndex = maxIndex;
        boolean isLessChild = curIndex < MAX_VISIBLE - 1;
        --curIndex;
        View childView = null;
        while ((isLessChild && curIndex >= 0) || curIndex >= 1) {
            childView = getChildAt(curIndex);
            int tempIndex = isLessChild ? (MAX_VISIBLE - 1 - (maxIndex - curIndex)) : curIndex;
            float curScale = CHILD_SCALE_BY_INDEX[isOffsetUp ? tempIndex : tempIndex + 1];
            float targetScale = CHILD_SCALE_BY_INDEX[isOffsetUp ? tempIndex + 1 : tempIndex];
            float curHeight = mCardHeight * curScale;
            float targetHeight = mCardHeight * targetScale;
            float scaleOfPer = curScale + (targetScale - curScale) * absPer;
            float transY = (mCardHeight - curHeight) * .5f + CHILD_VERTICAL_OFFSET_BY_INDEX[tempIndex] -
                    ((targetHeight - curHeight) * .5f + mCardVerticalOffset) * absPer;
            if (!isOffsetUp) {
                transY = (mCardHeight - curHeight) * .5f + CHILD_VERTICAL_OFFSET_BY_INDEX[tempIndex + 1] +
                        ((curHeight - targetHeight) * .5f + mCardVerticalOffset) * absPer;
            }
            childView.setTranslationY(transY);
            childView.setScaleX(scaleOfPer);
            childView.setScaleY(scaleOfPer);
            /*log(curIndex + ";" + converChildView(childView).getTag() + ";curScale:" + curScale
                    + ";targetScale:" + targetScale + ";curHeight:" + curHeight + ";targetHeight:"
                    + targetHeight + ";scaleOfPer:" + scaleOfPer + ";absPer:" + absPer
                    + ";Y:" + transY);*/
            --curIndex;
        }
    }

    private View makeAndAddView(int pos, View view) {
        return makeAndAddView(pos, view, false);
    }

    private View makeAndAddView(int pos, View view, boolean isAddFirstCard) {
        SwipeChildContainer child = null;
        SwipeLayoutParame lp = null;
        if (view.getParent() != null && view.getParent() instanceof SwipeChildContainer) {
            child = (SwipeChildContainer) view.getParent();
            lp = (SwipeLayoutParame) child.getLayoutParams();
        } else if (view instanceof SwipeChildContainer) {
            child = (SwipeChildContainer) view;
            lp = (SwipeLayoutParame) view.getLayoutParams();
        } else {
            SwipeLayoutParame viewLp = (SwipeLayoutParame) view.getLayoutParams();
            //TODO 理论这个不应该为空 先屏蔽掉 看下测试结果
            /*if (viewLp == null) {
                viewLp = new SwipeLayoutParame(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            }*/
            child = new SwipeChildContainer(getContext(), null);
            lp = new SwipeLayoutParame(viewLp);
            child.addView(view, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }
        child.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        addViewInLayout(child, isAddFirstCard ? -1 : 0, lp, true);
        lp.position = pos;

        final boolean needToMeasure = child.isLayoutRequested();
        if (needToMeasure) {
            int childWidthSpec = getChildMeasureSpec(getWidthMeasureSpec(),
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                    lp.width);
            int childHeightSpec = getChildMeasureSpec(getHeightMeasureSpec(),
                    getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
                    lp.height);
            child.measure(childWidthSpec, childHeightSpec);
        } else {
            cleanupLayoutState(child);
        }


        int w = child.getMeasuredWidth();
        int h = child.getMeasuredHeight();

        int gravity = lp.gravity;
        if (gravity == -1) {
            gravity = Gravity.TOP | Gravity.START;
        }

        int layoutDirection = ViewCompat.getLayoutDirection(this);
        final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
        final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

        int childLeft;
        int childTop;
        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                if (lp.width == LayoutParams.MATCH_PARENT) {
                    w = getWidth() - (lp.leftMargin + lp.rightMargin);
                    childLeft = getPaddingLeft() + lp.leftMargin;
                } else {
                    childLeft = (getWidth() + getPaddingLeft() - getPaddingRight() - w) / 2 +
                            lp.leftMargin - lp.rightMargin;
                }
                break;
            case Gravity.END:
                childLeft = getWidth() + getPaddingRight() - w - lp.rightMargin;
                break;
            case Gravity.START:
            default:
                childLeft = getPaddingLeft() + lp.leftMargin;
                break;
        }
        switch (verticalGravity) {
            case Gravity.CENTER_VERTICAL:
                childTop = (getHeight() + getPaddingTop() - getPaddingBottom() - h) / 2 +
                        lp.topMargin - lp.bottomMargin;
                break;
            case Gravity.BOTTOM:
                childTop = getHeight() - getPaddingBottom() - h - lp.bottomMargin;
                break;
            case Gravity.TOP:
            default:
                childTop = getPaddingTop() + lp.topMargin;
                break;
        }
        child.layout(childLeft, childTop, childLeft + w, childTop + h);
        return child;
    }

    private void setTopView() {
        if (getChildCount() > 0) {
            View cardView = getChildAt(LAST_OBJECT_IN_STACK);
            mActiveCard = cardView;
        }

        if (true)return;
        if (getChildCount() > 0) {
            View cardView = getChildAt(LAST_OBJECT_IN_STACK);
            if (cardView != null && cardView != mActiveCard) {
                mActiveCard = cardView;
                //TODO 快速向左短距离划，再向右滑时，这个card view的tranX/tranY值前面矫正了，但在这又被恢复了，原因未知
                resetChildView(mActiveCard);
                flingCardListener = new SwipeFlingCardListener(mActiveCard, mAdapter.getItem(0),
                        ROTATION_DEGREES, new SwipeFlingCardListener.FlingListener() {

                    @Override
                    public void onStart() {
                        mFlingListener.onStart();
                    }

                    @Override
                    public void onStartDragCard() {
                        mFlingListener.onStartDragCard();
                    }

                    @Override
                    public void onPreCardExited() {
                        mFlingListener.onPreCardExit();
                        hasCardTouched = false;
                    }

                    @Override
                    public void onCardExited() {
                        View activeCard = mActiveCard;
                        if (activeCard == null) {
                            return;
                        } else {
                            mCurPositon += 1;
                            activeCard.setOnTouchListener(null);
                            mRecycleBin.removeActiveView(activeCard);
                            removeViewInLayout(activeCard);
                            mActiveCard = null;
                        }
                        requestLayout();
                    }

                    @Override
                    public boolean canLeftExit() {
                        return mFlingListener.canLeftCardExit();
                    }

                    @Override
                    public void leftExit(View view, Object dataObject, boolean triggerByTouchMove) {
                        mFlingListener.onLeftCardExit(converChildView(view), dataObject, triggerByTouchMove);
                    }

                    @Override
                    public boolean canRightExit() {
                        return mFlingListener.canRightCardExit();
                    }

                    @Override
                    public void onSuperLike(View view, Object dataObject, boolean triggerByTouchMove) {
                        mFlingListener.onSuperLike(view, dataObject, triggerByTouchMove);
                    }

                    @Override
                    public void rightExit(View view, Object dataObject, boolean triggerByTouchMove) {
                        mFlingListener.onRightCardExit(converChildView(view), dataObject, triggerByTouchMove);
                    }

                    @Override
                    public void onClick(Object dataObject) {
                        if (mOnItemClickListener != null)
                            mOnItemClickListener.onItemClicked(0, dataObject);

                    }

                    @Override
                    public void onScroll(float scrollProgressPercent, boolean isCallbackForOnScroll) {
                        if (isCallbackForOnScroll) {
                            mFlingListener.onScroll(getSelectedView(), scrollProgressPercent);
                        }
                        updateChildrenOffset(true, scrollProgressPercent);
                    }

                    @Override
                    public void onEndDragCard() {
                        mFlingListener.onEndDragCard();
                    }

                    @Override
                    public void onEnd() {
                        mFlingListener.onEnd();
                    }
                });
                hasCardTouched = true;
                //mActiveCard.setOnTouchListener(flingCardListener);
                mFlingListener.onTopCardViewFinish();
            } else {
                mActiveCard = cardView;
                log("mActiveCard=null LAST_OBJECT_IN_STACK=" + LAST_OBJECT_IN_STACK);
            }
        }
    }

    private View converChildView(View childView) {
        if (childView == null) return null;
        if (childView instanceof SwipeChildContainer) {
            return ((ViewGroup) childView).getChildAt(0);
        }
        return childView;
    }

    public SwipeFlingCardListener getTopCardListener() {
        return flingCardListener;
    }

    public void selectLeft() {
        selectLeft(true);
    }

    public void selectRight() {
        selectRight(true);
    }

    public void selectRight(boolean isCallbackForOnScroll) {
        if (flingCardListener != null) {
            flingCardListener.selectRight(isCallbackForOnScroll);
        }
    }

    public void selectLeft(boolean isCallbackForOnScroll) {
        if (flingCardListener != null) {
            flingCardListener.selectLeft(isCallbackForOnScroll);
        }
    }

    public void selectSuperLike(boolean isCallbackForOnScroll) {
        if (flingCardListener != null) {
            flingCardListener.selectSuperLike(isCallbackForOnScroll);
        }
    }

    /**
     * 返回上一个已经划过去的卡片
     * @param fromLeft true:从左边返回 反之右边返回
     */
    public void selectComeBackCard(boolean fromLeft) {
        if (isFirstCard()) {
            return;
        }
        View activeCard = mActiveCard;
        if (activeCard != null) {
            mCurPositon -= 1;
            View lastView = mRecycleBin.getLastActiveView();
            if (lastView != null) {
                activeCard.setOnTouchListener(null);
                removeViewInLayout(lastView);
            }
            View newChild = mAdapter.getView(mCurPositon, converChildView(lastView), this);
            //newChild.setVisibility(View.INVISIBLE);
            newChild = addViewOfComebackCard(0, newChild, true, fromLeft);
            mRecycleBin.removeAndAddLastActiveView(lastView, newChild);
            //setTopView();
            startComeBackCardAnim(newChild, fromLeft, 300);
            //requestLayout();
        }
    }

    private View addViewOfComebackCard(int pos, View view, boolean isAddFirstCard, boolean isLeft) {
        SwipeChildContainer child = null;
        SwipeLayoutParame lp = null;
        if (view.getParent() != null && view.getParent() instanceof SwipeChildContainer) {
            child = (SwipeChildContainer) view.getParent();
            lp = (SwipeLayoutParame) child.getLayoutParams();
        } else if (view instanceof SwipeChildContainer) {
            child = (SwipeChildContainer) view;
            lp = (SwipeLayoutParame) view.getLayoutParams();
        } else {
            SwipeLayoutParame viewLp = (SwipeLayoutParame) view.getLayoutParams();
            //TODO 理论这个不应该为空 先屏蔽掉 看下测试结果
            /*if (viewLp == null) {
                viewLp = new SwipeLayoutParame(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            }*/
            child = new SwipeChildContainer(getContext(), null);
            lp = new SwipeLayoutParame(viewLp);
            child.addView(view, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }
        child.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        addViewInLayout(child, isAddFirstCard ? -1 : 0, lp, true);
        lp.position = pos;

        final boolean needToMeasure = child.isLayoutRequested();
        if (needToMeasure) {
            int childWidthSpec = getChildMeasureSpec(getWidthMeasureSpec(),
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                    lp.width);
            int childHeightSpec = getChildMeasureSpec(getHeightMeasureSpec(),
                    getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
                    lp.height);
            child.measure(childWidthSpec, childHeightSpec);
        } else {
            cleanupLayoutState(child);
        }


        int w = child.getMeasuredWidth();
        int h = child.getMeasuredHeight();

        int gravity = lp.gravity;
        if (gravity == -1) {
            gravity = Gravity.TOP | Gravity.START;
        }

        int layoutDirection = ViewCompat.getLayoutDirection(this);
        final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
        final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

        int childLeft;
        int childTop;
        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                if (lp.width == LayoutParams.MATCH_PARENT) {
                    w = getWidth() - (lp.leftMargin + lp.rightMargin);
                    childLeft = getPaddingLeft() + lp.leftMargin;
                } else {
                    childLeft = (getWidth() + getPaddingLeft() - getPaddingRight() - w) / 2 +
                            lp.leftMargin - lp.rightMargin;
                }
                break;
            case Gravity.END:
                childLeft = getWidth() + getPaddingRight() - w - lp.rightMargin;
                break;
            case Gravity.START:
            default:
                childLeft = getPaddingLeft() + lp.leftMargin;
                break;
        }
        switch (verticalGravity) {
            case Gravity.CENTER_VERTICAL:
                childTop = (getHeight() + getPaddingTop() - getPaddingBottom() - h) / 2 +
                        lp.topMargin - lp.bottomMargin;
                break;
            case Gravity.BOTTOM:
                childTop = getHeight() - getPaddingBottom() - h - lp.bottomMargin;
                break;
            case Gravity.TOP:
            default:
                childTop = getPaddingTop() + lp.topMargin;
                break;
        }

        int widthOffset = (int) (w / MAX_COS - w);
        if (isLeft) {
            child.layout(-(w + widthOffset), childTop, -widthOffset, childTop + h);
        } else {
            child.layout(getWidth() + widthOffset, childTop, getWidth() + w + widthOffset, childTop + h);
        }
        return child;
    }

    private void startComeBackCardAnim(final View frame, final boolean fromLeft, int duration) {
        MarginLayoutParams lp = ((MarginLayoutParams) frame.getLayoutParams());
        final int childLeft = getPaddingLeft() + lp.leftMargin;
        final int childTop = getPaddingTop() + lp.topMargin;
        final float originX = childLeft;//frame.getX();
        float originY = frame.getY();
        float startY = frame.getY();
        float startX;
        if (fromLeft) {
            startX = -frame.getWidth() - getRotationWidthOffset(frame);
        } else {
            startX = getWidth() + getRotationWidthOffset(frame);
        }
        final float rotation = getEnterRotation(frame, fromLeft);

        //frame.setX(startX);
        //frame.setY(startY);
        frame.setRotation(rotation);

        final float x = frame.getX();
        final ValueAnimator anim = ValueAnimator.ofFloat(0.f, 1.f);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float frac =animation.getAnimatedFraction();
                frame.setX(x + (originX - x) * frac);
                frame.setRotation(rotation * (1 - frac));
                updateChildrenOffset(false, frac);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                //frame.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                frame.layout(childLeft, childTop, childLeft + frame.getWidth(), childTop + frame.getHeight());
                resetChildView(frame);
                //frame.requestLayout();
                setTopView();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
            }
        });
        anim.setDuration(duration);
        anim.start();
    }

    private float getEnterRotation(View frame, boolean fromLeft) {
        /*float rotation = ROTATION_DEGREES * 2.f * (getWidth() - frame.getX()) / getWidth();
        if (fromLeft) {
            rotation = -rotation;
        }*/
        float rotation = fromLeft ? -30.f : 30.f;
        return rotation;
    }

    private float getRotationWidthOffset(View frame) {
        return frame.getWidth() / MAX_COS - frame.getWidth();
    }

    public boolean isFirstCard() {
        return mCurPositon == 0;
    }

    public boolean isAnimationRunning() {
        if (flingCardListener != null) {
            return flingCardListener.isAnimationRunning();
        }
        return false;
    }

    public void setMaxVisible(int MAX_VISIBLE) {
        this.MAX_VISIBLE = MAX_VISIBLE;
    }

    public void setMinStackInAdapter(int minAdapterStack) {
        this.MIN_ADAPTER_STACK = minAdapterStack;
    }

    @Override
    public Adapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mDataSetObserver = null;
        }

        mAdapter = adapter;

        if (mAdapter != null && mDataSetObserver == null) {
            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }
    }

    public void setFlingListener(onSwipeListener onFlingListener) {
        this.mFlingListener = onFlingListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public final int getCurPositon() {
        return mCurPositon;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new SwipeLayoutParame(getContext(), attrs);
    }

    private void resetChildView(View childView) {
        if (childView == null) return;
        childView.setTranslationX(0.f);
        childView.setTranslationY(0.f);
        childView.setScaleX(1.f);
        childView.setScaleY(1.f);
        childView.setRotation(0);
        childView.animate().setListener(null);
    }

    @Override
    public void setSelection(int i) {
        throw new UnsupportedOperationException("Not supported");
    }

    private void resetData() {
        mCurPositon = 0;
        mRecycleBin.clearCache();
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            //TODO zc 这快可以细致处理，不过维护的逻辑比较复杂，以后再看
            resetData();
            removeAllViewsInLayout();
            requestLayout();
        }

    }

    private class RecycleBin {
        private ArrayList<View> mActiveViews;
        private View mRecycleView;

        RecycleBin() {
            mActiveViews = new ArrayList(MAX_VISIBLE);
            for (int i = 0; i < MAX_VISIBLE; i++) {
                mActiveViews.add(null);
            }
        }

        void addActiveView(View view, int pos) {
            //SwipeLayoutParame lp = (SwipeLayoutParame) view.getLayoutParams();
            //int pos = lp.position;
            if (DEBUG) {
                //log(view + ";pos:" + pos);
                //pritfViews("preadd");
            }
            View itemView = mActiveViews.set(pos, view);
            if (DEBUG) {
                pritfViews("postadd");
            }
        }

        void removeAndAddLastActiveView(View oldView, View newView) {
            for (int i = 0; i < mActiveViews.size(); i++) {
                if (oldView == mActiveViews.get(i)) {
                    mActiveViews.remove(i);
                    resetChildView(oldView);
                    pritfViews("removeAndAddLastActiveView remove");
                    break;
                }
            }

            if (newView != null) {
                mActiveViews.add(newView);
            }

            if (DEBUG) {
                pritfViews("removeAndAddLastActiveView add");
            }
        }

        void removeActiveView(View view) {
            if (view == null) return;
            for (int i = mActiveViews.size() -1; i >= 0; i--) {
                if (view == mActiveViews.get(i)) {
                    mActiveViews.remove(i);
                    mRecycleView = view;
                    resetChildView(mRecycleView);
                    mActiveViews.add(0, null);
                    if (DEBUG) {
                        Log.d(TAG, "removeActiveView:" + i + ";");
                    }
                    pritfViews("removeActiveView");
                    break;
                }
            }
        }

        View getActiveView(int pos) {
            return mActiveViews.get(pos);
        }

        View getLastActiveView() {
            if (mActiveViews.size() == MAX_VISIBLE) {
                return mActiveViews.get(0);
            }
            return null;
        }

        View getAndResetRecycleView() {
            View view = mRecycleView;
            mRecycleView = null;
            return view;
        }

        View getRecycleView() {
            return mRecycleView;
        }

        void resetRecycleView() {
            this.mRecycleView = null;
        }

        boolean hasVaildBin() {
            if (mAdapter == null || mActiveViews.size() == 0) {
                return false;
            }

            if (DEBUG) {
                //pritfViews("hasVaildBin");
            }

            int count = mActiveViews.size();
            int num = 0;
            for (int i = 0; i < count; i++) {
                if (mActiveViews.get(i) == null) {
                    ++num;
                }
            }
            if (num == count) {
                return false;
            }
            return true;
        }

        void clearCache() {
            for (int i = 0; i < MAX_VISIBLE; i++) {
                mActiveViews.set(i, null);
            }
            mRecycleView = null;
        }

        boolean isTopView(View view) {
            int index = mActiveViews.indexOf(view);
            if (index == mActiveViews.size() - 1) {
                return true;
            }
            return false;
        }

        public void pritfViews(String tag) {
            //test
            String ss = tag + " activeviews:";
            for (int i = 0; i < mActiveViews.size(); i++) {
                ss += (mActiveViews.get(i) != null ? converChildView(mActiveViews.get(i)).getTag() : null) + ">>";
            }
            log(ss);
        }

    }

    private class SwipeLayoutParame extends FrameLayout.LayoutParams {

        int position;

        public SwipeLayoutParame(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public SwipeLayoutParame(int width, int height) {
            super(width, height);
        }

        public SwipeLayoutParame(SwipeLayoutParame source) {
            super((MarginLayoutParams) source);
            this.gravity = source.gravity;
        }
    }

    public interface OnItemClickListener {
        void onItemClicked(int itemPosition, Object dataObject);
    }

    public interface onSwipeListener {

        void onStart();

        void onStartDragCard();

        void onPreCardExit();

        void onTopCardViewFinish();//top card 设置完毕调用

        boolean canLeftCardExit();

        boolean canRightCardExit();

        void onLeftCardExit(View view, Object dataObject, boolean triggerByTouchMove);

        void onRightCardExit(View view, Object dataObject, boolean triggerByTouchMove);

        void onSuperLike(View view, Object dataObject, boolean triggerByTouchMove);

        void onAdapterAboutToEmpty(int itemsInAdapter);

        void onAdapterEmpty();

        void onScroll(View selectedView, float scrollProgressPercent);

        void onEndDragCard();

        void onEnd();
    }

    private void log(String log) {
        if (DEBUG) {
            Log.d(TAG, log);
        }
    }

    public boolean hasNoEnoughCardSwipe() {
        return mCurPositon == mAdapter.getCount();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean should = mViewDragHelper.shouldInterceptTouchEvent(ev);
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            // ACTION_DOWN的时候就对view重新排序
            resetChildren();
            // 保存初次按下时arrowFlagView的Y坐标
            // action_down时就让mDragHelper开始工作，否则有时候导致异常
            //mViewDragHelper.processTouchEvent(ev);
        }
        return should;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mViewDragHelper.processTouchEvent(ev);
        return true;
    }

    private static class SwipeChildContainer extends FrameLayout {

        private float mDownX, mDownY;
        private float mTouchSlop;
        private SwipeFlingCardListener mOnTouchListener;

        public SwipeChildContainer(Context context, AttributeSet attrs) {
            super(context, attrs);
            ViewConfiguration conf = ViewConfiguration.get(context);
            mTouchSlop = conf.getScaledTouchSlop();
            setClipChildren(false);
        }

        @Override
        public void setOnTouchListener(OnTouchListener l) {
            super.setOnTouchListener(l);
            if (l == null && mOnTouchListener != null) {
                mOnTouchListener.recycle();
            }
            this.mOnTouchListener = (SwipeFlingCardListener) l;
        }

        @Override
        public void computeScroll() {
            if (mOnTouchListener != null) {
                mOnTouchListener.computeScroll();
            }
        }
    }

    public void onScroll(View changedView, boolean isOffsetUp) {
        int left = changedView.getLeft();
        int top = changedView.getTop();
        float scrollProgressPercent = getScrollProgressPercent(left, top);
        updateChildrenOffset(isOffsetUp, scrollProgressPercent);
    }

    private boolean movedBeyondLeftBorder(int left) {
        //Log.d("xxxx", "movedBeyondLeftBorder left:" + left + ";leftBorder:" + leftBorder()+";"+(left + mCardHalfWidth < leftBorder()));
        return left + mCardHalfWidth < leftBorder();
    }

    private boolean movedBeyondRightBorder(int left) {
        //Log.d("xxxx", "movedBeyondRightBorder left:" + left + ";rightBorder:" + rightBorder()+";"+(left + mCardHalfWidth > rightBorder()));
        return left + mCardHalfWidth > rightBorder();
    }

    protected float leftBorder() {
        return getWidth() / 4.f;
    }

    protected float rightBorder() {
        return 3.f * getWidth() / 4.f;
    }

    protected float computeScrollPercent(int left, int top) {
        float zeroToOneValue = (left + mCardHalfWidth - leftBorder()) / (rightBorder() - leftBorder());
        //Log.d("xxxx", "zeroToOneValue:"+zeroToOneValue);
        return (zeroToOneValue * 2.f - 1f);
    }

    private float getScrollProgressPercent(int left, int top) {
        if (movedBeyondLeftBorder(left)) {
            return -1f;
        } else if (movedBeyondRightBorder(left)) {
            return 1f;
        } else {
            return computeScrollPercent(left, top);
        }
    }

    protected void onViewPositionChanged(View changedView, int left, int top,
                                      int dx, int dy) {
        onScroll(changedView, true);
    }

    protected boolean tryCaptureView(View child, int pointerId) {
        Log.d(TAG, "tryCaptureView visibility:" + (child.getVisibility() == View.VISIBLE)
                + ";ScaleX:" + (child.getScaleX())
                + ";hasVaildBin:" + mRecycleBin.hasVaildBin()
                + ";isTopView:" + mRecycleBin.isTopView(child));
        if (child.getVisibility() != View.VISIBLE
                || child.getScaleX() <= 1.0f - SCALE_STEP
                || !mRecycleBin.hasVaildBin()
                || !mRecycleBin.isTopView(child)) {
            return false;
        }

        // 如果数据List为空，或者子View不可见，则不予处理
        /*if (child == bottomLayout || dataList == null || dataList.size() == 0
                || child.getVisibility() != View.VISIBLE || child.getScaleX() <= 1.0f - SCALE_STEP) {
            // 一般来讲，如果拖动的是第三层、或者第四层的View，则直接禁止
            // 此处用getScale的用法来巧妙回避
            return false;
        }

        if (btnLock) {
            return false;
        }

        // 只捕获顶部view(rotation=0)
        int childIndex = viewList.indexOf(child);
        if (childIndex > 0) {
            return false;
        }

        mCurCardView = child;
        ((CardItemView) child).onStartDragging();*/
        return true;
    }

    protected void onViewCaptured(View capturedChild, int activePointerId) {
        mOriginTopViewX = capturedChild.getLeft();
        mOriginTopViewY = capturedChild.getTop();
        mCardWidth = capturedChild.getWidth();
        mCardHalfWidth = mCardWidth * .5f;
        Log.d("xxxx", "mOriginTopViewX:"+mOriginTopViewX+";mOriginTopViewY:"+mOriginTopViewY);
    }

    protected void onViewReleased(View releasedChild, float xvel, float yvel) {
        //animToSide((CardItemView) releasedChild, xvel);
        Log.d("xxxx", "xvel:" + xvel+";mMinFlingVelocity:"+mMinFlingVelocity);
        int left = releasedChild.getLeft();
        if (xvel < -mMinFlingVelocity || movedBeyondLeftBorder(left)) {
            //fling left
            onSelected(releasedChild, true);
        } else if (xvel > mMinFlingVelocity || movedBeyondRightBorder(left)) {
            //fling right
            onSelected(releasedChild, false);
        } else {
            //reset postion
            resetReleasedChildPos(releasedChild, mOriginTopViewX, mOriginTopViewY);
        }
    }

    private void onSelected(View releasedChild, boolean isLeft) {
        final int width = getWidth();
        int halfHeight = getHeight() / 2;
        int dx = releasedChild.getLeft() - mOriginTopViewX;
        int dy = releasedChild.getTop() - mOriginTopViewY;
        if (dx == 0) {
            dx = 1;
        }
        int exitX = isLeft ? -width : width;
        int exitY = dy * width / Math.abs(dx) + mOriginTopViewY;

        if (exitY > halfHeight) {
            exitY = halfHeight;
        } else if (exitY < -halfHeight) {
            exitY = -halfHeight;
        }

        mReleasedViewList.add(releasedChild);
        if (mViewDragHelper.smoothSlideViewTo(releasedChild, exitX, exitY)) {
            computeScrollByFling();
        }
    }

    protected void computeScrollByFling() {
        if (mViewDragHelper.continueSettling(true)) {
            //Log.d("xxxx", "computeScrollByFling running");
            ViewCompat.postOnAnimation(this, new Runnable() {
                @Override
                public void run() {
                    computeScrollByFling();
                }
            });
        } else {
            Log.d("xxxx", "computeScrollByFlingover " + mViewDragHelper.getViewDragState());
            // 动画结束
            synchronized (this) {
                onCardExit();
            }
        }
    }

    /**
     * 卡片向二侧飞出动画完成时回调
     */
    private void onCardExit() {
        resetChildren();

        //无数据时 不回调
        final int adapterCount = mAdapter.getCount();
        if (adapterCount > 0 && ((adapterCount - mCurPositon) == MIN_ADAPTER_STACK)) {
            mFlingListener.onAdapterAboutToEmpty(adapterCount);
        }
        if (adapterCount > 0 && adapterCount == mCurPositon) {
            //mPositonByEmptyData = mCurPositon;
            mFlingListener.onAdapterEmpty();
        }
    }

    private void resetChildren() {
        if (mReleasedViewList.size() == 0) {
            return;
        }
        View activeCard = mReleasedViewList.remove(0);
        if (activeCard == null) {
            return;
        } else {
            mCurPositon += 1;
            mRecycleBin.removeActiveView(activeCard);
            removeViewInLayout(activeCard);
            mActiveCard = null;
        }
        requestLayout();
    }

    private void resetReleasedChildPos(final View releasedChild, int originX, int originY) {
        final int curX = releasedChild.getLeft() ;
        final int curY = releasedChild.getTop();
        final int dx = originX - curX;
        final int dy = originY - curY;
        ValueAnimator animator = ValueAnimator.ofFloat(0.f, 1.f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float frac = animation.getAnimatedFraction();
                releasedChild.offsetLeftAndRight((int) (curX + dx * frac - releasedChild.getLeft()));
                releasedChild.offsetTopAndBottom((int) (curY + dy * frac - releasedChild.getTop()));
                onScroll(releasedChild, true);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });
        animator.setDuration(300);
        animator.start();
    }

}

