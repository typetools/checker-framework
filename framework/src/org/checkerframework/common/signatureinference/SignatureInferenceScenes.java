package org.checkerframework.common.signatureinference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;

import annotations.Annotation;
import annotations.el.AClass;
import annotations.el.AField;
import annotations.el.AMethod;
import annotations.el.AScene;
import annotations.el.AnnotationDef;
import annotations.el.DefException;
import annotations.field.AnnotationFieldType;
import annotations.field.ArrayAFT;
import annotations.field.BasicAFT;
import annotations.field.ScalarAFT;
import annotations.io.IndexFileParser;
import annotations.io.IndexFileWriter;
import annotations.util.JVMNames;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute.Array;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;

/**
 * This class writes inferred types for fields, method return types and method
 * parameters to a .jaif file.  Calling an update* method
 * ({@link SignatureInferenceScenes#updateFieldTypeInScene updateFieldTypeInScene},
 * {@link SignatureInferenceScenes#updateMethodParameterTypeInScene updateMethodParameterTypeInScene} or
 * {@link SignatureInferenceScenes#updateMethodReturnTypeInScene updateMethodReturnTypeInScene}) 
 * reads the currently-stored type, if any, and replaces it by the LUB of
 * it and the update method's argument.
 *
 * This class does not perform inference for elements that have explicit
 * annotations - an explicitly annotated field type, method return type or
 * method parameter type will not have its respective inferred type written
 * into a .jaif file.
 *
 * @author pbsf
 */
public class SignatureInferenceScenes {

    /**
     * Path to where .jaif files will be written to and read from.
     * This path is relative to where the CF's javac command is executed.
     */
    public final static String JAIF_FILES_PATH = "build/jaif-files/";

    /** Maps file paths (Strings) to Scenes. */
    private static Map<String, AScene> scenes = new HashMap<String, AScene>();

    /** Set containing all Scenes that were modified in the current ClassTree.
     * Modifying a Scene means adding (or changing) a type annotation for a
     * field, method return type or method parameter type in the Scene.
     * (Scenes are modified by the method
     * {@link SignatureInferenceScenes#updateAnnotationSetInScene updateAnnotationSetInScene)
     */
    private static Set<String> modifiedScenes = new HashSet<String>();

    /**
     * Returns the Scene related to a .jaif file path passed as input.
     */
    private static AScene getScene(String jaifPath) {
        if (!scenes.containsKey(jaifPath)) {
            File jaifFile = new File(jaifPath);
            AScene scene = new AScene();
            if (jaifFile.exists()) {
                try {
                    IndexFileParser.parseFile(jaifPath, scene);
                } catch (IOException e) {
                    ErrorReporter.errorAbort("Could not open file in: " + jaifPath + "."
                            + " Exception message: " + e.getMessage());
                }
            }
            scenes.put(jaifPath, scene);
        }
        return scenes.get(jaifPath);
    }

    /**
     * Clears the set of modified scenes.
     * (Scenes are modified by the method
     * {@link SignatureInferenceScenes#updateAnnotationSetInScene updateAnnotationSetInScene)
     */
    public static void clearModifiedScenes() {
        modifiedScenes.clear();
    }

    /**
     * Adds the identifier of a Scene in the set of modified scenes.
     * (Scenes are modified by the method
     * {@link SignatureInferenceScenes#updateAnnotationSetInScene updateAnnotationSetInScene)
     */
    private static void addModifiedScene(String scene) {
        modifiedScenes.add(scene);
    }

    /**
     * Write all modified scenes into .jaif files.
     * (Scenes are modified by the method
     * {@link SignatureInferenceScenes#updateAnnotationSetInScene updateAnnotationSetInScene)
     */
    public static void writeScenesToJaif() {
        for (String jaifPath : modifiedScenes) {
            try {
                File jaifFile = new File(jaifPath);
                if (!jaifFile.exists()) {
                    jaifFile.getParentFile().mkdirs();
                }
                AScene scene = scenes.get(jaifPath);
                IndexFileWriter.write(scene, new FileWriter(jaifPath));
            } catch (IOException e) {
                ErrorReporter.errorAbort("Could not open file in: " + jaifPath
                        + ". Exception message: " + e.getMessage());
            } catch (DefException e) {
                ErrorReporter.errorAbort(e.getMessage());
            } catch (Exception e) {
                System.out.println();
            }
        }
    }

