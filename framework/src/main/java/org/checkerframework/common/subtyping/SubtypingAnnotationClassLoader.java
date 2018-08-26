package org.checkerframework.common.subtyping;

import java.lang.annotation.Annotation;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.type.AnnotationClassLoader;

public class SubtypingAnnotationClassLoader extends AnnotationClassLoader {

    public SubtypingAnnotationClassLoader(BaseTypeChecker checker) {
        super(checker);
    }

    // Unqualified is a supported annotation for the Subtyping Checker, and is loaded only if listed
    // in -Aquals. It intentionally has an empty @Target meta-annotation. All other annotations used
    // with the subtyping checker must have a well-defined @Target meta-annotation.
    @Override
    protected boolean hasWellDefinedTargetMetaAnnotation(Class<? extends Annotation> annoClass) {
        return super.hasWellDefinedTargetMetaAnnotation(annoClass)
                || annoClass.equals(Unqualified.class);
    }
}
