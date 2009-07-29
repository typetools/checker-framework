package checkers.lock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

import com.sun.source.tree.*;

import checkers.basetype.BaseTypeVisitor;
import checkers.lock.quals.Holding;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.TreeUtils;

//Disclaimer:
//This class is currently in its alpha form.  For sample code on how to write
//checkers, please review other checkers for code samples.

/**
 * A type-checking visitor for the Lock type system.
 * This visitor reports errors ("unguarded.access") or warnings for violations
 * for accessing a field or calling a method without holding their locks.
 */
public class LockVisitor extends BaseTypeVisitor<Void, Void> {

    LockAnnotatedTypeFactory atypeFactory;

    public LockVisitor(LockChecker checker, CompilationUnitTree root) {
        super(checker, root);
        this.atypeFactory = (LockAnnotatedTypeFactory)super.atypeFactory;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node);
        if (!type.getAnnotations().isEmpty()) {
            checker.report(Result.failure("unguarded.access", node, type), node);
        }
        return super.visitIdentifier(node, p);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node);
        if (!type.getAnnotations().isEmpty()) {
            checker.report(Result.failure("unguarded.access", node, type), node);
        }
        return super.visitMemberSelect(node, p);
    }

    private <T> List<T> append(List<T> lst, T o) {
        if (o == null)
            return lst;

        List<T> newList = new ArrayList<T>(lst.size() + 1);
        newList.addAll(lst);
        newList.add(o);
        return newList;
    }

    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {
        List<String> prevLocks = atypeFactory.getHeldLock();

        try {
            List<String> locks = append(prevLocks, TreeUtils.skipParens(node.getExpression()).toString());
            atypeFactory.setHeldLocks(locks);
            return super.visitSynchronized(node, p);
        } finally {
            atypeFactory.setHeldLocks(prevLocks);
        }
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        List<String> prevLocks = atypeFactory.getHeldLock();
        List<String> locks = prevLocks;
        try {
            Element method = TreeUtils.elementFromDeclaration(node);
            if (method.getModifiers().contains(Modifier.SYNCHRONIZED)
                || method.getKind() == ElementKind.CONSTRUCTOR) {
                if (method.getModifiers().contains(Modifier.STATIC)) {
                    String enclosingClass = method.getEnclosingElement().getSimpleName().toString();
                    locks = append(locks, enclosingClass + ".class");
                } else {
                    locks = append(locks, "this");
                }
            }

            Holding holding = method.getAnnotation(Holding.class);
            if (holding != null) {
                locks = new ArrayList<String>(locks);
                locks.addAll(Arrays.asList(holding.value()));
            }
            atypeFactory.setHeldLocks(locks);

            return super.visitMethod(node, p);
        } finally {
            atypeFactory.setHeldLocks(prevLocks);
        }
    }

    private static String receiver(ExpressionTree methodSel) {
        if (methodSel.getKind() == Tree.Kind.IDENTIFIER)
            return "this";
        else if (methodSel.getKind() == Tree.Kind.MEMBER_SELECT)
            return ((MemberSelectTree)methodSel).getExpression().toString();
        else
            throw new IllegalArgumentException("Unknown tree type: " + methodSel);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        // does it introduce new locks
        Element methodElt = TreeUtils.elementFromUse(node);

        String lock = receiver(node.getMethodSelect());
        if (methodElt.getSimpleName().contentEquals("lock")) {
            List<String> locks = append(atypeFactory.getHeldLock(), lock);
            atypeFactory.setHeldLocks(locks);
        } else if (methodElt.getSimpleName().contentEquals("unlock")) {
            List<String> locks = new ArrayList<String>(atypeFactory.getHeldLock());
            locks.remove(lock);
            atypeFactory.setHeldLocks(locks);
        }

        // does it require holding a lock
        Holding holding = methodElt.getAnnotation(Holding.class);
        if (holding != null
            && !atypeFactory.getHeldLock().containsAll(Arrays.asList(holding.value()))) {
            checker.report(Result.failure("unguarded.invocation",
                    methodElt, Arrays.toString(holding.value())), node);
        }

        return super.visitMethodInvocation(node, p);
    }

    protected boolean checkMethodInvocability(AnnotatedExecutableType method,
            MethodInvocationTree node) {
        return true;
    }
}
