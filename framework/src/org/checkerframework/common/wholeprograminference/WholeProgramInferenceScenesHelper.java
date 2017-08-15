package org.checkerframework.common.wholeprograminference;

import annotations.Annotation;
import annotations.el.AClass;
import annotations.el.AField;
import annotations.el.AMethod;
import annotations.el.AScene;
import annotations.el.ATypeElement;
import annotations.el.DefException;
import annotations.el.InnerTypeLocation;
import annotations.io.IndexFileParser;
import annotations.io.IndexFileWriter;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;

/**
 * This class stores annotations for fields, method return types, and method parameters.
 *
 * <p>The set of annotations inferred for a certain class is stored in an {@link
 * annotations.el.AScene}, which {@link #writeScenesToJaif} can write into a .jaif file. For
 * example, a class field of a class whose fully-qualified name is {@code my.package.MyClass} will
 * have its inferred type stored in a Scene, and later written into a file named {@code
 * my.package.MyClass.jaif}.
 *
 * <p>This class populates the initial Scenes by reading existing .jaif files on the {@link
 * #jaifFilesPath} directory. Having more information in those initial .jaif files means that the
 * precision achieved by the whole-program inference analysis will be better. {@link
 * #writeScenesToJaif} rewrites the initial .jaif files, and may create new ones.
 */
public class WholeProgramInferenceScenesHelper {

    /**
     * Maps the toString() representation of an ATypeElement and its TypeUseLocation to a set of
     * names of annotations that should not be added to .jaif files for that location.
     */
    private final Map<Pair<String, TypeUseLocation>, Set<String>> annosToIgnore = new HashMap<>();

    /**
     * Directory where .jaif files will be written to and read from. This directory is relative to
     * where the CF's javac command is executed.
     */
    public static final String jaifFilesPath =
            "build" + File.separator + "whole-program-inference" + File.separator;

    /** Indicates whether assignments where the rhs is null should be ignored. */
    private final boolean ignoreNullAssignments;

    /** Maps .jaif file paths (Strings) to Scenes. Relatives to jaifFilesPath. */
    private final Map<String, AScene> scenes = new HashMap<>();

    /**
     * Set representing Scenes that were modified since the last time all Scenes were written into
     * .jaif files. Each String element of this set is a path to the .jaif file of the corresponding
     * Scene in the set. It is obtained by passing a class name as argument to the {@link
     * #getJaifPath} method.
     *
     * <p>Modifying a Scene means adding (or changing) a type annotation for a field, method return
     * type, or method parameter type in the Scene. (Scenes are modified by the method {@link
     * #updateAnnotationSetInScene}.)
     */
    private final Set<String> modifiedScenes = new HashSet<>();

    public WholeProgramInferenceScenesHelper(boolean ignoreNullAssignments) {
        this.ignoreNullAssignments = ignoreNullAssignments;
    }

    /**
     * Write all modified scenes into .jaif files. (Scenes are modified by the method {@link
     * #updateAnnotationSetInScene}.)
     */
    public void writeScenesToJaif() {
        // Create .jaif files directory if it doesn't exist already.
        File jaifDir = new File(jaifFilesPath);
        if (!jaifDir.exists()) {
            jaifDir.mkdirs();
        }
        // Write scenes into .jaif files.
        for (String jaifPath : modifiedScenes) {
            try {
                AScene scene = scenes.get(jaifPath).clone();
                removeIgnoredAnnosFromScene(scene);
                new File(jaifPath).delete();
                if (!scene.prune()) {
                    // Only write non-empty scenes into .jaif files.
                    IndexFileWriter.write(scene, new FileWriter(jaifPath));
                }
            } catch (IOException e) {
                ErrorReporter.errorAbort(
                        "Problem while reading file in: "
                                + jaifPath
                                + ". Exception message: "
                                + e.getMessage(),
                        e);
            } catch (DefException e) {
                ErrorReporter.errorAbort(e.getMessage(), e);
            }
        }
        modifiedScenes.clear();
    }

    /** Returns the String representing the .jaif path of a class given its name. */
    protected String getJaifPath(String className) {
        String jaifPath = jaifFilesPath + className + ".jaif";
        return jaifPath;
    }

