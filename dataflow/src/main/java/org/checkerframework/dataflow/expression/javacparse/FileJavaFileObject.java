package org.checkerframework.dataflow.expression.javacparse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/** A JavaFileObject constructed from a file. */
class FileJavaFileObject extends SimpleJavaFileObject {

  /** The contents of the file. */
  private String javaCode;

  /**
   * Creates a FileJavaFileObject for the given file.
   *
   * @param filename the file name of a Java source file
   * @throws IOException if there is trouble reading the file
   */
  public FileJavaFileObject(String filename) throws IOException {
    super(Path.of(filename).toUri(), JavaFileObject.Kind.SOURCE);
    File file = new File(filename);
    if (!file.exists()) {
      throw new IOException("file does not exist: " + filename);
    }
    if (!file.canRead()) {
      throw new IOException("cannot read file: " + filename);
    }
    javaCode = new String(Files.readAllBytes(Path.of(filename)), Charset.defaultCharset());
  }

  /**
   * Creates a StringJavaFileObject for the given path.
   *
   * @param pathname the path name of a Java source file
   * @throws IOException if there is trouble reading the file
   */
  public FileJavaFileObject(Path pathname) throws IOException {
    this(pathname.toString());
  }

  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
    return javaCode;
  }
}
