package com.cugkuan.flow;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        int[] a = new int[2];
        a[0] = 10;
        a[1] = 20;

        System.out.println(getSecondMin(a));
    }

    private int getSecondMin(int[] arr) {

        if (arr.length == 1){
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
}
