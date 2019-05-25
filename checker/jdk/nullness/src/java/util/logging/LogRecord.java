/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.util.logging;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.ws.soap.Addressing;

import java.io.*;

import sun.misc.JavaLangAccess;
import sun.misc.SharedSecrets;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

/**
 * LogRecord objects are used to pass logging requests between
 * the logging framework and individual log Handlers.
 * <p>
 * When a LogRecord is passed into the logging framework it
 * logically belongs to the framework and should no longer be
 * used or updated by the client application.
 * <p>
 * Note that if the client application has not specified an
 * explicit source method name and source class name, then the
 * LogRecord class will infer them automatically when they are
 * first accessed (due to a call on getSourceMethodName or
 * getSourceClassName) by analyzing the call stack.  Therefore,
 * if a logging Handler wants to pass off a LogRecord to another
 * thread, or to transmit it over RMI, and if it wishes to subsequently
 * obtain method name or class name information it should call
 * one of getSourceClassName or getSourceMethodName to force
 * the values to be filled in.
 * <p>
 * <b> Serialization notes:</b>
 * <ul>
 * <li>The LogRecord class is serializable.
 *
 * <li> Because objects in the parameters array may not be serializable,
 * during serialization all objects in the parameters array are
 * written as the corresponding Strings (using Object.toString).
 *
 * <li> The ResourceBundle is not transmitted as part of the serialized
 * form, but the resource bundle name is, and the recipient object's
 * readObject method will attempt to locate a suitable resource bundle.
 *
 * </ul>
 *
 * @since 1.4
 */

public class LogRecord implements java.io.Serializable {
    private static final AtomicLong globalSequenceNumber
        = new AtomicLong(0);

    /**
     * The default value of threadID will be the current thread's
     * thread id, for ease of correlation, unless it is greater than
     * MIN_SEQUENTIAL_THREAD_ID, in which case we try harder to keep
     * our promise to keep threadIDs unique by avoiding collisions due
     * to 32-bit wraparound.  Unfortunately, LogRecord.getThreadID()
     * returns int, while Thread.getId() returns long.
     */
    private static final int MIN_SEQUENTIAL_THREAD_ID = Integer.MAX_VALUE / 2;

    private static final AtomicInteger nextThreadId
        = new AtomicInteger(MIN_SEQUENTIAL_THREAD_ID);

    private static final ThreadLocal<@Nullable Integer> threadIds = new ThreadLocal<>();

    /**
     * @serial Logging message level
     */
    private Level level;

    /**
     * @serial Sequence number
     */
    private long sequenceNumber;

    /**
     * @serial Class that issued logging call
     */
    private @Nullable String sourceClassName;

    /**
     * @serial Method that issued logging call
     */
    private @Nullable String sourceMethodName;

    /**
     * @serial Non-localized raw message text
     */
    private @Nullable String message;

    /**
     * @serial Thread ID for thread that issued logging call.
     */
    private int threadID;

    /**
     * @serial Event time in milliseconds since 1970
     */
    private long millis;

    /**
     * @serial The Throwable (if any) associated with log message
     */
    private @Nullable Throwable thrown;

    /**
     * @serial Name of the source Logger.
     */
    private @Nullable String loggerName;

    /**
     * @serial Resource bundle name to localized log message.
     */
    private @Nullable String resourceBundleName;

    private transient boolean needToInferCaller;
    private transient @Nullable Object parameters @Nullable [];
    private transient @Nullable ResourceBundle resourceBundle;

    /**
     * Returns the default value for a new LogRecord's threadID.
     */
    private int defaultThreadID(@UnderInitialization LogRecord this) {
        long tid = Thread.currentThread().getId();
        if (tid < MIN_SEQUENTIAL_THREAD_ID) {
            return (int) tid;
        } else {
            Integer id = threadIds.get();
            if (id == null) {
                id = nextThreadId.getAndIncrement();
                threadIds.set(id);
            }
            return id;
        }
    }

    /**
     * Construct a LogRecord with the given level and message values.
     * <p>
     * The sequence property will be initialized with a new unique value.
     * These sequence values are allocated in increasing order within a VM.
     * <p>
     * The millis property will be initialized to the current time.
     * <p>
     * The thread ID property will be initialized with a unique ID for
     * the current thread.
     * <p>
     * All other properties will be initialized to "null".
     *
     * @param level  a logging level value
     * @param msg  the raw non-localized logging message (may be null)
     */
    public LogRecord(Level level, @Nullable String msg) {
        // Make sure level isn't null, by calling random method.
        level.getClass();
        this.level = level;
        message = msg;
        // Assign a thread ID and a unique sequence number.
        sequenceNumber = globalSequenceNumber.getAndIncrement();
        threadID = defaultThreadID();
        millis = System.currentTimeMillis();
        needToInferCaller = true;
   }

    /**
     * Get the source Logger's name.
     *
     * @return source logger name (may be null)
     */
    public @Nullable String getLoggerName() {
        return loggerName;
    }

