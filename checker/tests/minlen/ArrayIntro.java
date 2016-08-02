import org.checkerframework.checker.upperbound.qual.*;

class ArrayIntro {
    void test () {
        @MinLen(5) int[] arr = new int[5];
        int a = 9;
        a += 4;
        a--;
        @MinLen(12) int[] arr1 = new int[a];
        @MinLen(3) Integer[] arr2 = {1,2,3};
        //:: error: (assignment.type.incompatible)
        @MinLen(4) Integer[] arr3 = {4,5,6};
        //:: error: (assignment.type.incompatible)
        @MinLen(7) int[] arr4 = new int[4];
        //:: error: (assignment.type.incompatible)
        @MinLen(16) int[] arr5 = new int[a];
    }
}
