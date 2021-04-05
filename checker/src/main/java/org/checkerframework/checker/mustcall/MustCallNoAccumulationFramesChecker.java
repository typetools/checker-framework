package org.checkerframework.checker.mustcall;

import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * This copy of the Must Call Checker is identical, except that it does not load the stub files at
 * initialization that treat unconnected sockets as @MustCall({}). See
 * SocketAccumulationFrames.astub.
 *
 * <p>The only difference is the contents of the @StubFiles annotation, below.
 */
@StubFiles({
  "Socket.astub",
  "NotOwning.astub",
  "Stream.astub",
  "NoObligationGenerics.astub",
  "NoObligationStreams.astub",
  "Reflection.astub",
})
@SupportedOptions({MustCallChecker.NO_ACCUMULATION_FRAMES})
public class MustCallNoAccumulationFramesChecker extends MustCallChecker {}
