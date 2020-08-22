package org.checkerframework.framework.test;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringJoiner;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.SystemUtil;
import org.junit.Assert;
import org.plumelib.util.UtilPlume;

/** Utilities for testing. */
public class TestUtilities {

    public static final boolean IS_AT_LEAST_9_JVM;
    public static final boolean IS_AT_LEAST_11_JVM;

    static {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        OutputStream err = new ByteArrayOutputStream();
        compiler.run(null, null, err, "-version");
        IS_AT_LEAST_9_JVM = SystemUtil.getJreVersion() >= 9;
        IS_AT_LEAST_11_JVM = SystemUtil.getJreVersion() >= 11;
    }

    public static List<File> findNestedJavaTestFiles(String... dirNames) {
        return findRelativeNestedJavaFiles(new File("tests"), dirNames);
    }

    public static List<File> findRelativeNestedJavaFiles(String parent, String... dirNames) {
        return findRelativeNestedJavaFiles(new File(parent), dirNames);
    }

    public static List<File> findRelativeNestedJavaFiles(File parent, String... dirNames) {
        File[] dirs = new File[dirNames.length];

        int i = 0;
        for (String dirName : dirNames) {
            dirs[i] = new File(parent, dirName);
            i += 1;
        }

        return getJavaFilesAsArgumentList(dirs);
    }

    /**
     * Returns a list where each item is a list of Java files, excluding any skip tests, for each
     * directory given by dirName and also a list for any subdirectory.
     *
     * @param parent parent directory of the dirNames directories
     * @param dirNames names of directories to search
     * @return list where each item is a list of Java test files grouped by directory
     */
    public static List<List<File>> findJavaFilesPerDirectory(File parent, String... dirNames) {
        List<List<File>> filesPerDirectory = new ArrayList<>();

        for (String dirName : dirNames) {
            File dir = new File(parent, dirName);
            if (dir.isDirectory()) {
                filesPerDirectory.addAll(findJavaTestFilesInDirectory(dir));
            }
        }

        return filesPerDirectory;
    }

    /**
     * Returns a list where each item is a list of Java files, excluding any skip tests. There is
     * one list for {@code dir}, and one list for each subdirectory of {@code dir}.
     *
     * @param dir directory in which to search for Java files
     * @return a list of list of Java test files
     */
    private static List<List<File>> findJavaTestFilesInDirectory(File dir) {
        List<List<File>> fileGroupedByDirectory = new ArrayList<>();
        List<File> filesInDir = new ArrayList<>();

        fileGroupedByDirectory.add(filesInDir);
        String[] dirContents = dir.list();
        if (dirContents == null) {
            throw new Error("Not a directory: " + dir);
        }
        Arrays.sort(dirContents);
        for (String fileName : dirContents) {
            File file = new File(dir, fileName);
            if (file.isDirectory()) {
                fileGroupedByDirectory.addAll(findJavaTestFilesInDirectory(file));
            } else if (isJavaTestFile(file)) {
                filesInDir.add(file);
            }
        }
        if (filesInDir.isEmpty()) {
            fileGroupedByDirectory.remove(filesInDir);
        }
        return fileGroupedByDirectory;
    }

    public static List<Object[]> findFilesInParent(File parent, String... fileNames) {
        List<Object[]> files = new ArrayList<>();
        for (String fileName : fileNames) {
            files.add(new Object[] {new File(parent, fileName)});
        }
        return files;
    }

    /**
     * Traverses the directories listed looking for Java test files.
     *
     * @param dirs directories in which to search for Java test files
     * @return a list of Java test files found in the directories
     */
    public static List<File> getJavaFilesAsArgumentList(File... dirs) {
        List<File> arguments = new ArrayList<>();
        for (File dir : dirs) {
            List<File> javaFiles = deeplyEnclosedJavaTestFiles(dir);

            for (File javaFile : javaFiles) {
                arguments.add(javaFile);
            }
        }
        return arguments;
    }

    /** Returns all the java files that are descendants of the given directory. */
    public static List<File> deeplyEnclosedJavaTestFiles(File directory) {
        if (!directory.exists()) {
            throw new IllegalArgumentException(
                    "directory does not exist: " + directory + " " + directory.getAbsolutePath());
        }
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("found file instead of directory: " + directory);
        }

        List<File> javaFiles = new ArrayList<>();

