package checkers.types;

/*>>>
import checkers.interning.quals.*;
*/

import java.util.Collection;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import javacutils.ErrorReporter;

import checkers.source.SourceChecker;

import com.sun.source.tree.CompilationUnitTree;

/**
 * A "general" annotated type factory that supports qualifiers from any type hierarchy.
 * One big limitation is that it does not support annotations coming from a stub file.
 */
public class GeneralAnnotatedTypeFactory extends AnnotatedTypeFactory {

    public GeneralAnnotatedTypeFactory(SourceChecker<? extends AnnotatedTypeFactory> checker, CompilationUnitTree root) {
        super(checker, new GeneralQualifierHierarchy(), null, root);
        postInit();
    }

    /** Return true to support any qualifier.
      * No handling of aliases.
      */
    @Override
    public boolean isSupportedQualifier(AnnotationMirror a) {
        return true;
    }

}

/** A very limited QualifierHierarchy that is used for access to
  * qualifiers from different type systems.
  */
class GeneralQualifierHierarchy extends QualifierHierarchy {

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
    public AnnotationMirror findCorrespondingAnnotation(
            AnnotationMirror aliased, Collection<AnnotationMirror> annotations) {
        return null;
    }

    // Not needed - raises error.
    @Override
    public Set<AnnotationMirror> getTopAnnotations() {
        ErrorReporter.errorAbort("GeneralQualifierHierarchy:getTopAnnotations() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public Set<AnnotationMirror> getBottomAnnotations() {
        ErrorReporter.errorAbort("GeneralQualifierHierarchy.getBottomAnnotations() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public Set</*@Interned*/ String> getTypeQualifiers() {
        ErrorReporter.errorAbort("GeneralQualifierHierarchy.getTypeQualifiers() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public boolean isSubtype(AnnotationMirror anno1, AnnotationMirror anno2) {
        ErrorReporter.errorAbort("GeneralQualifierHierarchy.isSubtype() was called! It shouldn't be called.");
        return false;
    }

    // Not needed - raises error.
    @Override
    public boolean isSubtypeTypeVariable(AnnotationMirror anno1, AnnotationMirror anno2) {
        ErrorReporter.errorAbort("GeneralQualifierHierarchy.isSubtypeTypeVariable() was called! It shouldn't be called.");
        return false;
    }

    // Not needed - raises error.
    @Override
    public boolean isSubtype(Collection<AnnotationMirror> rhs,
            Collection<AnnotationMirror> lhs) {
        ErrorReporter.errorAbort("GeneralQualifierHierarchy.isSubtype() was called! It shouldn't be called.");
        return false;
    }

    // Not needed - raises error.
    @Override
    public boolean isSubtypeTypeVariable(Collection<AnnotationMirror> rhs,
            Collection<AnnotationMirror> lhs) {
        ErrorReporter.errorAbort("GeneralQualifierHierarchy.isSubtypeTypeVariable() was called! It shouldn't be called.");
        return false;
    }

    // Not needed - raises error.
    @Override
    public AnnotationMirror leastUpperBound(AnnotationMirror a1,
            AnnotationMirror a2) {
        ErrorReporter.errorAbort("GeneralQualifierHierarchy.leastUpperBound() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public AnnotationMirror leastUpperBoundTypeVariable(AnnotationMirror a1,
            AnnotationMirror a2) {
        ErrorReporter.errorAbort("GeneralQualifierHierarchy.leastUpperBoundTypeVariable() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1,
            AnnotationMirror a2) {
        ErrorReporter.errorAbort("GeneralQualifierHierarchy.greatestLowerBound() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public AnnotationMirror greatestLowerBoundTypeVariable(AnnotationMirror a1,
            AnnotationMirror a2) {
        ErrorReporter.errorAbort("GeneralQualifierHierarchy.greatestLowerBoundTypeVariable() was called! It shouldn't be called.");
        return null;
    }

    @Override
    public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
        ErrorReporter.errorAbort("GeneralQualifierHierarchy.getPolymorphicAnnotation() was called! It shouldn't be called.");
        return null;
    }
}
