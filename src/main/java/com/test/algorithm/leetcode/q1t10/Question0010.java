package com.test.algorithm.leetcode.q1t10;

/**
 * @Author: xs
 * @Date: 2019-12-27 14:11
 * @Description: 给你一个字符串 s 和一个字符规律 p，请你来实现一个支持 '.' 和 '*' 的正则表达式匹配。
 * <p>
 * '.' 匹配任意单个字符
 * '*' 匹配零个或多个前面的那一个元素
 * 所谓匹配，是要涵盖 整个 字符串 s的，而不是部分字符串。
 * <p>
 * 说明:
 * <p>
 * s 可能为空，且只包含从 a-z 的小写字母。
 * p 可能为空，且只包含从 a-z 的小写字母，以及字符 . 和 *。
 */
public class Question0010 {
    public static void main(String[] args) {
        System.out.println(solution("aa", "."));
    }

    public static boolean solution(String text, String pattern) {
        if (pattern.isEmpty()) return text.isEmpty();
        boolean first_match = (!text.isEmpty() &&
                (pattern.charAt(0) == text.charAt(0) || pattern.charAt(0) == '.'));

        if (pattern.length() >= 2 && pattern.charAt(1) == '*') {
            return (solution(text, pattern.substring(2)) ||
                    (first_match && solution(text.substring(1), pattern)));
        } else {
            return first_match && solution(text.substring(1), pattern.substring(1));
        }
    }
}
