package checkers.regex;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;

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
                isRegexSimple((MethodInvocationTree)tree)) {
            adaptStateSimple(rec, (MethodInvocationTree)tree, true);
        } else if (tree.getKind() == Tree.Kind.METHOD_INVOCATION &&
                isRegexCount((MethodInvocationTree)tree)) {
            adaptStateCount((MethodInvocationTree)tree, true);
        } else if (tree.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
            tree = TreeUtils.skipParens(((UnaryTree)tree).getExpression());
            if (tree.getKind() == Tree.Kind.METHOD_INVOCATION &&
                    isRegexSimple((MethodInvocationTree)tree)) {
                adaptStateSimple(rec, (MethodInvocationTree)tree, false);
            } else if (tree.getKind() == Tree.Kind.METHOD_INVOCATION &&
                    isRegexCount((MethodInvocationTree)tree)) {
                adaptStateCount((MethodInvocationTree)tree, false);
            }
        }
    }

    private void adaptStateSimple(RegexChecker rec, MethodInvocationTree tree, boolean pos) {
        VariableElement arg = extractStringArg((MethodInvocationTree)tree);
        if (arg!=null) {
            int idx = flowState_whenTrue.vars.indexOf(arg);
            if (idx >= 0) {
                DefaultFlowState s1 = pos ? flowState_whenTrue : flowState_whenFalse;
                DefaultFlowState s2 = pos ? flowState_whenFalse : flowState_whenTrue;

                s1.annos.clearInAll(idx);
                s1.annos.set(rec.REGEX, idx);
                s2.annos.clear(rec.REGEX, idx);
            }
        }
    }

    private void adaptStateCount(MethodInvocationTree tree, boolean pos) {
        VariableElement arg = extractStringArg((MethodInvocationTree)tree);
        Integer cnt = extractCountArg((MethodInvocationTree)tree);
        if (arg!=null && cnt!=null) {
            int idx = flowState_whenTrue.vars.indexOf(arg);
            if (idx >= 0) {
                DefaultFlowState s1 = pos ? flowState_whenTrue : flowState_whenFalse;
                DefaultFlowState s2 = pos ? flowState_whenFalse : flowState_whenTrue;

                AnnotationMirror recnt = ((RegexAnnotatedTypeFactory)this.factory).createRegexAnnotation(cnt);
                s1.annos.clearInAll(idx);
                s1.annos.set(recnt, idx);
                s1.getAnnotations().add(recnt);
                s2.getAnnotations().add(recnt);
                s2.annos.set(recnt, idx);
                s2.annos.clear(recnt, idx);
            }
        }
    }

    private boolean isRegexSimple(MethodInvocationTree tree) {
        ExecutableElement method = TreeUtils.elementFromUse(tree);
        String sig = method.getEnclosingElement().toString() + "." + method.toString();
        for (String rc : RegexAnnotatedTypeFactory.asRegexClasses) {
            String irc = rc + "." + "isRegex(java.lang.String)";
            if (irc.equals(sig)) return true;
        }
        return false;
    }

    private boolean isRegexCount(MethodInvocationTree tree) {
        ExecutableElement method = TreeUtils.elementFromUse(tree);
        String sig = method.getEnclosingElement().toString() + "." + method.toString();
        for (String rc : RegexAnnotatedTypeFactory.asRegexClasses) {
            String irc = rc + "." + "isRegex(java.lang.String,int)";
            if (irc.equals(sig)) return true;
        }
        return false;
    }

    private VariableElement extractStringArg(MethodInvocationTree tree) {
        ExpressionTree argtree = tree.getArguments().get(0);
        Element arg = TreeUtils.elementFromUse(argtree);
        if (arg!=null && arg instanceof VariableElement) {
            return (VariableElement) arg;
        } else {
            return null;
        }
    }

    private Integer extractCountArg(MethodInvocationTree tree) {
        ExpressionTree argtree = tree.getArguments().get(1);
        if (argtree.getKind() == Kind.INT_LITERAL) {
            LiteralTree literal = (LiteralTree) argtree;
            return (Integer) literal.getValue();
        } else {
            return null;
        }
    }

}