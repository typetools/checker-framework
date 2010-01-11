package checkers.javari;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.*;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.annotation.processing.ProcessingEnvironment;

import checkers.quals.ReadOnly;
import checkers.quals.Mutable;
import checkers.quals.RoMaybe;
import checkers.quals.QReadOnly;

import checkers.source.SourceVisitor;
import checkers.source.SourceChecker;
import checkers.types.*;

import com.sun.source.tree.CompilationUnitTree;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.type.TypeKind;

/**
 * An annotation processor that checks a program's use of the Javari
 * mutability type annotations ({@code @ReadOnly} and {@code @Mutable}).
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({"checkers.quals.ReadOnly",
                               "checkers.quals.Mutable",
                               "checkers.quals.QReadOnly",
                               "checkers.quals.RoMaybe",
                               "checkers.quals.Assignable"})
public class JavariChecker extends SourceChecker {

    @Override
    protected Properties getMessages(String fileName) throws IOException {

        // Use a message file if one exists.
        if (new File(fileName).exists()){
            String path = System.getProperty("messages", ".");
            Properties p = new Properties();
            p.load(new FileInputStream(path + File.separator + fileName));
            return p;
        }

        // Sets message defaults.
        Properties msgDefaults = new Properties();
        msgDefaults.setProperty("assignment.invalid", "cannot assign a ReadOnly expression to a Mutable variable");
        msgDefaults.setProperty("argument.invalid", "cannot pass ReadOnly argument as Mutable parameter");
        msgDefaults.setProperty("receiver.invalid", "cannot declare a method with Mutable receiver inside a ReadOnly type");
        msgDefaults.setProperty("return.invalid", "cannot return a ReadOnly value as Mutable");
        msgDefaults.setProperty("extends.invalid", "cannot reduce mutability of superclass");
        msgDefaults.setProperty("implements.invalid", "cannot reduce mutability of interface");
        msgDefaults.setProperty("override.param.invalid", "cannot override parameter increasing mutability");
        msgDefaults.setProperty("override.return.invalid", "cannot override return value reducing mutability");
        msgDefaults.setProperty("override.throws.invalid", "cannot override throw type reducing mutability");
        msgDefaults.setProperty("override.receiver.invalid", "cannot override receiver type increasing mutability");
        msgDefaults.setProperty("mutable.cast", "cast increases mutability access");
        msgDefaults.setProperty("supertype.loss", "mutability constraints are lost when assigning to supertype");
        msgDefaults.setProperty("primitive.ro", "cannot declare primitives as ReadOnly");
        msgDefaults.setProperty("ro.field", "a field of a ReadOnly object is not assignable");
        msgDefaults.setProperty("ro.reference", "cannot invoke a Mutable method from a ReadOnly reference");
        msgDefaults.setProperty("ro.and.mutable", "a type cannot be declared ReadOnly and Mutable");
        msgDefaults.setProperty("romaybe.only", "a RoMaybe type cannot be declared as ReadOnly or Mutable");
        msgDefaults.setProperty("romaybe.type", "a type can only be declared as RoMaybe inside a method with RoMaybe receiver type");

        return msgDefaults;
    }

    @Override
    public SourceVisitor getSourceVisitor(CompilationUnitTree root) {
        return new JavariVisitor(this, root);
    }

    /**
     * Overrides getFactory to produce a {@link JavariAnnotatedTypeFactory}.
     *
     * @param env
     *            the {@link ProcessingEnvironment} instance to use
     * @param root
     *            the root of the syntax tree that this factory produces
     *            annotated types for
     * @throws IllegalArgumentException
     *             if either argument is {@code null}.
     */
    @Override
    public AnnotatedTypeFactory getFactory(ProcessingEnvironment env, CompilationUnitTree root) {
        return new JavariAnnotatedTypeFactory(env, root);
    }


    /**
     * Determines whether the first type is a subtype of the second type
     * with respect to the annotations on raw types.  It ignores the actual
     * type and only checks annotations, and is only concerned with annotations
     * on raw types (it ignores all generic and array annotations).
     *
     * @param child the type that may or may not be a subtype
     * @param parent the parent type
     * @return true if {@code child} is a subtype of {@code parent} with respect
     *         to its type qualifier annotations, false otherwise
     */
    @Deprecated
    public boolean isSubtypeIgnoringTypeParameters(AnnotatedClassType child, AnnotatedClassType parent) {

        return isSubtypeAt(child, parent, AnnotationLocation.RAW);
    }

    private boolean isSubtypeAt(AnnotatedClassType child,
                                AnnotatedClassType parent,
                                AnnotationLocation location) {

        // anything can be assigned to ? readonly
        if (child.hasAnnotationAt(QReadOnly.class, location))
            return true;

        if (child.hasAnnotationAt(Mutable.class, location)) {
            if (parent.hasAnnotationAt(Mutable.class, location))
                return true;
            if (parent.hasAnnotationAt(ReadOnly.class, location))
                return location.equals(AnnotationLocation.RAW)
                    || parent.getElement().asType().getKind()
                    == TypeKind.ARRAY;
            if (parent.hasAnnotationAt(RoMaybe.class, location))
                return true;
        }

        if (child.hasAnnotationAt(ReadOnly.class, location)) {
            return parent.hasAnnotationAt(ReadOnly.class, location);
        }

        if (child.hasAnnotationAt(RoMaybe.class, location)) {
            if (parent.hasAnnotationAt(Mutable.class, location))
                return false;
            if (parent.hasAnnotationAt(ReadOnly.class, location))
                return location.equals(AnnotationLocation.RAW);
            if (parent.hasAnnotationAt(RoMaybe.class, location))
                return true;
        }

        return true;
    }


    /**
     * Determines whether the first type is a subtype of the second type.
     * It ignores the actual type and only checks annotations.
     *
     * @param child the type that may or may not be a subtype
     * @param parent the parent type
     * @return true if {@code child} is a subtype of {@code parent} with respect
     *         to its type qualifier annotations, false otherwise
     */
    public boolean isSubtype(AnnotatedClassType child, AnnotatedClassType parent) {

        // FIXME: this needs to check checkers.skipClasses!
        if (child.getElement() != null) {
            Elements elts = env.getElementUtils();
            Element childElt = child.getElement();
            if (elts.getPackageOf(childElt).getQualifiedName().toString().startsWith("java"))
                return true;
        }

        // Check raw type, ignoring generics/arrays.
        if (!isSubtypeAt(child, parent, AnnotationLocation.RAW))
            return false;

        for (AnnotationLocation location : parent.getAnnotatedLocations()) {
            if (!isSubtypeAt(child, parent, location))
                return false;
        }

        for (AnnotationLocation location : child.getAnnotatedLocations()) {
            if (!isSubtypeAt(child, parent, location))
                return false;
        }

        return true;
    }


    /**
     * Checks whether an AnnotatedClassType refers to a primitive type
     * marked as {@code @ReadOnly} (which is illegal).
     *
     * @param type the (@link AnnotatedClassType} to check
     * @return true if the type refers to a primitive and marked as
     * {@code @ReadOnly}.
     */
    public boolean isPrimitiveReadOnly(AnnotatedClassType type) {
        Element typeElt = type.getElement();
        if (typeElt instanceof VariableElement) {
            if (typeElt.asType().getKind().isPrimitive())
                return type.hasAnnotationAt(ReadOnly.class,
                                            AnnotationLocation.RAW);
        }
        return false;
    }

}