    /**
     * Updates the parameters' types of the method methodElt in the Scene of the
     * class with symbol classSymbol.
     *
     * For each method parameter in methodElt:
     *   If the Scene does not contain an annotated type for that parameter,
     *   then the type of the respective value passed as argument in the
     *   method call methodInvNode will be added to the parameter in the Scene.
     *   If the Scene previously contained an annotated type for that parameter,
     *   then its new type will be the LUB between the previous type and the
     *   type of the respective value passed as argument in the method call.
     *
     * @param methodInvNode the node representing a method invocation.
     * @param classSymbol the symbol of the class that contains the method being
     * invoked.
     * @param methodElt the element of the method being invoked.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the method parameters' types.
     */
    public static void updateMethodParameterTypeInScene(
            MethodInvocationNode methodInvNode, ClassSymbol classSymbol,
            ExecutableElement methodElt, AnnotatedTypeFactory atf) {
        if (classSymbol == null) return;
        String jaifPath = JAIF_FILES_PATH + classSymbol.flatname.toString() +
                ".jaif";
        AScene scene = getScene(jaifPath);

        AClass clazz = getJaifClass(classSymbol, scene);
        if (clazz == null) return; // Anonymous class => Ignore, for now.
        String methodName = JVMNames.getJVMMethodName(methodElt);
        AMethod method = getJaifMethod(clazz, methodName, scene);
        for (int i = 0; i < methodInvNode.getArguments().size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            if (atf.getAnnotatedType(ve).getExplicitAnnotations().size() > 0) {
                // Ignore parameters that have a declared annotated type.
                continue;
            }

            Node arg = methodInvNode.getArgument(i);
            AnnotatedTypeMirror argType = atf.getAnnotatedType(arg.getTree());
            AField param = method.parameters.vivify(i);
            updateAnnotationSetInScene(
                    param.type.tlAnnotationsHere, atf, jaifPath, argType);
        }
    }

    /**
     * Updates the type of the field lhs in the Scene of the class with
     * symbol classSymbol.
     *
     * If the Scene contains no entry for the field lhs,
     * the entry will be created and its type will be the type of rhs. If the
     * Scene previously contained an entry/type for lhs, its new type will be
     * the LUB between the previous type and the type of rhs.
     *
     * @param lhs the field.
     * @param rhs the expression being assigned to the field.
     * @param classSymbol the symbol of the class that contains the field.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the field's type.
     */
    public static void updateFieldTypeInScene(FieldAccessNode lhs, Node rhs,
            ClassSymbol classSymbol, AnnotatedTypeFactory atf) {
        if (classSymbol == null) return;
        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(lhs.getTree());
        if (lhsATM.getExplicitAnnotations().size() > 0) {
            // We do not infer types if there are explicit annotations.
            // See https://github.com/typetools/annotation-tools/issues/105
            return;
        }
        String jaifPath = JAIF_FILES_PATH + classSymbol.flatname.toString() +
                ".jaif";
        AScene scene = getScene(jaifPath);
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(rhs.getTree());

        AClass clazz = getJaifClass(classSymbol, scene);
        if (clazz == null) return; // Anonymous class => Ignore, for now.
        AField field = getJaifField(clazz, lhs);
        updateAnnotationSetInScene(
                field.tlAnnotationsHere, atf, jaifPath, rhsATM);
    }

