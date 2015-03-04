package org.checkerframework.framework.util.typeinference;

import com.sun.source.tree.ExpressionTree;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.GeneralAnnotatedTypeFactory;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.framework.util.typeinference.constraint.*;
import org.checkerframework.framework.util.typeinference.solver.*;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredType;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;
import java.util.*;

import static org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil.expressionToArgTrees;
import static org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil.treesToTypes;

/**
 * An implementation of TypeArgumentInference that closely follows the process outlined in JLS7
 * @link http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.7
 *
 * This implementation works in 5 steps:
 *     1.) build up a set of constraints between the method invocation's arguments (to its formal parameters)
 *     2.) solve that set of constraints, inferring as many type arguments as possible
 *     3.) build up a set of constraints between the method invocation's return type and any type it is assigned to
 *         (using object if there is NONE)
 *     4.) for all type arguments that have not been inferred, use the set of constraints from step 3 to infer
 *         their arguments.
 *
 * Note, there are some deviations JLS 7 for the following cases:
 *     a.) Places where the JLS is vague.  For these cases, first the OpenJDK implementation was consulted
 *     and then we favored the behavior we desire rather than the implied behavior of the JLS or JDK implementation.
 *
 *     b.) The fact that any given type variable type may or may not have annotations for multiple hierarchies means
 *     that constraints are more complicated than their Java equivalents.  Every constraint must identify the
 *     hierarchies to which they apply.  This makes solving the constraint sets more complicated.
 */
public class DefaultTypeArgumentInference implements TypeArgumentInference {
    private final EqualitiesSolver equalitiesSolver = new EqualitiesSolver();
    private final SupertypesSolver supertypesSolver = new SupertypesSolver();
    private final SubtypesSolver subtypesSolver = new SubtypesSolver();
    private final ConstraintMapBuilder constraintMapBuilder = new ConstraintMapBuilder();


    @Override
    public Map<TypeVariable, AnnotatedTypeMirror> inferTypeArgs(AnnotatedTypeFactory typeFactory,
                                                                ExpressionTree invocation,
                                                                ExecutableElement methodElem,
                                                                AnnotatedExecutableType methodType) {

        //TODO: REMOVE THIS HACK WHEN YOU CAN CALL getTopAnnotations on GeneralAnnotatedTypeFactory
        //TODO: currently this will only affect inferring METHOD type arguments on constructor
        //TODO: invocations for the Nullness type system
        if (typeFactory instanceof GeneralAnnotatedTypeFactory) {
            return new HashMap<>();
        }

        final Set<TypeVariable> targets = TypeArgInferenceUtil.methodTypeToTargets(methodType);
        final Map<TypeVariable, AnnotatedTypeMirror> inferredArgs = infer(typeFactory, invocation, methodType, targets);
        handleUninferredTypeVariables(typeFactory, methodType, targets, inferredArgs);
        return inferredArgs;
    }


    protected Set<AFConstraint> createArgumentAFConstraints(final AnnotatedTypeFactory typeFactory,
                                                            final ExpressionTree expression,
                                                            final AnnotatedExecutableType methodType,
                                                            final Set<TypeVariable> targets) {
        final List<? extends ExpressionTree> argTrees = expressionToArgTrees(expression);
        final List<AnnotatedTypeMirror> paramTypes = AnnotatedTypes.expandVarArgs(typeFactory, methodType, argTrees);
        final List<AnnotatedTypeMirror> argTypes = treesToTypes(argTrees, typeFactory);

        if (argTypes.size() != paramTypes.size()) {
            ErrorReporter.errorAbort(
                    "Mismatch between formal parameter count and argument count!\n"
                            + "paramTypes=" + PluginUtil.join(",", paramTypes) + "\n"
                            + "argTypes=" + PluginUtil.join(",", argTypes)
            );
        }

        final int numberOfParams = paramTypes.size();
        final LinkedList<AFConstraint> afConstraints = new LinkedList<>();
        for(int i = 0; i < numberOfParams; i++) {
            afConstraints.add(new A2F(argTypes.get(i), paramTypes.get(i)));
        }

        final Set<AFConstraint> reducedConstraints = new LinkedHashSet<>();

        reduceAfConstraints(typeFactory, reducedConstraints, afConstraints, targets);
        return reducedConstraints;
    }

