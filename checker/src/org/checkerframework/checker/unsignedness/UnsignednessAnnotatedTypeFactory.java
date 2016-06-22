package org.checkerframework.checker.unsignedness;

import org.checkerframework.checker.unsignedness.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationUtils;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree;

import javax.lang.model.element.AnnotationMirror;

/**
 * @checker_framework.manual #unsignedness-checker Unsignedness Checker
 */
public class UnsignednessAnnotatedTypeFactory extends BaseAnnotatedTypeFactory{

    private final AnnotationMirror UNSIGNED;
    private final AnnotationMirror SIGNED;
    private final AnnotationMirror UNKNOWN_SIGNEDNESS;

    // These are commented out until issues with making boxed implicitly signed
    // are worked out. (https://github.com/typetools/checker-framework/issues/797)
    /*
    private final String JAVA_LANG_BYTE = "java.lang.Byte";
    private final String JAVA_LANG_SHORT = "java.lang.Short";
    private final String JAVA_LANG_INTEGER = "java.lang.Integer";
    private final String JAVA_LANG_LONG = "java.lang.Long";
    */

    public UnsignednessAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNSIGNED = AnnotationUtils.fromClass(elements, Unsigned.class);
        SIGNED = AnnotationUtils.fromClass(elements, Signed.class);
        UNKNOWN_SIGNEDNESS = AnnotationUtils.fromClass(elements, UnknownSignedness.class);

        postInit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        // When it is possible to default types based on their TypeKinds,
        // this method will no longer be needed.
        // Currently, it is adding the LOCAL_VARIABLE default for
        // bytes, shorts, ints, and longs so that the implicit for
        // those types is not applied when they are local variables.
        // Only the local variable default is applied first because
        // it is the only refinable location (other than fields) that could
        // have a primitive type.

        addUnknownSignednessToSomeLocals(tree, type);
        super.addComputedTypeAnnotations(tree, type, iUseFlow);
    }

    /**
     * If the tree is a local variable and the type is a byte, short, int or long,
     * then it adds the UnknownSignedness annotation so that data flow can refine it.
     */
    private void addUnknownSignednessToSomeLocals(Tree tree, AnnotatedTypeMirror type) {
        switch (type.getKind()) {
        case BYTE:
        case SHORT:
        case INT:
        case LONG:
        case FLOAT:
        case DOUBLE:
        case CHAR:
            QualifierDefaults defaults = new QualifierDefaults(elements, this);
            defaults.addCheckedCodeDefault(UNKNOWN_SIGNEDNESS, TypeUseLocation.LOCAL_VARIABLE);
            defaults.annotate(tree, type);
        }

        // This code commented out until issues with making boxed implicitly signed
        // are worked out. (https://github.com/typetools/checker-framework/issues/797)

        /*switch (TypesUtils.getQualifiedName(type.getUnderlyingType()).toString()) {
        case JAVA_LANG_BYTE:
        case JAVA_LANG_SHORT:
        case JAVA_LANG_INTEGER:
        case JAVA_LANG_LONG:
            QualifierDefaults defaults = new QualifierDefaults(elements, this);
            defaults.addCheckedCodeDefault(UNKNOWN_SIGNEDNESS, TypeUseLocation.LOCAL_VARIABLE);
            defaults.annotate(tree, type);
        }*/

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
            new UnsignednessTreeAnnotator(this),
            super.createTreeAnnotator()
        );
    }

    /**
     * This TreeAnnotator ensures that booleans expressions are not
     * given Unsigned or Signed annotations by {@link PropagationTreeAnnotator}
     */
    private class UnsignednessTreeAnnotator extends TreeAnnotator {

        public UnsignednessTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**
         * Change the type of booleans to @UnknownSignedness so that the {@link PropagationTreeAnnotator}
         * does not change the type of them.
         */
        private Void annotateBoolean(AnnotatedTypeMirror type) {
            switch (type.getKind()) {
                case BOOLEAN:
                    type.addAnnotation(UNKNOWN_SIGNEDNESS);
            }

            return null;
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            return annotateBoolean(type);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree tree, AnnotatedTypeMirror type) {
            return annotateBoolean(type);
        }
    }
}
