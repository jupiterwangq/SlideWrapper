package com.jupiter;

/**
 * Created by wangqiang on 16/11/17.
 */

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;
import com.tencent.stringtest.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangqiang on 16/11/16.
 * 滑动删除的包装器，任何想拥有侧滑删除功能的view都可以用该包装器来包装
 */

public class SlideWrapper extends RelativeLayout {

    private static final String TAG = "SlideWrapper";

    /**
     * 静态模式，侧滑view不动，仅被装的view移动
     */
    public static final String MODE_STATIC  = "static";
    /**
     * 动态模式，侧滑view跟随被包装的view一起移动
     */
    public static final String MODE_DYNAMIC = "dynamic";

    /**
     * 回调
     */
    public interface Callback {
        /**
         * 完全展开时的回调
         * @param wrapper
         */
        void onCompletelyOpen(SlideWrapper wrapper);

        /**
         * 从打开状态变成关闭时的回调
         * @param wrapper
         */
        void onCompleteClosed(SlideWrapper wrapper);

        /**
         * 拖到中途手势抬起后被拉回后的回调
         * @param wrapper
         */
        void onPullBack(SlideWrapper wrapper);
    }

    /**
     * 不在布局中写，运行期动态包装时可实现该接口提供侧滑view
     */
    public interface ViewFactory {
        /**
         * 生成子View
         * @param context
         * @return
         */
        List<View> getView(Context context);

        /**
         * 每个子view的宽度，以dp为单位
         */
        List<Integer> getWidthDp();
    }

    public void setCallback(Callback cb) {
        mCallback = new CallbackWrpper(cb);
    }

    //设置自己的ViewFactory,需要在运行时包装子View时调用
    public void setViewFactory(ViewFactory factory) {
        if (factory == null) return;
        mFactory = factory;
        mControlView = mFactory.getView(getContext());
        mIsFromFactory = true;
        if (mControlView != null) {
            for (View v : mControlView) {
                addView(v);
            }
        }
    }

    /**
     * 空的实现
     */
    public static class EmptyViewFactory implements ViewFactory {
        @Override
        public List<View> getView(Context context) {
            return null;
        }

        @Override
        public List<Integer> getWidthDp() {
            return null;
        }
    }

    /**
     * view factory实现例子，侧滑块是若干个TextView，默认实现是2个，
     * 可以重写某些方法自定义
     */
    public static class TextViewFactory implements ViewFactory{

        public int paddingDp = 8;

        public List<Integer> getWidthDp() {
            List<Integer> width = new ArrayList<Integer>();
            width.add(90);
            width.add(80);
            return width;
        }

        /**
         * 返回每个按钮的文字
         * @return
         */
        protected List<String> getText() {
            ArrayList<String> text = new ArrayList<>();
            text.add("置顶");
            text.add("删除");
            return text;
        }

        /**
         * 返回每个按钮的背景色
         * @return
         */
        protected List<Integer> getBackgroundColor() {
            ArrayList<Integer> bg = new ArrayList<>();
            bg.add(Color.LTGRAY);
            bg.add(Color.RED);
            return bg;
        }

        /**
         * 返回每个按钮的文字颜色
         * @return
         */
        protected List<Integer> getTextColor() {
            ArrayList<Integer> txtClr = new ArrayList<>();
            txtClr.add(Color.WHITE);
            txtClr.add(Color.WHITE);
            return txtClr;
        }

        /**
         * 返回文字大小（dp为单位）
         * @return
         */
        protected List<Integer> getTextSize() {
            ArrayList<Integer> txtSize = new ArrayList<>();
            txtSize.add(8);
            txtSize.add(8);
            return txtSize;
        }

        @Override
        public List<View> getView(Context context) {
            TextView toTop = new TextView(context);
            TextView delete = new TextView(context);
            ArrayList<View> views = new ArrayList<>();
            views.add(toTop);
            views.add(delete);
            for (int i = 0; i < views.size(); i++) {
                TextView tv = (TextView)views.get(i);
                paddingDp = dip2px(context, paddingDp);
                tv.setBackgroundColor(getBackgroundColor().get(i));
                tv.setTextColor(getTextColor().get(i));
                tv.setText(getText().get(i));
                tv.setTextSize(dip2px(context, getTextSize().get(i)));
                tv.setGravity(Gravity.CENTER);
            }
            return views;
        }
    }

    public SlideWrapper(Context context) {
        super(context);
        init(null);
    }

