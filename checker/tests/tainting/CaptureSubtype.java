import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class CaptureSubtype {

    class MyGeneric<T extends @Tainted Number> {}

    class SubGeneric<T extends @Untainted Number> extends MyGeneric<T> {}

    class UseMyGeneric {
        SubGeneric<? extends @Tainted Object> wildcardUnbounded =
                new SubGeneric<@Untainted Number>();

        MyGeneric<?> wildcardOutsideUB = wildcardUnbounded;
        MyGeneric<? extends @Untainted Number> wildcardInsideUB2 = wildcardUnbounded;
    }
}
