/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


package java.util.logging;

import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * A Logger object is used to log messages for a specific
 * system or application component.  Loggers are normally named,
 * using a hierarchical dot-separated namespace.  Logger names
 * can be arbitrary strings, but they should normally be based on
 * the package name or class name of the logged component, such
 * as java.net or javax.swing.  In addition it is possible to create
 * "anonymous" Loggers that are not stored in the Logger namespace.
 * <p>
 * Logger objects may be obtained by calls on one of the getLogger
 * factory methods.  These will either create a new Logger or
 * return a suitable existing Logger. It is important to note that
 * the Logger returned by one of the {@code getLogger} factory methods
 * may be garbage collected at any time if a strong reference to the
 * Logger is not kept.
 * <p>
 * Logging messages will be forwarded to registered Handler
 * objects, which can forward the messages to a variety of
 * destinations, including consoles, files, OS logs, etc.
 * <p>
 * Each Logger keeps track of a "parent" Logger, which is its
 * nearest existing ancestor in the Logger namespace.
 * <p>
 * Each Logger has a "Level" associated with it.  This reflects
 * a minimum Level that this logger cares about.  If a Logger's
 * level is set to <tt>null</tt>, then its effective level is inherited
 * from its parent, which may in turn obtain it recursively from its
 * parent, and so on up the tree.
 * <p>
 * The log level can be configured based on the properties from the
 * logging configuration file, as described in the description
 * of the LogManager class.  However it may also be dynamically changed
 * by calls on the Logger.setLevel method.  If a logger's level is
 * changed the change may also affect child loggers, since any child
 * logger that has <tt>null</tt> as its level will inherit its
 * effective level from its parent.
 * <p>
 * On each logging call the Logger initially performs a cheap
 * check of the request level (e.g., SEVERE or FINE) against the
 * effective log level of the logger.  If the request level is
 * lower than the log level, the logging call returns immediately.
 * <p>
 * After passing this initial (cheap) test, the Logger will allocate
 * a LogRecord to describe the logging message.  It will then call a
 * Filter (if present) to do a more detailed check on whether the
 * record should be published.  If that passes it will then publish
 * the LogRecord to its output Handlers.  By default, loggers also
 * publish to their parent's Handlers, recursively up the tree.
 * <p>
 * Each Logger may have a ResourceBundle name associated with it.
 * The named bundle will be used for localizing logging messages.
 * If a Logger does not have its own ResourceBundle name, then
 * it will inherit the ResourceBundle name from its parent,
 * recursively up the tree.
 * <p>
 * Most of the logger output methods take a "msg" argument.  This
 * msg argument may be either a raw value or a localization key.
 * During formatting, if the logger has (or inherits) a localization
 * ResourceBundle and if the ResourceBundle has a mapping for the msg
 * string, then the msg string is replaced by the localized value.
 * Otherwise the original msg string is used.  Typically, formatters use
 * java.text.MessageFormat style formatting to format parameters, so
 * for example a format string "{0} {1}" would format two parameters
 * as strings.
 * <p>
 * A set of methods alternatively take a "msgSupplier" instead of a "msg"
 * argument.  These methods take a {@link Supplier}{@code <String>} function
 * which is invoked to construct the desired log message only when the message
 * actually is to be logged based on the effective log level thus eliminating
 * unnecessary message construction. For example, if the developer wants to
 * log system health status for diagnosis, with the String-accepting version,
 * the code would look like:
 <pre><code>

   class DiagnosisMessages {
     static String systemHealthStatus() {
       // collect system health information
       ...
     }
   }
   ...
   logger.log(Level.FINER, DiagnosisMessages.systemHealthStatus());
</code></pre>
 * With the above code, the health status is collected unnecessarily even when
 * the log level FINER is disabled. With the Supplier-accepting version as
 * below, the status will only be collected when the log level FINER is
 * enabled.
 <pre><code>

   logger.log(Level.FINER, DiagnosisMessages::systemHealthStatus);
