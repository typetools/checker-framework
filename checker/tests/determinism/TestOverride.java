//import java.util.ArrayList;
//import org.checkerframework.checker.determinism.qual.*;
//
//public class TestOverride {
//    protected @PolyDet int mult(@PolyDet int a){
//        return a * a;
//    }
////    protected @PolyDet ArrayList<Integer> multList(@PolyDet int a){
////        return new ArrayList<Integer>(a);
////    }
//}
//
//class Child extends TestOverride{
//    @Override
//    protected @NonDet int mult(@Det int a) {
//        return 5;
//    }
//
////    @Override
////    protected @NonDet ArrayList<@Det Integer> multList(@Det int a) {
////        return new ArrayList<Integer>(a);
////    }
//}
