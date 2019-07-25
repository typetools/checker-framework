/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package java.rmi.server;

import java.io.*;
import java.util.*;

import org.checkerframework.checker.signedness.qual.PolySigned;

/**
 * <code>LogStream</code> provides a mechanism for logging errors that are
 * of possible interest to those monitoring a system.
 *
 * @author  Ann Wollrath (lots of code stolen from Ken Arnold)
 * @since   JDK1.1
 * @deprecated no replacement
 */
@Deprecated
public class LogStream extends PrintStream {

    /** table mapping known log names to log stream objects */
    private static Map<String,LogStream> known = new HashMap<>(5);
    /** default output stream for new logs */
    private static PrintStream  defaultStream = System.err;

    /** log name for this log */
    private String name;

    /** stream where output of this log is sent to */
    private OutputStream logOut;

    /** string writer for writing message prefixes to log stream */
    private OutputStreamWriter logWriter;

    /** string buffer used for constructing log message prefixes */
    private StringBuffer buffer = new StringBuffer();

    /** stream used for buffering lines */
    private ByteArrayOutputStream bufOut;

    /**
     * Create a new LogStream object.  Since this only constructor is
     * private, users must have a LogStream created through the "log"
     * method.
     * @param name string identifying messages from this log
     * @out output stream that log messages will be sent to
     * @since JDK1.1
     * @deprecated no replacement
     */
    @Deprecated
    private LogStream(String name, OutputStream out)
    {
        super(new ByteArrayOutputStream());
        bufOut = (ByteArrayOutputStream) super.out;

        this.name = name;
        setOutputStream(out);
    }

    /**
     * Return the LogStream identified by the given name.  If
     * a log corresponding to "name" does not exist, a log using
     * the default stream is created.
     * @param name name identifying the desired LogStream
     * @return log associated with given name
     * @since JDK1.1
     * @deprecated no replacement
     */
    @Deprecated
    public static LogStream log(String name) {
        LogStream stream;
        synchronized (known) {
            stream = known.get(name);
            if (stream == null) {
                stream = new LogStream(name, defaultStream);
            }
            known.put(name, stream);
        }
        return stream;
    }

    /**
     * Return the current default stream for new logs.
     * @return default log stream
     * @see #setDefaultStream
     * @since JDK1.1
     * @deprecated no replacement
     */
    @Deprecated
    public static synchronized PrintStream getDefaultStream() {
        return defaultStream;
    }

    /**
     * Set the default stream for new logs.
     * @param newDefault new default log stream
     * @see #getDefaultStream
     * @since JDK1.1
     * @deprecated no replacement
     */
    @Deprecated
    public static synchronized void setDefaultStream(PrintStream newDefault) {
        SecurityManager sm = System.getSecurityManager();

        if (sm != null) {
            sm.checkPermission(
                new java.util.logging.LoggingPermission("control", null));
        }

        defaultStream = newDefault;
    }

    /**
     * Return the current stream to which output from this log is sent.
     * @return output stream for this log
     * @see #setOutputStream
     * @since JDK1.1
     * @deprecated no replacement
     */
    @Deprecated
    public synchronized OutputStream getOutputStream()
    {
        return logOut;
    }

    /**
     * Set the stream to which output from this log is sent.
     * @param out new output stream for this log
     * @see #getOutputStream
     * @since JDK1.1
     * @deprecated no replacement
     */
    @Deprecated
    public synchronized void setOutputStream(OutputStream out)
    {
        logOut = out;
        // Maintain an OutputStreamWriter with default CharToByteConvertor
        // (just like new PrintStream) for writing log message prefixes.
        logWriter = new OutputStreamWriter(logOut);
    }

    /**
     * Write a byte of data to the stream.  If it is not a newline, then
     * the byte is appended to the internal buffer.  If it is a newline,
     * then the currently buffered line is sent to the log's output
     * stream, prefixed with the appropriate logging information.
     * @since JDK1.1
     * @deprecated no replacement
     */
    @Deprecated
    public void write(int b)
    {
        if (b == '\n') {
            // synchronize on "this" first to avoid potential deadlock
            synchronized (this) {
                synchronized (logOut) {
                    // construct prefix for log messages:
                    buffer.setLength(0);;
                    buffer.append(              // date/time stamp...
                        (new Date()).toString());
                    buffer.append(':');
                    buffer.append(name);        // ...log name...
                    buffer.append(':');
                    buffer.append(Thread.currentThread().getName());
                    buffer.append(':'); // ...and thread name

                    try {
                        // write prefix through to underlying byte stream
                        logWriter.write(buffer.toString());
                        logWriter.flush();

                        // finally, write the already converted bytes of
                        // the log message
                        bufOut.writeTo(logOut);
                        logOut.write(b);
                        logOut.flush();
                    } catch (IOException e) {
                        setError();
                    } finally {
                        bufOut.reset();
                    }
                }
            }
        }
        else
            super.write(b);
    }

    /**
     * Write a subarray of bytes.  Pass each through write byte method.
     * @since JDK1.1
     * @deprecated no replacement
     */
    @Deprecated
    public void write(@PolySigned byte b[], int off, int len)
    {
        if (len < 0)
            throw new ArrayIndexOutOfBoundsException(len);
        for (int i = 0; i < len; ++ i)
            write(b[off + i]);
    }

    /**
     * Return log name as string representation.
     * @return log name
     * @since JDK1.1
     * @deprecated no replacement
     */
    @Deprecated
    public String toString()
    {
        return name;
    }

    /** log level constant (no logging). */
    public static final int SILENT  = 0;
    /** log level constant (brief logging). */
    public static final int BRIEF   = 10;
    /** log level constant (verbose logging). */
    public static final int VERBOSE = 20;

    /**
     * Convert a string name of a logging level to its internal
     * integer representation.
     * @param s name of logging level (e.g., 'SILENT', 'BRIEF', 'VERBOSE')
     * @return corresponding integer log level
     * @since JDK1.1
     * @deprecated no replacement
     */
    @Deprecated
    public static int parseLevel(String s)
    {
        if ((s == null) || (s.length() < 1))
            return -1;

        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }
        if (s.length() < 1)
            return -1;

        if ("SILENT".startsWith(s.toUpperCase()))
            return SILENT;
        else if ("BRIEF".startsWith(s.toUpperCase()))
            return BRIEF;
        else if ("VERBOSE".startsWith(s.toUpperCase()))
            return VERBOSE;

        return -1;
    }
}
