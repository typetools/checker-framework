import org.checkerframework.checker.nullness.qual.*;

/*
 * The test checks annotations in multidimention arrays.
 * Each array dimention is beeing annotated with eather @Nullable or @NonNull
 * to check error is thrown if assignment type is incompatible on eather
 * array level.
 * Tests uses 3 dimentional arrays. Each annotaion combination is used once starting
 * with @Nullable [] @Nullable [] @Nullable [] and
 * ends with @NonNull [] @NonNull [] @NonNull [].
 *
 * Test has 8 methods that returns 3-dimentional arrays where each dimention is annotated
 * with eather @Nullable or @NonNull.
 *
 * Test containg 8 methods where all variables are beeing assign with one of arrays from
 * method that returns annotated arrays.
 *
 * Errors are expected if one or more array levels in a declaration are annotated with @NonNull, but
 * in assignment are annotated with @Nullable.
 */
public class MultidimentionalArrayAnnotationTest {

    int numb = 1;

    // Declared 8 3-dimentional variables.
    Object @Nullable [] @Nullable [] @Nullable [] obj1 = new Object[numb][numb][numb];
    Object @NonNull [] @Nullable [] @Nullable [] obj2 = new Object[numb][numb][numb];
    Object @Nullable [] @NonNull [] @Nullable [] obj3 = new Object[numb][numb][numb];
    Object @Nullable [] @Nullable [] @NonNull [] obj4 = new Object[numb][numb][numb];
    Object @NonNull [] @NonNull [] @Nullable [] obj5 = new Object[numb][numb][numb];
    Object @NonNull [] @Nullable [] @NonNull [] obj6 = new Object[numb][numb][numb];
    Object @Nullable [] @NonNull [] @NonNull [] obj7 = new Object[numb][numb][numb];
    Object @NonNull [] @NonNull [] @NonNull [] obj8 = new Object[numb][numb][numb];

    /*
     * Call to method 1 that returns Object @NonNull [] @NonNull [] @NonNull [].
     * Errors are not expected.
     */
    void callTomethod1() {
        obj1 = method1();
        obj2 = method1();
        obj3 = method1();
        obj4 = method1();
        obj5 = method1();
        obj6 = method1();
        obj7 = method1();
        obj8 = method1();
    }

    /*
     * Call to method 2 that returns Object @Nullable [] @NonNull [] @NonNull [].
     */
    void callTomethod2() {
        obj1 = method2();
        // :: error: (assignment.type.incompatible)
        obj2 = method2();
        obj3 = method2();
        obj4 = method2();
        // :: error: (assignment.type.incompatible)
        obj5 = method2();
        // :: error: (assignment.type.incompatible)
        obj6 = method2();
        obj7 = method2();
        // :: error: (assignment.type.incompatible)
        obj8 = method2();
    }

    /*
     * Call to method 3 that returns Object @NonNull [] @Nullable [] @NonNull [].
     */
    void callTomethod3() {
        obj1 = method3();
        obj2 = method3();
        // :: error: (assignment.type.incompatible)
        obj3 = method3();
        obj4 = method3();
        // :: error: (assignment.type.incompatible)
        obj5 = method3();
        obj6 = method3();
        // :: error: (assignment.type.incompatible)
        obj7 = method3();
        // :: error: (assignment.type.incompatible)
        obj8 = method3();
    }

    /*
     * Call to method 4 that returns Object @NonNull [] @NonNull [] @Nullable [].
     */
    void callTomethod4() {
        obj1 = method4();
        obj2 = method4();
        obj3 = method4();
        // :: error: (assignment.type.incompatible)
        obj4 = method4();
        obj5 = method4();
        // :: error: (assignment.type.incompatible)
        obj6 = method4();
        // :: error: (assignment.type.incompatible)
        obj7 = method4();
        // :: error: (assignment.type.incompatible)
        obj8 = method4();
    }

    /*
     * Call to method 5 that returns Object @Nullable [] @Nullable [] @NonNull [].
     */
    void callTomethod5() {
        obj1 = method5();
        // :: error: (assignment.type.incompatible)
        obj2 = method5();
        // :: error: (assignment.type.incompatible)
        obj3 = method5();
        obj4 = method5();
        // :: error: (assignment.type.incompatible)
        obj5 = method5();
        // :: error: (assignment.type.incompatible)
        obj6 = method5();
        // :: error: (assignment.type.incompatible)
        obj7 = method5();
        // :: error: (assignment.type.incompatible)
        obj8 = method5();
    }

    /*
     * Call to method 6 that returns Object @Nullable [] @NonNull [] @Nullable [].
     */
    void callTomethod6() {
        obj1 = method6();
        // :: error: (assignment.type.incompatible)
        obj2 = method6();
        obj3 = method6();
        // :: error: (assignment.type.incompatible)
        obj4 = method6();
        // :: error: (assignment.type.incompatible)
        obj5 = method6();
        // :: error: (assignment.type.incompatible)
        obj6 = method6();
        // :: error: (assignment.type.incompatible)
        obj7 = method6();
        // :: error: (assignment.type.incompatible)
        obj8 = method6();
    }

    /*
     * Call to method 7 that returns Object @NonNull [] @Nullable [] @Nullable [].
     */
    void callTomethod7() {
        obj1 = method7();
        obj2 = method7();
        // :: error: (assignment.type.incompatible)
        obj3 = method7();
        // :: error: (assignment.type.incompatible)
        obj4 = method7();
        // :: error: (assignment.type.incompatible)
        obj5 = method7();
        // :: error: (assignment.type.incompatible)
        obj6 = method7();
        // :: error: (assignment.type.incompatible)
        obj7 = method7();
        // :: error: (assignment.type.incompatible)
        obj8 = method7();
    }

    /*
     * Call to method 8 that returns Object @Nullable [] @Nullable [] @Nullable [].
     */
    void callTomethod8() {
        obj1 = method8();
        // :: error: (assignment.type.incompatible)
        obj2 = method8();
        // :: error: (assignment.type.incompatible)
        obj3 = method8();
        // :: error: (assignment.type.incompatible)
        obj4 = method8();
        // :: error: (assignment.type.incompatible)
        obj5 = method8();
        // :: error: (assignment.type.incompatible)
        obj6 = method8();
        // :: error: (assignment.type.incompatible)
        obj7 = method8();
        // :: error: (assignment.type.incompatible)
        obj8 = method8();
    }

    Object[][][] method1() {
        return new Object[numb][numb][numb];
    }

    Object @Nullable [][][] method2() {
        return new Object[numb][numb][numb];
    }

    Object[] @Nullable [][] method3() {
        return new Object[numb][numb][numb];
    }

    Object[][] @Nullable [] method4() {
        return new Object[numb][numb][numb];
    }

    Object @Nullable [] @Nullable [][] method5() {
        return new Object[numb][numb][numb];
    }

    Object @Nullable [][] @Nullable [] method6() {
        return new Object[numb][numb][numb];
    }

    Object[] @Nullable [] @Nullable [] method7() {
        return new Object[numb][numb][numb];
    }

    Object @Nullable [] @Nullable [] @Nullable [] method8() {
        return new Object[numb][numb][numb];
    }
}
