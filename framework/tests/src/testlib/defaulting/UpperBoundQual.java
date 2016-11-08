package testlib.defaulting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/** Created by jburke on 9/29/14. */
public class UpperBoundQual {

    @DefaultQualifierInHierarchy
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf({})
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public static @interface UB_TOP {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(UB_TOP.class)
    @DefaultFor(TypeUseLocation.IMPLICIT_UPPER_BOUND)
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public static @interface UB_IMPLICIT {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(UB_TOP.class)
    @DefaultFor(TypeUseLocation.EXPLICIT_UPPER_BOUND)
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public static @interface UB_EXPLICIT {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf({UB_IMPLICIT.class, UB_EXPLICIT.class})
    @DefaultFor(TypeUseLocation.LOWER_BOUND)
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public static @interface UB_BOTTOM {}
}
