package org.checkerframework.framework.stub;

import com.sun.source.tree.CompilationUnitTree;
import io.github.classgraph.ClassGraph;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalNameOrEmpty;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.stub.AnnotationFileParser.AnnotationFileAnnotations;
import org.checkerframework.framework.stub.AnnotationFileParser.RecordComponentStub;
import org.checkerframework.framework.stub.AnnotationFileUtil.AnnotationFileType;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.IPair;
import org.plumelib.util.SystemPlume;

/**
 * Holds information about types parsed from annotation files (stub files or ajava files). When
 * using an ajava file, only holds information on public elements as with stub files.
 */
public class AnnotationFileElementTypes {
  /** Annotations from annotation files (but not from annotated JDK files). */
  private final AnnotationFileAnnotations annotationFileAnnos;

  /**
   * True if a file is currently being parsed. (If one is being parsed, don't try to parse another.)
   */
  private boolean parsing;

  /** AnnotatedTypeFactory. */
  private final AnnotatedTypeFactory factory;

  /**
   * Mapping from fully-qualified class name to corresponding JDK stub file from the file system
   * that have not yet been read. When a file is read, its mapping is removed from this map.
   *
   * <p>By contrast, {@link #remainingJdkStubFilesJar} contains JDK stub files from checker.jar.
   */
  private final Map<String, Path> remainingJdkStubFiles = new HashMap<>();

  /**
   * Mapping from fully-qualified class name to corresponding JDK stub files from checker.jar that
   * have not yet been read. When a file is read, its mapping is removed from this map.
   *
   * <p>By contrast, {@link #remainingJdkStubFiles} contains JDK stub files from the file system.
   */
  private final Map<String, String> remainingJdkStubFilesJar = new HashMap<>();

  /** Which version number of the annotated JDK should be used? */
  private final String annotatedJdkVersion;

  /** Should the JDK be parsed? */
  private final boolean shouldParseJdk;

  /** Parse all JDK files at startup rather than as needed. */
  private final boolean parseAllJdkFiles;

  /** True if -ApermitMissingJdk was passed on the command line. */
  private final boolean permitMissingJdk;

  /** True if -Aignorejdkastub was passed on the command line. */
  private final boolean ignorejdkastub;

  /** True if -AstubDebug was passed on the command line. */
  private final boolean stubDebug;

  /**
   * Creates an empty annotation source.
   *
   * @param factory a type factory
   */
  public AnnotationFileElementTypes(AnnotatedTypeFactory factory) {
    this.factory = factory;
    this.annotationFileAnnos = new AnnotationFileAnnotations();
    this.parsing = false;
    String release = SystemUtil.getReleaseValue(factory.getProcessingEnv());
    this.annotatedJdkVersion = release != null ? release : String.valueOf(SystemUtil.jreVersion);

    this.shouldParseJdk = !factory.getChecker().hasOption("ignorejdkastub");
    this.parseAllJdkFiles = factory.getChecker().hasOption("parseAllJdk");
    this.permitMissingJdk = factory.getChecker().hasOption("permitMissingJdk");
    this.ignorejdkastub = factory.getChecker().hasOption("ignorejdkastub");
    this.stubDebug = factory.getChecker().hasOption("stubDebug");
  }

  /**
   * Returns true if files are currently being parsed; otherwise, false.
   *
   * @return true if files are currently being parsed; otherwise, false
   */
  public boolean isParsing() {
    return parsing;
  }

