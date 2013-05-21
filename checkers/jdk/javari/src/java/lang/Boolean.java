package java.lang;
import checkers.javari.quals.*;

public final @ReadOnly class Boolean implements java.io.Serializable,
                                      Comparable<Boolean>
{
    private static final long serialVersionUID = 0L;
    public static final Boolean TRUE = new Boolean(true);
    public static final Boolean FALSE = new Boolean(false);
    public static final Class<Boolean> TYPE = null;

    public Boolean(boolean value) {
        throw new RuntimeException("skeleton method");
    }

    public Boolean(String s) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean parseBoolean(String s) {
        throw new RuntimeException("skeleton method");
    }

    public boolean booleanValue() {
        throw new RuntimeException("skeleton method");
    }

    public static Boolean valueOf(boolean b) {
        throw new RuntimeException("skeleton method");
    }

    public static Boolean valueOf(String s) {
        throw new RuntimeException("skeleton method");
    }

    public static String toString(boolean b) {
        throw new RuntimeException("skeleton method");
    }

    public String toString() {
        throw new RuntimeException("skeleton method");
    }

    public int hashCode() {
        throw new RuntimeException("skeleton method");
    }

    public boolean equals(@ReadOnly Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean getBoolean(String name) {
        throw new RuntimeException("skeleton method");
    }

    public int compareTo(Boolean b) {
        throw new RuntimeException("skeleton method");
    }

    private static boolean toBoolean(String name) {
        throw new RuntimeException("skeleton method");
    }
}
