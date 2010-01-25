package checkers.util;

import checkers.quals.*;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.signature.*;
import org.objectweb.asm.util.*;


/**
 * Generates a "skeleton" class, in which the bodies of non-abstract methods
 * are replaced by a stub code (in this case, throwing a {@link
 * RuntimeException}).
 *
 * <p>
 *
 * This class is useful for adding annotations to the public fields and method
 * signatures of libraries. A programmer may generate skeleton classes for a
 * library using this tool and annotate its methods and fields. Then, a
 * programmer can compile against these skeleton classes for typechecking (and
 * must remove them when running the program, so that the stub methods are not
 * invoked instead of the library's real methods).
 */
public class Skeleton implements ClassVisitor {

    /** A writer for outputting the skeleton class. */
    private final PrintWriter pw;
    
    /** The name of the class. */
    private final String name;

    /** The string for initial indentation. */
    private final String base; 

    /** The string for extra indentation. */
    private final String tab; 

    /** A buffer for locally storing output. */
    private final StringBuffer buf;

    /** A queue of strings to output (after ASM's TraceClassVisitor). */
    private final List<String> text;
    
    /** A set of class names that have already been visited and can be skipped. */
    private final Set<String> visited;

    private boolean aborted = false;
    
    /**
     * Creates a new skeleton class generator.
     *
     * @param name the name of the class
     * @param base the initial indentation
     * @param tab the extra indentation
     * @param pw the writer for outputting the skeleton class
     * @param visited the set of classes that have already been visited
     */
    public Skeleton(String name, String base, String tab, PrintWriter pw, Set<String> visited) {
        this.pw = pw;
        this.name = name.replace('$', '.');
        this.base = base;
        this.tab = tab;
        this.buf = new StringBuffer();
        this.text = new LinkedList<String>();
        if (visited == null)
            this.visited = new HashSet<String>();
        else
            this.visited = visited;
    }

    /*
     * Prints the skeleton class for the class named by the first command-line
     * argument to standard out, including its package definition.
     */
    public static void main(final String[] args) throws Exception {

        if (args.length == 0 || args.length > 2) {
            System.err.println("usage: Skeleton [class name] [jar file]");
            return;
        }
        
        String baseDir = null;
        
        if (args.length == 1) {
            PrintWriter pw = new PrintWriter(System.out);
            Class<?> cls = Class.forName(args[0]);
            pw.println("package " + cls.getPackage().getName() + ";\n");
            Skeleton.read(args[0], "", "  ", pw, new HashSet<String>());
        } else if (args.length == 2) {
            List<String> classes = getFiles(args[0], args[1], true);
            for (String className : classes) {
                createDirForClass(className, baseDir);
                Class<?> cls = Class.forName(className);
                PrintWriter pw = new PrintWriter(new File(getClassFileName(className)));
                pw.println("package " + cls.getPackage().getName() + ";\n");
                Skeleton.read(className, "", "  ", pw, new HashSet<String>());
                System.out.println("Output : " + className);
            }
        }
    }
    
    public static String getClassFileName(String className) {
        String classFileName = className.replace(".", "/");
        classFileName = classFileName + ".java";
        return classFileName;
    }
    
    public static boolean createDirForClass(String className, String baseDir) {
        // Ignore className
        String finalDirectory = className.substring(0, className.lastIndexOf('.'));
        finalDirectory = finalDirectory.replace('.', '/');

        if (baseDir != null) {
            if (baseDir.endsWith("/"))
                finalDirectory = baseDir + finalDirectory;
            else
                finalDirectory = baseDir + '/' + finalDirectory;
        }
        
        return new File(finalDirectory).mkdirs();
    }
    