  /**
   * Parses the stub files in the following order:
   *
   * <ol>
   *   <li>jdk.astub in this directory, if it exists and ignorejdkastub option is not supplied
   *   <li>jdk.astub in the same directory as the checker, if it exists and ignorejdkastub option is
   *       not supplied
   *   <li>If parsing annotated JDK as stub files, all package-info.java files under the jdk/
   *       directory
   *   <li>Stub files listed in @StubFiles annotation on the checker; must be in same directory as
   *       the checker
   *   <li>Stub files returned by {@link BaseTypeChecker#getExtraStubFiles} (treated like those
   *       listed in @StubFiles annotation)
   *   <li>Stub files provided via {@code -Astubs} compiler option
   * </ol>
   *
   * <p>If a type is annotated with a qualifier from the same hierarchy in more than one stub file,
   * the qualifier in the last stub file is applied.
   *
   * <p>If using JDK 11, then the JDK stub files are only parsed if a type or declaration annotation
   * is requested from a class in that file.
   */
  public void parseStubFiles() {
    if (stubDebug) {
      System.out.printf(
          "entered parseStubFiles() for %s, ignorejdkastub=%s%n",
          factory.getClass().getSimpleName(), ignorejdkastub);
    }
    parsing = true;
    BaseTypeChecker checker = factory.getChecker();
    if (!ignorejdkastub) {
      // 1. jdk.astub
      // Only look in .jar files, and parse it right away.
      String jdkVersionStub = "jdk" + annotatedJdkVersion + ".astub";
      parseOneStubFile(this.getClass(), "jdk.astub");
      parseOneStubFile(this.getClass(), jdkVersionStub);
      parseOneStubFile(checker.getClass(), "jdk.astub");
      parseOneStubFile(checker.getClass(), jdkVersionStub);
      // This needs to be special-cased for every jdkX.astub for which files exist. :-(
      if (annotatedJdkVersion.equals("8")) {
        String jdk11Stub = "jdk11.astub";
        parseOneStubFile(this.getClass(), jdk11Stub);
        parseOneStubFile(checker.getClass(), jdk11Stub);
      }

      // 2. Annotated JDK
      // This preps but does not parse the JDK files (except package-info.java files).
      // The JDK source code files will be parsed later, on demand.
      prepJdkStubs();
      // prepping the JDK parses all package-info.java files, which sets the `parsing` field
      // to false, so re-set it to true.
      parsing = true;
    }

    // 3. Stub files listed in @StubFiles annotation on the checker
    StubFiles stubFilesAnnotation = checker.getClass().getAnnotation(StubFiles.class);
    if (stubFilesAnnotation != null) {
      parseAnnotationFiles(
          Arrays.asList(stubFilesAnnotation.value()), AnnotationFileType.BUILTIN_STUB);
    }

    // 4. Stub files returned by the `getExtraStubFiles()` method
    parseAnnotationFiles(checker.getExtraStubFiles(), AnnotationFileType.BUILTIN_STUB);

    // 5. Stub files provided via -Astubs command-line option
    String stubsOption = checker.getOption("stubs");
    if (stubsOption != null) {
      parseAnnotationFiles(
          SystemUtil.pathSeparatorSplitter.splitToList(stubsOption),
          AnnotationFileType.COMMAND_LINE_STUB);
    }

    parsing = false;

    if (stubDebug) {
      System.out.printf("exited parseStubFiles() for %s%n", factory.getClass().getSimpleName());
    }
  }

  /**
   * Parse one .astub file.
   *
   * @param checkerClass the location of the resource in the checker.jar file
   * @param stubFileName the basename of the .astub file
   */
  private void parseOneStubFile(Class<?> checkerClass, String stubFileName) {
    BaseTypeChecker checker = factory.getChecker();
    ProcessingEnvironment processingEnv = factory.getProcessingEnv();
    try (InputStream jdkVersionStubIn = checkerClass.getResourceAsStream(stubFileName)) {
      if (jdkVersionStubIn != null) {
        if (stubDebug) {
          AnnotationFileParser.stubDebugStatic(
              processingEnv,
              "parseOneStubFile(%s, %s): jdkVersionStubIn = %s%n",
              checkerClass.getSimpleName(),
              stubFileName,
              jdkVersionStubIn);
        }
        AnnotationFileParser.parseStubFile(
            checkerClass.getResource(stubFileName).toString(),
            jdkVersionStubIn,
            factory,
            processingEnv,
            annotationFileAnnos,
            AnnotationFileType.BUILTIN_STUB);
      }
    } catch (IOException e) {
      checker.message(
          Diagnostic.Kind.NOTE,
          "Could not read annotation resource from " + checkerClass + ": " + stubFileName);
    }
  }

  /** Parses the ajava files passed through the -Aajava command-line option. */
  public void parseAjavaFiles() {
    parsing = true;
    try {
      // TODO: Error if this is called more than once?
      SourceChecker checker = factory.getChecker();
      List<String> ajavaFiles = checker.getStringsOption("ajava", File.pathSeparator);
      parseAnnotationFiles(ajavaFiles, AnnotationFileType.AJAVA);
    } finally {
      parsing = false;
    }
  }

