/*
 * Created on 30/06/2008
 */
package org.checkerframework.stubparser.ast.test;

import org.checkerframework.stubparser.JavaParser;
import org.checkerframework.stubparser.ParseException;
import org.checkerframework.stubparser.ast.CompilationUnit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;

/**
 * @author Julio Vilmar Gesser
 */
final class Helper {

    private Helper() {
        // hide the constructor
    }

    private static File getFile(String sourceFolder, Class<?> clazz) {
        return new File(sourceFolder, clazz.getName().replace('.', '/') + ".java");
    }

    public static CompilationUnit parserClass(String sourceFolder, Class<?> clazz) throws ParseException {
        try {
            return JavaParser.parse(getFile(sourceFolder, clazz));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CompilationUnit parserString(String source) throws ParseException {
        return JavaParser.parse(new StringBufferInputStream(source));
    }

    public static String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        try {
            StringBuilder ret = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                ret.append(line);
                ret.append("\n");
            }
            return ret.toString();
        } finally {
            reader.close();
        }
    }

    public static String readClass(String sourceFolder, Class<?> clazz) throws IOException {
        return readFile(getFile(sourceFolder, clazz));
    }

}