    /**
     * Updates the return type of the method methodTree in the
     * Scene of the class with symbol classSymbol.
     *
     * If the Scene does not contain an annotated return type for the method
     * methodTree, then the type of the value passed to the return expression
     * will be added to the return type of that method in the Scene.
     * If the Scene previously contained an annotated return type for the
     * method methodTree, its new type will be the LUB between the previous
     * type and the type of the value passed to the return expression.
     *
     * @param retNode the node that contains the expression returned.
     * @param classSymbol the symbol of the class that contains the method.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the method's return type.
     */
    public static void updateMethodReturnTypeInScene(ReturnNode retNode,
            ClassSymbol classSymbol, MethodTree methodTree,
            AnnotatedTypeFactory atf) {
        if (classSymbol == null) return;
        AnnotatedTypeMirror methodReturnType = atf.getAnnotatedType(methodTree).
                getReturnType();
        if (methodReturnType.getExplicitAnnotations().size() > 0) {
            // We do not infer types if there are explicit annotations.
            // See https://github.com/typetools/annotation-tools/issues/105
            return;
        }
        String jaifPath = JAIF_FILES_PATH + classSymbol.flatname.toString() +
                ".jaif";
        AScene scene = getScene(jaifPath);
        AnnotatedTypeMirror returnExprATM = atf.getAnnotatedType(retNode.
                getTree().getExpression());

        AClass clazz = getJaifClass(classSymbol, scene);
        if (clazz == null) return; // Anonymous class => Ignore, for now.
        String methodName = JVMNames.getJVMMethodName(methodTree);
        AMethod method = getJaifMethod(clazz, methodName, scene);
        if (method.returnType != null) {
            updateAnnotationSetInScene(
                    method.returnType.tlAnnotationsHere, atf,
                    jaifPath, returnExprATM);
        }
    }

    /**
     * Updates the set of annotations in a location of a Scene. If the set of
     * annotations (curAnnos) is empty, then the updated set will be the set of
     * annotations in newATM.
     * If curAnnos is not empty, the updated set will be the LUB between
     * curAnnos and newATM.
     *
     * @param curAnnos set of annotations located somewhere in a Scene.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update curAnnos.
     * @param jaifPath identifies a Scene.
     * @param newATM the type containing a set of annotations to be
     * added (or "lubbed") to the Scene.
     */
    private static void updateAnnotationSetInScene(Set<Annotation> curAnnos,
            AnnotatedTypeFactory atf, String jaifPath,
            AnnotatedTypeMirror newATM) {
        if (curAnnos != null && curAnnos.size() > 0) {
            AnnotatedTypeMirror curAnnosATM = setOfAnnotationsToATM(
                    curAnnos, atf, false, newATM.getUnderlyingType());
            if (curAnnosATM != null) {
                newATM = AnnotatedTypes.leastUpperBound(
                        atf.getProcessingEnv(), atf, newATM, curAnnosATM);
            }
        }

        Set<Annotation> setOfAnnos = atmToSetOfAnnotations(newATM, atf);
        curAnnos.clear();
        curAnnos.addAll(setOfAnnos);
        addModifiedScene(jaifPath);
    }

    /**
     * Returns the AField in an AScene, given an AClass and a FieldAccessNode.
     */
    private static AField getJaifField(AClass clazz, FieldAccessNode lhs) {
        String fieldName = lhs.getFieldName();
        AField field = clazz.fields.get(fieldName);
        if (field == null) {
            field = clazz.fields.vivify(fieldName);
        }
        return field;
    }

    /**
     * Returns the AMethod in an AScene, given an AClass and a MethodTree.
     */
    private static AMethod getJaifMethod(AClass clazz, String methodName,
            AScene scene) {
        AMethod method = clazz.methods.get(methodName);
        if (method == null) {
            method = clazz.methods.vivify(methodName);
        }
        return method;
    }

    /**
     * Returns the AClass in an AScene, given a ClassSymbol.
     */
    private static AClass getJaifClass(ClassSymbol classSymbol, AScene scene) {
        String className = classSymbol.getQualifiedName().toString();
        AClass clazz = scene.classes.get(className);
        if (clazz == null) {
            clazz = scene.classes.vivify(className);
        }
        if (className.equals("")) {
            // TODO: Handle anonymous classes soundly, if possible
            // (I can't think of a way to do that).
            // Their name is equal to an empty String. For now we are ignoring them.
            return null;
        }
        return clazz;
    }


    /**
     * Returns true if am should not be inserted in source code,
     * but is rather an implementation detail.
     * I.E. {@link org.checkerframework.common.value.qual.BottomVal}.
     * Returns false otherwise.
     */
    private static boolean ignoreAnnotation(AnnotationMirror am) {
        Target target = am.getAnnotationType().asElement().
                getAnnotation(Target.class);
        return target.value().length == 0;
    }

    // The four conversion methods below could be somewhere else. Maybe in AFU?

