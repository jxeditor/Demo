package com.test.algorithm.leetcode.q0001t0100.q2;

/**
 * @Author: xs
 * @Date: 2019-12-27 15:39
 * @Description: 罗马数字包含以下七种字符： I， V， X， L，C，D 和 M。
 * <p>
 * 字符          数值
 * I             1
 * V             5
 * X             10
 * L             50
 * C             100
 * D             500
 * M             1000
 * 例如， 罗马数字 2 写做 II ，即为两个并列的 1。12 写做 XII ，即为 X + II 。 27 写做  XXVII, 即为 XX + V + II 。
 */
public class Question0012 {
    public static void main(String[] args) {
        System.out.println(solution(6));
    }

    private static String solution(int num) {
        StringBuilder stringBuilder = new StringBuilder();
        int[] moneys = new int[]{1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] moneyToStr = new String[]{"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int index = 0;
        while (num > 0) {
            if (num >= moneys[index]) {
                stringBuilder.append(moneyToStr[index]);
                num -= moneys[index];
                index--;
            }
            index++;
        }
        return stringBuilder.toString();
    }
}
