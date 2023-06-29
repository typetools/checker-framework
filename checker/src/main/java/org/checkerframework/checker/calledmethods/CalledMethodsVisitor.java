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
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsVarArgs;
import org.checkerframework.common.accumulation.AccumulationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
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
   * Issue an error at every EnsuresCalledMethodsVarArgs annotation, because using it is unsound.
   */
  @Override
  public Void visitAnnotation(AnnotationTree tree, Void p) {
    AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(tree);
    if (AnnotationUtils.areSameByName(
        anno, "org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsVarArgs")) {
      // We can't verify these yet.  Emit an error (which will have to be suppressed) for now.
      checker.report(tree, new DiagMessage(Diagnostic.Kind.ERROR, "ensuresvarargs.unverified"));
    }
    return super.visitAnnotation(tree, p);
  }

  @Override
  public Void visitMethod(MethodTree tree, Void p) {
    ExecutableElement elt = TreeUtils.elementFromDeclaration(tree);
    AnnotationMirror ecmva = atypeFactory.getDeclAnnotation(elt, EnsuresCalledMethodsVarArgs.class);
    if (ecmva != null) {
      if (!elt.isVarArgs()) {
        checker.report(tree, new DiagMessage(Diagnostic.Kind.ERROR, "ensuresvarargs.invalid"));
      }
    }
    return super.visitMethod(tree, p);
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
