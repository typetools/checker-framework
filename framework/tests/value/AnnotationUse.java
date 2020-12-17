import org.checkerframework.common.value.qual.IntRange;

public class AnnotationUse {
    // :: error: (annotation.intrange.on.noninteger)
    @IntRange(to = 0) String s1;

    // :: error: (annotation.intrange.on.noninteger)
    @IntRange(from = 0) String s2;

    // Allowed on j.l.Object, because of possible boxing
    @IntRange(to = 0) Object o;
}
