// package determinism;
//
// import java.util.*;
// import org.checkerframework.checker.determinism.qual.*;
//
// class Issue8 {
//    public static @PolyDet List<@PolyDet String> copyList(@PolyDet List<@PolyDet String> strings)
// {
//        @PolyDet List<@PolyDet String> copy;
//        copy = new @PolyDet ArrayList<@PolyDet String>();
//        for (String s : strings) {
//            copy.add(s);
//        }
//        return copy;
//    }
//
//    void checkPolyLocal(@PolyDet String a){
//        @PolyDet String c = new String("hello");
//        System.out.println(c);
//    }
//
// }
