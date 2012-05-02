package checkers.regex;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import com.sun.source.tree.*;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.DefaultFlow;
import checkers.flow.DefaultFlowState;
import checkers.types.AnnotatedTypeFactory;
import checkers.util.TreeUtils;

public class RegexFlow extends DefaultFlow<DefaultFlowState> {

    public RegexFlow(BaseTypeChecker checker, CompilationUnitTree root,
            Set<AnnotationMirror> annotations, AnnotatedTypeFactory factory) {
        super(checker, root, annotations, factory);
    }

    @Override
    protected void scanCond(ExpressionTree tree) {
        if (debug != null) {
            debug.println("RegexFlow::scanCond: " + tree);
        }

        super.scanCond(tree);

        if (tree == null) {
            // The tree can be null for conditions in "for(;;)" loops
            return;
        }

        tree = TreeUtils.skipParens(tree);

        RegexChecker rec = (RegexChecker) this.checker;

        if (tree.getKind() == Tree.Kind.METHOD_INVOCATION &&
                isRegex((MethodInvocationTree)tree)) {
            VariableElement arg = extractArg((MethodInvocationTree)tree);
            if (arg!=null) {
                int idx = flowState_whenTrue.vars.indexOf(arg);
                if (idx >= 0) {
                    flowState_whenTrue.annos.clearInAll(idx);
                    flowState_whenTrue.annos.set(rec.REGEX, idx);
                    flowState_whenFalse.annos.clear(rec.REGEX, idx);
                }
            }
        } else if (tree.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
            tree = TreeUtils.skipParens(((UnaryTree)tree).getExpression());
            if (tree.getKind() == Tree.Kind.METHOD_INVOCATION &&
                    isRegex((MethodInvocationTree)tree)) {
                VariableElement arg = extractArg((MethodInvocationTree)tree);
                if (arg!=null) {
                    int idx = flowState_whenTrue.vars.indexOf(arg);
                    if (idx >= 0) {
                        flowState_whenTrue.annos.clear(rec.REGEX, idx);
                        flowState_whenFalse.annos.clearInAll(idx);
                        flowState_whenFalse.annos.set(rec.REGEX, idx);
                    }
                }
            }
        }
    }

    private static final String[] isRegexNames = {
        "checkers.regex.RegexUtil.isRegex(java.lang.String)",
        "plume.RegexUtil.isRegex(java.lang.String)"
    };

    private boolean isRegex(MethodInvocationTree tree) {
        ExecutableElement method = TreeUtils.elementFromUse(tree);
        String sig = method.getEnclosingElement().toString() + "." + method.toString();
        for (String irn : isRegexNames) {
            if (irn.equals(sig)) return true;
        }
        return false;
    }

    private VariableElement extractArg(MethodInvocationTree tree) {
        ExpressionTree argtree = tree.getArguments().get(0);
        Element arg = TreeUtils.elementFromUse(argtree);
        if (arg!=null && arg instanceof VariableElement) {
            return (VariableElement) arg;
        } else {
            return null;
        }
    }

}