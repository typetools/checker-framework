package tests.defaulting;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.*;

public class LowerBoundQual {

    @DefaultQualifierInHierarchy
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf({})
    @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
    public static @interface LB_TOP {}


    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(LB_TOP.class)
    @DefaultFor(DefaultLocation.IMPLICIT_LOWER_BOUNDS)
    @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
    public static @interface LB_IMPLICIT {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(LB_TOP.class)
    @DefaultFor(DefaultLocation.EXPLICIT_LOWER_BOUNDS)
    @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
    public static @interface LB_EXPLICIT {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)

    @SubtypeOf({LB_IMPLICIT.class, LB_EXPLICIT.class})
    @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
    public static @interface LB_BOTTOM {}

}
