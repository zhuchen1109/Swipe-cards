package com.example.swipecards.test;

import android.content.Context;

import com.example.swipecards.util.BaseModel;
import com.example.swipecards.util.CardEntity;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class TestData {

    public static ArrayList<CardEntity> getApiData(Context context) {
        BaseModel<ArrayList<CardEntity>> model = null;
        try {
            model = new GsonBuilder().create().fromJson(
                    new InputStreamReader(context.getAssets().open("test.json")),
                    new TypeToken<BaseModel<ArrayList<CardEntity>>>() {}.getType()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return model != null ? model.results : null;
    }

}
