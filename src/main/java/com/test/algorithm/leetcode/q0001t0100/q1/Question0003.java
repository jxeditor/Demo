package com.test.algorithm.leetcode.q0001t0100.q1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Author: xs
 * @Date: 2019-12-19 16:03
 * @Description: 给定一个字符串，请你找出其中不含有重复字符的 最长子串 的长度。
 * <p>
 * 示例 1:
 * <p>
 * 输入: "abcabcbb"
 * 输出: 3
 * 解释: 因为无重复字符的最长子串是 "abc"，所以其长度为 3。
 * 示例 2:
 * <p>
 * 输入: "bbbbb"
 * 输出: 1
 * 解释: 因为无重复字符的最长子串是 "b"，所以其长度为 1。
 * 示例 3:
 * <p>
 * 输入: "pwwkew"
 * 输出: 3
 * 解释: 因为无重复字符的最长子串是 "wke"，所以其长度为 3。
 *      请注意，你的答案必须是 子串 的长度，"pwke" 是一个子序列，不是子串。
 */
public class Question0003 {
    public static void main(String[] args) {
        String str = "pwwkew";
        solution01(str);
        solution02(str);
        solution03(str);
        solution04(str);
    }

    private static void solution01(String str) {
        char[] chars = str.toCharArray();
        String result = "";
        int temp = 0;
        for (int i = 0; i < chars.length; i++) {
            for (int j = i; j < chars.length; j++) {
                if (!result.contains(chars[j] + "")) {
                    result += chars[j];
                } else {
                    result = "";
                }
                if (result.length() > temp) {
                    temp = result.length();
                }
            }
        }
        System.out.println(temp);
    }

    /**
     * 移除
     * set记录不重复的子串
     * 不含重复时,添加,并获取长度
     * 含重复时,将从开始删除,直到重复字符被删除
     *
     * @param s
     */
    private static void solution02(String s) {
        int n = s.length();
        Set<Character> set = new HashSet<>();
        int ans = 0, i = 0, j = 0;
        while (i < n && j < n) {
            if (!set.contains(s.charAt(j))) {
                set.add(s.charAt(j++));
                ans = Math.max(ans, j - i);
            } else {
                set.remove(s.charAt(i++));
            }
        }
        System.out.println(ans);
    }

    /**
     * 滑动
     * i记录出现重复字符时,上次出现时的子串长度
     * ans记录不重复子串的最长长度
     * 例如abcbc
     * 第二次出现b时,i为max(2,0),第二次出现c时,i为max(3,2)
     * 第二次出现b时,ans为max(3,2),第二次出现c时,ans为max(3,2)
     *
     * @param str
     */
    private static void solution03(String str) {
        int n = str.length(), ans = 0;
        int[] index = new int[128];
        for (int j = 0, i = 0; j < n; j++) {
            i = Math.max(index[str.charAt(j)], i);
            ans = Math.max(ans, j - i + 1);
            index[str.charAt(j)] = j + 1;
        }
        System.out.println(ans);
    }

    /**
     * 与solution02类似
     * map存放字符,与其对应的位置长度
     * 判断包含重复字符
     * i则为重复字符之前对应位置长度
     * ans为最长无重复字符长度
     *
     * @param s
     */
    private static void solution04(String s) {
        int n = s.length(), ans = 0;
        Map<Character, Integer> map = new HashMap<>();
        for (int j = 0, i = 0; j < n; j++) {
            if (map.containsKey(s.charAt(j))) {
                i = Math.max(map.get(s.charAt(j)), i);
            }
            ans = Math.max(ans, j - i + 1);
            map.put(s.charAt(j), j + 1);
        }
        System.out.println(ans);
    }
}
