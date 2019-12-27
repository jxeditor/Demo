package com.test.algorithm.leetcode.q1t10;

/**
 * @Author: xs
 * @Date: 2019-12-19 15:26
 * @Description: 给出两个 非空 的链表用来表示两个非负的整数。其中，它们各自的位数是按照 逆序 的方式存储的，并且它们的每个节点只能存储一位数字。
 * <p>
 * 如果，我们将这两个数相加起来，则会返回一个新的链表来表示它们的和。
 * <p>
 * 您可以假设除了数字 0 之外，这两个数都不会以 0 开头。
 * <p>
 * 示例：
 * <p>
 * 输入：(2 -> 4 -> 3) + (5 -> 6 -> 4)
 * 输出：7 -> 0 -> 8
 * 原因：342 + 465 = 807
 * <p>
 */
public class Question0002 {
    public static void main(String[] args) {
        ListNode first = new ListNode(2);
        first.next = new ListNode(4);
        first.next.next = new ListNode(3);

        ListNode second = new ListNode(5);
        second.next = new ListNode(6);
        second.next.next = new ListNode(4);

        // 创建两个链表的原因,t会进行重置,而值还是赋在了result上
        ListNode result = new ListNode(0);
        ListNode t = result;
        int temp = 0;
        while (first != null || second != null) {
            // 以防长度不一样
            int f = first != null ? first.val : 0;
            int s = second != null ? second.val : 0;

            // 计算和值
            int sum = f + s + temp;
            // 和值大于2位数,temp会为1,参与下一层计算
            temp = sum / 10;

            // 填入数据,并重置t为下一节点
            t.next = new ListNode(sum % 10);
            t = t.next;

            if (first != null) first = first.next;
            if (second != null) second = second.next;
        }

        if (temp > 0) {
            t.next = new ListNode(temp);
        }

        System.out.println(result.next.next.next.val);
    }

    static class ListNode {
        int val;

        ListNode next;

        ListNode(int x) {
            val = x;
        }
    }
}

