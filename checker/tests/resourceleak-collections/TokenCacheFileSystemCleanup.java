package com.linkedin.tony.security;

import java.io.*;
import java.util.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class GetUriReturnType {}

class ErrorReturnType {}

class Log {

  public ErrorReturnType error(java.lang.String parameter0, java.lang.Exception parameter1) {
    throw new java.lang.Error();
  }
}

@InheritableMustCall("close")
class FileSystem {

  public GetUriReturnType getUri() {
    throw new java.lang.Error();
  }

  public void close() throws java.io.IOException {
    throw new java.lang.Error();
  }
}

class Path {

  public FileSystem getFileSystem() {
    throw new java.lang.Error();
  }
}

/*
 * Reproducer for collecting FileSystem instances, using them, and closing them in a finally
 * block.
 */
class TokenCacheFileSystemCleanup {

  private static final Log LOG = null;

  public static void obtainTokensForNamenodes(Path[] paths, String renewer) throws IOException {
    @OwningCollection List<FileSystem> fsSet = new ArrayList<>();
    try {
      for (Path path : paths) {
        fsSet.add(path.getFileSystem());
      }
      for (FileSystem fs : fsSet) {
        try {
          obtainTokensForNamenodesInternal(fs, renewer);
        } catch (Exception e) {
          LOG.error("Errors on getting delegation token for " + fs.getUri(), e);
        }
      }
    } finally {
      for (FileSystem fs : fsSet) {
        try {
          fs.close();
        } catch (Exception e) {
          LOG.error("Errors on closing FileSystem " + fs.getUri(), e);
        }
      }
    }
  }

  private static void obtainTokensForNamenodesInternal(FileSystem fs, String renewer)
      throws IOException {
    throw new java.lang.Error();
  }
}
