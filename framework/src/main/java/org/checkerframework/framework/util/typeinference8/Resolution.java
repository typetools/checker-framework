package org.checkerframework.framework.util.typeinference8;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.ContainsInferenceVariable;
import org.checkerframework.framework.util.typeinference8.types.Dependencies;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds;
import org.checkerframework.framework.util.typeinference8.types.typemirror.ProperTypeMirror;
import org.checkerframework.framework.util.typeinference8.util.FalseBoundException;
import org.checkerframework.framework.util.typeinference8.util.InternalInferenceUtils;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.TypesUtils;

public class Resolution {
    public static BoundSet resolve(
            Collection<Variable> as, BoundSet boundSet, Java8InferenceContext context) {
        if (as.isEmpty()) {
            return boundSet;
        }
        Dependencies dependencies = boundSet.getDependencies();
        Queue<Variable> unresolvedVars = new ArrayDeque<>(as);
        for (Variable var : as) {
            for (Variable dep : dependencies.get(var)) {
                if (!unresolvedVars.contains(dep)) {
                    unresolvedVars.add(dep);
                }
            }
        }

        List<Variable> resolvedVars = boundSet.getInstantiatedVariables();
        unresolvedVars.removeAll(resolvedVars);
        if (unresolvedVars.isEmpty()) {
            return boundSet;
        }

        Resolution resolution = new Resolution(context, dependencies);
        boundSet = resolution.resolve(boundSet, unresolvedVars);
        assert !boundSet.containsFalse();
        return boundSet;
    }

    public static BoundSet resolve(Variable a, BoundSet boundSet, Java8InferenceContext context) {
        if (a.hasInstantiation()) {
            return boundSet;
        }
        Dependencies dependencies = boundSet.getDependencies();

        LinkedHashSet<Variable> unresolvedVars = new LinkedHashSet<>();
        unresolvedVars.add(a);
        Resolution resolution = new Resolution(context, dependencies);
        boundSet = resolution.resolve(unresolvedVars, boundSet);
        assert !boundSet.containsFalse();
        return boundSet;
    }

    private final Java8InferenceContext context;
    private final Dependencies dependencies;

    private Resolution(Java8InferenceContext context, Dependencies dependencies) {
        this.context = context;
        this.dependencies = dependencies;
    }

    public BoundSet resolve(BoundSet boundSet, Queue<Variable> unresolvedVars) {
        List<Variable> resolvedVars = boundSet.getInstantiatedVariables();

        while (!unresolvedVars.isEmpty()) {
            assert !boundSet.containsFalse();

            LinkedHashSet<Variable> smallestDependencySet =
                    getSmallestDependecySet(resolvedVars, unresolvedVars);

            // Resolve the smallest unresolved dependency set.
            boundSet = resolve(smallestDependencySet, boundSet);

            resolvedVars = boundSet.getInstantiatedVariables();
            unresolvedVars.removeAll(resolvedVars);
        }
        return boundSet;
    }

    private LinkedHashSet<Variable> getSmallestDependecySet(
            List<Variable> resolvedVars, Queue<Variable> unresolvedVars) {
        LinkedHashSet<Variable> smallestDependencySet = null;
        // This loop is looking for the smallest set of dependencies that have not been resolved.
        for (Variable alpha : unresolvedVars) {
            LinkedHashSet<Variable> alphasDependencySet = dependencies.get(alpha);
            alphasDependencySet.removeAll(resolvedVars);

            if (smallestDependencySet == null
                    || alphasDependencySet.size() < smallestDependencySet.size()) {
                smallestDependencySet = alphasDependencySet;
            }

            if (smallestDependencySet.size() == 1) {
                // If the size is 1, then alpha has the smallest possible set of unresolved dependencies.
                // (A variable is always dependent on itself.) So, stop looking for smaller ones.
                break;
            }
        }
        return smallestDependencySet;
    }

    private BoundSet resolve(LinkedHashSet<Variable> as, BoundSet boundSet) {
        assert !boundSet.containsFalse();

        BoundSet resolvedBounds;
        if (boundSet.containsCapture(as)) {
            // First resolve the non-capture variables using the usual resolution algorithm.
            for (Variable ai : as) {
                if (!ai.isCaptureVariable()) {
                    resolveNoCapture(ai);
                }
            }
            as.removeAll(boundSet.getInstantiatedVariables());
            // Then resolve the capture variables
            resolvedBounds = resolveWithCapture(as, boundSet, context);
        } else {
            BoundSet copy = new BoundSet(boundSet);
            try {
                resolvedBounds = resolveNoCapture(as, boundSet);
            } catch (FalseBoundException ex) {
                resolvedBounds = null;
            }
            if (resolvedBounds == null || resolvedBounds.containsFalse()) {
                boundSet = copy;
                boundSet.restore();
                resolvedBounds = resolveWithCapture(as, boundSet, context);
            }
        }
        return resolvedBounds;
    }

    /** https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.4-320-A */
    private BoundSet resolveNoCapture(LinkedHashSet<Variable> as, BoundSet boundSet) {
        BoundSet resolvedBoundSet = new BoundSet(context);
        for (Variable ai : as) {
            assert !ai.hasInstantiation();
            resolveNoCapture(ai);
            if (!ai.hasInstantiation()) {
                resolvedBoundSet.addFalse();
                break;
            }
        }
        boundSet.incorporateToFixedPoint(resolvedBoundSet);
        return boundSet;
    }

