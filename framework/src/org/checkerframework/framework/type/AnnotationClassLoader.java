package org.checkerframework.framework.type;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
 */

/**
 * This class assists the {@link AnnotatedTypeFactory} by reflectively looking up the list of
 * annotation class names in each checker's qual directory, and then loading and returning it as a
 * set of annotation classes. It can also look up and load annotation classes from external
 * directories that are passed as arguments to checkers that have extension capabilities such as the
 * Subtyping Checker, Fenum Checker, and Units Checker.
 *
 * <p>To load annotations using this class, their directory structure and package structure must be
 * identical.
 *
 * <p>Only annotation classes that have the {@link Target} meta-annotation with the value of {@link
 * ElementType#TYPE_USE} (and optionally {@link ElementType#TYPE_PARAMETER}) are loaded. If it has
 * other {@link ElementType} values, it won't be loaded. Other annotation classes must be manually
 * listed in a checker's annotated type factory by overriding {@link
 * AnnotatedTypeFactory#createSupportedTypeQualifiers()}.
 *
 * <p>Checker writers may wish to subclass this class if they wish to implement some custom rules to
 * filter or process loaded annotation classes, by providing an override implementation of {@link
 * #isSupportedAnnotationClass(Class)}. See {@link
 * org.checkerframework.checker.units.UnitsAnnotationClassLoader UnitsAnnotationClassLoader} for an
 * example.
 *
 * @author Jeff Luo
 */
public class AnnotationClassLoader {
    // For issuing errors to the user
    private final BaseTypeChecker checker;

    // For loading from a source package directory
    private final String packageName;
    private final String packageNameWithSlashes;
    private final List<String> fullyQualifiedPackageNameSegments;
    private static final String QUAL_PACKAGE_SUFFIX = ".qual";

    // For loading from a Jar file
    private static final String JAR_SUFFIX = ".jar";
    private static final String CLASS_SUFFIX = ".class";

    // For loading from external directories
    private static final String JAVA_SUFFIX = ".java";

    // Constants
    private static final char DOT = '.';
    private static final char SLASH = '/';

    /**
     * Processing Env used to create an {@link AnnotationBuilder}, which is in turn used to build
     * the annotation mirror from the loaded class.
     */
    protected final ProcessingEnvironment processingEnv;

    /** The resource URL of the qual directory of a checker class */
    private final URL resourceURL;

    /**
     * The loaded annotation classes. Call {@link #getLoadedAnnotationClasses} rather than using
     * this field directly as it may be null.
     */
    private Set<Class<? extends Annotation>> loadedAnnotations;

    /**
     * Constructor for loading annotations defined for a checker.
     *
     * @param checker a {@link BaseTypeChecker} or its subclass
     */
    public AnnotationClassLoader(final BaseTypeChecker checker) {
        this.checker = checker;
        processingEnv = checker.getProcessingEnvironment();

        // package name must use dots, this is later prepended to annotation
        // class names as we load the classes using the class loader
        packageName =
                checker.getClass().getPackage() != null
                        ? checker.getClass().getPackage().getName() + QUAL_PACKAGE_SUFFIX
                        : QUAL_PACKAGE_SUFFIX.substring(1);

        // the package name with dots replaced by slashes will be used to scan
        // file directories
        packageNameWithSlashes = packageName.replace(DOT, SLASH);

        // each component of the fully qualified package name will be used later
        // to recursively descend from a root directory to see if the package
        // exists in some particular root directory
        fullyQualifiedPackageNameSegments = new ArrayList<String>();

        // from the fully qualified package name, split it at every dot then add
        // to the list
        fullyQualifiedPackageNameSegments.addAll(
                Arrays.asList(
                        Pattern.compile(Character.toString(DOT), Pattern.LITERAL)
                                .split(packageName)));

        // Only load annotations if requested.  This avoids issuing an error
        // if the qual package contains an annotation that is not a qualifier,
        // but the checker does not try to use it as a qualifier.
        loadedAnnotations = null;

        ClassLoader applicationClassloader = getAppClassLoader();

        if (applicationClassloader != null) {
            // if the application classloader is accessible, then directly
            // retrieve the resource URL of the qual package
            // resource URLs must use slashes
            resourceURL = applicationClassloader.getResource(packageNameWithSlashes);

            // thread based application classloader, if needed in the future:
            // resourceURL = Thread.currentThread().getContextClassLoader().getResource(packageNameWithSlashes);
        } else {
            // if the application classloader is not accessible (which means the
            // checker class was loaded using the bootstrap classloader)
            // then scan the classpaths to find a jar or directory which
            // contains the qual package and set the resource URL to that jar or
            // qual directory
            resourceURL = getURLFromClasspaths();
        }
    }

