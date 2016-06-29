package org.checkerframework.checker.lowerbound;

import org.checkerframework.checker.lowerbound.qual.*;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.javacutil.AnnotationUtils;

import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

import com.sun.source.tree.Tree;
import com.sun.source.tree.LiteralTree;

public class LowerBoundAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    private final AnnotationMirror N1P, NN, POS;//, UNKNOWN;

    public LowerBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
	super(checker);
	N1P = AnnotationUtils.fromClass(elements, NegativeOnePlus.class);
	NN = AnnotationUtils.fromClass(elements, NonNegative.class);
	POS = AnnotationUtils.fromClass(elements, Positive.class);
	//	UNKNOWN = AnnotationUtils.fromClass(elements, Unknown.class);

	this.postInit();
    }

    // this is apparently just a required thing
    @Override
    public TreeAnnotator createTreeAnnotator() {
	return new LowerBoundTreeAnnotator(this);
    }

    private class LowerBoundTreeAnnotator extends TreeAnnotator{
	public LowerBoundTreeAnnotator(AnnotatedTypeFactory annotatedTypeFactory) {
	    super(annotatedTypeFactory);
	}

	// annotate literal integers appropriately
	@Override
	public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            // if this is an Integer specifically
            if (tree.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int) tree.getValue();
		if (val == -1) {
		    type.addAnnotation(N1P);
		} else if (val == 0) {
                    type.addAnnotation(NN);
                } else if (val > 0) {
		    type.addAnnotation(POS);
		}
            } // no else, only annotate Integers
            return super.visitLiteral(tree, type);
        }

    }

}
