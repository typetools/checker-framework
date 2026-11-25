package org.checkerframework.common.basetype;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeVisitor.CallableDeclaration;
import org.checkerframework.common.basetype.BaseTypeVisitor.FoundRequired;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.FormalParameter;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionConverter;
import org.checkerframework.dataflow.expression.JavaExpressionParseException;
import org.checkerframework.dataflow.expression.SuperReference;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.expression.Unknown;
import org.checkerframework.dataflow.expression.ValueLiteral;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.dataflow.util.PurityChecker;
import org.checkerframework.dataflow.util.PurityKind;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.Contract;
import org.checkerframework.framework.util.Contract.ConditionalPostcondition;
import org.checkerframework.framework.util.Contract.Postcondition;
import org.checkerframework.framework.util.Contract.Precondition;
import org.checkerframework.framework.util.ContractsFromMethod;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtils.MemberReferenceKind;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.ArrayMap;
import org.plumelib.util.ArraySet;

/**
 * Class to perform method override and method reference checks.
 *
 * <p>Method references are checked similarly to method overrides, with the method reference viewed
 * as overriding the functional interface's method.
 *
 * <p>Checks that an overriding method's return type, parameter types, and receiver type are correct
 * with respect to the annotations on the overridden method's return type, parameter types, and
 * receiver type.
 *
 * <p>Furthermore, any contracts on the method must satisfy behavioral subtyping, that is,
 * postconditions must be at least as strong as the postcondition on the superclass, and
 * preconditions must be at most as strong as the condition on the superclass.
 *
 * <p>This method returns the result of the check, but also emits error messages as a side effect.
 */
public class OverrideChecker {

  /** The visitor that uses this to check overrides and method references. */
  protected final BaseTypeVisitor<?> visitor;

  /** The type utilities. */
  private final Types types;

  /**
   * The declaration of an overriding method. Or, it could be a method reference that is being
   * passed to a method.
   */
  protected final Tree overriderTree;

  /** True if {@link #overriderTree} is a MEMBER_REFERENCE. */
  protected final boolean isMethodReference;

  /** The type of the overriding method. */
  protected final AnnotatedExecutableType overrider;

  /** The subtype that declares the overriding method. */
  protected final AnnotatedTypeMirror overriderType;

  /** The type of the overridden method. */
  protected final AnnotatedExecutableType overridden;

  /** The supertype that declares the overridden method. */
  protected final AnnotatedDeclaredType overriddenType;

  /** The return type of the overridden method. */
  protected final AnnotatedTypeMirror overriddenReturnType;

  /** The return type of the overriding method. */
  protected final AnnotatedTypeMirror overriderReturnType;

  /**
   * A mapping from each type variable of the overriding method to the corresponding type variable
   * of the overridden method; null if the two methods have no corresponding type variables, and
   * also null for every method reference.
   *
   * <p>Substituting according to this mapping expresses a type from the overriding method's
   * signature in terms of the overridden method's type variables, which is what makes the two
   * signatures comparable. See {@link #adaptToOverridden}.
   *
   * <p>This mapping is null for a method reference. No mapping is possible: the overrider is the
   * invocation type of the method reference's compile-time declaration, whose type variables, when
   * any remain, do not correspond to the functional interface method's type variables.
   */
  protected final @Nullable Map<TypeVariable, AnnotatedTypeMirror> typeVarMapping;

  /**
   * Creates an OverrideChecker.
   *
   * <p>Notice that the return types are passed in separately. This is to support some types of
   * method references where the overrider's return type is not the appropriate type to check.
   *
   * @param visitor the visitor that uses this to check overrides and method references
   * @param overriderTree the AST node of the overriding method or method reference
   * @param overrider the type of the overriding method
   * @param overriderType the type enclosing the overrider method, usually an AnnotatedDeclaredType;
   *     for method references, it might be something else
   * @param overriderReturnType the return type of the overriding method
   * @param overridden the type of the overridden method
   * @param overriddenType the declared type enclosing the overridden method
   * @param overriddenReturnType the return type of the overridden method
   */
  public OverrideChecker(
      BaseTypeVisitor<?> visitor,
      Tree overriderTree,
      AnnotatedExecutableType overrider,
      AnnotatedTypeMirror overriderType,
      AnnotatedTypeMirror overriderReturnType,
      AnnotatedExecutableType overridden,
      AnnotatedDeclaredType overriddenType,
      AnnotatedTypeMirror overriddenReturnType) {

    this.visitor = visitor;
    this.types = visitor.getTypeFactory().getProcessingEnv().getTypeUtils();
    this.overriderTree = overriderTree;
    this.overrider = overrider;
    this.overriderType = overriderType;
    this.overriderReturnType = overriderReturnType;
    this.overridden = overridden;
    this.overriddenType = overriddenType;
    this.overriddenReturnType = overriddenReturnType;

    this.isMethodReference = overriderTree instanceof MemberReferenceTree;
    this.typeVarMapping = createTypeVarMapping(isMethodReference, overrider, overridden);
  }

  /**
   * Returns a mapping from each type variable of the overriding method to the corresponding type
   * variable of the overridden method, or null if the two methods have no corresponding type
   * variables. Always returns null for a method reference; see {@link #typeVarMapping}.
   *
   * @param isMethodReference true if the overrider is a method reference
   * @param overrider the type of the overriding method
   * @param overridden the type of the overridden method
   * @return a mapping from the overriding method's type variables to the overridden method's, or
   *     null
   */
  private static @Nullable Map<TypeVariable, AnnotatedTypeMirror> createTypeVarMapping(
      boolean isMethodReference,
      AnnotatedExecutableType overrider,
      AnnotatedExecutableType overridden) {
    if (isMethodReference) {
      return null;
    }
    List<AnnotatedTypeVariable> overriderTypeVars = overrider.getTypeVariables();
    List<AnnotatedTypeVariable> overriddenTypeVars = overridden.getTypeVariables();
    if (overriderTypeVars.isEmpty() || overriderTypeVars.size() != overriddenTypeVars.size()) {
      return null;
    }
    Map<TypeVariable, AnnotatedTypeMirror> result =
        ArrayMap.newArrayMapOrHashMap(overriderTypeVars.size());
    for (int i = 0; i < overriderTypeVars.size(); i++) {
      result.put(overriderTypeVars.get(i).getUnderlyingType(), overriddenTypeVars.get(i).asUse());
    }
    return result;
  }

  /**
   * Expresses a type from the overriding method's signature in terms of the overridden method's
   * type variables, by replacing each use of a type variable of the overriding method by the
   * corresponding type variable of the overridden method.
   *
   * <p>Both methods are instantiated with the same type arguments at every call site, so this
   * substitution loses no information. It permits the ordinary subtyping rules for two uses of the
   * same type variable to be applied to the two methods' signatures.
   *
   * @param type a type that might contain uses of the overriding method's type variables
   * @return {@code type}, expressed in terms of the overridden method's type variables
   */
  protected AnnotatedTypeMirror adaptToOverridden(AnnotatedTypeMirror type) {
    if (typeVarMapping == null || !containsAnyTypeVariable(type, typeVarMapping.keySet())) {
      return type;
    }
    return visitor.atypeFactory.getTypeVarSubstitutor().substitute(typeVarMapping, type);
  }

