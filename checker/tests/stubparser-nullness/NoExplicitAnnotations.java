import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NoExplicitAnnotations {}

class NoExplicitAnnotationsSuper {
    @Nullable String method1() {
        return helper();
    }

    @Nullable String method2() {
        return helper();
    }

    @Nullable String method3() {
        return helper();
    }

    @Nullable String helper() {
        return null;
    }
}

class NoExplicitAnnotationsSub1 extends NoExplicitAnnotationsSuper {
    @Override
    String helper() {
        return "hello";
    }
}

class NoExplicitAnnotationsSub2 extends NoExplicitAnnotationsSuper {
    @Override
    String helper() {
        return "hello";
    }
}

class NoExplicitAnnotationsSub3 extends NoExplicitAnnotationsSuper {
    @Override
    String helper() {
        return "hello";
    }
}

class NoExplicitAnnotationsUse {
    @Nullable String nble = null;
    @NonNull String nn = "hello";

    void use(
            NoExplicitAnnotationsSub1 sub1,
            NoExplicitAnnotationsSub2 sub2,
            NoExplicitAnnotationsSub3 sub3) {
        nble = sub1.method1();
        nn = sub1.method1();
        nble = sub2.method2();
        nn = sub2.method2();
        nble = sub3.method3();
        // :: error: (assignment.type.incompatible)
        nn = sub3.method3();

        // :: error: (assignment.type.incompatible)
        nn = nble;
    }
}
