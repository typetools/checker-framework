package lib;

import lombok.Builder;
import org.checkerframework.checker.nullness.qual.Nullable;

@Builder
public class Foo {
    private @Nullable Integer x;
    private Integer y;

    void demo() {
        x = null; // ok
        y = null; // error
    }
}
