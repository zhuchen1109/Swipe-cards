# 有图有真相：

<td>
	<img src=“https://github.com/zhuchen1109/Swipe-cards/blob/master/doc/capture0.png” width=“720” height=“1280” /><br>
  	<img src=“https://github.com/zhuchen1109/Swipe-cards/blob/master/doc/capture1.gif” width=“382” height=“657” />
</td>


# 如何使用（直接看demo工程，清晰易懂）：
<a href=“https://github.com/zhuchen1109/Swipe-cards/blob/master/doc/demo.apk”>DEMO体验下载</a>

主要代码：
```
mAdapter = new UserAdapter(getActivity(), mGrilList);
mSwipeFlingView.setAdapter(mAdapter);
mSwipeFlingView.setFlingListener(this);
mSwipeFlingView.setOnItemClickListener(this);
```



# 请允许我来给这个项目吹吹牛


* 1.易用  和使用listview一样方便，提供Adapter来定制视图

* 2.视图复用  无论划多少页，全局只有4个卡片view

* 3.流畅  再快的手势也永远感觉不到卡的刹那

* 4.高效  开启硬件加速，拖拽操作不使用invalidate（invalidate会重新生成FBO，使渲染效率降低）

* 5.精细  卡片与卡片之间动画联动效果是像素级的



# API说明：

这个项目在实际产品中经历过各种需求的洗礼，有着多轮的迭代优化，现在API已较为完善

核心回调如下：

```
public interface onSwipeListener {

        //void onStart(SwipeFlingViewNew swipeFlingView);

        /**
         * 拖拽开始时调用
         */
        void onStartDragCard();

        /**
         * 用来判断是否允许卡片向左离开(fling)
         *
         * @return true:允许卡片向左离开(fling)
         */
        boolean canLeftCardExit();

        /**
         * 用来判断是否允许卡片向右离开(fling)
         *
         * @return true:允许卡片向右离开(fling)
         */
        boolean canRightCardExit();

        /**
         * 在卡片即将要离开(fling)前，会回调此函数
         */
        void onPreCardExit();

        /**
         * 在卡片向左完全离开时，会回调此函数
         *
         * @param view               当前的view
         * @param dataObject
         * @param triggerByTouchMove 若true:表示此次卡片离开是来之于手势拖拽 反之则来之于点击按钮触发之类的
         */
        void onLeftCardExit(View view, Object dataObject, boolean triggerByTouchMove);

        /**
         * 在卡片向右完全离开时，会回调此函数
         *
         * @param view               当前的view
         * @param dataObject
         * @param triggerByTouchMove 若true:表示此次卡片离开是来之于手势拖拽 反之则来之于点击按钮触发之类的
         */
        void onRightCardExit(View view, Object dataObject, boolean triggerByTouchMove);

        /**
         * 在卡片完全离开时，若来之于superlike，会回调此函数
         *
         * @param view               当前的view
         * @param dataObject
         * @param triggerByTouchMove 若true:表示此次卡片离开是来之于手势拖拽 反之则来之于点击按钮触发之类的
         */
        void onSuperLike(View view, Object dataObject, boolean triggerByTouchMove);

        /**
         * 在顶部卡片划走时，会刷新布局，将设置顶部下面的卡片为顶部卡片，此时会回调此函数
         */
        void onTopCardViewFinish();

        /**
         * 当剩余卡片数等于{@link SwipeFlingView#MIN_ADAPTER_STACK}时，会回调此函数
         * 意味着卡片即将划完了，在这个时机可以做预加载下一批数据的工作
         *
         * @param itemsInAdapter
         */
        void onAdapterAboutToEmpty(int itemsInAdapter);

        /**
         * 当所有卡片都划完了，就回调此函数
         */
        void onAdapterEmpty();

        /**
         * 卡片因拖拽或动画发生位移时，会实时回调此函数
         *
         * @param selectedView          发生位移的view
         * @param scrollProgressPercent 范围[-1,1] 默认是0 向左位移是0->-1,向右位移是0->1.
         *                              左侧最大值由{@link #leftBorder()}决定，右侧最大值由{@link #rightBorder()} ()}决定，
         */
        void onScroll(View selectedView, float scrollProgressPercent);

        /**
         * 拖拽结束时调用
         */
        void onEndDragCard();

        //void onEnd();
    }
```


