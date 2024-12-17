package org.checkerframework.checker.resourceleak;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.calledmethods.CalledMethodsVisitor;
import org.checkerframework.checker.calledmethods.EnsuresCalledMethodOnExceptionContract;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.CreatesMustCallForToJavaExpression;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.mustcall.qual.PolyMustCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The visitor for the Resource Leak Checker. Responsible for checking that the rules for {@link
 * Owning} fields are satisfied, and for checking that {@link CreatesMustCallFor} overrides are
 * valid.
 */
public class ResourceLeakVisitor extends CalledMethodsVisitor {

  /** True if errors related to static owning fields should be suppressed. */
  private final boolean permitStaticOwning;

  /**
   * Because CalledMethodsVisitor doesn't have a type parameter, we need a reference to the type
   * factory that has this static type to access the features that ResourceLeakAnnotatedTypeFactory
   * implements but CalledMethodsAnnotatedTypeFactory does not.
   */
  private final ResourceLeakAnnotatedTypeFactory rlTypeFactory;

  /** True if -AnoLightweightOwnership was supplied on the command line. */
  private final boolean noLightweightOwnership;

  /**
   * True if -AenableWpiForRlc was passed on the command line. See {@link
   * ResourceLeakChecker#ENABLE_WPI_FOR_RLC}.
   */
  private final boolean enableWpiForRlc;

  /**
   * Create the visitor.
   *
   * @param checker the type-checker associated with this visitor
   */
  public ResourceLeakVisitor(BaseTypeChecker checker) {
    super(checker);
    rlTypeFactory = (ResourceLeakAnnotatedTypeFactory) atypeFactory;
    permitStaticOwning = checker.hasOption("permitStaticOwning");
    noLightweightOwnership = checker.hasOption("noLightweightOwnership");
    enableWpiForRlc = checker.hasOption(ResourceLeakChecker.ENABLE_WPI_FOR_RLC);
  }

  @Override
  protected ResourceLeakAnnotatedTypeFactory createTypeFactory() {
    return new ResourceLeakAnnotatedTypeFactory(checker);
  }

  @Override
  public void processMethodTree(String className, MethodTree tree) {
    ExecutableElement elt = TreeUtils.elementFromDeclaration(tree);
    MustCallAnnotatedTypeFactory mcAtf =
        rlTypeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
    List<String> cmcfValues = getCreatesMustCallForValues(elt, mcAtf, rlTypeFactory);
    if (!cmcfValues.isEmpty()) {
      checkCreatesMustCallForOverrides(tree, elt, mcAtf, cmcfValues);
      checkCreatesMustCallForTargetsHaveNonEmptyMustCall(tree, mcAtf);
    }
    checkOwningOverrides(tree, elt, mcAtf);
    if (TreeUtils.isConstructor(tree)) {
      checkMustCallAliasAnnotationForConstructor(tree);
    } else {
      checkMustCallAliasAnnotationForMethod(tree, mcAtf);
    }
    super.processMethodTree(className, tree);
  }

  /**
   * checks that any created must-call obligation has a declared type with a non-empty
   * {@code @MustCall} obligation
   *
   * @param tree the method
   * @param mcAtf the type factory
   */
  private void checkCreatesMustCallForTargetsHaveNonEmptyMustCall(
      MethodTree tree, MustCallAnnotatedTypeFactory mcAtf) {
    // Get all the JavaExpressions for all CreatesMustCallFor annotations
    List<JavaExpression> createsMustCallExprs =
        CreatesMustCallForToJavaExpression.getCreatesMustCallForExpressionsAtMethodDeclaration(
            tree, mcAtf, mcAtf);
    for (JavaExpression targetExpr : createsMustCallExprs) {
      AnnotationMirror mustCallAnno =
          mcAtf
              .getAnnotatedType(TypesUtils.getTypeElement(targetExpr.getType()))
              .getPrimaryAnnotationInHierarchy(mcAtf.TOP);
      if (rlTypeFactory.getMustCallValues(mustCallAnno).isEmpty()) {
        checker.reportError(
            tree,
            "creates.mustcall.for.invalid.target",
            targetExpr.toString(),
            targetExpr.getType().toString());
      }
    }
  }

