package checkers.interning;

import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import checkers.basetype.BaseTypeChecker;
import checkers.interning.quals.Interned;
import checkers.interning.quals.PolyInterned;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.source.SupportedLintOptions;

/**
 * A typechecker plug-in for the {@link Interned} qualifier that
 * finds (and verifies the absence of) equality-testing and interning errors.
 *
 * <p>
 *
 * The {@link Interned} annotation indicates that a variable
 * refers to the canonical instance of an object, meaning that it is safe to
 * compare that object using the "==" operator. This plugin warns whenever
 * "==" is used in cases where one or both operands are not
 * {@link Interned}.  Optionally, it suggests using "=="
 * instead of ".equals" where possible.
 *
 * @checker.framework.manual #interning-checker Interning checker
 */
@TypeQualifiers({ Interned.class, Unqualified.class,
    PolyInterned.class, PolyAll.class})
@SupportedLintOptions({"dotequals"})
@SupportedOptions({"checkclass"})
public final class InterningChecker extends BaseTypeChecker {

    /**
     * Returns the declared type of which the equality tests should be tested,
     * if the user explicitly passed one.  The user can pass the class name
     * via the {@code -Acheckclass=...} option.
     *
     * If no class is specified, or the class specified isn't in the
     * classpath, it returns null.
     *
     */
    DeclaredType typeToCheck() {
        String className = processingEnv.getOptions().get("checkclass");
        if (className == null) return null;

        TypeElement classElt = processingEnv.getElementUtils().getTypeElement(className);
        if (classElt == null) return null;

        return processingEnv.getTypeUtils().getDeclaredType(classElt);
    }
}
