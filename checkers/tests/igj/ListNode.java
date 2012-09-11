import checkers.igj.quals.*;

@I
public class ListNode {
    @I ListNode prev;
    @I ListNode next;

    public ListNode(@AssignsFields ListNode this, int length) {
        if (length == 0) {
            next = this;
            prev = this;
        } else {
            next = new @I ListNode(this, this, length - 1);
            @I ListNode temp = next;
            while (temp.next != this) temp = temp.next;
            prev = temp;
        }
    }

    public ListNode(@I ListNode first, @I ListNode prev, int length) {
        this.prev = prev;
        this.next = (length == 0) ? first : new @I ListNode(first, this, length - 1);
    }

    public static void main(String[] args) {
        @Mutable ListNode mutableList = new @Mutable ListNode(3);
        {
            @Mutable ListNode n1 = mutableList.next;
            @Immutable ListNode n2 = mutableList.next;  // should emit error
        }

        @Immutable ListNode immutableList = new @Immutable ListNode(3);
        {
            @Mutable ListNode m1 = immutableList.next;    // should emit error
            @Immutable ListNode m2 = immutableList.next;
        }
    }
}
