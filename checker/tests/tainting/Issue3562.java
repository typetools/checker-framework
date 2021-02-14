import org.checkerframework.checker.tainting.qual.*;

public class Issue3562 {
    // This used to issue type.invalid.conflicting.annos
    @Tainted Issue3562.@Untainted Inner field;

    class Inner {}
}
