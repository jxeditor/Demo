package com.test.algorithm;

/**
 * s1为应该输出的字符串
 * s2为实际输出的字符串
 * 打印错误键
 *
 * @author XiaShuai on 2020/6/12.
 */
public class KeyBoard {
    public static void main(String[] args) {
        String s1 = "7_This_is_a_test".toUpperCase();
        String s2 = "_hs_s_a_es".toUpperCase();

        char[] c1 = s1.toCharArray();
        for (int i = 0; i < c1.length; i++) {
            if (!s2.contains("" + c1[i])) {
                System.out.println(c1[i]);
            }
        }


    }
}
