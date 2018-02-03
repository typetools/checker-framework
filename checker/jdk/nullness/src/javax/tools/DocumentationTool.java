/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javax.tools;

import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.Callable;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface to invoke Java&trade; programming language documentation tools from
 * programs.
 */
public interface DocumentationTool extends Tool, OptionChecker {
    /**
     * Creates a future for a documentation task with the given
     * components and arguments.  The task might not have
     * completed as described in the DocumentationTask interface.
     *
     * <p>If a file manager is provided, it must be able to handle all
     * locations defined in {@link DocumentationTool.Location},
     * as well as
     * {@link StandardLocation#SOURCE_PATH},
     * {@link StandardLocation#CLASS_PATH}, and
     * {@link StandardLocation#PLATFORM_CLASS_PATH}.
     *
     * @param out a Writer for additional output from the tool;
     * use {@code System.err} if {@code null}
     *
     * @param fileManager a file manager; if {@code null} use the
     * tool's standard filemanager
     *
     * @param diagnosticListener a diagnostic listener; if {@code null}
     * use the tool's default method for reporting diagnostics
     *
     * @param docletClass a class providing the necessary methods required
     * of a doclet
     *
     * @param options documentation tool options and doclet options,
     * {@code null} means no options
     *
     * @param compilationUnits the compilation units to compile, {@code
     * null} means no compilation units
     *
     * @return an object representing the compilation
     *
     * @throws RuntimeException if an unrecoverable error
     * occurred in a user supplied component.  The
     * {@linkplain Throwable#getCause() cause} will be the error in
     * user code.
     *
     * @throws IllegalArgumentException if any of the given
     * compilation units are of other kind than
     * {@linkplain JavaFileObject.Kind#SOURCE source}
     */
    DocumentationTask getTask(@Nullable Writer out,
                            @Nullable JavaFileManager fileManager,
                            @Nullable DiagnosticListener<? super JavaFileObject> diagnosticListener,
                            Class<?> docletClass,
                            @Nullable Iterable<String> options,
                            @Nullable Iterable<? extends JavaFileObject> compilationUnits);

    /**
     * Gets a new instance of the standard file manager implementation
     * for this tool.  The file manager will use the given diagnostic
     * listener for producing any non-fatal diagnostics.  Fatal errors
     * will be signaled with the appropriate exceptions.
     *
     * <p>The standard file manager will be automatically reopened if
     * it is accessed after calls to {@code flush} or {@code close}.
     * The standard file manager must be usable with other tools.
     *
     * @param diagnosticListener a diagnostic listener for non-fatal
     * diagnostics; if {@code null} use the compiler's default method
     * for reporting diagnostics
     *
     * @param locale the locale to apply when formatting diagnostics;
     * {@code null} means the {@linkplain Locale#getDefault() default locale}.
     *
     * @param charset the character set used for decoding bytes; if
     * {@code null} use the platform default
     *
     * @return the standard file manager
     */
    StandardJavaFileManager getStandardFileManager(
        @Nullable DiagnosticListener<? super JavaFileObject> diagnosticListener,
        @Nullable Locale locale,
        @Nullable Charset charset);

    /**
     * Interface representing a future for a documentation task.  The
     * task has not yet started.  To start the task, call
     * the {@linkplain #call call} method.
     *
     * <p>Before calling the call method, additional aspects of the
     * task can be configured, for example, by calling the
     * {@linkplain #setLocale setLocale} method.
     */
    interface DocumentationTask extends Callable<Boolean> {
        /**
         * Set the locale to be applied when formatting diagnostics and
         * other localized data.
         *
         * @param locale the locale to apply; {@code null} means apply no
         * locale
         * @throws IllegalStateException if the task has started
         */
        void setLocale(@Nullable Locale locale);

        /**
         * Performs this documentation task.  The task may only
         * be performed once.  Subsequent calls to this method throw
         * IllegalStateException.
         *
         * @return true if and only all the files were processed without errors;
         * false otherwise
         *
         * @throws RuntimeException if an unrecoverable error occurred
         * in a user-supplied component.  The
         * {@linkplain Throwable#getCause() cause} will be the error
         * in user code.
         *
         * @throws IllegalStateException if called more than once
         */
        Boolean call();
    }

    /**
     * Locations specific to {@link DocumentationTool}.
     *
     * @see StandardLocation
     */
    enum Location implements JavaFileManager.Location {
        /**
         * Location of new documentation files.
         */
        DOCUMENTATION_OUTPUT,

        /**
         * Location to search for doclets.
         */
        DOCLET_PATH,

        /**
         * Location to search for taglets.
         */
        TAGLET_PATH;

        public String getName() { return name(); }

        public boolean isOutputLocation() {
            switch (this) {
                case DOCUMENTATION_OUTPUT:
                    return true;
                default:
                    return false;
            }
        }
    }

}