  /**
   * Check that an overriding method does not reduce the number of created must-call obligations
   *
   * @param tree overriding method
   * @param elt element for overriding method
   * @param mcAtf the type factory
   * @param cmcfValues must call values created by overriding method
   */
  private void checkCreatesMustCallForOverrides(
      MethodTree tree,
      ExecutableElement elt,
      MustCallAnnotatedTypeFactory mcAtf,
      List<String> cmcfValues) {
    // If this method overrides another method, it must create at least as many
    // obligations. Without this check, dynamic dispatch might allow e.g. a field to be
    // overwritten by a CMCF method, but the CMCF effect wouldn't occur.
    for (ExecutableElement overridden : ElementUtils.getOverriddenMethods(elt, this.types)) {
      List<String> overriddenCmcfValues =
          getCreatesMustCallForValues(overridden, mcAtf, rlTypeFactory);
      if (!overriddenCmcfValues.containsAll(cmcfValues)) {
        String foundCmcfValueString = String.join(", ", cmcfValues);
        String neededCmcfValueString = String.join(", ", overriddenCmcfValues);
        String actualClassname = ElementUtils.getEnclosingClassName(elt);
        String overriddenClassname = ElementUtils.getEnclosingClassName(overridden);
        checker.reportError(
            tree,
            "creates.mustcall.for.override.invalid",
            actualClassname + "#" + elt,
            overriddenClassname + "#" + overridden,
            foundCmcfValueString,
            neededCmcfValueString);
      }
    }
  }

  /**
   * Checks that overrides respect behavioral subtyping for @Owning and @NotOwning annotations. In
   * particular, checks that 1) if an overridden method has an @Owning parameter, then that
   * parameter is @Owning in the overrider, and 2) if an overridden method has an @NotOwning return,
   * then the overrider also has an @NotOwning return.
   *
   * @param tree overriding method, for error reporting
   * @param overrider element for overriding method
   * @param mcAtf the type factory
   */
  private void checkOwningOverrides(
      MethodTree tree, ExecutableElement overrider, MustCallAnnotatedTypeFactory mcAtf) {
    for (ExecutableElement overridden : ElementUtils.getOverriddenMethods(overrider, this.types)) {
      // Check for @Owning parameters. Must use an explicitly-indexed for loop so that the
      // same parameter index can be accessed in the overrider's parameter list, which is
      // the same length.
      for (int i = 0; i < overridden.getParameters().size(); i++) {
        if (mcAtf.getDeclAnnotation(overridden.getParameters().get(i), Owning.class) != null) {
          if (mcAtf.getDeclAnnotation(overrider.getParameters().get(i), Owning.class) == null) {
            checker.reportError(
                tree,
                "owning.override.param",
                overrider.getParameters().get(i).getSimpleName().toString(),
                overrider.getSimpleName().toString(),
                ElementUtils.getEnclosingClassName(overrider),
                overridden.getSimpleName().toString(),
                ElementUtils.getEnclosingClassName(overridden));
          }
        }
      }
      // Check for @NotOwning returns.
      if (mcAtf.getDeclAnnotation(overridden, NotOwning.class) != null
          && mcAtf.getDeclAnnotation(overrider, NotOwning.class) == null) {
        checker.reportError(
            tree,
            "owning.override.return",
            overrider.getSimpleName().toString(),
            ElementUtils.getEnclosingClassName(overrider),
            overridden.getSimpleName().toString(),
            ElementUtils.getEnclosingClassName(overridden));
      }
    }
  }

  /**
   * If a {@code @MustCallAlias} annotation appears in a method declaration, it must appear as an
   * annotation on both the return type, and a parameter type.
   *
   * <p>The return type is checked if it is annotated with {@code @PolyMustCall} because the Must
   * Call Checker treats {@code @MustCallAlias} as an alias of {@code @PolyMustCall}.
   *
   * @param tree the method declaration
   * @param mcAtf the MustCallAnnotatedTypeFactory
   */
  private void checkMustCallAliasAnnotationForMethod(
      MethodTree tree, MustCallAnnotatedTypeFactory mcAtf) {

    Element paramWithMustCallAliasAnno = getParameterWithMustCallAliasAnno(tree);
    boolean isMustCallAliasAnnoOnParameter = paramWithMustCallAliasAnno != null;

    if (TreeUtils.isVoidReturn(tree) && isMustCallAliasAnnoOnParameter) {
      checker.reportWarning(
          tree, "mustcallalias.method.return.and.param", "this method has a void return");
      return;
    }

    AnnotatedTypeMirror returnType = mcAtf.getMethodReturnType(tree);
    boolean isMustCallAliasAnnoOnReturnType = returnType.hasPrimaryAnnotation(PolyMustCall.class);
    checkMustCallAliasAnnoMismatch(
        paramWithMustCallAliasAnno, isMustCallAliasAnnoOnReturnType, tree);
  }

