package org.checkerframework.checker.mustcall;

import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * This copy of the Must Call Checker is identical, except that it does not load the stub files that
 * treat unconnected sockets as {@code @MustCall({})}. See SocketCreatesMustCallFor.astub.
 *
 * <p>The only difference is the contents of the @StubFiles annotation.
 */
@StubFiles({
  "JavaEE.astub",
  "Reflection.astub",
})
@SupportedOptions({MustCallChecker.NO_CREATES_OBLIGATION})
public class MustCallNoCreatesMustCallForChecker extends MustCallChecker {}
