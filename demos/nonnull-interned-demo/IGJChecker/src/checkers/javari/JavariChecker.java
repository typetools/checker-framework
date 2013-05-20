package checkers.javari;

import checkers.basetype.*;
import checkers.source.SuppressWarningsKey;
import checkers.types.*;
import checkers.quals.*;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;

import com.sun.source.tree.*;

/**
 * An annotation processor that checks a program's use of the Javari
 * type annotations ({@code @ReadOnly}, {@code @Mutable}, {@code @Assignable},
 * {@code @RoMaybe} and {@code @QReadOnly}). For specific
 * instructions on the usage of each of these annotations, see the
 * Javari language specification.
 */
@SupportedAnnotationTypes({"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SuppressWarningsKey("javari")
public class JavariChecker extends BaseTypeChecker {

    protected AnnotationMirror READONLY, THISMUTABLE, MUTABLE, ROMAYBE, QREADONLY, ASSIGNABLE;
    private JavariSubtypeRelation relation;

    /**
     * Initializes the checker: calls init method on super class,
     * creates a local AnnotationFactory based on the processing
     * environment, and uses it to create the protected
     * AnnotationMirrors used through this checker.
     * @param processingEnv the processing environment to use in the local AnnotationFactory
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        AnnotationFactory annoFactory = new AnnotationFactory(this.env);
        this.READONLY = annoFactory.fromName(ReadOnly.class.getCanonicalName());
        this.THISMUTABLE = annoFactory.fromName(ThisMutable.class.getCanonicalName());
        this.MUTABLE = annoFactory.fromName(Mutable.class.getCanonicalName());
        this.ROMAYBE = annoFactory.fromName(RoMaybe.class.getCanonicalName());
        this.QREADONLY = annoFactory.fromName(QReadOnly.class.getCanonicalName());
        this.ASSIGNABLE = annoFactory.fromName(Assignable.class.getCanonicalName());
        relation = new JavariSubtypeRelation(READONLY, THISMUTABLE, MUTABLE, ROMAYBE, QREADONLY, ASSIGNABLE);
    }

    /**
     * Uses an AnnotatedTypePairScanner with this checker to check
     * whether the two type trees are compatible.
     *
     * @param variable the AnnotatedTypeMirror for the variable
     * @param value the AnnotatedTypeMirror for the value
     * @return true if the trees are compatible, false otherwise.
     */
    @Override
    public boolean isSubtype(AnnotatedTypeMirror variable, AnnotatedTypeMirror value) {
        return relation.isSubtype(variable, value);
    }

    @Override
    public boolean isValidUse(AnnotatedTypeMirror elemType, AnnotatedTypeMirror useType) {
        if (useType.hasAnnotation(READONLY) || elemType.hasAnnotation(ROMAYBE) ||
                useType.hasAnnotation(ROMAYBE))
            return true;
        else
            return isSubtype(elemType, useType);
    }

    /**
     * Provides specific error messages to the Javari checker, from a file or from default values.
     *
     * @return a Properties object with the error messages set.
     */
    @Override
    protected Properties getMessages() {

        // Sets message defaults.
        Properties msgDefaults = super.getMessages();
        //msgDefaults.setProperty("type.incompatible", "cannot assign a ReadOnly expression to a Mutable variable");
        //msgDefaults.setProperty("assignment.invalid", "cannot assign a ReadOnly expression to a Mutable variable");
        //msgDefaults.setProperty("argument.invalid", "cannot pass ReadOnly argument as Mutable parameter");
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

    /**
     * Returns a new JavariVisitor.
     *
     * @param root the CompilationUnitTree to use in the visitor initialization.
     * @return a new JavariVisitor.
     */
    @Override
    public JavariVisitor getSourceVisitor(CompilationUnitTree root) {
        return new JavariVisitor(this, root);
    }

    /**
     * Returns a new JavariAnnotatedTypeFactory.
     *
     * @param env the ProcessingEnvironment to use in the factory initialization.
     * @param root the CompilationTreeUnit to use in the factory initialization.
     * @return a new JavariAnnotatedTypeFactory.
     */
    @Override
    public JavariAnnotatedTypeFactory getFactory(ProcessingEnvironment env, CompilationUnitTree root) {
        return new JavariAnnotatedTypeFactory(env, root);
    }
}