  /**
   * Parses the ajava file at {@code ajavaPath} assuming {@code root} represents the compilation
   * unit of that file. Uses {@code root} to get information from javac on specific elements of
   * {@code ajavaPath}, enabling storage of more detailed annotation information than with just the
   * ajava file.
   *
   * @param ajavaPath path to an ajava file
   * @param root javac tree for the compilation unit stored in {@code ajavaFile}
   */
  public void parseAjavaFileWithTree(String ajavaPath, CompilationUnitTree root) {
    parsing = true;
    SourceChecker checker = factory.getChecker();
    ProcessingEnvironment processingEnv = factory.getProcessingEnv();
    try (InputStream in = new FileInputStream(ajavaPath)) {
      if (stubDebug) {
        AnnotationFileParser.stubDebugStatic(
            processingEnv,
            "parseAjavaFileWithTree(%s, %s): checker = %s, in = %s%n",
            ajavaPath,
            System.identityHashCode(root),
            checker.getClass().getSimpleName(),
            in);
      }
      AnnotationFileParser.parseAjavaFile(
          ajavaPath, in, root, factory, processingEnv, annotationFileAnnos);
    } catch (IOException e) {
      checker.message(Diagnostic.Kind.NOTE, "Could not read ajava file: " + ajavaPath);
    } finally {
      parsing = false;
    }
  }

  /**
   * Parses the files in {@code annotationFiles} of the given file type. This includes files listed
   * directly in {@code annotationFiles} and for each listed directory, also includes all files
   * located in that directory (recursively).
   *
   * @param annotationFiles list of files and directories to parse
   * @param fileType the file type of files to parse
   */
  @SuppressWarnings("builder:required.method.not.called" // `allFiles` may contain multiple
  // JarEntryAnnotationFileResource.  Each of those references a zip file entry resource, which
  // itself references a ZipFile resource -- the same ZipFile for multiple zip file entries.
  // Closing any one of the zip file entries will close the ZipFile, which invalidates the
  // other zipfile entries.  Therefore, this code does not close any of them.  This code may
  // leak resources.
  )
  private void parseAnnotationFiles(List<String> annotationFiles, AnnotationFileType fileType) {
    if (annotationFiles.isEmpty()) {
      return;
    }

    SourceChecker checker = factory.getChecker();
    ProcessingEnvironment processingEnv = factory.getProcessingEnv();
    if (stubDebug) {
      AnnotationFileParser.stubDebugStatic(
          processingEnv, "AFET.parseAnnotationFiles(%s, %s)", annotationFiles, fileType);
    }
    for (String path : annotationFiles) {
      // Special case when running in jtreg.
      String base = System.getProperty("test.src");
      String fullPath = (base == null) ? path : base + "/" + path;

      List<AnnotationFileResource> allFiles =
          AnnotationFileUtil.allAnnotationFiles(fullPath, fileType);
      if (allFiles != null) {
        for (AnnotationFileResource resource : allFiles) {
          BufferedInputStream annotationFileStream;
          try {
            annotationFileStream = new BufferedInputStream(resource.getInputStream());
          } catch (IOException e) {
            checker.message(
                Diagnostic.Kind.NOTE,
                "Could not read annotation resource: " + resource.getDescription());
            continue;
          }
          // Is it necessary to also skip files that consist only of Java comments?
          Boolean isWhitespaceOnly = SystemUtil.isWhitespaceOnly(annotationFileStream, 100);
          if (isWhitespaceOnly != null && (boolean) isWhitespaceOnly) {
            continue;
          }
          // We use parseStubFile here even for ajava files because at this stage
          // ajava files are parsed as stub files. The extra annotation data in an
          // ajava file is parsed when type-checking the ajava file's corresponding
          // Java file.
          AnnotationFileParser.parseStubFile(
              resource.getDescription(),
              annotationFileStream,
              factory,
              processingEnv,
              annotationFileAnnos,
              fileType == AnnotationFileType.AJAVA ? AnnotationFileType.AJAVA_AS_STUB : fileType);
        }
      } else {
        // We didn't find the files.
        // If the file has a prefix of "checker.jar/" then look for the file in the top
        // level directory of the jar that contains the checker.
        if (path.startsWith("checker.jar/")) {
          path = "/" + path.substring("checker.jar/".length());
        }
        try (InputStream in = checker.getClass().getResourceAsStream(path)) {
          if (in != null) {
            AnnotationFileParser.parseStubFile(
                path, in, factory, processingEnv, annotationFileAnnos, fileType);
          } else {
            // Didn't find the file.  Issue a warning.

            // When using a compound checker, the target file may be found by the
            // current checker's parent checkers. Also check this to avoid a false
            // warning. Currently, only the original checker will try to parse the
            // target file, the parent checkers are only used to reduce false
            // warnings.
            SourceChecker currentChecker = checker;
            boolean findByParentCheckers = false;
            while (currentChecker != null) {
              URL topLevelResource = currentChecker.getClass().getResource("/" + path);
              if (topLevelResource != null) {
                currentChecker.message(
                    Diagnostic.Kind.WARNING,
                    path
                        + " should be in the same directory as "
                        + currentChecker.getClass().getSimpleName()
                        + ".class, but is at the top level of a jar file: "
                        + topLevelResource);
                findByParentCheckers = true;
                break;
              } else {
                currentChecker = currentChecker.getParentChecker();
              }
            }
            // If there exists one parent checker that can find this file, don't report
            // a warning.
            if (!findByParentCheckers) {
              File parentPath = new File(path).getParentFile();
              String parentPathDescription =
                  (parentPath == null
                      ? "current directory"
                      : "directory " + parentPath.getAbsolutePath());
              String msg =
                  checker.getClass().getSimpleName()
                      + " did not find annotation file or directory "
                      + path
                      + " on classpath or within "
                      + parentPathDescription
                      + (fullPath.equals(path) ? "" : (" or at " + fullPath));
              StringJoiner sj = new StringJoiner(System.lineSeparator() + "  ");
              sj.add(msg);
              sj.add("Classpath:");
              for (URI uri : new ClassGraph().getClasspathURIs()) {
                sj.add(uri.toString());
              }
              checker.message(Diagnostic.Kind.WARNING, sj.toString());
            }
          }
        } catch (IOException e) {
          checker.message(Diagnostic.Kind.NOTE, "Could not read annotation resource: " + path);
        }
      }
    }
  }

