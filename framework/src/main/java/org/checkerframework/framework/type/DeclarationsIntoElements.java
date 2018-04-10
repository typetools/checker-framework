package org.checkerframework.framework.type;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;

/**
 * A helper class that puts the declaration annotations from a method declaration back into the
 * corresponding Elements, so that they get stored in the bytecode by the compiler.
 *
 * <p>This is similar to {@code TypesIntoElements} but for declaration annotations.
 *
 * <p>This class deals with javac internals and liberally imports such classes.
 */
public class DeclarationsIntoElements {

    /**
     * The entry point.
     *
     * @param atypeFactory the type factory
     * @param tree the ClassTree to process
     */
    public static void store(
            ProcessingEnvironment env, AnnotatedTypeFactory atypeFactory, ClassTree tree) {
        for (Tree mem : tree.getMembers()) {
            if (mem.getKind() == Tree.Kind.METHOD) {
                storeMethod(env, atypeFactory, (MethodTree) mem);
            }
        }
    }

    /**
     * Add inherited declaration annotations from overridden methods into the corresponding Elements
     * so they are written into bytecode.
     *
     * @param env ProcessingEnvironment
     * @param atypeFactory the type factory
     * @param meth the MethodTree to add the annotations
     */
    private static void storeMethod(
            ProcessingEnvironment env, AnnotatedTypeFactory atypeFactory, MethodTree meth) {
        ExecutableElement element = TreeUtils.elementFromDeclaration(meth);
        MethodSymbol sym = (MethodSymbol) element;
        java.util.List<? extends AnnotationMirror> elementAnnos = element.getAnnotationMirrors();

        Set<AnnotationMirror> declAnnotations = atypeFactory.getDeclAnnotations(sym);
        List<Compound> tcs = List.nil();

        for (AnnotationMirror anno : declAnnotations) {
            // Only add the annotation if it isn't in the Element already.
            if (!AnnotationUtils.containsSame(elementAnnos, anno)) {
                tcs = tcs.append(TypeAnnotationUtils.createCompoundFromAnnotationMirror(anno, env));
            }
        }

        sym.appendAttributes(tcs);
    }
}
