package org.checkerframework.framework.util.element;

import static com.sun.tools.javac.code.TargetType.CAST;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_REFERENCE;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.EXCEPTION_PARAMETER;
import static com.sun.tools.javac.code.TargetType.INSTANCEOF;
import static com.sun.tools.javac.code.TargetType.LOCAL_VARIABLE;
import static com.sun.tools.javac.code.TargetType.METHOD_FORMAL_PARAMETER;
import static com.sun.tools.javac.code.TargetType.METHOD_INVOCATION_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.METHOD_RECEIVER;
import static com.sun.tools.javac.code.TargetType.METHOD_REFERENCE;
import static com.sun.tools.javac.code.TargetType.METHOD_REFERENCE_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.METHOD_RETURN;
import static com.sun.tools.javac.code.TargetType.METHOD_TYPE_PARAMETER;
import static com.sun.tools.javac.code.TargetType.METHOD_TYPE_PARAMETER_BOUND;
import static com.sun.tools.javac.code.TargetType.NEW;
import static com.sun.tools.javac.code.TargetType.RESOURCE_VARIABLE;
import static com.sun.tools.javac.code.TargetType.THROWS;
import static org.checkerframework.framework.util.element.ElementAnnotationUtil.addAnnotationsFromElement;
import static org.checkerframework.framework.util.element.ElementAnnotationUtil.annotateViaTypeAnnoPosition;
import static org.checkerframework.framework.util.element.ElementAnnotationUtil.applyAllElementAnnotations;
import static org.checkerframework.framework.util.element.ElementAnnotationUtil.partitionByTargetType;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;

/**
 * Adds annotations from element to the return type, formal parameter types, type parameters, and
 * throws clauses of the AnnotatedExecutableType type.
 */
public class MethodApplier extends TargetedElementAnnotationApplier {

    public static void apply(
            AnnotatedTypeMirror type, Element element, AnnotatedTypeFactory typeFactory) {
        new MethodApplier(type, element, typeFactory).extractAndApply();
    }

    public static boolean accepts(final AnnotatedTypeMirror typeMirror, final Element element) {
        return element instanceof Symbol.MethodSymbol
                && typeMirror instanceof AnnotatedExecutableType;
    }

    private final AnnotatedTypeFactory typeFactory;

    /** Method being annotated, this symbol contains all relevant annotations */
    private final Symbol.MethodSymbol methodSymbol;

    private final AnnotatedExecutableType methodType;

    MethodApplier(AnnotatedTypeMirror type, Element element, AnnotatedTypeFactory typeFactory) {
        super(type, element);
        this.typeFactory = typeFactory;
        this.methodSymbol = (Symbol.MethodSymbol) element;
        this.methodType = (AnnotatedExecutableType) type;
    }

    /**
     * @return receiver, returns, and throws. See extract and apply as we also annotate type params.
     */
    @Override
    protected TargetType[] annotatedTargets() {
        return new TargetType[] {METHOD_RECEIVER, METHOD_RETURN, THROWS};
    }

    /** @return all possible annotation positions for a method except those in annotatedTargets */
    @Override
    protected TargetType[] validTargets() {
        return new TargetType[] {
            LOCAL_VARIABLE,
            RESOURCE_VARIABLE,
            EXCEPTION_PARAMETER,
            NEW,
            CAST,
            INSTANCEOF,
            METHOD_INVOCATION_TYPE_ARGUMENT,
            CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT,
            METHOD_REFERENCE,
            CONSTRUCTOR_REFERENCE,
            METHOD_REFERENCE_TYPE_ARGUMENT,
            CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT,
            METHOD_TYPE_PARAMETER,
            METHOD_TYPE_PARAMETER_BOUND,
            METHOD_FORMAL_PARAMETER
        };
    }

    /** @return the annotations on the method symbol (element) */
    @Override
    protected Iterable<Attribute.TypeCompound> getRawTypeAttributes() {
        return methodSymbol.getRawTypeAttributes();
    }

    @Override
    protected boolean isAccepted() {
        return MethodApplier.accepts(type, element);
    }

