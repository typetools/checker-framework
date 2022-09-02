import java.io.IOException;
import java.io.InputStream;

/**
 * Tests that the detector works properly in the presence of multiple identical return statements in
 * a method
 */
class MultipleIdenticalReturns {
    static class Repro {
        private java.lang.ClassLoader loader;

        public Object loadClass(final String className) throws ClassNotFoundException {
            final String classFile = className.replace('.', '/');
            Object RC = null;
            if (RC != null) {
                return RC;
            }
            try (InputStream is = loader.getResourceAsStream(classFile + ".class")) {
                // no warning here, since parse() is invoked in parser
                ClassParser parser = new ClassParser();
                RC = parser.parse();
                return RC;
            } catch (final IOException e) {
                throw new ClassNotFoundException(className + " not found: " + e, e);
            }
        }
    }

    @org.checkerframework.checker.mustcall.qual.InheritableMustCall("parse")
    static class ClassParser {
        public ClassParser() {}

        public Object parse() {
            return null;
        }
    }
}