    /**
     * Converts a set of {@link annotations.Annotation} into an
     * {@link org.checkerframework.framework.type.AnnotatedTypeMirror} that
     * contains all annotations in the original set.
     */
    private static AnnotatedTypeMirror setOfAnnotationsToATM(
            Set<Annotation> annotations, AnnotatedTypeFactory atf,
            boolean isDeclaration, TypeMirror tm) {
        if (annotations == null) return null;
        AnnotatedTypeMirror atm = AnnotatedTypeMirror.createType(tm, atf,
                isDeclaration);
        for (Annotation anno : annotations) {
            AnnotationMirror am = annotationToAnnotationMirror(
                    anno, atf.getProcessingEnv());
            if (!ignoreAnnotation(am)) {
                atm.addAnnotation(am);
            }
        }
        return atm;
    }

    /**
     * Converts an {@link org.checkerframework.framework.type.AnnotatedTypeMirror}
     * into a set of {@link annotations.Annotation}.
     */
    private static Set<Annotation> atmToSetOfAnnotations(AnnotatedTypeMirror atm,
            AnnotatedTypeFactory atf) {
        Set<Annotation> output = new HashSet<Annotation>();
        for (AnnotationMirror am : atm.getAnnotations()) {
            if (!ignoreAnnotation(am)) {
                Annotation anno = annotationMirrorToAnnotation(am);
                if (anno != null) {
                    output.add(anno);
                }
            }
        }
        return output;
    }

