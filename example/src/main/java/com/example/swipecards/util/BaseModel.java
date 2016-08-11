package com.example.swipecards.util;

import android.support.annotation.Keep;

/**
 * 接口数据model基类
 * @zc
 */
@Keep
public class BaseModel<T> {
    public boolean error;
    public T results;
}
