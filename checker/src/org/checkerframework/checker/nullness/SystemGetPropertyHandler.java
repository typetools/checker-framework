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
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Utility class for handling {@link java.lang.System#getProperty(String)} invocations.
 *
 * <p>If the argument is a literal key that is guaranteed to be present in the system properties may
 * (according to the documentation of {@link java.lang.System#getProperty(String)}), as in {@code
 * System.getProperties("line.separator")}, then the result of the method call is assumed to be
 * non-null.
 */
public class SystemGetPropertyHandler {

    protected final ProcessingEnvironment env;
    protected final NullnessAnnotatedTypeFactory factory;

    protected final ExecutableElement systemGetProperty;

    // This list is from the Javadoc of System.getProperties.
    Collection<String> systemProperties =
            new HashSet<>(
                    Arrays.asList(
                            "java.version",
                            "java.vendor",
                            "java.vendor.url",
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
                            "java.ext.dirs",
                            "os.name",
                            "os.arch",
                            "os.version",
                            "file.separator",
                            "path.separator",
                            "line.separator",
                            "user.name",
                            "user.home",
                            "user.dir"));

    public SystemGetPropertyHandler(
            ProcessingEnvironment env, NullnessAnnotatedTypeFactory factory) {
        this.env = env;
        this.factory = factory;

        systemGetProperty =
                TreeUtils.getMethod(java.lang.System.class.getName(), "getProperty", 1, env);
    }

    public void handle(MethodInvocationTree tree, AnnotatedExecutableType method) {
        if (TreeUtils.isMethodInvocation(tree, systemGetProperty, env)) {
            List<? extends ExpressionTree> args = tree.getArguments();
            assert args.size() == 1;
            ExpressionTree arg = args.get(0);
            if (arg.getKind() == Tree.Kind.STRING_LITERAL) {
                String literal = (String) ((LiteralTree) arg).getValue();
                if (systemProperties.contains(literal)) {
                    AnnotatedTypeMirror type = method.getReturnType();
                    type.replaceAnnotation(factory.NONNULL);
                }
            }
        }
    }
}
