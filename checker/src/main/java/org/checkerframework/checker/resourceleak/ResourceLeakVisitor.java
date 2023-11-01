package org.checkerframework.checker.resourceleak;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.calledmethods.CalledMethodsVisitor;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.CreatesMustCallForToJavaExpression;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
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
  public Void visitMethod(MethodTree tree, Void p) {
    ExecutableElement elt = TreeUtils.elementFromDeclaration(tree);
    MustCallAnnotatedTypeFactory mcAtf =
        rlTypeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
    List<String> cmcfValues = getCreatesMustCallForValues(elt, mcAtf, rlTypeFactory);
    if (!cmcfValues.isEmpty()) {
      checkCreatesMustCallForOverrides(tree, elt, mcAtf, cmcfValues);
      checkCreatesMustCallForTargetsHaveNonEmptyMustCall(tree, mcAtf);
    }
    checkOwningOverrides(tree, elt, mcAtf);
    return super.visitMethod(tree, p);
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
      // Check for @Owning parameters. Must use an explicitly-indexed for loop so that the same
      // parameter index can be accessed in the overrider's parameter list, which is the same
      // length.
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

  @Override
  protected boolean shouldPerformContractInference() {
    return atypeFactory.getWholeProgramInference() != null && isWpiEnabledForRLC();
  }

  // Overwritten to check that destructors (i.e. methods responsible for resolving
  // the must-call obligations of owning fields) enforce a stronger version of
  // @EnsuresCalledMethods: that the claimed @CalledMethods annotation is true on
  // both exceptional and regular exits, not just on regular exits.
  @Override
  protected void checkPostcondition(
      MethodTree methodTree, AnnotationMirror annotation, JavaExpression expression) {
    super.checkPostcondition(methodTree, annotation, expression);
    // Only check if the required annotation is a CalledMethods annotation (implying
    // the method was annotated with @EnsuresCalledMethods).
    if (!AnnotationUtils.areSameByName(
        annotation, "org.checkerframework.checker.calledmethods.qual.CalledMethods")) {
      return;
    }
    if (!isMustCallMethod(methodTree)) {
      // In this case, the method has an EnsuresCalledMethods annotation but is not a
      // destructor, so no further checking is required.
      return;
    }
    CFAbstractStore<?, ?> exitStore = atypeFactory.getExceptionalExitStore(methodTree);
    if (exitStore == null) {
      // If there is no exceptional exitStore, then the method cannot throw an exception and
      // there is no need to check anything else.
    } else {
      CFAbstractValue<?> value = exitStore.getValue(expression);
      AnnotationMirror inferredAnno = null;
      if (value != null) {
        AnnotationMirrorSet annos = value.getAnnotations();
        inferredAnno = qualHierarchy.findAnnotationInSameHierarchy(annos, annotation);
      }
      if (!checkContract(expression, annotation, inferredAnno, exitStore)) {
        String inferredAnnoStr =
            inferredAnno == null
                ? "no information about " + expression.toString()
                : inferredAnno.toString();
        checker.reportError(
            methodTree,
            "destructor.exceptional.postcondition",
            methodTree.getName(),
            expression.toString(),
            inferredAnnoStr,
            annotation);
      }
    }
  }

  /**
   * Returns true iff the {@code MustCall} annotation of the class that encloses the methodTree
   * names this method.
   *
   * @param methodTree the declaration of a method
   * @return whether that method is one of the must-call methods for its enclosing class
   */
  private boolean isMustCallMethod(MethodTree methodTree) {
    ExecutableElement elt = TreeUtils.elementFromDeclaration(methodTree);
    TypeElement containingClass = ElementUtils.enclosingTypeElement(elt);
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        rlTypeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotationMirror mcAnno =
        mustCallAnnotatedTypeFactory
            .getAnnotatedType(containingClass)
            .getPrimaryAnnotationInHierarchy(mustCallAnnotatedTypeFactory.TOP);
    List<String> mcValues =
        AnnotationUtils.getElementValueArray(
            mcAnno, mustCallAnnotatedTypeFactory.getMustCallValueElement(), String.class);
    String methodName = elt.getSimpleName().toString();
    return mcValues.contains(methodName);
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

    // This value is side-effected.
    List<String> unsatisfiedMustCallObligationsOfOwningField =
        rlTypeFactory.getMustCallValues(field);

    if (unsatisfiedMustCallObligationsOfOwningField.isEmpty()) {
      return;
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

          AnnotationMirrorSet allEnsuresCalledMethodsAnnos =
              getEnsuresCalledMethodsAnnotations((ExecutableElement) siblingElement, rlTypeFactory);
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
                unsatisfiedMustCallObligationsOfOwningField.removeAll(methods);
              }
            }
            if (unsatisfiedMustCallObligationsOfOwningField.isEmpty()) {
              return;
            }
          }

          if (!unsatisfiedMustCallObligationsOfOwningField.isEmpty()) {
            // This variable could be set immediately before reporting the error, but
            // IMO it is more clear to set it here.
            error =
                " @EnsuresCalledMethods written on MustCall methods doesn't contain "
                    + MustCallConsistencyAnalyzer.formatMissingMustCallMethods(
                        unsatisfiedMustCallObligationsOfOwningField);
          }
        }
      }
    }

    if (!unsatisfiedMustCallObligationsOfOwningField.isEmpty()) {
      checker.reportError(
          field,
          "required.method.not.called",
          MustCallConsistencyAnalyzer.formatMissingMustCallMethods(
              unsatisfiedMustCallObligationsOfOwningField),
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
}
