//import java.util.*;
//import org.checkerframework.checker.determinism.qual.*;
//
//public class TestTypeRefinementSort {
//    void testSort(@Det List<@Det Integer> detList, @Det Comparator<@Det Integer> cmp, @Det List<@Det Integer> check){
//        detList.sort(cmp);
//        System.out.println(detList.equals(check));
//    }
//    void testSort(@Det List<@Det Integer> detList, @NonDet Comparator<@Det Integer> cmp, @Det List<@Det Integer> check){
//        detList.sort(cmp);
//        System.out.println(detList.equals(check));
//    }
//    void testSort(@NonDet List<@Det Integer> detList, @NonDet Comparator<@Det Integer> cmp, @Det List<@Det Integer> check){
//        detList.sort(cmp);
//        System.out.println(detList.equals(check));
//    }
//    void testSort(@OrderNonDet List<@Det Integer> detList, @Det Comparator<@Det Integer> cmp, @Det List<@Det Integer> check){
//        detList.sort(cmp);
//        System.out.println(detList.equals(check));
//    }
//}
