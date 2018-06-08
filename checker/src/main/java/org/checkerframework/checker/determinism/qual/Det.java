package org.checkerframework.checker.determinism.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.*;

/**
 * An expression of type {@code @Det} exvaluates to the same value on all executions. For
 * collections, the iteration order is also the same on all executions.
 *
 * @checker_framework.manual #determinism-checker Determinism Checker
 */
@Documented
//@ImplicitFor(
//        literals = {LiteralKind.STRING},
//        types = {
//                TypeKind.PACKAGE,
//                TypeKind.INT,
//                TypeKind.BOOLEAN,
//                TypeKind.CHAR,
//                TypeKind.DOUBLE,
//                TypeKind.FLOAT,
//                TypeKind.LONG,
//                TypeKind.SHORT,
//                TypeKind.BYTE
//        }
//)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({OrderNonDet.class})
@DefaultQualifierInHierarchy
public @interface Det {}
