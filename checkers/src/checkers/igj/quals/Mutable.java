package checkers.igj.quals;

import java.lang.annotation.*;

import javax.lang.model.type.TypeKind;

import com.sun.source.tree.Tree;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Indicates that the annotated reference is an immutable reference to an
 * immutable object.
 *
 * An Immutable object cannot be modified. Its fields may be reassigned or
 * mutated only if they are explicitly marked as Mutable or Assignable.
 *
 * @checker.framework.manual #igj-checker IGJ Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(AssignsFields.class)
@DefaultQualifierInHierarchy
public @interface Mutable {}
