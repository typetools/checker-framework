import checkers.javari.quals.*;

public class ReadOnlyConstructor {
    int i;

    ReadOnlyConstructor() @ReadOnly {
        this.i = 3;
    }
}
