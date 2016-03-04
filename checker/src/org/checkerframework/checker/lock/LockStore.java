package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import org.checkerframework.checker.lock.LockAnnotatedTypeFactory.SideEffectAnnotation;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockPossiblyHeld;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ArrayAccess;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;

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

    protected final AnnotationMirror LOCKHELD = AnnotationUtils.fromClass(analysis.getTypeFactory().getElementUtils(), LockHeld.class);
    protected final AnnotationMirror LOCKPOSSIBLYHELD = AnnotationUtils.fromClass(analysis.getTypeFactory().getElementUtils(), LockPossiblyHeld.class);

    public LockStore(LockAnalysis analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    /** Copy constructor. */
    public LockStore(LockAnalysis analysis,
            CFAbstractStore<CFValue, LockStore> other) {
        super(other);
        inConstructorOrInitializer = ((LockStore)other).inConstructorOrInitializer;
    }

    @Override
    public LockStore leastUpperBound(LockStore other) {
        LockStore newStore = super.leastUpperBound(other);

        // Least upper bound of a boolean
        newStore.inConstructorOrInitializer = this.inConstructorOrInitializer && other.inConstructorOrInitializer;

        return newStore;
    }

    /*
     * Insert an annotation exactly, without regard to whether an annotation was already present.
     * This is only done for @LockPossiblyHeld. This is not sound for other type qualifiers.
     */
    public void insertLockPossiblyHeld(FlowExpressions.Receiver r) {
        CFValue value = analysis.createSingleAnnotationValue(LOCKPOSSIBLYHELD, r.getType());
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
        } else if (r instanceof FlowExpressions.PureMethodCall) {
            FlowExpressions.PureMethodCall method = (FlowExpressions.PureMethodCall) r;
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
            if (expr instanceof FlowExpressions.ThisReference) {
                initializeThisValue(LOCKHELD, expr.getType());
            } else if (expr instanceof FlowExpressions.FieldAccess) {
                FlowExpressions.FieldAccess fieldAcc = (FlowExpressions.FieldAccess) expr;
                if (!fieldAcc.isStatic() && // Static fields are not automatically considered synchronized within a constructor or initializer
                    fieldAcc.getReceiver() instanceof FlowExpressions.ThisReference) {
                    insertValue(fieldAcc.getReceiver(), LOCKHELD);
                }
            }
        }

        return super.getValue(expr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalDotOutput(StringBuilder result) {
        result.append("  inConstructorOrInitializer = " + inConstructorOrInitializer
                + "\\n");
        super.internalDotOutput(result);
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
            // Necessary because a method could unlock a lock that is a local variable, e.g.:
            // ReentrantLock lock = new ReentrantLock();
            // lock.lock();
            // unlockMyLock(lock);
            localVariableValues.clear();
        }
    }

    boolean hasLockHeld(CFValue value) {
        assert value != null;
        AnnotatedTypeMirror type = value.getType();
        if (type != null) {
            AnnotationMirror anno = type.getAnnotationInHierarchy(LOCKPOSSIBLYHELD);
            if (anno != null) {
                return anno.equals(LOCKHELD);
            }
        }

        return false;
    }

    boolean hasLockPossiblyHeld(CFValue value) {
        assert value != null;
        AnnotatedTypeMirror type = value.getType();
        if (type != null) {
            AnnotationMirror anno = type.getAnnotationInHierarchy(LOCKPOSSIBLYHELD);
            if (anno != null) {
                return anno.equals(LOCKPOSSIBLYHELD);
            }
        }

        return false;
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
            } else if (r instanceof FlowExpressions.PureMethodCall) {
                FlowExpressions.PureMethodCall method = (FlowExpressions.PureMethodCall) r;
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
