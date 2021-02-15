package org.checkerframework.framework.stub;

import com.sun.source.tree.CompilationUnitTree;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.checker.signature.qual.CanonicalNameOrEmpty;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.stub.AnnotationFileParser.AnnotationFileAnnotations;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.SystemUtil;

/**
 * Holds information about types parsed from annotation files (stub files or ajava files). When
 * using an ajava file, only holds information on public elements as with stub files.
 */
public class AnnotationFileElementTypes {
    /** Annotations from annotation files (but not from annotated JDK files). */
    private final AnnotationFileAnnotations annotationFileAnnos;

    /**
     * Whether or not a file is currently being parsed. (If one is being parsed, don't try to parse
     * another.)
     */
    private boolean parsing;

    /** AnnotatedTypeFactory. */
    private final AnnotatedTypeFactory factory;

    /**
     * Mapping from fully-qualified class name to corresponding JDK stub file from the file system.
     */
    private final Map<String, Path> jdkStubFiles = new HashMap<>();

    /** Mapping from fully-qualified class name to corresponding JDK stub files from checker.jar. */
    private final Map<String, String> jdkStubFilesJar = new HashMap<>();

    /** Which version number of the annotated JDK should be used? */
    private final String annotatedJdkVersion;

    /** Should the JDK be parsed? */
    private final boolean shouldParseJdk;

    /** Parse all JDK files at startup rather than as needed. */
    private final boolean parseAllJdkFiles;

