package checkers.interning;

import checkers.basetype.BaseTypeChecker;
import checkers.interning.quals.Interned;
import checkers.interning.quals.PolyInterned;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.source.SupportedLintOptions;

import javax.annotation.processing.SupportedOptions;

/**
 * A type-checker plug-in for the {@link Interned} qualifier that
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
 * @checker.framework.manual #interning-checker Interning Checker
 */
@TypeQualifiers({ Interned.class, Unqualified.class,
    PolyInterned.class, PolyAll.class})
@SupportedLintOptions({"dotequals"})
@SupportedOptions({"checkclass"})
public final class InterningChecker extends BaseTypeChecker {
}
