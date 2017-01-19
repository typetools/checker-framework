package org.checkerframework.checker.index;

import com.sun.source.tree.Tree;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This class stores information about interesting methods and allows its clients to query it to
 * determine if a method belongs to a particular class.
 */
public class IndexMethodIdentifier {

    private final ExecutableElement fcnRandom;
    private final ExecutableElement fcnNextDouble;

    private final List<ExecutableElement> mathMinMethods;
    private final List<ExecutableElement> mathMaxMethods;

    public IndexMethodIdentifier(ProcessingEnvironment processingEnv) {
        fcnRandom = TreeUtils.getMethod("java.lang.Math", "random", 0, processingEnv);
        fcnNextDouble = TreeUtils.getMethod("java.util.Random", "nextDouble", 0, processingEnv);

        mathMinMethods = TreeUtils.getMethodList("java.lang.Math", "min", 2, processingEnv);
        mathMaxMethods = TreeUtils.getMethodList("java.lang.Math", "max", 2, processingEnv);
    }

    public boolean isMathMin(Tree methodTree, ProcessingEnvironment processingEnv) {
        return isInvocationOfOne(methodTree, processingEnv, mathMinMethods);
    }

    public boolean isMathMax(Tree methodTree, ProcessingEnvironment processingEnv) {
        return isInvocationOfOne(methodTree, processingEnv, mathMaxMethods);
    }

    private static boolean isInvocationOfOne(
            Tree methodTree, ProcessingEnvironment processingEnv, List<ExecutableElement> methods) {
        for (ExecutableElement minMethod : methods) {
            if (TreeUtils.isMethodInvocation(methodTree, minMethod, processingEnv)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMathRandom(Tree tree, ProcessingEnvironment processingEnv) {
        return TreeUtils.isMethodInvocation(tree, fcnRandom, processingEnv);
    }

    public boolean isRandomNextDouble(Tree tree, ProcessingEnvironment processingEnv) {
        return TreeUtils.isMethodInvocation(tree, fcnNextDouble, processingEnv);
    }
}