    /**
     * Set the source Logger's name.
     *
     * @param name   the source logger name (may be null)
     */
    public void setLoggerName(@Nullable String name) {
        loggerName = name;
    }

    /**
     * Get the localization resource bundle
     * <p>
     * This is the ResourceBundle that should be used to localize
     * the message string before formatting it.  The result may
     * be null if the message is not localizable, or if no suitable
     * ResourceBundle is available.
     * @return the localization resource bundle
     */
    public @Nullable ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
     * Set the localization resource bundle.
     *
     * @param bundle  localization bundle (may be null)
     */
    public void setResourceBundle(@Nullable ResourceBundle bundle) {
        resourceBundle = bundle;
    }

    /**
     * Get the localization resource bundle name
     * <p>
     * This is the name for the ResourceBundle that should be
     * used to localize the message string before formatting it.
     * The result may be null if the message is not localizable.
     * @return the localization resource bundle name
     */
    public @Nullable String getResourceBundleName() {
        return resourceBundleName;
    }

    /**
     * Set the localization resource bundle name.
     *
     * @param name  localization bundle name (may be null)
     */
    public void setResourceBundleName(@Nullable String name) {
        resourceBundleName = name;
    }

    /**
     * Get the logging message level, for example Level.SEVERE.
     * @return the logging message level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Set the logging message level, for example Level.SEVERE.
     * @param level the logging message level
     */
    public void setLevel(Level level) {
        if (level == null) {
            throw new NullPointerException();
        }
        this.level = level;
    }

    /**
     * Get the sequence number.
     * <p>
     * Sequence numbers are normally assigned in the LogRecord
     * constructor, which assigns unique sequence numbers to
     * each new LogRecord in increasing order.
     * @return the sequence number
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Set the sequence number.
     * <p>
     * Sequence numbers are normally assigned in the LogRecord constructor,
     * so it should not normally be necessary to use this method.
     * @param seq the sequence number
     */
    public void setSequenceNumber(long seq) {
        sequenceNumber = seq;
    }

    /**
     * Get the  name of the class that (allegedly) issued the logging request.
     * <p>
     * Note that this sourceClassName is not verified and may be spoofed.
     * This information may either have been provided as part of the
     * logging call, or it may have been inferred automatically by the
     * logging framework.  In the latter case, the information may only
     * be approximate and may in fact describe an earlier call on the
     * stack frame.
     * <p>
     * May be null if no information could be obtained.
     *
     * @return the source class name
     */
    public @Nullable String getSourceClassName() {
        if (needToInferCaller) {
            inferCaller();
        }
        return sourceClassName;
    }

    /**
     * Set the name of the class that (allegedly) issued the logging request.
     *
     * @param sourceClassName the source class name (may be null)
     */
    public void setSourceClassName(@Nullable String sourceClassName) {
        this.sourceClassName = sourceClassName;
        needToInferCaller = false;
    }

    /**
     * Get the  name of the method that (allegedly) issued the logging request.
     * <p>
     * Note that this sourceMethodName is not verified and may be spoofed.
     * This information may either have been provided as part of the
     * logging call, or it may have been inferred automatically by the
     * logging framework.  In the latter case, the information may only
     * be approximate and may in fact describe an earlier call on the
     * stack frame.
     * <p>
     * May be null if no information could be obtained.
     *
     * @return the source method name
     */
    public @Nullable String getSourceMethodName() {
        if (needToInferCaller) {
            inferCaller();
        }
        return sourceMethodName;
    }

    /**
     * Set the name of the method that (allegedly) issued the logging request.
     *
     * @param sourceMethodName the source method name (may be null)
     */
    public void setSourceMethodName(@Nullable String sourceMethodName) {
        this.sourceMethodName = sourceMethodName;
        needToInferCaller = false;
    }

    /**
     * Get the "raw" log message, before localization or formatting.
     * <p>
     * May be null, which is equivalent to the empty string "".
     * <p>
     * This message may be either the final text or a localization key.
     * <p>
     * During formatting, if the source logger has a localization
     * ResourceBundle and if that ResourceBundle has an entry for
     * this message string, then the message string is replaced
     * with the localized value.
     *
     * @return the raw message string
     */
    public @Nullable String getMessage() {
        return message;
    }

    /**
     * Set the "raw" log message, before localization or formatting.
     *
     * @param message the raw message string (may be null)
     */
    public void setMessage(@Nullable String message) {
        this.message = message;
    }

    /**
     * Get the parameters to the log message.
     *
     * @return the log message parameters.  May be null if
     *                  there are no parameters.
     */
    public @Nullable Object @Nullable [] getParameters() {
        return parameters;
    }

    /**
     * Set the parameters to the log message.
     *
     * @param parameters the log message parameters. (may be null)
     */
    public void setParameters(@Nullable Object parameters @Nullable []) {
        this.parameters = parameters;
    }

    /**
     * Get an identifier for the thread where the message originated.
     * <p>
     * This is a thread identifier within the Java VM and may or
     * may not map to any operating system ID.
     *
     * @return thread ID
     */
    public int getThreadID() {
        return threadID;
    }

