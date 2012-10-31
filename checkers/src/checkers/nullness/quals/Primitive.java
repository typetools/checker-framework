package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.nullness.NullnessChecker;
import checkers.quals.ImplicitFor;
import checkers.quals.InvisibleQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;

import com.sun.source.tree.Tree;

/**
 * {@code @Primitive} is a type annotation internally used for primitive types.
 *
 * This annotation is not written in source code and only internally used.
 *
 * This annotation is associated with the {@link NullnessChecker}.
 *
 * @see Nullable
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// This qualifier is internal and cannot be explicitly written
@Target({})
@TypeQualifier
@InvisibleQualifier
/*
 * Subtyping for this qualifier is a bit tricky. Basically we want primitives to
 * be interchangeable with NonNull, to account for auto-boxing. If we made Primitive
 * a subtype of NonNull, some generic test cases would fail, as the Bottom of the
 * hierarchy would no longer be NonNull. Same problem if both are below LazyNonNull
 * on the same level.
 * Now I made NonNull <: Primitive <: LazyNonNull and special case that also
 * Primitive <: NonNull.
 * This seems to work correctly so far.
 */
@SubtypeOf( LazyNonNull.class )
@ImplicitFor(
    typeClasses = { AnnotatedPrimitiveType.class },
    trees = {
        // All literals except NULL_LITERAL and STRING_LITERAL:
        Tree.Kind.BOOLEAN_LITERAL,
        Tree.Kind.CHAR_LITERAL,
        Tree.Kind.DOUBLE_LITERAL,
        Tree.Kind.FLOAT_LITERAL,
        Tree.Kind.INT_LITERAL,
        Tree.Kind.LONG_LITERAL,
    })
public @interface Primitive { }
