package org.checkerframework.common.signatureinference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
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
import annotations.el.ATypeElement;
import annotations.el.AnnotationDef;
import annotations.el.DefException;
import annotations.el.InnerTypeLocation;
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
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;

/**
 * SignatureInferenceScenes represents a set of annotations that are inferred
 * in a program. It also writes those annotations into .jaif files.
 * This class stores annotations for fields, method return types, and method
 * parameters.
 * <p>
 * The set of annotations inferred for a certain class is stored in an
 * {@link annotations.el.AScene}, which {@link #writeScenesToJaif} can write
 * into a .jaif file.
 * For example, a class field of a class whose fully-qualified name is
 * my.package.MyClass will have its inferred type stored in a Scene, and later
 * written into a file named my.package.MyClass.jaif.
 * <p>
 * This class populates the initial Scenes by reading existing .jaif files
 * on the {@link #JAIF_FILES_PATH} directory. Having more information on those
 * initial .jaif files means that the precision achieved by the signature
 * inference analysis will be better. {@link #writeScenesToJaif} rewrites
 * the initial .jaif files, and may create new ones.
 * <p>
 * Calling an update* method
 * ({@link updateInferredFieldType},
 * {@link updateInferredMethodParametersTypes}, or
 * {@link updateInferredMethodReturnType}) 
 * replaces the currently-stored type for an element in a Scene, if any,
 * by the LUB of it and the update method's argument.
 * <p>
 * This class does not store annotations for an element if the element has
 * explicit annotations -- when calling an update* method passing as argument an
 * explicitly annotated field, method return, or method parameter, its inferred
 * type is not stored in a Scene.
 * <p>
 *  @author pbsf
 */
//  TODO: We could add an option to update the type of explicitly annotated
//  elements, but this currently is not recommended since the
//  insert-annotations-to-source tool, which adds annotations from .jaif files
//  into source code, adds annotations on top of existing
//  annotations. See https://github.com/typetools/annotation-tools/issues/105
//  TODO: Ensure that annotations are added deterministically to .jaif files.
public class SignatureInferenceScenes {

    /**
     * Path to where .jaif files will be written to and read from.
     * This path is relative to where the CF's javac command is executed.
     */
    public final static String JAIF_FILES_PATH = "build/jaif-files/";

    /** Maps .jaif file paths (Strings) to Scenes. */
    private static Map<String, AScene> scenes = new HashMap<>();

    /** Set containing Scenes that were modified since the last time all
     * Scenes were written into .jaif files. The String argument of this set
     * is a path to the .jaif file of the corresponding Scene in the set. It
     * is obtained by passing a class name as argument to the
     * {@link getJaifPath} method.
     * <p>
     * Modifying a Scene means adding (or changing) a type annotation for a
     * field, method return type, or method parameter type in the Scene.
     * (Scenes are modified by the method {@link updateAnnotationSetInScene}).
     */
    private static Set<String> modifiedScenes = new HashSet<>();

    /**
     * Returns the String representing the .jaif path of a class given its name.
     */
    private static String getJaifPath(String className) {
        String jaifPath = JAIF_FILES_PATH + className + ".jaif";
        return jaifPath;
    }

    /**
     * Returns the Scene stored in a .jaif file path passed as input.
     * If the file does not exist, an empty Scene is created.
     */
    private static AScene getScene(String jaifPath) {
        if (!scenes.containsKey(jaifPath)) {
            File jaifFile = new File(jaifPath);
            AScene scene = new AScene();
            if (jaifFile.exists()) {
                try {
                    IndexFileParser.parseFile(jaifPath, scene);
                } catch (IOException e) {
                    ErrorReporter.errorAbort("Problem while reading file in: " + jaifPath + "."
                            + " Exception message: " + e.getMessage());
                }
            }
            scenes.put(jaifPath, scene);
        }
        return scenes.get(jaifPath);
    }

