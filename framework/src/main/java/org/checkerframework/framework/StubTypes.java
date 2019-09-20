package org.checkerframework.framework;

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
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.stub.StubParser;
import org.checkerframework.framework.stub.StubResource;
import org.checkerframework.framework.stub.StubUtil;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.PluginUtil;

public class StubTypes {
    /** Types read from stub files (but not those from the annotated JDK jar file). */
    private Map<Element, AnnotatedTypeMirror> typesFromStubFiles;

    /**
     * Declaration annotations read from stub files (but not those from the annotated JDK jar file).
     * Map keys cannot be Element, because a different Element appears in the stub files than in the
     * real files. So, map keys are the verbose element name, as returned by
     * ElementUtils.getVerboseName.
     */
    private Map<String, Set<AnnotationMirror>> declAnnosFromStubFiles;

    /**
     * Whether or not a stub file is currently being parsed. (If one is being parsed, don't try to
     * parse another.)
     */
    private boolean parsing;

    /** AnnotatedTypeFactory */
    private AnnotatedTypeFactory factory;

    private Map<String, Path> jdk11StubFiles = new HashMap<>();
    private Map<String, String> jdk11StubFilesJar = new HashMap<>();

    public StubTypes(AnnotatedTypeFactory factory) {
        this.factory = factory;
        this.typesFromStubFiles = new HashMap<>();
        this.declAnnosFromStubFiles = new HashMap<>();
        this.parsing = false;
    }

    public boolean isParsing() {
        return parsing;
    }

    public void prepJdkStubs() {
        if (PluginUtil.getJreVersion() < 11) {
            return;
        }
        URL resourceURL = factory.getClass().getResource("/jdk11");
        if (resourceURL.getProtocol().contentEquals("jar")) {
            prepJdk11FromJar(resourceURL);
        } else if (resourceURL.getProtocol().contentEquals("file")) {
            prepJdk11FromFile(resourceURL);
        } else {
            throw new BugInCF("JDK not found");
        }
    }

    private void prepJdk11FromFile(URL resourceURL) {
        Path root;
        try {
            root = Paths.get(resourceURL.toURI());
        } catch (URISyntaxException e) {
            throw new BugInCF("Can parse URL: %s", resourceURL.toString());
        }
        Stream<Path> walk;
        try {
            walk = Files.walk(root);
        } catch (IOException e) {
            throw new BugInCF("File Not Found");
        }
        List<Path> paths =
                walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                        .collect(Collectors.toList());
        for (Path path : paths) {
            if (path.getFileName().toString().equals("package-info.java")) {
                parseStubFile(path);
                continue;
            } else {
                parseStubFile(path);
            }
            Path relativePath = root.relativize(path);
            // 4: /src/<module>/share/classes
            Path savepath = relativePath.subpath(4, relativePath.getNameCount());
            String s = savepath.toString().replace(".java", "").replace(File.separatorChar, '.');
            jdk11StubFiles.put(s, path);
        }
    }

    private void prepJdk11FromJar(URL resourceURL) {
        JarURLConnection connection;
        try {
            connection = (JarURLConnection) resourceURL.openConnection();

            // disable caching / connection sharing of the low level URLConnection to the Jarfile
            connection.setDefaultUseCaches(false);
            connection.setUseCaches(false);

            connection.connect();
        } catch (IOException e) {
            throw new BugInCF("cannot open a connection to the Jar file " + resourceURL.getFile());
        }

        try (JarFile jarFile = connection.getJarFile()) {
            for (JarEntry je : jarFile.stream().collect(Collectors.toList())) {
                // filter out directories and non-class files
                if (!je.isDirectory()
                        && je.getName().endsWith(".java")
                        && je.getName().startsWith("jdk11")) {
                    String jeNAme = je.getName();
                    int index = je.getName().indexOf("/share/classes/");
                    String shortName =
                            jeNAme.substring(index + "/share/classes/".length())
                                    .replace(".java", "")
                                    .replace('/', '.');
                    jdk11StubFilesJar.put(shortName, jeNAme);
                    parseJarEntry(jeNAme);
                }
            }
        } catch (IOException e) {
            throw new BugInCF("cannot open the Jar file " + resourceURL.getFile());
        }
    }

