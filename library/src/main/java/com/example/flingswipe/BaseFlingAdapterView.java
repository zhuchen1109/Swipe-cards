package com.example.flingswipe;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AdapterView;

abstract class BaseFlingAdapterView extends AdapterView {





    public BaseFlingAdapterView(Context context) {
        super(context);
    }

    public BaseFlingAdapterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseFlingAdapterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setSelection(int i) {
        throw new UnsupportedOperationException("Not supported");
    }






}
