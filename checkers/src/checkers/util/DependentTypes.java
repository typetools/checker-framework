package checkers.util;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;

import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;

import checkers.quals.Dependent;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;

public class DependentTypes {
    private final AnnotatedTypeFactory factory;
    private final AnnotationUtils annoUtils;

    public DependentTypes(ProcessingEnvironment env, CompilationUnitTree root) {
        this.factory = new AnnotatedTypeFactory(env, null, root, null);
        this.annoUtils = AnnotationUtils.getInstance(env);
    }

    AnnotationMirror getResult(Dependent anno) {
        try {
            anno.result();
        } catch (MirroredTypeException exp) {
            Name valName = TypesUtils.getQualifiedName((DeclaredType)exp.getTypeMirror());
            return annoUtils.fromName(valName);
        }
        assert false : "shouldn't be here";
        return null;
    }

    AnnotationMirror getWhen(Dependent anno) {
        try {
            anno.when();
        } catch (MirroredTypeException exp) {
            Name valName = TypesUtils.getQualifiedName((DeclaredType)exp.getTypeMirror());
            return annoUtils.fromName(valName);
        }
        assert false : "shouldn't be here";
        return null;
    }

    private Dependent findDependent(Element element) {
        return (Dependent) JavacElements.getAnnotation(((Symbol) element).typeAnnotations, Dependent.class);
    }

    public void doSubsitution(Element symbol, AnnotatedTypeMirror type, AnnotatedTypeMirror receiver) {
        Dependent dependentInfo = findDependent(symbol);
        if (dependentInfo == null)
            return;

        if (receiver == null)
            return;

        AnnotationMirror ifpresent = getWhen(dependentInfo);
        if (receiver.hasAnnotation(ifpresent)) {
            AnnotationMirror then = getResult(dependentInfo);
            type.replaceAnnotation(then);
        }
    }

    public void handle(Tree tree, AnnotatedTypeMirror type) {
        if (!(tree instanceof ExpressionTree))
            return;
        ExpressionTree expr = (ExpressionTree)tree;
        Element symbol = null;
        if (expr instanceof IdentifierTree)
            symbol = TreeUtils.elementFromUse((IdentifierTree)expr);
        else if (expr instanceof MemberSelectTree)
            symbol = TreeUtils.elementFromUse((MemberSelectTree)expr);

        if (symbol == null
                || (!symbol.getKind().isField()
                    && symbol.getKind() != ElementKind.LOCAL_VARIABLE))
            return;

        // FIXME: handle this case
        AnnotatedTypeMirror receiver = factory.getReceiverType(expr);
        if (receiver != null)
            doSubsitution(symbol, type, receiver);
    }

    public void handleConstructor(NewClassTree tree, AnnotatedExecutableType type) {
        if (!(tree.getIdentifier() instanceof AnnotatedTypeTree))
            return;
        AnnotatedTypeMirror dt = factory.getAnnotatedType(tree);

        ExecutableElement constructorElt = InternalUtils.constructor(tree);
        for (int i = 0; i < constructorElt.getParameters().size(); ++i) {
            Element parameter = constructorElt.getParameters().get(i);
            AnnotatedTypeMirror paramType = type.getParameterTypes().get(i);
            doSubsitution(parameter, paramType, dt);
        }
    }
}
