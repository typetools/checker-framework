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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.checker.signature.qual.Identifier;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.UserError;
import org.plumelib.reflection.Signatures;

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
 * #isSupportedAnnotationClass(Class)}. See {@code
 * org.checkerframework.checker.units.UnitsAnnotationClassLoader} for an example.
 */
public class AnnotationClassLoader {
  /** For issuing errors to the user. */
  protected final BaseTypeChecker checker;

  // For loading from a source package directory
  /** The package name. */
  private final @DotSeparatedIdentifiers String packageName;
  /** The package name, with periods replaced by slashes. */
  private final String packageNameWithSlashes;
  /** The atomic package names (the package name split at dots). */
  private final List<@Identifier String> fullyQualifiedPackageNameSegments;
  /** The name of a Checker's qualifier package. */
  private static final String QUAL_PACKAGE = "qual";

  // For loading from a Jar file
  /** The suffix for a .jar file. */
  private static final String JAR_SUFFIX = ".jar";
  /** The suffix for a .class file. */
  private static final String CLASS_SUFFIX = ".class";

  // Constants
  /** The package separator. */
  private static final char DOT = '.';
  /** The path separator, in .jar files, binary names, etc. */
  private static final char SLASH = '/';

  /**
   * Processing Env used to create an {@link AnnotationBuilder}, which is in turn used to build the
   * annotation mirror from the loaded class.
   */
  protected final ProcessingEnvironment processingEnv;

  /** The resource URL of the qual directory of a checker class. */
  private final URL resourceURL;

  /** The class loader used to load annotation classes. */
  protected final URLClassLoader classLoader;

  /**
   * The annotation classes bundled with a checker (located in its qual directory) that are deemed
   * supported by the checker (non-alias annotations). Each checker can override {@link
   * #isSupportedAnnotationClass(Class)} to determine whether an annotation is supported or not.
   * Call {@link #getBundledAnnotationClasses()} to obtain a reference to the set of classes.
   */
  private final Set<Class<? extends Annotation>> supportedBundledAnnotationClasses;

  /** The package separator: ".". */
  private static final Pattern DOT_LITERAL_PATTERN =
      Pattern.compile(Character.toString(DOT), Pattern.LITERAL);

  /**
   * Constructor for loading annotations defined for a checker.
   *
   * @param checker a {@link BaseTypeChecker} or its subclass
   */
  @SuppressWarnings("signature") // TODO: reduce use of string manipulation
  public AnnotationClassLoader(final BaseTypeChecker checker) {
    this.checker = checker;
    processingEnv = checker.getProcessingEnvironment();

    // package name must use dots, this is later prepended to annotation
    // class names as we load the classes using the class loader
    Package checkerPackage = checker.getClass().getPackage();
    packageName =
        checkerPackage != null && !checkerPackage.getName().isEmpty()
            ? checkerPackage.getName() + DOT + QUAL_PACKAGE
            : QUAL_PACKAGE;

    // the package name with dots replaced by slashes will be used to scan file directories
    packageNameWithSlashes = packageName.replace(DOT, SLASH);

    // Each component of the fully qualified package name will be used later to recursively descend
    // from a root directory to see if the package exists in some particular root directory.
    fullyQualifiedPackageNameSegments = new ArrayList<>();

    // from the fully qualified package name, split it at every dot then add to the list
    fullyQualifiedPackageNameSegments.addAll(Arrays.asList(DOT_LITERAL_PATTERN.split(packageName)));

    classLoader = getClassLoader();

    URL localResourceURL;
    if (classLoader != null) {
      // if the application classloader is accessible, then directly retrieve the resource URL of
      // the qual package resource URLs must use slashes
      localResourceURL = classLoader.getResource(packageNameWithSlashes);

      // thread based application classloader, if needed in the future:
      // resourceURL =
      // Thread.currentThread().getContextClassLoader().getResource(packageNameWithSlashes);
    } else {
      // Signal failure to find resource
      localResourceURL = null;
    }

    if (localResourceURL == null) {
      // if the application classloader is not accessible (which means the checker class was loaded
      // using the bootstrap classloader)
      // or if the classloader didn't find the package,
      // then scan the classpaths to find a jar or directory which contains the qual package and set
      // the resource URL to that jar or qual directory
      localResourceURL = getURLFromClasspaths();
    }
    resourceURL = localResourceURL;

    supportedBundledAnnotationClasses = new LinkedHashSet<>();

    loadBundledAnnotationClasses();
  }

