package org.checkerframework.framework.stub;

import static org.checkerframework.framework.stub.AnnotationFileTestUtil.write;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.SystemUtil;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests the diagnostics that {@link AnnotationFileElementTypes} issues about annotation files that
 * cannot be read: those named by the {@code -Astubs} and {@code -Aajava} command-line options, and
 * a checker's own stub files.
 */
public class AnnotationFileElementTypesTest {

  /** A temporary directory, deleted after each test. */
  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  /** The fully-qualified name of the checker that most of the tests run. */
  private static final String CHECKER = "org.checkerframework.common.value.ValueChecker";

  /** The message of the exception that {@link CloseFailingClassLoader} throws. */
  private static final String CLOSE_FAILURE = "simulated failure to close";

  /**
   * Compiles a trivial class with the Value Checker and the given {@code -Astubs} command-line
   * option, and returns the compiler's diagnostics.
   *
   * @param stubs the argument to the {@code -Astubs} command-line option
   * @return the diagnostics, each formatted as its kind, a colon, and its message, such as
   *     "WARNING: Ignoring Foo.stub: ..."
   * @throws IOException if the source file cannot be written or the compiler cannot be run
   */
  private List<String> diagnostics(String stubs) throws IOException {
    return diagnostics(CHECKER, null, "-Astubs=" + stubs);
  }

  /**
   * Compiles a trivial class with the Value Checker and the given {@code -Astubs} command-line
   * option, and returns the compiler's diagnostics.
   *
   * @param stubs the argument to the {@code -Astubs} command-line option
   * @param extraClasspath a directory to prepend to the classpath from which the checker is loaded,
   *     so that the checker finds the annotation files in it
   * @return the diagnostics, each formatted as its kind, a colon, and its message, such as
   *     "WARNING: Ignoring Foo.stub: ..."
   * @throws IOException if the source file cannot be written or the compiler cannot be run
   */
  private List<String> diagnostics(String stubs, File extraClasspath) throws IOException {
    try (URLClassLoader checkerLoader =
        new URLClassLoader(
            checkerClasspath(extraClasspath), ClassLoader.getPlatformClassLoader())) {
      return diagnostics(CHECKER, checkerLoader, "-Astubs=" + stubs);
    }
  }

