package org.checkerframework.checker.unsignedness;

import org.checkerframework.checker.unsignedness.qual.UnknownSignedness;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.Tree;

public class UnsignednessAnnotatedTypeFactory extends BaseAnnotatedTypeFactory{
    private final AnnotationMirror UNKNOWN_SIGNEDNESS;
    public UnsignednessAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN_SIGNEDNESS = AnnotationUtils.fromClass(elements, UnknownSignedness.class);

        postInit();
    }

    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
// When it is possible to default types based on their TypeKinds,
// this method will no longer be need.
// Currently, it is adding the LOCAL_VARIABLE default for 
// bytes, shorts, ints, and longs so that the implicit for 
// those types is not applied when they are local variables.
// Only the local variable default is applied first because 
// it is the only refinable location (other than fields) that could 
// have a primitive type.

        addUnknownSignednessToSomeLocals(tree, type);
        super.annotateImplicit(tree, type, iUseFlow);
    }

    /**
     * If the tree is a local variable and the type is a byte, short, int or long,
     * then it adds the @UnknownSignedness annootation.
     *
     * @param tree
     * @param type
     */
    private void addUnknownSignednessToSomeLocals(Tree tree, AnnotatedTypeMirror type) {
        switch (type.getKind()) {
        case BYTE:
        case SHORT:
        case INT:
        case LONG:
            QualifierDefaults defaults = new QualifierDefaults(elements, this);
            defaults.addCheckedCodeDefault(UNKNOWN_SIGNEDNESS, TypeUseLocation.LOCAL_VARIABLE);
            defaults.annotate(tree, type);
        }

    }
}