  /**
   * Returns the annotated type for {@code e} containing only annotations explicitly written in an
   * annotation file. Returns {@code null} if {@code e} does not appear in an annotation file.
   *
   * @param e an Element whose type is returned
   * @return an AnnotatedTypeMirror for {@code e} containing only annotations explicitly written in
   *     the annotation file and in the element. Returns {@code null} if {@code element} does not
   *     appear in an annotation file.
   */
  public @Nullable AnnotatedTypeMirror getAnnotatedTypeMirror(Element e) {
    if (isParsing()) {
      return null;
    }
    maybeParseEnclosingJdkClass(e);
    AnnotatedTypeMirror type = annotationFileAnnos.atypes.get(e);
    return type == null ? null : type.deepCopy();
  }

  /**
   * Returns the set of declaration annotations for {@code e} containing only annotations explicitly
   * written in an annotation file or the empty set if {@code e} does not appear in an annotation
   * file.
   *
   * @param elt element for which annotations are returned
   * @return an AnnotatedTypeMirror for {@code e} containing only annotations explicitly written in
   *     the annotation file and in the element. {@code null} is returned if {@code element} does
   *     not appear in an annotation file.
   */
  public @Nullable AnnotationMirrorSet getDeclAnnotations(Element elt) {
    if (stubDebug) {
      if (isParsing()) {
        System.out.printf(
            "AFET.getDeclAnnotations(%s [%s]): isParsing() => true, returning emptySet.%n",
            elt, elt.getClass());
      } else {
        System.out.printf(
            "AFET.getDeclAnnotations(%s [%s]): isParsing() => false, proceeding.%n",
            elt, elt.getClass());
      }
    }

    if (isParsing()) {
      return AnnotationMirrorSet.emptySet();
    }

    maybeParseEnclosingJdkClass(elt);
    String eltName = ElementUtils.getQualifiedName(elt);
    if (annotationFileAnnos.declAnnos.containsKey(eltName)) {
      return annotationFileAnnos.declAnnos.get(eltName);
    } else {
      // Handle annotations on record declarations.
      boolean canTransferAnnotationsToSameName;
      Element enclosingType; // Do nothing unless this element is a record.
      switch (elt.getKind()) {
        case METHOD:
          // Annotations transfer to zero-arg accessor methods of same name:
          canTransferAnnotationsToSameName = ((ExecutableElement) elt).getParameters().isEmpty();
          enclosingType = elt.getEnclosingElement();
          break;
        case FIELD:
          // Annotations transfer to fields of same name:
          canTransferAnnotationsToSameName = true;
          enclosingType = elt.getEnclosingElement();
          break;
        case PARAMETER:
          // Annotations transfer to compact canonical constructor parameter of same name:
          canTransferAnnotationsToSameName =
              ElementUtils.isCompactCanonicalRecordConstructor(elt.getEnclosingElement())
                  && elt.getEnclosingElement().getKind() == ElementKind.CONSTRUCTOR;
          enclosingType = elt.getEnclosingElement().getEnclosingElement();
          break;
        default:
          canTransferAnnotationsToSameName = false;
          enclosingType = null;
          break;
      }

      if (canTransferAnnotationsToSameName && enclosingType.getKind().toString().equals("RECORD")) {
        AnnotationFileParser.RecordStub recordStub =
            annotationFileAnnos.records.get(enclosingType.getSimpleName().toString());
        if (recordStub != null
            && recordStub.componentsByName.containsKey(elt.getSimpleName().toString())) {
          RecordComponentStub recordComponentStub =
              recordStub.componentsByName.get(elt.getSimpleName().toString());
          return recordComponentStub.getAnnotationsForTarget(elt.getKind());
        }
      }
    }
    return AnnotationMirrorSet.emptySet();
  }

