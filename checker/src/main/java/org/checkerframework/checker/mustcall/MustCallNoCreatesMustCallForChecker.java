package org.checkerframework.checker.mustcall;

import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

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
@SuppressWarningsPrefix({
    // Preferred checkername, so that warnings are suppressed regardless of the option passed.
    "mustcall",
    // Also supported, but will only suppress warnings from this checker (and not from the regular
    // Must Call Checker).
    "mustcallnocreatesmustcallfor"
})
@SupportedOptions({MustCallChecker.NO_CREATES_MUSTCALLFOR})
public class MustCallNoCreatesMustCallForChecker extends MustCallChecker {}
