package com.cugkuan.flow;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 流式布局
 * 请注意，忽略了 元素的 margin 属性
 * 设置了也不起作用
 */
public class KFlowLayout extends ViewGroup {

    /**
     * 水平方向的距离
     */
    private int mHorizontalSpacing = 10;

    /**
     * 垂直方向元素的距离
     */
    private int mVerticalSpacing = 10;

    /**
     * 每一行的最大个数
     */
    private int mLineMax = 3;


    private KFAdapter mAdapter;


    private List<Node> mNodes = new LinkedList<>();


    /**
     * 自动调整
     */
    private boolean mAutoAdjust = true;
    //水平的分割线
    private Drawable mHorizontalDivider;
    private Drawable mVerticalDivider;

    public KFlowLayout(Context context) {
        super(context, null);
    }

    public KFlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public KFlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.KFlowLayout);
        mHorizontalSpacing = array.getDimensionPixelSize(R.styleable.KFlowLayout_kf_horizontalSpacing, 10);
        mVerticalSpacing = array.getDimensionPixelOffset(R.styleable.KFlowLayout_kf_verticalSpacing, 10);
        mLineMax = array.getInteger(R.styleable.KFlowLayout_kf_line_max_num, 3);
        if (mLineMax < 2) {
            mLineMax = 2;
        }
        mAutoAdjust = array.getBoolean(R.styleable.KFlowLayout_kf_adjust, true);
        mHorizontalDivider = array.getDrawable(R.styleable.KFlowLayout_kf_horizontalDivider);
        if (mHorizontalDivider != null) {
            setWillNotDraw(false);
        }
        mVerticalDivider = array.getDrawable(R.styleable.KFlowLayout_kf_verticalDivider);
        if (mVerticalDivider != null) {
            setWillNotDraw(false);
        }
        array.recycle();
    }

    private DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            removeAllViews();
            int size = mAdapter.getCount();
            if (size <= 0) {
                return;
            }
            for (int i = 0; i < size; i++) {
                View view = mAdapter.getView(getContext(), KFlowLayout.this, i);
                if (view.getVisibility() != GONE) {
                    Node node = new Node(view);
                    mNodes.add(node);
                    addView(node.getView());
                }
            }
            invalidate();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
        }
    };

    public void setAutoAdjust(boolean autoAdjust) {
        mAutoAdjust = autoAdjust;
    }

    public void setAdapter(KFAdapter adapter) {
        mAdapter = adapter;
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(mDataSetObserver);
            mAdapter.notifyDataSetChanged();
        } else {
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int row = 0;
        int line = 0;
        int usedWidth = getPaddingLeft() + getPaddingRight();
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        /**
         * 第一次确定每个View 的行数和列数
         */
        int height = 0;
        List<Node> lineNodes = new ArrayList<>();
        for (int i = 0; i < mNodes.size(); i++) {
            Node node = mNodes.get(i);
            View view = node.getView();
            usedWidth = usedWidth + view.getMeasuredWidth() + mHorizontalSpacing;

            if (mHorizontalDivider != null) {
                usedWidth = usedWidth + mHorizontalDivider.getIntrinsicWidth() + mHorizontalSpacing;
            }
            //该换行了
            if (row >= mLineMax || (usedWidth > widthSize && row > 0)) {
                //进行修正
                int correct = usedWidth - mHorizontalSpacing;
                if (mHorizontalDivider != null) {
                    correct = usedWidth - mHorizontalSpacing - mHorizontalDivider.getIntrinsicWidth();
                }
                if (correct > widthSize) {
                    row = 0;
                    line++;
                    usedWidth = getPaddingLeft() + getPaddingRight() + view.getMeasuredWidth() + mHorizontalSpacing;
                    //为上一行的元素，进行重新的宽度的分配计算
                    height = height + dealNodesLine(widthSize, heightMeasureSpec, lineNodes);
                    lineNodes.clear();
                }
            }
            node.setLine(line);
            node.setRow(row);
            row++;
            lineNodes.add(node);
        }
        /**
         * 别忘了最后一行元素的处理
         */
        height = height + dealNodesLine(widthSize, heightMeasureSpec, lineNodes);
        int resultHeight = height + getPaddingTop() + getPaddingBottom()
                + (mVerticalDivider == null ?
                mVerticalSpacing * line :
                (mVerticalSpacing + mVerticalDivider.getIntrinsicHeight()) * 2 * line);
        setMeasuredDimension(widthSize, resultHeight);
    }

    /**
     * 对一行中的所有元素的宽度进行重新计算和分配
     * <p>
     * 例如：有三个元素，其宽度分别为：
     * {1,4,1},其总的空间是 10;那么重新分配计算完成后的空间分配情况是：
     * {3,4,3};
     * 如果是初始空间是{1,4,2};那么分配完成的空间应该是：
     * {3,4,3}
     *
     * @param widthSize
     * @param heightSpace
     * @param nodes
     * @return
     */
    private int dealNodesLine(int widthSize, int heightSpace, List<Node> nodes) {

        if (nodes == null || nodes.isEmpty()) {
            return 0;
        }
        int height = 0;
        if (!mAutoAdjust) {
            for (Node node : nodes) {
                height = Math.max(height, node.getView().getMeasuredHeight());
            }
        } else {
            int usedWidth = widthSize - getPaddingLeft() - getPaddingRight();
            if (mHorizontalDivider != null) {
                usedWidth = usedWidth - (nodes.size() - 1) * (mHorizontalSpacing + mHorizontalDivider.getIntrinsicWidth() + mHorizontalSpacing);
            } else {
                usedWidth = usedWidth - (nodes.size() - 1) * mHorizontalSpacing;
            }
            int[] space = new int[nodes.size()];
            for (int i = 0; i < space.length; i++) {
                int vWidth = nodes.get(i).getView().getMeasuredWidth();
                usedWidth = usedWidth - vWidth;
                space[i] = vWidth;
            }
            int[] result = getAllocation(space, usedWidth);
            for (int i = 0; i < nodes.size(); i++) {
                View view = nodes.get(i).getView();
                int widthS = MeasureSpec.makeMeasureSpec(result[i], MeasureSpec.EXACTLY);
                LayoutParams lp = view.getLayoutParams();
                view.measure(widthS, getChildMeasureSpec(heightSpace, 0, lp.height));
                height = Math.max(height, view.getMeasuredHeight());
            }
        }
        return height;
    }

    /**
     * 空间分配的核心算法
     *
     * @param value
     * @param reduce
     * @return
     */
    private int[] getAllocation(int[] value, int reduce) {
        if (reduce <= 0) {
            return value;
        } else {
            int secondMin = getSecondMin(value);
            List<Integer> index = getWaitAllocationIndex(value);
            int min = getMin(value);
            int gab = secondMin - min;
            if (gab == 0) {
                //gab =0 表明这个是这个是平均分配
                int level = reduce / index.size();
                for (int i = 0; i < index.size(); i++) {
                    int p = index.get(i);
                    value[p] = value[p] + level;
                }
                return value;
            } else {
                if (gab * index.size() < reduce) {
                    for (int i = 0; i < index.size(); i++) {
                        int p = index.get(i);
                        value[p] = value[p] + gab;
                    }
                    return getAllocation(value, reduce - gab * index.size());
                } else {
                    int level = reduce / index.size();
                    for (int i = 0; i < index.size(); i++) {
                        int p = index.get(i);
                        value[p] = value[p] + level;
                    }
                    return getAllocation(value, 0);
                }
            }
        }
    }

    private List<Integer> getWaitAllocationIndex(int[] value) {
        int min = getMin(value);
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < value.length; i++) {
            if (value[i] == min) {
                list.add(i);
            }
        }
        return list;
    }

    private int getMin(int[] value) {
        int v = value[0];
        for (int i = 0; i < value.length; i++) {
            if (value[i] < v) {
                v = value[i];
            }
        }
        return v;
    }

    private int getSecondMin(int[] arr) {
        if (arr.length == 1) {
            return arr[0];
        }
        int firstMin = Integer.MAX_VALUE;   //第一小的元素  初始值设为int的最大取值
        int secondMin = Integer.MAX_VALUE;   //第二小的元素  初始值设为int的最大取值
        for (int value : arr) {
            if (value < firstMin) {
                //小于最小的元素 更新1和2
                secondMin = firstMin;
                firstMin = value;
            } else if (value < secondMin && value != firstMin) { //小于倒数二的 更新2
                secondMin = value;
            }
        }
        return secondMin;
    }

    private int getMax(int[] value) {
        int v = value[0];
        for (int i = 0; i < value.length; i++) {
            if (value[i] > v) {
                v = value[i];
            }
        }
        return v;
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        l = getPaddingLeft();
        t = getPaddingTop();
        int line = 0;
        int rowHeight = 0;
        for (int i = 0; i < mNodes.size(); i++) {
            Node node = mNodes.get(i);
            View view = node.getView();
            if (node.getLine() == line) {
                view.layout(l, t, l + view.getMeasuredWidth(), t + view.getMeasuredHeight());
                rowHeight = Math.max(rowHeight, view.getMeasuredHeight());
            } else {
                line = node.getLine();
                t = t + rowHeight +
                        (mVerticalDivider == null ?
                                mVerticalSpacing :
                                (mVerticalSpacing + mVerticalDivider.getIntrinsicHeight()) * 2);
                l = getPaddingLeft();
                rowHeight = view.getMeasuredHeight();
                view.layout(l, t, l + view.getMeasuredWidth(), t + view.getMeasuredHeight());
            }
            if (mHorizontalDivider != null) {
                l = l + view.getMeasuredWidth() + mHorizontalSpacing * 2 + mHorizontalDivider.getIntrinsicWidth();
            } else {
                l = l + view.getMeasuredWidth() + mHorizontalSpacing;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //这里主要是绘制分割线
        super.onDraw(canvas);
        int currentLine = 0;
        int currentLinMaxHeight = 0;
        int drawableTop = 0;
        for (int i = 0; i < mNodes.size(); i++) {
            Node node = mNodes.get(i);
            View view = node.getView();
            currentLinMaxHeight = Math.max(currentLinMaxHeight, view.getMeasuredHeight());
            Node next = null;
            if (i + 1 < mNodes.size()) {
                next = mNodes.get(i + 1);
            }
            //绘制水平的分割线
            if (mHorizontalDivider != null) {
                if (next != null && node.getLine() == next.getLine()) {
                    mHorizontalDivider.setBounds(view.getRight() + mHorizontalSpacing,
                            view.getTop(),
                            view.getRight() + mHorizontalSpacing + mHorizontalDivider.getIntrinsicWidth(),
                            view.getBottom());
                    mHorizontalDivider.draw(canvas);
                }
            }
            if (mVerticalDivider != null) {
                //绘制竖直方向的分割线
                int line = node.getLine();
                if (line != currentLine) {
                    drawableTop = drawableTop + currentLinMaxHeight + mVerticalSpacing;
                    mVerticalDivider.setBounds(getPaddingLeft(),
                            drawableTop,
                            getMeasuredWidth() - getPaddingRight(),
                            drawableTop + mVerticalDivider.getIntrinsicHeight());
                    mVerticalDivider.draw(canvas);
                    currentLinMaxHeight = 0;
                    currentLine = line;
                }
            }

        }
    }


    /**
     * 节点的暂存
     */
    static class Node {
        //行
        private int row;
        //列
        private int line;
        private View view;

        public Node(View view) {
            this.view = view;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }

        public View getView() {
            return view;
        }

        public void setView(View view) {
            this.view = view;
        }
    }

    public static abstract class KFAdapter {

        private final DataSetObservable mDataSetObservable = new DataSetObservable();

        abstract protected View getView(Context context, View parent, int position);

        abstract public int getCount();

        public void notifyDataSetChanged() {
            mDataSetObservable.notifyChanged();
        }

        /**
         * Register an observer that is called when changes happen to the data used by this adapter.
         *
         * @param observer the object that gets notified when the data set changes.
         */
        void registerDataSetObserver(DataSetObserver observer) {
            mDataSetObservable.registerObserver(observer);
        }

        /**
         * Unregister an observer that has previously been registered with this
         * adapter via {@link #registerDataSetObserver}.
         *
         * @param observer the object to unregister.
         */
        void unregisterDataSetObserver(DataSetObserver observer) {
            mDataSetObservable.unregisterObserver(observer);
        }

    }
}
