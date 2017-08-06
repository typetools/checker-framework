import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.common.value.qual.MinLen;
/*
 * This test reads two stub files (in addition to flow.astub):
 */
public class MultidimentionalArrayAnnotationTest {

    int numb = 1;

    void method() {
        String @MinLen(2) [] @MinLen(2) [] @MinLen(2) [] obj0 = method0();
        String @Nullable [] @MinLen(2) [] @MinLen(2) [] obj1 = method0();
        String @MinLen(2) [] @Nullable [] @MinLen(2) [] obj2 = method0();
        String @MinLen(2) [] @MinLen(2) [] @Nullable [] obj3 = method0();
        String @Nullable [] @Nullable [] @MinLen(2) [] obj4 = method0();
        String @Nullable [] @MinLen(2) [] @Nullable [] obj5 = method0();
        String @MinLen(2) [] @Nullable [] @Nullable [] obj6 = method0();
        String @Nullable [] @Nullable [] @Nullable [] obj7 = method0();
    }

    String @Nullable [] @Nullable [] @MinLen(1) [] method0() {
        return new String[numb][numb][numb];
    };

    String @Nullable [][][] method1() {
        return new String[numb][numb][numb];
    };
    //    String[]@Nullable[][] method2(){};
    //    String[][]@Nullable[] method3(){};
    //    String @Nullable[]@Nullable[][] method4(){};
    //    String @Nullable[][]@Nullable[] method5(){};
    //    String []@Nullable[]@Nullable[] method6(){};
    //    String @Nullable[]@Nullable[]@Nullable[] method7(){};
}