  /**
   * Adds annotations from stub files for the corresponding record components (if the given
   * constructor/method is the canonical constructor or a record accessor). Such transfer is
   * automatically done by javac usually, but not from stubs.
   *
   * @param types a Types instance used for checking type equivalence
   * @param elt a member. This method does nothing if it's not a method or constructor.
   * @param memberType the type corresponding to the element elt; side-effected by this method
   */
  public void injectRecordComponentType(
      Types types, Element elt, AnnotatedExecutableType memberType) {
    if (isParsing()) {
      throw new BugInCF("parsing while calling injectRecordComponentType");
    }

    if (elt.getKind() == ElementKind.METHOD) {
      if (((ExecutableElement) elt).getParameters().isEmpty()) {
        String recordName = ElementUtils.getQualifiedName(elt.getEnclosingElement());
        AnnotationFileParser.RecordStub recordComponentType =
            annotationFileAnnos.records.get(recordName);
        if (recordComponentType != null) {
          // If the record component has an annotation in the stub, the component
          // annotation replaces any from the same hierarchy on the accessor method,
          // unless there is an accessor in the stubs file (which may or may not have an
          // annotation in the same hierarchy; the user may want to specify the annotation
          // or deliberately not annotate the accessor).
          // We thus only replace the method annotation with the component annotation
          // if there is no accessor in the stubs file:
          RecordComponentStub recordComponentStub =
              recordComponentType.componentsByName.get(elt.getSimpleName().toString());
          if (recordComponentStub != null && !recordComponentStub.hasAccessorInStubs()) {
            memberType
                .getReturnType()
                .replaceAnnotations(recordComponentStub.type.getPrimaryAnnotations());
          }
        }
      }
    } else if (elt.getKind() == ElementKind.CONSTRUCTOR) {
      if (AnnotationFileUtil.isCanonicalConstructor((ExecutableElement) elt, types)) {
        TypeElement enclosing = (TypeElement) elt.getEnclosingElement();
        AnnotationFileParser.RecordStub recordComponentType =
            annotationFileAnnos.records.get(enclosing.getQualifiedName().toString());
        if (recordComponentType != null) {
          List<AnnotatedTypeMirror> componentsInCanonicalConstructor =
              recordComponentType.getComponentsInCanonicalConstructor();
          if (componentsInCanonicalConstructor != null) {
            for (int i = 0; i < componentsInCanonicalConstructor.size(); i++) {
              memberType
                  .getParameterTypes()
                  .get(i)
                  .replaceAnnotations(
                      componentsInCanonicalConstructor.get(i).getPrimaryAnnotations());
            }
          }
        }
      }
    }
  }

