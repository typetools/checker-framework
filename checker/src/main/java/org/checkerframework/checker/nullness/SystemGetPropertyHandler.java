package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Utility class for handling {@link java.lang.System#getProperty(String)} invocations.
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
    protected final boolean permitClearProperty;

    /** The processing environment. */
    protected final ProcessingEnvironment env;

    /** The factory for constructing and looking up types. */
    protected final NullnessAnnotatedTypeFactory factory;

    /** The checker for issuing errors. */
    protected final BaseTypeChecker checker;

    /** The System.getProperty(String) method. */
    protected final ExecutableElement systemGetProperty;

    /** The System.clearProperty(String) method. */
    protected final ExecutableElement systemClearProperty;

    /** The System.setProperties(String) method. */
    protected final ExecutableElement systemSetProperties;

    /**
     * System properties that are defined at startup on every JVM.
     *
     * <p>This list is from the Javadoc of System.getProperties, for Java 11.
     */
    Collection<String> predefinedSystemProperties =
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
     * @param checker the checker for issuing errors
     */
    public SystemGetPropertyHandler(
            ProcessingEnvironment env,
            NullnessAnnotatedTypeFactory factory,
            BaseTypeChecker checker) {
        this.permitClearProperty =
                checker.getLintOption(
                        NullnessChecker.LINT_PERMITCLEARPROPERTY,
                        NullnessChecker.LINT_DEFAULT_PERMITCLEARPROPERTY);
        this.env = env;
        this.factory = factory;
        this.checker = checker;

        systemGetProperty =
                TreeUtils.getMethod(java.lang.System.class.getName(), "getProperty", 1, env);
        systemClearProperty =
                TreeUtils.getMethod(java.lang.System.class.getName(), "clearProperty", 1, env);
        systemSetProperties =
                TreeUtils.getMethod(java.lang.System.class.getName(), "setProperties", 1, env);
    }

    /**
     * If the first argument of a method call is a literal, return it; otherwise return null.
     *
     * @param tree a method invocation whose first formal parameter is of String type
     * @return the first argument if it is a literal, otherwise null
     */
    private static @Nullable String literalFirstArgument(MethodInvocationTree tree) {
        List<? extends ExpressionTree> args = tree.getArguments();
        assert args.size() == 1;
        ExpressionTree arg = args.get(0);
        if (arg.getKind() == Tree.Kind.STRING_LITERAL) {
            String literal = (String) ((LiteralTree) arg).getValue();
            return literal;
        }
        return null;
    }

    /**
     * Apply rules relating to System.getProperty.
     *
     * @param tree a method invocation
     * @param method the method being invoked
     */
    public void handle(MethodInvocationTree tree, AnnotatedExecutableType method) {
        if (permitClearProperty) {
            return;
        }
        if (TreeUtils.isMethodInvocation(tree, systemGetProperty, env)) {
            String literal = literalFirstArgument(tree);
            if (literal != null && predefinedSystemProperties.contains(literal)) {
                AnnotatedTypeMirror type = method.getReturnType();
                type.replaceAnnotation(factory.NONNULL);
            }
        }
        if (TreeUtils.isMethodInvocation(tree, systemClearProperty, env)) {
            String literal = literalFirstArgument(tree);
            if (literal == null || predefinedSystemProperties.contains(literal)) {
                checker.reportError(tree, "clear.system.property");
            }
        }
        if (TreeUtils.isMethodInvocation(tree, systemSetProperties, env)) {
            checker.reportError(tree, "clear.system.property");
        }
    }
}