  /**
   * Returns true if the given type contains a use of any of the given type variables. This is
   * cheaper than substitution, which copies the type even when it substitutes nothing.
   *
   * @param type a type
   * @param typeVars type variables
   * @return true if {@code type} contains a use of any of {@code typeVars}
   */
  private boolean containsAnyTypeVariable(
      AnnotatedTypeMirror type, Collection<? extends TypeVariable> typeVars) {
    Type underlying = (Type) type.getUnderlyingType();
    for (TypeVariable typeVar : typeVars) {
      if (underlying.contains((Type) typeVar)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Performs the check.
   *
   * @return true if the override is allowed
   */
  public boolean checkOverride() {
    if (visitor.checker.shouldSkipUses(overriddenType.getUnderlyingType().asElement())) {
      return true;
    }

    boolean result = checkTypeParameterBounds();
    result &= checkReturn();
    result &= checkParameters();
    if (isMethodReference) {
      result &= checkMemberReferenceReceivers();
      checkMethodReferencePurity();
    } else {
      result &= checkReceiverOverride();
    }
    checkPreAndPostConditions();
    checkSideEffectsOnly();

    return result;
  }

  /**
   * Formats a method's name and formal parameters for a diagnostic message.
   *
   * @param method a method
   * @return the method's name and formal parameters
   */
  private String methodSignature(ExecutableElement method) {
    List<? extends VariableElement> parameters = method.getParameters();
    StringJoiner result =
        new StringJoiner(", ", ElementUtils.getSimpleDescription(method) + "(", ")");
    for (int i = 0; i < parameters.size(); i++) {
      VariableElement parameter = parameters.get(i);
      TypeMirror parameterType = parameter.asType();
      String typeString;
      if (method.isVarArgs() && i == parameters.size() - 1) {
        typeString =
            TypesUtils.simpleTypeName(((ArrayType) parameterType).getComponentType()) + "...";
      } else {
        typeString = TypesUtils.simpleTypeName(parameterType);
      }
      result.add(typeString + " " + parameter.getSimpleName());
    }
    return result.toString();
  }

  /**
   * Checks that the referenced method is at least as pure as each method through which a call can
   * run it: the functional interface method that the method reference implements, and the other
   * methods that {@link BaseTypeVisitor#methodsThatRun} returns.
   *
   * <p>Reports an error for each of those methods that requires a purity kind that the referenced
   * method lacks, except a method whose missing kinds are among those of another reported method.
   * Thus, the errors together name every missing kind, and no error is redundant.
   *
   * <p>Only a method reference needs this check. A method that overrides another one inherits its
   * purity annotations, and is checked by {@link BaseTypeVisitor#checkPurityAnnotations}.
   *
   * <p>Under {@code -Ainfer}, reports no error. Instead, {@link
   * BaseTypeVisitor#inferMethodReferencePurity} uses the method reference to infer the purity of
   * the same methods.
   */
  private void checkMethodReferencePurity() {
    // Do not report errors while inferring.  The purity annotations on the functional interface
    // method may have been written by inference, which has not yet taken this method reference
    // into account.  The annotations are checked when the inference output is type-checked.
    if (!visitor.checkPurityAnnotations || visitor.infer) {
      return;
    }
    // Apply the command-line assumptions to the referenced method, so that a method reference is
    // treated like a lambda whose body calls the referenced method.  Like such a call, a
    // side-effect-free referenced method that returns void is deterministic.
    EnumSet<PurityKind> subPurity = purityKinds(overrider.getElement(), true);
    // `f::apply`, where `f` is a functional-interface parameter of the enclosing method, denotes
    // the code that the caller of that method was required to check, as in
    // `checkFunctionalArgument`.
    EnumSet<PurityKind> parameterPurity =
        PurityChecker.functionalParameterCallPurity(
            visitor.atypeFactory,
            ((MemberReferenceTree) overriderTree).getQualifierExpression(),
            overrider.getElement(),
            TreePathUtil.enclosingMethod(visitor.getCurrentPath()),
            visitor.atypeFactory.getProcessingEnv());
    if (parameterPurity.contains(PurityKind.SIDE_EFFECT_FREE)) {
      subPurity.remove(PurityKind.SIDE_EFFECTS_ONLY);
    }
    subPurity.addAll(parameterPurity);

    List<CallableDeclaration> declarations = methodsThatRunReference();
    // The methods to report, each paired with the purity kinds that it requires and the
    // referenced method lacks.  No element's kinds are a subset of another element's kinds.
    Map<ExecutableElement, EnumSet<PurityKind>> toReport = new LinkedHashMap<>();
    for (CallableDeclaration callable : declarations) {
      ExecutableElement declaration = callable.declaration();
      EnumSet<PurityKind> superPurity = purityKinds(declaration, false);
      EnumSet<PurityKind> missing = EnumSet.copyOf(superPurity);
      missing.removeAll(subPurity);
      if (superPurity.contains(PurityKind.SIDE_EFFECTS_ONLY)) {
        boolean sideEffectsOk;
        if (!visitor.checkPurityAnnotationsOption) {
          // A @SideEffectsOnly annotation is verified only when -AcheckPurityAnnotations itself
          // was supplied, not merely -AsuggestPureMethods or -Ainfer; see
          // checkSideEffectsOnlyAnnotation.
          sideEffectsOk = true;
        } else if (subPurity.contains(PurityKind.SIDE_EFFECT_FREE)) {
          sideEffectsOk = true;
        } else if (subPurity.contains(PurityKind.SIDE_EFFECTS_ONLY)) {
          // Both methods are annotated with @SideEffectsOnly, and the referenced method is
          // allowed to perform fewer side effects.
          sideEffectsOk = sideEffectsOnlyIsNarrowed(declaration);
        } else {
          // The declaration has @SideEffectsOnly, and the referenced method has no side-effect
          // annotation.  The referenced method inherits nothing from the declaration.
          sideEffectsOk = false;
        }
        if (sideEffectsOk) {
          missing.remove(PurityKind.SIDE_EFFECTS_ONLY);
        } else {
          missing.add(PurityKind.SIDE_EFFECTS_ONLY);
        }
      }
      if (missing.isEmpty()
          || toReport.values().stream().anyMatch(kinds -> kinds.containsAll(missing))) {
        continue;
      }
      toReport.values().removeIf(missing::containsAll);
      toReport.put(declaration, missing);
    }

    for (ExecutableElement declaration : toReport.keySet()) {
      TypeElement declaringType = (TypeElement) declaration.getEnclosingElement();
      // The type that the error message says declares `declaration`.
      AnnotatedTypeMirror declarationOwnerType;
      AnnotatedExecutableType declarationType;
      if (types.isSubtype(
          types.erasure(overriddenType.getUnderlyingType()),
          types.erasure(declaringType.asType()))) {
        declarationOwnerType = overriddenType;
        declarationType =
            declaration.equals(overridden.getElement())
                ? overridden
                : AnnotatedTypes.asMemberOf(
                    types, visitor.atypeFactory, overriddenType, declaration);
      } else {
        // The declaration is in the hierarchy of another bound of an intersection cast.
        declarationOwnerType = visitor.atypeFactory.getAnnotatedType(declaringType);
        declarationType = visitor.atypeFactory.getAnnotatedType(declaration);
      }
      visitor.checker.reportError(
          overriderTree,
          "purity.methodref",
          overriderType,
          purityKindsToString(subPurity, overrider.getElement()),
          overrider,
          declarationOwnerType,
          purityKindsToString(purityKinds(declaration, false), declaration),
          declarationType);
    }
    if (!visitor.infer) {
      checkMethodReferenceFunctionalParameters(declarations);
    }
  }

  /**
   * Checks that every call that can run the referenced method through the method reference
   * guarantees, of the code that it passes to each functional-interface parameter of the referenced
   * method, the purity that the referenced method's body assumes of it. That is the purity that
   * {@link PurityChecker#functionalParameterPurity} computes, less the purity that every call to
   * the parameter's functional method has regardless of the argument: the purity of the functional
   * method's own annotations and of the command-line assumptions such as {@code
   * -AassumeSideEffectFree}.
   *
   * <p>Each call checks its arguments against the declaration that it invokes. The arguments
   * themselves have the declaration's parameter types as members of the type through which the call
   * is made, which can be more specific.
   *
   * @param declarations the methods through which a call can run the referenced method, as computed
   *     by {@link #methodsThatRunReference}
   */
  private void checkMethodReferenceFunctionalParameters(List<CallableDeclaration> declarations) {
    ExecutableElement referenced = overrider.getElement();
    ProcessingEnvironment env = visitor.atypeFactory.getProcessingEnv();
    if (PurityChecker.functionalParameterKinds(visitor.atypeFactory, referenced, env).isEmpty()) {
      // The referenced method assumes nothing of its functional-interface arguments.
      return;
    }
    int offset = methodRefParameterOffset();
    List<? extends VariableElement> params = referenced.getParameters();
    for (int i = 0; i < params.size(); i++) {
      ExecutableElement parameterFunction =
          PurityChecker.parameterFunctionalMethod(referenced, i, env);
      if (parameterFunction == null) {
        continue;
      }
      EnumSet<PurityKind> assumed =
          PurityChecker.functionalParameterPurity(
              visitor.atypeFactory, referenced, i, parameterFunction, env);
      assumed.removeAll(visitor.implementationPurityKinds(parameterFunction));
      if (assumed.isEmpty()) {
        continue;
      }
      for (CallableDeclaration callable : declarations) {
        ExecutableElement declaration = callable.declaration();
        int declarationIndex = i + offset;
        if (declarationIndex >= declaration.getParameters().size()) {
          continue;
        }
        // Use the parameter's type in the type through which the call is made, such as the
        // functional interface type that the method reference implements, so that a type
        // argument such as the `SafeCallback` of `Target<SafeCallback>` supplies the purity of
        // its functional method.
        ExecutableType declarationType =
            (ExecutableType) types.asMemberOf(callable.receiverType(), declaration);
        EnumSet<PurityKind> provided =
            PurityChecker.guaranteedFunctionalArgumentPurity(
                visitor.atypeFactory,
                declaration,
                declarationIndex,
                declarationType.getParameterTypes().get(declarationIndex),
                parameterFunction,
                env);
        if (!provided.containsAll(assumed)) {
          // The error message names the first declaration whose calls lack the guarantee.
          if (visitor.mightRelyOnFunctionalParameter(
              referenced, params.get(i), parameterFunction)) {
            visitor.reportErrorOnce(
                overriderTree,
                "purity.methodref.functional.parameter",
                params.get(i).getSimpleName(),
                ElementUtils.getSimpleDescription(referenced),
                BaseTypeVisitor.purityKindsToString(assumed),
                ElementUtils.getSimpleDescription(declaration),
                BaseTypeVisitor.purityKindsToString(provided));
          }
          break;
        }
      }
    }
  }

  /**
   * Returns the methods through which a call can run the referenced method (see {@link
   * BaseTypeVisitor#methodsThatRun}), each with the type through which such a call is made. That
   * type is the functional interface type that the method reference implements, unless the method
   * is in the hierarchy of another bound of an intersection cast; then it is that bound.
   *
   * @return the methods through which a call can run the referenced method, starting with the
   *     functional interface method
   */
  private List<CallableDeclaration> methodsThatRunReference() {
    DeclaredType targetType = overriddenType.getUnderlyingType();
    List<DeclaredType> castBounds = visitor.intersectionCastBounds(overriderTree);
    List<CallableDeclaration> result = new ArrayList<>();
    for (ExecutableElement method :
        visitor.methodsThatRun(overriderTree, overridden.getElement())) {
      TypeMirror erasedOwner = types.erasure(method.getEnclosingElement().asType());
      DeclaredType receiverType = targetType;
      if (!types.isSubtype(types.erasure(targetType), erasedOwner)) {
        for (DeclaredType bound : castBounds) {
          if (types.isSubtype(types.erasure(bound), erasedOwner)) {
            receiverType = bound;
            break;
          }
        }
      }
      result.add(new CallableDeclaration(method, receiverType));
    }
    return result;
  }

  /**
   * Returns the index of the functional interface method's parameter that corresponds to the
   * referenced method's first parameter. It is 1 for an unbound method reference such as {@code
   * Collection::clear}, whose functional interface method's first parameter is the referenced
   * method's receiver; then the interface method's parameter {@code #(i+1)} is the referenced
   * method's parameter {@code #i}. It is 0 for every other method reference.
   *
   * <p>This method requires that {@link #overriderTree} is a method reference.
   *
   * @return 1 if the method reference is unbound, and 0 otherwise
   */
  private int methodRefParameterOffset() {
    return MemberReferenceKind.getMemberReferenceKind((MemberReferenceTree) overriderTree)
            .isUnbound()
        ? 1
        : 0;
  }

  /**
   * Returns the purity kinds of {@code method}, including {@link PurityKind#SIDE_EFFECTS_ONLY} if
   * {@code method} is not side-effect-free and has or inherits a {@code @SideEffectsOnly}
   * annotation.
   *
   * <p>{@link PurityUtils#getPurityKinds} never reports {@link PurityKind#SIDE_EFFECTS_ONLY}.
   * {@code @SideEffectsOnly} is not inherited as an annotation; see {@link
   * AnnotatedTypeFactory#getSideEffectsOnlyExpressionMap}.
   *
   * @param method a method
   * @param isImplementation true if {@code method} is the referenced method, which implements the
   *     functional interface method. If so, the result also includes what the command-line
   *     assumptions, such as {@code -AassumeSideEffectFree}, grant {@code method}, and it includes
   *     {@link PurityKind#DETERMINISTIC} if {@code method} is side-effect-free and returns void;
   *     see {@link BaseTypeVisitor#implementationPurityKinds}.
   * @return the purity kinds of {@code method}
   */
  private EnumSet<PurityKind> purityKinds(ExecutableElement method, boolean isImplementation) {
    EnumSet<PurityKind> result =
        isImplementation
            ? visitor.implementationPurityKinds(method)
            : PurityUtils.getPurityKinds(visitor.atypeFactory, method);
    if (!result.contains(PurityKind.SIDE_EFFECT_FREE)
        && visitor.atypeFactory.getSideEffectsOnlyExpressionMap(method) != null) {
      result.add(PurityKind.SIDE_EFFECTS_ONLY);
    }
    return result;
  }

  /**
   * Returns true if the overriding method's {@code @SideEffectsOnly} annotation permits no more
   * side effects than {@code superMethod}'s does. That is the case when every expression in the
   * overrider's annotation is covered by some expression in {@code superMethod}'s annotation: it is
   * that expression, or it is reached through it.
   *
   * <p>This method requires that both the overrider and {@code superMethod} are annotated with
   * {@code @SideEffectsOnly}.
   *
   * @param superMethod the overridden method, or another method through which a call can run the
   *     referenced method; see {@link #methodsThatRunReference}
   * @return true if the overrider's side effects are within {@code superMethod}'s
   */
  private boolean sideEffectsOnlyIsNarrowed(ExecutableElement superMethod) {
    Map<ExecutableElement, List<String>> seOnlySuperStrings =
        visitor.atypeFactory.getSideEffectsOnlyExpressionMap(superMethod);
    Map<ExecutableElement, List<String>> seOnlySubStrings =
        visitor.atypeFactory.getSideEffectsOnlyExpressionMap(overrider.getElement());
    if (seOnlySuperStrings == null || seOnlySubStrings == null) {
      throw new BugInCF(
          "sideEffectsOnlyIsNarrowed requires @SideEffectsOnly on both methods: %s %s",
          superMethod, overrider.getElement());
    }

    List<JavaExpression> seOnlySuperExpressions = parseSideEffectsOnly(seOnlySuperStrings);
    List<JavaExpression> seOnlySubExpressions = parseReceiverRelative(seOnlySubStrings);
    if (seOnlySuperExpressions == null || seOnlySubExpressions == null) {
      // An argument could not be parsed, so the annotations cannot be compared.  The strings
      // cannot be compared instead, because the two methods' parameters need not correspond
      // positionally.  The parse error itself is reported at the declaration that contains it,
      // if that declaration is being compiled; if it appears in a stub file or in another
      // compilation unit, it is reported nowhere.
      return false;
    }

    seOnlySubExpressions = atFunctionalInterfaceMethod(seOnlySubExpressions);
    if (seOnlySubExpressions == null) {
      return false;
    }

    for (JavaExpression seOnlySubExpression : seOnlySubExpressions) {
      if (!isPermittedSideEffect(seOnlySubExpression, seOnlySuperExpressions)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Parses the given {@code @SideEffectsOnly} expressions, as {@link #parseSideEffectsOnly} does,
   * but replaces each {@code this} that is not the receiver of the method that declares it, such as
   * {@code Outer.this}, by {@link Unknown}. Afterward, every {@link ThisReference} in the result is
   * the receiver of the declaring method, and therefore of the method that inherits the annotation.
   *
   * @param expressionStrings {@code @SideEffectsOnly} expressions, indexed by the method whose
   *     declaration contains them
   * @return the parsed expressions, or null if any of them cannot be parsed
   */
  private @Nullable List<JavaExpression> parseReceiverRelative(
      Map<ExecutableElement, List<String>> expressionStrings) {
    List<JavaExpression> result = new ArrayList<>();
    for (Map.Entry<ExecutableElement, List<String>> entry : expressionStrings.entrySet()) {
      List<JavaExpression> parsed =
          parseSideEffectsOnly(Collections.singletonMap(entry.getKey(), entry.getValue()));
      if (parsed == null) {
        return null;
      }
      TypeElement declaringClass = ElementUtils.enclosingTypeElement(entry.getKey());
      JavaExpressionConverter outerThisToUnknown =
          new JavaExpressionConverter() {
            @Override
            protected JavaExpression visitThisReference(ThisReference thisExpr, Void unused) {
              // The parser gives the receiver `this` the type of the declaring class, and an
              // outer `this`, whether written or implicit, the type of the outer class.
              if (Objects.equals(declaringClass, TypesUtils.getTypeElement(thisExpr.getType()))) {
                return thisExpr;
              }
              return new Unknown(thisExpr.getType());
            }
          };
      for (JavaExpression expr : parsed) {
        result.add(outerThisToUnknown.convert(expr));
      }
    }
    return result;
  }

  /**
   * Translates expressions written at the declaration of the method that a method reference refers
   * to, into the frame of the functional interface method that the reference implements.
   *
   * <p>The two methods' parameters do not necessarily correspond positionally; see {@link
   * #methodRefParameterOffset}. When the referenced method is invoked with variable arity, an
   * element {@code #n[k]} of its varargs parameter {@code #n} is the interface method's
   * corresponding parameter, provided that the referenced method cannot reassign the element (see
   * {@link BaseTypeVisitor#varargsElementsMightBeReassigned}); any other use of {@code #n} has no
   * counterpart there, because the array is created for the call.
   *
   * <p>For an unbound reference, {@code this} and {@code super} are the interface method's first
   * parameter. For a constructor reference, the expression {@code this} is dropped, because the
   * object under construction did not exist before the call. An expression that is reached through
   * that object, such as {@code this.f}, may denote an object that existed before the call, but the
   * interface method's annotation cannot name it; this method returns null in that case. For a
   * bound or {@code super} reference, {@code this} is the bound receiver, which the interface
   * method's annotation cannot name either; this method returns null in that case too.
   *
   * <p>This method requires that {@link #overriderTree} is a method reference.
   *
   * @param subExpressions expressions written at the referenced method's declaration, as returned
   *     by {@link #parseReceiverRelative}
   * @return the expressions written at the interface method's declaration, or null if some
   *     expression has no counterpart there
   */
  private @Nullable List<JavaExpression> atFunctionalInterfaceMethod(
      List<JavaExpression> subExpressions) {
    MemberReferenceKind methodRefKind =
        MemberReferenceKind.getMemberReferenceKind((MemberReferenceTree) overriderTree);
    List<FormalParameter> superParameters =
        JavaExpression.getFormalParameters(overridden.getElement());
    int shift = methodRefParameterOffset();
    int subParameterCount = overrider.getElement().getParameters().size();
    // True if the referenced method is invoked with variable arity:  the interface method's
    // parameters from the varargs position onward become the elements of a new array, which is
    // the referenced method's varargs parameter.  That is so even when the two methods have the
    // same number of parameters.
    boolean varargsAdapted = TreeUtils.hasVarargsParameter((MemberReferenceTree) overriderTree);
    // The parameter correspondence matters only if some expression refers to a formal parameter.
    // It does not matter for `this`:  for an unbound reference, `this` is always the interface
    // method's first parameter, and otherwise `this` has no counterpart regardless of arity.
    boolean needsCorrespondence =
        subExpressions.stream().anyMatch(e -> e.containsOfClass(FormalParameter.class));
    if (needsCorrespondence
        && !varargsAdapted
        && superParameters.size() != subParameterCount + shift) {
      // The parameters do not correspond one-to-one.  Do not guess at a correspondence.
      reportParametersDoNotCorrespond();
      return null;
    }

    // Set to true if an expression uses the varargs array other than through a constant index.
    // That array is created for the call, so it has no counterpart at the interface method.
    boolean[] usesVarargsArray = {false};
    JavaExpressionConverter converter =
        new JavaExpressionConverter() {
          @Override
          protected JavaExpression visitFormalParameter(FormalParameter parameter, Void unused) {
            if (varargsAdapted && parameter.getIndex() == subParameterCount) {
              usesVarargsArray[0] = true;
              return new Unknown(parameter.getType());
            }
            return superParameters.get(parameter.getIndex() - 1 + shift);
          }

          @Override
          protected JavaExpression visitArrayAccess(ArrayAccess arrayAccess, Void unused) {
            // Under variable-arity invocation, element `k` of the varargs array is the interface
            // method's parameter at the varargs position plus `k`.
            // That holds only if the element still holds the argument when the effect occurs; see
            // `varargsElementsMightBeReassigned`.
            if (varargsAdapted
                && arrayAccess.getArray() instanceof FormalParameter array
                && array.getIndex() == subParameterCount
                && arrayAccess.getIndex() instanceof ValueLiteral indexLiteral
                && indexLiteral.getValue() instanceof Integer k) {
              // The index of the interface method's parameter that is element 0.
              int firstVarargsIndex = subParameterCount - 1 + shift;
              // Compare `k` before adding to it, because the sum can overflow.
              if (k >= 0 && k < superParameters.size() - firstVarargsIndex) {
                if (visitor.varargsElementsMightBeReassigned(overrider.getElement())) {
                  // The parameters correspond, but the element might not hold the argument when
                  // the effect occurs.  Do not report that the parameters do not correspond.
                  return new Unknown(arrayAccess.getType());
                }
                return superParameters.get(firstVarargsIndex + k);
              }
            }
            return super.visitArrayAccess(arrayAccess, unused);
          }

          @Override
          protected JavaExpression visitThisReference(ThisReference thisExpr, Void unused) {
            return receiverAtFunctionalInterfaceMethod(thisExpr.getType());
          }

          @Override
          protected JavaExpression visitSuperReference(SuperReference superExpr, Void unused) {
            // `super.f` is field `f` of the receiver, as declared in the superclass.  The field
            // access that contains this reference retains that field.
            return receiverAtFunctionalInterfaceMethod(superExpr.getType());
          }

          /*
           * Returns the expression at the interface method's declaration that denotes the
           * referenced method's receiver.
           *
           * @param type the type of the receiver expression being translated
           * @return the interface method's first parameter for an unbound reference, or {@link
           *     Unknown} otherwise
           */
          private JavaExpression receiverAtFunctionalInterfaceMethod(TypeMirror type) {
            if (methodRefKind.isUnbound()) {
              return superParameters.get(0);
            }
            // The receiver is fixed by the method reference itself, or is the object under
            // construction, so no expression at the interface method's declaration denotes it.
            // `Unknown` is never covered.
            return new Unknown(type);
          }
        };

    List<JavaExpression> result = new ArrayList<>(subExpressions.size());
    for (JavaExpression subExpression : subExpressions) {
      if (methodRefKind.isConstructorReference() && subExpression instanceof ThisReference) {
        // The object under construction did not exist before the call, so modifying it is not a
        // side effect that is visible to the caller.
        continue;
      }
      JavaExpression converted = converter.convert(subExpression);
      if (usesVarargsArray[0]) {
        reportParametersDoNotCorrespond();
        return null;
      }
      if (converted.containsOfClass(Unknown.class)) {
        return null;
      }
      result.add(converted);
    }
    return result;
  }

  /**
   * Warns that the referenced method's {@code @SideEffectsOnly} annotation cannot be translated to
   * the interface method's frame, because the two methods' parameters do not correspond. That is
   * the case when the arities differ, or when the annotation mentions the varargs array that a
   * variable-arity invocation creates.
   */
  private void reportParametersDoNotCorrespond() {
    visitor.checker.reportWarning(
        overriderTree,
        "purity.parameters.sideeffectsonly",
        ElementUtils.getSimpleDescription(overrider.getElement()),
        methodSignature(overrider.getElement()),
        methodSignature(overridden.getElement()));
  }

  /**
   * Formats purity kinds for a diagnostic message, as {@link
   * BaseTypeVisitor#purityKindsToString(EnumSet)} does, but with the arguments of the method's
   * {@link SideEffectsOnly} annotation.
   *
   * @param purityKinds a set of purity kinds
   * @param method the method that {@code purityKinds} are the purity kinds of, used to print the
   *     arguments of its {@link SideEffectsOnly} annotation
   * @return the annotations corresponding to {@code purityKinds}, space-separated
   */
  private String purityKindsToString(EnumSet<PurityKind> purityKinds, ExecutableElement method) {
    if (!purityKinds.contains(PurityKind.SIDE_EFFECTS_ONLY)) {
      return BaseTypeVisitor.purityKindsToString(purityKinds);
    }
    return BaseTypeVisitor.purityKindsToString(purityKinds, sideEffectsOnlyToString(method));
  }

  /**
   * Formats the given method's {@link SideEffectsOnly} annotation, with its arguments, as a user
   * would write it.
   *
   * @param method a method that is annotated with {@link SideEffectsOnly}
   * @return the method's {@link SideEffectsOnly} annotation, as a user would write it
   */
  private String sideEffectsOnlyToString(ExecutableElement method) {
    return sideEffectsOnlyToString(visitor.atypeFactory.getSideEffectsOnlyExpressionMap(method));
  }

  /**
   * Checks that an override side-effects no more than the overridden method's
   * {@code @SideEffectsOnly} annotation permits.
   *
   * <p>This check is necessary for soundness. A call site is checked against the
   * {@code @SideEffectsOnly} annotation of the method that the call statically resolves to, but the
   * call might execute an override of that method. Without this check, the override's extra side
   * effects would be invisible at every call whose receiver is statically of the supertype.
   *
   * <p>The check also reports a method that overrides methods in two supertypes with incompatible
   * {@code @SideEffectsOnly} annotations, even if the method writes no annotation of its own: such
   * a method inherits the union of what the supertypes permit, which is more than either one of
   * them permits. See {@code AnnotatedTypeFactory.getSideEffectsOnlyExpressionMap}.
   */
  private void checkSideEffectsOnly() {
    if (isMethodReference) {
      // `checkMethodReferencePurity` checks a method reference against the `@SideEffectsOnly`
      // annotation of the functional interface method.
      return;
    }
    Map<ExecutableElement, List<String>> overriddenExpressionStrings =
        visitor.atypeFactory.getSideEffectsOnlyExpressionMap(overridden.getElement());
    if (overriddenExpressionStrings == null) {
      // The overridden method has no `@SideEffectsOnly` annotation, so it permits every side
      // effect that the overriding method might have.  (If the overridden method is
      // `@SideEffectFree` or `@Pure`, `checkPurity` has already compared the two methods.)
      return;
    }
    ExecutableElement overriderElement = overrider.getElement();
    if (PurityUtils.isSideEffectFree(visitor.atypeFactory, overriderElement)) {
      // The overriding method side-effects nothing, which is no more than any
      // `@SideEffectsOnly` annotation permits.
      return;
    }
    List<JavaExpression> permitted = parseSideEffectsOnly(overriddenExpressionStrings);
    if (permitted == null) {
      // An expression of the overridden method's annotation cannot be parsed, so what that
      // method permits is unknown.  The parse error is reported at its declaration and at every
      // call of it.
      return;
    }
    Map<ExecutableElement, List<String>> overriderExpressionStrings =
        visitor.atypeFactory.getSideEffectsOnlyExpressionMap(overriderElement);
    if (overriderExpressionStrings == null) {
      // The overriding method promises nothing, so it may side-effect more than is permitted.
      reportSideEffectsOnlyOverride(null, overriddenExpressionStrings, null);
      return;
    }

    for (Map.Entry<ExecutableElement, List<String>> entry : overriderExpressionStrings.entrySet()) {
      // The method whose `@SideEffectsOnly` annotation contains the expressions.  It is the
      // overriding method, unless that method inherits the annotation.
      ExecutableElement declaringMethod = entry.getKey();
      for (String exprString : entry.getValue()) {
        JavaExpression expr;
        try {
          // An expression is parsed in the scope of the method that declares it, which is not
          // necessarily the scope of the overriding method.
          expr = StringToJavaExpression.atMethodDecl(exprString, declaringMethod, visitor.checker);
        } catch (JavaExpressionParseException ex) {
          // The parse error is reported at the declaration that writes the expression.
          continue;
        }
        if (!isPermittedSideEffect(expr, permitted)) {
          reportSideEffectsOnlyOverride(
              overriderExpressionStrings, overriddenExpressionStrings, exprString);
        }
      }
    }
  }

  /**
   * Returns true if side-effecting the given expression is permitted by the given
   * {@code @SideEffectsOnly} expressions: it is one of them, or is reached through one of them.
   *
   * @param expr an expression that a method might side-effect
   * @param permitted the expressions that the method is permitted to side-effect
   * @return true if side-effecting {@code expr} is permitted
   */
  private boolean isPermittedSideEffect(JavaExpression expr, List<JavaExpression> permitted) {
    for (JavaExpression permittedExpression : permitted) {
      if (expr.containsAsReceiver(visitor.atypeFactory, permittedExpression)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Parses the given {@code @SideEffectsOnly} expressions, each in the scope of the method that
   * declares it. Returns null if any of them cannot be parsed.
   *
   * <p>Two expressions that are written in different methods' annotations are comparable to one
   * another, because {@link JavaExpression#syntacticEquals} compares a formal parameter by its
   * index and a field by its element, neither of which depends on the scope.
   *
   * @param expressionStrings {@code @SideEffectsOnly} expressions, indexed by the method whose
   *     declaration contains them
   * @return the parsed expressions, or null if any of them cannot be parsed
   */
  private @Nullable List<JavaExpression> parseSideEffectsOnly(
      Map<ExecutableElement, List<String>> expressionStrings) {
    List<JavaExpression> result = new ArrayList<>(expressionStrings.size());
    for (Map.Entry<ExecutableElement, List<String>> entry : expressionStrings.entrySet()) {
      ExecutableElement declaringMethod = entry.getKey();
      for (String exprString : entry.getValue()) {
        try {
          result.add(
              StringToJavaExpression.atMethodDecl(exprString, declaringMethod, visitor.checker));
        } catch (JavaExpressionParseException ex) {
          return null;
        }
      }
    }
    return result;
  }

  /**
   * Reports that the overriding method might side-effect more than the overridden method permits.
   *
   * @param overriderExpressionStrings the {@code @SideEffectsOnly} expressions that apply to the
   *     overriding method, or null if none does
   * @param overriddenExpressionStrings the {@code @SideEffectsOnly} expressions that apply to the
   *     overridden method
   * @param exprString the overriding method's expression that the overridden method does not
   *     permit, or null if the overriding method has no {@code @SideEffectsOnly} annotation
   */
  private void reportSideEffectsOnlyOverride(
      @Nullable Map<ExecutableElement, List<String>> overriderExpressionStrings,
      Map<ExecutableElement, List<String>> overriddenExpressionStrings,
      @Nullable String exprString) {
    visitor.checker.reportError(
        overriderTree,
        "purity.sideeffectsonly.overriding",
        overriderType,
        sideEffectsOnlyToString(overriderExpressionStrings),
        overrider,
        overriddenType,
        sideEffectsOnlyToString(overriddenExpressionStrings),
        overridden,
        exprString == null ? "arbitrary expressions" : "\"" + exprString + "\"");
  }

  /**
   * Formats {@code @SideEffectsOnly} expressions for a diagnostic message, as the annotation that a
   * user writes rather than as the map that represents them.
   *
   * @param expressionStrings {@code @SideEffectsOnly} expressions, indexed by the method whose
   *     declaration contains them; or null if no {@code @SideEffectsOnly} annotation applies
   * @return the annotation corresponding to {@code expressionStrings}
   */
  private String sideEffectsOnlyToString(
      @Nullable Map<ExecutableElement, List<String>> expressionStrings) {
    if (expressionStrings == null) {
      return "(no side effect annotation)";
    }
    StringJoiner result = new StringJoiner(", ", "@SideEffectsOnly({", "})");
    for (List<String> expressions : expressionStrings.values()) {
      for (String expression : expressions) {
        result.add("\"" + expression + "\"");
      }
    }
    return result.toString();
  }

  /**
   * Checks that overrides obey behavioral subtyping, that is, postconditions must be at least as
   * strong as the postcondition on the superclass, and preconditions must be at most as strong as
   * the condition on the superclass.
   */
  private void checkPreAndPostConditions() {
    String msgKey = isMethodReference ? "methodref" : "override";
    if (isMethodReference) {
      // TODO: Support postconditions and method references.
      // The parse context always expects instance methods, but method references can be
      // static.
      return;
    }

    ContractsFromMethod contractsUtils = visitor.atypeFactory.getContractsFromMethod();

    // Check preconditions
    Set<Precondition> superPre = contractsUtils.getPreconditions(overridden.getElement());
    Set<Precondition> subPre = contractsUtils.getPreconditions(overrider.getElement());
    Set<LocalizedContract> superPre2 = parseAndLocalizeContracts(superPre, overridden);
    Set<LocalizedContract> subPre2 = parseAndLocalizeContracts(subPre, overrider);
    @SuppressWarnings("compilermessages")
    @CompilerMessageKey String premsg = "contracts.precondition." + msgKey;
    checkContractsSubset(overriderType, overriddenType, subPre2, superPre2, premsg);

    // Check postconditions
    Set<Postcondition> superPost = contractsUtils.getPostconditions(overridden.getElement());
    Set<Postcondition> subPost = contractsUtils.getPostconditions(overrider.getElement());
    Set<LocalizedContract> superPost2 = parseAndLocalizeContracts(superPost, overridden);
    Set<LocalizedContract> subPost2 = parseAndLocalizeContracts(subPost, overrider);
    @SuppressWarnings("compilermessages")
    @CompilerMessageKey String postmsg = "contracts.postcondition." + msgKey;
    checkContractsSubset(overriderType, overriddenType, superPost2, subPost2, postmsg);

    // Check conditional postconditions
    Set<ConditionalPostcondition> superCPost =
        contractsUtils.getConditionalPostconditions(overridden.getElement());
    Set<ConditionalPostcondition> subCPost =
        contractsUtils.getConditionalPostconditions(overrider.getElement());
    // consider only 'true' postconditions
    Set<Postcondition> superCPostTrue = filterConditionalPostconditions(superCPost, true);
    Set<Postcondition> subCPostTrue = filterConditionalPostconditions(subCPost, true);
    Set<LocalizedContract> superCPostTrue2 = parseAndLocalizeContracts(superCPostTrue, overridden);
    Set<LocalizedContract> subCPostTrue2 = parseAndLocalizeContracts(subCPostTrue, overrider);
    @SuppressWarnings("compilermessages")
    @CompilerMessageKey String posttruemsg = "contracts.conditional.postcondition.true." + msgKey;
    checkContractsSubset(
        overriderType, overriddenType, superCPostTrue2, subCPostTrue2, posttruemsg);

    // consider only 'false' postconditions
    Set<Postcondition> superCPostFalse = filterConditionalPostconditions(superCPost, false);
    Set<Postcondition> subCPostFalse = filterConditionalPostconditions(subCPost, false);
    Set<LocalizedContract> superCPostFalse2 =
        parseAndLocalizeContracts(superCPostFalse, overridden);
    Set<LocalizedContract> subCPostFalse2 = parseAndLocalizeContracts(subCPostFalse, overrider);
    @SuppressWarnings("compilermessages")
    @CompilerMessageKey String postfalsemsg = "contracts.conditional.postcondition.false." + msgKey;
    checkContractsSubset(
        overriderType, overriddenType, superCPostFalse2, subCPostFalse2, postfalsemsg);
  }

  /**
   * Issues a "methodref.receiver" or "methodref.receiver.bound" error if the receiver for the
   * method reference does not satisfy overriding rules.
   *
   * @return true if the override is legal
   */
  private boolean checkMemberReferenceReceivers() {
    if (overriderType.getKind() == TypeKind.ARRAY) {
      // Assume the receiver for all method on arrays are @Top.
      // This simplifies some logic because an AnnotatedExecutableType for an array method
      // (ie String[]::clone) has a receiver of "Array." The UNBOUND check would then
      // have to compare "Array" to "String[]".
      return true;
    }
    MemberReferenceTree memberTree = (MemberReferenceTree) overriderTree;
    MemberReferenceKind methodRefKind =
        MemberReferenceKind.getMemberReferenceKind((MemberReferenceTree) overriderTree);
    // These act like a traditional override
    if (methodRefKind == MemberReferenceKind.UNBOUND) {
      AnnotatedTypeMirror overriderReceiver = overrider.getReceiverType();
      AnnotatedTypeMirror overriddenReceiver = overridden.getParameterTypes().get(0);
      boolean success = visitor.typeHierarchy.isSubtype(overriddenReceiver, overriderReceiver);
      if (!success) {
        visitor.checker.reportError(
            overriderTree,
            "methodref.receiver",
            overriderReceiver,
            overriddenReceiver,
            overriderType,
            overrider,
            overriddenType,
            overridden);
      }
      return success;
    }

    // The rest act like method invocations
    AnnotatedTypeMirror receiverDecl;
    AnnotatedTypeMirror receiverArg;
    switch (methodRefKind) {
      case UNBOUND:
        throw new BugInCF("Case UNBOUND should already be handled.");
      case SUPER:
        receiverDecl = overrider.getReceiverType();
        receiverArg = visitor.atypeFactory.getAnnotatedType(memberTree.getQualifierExpression());

        AnnotatedTypeMirror selfType = visitor.atypeFactory.getSelfType(memberTree);
        receiverArg.replaceAnnotations(selfType.getPrimaryAnnotations());
        break;
      case BOUND:
        receiverDecl = overrider.getReceiverType();
        receiverArg = overriderType;
        break;
      case IMPLICIT_INNER:
        // JLS 15.13.1 "It is a compile-time error if the method reference expression is
        // of the form ClassType :: [TypeArguments] new and a compile-time error would
        // occur when determining an enclosing instance for ClassType as specified in
        // 15.9.2 (treating the method reference expression as if it were an unqualified
        // class instance creation expression)."

        // So a member reference can only refer to an inner class constructor if a type
        // that encloses the inner class can be found. So either "this" is that
        // enclosing type or "this" has an enclosing type that is that type.
        receiverDecl = overrider.getReceiverType();
        receiverArg = visitor.atypeFactory.getSelfType(memberTree);
        while (!TypesUtils.isErasedSubtype(
            receiverArg.getUnderlyingType(), receiverDecl.getUnderlyingType(), types)) {
          receiverArg = ((AnnotatedDeclaredType) receiverArg).getEnclosingType();
        }

        break;
      case TOPLEVEL:
      case STATIC:
      case ARRAY_CTOR:
      default:
        // Intentional fallthrough
        // These don't have receivers
        return true;
    }

    boolean success = visitor.typeHierarchy.isSubtype(receiverArg, receiverDecl);
    if (!success) {
      visitor.checker.reportError(
          overriderTree,
          "methodref.receiver.bound",
          receiverArg,
          receiverDecl,
          receiverArg,
          overriderType,
          overrider);
    }

    return success;
  }

  /**
   * Issues an "override.receiver" error if the receiver override is not valid.
   *
   * @return true if the override is legal
   */
  protected boolean checkReceiverOverride() {
    AnnotatedDeclaredType overriderReceiver = overrider.getReceiverType();
    AnnotatedDeclaredType overriddenReceiver = overridden.getReceiverType();
    // Check the receiver type.
    // Just check primary annotations rather than using `isSubtype()`.  `isSubtype()` requires its
    // arguments to be actual subtypes with respect to the JLS, but an overrider receiver is not a
    // subtype of the overridden receiver.
    if (!visitor.typeHierarchy.isSubtypeShallowEffective(overriddenReceiver, overriderReceiver)) {
      AnnotationMirrorSet declaredAnnos =
          visitor.atypeFactory.getTypeDeclarationBounds(overriderType.getUnderlyingType());
      // The type of an object is no higher than its upper bound. If the receiver is
      // annotated with the upper bound qualifiers, and that is a subtype of the type in the
      // supertype, then the override is safe.
      if (visitor.typeHierarchy.equalsShallowEffective(overriderReceiver, declaredAnnos)
          && visitor.typeHierarchy.isSubtypeShallowEffective(
              overriderReceiver, overriddenReceiver)) {
        return true;
      }
      FoundRequired pair = FoundRequired.of(overriderReceiver, overriddenReceiver);
      visitor.checker.reportError(
          overriderTree,
          "override.receiver",
          pair.found,
          pair.required,
          overriderType,
          overrider,
          overriddenType,
          overridden);
      return false;
    }
    return true;
  }

  /**
   * Issues an error if a type parameter of the overriding method has bounds that do not contain the
   * bounds of the corresponding type parameter of the overridden method.
   *
   * <p>Nothing is checked if the overridden method is unchecked code that receives conservative
   * defaults (see {@code -AuseConservativeDefaultsForUncheckedCode}). Its type parameters then have
   * the widest possible bounds, which are conservative for its clients, but which no overriding
   * method could contain.
   *
   * @return true if the type parameter bounds are correct
   */
  protected boolean checkTypeParameterBounds() {
    if (typeVarMapping == null
        || visitor.atypeFactory.applyConservativeDefaults(overridden.getElement())) {
      return true;
    }
    boolean result = true;
    for (int i = 0; i < overrider.getTypeVariables().size(); i++) {
      // Do not short-circuit, because `checkTypeParameter*Bound` issues warnings.
      result &= checkTypeParameterUpperBound(i);
      result &= checkTypeParameterLowerBound(i);
    }
    return result;
  }

  /**
   * Issues an error if the upper bound of the overriding method's type parameter at the given index
   * is not a supertype of the upper bound of the overridden method's type parameter.
   *
   * @param index the index of the type parameter
   * @return true if the upper bound is correct
   */
  protected boolean checkTypeParameterUpperBound(int index) {
    AnnotatedTypeMirror overriderUpper = overrider.getTypeVariables().get(index).getUpperBound();
    AnnotatedTypeMirror overriddenUpper = overridden.getTypeVariables().get(index).getUpperBound();
    boolean success =
        visitor.typeHierarchy.isSubtype(overriddenUpper, adaptToOverridden(overriderUpper));
    checkTypeParameterBoundMsg(
        success, index, "override.type.parameter.upper", overriderUpper, overriddenUpper);
    return success;
  }

  /**
   * Issues an error if the lower bound of the overriding method's type parameter at the given index
   * is not a subtype of the lower bound of the overridden method's type parameter.
   *
   * @param index the index of the type parameter
   * @return true if the lower bound is correct
   */
  protected boolean checkTypeParameterLowerBound(int index) {
    AnnotatedTypeMirror overriderLower = overrider.getTypeVariables().get(index).getLowerBound();
    AnnotatedTypeMirror overriddenLower = overridden.getTypeVariables().get(index).getLowerBound();
    boolean success =
        visitor.typeHierarchy.isSubtype(adaptToOverridden(overriderLower), overriddenLower);
    checkTypeParameterBoundMsg(
        success, index, "override.type.parameter.lower", overriderLower, overriddenLower);
    return success;
  }

  /**
   * Issues an error if the check of one bound of the overriding method's type parameter at the
   * given index failed. Also prints debugging output if {@code -Ashowchecks} was passed.
   *
   * @param success true if the check succeeded
   * @param index the index of the type parameter
   * @param messageKey the message key, which names the bound that is checked
   * @param overriderBound the bound of the overriding method's type parameter
   * @param overriddenBound the bound of the overridden method's type parameter
   */
  private void checkTypeParameterBoundMsg(
      boolean success,
      int index,
      @CompilerMessageKey String messageKey,
      AnnotatedTypeMirror overriderBound,
      AnnotatedTypeMirror overriddenBound) {
    if (success && !visitor.showchecks) {
      return;
    }

    // The overrider is never a method reference, because typeVarMapping is null for one.
    Tree posTree = ((MethodTree) overriderTree).getTypeParameters().get(index);
    // Print both bounds verbosely.  What is at issue is exactly the bounds' qualifiers, and the
    // non-verbose rendering of a bound can omit the very qualifier that causes the error.  For
    // example, the Nullness Checker prints a `@Nullable` lower bound as "null (NullType)".
    String overriderBoundString = overriderBound.toString(true);
    String overriddenBoundString = overriddenBound.toString(true);

    if (visitor.showchecks) {
      System.out.printf(
          " %s (at %s):%n"
              + "     overrider: %s %s (bound %s)%n"
              + "    overridden: %s %s (bound %s)%n",
          (success
              ? "success: overriding type parameter bound contains overridden"
              : "FAILURE: overriding type parameter bound does not contain overridden"),
          visitor.fileAndLineNumber(posTree),
          overrider,
          overriderType,
          overriderBoundString,
          overridden,
          overriddenType,
          overriddenBoundString);
    }
    if (!success) {
      visitor.checker.reportError(
          posTree,
          messageKey,
          overrider.getElement().getTypeParameters().get(index).getSimpleName(),
          overriderBoundString,
          overriddenBoundString,
          overriderType,
          overrider,
          overriddenType,
          overridden);
    }
  }

  /**
   * Returns true if each parameter type of the overridden method is a subtype of the corresponding
   * parameter type of the overriding method.
   *
   * @return true if the parameter types are correct
   */
  private boolean checkParameters() {
    List<AnnotatedTypeMirror> overriderParams = overrider.getParameterTypes();
    List<AnnotatedTypeMirror> overriddenParams = overridden.getParameterTypes();

    // Fix up method reference parameters.
    // See https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.13.1
    if (isMethodReference) {
      // The functional interface of an unbound member reference has an extra parameter
      // (the receiver).
      if (MemberReferenceKind.getMemberReferenceKind((MemberReferenceTree) overriderTree)
          == MemberReferenceKind.UNBOUND) {
        overriddenParams = new ArrayList<>(overriddenParams);
        overriddenParams.remove(0);
      }
      // Deal with varargs
      if (overrider.isVarargs() && !overridden.isVarargs()) {
        overriderParams =
            AnnotatedTypes.expandVarargsParametersFromTypes(overrider, overriddenParams);
      }
    }

    boolean result = true;
    for (int i = 0; i < overriderParams.size(); ++i) {
      AnnotatedTypeMirror capturedParam =
          visitor.atypeFactory.applyCaptureConversion(overriddenParams.get(i));
      // Express the overriding method's parameter type in terms of the overridden method's type
      // variables, so that the two can be compared.  Diagnostics use the unadapted type, which
      // names the type variables that the overriding method declares.
      boolean success =
          visitor.typeHierarchy.isSubtype(capturedParam, adaptToOverridden(overriderParams.get(i)));

      checkParametersMsg(success, i, overriderParams, overriddenParams);
      result &= success;
    }
    return result;
  }

  private void checkParametersMsg(
      boolean success,
      int index,
      List<AnnotatedTypeMirror> overriderParams,
      List<AnnotatedTypeMirror> overriddenParams) {
    if (success && !visitor.showchecks) {
      return;
    }

    String msgKey = isMethodReference ? "methodref.param" : "override.param";
    Tree posTree =
        overriderTree instanceof MethodTree overriderMt
            ? overriderMt.getParameters().get(index)
            : overriderTree;

    if (visitor.showchecks) {
      System.out.printf(
          " %s (at %s):%n"
              + "     overrider: %s %s (parameter %d type %s)%n"
              + "    overridden: %s %s"
              + " (parameter %d type %s)%n",
          (success
              ? "success: overridden parameter type is subtype of overriding"
              : "FAILURE: overridden parameter type is not subtype of overriding"),
          visitor.fileAndLineNumber(posTree),
          overrider,
          overriderType,
          index,
          overriderParams.get(index).toString(),
          overridden,
          overriddenType,
          index,
          overriddenParams.get(index).toString());
    }
    if (!success) {
      FoundRequired pair =
          FoundRequired.of(overriderParams.get(index), overriddenParams.get(index));
      visitor.checker.reportError(
          posTree,
          msgKey,
          overrider.getElement().getParameters().get(index).toString(),
          pair.found,
          pair.required,
          overriderType,
          overrider,
          overriddenType,
          overridden);
    }
  }

  /**
   * Returns true if the return type of the overridden method is a subtype of the return type of the
   * overriding method.
   *
   * @return true if the return type is correct
   */
  private boolean checkReturn() {
    if ((overriderReturnType.getKind() == TypeKind.VOID)) {
      // Nothing to check.
      return true;
    }
    // Express the overriding method's return type in terms of the overridden method's type
    // variables, so that the two can be compared.
    boolean success =
        visitor.typeHierarchy.isSubtype(
            adaptToOverridden(overriderReturnType), overriddenReturnType);

    // Sometimes the overridden return type of a method reference becomes a captured
    // type variable.  This leads to defaulting that often makes the overriding return type
    // invalid.  We ignore these.  This happens in Issue403/Issue404.
    if (!success
        && isMethodReference
        && TypesUtils.isCapturedTypeVariable(overriddenReturnType.getUnderlyingType())) {
      if (ElementUtils.isMethod(
          overridden.getElement(),
          visitor.functionApply,
          visitor.atypeFactory.getProcessingEnv())) {
        success =
            visitor.typeHierarchy.isSubtype(
                overriderReturnType,
                ((AnnotatedTypeVariable) overriddenReturnType).getUpperBound());
      }
    }

    checkReturnMsg(success);
    return success;
  }

  /**
   * Issues an error message or log message about checking an overriding return type.
   *
   * @param success true if the check succeeded
   */
  private void checkReturnMsg(boolean success) {
    if (success && !visitor.showchecks) {
      return;
    }

    String msgKey = isMethodReference ? "methodref.return" : "override.return";
    Tree posTree =
        overriderTree instanceof MethodTree overriderMt
            ? overriderMt.getReturnType()
            : overriderTree;
    // The return type of a MethodTree is null for a constructor.
    if (posTree == null) {
      posTree = overriderTree;
    }

    if (visitor.showchecks) {
      System.out.printf(
          " %s (at %s):%n"
              + "     overrider: %s %s (return type %s)%n"
              + "    overridden: %s %s (return type %s)%n",
          (success
              ? "success: overriding return type is subtype of overridden"
              : "FAILURE: overriding return type is not subtype of overridden"),
          visitor.fileAndLineNumber(posTree),
          overrider,
          overriderType,
          overrider.getReturnType().toString(),
          overridden,
          overriddenType,
          overridden.getReturnType().toString());
    }
    if (!success) {
      FoundRequired pair = FoundRequired.of(overriderReturnType, overriddenReturnType);
      visitor.checker.reportError(
          posTree,
          msgKey,
          pair.found,
          pair.required,
          overriderType,
          overrider,
          overriddenType,
          overridden);
    }
  }

  /**
   * Checks that {@code mustSubset} is a subset of {@code set} in the following sense: For every
   * expression in {@code mustSubset} there must be the same expression in {@code set}, with the
   * same (or a stronger) annotation.
   *
   * <p>This uses field {@link BaseTypeVisitor#methodTree} to determine where to issue an error
   * message.
   *
   * @param overriderType the subtype
   * @param overriddenType the supertype
   * @param mustSubset annotations that should be weaker
   * @param set annotations that should be stronger
   * @param messageKey message key for error messages
   */
  private void checkContractsSubset(
      AnnotatedTypeMirror overriderType,
      AnnotatedDeclaredType overriddenType,
      Set<LocalizedContract> mustSubset,
      Set<LocalizedContract> set,
      @CompilerMessageKey String messageKey) {

    for (LocalizedContract weak : mustSubset) {
      JavaExpression jexpr = weak.expression();
      TypeMirror jexprTM = jexpr.getType();

      // Is there a contract of the same receiver, whose annotation is a subtype?
      boolean found =
          set.stream()
              .anyMatch(
                  strong ->
                      jexpr.equals(strong.expression())
                          && visitor.qualHierarchy.isSubtypeShallow(
                              strong.annotation(), jexprTM, weak.annotation(), jexprTM));

      if (!found) {

        String overriddenTypeString = overriddenType.getUnderlyingType().asElement().toString();
        String overriderTypeString;
        if (overriderType.getKind() == TypeKind.DECLARED) {
          DeclaredType overriderTypeMirror =
              ((AnnotatedDeclaredType) overriderType).getUnderlyingType();
          overriderTypeString = overriderTypeMirror.asElement().toString();
        } else {
          overriderTypeString = overriderType.toString();
        }

        // weak.annotation() is the AnnotationMirror that is too strong.  It might be from the
        // precondition or the postcondition.

        // These are the annotations that are too weak.
        StringJoiner strongRelevantAnnos = new StringJoiner(" ").setEmptyValue("no information");
        for (LocalizedContract strong : set) {
          if (jexpr.equals(strong.expression())) {
            strongRelevantAnnos.add(strong.annotation().toString());
          }
        }

        Object overriddenAnno;
        Object overriderAnno;
        if (messageKey.contains(".precondition.")) {
          overriddenAnno = strongRelevantAnnos;
          overriderAnno = weak.annotation();
        } else {
          overriddenAnno = weak.annotation();
          overriderAnno = strongRelevantAnnos;
        }

        visitor.checker.reportError(
            visitor.methodTree,
            messageKey,
            jexpr,
            visitor.methodTree.getName(),
            overriddenTypeString,
            overriddenAnno,
            overriderTypeString,
            overriderAnno);
      }
    }
  }

  /**
   * Localizes some contracts -- that is, viewpoint-adapts them to some method body, according to
   * the value of {@link BaseTypeVisitor#methodTree}.
   *
   * <p>The input is a set of {@link Contract}s, each of which contains an expression string and an
   * annotation. In a {@link Contract}, Java expressions are exactly as written in source code, not
   * standardized or viewpoint-adapted.
   *
   * <p>The output is a set of {@link LocalizedContract}s, each of which pairs a {@link
   * JavaExpression} (parsed expression string) with a standardized annotation (with respect to the
   * path of {@link BaseTypeVisitor#methodTree}). This method discards any contract whose expression
   * cannot be parsed into a JavaExpression.
   *
   * @param contractSet a set of contracts
   * @param methodType the type of the method that the contracts are for
   * @return localized contracts
   */
  private Set<LocalizedContract> parseAndLocalizeContracts(
      Set<? extends Contract> contractSet, AnnotatedExecutableType methodType) {
    if (contractSet.isEmpty()) {
      return Collections.emptySet();
    }

    // This is the path to a place where the contract is being used, which might or might not be
    // where the contract was defined.  For example, methodTree might be an overriding
    // definition, and the contract might be for a superclass.
    MethodTree methodTree = visitor.methodTree;

    StringToJavaExpression stringToJavaExpr =
        expression -> {
          JavaExpression javaExpr =
              StringToJavaExpression.atMethodDecl(
                  expression, methodType.getElement(), visitor.checker);
          // methodType.getElement() is not necessarily the same method as methodTree, so
          // viewpoint-adapt it to methodTree.
          return javaExpr.atMethodBody(methodTree);
        };

    Set<LocalizedContract> result = ArraySet.newArraySetOrHashSet(contractSet.size());
    for (Contract p : contractSet) {
      String expressionString = p.expressionString;
      AnnotationMirror annotation =
          p.viewpointAdaptDependentTypeAnnotation(
              visitor.atypeFactory, stringToJavaExpr, methodTree);
      JavaExpression exprJe;
      try {
        // TODO: currently, these expressions are parsed many times.
        // This could be optimized to store the result the first time.
        // (same for other annotations)
        exprJe = stringToJavaExpr.toJavaExpression(expressionString);
      } catch (JavaExpressionParseException e) {
        // report errors here
        visitor.checker.report(methodTree, new DiagMessage(e));
        continue;
      }
      @SuppressWarnings("nullness:argument") // the lambda stringToJavaExpr never returns null
      LocalizedContract contract = new LocalizedContract(exprJe, annotation);
      result.add(contract);
    }
    return result;
  }

  /**
   * Filters the set of conditional postconditions to return only those whose annotation result
   * value matches the value of the given boolean {@code b}. For example, if {@code b == true}, then
   * the following {@code @EnsuresNonNullIf} conditional postcondition would match:<br>
   * {@code @EnsuresNonNullIf(expression="#1", result=true)}<br>
   * {@code boolean equals(@Nullable Object o)}
   *
   * @param conditionalPostconditions each is a ConditionalPostcondition
   * @param b the value required for the {@code result} element
   * @return all the given conditional postconditions whose {@code result} is {@code b}
   */
  private Set<Postcondition> filterConditionalPostconditions(
      Set<ConditionalPostcondition> conditionalPostconditions, boolean b) {
    if (conditionalPostconditions.isEmpty()) {
      return Collections.emptySet();
    }

    Set<Postcondition> result =
        ArraySet.newArraySetOrLinkedHashSet(conditionalPostconditions.size());
    for (Contract c : conditionalPostconditions) {
      ConditionalPostcondition p = (ConditionalPostcondition) c;
      if (p.resultValue == b) {
        result.add(new Postcondition(p.expressionString, p.annotation, p.contractAnnotation));
      }
    }
    return result;
  }

  /**
   * A contract that has been viewpoint-adapted to a particular method body.
   *
   * @param expression the contract's Java expression
   * @param annotation the contract's annotation, standardized with respect to the method body
   */
  private record LocalizedContract(JavaExpression expression, AnnotationMirror annotation) {}
}
