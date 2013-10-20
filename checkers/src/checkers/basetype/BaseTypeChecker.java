package checkers.basetype;

/*>>>
import checkers.igj.quals.*;
*/

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifiers;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;
import checkers.types.TypeHierarchy;

import javacutils.AbstractTypeProcessor;
import javacutils.ErrorReporter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An abstract {@link SourceChecker} that provides a simple {@link
 * checkers.source.SourceVisitor} implementation for typical assignment and
 * pseudo-assignment checking of annotated types.  Pseudo-assignment checks
 * include method overriding checks, parameter passing, and method invocation.
 *
 * Most type-checker plug-ins will want to extend this class, instead of
 * {@link SourceChecker}.  Checkers that require annotated types but not
 * subtype checking (e.g. for testing purposes)
 * should extend {@link SourceChecker}.
 *
 * Non-type checkers (e.g. checkers to enforce coding
 * styles) should extend {@link SourceChecker} or {@link AbstractTypeProcessor}
 * directly; the Checker Framework is not designed for such checkers.
 *
 * <p>
 *
 * It is a convention that, for a type system Foo, the checker, the visitor,
 * and the annotated type factory are named as  <i>FooChecker</i>,
 * <i>FooVisitor</i>, and <i>FooAnnotatedTypeFactory</i>.  Some factory
 * methods uses this convention to construct the appropriate classes
 * reflectively.
 *
 * <p>
 *
 * {@code BaseTypeChecker} encapsulates a group for factories for various
 * representations/classes related the type system, mainly:
 * <ul>
 *  <li> {@link QualifierHierarchy}:
 *      to represent the supported qualifiers in addition to their hierarchy,
 *      mainly, subtyping rules</li>
 *  <li> {@link TypeHierarchy}:
 *      to check subtyping rules between <b>annotated types</b> rather than qualifiers</li>
 *  <li> {@link AnnotatedTypeFactory}:
 *      to construct qualified types enriched with implicit qualifiers
 *      according to the type system rules</li>
 *  <li> {@link BaseTypeVisitor}:
 *      to visit the compiled Java files and check for violations of the type
 *      system rules</li>
 * </ul>
 *
 * <p>
 *
 * Subclasses must specify the set of type qualifiers they support either by
 * annotating the subclass with {@link TypeQualifiers} or by overriding the
 * {@link BaseAnnotatedTypeFactory#getSupportedTypeQualifiers()} method.
 *
 * <p>
 *
 * If the specified type qualifiers are meta-annotated with {@link SubtypeOf},
 * this implementation will automatically construct the type qualifier
 * hierarchy. Otherwise, or if this behavior must be overridden, the subclass
 * may override the {@link BaseAnnotatedTypeFactory#createQualifierHierarchy()} method.
 *
 * @see checkers.quals
 */
public abstract class BaseTypeChecker extends SourceChecker {

    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */

    /**
     * Returns the appropriate visitor that type-checks the compilation unit
     * according to the type system rules.
     *
     * This implementation uses the checker naming convention to create the
     * appropriate visitor.  If no visitor is found, it returns an instance of
     * {@link BaseTypeVisitor}.  It reflectively invokes the constructor that
     * accepts this checker and the compilation unit tree (in that order)
     * as arguments.
     *
     * Subclasses have to override this method to create the appropriate
     * visitor if they do not follow the checker naming convention.
     *
     * @return the type-checking visitor
     */
    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        // Try to reflectively load the visitor.
        Class<?> checkerClass = this.getClass();

        while (checkerClass != BaseTypeChecker.class) {
            final String classToLoad =
                checkerClass.getName().replace("Checker", "Visitor")
                                      .replace("Subchecker", "Visitor");
            BaseTypeVisitor<?> result = invokeConstructorFor(classToLoad,
                    new Class<?>[] { BaseTypeChecker.class },
                    new Object[] { this });
            if (result != null)
                return result;
            checkerClass = checkerClass.getSuperclass();
        }

        // If a visitor couldn't be loaded reflectively, return the default.
        return new BaseTypeVisitor<BaseAnnotatedTypeFactory>(this);
    }


    // **********************************************************************
    // Misc. methods
    // **********************************************************************

    /**
     * Specify supported lint options for all type-checkers.
     */
    @Override
    public Set<String> getSupportedLintOptions() {
        Set<String> lintSet = new HashSet<String>(super.getSupportedLintOptions());
        lintSet.add("cast");
        lintSet.add("cast:redundant");
        lintSet.add("cast:unsafe");

        return Collections.unmodifiableSet(lintSet);
    }

    /**
     * Invokes the constructor belonging to the class
     * named by {@code name} having the given parameter types on the given
     * arguments. Returns {@code null} if the class cannot be found, or the
     * constructor does not exist or cannot be invoked on the given arguments.
     *
     * @param <T> the type to which the constructor belongs
     * @param name the name of the class to which the constructor belongs
     * @param paramTypes the types of the constructor's parameters
     * @param args the arguments on which to invoke the constructor
     * @return the result of the constructor invocation on {@code args}, or
     *         null if the constructor does not exist or could not be invoked
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeConstructorFor(String name,
            Class<?>[] paramTypes, Object[] args) {

        // Load the class.
        Class<T> cls = null;
        try {
            cls = (Class<T>) Class.forName(name);
        } catch (Exception e) {
            // no class is found, simply return null
            return null;
        }

        assert cls != null : "reflectively loading " + name + " failed";

        // Invoke the constructor.
        try {
            Constructor<T> ctor = cls.getConstructor(paramTypes);
            return ctor.newInstance(args);
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException) {
                Throwable err = t.getCause();
                String msg;
                if (err instanceof CheckerError) {
                    msg = err.getMessage();
                } else {
                    msg = err.toString();
                }
                ErrorReporter.errorAbort("InvocationTargetException when invoking constructor for class " + name +
                        "; Underlying cause: " + msg, t);
            } else {
                ErrorReporter.errorAbort("Unexpected " + t.getClass().getSimpleName() + " for " +
                        "class " + name +
                        " when invoking the constructor; parameter types: " + Arrays.toString(paramTypes),
                        // + " and args: " + Arrays.toString(args),
                        t);
            }
            return null; // dead code
        }
    }

}
