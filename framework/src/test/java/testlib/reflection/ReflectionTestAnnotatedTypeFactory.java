package testlib.reflection;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import testlib.reflection.qual.ReflectBottom;

/**
 * AnnotatedTypeFactory with reflection resolution enabled. The used qualifier hierarchy is
 * straightforward and only intended for test purposes.
 */
public final class ReflectionTestAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    public ReflectionTestAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
        AnnotationMirror bottom = AnnotationBuilder.fromClass(elements, ReflectBottom.class);
        addTypeNameImplicit(java.lang.Void.class, bottom);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        ImplicitsTreeAnnotator implicitsTreeAnnotator = new ImplicitsTreeAnnotator(this);
        AnnotationMirror bottom = AnnotationBuilder.fromClass(elements, ReflectBottom.class);
        implicitsTreeAnnotator.addTreeKind(com.sun.source.tree.Tree.Kind.NULL_LITERAL, bottom);
        implicitsTreeAnnotator.addTreeKind(com.sun.source.tree.Tree.Kind.INT_LITERAL, bottom);

        return new ListTreeAnnotator(new PropagationTreeAnnotator(this), implicitsTreeAnnotator);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        AnnotationMirror bottom = AnnotationBuilder.fromClass(elements, ReflectBottom.class);
        return new GraphQualifierHierarchy(factory, bottom);
    }
}
