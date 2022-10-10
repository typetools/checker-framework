// @above-java17-jdk-skip-test TODO: reinstate, false positives may be due to issue #979

package wildcards;

public class Iterate {
    void method(Iterable<? extends Object> files) {
        for (Object file : files) {
            file.getClass();
        }
    }
}