    /**
     * Scans all classpaths and returns the resource URL to the jar which contains the checker's
     * qual package, or the qual package directory if it exists, or null if no jar or directory
     * contains the package
     *
     * @return a URL to the jar that contains the qual package, or to the qual package's directory,
     *     or null if no jar or directory contains the qual package
     */
    private final /*@Nullable*/ URL getURLFromClasspaths() {
        // Debug use, uncomment if needed to see all of the classpaths (boot
        // classpath, extension classpath, and classpath)
        // printPaths();

        URL url = null;

        // obtain all classpaths
        Set<String> paths = getClasspaths();

        // In checkers, there will be a resource URL for the qual directory. But
        // when called in the framework (eg GeneralAnnotatedTypeFactory), there
        // won't be a resourceURL since there isn't a qual directory

        // each path from the set of classpaths will be checked to see if it
        // contains the qual directory of a checker, if so, the first
        // directory or jar that contains the package will be used as the source
        // for loading classes from the qual package

        // if either a directory or a jar contains the package, resourceURL will
        // be updated to refer to that source, otherwise resourceURL remains as
        // null

        // if both a jar and a directory contain the qual package, then the
        // order of the jar and the directory in the command line option(s)
        // or environment variables will decide which one gets examined first
        for (String path : paths) {
            // see if the current classpath segment is a jar or a directory
            if (path.endsWith(JAR_SUFFIX)) {
                // current classpath segment is a jar
                url = getJarURL(path);

                // see if the jar contains the package
                if (url != null && containsPackage(url)) {
                    return url;
                }
            } else {
                // current classpath segment is a directory
                url = getDirectoryURL(path);

                // see if the directory contains the package
                if (url != null && containsPackage(url)) {
                    // append a slash if necessary
                    if (!path.endsWith(Character.toString(SLASH))) {
                        path += SLASH;
                    }

                    // update URL to the qual directory
                    url = getDirectoryURL(path + packageNameWithSlashes);

                    return url;
                }
            }
        }

        // if no jar or directory contains the qual package, then return null
        return null;
    }

    /**
     * Checks to see if the jar or directory referred by the URL contains the qual package of a
     * specific checker
     *
     * @param url a URL referring to either a jar or a directory
     * @return true if the jar or the directory contains the qual package, false otherwise
     */
    private final boolean containsPackage(final URL url) {
        // see whether the resource URL has a protocol of jar or file
        if (url.getProtocol().equals("jar")) {
            // try to open up the jar file
            try {
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                JarFile jarFile = connection.getJarFile();

                // check to see if the jar file contains the package
                return checkJarForPackage(jarFile);
            } catch (IOException e) {
                // do nothing for missing or un-openable Jar files
            }
        } else if (url.getProtocol().equals("file")) {
            // open up the directory
            File rootDir = new File(url.getFile());

            // check to see if the directory contains the package
            return checkDirForPackage(rootDir, fullyQualifiedPackageNameSegments.iterator());
        }

        return false;
    }

