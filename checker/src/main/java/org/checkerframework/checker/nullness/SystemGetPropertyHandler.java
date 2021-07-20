package org.checkerframework.checker.nullness;

import com.sun.source.tree.MethodInvocationTree;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;

/**
 * Utility class for handling {@link java.lang.System#getProperty(String)} and related invocations.
 *
 * <p>The result of the method call is is assumed to be non-null if the argument is a literal key
 * that is guaranteed to be present in the system properties (according to the documentation of
 * {@link java.lang.System#getProperty(String)}), as in {@code
 * System.getProperties("line.separator")}.
 */
public class SystemGetPropertyHandler {

    /**
     * If true, client code may clear system properties, and this class (SystemGetPropertyHandler)
     * has no effect.
     */
    private final boolean permitClearProperty;

    /** The processing environment. */
    private final ProcessingEnvironment env;

    /** The factory for constructing and looking up types. */
    private final NullnessAnnotatedTypeFactory factory;

    /** The System.getProperty(String) method. */
    private final ExecutableElement systemGetProperty;

    /** The System.setProperty(String) method. */
    private final ExecutableElement systemSetProperty;

    /**
     * System properties that are defined at startup on every JVM.
     *
     * <p>This list is from the Javadoc of System.getProperties, for Java 11.
     */
    public static final Collection<String> predefinedSystemProperties =
            new HashSet<>(
                    Arrays.asList(
                            "java.version",
                            "java.version.date",
                            "java.vendor",
                            "java.vendor.url",
                            "java.vendor.version",
                            "java.home",
                            "java.vm.specification.version",
                            "java.vm.specification.vendor",
                            "java.vm.specification.name",
                            "java.vm.version",
                            "java.vm.vendor",
                            "java.vm.name",
                            "java.specification.version",
                            "java.specification.vendor",
                            "java.specification.name",
                            "java.class.version",
                            "java.class.path",
                            "java.library.path",
                            "java.io.tmpdir",
                            "java.compiler",
                            "os.name",
                            "os.arch",
                            "os.version",
                            "file.separator",
                            "path.separator",
                            "line.separator",
                            "user.name",
                            "user.home",
                            "user.dir"));

    /**
     * Creates a SystemGetPropertyHandler.
     *
     * @param env the processing environment
     * @param factory the factory for constructing and looking up types
     * @param permitClearProperty if true, client code may clear system properties, and this object
     *     does nothing
     */
    public SystemGetPropertyHandler(
            ProcessingEnvironment env,
            NullnessAnnotatedTypeFactory factory,
            boolean permitClearProperty) {
        this.env = env;
        this.factory = factory;
        this.permitClearProperty = permitClearProperty;

        systemGetProperty = TreeUtils.getMethod("java.lang.System", "getProperty", 1, env);
        systemSetProperty = TreeUtils.getMethod("java.lang.System", "setProperty", 2, env);
    }

    /**
     * Apply rules regarding System.getProperty and related methods.
     *
     * @param tree a method invocation
     * @param method the method being invoked
     */
    public void handle(MethodInvocationTree tree, AnnotatedExecutableType method) {
        if (permitClearProperty) {
            return;
        }
        if (TreeUtils.isMethodInvocation(tree, systemGetProperty, env)
                || TreeUtils.isMethodInvocation(tree, systemSetProperty, env)) {
            String literal = NullnessVisitor.literalFirstArgument(tree);
            if (literal != null && predefinedSystemProperties.contains(literal)) {
                AnnotatedTypeMirror type = method.getReturnType();
                type.replaceAnnotation(factory.NONNULL);
            }
        }
    }
}