    public SlideWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SlideWrapper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mControlView = findControlView();
        mWrappedView = findContentView();
        enableControlView(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Context c = getContext();
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        View contentView = findContentView();
        if (contentView != null) {
            int h = contentView.getMeasuredHeight();
            if (mControlView != null && mControlView.size() > 0) {
                int controlViewW = 0;
                for (int i = 0; i < mControlView.size(); i++) {
                    View cv = mControlView.get(i);
                    controlViewW = mIsFromFactory ? dip2px(c, mFactory.getWidthDp().get(i)) : cv.getMeasuredWidth();
                    int wspec = MeasureSpec.makeMeasureSpec(controlViewW, MeasureSpec.EXACTLY);
                    int hspec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
                    cv.measure(wspec, hspec);
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View contentView = findContentView();
        if (contentView == null) {
            Log.e(TAG, "No content view!!!");
            return;
        }
        contentView.layout(0, 0, contentView.getMeasuredWidth(), contentView.getMeasuredHeight());
        if (mControlView != null && mControlView.size() > 0) {
            if (mMode.equals(MODE_DYNAMIC)) {
                int left = getMeasuredWidth();
                for (int i = 0; i < mControlView.size(); i++) {
                    View ctrlView = mControlView.get(i);
                    ctrlView.layout(left, 0, left + ctrlView.getMeasuredWidth(), ctrlView.getMeasuredHeight());
                    left += ctrlView.getMeasuredWidth();
                }
            } else {
                int right = getMeasuredWidth();
                for (int i = mControlView.size() -1; i >= 0; i--) {
                    View ctrlView = mControlView.get(i);
                    ctrlView.layout(right - ctrlView.getMeasuredWidth(), 0, right, getMeasuredHeight());
                    right -= ctrlView.getMeasuredWidth();
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int deltaX = 0, deltaY = 0;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsDecided = false;
                mIsOpenWhenTouchDown = false;
                mXDown = (int)ev.getX();
                mYDown = (int)ev.getY();
                mIsMoved = false;
                mX = mXDown;
                mY = mYDown;
                if (!doNotPerformClick) {
                    postDelayed(mLongPressRunnable, LONG_CLICK_INTERVAL);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(ev.getX() - mX) > CLICK_DISTANCE &&
                        Math.abs(ev.getY() - mY) > CLICK_DISTANCE) {
                    mIsMoved = true;
                    removeCallbacks(mLongPressRunnable);
                }
                if (!mIsDecided) {
                    deltaX = Math.abs((int) ev.getX() - (int) mXDown);
                    deltaY = Math.abs((int) ev.getY() - (int) mYDown);
                    if (deltaX > 0 && deltaY > 0) {
                        mIsDecided = true;
                        float tan = (float) deltaX / deltaY;
                        if (tan > TAN60) {
                            //当角度值超过60度的时候，就处理触摸事件，同时把点击事件暂时禁用掉
                            doNotPerformClick = true;
                            removeCallbacks(mLongPressRunnable);
                            requestDisallowInterceptTouchEvent(true);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                removeCallbacks(mLongPressRunnable);
                requestDisallowInterceptTouchEvent(false);
                if (mIsOpenWhenTouchDown && mCloseOnClick) {
                    close();
                }
                break;
        }
        //2.计算可滑动的距离值
        if (mScrollable == -1 ) {
            mScrollable = clacScrollableDistance();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isOpen()) {
            //如果当前是展开状态，就不拦截了
            mIsOpenWhenTouchDown = true;
            return false;
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mXDown = ev.getRawX();
                mXLastMove = mXDown;
                break;
            case MotionEvent.ACTION_MOVE:
                mXMove = ev.getRawX();
                mXLastMove = mXMove;
                return true;

        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mWrappedView == null) {
            mWrappedView = findContentView();
            if (mWrappedView == null) {
                return false;
            }
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownTime = System.currentTimeMillis();
                return true;
            case MotionEvent.ACTION_MOVE:

                mXMove = event.getRawX();
                int scrolledX = Math.round(mXMove - mXLastMove);
                if (mMode.equals(MODE_DYNAMIC)) {
                    //动态模式的滑动事件处理
                    handleDynaimcMoveEvent(scrolledX);
                } else {
                    //静态模式的滑动事件处理
                    handleStaticMoveEvent(scrolledX);
                }
                mXLastMove = mXMove;
                return true;
            case MotionEvent.ACTION_UP:
                mUpTime = System.currentTimeMillis();
                if (mUpTime - mDownTime <= CLICK_INTERVAL &&
                    (Math.abs(event.getX() - mX) < CLICK_DISTANCE  &&
                     Math.abs(event.getY() - mY) < CLICK_DISTANCE) && !doNotPerformClick) {
                    Log.e(TAG, "clicked");
                    performClick();
                    return true;
                }
                //抬起的时候做个动画
                doAnimation();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        if (mScroller == null) return;
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), 0);
            invalidate();
        } else {
            callback();
        }
    }

    //是否是展开状态
    public boolean isOpen() {
        return getXOffset() >= mScrollable;
    }

    //如果是展开的，关闭掉
    public void close() {
        if (isOpen()) {
            mNotifyCompleteClose = true;
            if (mMode.equals(MODE_DYNAMIC)) {
                int dx = -mScrollable;
                dynamicAnimation(dx);
            } else {
                staticAnimation(mWrappedView.getTranslationX(), 0.0f);
            }
        }
    }

    //查找被包装的view
    private View findContentView() {
        int children = getChildCount();
        for (int i = 0; i < children; i++) {
            View child = getChildAt(i);
            if (child != null) {
                Object tag = child.getTag();
                if ( tag == null ||(tag != null && (!(tag instanceof String) || !("ctrl".equals(tag)))) ) {
                    return child;
                }
            }
        }
        return null;
    }

    //查找ctrl view
    private List<View> findControlView() {
        List<View> ctrlView = new ArrayList<>();
        int children = getChildCount();
        for (int i = 0; i < children; i++) {
            View child = getChildAt(i);
            if (child != null) {
                Object tag = child.getTag();
                if (tag != null && "ctrl".equals(tag)) {
                    ctrlView.add(child);
                }
            }
        }
        return ctrlView;
    }

    //计算可滚动的距离
    private int clacScrollableDistance() {
        //1.优先检查用户动态设定的可滚动值
        int dis = 0;
        if (mIsFromFactory) {
            List<Integer> width = mFactory.getWidthDp();
            if (width != null && width.size() > 0) {
                for (int w : width) {
                    dis += dip2px(getContext(), w);
                }
                return dis;
            }
        }
        //2.用户没有指定，则自动计算
        if (mControlView != null) {
            for (View v : mControlView) {
                dis += v.getMeasuredWidth();
            }
        }
        return dis;
    }

    private void init(AttributeSet attr) {
        if (attr != null) {
            TypedArray a = getContext().obtainStyledAttributes(attr, R.styleable.SlideWrapper);
            mMode = a.getString(R.styleable.SlideWrapper_mode);
            mCloseOnClick = a.getBoolean(R.styleable.SlideWrapper_close_onclick, true);
            mAnimationDurationMs = a.getInt(R.styleable.SlideWrapper_anim_duration, ANIMATION_DURATION);
            if (mMode.equals(MODE_DYNAMIC)) {
                mScroller = new Scroller(getContext());
            }
            a.recycle();
        }
        mCallback = new CallbackWrpper(null);
    }

    private void scroll(int xoffset) {
        if (mMode.equals(MODE_STATIC)) {
            //静态的时候滚动view自己
            if (mWrappedView != null) {
                mWrappedView.setTranslationX(xoffset);
            }
        } else {
            //动态的时候滚动内容
            scrollBy(xoffset, 0);
        }
    }

    //复位，滚回原处
    private void reset() {
        if (mMode.equals(MODE_STATIC)) {
            //
        } else {
            scrollTo(0, 0);
        }
    }

    private int getXOffset() {
        int scrollX = mMode.equals(MODE_DYNAMIC) ? getScrollX() : (int)mWrappedView.getTranslationX();
        return Math.abs(scrollX);
    }

    //做动画
    private void doAnimation() {
        int scrollX = mMode.equals(MODE_DYNAMIC) ? getScrollX() : (int)mWrappedView.getTranslationX();
        scrollX = Math.abs(scrollX);
        if (scrollX <= 0 || scrollX >= mScrollable) return;
        int dx = 0;
        boolean close = true;
        if (scrollX < mScrollable / 3) {
            //小于1/3的时候就回滚
            dx = -scrollX;
            mNotifyPullback = mNotifyCompleteClose ? false : true;
        } else {
            //大于1/3就展开
            mNotifyCompleteOpen = true;
            close = false;
            dx = mScrollable - scrollX;
        }
        if (mMode.equals(MODE_STATIC)) {
            //静态模式，用属性动画来做
            staticAnimation(mWrappedView.getTranslationX(), close ? 0.0f : -mScrollable);
        } else {
            //动态模式，用Scroller来做动画
            dynamicAnimation(dx);
        }
    }

    private void dynamicAnimation(int dx) {
        mScroller.startScroll(getScrollX(), 0, dx, 0, mAnimationDurationMs);
        invalidate();
    }

    private void staticAnimation(float from, final float to) {
        final ValueAnimator animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(mAnimationDurationMs);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float v = (Float)animator.getAnimatedValue();
                mWrappedView.setTranslationX(v);
                if (v == to) {
                    callback();
                }
            }
        });
        animator.start();
    }

    private void callback() {
        if (mCallback == null) return;
        if (mNotifyCompleteOpen) {
            mCallback.onCompletelyOpen(this);
            mNotifyCompleteOpen = false;
        }
        if (mNotifyPullback) {
            mCallback.onPullBack(this);
            mNotifyPullback = false;
        }
        if (mNotifyCompleteClose) {
            mCallback.onCompleteClosed(this);
            mNotifyCompleteClose = false;
        }
    }

    private void handleDynaimcMoveEvent(int scrolledX) {
        if (scrolledX < 0) {
            //向左滑动
            int scrolled = Math.abs(getScrollX());
            if (scrolled < mScrollable) {
                if ((scrolled + Math.abs(scrolledX)) >= mScrollable) {
                    scrolledX = 0;
                }
                scroll(-scrolledX);
            }
        } else {
            //向右滑动
            int scrolled = Math.abs(getScrollX());
            if (scrolled > 0) {
                if ((scrolled - scrolledX <= 0)) {
                    scrolledX = 0;
                    //这里会有一点偏移，手动滚回原位
                    reset();
                }
                scroll(-scrolledX);
            }
        }
    }

    private void handleStaticMoveEvent(int scrolledX) {
        if (scrolledX < 0) {
            //向左滑动
            float xTrans = mWrappedView.getTranslationX();
            if (Math.abs(xTrans) >= mScrollable) {
                //不能在滑动了
            } else {
                if (Math.abs(xTrans + scrolledX) >= mScrollable) {
                    scroll(-mScrollable);
                } else {
                    scroll(Math.round(xTrans + scrolledX));
                }
            }
        } else {
            //向右滑动
            float xTrans = mWrappedView.getTranslationX();
            if (xTrans >= 0) {
                //不能继续右滑了
            } else {
                if (xTrans + scrolledX >= 0) {
                    scroll(0);
                } else {
                    scroll(Math.round(xTrans + scrolledX));
                }
            }
        }
    }

    private static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    private void enableControlView(boolean enable) {
        if (mEnable != enable) {
            mEnable = enable;
            if (mControlView != null && mMode.equals(MODE_STATIC)) {
                for (View v : mControlView) {
                    v.setEnabled(enable);
                }
            }
        }
    }

    private Runnable mLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            performLongClick();
        }
    };

    private class CallbackWrpper implements Callback {

        CallbackWrpper(Callback raw) {
            mRaw = raw;
        }

        @Override
        public void onCompletelyOpen(SlideWrapper wrapper) {
            //打开时，如果是静态模式，把侧滑view使能使之可以响应点击事件
            doNotPerformClick = true;
            enableControlView(true);
            if (mRaw != null) {
                mRaw.onCompletelyOpen(wrapper);
            }
        }

        @Override
        public void onCompleteClosed(SlideWrapper wrapper) {
            //关闭时，如果是静态模式，把侧滑view禁用不让它响应点击事件
            doNotPerformClick = false;
            enableControlView(false);
            if (mRaw != null) {
                mRaw.onCompleteClosed(wrapper);
            }
        }

        @Override
        public void onPullBack(SlideWrapper wrapper) {
            doNotPerformClick = false;
            if (mRaw != null) {
                mRaw.onPullBack(wrapper);
            }
        }

        Callback mRaw;
    }

    private float mXDown, mYDown;
    private float mXMove;
    private float mXLastMove;

    private List<View> mControlView;
    private View mWrappedView;

    private int mScrollable = -1;

    private final float TAN60 = 1.73f;

    private ViewFactory mFactory = new EmptyViewFactory();
    private boolean mIsFromFactory = false;

    //是否已经确定了阻止父控件拦截触摸事件，提升效率用的标记，避免频繁在dispatchTouchEvent中做浮点运算
    private boolean mIsDecided = false;

    private static final int ANIMATION_DURATION = 300;

    private Scroller mScroller;

    private boolean mNotifyCompleteOpen = false, mNotifyCompleteClose = false,
            mNotifyPullback = false;

    private Callback mCallback;

    private int mAnimationDurationMs = ANIMATION_DURATION;
    private boolean mCloseOnClick = true;
    private String mMode = MODE_STATIC;

    private boolean mEnable = true;

    //按下时是处于展开状态
    private boolean mIsOpenWhenTouchDown = false;

    //以下用于辅助判断长按和点击事件
    private long mDownTime, mUpTime;
    //不触发点击、长按（在展开状态下点击时不触发）
    private boolean doNotPerformClick = false;
    private float mX, mY;
    private boolean mIsMoved = false;
    private final int CLICK_INTERVAL = 300;
    private final int CLICK_DISTANCE = 20;
    private final int LONG_CLICK_INTERVAL = ViewConfiguration.getLongPressTimeout();
}

