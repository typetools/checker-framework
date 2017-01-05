package org.checkerframework.checker.index;

import com.sun.source.tree.Tree;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This class stores information about interesting methods and allows its clients to query it to
 * determine if a method belongs to a particular class.
 */
public class IndexMethodIdentifier {

    private final ExecutableElement fcnRandom;
    private final ExecutableElement fcnNextDouble;

    private final List<ExecutableElement> listRemoveMethods;
    private final List<ExecutableElement> listClearMethods;
    private final List<ExecutableElement> listAddMethods;
    private final List<ExecutableElement> mathMinMethods;
    private final List<ExecutableElement> mathMaxMethods;

    public IndexMethodIdentifier(ProcessingEnvironment processingEnv) {
        fcnRandom = TreeUtils.getMethod("java.lang.Math", "random", 0, processingEnv);
        fcnNextDouble = TreeUtils.getMethod("java.util.Random", "nextDouble", 0, processingEnv);

        listRemoveMethods = TreeUtils.getMethodList("java.util.List", "remove", 1, processingEnv);
        listClearMethods = TreeUtils.getMethodList("java.util.List", "clear", 0, processingEnv);
        listClearMethods.add(TreeUtils.getMethod("java.util.List", "removeAll", 1, processingEnv));
        listClearMethods.add(TreeUtils.getMethod("java.util.List", "retainAll", 1, processingEnv));
        listAddMethods = TreeUtils.getMethodList("java.util.List", "add", 1, processingEnv);
        mathMinMethods = TreeUtils.getMethodList("java.lang.Math", "min", 2, processingEnv);
        mathMaxMethods = TreeUtils.getMethodList("java.lang.Math", "max", 2, processingEnv);
    }

    public boolean isMathMin(Tree methodTree, ProcessingEnvironment processingEnv) {
        for (ExecutableElement minMethod : mathMinMethods) {
            if (TreeUtils.isMethodInvocation(methodTree, minMethod, processingEnv)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMathMax(Tree methodTree, ProcessingEnvironment processingEnv) {
        for (ExecutableElement maxMethod : mathMaxMethods) {
            if (TreeUtils.isMethodInvocation(methodTree, maxMethod, processingEnv)) {
                return true;
            }
        }
        return false;
    }

    public boolean isListRemove(ExecutableElement method, ProcessingEnvironment processingEnv) {
        for (ExecutableElement removeMethod : listRemoveMethods) {
            if (ElementUtils.isMethod(method, removeMethod, processingEnv)) {
                return true;
            }
        }
        return false;
    }

    public boolean isListClear(ExecutableElement method, ProcessingEnvironment processingEnv) {
        for (ExecutableElement removeMethod : listClearMethods) {
            if (ElementUtils.isMethod(method, removeMethod, processingEnv)) {
                return true;
            }
        }
        return false;
    }

    public boolean isListAdd(ExecutableElement method, ProcessingEnvironment processingEnv) {
        for (ExecutableElement addMethod : listAddMethods) {
            if (ElementUtils.isMethod(method, addMethod, processingEnv)) {
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
