package org.checkerframework.checker.calledmethodsonelements;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFValue;

/**
 * The analysis for the Called Methods Checker. The analysis is specialized to ignore certain
 * exception types; see {@link #isIgnoredExceptionType(TypeMirror)}.
 */
public class CalledMethodsOnElementsAnalysis extends CFAnalysis {

  @Override
  public void performAnalysis(ControlFlowGraph cfg, List<FieldInitialValue<CFValue>> fieldValues) {
    System.out.println("cmOE");
    System.out.println("cfg: " + ((CFGMethod) cfg.getUnderlyingAST()).getMethodName());
    CalledMethodsAnnotatedTypeFactory.postAnalyzeStatically(cfg);
    super.performAnalysis(cfg, fieldValues);
  }

  /**
   * The fully-qualified names of the exception types that are ignored by this checker when
   * computing dataflow stores.
   */
  protected static final Set<@CanonicalName String> ignoredExceptionTypes =
      ImmutableSet.of(
          // Use the Nullness Checker instead.
          NullPointerException.class.getCanonicalName(),
          // Ignore run-time errors, which cannot be predicted statically. Doing
          // so is unsound in the sense that they could still occur - e.g., the
          // program could run out of memory - but if they did, the checker's
          // results would be useless anyway.
          Error.class.getCanonicalName(),
          RuntimeException.class.getCanonicalName());

  /**
   * Creates a new {@code CalledMethodsOnElementsAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  public CalledMethodsOnElementsAnalysis(
      BaseTypeChecker checker, CalledMethodsOnElementsAnnotatedTypeFactory factory) {
    super(checker, factory);
  }
}
