package com.example.swipecards.ui;

import android.app.Activity;
import android.os.Bundle;

import com.example.swipecards.R;

import butterknife.ButterKnife;


public class MyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        ButterKnife.inject(this);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new CardFragment())
                    .commitAllowingStateLoss();
        }
    }

}
