package org.checkerframework.checker.calledmethods;

import com.sun.source.tree.MethodInvocationTree;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.calledmethods.builder.BuilderFrameworkSupport;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.accumulation.AccumulationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This visitor implements the custom error message finalizer.invocation.invalid, and supports
 * counting the number of framework build calls.
 */
public class CalledMethodsVisitor extends AccumulationVisitor {

    /** Error message key for incorrect finalizer invocations. */
    public static final @CompilerMessageKey String FINALIZER_INVOCATION_INVALID =
            "finalizer.invocation.invalid";

    /**
     * Constructor that only calls super.
     *
     * @param checker the type-checker associated with this visitor
     */
    public CalledMethodsVisitor(final BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

        if (checker.getBooleanOption(CalledMethodsChecker.COUNT_FRAMEWORK_BUILD_CALLS)) {
            ExecutableElement element = TreeUtils.elementFromUse(node);
            for (BuilderFrameworkSupport builderFrameworkSupport :
                    ((CalledMethodsAnnotatedTypeFactory) getTypeFactory())
                            .getBuilderFrameworkSupports()) {
                if (builderFrameworkSupport.isBuilderBuildMethod(element)) {
                    ((CalledMethodsChecker) checker).numBuildCalls++;
                    break;
                }
            }
        }
        return super.visitMethodInvocation(node, p);
    }

    /**
     * Adds special reporting for method.invocation.invalid errors to turn them into
     * finalizer.invocation.invalid errors.
     */
    @Override
    protected void reportMethodInvocabilityError(
            MethodInvocationTree node, AnnotatedTypeMirror found, AnnotatedTypeMirror expected) {

        AnnotationMirror expectedCM = expected.getAnnotation(CalledMethods.class);
        if (expectedCM != null) {
            AnnotationMirror foundCM = found.getAnnotation(CalledMethods.class);
            Set<String> foundMethods =
                    foundCM == null
                            ? Collections.emptySet()
                            : new HashSet<>(atypeFactory.getAccumulatedValues(foundCM));
            List<String> expectedMethods = atypeFactory.getAccumulatedValues(expectedCM);
            StringJoiner missingMethods = new StringJoiner(" ");
            for (String expectedMethod : expectedMethods) {
                if (!foundMethods.contains(expectedMethod)) {
                    missingMethods.add(expectedMethod + "()");
                }
            }

            checker.reportError(node, FINALIZER_INVOCATION_INVALID, missingMethods.toString());
        } else {
            super.reportMethodInvocabilityError(node, found, expected);
        }
    }
}
