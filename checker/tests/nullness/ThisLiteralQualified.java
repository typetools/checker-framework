import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class ThisLiteralQualified {
    public ThisLiteralQualified() {
        super();
        @UnderInitialization @Raw ThisLiteralQualified b = this;
        @UnderInitialization @Raw ThisLiteralQualified a = ThisLiteralQualified.this;
    }
}
