package org.checkerframework.qualframework.poly;

import java.util.*;

import org.checkerframework.javacutil.Pair;

import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.base.TypeHierarchy;
import org.checkerframework.qualframework.base.QualifierMapVisitor;

import org.checkerframework.qualframework.poly.PolyQual.*;

/** Helper class for performing method qualifier parameter inference.
 */
class MethodParameterInference<Q> {
    /** The names of the qualifier parameters we are trying to infer. */
    private List<String> qualParams;
    /** The types of the method's formal parameters (ordinary parameters, not
     * type or qualifier parameters). */
    private List<? extends QualifiedTypeMirror<QualParams<Q>>> formals;
    /** The actual parameter types from the method call site, after type
     * parameter inference has been done. */
    private List<? extends QualifiedTypeMirror<QualParams<Q>>> actuals;

    private QualifierHierarchy<Q> groundHierarchy;
    private QualifierHierarchy<PolyQual<Q>> polyQualHierarchy;
    private QualifierParameterHierarchy<Q> qualParamHierarchy;
    private TypeHierarchy<QualParams<Q>> typeHierarchy;

    /** This variable will be set to true if an unsatisfiable constraint is
     * found. */
    private boolean unsatisfiable;
    /** The strictest upper bound currently known for each qualifier parameter
     * listed in {@link qualParams}. */
    private List<PolyQual<Q>> upperBounds;
    /** The strictest lower bound currently known for each qualifier parameter
     * listed in {@link qualParams}. */
    private List<PolyQual<Q>> lowerBounds;

    /** Set to true if the infer() method has already been run. */
    private boolean alreadyRan;


    public MethodParameterInference(
            List<String> qualParams,
            List<? extends QualifiedTypeMirror<QualParams<Q>>> formals,
            List<? extends QualifiedTypeMirror<QualParams<Q>>> actuals,
            QualifierHierarchy<Q> groundHierarchy,
            QualifierHierarchy<PolyQual<Q>> polyQualHierarchy,
            QualifierParameterHierarchy<Q> qualParamHierarchy,
            TypeHierarchy<QualParams<Q>> typeHierarchy) {
        this.qualParams = qualParams;
        this.formals = formals;
        this.actuals = actuals;
        this.groundHierarchy = groundHierarchy;
        this.polyQualHierarchy = polyQualHierarchy;
        this.qualParamHierarchy = qualParamHierarchy;
        this.typeHierarchy = typeHierarchy;

        this.unsatisfiable = false;
        this.lowerBounds = new ArrayList<>();
        this.upperBounds = new ArrayList<>();
        for (int i = 0; i < qualParams.size(); ++i) {
            this.upperBounds.add(polyQualHierarchy.getTop());
            this.lowerBounds.add(polyQualHierarchy.getBottom());
        }

        this.alreadyRan = false;
    }


    // Methods for creating and manipulating inference variables.

    private static final String INFER_TAG = "_INFER";

    private QualVar<Q> makeInferVar(String name, int i) {
        return new QualVar<Q>(INFER_TAG + i + ":" + name,
                groundHierarchy.getBottom(), groundHierarchy.getTop());
    }

    private boolean isInferVar(PolyQual<Q> q) {
        return q instanceof QualVar && ((QualVar<Q>)q).getName().startsWith(INFER_TAG);
    }

    private int inferVarIndex(PolyQual<Q> q) {
        QualVar<Q> v = (QualVar<Q>)q;
        String name = v.getName();
        return Integer.parseInt(name.substring(INFER_TAG.length(), name.indexOf(':')));
    }


    /** Run qualifier parameter inference using the arguments provided to the
     * constructor. */
    public Map<String, PolyQual<Q>> infer() {
        if (this.alreadyRan) {
            throw new IllegalStateException("already ran infer() on this MethodParameterInference object");
        }
        this.alreadyRan = true;

        List<Pair<Wildcard<Q>, Wildcard<Q>>> constraints = findConstraints();

        for (Pair<Wildcard<Q>, Wildcard<Q>> p : constraints) {
            if (p == null) {
                unsatisfiable = true;
                continue;
            }

            Wildcard<Q> subset = p.first;
            Wildcard<Q> superset = p.second;

            processConstraint(subset, superset);
        }

        return getAssignment();
    }