    protected Set<FIsA> createInitialAssignmentConstraints(final AnnotatedTypeMirror assignedTo,
                                                           final AnnotatedTypeMirror boxedReturnType,
                                                           final AnnotatedTypeFactory typeFactory,
                                                           final Set<TypeVariable> targets) {
        final Set<FIsA> result = new LinkedHashSet<>();

        if (assignedTo != null) {
            final Set<AFConstraint> reducedConstraints = new LinkedHashSet<>();

            final Queue<AFConstraint> constraints = new LinkedList<>();
            constraints.add(new F2A(boxedReturnType, assignedTo));

            reduceAfConstraints(typeFactory, reducedConstraints, constraints, targets);

            for (final AFConstraint reducedConstraint : reducedConstraints) {
                if (reducedConstraint instanceof FIsA) {
                    result.add((FIsA) reducedConstraint);
                }
            }
        }

        return result;
    }

    private Pair<InferenceResult,InferenceResult> inferFromArguments(final AnnotatedTypeFactory typeFactory,
                                                                     final ExpressionTree expression,
                                                                     final AnnotatedExecutableType methodType,
                                                                     final Set<AFConstraint> afArgumentConstraints,
                                                                     final Set<TypeVariable> targets) {
        Set<TUConstraint> tuArgConstraints = afToTuConstraints(afArgumentConstraints, targets);
        addConstraintsBetweenTargets(tuArgConstraints, targets, false, typeFactory);

        ConstraintMap argConstraints = constraintMapBuilder.build(targets, tuArgConstraints, typeFactory);

        InferenceResult inferredFromArgEqualities = equalitiesSolver.solveEqualities(targets, argConstraints, typeFactory);

        Set<TypeVariable> remainingTargets =  inferredFromArgEqualities.getRemainingTargets(targets, true);
        InferenceResult inferredFromLubs = supertypesSolver.solveFromArguments(remainingTargets, argConstraints, typeFactory);

        return Pair.of(inferredFromArgEqualities, inferredFromLubs);
    }

    private InferenceResult inferFromAssignmentEqualities(final AnnotatedTypeMirror assignedTo,
                                                          final AnnotatedTypeMirror boxedReturnType,
                                                          final Set<TypeVariable> targets,
                                                          final AnnotatedTypeFactory typeFactory) {
        //TODO: REWORK COMMENT THEY DON'T TAKE PRECEDENCE
        //placing the equality constraints for the return type/assignment context means they
        //will take precedence over the supertype constraints from the arguments (but not the
        //equality constraints of the arguments).
        //Consider the following:
        // <T> Set<T> makeSet(T ... t) {...}
        // Set<@Nullable String> nullableSet = makeSet("a","b","c");         //TODO: ADD THIS AS A TEST
        //
        // Given the types to the var args t, a valid inferred type would be @NonNull String
        // However, since T appears in an invariant location in the return type this causes
        // a type checking error when assigned to nullableSet
        // Inferring the type to be @Nullable String would support both the set of arguments
        // and the return type.
        Set<FIsA> afInitialAssignmentConstraints =
                createInitialAssignmentConstraints(assignedTo, boxedReturnType, typeFactory, targets);


        Set<TUConstraint> tuInitialAssignmentConstraints = afToTuConstraints(afInitialAssignmentConstraints, targets);
        ConstraintMap initialAssignmentConstraints = constraintMapBuilder.build(targets, tuInitialAssignmentConstraints, typeFactory);
        return equalitiesSolver.solveEqualities(targets, initialAssignmentConstraints, typeFactory);
    }

