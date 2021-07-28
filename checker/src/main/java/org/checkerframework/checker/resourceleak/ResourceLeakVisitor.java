package org.checkerframework.checker.resourceleak;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.calledmethods.CalledMethodsVisitor;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.CreatesMustCallForElementSupplier;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The visitor for the Resource Leak Checker. Responsible for checking that the rules for {@link
 * Owning} fields are satisfied, and for checking that {@link CreatesMustCallFor} overrides are
 * valid.
 */
public class ResourceLeakVisitor extends CalledMethodsVisitor {

  /**
   * Because CalledMethodsVisitor doesn't have a type parameter, we need a reference to the type
   * factory that has this static type to access the features that ResourceLeakAnnotatedTypeFactory
   * implements but CalledMethodsAnnotatedTypeFactory does not.
   */
  private final ResourceLeakAnnotatedTypeFactory rlTypeFactory;

  /**
   * Create the visitor.
   *
   * @param checker the type-checker associated with this visitor
   */
  public ResourceLeakVisitor(final BaseTypeChecker checker) {
    super(checker);
    rlTypeFactory = (ResourceLeakAnnotatedTypeFactory) atypeFactory;
  }

  @Override
  protected ResourceLeakAnnotatedTypeFactory createTypeFactory() {
    return new ResourceLeakAnnotatedTypeFactory(checker);
  }

  @Override
  public Void visitMethod(MethodTree node, Void p) {
    ExecutableElement elt = TreeUtils.elementFromDeclaration(node);
    MustCallAnnotatedTypeFactory mcAtf =
        rlTypeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
    List<String> cmcfValues = getCreatesMustCallForValues(elt, mcAtf, rlTypeFactory);
    if (!cmcfValues.isEmpty()) {
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
              node,
              "creates.mustcall.for.override.invalid",
              actualClassname + "#" + elt,
              overriddenClassname + "#" + overridden,
              foundCmcfValueString,
              neededCmcfValueString);
        }
      }
    }
    return super.visitMethod(node, p);
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
   * CreatesMustCallForElementSupplier#getCreatesMustCallForExpressions} which does.
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

  @Override
  public Void visitVariable(VariableTree node, Void p) {
    Element varElement = TreeUtils.elementFromTree(node);

    if (varElement.getKind().isField()
        && !checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
        && rlTypeFactory.getDeclAnnotation(varElement, Owning.class) != null) {
      checkOwningField(varElement);
    }

    return super.visitVariable(node, p);
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
  private void checkOwningField(Element field) {

    if (checker.shouldSkipUses(field)) {
      return;
    }

    // This value is side-effected.
    List<String> unsatisfiedMustCallObligationsOfOwningField =
        rlTypeFactory.getMustCallValue(field);

    if (unsatisfiedMustCallObligationsOfOwningField.isEmpty()) {
      return;
    }

    String error = "";
    Element enclosingElement = field.getEnclosingElement();
    List<String> enclosingMustCallValues = rlTypeFactory.getMustCallValue(enclosingElement);

    if (enclosingMustCallValues == null) {
      error =
          " The enclosing element "
              + ElementUtils.getQualifiedName(enclosingElement)
              + " doesn't have a @MustCall annotation";
    } else {
      List<? extends Element> siblingsOfOwningField = enclosingElement.getEnclosedElements();
      for (Element siblingElement : siblingsOfOwningField) {
        if (siblingElement.getKind() == ElementKind.METHOD
            && enclosingMustCallValues.contains(siblingElement.getSimpleName().toString())) {
          AnnotationMirror ensuresCalledMethodsAnno =
              rlTypeFactory.getDeclAnnotation(siblingElement, EnsuresCalledMethods.class);

          if (ensuresCalledMethodsAnno != null) {
            List<String> values =
                AnnotationUtils.getElementValueArray(
                    ensuresCalledMethodsAnno,
                    rlTypeFactory.ensuresCalledMethodsValueElement,
                    String.class);
            for (String value : values) {
              if (value.contains(field.getSimpleName().toString())) {
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
            // This variable could be set immediately before reporting the error, but IMO
            // it is more clear to set it here.
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
          "field " + field.getSimpleName(),
          field.asType().toString(),
          error);
    }
  }
}
