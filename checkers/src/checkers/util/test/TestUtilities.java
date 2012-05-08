package checkers.util.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class TestUtilities {

    private TestUtilities() {
        throw new AssertionError("not instantiated class");
    }

    /**
     * Returns true if the file is a file ending with {@code .java}
     */
    public static boolean isJavaFile(File file) {
        return file.isFile() && file.getName().endsWith(".java");
    }

    /**
     * Checks if the given file is a java test file not to be ignored.
     *
     * Returns true if it is a file and does not contain
     * {@code @skip-test} in the declaration comment of the file.
     */
    public static boolean isJavaTestFile(File file) {
        if (!isJavaFile(file))
            return false;
        Scanner in = null;
        try {
            in = new Scanner(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        boolean seenKeyword = false;
        while (in.hasNext()) {
            String nextLine = in.nextLine();
            if (nextLine.contains("@skip-test"))
                return false;
            if (nextLine.contains("class")
                    || nextLine.contains("interface")
                    || nextLine.contains("enum"))
                seenKeyword = true;
        }
        return seenKeyword;
    }

    /**
     * Returns true if the compilation associated with the given expected
     * output should succeed without any errors.
     *
     * In particular, it returns true if the expected file doesn't exist,
     * or all the found errors are warnings.
     */
    public static boolean shouldSucceed(File expectedFile) {
        if (!expectedFile.exists())
            return true;
        // Check if expectedFile has any errors
        try {
            Scanner in = new Scanner(new FileReader(expectedFile));
            while (in.hasNextLine()) {
                if (!in.nextLine().contains("warning"))
                    return false;
            }
            return true;
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * Returns all the java files that are direct children of the given
     * directory
     */
    public static List<File> enclosedJavaTestFiles(File directory) {
        if (!directory.isDirectory())
            throw new IllegalArgumentException("file not directory: " + directory);

        List<File> javaFiles = new ArrayList<File>();

        for (File file : directory.listFiles()) {
            if (isJavaTestFile(file))
                javaFiles.add(file);
        }

        return javaFiles;
    }

    /**
     * Returns all the java files that are descendants of the given directory
     */
    public static List<File> deeplyEnclosedJavaTestFiles(File directory) {
        if (!directory.isDirectory())
            throw new IllegalArgumentException("file not directory: " + directory);

        List<File> javaFiles = new ArrayList<File>();

        for (File file : directory.listFiles()) {
            if (file.isDirectory())
                javaFiles.addAll(deeplyEnclosedJavaTestFiles(file));
            else if (isJavaTestFile(file))
                javaFiles.add(file);
        }

        return javaFiles;
    }

    public static List<String> expectedDiagnostics(File file) {
        List<String> expected = new ArrayList<String>();

        try {
            LineNumberReader reader = new LineNumberReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("//::")) {
                    int errorLine = reader.getLineNumber() + 1;
                    // drop the //::
                    line = line.substring(4);
                    String[] msgs = line.split("::");
                    for (String msg : msgs) {
                        // The trim removes spaces before and after the message.
                        // This allows us to write "//:: A :: B
                        // But it prevents us to check on leading spaces in messages.
                        // I think that's OK, as we're always testing against "(codes)".
                        msg = ":" + errorLine + ": " + msg.trim();
                        expected.add(msg);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return expected;
    }

    public static List<String> expectedDiagnostics(String prefix, String[] files) {
        List<String> expected = new ArrayList<String>();

        for (String file : files)
            expected.addAll(expectedDiagnostics(new File(prefix + file)));

        return expected;
    }

    public static List<String> expectedDiagnostics(String prefix, File[] files) {
        List<String> expected = new ArrayList<String>();

        for (File file : files)
            expected.addAll(expectedDiagnostics(file));

        return expected;
    }

}
