package org.checkerframework.checker.calledmethods;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.common.accumulation.AccumulationChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

import java.util.LinkedHashSet;

/**
 * The Called Methods Checker tracks the methods that have definitely been called on an object. One
 * common use case for the Called Methods Checker is to specify safe combinations of options to
 * builder or builder-like interfaces, preventing objects from being instantiated incompletely.
 */
@SuppressWarningsPrefix({
    // Preferred checkername.
    "calledmethods",
    // Deprecated checkernames, supported for backward compatibility.
    "builder",
    "object.construction",
    "objectconstruction"
})
@SupportedOptions({
    CalledMethodsChecker.USE_VALUE_CHECKER,
    CalledMethodsChecker.COUNT_FRAMEWORK_BUILD_CALLS,
    CalledMethodsChecker.DISABLE_BUILDER_FRAMEWORK_SUPPORTS,
    CalledMethodsChecker.DISABLE_RETURNS_RECEIVER
})
@StubFiles({"DescribeImages.astub", "GenerateDataKey.astub"})
public class CalledMethodsChecker extends AccumulationChecker {

    /**
     * If this option is supplied, count the number of analyzed calls to build() in supported
     * builder frameworks and print it when analysis is complete. Useful for collecting metrics.
     */
    public static final String COUNT_FRAMEWORK_BUILD_CALLS = "countFrameworkBuildCalls";

    /**
     * This option disables the support for (and therefore the automated checking of) code that uses
     * the given builder frameworks. Useful when a user **only** wants to enforce specifications on
     * custom builder objects (such as the AWS SDK examples).
     */
    public static final String DISABLE_BUILDER_FRAMEWORK_SUPPORTS =
            "disableBuilderFrameworkSupports";

    /**
     * If this option is supplied, use the Value Checker to reduce false positives when analyzing
     * calls to the AWS SDK.
     */
    public static final String USE_VALUE_CHECKER = "useValueChecker";

    /**
     * Some use cases for the Called Methods Checker do not involve checking fluent APIs, and in
     * those cases disabling the Returns Receiver Checker using this flag will make the Called
     * Methods Checker run much faster.
     */
    public static final String DISABLE_RETURNS_RECEIVER = "disableReturnsReceiver";

    /**
     * The number of calls to build frameworks supported by this invocation. Incremented only if the
     * {@link #COUNT_FRAMEWORK_BUILD_CALLS} option was supplied.
     */
    int numBuildCalls = 0;

    /** Never access this boolean directly. Call {@link #isReturnsReceiverDisabled()} instead. */
    private @MonotonicNonNull Boolean returnsReceiverDisabled = null;

    /**
     * Was the Returns Receiver Checker disabled on the command line?
     *
     * @return whether the -AdisableReturnsReceiver option was specified on the command line
     */
    private boolean isReturnsReceiverDisabled() {
        if (returnsReceiverDisabled == null) {
            returnsReceiverDisabled = hasOptionNoSubcheckers(DISABLE_RETURNS_RECEIVER);
        }
        return returnsReceiverDisabled;
    }

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
                super.getImmediateSubcheckerClasses();
        if (!isReturnsReceiverDisabled()) {
            checkers.add(ReturnsReceiverChecker.class);
        }
        // BaseTypeChecker#hasOption calls this method (so that all subcheckers' options are
        // considered), so the processingEnvironment must be checked for options directly.
        if (this.processingEnv.getOptions().containsKey(USE_VALUE_CHECKER)
                || this.processingEnv
                        .getOptions()
                        .containsKey(this.getClass().getSimpleName() + "_" + USE_VALUE_CHECKER)) {
            checkers.add(ValueChecker.class);
        }
        return checkers;
    }

    /**
     * Check whether the given alias analysis is enabled by this particular accumulation checker.
     *
     * @param aliasAnalysis the analysis to check
     * @return true iff the analysis is enabled
     */
    @Override
    public boolean isEnabled(AliasAnalysis aliasAnalysis) {
        if (aliasAnalysis == AliasAnalysis.RETURNS_RECEIVER) {
            return !isReturnsReceiverDisabled();
        }
        return super.isEnabled(aliasAnalysis);
    }

    @Override
    public void typeProcessingOver() {
        if (getBooleanOption(COUNT_FRAMEWORK_BUILD_CALLS)) {
            System.out.printf("Found %d build() method calls.%n", numBuildCalls);
        }
        super.typeProcessingOver();
    }
}
