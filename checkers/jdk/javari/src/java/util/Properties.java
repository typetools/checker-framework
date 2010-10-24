package java.util;
import checkers.javari.quals.*;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class Properties extends Hashtable<Object,Object> {
    private static final long serialVersionUID = 4112578634029874840L;

    protected Properties defaults;

    public Properties() {
        throw new RuntimeException("skeleton method");
    }

    public Properties(Properties defaults) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized Object setProperty(String key, String value) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void load(Reader reader) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void load(InputStream inStream) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    @Deprecated
    public synchronized void save(OutputStream out, String comments) @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public void store(Writer writer, String comments) @ReadOnly throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void store(OutputStream out, String comments) @ReadOnly throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void loadFromXML(InputStream in)
        throws IOException, InvalidPropertiesFormatException
    {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void storeToXML(OutputStream os, String comment) @ReadOnly throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void storeToXML(OutputStream os, String comment, String encoding) @ReadOnly
        throws IOException
    {
        throw new RuntimeException("skeleton method");
    }

    public String getProperty(String key) @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public String getProperty(String key, String defaultValue) @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public Enumeration<?> propertyNames() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public Set<String> stringPropertyNames() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public void list(PrintStream out) @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public void list(PrintWriter out) @ReadOnly {
        throw new RuntimeException("skeleton method");
    }
}
