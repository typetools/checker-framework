package org.checkerframework.framework.stub;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.StubUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.SimpleVoidVisitor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.javacutil.BugInCF;

/** Utility class for annotation files (stub files and ajava files). */
public class AnnotationFileUtil {

  /** Do not instantiate. */
  private AnnotationFileUtil() {
    throw new Error("Do not instantiate");
  }

  /**
   * The types of files that can contain annotations. Also indicates the file's source, such as from
   * the JDK, built in, or from the command line.
   *
   * <p>Stub files have extension ".astub". Ajava files have extension ".ajava".
   */
  public enum AnnotationFileType {
    /** Stub file in the annotated JDK. */
    JDK_STUB,
    /** Stub file built into a checker. */
    BUILTIN_STUB,
    /** Stub file provided on command line. */
    COMMAND_LINE_STUB,
    /** Ajava file being parsed as if it is a stub file. */
    AJAVA_AS_STUB,
    /** Ajava file provided on command line. */
    AJAVA,
    /** IntelliJ annotations. */
    INTELLIJ_ANNOTATIONS;

    /**
     * Returns true if this represents a stub file.
     *
     * @return true if this represents a stub file
     */
    public boolean isStub() {
      return switch (this) {
        case JDK_STUB, BUILTIN_STUB, COMMAND_LINE_STUB, AJAVA_AS_STUB -> true;
        case AJAVA, INTELLIJ_ANNOTATIONS -> false;
        default -> throw new BugInCF("unhandled case " + this);
      };
    }

    /**
     * Returns true if this annotation file is built-in (not provided on the command line).
     *
     * @return true if this annotation file is built-in (not provided on the command line)
     */
    public boolean isBuiltIn() {
      return switch (this) {
        case JDK_STUB, BUILTIN_STUB -> true;
        case COMMAND_LINE_STUB, AJAVA_AS_STUB, AJAVA, INTELLIJ_ANNOTATIONS -> false;
        default -> throw new BugInCF("unhandled case " + this);
      };
    }

    /**
     * Returns true if this annotation file was provided on the command line (not built-in).
     *
     * @return true if this annotation file was provided on the command line (not built-in)
     */
    public boolean isCommandLine() {
      return switch (this) {
        case JDK_STUB, BUILTIN_STUB -> false;
        case COMMAND_LINE_STUB, AJAVA_AS_STUB, AJAVA, INTELLIJ_ANNOTATIONS -> true;
        default -> throw new BugInCF("unhandled case " + this);
      };
    }
  }

  /**
   * Finds the type declaration with the given class name in a StubUnit.
   *
   * @param className fully qualified name of the type declaration to find
   * @param indexFile a StubUnit to search
   * @return the declaration in {@code indexFile} with {@code className} if it exists, null
   *     otherwise
   */
  /*package-private*/ static @Nullable TypeDeclaration<?> findDeclaration(
      String className, StubUnit indexFile) {
    int indexOfDot = className.lastIndexOf('.');

    if (indexOfDot == -1) {
      // classes not within a package needs to be the first in the index file
      CompilationUnit cu = indexFile.getCompilationUnits().get(0);
      assert !cu.getPackageDeclaration().isPresent();
      return findDeclaration(className, cu);
    }

    String packageName = className.substring(0, indexOfDot);
    String simpleName = className.substring(indexOfDot + 1);

    for (CompilationUnit cu : indexFile.getCompilationUnits()) {
      if (cu.getPackageDeclaration().isPresent()
          && cu.getPackageDeclaration().get().getNameAsString().equals(packageName)) {
        TypeDeclaration<?> type = findDeclaration(simpleName, cu);
        if (type != null) {
          return type;
        }
      }
    }

    // Couldn't find it
    return null;
  }

  /*package-private*/ static TypeDeclaration<?> findDeclaration(
      TypeElement type, StubUnit indexFile) {
    return findDeclaration(type.getQualifiedName().toString(), indexFile);
  }

  /*package-private*/ static @Nullable FieldDeclaration findDeclaration(
      VariableElement field, StubUnit indexFile) {
    TypeDeclaration<?> type = findDeclaration((TypeElement) field.getEnclosingElement(), indexFile);
    if (type == null) {
      return null;
    }

    for (BodyDeclaration<?> member : type.getMembers()) {
      if (!(member instanceof FieldDeclaration decl)) {
        continue;
      }
      for (VariableDeclarator var : decl.getVariables()) {
        if (toString(var).equals(field.getSimpleName().toString())) {
          return decl;
        }
      }
    }
    return null;
  }

