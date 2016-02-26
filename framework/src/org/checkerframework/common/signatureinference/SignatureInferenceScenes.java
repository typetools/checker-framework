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
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.IgnoreInSignatureInference;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.qualframework.base.TypeMirrorConverter.Key;

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
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;

/**
 * SignatureInferenceScenes represents a set of annotations that are inferred
 * in a program.
 * This class stores annotations for fields, method return types, and method
 * parameters.
 * <p>
 * The set of annotations inferred for a certain class is stored in an
 * {@link annotations.el.AScene}, which {@link #writeScenesToJaif} can write
 * into a .jaif file.
 * For example, a class field of a class whose fully-qualified name is
 * {@code my.package.MyClass} will have its inferred type stored in a Scene,
 * and later written into a file named {@code my.package.MyClass.jaif}.
 * <p>
 * This class populates the initial Scenes by reading existing .jaif files
 * on the {@link #JAIF_FILES_PATH} directory. Having more information in those
 * initial .jaif files means that the precision achieved by the signature
 * inference analysis will be better. {@link #writeScenesToJaif} rewrites
 * the initial .jaif files, and may create new ones.
 * <p>
 * Calling an update* method
 * ({@link #updateInferredFieldType},
 * {@link #updateInferredMethodParametersTypes}, or
 * {@link #updateInferredMethodReturnType})
 * replaces the currently-stored type for an element in a Scene, if any,
 * by the LUB of it and the update method's argument.
 * <p>
 * This class does not store annotations for an element if the element has
 * explicit annotations:  an update* method ignores an
 * explicitly annotated field, method return, or method parameter when
 * passed as an argument.
 * <p>
 *  @author pbsf
 */
//  TODO: We could add an option to update the type of explicitly annotated
//  elements, but this currently is not recommended since the
//  insert-annotations-to-source tool, which adds annotations from .jaif files
//  into source code, adds annotations on top of existing
//  annotations. See https://github.com/typetools/annotation-tools/issues/105 .
//  TODO: Ensure that annotations are inserted deterministically into .jaif
//  files. This is important otherwise developers might achieve different
//  results (order of annotations) when running the signature inference for
//  the same set of files.
public class SignatureInferenceScenes {

    /**
     * Directory where .jaif files will be written to and read from.
     * This directory is relative to where the CF's javac command is executed.
     */
    public final static String JAIF_FILES_PATH = "build" + File.separator +
            "signature-inference" + File.separator;

    /** Maps .jaif file paths (Strings) to Scenes. */
    private static Map<String, AScene> scenes = new HashMap<>();

    /** Maps a DefaultLocation to the name of all annotations that should not be
     * added to .jaif files for that location.
     */
    private static Map<DefaultLocation, Set<String>> annosToIgnore = new HashMap<>();

