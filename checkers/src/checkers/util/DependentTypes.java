package checkers.util;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.Dependent;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.GeneralAnnotatedTypeFactory;

import javacutils.AnnotationUtils;
import javacutils.InternalUtils;
import javacutils.TreeUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.util.Elements;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;

public class DependentTypes {
    private final Elements elements;
    private final GeneralAnnotatedTypeFactory atypeFactory;

    public DependentTypes(BaseTypeChecker checker) {
        this.elements = checker.getProcessingEnvironment().getElementUtils();
        this.atypeFactory = new GeneralAnnotatedTypeFactory(checker);
    }

    private AnnotationMirror getResult(AnnotationMirror anno) {
        Name valName = AnnotationUtils.getElementValueClassName(anno, "result", false);
        return AnnotationUtils.fromName(elements, valName);
    }

    private AnnotationMirror getWhen(AnnotationMirror anno) {
        Name valName = AnnotationUtils.getElementValueClassName(anno, "when", false);
        return AnnotationUtils.fromName(elements, valName);
    }

    private AnnotationMirror findDependent(Element element) {
        // TODO: does this work with a .astub file?
        List<TypeCompound> tas = ((Symbol) element).getRawTypeAttributes();
        for (TypeCompound ta : tas) {
            if (ta.getAnnotationType().toString().equals(Dependent.class.getCanonicalName())) {
                return ta;
            }
        }
        return null;
    }

    public void doSubsitution(Element symbol, AnnotatedTypeMirror type, AnnotatedTypeMirror receiver) {
        AnnotationMirror dependentInfo = findDependent(symbol);
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
            symbol = TreeUtils.elementFromUse(expr);
        else if (expr instanceof MemberSelectTree)
            symbol = TreeUtils.elementFromUse(expr);

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
