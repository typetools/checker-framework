package tests.reflection;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.ReflectionResolutionAnnotatedTypeFactory;
import org.checkerframework.framework.qual.Bottom;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

import tests.reflection.qual.Sibling1;
import tests.reflection.qual.Sibling2;
import tests.reflection.qual.Top;



/**
 * AnnotatedTypeFactory with reflection resolution enabled. The used qualifier
 * hierarchy is straightforward and only intended for test purposes.
 *
 * @author rjust, smillst
 */
@TypeQualifiers({ Top.class, Sibling1.class, Sibling2.class, Bottom.class })
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
        this.treeAnnotator.addTreeKind(
                com.sun.source.tree.Tree.Kind.INT_LITERAL, bottom);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        AnnotationMirror bottom = AnnotationUtils.fromClass(elements,
                Bottom.class);
        return new GraphQualifierHierarchy(factory, bottom);
    }
}
