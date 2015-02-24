package org.checkerframework.framework.util.typeinference.solver;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredTarget;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredType;

import javax.lang.model.type.TypeVariable;
import java.util.*;
import java.util.Map.Entry;

public class InferenceResult extends LinkedHashMap<TypeVariable, InferredValue> {
    private static final long serialVersionUID = 6911459752070485818L;

    public Set<TypeVariable> getRemainingTargets(final Set<TypeVariable> allTargets, boolean inferredTypesOnly) {
        final LinkedHashSet<TypeVariable> remainingTargets = new LinkedHashSet<>(allTargets);

        if (inferredTypesOnly) {

            for (TypeVariable target : keySet()) {
                if (this.get(target) instanceof InferredType) {
                    remainingTargets.remove(target);
                }

            }

        } else {
            remainingTargets.removeAll(this.keySet());
        }

        return remainingTargets;
    }

    public boolean isComplete(final Set<TypeVariable> targets) {
        for (final TypeVariable target : targets) {
            final InferredValue inferred = this.get(target);

            if (inferred == null || inferred instanceof InferredTarget) {
                return false;
            }
        }
        return this.keySet().containsAll(targets);
    }


    //TODO: EXPLAIN BETTER
    //if we have T0 = T1  and T1 = ATM make the first constraint T0 = ATM
    public void resolveChainedTargets() {
        final Map<TypeVariable, InferredValue> inferredTypes = new LinkedHashMap<>(this.size());

        //TODO: UPDATE THE ConbstraintMap after doing this?
        //TODO: WE CAN MAKE THIS A LITTLE MORE EFFICIENT
        boolean grew = true;
        while (grew == true) {
            grew = false;
            for (final Entry<TypeVariable, InferredValue> inferred : this.entrySet()) {
                final TypeVariable target = inferred.getKey();
                final InferredValue value = inferred.getValue();

                if (value instanceof InferredType) {
                    inferredTypes.put(target, value);

                } else {
                    final InferredTarget currentTarget = (InferredTarget) value;
                    final InferredType equivalentType = (InferredType) inferredTypes.get(((InferredTarget) value).target);

                    if (equivalentType != null) {
                        grew = true;
                        final AnnotatedTypeMirror type = equivalentType.type.deepCopy();
                        type.replaceAnnotations(currentTarget.additionalAnnotations);
                        final InferredType newConstraint =
                                new InferredType(type);

                        inferredTypes.put(currentTarget.target, newConstraint);
                    }
                }
            }
        }

        this.putAll(inferredTypes);
    }

    public Map<TypeVariable, AnnotatedTypeMirror> toAtmMap() {
        final Map<TypeVariable, AnnotatedTypeMirror> result = new LinkedHashMap<>(this.size());
        for (final Entry<TypeVariable, InferredValue> entry : this.entrySet()) {
            final InferredValue inferredValue = entry.getValue();
            if (inferredValue instanceof InferredType) {
                result.put(entry.getKey(), ((InferredType) inferredValue).type);
            }
        }

        return result;
    }

    public void mergeSubordinate(final InferenceResult subordinate) {
        final LinkedHashSet<TypeVariable> previousKeySet = new LinkedHashSet<>(this.keySet());
        final LinkedHashSet<TypeVariable> remainingSubKeys = new LinkedHashSet<>(subordinate.keySet());
        remainingSubKeys.removeAll(keySet());

        for (TypeVariable target : previousKeySet) {
            mergeTarget(target, subordinate);
        }


        for (TypeVariable target : remainingSubKeys) {
            this.put(target, subordinate.get(target));
        }


        //TODO: IS THIS STRICTLY NECESSARY
        resolveChainedTargets();
    }

    protected InferredType mergeTarget(final TypeVariable target, final InferenceResult subordinate) {
        final InferredValue inferred = this.get(target);
        if (inferred instanceof InferredTarget) {
            InferredType newType = mergeTarget(((InferredTarget) inferred).target, subordinate);

            if (newType == null) {
                final InferredValue subValue = subordinate.get(target);
                if (subValue != null && subValue instanceof InferredType) {
                    this.put(target, subValue);
                    return newType;
                }
            } else {
                this.put(target, newType);
                return newType;
            }

            return null;
        } //else

        return (InferredType) inferred;
    }

}
