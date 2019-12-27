package com.test.algorithm.leetcode.q0001t0100.q2;

/**
 * @Author: xs
 * @Date: 2019-12-27 15:23
 * @Description: 给定 n 个非负整数 a1，a2，...，an，每个数代表坐标中的一个点 (i, ai) 。在坐标内画 n 条垂直线，垂直线 i 的两个端点分别为 (i, ai) 和 (i, 0)。找出其中的两条线，使得它们与 x 轴共同构成的容器可以容纳最多的水。
 * <p>
 * 说明：你不能倾斜容器，且 n 的值至少为 2。
 */
public class Question0011 {
    public static void main(String[] args) {
//        int[] arr = {1, 8, 6, 2, 5, 4, 8, 3, 7};
        int[] arr = {7, 3, 8, 4, 5, 2, 6, 8, 1};
        System.out.println(solution01(arr));
        System.out.println(solution02(arr));

    }
    public static int solution01(int[] height) {
        int maxarea = 0;
        for (int i = 0; i < height.length; i++)
            for (int j = i + 1; j < height.length; j++)
                maxarea = Math.max(maxarea, Math.min(height[i], height[j]) * (j - i));
        return maxarea;
    }


    public static int solution02(int[] height) {
        int maxarea = 0, l = 0, r = height.length - 1;
        while (l < r) {
            maxarea = Math.max(maxarea, Math.min(height[l], height[r]) * (r - l));
            if (height[l] < height[r])
                l++;
            else
                r--;
        }
        return maxarea;
    }

}
