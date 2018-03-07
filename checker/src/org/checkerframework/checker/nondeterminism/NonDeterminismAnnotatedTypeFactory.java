package org.checkerframework.checker.nondeterminism;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.nondeterminism.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.QualifierPolymorphism;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

public class NonDeterminismAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    public final AnnotationMirror POLYDET = AnnotationBuilder.fromClass(elements, PolyDet.class);
    public final AnnotationMirror POLYDET2 = AnnotationBuilder.fromClass(elements, PolyDet2.class);
    public final AnnotationMirror POLYDET3 = AnnotationBuilder.fromClass(elements, PolyDet3.class);
    public final AnnotationMirror OND2D = AnnotationBuilder.fromClass(elements, Ond2D.class);
    public final AnnotationMirror OND2VND = AnnotationBuilder.fromClass(elements, Ond2Vnd.class);
    public final AnnotationMirror ORDERNONDET =
            AnnotationBuilder.fromClass(elements, OrderNonDet.class);
    public final AnnotationMirror VALUENONDET =
            AnnotationBuilder.fromClass(elements, ValueNonDet.class);

    public NonDeterminismAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(
                        Det.class,
                        OrderNonDet.class,
                        ValueNonDet.class,
                        PolyDet.class,
                        PolyDet2.class,
                        PolyDet3.class));
    }

    @Override
    protected QualifierPolymorphism createQualifierPolymorphism() {
        return new NonDetQualifierPolymorphism(processingEnv, this);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new NonDeterminismTreeAnnotator(this), super.createTreeAnnotator());
    }

    private class NonDeterminismTreeAnnotator extends TreeAnnotator {

        public NonDeterminismTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
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
                            declAnno.getAnnotationType(), OND2VND.getAnnotationType())) {
                        if (types.isSameType(
                                returnAnno.getAnnotationType(), ORDERNONDET.getAnnotationType())) {
                            p.clearAnnotations();
                            p.addAnnotation(
                                    AnnotationBuilder.fromClass(elements, ValueNonDet.class));
                        }
                    }
                }
            }
            return super.visitMethodInvocation(node, p);
        }
    }
}
