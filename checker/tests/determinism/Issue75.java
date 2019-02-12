import org.checkerframework.checker.determinism.qual.*;

public class Issue75 {
    String detReceiver1(@Det Issue75 this) {
        return null;
    }

    String detReceiver2(@Det Issue75 this, @Det String a) {
        return null;
    }

    static void testInstanceMethodReturn(@Det Issue75 a, @Det String b) {
        @Det String c = a.detReceiver1();
        @Det String d = a.detReceiver2(b);
    }
}