    /**
     * Set an identifier for the thread where the message originated.
     * @param threadID  the thread ID
     */
    public void setThreadID(int threadID) {
        this.threadID = threadID;
    }

    /**
     * Get event time in milliseconds since 1970.
     *
     * @return event time in millis since 1970
     */
    public long getMillis() {
        return millis;
    }

    /**
     * Set event time.
     *
     * @param millis event time in millis since 1970
     */
    public void setMillis(long millis) {
        this.millis = millis;
    }

    /**
     * Get any throwable associated with the log record.
     * <p>
     * If the event involved an exception, this will be the
     * exception object. Otherwise null.
     *
     * @return a throwable
     */
    public @Nullable Throwable getThrown() {
        return thrown;
    }

    /**
     * Set a throwable associated with the log event.
     *
     * @param thrown  a throwable (may be null)
     */
    public void setThrown(@Nullable Throwable thrown) {
        this.thrown = thrown;
    }

    private static final long serialVersionUID = 5372048053134512534L;

    /**
     * @serialData Default fields, followed by a two byte version number
     * (major byte, followed by minor byte), followed by information on
     * the log record parameter array.  If there is no parameter array,
     * then -1 is written.  If there is a parameter array (possible of zero
     * length) then the array length is written as an integer, followed
     * by String values for each parameter.  If a parameter is null, then
     * a null String is written.  Otherwise the output of Object.toString()
     * is written.
     */
    @SuppressWarnings("dereference.of.nullable") // out.writeInt and out.writeObject do not affect parameters field. http://tinyurl.com/cfissue/984
    private void writeObject(ObjectOutputStream out) throws IOException {
        // We have to call defaultWriteObject first.
        out.defaultWriteObject();

        // Write our version number.
        out.writeByte(1);
        out.writeByte(0);
        if (parameters == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(parameters.length);
        // Write string values for the parameters.
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == null) {
                out.writeObject(null);
            } else {
                out.writeObject(parameters[i].toString());
            }
        }
    }

    @SuppressWarnings({"dereference.of.nullable"}) // in.readObject does not affect parameters field. http://tinyurl.com/cfissue/984
    private void readObject(ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
        // We have to call defaultReadObject first.
        in.defaultReadObject();

        // Read version number.
        byte major = in.readByte();
        byte minor = in.readByte();
        if (major != 1) {
            throw new IOException("LogRecord: bad version: " + major + "." + minor);
        }
        int len = in.readInt();
        if (len < -1) {
            throw new NegativeArraySizeException();
        } else if (len == -1) {
            parameters = null;
        } else if (len < 255) {
            parameters = new Object[len];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = in.readObject();
            }
        } else {
            List<Object> params = new ArrayList<>(Math.min(len, 1024));
            for (int i = 0; i < len; i++) {
                params.add(in.readObject());
            }
            parameters = params.toArray(new Object[params.size()]);
        }
        // If necessary, try to regenerate the resource bundle.
        if (resourceBundleName != null) {
            try {
                // use system class loader to ensure the ResourceBundle
                // instance is a different instance than null loader uses
                final ResourceBundle bundle =
                        ResourceBundle.getBundle(resourceBundleName,
                                Locale.getDefault(),
                                ClassLoader.getSystemClassLoader());
                resourceBundle = bundle;
            } catch (MissingResourceException ex) {
                // This is not a good place to throw an exception,
                // so we simply leave the resourceBundle null.
                resourceBundle = null;
            }
        }

        needToInferCaller = false;
    }

    // Private method to infer the caller's class and method names
    private void inferCaller() {
        needToInferCaller = false;
        JavaLangAccess access = SharedSecrets.getJavaLangAccess();
        Throwable throwable = new Throwable();
        int depth = access.getStackTraceDepth(throwable);

        boolean lookingForLogger = true;
        for (int ix = 0; ix < depth; ix++) {
            // Calling getStackTraceElement directly prevents the VM
            // from paying the cost of building the entire stack frame.
            StackTraceElement frame =
                access.getStackTraceElement(throwable, ix);
            String cname = frame.getClassName();
            boolean isLoggerImpl = isLoggerImplFrame(cname);
            if (lookingForLogger) {
                // Skip all frames until we have found the first logger frame.
                if (isLoggerImpl) {
                    lookingForLogger = false;
                }
            } else {
                if (!isLoggerImpl) {
                    // skip reflection call
                    if (!cname.startsWith("java.lang.reflect.") && !cname.startsWith("sun.reflect.")) {
                       // We've found the relevant frame.
                       setSourceClassName(cname);
                       setSourceMethodName(frame.getMethodName());
                       return;
                    }
                }
            }
        }
        // We haven't found a suitable frame, so just punt.  This is
        // OK as we are only committed to making a "best effort" here.
    }

    private boolean isLoggerImplFrame(String cname) {
        // the log record could be created for a platform logger
        return (cname.equals("java.util.logging.Logger") ||
                cname.startsWith("java.util.logging.LoggingProxyImpl") ||
                cname.startsWith("sun.util.logging."));
    }
}