  /*package-private*/ static @Nullable BodyDeclaration<?> findDeclaration(
      ExecutableElement method, StubUnit indexFile) {
    TypeDeclaration<?> type =
        findDeclaration((TypeElement) method.getEnclosingElement(), indexFile);
    if (type == null) {
      return null;
    }

    String methodRep = toString(method);

    for (BodyDeclaration<?> member : type.getMembers()) {
      if (member instanceof MethodDeclaration md) {
        if (toString(md).equals(methodRep)) {
          return member;
        }
      } else if (member instanceof ConstructorDeclaration cd) {
        if (toString(cd).equals(methodRep)) {
          return member;
        }
      }
    }
    return null;
  }

  /*package-private*/ static @Nullable TypeDeclaration<?> findDeclaration(
      String simpleName, CompilationUnit cu) {
    for (TypeDeclaration<?> type : cu.getTypes()) {
      if (simpleName.equals(type.getNameAsString())) {
        return type;
      }
    }
    // Couldn't find it
    return null;
  }

  /*package-private*/ static String toString(MethodDeclaration method) {
    return ElementPrinter.toString(method);
  }

  /*package-private*/ static String toString(ConstructorDeclaration constructor) {
    return ElementPrinter.toString(constructor);
  }

  /*package-private*/ static String toString(VariableDeclarator field) {
    return field.getNameAsString();
  }

  /*package-private*/ static String toString(FieldDeclaration field) {
    assert field.getVariables().size() == 1;
    return toString(field.getVariables().get(0));
  }

  /*package-private*/ static String toString(VariableElement element) {
    assert element.getKind().isField();
    return element.getSimpleName().toString();
  }

  /**
   * Returns a string representation of the given element, or null.
   *
   * @param element an Element
   * @return a string representation of the given element, or null
   */
  /*package-private*/ static @Nullable String toString(Element element) {
    if (element instanceof ExecutableElement ee) {
      return toString(ee);
    } else if (element instanceof VariableElement ve) {
      return toString(ve);
    } else {
      return null;
    }
  }

  /**
   * A qualified name, split into the part before the last period and the part after it.
   *
   * @param typeName the part before the last period
   * @param memberName the part after the last period; the name of a field or method of {@code
   *     typeName}
   */
  public record QualifiedName(@FullyQualifiedName String typeName, String memberName) {}

  /**
   * Split a name (which comes from an import statement) into the part before the last period and
   * the part after the last period.
   *
   * @param imported the name to split
   * @return the type name and the member name
   */
  @SuppressWarnings("signature") // string parsing
  public static QualifiedName partitionQualifiedName(String imported) {
    int lastDot = imported.lastIndexOf('.');
    @FullyQualifiedName String typeName = imported.substring(0, lastDot);
    String memberName = imported.substring(lastDot + 1);
    return new QualifiedName(typeName, memberName);
  }

  private static final class ElementPrinter extends SimpleVoidVisitor<Void> {
    public static String toString(Node n) {
      ElementPrinter printer = new ElementPrinter();
      n.accept(printer, null);
      return printer.getOutput();
    }

    private final StringBuilder sb = new StringBuilder();

    public String getOutput() {
      return sb.toString();
    }

    @Override
    public void visit(ConstructorDeclaration n, Void arg) {
      sb.append("<init>");

      sb.append('(');
      if (n.getParameters() != null) {
        for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext(); ) {
          Parameter p = i.next();
          p.accept(this, arg);

          if (i.hasNext()) {
            sb.append(',');
          }
        }
      }
      sb.append(')');
    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {
      sb.append(n.getName());

      sb.append('(');
      if (n.getParameters() != null) {
        for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext(); ) {
          Parameter p = i.next();
          p.accept(this, arg);

          if (i.hasNext()) {
            sb.append(',');
          }
        }
      }
      sb.append(')');
    }

    @Override
    public void visit(Parameter n, Void arg) {
      n.getType().accept(this, arg);
      if (n.isVarArgs()) {
        sb.append("[]");
      }
    }

    // Types
    @Override
    public void visit(ClassOrInterfaceType n, Void arg) {
      sb.append(n.getName());
    }

    @Override
    public void visit(PrimitiveType n, Void arg) {
      switch (n.getType()) {
        case BOOLEAN -> sb.append("boolean");
        case BYTE -> sb.append("byte");
        case CHAR -> sb.append("char");
        case DOUBLE -> sb.append("double");
        case FLOAT -> sb.append("float");
        case INT -> sb.append("int");
        case LONG -> sb.append("long");
        case SHORT -> sb.append("short");
        default -> throw new BugInCF("AnnotationFileUtil: unknown type: " + n.getType());
      }
    }

