package org.checkerframework.checker.units.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * A polymorphic qualifier for the units-of-measure type system implemented by the Units Checker.
 *
 * <p>Any method written using @PolyUnit conceptually has many versions: in each one, every instance
 * of @PolyUnit has been replaced by a different unit qualifier such as @kg (kilograms) or @h
 * (hours).
 *
 * <p>The following example shows how method {@code triplePolyUnit} can be used to process either
 * meters or seconds:
 *
 * <pre><code>
 * {@literal @}PolyUnit int triplePolyUnit(@PolyUnit int amount) {
 *    return 3*amount;
 *  }
 *
 *  void testPolyUnit() {
 *   {@literal @}m int m1 = 7 * UnitsTools.m;
 *   {@literal @}m int m2 = triplePolyUnit(m1);
 *
 *   {@literal @}s int sec1 = 7 * UnitsTools.s;
 *   {@literal @}s int sec2 = triplePolyUnit(sec1);
 *
 *    // :: error: (assignment.type.incompatible)
 *   {@literal @}s int sec3 = triplePolyUnit(m1);
 *  }
 * </code></pre>
 *
 * @checker_framework.manual #units-checker Units Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(UnknownUnits.class)
public @interface PolyUnit {}
