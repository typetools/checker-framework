import org.checkerframework.checker.index.qual.LessThan;

class Issue2613 {

    void demo() {
        require_lt(0, Integer.MAX_VALUE);
    }

    void require_lt(@LessThan("#2") int a, int b) {}
}
