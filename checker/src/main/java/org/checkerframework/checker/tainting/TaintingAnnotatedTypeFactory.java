package org.checkerframework.checker.tainting;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class TaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    //    private final AnnotationMirror UNTAINTED =
    //            AnnotationBuilder.fromClass(elements, Untainted.class);

    public TaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    protected void addComputedTypeAnnotations(
            Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        super.addComputedTypeAnnotations(tree, type, iUseFlow);
        if (TreeUtils.isArrayLengthAccess(tree)) {
            AnnotatedTypeMirror arrayType =
                    getAnnotatedType(((MemberSelectTree) tree).getExpression());
            type.replaceAnnotations(arrayType.getEffectiveAnnotations());
        }
    }
}
