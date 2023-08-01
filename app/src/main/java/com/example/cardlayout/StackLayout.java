package com.example.cardlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;


/**
 * <pre>
 * Created by zhuguohui
 * Date: 2023/7/28
 * Time: 10:37
 * Desc:
 * </pre>
 */
public class StackLayout extends ViewGroup {


    private final GestureDetector detector;
    private final Paint logPaint;

    public StackLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        logPaint = new Paint();
        logPaint.setTextSize(60);
        logPaint.setColor(Color.WHITE);

        detector = new GestureDetector(getContext(), listener);
    }

    enum Direction {
        Left(true), TOP(false), RIGHT(true), BOTTOM(false);
        boolean isHorizontal;

        Direction(boolean isHorizontal) {
            this.isHorizontal = isHorizontal;
        }
    }

    private StackAdapter adapter;

    public void setAdapter(StackAdapter adapter) {
        this.adapter = adapter;
        fillViews(true);
    }

    private List<View> detachViews = new ArrayList<>();

    private void fillViews(boolean fromZero) {
        if (fromZero) {
            removeAllViews();
        } else {
            detachViews.clear();
            for (int i = 0; i < getChildCount(); i++) {
                detachViews.add(getChildAt(i));
            }
            detachAllViewsFromParent();
        }

        if (adapter == null || adapter.getCount() == 0) {
            return;
        }
        if (fromZero) {
            mFirstDataPosition = 0;

        }
        int start = mFirstDataPosition;
        int end = Math.min(adapter.getCount(), mFirstDataPosition + adapter.getVisibleCount());

        for (int i = start; i < end; i++) {
            View view = tryGetViewByPosition(i);
            if (view == null) {
                return;
            }
            addView(view);
        }

    }

    private int recycleViewsMinSize = 2;

    private View tryGetViewByPosition(int position) {
        int count = adapter.getCount();
        if (position > (count - 1) || position < 0) {
            return null;
        }

        for (View view : detachViews) {
            StackLayoutParams sp = (StackLayoutParams) view.getLayoutParams();
            if (sp.dataPosition == position) {
                detachViews.remove(view);
                return view;
            }
        }
        //从回收列表中找一个
        View view = null;
        if (!recycledViews.isEmpty()) {

            for (View reuseView : recycledViews) {
                StackLayoutParams sp = (StackLayoutParams) reuseView.getLayoutParams();
                if (sp.dataPosition == position) {
                    recycledViews.remove(reuseView);
                    resetView(reuseView);
                    sp.dataPosition=position;
                    return reuseView;
                }
            }
            if ( recycledViews.size() > recycleViewsMinSize) {
                //recycledViews 作为一个队列使用。队列大小固定为2。
                //如果recycleViews 的大小小于2
                view = recycledViews.remove(0);
                resetView(view);
            }

        }
        //找不到新建一个
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = adapter.getView(position, inflater, this);
            Log.i("zzz", " 新建view position="+position);
        }
        StackLayoutParams sp = (StackLayoutParams) view.getLayoutParams();
        sp.dataPosition = position;
        adapter.bindData(view, position);
        Log.i("zzz", " 绑定view数据 position="+position);
        return view;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            StackLayoutParams sp = (StackLayoutParams) child.getLayoutParams();
            sp.dataPosition = i;
        }
    }

    DirectionHelper directionHelper = new DirectionHelper();

    private class DirectionHelper {
        Handler handler = new android.os.Handler();
        Direction direction = null;
        View dragView = null;

        public void start() {
            direction = null;
        }


        private ValueAnimator animator;
        private Runnable autoStop = new Runnable() {
            @Override
            public void run() {
                if (getChildCount() == 0) {
                    return;
                }
                View child = dragView;
                if (child == null) {
                    return;
                }
                StackLayoutParams p = (StackLayoutParams) child.getLayoutParams();
                if (p.direction == null) {
                    return;
                }
                float from = p.direction.isHorizontal ? child.getTranslationX() : child.getTranslationY();
                float to = 0;
                boolean moveOut = false;
                if (p.outOffset < 0.5) {
                    //返回

                } else {
                    moveOut = true;
                    //移除
                    int pw = getWidth();
                    int ph = getHeight();
                    int cw = child.getWidth();
                    int ch = child.getHeight();
                    switch (p.direction) {
                        case TOP:
                            to = -ph * 1.0f / 2 - ch * 1.0f / 2;
                            break;
                        case BOTTOM:
                            to = 0;
                            moveOut = false;
                            break;
                        case Left:
                            to = -pw * 1.0f / 2 - cw * 1.0f / 2;
                            break;
                        case RIGHT:
                            to = pw * 1.0f / 2 + cw * 1.0f / 2;
                            break;
                    }
                }

                startAnimation(child, from, to, p.direction.isHorizontal, moveOut);
            }

            private void startAnimation(View child, float from, float to, boolean isHorizontal, boolean moveOut) {
                animator = null;
                animator = ObjectAnimator.ofFloat(from, to);
                animator.addUpdateListener(animation -> {
                    float t = (float) animation.getAnimatedValue();
                    if (isHorizontal) {
                        child.setTranslationX(t);
                    } else {
                        child.setTranslationY(t);
                    }
                });
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (moveOut) {
                            mFirstDataPosition++;
                            recycleView(child);
                        } else {
                            resetView(child);

                        }
                        //回收看不见的View
                        int count = getChildCount();
                        if (count > adapter.getVisibleCount()) {
                            List<View> needRecycleViews = new ArrayList<>();
                            for (int i = adapter.getVisibleCount(); i < count; i++) {
                                needRecycleViews.add(getChildAt(i));
                            }
                            for (View view : needRecycleViews) {
                                recycleView(view);
                            }
                        }
                        dragView = null;
                        direction = null;
                    }
                });
                animator.setDuration(200);
                animator.start();
            }
        };

        private void cancelAnimator() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
        }

        public void onScroll(float distanceX, float distanceY) {

            StackLayoutParams p = null;
            if (direction == null) {
                if (Math.abs(distanceX) > Math.abs(distanceY)) {
                    direction = distanceX > 0 ? Direction.Left : Direction.RIGHT;
                } else {
                    direction = distanceY > 0 ? Direction.TOP : Direction.BOTTOM;
                }

            }
            if (dragView == null) {
                if (direction == Direction.BOTTOM) {
                    dragView = tryGetViewByPosition(mFirstDataPosition - 1);
                    if (dragView != null && dragView.getParent() == null) {
                        showPreView(dragView);
                    }
                } else {
                    dragView = getChildAt(0);
                }
            }
            if (dragView == null) {
                return;
            }
            p = (StackLayoutParams) dragView.getLayoutParams();
            p.direction = direction;

            float translationX = dragView.getTranslationX();
            float translationY = dragView.getTranslationY();
            p.isDrag = true;
            float outOffset = 0;
            switch (direction) {
                case RIGHT:
                case Left:
                    translationX += (-distanceX);
                    outOffset = Math.abs(translationX) / dragView.getWidth();
                    break;
                case TOP:
                case BOTTOM:
                    if (translationY <= 0) {
                        translationY += (-distanceY);
                        translationY = Math.min(0, translationY);
                        outOffset = Math.abs(translationY) / dragView.getHeight();
                    }
                    break;
            }


            dragView.setTranslationX(translationX);
            dragView.setTranslationY(translationY);

            outOffset = Math.max(0, outOffset);
            outOffset = Math.min(1, outOffset);
            p.outOffset = outOffset;

            dragView.setAlpha(1 - outOffset * 0.5f);
            applyTranslation(outOffset);


        }

        private final int outFlingVelocity = 4000;

        public void onFling(float velocityX, float velocityY) {
            cancelAnimator();
            if (dragView == null) {
                return;
            }
            StackLayoutParams p = (StackLayoutParams) dragView.getLayoutParams();
            if (direction == Direction.Left) {
                if (velocityX < -outFlingVelocity) {
                    p.outOffset = 1.0f;
                }
            } else if (direction == Direction.RIGHT) {
                if (velocityX > outFlingVelocity) {
                    p.outOffset = 1.0f;
                }
            } else if (direction == Direction.TOP) {
                if (velocityY < -outFlingVelocity) {
                    p.outOffset = 1.0f;
                }
            } else if (direction == Direction.BOTTOM) {
                if (velocityY > outFlingVelocity) {
                    p.outOffset = 1.0f;
                }
            }

        }

        private void moveOut() {
            handler.post(autoStop);
        }
    }


    private int mFirstDataPosition = 0;

    private void showPreView(View view) {

        StackLayoutParams sp = (StackLayoutParams) view.getLayoutParams();
        addView(view, 0);

        view.setTranslationX(0);
        view.setTranslationY(-getHeight() * 1.0f / 2);
        view.setVisibility(VISIBLE);
        view.setTranslationZ(100);
        view.setAlpha(0.5f);
        sp.isBefore = true;
        mFirstDataPosition--;
        sp.dataPosition = mFirstDataPosition;
        sp.isDrag = true;

    }


    private void resetView(View child) {
        if (child == null) {
            return;
        }
        StackLayoutParams p = (StackLayoutParams) child.getLayoutParams();
        p.reset();
        child.setAlpha(1.0f);
        child.setTranslationZ(0);
        child.setTranslationY(0);
        child.setTranslationX(0);
        child.setVisibility(VISIBLE);
        applyTranslation(0);
    }

    private List<View> recycledViews = new ArrayList<>();

    private void recycleView(View child) {
        detachViewFromParent(child);
        child.setVisibility(GONE);
        recycledViews.add(child);
        fillViews(false);
    }


    private GestureDetector.OnGestureListener listener = new GestureDetector.SimpleOnGestureListener() {


        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            directionHelper.start();
            return true;
        }

        @Override
        public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            // Log.d("zzz", "onScroll()  distanceX = [" + distanceX + "], distanceY = [" + distanceY + "]");

            directionHelper.onScroll(distanceX, distanceY);
            return true;
        }

        @Override
        public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            directionHelper.onFling(velocityX, velocityY);
            return true;
        }

        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent e) {
//            Log.d("zzz", "onSingleTapUp() called ");
            return true;
        }
    };

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            directionHelper.moveOut();
        }
        return detector.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        for (int i = 0; i < getChildCount(); i++) {
            measureChild(getChildAt(i), widthMeasureSpec, heightMeasureSpec);
        }
    }


    final float scaleStep = 0.05f;
    final float translationXStep = 40;
    final float translationZStep = 5;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int pWidth = getWidth();
        int pHeight = getHeight();

        for (int i = 0; i < getChildCount(); i++) {

            View child = getChildAt(i);
            if (i >= adapter.getVisibleCount()) {
                child.setVisibility(GONE);
                continue;
            } else {
                child.setVisibility(VISIBLE);
            }
            int width = child.getMeasuredWidth();
            int height = child.getMeasuredHeight();
            int left = (pWidth - width) / 2;
            int top = (pHeight - height) / 2;
            child.layout(left, top, left + width, top + height);

        }
        applyTranslation(0);
    }

    private void applyTranslation(float outOffset) {
        int maxZ = (int) (getChildCount() * translationZStep);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            StackLayoutParams p = (StackLayoutParams) child.getLayoutParams();
            if (p.isDrag) {
                continue;
            }
            float offset = i - outOffset;

            child.setTranslationZ(maxZ - offset * translationZStep);
            float scale = (float) (1 - scaleStep * offset);
            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setTranslationX(translationXStep * offset);
        }
    }


    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new StackLayoutParams(getContext(), attrs);
    }

    public void setOutOffset(float offset) {
        applyTranslation(offset);
    }

    private static class StackLayoutParams extends LayoutParams {

        public StackLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        boolean isDrag = false;
        float outOffset = 0;
        Direction direction;
        boolean isBefore = false;
        int dataPosition = NO_SET;
        private static final int NO_SET = -1;

        public void reset() {
            isDrag = false;
            outOffset = 0;
            direction = null;
            isBefore = false;
        }

    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        canvas.drawText("child count=" + getChildCount(), (getWidth() >> 1) - 200, (getHeight() >> 1) + 200, logPaint);
    }
}
