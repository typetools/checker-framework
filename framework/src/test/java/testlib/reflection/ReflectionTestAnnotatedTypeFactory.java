package testlib.reflection;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
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
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        LiteralTreeAnnotator literalTreeAnnotator = new LiteralTreeAnnotator(this);
        AnnotationMirror bottom = AnnotationBuilder.fromClass(elements, ReflectBottom.class);
        literalTreeAnnotator.addLiteralKind(LiteralKind.INT, bottom);
        literalTreeAnnotator.addStandardLiteralQualifiers();

        return new ListTreeAnnotator(new PropagationTreeAnnotator(this), literalTreeAnnotator);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        AnnotationMirror bottom = AnnotationBuilder.fromClass(elements, ReflectBottom.class);
        return new GraphQualifierHierarchy(factory, bottom);
    }
}
