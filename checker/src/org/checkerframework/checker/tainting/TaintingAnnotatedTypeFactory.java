package org.checkerframework.checker.tainting;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationUtils;

public class TaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror TAINTED, UNTAINTED, POLYTAINTED;

    public TaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        TAINTED = AnnotationUtils.fromClass(elements, Tainted.class);
        UNTAINTED = AnnotationUtils.fromClass(elements, Untainted.class);
        POLYTAINTED = AnnotationUtils.fromClass(elements, PolyTainted.class);

        postInit();
    }
}
