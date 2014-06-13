package org.checkerframework.checker.qualparam;

import java.util.*;

import org.checkerframework.javacutil.Pair;

import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.base.TypeHierarchy;
import org.checkerframework.qualframework.base.QualifierMapVisitor;

import org.checkerframework.checker.qualparam.PolyQual.*;

class InferenceContext<Q> {
    private List<String> qualParams;
    private List<? extends QualifiedTypeMirror<QualParams<Q>>> formals;
    private List<? extends QualifiedTypeMirror<QualParams<Q>>> actuals;

    private QualifierHierarchy<Q> groundHierarchy;
    private QualifierHierarchy<PolyQual<Q>> polyQualHierarchy;

    public InferenceContext(
            List<String> qualParams,
            List<? extends QualifiedTypeMirror<QualParams<Q>>> formals,
            List<? extends QualifiedTypeMirror<QualParams<Q>>> actuals,
            QualifierHierarchy<Q> groundHierarchy,
            QualifierHierarchy<PolyQual<Q>> polyQualHierarchy) {
        this.qualParams = qualParams;
        this.formals = formals;
        this.actuals = actuals;
        this.groundHierarchy = groundHierarchy;
        this.polyQualHierarchy = polyQualHierarchy;
    }

    private static final String INFER_TAG = "_INFER";

    private QualVar<Q> makeInferVar(String name, int i) {
        return new QualVar<Q>(INFER_TAG + i + ":" + name,
                groundHierarchy.getBottom(), groundHierarchy.getTop());
    }

    public void run(TypeHierarchy<QualParams<Q>> typeHierarchy,
            QualifierParameterHierarchy<Q> qualParamHierarchy) {
        List<QualVar<Q>> inferVars = new ArrayList<>();
        Map<String, Wildcard<Q>> inferSubst = new HashMap<>();
        for (int i = 0; i < qualParams.size(); ++i) {
            inferVars.add(makeInferVar(qualParams.get(i), i));
            inferSubst.put(qualParams.get(i), new Wildcard<Q>(inferVars.get(i)));
        }

        List<QualifiedTypeMirror<QualParams<Q>>> substitutedFormals = new ArrayList<>();
        for (QualifiedTypeMirror<QualParams<Q>> formal : formals) {
            substitutedFormals.add(SUBSTITUTE_VISITOR.visit(formal, inferSubst));
        }


        List<Pair<Wildcard<Q>, Wildcard<Q>>> constraints = new ArrayList<>();
        qualParamHierarchy.setConstraintTarget(constraints);

        for (int i = 0; i < formals.size(); ++i) {
            typeHierarchy.isSubtype(actuals.get(i), substitutedFormals.get(i));
        }

        qualParamHierarchy.setConstraintTarget(null);

        this.unsatisfiable = false;
        this.assignments = new ArrayList<>();
        this.lowerBounds = new ArrayList<>();
        this.upperBounds = new ArrayList<>();
        for (int i = 0; i < qualParams.size(); ++i) {
            this.assignments.add(null);
            this.upperBounds.add(polyQualHierarchy.getTop());
            this.lowerBounds.add(polyQualHierarchy.getBottom());
        }

        for (Pair<Wildcard<Q>, Wildcard<Q>> p : constraints) {
            if (p == null) {
                unsatisfiable = true;
                continue;
            }

            Wildcard<Q> subset = p.first;
            Wildcard<Q> superset = p.second;

            processConstraint(subset, superset);
        }
    }

    private QualifierHierarchy<Q> baseHierarchy;
    private boolean unsatisfiable;
    private List<PolyQual<Q>> assignments;
    private List<PolyQual<Q>> upperBounds;
    private List<PolyQual<Q>> lowerBounds;

    private void processConstraint(Wildcard<Q> subset, Wildcard<Q> superset) {
        addSubtypeBound(superset.getLowerBound(), subset.getLowerBound());
        addSubtypeBound(subset.getLowerBound(), subset.getUpperBound());
        addSubtypeBound(subset.getUpperBound(), superset.getUpperBound());
    }

    private boolean isInferVar(PolyQual<Q> q) {
        return q instanceof QualVar && ((QualVar<Q>)q).getName().startsWith(INFER_TAG);
    }

    private int inferVarIndex(PolyQual<Q> q) {
        QualVar<Q> v = (QualVar<Q>)q;
        String name = v.getName();
        return Integer.parseInt(name.substring(INFER_TAG.length(), name.indexOf(':')));
    }

    private void addSubtypeBound(PolyQual<Q> subtype, PolyQual<Q> supertype) {
        if (isInferVar(subtype) && isInferVar(supertype)) {
            throw new UnsupportedOperationException();
        } else if (isInferVar(subtype)) {
            int id = inferVarIndex(subtype);
            PolyQual<Q> oldUpper = upperBounds.get(id);
            upperBounds.set(id, polyQualHierarchy.greatestLowerBound(oldUpper, supertype));
        } else if (isInferVar(supertype)) {
            int id = inferVarIndex(supertype);
            PolyQual<Q> oldLower = lowerBounds.get(id);
            lowerBounds.set(id, polyQualHierarchy.leastUpperBound(oldLower, subtype));
        } else {
            if (!polyQualHierarchy.isSubtype(subtype, supertype)) {
                addFalseBound();
            }
        }
    }

    private void addEqualityBound(PolyQual<Q> a, PolyQual<Q> b) {
        if (isInferVar(a) && isInferVar(b)) {
            throw new UnsupportedOperationException();
        } else if (isInferVar(a)) {
            int id = inferVarIndex(a);
            PolyQual<Q> oldAssign = this.assignments.get(id);
            if (oldAssign != null) {
                addEqualityBound(oldAssign, b);
            }
            this.assignments.set(id, b);
        } else if (isInferVar(b)) {
            addEqualityBound(b, a);
        } else {
            if (!a.equals(b)) {
                addFalseBound();
            }
        }
    }

    private void addFalseBound() {
        this.unsatisfiable = true;
    }

    private QualifierMapVisitor<QualParams<Q>, QualParams<Q>, Map<String, Wildcard<Q>>> SUBSTITUTE_VISITOR =
        new QualifierMapVisitor<QualParams<Q>, QualParams<Q>, Map<String, Wildcard<Q>>>() {
            @Override
            public QualParams<Q> process(QualParams<Q> params, Map<String, Wildcard<Q>> substs) {
                return params.substituteAll(substs);
            }
        };

    public Map<String, PolyQual<Q>> getAssignment() {
        if (unsatisfiable) {
            return null;
        }

        Map<String, PolyQual<Q>> map = new HashMap<>();
        for (int i = 0; i < qualParams.size(); ++i) {
            PolyQual<Q> assign = assignments.get(i);
            PolyQual<Q> lower = lowerBounds.get(i);
            PolyQual<Q> upper = upperBounds.get(i);
            if (assign == null) {
                assign = lower;
            }
            map.put(qualParams.get(i), assign);
            // TODO: check that `lower <: assign <: upper`.
        }

        return map;
    }
}
