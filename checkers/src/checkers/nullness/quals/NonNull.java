package checkers.nullness.quals;

import java.lang.annotation.*;

import javax.lang.model.type.TypeKind;

import checkers.nullness.NullnessChecker;
import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import com.sun.source.tree.Tree;

/**
 * {@code @NonNull} is a type annotation that indicates that a value is never null.
 * <p>
 *
 * When applied to a member field's type, indicates that the field is never
 * null after instantiation (construction) completes.  When applied to a
 * static field's type, indicates that the field is never null after the
 * containing class is initialized.
 * <p>
 *
 * This annotation is rarely written in source code, because it is the default.
 * No more than one of {@code @NonNull} and {@link Nullable} may be
 * written on a given type.
 * <p>
 *
 * This annotation is associated with the {@link NullnessChecker}.
 *
 * @see Nullable
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
// See note on subtyping in Primitive.java.
@SubtypeOf(Primitive.class)
@ImplicitFor(
    types = { TypeKind.PACKAGE },
    trees = { Tree.Kind.NEW_CLASS,
        Tree.Kind.NEW_ARRAY,
        Tree.Kind.PLUS,         // for String concatenation
        // The NULL_LITERAL is @Nullable and all primitive type literals are @Primitive
        Tree.Kind.STRING_LITERAL
    })
public @interface NonNull {}
