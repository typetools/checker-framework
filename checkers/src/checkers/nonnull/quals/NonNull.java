package checkers.nonnull.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.lang.model.type.TypeKind;

import checkers.initialization.InitializationChecker;
import checkers.nonnull.AbstractNullnessChecker;
import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;

import com.sun.source.tree.Tree;

/**
 * {@link NonNull} is a type annotation that indicates that an expression is
 * never {@code null}.
 *
 * <p>
 * For fields of a class, the {@link NonNull} annotation indicates that this
 * field is never {@code null}
 * <em>after the class has been fully initialized</em>. Class initialization is
 * controlled by the Freedom Before Commitment type system, see
 * {@link InitializationChecker} for more details.
 *
 * <p>
 * For static fields, the {@link NonNull} annotation indicates that this field
 * is never {@code null} <em>after the containing class is initialized</em>.
 *
 * <p>
 * This annotation is rarely written in source code, because it is the default.
 *
 * <p>
 * This annotation is associated with the {@link AbstractNullnessChecker}.
 *
 * @see Nullable
 * @see MonotonicNonNull
 * @see AbstractNullnessChecker
 */
@Documented
@TypeQualifier
@SubtypeOf(MonotonicNonNull.class)
@DefaultQualifierInHierarchy
@Retention(RetentionPolicy.RUNTIME)
@ImplicitFor(types = { TypeKind.PACKAGE }, typeClasses = { AnnotatedPrimitiveType.class }, trees = {
        Tree.Kind.NEW_CLASS,
        Tree.Kind.NEW_ARRAY,
        Tree.Kind.PLUS, // for String concatenation
        // All literals except NULL_LITERAL:
        Tree.Kind.BOOLEAN_LITERAL, Tree.Kind.CHAR_LITERAL,
        Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL,
        Tree.Kind.INT_LITERAL, Tree.Kind.LONG_LITERAL, Tree.Kind.STRING_LITERAL })
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface NonNull {
}
