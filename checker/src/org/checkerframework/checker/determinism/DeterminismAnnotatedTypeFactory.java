package org.checkerframework.checker.determinism;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.determinism.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.QualifierPolymorphism;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

public class DeterminismAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    public final AnnotationMirror POLYDET = AnnotationBuilder.fromClass(elements, PolyDet.class);
    //public final AnnotationMirror POLYDET2 = AnnotationBuilder.fromClass(elements, PolyDet2.class);
    //public final AnnotationMirror POLYDET3 = AnnotationBuilder.fromClass(elements, PolyDet3.class);
    public final AnnotationMirror OND2D = AnnotationBuilder.fromClass(elements, Ond2D.class);
    public final AnnotationMirror OND2ND = AnnotationBuilder.fromClass(elements, Ond2Nd.class);
    public final AnnotationMirror ORDERNONDET =
            AnnotationBuilder.fromClass(elements, OrderNonDet.class);
    public final AnnotationMirror NONDET = AnnotationBuilder.fromClass(elements, NonDet.class);
    public final AnnotationMirror DET = AnnotationBuilder.fromClass(elements, Det.class);

    public DeterminismAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(
                        Det.class, OrderNonDet.class, NonDet.class, PolyDet.class
                        /*PolyDet2.class,
                        PolyDet3.class*/ ));
    }

    @Override
    protected QualifierPolymorphism createQualifierPolymorphism() {
        return new DetQualifierPolymorphism(processingEnv, this);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new DeterminismTreeAnnotator(this), super.createTreeAnnotator());
    }

    private class DeterminismTreeAnnotator extends TreeAnnotator {

        public DeterminismTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitReturn(ReturnTree node, AnnotatedTypeMirror p) {
            System.out.println("Correct return anno: " + node);
            return super.visitReturn(node, p);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, AnnotatedTypeMirror p) {
            if (p.getAnnotations().size() > 0) {
                ExecutableElement method = TreeUtils.elementFromUse(node);
                AnnotationMirror declAnno = getDeclAnnotation(method, Ond2D.class);
                AnnotationMirror returnAnno = p.getAnnotations().iterator().next();

                if (declAnno != null) {
                    if (types.isSameType(declAnno.getAnnotationType(), OND2D.getAnnotationType())) {
                        if (types.isSameType(
                                returnAnno.getAnnotationType(), ORDERNONDET.getAnnotationType())) {
                            p.clearAnnotations();
                            p.addAnnotation(AnnotationBuilder.fromClass(elements, Det.class));
                        }
                    }
                    if (types.isSameType(
                            declAnno.getAnnotationType(), OND2ND.getAnnotationType())) {
                        if (types.isSameType(
                                returnAnno.getAnnotationType(), ORDERNONDET.getAnnotationType())) {
                            p.clearAnnotations();
                            p.addAnnotation(AnnotationBuilder.fromClass(elements, NonDet.class));
                        }
                    }
                }
            }
            return super.visitMethodInvocation(node, p);
        }
    }
}
