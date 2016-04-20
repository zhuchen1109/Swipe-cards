package com.example.swipecards;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

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
    SwipeFlingView flingContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        ButterKnife.inject(this);


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


        flingContainer.setAdapter(arrayAdapter);
        flingContainer.setFlingListener(new SwipeFlingView.onSwipeListener() {
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
            public void onScroll(float scrollProgressPercent) {
                View view = flingContainer.getSelectedView();
                if (view != null) {
                    view.findViewById(R.id.item_swipe_right_indicator).setAlpha(scrollProgressPercent < 0 ? -scrollProgressPercent : 0);
                    view.findViewById(R.id.item_swipe_left_indicator).setAlpha(scrollProgressPercent > 0 ? scrollProgressPercent : 0);
                }
            }
        });


        // Optionally add an OnItemClickListener
        flingContainer.setOnItemClickListener(new SwipeFlingView.OnItemClickListener() {
            @Override
            public void onItemClicked(int itemPosition, Object dataObject) {
                makeToast(MyActivity.this, "Clicked!");
            }
        });

    }

    static void makeToast(Context ctx, String s){
        Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show();
    }


    @OnClick(R.id.right)
    public void right() {
        /**
         * Trigger the right event manually.
         */
        flingContainer.getTopCardListener().selectRight();
    }

    @OnClick(R.id.left)
    public void left() {
        flingContainer.getTopCardListener().selectLeft();
    }




}
