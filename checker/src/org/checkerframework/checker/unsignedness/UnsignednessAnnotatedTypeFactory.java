package org.checkerframework.checker.unsignedness;

import org.checkerframework.checker.unsignedness.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationUtils;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree;

import javax.lang.model.element.AnnotationMirror;

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
     * then it adds the @UnknownSignedness annotation.
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
        case FLOAT:
        case DOUBLE:
        case CHAR:
            QualifierDefaults defaults = new QualifierDefaults(elements, this);
            defaults.addCheckedCodeDefault(UNKNOWN_SIGNEDNESS, TypeUseLocation.LOCAL_VARIABLE);
            defaults.annotate(tree, type);
        }

    }
    
    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
            super.createTreeAnnotator(),
            new UnsignednessTreeAnnotator(this)
        );
    }
    
    // Do not allow Unsigned or Signed to propogate through boolean binary operations
    private class UnsignednessTreeAnnotator extends TreeAnnotator {
        private final AnnotationMirror UNSIGNED;
        private final AnnotationMirror SIGNED;
        
        public UnsignednessTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
            UNSIGNED = AnnotationUtils.fromClass(elements, Unsigned.class);
            SIGNED = AnnotationUtils.fromClass(elements, Signed.class);
        }    
        
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            
            switch (type.getKind()) {
            case BOOLEAN:
                type.removeAnnotation(UNSIGNED);
                type.removeAnnotation(SIGNED);
            }    
            
            return null;
        }
        
        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree tree, AnnotatedTypeMirror type) {
            switch (type.getKind()) {
            case BOOLEAN:
                type.removeAnnotation(UNSIGNED);
                type.removeAnnotation(SIGNED);
            } 
            
            return null;
        }
    }
}

