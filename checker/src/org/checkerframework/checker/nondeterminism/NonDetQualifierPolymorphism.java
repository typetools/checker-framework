package org.checkerframework.checker.nondeterminism;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.util.HashMap;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.QualifierPolymorphism;
import org.checkerframework.javacutil.ErrorReporter;

public class NonDetQualifierPolymorphism extends QualifierPolymorphism {

    ProcessingEnvironment env;
    NondeterminismAnnotatedTypeFactory factory;

    public NonDetQualifierPolymorphism(
            ProcessingEnvironment env, NondeterminismAnnotatedTypeFactory factory) {
        super(env, factory, new HashMap<>());
        this.env = env;
        this.factory = factory;
    }

    @Override
    public void annotate(MethodInvocationTree tree, AnnotatedExecutableType type) {
        Set<? extends AnnotationMirror> tops = qualhierarchy.getTopAnnotations();
        if (tops.size() != 1) {
            ErrorReporter.errorAbort(
                    "QualifierPolymorphism: PolymorphicQualifier has to specify type hierarchy, if more than one exist; top types: "
                            + tops);
        }
        polyQuals.clear();
        polyQuals.put(factory.VALUENONDET, factory.POLYDET);
        super.annotate(tree, type);
        polyQuals.put(factory.VALUENONDET, factory.POLYDET2);
        super.annotate(tree, type);
        polyQuals.put(factory.VALUENONDET, factory.POLYDET3);
        super.annotate(tree, type);
    }

    @Override
    public void annotate(NewClassTree tree, AnnotatedExecutableType type) {
        Set<? extends AnnotationMirror> tops = qualhierarchy.getTopAnnotations();
        if (tops.size() != 1) {
            ErrorReporter.errorAbort(
                    "QualifierPolymorphism: PolymorphicQualifier has to specify type hierarchy, if more than one exist; top types: "
                            + tops);
        }
        AnnotationMirror ttreetop = tops.iterator().next();
        polyQuals.put(factory.VALUENONDET, factory.POLYDET);

        super.annotate(tree, type);
        polyQuals.put(factory.VALUENONDET, factory.POLYDET2);
        super.annotate(tree, type);
    }
}
