package checkers.reflection.quals;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This represents a Method object where the upper bound of the Class it is a
 * member of, the exact name of the method, and the number of parameters the
 * method takes, are known. One or more of these may be multiple values, in
 * which case this annotation contains all these possibilities linked by index,
 * such that className[i], methodName[i], params[i] is one possible method
 * signature.
 */
@TypeQualifier
@SubtypeOf({ UnknownMethod.class })
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE })
public @interface MethodVal {
    String[] className();

    String[] methodName();

    int[] params();
}