    /**
     * Write all modified scenes into .jaif files.
     * (Scenes are modified by the method
     * {@link updateAnnotationSetInScene})
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
                ErrorReporter.errorAbort("Problem while reading file in: " + jaifPath
                        + ". Exception message: " + e.getMessage());
            } catch (DefException e) {
                ErrorReporter.errorAbort(e.getMessage());
            }
        }
        modifiedScenes.clear();
    }

    /**
     * Updates the parameters' types of the method methodElt in the Scene of the
     * class with symbol classSymbol.
     * <p>
     * For each method parameter in methodElt:
     *   <ul>
     *     <li>If the Scene does not contain an annotated type for that
     *     parameter, then the type of the respective value passed as argument
     *     in the method call methodInvNode will be added to the parameter in
     *     the Scene.</li>
     *     <li>If the Scene previously contained an annotated type for that
     *     parameter, then its new type will be the LUB between the previous
     *     type and the type of the respective value passed as argument in the
     *     method call.</li>
     *   </ul>
     * <p>
     * @param methodInvNode the node representing a method invocation.
     * @param classSymbol the symbol of the class that contains the method being
     * invoked.
     * @param methodElt the element of the method being invoked.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the method parameters' types.
     */
    public static void updateInferredMethodParametersTypes(
            MethodInvocationNode methodInvNode, ClassSymbol classSymbol,
            ExecutableElement methodElt, AnnotatedTypeFactory atf) {
        if (classSymbol == null) return; // Anonymous class => Ignore, for now.
        String className = classSymbol.flatname.toString();

        String jaifPath = getJaifPath(className);
        AClass clazz = getAClass(className, jaifPath);

        String methodName = JVMNames.getJVMMethodName(methodElt);
        AMethod method = clazz.methods.vivify(methodName);

        for (int i = 0; i < methodInvNode.getArguments().size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            AnnotatedTypeMirror paramATM = atf.getAnnotatedType(ve);

            Node arg = methodInvNode.getArgument(i);
            Tree treeNode = arg.getTree();
            if (treeNode == null) continue;
            AnnotatedTypeMirror argATM = atf.getAnnotatedType(treeNode);
            AField param = method.parameters.vivify(i);
            updateAnnotationSetInScene(
                    param.type, atf, jaifPath, argATM, paramATM);
        }
    }

    /**
     * Updates the type of the field lhs in the Scene of the class with
     * symbol classSymbol.
     * <p>
     * If the Scene contains no entry for the field lhs,
     * the entry will be created and its type will be the type of rhs. If the
     * Scene previously contained an entry/type for lhs, its new type will be
     * the LUB between the previous type and the type of rhs.
     * <p>
     * @param lhs the field whose type will be refined.
     * @param rhs the expression being assigned to the field.
     * @param classTree the ClassTree where the assignment is invoked 
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the field's type.
     */
    public static void updateInferredFieldType(
            Node lhs, Node rhs, ClassTree classTree, AnnotatedTypeFactory atf) {
        FieldAccessNode lhsFieldNode = (FieldAccessNode) lhs;
        ClassSymbol classSymbol = getEnclosingClassSymbol(classTree, lhsFieldNode);
        if (classSymbol == null) return; // Anonymous class => Ignore, for now.
        String className = classSymbol.flatname.toString();

        String jaifPath = getJaifPath(className);
        AClass clazz = getAClass(className, jaifPath);

        AField field = clazz.fields.vivify(lhsFieldNode.getFieldName());
        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(lhs.getTree());
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(rhs.getTree());
        updateAnnotationSetInScene(
                field.type, atf, jaifPath, rhsATM, lhsATM);
    }

