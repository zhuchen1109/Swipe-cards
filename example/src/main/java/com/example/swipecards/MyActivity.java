package com.example.swipecards;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private float mDensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        ButterKnife.inject(this);

        mDensity = getResources().getDisplayMetrics().density;

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
        mSwipeFlingDetailLayut.setVisibility(View.VISIBLE);
        mSwipeFlingView.getSelectedView();
        updateContainerLayout();
    }

    private void dismissDetailLayout() {
        mSwipeFlingDetailLayut.setVisibility(View.INVISIBLE);
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


}
