package com.example.swipecards.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.swipecards.R;
import com.example.swipecards.util.BaseModel;
import com.example.swipecards.util.CardEntity;
import com.example.swipecards.util.RetrofitHelper;
import com.example.swipecards.view.SwipeFlingBottomLayout;
import com.zc.swiple.SwipeFlingView;
import com.zc.swiple.SwipeFlingViewNew;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 卡片Fragment
 *
 * @author zc
 */
public class CardFragment extends Fragment implements SwipeFlingViewNew.onSwipeListener,
        SwipeFlingBottomLayout.OnBottomItemClickListener, SwipeFlingViewNew.OnItemClickListener {

    private final static String TAG = CardFragment.class.getSimpleName();
    private final static boolean DEBUG = true;

    @InjectView(R.id.frame)
    SwipeFlingViewNew mSwipeFlingView;

    @InjectView(R.id.swipe_fling_bottom)
    SwipeFlingBottomLayout mBottomLayout;

    private UserAdapter mAdapter;

    private int mPageIndex = 0;
    private boolean mIsRequestGirlList;
    private ArrayList<CardEntity> mGrilList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.card_layout, null);
        ButterKnife.inject(this, rootView);
        initView();
        requestGirlList();
        return rootView;
    }

    private void initView() {
        mAdapter = new UserAdapter(getActivity(), mGrilList);
        mSwipeFlingView.setAdapter(mAdapter);
        mSwipeFlingView.setFlingListener(this);
        mSwipeFlingView.setOnItemClickListener(this);
        mBottomLayout.setOnBottomItemClickListener(this);
    }

    private void updateListView(ArrayList<CardEntity> list) {
        mGrilList.addAll(list);
        mAdapter.notifyDataSetChanged();
    }

    private void requestGirlList() {
        if (mIsRequestGirlList) {
            return;
        }
        mIsRequestGirlList = true;
        Call<BaseModel<ArrayList<CardEntity>>> call = RetrofitHelper.api().getGirlList(mPageIndex);
        RetrofitHelper.call(call, new RetrofitHelper.ApiCallback<ArrayList<CardEntity>>() {
            @Override
            public void onLoadSucceed(ArrayList<CardEntity> result) {
                updateListView(result);
                ++mPageIndex;
                mIsRequestGirlList = false;
            }

            @Override
            public void onLoadFail(int statusCode) {
                mIsRequestGirlList = false;
            }

            @Override
            public void onForbidden() {
                mIsRequestGirlList = false;
            }
        });
    }

    @Override
    public void onStartDragCard() {
        if (DEBUG) {
            Log.d(TAG, "SwipeFlingView onStartDragCard");
        }
    }

    @Override
    public void onPreCardExit() {
        if (DEBUG) {
            Log.d(TAG, "SwipeFlingView onPreCardExit");
        }
    }

    @Override
    public void onTopCardViewFinish() {
        if (DEBUG) {
            Log.d(TAG, "SwipeFlingView onTopCardViewFinish");
        }
    }

    @Override
    public boolean canLeftCardExit() {
        if (DEBUG) {
            Log.d(TAG, "SwipeFlingView canLeftCardExit");
        }
        return true;
    }

    @Override
    public boolean canRightCardExit() {
        if (DEBUG) {
            Log.d(TAG, "SwipeFlingView canRightCardExit");
        }
        return true;
    }

    @Override
    public void onLeftCardExit(View view, Object dataObject, boolean triggerByTouchMove) {
        if (DEBUG) {
            Log.d(TAG, "SwipeFlingView onLeftCardExit");
        }
    }

    @Override
    public void onRightCardExit(View view, Object dataObject, boolean triggerByTouchMove) {
        if (DEBUG) {
            Log.d(TAG, "SwipeFlingView onRightCardExit");
        }
    }

    @Override
    public void onSuperLike(View view, Object dataObject, boolean triggerByTouchMove) {
        if (DEBUG) {
            Log.d(TAG, "SwipeFlingView onSuperLike");
        }
    }

    @Override
    public void onAdapterAboutToEmpty(int itemsInAdapter) {
        if (DEBUG) {
            Log.d(TAG, "SwipeFlingView onAdapterAboutToEmpty");
        }
        requestGirlList();
    }

    @Override
    public void onAdapterEmpty() {
        if (DEBUG) {
            Log.d(TAG, "SwipeFlingView onAdapterEmpty");
        }
    }

    @Override
    public void onScroll(View selectedView, float scrollProgressPercent) {
        if (DEBUG) {
            Log.d(TAG, "SwipeFlingView onScroll " + scrollProgressPercent);
        }
    }

    @Override
    public void onEndDragCard() {
        if (DEBUG) {
            Log.d(TAG, "SwipeFlingView onEndDragCard");
        }
    }

    @Override
    public void onComeBackClick() {
        //参数决定动画开始位置是从左边还是右边出现
        mSwipeFlingView.selectComeBackCard(true);
    }

    @Override
    public void onSuperLikeClick() {
        if (mSwipeFlingView.isAnimationRunning()) {
            return;
        }
        mSwipeFlingView.selectSuperLike(false);
    }

    @Override
    public void onLikeClick() {
        if (mSwipeFlingView.isAnimationRunning()) {
            return;
        }
        mSwipeFlingView.selectRight(false);
    }

    @Override
    public void onUnLikeClick() {
        if (mSwipeFlingView.isAnimationRunning()) {
            return;
        }
        mSwipeFlingView.selectLeft(false);
    }

    @Override
    public void onItemClicked(int itemPosition, Object dataObject) {
        if (DEBUG) {
            Log.d(TAG, "onItemClicked itemPosition:" + itemPosition);
        }
    }
}
