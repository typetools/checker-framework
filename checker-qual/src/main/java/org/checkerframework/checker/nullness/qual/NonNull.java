package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeKind;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.qual.UpperBoundFor;

/**
 * If an expression's type is qualified by {@code @NonNull}, then the expression never evaluates to
 * {@code null}. (Unless the program has a bug; annotations specify intended behavior.)
 *
 * <p>For fields of a class, the {@link NonNull} annotation indicates that this field is never
 * {@code null} <em>after the class has been fully initialized</em>. For static fields, the {@link
 * NonNull} annotation indicates that this field is never {@code null} <em>after the containing
 * class is initialized</em>. "Fully initialized" essentially means that the Java constructor has
 * completed. See the <a
 * href="https://checkerframework.org/manual/#initialization-checker">Initialization Checker
 * documentation</a> for more details.
 *
 * <p>This annotation is rarely written in source code, because it is the default.
 *
 * @see Nullable
 * @see MonotonicNonNull
 * @checker_framework.manual #nullness-checker Nullness Checker
 * @checker_framework.manual #initialization-checker Initialization Checker
 * @checker_framework.manual #bottom-type the bottom type
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(MonotonicNonNull.class)
@DefaultQualifierInHierarchy
@QualifierForLiterals(LiteralKind.STRING)
@DefaultFor(TypeUseLocation.EXCEPTION_PARAMETER)
@UpperBoundFor(
    typeKinds = {
      TypeKind.PACKAGE,
      TypeKind.INT,
      TypeKind.BOOLEAN,
      TypeKind.CHAR,
      TypeKind.DOUBLE,
      TypeKind.FLOAT,
      TypeKind.LONG,
      TypeKind.SHORT,
      TypeKind.BYTE
    })
public @interface NonNull {}
