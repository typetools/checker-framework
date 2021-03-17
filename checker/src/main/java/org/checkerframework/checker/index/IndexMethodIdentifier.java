package org.checkerframework.checker.index;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.index.qual.LengthOf;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This class stores information about interesting methods and allows its clients to query it to
 * determine if a method belongs to a particular class.
 */
public class IndexMethodIdentifier {

    /** The {@code java.lang.Math#random()} method. */
    private final ExecutableElement mathRandom;
    /** The {@code java.util.Random#nextDouble()} method. */
    private final ExecutableElement randomNextDouble;
    /** The {@code java.util.Random#nextInt()} method. */
    private final ExecutableElement randomNextInt;
    /** The {@code java.lang.String#length()} method. */
    private final ExecutableElement stringLength;
    /** The {@code java.lang.Math#min()} methods. */
    private final List<ExecutableElement> mathMinMethods;
    /** The {@code java.lang.Math#max()} methods. */
    private final List<ExecutableElement> mathMaxMethods;

    /** The LengthOf.value argument/element. */
    private final ExecutableElement lengthOfValueElement;

    /** The type factory. */
    private final AnnotatedTypeFactory factory;

    public IndexMethodIdentifier(AnnotatedTypeFactory factory) {
        this.factory = factory;
        ProcessingEnvironment processingEnv = factory.getProcessingEnv();
        mathRandom = TreeUtils.getMethod("java.lang.Math", "random", 0, processingEnv);
        randomNextDouble = TreeUtils.getMethod("java.util.Random", "nextDouble", 0, processingEnv);
        randomNextInt = TreeUtils.getMethod("java.util.Random", "nextInt", 1, processingEnv);

        stringLength = TreeUtils.getMethod("java.lang.String", "length", 0, processingEnv);

        mathMinMethods = TreeUtils.getMethods("java.lang.Math", "min", 2, processingEnv);
        mathMaxMethods = TreeUtils.getMethods("java.lang.Math", "max", 2, processingEnv);

        lengthOfValueElement =
                TreeUtils.getMethod(LengthOf.class.getCanonicalName(), "value", 0, processingEnv);
    }

    /** Returns true iff the argument is an invocation of Math.min. */
    public boolean isMathMin(Tree methodTree) {
        ProcessingEnvironment processingEnv = factory.getProcessingEnv();
        return TreeUtils.isMethodInvocation(methodTree, mathMinMethods, processingEnv);
    }

    /** Returns true iff the argument is an invocation of Math.max. */
    public boolean isMathMax(Tree methodTree) {
        ProcessingEnvironment processingEnv = factory.getProcessingEnv();
        return TreeUtils.isMethodInvocation(methodTree, mathMaxMethods, processingEnv);
    }

    /** Returns true iff the argument is an invocation of Math.random(). */
    public boolean isMathRandom(Tree tree, ProcessingEnvironment processingEnv) {
        return TreeUtils.isMethodInvocation(tree, mathRandom, processingEnv);
    }

    /** Returns true iff the argument is an invocation of Random.nextDouble(). */
    public boolean isRandomNextDouble(Tree tree, ProcessingEnvironment processingEnv) {
        return TreeUtils.isMethodInvocation(tree, randomNextDouble, processingEnv);
    }

    /** Returns true iff the argument is an invocation of Random.nextInt(). */
    public boolean isRandomNextInt(Tree tree, ProcessingEnvironment processingEnv) {
        return TreeUtils.isMethodInvocation(tree, randomNextInt, processingEnv);
    }

    /**
     * Returns true if {@code tree} is an invocation of a method that returns the length of "this"
     *
     * @param tree a tree
     * @return true if {@code tree} is an invocation of a method that returns the length of {@code
     *     this}
     */
    public boolean isLengthOfMethodInvocation(Tree tree) {
        if (tree.getKind() != Kind.METHOD_INVOCATION) {
            return false;
        }
        return isLengthOfMethodInvocation(TreeUtils.elementFromUse((MethodInvocationTree) tree));
    }

    /**
     * Returns true if {@code tree} evaluates to the length of "this". This might be a call to
     * String,length, or a method annotated with @LengthOf.
     *
     * @return true if {@code tree} evaluates to the length of "this"
     */
    public boolean isLengthOfMethodInvocation(ExecutableElement ele) {
        if (stringLength.equals(ele)) {
            // TODO: Why not just annotate String.length with @LengthOf and thus eliminate the
            // special case in this method's implementation?
            return true;
        }

        AnnotationMirror lengthOfAnno = factory.getDeclAnnotation(ele, LengthOf.class);
        if (lengthOfAnno == null) {
            return false;
        }
        AnnotationValue lengthOfValue = lengthOfAnno.getElementValues().get(lengthOfValueElement);
        return AnnotationUtils.annotationValueContains(lengthOfValue, "this");
    }

    /**
     * Returns true if {@code node} is an invocation of a method that returns the length of {@code
     * this}
     *
     * @param node a node
     * @return true if {@code node} is an invocation of a method that returns the length of {@code
     *     this}
     */
    public boolean isLengthOfMethodInvocation(Node node) {
        if (node instanceof MethodInvocationNode) {
            MethodInvocationNode methodInvocationNode = (MethodInvocationNode) node;
            MethodAccessNode methodAccessNode = methodInvocationNode.getTarget();
            ExecutableElement ele = methodAccessNode.getMethod();

            return isLengthOfMethodInvocation(ele);
        }
        return false;
    }
}