    /**
     * Returns the Scene stored in a .jaif file path passed as input. If the file does not exist, an
     * empty Scene is created.
     */
    protected AScene getScene(String jaifPath) {
        AScene scene;
        if (!scenes.containsKey(jaifPath)) {
            File jaifFile = new File(jaifPath);
            scene = new AScene();
            if (jaifFile.exists()) {
                try {
                    IndexFileParser.parseFile(jaifPath, scene);
                } catch (IOException e) {
                    ErrorReporter.errorAbort(
                            "Problem while reading file in: "
                                    + jaifPath
                                    + "."
                                    + " Exception message: "
                                    + e.getMessage(),
                            e);
                }
            }
            scenes.put(jaifPath, scene);
        } else {
            scene = scenes.get(jaifPath);
        }
        return scene;
    }

    /** Returns the AClass in an AScene, given a className and a jaifPath. */
    protected AClass getAClass(String className, String jaifPath) {
        // Possibly reads .jaif file to obtain a Scene.
        AScene scene = getScene(jaifPath);
        return scene.classes.vivify(className);
    }

    /**
     * Updates the set of annotations in a location of a Scene.
     *
     * <ul>
     *   <li>If there was no previous annotation for that location, then the updated set will be the
     *       annotations in newATM.
     *   <li>If there was a previous annotation, the updated set will be the LUB between the
     *       previous annotation and newATM.
     * </ul>
     *
     * <p>
     *
     * @param type ATypeElement of the Scene which will be modified
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @param jaifPath used to identify a Scene
     * @param rhsATM the RHS of the annotated type on the source code
     * @param lhsATM the LHS of the annotated type on the source code
     * @param defLoc the location where the annotation will be added
     */
    protected void updateAnnotationSetInScene(
            ATypeElement type,
            AnnotatedTypeFactory atf,
            String jaifPath,
            AnnotatedTypeMirror rhsATM,
            AnnotatedTypeMirror lhsATM,
            TypeUseLocation defLoc) {
        if (rhsATM instanceof AnnotatedNullType && ignoreNullAssignments) {
            return;
        }
        AnnotatedTypeMirror atmFromJaif =
                AnnotatedTypeMirror.createType(rhsATM.getUnderlyingType(), atf, false);
        typeElementToATM(atmFromJaif, type, atf);
        updatesATMWithLUB(atf, rhsATM, atmFromJaif);
        if (lhsATM instanceof AnnotatedTypeVariable) {
            Set<AnnotationMirror> upperAnnos =
                    ((AnnotatedTypeVariable) lhsATM).getUpperBound().getEffectiveAnnotations();
            // If the inferred type is a subtype of the upper bounds of the
            // current type on the source code, halt.
            if (upperAnnos.size() == rhsATM.getAnnotations().size()
                    && atf.getQualifierHierarchy().isSubtype(rhsATM.getAnnotations(), upperAnnos)) {
                return;
            }
        }
        updateTypeElementFromATM(rhsATM, lhsATM, atf, type, 1, defLoc);
        modifiedScenes.add(jaifPath);
    }

    /**
     * Removes all annotations that should be ignored from an AScene. (See {@link #shouldIgnore}).
     */
    private void removeIgnoredAnnosFromScene(AScene scene) {
        for (AClass aclass : scene.classes.values()) {
            for (AField field : aclass.fields.values()) {
                removeIgnoredAnnosFromATypeElement(field.type, TypeUseLocation.FIELD);
            }
            for (AMethod method : aclass.methods.values()) {
                // Return type
                removeIgnoredAnnosFromATypeElement(method.returnType, TypeUseLocation.RETURN);
                // Receiver type
                removeIgnoredAnnosFromATypeElement(method.receiver.type, TypeUseLocation.RECEIVER);
                // Parameter type
                for (AField param : method.parameters.values()) {
                    removeIgnoredAnnosFromATypeElement(param.type, TypeUseLocation.PARAMETER);
                }
            }
        }
    }

