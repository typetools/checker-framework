package org.checkerframework.checker.index.upperbound;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;

public class UpperBoundStore extends CFAbstractStore<CFValue, UpperBoundStore> {

    private static /*@CompilerMessageKey*/ String NO_REASSIGN = "reassignment.not.permitted";
    private static /*@CompilerMessageKey*/ String NO_REASSIGN_FIELD =
            "reassignment.field.not.permitted";
    private static /*@CompilerMessageKey*/ String SIDE_EFFECTING_METHOD =
            "side.effect.invalidation";
    private static /*@CompilerMessageKey*/ String NO_REASSIGN_FIELD_METHOD =
            "reassignment.field.not.permitted.method";

    private enum CheckController {
        NAME_ONLY,
        NAME_AND_METHOD_CALLS,
        NONFINAL_REFS_AND_METHOD_CALLS,
        NONFINAL_REFS_AND_METHOD_CALLS_METHOD
    }

    public UpperBoundStore(UpperBoundAnalysis analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    public UpperBoundStore(
            UpperBoundAnalysis analysis, CFAbstractStore<CFValue, UpperBoundStore> other) {
        super(other);
    }

    @Override
    public void updateForMethodCall(
            MethodInvocationNode n, AnnotatedTypeFactory factory, CFValue val) {
        super.updateForMethodCall(n, factory, val);

        ExecutableElement elt = n.getTarget().getMethod();

        if (!isSideEffectFree(factory, elt)) {
            List<? extends Element> enclosedElts =
                    ElementUtils.enclosingClass(elt).getEnclosedElements();
            List<AnnotatedTypeMirror> enclosedTypes = new ArrayList<AnnotatedTypeMirror>();

            findEnclosedTypes(enclosedTypes, enclosedElts);

            // Should include all method calls and non-array references, but not the name of the method.
            clearFromStore("", n, CheckController.NONFINAL_REFS_AND_METHOD_CALLS_METHOD);
            checkForRemainingAnnotations(
                    enclosedTypes, "", n, CheckController.NONFINAL_REFS_AND_METHOD_CALLS_METHOD);
        }
    }

    @Override
    public void updateForAssignment(Node n, CFValue val) {
        // Do reassignment things here.
        super.updateForAssignment(n, val);

        // This code determines the list of dependences in types that are to be invalidated
        CheckController checkController = null;

        if (n.getType().getKind() == TypeKind.ARRAY) {
            if (n instanceof LocalVariableNode) {
                // Do not warn about assigning to a final variable. javac handles this.
                if (!ElementUtils.isFinal(((LocalVariableNode) n).getElement())) {
                    checkController = CheckController.NAME_ONLY;
                }
            }
            if (n instanceof FieldAccessNode) {
                // Do not warn about assigning to a final field. javac handles this.
                if (!ElementUtils.isFinal(((FieldAccessNode) n).getElement())) {
                    checkController = CheckController.NAME_AND_METHOD_CALLS;
                }
            }
        } else {
            if (n instanceof FieldAccessNode) {
                if (!n.getType().getKind().isPrimitive()) {
                    if (!ElementUtils.isFinal(((FieldAccessNode) n).getElement())) {
                        checkController = CheckController.NONFINAL_REFS_AND_METHOD_CALLS;
                    }
                }
            }
        }

        // Find all possibly-invalidated types
        if (checkController != null) {

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

            findEnclosedTypes(enclosedTypes, enclosedElts);

            FlowExpressions.Receiver rec =
                    FlowExpressions.internalReprOf(analysis.getTypeFactory(), n);
            String canonicalTargetName = rec.toString();

            clearFromStore(canonicalTargetName, n, checkController);
            checkForRemainingAnnotations(enclosedTypes, canonicalTargetName, n, checkController);
        }
    }

    void findEnclosedTypes(
            List<AnnotatedTypeMirror> enclosedTypes, List<? extends Element> enclosedElts) {
        for (Element e : enclosedElts) {
            AnnotatedTypeMirror atm = analysis.getTypeFactory().getAnnotatedType(e);
            enclosedTypes.add(atm);
            if (e.getKind() == ElementKind.METHOD) {
                ExecutableElement ee = (ExecutableElement) e;
                List<? extends Element> rgparam = ee.getParameters();
                for (Element param : rgparam) {
                    AnnotatedTypeMirror atmP = analysis.getTypeFactory().getAnnotatedType(param);
                    enclosedTypes.add(atmP);
                }
            }
        }
    }

    private boolean checkAnno(
            AnnotationMirror anno,
            String canonicalTargetName,
            Node n,
            CheckController checkController,
            boolean report) {
        if (AnnotationUtils.hasElementValue(anno, "value")) {
            UBQualifier.LessThanLengthOf qual =
                    (UBQualifier.LessThanLengthOf) UBQualifier.createUBQualifier(anno);

            List<String> strings = new ArrayList<>();
            for (String s : qual.getArrays()) {
                strings.add(s);
            }
            strings.addAll(qual.getOffsetsAsStrings());

            List<String> canonicalStrings = new ArrayList<>();
            for (String s : strings) {
                try {
                    FlowExpressions.Receiver r =
                            analysis.getTypeFactory()
                                    .getReceiverFromJavaExpressionString(
                                            s, analysis.getTypeFactory().getPath(n.getTree()));
                    canonicalStrings.add(r.toString());
                    while (r instanceof FlowExpressions.FieldAccess) {
                        // I included an exception here for "this", because otherwise the rules don't make sense - any
                        // field, including final ones, would be invalidated, since it's impossible to know if this is
                        // final or not. But this can't be modified by calling other code, so it's fine.
                        if (checkController == CheckController.NONFINAL_REFS_AND_METHOD_CALLS
                                || checkController
                                        == CheckController.NONFINAL_REFS_AND_METHOD_CALLS_METHOD) {
                            if (!((FlowExpressions.FieldAccess) r)
                                            .getReceiver()
                                            .toString()
                                            .equals("this")
                                    && !((FlowExpressions.FieldAccess) r).isFinal()) {
                                if (report
                                        && checkController
                                                != CheckController
                                                        .NONFINAL_REFS_AND_METHOD_CALLS_METHOD) {
                                    ((UpperBoundAnalysis) analysis)
                                            .getChecker()
                                            .report(
                                                    Result.failure(
                                                            NO_REASSIGN_FIELD, anno.toString()),
                                                    n.getTree());
                                } else if (report) {
                                    ((UpperBoundAnalysis) analysis)
                                            .getChecker()
                                            .report(
                                                    Result.failure(
                                                            SIDE_EFFECTING_METHOD, anno.toString()),
                                                    n.getTree());
                                }
                                return true;
                            } else {
                                FlowExpressions.Receiver oldR = r;
                                r =
                                        analysis.getTypeFactory()
                                                .getReceiverFromJavaExpressionString(
                                                        ((FlowExpressions.FieldAccess) r)
                                                                .getField()
                                                                .toString(),
                                                        analysis.getTypeFactory()
                                                                .getPath(n.getTree()));
                                if (oldR.equals(r)) {
                                    break;
                                }
                            }
                        } else {
                            break;
                        }
                    }
                    if (r instanceof FlowExpressions.MethodCall) {
                        if (checkController == CheckController.NAME_AND_METHOD_CALLS
                                || checkController == CheckController.NONFINAL_REFS_AND_METHOD_CALLS
                                || checkController
                                        == CheckController.NONFINAL_REFS_AND_METHOD_CALLS_METHOD) {
                            if (report
                                    && (checkController
                                                    == CheckController
                                                            .NONFINAL_REFS_AND_METHOD_CALLS
                                            || checkController
                                                    == CheckController.NAME_AND_METHOD_CALLS)) {
                                ((UpperBoundAnalysis) analysis)
                                        .getChecker()
                                        .report(
                                                Result.failure(
                                                        NO_REASSIGN_FIELD_METHOD, anno.toString()),
                                                n.getTree());
                            }
                            if (report
                                    && checkController
                                            == CheckController
                                                    .NONFINAL_REFS_AND_METHOD_CALLS_METHOD) {
                                ((UpperBoundAnalysis) analysis)
                                        .getChecker()
                                        .report(
                                                Result.failure(
                                                        SIDE_EFFECTING_METHOD, anno.toString()),
                                                n.getTree());
                            }
                            return true;
                        }
                        for (FlowExpressions.Receiver param :
                                ((FlowExpressions.MethodCall) r).getParameters()) {
                            canonicalStrings.add(param.toString());
                        }
                    }
                } catch (Exception e) { //FlowExpressionParseUtil.FlowExpressionParseException e) {
                }
            }
            if ((checkController == CheckController.NAME_AND_METHOD_CALLS
                            || checkController == CheckController.NAME_ONLY)
                    && canonicalStrings.contains(canonicalTargetName)) {
                if (report) {
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
                return true;
            }
        }
        return false;
    }

    void checkForRemainingAnnotations(
            List<AnnotatedTypeMirror> enclosedTypes,
            String canonicalTargetName,
            Node n,
            CheckController checkController) {
        for (AnnotatedTypeMirror atm : enclosedTypes) {
            for (AnnotationMirror anno : atm.getAnnotations()) {
                checkAnno(anno, canonicalTargetName, n, checkController, true);
            }
        }
    }

    void buildClearListForString(
            Map<? extends FlowExpressions.Receiver, CFValue> map,
            List<FlowExpressions.Receiver> toClear,
            String canonicalTargetName,
            Node n,
            CheckController checkController) {
        for (FlowExpressions.Receiver r : map.keySet()) {
            Set<AnnotationMirror> annos = map.get(r).getAnnotations();
            for (AnnotationMirror anno : annos) {
                if (checkAnno(anno, canonicalTargetName, n, checkController, false)) {
                    toClear.add(r);
                }
            }
        }
    }

    void clearFromStore(String canonicalTargetName, Node n, CheckController checkController) {
        List<FlowExpressions.Receiver> toClear = new ArrayList<>();
        buildClearListForString(
                localVariableValues, toClear, canonicalTargetName, n, checkController);
        buildClearListForString(methodValues, toClear, canonicalTargetName, n, checkController);
        buildClearListForString(classValues, toClear, canonicalTargetName, n, checkController);
        buildClearListForString(fieldValues, toClear, canonicalTargetName, n, checkController);
        buildClearListForString(arrayValues, toClear, canonicalTargetName, n, checkController);
        for (FlowExpressions.Receiver r : toClear) {
            this.clearValue(r);
        }
    }
}
