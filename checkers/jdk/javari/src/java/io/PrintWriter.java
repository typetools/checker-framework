package java.io;

import java.util.Formatter;
import java.util.Locale;

import checkers.javari.quals.*;

public class PrintWriter extends Writer {

    protected Writer out;

    private boolean autoFlush = false;
    private boolean trouble = false;
    private Formatter formatter;
    private PrintStream psOut = null;

    private String lineSeparator;

    public PrintWriter (Writer out) {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter(Writer out,
               boolean autoFlush) {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter(OutputStream out) {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter(OutputStream out, boolean autoFlush) {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter(String fileName) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter(String fileName, String csn)
    throws FileNotFoundException, UnsupportedEncodingException {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter(File file) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter(File file, String csn)
    throws FileNotFoundException, UnsupportedEncodingException {
        throw new RuntimeException("skeleton method");
    }

    private void ensureOpen() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void flush() {
        throw new RuntimeException("skeleton method");
    }

    public void close() {
        throw new RuntimeException("skeleton method");
    }

    public boolean checkError() {
        throw new RuntimeException("skeleton method");
    }

    protected void setError() {
        throw new RuntimeException("skeleton method");
    }

    protected void clearError() {
        throw new RuntimeException("skeleton method");
    }

    public void write(int c) {
        throw new RuntimeException("skeleton method");
    }

    public void write(char @ReadOnly [] buf, int off, int len) {
        throw new RuntimeException("skeleton method");
    }

    public void write(char @ReadOnly [] buf) {
        throw new RuntimeException("skeleton method");
    }

    public void write(String s, int off, int len) {
        throw new RuntimeException("skeleton method");
    }

    public void write(String s) {
        throw new RuntimeException("skeleton method");
    }

    private void newLine() {
        throw new RuntimeException("skeleton method");
    }

    public void print(boolean b) {
        throw new RuntimeException("skeleton method");
    }

    public void print(char c) {
        throw new RuntimeException("skeleton method");
    }

    public void print(int i) {
        throw new RuntimeException("skeleton method");
    }

    public void print(long l) {
        throw new RuntimeException("skeleton method");
    }

    public void print(float f) {
        throw new RuntimeException("skeleton method");
    }

    public void print(double d) {
        throw new RuntimeException("skeleton method");
    }

    public void print(char @ReadOnly [] s) {
        throw new RuntimeException("skeleton method");
    }

    public void print(String s) {
        throw new RuntimeException("skeleton method");
    }

    public void print(@ReadOnly Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public void println() {
        throw new RuntimeException("skeleton method");
    }

    public void println(boolean x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(char x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(int x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(long x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(float x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(double x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(char @ReadOnly [] x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(String x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(@ReadOnly Object x) {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter printf(String format, @ReadOnly Object ... args) {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter printf(Locale l, String format, @ReadOnly Object ... args) {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter format(String format, @ReadOnly Object ... args) {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter format(Locale l, String format, @ReadOnly Object ... args) {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter append(@ReadOnly CharSequence csq) {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter append(@ReadOnly CharSequence csq, int start, int end) {
        throw new RuntimeException("skeleton method");
    }

    public PrintWriter append(char c) {
        throw new RuntimeException("skeleton method");
    }
}
