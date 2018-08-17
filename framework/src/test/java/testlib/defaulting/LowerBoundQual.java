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

public class LowerBoundQual {

    @DefaultQualifierInHierarchy
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf({})
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public static @interface LbTop {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(LbTop.class)
    @DefaultFor(TypeUseLocation.IMPLICIT_LOWER_BOUND)
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public static @interface LbImplicit {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(LbTop.class)
    @DefaultFor(TypeUseLocation.EXPLICIT_LOWER_BOUND)
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public static @interface LbExplicit {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf({LbImplicit.class, LbExplicit.class})
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public static @interface LbBottom {}
}
