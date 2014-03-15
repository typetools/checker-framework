package tests.reflection;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.ReflectionResolutionAnnotatedTypeFactory;
import org.checkerframework.framework.qual.Bottom;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.qual.Unqualified;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

import tests.util.Even;
import tests.util.Odd;

/**
 * AnnotatedTypeFactory with reflection resolution enabled. The used qualifier
 * hierarchy is straightforward and only intended for test purposes.
 *
 * @author rjust
 */
@TypeQualifiers({ Even.class, Odd.class, Bottom.class, Unqualified.class })
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
