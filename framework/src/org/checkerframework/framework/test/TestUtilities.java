package org.checkerframework.framework.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

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
     * Checks if the given file is a Java test file not to be ignored.
     *
     * Returns true if {@code file} is a {@code .java} file and
     * it does not contain {@code @skip-test} anywhere in the file.
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
        while (in.hasNext()) {
            String nextLine = in.nextLine();
            if (nextLine.contains("@skip-test") ||
                    (!isJSR308Compiler && nextLine.contains("@non-308-skip-test"))) {
                in.close();
                return false;
            }
        }
        in.close();
        return true;
    }

    public static final boolean isJSR308Compiler;
    static {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        OutputStream err = new ByteArrayOutputStream();
        compiler.run(null, null, err, "-version");
        isJSR308Compiler = err.toString().contains("jsr308");
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
                if (!in.nextLine().contains("warning")) {
                    in.close();
                    return false;
                }
            }
            in.close();
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
        if (!directory.exists())
            throw new IllegalArgumentException("directory does not exist: " + directory);
        if (!directory.isDirectory())
            throw new IllegalArgumentException("found file instead of directory: " + directory);

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
        if (!directory.exists())
            throw new IllegalArgumentException("directory does not exist: " + directory);
        if (!directory.isDirectory())
            throw new IllegalArgumentException("found file instead of directory: " + directory);

        List<File> javaFiles = new ArrayList<File>();

        File[] in = directory.listFiles();
        Arrays.sort(in, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (File file : in) {
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
                } else if(line.startsWith("//warning:")) {
                    // com.sun.tools.javac.util.AbstractDiagnosticFormatter.formatKind(JCDiagnostic, Locale)
                    // These are warnings from javax.tools.Diagnostic.Kind.WARNING
                    String msg = line.substring(2);
                    expected.add(msg);
                }
            }
            reader.close();
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

    public static List<String> expectedDiagnostics(File[] files) {
        List<String> expected = new ArrayList<String>();

        for (File file : files)
            expected.addAll(expectedDiagnostics(file));

        return expected;
    }

}
