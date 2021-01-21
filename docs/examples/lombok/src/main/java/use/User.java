package use;

import lib.Foo;

public class User {
    Foo demo() {
        return Foo.builder()
                .x(null) // ok
                .y(null) // error
                .build();
    }
}
