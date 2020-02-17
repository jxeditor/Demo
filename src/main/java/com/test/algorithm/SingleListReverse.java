package com.test.algorithm;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ScriptEvaluator;

import java.lang.reflect.InvocationTargetException;

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

    public static void main(String[] args) throws CompileException, InvocationTargetException {
        ListNode a = new ListNode(1);
        ListNode b = new ListNode(2);
        ListNode c = new ListNode(3);
        ListNode d = new ListNode(4);
        a.next = b;
        b.next = c;
        c.next = d;

        ListNode result = reverseList(a);
        System.out.println(result.next.val);

        String value = "\"{'data': [-1, 2358513858109449, 1, 'ffffffff-d066-4f4b-ffff-ffffc64e4518', '1db8248e-2ca9-4337-9c8e-9765d3b21a63', null, '337956e4408101f716aefab6b0b7b0c4', 'f8ffc8c37ce4', 'QC_Reference_Phone,Xiaomi,armeabi-v7a,santoni,Xiaomi,Redmi 4X,santoni', 1576759226316], 'createTime': 1576759226316}\"";
        ScriptEvaluator se = new ScriptEvaluator();
        se.setReturnType(String.class);
        se.cook("import com.alibaba.fastjson.JSON;\n" +
                "        import com.alibaba.fastjson.JSONArray;\n" +
                "        import com.alibaba.fastjson.JSONObject;\n" +
                "        System.out.println(" + value + ");" +
                "        String valueData = " + value + ";\n" +
                "        JSONObject jsonObject = JSON.parseObject(valueData);\n" +
                "        JSONArray data = jsonObject.getJSONArray(\"data\");\n" +
                "        String adjustId = (String) data.get(3);\n" +
                "        return adjustId;");
        Object evaluate = se.evaluate(new Object[]{});
        System.out.println(evaluate.toString());
    }
}
