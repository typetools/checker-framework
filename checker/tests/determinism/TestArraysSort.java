// import java.util.*;
// import org.checkerframework.checker.determinism.qual.*;
//
// public class TestArraysSort {
//    void testSort(@Det int @Det [] a) {
//        Arrays.sort(a);
//    }
//
//    void testSort1(@Det int @OrderNonDet [] a) {
//        // :: error: (argument.type.incompatible)
//        System.out.println(a[0]);
////        System.out.println(a[0]);
//        Arrays.sort(a);
//    }
//
//    void testSort2(@Det Integer @OrderNonDet [] a) {
//        // :: error: (argument.type.incompatible)
//        System.out.println(a[0]);
//        IntComparator c = new IntComparator();
//        Arrays.sort(a, c);
//        System.out.println(a[0]);
//    }
// }
//
// class IntComparator implements Comparator<Integer> {
//     public @PolyDet IntComparator(){
//
//     }
//     public int compare(Integer i1, Integer i2) {
//         return 0;
//     }
// }
