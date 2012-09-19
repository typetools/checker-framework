package checkers.util;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;

import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;

import checkers.quals.Dependent;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.GeneralAnnotatedTypeFactory;

public class DependentTypes {
    private final Elements elements;
    private final GeneralAnnotatedTypeFactory atypeFactory;

    public DependentTypes(SourceChecker checker, CompilationUnitTree root) {
        this.elements = checker.getProcessingEnvironment().getElementUtils();
        this.atypeFactory = new GeneralAnnotatedTypeFactory(checker, root);
    }

    private AnnotationMirror getResult(Dependent anno) {
        try {
            anno.result();
        } catch (MirroredTypeException exp) {
            // TODO: find nicer way to access Class annotation attributes.
            Name valName = TypesUtils.getQualifiedName((DeclaredType)exp.getTypeMirror());
            return AnnotationUtils.fromName(elements, valName);
        }
        assert false : "shouldn't be here";
        return null;
    }

    private AnnotationMirror getWhen(Dependent anno) {
        try {
            anno.when();
        } catch (MirroredTypeException exp) {
            // TODO: find nicer way to access Class annotation attributes.
            Name valName = TypesUtils.getQualifiedName((DeclaredType)exp.getTypeMirror());
            return AnnotationUtils.fromName(elements, valName);
        }
        assert false : "shouldn't be here";
        return null;
    }

    private static Dependent findDependent(Element element) {
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
        if (!TreeUtils.isExpressionTree(tree))
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
        AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(expr);
        if (receiver != null)
            doSubsitution(symbol, type, receiver);
    }

    public void handleConstructor(NewClassTree tree, AnnotatedTypeMirror ctr, AnnotatedExecutableType type) {
        ExecutableElement constructorElt = InternalUtils.constructor(tree);
        for (int i = 0; i < constructorElt.getParameters().size(); ++i) {
            Element parameter = constructorElt.getParameters().get(i);
            AnnotatedTypeMirror paramType = type.getParameterTypes().get(i);
            doSubsitution(parameter, paramType, ctr);
        }
    }
}