  /**
   * Returns the method type of the most specific fake override for the given element, when used as
   * a member of the given type.
   *
   * @param elt element for which annotations are returned
   * @param receiverType the type of the class that contains member (or a subtype of it)
   * @return the most specific AnnotatedTypeMirror for {@code elt} that is a fake override, or null
   *     if there are no fake overrides
   */
  public @Nullable AnnotatedExecutableType getFakeOverride(
      Element elt, AnnotatedTypeMirror receiverType) {
    if (isParsing()) {
      throw new BugInCF("parsing while calling getFakeOverride");
    }

    if (elt.getKind() != ElementKind.METHOD) {
      return null;
    }

    ExecutableElement method = (ExecutableElement) elt;

    // This is a list of pairs of (where defined, method type) for fake overrides.  The second
    // element of each pair is currently always an AnnotatedExecutableType.
    List<IPair<TypeMirror, AnnotatedTypeMirror>> candidates =
        annotationFileAnnos.fakeOverrides.get(method);

    if (candidates == null || candidates.isEmpty()) {
      return null;
    }

    TypeMirror receiverTypeMirror = receiverType.getUnderlyingType();

    // A list of fake receiver types.
    List<TypeMirror> applicableClasses = new ArrayList<>();
    List<TypeMirror> applicableInterfaces = new ArrayList<>();
    for (IPair<TypeMirror, AnnotatedTypeMirror> candidatePair : candidates) {
      TypeMirror fakeLocation = candidatePair.first;
      AnnotatedExecutableType candidate = (AnnotatedExecutableType) candidatePair.second;
      if (factory.types.isSameType(receiverTypeMirror, fakeLocation)) {
        return candidate;
      } else if (factory.types.isSubtype(receiverTypeMirror, fakeLocation)) {
        TypeElement fakeElement = TypesUtils.getTypeElement(fakeLocation);
        switch (fakeElement.getKind()) {
          case CLASS:
          case ENUM:
            applicableClasses.add(fakeLocation);
            break;
          case INTERFACE:
          case ANNOTATION_TYPE:
            applicableInterfaces.add(fakeLocation);
            break;
          default:
            throw new BugInCF(
                "What type? %s %s %s", fakeElement.getKind(), fakeElement.getClass(), fakeElement);
        }
      }
    }

    if (applicableClasses.isEmpty() && applicableInterfaces.isEmpty()) {
      return null;
    }
    TypeMirror fakeReceiverType =
        TypesUtils.mostSpecific(
            !applicableClasses.isEmpty() ? applicableClasses : applicableInterfaces,
            factory.getProcessingEnv());
    if (fakeReceiverType == null) {
      StringJoiner message = new StringJoiner(System.lineSeparator());
      message.add(
          String.format(
              "No most specific fake override found for %s with receiver %s."
                  + " These fake overrides are applicable:",
              elt, receiverTypeMirror));
      for (TypeMirror candidate : applicableClasses) {
        message.add("  class candidate: " + candidate);
      }
      for (TypeMirror candidate : applicableInterfaces) {
        message.add("  interface candidate: " + candidate);
      }
      throw new BugInCF(message.toString());
    }

    for (IPair<TypeMirror, AnnotatedTypeMirror> candidatePair : candidates) {
      TypeMirror candidateReceiverType = candidatePair.first;
      if (factory.types.isSameType(fakeReceiverType, candidateReceiverType)) {
        return (AnnotatedExecutableType) candidatePair.second;
      }
    }

    throw new BugInCF(
        "No match for %s in %s %s %s",
        fakeReceiverType, candidates, applicableClasses, applicableInterfaces);
  }

  //
  // End of public methods, private helper methods follow
  //

  /**
   * Parses the outermost enclosing class of {@code e} if it is in the annotated JDK and it has not
   * already been parsed.
   *
   * @param e element whose outermost enclosing class might be parsed, if it is in the JDK and has
   *     not already been parsed
   */
  private void maybeParseEnclosingJdkClass(Element e) {
    if (stubDebug) {
      System.out.printf(
          "maybeParseEnclosingJdkClass(%s encloses %s), shouldParseJdk=%s%n",
          getOutermostEnclosingClass(e), e, shouldParseJdk);
    }
    if (!shouldParseJdk) {
      return;
    }
    String className = getOutermostEnclosingClass(e);
    // `className` can be null if `e` is a package or module element.
    if (className == null || className.isEmpty()) {
      return;
    }
    if (remainingJdkStubFiles.containsKey(className)) {
      parseJdkStubFile(remainingJdkStubFiles.remove(className));
    } else if (remainingJdkStubFilesJar.containsKey(className)) {
      parseJdkJarEntry(remainingJdkStubFilesJar.remove(className));
    } else {
      if (stubDebug) {
        System.out.printf("  not in remaining JDK stub files: %s%n", className);
      }
    }
  }

  /**
   * Returns the fully qualified name of the outermost enclosing class of {@code e} or {@code null}
   * if no such class exists for {@code e}, such as when {@code e} is a package or module element.
   *
   * @param e an element whose outermost enclosing class to return
   * @return the canonical name of the outermost enclosing class of {@code e} or {@code null} if no
   *     class encloses {@code e}
   */
  private @Nullable @CanonicalNameOrEmpty String getOutermostEnclosingClass(Element e) {
    TypeElement enclosingClass = ElementUtils.enclosingTypeElement(e);
    if (enclosingClass == null) {
      return null;
    }
    while (true) {
      Element element = enclosingClass.getEnclosingElement();
      if (element == null || element.getKind() == ElementKind.PACKAGE) {
        break;
      }
      TypeElement t = ElementUtils.enclosingTypeElement(element);
      if (t == null) {
        break;
      }
      enclosingClass = t;
    }
    @SuppressWarnings("signature:assignment" // https://tinyurl.com/cfissue/658:
    // Name.toString should be @PolySignature
    )
    @CanonicalNameOrEmpty String result = enclosingClass.getQualifiedName().toString();
    return result;
  }

