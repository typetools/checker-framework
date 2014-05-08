package org.checkerframework.checker.qualparam;

import java.util.*;

import org.checkerframework.javacutil.Pair;

import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.base.TypeHierarchy;
import org.checkerframework.qualframework.util.QualifierMapVisitor;

class InferenceContext<Q> {
    /*
    private List<String> qualParams;
    private List<? extends QualifiedTypeMirror<QualParams<Q>>> formals;
    private List<? extends QualifiedTypeMirror<QualParams<Q>>> actuals;


    public InferenceContext(
            List<String> qualParams,
            List<? extends QualifiedTypeMirror<QualParams<Q>>> formals,
            List<? extends QualifiedTypeMirror<QualParams<Q>>> actuals) {
        this.qualParams = qualParams;
        this.formals = formals;
        this.actuals = actuals;
    }

    public void run(TypeHierarchy<QualParams<Q>> typeHierarchy,
            QualifierParameterHierarchy<Q> qualParamHierarchy) {
        List<InferVar<Q>> inferVars = new ArrayList<>();
        Map<String, ParamValue<Q>> inferSubst = new HashMap<>();
        for (int i = 0; i < qualParams.size(); ++i) {
            inferVars.add(new InferVar<>(qualParams.get(i), i));
            inferSubst.put(qualParams.get(i), inferVars.get(i));
        }

        List<QualifiedTypeMirror<QualParams<Q>>> substitutedFormals = new ArrayList<>();
        for (QualifiedTypeMirror<QualParams<Q>> formal : formals) {
            substitutedFormals.add(SUBSTITUTE_VISITOR.visit(formal, inferSubst));
        }


        List<Pair<ParamValue<Q>, ParamValue<Q>>> constraints = new ArrayList<>();
        qualParamHierarchy.setConstraintTarget(constraints);

        for (int i = 0; i < formals.size(); ++i) {
            typeHierarchy.isSubtype(actuals.get(i), substitutedFormals.get(i));
        }

        qualParamHierarchy.setConstraintTarget(null);

        baseHierarchy = qualParamHierarchy.getContaintmentHierarchy().getBaseHierarchy();


        this.unsatisfiable = false;
        this.assignments = new ArrayList<>();
        this.lowerBounds = new ArrayList<>();
        this.upperBounds = new ArrayList<>();
        for (int i = 0; i < qualParams.size(); ++i) {
            this.assignments.add(null);
            this.upperBounds.add(new BaseQual<>(baseHierarchy.getTop()));
            this.lowerBounds.add(new BaseQual<>(baseHierarchy.getBottom()));
        }

        for (Pair<ParamValue<Q>, ParamValue<Q>> p : constraints) {
            if (p == null) {
                unsatisfiable = true;
                continue;
            }

            ParamValue<Q> subset = p.first;
            ParamValue<Q> superset = p.second;

            processConstraint(subset, superset);
        }
    }

    private QualifierHierarchy<Q> baseHierarchy;
    private boolean unsatisfiable;
    private List<ParamValue<Q>> assignments;
    private List<ParamValue<Q>> upperBounds;
    private List<ParamValue<Q>> lowerBounds;

    private void processConstraint(ParamValue<Q> subset, ParamValue<Q> superset) {
        if (subset instanceof WildcardQual && superset instanceof WildcardQual) {
            WildcardQual<Q> subWild = (WildcardQual<Q>)subset;
            WildcardQual<Q> superWild = (WildcardQual<Q>)superset;
            addSubtypeBound(superWild.getLower(), subWild.getLower());
            addSubtypeBound(subWild.getLower(), subWild.getUpper());
            addSubtypeBound(subWild.getUpper(), superWild.getUpper());
        } else if (subset instanceof WildcardQual) {
            addFalseBound();
        } else if (superset instanceof WildcardQual) {
            WildcardQual<Q> superWild = (WildcardQual<Q>)superset;
            addSubtypeBound(superWild.getLower(), subset);
            addSubtypeBound(subset, superWild.getUpper());
        } else {
            addEqualityBound(superset, subset);
        }
    }

    private void addSubtypeBound(ParamValue<Q> subtype, ParamValue<Q> supertype) {
        if (subtype instanceof InferVar && supertype instanceof InferVar) {
            throw new UnsupportedOperationException();
        } else if (subtype instanceof InferVar) {
            InferVar<Q> subInfer = (InferVar<Q>)subtype;
            ParamValue<Q> oldUpper = upperBounds.get(subInfer.id);
            upperBounds.set(subInfer.id, greatestLowerBound(oldUpper, supertype));
        } else if (supertype instanceof InferVar) {
            InferVar<Q> superInfer = (InferVar<Q>)supertype;
            ParamValue<Q> oldLower = lowerBounds.get(superInfer.id);
            lowerBounds.set(superInfer.id, leastUpperBound(oldLower, subtype));
        } else {
            // `subtype` and `supertype` are either `BaseQual` or `QualVar`.
            Q subMax = subtype.getMaximum().getBase(baseHierarchy);
            Q superMin = supertype.getMinimum().getBase(baseHierarchy);
            if (!baseHierarchy.isSubtype(subMax, superMin)) {
                addFalseBound();
            }
        }
    }

    private void addEqualityBound(ParamValue<Q> a, ParamValue<Q> b) {
        if (a instanceof InferVar && b instanceof InferVar) {
            throw new UnsupportedOperationException();
        } else if (a instanceof InferVar) {
            InferVar<Q> aInfer = (InferVar<Q>)a;
            ParamValue<Q> oldAssign = this.assignments.get(aInfer.id);
            if (oldAssign != null) {
                addEqualityBound(oldAssign, b);
            }
            this.assignments.set(aInfer.id, b);
        } else if (b instanceof InferVar) {
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

    private ParamValue<Q> leastUpperBound(ParamValue<Q> a, ParamValue<Q> b) {
        if (a.equals(b)) {
            return a;
        }
        if (baseHierarchy.isSubtype(a.getMaximum().getBase(baseHierarchy), b.getMinimum().getBase(baseHierarchy))) {
            // In every assignment, a <: b
            return b;
        }
        if (baseHierarchy.isSubtype(b.getMaximum().getBase(baseHierarchy), a.getMinimum().getBase(baseHierarchy))) {
            // In every assignment, b <: a
            return a;
        }
        // Sometimes a <: b and sometimes b <: a.  Return the least upper
        // bound of their maximums.
        return new BaseQual<>(baseHierarchy.leastUpperBound(
                    a.getMaximum().getBase(baseHierarchy), b.getMaximum().getBase(baseHierarchy)));
    }

    private ParamValue<Q> greatestLowerBound(ParamValue<Q> a, ParamValue<Q> b) {
        if (a.equals(b)) {
            return a;
        }
        if (baseHierarchy.isSubtype(a.getMaximum().getBase(baseHierarchy), b.getMinimum().getBase(baseHierarchy))) {
            // In every assignment, a <: b
            return a;
        }
        if (baseHierarchy.isSubtype(b.getMaximum().getBase(baseHierarchy), a.getMinimum().getBase(baseHierarchy))) {
            // In every assignment, b <: a
            return b;
        }
        // Sometimes a <: b and sometimes b <: a.  Return the greatest lower
        // bound of their minimums.
        return new BaseQual<>(baseHierarchy.greatestLowerBound(
                    a.getMinimum().getBase(baseHierarchy), b.getMinimum().getBase(baseHierarchy)));
    }

    private QualifierMapVisitor<QualParams<Q>, QualParams<Q>, Map<String, ParamValue<Q>>> SUBSTITUTE_VISITOR =
        new QualifierMapVisitor<QualParams<Q>, QualParams<Q>, Map<String, ParamValue<Q>>>() {
            @Override
            public QualParams<Q> process(QualParams<Q> params, Map<String, ParamValue<Q>> substs) {
                return params.substituteAll(substs);
            }
        };


    public Map<String, ParamValue<Q>> getAssignment() {
        if (unsatisfiable) {
            return null;
        }

        Map<String, ParamValue<Q>> map = new HashMap<>();
        for (int i = 0; i < qualParams.size(); ++i) {
            ParamValue<Q> assign = assignments.get(i);
            ParamValue<Q> lower = lowerBounds.get(i);
            ParamValue<Q> upper = upperBounds.get(i);
            map.put(qualParams.get(i), assignments.get(i));
        }

        return map;
    }

    private static class InferVar<Q> extends ParamValue<Q> {
        public String name;
        public int id;

        public InferVar(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public ParamValue<Q> substitute(String name, ParamValue<Q> value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ParamValue<Q> capture() {
            throw new UnsupportedOperationException();
        }

        @Override
        public BaseQual<Q> getMinimum() {
            throw new UnsupportedOperationException();
        }

        @Override
        public BaseQual<Q> getMaximum() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "?" + id + ":" + name;
        }
    }
    */
}
