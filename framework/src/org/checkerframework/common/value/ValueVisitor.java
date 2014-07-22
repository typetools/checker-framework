package org.checkerframework.common.value;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

import com.sun.source.tree.Tree;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.source.Result;
import com.sun.source.tree.AnnotationTree;
import org.checkerframework.common.value.qual.ArrayLen;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import org.checkerframework.common.value.qual.BoolVal;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.AssignmentTree;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.qual.UnknownVal;

import org.checkerframework.javacutil.AnnotationUtils;

/**
 * @author plvines
 * 
 *         TODO
 * 
 */
public class ValueVisitor extends BaseTypeVisitor<ValueAnnotatedTypeFactory> {

    public ValueVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected ValueAnnotatedTypeFactory createTypeFactory() {
        return new ValueAnnotatedTypeFactory(checker);
    }

    @Override
    public Void visitAnnotation(AnnotationTree tree, Void p){
        
        AnnotationMirror newAnno = AnnotationUtils.fromName(elements, "org.checkerframework.common.value.qual." + ((IdentifierTree)tree.getAnnotationType()).getName().toString());

        if (newAnno != null){
            for (AnnotationMirror anno : atypeFactory.constantAnnotations){
                if (AnnotationUtils.areSameIgnoringValues(newAnno, anno)){
                    if (tree.getArguments().size() > 0 && tree.getArguments().get(0).getKind() == Tree.Kind.ASSIGNMENT && ((AssignmentTree)tree.getArguments().get(0)).getExpression().getKind() == Tree.Kind.NEW_ARRAY){
                        int numArgs = ((NewArrayTree)((AssignmentTree)tree.getArguments().get(0)).getExpression()).getInitializers().size();
                        
                        if (numArgs > ValueAnnotatedTypeFactory.MAX_VALUES){
                            checker.report(Result.failure("too.many.values.given", ValueAnnotatedTypeFactory.MAX_VALUES), this.getCurrentPath().getLeaf());
                            return null;
                        }
                    }
                }
            }
        }
         
        return super.visitAnnotation(tree, p);
    }

}