    /**
     * Set representing Scenes that were modified since the last time all
     * Scenes were written into .jaif files. Each String element of this set
     * is a path to the .jaif file of the corresponding Scene in the set. It
     * is obtained by passing a class name as argument to the
     * {@link #getJaifPath} method.
     * <p>
     * Modifying a Scene means adding (or changing) a type annotation for a
     * field, method return type, or method parameter type in the Scene.
     * (Scenes are modified by the method {@link #updateAnnotationSetInScene}.)
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
     * (Scenes are modified by the method {@link #updateAnnotationSetInScene}.)
     */
    public static void writeScenesToJaif() {
        // Create .jaif files directory if it doesn't exist already.
        File jaifDir = new File(JAIF_FILES_PATH);
        if (!jaifDir.exists()) {
            jaifDir.mkdirs();
        }
        // Write scenes into .jaif files.
        for (String jaifPath : modifiedScenes) {
            try {
                AScene scene = scenes.get(jaifPath);
                removeIgnoredAnnosFromScene(scene);
                if (!scene.prune()) {
                    // Only write non-empty scenes into .jaif files.
                    IndexFileWriter.write(scene, new FileWriter(jaifPath));
                }
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
     * Removes all annotations that should be ignored from an AScene.
     * (See {@link #shouldIgnore}).
     */
    private static void removeIgnoredAnnosFromScene(AScene scene) {
        for (AClass aclass : scene.classes.values()) {
            for (AField field : aclass.fields.values()) {
                removeIgnoredAnnosFromATypeElement(
                        field.type, DefaultLocation.FIELD);
            }
            for (AMethod method : aclass.methods.values()) {
                // Return type
                removeIgnoredAnnosFromATypeElement(
                        method.returnType, DefaultLocation.RETURNS);
                // Parameter type
                for (AField param : method.parameters.values()) {
                    removeIgnoredAnnosFromATypeElement(
                            param.type, DefaultLocation.PARAMETERS);
                }
            }
        }
    }

    /**
     * Removes all annotations that should be ignored from an ATypeElement.
     * (See {@link #shouldIgnore}).
     */
    private static void removeIgnoredAnnosFromATypeElement(ATypeElement typeEl,
            DefaultLocation loc) {
        Set<Annotation> annosToRemove = new HashSet<>();
        Set<String> annosToIgnoreForLocation = annosToIgnore.get(loc);
        if (annosToIgnoreForLocation == null) return; // No annotations to ignore for that position.
        for (Annotation anno : typeEl.tlAnnotationsHere) {
            if (annosToIgnoreForLocation.contains(anno.def().name)) {
                annosToRemove.add(anno);
            }
        }
        typeEl.tlAnnotationsHere.removeAll(annosToRemove);

        // Remove annotations recursively for inner types.
        for (ATypeElement innerType : typeEl.innerTypes.values()) {
            removeIgnoredAnnosFromATypeElement(innerType, loc);
        }
    }

    /**
     * Updates the parameter types of the method methodElt in the Scene of the
     * receiverTree's enclosing class.
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
     * @param receiverTree the Tree of the class that contains the method being
     * invoked.
     * @param methodElt the element of the method being invoked.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the method parameters' types.
     */
    public static void updateInferredMethodParametersTypes(
            MethodInvocationNode methodInvNode, Tree receiverTree,
            ExecutableElement methodElt, AnnotatedTypeFactory atf) {
        if (receiverTree == null) {
            // TODO: Method called from static context.
            // I struggled to obtain the ClassTree of a method called
            // from a static context and currently I'm ignoring it.
            return;
        }
        ClassSymbol classSymbol = getEnclosingClassSymbol(receiverTree);
        if (classSymbol == null) {
            // TODO: Handle anonymous classes.
            // Also struggled to obtain the ClassTree from an anonymous class.
            // Ignoring it for now.
            return;
        }
        // TODO: We must handle cases where the method is declared on a superclass.
        // Currently we are ignoring them. See ElementUtils#getSuperTypes.
        if (!classSymbol.getEnclosedElements().contains(methodElt)) return;

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
            if (treeNode == null) {
                // TODO: Handle variable-length list as parameter.
                // An ArrayCreationNode with a null tree is created when the
                // parameter is a variable-length list. We are ignoring it for now.
                continue;
            }
            AnnotatedTypeMirror argATM = atf.getAnnotatedType(treeNode);
            AField param = method.parameters.vivify(i);
            updateAnnotationSetInScene(
                    param.type, atf, jaifPath, argATM, paramATM,
                    DefaultLocation.PARAMETERS);
        }
    }

    /**
     * Updates the type of the field lhs in the Scene of the class with
     * tree classTree.
     * <p>
     * If the Scene contains no entry for the field lhs,
     * the entry will be created and its type will be the type of rhs. If the
     * Scene previously contained an entry/type for lhs, its new type will be
     * the LUB between the previous type and the type of rhs.
     * <p>
     * @param lhs the field whose type will be refined.
     * @param rhs the expression being assigned to the field.
     * @param classTree the ClassTree for the enclosing class of the assignment.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the field's type.
     */
    public static void updateInferredFieldType(
            Node lhs, Node rhs, ClassTree classTree, AnnotatedTypeFactory atf) {
        FieldAccessNode lhsFieldNode = (FieldAccessNode) lhs;
        ClassSymbol classSymbol = getEnclosingClassSymbol(classTree, lhsFieldNode);
        if (classSymbol == null) return; // TODO: Handle anonymous classes.
        // TODO: We must handle cases where the field is declared on a superclass.
        // Currently we are ignoring them. See ElementUtils#getSuperTypes.
        if (!classSymbol.getEnclosedElements().contains(lhsFieldNode.getElement())) return;

        String className = classSymbol.flatname.toString();
        String jaifPath = getJaifPath(className);
        AClass clazz = getAClass(className, jaifPath);

        AField field = clazz.fields.vivify(lhsFieldNode.getFieldName());
        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(lhs.getTree());
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(rhs.getTree());
        updateAnnotationSetInScene(
                field.type, atf, jaifPath, rhsATM, lhsATM, DefaultLocation.FIELD);
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
     * @param methodTree the tree of the method whose return type
     * may be updated.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the method's return type.
     */
    public static void updateInferredMethodReturnType(ReturnNode retNode,
            ClassSymbol classSymbol, MethodTree methodTree,
            AnnotatedTypeFactory atf) {
        if (classSymbol == null) return; // TODO: Handle anonymous classes.
        String className = classSymbol.flatname.toString();

        String jaifPath = getJaifPath(className);
        AClass clazz = getAClass(className, jaifPath);

        AMethod method = clazz.methods.vivify(JVMNames.getJVMMethodName(methodTree));
        // Method return type
        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(methodTree).getReturnType();
        // Type of the expression returned
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(retNode.getTree().getExpression());
        updateAnnotationSetInScene(
                method.returnType, atf, jaifPath, rhsATM, lhsATM,
                DefaultLocation.RETURNS);
    }

    /**
     * Updates the set of annotations in a location of a Scene.
     *   <ul>
     *     <li>If there was no previous annotation for that location, then the
     *      updated set will be the annotations in newATM.</li>
     *     <li>If there was a previous annotation, the updated set will be the
     *      LUB between the previous annotation and newATM.</li>
     *   </ul>
     * <p>
     * @param type ATypeElement of the Scene which will be modified.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used.
     * @param jaifPath used to identify a Scene.
     * @param rhsATM the RHS of the annotated type on the source code.
     * @param lhsATM the LHS of the annotated type on the source code.
     * @param defLoc the location where the annotation will be added.
     */
    private static void updateAnnotationSetInScene(ATypeElement type,
            AnnotatedTypeFactory atf, String jaifPath,
            AnnotatedTypeMirror rhsATM, AnnotatedTypeMirror lhsATM,
            DefaultLocation defLoc) {
        AnnotatedTypeMirror atmFromJaif = AnnotatedTypeMirror.createType(
                rhsATM.getUnderlyingType(), atf, false);
        typeElementToATM(atmFromJaif, type, atf);
        updatesATMWithLUB(atf, rhsATM, atmFromJaif);
        if (lhsATM instanceof AnnotatedTypeVariable) {
            Set<AnnotationMirror> upperAnnos = ((AnnotatedTypeVariable) lhsATM).
                        getUpperBound().getAnnotations();
            // If the inferred type is a subtype of the upper bounds of the
            // current type on the source code, halt.
            if (upperAnnos.size() == rhsATM.getAnnotations().size() &&
                    atf.getQualifierHierarchy().isSubtype(
                            rhsATM.getAnnotations(), upperAnnos)) {
                return;
            }
        }
        updateTypeElementFromATM(rhsATM, lhsATM, atf, type, 1, defLoc);
        modifiedScenes.add(jaifPath);
    }

    /**
     * Updates sourceCodeATM to contain the LUB between sourceCodeATM and
     * jaifATM, ignoring missing AnnotationMirrors from jaifATM -- it considers
     * the LUB between an AnnotationMirror am and a missing AnnotationMirror to be am.
     * The results are stored in sourceCodeATM.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used.
     * @param sourceCodeATM the annotated type on the source code.
     * @param jaifATM the annotated type on the .jaif file.
     */
    private static void updatesATMWithLUB(AnnotatedTypeFactory atf,
            AnnotatedTypeMirror sourceCodeATM, AnnotatedTypeMirror jaifATM) {
        if (sourceCodeATM instanceof AnnotatedTypeVariable) {
            updatesATMWithLUB(atf, ((AnnotatedTypeVariable) sourceCodeATM).getLowerBound(),
                    ((AnnotatedTypeVariable) jaifATM).getLowerBound());
            updatesATMWithLUB(atf, ((AnnotatedTypeVariable) sourceCodeATM).getUpperBound(),
                    ((AnnotatedTypeVariable) jaifATM).getUpperBound());
        }
        if (sourceCodeATM instanceof AnnotatedArrayType) {
            updatesATMWithLUB(atf, ((AnnotatedArrayType) sourceCodeATM).getComponentType(),
                    ((AnnotatedArrayType) jaifATM).getComponentType());
        }
        Set<AnnotationMirror> annosToReplace = new HashSet<>();
        for (AnnotationMirror amSource : sourceCodeATM.getAnnotations()) {
            AnnotationMirror amJaif = jaifATM.getAnnotationInHierarchy(amSource);
            if (amJaif != null) {
                amSource = atf.getQualifierHierarchy().leastUpperBound(
                        amSource, amJaif);
            }
            annosToReplace.add(amSource);
        }
        sourceCodeATM.replaceAnnotations(annosToReplace);
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
     * Returns true if am should not be inserted in source code, for example 
     * {@link org.checkerframework.common.value.qual.BottomVal}. This happens
     * when am cannot be inserted in source code or is the default for the
     * location passed as argument.
     * <p>
     * Invisible qualifiers, which are annotations that contain the
     * {@link org.checkerframework.framework.qual.InvisibleQualifier}
     * meta-annotation, also return true.
     * <p>
     * TODO: Merge functionality somewhere else with
     * {@link org.checkerframework.framework.type.GenericAnnotatedTypeFactory#createQualifierDefaults}.
     * Look into the createQualifierDefaults method before changing anything here.
     */
    private static boolean shouldIgnore(AnnotationMirror am,
            DefaultLocation location, AnnotatedTypeFactory atf,
            AnnotatedTypeMirror atm) {
        AnnotationMirror bottomAnno = atf.getQualifierHierarchy().getBottomAnnotation(am);
        if (AnnotationUtils.annotationName(bottomAnno) == AnnotationUtils.annotationName(am)) {
            // Ignore annotation if it is the bottom type.
            return true;
        }
        Element elt = am.getAnnotationType().asElement();
        if (elt.getAnnotation(IgnoreInSignatureInference.class) != null) {
            return true;
        }
        // Checks if am is an implementation detail (a type qualifier used
        // internally by the type system and not meant to be seen by the user.)
        Target target = elt.getAnnotation(Target.class);
        if (target != null && target.value().length == 0) return true;
        if (elt.getAnnotation(InvisibleQualifier.class) != null) return true;

        // Checks if am is default
        if (elt.getAnnotation(DefaultQualifierInHierarchy.class) != null) {
            return true;
        }
        DefaultQualifier defaultQual = elt.getAnnotation(DefaultQualifier.class);
        if (defaultQual != null) {
            for (DefaultLocation loc : defaultQual.locations()) {
                if (loc == DefaultLocation.ALL || loc == location) {
                    return true;
                }
            }
        }
        DefaultFor defaultQualForLocation = elt.getAnnotation(DefaultFor.class);
        if (defaultQualForLocation != null) {
            for (DefaultLocation loc : defaultQualForLocation.value()) {
                if (loc == DefaultLocation.ALL || loc == location) {
                    return true;
                }
            }
        }

        // Checks if am is an implicit annotation
        // TODO: Handles cases of implicit annotations added via an
        // org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator
        ImplicitFor implicitFor = elt.getAnnotation(ImplicitFor.class);
        if (implicitFor != null) {
            TypeKind[] types = implicitFor.types();
            for (TypeKind tk : types) if (tk == atm.getKind()) return true;

            try {
                Class<? extends AnnotatedTypeMirror>[] classes =
                        implicitFor.typeClasses();
                for (Class<? extends AnnotatedTypeMirror> c : classes) {
                    if (c.isInstance(atm)) return true;
                }
    
                Class<?>[] names = implicitFor.typeNames();
                for (Class<?> c : names) {
                    TypeMirror underlyingtype = atm.getUnderlyingType();
                    while (underlyingtype instanceof javax.lang.model.type.ArrayType) {
                            underlyingtype = ((javax.lang.model.type.ArrayType)underlyingtype).
                                    getComponentType();
                    }
                    if (c.getCanonicalName().equals(
                            atm.getUnderlyingType().toString())) {
                        return true;
                    }
                }
            } catch (MirroredTypesException e) {}
        }

        // Special cases that should be ignored:
        // {@link org.checkerframework.qualframework.base.TypeMirrorConverter.Key}
        if (AnnotationUtils.areSameByClass(am, Key.class)) {
            return true;
        }
        return false;
    }

    /**
     * Returns a subset of annosSet, consisting of the annotations supported
     * by atf.
     */
    private static Set<Annotation> getSupportedAnnosInSet(Set<Annotation> annosSet,
            AnnotatedTypeFactory atf) {
        Set<Annotation> output = new HashSet<>();
        Set<Class<? extends java.lang.annotation.Annotation>> supportedAnnos
                = atf.getSupportedTypeQualifiers();
        for (Annotation anno: annosSet) {
            for (Class<? extends java.lang.annotation.Annotation> clazz : supportedAnnos) {
                // TODO: Remove comparison by name, and make this routine more efficient.
                if (clazz.getName().equals(anno.def.name)) {
                    output.add(anno);
                }
            }
        }
        return output;
    }

    /**
     * Updates an {@link org.checkerframework.framework.type.AnnotatedTypeMirror}
     * to contain the {@link annotations.Annotation}s of an
     * {@link annotations.el.ATypeElement}.
     * @param atm the AnnotatedTypeMirror to be modified
     * @param type the {@link annotations.el.ATypeElement}.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used.
     */
    private static void typeElementToATM(AnnotatedTypeMirror atm,
            ATypeElement type, AnnotatedTypeFactory atf) {
        Set<Annotation> annos = getSupportedAnnosInSet(type.tlAnnotationsHere,
                atf);
        for (Annotation anno: annos) {
            AnnotationMirror am = annotationToAnnotationMirror(
                    anno, atf.getProcessingEnv());
            atm.addAnnotation(am);
        }
        if (atm.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType aat = (AnnotatedArrayType) atm;
            for (ATypeElement innerType : type.innerTypes.values()) {
                typeElementToATM(aat.getComponentType(), innerType, atf);
            }
        }
        if (atm.getKind() == TypeKind.TYPEVAR) {
            AnnotatedTypeVariable atv = (AnnotatedTypeVariable) atm;
            for (ATypeElement innerType : type.innerTypes.values()) {
                typeElementToATM(atv.getLowerBound(), innerType, atf);
                typeElementToATM(atv.getUpperBound(), innerType, atf);
            }
        }
    }

   /**
    * Updates an {@link annotations.el.ATypeElement} to have the annotations of an
    * {@link org.checkerframework.framework.type.AnnotatedTypeMirror} passed
    * as argument. Annotations in the original set that should be ignored
    * (see {@link #shouldIgnore}) are not added to the resulting set.
    * This method also checks if the AnnotatedTypeMirror has explicit
    * annotations in source code, and if that is the case no annotations are
    * added for that location.
    * <p>
    * This method removes from the ATypeElement all annotations supported by atf
    * before inserting new ones. It is assumed that every time this method is
    * called, the AnnotatedTypeMirror has a better type estimate for the
    * ATypeElement. Therefore, it is not a problem to remove all annotations
    * before inserting  the new annotations.
    *
    * @param newATM the AnnotatedTypeMirror whose annotations will be added to
    * the ATypeElement.
    * @param curATM used to check if the element which will be updated has
    * explicit annotations in source code.
    * @param atf the annotated type factory of a given type system, whose
    * type hierarchy will be used.
    * @param typeToUpdate the ATypeElement which will be updated.
    * @param idx used to write annotations on compound types of an ATypeElement.
    * @param defLoc the location where the annotation will be added.
    */
    private static void updateTypeElementFromATM(AnnotatedTypeMirror newATM,
            AnnotatedTypeMirror curATM, AnnotatedTypeFactory atf,
            ATypeElement typeToUpdate, int idx, DefaultLocation defLoc) {
        // Clears only the annotations that are supported by atf.
        // The others stay intact.
        if (idx == 1) {
            // This if avoids clearing the annotations multiple times in cases
            // of type variables and compound types.
            Set<Annotation> annosToRemove = getSupportedAnnosInSet(
                    typeToUpdate.tlAnnotationsHere, atf);
            // This method may be called consecutive times for the same ATypeElement.
            // Each time it is called, the AnnotatedTypeMirror has a better type
            // estimate for the ATypeElement. Therefore, it is not a problem to remove
            // all annotations before inserting the new annotations.
            typeToUpdate.tlAnnotationsHere.removeAll(annosToRemove);
        }

        // Only update the ATypeElement if there are no explicit annotations
        if (curATM.getExplicitAnnotations().size() == 0) {
            for (AnnotationMirror am : newATM.getAnnotations()) {
                Annotation anno = annotationMirrorToAnnotation(am);
                if (anno != null) {
                    if (shouldIgnore(am, defLoc, atf, newATM)) {
                        Set<String> annosIgnored = annosToIgnore.get(defLoc);
                        if (annosIgnored == null) {
                            annosIgnored = new HashSet<>();
                            annosToIgnore.put(defLoc, annosIgnored);
                        }
                        annosIgnored.add(anno.def().name);
                    }
                    typeToUpdate.tlAnnotationsHere.add(anno);
                }
            }
        }

        // Recursively update compound type and type variable type if they exist.
        if (newATM.getKind() == TypeKind.ARRAY &&
                curATM.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType newAAT = (AnnotatedArrayType) newATM;
            AnnotatedArrayType oldAAT = (AnnotatedArrayType) curATM;
            updateTypeElementFromATM(newAAT.getComponentType(), oldAAT.getComponentType(),
                    atf, typeToUpdate.innerTypes.vivify(new InnerTypeLocation(
                            TypeAnnotationPosition.getTypePathFromBinary(
                                    Collections.nCopies(2 * idx, 0)))), idx+1, defLoc);
        } else if (newATM.getKind() == TypeKind.TYPEVAR &&
                curATM.getKind() == TypeKind.TYPEVAR) {
            AnnotatedTypeVariable newATV = (AnnotatedTypeVariable) newATM;
            AnnotatedTypeVariable oldATV = (AnnotatedTypeVariable) curATM;
            updateTypeElementFromATM(newATV.getUpperBound(), oldATV.getUpperBound(),
                    atf, typeToUpdate, idx, defLoc);
            updateTypeElementFromATM(newATV.getLowerBound(), oldATV.getLowerBound(),
                    atf, typeToUpdate, idx, defLoc);
        }
    }

    // TODO: The two conversion methods below could be somewhere else. Maybe in AFU?
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
    private static AnnotationFieldType getAnnotationFieldType(ExecutableElement ee, Object value) {
        if (value instanceof List<?>) {
            AnnotationValue defaultValue = ee.getDefaultValue();
            if (defaultValue == null || ((ArrayType)((Array)defaultValue).type) == null) {
                List<?> listV = (List<?>)value;
                if (!listV.isEmpty()) {
                    ScalarAFT scalarAFT = (ScalarAFT) getAnnotationFieldType(ee,
                            ((AnnotationValue)listV.get(0)).getValue());
                    if (scalarAFT != null) {
                        return new ArrayAFT(scalarAFT);
                    } else {
                        return null;
                    }
                }
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
     * obtains the ClassSymbol by using classTree. Otherwise, the ClassSymbol
     * is from the field's receiver.
     */
    // TODO: These methods below could be moved somewhere else.
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
        return getEnclosingClassSymbol(receiverNode.getTree());
    }

    /**
     * Returns the ClassSymbol of the class encapsulating
     * tree passed as parameter.
     */
    private static ClassSymbol getEnclosingClassSymbol(Tree tree) {
        Element symbol = InternalUtils.symbol(tree);
        if (symbol instanceof ClassSymbol) {
            return (ClassSymbol) symbol;
        } else if (symbol instanceof VarSymbol) {
            return ((VarSymbol)symbol).asType().asElement().enclClass();
        } else if (symbol instanceof MethodSymbol) {
            return ((MethodSymbol)symbol).enclClass();
        }
        return null;
    }

}
