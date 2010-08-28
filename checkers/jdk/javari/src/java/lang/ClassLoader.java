// This class could be better annotated.
package java.lang;
import checkers.javari.quals.*;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Enumeration;

public abstract class ClassLoader {
    protected ClassLoader(ClassLoader parent) {
        throw new RuntimeException("skeleton method");
    }

    protected ClassLoader() {
        throw new RuntimeException("skeleton method");
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        throw new RuntimeException("skeleton method");
    }

    protected Object getClassLoadingLock(String className) {
        throw new RuntimeException("skeleton method");
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    @Deprecated
    protected final Class<?> defineClass(byte[] b, int off, int len)
        throws ClassFormatError
    {
        throw new RuntimeException("skeleton method");
    }

    protected final Class<?> defineClass(String name, byte[] b, int off, int len)
        throws ClassFormatError
    {
        throw new RuntimeException("skeleton method");
    }

    protected final Class<?> defineClass(String name, byte[] b, int off, int len,
                                         ProtectionDomain protectionDomain)
        throws ClassFormatError
    {
        throw new RuntimeException("skeleton method");
    }

    protected final Class<?> defineClass(String name, java.nio.ByteBuffer b,
                                         ProtectionDomain protectionDomain)
        throws ClassFormatError
    {
        throw new RuntimeException("skeleton method");
    }

    protected final void resolveClass(Class<?> c) {
        throw new RuntimeException("skeleton method");
    }

    protected final Class<?> findSystemClass(String name)
        throws ClassNotFoundException
    {
        throw new RuntimeException("skeleton method");
    }

    protected final Class<?> findLoadedClass(String name) {
        throw new RuntimeException("skeleton method");
    }

    protected final void setSigners(Class<?> c, Object[] signers) {
        throw new RuntimeException("skeleton method");
    }

    public URL getResource(String name) {
        throw new RuntimeException("skeleton method");
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    protected URL findResource(String name) {
        throw new RuntimeException("skeleton method");
    }

    protected Enumeration<URL> findResources(String name) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    protected static boolean registerAsParallelCapable() {
        throw new RuntimeException("skeleton method");
    }

    public static URL getSystemResource(String name) {
        throw new RuntimeException("skeleton method");
    }

    public static Enumeration<URL> getSystemResources(String name)
        throws IOException
    {
        throw new RuntimeException("skeleton method");
    }

    public InputStream getResourceAsStream(String name) {
        throw new RuntimeException("skeleton method");
    }

    public static InputStream getSystemResourceAsStream(String name) {
        throw new RuntimeException("skeleton method");
    }

    public final ClassLoader getParent() {
        throw new RuntimeException("skeleton method");
    }

    public static ClassLoader getSystemClassLoader() {
        throw new RuntimeException("skeleton method");
    }

    protected Package definePackage(String name, String specTitle,
                                    String specVersion, String specVendor,
                                    String implTitle, String implVersion,
                                    String implVendor, URL sealBase)
        throws IllegalArgumentException
    {
        throw new RuntimeException("skeleton method");
    }

    protected Package getPackage(String name) {
        throw new RuntimeException("skeleton method");
    }

    protected Package[] getPackages() {
        throw new RuntimeException("skeleton method");
    }

    protected String findLibrary(String libname) {
        throw new RuntimeException("skeleton method");
    }

    public void setDefaultAssertionStatus(boolean enabled) {
        throw new RuntimeException("skeleton method");
    }

    public void setPackageAssertionStatus(String packageName,
                                          boolean enabled) {
        throw new RuntimeException("skeleton method");
    }

    public void setClassAssertionStatus(String className, boolean enabled) {
        throw new RuntimeException("skeleton method");
    }

    public void clearAssertionStatus() {
        throw new RuntimeException("skeleton method");
    }
}
