package checkers.igj;

import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeVisitor;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror;

/**
 * A type-checking visitor for the IGJ type
 * qualifier that uses the {@link BaseTypeVisitor} implementation. This visitor
 * reports errors or warnings for violations for the following cases:
 *
 * <ol>
 * <li value="1">constructing an infeasible type
 * </ol>
 *
 * @see BaseTypeVisitor
 */
public class IGJVisitor extends BaseTypeVisitor<Void, Void> {
    IGJChecker checker;

    public IGJVisitor(IGJChecker checker, CompilationUnitTree root) {
        super(checker, root);
        this.checker = checker;

        checkForAnnotatedJdk();
    }

    private static boolean checkedJDK = false;

    /** Warn if the annotated JDK is not being used. */
    private void checkForAnnotatedJdk() {
        if (checkedJDK) {
            return;
        }
        checkedJDK = true;
        TypeElement objectTE = elements.getTypeElement("java.lang.Object");
        TypeMirror objectTM = objectTE.asType();
        AnnotatedTypeMirror objectATM = plainFactory.toAnnotatedType(objectTM);
        List<? extends Element> members = elements.getAllMembers(objectTE);
        for (Element member : members) {
            if (member.toString().equals("equals(java.lang.Object)")) {
                ExecutableElement m = (ExecutableElement) member;
                AnnotatedTypeMirror.AnnotatedExecutableType objectEqualsAET = annoTypes.asMemberOf(objectATM, m);
                AnnotatedDeclaredType objectEqualsParamADT = (AnnotatedDeclaredType) objectEqualsAET.getParameterTypes().get(0);
                if (! objectEqualsParamADT.hasAnnotation(checkers.igj.quals.ReadOnly.class)) {
                    // TODO: Use standard compiler output mechanism?
                    System.out.printf("Warning:  you do not seem to be using the IGJ-annotated JDK.%nSupply javac the argument:  -Xbootclasspath/p:.../checkers/jdk/jdk.jar%n");
                }
            }
        }
    }

    @Override
    protected boolean checkConstructorInvocation(AnnotatedDeclaredType dt,
            AnnotatedExecutableType constructor, Tree src) {
        Collection<AnnotationMirror> annos = constructor.getReceiverType().getAnnotations();
        if (annos.contains(checker.I) || annos.contains(checker.ASSIGNS_FIELDS))
            return true;
        else
            return super.checkConstructorInvocation(dt, constructor, src);
    }
}
