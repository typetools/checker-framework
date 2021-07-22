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
    "SocketCreatesMustCallFor.astub",
})
@SupportedOptions({
    MustCallChecker.NO_CREATES_MUSTCALLFOR,
    MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP,
    MustCallChecker.NO_RESOURCE_ALIASES
})
public class MustCallChecker extends BaseTypeChecker {

    /** Disables @CreatesMustCallFor support. Not of interest to most users. */
    public static final String NO_CREATES_MUSTCALLFOR = "noCreatesMustCallFor";

    /** Disables @Owning/@NotOwning support. Not of interest to most users. */
    public static final String NO_LIGHTWEIGHT_OWNERSHIP = "noLightweightOwnership";

    /** Disables @MustCallAlias support. Not of interest to most users. */
    public static final String NO_RESOURCE_ALIASES = "noResourceAliases";
}