    @Override
    public void visit(com.github.javaparser.ast.type.ArrayType n, Void arg) {
      n.getComponentType().accept(this, arg);
      sb.append("[]");
    }

    @Override
    public void visit(VoidType n, Void arg) {
      sb.append("void");
    }

    @Override
    public void visit(WildcardType n, Void arg) {
      // We don't write type arguments
      // TODO: Why?
      throw new BugInCF("AnnotationFileUtil: don't print type args");
    }
  }

  /**
   * Returns the file system location that a path names. Looks for it as an absolute path or
   * relative to the current directory, then relative to the {@code user.dir} system property, then
   * relative to the {@code test.src} system property (which jtreg sets to the directory that
   * contains the test).
   *
   * @param location a relative or absolute path
   * @return the file or directory that {@code location} names, or null if none exists
   */
  public static @Nullable File findFileSystemLocation(String location) {
    File file = new File(location);
    if (file.exists()) {
      return file;
    }
    if (file.isAbsolute()) {
      return null;
    }

    file = new File(System.getProperty("user.dir"), location);
    if (file.exists()) {
      return file;
    }

    String testSrc = System.getProperty("test.src");
    if (testSrc != null) {
      file = new File(testSrc, location);
      if (file.exists()) {
        return file;
      }
    }

    return null;
  }

  /**
   * Returns annotation files found at a given file system location (does not look on classpath).
   *
   * @param location an annotation file (a stub file, ajava file, or IntelliJ IDEA annotation file),
   *     a jar or zip archive, or a directory. Look for it as described in {@link
   *     #findFileSystemLocation}. A file that is named explicitly is used if {@link
   *     #isUsableNamedFile} returns true for it; within an archive or directory, only files whose
   *     name indicates that they are of type {@code fileType} are used.
   * @param fileType file type of files to collect
   * @return annotation files with the given file type found in the file system (does not look on
   *     classpath). Returns null if the file system location does not exist; the caller may wish to
   *     issue a warning in that case.
   */
  public static @Nullable List<AnnotationFileResource> allAnnotationFiles(
      String location, AnnotationFileType fileType) {
    File file = findFileSystemLocation(location);
    if (file == null) {
      return null;
    }
    List<AnnotationFileResource> resources = new ArrayList<>();
    addAnnotationFilesToList(file, resources, fileType, true);
    return resources;
  }

  /**
   * Returns the entry with the given name in the given archive, or null if there is none.
   *
   * @param archive a JAR or ZIP archive
   * @param entryName the name of an entry in the archive, using "/" as the separator
   * @return the entry, or null if {@code archive} has no such entry or cannot be read
   */
  public static @Nullable AnnotationFileResource archiveEntry(File archive, String entryName) {
    try (JarFile file = new JarFile(archive)) {
      JarEntry entry = file.getJarEntry(entryName);
      if (entry == null) {
        return null;
      }
      try (InputStream in = file.getInputStream(entry)) {
        return new JarEntryAnnotationFileResource(archive, entryName, in.readAllBytes());
      }
    } catch (IOException e) {
      System.err.println(
          "AnnotationFileUtil: could not process archive "
              + archive
              + ": "
              + e.getClass().getName()
              + ": "
              + e.getMessage());
      return null;
    }
  }

  /**
   * Returns true if the given file is an annotation file of the given kind. A hidden file (one
   * whose name starts with ".") is not an annotation file; an example is the {@code ._Foo.astub}
   * metadata file that macOS adds to archives and to non-Mac file systems.
   *
   * @param path a file name or path, or the name of an archive entry
   * @param fileType the type of file to check against
   * @return true if {@code path} represents a file with file type matching {@code fileType}, false
   *     otherwise
   */
  static boolean isAnnotationFile(String path, AnnotationFileType fileType) {
    if (fileType == AnnotationFileType.INTELLIJ_ANNOTATIONS) {
      // Within a directory or an archive, an IntelliJ IDEA annotation file is always named
      // "annotations.xml".  (A file named on the command line may have any name; see
      // addAnnotationFilesToList.)
      return "annotations.xml".equals(new File(path).getName());
    }
    // Archive entry names use "/" as the separator on every platform.
    String name = path.substring(path.lastIndexOf('/') + 1);
    return !name.startsWith(".") && name.endsWith(fileType.isStub() ? ".astub" : ".ajava");
  }

  /**
   * Returns true if {@code name} is the name of a JAR or ZIP archive file. The file name extension
   * is compared case-insensitively, so {@code STUBS.ZIP} is an archive.
   *
   * @param name a file name
   * @return true if {@code name} is the name of a JAR or ZIP file
   */
  public static boolean isJarOrZip(String name) {
    String lowerCaseName = name.toLowerCase(Locale.ROOT);
    return lowerCaseName.endsWith(".jar") || lowerCaseName.endsWith(".zip");
  }

  /**
   * Returns true if a file that the user named explicitly, and that is not an archive, should be
   * used as an annotation file of type {@code fileType}. Every ajava file, and no stub file, must
   * end with {@code .ajava}.
   *
   * @param path a file name or path
   * @param fileType the type of annotation file that the user supplied
   * @return true if the named file should be used
   */
  static boolean isUsableNamedFile(String path, AnnotationFileType fileType) {
    if (fileType.isStub()) {
      return !isAnnotationFile(path, AnnotationFileType.AJAVA);
    } else {
      return isAnnotationFile(path, fileType);
    }
  }

  /**
   * Side-effects {@code resources} by adding annotation files of the given file type to it.
   *
   * @param location an annotation file (a stub file, ajava file, or IntelliJ IDEA annotation file),
   *     a jar or zip archive, or a directory. If an annotation file, add it to the {@code
   *     resources} list. If an archive, use all annotation files (of type {@code fileType})
   *     contained in it. If a directory, recurse on all files contained in it.
   * @param resources the list to add the found files to
   * @param fileType type of annotation files to add
   * @param isUserSupplied true if {@code location} was named by the user (say, on the command line)
   *     rather than being found by searching a directory or an archive. A file named by the user is
   *     used if {@link #isUsableNamedFile} returns true for it; a file found by searching is used
   *     only if its name indicates that it is an annotation file of type {@code fileType}. An
   *     archive found by searching a directory is not opened.
   */
  @SuppressWarnings("JdkObsolete") // JarFile.entries()
  private static void addAnnotationFilesToList(
      File location,
      List<AnnotationFileResource> resources,
      AnnotationFileType fileType,
      boolean isUserSupplied) {
    if (location.isDirectory()) {
      File[] directoryContents = location.listFiles();
      if (directoryContents == null) {
        System.err.println("AnnotationFileUtil: could not list directory: " + location);
        return;
      }
      Arrays.sort(directoryContents, Comparator.comparing(File::getName));
      for (File enclosed : directoryContents) {
        addAnnotationFilesToList(enclosed, resources, fileType, false);
      }
    } else if (location.isFile()) {
      String name = location.getName();
      if (isUserSupplied && isJarOrZip(name)) {
        try (JarFile file = new JarFile(location)) {
          Enumeration<JarEntry> entries = file.entries();
          while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (isAnnotationFile(entry.getName(), fileType)) {
              byte[] contents;
              try (InputStream in = file.getInputStream(entry)) {
                contents = in.readAllBytes();
              }
              resources.add(
                  new JarEntryAnnotationFileResource(location, entry.getName(), contents));
            }
          }
        } catch (IOException e) {
          System.err.println(
              "AnnotationFileUtil: could not process archive "
                  + location
                  + ": "
                  + e.getClass().getName()
                  + ": "
                  + e.getMessage());
        }
      } else if (isUserSupplied
          ? isUsableNamedFile(name, fileType)
          : isAnnotationFile(name, fileType)) {
        resources.add(new FileAnnotationFileResource(location));
      }
    }
  }

  /**
   * Returns true if the given {@link ExecutableElement} is the canonical constructor of a record
   * (i.e., the parameter types of the constructor correspond to the parameter types of the record
   * components, ignoring annotations).
   *
   * @param elt the constructor/method to check
   * @param types the Types instance to use for comparing types
   * @return true if elt is the canonical constructor of the record containing it
   */
  public static boolean isCanonicalConstructor(ExecutableElement elt, Types types) {
    if (elt.getKind() != ElementKind.CONSTRUCTOR) {
      return false;
    }
    TypeElement enclosing = (TypeElement) elt.getEnclosingElement();
    if (enclosing.getKind() != ElementKind.RECORD) {
      return false;
    }
    List<? extends Element> recordComponents = enclosing.getRecordComponents();
    if (recordComponents.size() == elt.getParameters().size()) {
      for (int i = 0; i < recordComponents.size(); i++) {
        if (!types.isSameType(
            recordComponents.get(i).asType(), elt.getParameters().get(i).asType())) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
}
