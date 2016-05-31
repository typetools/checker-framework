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
import javax.lang.model.type.TypeMirror;

/**
 * UnsignednessAnnotatedTypeFactory removes implicit types from local variables
 * because currently types are assigned implicitely rather than defaultly because
 * they are linked to types, not locations. This ensures users may add @Unsigned
 * types to local variables.
 *
 * Furthermore, this removes all annotations from booleans to stop @Unsigned types
 * from bubling up through comparisons.
 */
public class UnsignednessAnnotatedTypeFactory extends BaseAnnotatedTypeFactory{

    private final AnnotationMirror UNKNOWN_SIGNEDNESS;
    
    /*
    private final String JAVA_LANG_BYTE = "java.lang.Byte";
    private final String JAVA_LANG_SHORT = "java.lang.Short";
    private final String JAVA_LANG_INTEGER = "java.lang.Integer";
    private final String JAVA_LANG_LONG = "java.lang.Long";
    */
    
    public UnsignednessAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN_SIGNEDNESS = AnnotationUtils.fromClass(elements, UnknownSignedness.class);

        postInit();
    }

    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        // When it is possible to default types based on their TypeKinds,
        // this method will no longer be needed.
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
        
        
        /*switch (type.getUnderlyingType().toString()) {
        case JAVA_LANG_BYTE:
        case JAVA_LANG_SHORT:
        case JAVA_LANG_INTEGER:
        case JAVA_LANG_LONG:
            QualifierDefaults defaults = new QualifierDefaults(elements, this);
            defaults.addCheckedCodeDefault(UNKNOWN_SIGNEDNESS, TypeUseLocation.LOCAL_VARIABLE);
            defaults.annotate(tree, type);
        }*/
        
    }
    
    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
            super.createTreeAnnotator(),
            new UnsignednessTreeAnnotator(this)
        );
    }
    
    /**
     * Do not allow @Unsigned or @Signed annotations to propogate up the syntax
     * tree through booleans. 
     */
    private class UnsignednessTreeAnnotator extends TreeAnnotator {
        private final AnnotationMirror UNSIGNED;
        private final AnnotationMirror SIGNED;
        
        public UnsignednessTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
            UNSIGNED = AnnotationUtils.fromClass(elements, Unsigned.class);
            SIGNED = AnnotationUtils.fromClass(elements, Signed.class);
        }    
        
        /**
         * Remove @Unsigned and @Signed annotations from boolean binary trees.
         * 
         * @param tree
         * @param type
         */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            
            switch (type.getKind()) {
            case BOOLEAN:
                type.removeAnnotation(UNSIGNED);
                type.removeAnnotation(SIGNED);
            }    
            
            return null;
        }
        
        /**
         * Remove @Unsigned and @Signed annotations from boolean compound assignment
         * trees.
         * 
         * @param tree
         * @param type
         */
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

