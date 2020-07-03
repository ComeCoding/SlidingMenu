package dream.menu;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

public class SlidingMenu extends ViewGroup {
    private float mLastMotionX;
    private float mOldMotionX;
    private int mScrollX;
    private Opposite mOpposite;
    private Scroller mScroller;
    private int mMenuWidth;

    private enum Opposite {
        NORMAL, LEFT, RIGHT
    }


    public SlidingMenu(Context context) {
        this(context, null);
    }

    public SlidingMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mScroller = new Scroller(getContext());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.setClickable(true);
        int measureWidth = 0;
        int measureHeight = 0;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.setClickable(true);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            if (i > 0) {
                mMenuWidth += child.getMeasuredWidth();
            } else {
                measureWidth = getChildAt(i).getMeasuredWidth();
            }
            measureHeight = Math.max(measureHeight, getChildAt(i).getMeasuredHeight());
        }
        setMeasuredDimension(measureWidth, measureHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int childCount = getChildCount();
        int left = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.layout(left, 0, left + child.getMeasuredWidth(), child.getMeasuredHeight());
            left += child.getMeasuredWidth();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean isDispatch = super.dispatchTouchEvent(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = ev.getX();
                mOldMotionX = ev.getX();
                mScrollX = getScrollX();
                mOpposite = Opposite.NORMAL;
                mMenuWidth = 200;
                break;
            case MotionEvent.ACTION_MOVE:
                float xDiff = 0;
                if (ev.getX() < mOldMotionX) {
                    if (mOpposite != Opposite.NORMAL && mOpposite != Opposite.LEFT) {
                        mLastMotionX = mOldMotionX;
                        mScrollX = getScrollX();
                    }
                    xDiff = mScrollX + (mLastMotionX - ev.getX());
                    if (xDiff >= mMenuWidth) {
                        xDiff = mMenuWidth;
                    }
                    mOpposite = Opposite.LEFT;
                } else if (ev.getX() > mOldMotionX) {
                    if (mOpposite != Opposite.NORMAL && mOpposite != Opposite.RIGHT) {
                        mLastMotionX = mOldMotionX;
                        mScrollX = getScrollX();
                    }
                    xDiff = mScrollX - (ev.getX() - mLastMotionX);
                    if (xDiff <= 0) {
                        xDiff = 0;
                    }
                    mOpposite = Opposite.RIGHT;
                }
                mOldMotionX = ev.getX();
                performScrollTo((int) xDiff, getScrollY());
                break;
            case MotionEvent.ACTION_UP:
                int dx = getScrollX() > mMenuWidth / 2 ? mMenuWidth - getScrollX() : -getScrollX();
                performSmoothScrollTo(getScrollX(), getScrollY(), dx, getScrollY(), 200);
                break;
        }
        return isDispatch;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean isIntercept = super.onInterceptTouchEvent(ev);
        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            isIntercept = true;
        }
        return isIntercept;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            performScrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    private void performScrollTo(int scrollX, int scrollY) {
        scrollTo(scrollX, scrollY);
    }

    private void performSmoothScrollTo(int startX, int startY, int dx, int dy, int duration) {
        mScroller.forceFinished(true);
        mScroller.startScroll(startX, startY, dx, dy, duration);
        invalidate();
    }

}