    /**
     * Creates an empty annotation source.
     *
     * @param factory AnnotatedTypeFactory
     */
    public AnnotationFileElementTypes(AnnotatedTypeFactory factory) {
        this.factory = factory;
        this.annotationFileAnnos = new AnnotationFileAnnotations();
        this.parsing = false;
        String release = SystemUtil.getReleaseValue(factory.getProcessingEnv());
        this.annotatedJdkVersion =
                release != null ? release : String.valueOf(SystemUtil.getJreVersion());

        this.shouldParseJdk = !factory.getChecker().hasOption("ignorejdkastub");
        this.parseAllJdkFiles = factory.getChecker().hasOption("parseAllJdk");
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
     *   <li>jdk.astub in the same directory as the checker, if it exists and ignorejdkastub option
     *       is not supplied
     *   <li>If parsing annotated JDK as stub files, all package-info.java files under the jdk/
     *       directory
     *   <li>Stub files listed in @StubFiles annotation on the checker; must be in same directory as
     *       the checker
     *   <li>Stub files provided via stubs system property
     *   <li>Stub files provided via stubs environment variable
     *   <li>Stub files provided via stubs compiler option
     * </ol>
     *
     * <p>If a type is annotated with a qualifier from the same hierarchy in more than one stub
     * file, the qualifier in the last stub file is applied.
     *
     * <p>If using JDK 11, then the JDK stub files are only parsed if a type or declaration
     * annotation is requested from a class in that file.
     */
    public void parseStubFiles() {
        parsing = true;
        SourceChecker checker = factory.getChecker();
        ProcessingEnvironment processingEnv = factory.getProcessingEnv();
        // 1. jdk.astub
        // Only look in .jar files, and parse it right away.
        if (!checker.hasOption("ignorejdkastub")) {
            InputStream jdkStubIn = checker.getClass().getResourceAsStream("jdk.astub");
            if (jdkStubIn != null) {
                AnnotationFileParser.parseStubFile(
                        checker.getClass().getResource("jdk.astub").toString(),
                        jdkStubIn,
                        factory,
                        processingEnv,
                        annotationFileAnnos);
            }
            String jdkVersionStub = "jdk" + annotatedJdkVersion + ".astub";
            InputStream jdkVersionStubIn = checker.getClass().getResourceAsStream(jdkVersionStub);
            if (jdkVersionStubIn != null) {
                AnnotationFileParser.parseStubFile(
                        checker.getClass().getResource(jdkVersionStub).toString(),
                        jdkVersionStubIn,
                        factory,
                        processingEnv,
                        annotationFileAnnos);
            }

            // 2. Annotated JDK
            prepJdkStubs();
            // prepping the Jdk will parse all package-info.java files.  This sets parsing to false,
            // so re-set it to true.
            parsing = true;
        }

        // Stub files specified via stubs compiler option, stubs system property,
        // stubs env. variable, or @StubFiles
        List<String> allAnnotationFiles = new ArrayList<>();

        // 3. Stub files listed in @StubFiles annotation on the checker
        StubFiles stubFilesAnnotation = checker.getClass().getAnnotation(StubFiles.class);
        if (stubFilesAnnotation != null) {
            Collections.addAll(allAnnotationFiles, stubFilesAnnotation.value());
        }

        // 4. Stub files provided via stubs system property
        String stubsProperty = System.getProperty("stubs");
        if (stubsProperty != null) {
            Collections.addAll(allAnnotationFiles, stubsProperty.split(File.pathSeparator));
        }

        // 5. Stub files provided via stubs environment variable
        String stubEnvVar = System.getenv("stubs");
        if (stubEnvVar != null) {
            Collections.addAll(allAnnotationFiles, stubEnvVar.split(File.pathSeparator));
        }

        // 6. Stub files provided via stubs command-line option
        String stubsOption = checker.getOption("stubs");
        if (stubsOption != null) {
            Collections.addAll(allAnnotationFiles, stubsOption.split(File.pathSeparator));
        }

        parseAnnotationFiles(allAnnotationFiles, AnnotationFileUtil.AnnotationFileType.STUB);
        parsing = false;
    }

    /** Parses the ajava files passed through the -Aajava option. */
    public void parseAjavaFiles() {
        parsing = true;
        // TODO: Error if this is called more than once?
        SourceChecker checker = factory.getChecker();
        List<String> ajavaFiles = new ArrayList<>();
        String ajavaOption = checker.getOption("ajava");
        if (ajavaOption != null) {
            Collections.addAll(ajavaFiles, ajavaOption.split(File.pathSeparator));
        }

        parseAnnotationFiles(ajavaFiles, AnnotationFileUtil.AnnotationFileType.AJAVA);
        parsing = false;
    }

    /**
     * Parses the ajava file at {@code ajavaPath} assuming {@code root} represents the compilation
     * unit of that file. Uses {@code root} to get information from javac on specific elements of
     * {@code ajavaPath}, enabling storage of more detailed annotation information than with just
     * the ajava file.
     *
     * @param ajavaPath path to an ajava file
     * @param root javac tree for the compilation unit stored in {@code ajavaFile}
     */
    public void parseAjavaFileWithTree(String ajavaPath, CompilationUnitTree root) {
        parsing = true;
        SourceChecker checker = factory.getChecker();
        ProcessingEnvironment processingEnv = factory.getProcessingEnv();
        InputStream in;
        try {
            in = new FileInputStream(ajavaPath);
            AnnotationFileParser.parseAjavaFile(
                    ajavaPath, in, root, factory, processingEnv, annotationFileAnnos);
        } catch (IOException e) {
            checker.message(Kind.NOTE, "Could not read ajava file: " + ajavaPath);
        }

        parsing = false;
    }

    /**
     * Parses the files in {@code annotationFiles} of the given file type. This includes files
     * listed directly in {@code annotationFiles} and for each listed directory, also includes all
     * files located in that directory (recursively).
     *
     * @param annotationFiles list of files and directories to parse
     * @param fileType the file type of files to parse
     */
    private void parseAnnotationFiles(
            List<String> annotationFiles, AnnotationFileUtil.AnnotationFileType fileType) {
        SourceChecker checker = factory.getChecker();
        ProcessingEnvironment processingEnv = factory.getProcessingEnv();
        for (String path : annotationFiles) {
            // Special case when running in jtreg.
            String base = System.getProperty("test.src");
            String fullPath = (base == null) ? path : base + "/" + path;

            List<AnnotationFileResource> allFiles =
                    AnnotationFileUtil.allAnnotationFiles(fullPath, fileType);
            if (allFiles != null) {
                for (AnnotationFileResource resource : allFiles) {
                    InputStream annotationFileStream;
                    try {
                        annotationFileStream = resource.getInputStream();
                    } catch (IOException e) {
                        checker.message(
                                Kind.NOTE,
                                "Could not read annotation resource: " + resource.getDescription());
                        continue;
                    }
                    AnnotationFileParser.parseStubFile(
                            resource.getDescription(),
                            annotationFileStream,
                            factory,
                            processingEnv,
                            annotationFileAnnos);
                }
            } else {
                // We didn't find the files.
                // If the file has a prefix of "checker.jar/" then look for the file in the top
                // level directory of the jar that contains the checker.
                if (path.startsWith("checker.jar/")) {
                    path = path.substring("checker.jar/".length());
                }
                InputStream in = checker.getClass().getResourceAsStream(path);
                if (in != null) {
                    AnnotationFileParser.parseStubFile(
                            path, in, factory, processingEnv, annotationFileAnnos);
                } else {
                    // Didn't find the file.  Issue a warning.

                    // When using a compound checker, the target file may be found by the
                    // current checker's parent checkers. Also check this to avoid a false
                    // warning. Currently, only the original checker will try to parse the target
                    // file, the parent checkers are only used to reduce false warnings.
                    SourceChecker currentChecker = checker;
                    boolean findByParentCheckers = false;
                    while (currentChecker != null) {
                        URL topLevelResource = currentChecker.getClass().getResource("/" + path);
                        if (topLevelResource != null) {
                            currentChecker.message(
                                    Kind.WARNING,
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
                    // If there exists one parent checker that can find this file, don't
                    // report an warning.
                    if (!findByParentCheckers) {
                        File parentPath = new File(path).getParentFile();
                        String parentPathDescription =
                                (parentPath == null
                                        ? "current directory"
                                        : "directory "
                                                + new File(path).getParentFile().getAbsolutePath());
                        checker.message(
                                Kind.WARNING,
                                "Did not find annotation file "
                                        + path
                                        + " on classpath or within "
                                        + parentPathDescription
                                        + (fullPath.equals(path) ? "" : (" or at " + fullPath)));
                    }
                }
            }
        }
    }

    /**
     * Returns the annotated type for {@code e} containing only annotations explicitly written in an
     * annotation file or {@code null} if {@code e} does not appear in an annotation file.
     *
     * @param e an Element whose type is returned
     * @return an AnnotatedTypeMirror for {@code e} containing only annotations explicitly written
     *     in the annotation file and in the element. {@code null} is returned if {@code element}
     *     does not appear in an annotation file.
     */
    public AnnotatedTypeMirror getAnnotatedTypeMirror(Element e) {
        if (parsing) {
            return null;
        }
        parseEnclosingClass(e);
        AnnotatedTypeMirror type = annotationFileAnnos.atypes.get(e);
        return type == null ? null : type.deepCopy();
    }

    /**
     * Returns the set of declaration annotations for {@code e} containing only annotations
     * explicitly written in an annotation file or the empty set if {@code e} does not appear in an
     * annotation file.
     *
     * @param elt element for which annotations are returned
     * @return an AnnotatedTypeMirror for {@code e} containing only annotations explicitly written
     *     in the annotation file and in the element. {@code null} is returned if {@code element}
     *     does not appear in an annotation file.
     */
    public Set<AnnotationMirror> getDeclAnnotation(Element elt) {
        if (parsing) {
            return Collections.emptySet();
        }

        parseEnclosingClass(elt);
        String eltName = ElementUtils.getQualifiedName(elt);
        if (annotationFileAnnos.declAnnos.containsKey(eltName)) {
            return annotationFileAnnos.declAnnos.get(eltName);
        }
        return Collections.emptySet();
    }

    ///
    /// End of public methods, private helper methods follow
    ///

    /**
     * Parses the outermost enclosing class of {@code e} if there exists an annotation file for it
     * and it has not already been parsed.
     *
     * @param e element whose outermost enclosing class will be parsed
     */
    private void parseEnclosingClass(Element e) {
        if (!shouldParseJdk) {
            return;
        }
        String className = getOutermostEnclosingClass(e);
        if (className == null || className.isEmpty()) {
            return;
        }
        if (jdkStubFiles.containsKey(className)) {
            parseStubFile(jdkStubFiles.get(className));
            jdkStubFiles.remove(className);
        } else if (jdkStubFilesJar.containsKey(className)) {
            parseJarEntry(jdkStubFilesJar.get(className));
            jdkStubFilesJar.remove(className);
        }
    }

    /**
     * Returns the fully qualified name of the outermost enclosing class of {@code e} or {@code
     * null} if no such class exists for {@code e}.
     *
     * @param e an element whose outermost enclosing class to return
     * @return the canonical name of the outermost enclosing class of {@code e} or {@code null} if
     *     no class encloses {@code e}
     */
    private @CanonicalNameOrEmpty String getOutermostEnclosingClass(Element e) {
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
        @SuppressWarnings(
                "signature:assignment.type.incompatible" // https://tinyurl.com/cfissue/658:
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
    private void parseStubFile(Path path) {
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
    private void parseJarEntry(String jarEntryName) {
        JarURLConnection connection = getJarURLConnectionToJdk();
        parsing = true;
        try (JarFile jarFile = connection.getJarFile()) {
            InputStream jdkStub;
            try {
                jdkStub = jarFile.getInputStream(jarFile.getJarEntry(jarEntryName));
            } catch (IOException e) {
                throw new BugInCF("cannot open the jdk stub file " + jarEntryName, e);
            }
            AnnotationFileParser.parseJdkFileAsStub(
                    jarEntryName,
                    jdkStub,
                    factory,
                    factory.getProcessingEnv(),
                    annotationFileAnnos);
        } catch (IOException e) {
            throw new BugInCF("cannot open the Jar file " + connection.getEntryName(), e);
        } catch (BugInCF e) {
            throw new BugInCF("Exception while parsing " + jarEntryName + ": " + e.getMessage(), e);
        } finally {
            parsing = false;
        }
    }

    /**
     * Returns a JarURLConnection to "/jdk*".
     *
     * @return a JarURLConnection to "/jdk*"
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
            throw new BugInCF(
                    "cannot open a connection to the Jar file " + resourceURL.getFile(), e);
        }
        return connection;
    }

    /**
     * Walk through the jdk directory and create a mapping, {@link #jdkStubFiles}, from file name to
     * the class contained with in it. Also, parses all package-info.java files.
     */
    private void prepJdkStubs() {
        if (!shouldParseJdk) {
            return;
        }
        URL resourceURL = factory.getClass().getResource("/annotated-jdk");
        if (resourceURL == null) {
            if (factory.getChecker().hasOption("permitMissingJdk")
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
            if (factory.getChecker().hasOption("permitMissingJdk")
                    // temporary, for backward compatibility
                    || factory.getChecker().hasOption("nocheckjdk")) {
                return;
            }
            throw new BugInCF("JDK not found");
        }
    }

    /**
     * Walk through the jdk directory and create a mapping, {@link #jdkStubFiles}, from file name to
     * the class contained with in it. Also, parses all package-info.java files.
     *
     * @param resourceURL the URL pointing to the JDK directory
     */
    private void prepJdkFromFile(URL resourceURL) {
        Path root;
        try {
            root = Paths.get(resourceURL.toURI());
        } catch (URISyntaxException e) {
            throw new BugInCF("Can parse URL: " + resourceURL.toString(), e);
        }

        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> paths =
                    walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                            .collect(Collectors.toList());
            for (Path path : paths) {
                if (path.getFileName().toString().equals("package-info.java")) {
                    parseStubFile(path);
                    continue;
                }
                if (path.getFileName().toString().equals("module-info.java")) {
                    // JavaParser can't parse module-info files, so skip them.
                    continue;
                }
                if (parseAllJdkFiles) {
                    parseStubFile(path);
                    continue;
                }
                Path relativePath = root.relativize(path);
                // 4: /src/<module>/share/classes
                Path savepath = relativePath.subpath(4, relativePath.getNameCount());
                String s =
                        savepath.toString().replace(".java", "").replace(File.separatorChar, '.');
                jdkStubFiles.put(s, path);
            }
        } catch (IOException e) {
            throw new BugInCF("prepJdkFromFile(" + resourceURL + ")", e);
        }
    }

    /**
     * Walk through the jdk directory and create a mapping, {@link #jdkStubFilesJar}, from file name
     * to the class contained with in it. Also, parses all package-info.java files.
     *
     * @param resourceURL the URL pointing to the JDK directory
     */
    private void prepJdkFromJar(URL resourceURL) {
        JarURLConnection connection = getJarURLConnectionToJdk();

        try (JarFile jarFile = connection.getJarFile()) {
            for (JarEntry jarEntry : jarFile.stream().collect(Collectors.toList())) {
                // filter out directories and non-class files
                if (!jarEntry.isDirectory()
                        && jarEntry.getName().endsWith(".java")
                        && jarEntry.getName().startsWith("annotated-jdk")
                        // JavaParser can't parse module-info files, so skip them.
                        && !jarEntry.getName().contains("module-info")) {
                    String jarEntryName = jarEntry.getName();
                    if (parseAllJdkFiles) {
                        parseJarEntry(jarEntryName);
                        continue;
                    }
                    int index = jarEntry.getName().indexOf("/share/classes/");
                    String shortName =
                            jarEntryName
                                    .substring(index + "/share/classes/".length())
                                    .replace(".java", "")
                                    .replace('/', '.');
                    jdkStubFilesJar.put(shortName, jarEntryName);
                    if (jarEntryName.endsWith("package-info.java")) {
                        parseJarEntry(jarEntryName);
                    }
                }
            }
        } catch (IOException e) {
            throw new BugInCF("cannot open the Jar file " + resourceURL.getFile(), e);
        }
    }
}
