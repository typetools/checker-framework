package checkers.util;

import javax.lang.model.element.*;
import javax.lang.model.util.Elements;

import com.sun.source.tree.*;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;

import javacutils.AnnotationUtils;
import javacutils.InternalUtils;
import javacutils.TreeUtils;

import checkers.quals.Dependent;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.GeneralAnnotatedTypeFactory;

public class DependentTypes {
    private final Elements elements;

    // TODO: the implementation of this class needs some serious refactoring
    // and thought (and documentation...).
    // @Dependent expresses the relationship between two separate
    // type systems. It is not enough to use the GeneralATF, which doesn't
    // apply defaulting appropriate for the second type system.
    // This issue is similar to the interaction between @Unused and
    // the Nullness Checker.
    private final GeneralAnnotatedTypeFactory atypeFactory;

    public DependentTypes(SourceChecker<?> checker, CompilationUnitTree root) {
        this.elements = checker.getProcessingEnvironment().getElementUtils();
        this.atypeFactory = new GeneralAnnotatedTypeFactory(checker, root);
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
            symbol = TreeUtils.elementFromUse((IdentifierTree)expr);
        else if (expr instanceof MemberSelectTree)
            symbol = TreeUtils.elementFromUse((MemberSelectTree)expr);

        if (symbol == null
                || (!symbol.getKind().isField()
                    && symbol.getKind() != ElementKind.LOCAL_VARIABLE))
            return;

        AnnotatedTypeMirror receiver;
        try {
            // Ugly hack to not crash type checking if the GeneralATF
            // runs into some problem determining the receiver type.
            // TODO: remove GeneralATF, see note with field.
            receiver = atypeFactory.getReceiverType(expr);
        } catch (Throwable t) {
            receiver = null;
        }

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
