package org.checkerframework.framework.test;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.checkerframework.javacutil.PluginUtil;
import org.junit.Assert;

public class TestUtilities {

    public static final boolean IS_AT_LEAST_9_JVM;

    static {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        OutputStream err = new ByteArrayOutputStream();
        compiler.run(null, null, err, "-version");
        IS_AT_LEAST_9_JVM = PluginUtil.getJreVersion() >= 1.9d;
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
     * Returns a list where each item is a list of Java files, excluding any skip tests, for each
     * subdirectory of {@code dir} and also a list of Java files in dir.
     *
     * @param dir directory in which to search for Java files
     * @return a list of list of Java test files
     */
    private static List<List<File>> findJavaTestFilesInDirectory(File dir) {
        assert dir.isDirectory();
        List<List<File>> fileGroupedByDirectory = new ArrayList<>();
        List<File> fileInDir = new ArrayList<>();

        fileGroupedByDirectory.add(fileInDir);
        for (String fileName : dir.list()) {
            File file = new File(dir, fileName);
            if (file.isDirectory()) {
                fileGroupedByDirectory.addAll(findJavaTestFilesInDirectory(file));
            } else if (isJavaTestFile(file)) {
                fileInDir.add(file);
            }
        }
        if (fileInDir.isEmpty()) {
            fileGroupedByDirectory.remove(fileInDir);
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

        File[] in = directory.listFiles();
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
                    || (!IS_AT_LEAST_9_JVM && nextLine.contains("@below-java9-jdk-skip-test"))) {
                in.close();
                return false;
            }
        }

        in.close();
        return true;
    }

    public static String diagnosticToString(
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
                if (result.contains("\n")) {
                    firstLine = result.substring(0, result.indexOf('\n'));
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

    public static String summarizeSourceFiles(List<File> javaFiles) {
        StringBuilder listStrBuilder = new StringBuilder();

        boolean first = true;
        for (File file : javaFiles) {
            if (first) {
                first = false;
            } else {
                listStrBuilder.append(", ");
            }
            listStrBuilder.append(file.getAbsolutePath());
        }

        return listStrBuilder.toString();
    }

    public static File getTestFile(String fileRelativeToTestsDir) {
        return new File("tests", fileRelativeToTestsDir);
    }

    public static File findComparisonFile(File testFile) {
        final File comparisonFile =
                new File(testFile.getParent(), testFile.getName().replace(".java", ".out"));
        return comparisonFile;
    }

    public static List<String> optionMapToList(Map<String, String> options) {
        List<String> optionList = new ArrayList<>(options.size() * 2);

        for (Entry<String, String> opt : options.entrySet()) {
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
        try {
            final BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            bw.write("File: " + testFile.getAbsolutePath() + "\n");
            bw.write("TestFailed: " + testFailed + "\n");
            bw.write("Using nomsgtxt: " + usingNoMsgText + "\n");
            bw.write(
                    "#Missing: "
                            + missing.size()
                            + "      #Unexpected: "
                            + unexpected.size()
                            + "\n");

            bw.write("Expected:\n");
            bw.write(String.join("\n", expected));
            bw.newLine();

            bw.write("Actual:\n");
            bw.write(String.join("\n", actual));
            bw.newLine();

            bw.write("Missing:\n");
            bw.write(String.join("\n", missing));
            bw.newLine();

            bw.write("Unexpected:\n");
            bw.write(String.join("\n", unexpected));
            bw.newLine();

            bw.newLine();
            bw.newLine();
            bw.flush();
            bw.close();

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
        try {
            final BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            bw.write("Files:\n");
            for (JavaFileObject f : files) {
                bw.write("    " + f.getName());
                bw.newLine();
            }
            bw.newLine();

            bw.write("Options:\n");
            for (String o : options) {
                bw.write("    " + o);
                bw.newLine();
            }
            bw.newLine();

            bw.write("Processors:\n");
            for (String p : processors) {
                bw.write("    " + p);
                bw.newLine();
            }
            bw.newLine();
            bw.newLine();

            bw.flush();
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO: REDO COMMENT Compares the result of the compiler against an array of Strings.
     *
     * <p>In a checker, we treat a more specific error message as subsumed by a general one. For
     * example, "new.array.type.invalid" is subsumed by "type.invalid". This is not the case in the
     * test framework; the exact error key is expected.
     */
    public static void assertResultsAreValid(TypecheckResult testResult) {
        if (testResult.didTestFail()) {
            Assert.fail(testResult.summarize());
        }
    }

    public static void ensureDirectoryExists(File path) {
        if (!path.exists()) {
            if (!path.mkdirs()) {
                throw new RuntimeException("Could not make directory: " + path.getAbsolutePath());
            }
        }
    }

    public static boolean testBooleanProperty(String propName) {
        return testBooleanProperty(propName, false);
    }

    public static boolean testBooleanProperty(String propName, boolean defaultValue) {
        return PluginUtil.getBooleanSystemProperty(propName, defaultValue);
    }

    public static boolean getShouldEmitDebugInfo() {
        return PluginUtil.getBooleanSystemProperty("emit.test.debug");
    }
}