    public ConstraintMap createAssignmentConstraints(final AnnotatedTypeMirror assignedTo,
                                                     final AnnotatedTypeMirror boxedReturnType,
                                                     final AnnotatedExecutableType methodType,
                                                     final Set<AFConstraint> afArgumentConstraints,
                                                     final Map<TypeVariable, AnnotatedTypeMirror> inferredArgs,
                                                     final Set<TypeVariable> targets,
                                                     final AnnotatedTypeFactory typeFactory) {

        //TODO: ANYTHING NOT INFERRED BY EQUALITY IN ARGUMENTS BUT INFERRED BY EQUALITY IN ASSIGNMENT SHOULD
        //TODO: USE THE ASSIGNMENT, NOTE WHY! PERHAPS USE ONLY THE ANNOTATIONS ON NULL VALUES
        final LinkedList<AFConstraint> assignmentAfs = new LinkedList<>();
        for(AnnotatedTypeVariable typeParam : methodType.getTypeVariables()) {
            final TypeVariable target = typeParam.getUnderlyingType();
            final AnnotatedTypeMirror inferredType = inferredArgs.get(target);
            //for all inferred types Ti:  Ti >> Bi where Bi is upper bound and Ti << Li where Li is the lower bound  //TODO: I THINK THIS ENDS UP JUST BEING IGNORED SINCE THE BOUND TYPES ARE ANTs
            //for all uninferred types Tu: Tu >> Bi a nd Lu >> Tu
            if (inferredType != null) {
                assignmentAfs.add(new A2F(inferredType, typeParam.getUpperBound()));
                assignmentAfs.add(new F2A(typeParam.getLowerBound(), inferredType));
            } else {
                assignmentAfs.add(new F2A(typeParam, typeParam.getUpperBound()));
                assignmentAfs.add(new A2F(typeParam.getLowerBound(), typeParam));
            }
        }

        for (AFConstraint argConstraint : afArgumentConstraints) {
            if (argConstraint instanceof F2A) {
                assignmentAfs.add(argConstraint);
            }
        }

        LinkedList<AFConstraint> substitutedAssignmentConstraints = new LinkedList<>();
        for (AFConstraint afConstraint : assignmentAfs) {
            substitutedAssignmentConstraints.add(afConstraint.substitute(inferredArgs));
        }

        final AnnotatedTypeMirror substitutedReturnType = TypeArgInferenceUtil.substitute(inferredArgs, boxedReturnType);
        substitutedAssignmentConstraints.add(new F2A(substitutedReturnType, assignedTo));


        //TODO: THIS IS What we should do
        //if the return type is a primitive, box it
        //substitute the inferred types into the return type
        //for all subtype constraints rewrite them using the inferred types
        //for all equality constraints rewrite them using the inferred types
        //resolve all equality constraints
        //take the glb of all subtype constraints
        //If Ti appears as a type argument in any Uk, then Ti is inferred to be a type variable X whose upper bound is the parameterized type given by glb(U1[Ti=X], ..., Uk[Ti=X]) and whose lower bound is the null type.

        final Set<AFConstraint> reducedConstraints = new LinkedHashSet<>();
        reduceAfConstraints(typeFactory, reducedConstraints, substitutedAssignmentConstraints, targets);
        final Set<TUConstraint> tuAssignmentConstraints = afToTuConstraints(reducedConstraints, targets);
        addConstraintsBetweenTargets(tuAssignmentConstraints, targets, true, typeFactory);
        return constraintMapBuilder.build(targets, tuAssignmentConstraints, typeFactory);
    }

    private InferenceResult inferFromAssignment(final AnnotatedTypeMirror assignedTo,
                                                final AnnotatedTypeMirror boxedReturnType,
                                                final AnnotatedExecutableType methodType,
                                                final Set<AFConstraint> afArgumentConstraints,
                                                final InferenceResult inferredArgs,
                                                final Set<TypeVariable> targets,
                                                final AnnotatedTypeFactory typeFactory) {
        ConstraintMap assignmentConstraints =
                createAssignmentConstraints(assignedTo, boxedReturnType, methodType, afArgumentConstraints,
                        inferredArgs.toAtmMap(), targets, typeFactory);

        InferenceResult equalitiesResult = equalitiesSolver.solveEqualities(targets, assignmentConstraints, typeFactory);

        Set<TypeVariable> remainingTargets = equalitiesResult.getRemainingTargets(targets, true);
        InferenceResult subtypesResult = subtypesSolver.solveFromAssignment(remainingTargets, assignmentConstraints, typeFactory);

        equalitiesResult.mergeSubordinate(subtypesResult);
        return equalitiesResult;
    }

