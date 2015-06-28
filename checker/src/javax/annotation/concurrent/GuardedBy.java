package javax.annotation.concurrent;


/*
 * Copyright (c) 2005 Brian Goetz
 * Released under the Creative Commons Attribution License
 *   (http://creativecommons.org/licenses/by/2.5)
 * Official home: http://www.jcip.net
 */

/*
 * Modified for use with the Checker Framework Lock Checker
 */

import java.lang.annotation.*;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;

/**
 * GuardedBy
 *
 * The field or method to which this annotation is applied can only be accessed
 * when holding a particular lock, which may be a built-in (synchronization)
 * lock, or may be an explicit java.util.concurrent.Lock.
 *
 * The argument determines which locks guard the annotated field or method: this :
 * The string literal "this" means that this field is guarded by the class in
 * which it is defined. class-name.this : For inner classes, it may be necessary
 * to disambiguate 'this'; the class-name.this designation allows you to specify
 * which 'this' reference is intended itself : For reference fields only; the
 * object to which the field refers. field-name : The lock object is referenced
 * by the (instance or static) field specified by field-name.
 * class-name.field-name : The lock object is reference by the static field
 * specified by class-name.field-name. method-name() : The lock object is
 * returned by calling the named nil-ary method. class-name.class : The Class
 * object for the specified class should be used as the lock object.
 */

// The JCIP annotation can be used on a field (in which case it corresponds
// to the Lock Checker's @GuardedBy annotation) or on a method (in which case
// it is a declaration annotation corresponding to the Lock Checker's @Holding
// annotation).

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.CONSTRUCTOR })
@PreconditionAnnotation(qualifier = LockHeld.class)
@PostconditionAnnotation(qualifier = LockHeld.class)
public @interface GuardedBy {
    /**
     * The Java expressions that need to be {@link LockHeld}.
     *
     * @see <a
     *      href="http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#java-expressions-as-arguments">Syntax
     *      of Java expressions</a>
     */
    String[] value();
}
