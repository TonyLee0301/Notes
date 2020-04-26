package info.tonyle.leetcode;

import jdk.nashorn.internal.ir.LiteralNode;

/**
 * 给出两个 非空 的链表用来表示两个非负的整数。其中，它们各自的位数是按照 逆序 的方式存储的，并且它们的每个节点只能存储 一位 数字。
 *
 * 如果，我们将这两个数相加起来，则会返回一个新的链表来表示它们的和。
 *
 * 您可以假设除了数字 0 之外，这两个数都不会以 0 开头。
 *
 * 示例：
 *
 * 输入：(2 -> 4 -> 3) + (5 -> 6 -> 4)
 * 输出：7 -> 0 -> 8
 * 原因：342 + 465 = 807
 *
 * 来源：力扣（LeetCode）
 * 链接：https://leetcode-cn.com/problems/add-two-numbers
 * 著作权归领扣网络所有。商业转载请联系官方授权，非商业转载请注明出处。
 */
public class TwoNumAdd {
    public static ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        ListNode listNode = new ListNode(0);
        ListNode q = l1, p = l2, head = listNode;
        int carry = 0;
        while(q != null || p != null){
            int x = q != null ? q.val : 0;
            int y = p != null ? p.val : 0;
            int sum = carry + x + y;
            carry = sum / 10;
            head.next = new ListNode( sum % 10);
            head = head.next;
            q = q != null ? q.next : null;
            p = p != null ? p.next : null;
        }
        if(carry != 0){
            head.next = new ListNode(carry);
        }
        return listNode.next;
    }

    public static void main(String[] args) {
        ListNode tmp;
        ListNode q = new ListNode(2);
        tmp = q.next = new ListNode(4);
        tmp.next = new ListNode(3);
        ListNode p = new ListNode(5);
        tmp = p.next = new ListNode(6);
        tmp.next = new ListNode(4);
        System.out.println(addTwoNumbers(q,p));
    }

    private static class ListNode {
        int val;
        ListNode next;
        ListNode(int x){
            val = x;
        }

        @Override
        public String toString() {
            return "ListNode{" +
                    "val=" + val +
                    ", next=" + next +
                    '}';
        }
    }
}