</code></pre>
 * <p>
 * When mapping ResourceBundle names to ResourceBundles, the Logger
 * will first try to use the Thread's ContextClassLoader.  If that
 * is null it will try the
 * {@linkplain java.lang.ClassLoader#getSystemClassLoader() system ClassLoader} instead.
 * <p>
 * Formatting (including localization) is the responsibility of
 * the output Handler, which will typically call a Formatter.
 * <p>
 * Note that formatting need not occur synchronously.  It may be delayed
 * until a LogRecord is actually written to an external sink.
 * <p>
 * The logging methods are grouped in five main categories:
 * <ul>
 * <li><p>
 *     There are a set of "log" methods that take a log level, a message
 *     string, and optionally some parameters to the message string.
 * <li><p>
 *     There are a set of "logp" methods (for "log precise") that are
 *     like the "log" methods, but also take an explicit source class name
 *     and method name.
 * <li><p>
 *     There are a set of "logrb" method (for "log with resource bundle")
 *     that are like the "logp" method, but also take an explicit resource
 *     bundle name for use in localizing the log message.
 * <li><p>
 *     There are convenience methods for tracing method entries (the
 *     "entering" methods), method returns (the "exiting" methods) and
 *     throwing exceptions (the "throwing" methods).
 * <li><p>
 *     Finally, there are a set of convenience methods for use in the
 *     very simplest cases, when a developer simply wants to log a
 *     simple string at a given log level.  These methods are named
 *     after the standard Level names ("severe", "warning", "info", etc.)
 *     and take a single argument, a message string.
 * </ul>
 * <p>
 * For the methods that do not take an explicit source name and
 * method name, the Logging framework will make a "best effort"
 * to determine which class and method called into the logging method.
 * However, it is important to realize that this automatically inferred
 * information may only be approximate (or may even be quite wrong!).
 * Virtual machines are allowed to do extensive optimizations when
 * JITing and may entirely remove stack frames, making it impossible
 * to reliably locate the calling class and method.
 * <P>
 * All methods on Logger are multi-thread safe.
 * <p>
 * <b>Subclassing Information:</b> Note that a LogManager class may
 * provide its own implementation of named Loggers for any point in
 * the namespace.  Therefore, any subclasses of Logger (unless they
 * are implemented in conjunction with a new LogManager class) should
 * take care to obtain a Logger instance from the LogManager class and
 * should delegate operations such as "isLoggable" and "log(LogRecord)"
 * to that instance.  Note that in order to intercept all logging
 * output, subclasses need only override the log(LogRecord) method.
 * All the other logging methods are implemented as calls on this
 * log(LogRecord) method.
 *
 * @since 1.4
 */
// These annotations mark every void method as @SideEffectFree.
// This is questionable, because a call could change the logger (say, if it caches messages, such as concatenating them into a string).
// However, logging generally cannot affect any other part of the program, so it seems reasonable to write these annotations.
public class Logger {
    private static final Handler emptyHandlers[] = new Handler[0];
    private static final int offValue = Level.OFF.intValue();
    private @Nullable LogManager manager;
    private @Nullable String name;
    private final CopyOnWriteArrayList<Handler> handlers =
        new CopyOnWriteArrayList<>();
    //private String resourceBundleName;
    private volatile boolean useParentHandlers = true;
    private volatile @Nullable Filter filter;
    private boolean anonymous;

    private @Nullable ResourceBundle catalog;     // Cached resource bundle
    private @Nullable String catalogName;         // name associated with catalog
    private @Nullable Locale catalogLocale;       // locale associated with catalog

    // The fields relating to parent-child relationships and levels
    // are managed under a separate lock, the treeLock.
    private static Object treeLock = new Object();
    // We keep weak references from parents to children, but strong
    // references from children to parents.
    private volatile @Nullable Logger parent;    // our nearest parent.
    private @Nullable ArrayList<LogManager.LoggerWeakRef> kids;   // WeakReferences to loggers that have us as parent
    private volatile @Nullable Level levelObject;
    private volatile int levelValue;  // current effective level value
    private @Nullable WeakReference<ClassLoader> callersClassLoaderRef;

    /**
     * GLOBAL_LOGGER_NAME is a name for the global logger.
     *
     * @since 1.6
     */
    public static final String GLOBAL_LOGGER_NAME = "global";

    /**
     * Return global logger object with the name Logger.GLOBAL_LOGGER_NAME.
     *
     * @return global logger object
     * @since 1.7
     */
    @Pure
    public static final Logger getGlobal() { throw new RuntimeException("skeleton method"); }

    /**
     * The "global" Logger object is provided as a convenience to developers
     * who are making casual use of the Logging package.  Developers
     * who are making serious use of the logging package (for example
     * in products) should create and use their own Logger objects,
     * with appropriate names, so that logging can be controlled on a
     * suitable per-Logger granularity. Developers also need to keep a
     * strong reference to their Logger objects to prevent them from
     * being garbage collected.
     * <p>
     * @deprecated Initialization of this field is prone to deadlocks.
     * The field must be initialized by the Logger class initialization
     * which may cause deadlocks with the LogManager class initialization.
     * In such cases two class initialization wait for each other to complete.
     * The preferred way to get the global logger object is via the call
     * <code>Logger.getGlobal()</code>.
     * For compatibility with old JDK versions where the
     * <code>Logger.getGlobal()</code> is not available use the call
     * <code>Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)</code>
     * or <code>Logger.getLogger("global")</code>.
     */
    @Deprecated
    public static final Logger global = new Logger(GLOBAL_LOGGER_NAME);

    /**
     * Protected method to construct a logger for a named subsystem.
     * <p>
     * The logger will be initially configured with a null Level
     * and with useParentHandlers set to true.
     *
     * @param   name    A name for the logger.  This should
     *                          be a dot-separated name and should normally
     *                          be based on the package name or class name
     *                          of the subsystem, such as java.net
     *                          or javax.swing.  It may be null for anonymous Loggers.
     * @param   resourceBundleName  name of ResourceBundle to be used for localizing
     *                          messages for this logger.  May be null if none
     *                          of the messages require localization.
     * @throws MissingResourceException if the resourceBundleName is non-null and
     *             no corresponding resource can be found.
     */
    protected Logger(@Nullable String name, @Nullable String resourceBundleName) { throw new RuntimeException("skeleton method"); }

    Logger(@Nullable String name, @Nullable String resourceBundleName, @Nullable Class<?> caller) { throw new RuntimeException("skeleton method"); }

    private void setCallersClassLoaderRef(@Nullable Class<?> caller) { throw new RuntimeException("skeleton method"); }

    private @Nullable ClassLoader getCallersClassLoader() { throw new RuntimeException("skeleton method"); }

    // This constructor is used only to create the global Logger.
    // It is needed to break a cyclic dependence between the LogManager
    // and Logger static initializers causing deadlocks.
    private Logger(String name) { throw new RuntimeException("skeleton method"); }

    // It is called from the LogManager.<clinit> to complete
    // initialization of the global Logger.
    void setLogManager(LogManager manager) { throw new RuntimeException("skeleton method"); }

    private void checkPermission() throws SecurityException { throw new RuntimeException("skeleton method"); }

    // Until all JDK code converted to call sun.util.logging.PlatformLogger
    // (see 7054233), we need to determine if Logger.getLogger is to add
    // a system logger or user logger.
    //
    // As an interim solution, if the immediate caller whose caller loader is
    // null, we assume it's a system logger and add it to the system context.
    // These system loggers only set the resource bundle to the given
    // resource bundle name (rather than the default system resource bundle).
    private static class SystemLoggerHelper {
        static boolean disableCallerCheck = getBooleanProperty("sun.util.logging.disableCallerCheck");
        private static boolean getBooleanProperty(final String key) { throw new RuntimeException("skeleton method"); }
    }

    private static Logger demandLogger(String name, @Nullable String resourceBundleName, Class<?> caller) { throw new RuntimeException("skeleton method"); }

    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the LogManager configuration and it will configured
     * to also send logging output to its parent's Handlers.  It will
     * be registered in the LogManager global namespace.
     * <p>
     * Note: The LogManager may only retain a weak reference to the newly
     * created Logger. It is important to understand that a previously
     * created Logger with the given name may be garbage collected at any
     * time if there is no strong reference to the Logger. In particular,
     * this means that two back-to-back calls like
     * {@code getLogger("MyLogger").log(...)} may use different Logger
     * objects named "MyLogger" if there is no strong reference to the
     * Logger named "MyLogger" elsewhere in the program.
     *
     * @param   name            A name for the logger.  This should
     *                          be a dot-separated name and should normally
     *                          be based on the package name or class name
     *                          of the subsystem, such as java.net
     *                          or javax.swing
     * @return a suitable Logger
     * @throws NullPointerException if the name is null.
     */

    // Synchronization is not required here. All synchronization for
    // adding a new Logger object is handled by LogManager.addLogger().
    // @CallerSensitive
    @Pure
    public static Logger getLogger(String name) { throw new RuntimeException("skeleton method"); }

    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the LogManager and it will configured to also send logging
     * output to its parent's Handlers.  It will be registered in
     * the LogManager global namespace.
     * <p>
     * Note: The LogManager may only retain a weak reference to the newly
     * created Logger. It is important to understand that a previously
     * created Logger with the given name may be garbage collected at any
     * time if there is no strong reference to the Logger. In particular,
     * this means that two back-to-back calls like
     * {@code getLogger("MyLogger", ...).log(...)} may use different Logger
     * objects named "MyLogger" if there is no strong reference to the
     * Logger named "MyLogger" elsewhere in the program.
     * <p>
     * If the named Logger already exists and does not yet have a
     * localization resource bundle then the given resource bundle
     * name is used.  If the named Logger already exists and has
     * a different resource bundle name then an IllegalArgumentException
     * is thrown.
     * <p>
     * @param   name    A name for the logger.  This should
     *                          be a dot-separated name and should normally
     *                          be based on the package name or class name
     *                          of the subsystem, such as java.net
     *                          or javax.swing
     * @param   resourceBundleName  name of ResourceBundle to be used for localizing
     *                          messages for this logger. May be <CODE>null</CODE> if none of
     *                          the messages require localization.
     * @return a suitable Logger
     * @throws MissingResourceException if the resourceBundleName is non-null and
     *             no corresponding resource can be found.
     * @throws IllegalArgumentException if the Logger already exists and uses
     *             a different resource bundle name.
     * @throws NullPointerException if the name is null.
     */

    // Synchronization is not required here. All synchronization for
    // adding a new Logger object is handled by LogManager.addLogger().
    // @CallerSensitive
    @Pure
    public static Logger getLogger(String name, @Nullable String resourceBundleName) { throw new RuntimeException("skeleton method"); }

    // package-private
    // Add a platform logger to the system context.
    // i.e. caller of sun.util.logging.PlatformLogger.getLogger
    static Logger getPlatformLogger(String name) { throw new RuntimeException("skeleton method"); }

    /**
     * Create an anonymous Logger.  The newly created Logger is not
     * registered in the LogManager namespace.  There will be no
     * access checks on updates to the logger.
     * <p>
     * This factory method is primarily intended for use from applets.
     * Because the resulting Logger is anonymous it can be kept private
     * by the creating class.  This removes the need for normal security
     * checks, which in turn allows untrusted applet code to update
     * the control state of the Logger.  For example an applet can do
     * a setLevel or an addHandler on an anonymous Logger.
     * <p>
     * Even although the new logger is anonymous, it is configured
     * to have the root logger ("") as its parent.  This means that
     * by default it inherits its effective level and handlers
     * from the root logger.
     * <p>
     *
     * @return a newly created private Logger
     */
    @Pure
    public static Logger getAnonymousLogger() { throw new RuntimeException("skeleton method"); }

    /**
     * Create an anonymous Logger.  The newly created Logger is not
     * registered in the LogManager namespace.  There will be no
     * access checks on updates to the logger.
     * <p>
     * This factory method is primarily intended for use from applets.
     * Because the resulting Logger is anonymous it can be kept private
     * by the creating class.  This removes the need for normal security
     * checks, which in turn allows untrusted applet code to update
     * the control state of the Logger.  For example an applet can do
     * a setLevel or an addHandler on an anonymous Logger.
     * <p>
     * Even although the new logger is anonymous, it is configured
     * to have the root logger ("") as its parent.  This means that
     * by default it inherits its effective level and handlers
     * from the root logger.
     * <p>
     * @param   resourceBundleName  name of ResourceBundle to be used for localizing
     *                          messages for this logger.
     *          May be null if none of the messages require localization.
     * @return a newly created private Logger
     * @throws MissingResourceException if the resourceBundleName is non-null and
     *             no corresponding resource can be found.
     */

    // Synchronization is not required here. All synchronization for
    // adding a new anonymous Logger object is handled by doSetParent().
    // @CallerSensitive
    @Pure
    public static Logger getAnonymousLogger(@Nullable String resourceBundleName) { throw new RuntimeException("skeleton method"); }

    /**
     * Retrieve the localization resource bundle for this
     * logger for the current default locale.  Note that if
     * the result is null, then the Logger will use a resource
     * bundle inherited from its parent.
     *
     * @return localization bundle (may be null)
     */
    @Pure
    public @Nullable ResourceBundle getResourceBundle() { throw new RuntimeException("skeleton method"); }

    /**
     * Retrieve the localization resource bundle name for this
     * logger.  Note that if the result is null, then the Logger
     * will use a resource bundle name inherited from its parent.
     *
     * @return localization bundle name (may be null)
     */
    @Pure
    public @Nullable String getResourceBundleName() { throw new RuntimeException("skeleton method"); }

    /**
     * Set a filter to control output on this Logger.
     * <P>
     * After passing the initial "level" check, the Logger will
     * call this Filter to check if a log record should really
     * be published.
     *
     * @param   newFilter  a filter object (may be null)
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public void setFilter(@Nullable Filter newFilter) throws SecurityException { throw new RuntimeException("skeleton method"); }

    /**
     * Get the current filter for this Logger.
     *
     * @return  a filter object (may be null)
     */
    @Pure
    public @Nullable Filter getFilter() { throw new RuntimeException("skeleton method"); }

    /**
     * Log a LogRecord.
     * <p>
     * All the other logging methods in this class call through
     * this method to actually perform any logging.  Subclasses can
     * override this single method to capture all log activity.
     *
     * @param record the LogRecord to be published
     */
    @SideEffectFree
    public void log(LogRecord record) { throw new RuntimeException("skeleton method"); }

    // private support method for logging.
    // We fill in the logger name, resource bundle name, and
    // resource bundle and then call "void log(LogRecord)".
    @SideEffectFree
    private void doLog(LogRecord lr) { throw new RuntimeException("skeleton method"); }


    //================================================================
    // Start of convenience methods WITHOUT className and methodName
    //================================================================

    /**
     * Log a message, with no arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   msg     The string message (or a key in the message catalog)
     */
    @SideEffectFree
    public void log(Level level, @Nullable String msg) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a message, which is only to be constructed if the logging level
     * is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     */
    @SideEffectFree public void log(Level level, Supplier<? extends @Nullable String> msgSupplier) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a message, with one object parameter.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   msg     The string message (or a key in the message catalog)
     * @param   param1  parameter to the message
     */
    @SideEffectFree
    public void log(Level level, @Nullable String msg, @Nullable Object param1) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a message, with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   msg     The string message (or a key in the message catalog)
     * @param   params  array of parameters to the message
     */
    @SideEffectFree
    public void log(Level level, @Nullable String msg, @Nullable Object params @Nullable []) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a message, with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   msg     The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with log message.
     */
    @SideEffectFree
    public void log(Level level, @Nullable String msg, @Nullable Throwable thrown) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a lazily constructed message, with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message level then the
     * message is constructed by invoking the provided supplier function. The
     * message and the given {@link Throwable} are then stored in a {@link
     * LogRecord} which is forwarded to all registered output handlers.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   thrown  Throwable associated with log message.
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    @SideEffectFree
    public void log(Level level, @Nullable Throwable thrown, Supplier<? extends @Nullable String> msgSupplier) { throw new RuntimeException("skeleton method"); }

    //================================================================
    // Start of convenience methods WITH className and methodName
    //================================================================

    /**
     * Log a message, specifying source class and method,
     * with no arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg     The string message (or a key in the message catalog)
     */
    @SideEffectFree
    public void logp(Level level, @Nullable String sourceClass, @Nullable String sourceMethod, @Nullable String msg) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a lazily constructed message, specifying source class and method,
     * with no arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    @SideEffectFree
    public void logp(Level level, @Nullable String sourceClass, @Nullable String sourceMethod,
                     Supplier<? extends @Nullable String> msgSupplier) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a message, specifying source class and method,
     * with a single object parameter to the log message.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg      The string message (or a key in the message catalog)
     * @param   param1    Parameter to the log message.
     */
    @SideEffectFree
    public void logp(Level level, @Nullable String sourceClass, @Nullable String sourceMethod,
                     @Nullable String msg, @Nullable Object param1) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a message, specifying source class and method,
     * with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg     The string message (or a key in the message catalog)
     * @param   params  Array of parameters to the message
     */
    @SideEffectFree
    public void logp(Level level, @Nullable String sourceClass, @Nullable String sourceMethod,
                     @Nullable String msg, @Nullable Object params @Nullable []) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a message, specifying source class and method,
     * with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg     The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with log message.
     */
    @SideEffectFree
    public void logp(Level level, @Nullable String sourceClass, @Nullable String sourceMethod,
                     @Nullable String msg, @Nullable Throwable thrown) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a lazily constructed message, specifying source class and method,
     * with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message level then the
     * message is constructed by invoking the provided supplier function. The
     * message and the given {@link Throwable} are then stored in a {@link
     * LogRecord} which is forwarded to all registered output handlers.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   thrown  Throwable associated with log message.
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
   @SideEffectFree
   public void logp(Level level, @Nullable String sourceClass, @Nullable String sourceMethod,
                    @Nullable Throwable thrown, Supplier<? extends @Nullable String> msgSupplier) { throw new RuntimeException("skeleton method"); }


    //=========================================================================
    // Start of convenience methods WITH className, methodName and bundle name.
    //=========================================================================

    // Private support method for logging for "logrb" methods.
    // We fill in the logger name, resource bundle name, and
    // resource bundle and then call "void log(LogRecord)".
    @SideEffectFree
    private void doLog(LogRecord lr, @Nullable String rbname) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a message, specifying source class, method, and resource bundle name
     * with no arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, or an empty String or invalid
     * then the msg string is not localized.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg,
     *                         can be null
     * @param   msg     The string message (or a key in the message catalog)
     */
    @SideEffectFree
    public void logrb(Level level, @Nullable String sourceClass, @Nullable String sourceMethod,
                      @Nullable String bundleName, @Nullable String msg) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a message, specifying source class, method, and resource bundle name,
     * with a single object parameter to the log message.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, or an empty String or invalid
     * then the msg string is not localized.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg,
     *                         can be null
     * @param   msg      The string message (or a key in the message catalog)
     * @param   param1    Parameter to the log message.
     */
    @SideEffectFree
    public void logrb(Level level, @Nullable String sourceClass, @Nullable String sourceMethod,
                      @Nullable String bundleName, @Nullable String msg, @Nullable Object param1) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a message, specifying source class, method, and resource bundle name,
     * with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, or an empty String or invalid
     * then the msg string is not localized.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg,
     *                         can be null.
     * @param   msg     The string message (or a key in the message catalog)
     * @param   params  Array of parameters to the message
     */
    @SideEffectFree
    public void logrb(Level level, @Nullable String sourceClass, @Nullable String sourceMethod,
                      @Nullable String bundleName, @Nullable String msg, @Nullable Object params @Nullable []) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a message, specifying source class, method, and resource bundle name,
     * with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, or an empty String or invalid
     * then the msg string is not localized.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg,
     *                         can be null
     * @param   msg     The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with log message.
     */
    @SideEffectFree
    public void logrb(Level level, @Nullable String sourceClass, @Nullable String sourceMethod,
                      @Nullable String bundleName, @Nullable String msg, @Nullable Throwable thrown) { throw new RuntimeException("skeleton method"); }


    //======================================================================
    // Start of convenience methods for logging method entries and returns.
    //======================================================================

    /**
     * Log a method entry.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY", log level
     * FINER, and the given sourceMethod and sourceClass is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     */
    @SideEffectFree
    public void entering(@Nullable String sourceClass, @Nullable String sourceMethod) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a method entry, with one parameter.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY {0}", log level
     * FINER, and the given sourceMethod, sourceClass, and parameter
     * is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     * @param   param1         parameter to the method being entered
     */
    @SideEffectFree
    public void entering(@Nullable String sourceClass, @Nullable String sourceMethod, @Nullable Object param1) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a method entry, with an array of parameters.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY" (followed by a
     * format {N} indicator for each entry in the parameter array),
     * log level FINER, and the given sourceMethod, sourceClass, and
     * parameters is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     * @param   params         array of parameters to the method being entered
     */
    @SideEffectFree
    public void entering(@Nullable String sourceClass, @Nullable String sourceMethod, @Nullable Object params @Nullable []) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a method return.
     * <p>
     * This is a convenience method that can be used to log returning
     * from a method.  A LogRecord with message "RETURN", log level
     * FINER, and the given sourceMethod and sourceClass is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of the method
     */
    @SideEffectFree
    public void exiting(@Nullable String sourceClass, @Nullable String sourceMethod) { throw new RuntimeException("skeleton method"); }


    /**
     * Log a method return, with result object.
     * <p>
     * This is a convenience method that can be used to log returning
     * from a method.  A LogRecord with message "RETURN {0}", log level
     * FINER, and the gives sourceMethod, sourceClass, and result
     * object is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of the method
     * @param   result  Object that is being returned
     */
    @SideEffectFree
    public void exiting(@Nullable String sourceClass, @Nullable String sourceMethod, @Nullable Object result) { throw new RuntimeException("skeleton method"); }

    /**
     * Log throwing an exception.
     * <p>
     * This is a convenience method to log that a method is
     * terminating by throwing an exception.  The logging is done
     * using the FINER level.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.  The
     * LogRecord's message is set to "THROW".
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod  name of the method.
     * @param   thrown  The Throwable that is being thrown.
     */
    @SideEffectFree
    public void throwing(@Nullable String sourceClass, @Nullable String sourceMethod, @Nullable Throwable thrown) { throw new RuntimeException("skeleton method"); }

    //=======================================================================
    // Start of simple convenience methods using level names as method names
    //=======================================================================

    /**
     * Log a SEVERE message.
     * <p>
     * If the logger is currently enabled for the SEVERE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    @SideEffectFree
    public void severe(@Nullable String msg) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a WARNING message.
     * <p>
     * If the logger is currently enabled for the WARNING message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    @SideEffectFree
    public void warning(@Nullable String msg) { throw new RuntimeException("skeleton method"); }

    /**
     * Log an INFO message.
     * <p>
     * If the logger is currently enabled for the INFO message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    @SideEffectFree
    public void info(@Nullable String msg) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a CONFIG message.
     * <p>
     * If the logger is currently enabled for the CONFIG message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    @SideEffectFree
    public void config(@Nullable String msg) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    @SideEffectFree
    public void fine(@Nullable String msg) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    @SideEffectFree
    public void finer(@Nullable String msg) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    @SideEffectFree
    public void finest(@Nullable String msg) { throw new RuntimeException("skeleton method"); }

    //=======================================================================
    // Start of simple convenience methods using level names as method names
    // and use Supplier<? extends @Nullable String>
    //=======================================================================

    /**
     * Log a SEVERE message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the SEVERE message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     * <p>
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    @SideEffectFree public void severe(Supplier<? extends @Nullable String> msgSupplier) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a WARNING message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the WARNING message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     * <p>
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    @SideEffectFree public void warning(Supplier<? extends @Nullable String> msgSupplier) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a INFO message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the INFO message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     * <p>
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    @SideEffectFree public void info(Supplier<? extends @Nullable String> msgSupplier) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a CONFIG message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the CONFIG message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     * <p>
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    @SideEffectFree public void config(Supplier<? extends @Nullable String> msgSupplier) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a FINE message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the FINE message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     * <p>
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    @SideEffectFree public void fine(Supplier<? extends @Nullable String> msgSupplier) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a FINER message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     * <p>
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    @SideEffectFree public void finer(Supplier<? extends @Nullable String> msgSupplier) { throw new RuntimeException("skeleton method"); }

    /**
     * Log a FINEST message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     * <p>
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    @SideEffectFree public void finest(Supplier<? extends @Nullable String> msgSupplier) { throw new RuntimeException("skeleton method"); }

    //================================================================
    // End of convenience methods
    //================================================================

    /**
     * Set the log level specifying which message levels will be
     * logged by this logger.  Message levels lower than this
     * value will be discarded.  The level value Level.OFF
     * can be used to turn off logging.
     * <p>
     * If the new level is null, it means that this node should
     * inherit its level from its nearest ancestor with a specific
     * (non-null) level value.
     *
     * @param newLevel   the new value for the log level (may be null)
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public void setLevel(@Nullable Level newLevel) throws SecurityException { throw new RuntimeException("skeleton method"); }

    /**
     * Get the log Level that has been specified for this Logger.
     * The result may be null, which means that this logger's
     * effective level will be inherited from its parent.
     *
     * @return  this Logger's level
     */
    @Pure
    public @Nullable Level getLevel() { throw new RuntimeException("skeleton method"); }

    /**
     * Check if a message of the given level would actually be logged
     * by this logger.  This check is based on the Loggers effective level,
     * which may be inherited from its parent.
     *
     * @param   level   a message logging level
     * @return  true if the given message level is currently being logged.
     */
    @Pure
    public boolean isLoggable(Level level) { throw new RuntimeException("skeleton method"); }

    /**
     * Get the name for this logger.
     * @return logger name.  Will be null for anonymous Loggers.
     */
    @Pure
    public @Nullable String getName() { throw new RuntimeException("skeleton method"); }

    /**
     * Add a log Handler to receive logging messages.
     * <p>
     * By default, Loggers also send their output to their parent logger.
     * Typically the root Logger is configured with a set of Handlers
     * that essentially act as default handlers for all loggers.
     *
     * @param   handler a logging Handler
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public void addHandler(Handler handler) throws SecurityException { throw new RuntimeException("skeleton method"); }

    /**
     * Remove a log Handler.
     * <P>
     * Returns silently if the given Handler is not found or is null
     *
     * @param   handler a logging Handler
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public void removeHandler(@Nullable Handler handler) throws SecurityException { throw new RuntimeException("skeleton method"); }

    /**
     * Get the Handlers associated with this logger.
     * <p>
     * @return  an array of all registered Handlers
     */
    @SideEffectFree
    public Handler[] getHandlers() { throw new RuntimeException("skeleton method"); }

    /**
     * Specify whether or not this logger should send its output
     * to its parent Logger.  This means that any LogRecords will
     * also be written to the parent's Handlers, and potentially
     * to its parent, recursively up the namespace.
     *
     * @param useParentHandlers   true if output is to be sent to the
     *          logger's parent.
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public void setUseParentHandlers(boolean useParentHandlers) { throw new RuntimeException("skeleton method"); }

    /**
     * Discover whether or not this logger is sending its output
     * to its parent logger.
     *
     * @return  true if output is to be sent to the logger's parent
     */
    @Pure
    public boolean getUseParentHandlers() { throw new RuntimeException("skeleton method"); }

    static final String SYSTEM_LOGGER_RB_NAME = "sun.util.logging.resources.logging";

    private static ResourceBundle findSystemResourceBundle(final Locale locale) { throw new RuntimeException("skeleton method"); }

    /**
     * Private utility method to map a resource bundle name to an
     * actual resource bundle, using a simple one-entry cache.
     * Returns null for a null name.
     * May also return null if we can't find the resource bundle and
     * there is no suitable previous cached value.
     *
     * @param name the ResourceBundle to locate
     * @param userCallersClassLoader if true search using the caller's ClassLoader
     * @return ResourceBundle specified by name or null if not found
     */
    private synchronized @Nullable ResourceBundle findResourceBundle(@Nullable String name,
                                                           boolean useCallersClassLoader) { throw new RuntimeException("skeleton method"); }

    // Private utility method to initialize our one entry
    // resource bundle name cache and the callers ClassLoader
    // Note: for consistency reasons, we are careful to check
    // that a suitable ResourceBundle exists before setting the
    // resourceBundleName field.
    // Synchronized to prevent races in setting the fields.
    private synchronized void setupResourceInfo(@Nullable String name,
                                                @Nullable Class<?> callersClass) { throw new RuntimeException("skeleton method"); }

    /**
     * Return the parent for this Logger.
     * <p>
     * This method returns the nearest extant parent in the namespace.
     * Thus if a Logger is called "a.b.c.d", and a Logger called "a.b"
     * has been created but no logger "a.b.c" exists, then a call of
     * getParent on the Logger "a.b.c.d" will return the Logger "a.b".
     * <p>
     * The result will be null if it is called on the root Logger
     * in the namespace.
     *
     * @return nearest existing parent Logger
     */
    @Pure
    public @Nullable Logger getParent() { throw new RuntimeException("skeleton method"); }

    /**
     * Set the parent for this Logger.  This method is used by
     * the LogManager to update a Logger when the namespace changes.
     * <p>
     * It should not be called from application code.
     * <p>
     * @param  parent   the new parent logger
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public void setParent(Logger parent) { throw new RuntimeException("skeleton method"); }

    // Private method to do the work for parenting a child
    // Logger onto a parent logger.
    private void doSetParent(Logger newParent) { throw new RuntimeException("skeleton method"); }

    // Package-level method.
    // Remove the weak reference for the specified child Logger from the
    // kid list. We should only be called from LoggerWeakRef.dispose().
    final void removeChildLogger(LogManager.LoggerWeakRef child) { throw new RuntimeException("skeleton method"); }

    // Recalculate the effective level for this node and
    // recursively for our children.

    private void updateEffectiveLevel() { throw new RuntimeException("skeleton method"); }


    // Private method to get the potentially inherited
    // resource bundle name for this Logger.
    // May return null
    //private String getEffectiveResourceBundleName() { throw new RuntimeException("skeleton method"); }


}
