import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class ThisLiteralSimpler {
    public ThisLiteralSimpler() {
        super();
        @UnderInitialization @Raw ThisLiteralSimpler b = this;
        @UnderInitialization @Raw ThisLiteralSimpler a = ThisLiteralSimpler.this;
    }
}
