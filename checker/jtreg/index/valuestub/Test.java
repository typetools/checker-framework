package valuestub;

import org.checkerframework.checker.index.qual.LengthOf;

@SupressWarnings("")
public class Test {
    public @LengthOf("this") int length() {
        return 1;
    }
}
