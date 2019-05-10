package com.cugkuan.flow;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class KFlowLayout extends ViewGroup {


    private int mHorizalSpaceing = 10;

    private int mVerticalSpaceing = 10;

    private int mLineMax = 3;


    private KFAdapter mAdapter;


    private List<Node> mNodes = new LinkedList<>();


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
        mHorizalSpaceing = array.getDimensionPixelSize(R.styleable.KFlowLayout_kf_horizontalSpacing, 10);
        mVerticalSpaceing = array.getDimensionPixelOffset(R.styleable.KFlowLayout_kf_verticalSpacing, 10);
        mLineMax = array.getDimensionPixelOffset(R.styleable.KFlowLayout_kf_line_max_num, 3);
        if (mLineMax < 2) {
            mLineMax = 2;
        }
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
                Node node = new Node(mAdapter.getView(getContext(), KFlowLayout.this, i));
                mNodes.add(node);
                addView(node.getView());
            }
            invalidate();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
        }
    };

    public void setAdapter(KFAdapter adapter) {
        mAdapter = adapter;
        mAdapter.registerDataSetObserver(mDataSetObserver);
        mAdapter.notifyDataSetChanged();
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
        for (int i = 0; i < mNodes.size(); i++) {
            Node node = mNodes.get(i);
            View view = node.getView();
            usedWidth = usedWidth + view.getMeasuredWidth() + mHorizalSpaceing;
            //该换行了
            if (row > mLineMax || (usedWidth > widthSize && row > 0)) {
                row = 0;
                line++;
                usedWidth = getPaddingLeft() + getPaddingRight() + view.getMeasuredWidth() + mHorizalSpaceing;
            }
            node.setLine(line);
            node.setRow(row);
            row++;
        }
        /**
         * 第二次，对每一个元素，需要重新分配宽度
         */
        int height = 0;
        List<Node> temps = new ArrayList<>();
        int temLine = 0;
        for (int i = 0; i < mNodes.size(); i++) {
            Node node = mNodes.get(i);
            if (node.getLine() != temLine){
                height = height + dealNodesLine(widthSize, heightMeasureSpec, temps);
                temps.clear();
                temLine = node.line;
            }
            temps.add(node);
        }
        /**
         * 别忘了最后一行元素的处理
         */
        height = height + dealNodesLine(widthSize, heightMeasureSpec, temps);
        int resultHeight = height + mVerticalSpaceing * (temLine - 1) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(widthSize, resultHeight);
    }

    //剩余空间分配问题，这个算法是关键，这里暂时使用最简单的方法，剩余的平均分配

    private int dealNodesLine(int widthSize, int heightSpace, List<Node> nodes) {
        int usedWidth = widthSize - getPaddingLeft() - getPaddingRight() - (nodes.size() - 1) * mHorizalSpaceing;
        int[] space = new int[nodes.size()];
        for (int i = 0; i < space.length; i++) {
            int vWidth = nodes.get(i).getView().getMeasuredWidth();
            usedWidth = usedWidth - vWidth;
            space[i] = vWidth;
        }
        int[] result = getAllocation(space, usedWidth);
        int height = 0;
        for (int i = 0; i < nodes.size(); i++) {
            View view = nodes.get(i).getView();
            int widthS = MeasureSpec.makeMeasureSpec(result[i], MeasureSpec.EXACTLY);
            LayoutParams lp = view.getLayoutParams();
            view.measure(widthS, getChildMeasureSpec(heightSpace, 0, lp.height));
            height = Math.max(height, view.getMeasuredHeight());
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
            if (value < firstMin) //小于最小的元素 更新1和2
            {
                secondMin = firstMin;
                firstMin = value;
            } else if (value < secondMin && value != firstMin) //小于倒数二的 更新2
            {
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
        l = l + getPaddingLeft();
        t = t + getPaddingTop();
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
                t = t + mVerticalSpaceing + rowHeight;
                l = getPaddingLeft();
                rowHeight = view.getMeasuredHeight();
                view.layout(l, t, l + view.getMeasuredWidth(), t + view.getMeasuredHeight());
            }
            l = l + view.getMeasuredWidth() + mHorizalSpaceing;
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

    public static abstract class KFAdapter<T> {

        private final DataSetObservable mDataSetObservable = new DataSetObservable();

        abstract protected View getView(Context context, View parent, int position);

        abstract public T getItem();

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