  /**
   * Given a constructor, a {@code @MustCallAlias} must appear in both the list of parameters and as
   * an annotation on the constructor itself, if it is to appear at all.
   *
   * <p>That is, a {@code @MustCallAlias} annotation must appear on both the constructor and its
   * parameter list, or not at all.
   *
   * @param tree the constructor
   */
  private void checkMustCallAliasAnnotationForConstructor(MethodTree tree) {
    ExecutableElement constructorDecl = TreeUtils.elementFromDeclaration(tree);
    boolean isMustCallAliasAnnoOnConstructor =
        constructorDecl != null && rlTypeFactory.hasMustCallAlias(constructorDecl);
    Element paramWithMustCallAliasAnno = getParameterWithMustCallAliasAnno(tree);
    checkMustCallAliasAnnoMismatch(
        paramWithMustCallAliasAnno, isMustCallAliasAnnoOnConstructor, tree);
  }

  /**
   * Construct the warning message for the case where a {@code @MustCallAlias} annotation does not
   * appear in pairs in a method or constructor declaration.
   *
   * <p>If a parameter of a method or a constructor is annotated with a {@code @MustCallAlias}
   * annotation, the return type (for a method) should also be annotated with
   * {@code @MustCallAlias}. In the case of a constructor, which has no return type, a
   * {@code @MustCallAlias} annotation must appear on its declaration.
   *
   * @param paramWithMustCallAliasAnno a parameter with a {@code @MustCallAlias} annotation, null if
   *     there are none
   * @param isMustCallAliasAnnoOnMethodOrConstructorDecl true if and only if a
   *     {@code @MustCallAlias} annotation appears on a method or constructor declaration
   * @param tree the method or constructor declaration
   */
  private void checkMustCallAliasAnnoMismatch(
      @Nullable Element paramWithMustCallAliasAnno,
      boolean isMustCallAliasAnnoOnMethodOrConstructorDecl,
      MethodTree tree) {
    boolean isMustCallAliasAnnotationOnParameter = paramWithMustCallAliasAnno != null;
    if (isMustCallAliasAnnotationOnParameter != isMustCallAliasAnnoOnMethodOrConstructorDecl) {
      String locationOfCheck = TreeUtils.isClassTree(tree) ? "this constructor" : "the return type";
      String message =
          isMustCallAliasAnnotationOnParameter
              ? String.format(
                  "there is no @MustCallAlias annotation on %s, even though the parameter %s is"
                      + " annotated with @MustCallAlias",
                  locationOfCheck, paramWithMustCallAliasAnno)
              : "no parameter has a @MustCallAlias annotation, even though the return type is"
                  + " annotated with @MustCallAlias";
      checker.reportWarning(tree, "mustcallalias.method.return.and.param", message);
    }
  }

  /**
   * Given a method and its parameter list, look through each of the parameters and see if any are
   * annotated with the {@code @MustCallAlias} annotation.
   *
   * <p>Return the first parameter that is annotated with {@code @MustCallAlias}, otherwise return
   * null.
   *
   * @param tree the method declaration
   * @return the first parameter that is annotated with {@code @MustCallAlias}, otherwise return
   *     null
   */
  private @Nullable Element getParameterWithMustCallAliasAnno(MethodTree tree) {
    VariableTree receiverParameter = tree.getReceiverParameter();
    if (receiverParameter != null && rlTypeFactory.hasMustCallAlias(receiverParameter)) {
      return TreeUtils.elementFromDeclaration(receiverParameter);
    }
    for (VariableTree param : tree.getParameters()) {
      if (rlTypeFactory.hasMustCallAlias(param)) {
        return TreeUtils.elementFromDeclaration(param);
      }
    }
    return null;
  }

  @Override
  protected boolean shouldPerformContractInference() {
    return atypeFactory.getWholeProgramInference() != null && isWpiEnabledForRLC();
  }

