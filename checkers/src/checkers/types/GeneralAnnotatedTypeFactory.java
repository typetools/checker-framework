package checkers.types;

import java.util.Collection;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Name;

import checkers.source.SourceChecker;

import com.sun.source.tree.CompilationUnitTree;

/**
 * A "general" annotated type factory that supports qualifiers from any type hierarchy.
 * One big limitation is that it does not support annotations coming from a stub files.
 */
public class GeneralAnnotatedTypeFactory extends AnnotatedTypeFactory {

    public GeneralAnnotatedTypeFactory(SourceChecker checker, CompilationUnitTree root) {
        super(checker, new GeneralQualifierHierarchy(), root);
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
            AnnotationMirror aliased, Set<AnnotationMirror> annotations) {
        return null;
    }

    // Not needed - raises error.
    @Override
    public Set<AnnotationMirror> getTopAnnotations() {
        SourceChecker.errorAbort("GeneralQualifierHierarchy:getTopAnnotations() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public Set<AnnotationMirror> getBottomAnnotations() {
        SourceChecker.errorAbort("GeneralQualifierHierarchy.getBottomAnnotations() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public Set<Name> getTypeQualifiers() {
        SourceChecker.errorAbort("GeneralQualifierHierarchy.getTypeQualifiers() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public boolean isSubtype(AnnotationMirror anno1, AnnotationMirror anno2) {
        SourceChecker.errorAbort("GeneralQualifierHierarchy.isSubtype() was called! It shouldn't be called.");
        return false;
    }

    // Not needed - raises error.
    @Override
    public boolean isSubtype(Collection<AnnotationMirror> rhs,
            Collection<AnnotationMirror> lhs) {
        SourceChecker.errorAbort("GeneralQualifierHierarchy.isSubtype() was called! It shouldn't be called.");
        return false;
    }

    // Not needed - raises error.
    @Override
    public AnnotationMirror leastUpperBound(AnnotationMirror a1,
            AnnotationMirror a2) {
        SourceChecker.errorAbort("GeneralQualifierHierarchy.leastUpperBound() was called! It shouldn't be called.");
        return null;
    }

    // Not needed - raises error.
    @Override
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1,
            AnnotationMirror a2) {
        SourceChecker.errorAbort("GeneralQualifierHierarchy.greatestLowerBound() was called! It shouldn't be called.");
        return null;
    }

    @Override
    public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
        SourceChecker.errorAbort("GeneralQualifierHierarchy.getPolymorphicAnnotation() was called! It shouldn't be called.");
        return null;
    }
}
