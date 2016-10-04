import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.minlen.qual.*;

class ArrayIntro {
    // Note that I had to put all these annotations in comments to prevent
    // the autoformatter from screwing with them.
    void test() {
        int /*@MinLen(5)*/[] arr = new int[5];
        int a = 9;
        a += 5;
        a -= 2;
        int /*@MinLen(12)*/[] arr1 = new int[a];
        int /*@MinLen(3)*/[] arr2 = {1, 2, 3};
        //:: error: (assignment.type.incompatible)
        int /*@MinLen(4)*/[] arr3 = {4, 5, 6};
        //:: error: (assignment.type.incompatible)
        int /*@MinLen(7)*/[] arr4 = new int[4];
        //:: error: (assignment.type.incompatible)
        int /*@MinLen(16)*/[] arr5 = new int[a];
    }

    // There are three expected failures in here. As soon as
    // list support is added, they should go away.
    void listToArray(@MinLen(10) List<String> arg) {
        //:: error: (assignment.type.incompatible)
        Object @MinLen(10) [] a1 = arg.toArray();
        //:: error: (assignment.type.incompatible)
        String @MinLen(10) [] a2 = arg.toArray(new String[0]);
    }

    void arrayToList(String @MinLen(10) [] arg) {
        @MinLen(10) List<String> lst = Arrays.asList(arg);
    }
}
