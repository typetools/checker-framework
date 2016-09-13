import org.checkerframework.checker.upperbound.qual.*;

class ArrayIntro {
    void test() {
        @MinLen(5)
        int[] arr = new int[5];
        int a = 9;
        a += 4;
        a--;
        @MinLen(12)
        int[] arr1 = new int[a];
        @MinLen(3)
        Integer[] arr2 = {1, 2, 3};
        //:: error: (assignment.type.incompatible)
        @MinLen(4)
        Integer[] arr3 = {4, 5, 6};
        //:: error: (assignment.type.incompatible)
        @MinLen(7)
        int[] arr4 = new int[4];
        //:: error: (assignment.type.incompatible)
        @MinLen(16)
        int[] arr5 = new int[a];
    }

    void listToArray(@MinLen(10) List<String> arg) {
        Object @MinLen(10) [] a1 = arg.toArray();
        String @MinLen(10) [] a2 = arg.toArray(new String[0]);
    }

    void arrayToList(String @MinLen(10) [] arg) {
        @MinLen(10)
        List<String> lst = Arrays.asList(arg);
    }
}
