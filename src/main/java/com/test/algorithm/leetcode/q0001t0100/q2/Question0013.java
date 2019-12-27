package com.test.algorithm.leetcode.q0001t0100.q2;

/**
 * @Author: xs
 * @Date: 2019-12-27 16:09
 * @Description: 罗马数字转整数
 */
public class Question0013 {
    public static void main(String[] args) {
        System.out.println(solution("LVIII"));
    }

    private static int solution(String str) {
        int[] moneys = new int[]{1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] moneyToStr = new String[]{"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int sum = 0;
        int index = 0;
        int start = 0;
        int length = str.length();
        while (length > 0) {
            if (str.substring(start).startsWith(moneyToStr[index])) {
                sum += moneys[index];
                length -= moneyToStr[index].length();
                start += moneyToStr[index].length();
            } else {
                index++;
            }
        }
        return sum;
    }
}