    /** Collect the containment constraints that arise from requiring each
     * element in {@code actuals} to be a subtype of the corresponding element
     * of {@code formals}.  The resulting constraints will have inference
     * variables in the place of the method's qualifier variables. */
    private List<Pair<Wildcard<Q>, Wildcard<Q>>> findConstraints() {
        // Replace each qualifier variable named in `this.qualParams` with a
        // new inference variable.  (An inference variable is just a qualifier
        // variable with a special name.)

        // Note that non-capture qualifier variables cannot have bounds other
        // than bottom/top, so we don't need to look at the bounds on the
        // parameter declarations.
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

        // Generate constraints by requiring each actual parameter to be a
        // subtype of the corresponding formal parameter (with inference
        // variables substituted into the formal parameters).

        List<Pair<Wildcard<Q>, Wildcard<Q>>> constraints = new ArrayList<>();
        qualParamHierarchy.setConstraintTarget(constraints);

        for (int i = 0; i < formals.size(); ++i) {
            typeHierarchy.isSubtype(actuals.get(i), substitutedFormals.get(i));
        }

        qualParamHierarchy.setConstraintTarget(null);

        return constraints;
    }

    /** Process a single containment constraint and update {@code upperBounds}
     * and {@code lowerBounds} accordingly. */
    private void processConstraint(Wildcard<Q> subset, Wildcard<Q> superset) {
        if (!isInferVar(subset.getLowerBound())
                && !isInferVar(subset.getUpperBound())
                && !isInferVar(superset.getLowerBound())
                && !isInferVar(superset.getUpperBound())) {
            // There are no vars to infer so the constraint isn't part of the solution
            return;
        }
        addSubtypeBound(superset.getLowerBound(), subset.getLowerBound());
        addSubtypeBound(subset.getLowerBound(), subset.getUpperBound());
        addSubtypeBound(subset.getUpperBound(), superset.getUpperBound());
    }

    private void addSubtypeBound(PolyQual<Q> subtype, PolyQual<Q> supertype) {
        if (isInferVar(subtype) && isInferVar(supertype)) {
            throw new UnsupportedOperationException();
        } else if (isInferVar(subtype)) {
            // INFER#1 <: TAINTED.  So the upper bound for #1 should be no
            // higher than TAINTED.
            int id = inferVarIndex(subtype);
            PolyQual<Q> oldUpper = upperBounds.get(id);
            upperBounds.set(id, polyQualHierarchy.greatestLowerBound(oldUpper, supertype));
        } else if (isInferVar(supertype)) {
            // TAINTED <: INFER#1.  So the lower bound for #1 should be no
            // lower than TAINTED.
            int id = inferVarIndex(supertype);
            PolyQual<Q> oldLower = lowerBounds.get(id);
            lowerBounds.set(id, polyQualHierarchy.leastUpperBound(oldLower, subtype));
        } else {
            // The constraint UNTAINTED <: TAINTED is always true, so do
            // nothing.  The constraint TAINTED <: UNTAINTED is always false,
            // so mark the current set of constraints as unsatisfiable.
            if (!polyQualHierarchy.isSubtype(subtype, supertype)) {
                this.unsatisfiable = true;
            }
        }
    }

    /** Build a map that gives the inferred qualifier for each qualifier
     * parameter, or null if the constraints are unsatisfiable. */
    public Map<String, PolyQual<Q>> getAssignment() {
        if (unsatisfiable) {
            return null;
        }

        Map<String, PolyQual<Q>> map = new HashMap<>();
        for (int i = 0; i < qualParams.size(); ++i) {
            map.put(qualParams.get(i), lowerBounds.get(i));
            // TODO: Check that `lower <: upper`.  The check itself is easy to
            // implement, but getting correct error reporting out of
            // QPTF.methodFromUse is a little harder.
        }

        return map;
    }


    /** Helper visitor to perform substitution at every location in a {@link
     * QualifiedTypeMirror}. */
    private QualifierMapVisitor<QualParams<Q>, QualParams<Q>, Map<String, Wildcard<Q>>> SUBSTITUTE_VISITOR =
        new QualifierMapVisitor<QualParams<Q>, QualParams<Q>, Map<String, Wildcard<Q>>>() {
            @Override
            public QualParams<Q> process(QualParams<Q> params, Map<String, Wildcard<Q>> substs) {
                if (params.equals(qualParamHierarchy.getBottom())) {
                    return qualParamHierarchy.getBottom();
                }

                return params.substituteAll(substs);
            }
        };
}