    private Map<TypeVariable, AnnotatedTypeMirror> infer(final AnnotatedTypeFactory typeFactory,
                                                         final ExpressionTree expression,
                                                         final AnnotatedExecutableType methodType,
                                                         final Set<TypeVariable> targets) {

        Set<AFConstraint> afArgumentConstraints = createArgumentAFConstraints(typeFactory, expression, methodType, targets);

        Pair<InferenceResult, InferenceResult> argInference =
                inferFromArguments(typeFactory, expression, methodType, afArgumentConstraints, targets);

        final InferenceResult fromArgEqualities = argInference.first;
        final InferenceResult fromArgSupertypes = argInference.second;

        //if this method invocation's has a return type and it is assigned/pseudo-assigned to
        //a variable, assignedTo is the type of that variable
        final AnnotatedTypeMirror assignedTo =
                TypeArgInferenceUtil.assignedTo(typeFactory, typeFactory.getPath(expression));

        if (assignedTo == null) {
            fromArgEqualities.mergeSubordinate(fromArgSupertypes);
            return fromArgEqualities.toAtmMap();
        } //else

        final AnnotatedTypeMirror declaredReturnType = methodType.getReturnType();
        final AnnotatedTypeMirror boxedReturnType;
        if (declaredReturnType == null) {
            boxedReturnType = null;
        } else if (declaredReturnType.getKind().isPrimitive()) {
            boxedReturnType = typeFactory.getBoxedType((AnnotatedPrimitiveType) declaredReturnType);
        } else {
            boxedReturnType = declaredReturnType;
        }

        InferenceResult fromAssignmentEqualities =
                inferFromAssignmentEqualities(assignedTo, boxedReturnType, targets, typeFactory);

        //TODO: EXPLAIN HERE
        InferenceResult combinedSupertypesAndAssignment =
                combineSupertypeAndAssignmentResults(targets, typeFactory, fromAssignmentEqualities, fromArgSupertypes);

        fromArgEqualities.mergeSubordinate(combinedSupertypesAndAssignment);
        final InferenceResult fromArguments = fromArgEqualities;

        if (!fromArguments.isComplete(targets)) {
            InferenceResult fromAssignment = inferFromAssignment(assignedTo, boxedReturnType, methodType, afArgumentConstraints,
                    fromArguments, targets, typeFactory);

            fromArguments.mergeSubordinate(fromAssignment);
        }

        return fromArguments.toAtmMap();

    }

    private InferenceResult combineSupertypeAndAssignmentResults(Set<TypeVariable> targets, AnnotatedTypeFactory typeFactory,
                                                                 InferenceResult equalityResult, InferenceResult supertypeResult) {
        final TypeHierarchy typeHierarchy = typeFactory.getTypeHierarchy();

        final InferenceResult result = new InferenceResult();
        for(final TypeVariable target : targets) {
            final InferredValue equalityInferred = equalityResult.get(target);
            final InferredValue supertypeInferred = supertypeResult.get(target);

            final InferredValue outputValue;
            if (equalityInferred != null && equalityInferred instanceof InferredType) {

                if (supertypeInferred != null && supertypeInferred instanceof InferredType) {
                    if (typeHierarchy.isSubtype(((InferredType) supertypeInferred).type, ((InferredType) equalityInferred).type)) {
                        outputValue = equalityInferred;
                    } else {
                        outputValue = supertypeInferred;
                    }

                } else {
                    outputValue = equalityInferred;
                }
            } else {
                if (supertypeInferred != null) {
                    outputValue = supertypeInferred;
                } else {
                    outputValue = null;
                }
            }

            if (outputValue != null) {
                result.put(target, outputValue);
            }
        }

        return result;
    }

    private void handleUninferredTypeVariables(AnnotatedTypeFactory typeFactory, AnnotatedExecutableType methodType,
                                              Set<TypeVariable> targets, Map<TypeVariable, AnnotatedTypeMirror> inferredArgs) {
        //TODO: NOT WHAT THE ACTUAL INFERENCE DOES
        for (AnnotatedTypeVariable atv : methodType.getTypeVariables()) {
            final TypeVariable typeVar = atv.getUnderlyingType();
            if (targets.contains(typeVar)) {
                final AnnotatedTypeMirror inferredType = inferredArgs.get(typeVar);

                if (inferredType == null || inferredType.getKind() == TypeKind.NULL) {
                    AnnotatedTypeMirror dummy = typeFactory.getUninferredWildcardType(atv);
                    inferredArgs.put(atv.getUnderlyingType(), dummy);

                    if (inferredType != null) { //then the type kind must be TypeKind.NULL
                        dummy.replaceAnnotations(inferredType.getAnnotations());
                    }
                }
            }
        }
    }

