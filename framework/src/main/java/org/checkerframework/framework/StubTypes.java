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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import org.checkerframework.framework.stub.StubParser;
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

    private boolean parsing;
    AnnotatedTypeFactory factory;

    public StubTypes(AnnotatedTypeFactory factory) {
        this.factory = factory;
        this.typesFromStubFiles = new HashMap<>();
        this.declAnnosFromStubFiles = new HashMap<>();
        this.parsing = false;
    }

    public boolean isParsing() {
        return parsing;
    }

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
        } else {
            return true;
        }
        jdk11StubFiles.remove(className);
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
            StubParser.parse(
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

    Map<String, Path> jdk11StubFiles = new HashMap<>();

    public void prepJdkStubs() {
        if (PluginUtil.getJreVersion() < 11) {
            return;
        }
        URL resourceURL = factory.getClass().getResource("/jdk11");
        if (resourceURL.getProtocol().contentEquals("jar")) {
            parseJdk11FromJar(resourceURL);
        } else if (resourceURL.getProtocol().contentEquals("file")) {
            parseJdk11FromFile(resourceURL);
        } else {
            new BugInCF("JDK not found");
        }
    }

    private void parseJdk11FromFile(URL resourceURL) {
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
            }
            Path relativePath = root.relativize(path);
            // 4: /src/<module>/share/classes
            Path savepath = relativePath.subpath(4, relativePath.getNameCount());
            String s = savepath.toString().replace(".java", "").replace(File.separatorChar, '.');
            jdk11StubFiles.put(s, path);
        }
    }

    private void parseJdk11FromJar(URL resourceURL) {
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
                    InputStream jdkStub;
                    try {
                        jdkStub = jarFile.getInputStream(je);
                    } catch (IOException e) {
                        throw new BugInCF("cannot open the jdk stub file " + je);
                    }

                    StubParser.parse(
                            je.getName(),
                            jdkStub,
                            factory,
                            factory.getProcessingEnv(),
                            typesFromStubFiles,
                            declAnnosFromStubFiles);
                }
            }
        } catch (IOException e) {
            throw new BugInCF("cannot open the Jar file " + resourceURL.getFile());
        }
    }
}
