package org.checkerframework.checker.lock;

import java.util.ArrayList;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.lock.LockAnnotatedTypeFactory.SideEffectAnnotation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The Lock Store behaves like CFAbstractStore but requires the ability to insert exact annotations.
 * This is because we want to be able to insert @LockPossiblyHeld to replace @LockHeld, which
 * normally is not possible in CFAbstractStore since @LockHeld is more specific.
 */
public class LockStore extends CFAbstractStore<CFValue, LockStore> {

    /**
     * If true, indicates that the store refers to a point in the code inside a constructor or
     * initializer. This is useful because constructors and initializers are special with regard to
     * the set of locks that is considered to be held. For example, 'this' is considered to be held
     * inside a constructor.
     */
    protected boolean inConstructorOrInitializer = false;

    private final LockAnnotatedTypeFactory atypeFactory;

    public LockStore(LockAnalysis analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
        this.atypeFactory = (LockAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    /** Copy constructor. */
    public LockStore(LockAnalysis analysis, CFAbstractStore<CFValue, LockStore> other) {
        super(other);
        this.inConstructorOrInitializer = ((LockStore) other).inConstructorOrInitializer;
        this.atypeFactory = ((LockStore) other).atypeFactory;
    }

    @Override
    public LockStore leastUpperBound(LockStore other) {
        LockStore newStore = super.leastUpperBound(other);

        // Least upper bound of a boolean
        newStore.inConstructorOrInitializer =
                this.inConstructorOrInitializer && other.inConstructorOrInitializer;

        return newStore;
    }

    /*
     * Insert an annotation exactly, without regard to whether an annotation was already present.
     * This is only done for @LockPossiblyHeld. This is not sound for other type qualifiers.
     */
    public void insertLockPossiblyHeld(JavaExpression je) {
        if (je.containsUnknown()) {
            // Expressions containing unknown expressions are not stored.
            return;
        }
        if (je instanceof LocalVariable) {
            LocalVariable localVar = (LocalVariable) je;
            CFValue current = localVariableValues.get(localVar);
            CFValue value = changeLockAnnoToTop(je, current);
            if (value != null) {
                localVariableValues.put(localVar, value);
            }
        } else if (je instanceof FieldAccess) {
            FieldAccess fieldAcc = (FieldAccess) je;
            CFValue current = fieldValues.get(fieldAcc);
            CFValue value = changeLockAnnoToTop(je, current);
            if (value != null) {
                fieldValues.put(fieldAcc, value);
            }
        } else if (je instanceof MethodCall) {
            MethodCall method = (MethodCall) je;
            CFValue current = methodValues.get(method);
            CFValue value = changeLockAnnoToTop(je, current);
            if (value != null) {
                methodValues.put(method, value);
            }
        } else if (je instanceof ArrayAccess) {
            ArrayAccess arrayAccess = (ArrayAccess) je;
            CFValue current = arrayValues.get(arrayAccess);
            CFValue value = changeLockAnnoToTop(je, current);
            if (value != null) {
                arrayValues.put(arrayAccess, value);
            }
        } else if (je instanceof ThisReference) {
            thisValue = changeLockAnnoToTop(je, thisValue);
        } else if (je instanceof ClassName) {
            ClassName className = (ClassName) je;
            CFValue current = classValues.get(className);
            CFValue value = changeLockAnnoToTop(je, current);
            if (value != null) {
                classValues.put(className, value);
            }
        } else {
            // No other types of expressions need to be stored.
        }
    }

    /**
     * Makes a new CFValue with the same annotations as currentValue except that the annotation in
     * the LockPossiblyHeld hierarchy is set to LockPossiblyHeld. If currentValue is null, then a
     * new value is created where the annotation set is LockPossiblyHeld and GuardedByUnknown
     */
    private CFValue changeLockAnnoToTop(JavaExpression je, CFValue currentValue) {
        if (currentValue == null) {
            Set<AnnotationMirror> set = AnnotationUtils.createAnnotationSet();
            set.add(atypeFactory.GUARDEDBYUNKNOWN);
            set.add(atypeFactory.LOCKPOSSIBLYHELD);
            return analysis.createAbstractValue(set, je.getType());
        }

        QualifierHierarchy hierarchy = atypeFactory.getQualifierHierarchy();
        Set<AnnotationMirror> currentSet = currentValue.getAnnotations();
        AnnotationMirror gb =
                hierarchy.findAnnotationInHierarchy(currentSet, atypeFactory.GUARDEDBYUNKNOWN);
        Set<AnnotationMirror> newSet = AnnotationUtils.createAnnotationSet();
        newSet.add(atypeFactory.LOCKPOSSIBLYHELD);
        if (gb != null) {
            newSet.add(gb);
        }
        return analysis.createAbstractValue(newSet, currentValue.getUnderlyingType());
    }

    public void setInConstructorOrInitializer() {
        inConstructorOrInitializer = true;
    }

    @Override
    public @Nullable CFValue getValue(JavaExpression expr) {

        if (inConstructorOrInitializer) {
            // 'this' is automatically considered as being held in a constructor or initializer.
            // The class name, however, is not.
            if (expr instanceof ThisReference) {
                initializeThisValue(atypeFactory.LOCKHELD, expr.getType());
            } else if (expr instanceof FieldAccess) {
                FieldAccess fieldAcc = (FieldAccess) expr;
                if (!fieldAcc.isStatic() && fieldAcc.getReceiver() instanceof ThisReference) {
                    insertValue(fieldAcc.getReceiver(), atypeFactory.LOCKHELD);
                }
            }
        }

        return super.getValue(expr);
    }

    @Override
    protected String internalVisualize(CFGVisualizer<CFValue, LockStore, ?> viz) {
        return viz.visualizeStoreKeyVal("inConstructorOrInitializer", inConstructorOrInitializer)
                + viz.getSeparator()
                + super.internalVisualize(viz);
    }

    @Override
    protected boolean isSideEffectFree(
            AnnotatedTypeFactory atypeFactory, ExecutableElement method) {
        LockAnnotatedTypeFactory lockAnnotatedTypeFactory = (LockAnnotatedTypeFactory) atypeFactory;
        SourceChecker checker = lockAnnotatedTypeFactory.getChecker();
        return checker.hasOption("assumeSideEffectFree")
                || checker.hasOption("assumePure")
                || lockAnnotatedTypeFactory.methodSideEffectAnnotation(method, false)
                        == SideEffectAnnotation.RELEASESNOLOCKS
                || super.isSideEffectFree(atypeFactory, method);
    }

    @Override
    public void updateForMethodCall(
            MethodInvocationNode n, AnnotatedTypeFactory atypeFactory, CFValue val) {
        super.updateForMethodCall(n, atypeFactory, val);
        ExecutableElement method = n.getTarget().getMethod();
        // The following behavior is similar to setting the sideEffectsUnrefineAliases field of
        // Lockannotatedtypefactory, but it affects only one of the two type hierarchies, so it
        // cannot use that logic.
        if (!isSideEffectFree(atypeFactory, method)) {
            // After the call to super.updateForMethodCall, only final fields are left in
            // fieldValues (if the method called is side-effecting). For the LockPossiblyHeld
            // hierarchy, even a final field might be locked or unlocked by a side-effecting
            // method.  So, final fields must be set to @LockPossiblyHeld, but the annotation in
            // the GuardedBy hierarchy should not be changed.
            for (FieldAccess field : new ArrayList<>(fieldValues.keySet())) {
                CFValue newValue = changeLockAnnoToTop(field, fieldValues.get(field));
                if (newValue != null) {
                    fieldValues.put(field, newValue);
                } else {
                    fieldValues.remove(field);
                }
            }

            // Local variables could also be unlocked via an alias
            for (LocalVariable var : new ArrayList<>(localVariableValues.keySet())) {
                CFValue newValue = changeLockAnnoToTop(var, localVariableValues.get(var));
                if (newValue != null) {
                    localVariableValues.put(var, newValue);
                }
            }

            if (thisValue != null) {
                thisValue = changeLockAnnoToTop(null, thisValue);
            }
        }
    }

    boolean hasLockHeld(CFValue value) {
        return AnnotationUtils.containsSame(value.getAnnotations(), atypeFactory.LOCKHELD);
    }

    boolean hasLockPossiblyHeld(CFValue value) {
        return AnnotationUtils.containsSame(value.getAnnotations(), atypeFactory.LOCKPOSSIBLYHELD);
    }

    @Override
    public void insertValue(JavaExpression je, @Nullable CFValue value) {
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
            if (je instanceof FieldAccess) {
                FieldAccess fieldAcc = (FieldAccess) je;
                CFValue oldValue = fieldValues.get(fieldAcc);
                CFValue newValue = value.mostSpecific(oldValue, null);
                if (newValue != null) {
                    fieldValues.put(fieldAcc, newValue);
                }
            } else if (je instanceof MethodCall) {
                MethodCall method = (MethodCall) je;
                CFValue oldValue = methodValues.get(method);
                CFValue newValue = value.mostSpecific(oldValue, null);
                if (newValue != null) {
                    methodValues.put(method, newValue);
                }
            }
        }

        super.insertValue(je, value);
    }
}
