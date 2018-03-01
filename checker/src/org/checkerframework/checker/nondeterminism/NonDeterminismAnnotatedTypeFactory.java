package org.checkerframework.checker.nondeterminism;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nondeterminism.qual.Det;
import org.checkerframework.checker.nondeterminism.qual.OrderNonDet;
import org.checkerframework.checker.nondeterminism.qual.PolyDet;
import org.checkerframework.checker.nondeterminism.qual.PolyDet2;
import org.checkerframework.checker.nondeterminism.qual.ValueNonDet;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.util.QualifierPolymorphism;
import org.checkerframework.javacutil.AnnotationBuilder;

public class NonDeterminismAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    public final AnnotationMirror POLYDET = AnnotationBuilder.fromClass(elements, PolyDet.class);
    public final AnnotationMirror POLYDET2 = AnnotationBuilder.fromClass(elements, PolyDet2.class);
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
                        PolyDet2.class));
    }

    @Override
    protected QualifierPolymorphism createQualifierPolymorphism() {
        return new NonDetQualifierPolymorphism(processingEnv, this);
    }
}
