//import java.util.ArrayList;
//import org.checkerframework.checker.determinism.qual.*;
//
//public class TestEquals {
//    void test1(@Det Node n, @Det Node m){
//        @Det boolean res = n.equals(m);
//    }
//    void test3(@Det Node n, @NonDet Node m){
//        // :: error: (assignment.type.incompatible)
//        @Det boolean res = n.equals(m);
//    }
//    void test7(@NonDet Node n, @Det Node m){
//        // :: error: (assignment.type.incompatible)
//        @Det boolean res = n.equals(m);
//    }
//    void test9(@NonDet Node n, @NonDet Node m){
//        // :: error: (assignment.type.incompatible)
//        @Det boolean res = n.equals(m);
//    }
//}
//
//class Node{
//    int data;
//    @Override
//    public @PolyDet boolean equals(@PolyDet Node this, @PolyDet Object o){
//        return this.data == ((Node)o).data;
//    }
//    @Override
//    public @NonDet int hashCode(@PolyDet Node this){
//        return data;
//    }
//}
