package wildcards;

public class Iterate {
    void method(Iterable<? extends Object> files) {
        for (Object file : files) {
            file.getClass();
        }
    }
}
