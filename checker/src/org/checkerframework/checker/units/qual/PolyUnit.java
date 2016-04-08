package org.checkerframework.checker.units.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * A polymorphic qualifier for the units-of-measure type system implemented
 * by the Units Checker.
 * <p>
 *
 * Any method written using @PolyUnit conceptually has many versions:  in
 * each one, every instance of @PolyUnit has been replaced by a different
 * unit qualifier such as @kg (kilograms) or @h (hours).
 * <p>
 *
 * The following example shows how method <code>triplePolyUnit</code> can be
 * used to process either meters or seconds:
 *
 * <pre> {@literal @}PolyUnit int triplePolyUnit(@PolyUnit int amount) {
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
 *    //:: error: (assignment.type.incompatible)
 *   {@literal @}s int sec3 = triplePolyUnit(m1);
 *  }
 * </pre>
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@Documented
@PolymorphicQualifier(UnknownUnits.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyUnit { }
