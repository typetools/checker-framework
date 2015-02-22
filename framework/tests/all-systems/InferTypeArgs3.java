
@SuppressWarnings("oigj") //this comes from the Immutability post annotator which adds bottom to Object types
                          //including upper bounds
class Test {
    void test() {
        java.util.Arrays.asList(new Integer(1), "");
    }
}