  /**
   * Scans all classpaths and returns the resource URL to the jar which contains the checker's qual
   * package, or the qual package directory if it exists, or null if no jar or directory contains
   * the package.
   *
   * @return a URL to the jar that contains the qual package, or to the qual package's directory, or
   *     null if no jar or directory contains the qual package
   */
  private final @Nullable URL getURLFromClasspaths() {
    // TODO: This method could probably be replaced with
    // io.github.classgraph.ClassGraph#getClasspathURIs()

    // Debug use, uncomment if needed to see all of the classpaths (boot
    // classpath, extension classpath, and classpath)
    // printPaths();

    URL url = null;

    // obtain all classpaths
    Set<String> paths = getClasspaths();

    // In checkers, there will be a resource URL for the qual directory. But when called in the
    // framework (eg GeneralAnnotatedTypeFactory), there won't be a resourceURL since there isn't a
    // qual directory.

    // Each path from the set of classpaths will be checked to see if it contains the qual directory
    // of a checker, if so, the first directory or jar that contains the package will be used as the
    // source for loading classes from the qual package.

    // If either a directory or a jar contains the package, resourceURL will be updated to refer to
    // that source, otherwise resourceURL remains as null.

    // If both a jar and a directory contain the qual package, then the order of the jar and the
    // directory in the command line option(s) or environment variables will decide which one gets
    // examined first.
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
   * specific checker.
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
   * Checks to see if the jar file contains the qual package of a specific checker.
   *
   * @param jar a jar file
   * @return true if the jar file contains the qual package, false otherwise
   */
  @SuppressWarnings("JdkObsolete")
  private final boolean checkJarForPackage(final JarFile jar) {
    Enumeration<JarEntry> jarEntries = jar.entries();

    // loop through the entries in the jar
    while (jarEntries.hasMoreElements()) {
      JarEntry je = jarEntries.nextElement();

      // Each entry is the fully qualified path and file name to a particular artifact in the jar
      // file (eg a class file).
      // If the jar has the package, one of the entry's name will begin with the package name in
      // slash notation.
      String entryName = je.getName();
      if (entryName.startsWith(packageNameWithSlashes + SLASH)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks to see if the current directory contains the qual package through recursion currentDir
   * starts at the root directory (a directory passed in as part of the classpaths), the iterator
   * goes through each segment of the fully qualified package name (each segment is separated by a
   * dot).
   *
   * <p>Each step of the recursion checks to see if there's a subdirectory in the current directory
   * that has a name matching the package name segment, if so, it recursively descends into that
   * subdirectory to check the next package name segment
   *
   * <p>If there's no more segments left, then we've found the qual directory of interest
   *
   * <p>If we've checked every subdirectory and none of them match the current package name segment,
   * then the qual directory of interest does not exist in the given root directory (at the
   * beginning of recursion)
   *
   * @param currentDir current directory
   * @param pkgNames an iterator which provides each segment of the fully qualified qual package
   *     name
   * @return true if the qual package exists within the root directory, false otherwise
   */
  private final boolean checkDirForPackage(final File currentDir, final Iterator<String> pkgNames) {
    // if the iterator has no more package name segments, then we've found
    // the qual directory of interest
    if (!pkgNames.hasNext()) {
      return true;
    }
    // if the file doesn't exist or it isn't a directory, return false
    if (currentDir == null || !currentDir.isDirectory()) {
      return false;
    }

    // if it isn't empty, dequeue one segment of the fully qualified package name
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
   * directory.
   *
   * @param absolutePathToDirectory an absolute path to a directory
   * @return a URL reference to the directory, or null if the URL is malformed
   */
  private final @Nullable URL getDirectoryURL(final String absolutePathToDirectory) {
    URL directoryURL = null;

    try {
      directoryURL = new File(absolutePathToDirectory).toURI().toURL();
    } catch (MalformedURLException e) {
      processingEnv
          .getMessager()
          .printMessage(Kind.NOTE, "Directory URL " + absolutePathToDirectory + " is malformed");
    }

    return directoryURL;
  }

  /**
   * Given an absolute path to a jar file, this method will return a URL reference to that jar file.
   *
   * @param absolutePathToJarFile an absolute path to a jar file
   * @return a URL reference to the jar file, or null if the URL is malformed
   */
  private final @Nullable URL getJarURL(final String absolutePathToJarFile) {
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
   * variables, and by examining the classloader to see what paths it has access to.
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
    Set<String> paths = new LinkedHashSet<>();

    // add all extension paths
    String extdirs = System.getProperty("java.ext.dirs");
    if (extdirs != null && !extdirs.isEmpty()) {
      paths.addAll(Arrays.asList(extdirs.split(File.pathSeparator)));
    }

    // add all paths in CLASSPATH, -cp, and -classpath
    paths.addAll(Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)));

    // add all paths that are examined by the classloader
    if (classLoader != null) {
      URL[] urls = classLoader.getURLs();
      for (int i = 0; i < urls.length; i++) {
        paths.add(urls[i].getFile().toString());
      }
    }

    return Collections.unmodifiableSet(paths);
  }

  /**
   * Obtains the classloader used to load the checker class, if that isn't available then it will
   * try to obtain the system classloader.
   *
   * @return the classloader used to load the checker class, or the system classloader, or null if
   *     both are unavailable
   */
  private final @Nullable URLClassLoader getClassLoader() {
    ClassLoader result = InternalUtils.getClassLoaderForClass(checker.getClass());
    if (result instanceof URLClassLoader) {
      return (@Nullable URLClassLoader) result;
    } else {
      // Java 9+ use an internal classloader that doesn't support getting URLs. Ignore.
      return null;
    }
  }

  /** Debug Use: Displays all classpaths examined by the class loader. */
  @SuppressWarnings("unused") // for debugging
  protected final void printPaths() {
    // all paths in Xbootclasspath
    String[] bootclassPaths = System.getProperty("sun.boot.class.path").split(File.pathSeparator);
    processingEnv.getMessager().printMessage(Kind.NOTE, "bootclass path:");
    for (String path : bootclassPaths) {
      processingEnv.getMessager().printMessage(Kind.NOTE, "\t" + path);
    }

    // all extension paths
    String[] extensionDirs = System.getProperty("java.ext.dirs").split(File.pathSeparator);
    processingEnv.getMessager().printMessage(Kind.NOTE, "extension dirs:");
    for (String path : extensionDirs) {
      processingEnv.getMessager().printMessage(Kind.NOTE, "\t" + path);
    }

    // all paths in CLASSPATH, -cp, and -classpath
    processingEnv.getMessager().printMessage(Kind.NOTE, "java.class.path property:");
    for (String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
      processingEnv.getMessager().printMessage(Kind.NOTE, "\t" + path);
    }

    // add all paths that are examined by the classloader
    processingEnv.getMessager().printMessage(Kind.NOTE, "classloader examined paths:");
    if (classLoader != null) {
      URL[] urls = classLoader.getURLs();
      for (int i = 0; i < urls.length; i++) {
        processingEnv.getMessager().printMessage(Kind.NOTE, "\t" + urls[i].getFile());
      }
    } else {
      processingEnv.getMessager().printMessage(Kind.NOTE, "classloader unavailable");
    }
  }

  /**
   * Loads the set of annotation classes in the qual directory of a checker shipped with the Checker
   * Framework.
   */
  private void loadBundledAnnotationClasses() {
    // retrieve the fully qualified class names of the annotations
    Set<@BinaryName String> annotationNames;
    // see whether the resource URL has a protocol of jar or file
    if (resourceURL != null && resourceURL.getProtocol().contentEquals("jar")) {
      // if the checker class file is contained within a jar, then the resource URL for the qual
      // directory will have the protocol "jar". This means the whole checker is loaded as a jar
      // file.

      JarURLConnection connection;
      // create a connection to the jar file
      try {
        connection = (JarURLConnection) resourceURL.openConnection();

        // disable caching / connection sharing of the low level URLConnection to the Jar file
        connection.setDefaultUseCaches(false);
        connection.setUseCaches(false);

        // connect to the Jar file
        connection.connect();
      } catch (IOException e) {
        throw new BugInCF(
            "AnnotationClassLoader: cannot open a connection to the Jar file "
                + resourceURL.getFile());
      }

      // open up that jar file and extract annotation class names
      try (JarFile jarFile = connection.getJarFile()) {
        // get class names inside the jar file within the particular package
        annotationNames = getBundledAnnotationNamesFromJar(jarFile);
      } catch (IOException e) {
        throw new BugInCF(
            "AnnotationClassLoader: cannot open the Jar file " + resourceURL.getFile());
      }

    } else if (resourceURL != null && resourceURL.getProtocol().contentEquals("file")) {
      // If the checker class file is found within the file system itself within some directory
      // (usually development build directories), then process the package as a file directory in
      // the file system and load the annotations contained in the qual directory.

      // open up the directory
      File packageDir = new File(resourceURL.getFile());
      annotationNames = getAnnotationNamesFromDirectory(packageName, packageDir, packageDir);
    } else {
      // We do not support a resource URL with any other protocols, so create an empty set.
      annotationNames = Collections.emptySet();
    }
    if (annotationNames.isEmpty()) {
      PackageElement pkgEle = checker.getElementUtils().getPackageElement(packageName);
      if (pkgEle != null) {
        for (Element e : pkgEle.getEnclosedElements()) {
          if (e.getKind() == ElementKind.ANNOTATION_TYPE) {
            @SuppressWarnings("signature:assignment") // Elements needs to be annotated.
            @BinaryName String annoBinName =
                checker.getElementUtils().getBinaryName((TypeElement) e).toString();
            annotationNames.add(annoBinName);
          }
        }
      }
    }
    supportedBundledAnnotationClasses.addAll(loadAnnotationClasses(annotationNames));
  }

  /**
   * Gets the set of annotation classes in the qual directory of a checker shipped with the Checker
   * Framework. Note that the returned set from this method is mutable. This method is intended to
   * be called within {@link AnnotatedTypeFactory#createSupportedTypeQualifiers()
   * createSupportedTypeQualifiers()} (or its helper methods) to help define the set of supported
   * qualifiers.
   *
   * @see AnnotatedTypeFactory#createSupportedTypeQualifiers()
   * @return a mutable set of the loaded bundled annotation classes
   */
  public final Set<Class<? extends Annotation>> getBundledAnnotationClasses() {
    return supportedBundledAnnotationClasses;
  }

  /**
   * Retrieves the annotation class file names from the qual directory contained inside a jar.
   *
   * @param jar the JarFile containing the annotation class files
   * @return a set of fully qualified class names of the annotations
   */
  @SuppressWarnings("JdkObsolete")
  private final Set<@BinaryName String> getBundledAnnotationNamesFromJar(final JarFile jar) {
    Set<@BinaryName String> annos = new LinkedHashSet<>();

    // get an enumeration iterator for all the content entries in the jar file
    Enumeration<JarEntry> jarEntries = jar.entries();

    // enumerate through the entries
    while (jarEntries.hasMoreElements()) {
      JarEntry je = jarEntries.nextElement();
      // filter out directories and non-class files
      if (je.isDirectory() || !je.getName().endsWith(CLASS_SUFFIX)) {
        continue;
      }

      String className = Signatures.classfilenameToBinaryName(je.getName());

      // filter for qual package
      if (className.startsWith(packageName + DOT)) {
        // add to set
        annos.add(className);
      }
    }

    return annos;
  }

  /**
   * This method takes as input the canonical name of an external annotation class and loads and
   * returns that class via the class loader. This method returns null if the external annotation
   * class was loaded successfully but was deemed not supported by a checker. Errors are issued if
   * the external class is not an annotation, or if it could not be loaded successfully.
   *
   * @param annoName canonical name of an external annotation class, e.g.
   *     "myproject.qual.myannotation"
   * @return the loaded annotation class, or null if it was not a supported annotation as decided by
   *     {@link #isSupportedAnnotationClass(Class)}
   */
  public final @Nullable Class<? extends Annotation> loadExternalAnnotationClass(
      final @BinaryName String annoName) {
    return loadAnnotationClass(annoName, true);
  }

  /**
   * This method takes as input a fully qualified path to a directory, and loads and returns the set
   * of all supported annotation classes from that directory.
   *
   * @param dirName absolute path to a directory containing annotation classes
   * @return a set of annotation classes
   */
  public final Set<Class<? extends Annotation>> loadExternalAnnotationClassesFromDirectory(
      final String dirName) {
    File rootDirectory = new File(dirName);
    Set<@BinaryName String> annoNames =
        getAnnotationNamesFromDirectory(null, rootDirectory, rootDirectory);
    return loadAnnotationClasses(annoNames);
  }

  /**
   * Retrieves all annotation names from the current directory, and recursively descends and
   * retrieves annotation names from sub-directories.
   *
   * @param packageName the name of the package that contains the qual package, or null
   * @param rootDirectory a {@link File} object representing the root directory of a set of
   *     annotations, which is subtracted from class names to retrieve each class's fully qualified
   *     class names
   * @param currentDirectory a {@link File} object representing the current sub-directory of the
   *     root directory
   * @return a set fully qualified annotation class name, for annotations in the root directory or
   *     its sub-directories
   */
  @SuppressWarnings("signature") // TODO: reduce use of string manipulation
  private final Set<@BinaryName String> getAnnotationNamesFromDirectory(
      final @Nullable @DotSeparatedIdentifiers String packageName,
      final File rootDirectory,
      final File currentDirectory) {
    Set<@BinaryName String> results = new LinkedHashSet<>();

    // Full path to root directory
    String rootPath = rootDirectory.getAbsolutePath();

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
        // TODO: simplify all this string manipulation.

        // Full file name, including path to file
        String fullFileName = file.getAbsolutePath();
        // Simple file name
        String fileName =
            fullFileName.substring(
                fullFileName.lastIndexOf(File.separator) + 1, fullFileName.length());
        // Path to file
        String filePath = fullFileName.substring(0, fullFileName.lastIndexOf(File.separator));
        // Package name beginning with "qual"
        String qualPackage = null;
        if (!filePath.equals(rootPath)) {
          qualPackage =
              filePath.substring(rootPath.length() + 1, filePath.length()).replace(SLASH, DOT);
        }
        // Simple annotation name, which is the same as the file name (without directory)
        // but with file extension removed.
        @BinaryName String annotationName = fileName;
        if (fileName.lastIndexOf(DOT) != -1) {
          annotationName = fileName.substring(0, fileName.lastIndexOf(DOT));
        }

        // Fully qualified annotation class name (a @BinaryName, not a @FullyQualifiedName)
        @BinaryName String fullyQualifiedAnnoName =
            Signatures.addPackage(packageName, Signatures.addPackage(qualPackage, annotationName));

        if (fileName.endsWith(CLASS_SUFFIX)) {
          // add the fully qualified annotation class name to the set
          results.add(fullyQualifiedAnnoName);
        }
      } else if (file.isDirectory()) {
        // recursively add all sub directories's fully qualified annotation class name
        results.addAll(getAnnotationNamesFromDirectory(packageName, rootDirectory, file));
      }
    }

    return results;
  }

  /**
   * Loads the class indicated by the name, and checks to see if it is an annotation that is
   * supported by a checker.
   *
   * @param className the name of the class, in binary name format
   * @param issueError set to true to issue a warning when a loaded annotation is not a type
   *     annotation. It is useful to set this to true if a given annotation must be a well-defined
   *     type annotation (eg for annotation class names given as command line arguments). It should
   *     be set to false if the annotation is a meta-annotation or non-type annotation.
   * @return the loaded annotation class if it has a {@code @Target} meta-annotation with the
   *     required ElementType values, and is a supported annotation by a checker. If the annotation
   *     is not supported by a checker, null is returned.
   */
  protected final @Nullable Class<? extends Annotation> loadAnnotationClass(
      final @BinaryName String className, boolean issueError) {

    // load the class
    Class<?> cls = null;
    try {
      if (classLoader != null) {
        cls = Class.forName(className, true, classLoader);
      } else {
        cls = Class.forName(className);
      }
    } catch (ClassNotFoundException e) {
      throw new UserError(
          checker.getClass().getSimpleName()
              + ": could not load class for annotation: "
              + className
              + ". Ensure that it is a type annotation"
              + " and your classpath is correct.");
    }

    // If the freshly loaded class is not an annotation, then issue error if required and then
    // return null
    if (!cls.isAnnotation()) {
      if (issueError) {
        throw new UserError(
            checker.getClass().getSimpleName()
                + ": the loaded class: "
                + cls.getCanonicalName()
                + " is not a type annotation.");
      }
      return null;
    }

    Class<? extends Annotation> annoClass = cls.asSubclass(Annotation.class);
    // Check the loaded annotation to see if it has a @Target meta-annotation with the required
    // ElementType values
    if (hasWellDefinedTargetMetaAnnotation(annoClass)) {
      // If so, return the loaded annotation if it is supported by a checker
      return isSupportedAnnotationClass(annoClass) ? annoClass : null;
    } else if (issueError) {
      // issueError is set to true for loading explicitly named external annotations.
      // We issue an error here when one of those annotations is not well-defined, since the
      // user expects these external annotations to be loaded.
      throw new UserError(
          checker.getClass().getSimpleName()
              + ": the loaded annotation: "
              + annoClass.getCanonicalName()
              + " is not a type annotation."
              + " Check its @Target meta-annotation.");
    } else {
      // issueError is set to false for loading the qual directory or any external directories.
      // We don't issue any errors since there may be meta-annotations or non-type annotations
      // in such directories.
      return null;
    }
  }

  /**
   * Loads a set of annotations indicated by their names.
   *
   * @param annoNames a set of binary names for annotation classes
   * @return a set of loaded annotation classes
   * @see #loadAnnotationClass(String, boolean)
   */
  protected final Set<Class<? extends Annotation>> loadAnnotationClasses(
      final @Nullable Set<@BinaryName String> annoNames) {
    Set<Class<? extends Annotation>> loadedClasses = new LinkedHashSet<>();

    if (annoNames != null && !annoNames.isEmpty()) {
      // loop through each class name & load the class
      for (String annoName : annoNames) {
        Class<? extends Annotation> annoClass = loadAnnotationClass(annoName, false);
        if (annoClass != null) {
          loadedClasses.add(annoClass);
        }
      }
    }

    return loadedClasses;
  }

  /**
   * Checks to see whether a particular annotation class has the {@link Target} meta-annotation, and
   * has the required {@link ElementType} values.
   *
   * <p>A subclass may override this method to load annotations that are not intended to be
   * annotated in source code. E.g.: {@code SubtypingChecker} overrides this method to load {@code
   * Unqualified}.
   *
   * @param annoClass an annotation class
   * @return true if the annotation is well defined, false if it isn't
   */
  protected boolean hasWellDefinedTargetMetaAnnotation(
      final Class<? extends Annotation> annoClass) {
    return annoClass.getAnnotation(Target.class) != null
        && AnnotationUtils.hasTypeQualifierElementTypes(
            annoClass.getAnnotation(Target.class).value(), annoClass);
  }

  /**
   * Checks to see whether a particular annotation class is supported.
   *
   * <p>By default, all loaded annotations that pass the basic checks in {@link
   * #loadAnnotationClass(String, boolean)} are supported.
   *
   * <p>Individual checkers can create a subclass of AnnotationClassLoader and override this method
   * to indicate whether a particular annotation is supported.
   *
   * @param annoClass an annotation class
   * @return true if the annotation is supported, false if it isn't
   */
  protected boolean isSupportedAnnotationClass(final Class<? extends Annotation> annoClass) {
    return true;
  }
}
