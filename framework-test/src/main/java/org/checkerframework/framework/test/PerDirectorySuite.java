package org.checkerframework.framework.test;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

// TODO: large parts of this file are the same as PerFileSuite.java.
// Reduce duplication by moving common parts to an abstract class.
/**
 * PerDirectorySuite runs a test class once for each set of javaFiles returned by its method marked
 * with {@code @Parameters}
 *
 * <p>To use:<br>
 * Annotated your test class with {@code @RunWith(PerDirectorySuite.class)}<br>
 * Create a javaFiles method by annotating a public static method with {@code @Parameters}. This
 * method must return either a {@code List<File>} where each element of the list is a Java file to
 * test against OR a {@code String []} where each String in the array is a directory in the tests
 * directory.
 */
public class PerDirectorySuite extends Suite {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Name {}

    private final ArrayList<Runner> runners = new ArrayList<Runner>();

    @Override
    protected List<Runner> getChildren() {
        return runners;
    }

    /**
     * Only called reflectively. Do not use programmatically.
     *
     * @param klass the class whose tests to run
     */
    public PerDirectorySuite(Class<?> klass) throws Throwable {
        super(klass, Collections.emptyList());
        final TestClass testClass = getTestClass();
        final Class<?> javaTestClass = testClass.getJavaClass();
        final List<List<File>> parametersList = getParametersList(testClass);

        for (List<File> parameters : parametersList) {
            runners.add(new PerParameterSetTestRunner(javaTestClass, parameters));
        }
    }

    /** Returns a list of one-element arrays, each containing a Java File. */
    private List<List<File>> getParametersList(TestClass klass) throws Throwable {
        FrameworkMethod method = getParametersMethod(klass);

        // We must have a method getTestDirs which returns String[],
        // or getParametersMethod would fail.
        if (!method.getReturnType().isArray()) {
            return new ArrayList<>();
        }
        String[] dirs = (String[]) method.invokeExplosively(null);
        return TestUtilities.findJavaFilesPerDirectory(new File("tests"), dirs);
    }

    /** Returns method annotated @Parameters, typically the getTestDirs or getTestFiles method. */
    private FrameworkMethod getParametersMethod(TestClass testClass) {
        final List<FrameworkMethod> parameterMethods =
                testClass.getAnnotatedMethods(Parameters.class);
        if (parameterMethods.size() != 1) {
            StringBuilder methods = new StringBuilder();

            if (parameterMethods.isEmpty()) {
                methods.append("[No methods specified]");
            } else {
                boolean first = true;
                for (FrameworkMethod method : parameterMethods) {
                    if (!first) {
                        methods.append(", ");
                    } else {
                        first = false;
                    }
                    methods.append(method.getName());
                }
            }

            throw new RuntimeException(
                    "Exactly one of the following methods should be declared:\n"
                            + requiredFormsMessage
                            + "\n"
                            + "testClass="
                            + testClass.getName()
                            + "\n"
                            + "parameterMethods="
                            + methods.toString());
        } // else

        FrameworkMethod method = parameterMethods.get(0);

        Class<?> returnType = method.getReturnType();
        String methodName = method.getName();
        switch (methodName) {
            case "getTestDirs":
                if (returnType.isArray()) {
                    if (!returnType.getComponentType().equals(String.class)) {
                        throw new RuntimeException(
                                "Component type of getTestDirs must be java.lang.String, found "
                                        + returnType.getComponentType().getCanonicalName());
                    }
                }
                break;

            default:
                throw new RuntimeException(
                        requiredFormsMessage
                                + "\n"
                                + "testClass="
                                + testClass.getName()
                                + "\n"
                                + "parameterMethods="
                                + method.toString());
        }

        int modifiers = method.getMethod().getModifiers();
        if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            throw new RuntimeException(
                    "Parameter method (" + method.getName() + ") must be public and static");
        }

        return method;
    }

    private static final String requiredFormsMessage =
            "Parameter method must the following form:\n" + "@Parameters String [] getTestDirs()";

    /** Runs the test class for the set of javaFiles passed in the constructor. */
    private static class PerParameterSetTestRunner extends BlockJUnit4ClassRunner {
        private final List<File> javaFiles;

        PerParameterSetTestRunner(Class<?> type, List<File> javaFiles) throws InitializationError {
            super(type);
            this.javaFiles = javaFiles;
        }

        @Override
        public Object createTest() throws Exception {
            Object[] arguments = Collections.singleton(javaFiles).toArray();
            return getTestClass().getOnlyConstructor().newInstance(arguments);
        }

        String testCaseName() {
            File file = javaFiles.get(0).getParentFile();
            return file.getPath().replace("tests" + System.getProperty("file.separator"), "");
        }

        @Override
        protected String getName() {
            return String.format("[%s]", testCaseName());
        }

        @Override
        protected String testName(final FrameworkMethod method) {
            return String.format("%s[%s]", method.getName(), testCaseName());
        }

        @Override
        protected void validateZeroArgConstructor(List<Throwable> errors) {
            // constructor should have args.
        }

        @Override
        protected Statement classBlock(RunNotifier notifier) {
            return childrenInvoker(notifier);
        }
    }
}
