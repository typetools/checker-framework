import java.util.Set;

public class Demo {
    void demo(Set<Short> s, short i) {
        s.remove(i - 1); // Error Prone error
        s.add(null); // Nullness Checker error
    }
}