  /**
   * Parses the stub file in {@code path}.
   *
   * @param path path to file to parse
   */
  private void parseJdkStubFile(Path path) {
    parsing = true;
    try (FileInputStream jdkStub = new FileInputStream(path.toFile())) {
      AnnotationFileParser.parseJdkFileAsStub(
          path.toFile().getName(),
          jdkStub,
          factory,
          factory.getProcessingEnv(),
          annotationFileAnnos);
    } catch (IOException e) {
      throw new BugInCF("cannot open the jdk stub file " + path, e);
    } finally {
      parsing = false;
    }
  }

  /**
   * Parses the stub file in the given jar entry.
   *
   * @param jarEntryName name of the jar entry to parse
   */
  private void parseJdkJarEntry(String jarEntryName) {
    if (stubDebug) {
      System.out.printf("entered parseJdkJarEntry(%s)%n", jarEntryName);
    }

    JarURLConnection connection = getJarURLConnectionToJdk();
    parsing = true;
    try (JarFile jarFile = connection.getJarFile()) {
      try (InputStream jdkStub = jarFile.getInputStream(jarFile.getJarEntry(jarEntryName))) {
        AnnotationFileParser.parseJdkFileAsStub(
            jarEntryName, jdkStub, factory, factory.getProcessingEnv(), annotationFileAnnos);
      } catch (IOException e) {
        throw new BugInCF("cannot open the jdk stub file " + jarEntryName, e);
      }
    } catch (IOException e) {
      throw new BugInCF("cannot open the Jar file " + connection.getEntryName(), e);
    } catch (BugInCF e) {
      throw new BugInCF("Exception while parsing " + jarEntryName + ": " + e.getMessage(), e);
    } finally {
      parsing = false;
    }

    if (stubDebug) {
      System.out.printf("exited parseJdkJarEntry(%s)%n", jarEntryName);
    }
  }

  /**
   * Returns a JarURLConnection to "/annotated-jdk".
   *
   * @return a JarURLConnection to "/annotated-jdk"
   */
  private JarURLConnection getJarURLConnectionToJdk() {
    URL resourceURL = factory.getClass().getResource("/annotated-jdk");
    JarURLConnection connection;
    try {
      connection = (JarURLConnection) resourceURL.openConnection();

      // disable caching / connection sharing of the low level URLConnection to the Jarfile
      connection.setDefaultUseCaches(false);
      connection.setUseCaches(false);

      connection.connect();
    } catch (IOException e) {
      throw new BugInCF("cannot open a connection to the Jar file " + resourceURL.getFile(), e);
    }
    return connection;
  }

  /**
   * Walk through the JDK directory and create a mapping, {@link #remainingJdkStubFiles}, from file
   * name to the class contained within it. Also, parses all {@code package-info.java} files.
   */
  private void prepJdkStubs() {
    if (!shouldParseJdk) {
      return;
    }
    URL resourceURL = factory.getClass().getResource("/annotated-jdk");
    if (resourceURL == null) {
      if (permitMissingJdk
          // temporary, for backward compatibility
          || factory.getChecker().hasOption("nocheckjdk")) {
        return;
      }
      throw new BugInCF("JDK not found");
    } else if (resourceURL.getProtocol().contentEquals("jar")) {
      prepJdkFromJar(resourceURL);
    } else if (resourceURL.getProtocol().contentEquals("file")) {
      prepJdkFromFile(resourceURL);
    } else {
      if (permitMissingJdk
          // temporary, for backward compatibility
          || factory.getChecker().hasOption("nocheckjdk")) {
        return;
      }
      throw new BugInCF("JDK not found");
    }
  }

