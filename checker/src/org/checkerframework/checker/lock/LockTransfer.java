package org.checkerframework.checker.lock;

import java.util.List;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockPossiblyHeld;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.UnderlyingAST.Kind;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.SynchronizedNode;

/*
 * LockTransfer handles constructors and synchronized methods and blocks.
 */
public class LockTransfer extends
    CFAbstractTransfer<CFValue, LockStore, LockTransfer> {

    /** Type-specific version of super.analysis. */
    protected LockAnalysis analysis;
    protected LockChecker checker;

    /** Annotations of the lock type system. */
    protected final AnnotationMirror LOCKHELD, LOCKPOSSIBLYHELD;

    public LockTransfer(LockAnalysis analysis, LockChecker checker) {
        super(analysis);
        this.analysis = analysis;
        this.checker = checker;
        LOCKHELD = AnnotationUtils.fromClass(analysis.getTypeFactory()
                .getElementUtils(), LockHeld.class);
        LOCKPOSSIBLYHELD = AnnotationUtils.fromClass(analysis.getTypeFactory()
                .getElementUtils(), LockPossiblyHeld.class);
    }

    /**
     * Sets a given {@link Node} to @LockHeld in the given {@code store}.
     */
    protected void makeLockHeld(LockStore store, Node node) {
        Receiver internalRepr = FlowExpressions.internalReprOf(
                analysis.getTypeFactory(), node);
        store.insertValue(internalRepr, LOCKHELD);
    }

    /**
     * Sets a given {@link Node} to @LockPossiblyHeld in the given {@code store}.
     */
    protected void makeLockPossiblyHeld(LockStore store, Node node) {
        Receiver internalRepr = FlowExpressions.internalReprOf(
                analysis.getTypeFactory(), node);

        // insertValue cannot change an annotation to a less
        // specific type (e.g. LockHeld to LockPossiblyHeld),
        // so we call insertExactValue.
        store.insertExactValue(internalRepr, LOCKPOSSIBLYHELD);
    }

    /**
     * Sets a given {@link Node} {@code node} to LockHeld in the given
     * {@link TransferResult}.
     */
    protected void makeLockHeld(
            TransferResult<CFValue, LockStore> result, Node node) {
        if (result.containsTwoStores()) {
            makeLockHeld(result.getThenStore(), node);
            makeLockHeld(result.getElseStore(), node);
        } else {
            makeLockHeld(result.getRegularStore(), node);
        }
    }

    /**
     * Sets a given {@link Node} {@code node} to LockPossiblyHeld in the given
     * {@link TransferResult}.
     */
    protected void makeLockPossiblyHeld(
            TransferResult<CFValue, LockStore> result, Node node) {
        if (result.containsTwoStores()) {
            makeLockPossiblyHeld(result.getThenStore(), node);
            makeLockPossiblyHeld(result.getElseStore(), node);
        } else {
            makeLockPossiblyHeld(result.getRegularStore(), node);
        }
    }

    @Override
    public LockStore initialStore(UnderlyingAST underlyingAST,
            /*@Nullable */ List<LocalVariableNode> parameters) {

        LockStore store = super.initialStore(underlyingAST, parameters);

        Kind astKind = underlyingAST.getKind();

        // Methods with the 'synchronized' modifier are
        // holding the 'this' lock.

        // There is a subtle difference between synchronized methods
        // and constructors/initializers. A synchronized method is only
        // taking the intrinsic lock of the current object. It says nothing
        // about any fields of the current object.

        // Furthermore, since the current object already exists,
        // other objects may be @GuardedBy the current object. So
        // a synchronized method can affect the behavior of other
        // objects.

        // A constructor/initializer behaves as if the current object
        // and all its non-static fields were held as locks. But in
        // reality no locks are held.

        // Furthermore, since the current object is being constructed,
        // no other object can be @GuardedBy it or any of its non-static
        // fields.


        // Handle synchronized methods and constructors.
        if (astKind == Kind.METHOD) {
            CFGMethod method = (CFGMethod) underlyingAST;
            MethodTree methodTree = method.getMethod();

            ExecutableElement methodElement = TreeUtils.elementFromDeclaration(methodTree);

            if (methodElement.getModifiers().contains(Modifier.SYNCHRONIZED)) {
                final ClassTree classTree = method.getClassTree();
                TypeMirror classType = InternalUtils.typeOf(classTree);

                store.insertThisValue(LOCKHELD, classType);
            }
            else if (methodElement.getKind() == ElementKind.CONSTRUCTOR) {
                store.setInConstructorOrInitializer();
            }
        }
        else if (astKind == Kind.ARBITRARY_CODE) { // Handle initializers
            store.setInConstructorOrInitializer();
        }

        return store;
    }

    @Override
    public TransferResult<CFValue, LockStore> visitSynchronized(SynchronizedNode n,
            TransferInput<CFValue, LockStore> p) {

        TransferResult<CFValue, LockStore> result = super.visitSynchronized(n,
                p);

        // Handle the entering and leaving of the synchronized block
        if (n.getIsStartOfBlock()) {
            makeLockHeld(result, n.getExpression());
        }
        else {
            makeLockPossiblyHeld(result, n.getExpression());
        }

        return result;
    }
}