    /**
     * Converts an {@link javax.lang.model.element.AnnotationMirror}
     * into an {@link annotations.Annotation}.
     */
    private static Annotation annotationMirrorToAnnotation(AnnotationMirror am) {
        AnnotationDef def = new AnnotationDef(AnnotationUtils.annotationName(am));
        Map<String, AnnotationFieldType> fieldTypes = new HashMap<String, AnnotationFieldType>();
        // Handling cases where there are fields in annotations.
        for (ExecutableElement ee : am.getElementValues().keySet()) {
            AnnotationFieldType aft = getAnnotationFieldType(ee, am.
                    getElementValues().get(ee).getValue());
            if (aft == null) return null;
            // Here we just add the type of the field into fieldTypes.
            fieldTypes.put(ee.getSimpleName().toString(), aft);
        }
        def.setFieldTypes(fieldTypes);

        // Now, we handle the values of those types below
        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                am.getElementValues();
        Map<String, Object> newValues = new HashMap<String, Object>();
        for (ExecutableElement ee : values.keySet()) {
            Object value = values.get(ee).getValue();
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> valueList = (List<Object>)value;
                List<Object> newList = new ArrayList<Object>();
                // If we have a List here, then it is a List of AnnotatedValue.
                // Converting each AnnotatedValue to its respective Java type:
                for (Object o : valueList) {
                    newList.add(((AnnotationValue)o).getValue());
                }
                value = newList;
            }
            newValues.put(ee.getSimpleName().toString(), value);
        }
        Annotation out = new Annotation(def, newValues);
        return out;
    }

    /**
     * Converts an {@link annotations.Annotation} into an
     * {@link javax.lang.model.element.AnnotationMirror}.
     */
    private static AnnotationMirror annotationToAnnotationMirror(
            Annotation anno, ProcessingEnvironment processingEnv) {
        final AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                anno.def().name);
        for (String fieldKey : anno.fieldValues.keySet()) {
            addFieldToAnnotationBuilder(fieldKey,
                    anno.fieldValues.get(fieldKey), builder);
        }
        return builder.build();
    }

    /**
     * Returns an AnnotationFieldType given an ExecutableElement or value.
     */
    private static AnnotationFieldType getAnnotationFieldType(ExecutableElement
            ee, Object value) {
        if (value instanceof List<?>) {
            // Handling cases of empty arrays was a bit troublesome here.
            AnnotationValue defaultValue = ee.getDefaultValue();
            if (defaultValue == null || ((ArrayType)((Array)defaultValue).type) == null) {
                List<?> listV = (List<?>)value;
                if (!listV.isEmpty()) {
                    return new ArrayAFT((ScalarAFT) getAnnotationFieldType(ee,
                            ((AnnotationValue)((List<?>)value).get(0)).getValue()));
                }
                return null;
            }
            Type elemType = ((ArrayType)((Array)defaultValue).type).elemtype;
            try {
                return new ArrayAFT((ScalarAFT) BasicAFT.
                        forType(Class.forName(elemType.toString())));
            } catch (ClassNotFoundException e) {
                ErrorReporter.errorAbort(e.getMessage());
            }
        } else if (value instanceof Boolean)
            return BasicAFT.forType(boolean.class);
        else if (value instanceof Character)
            return BasicAFT.forType(char.class);
        else if (value instanceof Double)
            return BasicAFT.forType(double.class);
        else if (value instanceof Float)
            return BasicAFT.forType(float.class);
        else if (value instanceof Integer)
            return BasicAFT.forType(int.class);
        else if (value instanceof Long)
            return BasicAFT.forType(long.class);
        else if (value instanceof Short)
            return BasicAFT.forType(short.class);
        else if (value instanceof String)
            return BasicAFT.forType(String.class);
        return null;
    }


    /**
     *  Adds a field to an AnnotationBuilder.
     * @param fieldKey is the name of the field
     * @param obj is the value of the field
     * @param builder is the AnnotationBuilder
     */
    @SuppressWarnings("unchecked") // This is actually checked in the first
    //instanceOf call.
    private static void addFieldToAnnotationBuilder(String fieldKey, Object obj,
            AnnotationBuilder builder) {
        if (obj instanceof List<?>) {
            builder.setValue(fieldKey, (List<Object>) obj);
        } else if (obj instanceof String) {
            builder.setValue(fieldKey, (String)obj);
        } else if (obj instanceof Integer) {
            builder.setValue(fieldKey, (Integer)obj);
        } else if (obj instanceof Float) {
            builder.setValue(fieldKey, (Float)obj);
        } else if (obj instanceof Long) {
            builder.setValue(fieldKey, (Long)obj);
        } else if (obj instanceof Boolean) {
            builder.setValue(fieldKey, (Boolean)obj);
        } else if (obj instanceof Character) {
            builder.setValue(fieldKey, (Character)obj);
        } else if (obj instanceof Class<?>) {
            builder.setValue(fieldKey, (Class<?>)obj);
        } else if (obj instanceof Double) {
            builder.setValue(fieldKey, (Double)obj);
        } else if (obj instanceof Float) {
            builder.setValue(fieldKey, (Float)obj);
        } else if (obj instanceof Enum<?>) {
            builder.setValue(fieldKey, (Enum<?>)obj);
        } else if (obj instanceof Enum<?>[]) {
            builder.setValue(fieldKey, (Enum<?>[])obj);
        } else if (obj instanceof AnnotationMirror) {
            builder.setValue(fieldKey, (AnnotationMirror)obj);
        } else if (obj instanceof Object[]) {
            builder.setValue(fieldKey, (Object[])obj);
        } else if (obj instanceof TypeMirror) {
            builder.setValue(fieldKey, (TypeMirror)obj);
        } else if (obj instanceof Short) {
            builder.setValue(fieldKey, (Short)obj);
        } else if (obj instanceof VariableElement) {
            builder.setValue(fieldKey, (VariableElement)obj);
        } else if (obj instanceof VariableElement[]) {
            builder.setValue(fieldKey, (VariableElement[])obj);
        } else {
            ErrorReporter.errorAbort("Unrecognized type: " + obj.getClass());
        }
    }

    /**
     * Returns the ClassSymbol of the class encapsulating
     * the node n passed as parameter.
     * TODO: This method could be moved somewhere else.
     */
    public static ClassSymbol getEnclosingClassSymbol(ClassTree classTree,
            Node n, Node receiverNode) {
        if (receiverNode instanceof ImplicitThisLiteralNode
                && classTree != null) {
            return (ClassSymbol) InternalUtils.symbol(classTree);
        }
        TypeMirror type = receiverNode.getType();
        if (type instanceof ClassType) {
            TypeSymbol tsym = ((ClassType) type).asElement();
            return tsym.enclClass();
        }
        Tree tree = receiverNode.getTree();
        Element symbol = InternalUtils.symbol(tree);
        if (symbol instanceof ClassSymbol) {
            return (ClassSymbol) symbol;
        } else if (symbol instanceof VarSymbol) {
            return ((VarSymbol)symbol).enclClass();
        }
        return null;
    }

}
