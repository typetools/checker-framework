// Test case for Issue 3447:
// https://github.com/typetools/checker-framework/issues/3447

public class Test {
    public void test() throws Exception {
        try {
            int[] myNumbers = {1};
            System.out.println(myNumbers[1]);
        } catch (Exception e) {
        }
    }
}
