package org.checkerframework.framework.type;

/*>>>
import org.checkerframework.checker.interning.qual.*;
*/

import com.sun.source.tree.ClassTree;
import java.util.Collection;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

/**
 * A "general" annotated type factory that supports qualifiers from any type hierarchy. One big
 * limitation is that it does not support annotations coming from a stub file.
 */
public class GeneralAnnotatedTypeFactory extends AnnotatedTypeFactory {

    public GeneralAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    public void postProcessClassTree(ClassTree tree) {
        // Do not store the qualifiers determined by this factory.
        // This factory adds declaration annotations as type annotations,
        // because TypeFromElement needs to read declaration annotations
        // and this factory blindly supports all annotations.
        // When storing those annotation to bytecode, the compiler chokes.
        // See testcase tests/nullness/GeneralATFStore.java
    }

    /** Return true to support any qualifier. No handling of aliases. */
    @Override
    public boolean isSupportedQualifier(AnnotationMirror a) {
        return true;
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new GeneralQualifierHierarchy(factory);
    }
}

/**
 * A very limited QualifierHierarchy that is used for access to qualifiers from different type
 * systems.
 */
class GeneralQualifierHierarchy extends MultiGraphQualifierHierarchy {

    public GeneralQualifierHierarchy(MultiGraphFactory factory) {
        super(factory);
    }

    // Always return true
    @Override
    public boolean isValid() {
        return true;
    }

    // Return the qualifier itself instead of the top.
    @Override
    public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
        return start;
    }

    // Return the qualifier itself instead of the bottom.
    @Override
    public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
        return start;
    }

    // Never find a corresponding qualifier.
    @Override
    public AnnotationMirror findAnnotationInSameHierarchy(
            Collection<? extends AnnotationMirror> annotations, AnnotationMirror annotationMirror) {
        return null;
    }

    // Not needed - raises error.
    @Override
    public Set<AnnotationMirror> getTopAnnotations() {
        ErrorReporter.errorAbort(
                "GeneralQualifierHierarchy:getTopAnnotations() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - should raise error. Unfortunately, in inference we ask for bottom annotations.
    // Return a dummy value that does no harm.
    @Override
    public Set<AnnotationMirror> getBottomAnnotations() {
        // ErrorReporter.errorAbort("GeneralQualifierHierarchy.getBottomAnnotations() was called! It
        // shouldn't be called.");
        return AnnotationUtils.createAnnotationSet();
    }

    // Not needed - raises error.
    @Override
    public Set<? extends AnnotationMirror> getTypeQualifiers() {
        ErrorReporter.errorAbort(
                "GeneralQualifierHierarchy.getTypeQualifiers() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        ErrorReporter.errorAbort(
                "GeneralQualifierHierarchy.isSubtype() was called! It shouldn't be called.");
        return false;
    }

    // Not needed - raises error.
    @Override
    public boolean isSubtypeTypeVariable(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        ErrorReporter.errorAbort(
                "GeneralQualifierHierarchy.isSubtypeTypeVariable() was called! It shouldn't be called.");
        return false;
    }

    // Not needed - raises error.
    @Override
    public boolean isSubtype(
            Collection<? extends AnnotationMirror> rhs,
            Collection<? extends AnnotationMirror> lhs) {
        ErrorReporter.errorAbort(
                "GeneralQualifierHierarchy.isSubtype() was called! It shouldn't be called.");
        return false;
    }

    // Not needed - raises error.
    @Override
    public boolean isSubtypeTypeVariable(
            Collection<? extends AnnotationMirror> subAnnos,
            Collection<? extends AnnotationMirror> superAnnos) {
        ErrorReporter.errorAbort(
                "GeneralQualifierHierarchy.isSubtypeTypeVariable() was called! It shouldn't be called.");
        return false;
    }

    // Not needed - raises error.
    @Override
    public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
        ErrorReporter.errorAbort(
                "GeneralQualifierHierarchy.leastUpperBound() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public AnnotationMirror leastUpperBoundTypeVariable(AnnotationMirror a1, AnnotationMirror a2) {
        ErrorReporter.errorAbort(
                "GeneralQualifierHierarchy.leastUpperBoundTypeVariable() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
        ErrorReporter.errorAbort(
                "GeneralQualifierHierarchy.greatestLowerBound() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public AnnotationMirror greatestLowerBoundTypeVariable(
            AnnotationMirror a1, AnnotationMirror a2) {
        ErrorReporter.errorAbort(
                "GeneralQualifierHierarchy.greatestLowerBoundTypeVariable() was called! It shouldn't be called.");
        return null;
    }

    @Override
    public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
        ErrorReporter.errorAbort(
                "GeneralQualifierHierarchy.getPolymorphicAnnotation() was called! It shouldn't be called.");
        return null;
    }
}
