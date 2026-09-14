package lib;

/**
 * Issue8055.java's {@code @run main DeleteMissingClassFile} deletes this class's class file after
 * the library is compiled, simulating a module that compiles against a jar whose own transitive
 * dependency is not on the compile classpath.
 */
public class Missing {}
