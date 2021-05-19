package org.checkerframework.checker.mustcall;

import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * This typechecker ensures that {@code @}{@link MustCall} annotations are consistent with one
 * another. The Resource Leak Checker verifies that the given methods are actually called.
 */
@StubFiles({
  "JavaEE.astub",
  "Reflection.astub",
  "SocketCreatesObligation.astub",
})
@SupportedOptions({
  MustCallChecker.NO_CREATES_OBLIGATION,
  MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP,
  MustCallChecker.NO_RESOURCE_ALIASES
})
public class MustCallChecker extends BaseTypeChecker {

  /** Disables @CreatesObligation support. */
  public static final String NO_CREATES_OBLIGATION = "noCreatesObligation";

  /** Disables @Owning/@NotOwning support. */
  public static final String NO_LIGHTWEIGHT_OWNERSHIP = "noLightweightOwnership";

  /** Disables @MustCallAlias support. */
  public static final String NO_RESOURCE_ALIASES = "noResourceAliases";
}
