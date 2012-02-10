package checkers.interning.quals;

import java.lang.annotation.*;

import checkers.interning.InterningChecker;
import checkers.quals.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;

import com.sun.source.tree.LiteralTree;

/**
 * Indicates that a variable has been interned, i.e., that the variable refers
 * to the canonical representation of an object.
 * <p>
 *
 * To specify that all objects of a given type are interned, annotate the class declaration:
 * <pre>
 *   public @Interned class MyInternedClass { ... }
 * </pre>
 * This is equivalent to annotating every use of MyInternedClass, in a
 * declaration or elsewhere.  For example, enum classes are implicitly so
 * annotated.
 * <p>
 *
 * This annotation is associated with the {@link InterningChecker}.
 *
 * @see InterningChecker
 * @checker.framework.manual #interning-checker Interning Checker
 */
@Documented
@TypeQualifier
@Inherited
@SubtypeOf(Unqualified.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@ImplicitFor(
    treeClasses={LiteralTree.class},
    typeClasses={AnnotatedPrimitiveType.class})
public @interface Interned {}