    /**
     * Removes all annotations that should be ignored from an ATypeElement. (See {@link
     * #shouldIgnore}).
     */
    private void removeIgnoredAnnosFromATypeElement(ATypeElement typeEl, TypeUseLocation loc) {
        Set<Annotation> annosToRemove = new HashSet<>();
        String firstKey = typeEl.description.toString() + typeEl.tlAnnotationsHere.toString();
        Set<String> annosToIgnoreForLocation = annosToIgnore.get(Pair.of(firstKey, loc));
        if (annosToIgnoreForLocation == null) {
            // No annotations to ignore for that position.
            return;
        }
        for (Annotation anno : typeEl.tlAnnotationsHere) {
            if (annosToIgnoreForLocation.contains(anno.def().toString())) {
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
     * Updates sourceCodeATM to contain the LUB between sourceCodeATM and jaifATM, ignoring missing
     * AnnotationMirrors from jaifATM -- it considers the LUB between an AnnotationMirror am and a
     * missing AnnotationMirror to be am. The results are stored in sourceCodeATM.
     *
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @param sourceCodeATM the annotated type on the source code
     * @param jaifATM the annotated type on the .jaif file.
     */
    private void updatesATMWithLUB(
            AnnotatedTypeFactory atf,
            AnnotatedTypeMirror sourceCodeATM,
            AnnotatedTypeMirror jaifATM) {

        switch (sourceCodeATM.getKind()) {
            case TYPEVAR:
                updatesATMWithLUB(
                        atf,
                        ((AnnotatedTypeVariable) sourceCodeATM).getLowerBound(),
                        ((AnnotatedTypeVariable) jaifATM).getLowerBound());
                updatesATMWithLUB(
                        atf,
                        ((AnnotatedTypeVariable) sourceCodeATM).getUpperBound(),
                        ((AnnotatedTypeVariable) jaifATM).getUpperBound());
                break;
                //        case WILDCARD:
                // Because inferring type arguments is not supported, wildcards won't be encoutered
                //            updatesATMWithLUB(atf, ((AnnotatedWildcardType) sourceCodeATM).getExtendsBound(),
                //                              ((AnnotatedWildcardType) jaifATM).getExtendsBound());
                //            updatesATMWithLUB(atf, ((AnnotatedWildcardType) sourceCodeATM).getSuperBound(),
                //                              ((AnnotatedWildcardType) jaifATM).getSuperBound());
                //            break;
            case ARRAY:
                updatesATMWithLUB(
                        atf,
                        ((AnnotatedArrayType) sourceCodeATM).getComponentType(),
                        ((AnnotatedArrayType) jaifATM).getComponentType());
                break;
                // case DECLARED:
                // inferring annotations on type arguments is not supported, so no need to recur on
                // generic types. If this was every implemented, this method would need VisitHistory
                // object to prevent infinite recursion on types such as T extends List<T>.
            default:
                // ATM only has primary annotations
                break;
        }

        // LUB primary annotations
        Set<AnnotationMirror> annosToReplace = new HashSet<>();
        for (AnnotationMirror amSource : sourceCodeATM.getAnnotations()) {
            AnnotationMirror amJaif = jaifATM.getAnnotationInHierarchy(amSource);
            // amJaif only contains  annotations from the jaif, so it might be missing
            // an annotation in the hierarchy
            if (amJaif != null) {
                amSource = atf.getQualifierHierarchy().leastUpperBound(amSource, amJaif);
            }
            annosToReplace.add(amSource);
        }
        sourceCodeATM.replaceAnnotations(annosToReplace);
    }

    /**
     * Returns true if {@code am} should not be inserted in source code, for example {@link
     * org.checkerframework.common.value.qual.BottomVal}. This happens when {@code am} cannot be
     * inserted in source code or is the default for the location passed as argument.
     *
     * <p>Invisible qualifiers, which are annotations that contain the {@link
     * org.checkerframework.framework.qual.InvisibleQualifier} meta-annotation, also return true.
     *
     * <p>TODO: Merge functionality somewhere else with {@link
     * org.checkerframework.framework.type.GenericAnnotatedTypeFactory#createQualifierDefaults}.
     * Look into the createQualifierDefaults method before changing anything here. See Issue 683
     * https://github.com/typetools/checker-framework/issues/683
     */
    private boolean shouldIgnore(
            AnnotationMirror am,
            TypeUseLocation location,
            AnnotatedTypeFactory atf,
            AnnotatedTypeMirror atm) {
        Element elt = am.getAnnotationType().asElement();
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
            for (TypeUseLocation loc : defaultQual.locations()) {
                if (loc == TypeUseLocation.ALL || loc == location) {
                    return true;
                }
            }
        }
        DefaultFor defaultQualForLocation = elt.getAnnotation(DefaultFor.class);
        if (defaultQualForLocation != null) {
            for (TypeUseLocation loc : defaultQualForLocation.value()) {
                if (loc == TypeUseLocation.ALL || loc == location) {
                    return true;
                }
            }
        }

        // Checks if am is an implicit annotation.
        // This case checks if it is meta-annotated with @ImplicitFor.
        // TODO: Handle cases of implicit annotations added via an
        // org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator.
        ImplicitFor implicitFor = elt.getAnnotation(ImplicitFor.class);
        if (implicitFor != null) {
            TypeKind[] types = implicitFor.types();
            TypeKind atmKind = atm.getUnderlyingType().getKind();
            for (TypeKind tk : types) {
                if (tk == atmKind) return true;
            }

            try {
                Class<?>[] names = implicitFor.typeNames();
                for (Class<?> c : names) {
                    TypeMirror underlyingtype = atm.getUnderlyingType();
                    while (underlyingtype instanceof javax.lang.model.type.ArrayType) {
                        underlyingtype =
                                ((javax.lang.model.type.ArrayType) underlyingtype)
                                        .getComponentType();
                    }
                    if (c.getCanonicalName().equals(atm.getUnderlyingType().toString())) {
                        return true;
                    }
                }
            } catch (MirroredTypesException e) {
            }
        }

        return false;
    }

    /** Returns a subset of annosSet, consisting of the annotations supported by atf. */
    private Set<Annotation> getSupportedAnnosInSet(
            Set<Annotation> annosSet, AnnotatedTypeFactory atf) {
        Set<Annotation> output = new HashSet<>();
        Set<Class<? extends java.lang.annotation.Annotation>> supportedAnnos =
                atf.getSupportedTypeQualifiers();
        for (Annotation anno : annosSet) {
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
     * Updates an {@link org.checkerframework.framework.type.AnnotatedTypeMirror} to contain the
     * {@link annotations.Annotation}s of an {@link annotations.el.ATypeElement}.
     *
     * @param atm the AnnotatedTypeMirror to be modified
     * @param type the {@link annotations.el.ATypeElement}
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     */
    private void typeElementToATM(
            AnnotatedTypeMirror atm, ATypeElement type, AnnotatedTypeFactory atf) {
        Set<Annotation> annos = getSupportedAnnosInSet(type.tlAnnotationsHere, atf);
        for (Annotation anno : annos) {
            AnnotationMirror am =
                    AnnotationConverter.annotationToAnnotationMirror(anno, atf.getProcessingEnv());
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
                typeElementToATM(atv.getUpperBound(), innerType, atf);
            }
        }
    }

    /**
     * Updates an {@link annotations.el.ATypeElement} to have the annotations of an {@link
     * org.checkerframework.framework.type.AnnotatedTypeMirror} passed as argument. Annotations in
     * the original set that should be ignored (see {@link #shouldIgnore}) are not added to the
     * resulting set. This method also checks if the AnnotatedTypeMirror has explicit annotations in
     * source code, and if that is the case no annotations are added for that location.
     *
     * <p>This method removes from the ATypeElement all annotations supported by atf before
     * inserting new ones. It is assumed that every time this method is called, the
     * AnnotatedTypeMirror has a better type estimate for the ATypeElement. Therefore, it is not a
     * problem to remove all annotations before inserting the new annotations.
     *
     * @param newATM the AnnotatedTypeMirror whose annotations will be added to the ATypeElement
     * @param curATM used to check if the element which will be updated has explicit annotations in
     *     source code
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @param typeToUpdate the ATypeElement which will be updated
     * @param idx used to write annotations on compound types of an ATypeElement
     * @param defLoc the location where the annotation will be added
     */
    private void updateTypeElementFromATM(
            AnnotatedTypeMirror newATM,
            AnnotatedTypeMirror curATM,
            AnnotatedTypeFactory atf,
            ATypeElement typeToUpdate,
            int idx,
            TypeUseLocation defLoc) {
        // Clears only the annotations that are supported by atf.
        // The others stay intact.
        if (idx == 1) {
            // This if avoids clearing the annotations multiple times in cases
            // of type variables and compound types.
            Set<Annotation> annosToRemove =
                    getSupportedAnnosInSet(typeToUpdate.tlAnnotationsHere, atf);
            // This method may be called consecutive times for the same ATypeElement.
            // Each time it is called, the AnnotatedTypeMirror has a better type
            // estimate for the ATypeElement. Therefore, it is not a problem to remove
            // all annotations before inserting the new annotations.
            typeToUpdate.tlAnnotationsHere.removeAll(annosToRemove);
        }

        // Only update the ATypeElement if there are no explicit annotations
        if (curATM.getExplicitAnnotations().size() == 0) {
            for (AnnotationMirror am : newATM.getAnnotations()) {
                addAnnotationsToATypeElement(
                        newATM, atf, typeToUpdate, defLoc, am, curATM.hasEffectiveAnnotation(am));
            }
        } else if (curATM.getKind() == TypeKind.TYPEVAR) {
            // getExplicitAnnotations will be non-empty for type vars whose bounds are explicitly annotated.
            // So instead, only insert the annotation if there is not primary annotation of the same hierarchy.
            // #shouldIgnore prevent annotations that are subtypes of type vars upper bound from being inserted.
            for (AnnotationMirror am : newATM.getAnnotations()) {
                if (curATM.getAnnotationInHierarchy(am) != null) {
                    // Don't insert if the type is already has a primary annotation
                    // in the same hierarchy.
                    break;
                }
                addAnnotationsToATypeElement(
                        newATM, atf, typeToUpdate, defLoc, am, curATM.hasEffectiveAnnotation(am));
            }
        }

        // Recursively update compound type and type variable type if they exist.
        if (newATM.getKind() == TypeKind.ARRAY && curATM.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType newAAT = (AnnotatedArrayType) newATM;
            AnnotatedArrayType oldAAT = (AnnotatedArrayType) curATM;
            updateTypeElementFromATM(
                    newAAT.getComponentType(),
                    oldAAT.getComponentType(),
                    atf,
                    typeToUpdate.innerTypes.vivify(
                            new InnerTypeLocation(
                                    TypeAnnotationPosition.getTypePathFromBinary(
                                            Collections.nCopies(2 * idx, 0)))),
                    idx + 1,
                    defLoc);
        }
    }

    private void addAnnotationsToATypeElement(
            AnnotatedTypeMirror newATM,
            AnnotatedTypeFactory atf,
            ATypeElement typeToUpdate,
            TypeUseLocation defLoc,
            AnnotationMirror am,
            boolean isEffectiveAnnotation) {
        Annotation anno = AnnotationConverter.annotationMirrorToAnnotation(am);
        if (anno != null) {
            typeToUpdate.tlAnnotationsHere.add(anno);
            if (isEffectiveAnnotation || shouldIgnore(am, defLoc, atf, newATM)) {
                // firstKey works as a unique identifier for each annotation
                // that should not be inserted in source code
                String firstKey =
                        typeToUpdate.description.toString()
                                + typeToUpdate.tlAnnotationsHere.toString();
                Pair<String, TypeUseLocation> key = Pair.of(firstKey, defLoc);
                Set<String> annosIgnored = annosToIgnore.get(key);
                if (annosIgnored == null) {
                    annosIgnored = new HashSet<>();
                    annosToIgnore.put(key, annosIgnored);
                }
                annosIgnored.add(anno.def().toString());
            }
        }
    }
}
