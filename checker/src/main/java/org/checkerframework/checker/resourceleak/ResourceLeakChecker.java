package org.checkerframework.checker.resourceleak;

import java.util.Set;
import javax.tools.Diagnostic;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesMustCallForChecker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * The entry point for the Resource Leak Checker. This checker is a modifed {@link
 * CalledMethodsChecker} that checks that the must-call obligations of each expression (as computed
 * via the {@link org.checkerframework.checker.mustcall.MustCallChecker} have been fulfilled.
 */
@SupportedOptions({
  "permitStaticOwning",
  "permitInitializationLeak",
  ResourceLeakChecker.COUNT_MUST_CALL,
  MustCallChecker.NO_CREATES_MUSTCALLFOR,
  MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP,
  MustCallChecker.NO_RESOURCE_ALIASES,
  ResourceLeakChecker.ENABLE_WPI_FOR_RLC,
})
@StubFiles("IOUtils.astub")
public class ResourceLeakChecker extends CalledMethodsChecker {

  /** Creates a ResourceLeakChecker. */
  public ResourceLeakChecker() {}

  /**
   * Command-line option for counting how many must-call obligations were checked by the Resource
   * Leak Checker, and emitting the number after processing all files. Used for generating tables
   * for a research paper. Not of interest to most users.
   */
  public static final String COUNT_MUST_CALL = "countMustCall";

  /**
   * Command-line option for enabling WPI for the Resource Leak Checker. By default, whole-program
   * inference is disabled when -Ainfer flag is used. Instead, the special mechanism as implemented
   * in {@link org.checkerframework.checker.resourceleak.MustCallInference} is executed as the best
   * inference mechanism for the Resource Leak Checker. However, for testing and future experimental
   * purposes, we defined this flag to enable whole-program inference (WPI) when running the
   * Resource Leak Checker inference.
   */
  public static final String ENABLE_WPI_FOR_RLC = "enableWpiForRlc";

  /**
   * The number of expressions with must-call obligations that were checked. Incremented only if the
   * {@link #COUNT_MUST_CALL} command-line option was supplied.
   */
  /*package-private*/ int numMustCall = 0;

  /**
   * The number of must-call-related errors issued. The count of verified must-call expressions is
   * the difference between this and {@link #numMustCall}.
   */
  private int numMustCallFailed = 0;

  @Override
  protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();

    if (this.processingEnv.getOptions().containsKey(MustCallChecker.NO_CREATES_MUSTCALLFOR)) {
      checkers.add(MustCallNoCreatesMustCallForChecker.class);
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
  public void reportError(
      @Nullable Object source, @CompilerMessageKey String messageKey, Object... args) {
    if (messageKey.equals("required.method.not.called")) {
      // This is safe because of the message key.
      String qualifiedTypeName = (String) args[1];
      // Only count classes in the JDK, not user-defined classes.
      if (MustCallConsistencyAnalyzer.isJdkClass(qualifiedTypeName)) {
        numMustCallFailed++;
      }
    }
    super.reportError(source, messageKey, args);
  }

  @Override
  public void typeProcessingOver() {
    if (hasOption(COUNT_MUST_CALL)) {
      message(Diagnostic.Kind.WARNING, "Found %d must call obligation(s).%n", numMustCall);
      message(
          Diagnostic.Kind.WARNING,
          "Successfully verified %d must call obligation(s).%n",
          numMustCall - numMustCallFailed);
    }
    super.typeProcessingOver();
  }
}
