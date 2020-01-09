package testlib.wholeprograminference;

import com.sun.source.tree.AnnotationTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import testlib.wholeprograminference.qual.DefaultType;

/** Visitor for a simple type system to test whole-program inference using .jaif files. */
public class WholeProgramInferenceTestVisitor
        extends BaseTypeVisitor<WholeProgramInferenceTestAnnotatedTypeFactory> {

    public WholeProgramInferenceTestVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected WholeProgramInferenceTestAnnotatedTypeFactory createTypeFactory() {
        return new WholeProgramInferenceTestAnnotatedTypeFactory(checker);
    }

    @Override
    public Void visitAnnotation(AnnotationTree node, Void p) {
        Element anno = TreeInfo.symbol((JCTree) node.getAnnotationType());
        if (anno.toString().equals(DefaultType.class.getName())) {
            checker.report(Result.failure("annotation.not.allowed.in.src", anno.toString()), node);
        }
        return super.visitAnnotation(node, p);
    }

    @Override
    protected void checkConstructorResult(
            AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
        // Skip this check.
    }
}
