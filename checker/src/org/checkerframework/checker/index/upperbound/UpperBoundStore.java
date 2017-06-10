package org.checkerframework.checker.index.upperbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;

public class UpperBoundStore extends CFAbstractStore<CFValue, UpperBoundStore> {

    private static String NO_REASSIGN = "reassignment.not.permitted";

    public UpperBoundStore(UpperBoundAnalysis analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    public UpperBoundStore(
            UpperBoundAnalysis analysis, CFAbstractStore<CFValue, UpperBoundStore> other) {
        super(other);
    }

    @Override
    public void updateForAssignment(Node n, CFValue val) {
        // Do reassignment things here.

        super.updateForAssignment(n, val);

        System.out.println(n);
        System.out.println(n.getClass());

        // This code determines the list of dependences in types that are to be invalidated
        boolean includeName = false;
        boolean includeAllMethodCalls = false;
        boolean includeNonFinalRefs = false; // not arrays, though

        if (n.getType().getKind() == TypeKind.ARRAY) {
            if (n instanceof LocalVariableNode) {
                includeName = true;
            }
            if (n instanceof FieldAccessNode) {
                //if (!((FieldAccessNode) n).getElement().getModifiers().contains(Modifier.PRIVATE)) { <- Design doc includes this but I don't think it's correct
                includeName = true;
                includeAllMethodCalls = true;
                //}
            }
        } else {
            if (n instanceof FieldAccessNode) {
                if (!n.getType().getKind().isPrimitive()) {
                    includeAllMethodCalls = true;
                    includeNonFinalRefs = true;
                }
            }
        }

        // Find all possibly-invalidated types

        Element elt;

        // So that assignments into arrays are treated correctly, as well as type casts
        Node oldN = n;
        while (n instanceof TypeCastNode || n instanceof ArrayAccessNode) {
            if (n instanceof TypeCastNode) {
                n = ((TypeCastNode) n).getOperand();
            }
            if (n instanceof ArrayAccessNode) {
                n = ((ArrayAccessNode) n).getArray();
            }
        }
        if (n instanceof FieldAccessNode) {
            elt = ((FieldAccessNode) n).getElement();
        } else if (n instanceof LocalVariableNode) {
            elt = ((LocalVariableNode) n).getElement();
        } else {
            assert false; // for testing, should be removed before PR
            return; // can't get an element, so there's nothing to do.
        }
        n = oldN;

        List<? extends Element> enclosedElts =
                ElementUtils.enclosingClass(elt).getEnclosedElements();
        List<AnnotatedTypeMirror> enclosedTypes = new ArrayList<AnnotatedTypeMirror>();
        for (Element e : enclosedElts) {
            AnnotatedTypeMirror atm = analysis.getTypeFactory().getAnnotatedType(e);
            enclosedTypes.add(atm);
            System.out.println("elt: " + e);
            System.out.println("atm: " + atm);
        }

        if (includeName) {
            FlowExpressions.Receiver rec =
                    FlowExpressions.internalReprOf(analysis.getTypeFactory(), n);
            String canonicalTargetName = rec.toString();

            clearNameFromStore(canonicalTargetName, n);
            checkForRemainingNameAnnotations(enclosedTypes, canonicalTargetName, n);
        }
    }

    boolean annoContainsCanonicalName(AnnotationMirror anno, String canonicalTargetName, Node n) {
        if (AnnotationUtils.hasElementValue(anno, "value")) {
            List<String> strings = IndexUtil.getValueOfAnnotationWithStringArgument(anno);
            List<String> canonicalStrings = new ArrayList<>();
            for (String s : strings) {
                try {
                    FlowExpressions.Receiver r =
                            analysis.getTypeFactory()
                                    .getReceiverFromJavaExpressionString(
                                            s, analysis.getTypeFactory().getPath(n.getTree()));
                    canonicalStrings.add(r.toString());
                } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {

                }
            }
            return canonicalStrings.contains(canonicalTargetName);
        }
        return false;
    }

    void checkForRemainingNameAnnotations(
            List<AnnotatedTypeMirror> enclosedTypes, String canonicalTargetName, Node n) {
        System.out.println(
                "entering checkForRemainingNameAnnos looking for " + canonicalTargetName);
        for (AnnotatedTypeMirror atm : enclosedTypes) {
            System.out.println("checking this atm: " + atm);
            for (AnnotationMirror anno : atm.getAnnotations()) {
                System.out.println("checking this anno: " + anno);
                if (annoContainsCanonicalName(anno, canonicalTargetName, n)) {
                    ((UpperBoundAnalysis) analysis)
                            .getChecker()
                            .report(
                                    Result.failure(
                                            NO_REASSIGN,
                                            canonicalTargetName,
                                            anno.toString(),
                                            canonicalTargetName),
                                    n.getTree());
                }
            }
        }
    }

    void buildClearList(
            Map<? extends FlowExpressions.Receiver, CFValue> map,
            List<FlowExpressions.Receiver> toClear,
            String canonicalTargetName,
            Node n) {
        for (FlowExpressions.Receiver r : map.keySet()) {
            Set<AnnotationMirror> annos = map.get(r).getAnnotations();
            for (AnnotationMirror anno : annos) {
                if (annoContainsCanonicalName(anno, canonicalTargetName, n)) {
                    toClear.add(r);
                }
            }
        }
    }

    void clearNameFromStore(String canonicalTargetName, Node n) {

        List<FlowExpressions.Receiver> toClear = new ArrayList<>();
        buildClearList(localVariableValues, toClear, canonicalTargetName, n);
        buildClearList(methodValues, toClear, canonicalTargetName, n);
        buildClearList(classValues, toClear, canonicalTargetName, n);
        buildClearList(fieldValues, toClear, canonicalTargetName, n);
        buildClearList(arrayValues, toClear, canonicalTargetName, n);

        for (FlowExpressions.Receiver r : toClear) {
            this.clearValue(r);
        }
    }
}
