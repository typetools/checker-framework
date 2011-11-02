package checkers.nullness;

import static checkers.util.Heuristics.Matchers.*;

import java.util.List;
import java.util.Arrays;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;

import checkers.nullness.quals.KeyFor;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeFactory;
import checkers.util.AnnotationUtils;
import checkers.util.Heuristics.Matcher;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;
import checkers.util.Resolver;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;

/**
 * Utility class for handling {@code System.getProperty(String)} invocations.
 *
 * If the argument is a literal key that is guaranteed to be present in the
 * system properties may (according to the documentation of
 * System.getProperties), as in
 * <pre>System.getProperties("line.separator")</pre>
 * , then the result of the method call is assumed to be non-null.
 */

/*package-scope*/ class SystemGetPropertyHandler {

    private final ProcessingEnvironment env;
    private final NullnessAnnotatedTypeFactory factory;
    private final AnnotatedTypeFactory keyForFactory;
    private final Resolver resolver;

    private final ExecutableElement systemGetProperty;

    // This list is from the Javadoc of System.getProperties.
    // I'm assuming they are all non-null.
    // (For efficiency, could use a TreeSet or HashSet.)
    List<String> systemProperties
        = Arrays.asList(
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
                        "user.dir"
                        );

    public SystemGetPropertyHandler(ProcessingEnvironment env,
            NullnessAnnotatedTypeFactory factory,
            AnnotatedTypeFactory keyForFactory) {
        this.env = env;
        this.factory = factory;
        this.keyForFactory = keyForFactory;
        this.resolver = new Resolver(env);

        systemGetProperty = TreeUtils.getMethod("java.lang.System", "getProperty", 1, env);
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
                    type.clearAnnotations();
                    type.addAnnotation(factory.NONNULL);
                }
            }
        }
    }

}