    public static List<String> getFiles(String packageName, String jar, boolean subpackage) throws Exception {
        List<String> results = new ArrayList<String>();
        packageName = packageName.replaceAll("\\.$", "");
        
        if (!jar.endsWith(".jar"))
            return Collections.<String>emptyList();
        
        JarFile jarFile = new JarFile(jar);
        for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
            JarEntry entry = e.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith(".class")) {
                String className = entryName.replaceAll("\\.class$", "").replace("/", ".");
                if (className.contains("$")) {
                    // inner class
                    continue;
                }
                if (".".equals(packageName) || className.startsWith(packageName)) {
                    if (!subpackage && className.substring(packageName.length() + 1).contains("."))
                        continue;
                    results.add(className);
                }
            }
        }
        return results;
    }

    /**
     * Reads a class with the given name and prints its skeleton to the given
     * {@link PrintWriter}. 
     *
     * @param className the name of the class for which to generate a skeleton
     * @param base the initial indentation
     * @param tab the extra indentation
     * @param pw the writer on which to output the skeleton
     * @param visited the set of visited classes that should not be read
     */
    public static void read(final String className, final String base, final
            String tab, PrintWriter pw, /*@NonNull*/ Set<String> visited) throws Exception {

        if (visited.contains(className))
            return;

        ClassReader cr = new ClassReader(className);
        visited.add(className);
        cr.accept(new Skeleton(className, base, tab, pw, visited), false); 
    }

    /**
     * Reads a class with the given name and prints its skeleton to the given
     * {@link PrintWriter}. 
     *
     * @param className the name of the class for which to generate a skeleton
     * @param base the initial indentation
     * @param tab the extra indentation
     * @param pw the writer on which to output the skeleton
     * @param visited the set of visited classes that should not be read
     */
    public static void read(final InputStream classStream, String className, final String base, final
            String tab, PrintWriter pw, /*@NonNull*/ Set<String> visited) throws Exception {

        if (visited.contains(className))
            return;

        ClassReader cr = new ClassReader(classStream);
        visited.add(className);
        cr.accept(new Skeleton(className, base, tab, pw, visited), false); 
    }

    /**
     * A class that prints a Java signature from a JVM signature with named
     * arguments.
     */
    static class ArgumentSignatureVisitor extends TraceSignatureVisitor {

        private int pos = 0;
        private int arg = 0;
        private boolean finished = true;
        private boolean skip = false;
        private int skipCount = 0;
        private final StringBuffer local;

        /**
         * Creates an {@link ArgumentSignatureVisitor} for a signature with the
         * given flags.
         *
         * @param access the signature's flags
         */
        public ArgumentSignatureVisitor(int access) {
            super(access);
            local = new StringBuffer();
        }
        
        /**
         * @return the parameter string with named arguments
         */
        public String getParams() {
            return local.toString();
        }
        
        /**
         * Updates the buffer with the parent's buffer, possibly adding a named
         * argument.
         *
         * @param addArg whether or not a named argument should be added
         */
        private void updateLocal(boolean addArg) {
            if (finished || skip) return;
            local.append(super.getDeclaration().substring(pos));
            pos = super.getDeclaration().length();
            if (addArg)
                local.append(" a" + (++arg));
        }

        @Override
        public SignatureVisitor visitParameterType() {
            finished = false;
            super.visitParameterType();
            local.append(super.getDeclaration().substring(pos)); 
            pos = super.getDeclaration().length();
            return this;
        }

        @Override
        public SignatureVisitor visitReturnType() {
            super.visitReturnType();
            updateLocal(false);
            finished = true;
            return this;
        }
        @Override
        public void visitBaseType(char descriptor) {
            super.visitBaseType(descriptor);
            updateLocal(true);
        }

        @Override
        public void visitClassType(String name) {
            if (skip == true)
                skipCount++;
            super.visitClassType(name);
        }
        
        @Override
        public void visitTypeArgument() {
            super.visitTypeArgument();
        }

        @Override
        public SignatureVisitor visitTypeArgument(char tag) {
            skip = true;
            super.visitTypeArgument(tag);
            return this;
        }

        @Override
        public void visitTypeVariable(String name) {
            super.visitTypeVariable(name);
            updateLocal(true);
        }

        @Override
        public void visitEnd() {
            skip = false;
            super.visitEnd();
            updateLocal(skipCount == 0);
            if (skipCount > 0) skipCount--;
        }
    }
    
    /**
     * Appends modifiers to the local buffer from the given set of flags.
     *
     * @param access the modifier flags
     * @param suppress flags for which no modifier should be added
     */
    private void appendAccess(final int access, final int suppress) {
        if (((access & ~suppress) & Opcodes.ACC_PUBLIC) != 0)
            buf.append("public ");
        if (((access & ~suppress) & Opcodes.ACC_PRIVATE) != 0) 
            buf.append("private "); 
        if (((access & ~suppress) & Opcodes.ACC_PROTECTED) != 0)
            buf.append("protected ");
        if (((access & ~suppress) & Opcodes.ACC_FINAL) != 0)
            buf.append("final ");
        if (((access & ~suppress) & Opcodes.ACC_STATIC) != 0)
            buf.append("static ");
        if (((access & ~suppress) & Opcodes.ACC_SYNCHRONIZED) != 0)
            buf.append("synchronized ");
        if (((access & ~suppress) & Opcodes.ACC_VOLATILE) != 0)
            buf.append("volatile ");
        if (((access & ~suppress) & Opcodes.ACC_TRANSIENT) != 0)
            buf.append("transient ");
        if (((access & ~suppress) & Opcodes.ACC_NATIVE) != 0)
            buf.append("native ");
        if (((access & ~suppress) & Opcodes.ACC_ABSTRACT) != 0)
            buf.append("abstract ");
        if (((access & ~suppress) & Opcodes.ACC_STRICT) != 0)
            buf.append("strictfp ");
        if (((access & ~suppress) & Opcodes.ACC_SYNTHETIC) != 0)
            buf.append("synthetic ");
    }

    /**
     * Appends the correct keyword for the type of class (class, interface,
     * annotation, enum).
     *
     * @param flags the modifier flags
     */
    private void appendType(final int flags) {
       if ((flags & Opcodes.ACC_ANNOTATION) != 0)
           buf.append("@interface");
       else if ((flags & Opcodes.ACC_INTERFACE) != 0)
           buf.append("interface ");
       else if ((flags & Opcodes.ACC_ENUM) != 0)
           buf.append("enum ");
       else 
           buf.append("class ");
       
    }

    /**
     * Appends the class name to the local buffer.
     *
     * @param name the class name
     */
    private void appendName(final String name) {
        buf.append(name.replace('/', '.').replace('$', '.'));
    }
    
    /**
     * Given signatures with and without generics as passed to the visitor (one
     * or both of which may be null), determines which one should be used for
     * printing.
     * 
     * @param signature the signature with generics
     * @param desc the signature without generics
     * @return the signature that should be used, or null if neither should
     */
    private final String chooseSignature(final String signature, final String desc) {
        if (signature != null)
            return signature;
        if (desc != null)
            return desc;
        return null;
    }
    
    /**
     * Appends to the local buffer the type variables for a class or field
     * declaration. 
     *
     * @param access the modifier flags
     * @param signature the signature with generics
     */
    private void appendGenerics(final int access, final String signature) {
        if (signature == null) return;
        TraceSignatureVisitor sv = new TraceSignatureVisitor(access);
        SignatureReader r = new SignatureReader(signature);
        r.accept(sv);
        buf.append(sv.getDeclaration()).append(" ");
    }

    /** 
     * Appends to the local buffer the field type for a field declaration.
     *
     * @param access the modifier flags
     * @param desc the field type description
     */
    private void appendFieldType(final int access, final String desc) {
        if (desc == null) return;
        TraceSignatureVisitor sv = new TraceSignatureVisitor(access);
        SignatureReader r = new SignatureReader(desc);
        r.acceptType(sv);
        buf.append(sv.getDeclaration()).append(" ");
    }

    /**
     * Appends to the local buffer the return type of a method.
     *
     * @param access the modifier flags
     * @param signature the (possibly null) signature with generics
     * @param desc the (possibly null) signature without generics
     */
    private void appendReturnType(final int access, final String signature,
            final String desc) {

        // Get the correct signature string.
        String str = chooseSignature(signature, desc);
        if (str == null) return;

        // Get the return type from the signature.
        TraceSignatureVisitor sv = new TraceSignatureVisitor(access);
        SignatureReader r = new SignatureReader(str);
        r.accept(sv);
        String ret = sv.getReturnType();

        // Adjust the return type string if necessary.
        if ("[]".equals(ret))
            buf.append("java.lang.Object[] ");
        else if ("".equals(ret))
            buf.append("java.lang.Object ");
        else buf.append(ret).append(" ");
    }

    private String formalAndTypeParams(final int access, final String
            signature, final String desc) {
        String str = chooseSignature(signature, desc);
        if (str == null) return null;
        
        ArgumentSignatureVisitor sv = new ArgumentSignatureVisitor(access);
        SignatureReader r = new SignatureReader(str);
        r.accept(sv);

        return sv.getParams();
    }

    /**
     * Appends to the local buffer the type parameters of a method.
     *
     * @param access the modifier flags
     * @param signature the (possibly null) signature with generics
     * @param desc the (possibly null) signature without generics
     */
    private void appendMethodTypeParams(final int access, final String
            signature, final String desc) { 
        
        String params = formalAndTypeParams(access, signature, desc);
        if (params == null) return;
        
        int paren = params.indexOf("(");
        if (paren > 0)
            buf.append(params.substring(0, paren)).append(" ");
    }

    /**
     * Appends to the local buffer the parameter type of a method (with named
     * arguments).
     *
     * @param access the modifier flags
     * @param signature the (possibly null) signature with generics
     * @param desc the (possibly null) signature without generics
     */
    private void appendParamType(final int access, final String signature,
            final String desc) {

        String allparams = formalAndTypeParams(access, signature, desc);
        if (allparams == null) return;

        // Adjust param string if necessary.
        if (allparams.length() == 0)
            buf.append("()");
        else {
            int paren = allparams.indexOf("(");
            String params = allparams.substring(paren);
            buf.append(params);
        }
    }
    
    public void visit(final int version, final int access, final String name,
            final String signature, final String superName, final String[] interfaces) {
        if ((access & Opcodes.ACC_PUBLIC) == 0) {
            this.aborted = true;
            return;
        }
        buf.setLength(0);
        buf.append(base);
        appendAccess(access, Opcodes.ACC_SYNCHRONIZED);
        appendType(access);
        appendName(this.name.substring(this.name.lastIndexOf('.')+1));
        appendGenerics(access, signature);
        buf.append("{\n");
        text.add(buf.toString());
    }
    
    public void visitSource(final String file, final String debug) {
        // Do nothing.
    }

    public void visitOuterClass(final String owner, final String name, final
            String desc) {
        // Do nothing.
    }
    
    public AnnotationVisitor visitAnnotation( final String desc, final boolean
            visible) {
        return new EmptyVisitor();
    }
    
    public ExtendedAnnotationVisitor visitExtendedAnnotation( final String
            desc, final boolean visible) {
        return new EmptyVisitor();
    }

    public void visitAttribute(final Attribute attr) {
        // Do nothing.
    }
    
    public void visitInnerClass(final String name, final String outerName,
            final String innerName, final int access) {
        if (this.aborted) return;
        if (this.name.equals(name.replace('/', '.')) || 
                !name.replace('/', '.').startsWith(this.name))
            return;
        try {
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            read(name.replace('/', '.'), base + tab, tab, new PrintWriter(ba), this.visited);
            buf.setLength(0);
            buf.append(ba.toString());
        } catch (Exception e) {
            buf.append("// couldn't read inner class ").append(name);
        }
        text.add(buf.toString());
    }
    
    public FieldVisitor visitField(int access, String name, String desc, String
            signature, Object value) {
        if (this.aborted)
            return new EmptyVisitor();
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            buf.setLength(0);
            buf.append(base).append(tab);
            appendAccess(access, 0);
            appendFieldType(access, desc);
            appendGenerics(access, signature);

            appendName(name);
            if (value != null) {
                buf.append(" = ");
                buf.append(value);
            }
            buf.append(";\n");
            text.add(buf.toString());
        }
        
        return new EmptyVisitor();
    }

    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        if (this.aborted || name.equals("<clinit>") || name.equals("clone")
                || (access & Opcodes.ACC_NATIVE) != 0
                || (access & Opcodes.ACC_SYNTHETIC) != 0)
            return new EmptyVisitor();
        
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            buf.setLength(0);
            buf.append(base).append(tab);

            appendAccess(access, Opcodes.ACC_TRANSIENT | Opcodes.ACC_VOLATILE);
            appendMethodTypeParams(access, signature, desc);

            String methodName;
            if (name.equals("<init>"))
                appendName(this.name.substring(this.name.lastIndexOf('.')+1));
            else {
                appendReturnType(access, signature, desc);
                appendName(name);
            }

            appendParamType(access, signature, desc);

            if (exceptions != null && exceptions.length > 0) {
                buf.append(" throws ");
                appendName(exceptions[0]);
                for (int i = 1; i < exceptions.length; i++) {
                    buf.append(", "); 
                    appendName(exceptions[i]);
                }
            }
            if ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0)
                buf.append(";");
            else
                buf.append(" { throw new RuntimeException(\"skeleton method\"); }");
                
            buf.append("\n");
            text.add(buf.toString());
        }

        return new EmptyVisitor();
    }

    public void visitEnd() {
        if (this.aborted) return;
        text.add(base);
        text.add("}\n");
        for (Object o : text)
            pw.print(o.toString());
        pw.flush();
    }
}