    /**
     * Updates the return type of the method methodTree in the
     * Scene of the class with symbol classSymbol.
     * <p>
     * If the Scene does not contain an annotated return type for the method
     * methodTree, then the type of the value passed to the return expression
     * will be added to the return type of that method in the Scene.
     * If the Scene previously contained an annotated return type for the
     * method methodTree, its new type will be the LUB between the previous
     * type and the type of the value passed to the return expression.
     * <p>
     * @param retNode the node that contains the expression returned.
     * @param classSymbol the symbol of the class that contains the method.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the method's return type.
     */
    public static void updateInferredMethodReturnType(ReturnNode retNode,
            ClassSymbol classSymbol, MethodTree methodTree,
            AnnotatedTypeFactory atf) {
        if (classSymbol == null) return; // Anonymous class => Ignore, for now.
        String className = classSymbol.flatname.toString();

        String jaifPath = getJaifPath(className);
        AClass clazz = getAClass(className, jaifPath);

        AMethod method = clazz.methods.vivify(JVMNames.getJVMMethodName(methodTree));
        // Method return type
        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(methodTree).getReturnType();
        // Type of the expression returned
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(retNode.getTree().getExpression());
        updateAnnotationSetInScene(
                method.returnType, atf, jaifPath, rhsATM, lhsATM);
    }

    /**
     * Updates the set of annotations in a location of a Scene.
     *   <ul>
     *     <li>If the set of annotations (curAnnos) is empty, then the updated
     *     set will be the set of annotations in newATM.</li>
     *     <li>If curAnnos is not empty, the updated set will be the LUB
     *     between curAnnos and newATM.</li>
     *   </ul>
     * <p>
     * @param type ATypeElement of the Scene which will be modified.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update curAnnos.
     * @param jaifPath used to identify a Scene.
     * @param newATM the type containing a set of annotations to be
     * added (or "lubbed") to the Scene.
     */
    private static void updateAnnotationSetInScene(ATypeElement type,
            AnnotatedTypeFactory atf, String jaifPath,
            AnnotatedTypeMirror newATM, AnnotatedTypeMirror oldATM) {
        AnnotatedTypeMirror curAnnosATM = typeElementToATM(
                type, atf, false, newATM.getUnderlyingType());
        if (curAnnosATM.getAnnotations().size() == newATM.getAnnotations().size()
                && newATM.getAnnotations().size() > 0) {
            newATM = AnnotatedTypes.leastUpperBound(
                    atf.getProcessingEnv(), atf, newATM, curAnnosATM);
        }
        atmToTypeElement(newATM, oldATM, atf, type, 1);
        modifiedScenes.add(jaifPath);
    }

    /**
     * Returns the AClass in an AScene, given a className and a jaifPath.
     */
    private static AClass getAClass(String className, String jaifPath) {
        // Possibly reads .jaif file to obtain a Scene.
        AScene scene = getScene(jaifPath);
        return scene.classes.vivify(className);
    }

    /**
     * Returns true if am should not be inserted in source code,
     * but is rather an implementation detail.
     * For example, {@link org.checkerframework.common.value.qual.BottomVal}.
     * Returns false otherwise.
     * TODO: The implementation checks for the @Target meta-annotation, which
     * is unreliable. See https://github.com/typetools/checker-framework/issues/515.
     */
    private static boolean ignoreAnnotation(AnnotationMirror am) {
        Target target = am.getAnnotationType().asElement().
                getAnnotation(Target.class);
        // If the @Target meta-annotations is missing, it can be used anywhere.
        return target != null && target.value().length == 0;
    }

    // The four conversion methods below could be somewhere else. Maybe in AFU?

    /**
     * Converts a set of {@link annotations.Annotation} into an
     * {@link org.checkerframework.framework.type.AnnotatedTypeMirror} that
     * contains all annotations in the first set.
     */
    private static AnnotatedTypeMirror typeElementToATM(
            ATypeElement type, AnnotatedTypeFactory atf,
            boolean isDeclaration, TypeMirror tm) {
        AnnotatedTypeMirror atm = AnnotatedTypeMirror.createType(tm, atf,
                isDeclaration);
        for (Annotation anno : type.tlAnnotationsHere) {
            AnnotationMirror am = annotationToAnnotationMirror(
                    anno, atf.getProcessingEnv());
            atm.addAnnotation(am);
        }
        if (tm.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType aat = (AnnotatedArrayType) atm;
            for (ATypeElement innerType : type.innerTypes.values()) {
                TypeMirror at = aat.getUnderlyingType().getComponentType();
                aat.setComponentType(typeElementToATM(innerType, atf,
                        isDeclaration, at));
            }
        }
        return atm;
    }

