package tests.defaulting;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.TypeUseLocation;
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
    @DefaultFor(TypeUseLocation.IMPLICIT_LOWER_BOUND)
    @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
    public static @interface LB_IMPLICIT {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(LB_TOP.class)
    @DefaultFor(TypeUseLocation.EXPLICIT_LOWER_BOUND)
    @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
    public static @interface LB_EXPLICIT {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)

    @SubtypeOf({LB_IMPLICIT.class, LB_EXPLICIT.class})
    @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
    public static @interface LB_BOTTOM {}

}
