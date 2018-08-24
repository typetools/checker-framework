package org.checkerframework.checker.determinism.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * A polymorphic qualifier for the determinism type system.
 *
 * <p>Any method written using {@code @PolyDet} conceptually has three versions: one in which every
 * instance of {@code @PolyDet} has been replaced by {@link NonDet}, one in which every instance of
 * {@code @PolyDet} has been replaced by {@link OrderNonDet}, and one in which every instance of
 * {@code @PolyDet} has been replaced by {@link Det}.
 *
 * @checker_framework.manual #determinism-checker Determinism Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@PolymorphicQualifier(NonDet.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultFor({TypeUseLocation.PARAMETER, TypeUseLocation.RETURN, TypeUseLocation.RECEIVER})
public @interface PolyDet {
    /**
     * Optionally, {@code @PolyDet} takes one of the three String values "up", "down" or "use".
     *
     * <p>If {@code @PolyDet} resolves to {@link OrderNonDet}, {@code @PolyDet("up")} gets replaced
     * by {@link NonDet}, and {@code @PolyDet("down")} by {@link Det}.
     *
     * <p>{@code @PolyDet("use")} should be the annotation on method parameters that are required to
     * have the same type as {@code @PolyDet} without affecting their instantiation. For example, a
     * method that is annotated as {@code void method_name (@PolyDet a, @PolyDet("use") b)} would
     * not allow the method invocation {@code method_name(@Det a, @NonDet b)}.
     */
    String value() default "";
}
