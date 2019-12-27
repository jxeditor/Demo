package com.test.algorithm.leetcode.q0001t0100.q1;

/**
 * @Author: xs
 * @Date: 2019-12-20 10:32
 * @Description: 请你来实现一个 atoi 函数，使其能将字符串转换成整数。
 */
public class Question0008 {
    public static void main(String[] args) {
        System.out.println(solution("145d"));
    }

    public static int solution(String str) {
        if(str.isEmpty()) return 0;
        char[] mychar=str.toCharArray();
        long ans=0;
        int i=0,sign=1,n=str.length();
        while(i<n&&mychar[i]==' ') {
            i++;
        }
        if(i < n &&mychar[i]=='+') {
            i++;
        }
        else if(i < n &&mychar[i]=='-') {
            i++;
            sign =-1;
        }
        //重点：只管是数字的时候，其余取0
        while(i<n&&(mychar[i]>='0'&&mychar[i]<='9')) {
            if(ans!=(int)ans) {
                return (sign==1)?Integer.MAX_VALUE:Integer.MIN_VALUE;
            }
            ans=ans*10+mychar[i++]-'0';
        }

        if(ans!=(int)ans) {
            return (sign==1)?Integer.MAX_VALUE:Integer.MIN_VALUE;
        }

        return (int)(ans*sign);

    }
}