    /**
     * @param e Element whose type is returned.
     * @return an AnnotatedTypeMirror for {@code e} containing only annotations explicitly written
     *     in the stubfile and in the element. {@code null} is returned if {@code element} does not
     *     appear in a stub file.
     */
    public AnnotatedTypeMirror getAnnotatedTypeMirror(Element e) {
        if (parsing) {
            return null;
        }
        if (typesFromStubFiles.containsKey(e)) {
            return typesFromStubFiles.get(e).deepCopy();
        }
        if (parseEnclosingClass(e)) {
            return null;
        }
        AnnotatedTypeMirror type = typesFromStubFiles.get(e);
        return type == null ? null : type.deepCopy();
    }

    public Set<AnnotationMirror> getDeclAnnotation(Element elt, String eltName) {
        if (parsing) {
            return Collections.emptySet();
        }
        if (declAnnosFromStubFiles.containsKey(eltName)) {
            return declAnnosFromStubFiles.get(eltName);
        }

        if (parseEnclosingClass(elt)) {
            return Collections.emptySet();
        }
        if (declAnnosFromStubFiles.containsKey(eltName)) {
            return declAnnosFromStubFiles.get(eltName);
        }
        return Collections.emptySet();
    }

    /**
     * Parses the outermost enclosing class of {@code e}.
     *
     * @return {@code true} is there exists a stub file for the outermost enclosing class of {@code
     *     e}; otherwise, returns {@code false}
     */
    private boolean parseEnclosingClass(Element e) {
        String className = getOuterMostEnclosingClass(e);
        if (className == null) {
            return true;
        }
        if (jdk11StubFiles.containsKey(className)) {
            parseStubFile(jdk11StubFiles.get(className));
        } else if (jdk11StubFilesJar.containsKey(className)) {
            parseJarEntry(jdk11StubFilesJar.get(className));
        }
        jdk11StubFiles.remove(className);
        jdk11StubFilesJar.remove(className);
        return false;
    }

    /**
     * @return the fully qualified name of the outermost enclosing class of {@code e} or {@code
     *     null} if no such class exists for {@code e}.
     */
    private String getOuterMostEnclosingClass(Element e) {
        TypeElement enclosingClass = ElementUtils.enclosingClass(e);
        if (enclosingClass == null) {
            return null;
        }
        while (true) {
            Element element = enclosingClass.getEnclosingElement();
            if (element == null || element.getKind() == ElementKind.PACKAGE) {
                break;
            }
            TypeElement t = ElementUtils.enclosingClass(element);
            if (t == null) {
                break;
            }
            enclosingClass = t;
        }
        String className = enclosingClass.getQualifiedName().toString();
        return className;
    }

    private void parseStubFile(Path path) {
        parsing = true;
        try (FileInputStream jdkStub = new FileInputStream(path.toFile()); ) {
            StubParser.parseJdkFileAsStub(
                    path.toFile().getName(),
                    jdkStub,
                    factory,
                    factory.getProcessingEnv(),
                    typesFromStubFiles,
                    declAnnosFromStubFiles);
        } catch (IOException e) {
            throw new BugInCF("cannot open the jdk stub file " + path);
        } finally {
            parsing = false;
        }
    }

    private void parseJarEntry(String jarEntryName) {
        URL resourceURL = factory.getClass().getResource("/jdk11");
        JarURLConnection connection;
        try {
            connection = (JarURLConnection) resourceURL.openConnection();

            // disable caching / connection sharing of the low level URLConnection to the Jarfile
            connection.setDefaultUseCaches(false);
            connection.setUseCaches(false);

            connection.connect();
        } catch (IOException e) {
            throw new BugInCF("cannot open a connection to the Jar file " + resourceURL.getFile());
        }
        parsing = true;
        try (JarFile jarFile = connection.getJarFile()) {
            InputStream jdkStub;
            try {
                jdkStub = jarFile.getInputStream(jarFile.getJarEntry(jarEntryName));
            } catch (IOException e) {
                throw new BugInCF("cannot open the jdk stub file " + jarEntryName);
            }
            StubParser.parseJdkFileAsStub(
                    jarEntryName,
                    jdkStub,
                    factory,
                    factory.getProcessingEnv(),
                    typesFromStubFiles,
                    declAnnosFromStubFiles);
        } catch (IOException e) {
            throw new BugInCF("cannot open the Jar file " + connection.getEntryName());
        } catch (BugInCF e) {
            throw new BugInCF("Exception while parsing " + jarEntryName, e);
        } finally {
            parsing = false;
        }
    }

