package org.checkerframework.checker.resourceleak;

import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesMustCallForChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedOptions;

import java.util.LinkedHashSet;

import javax.tools.Diagnostic.Kind;

/**
 * The entry point for the Resource Leak Checker. This checker is a modifed {@link
 * CalledMethodsChecker} that checks that the must-call obligations of each expression (as computed
 * via the {@link org.checkerframework.checker.mustcall.MustCallChecker} have been fulfilled.
 */
@SupportedOptions({
    ResourceLeakChecker.COUNT_MUST_CALL,
    MustCallChecker.NO_CREATES_MUSTCALLFOR,
    MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP,
    MustCallChecker.NO_RESOURCE_ALIASES
})
public class ResourceLeakChecker extends CalledMethodsChecker {

    /**
     * Command-line option for counting how many must-call obligations were checked by the Resource
     * Leak Checker, and emitting the number after processing all files. Not of interest to most
     * users.
     */
    public static final String COUNT_MUST_CALL = "countMustCall";

    /**
     * The number of expressions with must-call obligations that were checked. Incremented only if
     * the {@link #COUNT_MUST_CALL} command-line option was supplied.
     */
    int numMustCall = 0;

    /**
     * The number of must-call-related errors issued. The count of verified must-call expressions is
     * the difference between this and {@link #numMustCall}.
     */
    int numMustCallFailed = 0;

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
                super.getImmediateSubcheckerClasses();

        if (this.processingEnv.getOptions().containsKey(MustCallChecker.NO_CREATES_MUSTCALLFOR)) {
            checkers.add(MustCallNoCreatesMustCallForChecker.class);
        } else {
            checkers.add(MustCallChecker.class);
        }

        return checkers;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new ResourceLeakVisitor(this);
    }

    @Override
    public void reportError(Object source, @CompilerMessageKey String messageKey, Object... args) {
        if (messageKey.equals("required.method.not.called")) {
            // This is safe because of the message key.
            String qualifiedTypeName = (String) args[1];
            // Only count classes in the JDK, not user-defined classes.
            if (MustCallConsistencyAnalyzer.isJdkClass(qualifiedTypeName)) {
                numMustCallFailed++;
            }
        }
        super.reportError(source, messageKey, args);
    }

    @Override
    public void typeProcessingOver() {
        if (hasOption(COUNT_MUST_CALL)) {
            message(Kind.WARNING, "Found %d must call obligation(s).%n", numMustCall);
            message(
                    Kind.WARNING,
                    "Successfully verified %d must call obligation(s).%n",
                    numMustCall - numMustCallFailed);
        }
        super.typeProcessingOver();
    }
}