  /**
   * Returns the {@link CreatesMustCallFor#value} element/argument of the given @CreatesMustCallFor
   * annotation, or "this" if there is none.
   *
   * <p>Does not vipewpoint-adaptation.
   *
   * @param createsMustCallFor an @CreatesMustCallFor annotation
   * @param mcAtf a MustCallAnnotatedTypeFactory, to source the value element
   * @return the string value
   */
  private static String getCreatesMustCallForValue(
      AnnotationMirror createsMustCallFor, MustCallAnnotatedTypeFactory mcAtf) {
    return AnnotationUtils.getElementValue(
        createsMustCallFor, mcAtf.getCreatesMustCallForValueElement(), String.class, "this");
  }

  /**
   * Returns all the {@link CreatesMustCallFor#value} elements/arguments of all @CreatesMustCallFor
   * annotations on the given element.
   *
   * <p>Does no viewpoint-adaptation, unlike {@link
   * CreatesMustCallForToJavaExpression#getCreatesMustCallForExpressionsAtInvocation} which does.
   *
   * @param elt an executable element
   * @param mcAtf a MustCallAnnotatedTypeFactory, to source the value element
   * @param atypeFactory a ResourceLeakAnnotatedTypeFactory
   * @return the literal strings present in the @CreatesMustCallFor annotation(s) of that element,
   *     substituting the default "this" for empty annotations. This method returns the empty list
   *     iff there are no @CreatesMustCallFor annotations on elt. The returned list is always
   *     modifiable if it is non-empty.
   */
  /*package-private*/ static List<String> getCreatesMustCallForValues(
      ExecutableElement elt,
      MustCallAnnotatedTypeFactory mcAtf,
      ResourceLeakAnnotatedTypeFactory atypeFactory) {
    AnnotationMirror createsMustCallForList =
        atypeFactory.getDeclAnnotation(elt, CreatesMustCallFor.List.class);
    List<String> result = new ArrayList<>(4);
    if (createsMustCallForList != null) {
      List<AnnotationMirror> createsMustCallFors =
          AnnotationUtils.getElementValueArray(
              createsMustCallForList,
              mcAtf.getCreatesMustCallForListValueElement(),
              AnnotationMirror.class);
      for (AnnotationMirror cmcf : createsMustCallFors) {
        result.add(getCreatesMustCallForValue(cmcf, mcAtf));
      }
    }
    AnnotationMirror createsMustCallFor =
        atypeFactory.getDeclAnnotation(elt, CreatesMustCallFor.class);
    if (createsMustCallFor != null) {
      result.add(getCreatesMustCallForValue(createsMustCallFor, mcAtf));
    }
    return result;
  }

  /**
   * Get all {@link EnsuresCalledMethods} annotations on an element.
   *
   * @param elt an executable element that might have {@link EnsuresCalledMethods} annotations
   * @param atypeFactory a <code>ResourceLeakAnnotatedTypeFactory</code>
   * @return a set of {@link EnsuresCalledMethods} annotations
   */
  @Pure
  private static AnnotationMirrorSet getEnsuresCalledMethodsAnnotations(
      ExecutableElement elt, ResourceLeakAnnotatedTypeFactory atypeFactory) {
    AnnotationMirror ensuresCalledMethodsAnnos =
        atypeFactory.getDeclAnnotation(elt, EnsuresCalledMethods.List.class);
    AnnotationMirrorSet result = new AnnotationMirrorSet();
    if (ensuresCalledMethodsAnnos != null) {
      result.addAll(
          AnnotationUtils.getElementValueArray(
              ensuresCalledMethodsAnnos,
              atypeFactory.getEnsuresCalledMethodsListValueElement(),
              AnnotationMirror.class));
    }
    AnnotationMirror ensuresCalledMethod =
        atypeFactory.getDeclAnnotation(elt, EnsuresCalledMethods.class);
    if (ensuresCalledMethod != null) {
      result.add(ensuresCalledMethod);
    }
    return result;
  }

  @Override
  public Void visitVariable(VariableTree tree, Void p) {
    VariableElement varElement = TreeUtils.elementFromDeclaration(tree);

    if (varElement.getKind().isField()
        && !noLightweightOwnership
        && rlTypeFactory.getDeclAnnotation(varElement, Owning.class) != null) {
      checkOwningField(varElement);
    }

    return super.visitVariable(tree, p);
  }

