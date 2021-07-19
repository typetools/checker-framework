package org.checkerframework.framework.util.typeinference;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.util.Types;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeVariableSubstitutor;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;

/** Miscellaneous utilities to help in type argument inference. */
public class TypeArgInferenceUtil {

    /**
     * Returns a list of boxed annotated types corresponding to the arguments in {@code
     * methodInvocation}.
     *
     * @param methodInvocation {@link MethodInvocationTree} or {@link NewClassTree}
     * @param typeFactory type factory
     * @return a list of boxed annotated types corresponding to the arguments in {@code
     *     methodInvocation}.
     */
    public static List<AnnotatedTypeMirror> getArgumentTypes(
            final ExpressionTree methodInvocation, final AnnotatedTypeFactory typeFactory) {
        final List<? extends ExpressionTree> argTrees;

    if (methodInvocation.getKind() == Tree.Kind.METHOD_INVOCATION) {
      argTrees = ((MethodInvocationTree) methodInvocation).getArguments();

    } else if (methodInvocation.getKind() == Tree.Kind.NEW_CLASS) {
      argTrees = ((NewClassTree) methodInvocation).getArguments();

        } else {
            throw new BugInCF(
                    "TypeArgumentInference.relationsFromMethodArguments:%n"
                            + "couldn't determine arguments from tree: %s",
                    methodInvocation);
        }

    List<AnnotatedTypeMirror> argTypes =
        CollectionsPlume.mapList(
            (Tree arg) -> {
              AnnotatedTypeMirror argType = typeFactory.getAnnotatedType(arg);
              if (TypesUtils.isPrimitive(argType.getUnderlyingType())) {
                return typeFactory.getBoxedType((AnnotatedPrimitiveType) argType);
              } else {
                return argType;
              }
            },
            argTrees);
    return argTypes;
  }

    /**
     * Given a set of type variables for which we are inferring a type, returns true if type is a
     * use of a type variable in the list of targetTypeVars.
     */
    public static boolean isATarget(
            final AnnotatedTypeMirror type, final Set<TypeVariable> targetTypeVars) {
        return type.getKind() == TypeKind.TYPEVAR
                && targetTypeVars.contains(
                        (TypeVariable)
                                TypeAnnotationUtils.unannotatedType(type.getUnderlyingType()));
    }

    /**
     * Given an AnnotatedExecutableType return a set of type variables that represents the generic
     * type parameters of that method.
     */
    public static Set<TypeVariable> methodTypeToTargets(final AnnotatedExecutableType methodType) {
        final List<AnnotatedTypeVariable> annotatedTypeVars = methodType.getTypeVariables();
        final Set<TypeVariable> targets = new LinkedHashSet<>(annotatedTypeVars.size());

        for (final AnnotatedTypeVariable atv : annotatedTypeVars) {
            targets.add(
                    (TypeVariable) TypeAnnotationUtils.unannotatedType(atv.getUnderlyingType()));
        }

        return targets;
    }

