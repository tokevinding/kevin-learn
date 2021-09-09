package com.kevin.java.stack;

import java.util.Stack;

/**
 * @author kevin
 * @date 2021-09-01 18:21:41
 * @desc
 */
public class FaceStack {
    public static void main(String[] args) {
        Stack<Integer> stack = new Stack<>();
        stack.push(1);
        stack.push(2);
        System.out.println(stack.empty());
        System.out.println(stack.search(2));
        System.out.println(stack.pop());
        System.out.println(stack.peek());
        System.out.println(stack.pop());
    }
}
