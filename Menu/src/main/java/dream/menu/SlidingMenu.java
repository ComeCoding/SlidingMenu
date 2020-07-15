package dream.menu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

public class SlidingMenu extends LinearLayout {
    private static final String TAG = "SlidingMenu1";
    private float mLastMotionX;
    private float mLastMotionY;
    private int mMenuWith;
    private Scroller mScroller;
    private boolean isScrolled;
    private static boolean isDowning;
    private static List<dream.menu.SlidingMenu> mSlidingMenu = new ArrayList<>();
    private static boolean isInterceptParentScroll;
    private dream.menu.SlidingMenu.OnMenuMoveListener mMoveListener;



    public SlidingMenu(Context context) {
        super(context);
        init();
    }

    public SlidingMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SlidingMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化一些东西
     */
    private void init() {
        //设置为水平摆放
        this.setOrientation(LinearLayout.HORIZONTAL);
        //一个可以实现平滑滚动的辅助类
        mScroller = new Scroller(getContext());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        //内部控件摆放完成以后，获取菜单的宽度，布局中第一个View以后的所有View的宽度都会累加为菜单宽度
        //所以一般第一个View是一个ViewGroup且占满此布局的宽度
        int childCount = getChildCount();
        if (childCount > 1 && mMenuWith == 0) {
            for (int i = 1; i < childCount; i++) {
                mMenuWith += getChildAt(i).getWidth();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:

                //如果下面super完后，没有child需要事件，isDispatch为false（CLICKABLE除外)，所以这里
                // 不设为true的话，自身及child都拿不到down以后的事件，也就没有move和up事件了，而且我们
                // 需要侧滑事件move
                super.dispatchTouchEvent(ev);
                //如果有已经打开的菜单，则需要关闭，返回false让其此次事件全部失效
                //有时候一次down事件会触发两次这里，所以设置一道门，不故道为什么，一次事件只有一次down
                if (!isDowning) {
                    //如果down落在打开的菜单上面
                    if (isDownMyself()) {
                        //再次判断是在内容上面，还是菜单上面
                        if (getWidth() - mMenuWith > ev.getX() && ev.getX() >= 0) {
                            //点在了内容上面，关闭菜单
                            closeOldMenu();
                            //下面是个静态布尔值，用来拦截父容器滚动的
                            //使用静态的原因是：在调试的时候用的是非静态，发现父容器还是在滚动，我就在想即然
                            //我都拦截了怎么还在滚动呢，我想肯定是对象变了，针对这种情况所以用了静态
                            //RecyclerView上面发现的
                            isInterceptParentScroll = true;
                        }
                        //关门down不会进来了，来多了扰乱我
                        isDowning = true;
                    } else {
                        //点到以外的地方也要关闭菜单
                        if (!mSlidingMenu.isEmpty()) {
                            closeOldMenu();
                            //下面是个静态布尔值，用来拦截父容器滚动的
                            isInterceptParentScroll = true;
                        }
                    }
                    //初始化downX downY
                    mLastMotionX = ev.getX();
                    mLastMotionY = ev.getY();
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                //传给child move事件
                super.dispatchTouchEvent(ev);
                //如果有菜单打开的，且用手指down在了屏幕上关闭了菜单没有抬起却滑动了起来，我们认为这次事件是
                //不该有效的
                if (isInterceptParentScroll) {
                    //请求拦截父容器的滚动事件
                    super.requestDisallowInterceptTouchEvent(true);
                    //false此次事件不要再有后续了
                    return false;
                }
                //算出手指移动后的 X Y 与 按下的时候的 X Y 的差异
                int xDiff = (int) (ev.getX() - mLastMotionX);
                int yDiff = (int) (ev.getY() - mLastMotionY);
                //水平移动差异大于垂直移动差异才触发侧滑效果，并且菜单宽度不为0
                if (Math.abs(xDiff) > Math.abs(yDiff) && mMenuWith > 0) {
                    //如果父容器也可以滚动，设置父容器禁止滚动，避免造成冲突
                    super.requestDisallowInterceptTouchEvent(true);
                    //如果左滑，xDiff为负数，加个负号使其变正数，算出距离是否大于菜单宽度
                    if (getScrollX() + (-xDiff) > mMenuWith) {
                        //如果继续左滑让其始终保持在菜单宽度的位置
                        scrollTo(mMenuWith, 0);
                        //如果右滑，xDiff为正数，加个负号使其为负数，算出距离是否小于0，如果回到正常位置还在滑
                        //则让其保持在 0，0 位置
                    } else if (getScrollX() + (-xDiff) < 0) {
                        scrollTo(0, 0);
                    } else {
                        //在上述两种情况内，手指移动多少，菜单就移动多少
                        //xDiff加负号的原因：
                        // 首先view的左上角为0，0 如果view要往左滑，x要为正数，右滑要为负数
                        //左滑xDiff本来就为负数，所以--得正
                        scrollBy(-xDiff, 0);
                    }
                    //滚动监听
                    if (mMoveListener != null) {
                        mMoveListener.onMenuMove(getScrollX(), 0);
                    }


                    //移动一丁点初始化一下x,y，因为用的是scrollBy，如果一直用按下去的x,y会使diff累加爆增
                    mLastMotionX = ev.getX();
                    mLastMotionY = ev.getY();
                    //用来拦截child的up事件,自身触发了滚动事件了,所以这次事件child不能有up
                    isScrolled = true;
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                //没有产生move事件，发给child，此方法最后super
                isDowning = false;
                isInterceptParentScroll = false;
                //如果为true，说明本身产生了move情况，则自身要拦截，不应发给child
                if (isScrolled && getScrollX() != 0) {
                    //清除之前动画
                    mScroller.forceFinished(true);
                    if (getScrollX() > mMenuWith / 2) {
                        smoothScrollTo(getScrollX(), 0, mMenuWith - getScrollX(), 0, 200);
                        //菜单被打开时，将这个菜单对象添加进List
                        mSlidingMenu.add(this);
                    } else {
                        smoothScrollTo(getScrollX(), 0, -getScrollX(), 0, 200);
                        //手动关闭菜单时，将这个菜单对象移除
                        mSlidingMenu.remove(this);
                    }
                    isScrolled = false;
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 这个方法是配合下面的方法用的流程为：
     * startScroll();
     * invalidate();
     * 重写此方法
     * 实现一个平滑移动效果类似于 smoothScrollTo
     */
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    /**
     * 平滑滚动
     *
     * @param startX   起始x
     * @param startY   起始Y
     * @param dx       起始点到目标点的差异
     * @param dy       起始点到目标点的差异
     * @param duration 动画持续时间
     */
    public void smoothScrollTo(int startX, int startY, int dx, int dy, int duration) {
        mScroller.startScroll(startX, startY, dx, dy, duration);
        invalidate();
    }


    /**
     * 关闭菜单 非手动关闭菜单时触发
     */
    public static void closeOldMenu() {
        if (!mSlidingMenu.isEmpty()) {
            for (int i = 0; i < mSlidingMenu.size(); i++) {
                dream.menu.SlidingMenu slidingMenu;
                slidingMenu = mSlidingMenu.get(i);
                if (slidingMenu != null) {
                    int startX = slidingMenu.getScrollX();
                    int dx = -slidingMenu.getScrollX();
                    //平滑归位
                    slidingMenu.smoothScrollTo(startX, 0, dx, 0, 200);
                    //归位完成，移除此菜单对象
                    mSlidingMenu.remove(slidingMenu);
                }
            }
        }
    }

    /**
     * down是否落在了已经打开的菜单身上
     *
     * @return 是为true
     */
    private boolean isDownMyself() {
        for (int i = 0; i < mSlidingMenu.size(); i++) {
            if (this == mSlidingMenu.get(i)) {
                return true;
            }
        }
        return false;
    }

    public void setOnMenuMoveListener(dream.menu.SlidingMenu.OnMenuMoveListener moveListener) {
        mMoveListener = moveListener;
    }

    /**
     * 移动监听接口 dx 范围 0 --- 菜单的宽度
     */
    public interface OnMenuMoveListener {
        void onMenuMove(int dx, int dy);
    }

    public void setOnMenuClickListener(final dream.menu.SlidingMenu.OnMenuClickListener clickListener, int viewId) {
        if (clickListener != null) {
            findViewById(viewId).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onMenuClick(v);
                }
            });
        }
    }

    public interface OnMenuClickListener {
        void onMenuClick(View v);
    }


}