    /**
     * Converts an {@link org.checkerframework.framework.type.AnnotatedTypeMirror}
     * into a set of {@link annotations.Annotation}. Annotations in the original
     * set that are an implementation detail are ignored and not added to the
     * resulting set.
     */
    private static void atmToTypeElement(AnnotatedTypeMirror newATM,
            AnnotatedTypeMirror oldATM,
            AnnotatedTypeFactory atf, ATypeElement typeToUpdate, int idx) {
        // Clears only the annotations that are supported by atf.
        // The others stay intact.
        Set<Class<? extends java.lang.annotation.Annotation>> supportedAnnos =
                atf.getSupportedTypeQualifiers();
        Set<Annotation> annosToRemove = new HashSet<>();
        for (Annotation anno: typeToUpdate.tlAnnotationsHere) {
            for (Class<? extends java.lang.annotation.Annotation> clazz : supportedAnnos) {
                // TODO: Remove comparison by name, and make this routine more efficient.
                if (clazz.getName().equals(anno.def.name)) {
                    annosToRemove.add(anno);
                }
            }
        }
        typeToUpdate.tlAnnotationsHere.removeAll(annosToRemove);

        if (oldATM.getExplicitAnnotations().size() == 0) {
            for (AnnotationMirror am : newATM.getAnnotations()) {
                if (!ignoreAnnotation(am)) {
                    Annotation anno = annotationMirrorToAnnotation(am);
                    if (anno != null) {
                        typeToUpdate.tlAnnotationsHere.add(anno);
                    }
                }
            }
        }

        // Recursively update compound type and type variable type if they exist.
        if (newATM.getKind() == TypeKind.ARRAY &&
                oldATM.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType newAAT = (AnnotatedArrayType) newATM;
            AnnotatedArrayType oldAAT = (AnnotatedArrayType) oldATM;
            atmToTypeElement(newAAT.getComponentType(), oldAAT.getComponentType(),
                    atf, typeToUpdate.innerTypes.vivify(new InnerTypeLocation(
                            TypeAnnotationPosition.getTypePathFromBinary(
                                    Collections.nCopies(2 * idx, 0)))), idx+1);
        } else if (newATM.getKind() == TypeKind.TYPEVAR &&
                oldATM.getKind() == TypeKind.TYPEVAR) {
            AnnotatedTypeVariable newATV = (AnnotatedTypeVariable) newATM;
            AnnotatedTypeVariable oldATV = (AnnotatedTypeVariable) oldATM;
            // It only considers the upper bounds for type variables.
            atmToTypeElement(newATV.getUpperBound(), oldATV.getUpperBound(),
                    atf, typeToUpdate, idx);
        }
    }

    /**
     * Converts an {@link javax.lang.model.element.AnnotationMirror}
     * into an {@link annotations.Annotation}.
     */
    private static Annotation annotationMirrorToAnnotation(AnnotationMirror am) {
        AnnotationDef def = new AnnotationDef(AnnotationUtils.annotationName(am));
        Map<String, AnnotationFieldType> fieldTypes = new HashMap<>();
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
        Map<String, Object> newValues = new HashMap<>();
        for (ExecutableElement ee : values.keySet()) {
            Object value = values.get(ee).getValue();
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> valueList = (List<Object>)value;
                List<Object> newList = new ArrayList<>();
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
     * Adds a field to an AnnotationBuilder.
     * @param fieldKey is the name of the field
     * @param obj is the value of the field
     * @param builder is the AnnotationBuilder
     */
    @SuppressWarnings("unchecked") // This is actually checked in the first
    // instanceOf call below.
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
     * <p>
     * If the receiver of field is an instance of "this", the implementation
     * obtains the ClassSymbol by using classTree. Otherwise, it finds the class
     * of the receiverNode and uses it to obtain the ClassSymbol.
     */
    // TODO: This method could be moved somewhere else.
    private static ClassSymbol getEnclosingClassSymbol(
            ClassTree classTree, FieldAccessNode field) {
        Node receiverNode = field.getReceiver();
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
