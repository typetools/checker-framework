package org.checkerframework.framework.test;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * <p>TestSuite runs a test class once for each set of parameters returned by its method marked with {@code, @Parameter}</p>
 * <p>To use:<br/>
 *  Annotated your test class with {@code, @RunWith(TestSuite.class)}<br/>
 *  Create a parameters method by annotating a public static method with {@code @Parameters}.  This method
 *  must return a {@code, List<Object[]>} where each array is a parameter set to instantiate the test class.
 * </p>
 */
public class TestSuite extends Suite {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Name {}

    /**
     * Only called reflectively. Do not use programmatically.
     */
    public TestSuite(Class<?> klass) throws Throwable {
        super(klass, Collections.<Runner>emptyList());
        final TestClass testClass = getTestClass();
        final Class<?> javaTestClass = testClass.getJavaClass();
        final List<Object[]> parametersList= getParametersList(testClass);

        for (Object [] parameters : parametersList) {
            runners.add(new PerParameterSetTestRunner(javaTestClass, parameters));
        }
    }

    private final ArrayList<Runner> runners = new ArrayList<Runner>();

    @Override
    protected List<Runner> getChildren() {
        return runners;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> getParametersList(TestClass klass) throws Throwable {
        return (List<Object[]>) getParametersMethod(klass).invokeExplosively(null);
    }

    private FrameworkMethod getParametersMethod(TestClass testClass) {
        final List<FrameworkMethod> parameterMethods = testClass.getAnnotatedMethods(Parameters.class);
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

            throw new RuntimeException("There should be exactly 1 method annotated with Parameters!:\n"
                                     + "testClass=" + testClass.getName() + "\n"
                                     + "paremeterMethods=" + methods.toString()
            );
        } //else

        FrameworkMethod method = parameterMethods.get(0);
        int modifiers = method.getMethod().getModifiers();
        if (!Modifier.isStatic(modifiers) || ! Modifier.isPublic(modifiers)) {
            throw new RuntimeException("Parameter method (" + method.getName() +") must be public and static");
        }

        return method;
    }


    /**
     * Runs the test class for the set of parameters passed in the constructor.
     */
    private class PerParameterSetTestRunner extends
            BlockJUnit4ClassRunner {
        private final Object[] parameters;

        PerParameterSetTestRunner(Class<?> type, Object[] parameters) throws InitializationError {
            super(type);
            this.parameters = parameters;
        }

        @Override
        public Object createTest() throws Exception {
            return getTestClass().getOnlyConstructor().newInstance(parameters);
        }

        String testCaseName() {
            File file = (File) parameters[0];
            String name = file.getPath().replace(".java", "").replace("tests" + System.getProperty("file.separator"), "");
            return name;
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