    /**
     * Returns the annotated type that the leaf of path is assigned to, if it is within an
     * assignment context. Returns the annotated type that the method invocation at the leaf is
     * assigned to. If the result is a primitive, return the boxed version.
     *
     * @param atypeFactory the type factory, for looking up types
     * @param path the path whole leaf to look up a type for
     * @return the type of path's leaf
     */
    @SuppressWarnings("interning:not.interned") // AST node comparisons
    public static AnnotatedTypeMirror assignedTo(AnnotatedTypeFactory atypeFactory, TreePath path) {
        Tree assignmentContext = TreePathUtil.getAssignmentContext(path);
        AnnotatedTypeMirror res;
        if (assignmentContext == null) {
            res = null;
        } else if (assignmentContext instanceof AssignmentTree) {
            ExpressionTree variable = ((AssignmentTree) assignmentContext).getVariable();
            res = atypeFactory.getAnnotatedType(variable);
        } else if (assignmentContext instanceof CompoundAssignmentTree) {
            ExpressionTree variable = ((CompoundAssignmentTree) assignmentContext).getVariable();
            res = atypeFactory.getAnnotatedType(variable);
        } else if (assignmentContext instanceof MethodInvocationTree) {
            MethodInvocationTree methodInvocation = (MethodInvocationTree) assignmentContext;
            // TODO move to getAssignmentContext
            if (methodInvocation.getMethodSelect() instanceof MemberSelectTree
                    && ((MemberSelectTree) methodInvocation.getMethodSelect()).getExpression()
                            == path.getLeaf()) {
                return null;
            }
            ExecutableElement methodElt = TreeUtils.elementFromUse(methodInvocation);
            AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(methodInvocation);
            res =
                    assignedToExecutable(
                            atypeFactory,
                            path,
                            methodElt,
                            receiver,
                            methodInvocation.getArguments());
        } else if (assignmentContext instanceof NewArrayTree) {
            // TODO: I left the previous implementation below, it definitely caused infinite loops
            // TODO: if you called it from places like the TreeAnnotator.
            res = null;

            // TODO: This may cause infinite loop
            //            AnnotatedTypeMirror type =
            //                    atypeFactory.getAnnotatedType((NewArrayTree)assignmentContext);
            //            type = AnnotatedTypes.innerMostType(type);
            //            return type;

    } else if (assignmentContext instanceof NewClassTree) {
      // This need to be basically like MethodTree
      NewClassTree newClassTree = (NewClassTree) assignmentContext;
      if (newClassTree.getEnclosingExpression() instanceof NewClassTree
          && (newClassTree.getEnclosingExpression() == path.getLeaf())) {
        return null;
      }
      ExecutableElement constructorElt = TreeUtils.constructor(newClassTree);
      AnnotatedTypeMirror receiver = atypeFactory.fromNewClass(newClassTree);
      res =
          assignedToExecutable(
              atypeFactory, path, constructorElt, receiver, newClassTree.getArguments());
    } else if (assignmentContext instanceof ReturnTree) {
      HashSet<Tree.Kind> kinds =
          new HashSet<>(Arrays.asList(Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.METHOD));
      Tree enclosing = TreePathUtil.enclosingOfKind(path, kinds);

      if (enclosing.getKind() == Tree.Kind.METHOD) {
        res = atypeFactory.getAnnotatedType((MethodTree) enclosing).getReturnType();
      } else {
        AnnotatedExecutableType fninf =
            atypeFactory.getFunctionTypeFromTree((LambdaExpressionTree) enclosing);
        res = fninf.getReturnType();
      }

        } else if (assignmentContext instanceof VariableTree) {
            res = assignedToVariable(atypeFactory, assignmentContext);
        } else {
            throw new BugInCF("AnnotatedTypes.assignedTo: shouldn't be here");
        }

        if (res != null && TypesUtils.isPrimitive(res.getUnderlyingType())) {
            return atypeFactory.getBoxedType((AnnotatedPrimitiveType) res);
        } else {
            return res;
        }
    }

    public static AnnotatedTypeMirror assignedToExecutable(
            AnnotatedTypeFactory atypeFactory,
            TreePath path,
            ExecutableElement methodElt,
            AnnotatedTypeMirror receiver,
            List<? extends ExpressionTree> arguments) {
        AnnotatedExecutableType method =
                AnnotatedTypes.asMemberOf(
                        atypeFactory.getChecker().getTypeUtils(),
                        atypeFactory,
                        receiver,
                        methodElt);
        int treeIndex = -1;
        for (int i = 0; i < arguments.size(); ++i) {
            ExpressionTree argumentTree = arguments.get(i);
            if (isArgument(path, argumentTree)) {
                treeIndex = i;
                break;
            }
        }
        final AnnotatedTypeMirror paramType;
        if (treeIndex == -1) {
            // The tree wasn't found as an argument, so it has to be the receiver.
            // This can happen for inner class constructors that take an outer class argument.
            paramType = method.getReceiverType();
        } else if (treeIndex + 1 >= method.getParameterTypes().size() && methodElt.isVarArgs()) {
            AnnotatedTypeMirror varArgsType =
                    method.getParameterTypes().get(method.getParameterTypes().size() - 1);
            paramType = ((AnnotatedArrayType) varArgsType).getComponentType();
        } else {
            paramType = method.getParameterTypes().get(treeIndex);
        }

        // Examples like this:
        // <T> T outMethod()
        // <U> void inMethod(U u);
        // inMethod(outMethod())
        // would require solving the constraints for both type argument inferences simultaneously
        if (paramType == null || containsUninferredTypeParameter(paramType, method)) {
            return null;
        }

        return paramType;
    }

