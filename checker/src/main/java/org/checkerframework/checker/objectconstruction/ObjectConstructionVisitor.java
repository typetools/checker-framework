package org.checkerframework.checker.objectconstruction;

import com.sun.source.tree.MethodInvocationTree;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.objectconstruction.framework.FrameworkSupport;
import org.checkerframework.checker.objectconstruction.qual.CalledMethods;
import org.checkerframework.common.accumulation.AccumulationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This visitor implements the custom error message finalizer.invocation.invalid, and supports
 * counting the number of framework build calls.
 */
public class ObjectConstructionVisitor extends AccumulationVisitor {
    /**
     * Constructor matching super.
     *
     * @param checker the type-checker associated with this visitor
     */
    public ObjectConstructionVisitor(final BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

        if (checker.getBooleanOption(ObjectConstructionChecker.COUNT_FRAMEWORK_BUILD_CALLS)) {
            ExecutableElement element = TreeUtils.elementFromUse(node);
            for (FrameworkSupport frameworkSupport :
                    ((ObjectConstructionAnnotatedTypeFactory) getTypeFactory())
                            .getFrameworkSupports()) {
                if (frameworkSupport.isBuilderBuildMethod(element)) {
                    ((ObjectConstructionChecker) checker).numBuildCalls++;
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
            StringBuilder missingMethods = new StringBuilder();
            for (String expectedMethod : expectedMethods) {
                if (!foundMethods.contains(expectedMethod)) {
                    missingMethods.append(expectedMethod);
                    missingMethods.append("() ");
                }
            }

            checker.reportError(node, "finalizer.invocation.invalid", missingMethods.toString());
        } else {
            super.reportMethodInvocabilityError(node, found, expected);
        }
    }
}
