package com.test.algorithm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Author: xs
 * @Date: 2019-11-27 15:21
 * @Description: 偷金问题
 */
public class StealGold {

    private static int stole(int[] array) {
        // 3种状态: 偷0个,偷1个,偷2个
        int[][] dp = new int[3][array.length + 1];
        for (int i = 0; i < dp.length; i++) {
            for (int j = 0; j < dp[0].length; j++) {
                dp[i][j] = -1;
            }
        }

        // calculate(金子分布,状态二维数组,已连偷个数,剩余个数)
        calculate(array, dp, 0, array.length);
        return dp[0][array.length];
    }

    private static int calculate(int[] array, int[][] dp, int front, int remaining) {
        // 如果没有剩余,返回0
        if (remaining == 0) return 0;
        // 如果状态值已经被赋值,则返回
        if (dp[front][remaining] != -1) return dp[front][remaining];

        // left为偷,right为不偷
        int left = -1;
        if (front < 2) {
            left = array[array.length - remaining] + calculate(array, dp, front + 1, remaining - 1);
        }
        int right = calculate(array, dp, 0, remaining - 1);
        // 取left和right比较的最大值
        dp[front][remaining] = Math.max(left, right);
        return dp[front][remaining];
    }

    public static void main(String[] args) {
        int[] arr = {1, 3, 5, 5, 10};
        System.out.println(stole(arr));
    }
}