        @SuppressWarnings("nullness") // checked above that it's a directory
        File @NonNull [] in = directory.listFiles();
        Arrays.sort(
                in,
                new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
        for (File file : in) {
            if (file.isDirectory()) {
                javaFiles.addAll(deeplyEnclosedJavaTestFiles(file));
            } else if (isJavaTestFile(file)) {
                javaFiles.add(file);
            }
        }

        return javaFiles;
    }

    public static boolean isJavaFile(File file) {
        return file.isFile() && file.getName().endsWith(".java");
    }

    public static boolean isJavaTestFile(File file) {
        if (!isJavaFile(file)) {
            return false;
        }

        // We could implement special filtering based on directory names,
        // but I prefer using @below-java9-jdk-skip-test
        // if (!IS_AT_LEAST_9_JVM && file.getAbsolutePath().contains("java9")) {
        //     return false;
        // }

        Scanner in = null;
        try {
            in = new Scanner(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        while (in.hasNext()) {
            String nextLine = in.nextLine();
            if (nextLine.contains("@skip-test")
                    || (!IS_AT_LEAST_9_JVM && nextLine.contains("@below-java9-jdk-skip-test"))
                    || (!IS_AT_LEAST_11_JVM && nextLine.contains("@below-java11-jdk-skip-test"))) {
                in.close();
                return false;
            }
        }

        in.close();
        return true;
    }

    public static @Nullable String diagnosticToString(
            final Diagnostic<? extends JavaFileObject> diagnostic, boolean usingAnomsgtxt) {

        String result = diagnostic.toString().trim();

        // suppress Xlint warnings
        if (result.contains("uses unchecked or unsafe operations.")
                || result.contains("Recompile with -Xlint:unchecked for details.")
                || result.endsWith(" declares unsafe vararg methods.")
                || result.contains("Recompile with -Xlint:varargs for details.")) {
            return null;
        }

        if (usingAnomsgtxt) {
            // Lines with "unexpected Throwable" are stack traces
            // and should be printed in full.
            if (!result.contains("unexpected Throwable")) {
                String firstLine;
                if (result.contains(System.lineSeparator())) {
                    firstLine = result.substring(0, result.indexOf(System.lineSeparator()));
                } else {
                    firstLine = result;
                }
                if (firstLine.contains(".java:")) {
                    firstLine = firstLine.substring(firstLine.indexOf(".java:") + 5).trim();
                }
                result = firstLine;
            }
        }

        return result;
    }

    public static Set<String> diagnosticsToStrings(
            final Iterable<Diagnostic<? extends JavaFileObject>> actualDiagnostics,
            boolean usingAnomsgtxt) {
        Set<String> actualDiagnosticsStr = new LinkedHashSet<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : actualDiagnostics) {
            String diagnosticStr = TestUtilities.diagnosticToString(diagnostic, usingAnomsgtxt);
            if (diagnosticStr != null) {
                actualDiagnosticsStr.add(diagnosticStr);
            }
        }

        return actualDiagnosticsStr;
    }

    /**
     * Return the file absolute pathnames, separated by commas.
     *
     * @param javaFiles a list of Java files
     * @return the file absolute pathnames, separated by commas
     */
    public static String summarizeSourceFiles(List<File> javaFiles) {
        StringJoiner sj = new StringJoiner(", ");
        for (File file : javaFiles) {
            sj.add(file.getAbsolutePath());
        }
        return sj.toString();
    }

    public static File getTestFile(String fileRelativeToTestsDir) {
        return new File("tests", fileRelativeToTestsDir);
    }

    public static File findComparisonFile(File testFile) {
        final File comparisonFile =
                new File(testFile.getParent(), testFile.getName().replace(".java", ".out"));
        return comparisonFile;
    }

    public static List<String> optionMapToList(Map<String, @Nullable String> options) {
        List<String> optionList = new ArrayList<>(options.size() * 2);

        for (Map.Entry<String, @Nullable String> opt : options.entrySet()) {
            optionList.add(opt.getKey());

            if (opt.getValue() != null) {
                optionList.add(opt.getValue());
            }
        }

        return optionList;
    }

    public static void writeLines(File file, Iterable<?> lines) {
        try {
            final BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            Iterator<?> iter = lines.iterator();
            while (iter.hasNext()) {
                Object next = iter.next();
                if (next == null) {
                    bw.write("<null>");
                } else {
                    bw.write(next.toString());
                }
                bw.newLine();
            }
            bw.flush();
            bw.close();

        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }

    public static void writeDiagnostics(
            File file,
            File testFile,
            List<String> expected,
            List<String> actual,
            List<String> unexpected,
            List<String> missing,
            boolean usingNoMsgText,
            boolean testFailed) {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
            pw.println("File: " + testFile.getAbsolutePath());
            pw.println("TestFailed: " + testFailed);
            pw.println("Using nomsgtxt: " + usingNoMsgText);
            pw.println("#Missing: " + missing.size() + "      #Unexpected: " + unexpected.size());

            pw.println("Expected:");
            pw.println(UtilPlume.joinLines(expected));
            pw.println();

            pw.println("Actual:");
            pw.println(UtilPlume.joinLines(actual));
            pw.println();

            pw.println("Missing:");
            pw.println(UtilPlume.joinLines(missing));
            pw.println();

            pw.println("Unexpected:");
            pw.println(UtilPlume.joinLines(unexpected));
            pw.println();

            pw.println();
            pw.println();
            pw.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeTestConfiguration(File file, TestConfiguration config) {
        try {
            final BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            bw.write(config.toString());
            bw.newLine();
            bw.newLine();
            bw.flush();
            bw.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeJavacArguments(
            File file,
            Iterable<? extends JavaFileObject> files,
            Iterable<String> options,
            Iterable<String> processors) {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
            pw.println("Files:");
            for (JavaFileObject f : files) {
                pw.println("    " + f.getName());
            }
            pw.println();

            pw.println("Options:");
            for (String o : options) {
                pw.println("    " + o);
            }
            pw.println();

            pw.println("Processors:");
            for (String p : processors) {
                pw.println("    " + p);
            }
            pw.println();
            pw.println();

            pw.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO: REDO COMMENT Compares the result of the compiler against an array of Strings.
     *
     * <p>In a checker, a more specific error message is subsumed by a general one. For example,
     * "new.array.type.invalid" is subsumed by "type.invalid". This is not the case in the test
     * framework, which must use the exact error message key.
     *
     * @param testResult the result of type-checking
     */
    public static void assertResultsAreValid(TypecheckResult testResult) {
        if (testResult.didTestFail()) {
            if (getShouldEmitDebugInfo()) {
                System.out.println("---------------- start of javac ouput ----------------");
                System.out.println(testResult.getCompilationResult().getJavacOutput());
                System.out.println("---------------- end of javac ouput ----------------");
            }
            Assert.fail(testResult.summarize());
        }
    }

    /**
     * Create the directory (and its parents) if it does not exist.
     *
     * @param dir the directory to create
     */
    public static void ensureDirectoryExists(String dir) {
        try {
            Files.createDirectories(Paths.get(dir));
        } catch (FileAlreadyExistsException e) {
            // directory already exists
        } catch (IOException e) {
            throw new RuntimeException("Could not make directory: " + dir + ": " + e.getMessage());
        }
    }

    /**
     * Return true if the system property is set to "true". Return false if the system property is
     * not set or is set to "false". Otherwise, errs.
     *
     * @param key system property to check
     * @return true if the system property is set to "true". Return false if the system property is
     *     not set or is set to "false". Otherwise, errs.
     * @deprecated Use {@link SystemUtil#getBooleanSystemProperty(String)} instead.
     */
    @Deprecated
    public static boolean testBooleanProperty(String key) {
        return testBooleanProperty(key, false);
    }

    /**
     * If the system property is set, return its boolean value; otherwise return {@code
     * defaultValue}. Errs if the system property is set to a non-boolean value.
     *
     * @param key system property to check
     * @param defaultValue value to use if the property is not set
     * @return the boolean value of {@code key} or {@code defaultValue} if {@code key} is not set
     * @deprecated Use {@link SystemUtil#getBooleanSystemProperty(String, boolean)} instead.
     */
    @Deprecated
    public static boolean testBooleanProperty(String key, boolean defaultValue) {
        return SystemUtil.getBooleanSystemProperty(key, defaultValue);
    }

    /**
     * Returns the value of system property "emit.test.debug".
     *
     * @return the value of system property "emit.test.debug"
     */
    public static boolean getShouldEmitDebugInfo() {
        return SystemUtil.getBooleanSystemProperty("emit.test.debug");
    }
}