  /**
   * Returns whether argumentTree is the tree at the leaf of path. If tree is a conditional
   * expression, isArgument is called recursively on the true and false expressions.
   *
   * @param path the path whose leaf to test
   * @param argumentTree the expression that might be path's leaf
   * @return true if {@code argumentTree} is the leaf of {@code path}
   */
  private static boolean isArgument(TreePath path, @FindDistinct ExpressionTree argumentTree) {
    argumentTree = TreeUtils.withoutParens(argumentTree);
    if (argumentTree == path.getLeaf()) {
      return true;
    } else if (argumentTree.getKind() == Tree.Kind.CONDITIONAL_EXPRESSION) {
      ConditionalExpressionTree conditionalExpressionTree =
          (ConditionalExpressionTree) argumentTree;
      return isArgument(path, conditionalExpressionTree.getTrueExpression())
          || isArgument(path, conditionalExpressionTree.getFalseExpression());
    }
    return false;
  }

    /**
     * If the variable's type is a type variable, return getAnnotatedTypeLhsNoTypeVarDefault(tree).
     * Rational:
     *
     * <p>For example:
     *
     * <pre>{@code
     * <S> S bar () {...}
     *
     * <T> T foo(T p) {
     *     T local = bar();
     *     return local;
     *   }
     * }</pre>
     *
     * During type argument inference of {@code bar}, the assignment context is {@code local}. If
     * the local variable default is used, then the type of assignment context type is
     * {@code @Nullable T} and the type argument inferred for {@code bar()} is {@code @Nullable T}.
     * And an incompatible types in return error is issued.
     *
     * <p>If instead, the local variable default is not applied, then the assignment context type is
     * {@code T} (with lower bound {@code @NonNull Void} and upper bound {@code @Nullable Object})
     * and the type argument inferred for {@code bar()} is {@code T}. During dataflow, the type of
     * {@code local} is refined to {@code T} and the return is legal.
     *
     * <p>If the assignment context type was a declared type, for example:
     *
     * <pre>{@code
     * <S> S bar () {...}
     * Object foo() {
     *     Object local = bar();
     *     return local;
     * }
     * }</pre>
     *
     * The local variable default must be used or else the assignment context type is missing an
     * annotation. So, an incompatible types in return error is issued in the above code. We could
     * improve type argument inference in this case and by using the lower bound of {@code S}
     * instead of the local variable default.
     *
     * @param atypeFactory AnnotatedTypeFactory
     * @param assignmentContext VariableTree
     * @return AnnotatedTypeMirror of Assignment context
     */
    public static AnnotatedTypeMirror assignedToVariable(
            AnnotatedTypeFactory atypeFactory, Tree assignmentContext) {
        if (atypeFactory instanceof GenericAnnotatedTypeFactory<?, ?, ?, ?>) {
            final GenericAnnotatedTypeFactory<?, ?, ?, ?> gatf =
                    ((GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory);
            return gatf.getAnnotatedTypeLhsNoTypeVarDefault(assignmentContext);
        } else {
            return atypeFactory.getAnnotatedType(assignmentContext);
        }
    }

  /**
   * Returns true if the type contains a use of a type variable from methodType.
   *
   * @return true if the type contains a use of a type variable from methodType
   */
  private static boolean containsUninferredTypeParameter(
      AnnotatedTypeMirror type, AnnotatedExecutableType methodType) {
    final List<AnnotatedTypeVariable> annotatedTypeVars = methodType.getTypeVariables();
    final List<TypeVariable> typeVars =
        CollectionsPlume.mapList(
            (AnnotatedTypeVariable annotatedTypeVar) ->
                (TypeVariable)
                    TypeAnnotationUtils.unannotatedType(annotatedTypeVar.getUnderlyingType()),
            annotatedTypeVars);

        return containsTypeParameter(type, typeVars);
    }

    /**
     * Returns true if {@code type} contains a use of a type variable in {@code typeVariables}.
     *
     * @param type type to search
     * @param typeVariables collection of type variables
     * @return true if {@code type} contains a use of a type variable in {@code typeVariables}
     */
    public static boolean containsTypeParameter(
            AnnotatedTypeMirror type, Collection<TypeVariable> typeVariables) {
        // note NULL values creep in because the underlying visitor uses them in various places
        final Boolean result = type.accept(new TypeVariableFinder(), typeVariables);
        return result != null && result;
    }

    /**
     * Take a set of annotations and separate them into a mapping of {@code hierarchy top =>
     * annotations in hierarchy}.
     */
    public static AnnotationMirrorMap<AnnotationMirror> createHierarchyMap(
            final AnnotationMirrorSet annos, final QualifierHierarchy qualifierHierarchy) {
        AnnotationMirrorMap<AnnotationMirror> result = new AnnotationMirrorMap<>();

        for (AnnotationMirror anno : annos) {
            result.put(qualifierHierarchy.getTopAnnotation(anno), anno);
        }

        return result;
    }

    /**
     * Throws an exception if the type is an uninferred type argument.
     *
     * <p>The error will be caught in DefaultTypeArgumentInference#infer and inference will be
     * aborted, but type-checking will continue.
     */
    public static void checkForUninferredTypes(AnnotatedTypeMirror type) {
        if (type.getKind() != TypeKind.WILDCARD) {
            return;
        }
        if (((AnnotatedWildcardType) type).isUninferredTypeArgument()) {
            throw new BugInCF("Can't make a constraint that includes an uninferred type argument.");
        }
    }

    /**
     * Used to detect if the visited type contains one of the type variables in the typeVars
     * parameter.
     */
    private static class TypeVariableFinder
            extends AnnotatedTypeScanner<Boolean, Collection<TypeVariable>> {

        /** Create TypeVariableFinder. */
        protected TypeVariableFinder() {
            super(Boolean::logicalOr, false);
        }

        @Override
        public Boolean visitTypeVariable(
                AnnotatedTypeVariable type, Collection<TypeVariable> typeVars) {
            if (typeVars.contains(
                    (TypeVariable) TypeAnnotationUtils.unannotatedType(type.getUnderlyingType()))) {
                return true;
            } else {
                return super.visitTypeVariable(type, typeVars);
            }
        }
    }

    /*
     * Various TypeArgumentInference steps require substituting types for type arguments that have already been
     * inferred into constraints that are used infer other type arguments.  Substituter is used in
     * the utility methods to do this.
     */
    private static final TypeVariableSubstitutor substitutor = new TypeVariableSubstitutor();

    // Substituter requires an input map that the substitute methods build.  We just reuse the same
    // map rather than recreate it each time.
    private static final Map<TypeVariable, AnnotatedTypeMirror> substituteMap = new HashMap<>(5);

    /**
     * Replace all uses of typeVariable with substitution in a copy of toModify using the normal
     * substitution rules. Return the copy
     *
     * @see TypeVariableSubstitutor
     */
    public static AnnotatedTypeMirror substitute(
            final TypeVariable typeVariable,
            final AnnotatedTypeMirror substitution,
            final AnnotatedTypeMirror toModify) {
        substituteMap.clear();
        substituteMap.put(typeVariable, substitution.deepCopy());

        final AnnotatedTypeMirror toModifyCopy = toModify.deepCopy();
        substitutor.substitute(substituteMap, toModifyCopy);
        return toModifyCopy;
    }

    /**
     * Create a copy of toModify. In the copy, for each pair {@code typeVariable => annotated type}
     * replace uses of typeVariable with the corresponding annotated type using normal substitution
     * rules (@see TypeVariableSubstitutor). Return the copy.
     */
    public static AnnotatedTypeMirror substitute(
            Map<TypeVariable, AnnotatedTypeMirror> substitutions,
            final AnnotatedTypeMirror toModify) {
        final AnnotatedTypeMirror substitution;
        if (toModify.getKind() == TypeKind.TYPEVAR) {
            substitution =
                    substitutions.get(
                            (TypeVariable)
                                    TypeAnnotationUtils.unannotatedType(
                                            toModify.getUnderlyingType()));
        } else {
            substitution = null;
        }
        if (substitution != null) {
            return substitution.deepCopy();
        }

        final AnnotatedTypeMirror toModifyCopy = toModify.deepCopy();
        substitutor.substitute(substitutions, toModifyCopy);
        return toModifyCopy;
    }

    /**
     * Successively calls least upper bound on the elements of types. Unlike leastUpperBound, this
     * method will box primitives if necessary
     */
    public static AnnotatedTypeMirror leastUpperBound(
            final AnnotatedTypeFactory typeFactory, final Iterable<AnnotatedTypeMirror> types) {
        final Iterator<AnnotatedTypeMirror> typesIter = types.iterator();
        if (!typesIter.hasNext()) {
            throw new BugInCF("Calling LUB on empty list");
        }
        AnnotatedTypeMirror lubType = typesIter.next();
        AnnotatedTypeMirror nextType = null;
        while (typesIter.hasNext()) {
            nextType = typesIter.next();

            if (lubType.getKind().isPrimitive()) {
                if (!nextType.getKind().isPrimitive()) {
                    lubType = typeFactory.getBoxedType((AnnotatedPrimitiveType) lubType);
                }
            } else if (nextType.getKind().isPrimitive()) {
                if (!lubType.getKind().isPrimitive()) {
                    nextType = typeFactory.getBoxedType((AnnotatedPrimitiveType) nextType);
                }
            }
            lubType = AnnotatedTypes.leastUpperBound(typeFactory, lubType, nextType);
        }

        return lubType;
    }

  /**
   * If the type arguments computed by DefaultTypeArgumentInference don't match the return type
   * mirror of {@code invocation}, then replace those type arguments with an uninferred wildcard.
   */
  protected static Map<TypeVariable, AnnotatedTypeMirror> correctResults(
      Map<TypeVariable, AnnotatedTypeMirror> result,
      ExpressionTree invocation,
      ExecutableType methodType,
      AnnotatedTypeFactory factory) {
    ProcessingEnvironment env = factory.getProcessingEnv();
    Types types = env.getTypeUtils();
    Map<TypeVariable, TypeMirror> fromReturn =
        getMappingFromReturnType(invocation, methodType, env);
    for (Map.Entry<TypeVariable, AnnotatedTypeMirror> entry :
        // result is side-effected by this loop, so iterate over a copy
        new ArrayList<>(result.entrySet())) {
      TypeVariable typeVariable = entry.getKey();
      if (!fromReturn.containsKey(typeVariable)) {
        continue;
      }
      TypeMirror correctType = fromReturn.get(typeVariable);
      TypeMirror inferredType = entry.getValue().getUnderlyingType();
      if (types.isSameType(types.erasure(correctType), types.erasure(inferredType))) {
        if (areSameCapture(correctType, inferredType)) {
          continue;
        }
      }
      if (!types.isSameType(correctType, inferredType)) {
        AnnotatedWildcardType wt =
            factory.getUninferredWildcardType(
                (AnnotatedTypeVariable)
                    AnnotatedTypeMirror.createType(typeVariable, factory, false));
        wt.replaceAnnotations(entry.getValue().getAnnotations());
        result.put(typeVariable, wt);
      }
    }
    return result;
  }

  /**
   * Returns true if actual and inferred are captures of the same wildcard or declared type.
   *
   * @param actual the actual type
   * @param inferred the inferred type
   * @return true if actual and inferred are captures of the same wildcard or declared type
   */
  private static boolean areSameCapture(TypeMirror actual, TypeMirror inferred) {
    if (TypesUtils.isCapturedTypeVariable(actual) && TypesUtils.isCapturedTypeVariable(inferred)) {
      return true;
    } else if (TypesUtils.isCapturedTypeVariable(actual)
        && inferred.getKind() == TypeKind.WILDCARD) {
      return true;
    } else if (actual.getKind() == TypeKind.DECLARED && inferred.getKind() == TypeKind.DECLARED) {
      DeclaredType actualDT = (DeclaredType) actual;
      DeclaredType inferredDT = (DeclaredType) inferred;
      if (actualDT.getTypeArguments().size() == inferredDT.getTypeArguments().size()) {
        for (int i = 0; i < actualDT.getTypeArguments().size(); i++) {
          if (!areSameCapture(
              actualDT.getTypeArguments().get(i), inferredDT.getTypeArguments().get(i))) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

    /**
     * Returns a mapping of type variable to type argument computed using the type of {@code
     * methodInvocationTree} and the return type of {@code methodType}.
     */
    private static Map<TypeVariable, TypeMirror> getMappingFromReturnType(
            ExpressionTree methodInvocationTree,
            ExecutableType methodType,
            ProcessingEnvironment env) {
        TypeMirror methodCallType = TreeUtils.typeOf(methodInvocationTree);
        Types types = env.getTypeUtils();
        GetMapping mapping = new GetMapping(methodType.getTypeVariables(), types);
        mapping.visit(methodType.getReturnType(), methodCallType);
        return mapping.subs;
    }

    /**
     * Helper class for {@link #getMappingFromReturnType(ExpressionTree, ExecutableType,
     * ProcessingEnvironment)}
     */
    private static class GetMapping implements TypeVisitor<Void, TypeMirror> {

        final Map<TypeVariable, TypeMirror> subs = new HashMap<>();
        final List<? extends TypeVariable> typeVariables;
        final Types types;

        private GetMapping(List<? extends TypeVariable> typeVariables, Types types) {
            this.typeVariables = typeVariables;
            this.types = types;
        }

        @Override
        public Void visit(TypeMirror t, TypeMirror mirror) {
            if (t == null || mirror == null) {
                return null;
            }
            return t.accept(this, mirror);
        }

        @Override
        public Void visit(TypeMirror t) {
            return null;
        }

        @Override
        public Void visitPrimitive(PrimitiveType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitNull(NullType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitArray(ArrayType t, TypeMirror mirror) {
            assert mirror.getKind() == TypeKind.ARRAY : mirror;
            return visit(t.getComponentType(), ((ArrayType) mirror).getComponentType());
        }

        @Override
        public Void visitDeclared(DeclaredType t, TypeMirror mirror) {
            assert mirror.getKind() == TypeKind.DECLARED : mirror;
            DeclaredType param = (DeclaredType) mirror;
            if (types.isSubtype(mirror, param)) {
                //                param = (DeclaredType) types.asSuper((Type) mirror, ((Type)
                // param).asElement());
            }
            if (t.getTypeArguments().size() == param.getTypeArguments().size()) {
                for (int i = 0; i < t.getTypeArguments().size(); i++) {
                    visit(t.getTypeArguments().get(i), param.getTypeArguments().get(i));
                }
            }
            return null;
        }

        @Override
        public Void visitError(ErrorType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitTypeVariable(TypeVariable t, TypeMirror mirror) {
            if (typeVariables.contains(t)) {
                subs.put(t, mirror);
            } else if (mirror.getKind() == TypeKind.TYPEVAR) {
                TypeVariable param = (TypeVariable) mirror;
                visit(t.getUpperBound(), param.getUpperBound());
                visit(t.getLowerBound(), param.getLowerBound());
            }
            // else it's not a method type variable
            return null;
        }

        @Override
        public Void visitWildcard(javax.lang.model.type.WildcardType t, TypeMirror mirror) {
            if (mirror.getKind() == TypeKind.WILDCARD) {
                javax.lang.model.type.WildcardType param =
                        (javax.lang.model.type.WildcardType) mirror;
                visit(t.getExtendsBound(), param.getExtendsBound());
                visit(t.getSuperBound(), param.getSuperBound());
            } else if (mirror.getKind() == TypeKind.TYPEVAR) {
                TypeVariable param = (TypeVariable) mirror;
                visit(t.getExtendsBound(), param.getUpperBound());
                visit(t.getSuperBound(), param.getLowerBound());
            } else {
                assert false : mirror;
            }
            return null;
        }

        @Override
        public Void visitExecutable(ExecutableType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitNoType(NoType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitUnknown(TypeMirror t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitUnion(UnionType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitIntersection(IntersectionType t, TypeMirror mirror) {
            assert mirror.getKind() == TypeKind.INTERSECTION : mirror;
            IntersectionType param = (IntersectionType) mirror;
            assert t.getBounds().size() == param.getBounds().size();

            for (int i = 0; i < t.getBounds().size(); i++) {
                visit(t.getBounds().get(i), param.getBounds().get(i));
            }

            return null;
        }
    }
}