  /**
   * An obligation that must be satisfied by a destructor. Helper type for {@link
   * #checkOwningField(VariableElement)}.
   */
  // TODO: In the future, this class should be a record.
  private static final class DestructorObligation {
    /** The method that must be called on the field. */
    final String mustCallMethod;

    /** When the method must be called. */
    final MustCallConsistencyAnalyzer.MethodExitKind exitKind;

    /**
     * Create a new obligation.
     *
     * @param mustCallMethod the method that must be called
     * @param exitKind when the method must be called
     */
    public DestructorObligation(
        String mustCallMethod, MustCallConsistencyAnalyzer.MethodExitKind exitKind) {
      this.mustCallMethod = mustCallMethod;
      this.exitKind = exitKind;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DestructorObligation that = (DestructorObligation) o;
      return mustCallMethod.equals(that.mustCallMethod) && exitKind == that.exitKind;
    }

    @Override
    public int hashCode() {
      return Objects.hash(mustCallMethod, exitKind);
    }
  }

  /**
   * Checks validity of a field {@code field} with an {@code @}{@link Owning} annotation. Say the
   * type of {@code field} is {@code @MustCall("m"}}. This method checks that the enclosing class of
   * {@code field} has a type {@code @MustCall("m2")} for some method {@code m2}, and that {@code
   * m2} has an annotation {@code @EnsuresCalledMethods(value = "this.field", methods = "m")},
   * guaranteeing that the {@code @MustCall} obligation of the field will be satisfied.
   *
   * @param field the declaration of the field to check
   */
  private void checkOwningField(VariableElement field) {

    if (checker.shouldSkipUses(field)) {
      return;
    }

    Set<Modifier> modifiers = field.getModifiers();
    if (modifiers.contains(Modifier.STATIC)) {
      if (permitStaticOwning) {
        return;
      }
      if (modifiers.contains(Modifier.FINAL)) {
        return;
      }
    }

    List<String> mustCallObligationsOfOwningField = rlTypeFactory.getMustCallValues(field);

    if (mustCallObligationsOfOwningField.isEmpty()) {
      return;
    }

    // This value is side-effected.
    Set<DestructorObligation> unsatisfiedMustCallObligationsOfOwningField = new LinkedHashSet<>();
    for (String mustCallMethod : mustCallObligationsOfOwningField) {
      for (MustCallConsistencyAnalyzer.MethodExitKind exitKind :
          MustCallConsistencyAnalyzer.MethodExitKind.values()) {
        unsatisfiedMustCallObligationsOfOwningField.add(
            new DestructorObligation(mustCallMethod, exitKind));
      }
    }

    String error;
    Element enclosingElement = field.getEnclosingElement();
    List<String> enclosingMustCallValues = rlTypeFactory.getMustCallValues(enclosingElement);

    if (enclosingMustCallValues == null) {
      error =
          " The enclosing element "
              + ElementUtils.getQualifiedName(enclosingElement)
              + " doesn't have a @MustCall annotation";
    } else if (enclosingMustCallValues.isEmpty()) {
      error =
          " The enclosing element "
              + ElementUtils.getQualifiedName(enclosingElement)
              + " has an empty @MustCall annotation";
    } else {
      error = " [[checkOwningField() did not find a reason!]]"; // should be reassigned
      List<? extends Element> siblingsOfOwningField = enclosingElement.getEnclosedElements();
      for (Element siblingElement : siblingsOfOwningField) {
        if (siblingElement.getKind() == ElementKind.METHOD
            && enclosingMustCallValues.contains(siblingElement.getSimpleName().toString())) {

          ExecutableElement siblingMethod = (ExecutableElement) siblingElement;

          AnnotationMirrorSet allEnsuresCalledMethodsAnnos =
              getEnsuresCalledMethodsAnnotations(siblingMethod, rlTypeFactory);
          for (AnnotationMirror ensuresCalledMethodsAnno : allEnsuresCalledMethodsAnnos) {
            List<String> values =
                AnnotationUtils.getElementValueArray(
                    ensuresCalledMethodsAnno,
                    rlTypeFactory.ensuresCalledMethodsValueElement,
                    String.class);
            for (String value : values) {
              if (expressionEqualsField(value, field)) {
                List<String> methods =
                    AnnotationUtils.getElementValueArray(
                        ensuresCalledMethodsAnno,
                        rlTypeFactory.ensuresCalledMethodsMethodsElement,
                        String.class);
                for (String method : methods) {
                  unsatisfiedMustCallObligationsOfOwningField.remove(
                      new DestructorObligation(
                          method, MustCallConsistencyAnalyzer.MethodExitKind.NORMAL_RETURN));
                }
              }
            }

            Set<EnsuresCalledMethodOnExceptionContract> exceptionalPostconds =
                rlTypeFactory.getExceptionalPostconditions(siblingMethod);
            for (EnsuresCalledMethodOnExceptionContract postcond : exceptionalPostconds) {
              if (expressionEqualsField(postcond.getExpression(), field)) {
                unsatisfiedMustCallObligationsOfOwningField.remove(
                    new DestructorObligation(
                        postcond.getMethod(),
                        MustCallConsistencyAnalyzer.MethodExitKind.EXCEPTIONAL_EXIT));
              }
            }

            // Optimization: stop early as soon as we've exhausted the list of
            // obligations.
            if (unsatisfiedMustCallObligationsOfOwningField.isEmpty()) {
              return;
            }
          }

          if (!unsatisfiedMustCallObligationsOfOwningField.isEmpty()) {
            // This variable could be set immediately before reporting the error, but
            // IMO it is more clear to set it here.
            error =
                "Postconditions written on MustCall methods are missing: "
                    + formatMissingMustCallMethodPostconditions(
                        field, unsatisfiedMustCallObligationsOfOwningField);
          }
        }
      }
    }

    if (!unsatisfiedMustCallObligationsOfOwningField.isEmpty()) {
      Set<String> missingMethods = new LinkedHashSet<>();
      for (DestructorObligation obligation : unsatisfiedMustCallObligationsOfOwningField) {
        missingMethods.add(obligation.mustCallMethod);
      }

      checker.reportError(
          field,
          "required.method.not.called",
          MustCallConsistencyAnalyzer.formatMissingMustCallMethods(new ArrayList<>(missingMethods)),
          "field " + field.getSimpleName().toString(),
          field.asType().toString(),
          error);
    }
  }

