package org.checkerframework.checker.nullness;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.javacutil.AnnotationUtils;

import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

public class KeyForVisitor extends BaseTypeVisitor<KeyForAnnotatedTypeFactory> {
    public KeyForVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /*
     * Given a string array 'values', returns an AnnotationMirror corresponding to @KeyFor(values)
     */
    private AnnotationMirror getKeyForAnnotationMirrorWithValue(ArrayList<String> values) {
        // Create an AnnotationBuilder with the ArrayList

        AnnotationBuilder builder =
                new AnnotationBuilder(atypeFactory.getProcessingEnv(), KeyFor.class);
        builder.setValue("value", values);

        // Return the resulting AnnotationMirror

        return builder.build();
    }

    /*
     * This method uses FlowExpressionsParseUtil to attempt to recognize the variable names indicated in the values in KeyFor(values).
     * 
     * This method modifies atm such that the values are replaced with the string representation of the Flow Expression Receiver
     * returned by FlowExpressionsParseUtil.parse. This ensures that when comparing KeyFor values later when doing subtype checking
     * that equivalent expressions (such as "field" and "this.field" when there is no local variable "field") are represented by the same
     * string so that string comparison will succeed.
     * 
     * This is necessary because when KeyForTransfer generates KeyFor annotations, it uses FlowExpressions to generate the values in KeyFor(values).
     * canonicalizeKeyForValues ensures that user-provided KeyFor annotations will contain values that match the format of those in the generated
     * KeyFor annotations.
     */
    private void canonicalizeKeyForValues(AnnotatedTypeMirror atm, FlowExpressionContext flowExprContext, TreePath path, Tree t) {
        Receiver varTypeReceiver = null;

        CFAbstractStore<?, ?> store = null;
        boolean unknownReceiver = false;

        if (flowExprContext.receiver.containsUnknown()) {
            // If the receiver is unknown, we will try local variables

            store = atypeFactory.getStoreBefore(t);
            unknownReceiver = true; // We could use store != null for this check, but this is clearer.
        }

        AnnotationMirror anno = atm.getAnnotation(KeyFor.class);

        if (anno != null) {
            boolean valuesChanged = false; // Indicates that at least one value was changed in the list.

            Map<? extends ExecutableElement, ? extends AnnotationValue> values = anno.getElementValues();
            ArrayList<String> newValues = new ArrayList<String>();

            for(AnnotationValue value : values.values()) {
                @SuppressWarnings("unchecked")
                List<? extends Object> v = (List<? extends Object>) value.getValue();
                for(final Object c : v) {
                    String s = c.toString();
                    s = s.substring(1, s.length() - 1); // Remove start and end quotes

                    boolean localVariableFound = false;

                    if (unknownReceiver) {
                        // If the receiver is unknown, try a local variable
                        CFAbstractValue<?> val = store.getValueOfLocalVariableByName(s);

                        if (val != null) {
                            newValues.add(s);
                            // Don't set valuesChanged to true since local variable names are already canonicalized
                            localVariableFound = true;
                        }
                    }

                    if (localVariableFound == false) {
                        try {
                            varTypeReceiver = FlowExpressionParseUtil.parse(s, flowExprContext, path);
                        } catch (FlowExpressionParseException e) {
                        }

                        if (unknownReceiver // The receiver type was unknown initially, and ...
                                && (varTypeReceiver == null
                                || varTypeReceiver.containsUnknown()) // ... the receiver type is still unknown after a call to parse
                                ) {
                            // parse did not find a static member field. Try a nonstatic field.

                            try {
                                varTypeReceiver = FlowExpressionParseUtil.parse("this." + s, // Try a field in the current object. Do not modify s itself since it is used in the newValue.equals(s) check below.
                                        flowExprContext, path);
                            } catch (FlowExpressionParseException e) {
                            }
                        }

                        if (varTypeReceiver != null) {
                            String newValue = varTypeReceiver.toString();
                            newValues.add(newValue);

                            if (!newValue.equals(s)) {
                                valuesChanged = true;
                            }
                        }
                        else {
                            newValues.add(s); // This will get ignored if valuesChanged is false after exiting the for loop
                        }
                    }
                }
            }

            if (valuesChanged) {
                atm.replaceAnnotation(getKeyForAnnotationMirrorWithValue(newValues));
            }
        }
    }

    @Override
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType, Tree valueTree, /*@CompilerMessageKey*/ String errorKey,
            boolean isLocalVariableAssignement) {

        // Build new varType and valueType with canonicalized expressions in the values

        com.sun.source.util.TreePath path = getCurrentPath();
        Tree t = path.getLeaf();

        Node node = atypeFactory.getNodeForTree(t);

        if (node != null) {
            Receiver r = FlowExpressions.internalReprOf(atypeFactory, node);

            FlowExpressionContext flowExprContext = new FlowExpressionContext(
                    r, null, checker);

            canonicalizeKeyForValues(varType, flowExprContext, path, t);
            canonicalizeKeyForValues(valueType, flowExprContext, path, t);

            // If they are local variable names, they are already canonicalized. So we only need to canonicalize
            // the names of static and instance fields.
        }

        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, isLocalVariableAssignement);
    }

    /**
     * The type validator to ensure correct usage of ownership modifiers.
     */
    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new KeyForTypeValidator(checker, this, atypeFactory);
    }

    private final static class KeyForTypeValidator extends BaseTypeValidator {

        public KeyForTypeValidator(BaseTypeChecker checker,
                BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
            super(checker, visitor, atypeFactory);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Tree p) {
            AnnotationMirror kf = type.getAnnotation(KeyFor.class);
            if (kf != null) {
                List<String> maps = AnnotationUtils.getElementValueArray(kf, "value", String.class, false);

                boolean inStatic = false;
                if (p.getKind() == Kind.VARIABLE) {
                    ModifiersTree mt = ((VariableTree) p).getModifiers();
                    if (mt.getFlags().contains(Modifier.STATIC)) {
                        inStatic = true;
                    }
                }

                for (String map : maps) {
                    if (map.equals("this")) {
                        // this is not valid in static context
                        if (inStatic) {
                            checker.report(
                                    Result.failure("keyfor.type.invalid",
                                            type.getAnnotations(),
                                            type.toString()), p);
                        }
                    } else if (map.matches("#(\\d+)")) {
                        // Accept parameter references
                        // TODO: look for total number of parameters and only
                        // allow the range 0 to n-1
                    } else {
                        // Only other option is local variable and field names?
                        // TODO: go through all possibilities.
                    }
                }
            }

            // TODO: Should BaseTypeValidator be parametric in the ATF?
            if (type.isAnnotatedInHierarchy(((KeyForAnnotatedTypeFactory)atypeFactory).KEYFOR)) {
                return super.visitDeclared(type, p);
            } else {
                // TODO: Something went wrong...
                return null;
            }
        }

        // TODO: primitive types? arrays?
        /*
        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType type, Tree p) {
          return super.visitPrimitive(type, p);
        }

        @Override
        public Void visitArray(AnnotatedArrayType type, Tree p) {
          return super.visitArray(type, p);
        }
        */
    }
}
