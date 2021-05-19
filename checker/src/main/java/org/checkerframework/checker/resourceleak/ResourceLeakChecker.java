package org.checkerframework.checker.resourceleak;

import static javax.tools.Diagnostic.Kind.WARNING;
import static org.checkerframework.checker.mustcall.MustCallChecker.NO_CREATES_OBLIGATION;

import java.util.LinkedHashSet;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesObligationChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * The main typechecker for the Resource Leak Checker. This checker is a modifed {@link
 * CalledMethodsChecker} that checks that the must-call obligations of each expression (as computed
 * via the {@link org.checkerframework.checker.mustcall.MustCallChecker} have been fulfilled.
 */
@SupportedOptions({
  ResourceLeakChecker.COUNT_MUST_CALL,
  MustCallChecker.NO_CREATES_OBLIGATION,
  MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP,
  MustCallChecker.NO_RESOURCE_ALIASES
})
public class ResourceLeakChecker extends CalledMethodsChecker {

  /**
   * An option for counting how many must-call obligations were checked by the Resource Leak
   * Checker, and emitting the number after processing all files.
   */
  public static final String COUNT_MUST_CALL = "countMustCall";

  /**
   * The number of expressions with must-call obligations that were checked. Incremented only if the
   * {@link #COUNT_MUST_CALL} option was supplied.
   */
  int numMustCall = 0;

  /**
   * The number of must-call related errors issued. The count of verified must-call expressions is
   * the difference between this and {@code numMustCall}.
   */
  int numMustCallFailed = 0;

  @Override
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
        super.getImmediateSubcheckerClasses();

    if (this.processingEnv.getOptions().containsKey(NO_CREATES_OBLIGATION)) {
      checkers.add(MustCallNoCreatesObligationChecker.class);
    } else {
      checkers.add(MustCallChecker.class);
    }

    return checkers;
  }

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new ResourceLeakVisitor(this);
  }

  @Override
  public void reportError(Object source, @CompilerMessageKey String messageKey, Object... args) {
    if (messageKey.equals("required.method.not.called")) {
      // This looks crazy but it's safe because of the warning key.
      String qualifiedTypeName = (String) args[1];
      if (qualifiedTypeName.startsWith("java")) {
        numMustCallFailed++;
      }
    }
    super.reportError(source, messageKey, args);
  }

  @Override
  public void typeProcessingOver() {
    if (hasOption(COUNT_MUST_CALL)) {
      message(WARNING, "Found %d must call obligation(s).%n", numMustCall);
      message(
          WARNING,
          "Successfully verified %d must call obligation(s).%n",
          numMustCall - numMustCallFailed);
    }
    super.typeProcessingOver();
  }
}
