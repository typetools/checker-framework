class BasicTest {
    // Random code just to make sure that the aggregate design pattern
    // does not throw any exceptions
    Object field = new Object();
    String[] array = {"hello", "world"};
    void foo(int arg) {
        field = array[0];
        field.toString();
        int a = 1 + arg;
    }
}
