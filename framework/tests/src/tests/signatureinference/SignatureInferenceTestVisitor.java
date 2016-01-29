package tests.signatureinference;

import javax.lang.model.element.Element;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;

import tests.signatureinference.qual.DefaultType;

import com.sun.source.tree.AnnotationTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
/**
 * Visitor for a simple type system to test signature inference
 * using .jaif files.
 *
 * @author pbsf
 */
public class SignatureInferenceTestVisitor extends BaseTypeVisitor<SignatureInferenceTestAnnotatedTypeFactory> {

    public SignatureInferenceTestVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected SignatureInferenceTestAnnotatedTypeFactory createTypeFactory() {
        return new SignatureInferenceTestAnnotatedTypeFactory(checker);
    }

    @Override
    public Void visitAnnotation(AnnotationTree node, Void p) {
        Element anno = TreeInfo.symbol((JCTree) node.getAnnotationType());
        if (anno.toString().equals(DefaultType.class.getName())) {
            checker.report(Result.failure("annotation.not.allowed.in.src",
                    anno.toString()), node);
        }
        return super.visitAnnotation(node, p);
    }
}
