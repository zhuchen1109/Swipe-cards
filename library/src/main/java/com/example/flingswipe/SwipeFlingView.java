package com.example.flingswipe;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.PointF;
import android.os.Build;
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

public class SwipeFlingView extends AdapterView {

    private final static boolean DEBUG = true;
    private static final String TAG = SwipeFlingView.class.getSimpleName();

    private int MAX_VISIBLE = 4;
    private int MIN_ADAPTER_STACK = 2;
    private float ROTATION_DEGREES = 15.f;
    private float SCALE = 0.93f;
    private float[] CHILD_SCALE_BY_INDEX;
    private float[] CHILD_VERTICAL_OFFSET_BY_INDEX;
    private float mCardVerticalOffset = -1;
    private float mCardHeight = -1;
    private RecycleBin mRecycleBin;
    private int mCurPositon;

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

    public SwipeFlingView(Context context) {
        this(context, null);
    }

    public SwipeFlingView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.SwipeFlingStyle);
    }

    public SwipeFlingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwipeFlingView, defStyle, 0);
        MAX_VISIBLE = a.getInt(R.styleable.SwipeFlingView_max_visible, MAX_VISIBLE);
        MIN_ADAPTER_STACK = a.getInt(R.styleable.SwipeFlingView_min_adapter_stack, MIN_ADAPTER_STACK);
        ROTATION_DEGREES = a.getFloat(R.styleable.SwipeFlingView_rotation_degrees, ROTATION_DEGREES);
        a.recycle();

        init(context);
    }

    public void init(final Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        mCardVerticalOffset = 6.7f * density;

        mRecycleBin = new RecycleBin();
        CHILD_SCALE_BY_INDEX = new float[MAX_VISIBLE];
        CHILD_VERTICAL_OFFSET_BY_INDEX = new float[MAX_VISIBLE];
        int index = 0;
        float tempScale = 1.f;
        float verticalOffset = 0;
        while (index < MAX_VISIBLE) {
            if (index == 0) {
                //
            } else if (index == MAX_VISIBLE - 1) {
                //
            } else {
                tempScale *= SCALE;
                verticalOffset += mCardVerticalOffset;
            }
            CHILD_SCALE_BY_INDEX[MAX_VISIBLE - 1 - index] = tempScale;
            CHILD_VERTICAL_OFFSET_BY_INDEX[MAX_VISIBLE - 1 - index] = verticalOffset;
            ++index;
        }
        //CHILD_SCALE_BY_INDEX = new float[] {0.25f, 0.25f, 0.5f, 1.f};
        if (DEBUG) {
            String log = "CHILD_SCALE_BY_INDEX:";
            for (float f : CHILD_SCALE_BY_INDEX) {
                log += f + ">>";
            }
            log(log + ";mCardVerticalOffset:" + mCardVerticalOffset);
        }
    }

    @Override
    public View getSelectedView() {
        return converChildView(mActiveCard);
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

        log("onLayout isVaildBin:" + mRecycleBin.isVaildBin() + ";mCurPositon:" + mCurPositon + ";adapterCount:" + adapterCount);
        if (adapterCount == 0 || mCurPositon >= adapterCount) {
            removeAllViewsInLayout();
        } else {
            if (mRecycleBin.isVaildBin()) {
                int startingIndex = 0;
                if (adapterCount > MAX_VISIBLE) {
                    int curChildCount = getChildCount();
                    while (startingIndex < Math.min(adapterCount, MAX_VISIBLE)) {
                        log("curChildCount:"+curChildCount);
                        //计算要复用view的index
                        int recycleActiveIndex = curChildCount >= MAX_VISIBLE -1 ?
                                startingIndex : MAX_VISIBLE - 1 - (curChildCount + startingIndex);
                        if (recycleActiveIndex > -1 && mRecycleBin.getActiveView(recycleActiveIndex) == null) {
                            int position = mCurPositon + startingIndex + curChildCount;
                            if (position >= adapterCount) {
                                break;
                            }
                            log("RecycleBin startingIndex:" + startingIndex + ";recycleActiveIndex:" + recycleActiveIndex);
                            View recycleView = mRecycleBin.getAndResetRecycleView();
                            resetChildView(recycleView);
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
                    //todo
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
        if ((adapterCount - mCurPositon) == MIN_ADAPTER_STACK) mFlingListener.onAdapterAboutToEmpty(adapterCount);
    }

    private void resetChildView(View childView) {
        if (childView == null) return;
        childView.setTranslationX(0.f);
        childView.setTranslationY(0.f);
        childView.setScaleX(1.f);
        childView.setScaleY(1.f);
        childView.setRotation(0);
    }

    private void layoutChildren(int startingIndex, int adapterCount) {
        while (startingIndex < Math.min(adapterCount, MAX_VISIBLE)) {
            layoutChild(startingIndex, null);
            startingIndex++;
        }
    }

    private void layoutChild(int startingIndex, View convertView) {
        View newUnderChild = mAdapter.getView(startingIndex, convertView, this);
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

    private void updateChildrenOffset(float scrollProgressPercent) {
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
            float curScale = CHILD_SCALE_BY_INDEX[tempIndex];
            float targetScale = CHILD_SCALE_BY_INDEX[tempIndex + 1];
            float curHeight = mCardHeight * curScale;
            float targetHeight = mCardHeight * targetScale;
            float scaleOfPer = curScale + (targetScale - curScale) * absPer;
            float transY = (mCardHeight - curHeight) * .5f + CHILD_VERTICAL_OFFSET_BY_INDEX[tempIndex] -
                    ((targetHeight - curHeight) * .5f + mCardVerticalOffset) * absPer;
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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private View makeAndAddView(int pos, View view) {
        SwipeChildContainer child = null;
        SwipeLayoutParame lp = null;
        if (view.getParent() != null && view.getParent() instanceof SwipeChildContainer) {
            child = (SwipeChildContainer) view.getParent();
            lp = (SwipeLayoutParame) child.getLayoutParams();
        } else {
            SwipeLayoutParame viewLp = (SwipeLayoutParame) view.getLayoutParams();
            //TODO 理论这个不应该为空 先屏蔽掉 看下测试结果
            /*if (viewLp == null) {
                viewLp = new SwipeLayoutParame(WRAP_CONTENT, WRAP_CONTENT);
            }*/
            child = new SwipeChildContainer(getContext(), null);
            lp = new SwipeLayoutParame(viewLp);
            child.addView(view, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        addViewInLayout(child, 0, lp, true);
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


        int layoutDirection = getLayoutDirection();
        final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
        final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

        int childLeft;
        int childTop;
        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                childLeft = (getWidth() + getPaddingLeft() - getPaddingRight() - w) / 2 +
                        lp.leftMargin - lp.rightMargin;
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

            mActiveCard = getChildAt(LAST_OBJECT_IN_STACK);
            if (mActiveCard != null) {

                flingCardListener = new SwipeFlingCardListener(mActiveCard, mAdapter.getItem(0),
                        ROTATION_DEGREES, new SwipeFlingCardListener.FlingListener() {

                    @Override
                    public void onCardExited() {
                        mCurPositon += 1;
                        mActiveCard.setOnTouchListener(null);
                        mRecycleBin.removeActiveView(mActiveCard);
                        removeViewInLayout(mActiveCard);
                        mActiveCard = null;
                        mFlingListener.removeFirstObjectInAdapter();
                        requestLayout();
                    }

                    @Override
                    public void leftExit(View view, Object dataObject, boolean triggerByTouchMove) {
                        mFlingListener.onLeftCardExit(converChildView(view), dataObject, triggerByTouchMove);
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
                    public void onScroll(float scrollProgressPercent) {
                        mFlingListener.onScroll(scrollProgressPercent);
                        updateChildrenOffset(scrollProgressPercent);
                    }
                });
                mActiveCard.setOnTouchListener(flingCardListener);
            } else {
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

    public SwipeFlingCardListener getTopCardListener() throws NullPointerException {
        /*if (flingCardListener == null) {
            throw new NullPointerException();
        }*/
        return flingCardListener;
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


    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new SwipeLayoutParame(getContext(), attrs);
    }

    @Override
    public void setSelection(int i) {
        throw new UnsupportedOperationException("Not supported");
    }

    private void resetData() {
        mCurPositon = -1;
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            post(new Runnable() {
                @Override
                public void run() {
                    requestLayout();
                }
            });

        }

        @Override
        public void onInvalidated() {
            resetData();
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
                pritfViews("preadd");
            }
            View itemView = mActiveViews.set(pos, view);
            if (DEBUG) {
                pritfViews("postadd");
            }
        }

        void removeActiveView(View view) {
            if (view == null) return;
            for (int i = mActiveViews.size() -1; i >= 0; i--) {
                if (view == mActiveViews.get(i)) {
                    mActiveViews.remove(i);
                    mRecycleView = view;
                    mActiveViews.add(0, null);
                    break;
                }
            }
        }

        View getActiveView(int pos) {
            return mActiveViews.get(pos);
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

        boolean isVaildBin() {
            if (mAdapter == null || mActiveViews.size() == 0) {
                return false;
            }

            if (DEBUG) {
                //pritfViews("isVaildBin");
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

        private void pritfViews(String tag) {
            //test
            String ss = tag + " activeviews:";
            for (int i = 0; i < mActiveViews.size(); i++) {
                ss += (mActiveViews.get(i) != null ? "-"+converChildView(mActiveViews.get(i)).getTag() : null) + ">>";
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
        void removeFirstObjectInAdapter();

        void onLeftCardExit(View view, Object dataObject, boolean triggerByTouchMove);

        void onRightCardExit(View view, Object dataObject, boolean triggerByTouchMove);

        void onAdapterAboutToEmpty(int itemsInAdapter);

        void onScroll(float scrollProgressPercent);
    }

    private void log(String log) {
        if (DEBUG && true) {
            Log.v(TAG, log);
        }
    }

    private static class SwipeChildContainer extends FrameLayout {

        private float mDownX, mDownY;
        private float mTouchSlop;

        public SwipeChildContainer(Context context, AttributeSet attrs) {
            super(context, attrs);
            ViewConfiguration conf = ViewConfiguration.get(context);
            mTouchSlop = conf.getScaledTouchSlop();
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            boolean b = super.onInterceptTouchEvent(ev);
            /*switch (ev.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    mDownX = ev.getX();
                    mDownY = ev.getY();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    float dx = ev.getX() - mDownX;
                    float dy = ev.getY() - mDownY;
                    if (Math.abs(dx) < mTouchSlop && Math.abs(dy) < mTouchSlop) {
                        return false;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }*/
            return b;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            return super.onTouchEvent(ev);
        }
    }

}