    private void resolveNoCapture(Variable ai) {
        assert !ai.hasInstantiation();
        LinkedHashSet<ProperType> lowerBounds = ai.findProperLowerBounds();
        if (!lowerBounds.isEmpty()) {
            TypeMirror ti = null;
            for (ProperType liProperType : lowerBounds) {
                TypeMirror li = liProperType.getJavaType();
                if (ti == null) {
                    ti = li;
                } else {
                    ti = InternalInferenceUtils.lub(context.env, ti, li);
                }
            }
            ai.addBound(VariableBounds.BoundKind.EQUAL, new ProperTypeMirror(ti, context));
            return;
        }

        LinkedHashSet<ProperType> upperBounds = ai.findProperUpperBounds();
        if (!upperBounds.isEmpty()) {
            TypeMirror ti = null;
            boolean useRuntimeEx = false;
            for (ProperType liProperType : upperBounds) {
                TypeMirror li = liProperType.getJavaType();
                if (ai.hasThrowsBound()
                        && context.env.getTypeUtils().isSubtype(context.runtimeEx, li)) {
                    useRuntimeEx = true;
                }
                if (ti == null) {
                    ti = li;
                } else {
                    ti = InternalInferenceUtils.glb(context.env, ti, li);
                }
            }
            if (useRuntimeEx) {
                ai.addBound(
                        VariableBounds.BoundKind.EQUAL,
                        new ProperTypeMirror(context.runtimeEx, context));
            } else {
                ai.addBound(VariableBounds.BoundKind.EQUAL, new ProperTypeMirror(ti, context));
            }
        }
    }

    /** https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.4-320-B */
    private static BoundSet resolveWithCapture(
            LinkedHashSet<Variable> as, BoundSet boundSet, Java8InferenceContext context) {
        assert !boundSet.containsFalse();
        boundSet.removeCaptures(as);
        BoundSet resolvedBoundSet = new BoundSet(context);
        List<Variable> asList = new ArrayList<>();
        List<TypeVariable> typeVar = new ArrayList<>();
        List<TypeMirror> typeArg = new ArrayList<>();

        for (Variable ai : as) {
            ai.applyInstantiationsToBounds(boundSet.getInstantiatedVariables());
            if (ai.hasInstantiation()) {
                // If ai is equal to a variable that was resolved previously,
                // ai would now have an instantiation.
                continue;
            }
            asList.add(ai);
            LinkedHashSet<ProperType> lowerBounds = ai.findProperLowerBounds();
            TypeMirror lowerBound = null;
            for (ProperType liProperType : lowerBounds) {
                TypeMirror li = liProperType.getJavaType();
                if (lowerBound == null) {
                    lowerBound = li;
                } else {
                    lowerBound = InternalInferenceUtils.lub(context.env, lowerBound, li);
                }
            }

            LinkedHashSet<AbstractType> upperBounds = ai.upperBounds();
            TypeMirror upperBound = null;
            for (AbstractType liAb : upperBounds) {
                TypeMirror li = liAb.getJavaType();
                if (upperBound == null) {
                    upperBound = li;
                } else {
                    upperBound = InternalInferenceUtils.glb(context.env, upperBound, li);
                }
            }

            typeVar.add(ai.getJavaType());
            TypeMirror freshTypeVar =
                    TypesUtils.createWildcard(lowerBound, upperBound, context.env.getTypeUtils());
            typeArg.add(freshTypeVar);
        }

        // Recursive types:
        for (int i = 0; i < typeArg.size(); i++) {
            Variable ai = asList.get(i);
            TypeMirror inst = typeArg.get(i);
            TypeVariable typeVariableI = ai.getJavaType();
            if (ContainsInferenceVariable.hasAnyTypeVariable(
                    Collections.singleton(typeVariableI), inst)) {
                // If the instantiation of ai includes a reference to ai,
                // then substitute ai with an unbound wildcard.  This isn't quite right but I'm not
                // sure how to make recursive types Java types.
                // TODO: This causes problems when incorporating the bounds.
                TypeMirror unbound = context.env.getTypeUtils().getWildcardType(null, null);
                inst =
                        TypesUtils.substitute(
                                inst,
                                Collections.singletonList(typeVariableI),
                                Collections.singletonList(unbound),
                                context.env);
                typeArg.remove(i);
                typeArg.add(i, inst);
            }
        }

        // Instantiations that refer to another variable
        List<TypeMirror> subsTypeArg = new ArrayList<>();
        for (TypeMirror type : typeArg) {
            subsTypeArg.add(TypesUtils.substitute(type, typeVar, typeArg, context.env));
        }

        // Create the new bounds.
        for (int i = 0; i < asList.size(); i++) {
            Variable ai = asList.get(i);
            ContainsInferenceVariable.getMentionedTypeVariables(
                    Collections.singleton(ai.getJavaType()), subsTypeArg.get(i));
            ai.addBound(
                    VariableBounds.BoundKind.EQUAL,
                    new ProperTypeMirror(subsTypeArg.get(i), context).capture());
        }

        boundSet.incorporateToFixedPoint(resolvedBoundSet);
        return boundSet;
    }
}
