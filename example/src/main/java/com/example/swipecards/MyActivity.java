package com.example.swipecards;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.flingswipe.SwipeFlingDetailLayut;
import com.example.flingswipe.SwipeFlingView;

import java.util.ArrayList;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MyActivity extends Activity {

    private ArrayList<String> al;
    private ArrayAdapter<String> arrayAdapter;
    private int i;

    private int[] imgRes = new int[] {R.drawable.test1, R.drawable.test2, R.drawable.test3, R.drawable.test4};

    @InjectView(R.id.frame)
    SwipeFlingView mSwipeFlingView;

    @InjectView(R.id.detail_layout)
    SwipeFlingDetailLayut mSwipeFlingDetailLayut;

    @InjectView(R.id.first_view)
    ViewPager mViewPager;

    private DetailImgsPageAdapter mDetailImgsPageAdapter;

    @InjectView(R.id.second_view)
    LinearLayout mContainerLayout;

    private View[] mDetailListViews;

    private float mDensity, mScreenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        ButterKnife.inject(this);

        mDensity = getResources().getDisplayMetrics().density;
        mScreenHeight = getResources().getDisplayMetrics().heightPixels;

        al = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            al.add("page0:" + i);
        }

        arrayAdapter = new ArrayAdapter<String>(this, R.layout.item, R.id.helloText, al ){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                view.setTag(getItem(position));
                ImageView iv = (ImageView) view.findViewById(R.id.img);
                Random random = new Random();
                iv.setImageResource(imgRes[random.nextInt(4)]);
                return view;
            }

            @Override
            public String getItem(int position) {
                String s = super.getItem(position);
                return s;
            }
        };


        mSwipeFlingView.setAdapter(arrayAdapter);
        mSwipeFlingView.setFlingListener(new SwipeFlingView.onSwipeListener() {
            int loadNum = 0;

            @Override
            public void onLeftCardExit(View view, Object dataObject, boolean triggerByTouchMove) {
                //Do something on the left!
                //You also have access to the original object.
                //If you want to use it just cast it (String) dataObject
                //Log.d("LIST", "onLeftCardExit:"+dataObject);
                makeToast(MyActivity.this, "Left!");
            }

            @Override
            public void onRightCardExit(View view, Object dataObject, boolean triggerByTouchMove) {
                //Log.d("LIST", "onRightCardExit:"+dataObject);
                makeToast(MyActivity.this, "Right!");
            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
                // Ask for more data here
                //al.add("XML ".concat(String.valueOf(i)));
                //arrayAdapter.notifyDataSetChanged();
                Log.d("LIST", "notified itemsInAdapter:" + itemsInAdapter + ";loadNum:" + loadNum);
                //i++;
                //test mulit page load data
                if (loadNum < 2) {
                    loadNum++;
                    int count = 4;
                    for (int i = 0; i < count; i++) {
                        al.add("page" + loadNum + ":" + i);
                    }
                    arrayAdapter.notifyDataSetChanged();

                }
            }

            @Override
            public void onScroll(View selectedView, float scrollProgressPercent) {
                if (selectedView != null) {
                    selectedView.findViewById(R.id.item_swipe_right_indicator).setAlpha(scrollProgressPercent < 0 ? -scrollProgressPercent : 0);
                    selectedView.findViewById(R.id.item_swipe_left_indicator).setAlpha(scrollProgressPercent > 0 ? scrollProgressPercent : 0);
                }
            }
        });


        // Optionally add an OnItemClickListener
        mSwipeFlingView.setOnItemClickListener(new SwipeFlingView.OnItemClickListener() {
            @Override
            public void onItemClicked(int itemPosition, Object dataObject) {
                makeToast(MyActivity.this, "Clicked!");
                showDetailLayout();
            }
        });

        //init detail
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        mDetailListViews = new View[3];
        for (int i = 0; i < mDetailListViews.length; i++) {
            View detailItemLayout = layoutInflater.inflate(R.layout.detail_img_item_view, null);
            mDetailListViews[i] = detailItemLayout;
        }
        mDetailImgsPageAdapter = new DetailImgsPageAdapter();
        mViewPager.setAdapter(mDetailImgsPageAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mDetailListViews[position % 3].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        makeToast(MyActivity.this, "Detail img Clicked!");
                        dismissDetailLayout();
                    }
                });
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mSwipeFlingDetailLayut.setFirstAndSecondeView(mViewPager, mContainerLayout);
        initContainerLayout();
    }

    private void updateDetailImgList() {

    }

    private void initContainerLayout() {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,  (int) (mDensity * 1000)));
        tv.setText("我是个大美女，哈哈哈！");
        mContainerLayout.addView(tv);
    }

    private void showDetailLayout() {
        updateContainerLayout();
        ImageView iv = (ImageView) mSwipeFlingView.getSelectedView().findViewById(R.id.img);
        int[] location = new int[2];
        iv.getLocationInWindow(location);
        int startWidth = iv.getWidth();
        int startHeight = iv.getHeight();
        float startX = location[0];
        float startY = location[1];

        mViewPager.getLocationInWindow(location);
        int endWidth = mViewPager.getWidth();
        int endHeight = mViewPager.getHeight();
        float endX = location[0];
        float endY = location[1];

        startShowDetailLayoutAnima(startWidth, startHeight, startX, startY, endWidth, endHeight, endX, endY);
    }

    private void startShowDetailLayoutAnima(int startWidth, int startHeight, float startX, float startY, int endWidth, int endHeight, float endX, float endY) {
        final float scaleX = (startWidth * 1.f / endWidth * 1.f);
        final float scaleY = (startHeight * 1.f / endHeight * 1.f);
        final float dx = startX + startWidth * .5f - (endX + endWidth * .5f);
        final float dy = startY + startHeight * .5f - (endY + endHeight * .5f);

        mViewPager.setScaleX(scaleX);
        mViewPager.setScaleY(scaleY);
        mViewPager.setTranslationX(dx);
        mViewPager.setTranslationY(dy);

        ValueAnimator viewPagerAnima = ValueAnimator.ofFloat(0.f, 1.f);
        viewPagerAnima.setInterpolator(sInterpolator);
        viewPagerAnima.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float frac = animation.getAnimatedFraction();
                mViewPager.setScaleX(scaleX + (1.f - scaleX) * frac);
                mViewPager.setScaleY(scaleY + (1.f - scaleY) * frac);
                mViewPager.setTranslationX(dx * (1.f - frac));
                mViewPager.setTranslationY(dy * (1.f - frac));
            }
        });
        viewPagerAnima.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mSwipeFlingDetailLayut.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }
        });
        viewPagerAnima.setDuration(500);

        mContainerLayout.setTranslationY(mScreenHeight - endY - endHeight);
        mContainerLayout.animate()
                .setInterpolator(sInterpolator)
                .setDuration(500)
                .translationY(0);

        viewPagerAnima.start();

        Log.d("xxxx", "startWidth:"+startWidth+"startHeight:"+startHeight+";startX:"+startX+";startY:"+startY+";endWidth:"+endWidth+";endHeight:"+endHeight+";endX:"+endX+";endY:"+endY
            +";scaleX:"+scaleX+";scaleY:"+scaleY+";dx:"+dx+";dy:"+dy+";"+(mScreenHeight - endY - endHeight));
    }

    private void dismissDetailLayout() {
        ImageView iv = (ImageView) mSwipeFlingView.getSelectedView().findViewById(R.id.img);
        int[] location = new int[2];
        iv.getLocationInWindow(location);
        int startWidth = iv.getWidth();
        int startHeight = iv.getHeight();
        float startX = location[0];
        float startY = location[1];

        mViewPager.getLocationInWindow(location);
        int endWidth = mViewPager.getWidth();
        int endHeight = mViewPager.getHeight();
        float endX = location[0];
        float endY = location[1];
        startDismissDetailLayoutAnima(startWidth, startHeight, startX, startY, endWidth, endHeight, endX, endY);
    }

    private void startDismissDetailLayoutAnima(int startWidth, int startHeight, float startX, float startY, int endWidth, int endHeight, float endX, float endY) {
        final float scaleX = (startWidth * 1.f / endWidth * 1.f);
        final float scaleY = (startHeight * 1.f / endHeight * 1.f);
        final float dx = startX + startWidth * .5f - (endX + endWidth * .5f);
        final float dy = startY + startHeight * .5f - (endY + endHeight * .5f);

        /*mViewPager.setScaleX(scaleX);
        mViewPager.setScaleY(scaleY);
        mViewPager.setTranslationX(dx);
        mViewPager.setTranslationY(dy);*/

        ValueAnimator viewPagerAnima = ValueAnimator.ofFloat(0.f, 1.f);
        viewPagerAnima.setInterpolator(sInterpolator);
        viewPagerAnima.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float frac = 1 - animation.getAnimatedFraction();
                mViewPager.setScaleX(scaleX + (1.f - scaleX) * frac);
                mViewPager.setScaleY(scaleY + (1.f - scaleY) * frac);
                mViewPager.setTranslationX(dx * (1.f - frac));
                mViewPager.setTranslationY(dy * (1.f - frac));
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
                mSwipeFlingDetailLayut.setVisibility(View.INVISIBLE);
                mViewPager.setScaleX(1.f);
                mViewPager.setScaleY(1.f);
                mViewPager.setTranslationX(0.f);
                mViewPager.setTranslationY(0.f);
            }
        });
        viewPagerAnima.setDuration(500);

        //mContainerLayout.setTranslationY(endHeight);
        mContainerLayout.animate()
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(300)
                .translationY(mScreenHeight - endY - endHeight);

        viewPagerAnima.start();

        /*Log.d("xxxx", "startWidth:"+startWidth+"startHeight:"+startHeight+";startX:"+startX+";startY:"+startY+";endWidth:"+endWidth+";endHeight:"+endHeight+";endX:"+endX+";endY:"+endY
            +";scaleX:"+scaleX+";scaleY:"+scaleY+";dx:"+dx+";dy:"+dy);*/
    }

    private void updateContainerLayout() {

    }

    static void makeToast(Context ctx, String s){
        Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show();
    }


    @OnClick(R.id.right)
    public void right() {
        /**
         * Trigger the right event manually.
         */
        mSwipeFlingView.getTopCardListener().selectRight();
    }

    @OnClick(R.id.left)
    public void left() {
        mSwipeFlingView.getTopCardListener().selectLeft();
    }

    private class DetailImgsPageAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view  == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = mDetailListViews[position % 3];
            container.addView(view);
            ((ImageView) view.findViewById(R.id.img)).setImageResource(imgRes[position]);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mDetailListViews[position % 3]);
        }
    }

    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

}