    //TO avoid infinite recursion keep a list of handled constraints
    private void reduceAfConstraints(final AnnotatedTypeFactory typeFactory,
                                     final Set<AFConstraint> outgoing, final Queue<AFConstraint> toProcess, //TODO: Comment note the reason we use the linked hash set is to add to the end
                                     final Set<TypeVariable> targets) {

        final Set<AFConstraint> visited = new HashSet<>();

        List<AFReducer> reducers = new ArrayList<>();
        reducers.add(new A2FReducer(typeFactory));
        reducers.add(new F2AReducer(typeFactory));
        reducers.add(new FIsAReducer(typeFactory));

        Set<AFConstraint> newConstraints = new HashSet<>(10);
        while (!toProcess.isEmpty()) {
            newConstraints.clear();
            AFConstraint constraint = toProcess.remove();

            if (!visited.contains(constraint)) {
                if (constraint.isIrreducible(targets)) {
                    outgoing.add(constraint);
                } else {

                    final Iterator<AFReducer> reducerIterator = reducers.iterator();
                    boolean handled = false;
                    while (!handled && reducerIterator.hasNext()) {
                        handled = reducerIterator.next().reduce(constraint, newConstraints, outgoing);
                    }

                    if (!handled) {
                        ErrorReporter.errorAbort("Unhandled constraint type: " + constraint.toString());
                    }

                    toProcess.addAll(newConstraints);
                }
                visited.add(constraint);
            }
        }
    }


    //TODO: FIX CAPITALIZATION ISSUES
    private Set<TUConstraint> afToTuConstraints(Set<? extends AFConstraint> afConstraints, Set<TypeVariable> targets) {
        final Set<TUConstraint> outgoing = new LinkedHashSet<>();
        for(final AFConstraint afConstraint : afConstraints) {
            if (!afConstraint.isIrreducible(targets)) {
                ErrorReporter.errorAbort(
                        "All afConstraints should be irreducible before conversion.\n"
                                + "afConstraints=[ " + PluginUtil.join(", ", afConstraints) + " ]\n"
                                + "targets=[ " + PluginUtil.join(", ", targets) + "]"
                );
            }

            outgoing.add(afConstraint.toTUConstraint());
        }

        return outgoing;
    }

    public void addConstraintsBetweenTargets(Set<TUConstraint> constraints, Set<TypeVariable> targets,
                                             boolean asSubtype, AnnotatedTypeFactory typeFactory) {
        final Types types = typeFactory.getProcessingEnv().getTypeUtils();
        final List<TypeVariable> targetList = new LinkedList<>(targets);

        final Map<TypeVariable, AnnotatedTypeVariable> paramDeclarations = new HashMap<>();

        while(targetList.size() > 1) {
            final TypeVariable head = targetList.remove(0);

            for(int i = 0; i < targetList.size(); i++) {
                final TypeVariable nextTarget = targetList.get(i);
                if (types.isSameType(head.getUpperBound(), nextTarget)) {
                    final AnnotatedTypeVariable headDecl = addOrGetDeclarations(head, typeFactory, paramDeclarations);
                    final AnnotatedTypeVariable nextDecl = addOrGetDeclarations(nextTarget, typeFactory, paramDeclarations);

                    if (asSubtype) {
                        constraints.add(new TSubU(headDecl, nextDecl));

                    } else {
                        constraints.add(new TSuperU(nextDecl, headDecl));

                    }
                } else if (types.isSameType(nextTarget.getUpperBound(), head)) {
                    final AnnotatedTypeVariable headDecl = addOrGetDeclarations(head, typeFactory, paramDeclarations);
                    final AnnotatedTypeVariable nextDecl = addOrGetDeclarations(nextTarget, typeFactory, paramDeclarations);

                    if (asSubtype) {
                        constraints.add(new TSubU(nextDecl, headDecl));

                    } else {
                        constraints.add(new TSuperU(headDecl, nextDecl));

                    }

                }
            }
        }
    }

    public AnnotatedTypeVariable addOrGetDeclarations(TypeVariable target, AnnotatedTypeFactory typeFactory,
                                                      Map<TypeVariable, AnnotatedTypeVariable> declarations) {
        AnnotatedTypeVariable atv = declarations.get(target);
        if (atv == null) {
            atv = (AnnotatedTypeVariable) typeFactory.getAnnotatedType(target.asElement());
            declarations.put(target, atv);
        }

        return atv;
    }
}
