package java.io;
import checkers.javari.quals.*;

import java.util.Arrays;

public class StreamTokenizer {

    private Reader reader = null;
    private InputStream input = null;

    private char buf[] = new char[20];

    private int peekc = NEED_CHAR;

    private static final int NEED_CHAR = Integer.MAX_VALUE;
    private static final int SKIP_LF = Integer.MAX_VALUE - 1;

    private boolean pushedBack;
    private boolean forceLower;
    private int LINENO = 1;

    private boolean eolIsSignificantP = false;
    private boolean slashSlashCommentsP = false;
    private boolean slashStarCommentsP = false;

    private byte ctype[] = new byte[256];
    private static final byte CT_WHITESPACE = 1;
    private static final byte CT_DIGIT = 2;
    private static final byte CT_ALPHA = 4;
    private static final byte CT_QUOTE = 8;
    private static final byte CT_COMMENT = 16;

    public int ttype = TT_NOTHING;
    public static final int TT_EOF = -1;
    public static final int TT_EOL = '\n';
    public static final int TT_NUMBER = -2;
    public static final int TT_WORD = -3;
    private static final int TT_NOTHING = -4;
    public String sval;
    public double nval;


    private StreamTokenizer() {
        throw new RuntimeException("skeleton method");
    }

    @Deprecated
    public StreamTokenizer(InputStream is) {
        throw new RuntimeException("skeleton method");
    }

    public StreamTokenizer( Reader r) {
        throw new RuntimeException("skeleton method");
    }

    public void resetSyntax() {
        throw new RuntimeException("skeleton method");
    }

    public void wordChars(int low, int hi) {
        throw new RuntimeException("skeleton method");
    }

    public void whitespaceChars(int low, int hi) {
        throw new RuntimeException("skeleton method");
    }

    public void ordinaryChars(int low, int hi) {
        throw new RuntimeException("skeleton method");
    }

    public void ordinaryChar(int ch) {
        throw new RuntimeException("skeleton method");
    }

    public void commentChar(int ch) {
        throw new RuntimeException("skeleton method");
    }

    public void quoteChar(int ch) {
        throw new RuntimeException("skeleton method");
    }

    public void parseNumbers() {
        throw new RuntimeException("skeleton method");
    }

    public void eolIsSignificant(boolean flag) {
        throw new RuntimeException("skeleton method");
    }

    public void slashStarComments(boolean flag) {
        throw new RuntimeException("skeleton method");
    }

    public void slashSlashComments(boolean flag) {
        throw new RuntimeException("skeleton method");
    }

    public void lowerCaseMode(boolean fl) {
        throw new RuntimeException("skeleton method");
    }

    private int read() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public int nextToken() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void pushBack() {
        throw new RuntimeException("skeleton method");
    }

    public int lineno() {
        throw new RuntimeException("skeleton method");
    }

    public String toString() {
        throw new RuntimeException("skeleton method");
    }
}
