package tests.reflection;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.Bottom;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.reflection.ReflectionResolutionAnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;
import checkers.units.quals.Mass;
import checkers.units.quals.g;
import checkers.units.quals.kg;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import javacutils.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;

/**
 * AnnotatedTypeFactory with reflection resolution enabled. The used qualifier
 * hierarchy is straightforward and only intended for test purposes.
 * 
 * @author rjust
 */
@TypeQualifiers({ Mass.class, kg.class, g.class, Bottom.class,
        Unqualified.class })
public final class ReflectionTestAnnotatedTypeFactory extends
        ReflectionResolutionAnnotatedTypeFactory {
    public ReflectionTestAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
        AnnotationMirror bottom = AnnotationUtils.fromClass(elements,
                Bottom.class);
        this.typeAnnotator.addTypeName(java.lang.Void.class, bottom);
        this.treeAnnotator.addTreeKind(
                com.sun.source.tree.Tree.Kind.NULL_LITERAL, bottom);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        AnnotationMirror bottom = AnnotationUtils.fromClass(elements,
                Bottom.class);
        return new GraphQualifierHierarchy(factory, bottom);
    }
}
