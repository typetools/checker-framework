package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import javax.lang.model.element.ExecutableElement;

import org.checkerframework.checker.lock.LockAnnotatedTypeFactory.SideEffectAnnotation;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ArrayAccess;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/*
 * The Lock Store behaves like CFAbstractStore but requires the ability
 * to insert exact annotations. This is because we want to be able to
 * insert @LockPossiblyHeld to replace @LockHeld, which normally is
 * not possible in CFAbstractStore since @LockHeld is more specific.
 */
public class LockStore extends CFAbstractStore<CFValue, LockStore> {

    /** If true, indicates that the store refers to a point in the code
      * inside a constructor or initializer. This is useful because
      * constructors and initializers are special with regard to
      * the set of locks that is considered to be held. For example,
      * 'this' is considered to be held inside a constructor.
      */
    protected boolean inConstructorOrInitializer = false;
    private LockAnnotatedTypeFactory atypeFactory;

    public LockStore(LockAnalysis analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
        this.atypeFactory = (LockAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    /** Copy constructor. */
    public LockStore(LockAnalysis analysis,
            CFAbstractStore<CFValue, LockStore> other) {
        super(other);
        inConstructorOrInitializer = ((LockStore)other).inConstructorOrInitializer;
        this.atypeFactory = ((LockStore)other).atypeFactory;
    }

    @Override
    public LockStore leastUpperBound(LockStore other) {
        LockStore newStore = super.leastUpperBound(other);

        // Least upper bound of a boolean
        newStore.inConstructorOrInitializer = this.inConstructorOrInitializer && other.inConstructorOrInitializer;
        newStore.atypeFactory = this.atypeFactory;

        return newStore;
    }

    /*
     * Insert an annotation exactly, without regard to whether an annotation was already present.
     * This is only done for @LockPossiblyHeld. This is not sound for other type qualifiers.
     */
    public void insertLockPossiblyHeld(FlowExpressions.Receiver r) {
        CFValue value = analysis.createSingleAnnotationValue(atypeFactory.LOCKPOSSIBLYHELD, r.getType());
        assert value != null;

        if (r.containsUnknown()) {
            // Expressions containing unknown expressions are not stored.
            return;
        }
        if (r instanceof FlowExpressions.LocalVariable) {
            FlowExpressions.LocalVariable localVar = (FlowExpressions.LocalVariable) r;
            localVariableValues.put(localVar, value);
        } else if (r instanceof FlowExpressions.FieldAccess) {
            FlowExpressions.FieldAccess fieldAcc = (FlowExpressions.FieldAccess) r;
            fieldValues.put(fieldAcc, value);
        } else if (r instanceof FlowExpressions.MethodCall) {
            FlowExpressions.MethodCall method = (FlowExpressions.MethodCall) r;
            methodValues.put(method, value);
        } else if (r instanceof FlowExpressions.ArrayAccess) {
            FlowExpressions.ArrayAccess arrayAccess = (ArrayAccess) r;
            arrayValues.put(arrayAccess, value);
        } else if (r instanceof FlowExpressions.ThisReference) {
            thisValue = value;
        } else if (r instanceof FlowExpressions.ClassName) {
            FlowExpressions.ClassName className = (FlowExpressions.ClassName) r;
            classValues.put(className, value);
        } else {
            // No other types of expressions need to be stored.
        }
    }

    public void setInConstructorOrInitializer() {
        inConstructorOrInitializer = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public /*@Nullable*/ CFValue getValue(FlowExpressions.Receiver expr) {

        if (inConstructorOrInitializer) {
            // 'this' is automatically considered as being held in a constructor or initializer.
            // The class name, however, is not.
            if (expr instanceof FlowExpressions.ThisReference) {
                initializeThisValue(atypeFactory.LOCKHELD, expr.getType());
            } else if (expr instanceof FlowExpressions.FieldAccess) {
                FlowExpressions.FieldAccess fieldAcc = (FlowExpressions.FieldAccess) expr;
                if (!fieldAcc.isStatic() &&
                    fieldAcc.getReceiver() instanceof FlowExpressions.ThisReference) {
                    insertValue(fieldAcc.getReceiver(), atypeFactory.LOCKHELD);
                }
            }
        }

        return super.getValue(expr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalVisualize(CFGVisualizer<CFValue, LockStore, ?> viz) {
        viz.visualizeStoreKeyVal("inConstructorOrInitializer", inConstructorOrInitializer);
        super.internalVisualize(viz);
    }

    @Override
    protected boolean isSideEffectFree(AnnotatedTypeFactory atypeFactory,
            ExecutableElement method) {
        LockAnnotatedTypeFactory lockAnnotatedTypeFactory = (LockAnnotatedTypeFactory) atypeFactory;
        return ((LockChecker) lockAnnotatedTypeFactory.getContext()).hasOption("assumeSideEffectFree") ||
                lockAnnotatedTypeFactory.methodSideEffectAnnotation(method, false) == SideEffectAnnotation.RELEASESNOLOCKS ||
                super.isSideEffectFree(atypeFactory, method);
    }

    @Override
    public void updateForMethodCall(MethodInvocationNode n,
        AnnotatedTypeFactory atypeFactory, CFValue val) {
        super.updateForMethodCall(n, atypeFactory, val);
        ExecutableElement method = n.getTarget().getMethod();
        if (!isSideEffectFree(atypeFactory, method)) {
            // Fields are always modifiable as far as the Lock Checker is concerned, even if they are final.
            fieldValues.clear();

            // Necessary because a method could unlock a lock that is a local variable, e.g.:
            // void foo() {
            //     ReentrantLock lock = new ReentrantLock();
            //     lock.lock();
            //     unlockMyLock(lock);
            // }
            localVariableValues.clear();
            // TODO: This is too conservative. Clear only the values for the local variables that could be affected.
        }
    }

    boolean hasLockHeld(CFValue value) {
        AnnotatedTypeMirror type = value.getType();
        if (type == null) {
            return false;
        }

        return type.hasAnnotation(atypeFactory.LOCKHELD);
    }

    boolean hasLockPossiblyHeld(CFValue value) {
        AnnotatedTypeMirror type = value.getType();
        if (type == null) {
            return false;
        }

        return type.hasAnnotation(atypeFactory.LOCKPOSSIBLYHELD);
    }

    @Override
    public void insertValue(FlowExpressions.Receiver r, /*@Nullable*/ CFValue value) {
        if (value == null) {
            // No need to insert a null abstract value because it represents
            // top and top is also the default value.
            return;
        }
        // Even with concurrent semantics enabled, a @LockHeld value must always be
        // stored for fields and @Pure method calls. This is sound because:
        // -Another thread can never release the lock on the current thread, and
        // -Locks are assumed to be effectively final, hence another thread will not
        // side effect the lock expression that has value @LockHeld.
        if (hasLockHeld(value)) {
            if (r instanceof FlowExpressions.FieldAccess) {
                FlowExpressions.FieldAccess fieldAcc = (FlowExpressions.FieldAccess) r;
                CFValue oldValue = fieldValues.get(fieldAcc);
                CFValue newValue = value.mostSpecific(oldValue, null);
                if (newValue != null) {
                    fieldValues.put(fieldAcc, newValue);
                }
            } else if (r instanceof FlowExpressions.MethodCall) {
                FlowExpressions.MethodCall method = (FlowExpressions.MethodCall) r;
                CFValue oldValue = methodValues.get(method);
                CFValue newValue = value.mostSpecific(oldValue, null);
                if (newValue != null) {
                    methodValues.put(method, newValue);
                }
            }
        }

        super.insertValue(r, value);
    }
}