  /**
   * Walk through the JDK directory and create a mapping, {@link #remainingJdkStubFiles}, from file
   * name to the class contained within it. Also, parses all {@code package-info.java} files.
   *
   * @param jdkDirectory the URL pointing to the JDK directory
   */
  private void prepJdkFromFile(URL jdkDirectory) {
    Path root;
    try {
      root = Paths.get(jdkDirectory.toURI());
    } catch (URISyntaxException e) {
      throw new BugInCF("Cannot parse URL: " + jdkDirectory.toString(), e);
    }

    try (Stream<Path> walk = Files.walk(root)) {
      List<Path> paths =
          walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
              .collect(Collectors.toList());
      paths.sort(Path::compareTo);
      for (Path path : paths) {
        if (path.getFileName().toString().equals("package-info.java")) {
          parseJdkStubFile(path);
          continue;
        }
        if (path.getFileName().toString().equals("module-info.java")) {
          // JavaParser can't parse module-info files, so skip them.
          continue;
        }
        if (parseAllJdkFiles) {
          parseJdkStubFile(path);
          continue;
        }
        Path relativePath = root.relativize(path);
        // The number 4 is to strip off "/src/<module>/share/classes".
        Path savepath = relativePath.subpath(4, relativePath.getNameCount());
        String savepathString = savepath.toString();
        // The number 5 is to remove trailing ".java".
        String savepathWithoutExtension = savepathString.substring(0, savepathString.length() - 5);
        String fqName = savepathWithoutExtension.replace(File.separatorChar, '.');
        remainingJdkStubFiles.put(fqName, path);
      }
      if (stubDebug) {
        System.out.printf(
            "Contents of remainingJdkStubFiles for %s from %s:%n",
            factory.getClass().getSimpleName(), jdkDirectory);
        printSortedIndented(remainingJdkStubFiles.keySet());
        System.out.printf(
            "End of remainingJdkStubFiles for %s from %s.%n",
            factory.getClass().getSimpleName(), jdkDirectory);
      }
    } catch (IOException e) {
      throw new BugInCF("prepJdkFromFile(" + jdkDirectory + ")", e);
    }
  }

  /**
   * Walk through the JDK directory and create a mapping, {@link #remainingJdkStubFilesJar}, from
   * file name to the class contained within it. Also, parses all {@code package-info.java} files.
   *
   * @param jdkJarfile the URL pointing to the JDK jarfile
   */
  private void prepJdkFromJar(@SuppressWarnings("UnusedVariable") URL jdkJarfile) {
    JarURLConnection connection = getJarURLConnectionToJdk();

    try (JarFile jarFile = connection.getJarFile()) {
      ArrayList<JarEntry> entries = CollectionsPlume.makeArrayList(jarFile.entries());
      entries.sort(Comparator.comparing(Object::toString));
      for (JarEntry jarEntry : entries) {
        // filter out directories and non-Java files
        if (jarEntry.isDirectory()) {
          continue;
        }
        String jarEntryName = jarEntry.getName();
        if (!(jarEntryName.startsWith("annotated-jdk") && jarEntryName.endsWith(".java"))
            // JavaParser can't parse module-info files, so skip them.
            || jarEntryName.endsWith("module-info.java")) {
          continue;
        }
        if (parseAllJdkFiles || jarEntryName.endsWith("package-info.java")) {
          parseJdkJarEntry(jarEntryName);
          continue;
        }
        int index = jarEntryName.indexOf("/share/classes/") + "/share/classes/".length();
        // "-5" is to remove ".java" from end of file name
        String fqClassName =
            jarEntryName.substring(index, jarEntryName.length() - 5).replace('/', '.');
        remainingJdkStubFilesJar.put(fqClassName, jarEntryName);
      }
      if (stubDebug) {
        String factoryClass = factory.getClass().getSimpleName().toString();
        String jarFileURL = connection.getJarFileURL().toString();
        System.out.printf(
            "Contents of remainingJdkStubFilesJar for %s from %s:%n", factoryClass, jarFileURL);
        printSortedIndented(remainingJdkStubFilesJar.keySet());
        System.out.printf(
            "End of remainingJdkStubFilesJar for %s from %s.%n", factoryClass, jarFileURL);

        System.out.printf("Contents of %s:%n", jarFileURL);
        assert jarFileURL.startsWith("file:");
        ProcessBuilder pb =
            new ProcessBuilder(
                "/bin/sh", "-c", "jar tf '" + jarFileURL.substring(5) + "' | LC_ALL=C sort");
        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);
        Process p = pb.start();
        try {
          p.waitFor();
        } catch (InterruptedException e) {
          // do nothing
        }
        System.out.flush();
        SystemPlume.sleep(1);
        System.out.printf("End of %s.%n", jarFileURL);
      }
    } catch (IOException e) {
      throw new BugInCF("Cannot open the jar file " + connection.getJarFileURL(), e);
    }
  }

  /**
   * Print the strings, in order, each on its own line, indented by two spaces.
   *
   * @param strings a collection of strings
   */
  private void printSortedIndented(Collection<String> strings) {
    List<String> stringList = new ArrayList<>(strings);
    stringList.sort(String::compareTo);
    for (String s : stringList) {
      System.out.printf("  %s%n", s);
    }
  }
}
