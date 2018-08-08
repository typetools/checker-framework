// package determinism;
//
// import java.util.*;
// import org.checkerframework.checker.determinism.qual.*;
//
// class Issue7 {
//    // Tests whether this.method is treated correctly.
//    int testDotThis() {
//        return this.returnZero();
//    }
//
//    // Tests whether super.method is treated correctly.
//    String testDotSuper() {
//        return super.toString();
//    }
//
//    int testWithoutThis() {
//        return returnZero();
//    }
//
//    int returnZero() {
//        return 0;
//    }
// }
