package org.checkerframework.checker.optional;

import java.util.LinkedHashSet;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A type-checker that prevents misuse of the {@link java.util.Optional} class.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
// Uncomment when the Checker Framework assumes Java 8: @RelevantJavaTypes(Optional.class)
public class OptionalChecker extends BaseTypeChecker {

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
                super.getImmediateSubcheckerClasses();
        checkers.add(NullnessChecker.class);
        return checkers;
    }
}