    /**
     * Sets the method's element, annotates its return type, parameters, type parameters, and throws
     * annotations.
     */
    @Override
    public void extractAndApply() {
        methodType.setElement(methodSymbol); // Preserves previous behavior

        // Add declaration annotations to the return type if
        if (methodType.getReturnType() instanceof AnnotatedTypeVariable) {
            applyTypeVarUseOnReturnType();
        }
        addAnnotationsFromElement(methodType.getReturnType(), methodSymbol.getAnnotationMirrors());

        final List<AnnotatedTypeMirror> params = methodType.getParameterTypes();
        for (int i = 0; i < params.size(); ++i) {
            // Add declaration annotations to the parameter type
            addAnnotationsFromElement(
                    params.get(i), methodSymbol.getParameters().get(i).getAnnotationMirrors());
        }

        // ensures that we check that there are only valid target types on this class, there are no
        // "invalid" locations
        super.extractAndApply();

        applyAllElementAnnotations(
                methodType.getParameterTypes(), methodSymbol.getParameters(), typeFactory);
        applyAllElementAnnotations(
                methodType.getTypeVariables(), methodSymbol.getTypeParameters(), typeFactory);
    }

    // NOTE that these are the only locations not handled elsewhere, otherwise we call apply
    @Override
    protected void handleTargeted(final List<Attribute.TypeCompound> targeted) {
        final List<TypeCompound> unmatched = new ArrayList<>();
        final Map<TargetType, List<TypeCompound>> targetTypeToAnno =
                partitionByTargetType(targeted, unmatched, METHOD_RECEIVER, METHOD_RETURN, THROWS);

        annotateViaTypeAnnoPosition(
                methodType.getReceiverType(), targetTypeToAnno.get(METHOD_RECEIVER));
        annotateViaTypeAnnoPosition(
                methodType.getReturnType(), targetTypeToAnno.get(METHOD_RETURN));
        applyThrowsAnnotations(targetTypeToAnno.get(THROWS));

        if (unmatched.size() > 0) {
            ErrorReporter.errorAbort(
                    "Unexpected annotations ( "
                            + PluginUtil.join(",", unmatched)
                            + " ) for"
                            + "type ( "
                            + type
                            + " ) and element ( "
                            + element
                            + " ) ");
        }
    }

    /** For each thrown type, collect all the annotations for that type and apply them */
    private void applyThrowsAnnotations(final List<Attribute.TypeCompound> annos) {
        final List<AnnotatedTypeMirror> thrown = methodType.getThrownTypes();
        if (thrown.isEmpty()) {
            return;
        }

        Map<AnnotatedTypeMirror, List<TypeCompound>> typeToAnnos = new LinkedHashMap<>();
        for (final AnnotatedTypeMirror thrownType : thrown) {
            typeToAnnos.put(thrownType, new ArrayList<TypeCompound>());
        }

        for (TypeCompound anno : annos) {
            final TypeAnnotationPosition annoPos = anno.position;
            if (annoPos.type_index >= 0 && annoPos.type_index < thrown.size()) {
                final AnnotatedTypeMirror thrownType = thrown.get(annoPos.type_index);
                typeToAnnos.get(thrownType).add(anno);

            } else {
                ErrorReporter.errorAbort(
                        "MethodApplier.applyThrowsAnnotation: "
                                + "invalid throws index "
                                + annoPos.type_index
                                + " for annotation: "
                                + anno
                                + " for element: "
                                + ElementUtils.getVerboseName(element));
            }
        }

        for (final Entry<AnnotatedTypeMirror, List<TypeCompound>> typeToAnno :
                typeToAnnos.entrySet()) {
            annotateViaTypeAnnoPosition(typeToAnno.getKey(), typeToAnno.getValue());
        }
    }

    /**
     * If the return type is a use of a type variable first apply the bound annotations from the
     * type variables declaration
     */
    private void applyTypeVarUseOnReturnType() {
        new TypeVarUseApplier(methodType.getReturnType(), methodSymbol, typeFactory)
                .extractAndApply();
    }
}
