package org.checkerframework.checker.calledmethods;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic;
import org.checkerframework.checker.calledmethods.builder.BuilderFrameworkSupport;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsVarargs;
import org.checkerframework.common.accumulation.AccumulationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This visitor implements the custom error message "finalizer.invocation". It also supports
 * counting the number of framework build calls.
 */
public class CalledMethodsVisitor extends AccumulationVisitor {

  /**
   * Creates a new CalledMethodsVisitor.
   *
   * @param checker the type-checker associated with this visitor
   */
  public CalledMethodsVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Issue an error at every EnsuresCalledMethodsVarargs annotation, because using it is unsound.
   */
  @Override
  public Void visitAnnotation(AnnotationTree tree, Void p) {
    AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(tree);
    if (AnnotationUtils.areSameByName(
            anno, "org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsVarargs")
        // Temporary, for backward compatibility.
        || AnnotationUtils.areSameByName(
            anno, "org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsVarArgs")) {
      // We can't verify these yet.  Emit an error (which will have to be suppressed) for now.
      checker.report(tree, new DiagMessage(Diagnostic.Kind.ERROR, "ensuresvarargs.unverified"));
    }
    return super.visitAnnotation(tree, p);
  }

  @Override
  @SuppressWarnings("deprecation") // EnsuresCalledMethodsVarArgs
  public void processMethodTree(String className, MethodTree tree) {
    ExecutableElement elt = TreeUtils.elementFromDeclaration(tree);
    AnnotationMirror ecmv = atypeFactory.getDeclAnnotation(elt, EnsuresCalledMethodsVarargs.class);
    if (ecmv != null) {
      if (!elt.isVarArgs()) {
        checker.report(tree, new DiagMessage(Diagnostic.Kind.ERROR, "ensuresvarargs.invalid"));
      }
    }
    for (EnsuresCalledMethodOnExceptionContract postcond :
        ((CalledMethodsAnnotatedTypeFactory) atypeFactory).getExceptionalPostconditions(elt)) {
      checkExceptionalPostcondition(postcond, tree);
    }
    super.processMethodTree(className, tree);
  }

  /**
   * Check if the given postcondition is really ensured by the body of the given method.
   *
   * @param postcond the postcondition to check
   * @param tree the method
   */
  protected void checkExceptionalPostcondition(
      EnsuresCalledMethodOnExceptionContract postcond, MethodTree tree) {
    CFAbstractStore<?, ?> exitStore = atypeFactory.getExceptionalExitStore(tree);
    if (exitStore == null) {
      // If there is no exceptional exitStore, then the method cannot throw exceptions and
      // there is no need to check anything.
      return;
    }

    JavaExpression e;
    try {
      e = StringToJavaExpression.atMethodBody(postcond.getExpression(), tree, checker);
    } catch (JavaExpressionParseUtil.JavaExpressionParseException ex) {
      checker.report(tree, ex.getDiagMessage());
      return;
    }

    AnnotationMirror requiredAnno = atypeFactory.createAccumulatorAnnotation(postcond.getMethod());

    CFAbstractValue<?> value = exitStore.getValue(e);
    AnnotationMirror inferredAnno = null;
    if (value != null) {
      AnnotationMirrorSet annos = value.getAnnotations();
      inferredAnno = qualHierarchy.findAnnotationInSameHierarchy(annos, requiredAnno);
    }

    if (!checkContract(e, requiredAnno, inferredAnno, exitStore)) {
      checker.reportError(
          tree,
          "contracts.exceptional.postcondition",
          tree.getName(),
          contractExpressionAndType(postcond.getExpression(), inferredAnno),
          contractExpressionAndType(postcond.getExpression(), requiredAnno));
    }
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    if (checker.getBooleanOption(CalledMethodsChecker.COUNT_FRAMEWORK_BUILD_CALLS)) {
      ExecutableElement element = TreeUtils.elementFromUse(tree);
      for (BuilderFrameworkSupport builderFrameworkSupport :
          ((CalledMethodsAnnotatedTypeFactory) getTypeFactory()).getBuilderFrameworkSupports()) {
        if (builderFrameworkSupport.isBuilderBuildMethod(element)) {
          ((CalledMethodsChecker) checker).numBuildCalls++;
          break;
        }
      }
    }
    return super.visitMethodInvocation(tree, p);
  }

  /** Turns some "method.invocation" errors into "finalizer.invocation" errors. */
  @Override
  protected void reportMethodInvocabilityError(
      MethodInvocationTree tree, AnnotatedTypeMirror found, AnnotatedTypeMirror expected) {

    AnnotationMirror expectedCM = expected.getPrimaryAnnotation(CalledMethods.class);
    if (expectedCM != null) {
      AnnotationMirror foundCM = found.getPrimaryAnnotation(CalledMethods.class);
      Set<String> foundMethods =
          foundCM == null
              ? Collections.emptySet()
              : new HashSet<>(atypeFactory.getAccumulatedValues(foundCM));
      List<String> expectedMethods = atypeFactory.getAccumulatedValues(expectedCM);
      StringJoiner missingMethods = new StringJoiner(" ");
      for (String expectedMethod : expectedMethods) {
        if (!foundMethods.contains(expectedMethod)) {
          missingMethods.add(expectedMethod + "()");
        }
      }

      checker.reportError(tree, "finalizer.invocation", missingMethods.toString());
    } else {
      super.reportMethodInvocabilityError(tree, found, expected);
    }
  }
}
