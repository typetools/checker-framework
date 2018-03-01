package org.checkerframework.checker.nondeterminism;

import com.sun.source.tree.NewClassTree;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nondeterminism.qual.PolyDet;
import org.checkerframework.checker.nondeterminism.qual.PolyDet2;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.QualifierPolymorphism;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.ErrorReporter;

public class NonDetQualifierPolymorphism extends QualifierPolymorphism {

    ProcessingEnvironment env;
    NonDeterminismAnnotatedTypeFactory factory;

    public NonDetQualifierPolymorphism(
            ProcessingEnvironment env, NonDeterminismAnnotatedTypeFactory factory) {
        super(env, factory);
        this.env = env;
        this.factory = factory;
    }

    //    @Override
    //    public void annotate(MethodInvocationTree tree, AnnotatedExecutableType type) {
    //        Set<? extends AnnotationMirror> tops = qualhierarchy.getTopAnnotations();
    //        if (tops.size() != 1) {
    //            ErrorReporter.errorAbort(
    //                    "QualifierPolymorphism: PolymorphicQualifier has to specify type hierarchy, if more than one exist; top types: "
    //                            + tops);
    //        }
    //        AnnotationMirror ttreetop = tops.iterator().next();
    //        polyQuals.clear();
    //        polyQuals.put(
    //                ttreetop, AnnotationBuilder.fromClass(factory.getElementUtils(), PolyDet.class));
    //        super.annotate(tree, type);
    ////        polyQuals.put(
    ////                ttreetop, AnnotationBuilder.fromClass(factory.getElementUtils(), PolyDet2.class));
    ////        super.annotate(tree, type);
    //    }

    @Override
    public void annotate(NewClassTree tree, AnnotatedExecutableType type) {
        Set<? extends AnnotationMirror> tops = qualhierarchy.getTopAnnotations();
        if (tops.size() != 1) {
            ErrorReporter.errorAbort(
                    "QualifierPolymorphism: PolymorphicQualifier has to specify type hierarchy, if more than one exist; top types: "
                            + tops);
        }
        AnnotationMirror ttreetop = tops.iterator().next();
        polyQuals.put(
                ttreetop, AnnotationBuilder.fromClass(factory.getElementUtils(), PolyDet.class));
        super.annotate(tree, type);
        polyQuals.put(
                ttreetop, AnnotationBuilder.fromClass(factory.getElementUtils(), PolyDet2.class));
        super.annotate(tree, type);
    }
}
