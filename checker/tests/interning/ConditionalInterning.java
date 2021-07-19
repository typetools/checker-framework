public class ConditionalInterning {
    int a, b, c;

    boolean cmp() {
        return (a > b ? a < c : a > c);
    }
}
