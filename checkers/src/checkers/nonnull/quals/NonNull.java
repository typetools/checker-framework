package checkers.nonnull.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.lang.model.type.TypeKind;

import checkers.nullness.NullnessChecker;
import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;

import com.sun.source.tree.Tree;

/**
 * {@code @NonNull} is a type annotation that indicates that a value is never
 * null.
 * <p>
 * 
 * When applied to a member field's type, indicates that the field is never null
 * after instantiation (construction) completes. When applied to a static
 * field's type, indicates that the field is never null after the containing
 * class is initialized.
 * <p>
 * 
 * This annotation is rarely written in source code, because it is the default.
 * No more than one of {@code @NonNull} and {@link Nullable} may be written on a
 * given type.
 * <p>
 * 
 * This annotation is associated with the {@link NullnessChecker}.
 * 
 * @see Nullable
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@TypeQualifier
@SubtypeOf(Nullable.class)
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