    /**
     * Checks to see if the jar file contains the qual package of a specific checker
     *
     * @param jar a jar file
     * @return true if the jar file contains the qual package, false otherwise
     */
    private final boolean checkJarForPackage(final JarFile jar) {
        Enumeration<JarEntry> jarEntries = jar.entries();

        // loop through the entries in the jar
        while (jarEntries.hasMoreElements()) {
            JarEntry je = jarEntries.nextElement();

            // each entry is the fully qualified path and file name to a
            // particular artifact in the jar file (eg a class file)
            // if the jar has the package, one of the entry's name will begin
            // with the package name in slash notation
            String entryName = je.getName();
            if (entryName.startsWith(packageNameWithSlashes)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks to see if the current directory contains the qual package through recursion currentDir
     * starts at the root directory (a directory passed in as part of the classpaths), the iterator
     * goes through each segment of the fully qualified package name (each segment is separated by a
     * dot)
     *
     * <p>Each step of the recursion checks to see if there's a subdirectory in the current
     * directory that has a name matching the package name segment, if so, it recursively descends
     * into that subdirectory to check the next package name segment
     *
     * <p>If there's no more segments left, then we've found the qual directory of interest
     *
     * <p>If we've checked every subdirectory and none of them match the current package name
     * segment, then the qual directory of interest does not exist in the given root directory (at
     * the beginning of recursion)
     *
     * @param currentDir current directory
     * @param pkgNames an iterator which provides each segment of the fully qualified qual package
     *     name
     * @return true if the qual package exists within the root directory, false otherwise
     */
    private final boolean checkDirForPackage(
            final File currentDir, final Iterator<String> pkgNames) {
        // if the iterator has no more package name segments, then we've found
        // the qual directory of interest
        if (!pkgNames.hasNext()) {
            return true;
        }
        // if the file doesn't exist or it isn't a directory, return false
        if (currentDir == null || !currentDir.isDirectory()) {
            return false;
        }

        // if it isn't empty, dequeue one segment of the fully qualified package
        // name
        String currentPackageDirName = pkgNames.next();

        // scan current directory to see if there's a sub-directory that has a
        // matching name as the package name segment
        for (File file : currentDir.listFiles()) {
            if (file.isDirectory() && file.getName().equals(currentPackageDirName)) {
                // if so, recursively descend and look at the next segment of
                // the package name
                return checkDirForPackage(file, pkgNames);
            }
        }

        // if no sub-directory has a matching name, then that means there isn't
        // a matching qual package
        return false;
    }

    /**
     * Given an absolute path to a directory, this method will return a URL reference to that
     * directory
     *
     * @param absolutePathToDirectory an absolute path to a directory
     * @return a URL reference to the directory, or null if the URL is malformed
     */
    private final /*@Nullable*/ URL getDirectoryURL(final String absolutePathToDirectory) {
        URL directoryURL = null;

        try {
            directoryURL = new File(absolutePathToDirectory).toURI().toURL();
        } catch (MalformedURLException e) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Kind.NOTE,
                            "Directory URL " + absolutePathToDirectory + " is malformed");
        }

        return directoryURL;
    }

    /**
     * Given an absolute path to a jar file, this method will return a URL reference to that jar
     * file
     *
     * @param absolutePathToJarFile an absolute path to a jar file
     * @return a URL reference to the jar file, or null if the URL is malformed
     */
    private final /*@Nullable*/ URL getJarURL(final String absolutePathToJarFile) {
        URL jarURL = null;

        try {
            jarURL = new URL("jar:file:" + absolutePathToJarFile + "!/");
        } catch (MalformedURLException e) {
            processingEnv
                    .getMessager()
                    .printMessage(Kind.NOTE, "Jar URL " + absolutePathToJarFile + " is malformed");
        }

        return jarURL;
    }

    /**
     * Obtains and returns a set of the classpaths from compiler options, system environment
     * variables, and by examining the classloader to see what paths it has access to
     *
     * <p>The classpaths will be obtained in the order of:
     *
     * <ol>
     *   <li>extension paths (from java.ext.dirs)
     *   <li>classpaths (set in {@code CLASSPATH}, or through {@code -classpath} and {@code -cp})
     *   <li>paths accessible and examined by the classloader
     * </ol>
     *
     * In each of these paths, the order of the paths as specified in the command line options or
     * environment variables will be the order returned in the set
     *
     * @return an immutable linked hashset of the classpaths
     */
    private final Set<String> getClasspaths() {
        Set<String> paths = new LinkedHashSet<String>();

        // add all extension paths
        paths.addAll(Arrays.asList(System.getProperty("java.ext.dirs").split(":")));

        // add all paths in CLASSPATH, -cp, and -classpath
        paths.addAll(Arrays.asList(System.getProperty("java.class.path").split(":")));

        // add all paths that are examined by the classloader
        ClassLoader applicationClassloader = getAppClassLoader();

        if (applicationClassloader != null) {
            URL[] urls = ((URLClassLoader) applicationClassloader).getURLs();
            for (int i = 0; i < urls.length; i++) {
                paths.add(urls[i].getFile().toString());
            }
        }

        return Collections.unmodifiableSet(paths);
    }

    /**
     * Obtains the classloader used to load the checker class, if that isn't available then it will
     * try to obtain the system classloader
     *
     * @return the classloader used to load the checker class, or the system classloader, or null if
     *     both are unavailable
     */
    private final /*@Nullable*/ ClassLoader getAppClassLoader() {
        return InternalUtils.getClassLoaderForClass(checker.getClass());
    }

    /** Debug Use Displays all classpaths */
    @SuppressWarnings("unused") // for debugging
    private final void printPaths() {
        // all paths in Xbootclasspath
        String[] bootclassPaths = System.getProperty("sun.boot.class.path").split(":");
        processingEnv.getMessager().printMessage(Kind.NOTE, "bootclass path:");
        for (String path : bootclassPaths) {
            processingEnv.getMessager().printMessage(Kind.NOTE, "\t" + path);
        }

        // all extension paths
        String[] extensionDirs = System.getProperty("java.ext.dirs").split(":");
        processingEnv.getMessager().printMessage(Kind.NOTE, "extension dirs:");
        for (String path : extensionDirs) {
            processingEnv.getMessager().printMessage(Kind.NOTE, "\t" + path);
        }

        // all paths in CLASSPATH, -cp, and -classpath
        String[] javaclassPaths = System.getProperty("java.class.path").split(":");
        processingEnv.getMessager().printMessage(Kind.NOTE, "java classpaths:");
        for (String path : javaclassPaths) {
            processingEnv.getMessager().printMessage(Kind.NOTE, "\t" + path);
        }

        // add all paths that are examined by the classloader
        ClassLoader applicationClassLoader = getAppClassLoader();
        processingEnv.getMessager().printMessage(Kind.NOTE, "classloader examined paths:");
        if (applicationClassLoader != null) {
            URL[] urls = ((URLClassLoader) applicationClassLoader).getURLs();
            for (int i = 0; i < urls.length; i++) {
                processingEnv.getMessager().printMessage(Kind.NOTE, "\t" + urls[i].getFile());
            }
        } else {
            processingEnv.getMessager().printMessage(Kind.NOTE, "classloader unavailable");
        }
    }

    /**
     * Gets the set of the loaded annotation classes. Note that the returned set from this method is
     * mutable. This method is intended to be called within {@link
     * AnnotatedTypeFactory#createSupportedTypeQualifiers() createSupportedTypeQualifiers()} (or its
     * helper methods) to help define the set of supported qualifiers. {@link
     * AnnotatedTypeFactory#createSupportedTypeQualifiers() createSupportedTypeQualifiers()} must
     * return an immutable set, and it is the responsibility of that method (or helper methods it
     * calls) to convert the set returned by this method, along with any additional annotation
     * classes, into an immutable set.
     *
     * @return the set of loaded annotation classes
     */
    public final Set<Class<? extends Annotation>> getLoadedAnnotationClasses() {
        if (loadedAnnotations == null) {
            loadedAnnotations = new LinkedHashSet<Class<? extends Annotation>>();
            if (resourceURL == null) {
                // if there's no resourceURL, then there's nothing we can load
                return loadedAnnotations;
            }

            // retrieve the fully qualified class names of the annotations
            Set<String> annotationNames = null;

            // see whether the resource URL has a protocol of jar or file
            if (resourceURL.getProtocol().equals("jar")) {
                // if the checker class file is contained within a jar, then the
                // resource URL for the qual directory will have the protocol
                // "jar". This means the whole checker is loaded as a jar file.

                // open up that jar file and extract annotation class names
                try {
                    JarURLConnection connection = (JarURLConnection) resourceURL.openConnection();
                    JarFile jarFile = connection.getJarFile();

                    // get class names inside the jar file within the particular
                    // package
                    annotationNames = getBundledAnnotationNamesFromJar(jarFile);
                } catch (IOException e) {
                    ErrorReporter.errorAbort(
                            "AnnotatedTypeLoader: cannot open the Jar file "
                                    + resourceURL.getFile());
                }
            } else if (resourceURL.getProtocol().equals("file")) {
                // if the checker class file is found within the file system itself
                // within some directory (usually development build directories),
                // then process the package as a file directory in the file system
                // and load the annotations contained in the qual directory

                // open up the directory
                File packageDir = new File(resourceURL.getFile());
                annotationNames =
                        getAnnotationNamesFromDirectory(
                                packageName + DOT, resourceURL.getFile(), packageDir, CLASS_SUFFIX);
            }

            loadedAnnotations.addAll(loadAnnotationClasses(annotationNames));
        }
        return loadedAnnotations;
    }

    /**
     * Retrieves the annotation class file names from the qual directory contained inside a jar
     *
     * @param jar the JarFile containing the annotation class files
     * @return a set of fully qualified class names of the annotations
     */
    private final Set<String> getBundledAnnotationNamesFromJar(final JarFile jar) {
        Set<String> annos = new LinkedHashSet<String>();

        // get an enumeration iterator for all the content entries in the jar
        // file
        Enumeration<JarEntry> jarEntries = jar.entries();

        // enumerate through the entries
        while (jarEntries.hasMoreElements()) {
            JarEntry je = jarEntries.nextElement();
            // filter out directories and non-class files
            if (je.isDirectory() || !je.getName().endsWith(CLASS_SUFFIX)) {
                continue;
            }

            // get rid of the .class suffix
            String className = je.getName().substring(0, je.getName().lastIndexOf('.'));
            // convert path notation to class notation
            className = className.replace(SLASH, DOT);

            // filter for qual package
            if (className.startsWith(packageName)) {
                // add to set
                annos.add(className);
            }
        }

        return annos;
    }

    /**
     * This method takes as input the canonical name of an external annotation class and loads and
     * returns that class via the class loader.
     *
     * @param annoName canonical name of an external annotation class, e.g.
     *     "myproject.qual.myannotation"
     * @return the loaded annotation class
     */
    public final /*@Nullable*/ Class<? extends Annotation> loadExternalAnnotationClass(
            final String annoName) {
        try {
            final Class<? extends Annotation> annoClass =
                    Class.forName(annoName, true, getAppClassLoader()).asSubclass(Annotation.class);
            return annoClass;
        } catch (ClassNotFoundException e) {
            checker.userErrorAbort(
                    checker.getClass().getSimpleName()
                            + ": could not load class for annotation: "
                            + annoName
                            + "; ensure that your classpath is correct");
        } catch (ClassCastException e) {
            checker.userErrorAbort(
                    checker.getClass().getSimpleName()
                            + ": class "
                            + annoName
                            + " is not an annotation");
        }
        return null;
    }

    /**
     * This method takes as input a fully qualified path to a directory, and loads and returns the
     * set of all annotation classes from that directory.
     *
     * @param dirName absolute path to a directory containing annotation classes
     * @return a set of annotation classes
     */
    public final Set<Class<? extends Annotation>> loadExternalAnnotationClassesFromDirectory(
            final String dirName) {
        File rootDirectory = new File(dirName);
        Set<String> annoNames =
                getAnnotationNamesFromDirectory("", dirName, rootDirectory, JAVA_SUFFIX);
        return loadAnnotationClasses(annoNames);
    }

    /**
     * Retrieves all annotation names from the current directory, and recursively descends and
     * retrieves annotation names from sub-directories.
     *
     * @param packageName a string storing the name of the package that contains the qual package
     * @param rootDirectory a string storing the absolute path of the root directory of a set of
     *     annotations, which is subtracted from class names to retrieve each class's fully
     *     qualified class names
     * @param currentDirectory a {@link File} object representing the current sub-directory of the
     *     root directory
     * @param fileExtension a file extension suffix that a file must have to be considered an
     *     annotation file, normally either {@link #CLASS_SUFFIX} or {@link #JAVA_SUFFIX} is passed
     *     in as its value
     * @return a set of strings where each string is the fully qualified class name of an annotation
     *     in the root directory or its sub-directories
     */
    private final Set<String> getAnnotationNamesFromDirectory(
            final String packageName,
            final String rootDirectory,
            final File currentDirectory,
            final String fileExtension) {
        Set<String> results = new LinkedHashSet<String>();

        // check every file and directory within the current directory
        File[] directoryContents = currentDirectory.listFiles();
        Arrays.sort(
                directoryContents,
                new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
        for (File file : directoryContents) {
            if (file.isFile()) {
                // Full file name, including path to file
                String fullFileName = file.getAbsolutePath();
                // Simple file name
                String fileName =
                        fullFileName.substring(
                                fullFileName.lastIndexOf(File.separator) + 1,
                                fullFileName.length());
                // Path to file
                String filePath =
                        fullFileName.substring(0, fullFileName.lastIndexOf(File.separator));
                // Package name beginning with "qual"
                String qualPackageName = "";
                if (!filePath.equals(rootDirectory)) {
                    qualPackageName =
                            filePath.substring(rootDirectory.length() + 1, filePath.length())
                                            .replace(SLASH, DOT)
                                    + DOT;
                }
                // Annotation name, which is the same as the file name but with
                // file extension removed
                String annotationName = fileName;
                if (fileName.lastIndexOf(DOT) != -1) {
                    annotationName = fileName.substring(0, fileName.lastIndexOf(DOT));
                }

                // Fully qualified annotation class name
                String fullyQualifiedAnnoName = packageName + qualPackageName + annotationName;

                if (fileName.endsWith(fileExtension)) {
                    // add the fully qualified annotation class name to the set
                    results.add(fullyQualifiedAnnoName);
                }
            } else if (file.isDirectory()) {
                // recursively add all sub directories's fully qualified annotation class name
                results.addAll(
                        getAnnotationNamesFromDirectory(
                                packageName, rootDirectory, file, fileExtension));
            }
        }

        return results;
    }

    /**
     * Loads the class indicated by the fullyQualifiedClassName, and checks to see if it is an
     * annotation that is supported by a checker.
     *
     * @param fullyQualifiedClassName the fully qualified name of the class
     * @return the loaded annotation class if it is defined with ElementType.TYPE_USE and is a
     *     supported annotation, null otherwise
     */
    private final /*@Nullable*/ Class<? extends Annotation> loadAnnotationClass(
            final String fullyQualifiedClassName) {
        Class<?> cls = null;

        try {
            // load the class
            cls = Class.forName(fullyQualifiedClassName, true, getAppClassLoader());
        } catch (ClassNotFoundException e) {
            // do nothing: projects can have annotation class files and regular
            // source files located within the same directory, and as such when
            // it tires to load an uncompiled source file, it will throw
            // ClassNotFoundException
        }

        // ensure that the freshly loaded class is an annotation, and has
        // the @Target annotation
        if (cls != null && cls.isAnnotation() && cls.getAnnotation(Target.class) != null) {
            // retrieve the set of ElementTypes in the @Target
            // meta-annotation and check to see if this annotation is
            // supported for automatic loading
            if (AnnotatedTypes.hasTypeQualifierElementTypes(
                    cls.getAnnotation(Target.class).value(), cls)) {
                // if it is supported, then subclass it as an Annotation
                // class
                Class<? extends Annotation> annoClass = cls.asSubclass(Annotation.class);

                // see if the annotation is supported by a checker
                if (isSupportedAnnotationClass(annoClass)) {
                    return annoClass;
                }
            }
        }

        return null;
    }

    /**
     * Loads a set of annotations indicated by fullyQualifiedAnnoNames.
     *
     * @param fullyQualifiedAnnoNames a set of strings where each string is a single annotation
     *     class's fully qualified name
     * @return a set of loaded annotation classes
     * @see #loadAnnotationClass(String)
     */
    private final Set<Class<? extends Annotation>> loadAnnotationClasses(
            final /*@Nullable*/ Set<String> fullyQualifiedAnnoNames) {
        Set<Class<? extends Annotation>> loadedClasses =
                new LinkedHashSet<Class<? extends Annotation>>();

        if (fullyQualifiedAnnoNames != null && !fullyQualifiedAnnoNames.isEmpty()) {
            // loop through each class name & load the class
            for (String fullyQualifiedAnnoName : fullyQualifiedAnnoNames) {
                Class<? extends Annotation> annoClass = loadAnnotationClass(fullyQualifiedAnnoName);
                if (annoClass != null) {
                    loadedClasses.add(annoClass);
                }
            }
        }

        return loadedClasses;
    }

    /**
     * Checks to see whether a particular annotation class is supported.
     *
     * <p>Every subclass of AnnotatedTypeLoader can override this method to indicate whether a
     * particular annotation is supported by its checker.
     *
     * @param annoClass an annotation class
     * @return true if the annotation is supported, false if it isn't
     */
    protected boolean isSupportedAnnotationClass(final Class<? extends Annotation> annoClass) {
        if (getLoadedAnnotationClasses().contains(annoClass)) {
            // if it has already been checked before, return true
            return true;
        } else {
            // The standard way to see if an annotation is supported is to build
            // its annotation mirror if there's no problems building the
            // mirror, then it is supported
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, annoClass);
            AnnotationMirror annoMirroResult = builder.build();
            // TODO: build() internally will error abort if it fails, can we
            // gracefully resume here?
            return (annoMirroResult != null);
        }
    }
}