  /**
   * Determine if the given expression <code>e</code> refers to <code>this.field</code>.
   *
   * @param e the expression
   * @param field the field
   * @return true if <code>e</code> refers to <code>this.field</code>
   */
  private boolean expressionEqualsField(String e, VariableElement field) {
    try {
      JavaExpression je = StringToJavaExpression.atFieldDecl(e, field, this.checker);
      return je instanceof FieldAccess && ((FieldAccess) je).getField().equals(field);
    } catch (JavaExpressionParseUtil.JavaExpressionParseException ex) {
      // The parsing error will be reported elsewhere, assuming e was derived from an
      // annotation.
      return false;
    }
  }

  /**
   * Checks if WPI is enabled for the Resource Leak Checker inference. See {@link
   * ResourceLeakChecker#ENABLE_WPI_FOR_RLC}.
   *
   * @return returns true if WPI is enabled for the Resource Leak Checker
   */
  protected boolean isWpiEnabledForRLC() {
    return enableWpiForRlc;
  }

  /**
   * Formats a list of must-call method post-conditions to be printed in an error message.
   *
   * @param field the value whose methods must be called
   * @param mustCallVal the list of must-call strings
   * @return a formatted string
   */
  /*package-private*/ static String formatMissingMustCallMethodPostconditions(
      Element field, Set<DestructorObligation> mustCallVal) {
    int size = mustCallVal.size();
    if (size == 0) {
      throw new TypeSystemError("empty mustCallVal " + mustCallVal);
    }
    String fieldName = field.getSimpleName().toString();
    return mustCallVal.stream()
        .map(
            o ->
                postconditionAnnotationFor(o.exitKind)
                    + "(value = \""
                    + fieldName
                    + "\", methods = \""
                    + o.mustCallMethod
                    + "\")")
        .collect(Collectors.joining(", "));
  }

  /**
   * Format a must-call post-condition to be printed in an error message.
   *
   * @param exitKind the kind of method exit
   * @return the name of the annotation
   */
  private static String postconditionAnnotationFor(
      MustCallConsistencyAnalyzer.MethodExitKind exitKind) {
    switch (exitKind) {
      case NORMAL_RETURN:
        return "@EnsuresCalledMethods";
      case EXCEPTIONAL_EXIT:
        return "@EnsuresCalledMethodsOnException";
      default:
        throw new UnsupportedOperationException(exitKind.toString());
    }
  }
}