  /**
   * Compiles a trivial class named {@code Test} with the given checker and command-line options,
   * and returns the compiler's diagnostics.
   *
   * @param checker the fully-qualified name of a checker, such as
   *     "org.checkerframework.common.value.ValueChecker"
   * @param checkerLoader the class loader from which to load {@code checker}, or null to let javac
   *     load it
   * @param checkerOptions command-line options for the checker, such as "-Astubs=foo.astub"
   * @return the diagnostics, each formatted as its kind, a colon, and its message, such as
   *     "WARNING: Ignoring Foo.stub: ..."
   * @throws IOException if the source file cannot be written or the compiler cannot be run
   */
  private List<String> diagnostics(
      String checker, @Nullable ClassLoader checkerLoader, String... checkerOptions)
      throws IOException {
    File sourceFile = write(new File(tmp.getRoot(), "src/Test.java"), "class Test {}");
    File classes = tmp.newFolder();
    List<String> options = new ArrayList<>();
    if (checkerLoader == null) {
      options.add("-processor");
      options.add(checker);
    }
    options.addAll(Arrays.asList(checkerOptions));
    options.addAll(
        Arrays.asList(
            "-ApermitMissingJdk",
            "-d",
            classes.getPath(),
            "-classpath",
            System.getProperty("java.class.path")));
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      Iterable<? extends JavaFileObject> javaFiles = fileManager.getJavaFileObjects(sourceFile);
      JavaCompiler.CompilationTask task =
          compiler.getTask(null, fileManager, collector, options, null, javaFiles);
      if (checkerLoader != null) {
        // The checker looks up its own resources with its own class loader, which javac's
        // -classpath command-line option does not affect.
        Processor processor =
            (Processor) checkerLoader.loadClass(checker).getDeclaredConstructor().newInstance();
        task.setProcessors(Collections.singletonList(processor));
      }
      task.call();
    } catch (UncheckedIOException e) {
      throw e.getCause();
    } catch (ReflectiveOperationException e) {
      throw new Error("Cannot instantiate " + checker, e);
    }
    List<String> result = new ArrayList<>();
    for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
      result.add(diagnostic.getKind() + ": " + diagnostic.getMessage(Locale.ROOT));
    }
    return result;
  }

  /**
   * Returns the given directory followed by this JVM's classpath. A class loader whose classpath is
   * this, and that does not delegate to the application class loader, loads classes that find
   * resources in {@code extraClasspath}.
   *
   * @param extraClasspath a directory
   * @return the URLs of {@code extraClasspath} and of the entries of this JVM's classpath
   * @throws IOException if a classpath entry cannot be converted to a URL
   */
  private static URL[] checkerClasspath(File extraClasspath) throws IOException {
    List<URL> urls = new ArrayList<>();
    urls.add(extraClasspath.toURI().toURL());
    for (String entry :
        SystemUtil.pathSeparatorSplitter.split(System.getProperty("java.class.path"))) {
      urls.add(new File(entry).toURI().toURL());
    }
    return urls.toArray(new URL[0]);
  }

  /**
   * A class loader that loads classes from a given directory and then from this JVM's classpath. It
   * does not delegate to the application class loader, so the classes that it loads find resources
   * in the given directory. The stream that it returns for a resource named {@code jdk.astub}
   * throws an exception when it is closed.
   */
  private static class CloseFailingClassLoader extends URLClassLoader {

    /**
     * Creates a class loader that loads classes from the given directory and then from this JVM's
     * classpath.
     *
     * @param extraClasspath a directory
     * @throws IOException if a classpath entry cannot be converted to a URL
     */
    CloseFailingClassLoader(File extraClasspath) throws IOException {
      super(checkerClasspath(extraClasspath), ClassLoader.getPlatformClassLoader());
    }

    @Override
    public @Nullable InputStream getResourceAsStream(String name) {
      InputStream in = super.getResourceAsStream(name);
      if (in == null || !name.endsWith("/jdk.astub")) {
        return in;
      }
      return new FilterInputStream(in) {
        @Override
        public void close() throws IOException {
          super.close();
          throw new IOException(CLOSE_FAILURE);
        }
      };
    }
  }

  /**
   * Asserts that one of the given diagnostics starts with the given text.
   *
   * @param prefix the expected start of a diagnostic, such as "WARNING: Ignoring"
   * @param diagnostics the diagnostics that the compiler issued
   */
  private static void assertHasDiagnostic(String prefix, List<String> diagnostics) {
    for (String diagnostic : diagnostics) {
      if (diagnostic.startsWith(prefix)) {
        return;
      }
    }
    Assert.fail("No diagnostic starts with \"" + prefix + "\"; diagnostics: " + diagnostics);
  }

  /**
   * Asserts that exactly one of the given diagnostics contains the given text, and that that
   * diagnostic is a warning.
   *
   * @param text the expected text of a diagnostic, such as "Could not read annotation resource"
   * @param diagnostics the diagnostics that the compiler issued
   */
  private static void assertOneWarning(String text, List<String> diagnostics) {
    List<String> matching = new ArrayList<>();
    for (String diagnostic : diagnostics) {
      if (diagnostic.contains(text)) {
        matching.add(diagnostic);
      }
    }
    Assert.assertEquals(
        "Diagnostics that contain \"" + text + "\"; all diagnostics: " + diagnostics,
        1,
        matching.size());
    Assert.assertTrue(matching.get(0), matching.get(0).startsWith("WARNING: "));
  }

  /** Tests that an archive within a named directory yields a note. */
  @Test
  public void archiveInDirectory() throws IOException {
    File dir = tmp.newFolder("stubdir");
    File jar = new File(dir, "lib.jar");
    try (OutputStream os = Files.newOutputStream(jar.toPath());
        ZipOutputStream zos = new ZipOutputStream(os)) {
      zos.putNextEntry(new ZipEntry("A.astub"));
      zos.closeEntry();
    }
    assertHasDiagnostic("NOTE: Not reading archive " + jar, diagnostics(dir.getPath()));
  }

  /**
   * Tests that a compound checker, each of whose checkers searches the named directory, yields one
   * note about an archive within the directory.
   */
  @Test
  public void archiveInDirectoryCompoundChecker() throws IOException {
    File dir = tmp.newFolder("stubdir");
    File jar = new File(dir, "lib.jar");
    try (OutputStream os = Files.newOutputStream(jar.toPath());
        ZipOutputStream zos = new ZipOutputStream(os)) {
      zos.putNextEntry(new ZipEntry("A.astub"));
      zos.closeEntry();
    }
    List<String> diagnostics =
        diagnostics(
            "org.checkerframework.framework.testchecker.compound.CompoundChecker",
            null,
            "-Astubs=" + dir.getPath());
    int count = 0;
    for (String diagnostic : diagnostics) {
      if (diagnostic.startsWith("NOTE: Not reading archive " + jar)) {
        count++;
      }
    }
    Assert.assertEquals(diagnostics.toString(), 1, count);
  }

  /** Tests that a named directory that cannot be listed yields a warning. */
  @Test
  public void unlistableDirectory() throws IOException {
    File dir = tmp.newFolder("stubdir");
    write(new File(dir, "A.astub"), "package p;");
    // Making a directory unreadable has no effect for the root user.
    Assume.assumeTrue(dir.setReadable(false));
    try {
      Assume.assumeTrue(dir.listFiles() == null);
      assertOneWarning(
          "Could not read annotation resource: " + dir.getPath() + ": ",
          diagnostics(dir.getPath()));
    } finally {
      dir.setReadable(true);
    }
  }

  /**
   * Tests that a directory that cannot be listed, within a named directory, yields a note rather
   * than a warning.
   */
  @Test
  public void unlistableDirectoryInDirectory() throws IOException {
    File dir = tmp.newFolder("stubdir");
    File sub = new File(dir, "sub");
    write(new File(sub, "A.astub"), "package p;");
    // Making a directory unreadable has no effect for the root user.
    Assume.assumeTrue(sub.setReadable(false));
    try {
      Assume.assumeTrue(sub.listFiles() == null);
      List<String> diagnostics = diagnostics(dir.getPath());
      assertHasDiagnostic("NOTE: Not reading directory " + sub, diagnostics);
      for (String diagnostic : diagnostics) {
        Assert.assertFalse(diagnostic, diagnostic.startsWith("WARNING: Could not read"));
      }
    } finally {
      sub.setReadable(true);
    }
  }

  /** Tests that a named archive that cannot be opened yields one warning. */
  @Test
  public void corruptArchive() throws IOException {
    File jar = write(new File(tmp.getRoot(), "stubs.jar"), "not a jar file");
    assertOneWarning(
        "Could not read annotation resource: " + jar.getPath() + ": ", diagnostics(jar.getPath()));
  }

  /**
   * Tests that a compound checker, each of whose checkers reads the named archive, yields one
   * warning about an archive that cannot be opened.
   */
  @Test
  public void corruptArchiveCompoundChecker() throws IOException {
    File jar = write(new File(tmp.getRoot(), "stubs.jar"), "not a jar file");
    assertOneWarning(
        "Could not read annotation resource: " + jar.getPath() + ": ",
        diagnostics(
            "org.checkerframework.framework.testchecker.compound.CompoundChecker",
            null,
            "-Astubs=" + jar.getPath()));
  }

  /**
   * Tests that a classpath annotation file that starts with much whitespace is parsed, not skipped
   * as if it were empty.
   */
  @Test
  public void leadingWhitespaceOnClasspath() throws IOException {
    File classpathDir = tmp.newFolder();
    write(
        new File(classpathDir, "Padded.astub"),
        " ".repeat(150) + "package p; class NoSuchClass {}");
    assertHasDiagnostic(
        "WARNING: /Padded.astub:", diagnostics("checker.jar/Padded.astub", classpathDir));
  }

  /** Tests that a classpath annotation file that cannot be read yields a warning. */
  @Test
  public void unreadableFileOnClasspath() throws IOException {
    File classpathDir = tmp.newFolder();
    File stub = write(new File(classpathDir, "Unreadable.astub"), "package p;");
    Assume.assumeTrue(stub.setReadable(false));
    // Making a file unreadable has no effect for the root user.
    Assume.assumeFalse(Files.isReadable(stub.toPath()));
    assertOneWarning(
        "Could not read annotation resource: /Unreadable.astub: ",
        diagnostics("checker.jar/Unreadable.astub", classpathDir));
  }

  /**
   * Tests that a checker's own stub file that cannot be read yields a warning. Reading such a file
   * fails only if closing it fails, so the checker is loaded by a class loader that simulates that
   * failure.
   */
  @Test
  public void unreadableCheckerStubFile() throws IOException {
    File classpathDir = tmp.newFolder();
    // The checker's stub files are in the same directory as the checker's class file.
    write(
        new File(classpathDir, "org/checkerframework/common/value/jdk.astub"),
        "package java.lang;");
    try (URLClassLoader checkerLoader = new CloseFailingClassLoader(classpathDir)) {
      assertOneWarning(
          "Could not read annotation resource from class "
              + CHECKER
              + ": jdk.astub: java.io.IOException: "
              + CLOSE_FAILURE,
          diagnostics(CHECKER, checkerLoader));
    }
  }

  /** Tests that the ajava file for a compilation unit that cannot be read yields a warning. */
  @Test
  public void unreadableAjavaFile() throws IOException {
    File ajavaDir = tmp.newFolder("ajava");
    File ajava = write(new File(ajavaDir, "Test-" + CHECKER + ".ajava"), "class Test {}");
    Assume.assumeTrue(ajava.setReadable(false));
    // Making a file unreadable has no effect for the root user.
    Assume.assumeFalse(Files.isReadable(ajava.toPath()));
    assertOneWarning(
        "Could not read ajava file: " + ajava.getPath() + ": ",
        diagnostics(CHECKER, null, "-Aajava=" + ajavaDir.getPath()));
  }
}
