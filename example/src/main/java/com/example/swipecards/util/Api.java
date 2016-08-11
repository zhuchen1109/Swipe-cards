package com.example.swipecards.util;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * 定义与服务端接口交互
 * @zc
 */
public interface Api {

    String HOST = "http://gank.io/api/data/%E7%A6%8F%E5%88%A9/";

    //http://gank.io/api/data/福利/10/1
    @GET("10/")
    Call<BaseModel<CardEntity>> getGirlList(@Url int pageIndex);

}
