package checkers.interning.quals;

import checkers.interning.InterningChecker;
import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
@TypeQualifier
@SubtypeOf(Unqualified.class)
@ImplicitFor(
        treeClasses = { LiteralTree.class },
        typeClasses = { AnnotatedPrimitiveType.class })
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Interned {}
