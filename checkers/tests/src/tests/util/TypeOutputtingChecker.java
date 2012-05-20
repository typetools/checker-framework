package tests.util;

import java.util.Collections;


import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;


/**
 * A testing class that can be used to test {@link TypeElement}.  In particular
 * it tests that the types read from classfiles are the same to the ones
 * from java files.
 *
 * For testing, you need to do the following:
 * 1. Run the Checker on the source file like any checker:
 *    <pre><code>
 *      java -processor tests.util.TypeOutputtingChecker [source-file]
 *    </code></pre>
 *
 * 2. Run the Checker on the bytecode, by simply running the main and passing
 *    the qualified name, e.g.
 *    <pre><code>
 *      java tests.util.TypeOutputtingChecker [qualified-name]
 *    </code></pre>
 *
 * 3. Apply a simple diff on the two outputs
 *
 */
public class TypeOutputtingChecker extends SourceChecker {

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new Visitor(this, root);
    }

    /**
     * Prints the types of the class and all of its enclosing
     * fields, methods, and inner classes
     */
    public static class Visitor extends SourceVisitor<Void, Void> {
        String currentClass;

        public Visitor(SourceChecker checker, CompilationUnitTree root) {
            super(checker, root);
        }

        // Print types of classes, methods, and fields
        @Override
        public Void visitClass(ClassTree node, Void p) {
            TypeElement element = TreeUtils.elementFromDeclaration(node);
            currentClass = element.getSimpleName().toString();

            AnnotatedDeclaredType type = atypeFactory.getAnnotatedType(node);
            System.out.println(node.getSimpleName() + "\t" + type + "\t" + type.directSuperTypes());

            return super.visitClass(node, p);
        }

        @Override
        public Void visitMethod(MethodTree node, Void p) {
            ExecutableElement elem = TreeUtils.elementFromDeclaration(node);

            AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node);
            System.out.println(currentClass + "." + elem + "\t\t" + type);
            // Don't dig deeper
            return null;
        }

        @Override
        public Void visitVariable(VariableTree node, Void p) {
            VariableElement elem = TreeUtils.elementFromDeclaration(node);
            if (elem.getKind().isField()) {
                AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node);
                System.out.println(currentClass + "." + elem + "\t\t" + type);
            }

            // Don't dig deeper
            return null;
        }

    }

    public static void main(String[] args) {
        ProcessingEnvironment env =
            new JavacProcessingEnvironment(new Context(), Collections.<Processor>emptyList());
        Elements elements = env.getElementUtils();

        AnnotatedTypeFactory atypeFactory = new AnnotatedTypeFactory(env, null, null, null);

        for (String className : args) {
            TypeElement typeElt = elements.getTypeElement(className);
            printClassType(typeElt, atypeFactory);
        }
    }

    /**
     * Prints the types of the class and all of its enclosing
     * fields, methods, and inner classes
     */
    protected static void printClassType(TypeElement typeElt, AnnotatedTypeFactory atypeFactory) {
        assert typeElt != null;

        String simpleName = typeElt.getSimpleName().toString();
        // Output class info
        AnnotatedDeclaredType type = atypeFactory.fromElement(typeElt);
        System.out.println(simpleName + "\t" + type + "\t" + type.directSuperTypes());

        // output fields and methods
        for (Element enclosedElt : typeElt.getEnclosedElements()) {
            if (enclosedElt instanceof TypeElement)
                printClassType((TypeElement)enclosedElt, atypeFactory);
            if (!enclosedElt.getKind().isField()
                    && !(enclosedElt instanceof ExecutableElement))
                continue;
            AnnotatedTypeMirror memberType = atypeFactory.fromElement(enclosedElt);
            System.out.println(simpleName + "." + enclosedElt + "\t\t" + memberType);
        }
    }
}
