package com.test.algorithm;

/**
 * @Author: xs
 * @Date: 2019-11-28 15:29
 * @Description: 单链表反转
 */
public class SingleListReverse {
    static class ListNode {
        int val;
        ListNode next;

        ListNode(int x) {
            val = x;
        }
    }

    /**
     * temp = b     a.next = null    prev = a    head = b
     * temp = c     b.next = a       prev = b    head = c
     * temp = d     c.next = b       prev = c    head = d
     * temp = null  d.next = c       prev = d    head = null
     *
     * @param head
     * @return
     */
    private static ListNode reverseList(ListNode head) {
        ListNode prev = null;
        while (head != null) {
            ListNode temp = head.next;
            head.next = prev;
            prev = head;
            head = temp;
        }
        return prev;
    }

    public static void main(String[] args) {
        ListNode a = new ListNode(1);
        ListNode b = new ListNode(2);
        ListNode c = new ListNode(3);
        ListNode d = new ListNode(4);
        a.next = b;
        b.next = c;
        c.next = d;

        ListNode result = reverseList(a);
        System.out.println(result.next.val);
    }
}
