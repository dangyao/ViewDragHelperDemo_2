package cn.hnytdy.dragview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.nineoldandroids.view.ViewHelper;

/**
 * Created by dangyao on 2016/9/10.
 */
public class DragView extends FrameLayout{


    private ViewDragHelper mViewDragHelper;
    private ViewGroup leftPanel;
    private ViewGroup mainPanel;
    private int mHeight;
    private int mWidth;
    private int mRange;

    public enum DragState{
        CLOSE,OPEN,DRAGING
    }
    private DragState state = DragState.CLOSE;//默认关闭状态

    public interface  OnDragChangeListener{
        void onOpen();
        void onClose();
        void onDraging(float percent);
    }
    private OnDragChangeListener onDragChangeListener;

    public OnDragChangeListener getOnDragChangeListener() {
        return onDragChangeListener;
    }

    public void setOnDragChangeListener(OnDragChangeListener onDragChangeListener) {
        this.onDragChangeListener = onDragChangeListener;
    }

    public DragView(Context context) {
        this(context,null);
    }

    public DragView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public DragView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //创建ViewDragHelper对象
        mViewDragHelper = ViewDragHelper.create(this, callback);
    }


    //决定事件是否拦截

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    //处理触摸事件

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            mViewDragHelper.processTouchEvent(event);
        } catch (Exception e) {
            //多指操作可能有异常
            e.printStackTrace();
        }
        return true;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();


        leftPanel = (ViewGroup) findViewById(R.id.ll_01);
        mainPanel = (ViewGroup) findViewById(R.id.ll_02);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        mRange = (int) (mWidth * 0.6);//水平方向的拖动范围
    }

    /**
     * 处理回调，解析触摸事件
     */
    ViewDragHelper.Callback callback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mainPanel;
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            super.onViewCaptured(capturedChild, activePointerId);
        }

        /**
         * 还没有移动，根据返回值决定将达到的位置
         * @param child 拖动的view
         * @param left  将到达的位置(当前位置+瞬间变化量)
         * @param dx    瞬间变化量
         * @return  left
         */
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {



            return left;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mRange;
        }
        private int fixedLeft(int left) {
            left = left < 0 ? 0 : left > mRange ? mRange : left;
            return left;
        }
        /**
         * 实际移动时调用，在这里处理，伴随动画，状态更新等
         */
        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {

            if (changedView ==leftPanel) {
                leftPanel.layout(0, 0, 0 + mWidth, 0 + mHeight);
                int newLeft = mainPanel.getLeft() + dx;
                //修正位置
                newLeft = fixedLeft(newLeft);
                mainPanel.layout(newLeft, 0, newLeft + mWidth, 0 + mHeight);
            }
            //状态更新
            dispatchEvent();
        }

        /**
         * 手指释放调用
         * @param releasedChild     释放的子view
         * @param xvel     水平方向速度
         * @param yvel      垂直方向速度
         */
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (xvel == 0 && mainPanel.getLeft() > mRange * 1.0f / 2) {
                //速度为0  并且 打开的距离超过了最大拖动范围的一般 打开
                open();
            } else if (xvel > 0) {
                open();
            } else {
                close();
            }
        }

    };

    private void dispatchEvent() {

        float percent = mainPanel.getLeft() * 1.0f / mRange;
        animViews(percent);
        //记录上一个状态
        DragState preState = state;
        state = updateState(percent);
        if (onDragChangeListener != null) {
            onDragChangeListener.onDraging(percent);
            if (state != preState) {

                if (state == DragState.OPEN) {
                    onDragChangeListener.onOpen();
                } else if (state == DragState.CLOSE) {
                    onDragChangeListener.onClose();
                }
            }
        }
    }
        /**
         * 获取最新的状态
         * @param percent
         * @return
         */
        private DragState updateState(float percent) {
            if (percent == 0){
                return  DragState.CLOSE;
            }else if(percent == 1){
                return DragState.OPEN;
            }
            return DragState.DRAGING;
        }

        private void animViews(float percent) {
            //主面板 缩放动画  1.0  --- 0.8   0.8  + (1 - percent ) * 0.2
            // 估值器

            ViewHelper.setScaleX(mainPanel, evaluate(percent, 1.0f, 0.8f));
            ViewHelper.setScaleY(mainPanel, evaluate(percent, 1.0f, 0.8f));

            // z左面版动画 缩放 0.7-1.0f  平移
            ViewHelper.setScaleX(leftPanel,evaluate(percent,0.7f,1.0f));
            ViewHelper.setScaleY(leftPanel,evaluate(percent,0.7f,1.0f));
            ViewHelper.setTranslationX(leftPanel,evaluate(percent,-mWidth* 1.0f / 2,0));

            // 背景 颜色的改变  黑色--- 透明色 ARGBEvalute

            getBackground().setColorFilter((Integer) evaluateColor(percent, Color.BLACK,Color.TRANSPARENT), PorterDuff.Mode.SRC_OVER);
        }

        public Object evaluateColor(float fraction, Object startValue, Object endValue) {
            int startInt = (Integer) startValue;
            int startA = (startInt >> 24) & 0xff;
            int startR = (startInt >> 16) & 0xff;
            int startG = (startInt >> 8) & 0xff;
            int startB = startInt & 0xff;

            int endInt = (Integer) endValue;
            int endA = (endInt >> 24) & 0xff;
            int endR = (endInt >> 16) & 0xff;
            int endG = (endInt >> 8) & 0xff;
            int endB = endInt & 0xff;

            return (int)((startA + (int)(fraction * (endA - startA))) << 24) |
                    (int)((startR + (int)(fraction * (endR - startR))) << 16) |
                    (int)((startG + (int)(fraction * (endG - startG))) << 8) |
                    (int)((startB + (int)(fraction * (endB - startB))));
        }
        public Float evaluate(float fraction, Number startValue, Number endValue) {
            float startFloat = startValue.floatValue();
            return startFloat + fraction * (endValue.floatValue() - startFloat);
        }
        @Override
        public void computeScroll() {
            super.computeScroll();
            // 判断是否 继续触发动画
            if (mViewDragHelper.continueSettling(true)){
                //触发动画 执行动画
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        public void close(boolean isSmooth){
            int finalLeft = 0;
            if (isSmooth){
                //决定是否触发动画
                boolean b = mViewDragHelper.smoothSlideViewTo(mainPanel, finalLeft, 0);
                if (b){
                    //触发动画 执行动画
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            }else{

                mainPanel.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
            }
        }
        /**
         * 关闭
         */
        public void close() {
            close(true);//默认平滑关闭
        }


        public void open(boolean isSmooth) {
            int finalLeft = mRange;
            if (isSmooth) {
// Scroller
                //决定是否触发动画
                boolean b = mViewDragHelper.smoothSlideViewTo(mainPanel, finalLeft, 0);
                if (b){
                    //触发动画 执行动画
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            } else {
               mainPanel.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
            }
        }

        /**
         * 打开
         */
        public void open() {
            open(true);//默认平滑打开
        }
    }

