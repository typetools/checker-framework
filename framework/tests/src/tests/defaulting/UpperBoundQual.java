package tests.defaulting;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.*;

/**
 * Created by jburke on 9/29/14.
 */
public class UpperBoundQual {


    @DefaultQualifierInHierarchy
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf({})
    @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
    public static @interface UB_TOP {}


    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(UB_TOP.class)
    @DefaultFor(DefaultLocation.IMPLICIT_UPPER_BOUNDS)
    @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
    public static @interface UB_IMPLICIT {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(UB_TOP.class)
    @DefaultFor(DefaultLocation.EXPLICIT_UPPER_BOUNDS)
    @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
    public static @interface UB_EXPLICIT {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)

    @SubtypeOf({UB_IMPLICIT.class, UB_EXPLICIT.class})
    @DefaultFor(DefaultLocation.LOWER_BOUNDS)
    @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
    public static @interface UB_BOTTOM {}
}
