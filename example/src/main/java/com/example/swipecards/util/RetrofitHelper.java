package com.example.swipecards.util;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 网络请求辅助类
 *
 * @zc
 */
public class RetrofitHelper {

    private static final String TAG = RetrofitHelper.class.getSimpleName();
    public static final int STATUS_HTTP_ERROR = -1;

    private static Api apiService = null;
    private static retrofit2.Retrofit retrofit = null;
    private static OkHttpClient okHttpClient = null;
    public static Context context;

    private RetrofitHelper() {
    }

    public static Api api() {
        if (apiService == null) {
            init();
        }
        return apiService;
    }

    private static void init() {
        Gson gson = new GsonBuilder().create();
        okHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        HttpUrl originalHttpUrl = original.url();
                        MyLog.w(TAG, "request " + originalHttpUrl);
                        return chain.proceed(original);
                    }
                })
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
        retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl(Api.HOST)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(Api.class);
    }

    /**
     * call里的回調.
     */
    public interface ApiCallback<T> {

        void onLoadSucceed(T result);

        void onLoadFail(int statusCode);

        void onForbidden();
    }

    public static <T> void call(Call<BaseModel<T>> call) {
        call(call, new RetrofitHelper.SimpleApiCallback<T>());
    }

    public static <T> void call(Call<BaseModel<T>> call, final ApiCallback<T> callback) {
        call(call, callback, true);
    }

    public static <T> void call(Call<BaseModel<T>> call, final ApiCallback<T> callback, final boolean allowNull) {
        call.enqueue(new Callback<BaseModel<T>>() {
            @Override
            public void onResponse(Call<BaseModel<T>> call, retrofit2.Response<BaseModel<T>> response) {
                final int statusCode = response.code();
                if (response.code() == HttpURLConnection.HTTP_FORBIDDEN) {
                    MyLog.d(TAG, "call() fail， onForbidden");
                    if (callback != null) {
                        callback.onForbidden();
                    }
                    return;
                }

                if (!response.isSuccessful()) {
                    MyLog.d(TAG, "call() fail， !isSuccessful");
                    if (callback != null) {
                        callback.onLoadFail(STATUS_HTTP_ERROR);
                    }
                    return;
                }

                BaseModel<T> model = response.body();
                if (model.error) {
                    MyLog.d(TAG, "call() fail， model.error = true");
                    if (callback != null) {
                        callback.onLoadFail(-100);
                    }
                    return;
                }
                T data = model.results;
                if (!allowNull && data == null) {
                    MyLog.d(TAG, "call() fail， data == null ");
                    callback.onLoadFail(-100);
                    return;
                }

                MyLog.d(TAG, "call success, result = " + ((data instanceof Collection) ? ((Collection) data).size() : data));
                if (callback != null) {
                    callback.onLoadSucceed(data);
                }
            }

            @Override
            public void onFailure(Call<BaseModel<T>> call, Throwable t) {
                MyLog.d(TAG, "call() fail. onFailure --> ", t);
                callback.onLoadFail(STATUS_HTTP_ERROR);
            }
        });
    }

    public static class SimpleRetrofitCallback<T> implements Callback<T> {
        public void onResponse(Call<T> call, retrofit2.Response<T> response) {
        }

        public void onFailure(Call<T> call, Throwable t) {
        }
    }

    public static class SimpleApiCallback<T> implements ApiCallback<T> {
        public void onLoadSucceed(T result) {
        }

        public void onLoadFail(int statusCode) {
        }

        public void onForbidden() {
        }
    }

}