    /**
     * Parses the stub files in the following order:
     *
     * <ol>
     *   <li>jdk.astub in the same directory as the checker, if it exists and ignorejdkastub option
     *       is not supplied <br>
     *   <li>Stub files listed in @StubFiles annotation on the checker; must be in same directory as
     *       the checker<br>
     *   <li>Stub files provide via stubs system property <br>
     *   <li>Stub files provide via stubs environment variable <br>
     *   <li>Stub files provide via stubs compiler option
     * </ol>
     *
     * <p>If a type is annotated with a qualifier from the same hierarchy in more than one stub
     * file, the qualifier in the last stub file is applied.
     *
     * <p>Sets typesFromStubFiles and declAnnosFromStubFiles by side effect, just before returning.
     */
    public void parseStubFiles() {
        // TODO: Error if this is called more than once?
        SourceChecker checker = factory.getContext().getChecker();
        ProcessingEnvironment processingEnv = factory.getProcessingEnv();
        // 1. jdk.astub
        // Only look in .jar files, and parse it right away.
        if (!checker.hasOption("ignorejdkastub")) {
            InputStream in = checker.getClass().getResourceAsStream("jdk.astub");
            if (in != null) {
                StubParser.parse(
                        checker.getClass().getResource("jdk.astub").toString(),
                        in,
                        factory,
                        processingEnv,
                        typesFromStubFiles,
                        declAnnosFromStubFiles);
            }
            // TODO: document
            prepJdkStubs();
        }

        // Stub files specified via stubs compiler option, stubs system property,
        // stubs env. variable, or @StubFiles
        List<String> allStubFiles = new ArrayList<>();

        // 2. Stub files listed in @StubFiles annotation on the checker
        StubFiles stubFilesAnnotation = checker.getClass().getAnnotation(StubFiles.class);
        if (stubFilesAnnotation != null) {
            Collections.addAll(allStubFiles, stubFilesAnnotation.value());
        }

        // 3. Stub files provided via stubs system property
        String stubsProperty = System.getProperty("stubs");
        if (stubsProperty != null) {
            Collections.addAll(allStubFiles, stubsProperty.split(File.pathSeparator));
        }

        // 4. Stub files provided via stubs environment variable
        String stubEnvVar = System.getenv("stubs");
        if (stubEnvVar != null) {
            Collections.addAll(allStubFiles, stubEnvVar.split(File.pathSeparator));
        }

        // 5. Stub files provided via stubs option
        String stubsOption = checker.getOption("stubs");
        if (stubsOption != null) {
            Collections.addAll(allStubFiles, stubsOption.split(File.pathSeparator));
        }

        // Parse stub files.
        for (String stubPath : allStubFiles) {
            // Special case when running in jtreg.
            String base = System.getProperty("test.src");
            String stubPathFull = stubPath;
            if (base != null) {
                stubPathFull = base + "/" + stubPath;
            }
            List<StubResource> stubs = StubUtil.allStubFiles(stubPathFull);
            if (stubs.isEmpty()) {
                // If the stub file has a prefix of "checker.jar/" then look for the file in the top
                // level directory of the jar that contains the checker.
                stubPath = stubPath.replace("checker.jar/", "/");
                InputStream in = checker.getClass().getResourceAsStream(stubPath);
                if (in == null) {
                    // Didn't find the stubfile.
                    URL topLevelResource = checker.getClass().getResource("/" + stubPath);
                    if (topLevelResource != null) {
                        checker.message(
                                Kind.WARNING,
                                stubPath
                                        + " should be in the same directory as "
                                        + checker.getClass().getSimpleName()
                                        + ".class, but is at the top level of a jar file: "
                                        + topLevelResource);
                    } else {
                        checker.message(
                                Kind.WARNING,
                                "Did not find stub file "
                                        + stubPath
                                        + " on classpath or within directory "
                                        + new File(stubPath).getAbsolutePath()
                                        + (stubPathFull.equals(stubPath)
                                                ? ""
                                                : (" or at " + stubPathFull)));
                    }
                } else {
                    StubParser.parse(
                            stubPath,
                            in,
                            factory,
                            processingEnv,
                            typesFromStubFiles,
                            declAnnosFromStubFiles);
                }
            }
            for (StubResource resource : stubs) {
                InputStream stubStream;
                try {
                    stubStream = resource.getInputStream();
                } catch (IOException e) {
                    checker.message(
                            Kind.NOTE,
                            "Could not read stub resource: " + resource.getDescription());
                    continue;
                }
                StubParser.parse(
                        resource.getDescription(),
                        stubStream,
                        factory,
                        processingEnv,
                        typesFromStubFiles,
                        declAnnosFromStubFiles);
            }
        }
    }
}
