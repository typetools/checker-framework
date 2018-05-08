//import java.util.*;
//import org.checkerframework.checker.determinism.qual.*;
//
//public class TestPolyArgsRet {
//    //No parameters, no return
//    static void callee1(){
//
//    }
//    //One parameter, no return
//    void callee2(){
//        @Det TestPolyArgsRet tst = this;
//    }
//    //two parameters, no return
//    void callee3(int a){
//
//    }
//    //No parameters, return
//    static int callee4(){
//        return 200;
//    }
//    //One parameter, return
//    int callee5(){
//        return 500;
//    }
//    //two parameters, return
//    int callee6(int a){
//        return a;
//    }
//    //No receiver, one parameter, return
//    static int callee7(int x){
//        return x;
//    }
//
//    void detCaller(@Det TestPolyArgsRet DetObj, @Det int i){
//        TestPolyArgsRet.callee1();
//        DetObj.callee2();
//        DetObj.callee3(i);
//        @Det int x = TestPolyArgsRet.callee4();
//        @Det int y = DetObj.callee5();
//        @Det int s = DetObj.callee6(y);
//        @Det int a = TestPolyArgsRet.callee7(y);
//    }
//
//    void nonDetCaller (@NonDet TestPolyArgsRet NonDetObj, @NonDet int i){
//        TestPolyArgsRet.callee1();
//        NonDetObj.callee2();
//        NonDetObj.callee3(i);
//        @NonDet int z = TestPolyArgsRet.callee4();
//        @NonDet int w = NonDetObj.callee5();
//        @NonDet int t = NonDetObj.callee6(w);
//        @NonDet int b = TestPolyArgsRet.callee7(w);
//    }
//}
