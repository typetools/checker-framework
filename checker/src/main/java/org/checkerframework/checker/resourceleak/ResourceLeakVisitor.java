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
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The visitor for the Resource Leak Checker. Responsible for some {@link CreatesMustCallFor}
 * checking.
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
    List<String> cmcfValues = getLiteralCreatesMustCallForValues(elt, mcAtf, rlTypeFactory);
    if (!cmcfValues.isEmpty()) {
      // Check the validity of the annotation, by ensuring that if this method is overriding another
      // method it also creates at least as many obligations. Without this check, dynamic dispatch
      // might allow e.g. a field to be overwritten by a CMCF method, but the CMCF effect wouldn't
      // occur.
      for (ExecutableElement overridden : ElementUtils.getOverriddenMethods(elt, this.types)) {
        List<String> overriddenCoValues =
            getLiteralCreatesMustCallForValues(overridden, mcAtf, rlTypeFactory);
        if (!overriddenCoValues.containsAll(cmcfValues)) {
          String foundCoValueString = String.join(", ", cmcfValues);
          String neededCoValueString = String.join(", ", overriddenCoValues);
          String actualClassname = ElementUtils.getEnclosingClassName(elt);
          String overriddenClassname = ElementUtils.getEnclosingClassName(overridden);
          checker.reportError(
              node,
              "creates.mustcall.for.override.invalid",
              actualClassname + "#" + elt,
              overriddenClassname + "#" + overridden,
              foundCoValueString,
              neededCoValueString);
        }
      }
    }
    return super.visitMethod(node, p);
  }

  /**
   * Returns the literal string present in the given @CreatesMustCallFor annotation, or "this" if
   * there is none.
   *
   * @param createsMustCallFor an @CreatesMustCallFor annotation
   * @param mcAtf a MustCallAnnotatedTypeFactory, to source the value element
   * @return the string value
   */
  private static String getLiteralCreatesMustCallForValue(
      AnnotationMirror createsMustCallFor, MustCallAnnotatedTypeFactory mcAtf) {
    return AnnotationUtils.getElementValue(
        createsMustCallFor, mcAtf.getCreatesMustCallForValueElement(), String.class, "this");
  }

  /**
   * Returns all the literal strings present in the @CreatesMustCallFor annotations on the given
   * element. This version correctly handles multiple CreatesMustCallFor annotations on the same
   * element. This differs from {@link
   * org.checkerframework.checker.mustcall.CreatesMustCallForElementSupplier#getCreatesMustCallForExpressions(MethodInvocationNode,
   * GenericAnnotatedTypeFactory, CreatesMustCallForElementSupplier)} in that this version does not
   * take into account the calling context when parsing the strings; instead, the literal values
   * written by the programmer are returned.
   *
   * @param elt an executable element
   * @param mcAtf a MustCallAnnotatedTypeFactory, to source the value element
   * @param atypeFactory a ResourceLeakAnnotatedTypeFactory
   * @return the literal strings present in the @CreatesMustCallFor annotation(s) of that element,
   *     substituting the default "this" for empty annotations. This method returns the empty list
   *     iff there are no @CreatesMustCallFor annotations on elt. The returned list is always
   *     modifiable if it is non-empty.
   */
  /*package-private*/ static List<String> getLiteralCreatesMustCallForValues(
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
        result.add(getLiteralCreatesMustCallForValue(cmcf, mcAtf));
      }
    }
    AnnotationMirror createsMustCallFor =
        atypeFactory.getDeclAnnotation(elt, CreatesMustCallFor.class);
    if (createsMustCallFor != null) {
      result.add(getLiteralCreatesMustCallForValue(createsMustCallFor, mcAtf));
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
   * Checks validity of a final field {@code field} with an {@code @Owning} annotation. Say the type
   * of {@code field} is {@code @MustCall("m"}}. This method checks that the enclosing class of
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

    List<String> fieldMCAnno = rlTypeFactory.getMustCallValue(field);
    String error = "";

    if (!fieldMCAnno.isEmpty()) {
      Element enclosingElement = field.getEnclosingElement();
      List<String> enclosingMCAnno = rlTypeFactory.getMustCallValue(enclosingElement);

      if (enclosingMCAnno != null) {
        List<? extends Element> classElements = enclosingElement.getEnclosedElements();
        for (Element element : classElements) {
          if (fieldMCAnno.isEmpty()) {
            return;
          }
          if (element.getKind() == ElementKind.METHOD
              && enclosingMCAnno.contains(element.getSimpleName().toString())) {
            AnnotationMirror ensuresCalledMethodsAnno =
                rlTypeFactory.getDeclAnnotation(element, EnsuresCalledMethods.class);

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
                  fieldMCAnno.removeAll(methods);
                }
              }
            }

            if (!fieldMCAnno.isEmpty()) {
              error =
                  " @EnsuresCalledMethods written on MustCall methods doesn't contain "
                      + MustCallConsistencyAnalyzer.formatMissingMustCallMethods(fieldMCAnno);
            }
          }
        }
      } else {
        error = " The enclosing element doesn't have a @MustCall annotation";
      }
    }

    if (!fieldMCAnno.isEmpty()) {
      checker.reportError(
          field,
          "required.method.not.called",
          MustCallConsistencyAnalyzer.formatMissingMustCallMethods(fieldMCAnno),
          field.asType().toString(),
          error);
    }
  }
}
