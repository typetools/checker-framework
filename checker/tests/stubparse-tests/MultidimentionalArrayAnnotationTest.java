import org.checkerframework.checker.nullness.qual.*;
/*
 * This test reads two stub files (in addition to flow.astub):
 */
public class MultidimentionalArrayAnnotationTest {

    int numb = 1;

    // Call to method 1
    void callTomethod1() {
        String @Nullable [] @Nullable [] @Nullable [] obj1 = method1();
        String @NonNull [] @Nullable [] @Nullable [] obj2 = method1();
        String @Nullable [] @NonNull [] @Nullable [] obj3 = method1();
        String @Nullable [] @Nullable [] @NonNull [] obj4 = method1();
        String @NonNull [] @NonNull [] @Nullable [] obj5 = method1();
        String @NonNull [] @Nullable [] @NonNull [] obj6 = method1();
        String @Nullable [] @NonNull [] @NonNull [] obj7 = method1();
        String @NonNull [] @NonNull [] @NonNull [] obj8 = method1();
    }

    // Call to method 2
    void callTomethod2() {
        String @Nullable [] @Nullable [] @Nullable [] obj1 = method2();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @Nullable [] @Nullable [] obj2 = method2();
        String @Nullable [] @NonNull [] @Nullable [] obj3 = method2();
        String @Nullable [] @Nullable [] @NonNull [] obj4 = method2();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @Nullable [] obj5 = method2();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @Nullable [] @NonNull [] obj6 = method2();
        String @Nullable [] @NonNull [] @NonNull [] obj7 = method2();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @NonNull [] obj8 = method2();
    }

    // Call to method 3
    void callTomethod3() {
        String @Nullable [] @Nullable [] @Nullable [] obj1 = method3();
        String @NonNull [] @Nullable [] @Nullable [] obj2 = method3();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @NonNull [] @Nullable [] obj3 = method3();
        String @Nullable [] @Nullable [] @NonNull [] obj4 = method3();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @Nullable [] obj5 = method3();
        String @NonNull [] @Nullable [] @NonNull [] obj6 = method3();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @NonNull [] @NonNull [] obj7 = method3();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @NonNull [] obj8 = method3();
    }

    // Call to method 4
    void callTomethod4() {
        String @Nullable [] @Nullable [] @Nullable [] obj1 = method4();
        String @NonNull [] @Nullable [] @Nullable [] obj2 = method4();
        String @Nullable [] @NonNull [] @Nullable [] obj3 = method4();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @Nullable [] @NonNull [] obj4 = method4();
        String @NonNull [] @NonNull [] @Nullable [] obj5 = method4();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @Nullable [] @NonNull [] obj6 = method4();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @NonNull [] @NonNull [] obj7 = method4();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @NonNull [] obj8 = method4();
    }

    // Call to method 5
    void callTomethod5() {
        String @Nullable [] @Nullable [] @Nullable [] obj1 = method5();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @Nullable [] @Nullable [] obj2 = method5();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @NonNull [] @Nullable [] obj3 = method5();
        String @Nullable [] @Nullable [] @NonNull [] obj4 = method5();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @Nullable [] obj5 = method5();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @Nullable [] @NonNull [] obj6 = method5();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @NonNull [] @NonNull [] obj7 = method5();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @NonNull [] obj8 = method5();
    }

    // Call to method 6
    void callTomethod6() {
        String @Nullable [] @Nullable [] @Nullable [] obj1 = method6();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @Nullable [] @Nullable [] obj2 = method6();
        String @Nullable [] @NonNull [] @Nullable [] obj3 = method6();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @Nullable [] @NonNull [] obj4 = method6();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @Nullable [] obj5 = method6();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @Nullable [] @NonNull [] obj6 = method6();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @NonNull [] @NonNull [] obj7 = method6();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @NonNull [] obj8 = method6();
    }

    // Call to method 7
    void callTomethod7() {
        String @Nullable [] @Nullable [] @Nullable [] obj1 = method7();
        String @NonNull [] @Nullable [] @Nullable [] obj2 = method7();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @NonNull [] @Nullable [] obj3 = method7();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @Nullable [] @NonNull [] obj4 = method7();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @Nullable [] obj5 = method7();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @Nullable [] @NonNull [] obj6 = method7();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @NonNull [] @NonNull [] obj7 = method7();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @NonNull [] obj8 = method7();
    }

    // Call to method 8
    void callTomethod8() {
        String @Nullable [] @Nullable [] @Nullable [] obj1 = method8();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @Nullable [] @Nullable [] obj2 = method8();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @NonNull [] @Nullable [] obj3 = method8();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @Nullable [] @NonNull [] obj4 = method8();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @Nullable [] obj5 = method8();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @Nullable [] @NonNull [] obj6 = method8();
        //:: error: (assignment.type.incompatible)
        String @Nullable [] @NonNull [] @NonNull [] obj7 = method8();
        //:: error: (assignment.type.incompatible)
        String @NonNull [] @NonNull [] @NonNull [] obj8 = method8();
    }

    String[][][] method1() {
        return new String[numb][numb][numb];
    };

    String @Nullable [][][] method2() {
        return new String[numb][numb][numb];
    };

    String[] @Nullable [][] method3() {
        return new String[numb][numb][numb];
    };

    String[][] @Nullable [] method4() {
        return new String[numb][numb][numb];
    };

    String @Nullable [] @Nullable [][] method5() {
        return new String[numb][numb][numb];
    };

    String @Nullable [][] @Nullable [] method6() {
        return new String[numb][numb][numb];
    };

    String[] @Nullable [] @Nullable [] method7() {
        return new String[numb][numb][numb];
    };

    String @Nullable [] @Nullable [] @Nullable [] method8() {
        return new String[numb][numb][numb];
    };
}
