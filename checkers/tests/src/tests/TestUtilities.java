package tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
     * Returns true if the java file is a java file without
     * that tag {@code @ignore} in the declaration comment of
     * the file
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
            if (nextLine.contains("@ignore"))
                return false;
            if (nextLine.contains("class")
                    || nextLine.contains("interface")
                    || nextLine.contains("enum"))
                break;
        }
        return true;
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
}
