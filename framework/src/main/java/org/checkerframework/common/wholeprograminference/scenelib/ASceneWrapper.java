package org.checkerframework.common.wholeprograminference.scenelib;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.wholeprograminference.SceneToStubWriter;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.UserError;
import scenelib.annotations.Annotation;
import scenelib.annotations.el.AClass;
import scenelib.annotations.el.AField;
import scenelib.annotations.el.AMethod;
import scenelib.annotations.el.AScene;
import scenelib.annotations.el.ATypeElement;
import scenelib.annotations.el.DefException;
import scenelib.annotations.io.IndexFileWriter;

/**
 * Every class in scene-lib (from the Annotation File Utilities) is final, so none of them can be
 * extended. However, scene-lib also doesn't provide enough information to usefully print stub
 * files: it lacks information about what is and is not an enum, about the base types of variables,
 * and more.
 *
 * <p>This class wraps AScene but provides access to that missing information. This allows us to
 * preserve the code that generates .jaif files, while allowing us to sanely and safely keep the
 * information we need to generate stubs.
 *
 * <p>TODO: remove the dependency on scene-lib entirely
 */
public class ASceneWrapper {

    /** The AScene being wrapped. */
    private AScene theScene;

    /** The classes in the scene. */
    private Map<@BinaryName String, AClassWrapper> classes;

    /**
     * Constructor. Pass the AScene to wrap.
     *
     * @param theScene the scene to wrap
     */
    public ASceneWrapper(AScene theScene) {
        this.theScene = theScene;
        this.classes = new HashMap<>();
    }

    /**
     * Fetch the classes in this scene, represented as AClassWrapper objects.
     *
     * @return an immutable map from binary names to AClassWrapper objects
     */
    public Map<@BinaryName String, AClassWrapper> getClasses() {
        return ImmutableMap.copyOf(classes);
    }

    /**
     * Removes all annotations that should be ignored from an AScene.
     *
     * @param scene the scene from which to remove annotations
     * @param annosToIgnore maps the toString() representation of an ATypeElement and its
     *     TypeUseLocation to a set of names of annotations that should not be added to .jaif files
     *     for that location.
     */
    private void removeIgnoredAnnosFromScene(
            AScene scene, Map<Pair<String, TypeUseLocation>, Set<String>> annosToIgnore) {
        for (AClass aclass : scene.classes.values()) {
            for (AField field : aclass.fields.values()) {
                removeIgnoredAnnosFromATypeElement(
                        field.type, TypeUseLocation.FIELD, annosToIgnore);
            }
            for (AMethod method : aclass.methods.values()) {
                // Return type
                removeIgnoredAnnosFromATypeElement(
                        method.returnType, TypeUseLocation.RETURN, annosToIgnore);
                // Receiver type
                removeIgnoredAnnosFromATypeElement(
                        method.receiver.type, TypeUseLocation.RECEIVER, annosToIgnore);
                // Parameter type
                for (AField param : method.parameters.values()) {
                    removeIgnoredAnnosFromATypeElement(
                            param.type, TypeUseLocation.PARAMETER, annosToIgnore);
                }
            }
        }
    }

    /**
     * Removes all annotations that should be ignored from an ATypeElement.
     *
     * @param typeEl the type element from which to remove annotations
     * @param loc the location where typeEl in used
     * @param annosToIgnore maps the toString() representation of an ATypeElement and its
     *     TypeUseLocation to a set of names of annotations that should not be added to .jaif files
     *     for that location.
     */
    private void removeIgnoredAnnosFromATypeElement(
            ATypeElement typeEl,
            TypeUseLocation loc,
            Map<Pair<String, TypeUseLocation>, Set<String>> annosToIgnore) {
        String firstKey = typeEl.description.toString() + typeEl.tlAnnotationsHere;
        Set<String> annosToIgnoreForLocation = annosToIgnore.get(Pair.of(firstKey, loc));
        if (annosToIgnoreForLocation != null) {
            Set<Annotation> annosToRemove = new HashSet<>();
            for (Annotation anno : typeEl.tlAnnotationsHere) {
                if (annosToIgnoreForLocation.contains(anno.def().toString())) {
                    annosToRemove.add(anno);
                }
            }
            typeEl.tlAnnotationsHere.removeAll(annosToRemove);
        }

        // Recursively remove ignored annotations from inner types
        if (!typeEl.innerTypes.isEmpty()) {
            for (ATypeElement innerType : typeEl.innerTypes.values()) {
                removeIgnoredAnnosFromATypeElement(innerType, loc, annosToIgnore);
            }
        }
    }

    /**
     * Write the scene wrapped by this object to a jaif on the given path.
     *
     * @param jaifPath the path to the jaif file to be written
     * @param annosToIgnore which annotations should be ignored in which contexts
     */
    public void writeToJaif(
            String jaifPath, Map<Pair<String, TypeUseLocation>, Set<String>> annosToIgnore) {
        try {
            AScene scene = theScene.clone();
            removeIgnoredAnnosFromScene(scene, annosToIgnore);
            scene.prune();
            new File(jaifPath).delete();
            if (!scene.isEmpty()) {
                // Only write non-empty scenes into .jaif files.
                IndexFileWriter.write(scene, new FileWriter(jaifPath));
            }
        } catch (IOException e) {
            throw new UserError(
                    "Problem while reading file in: "
                            + jaifPath
                            + ". Exception message: "
                            + e.getMessage(),
                    e);
        } catch (DefException e) {
            throw new BugInCF(e);
        }
    }

    /**
     * Write the scene represented by this object to a stub file.
     *
     * @param jaifPath a path that ends in ".jaif". The stub file will be created on the same path,
     *     but the extension will be replaced with ".astub"
     * @param annosToIgnore which annotations to ignore in which contexts
     */
    public void writeToStub(
            String jaifPath, Map<Pair<String, TypeUseLocation>, Set<String>> annosToIgnore) {
        removeIgnoredAnnosFromScene(theScene, annosToIgnore);
        theScene.prune();
        String stubPath = jaifPath.replace(".jaif", ".astub");
        new File(stubPath).delete();
        if (!theScene.isEmpty()) {
            // Only write non-empty scenes into .astub files.
            try {
                SceneToStubWriter.write(this, new FileWriter(stubPath));
            } catch (IOException e) {
                throw new UserError("Problem while writing %s: %s", stubPath, e.getMessage());
            }
        }
    }

    /**
     * Interact with scenelib to add a class to the scene.
     *
     * @param className the binary name of the class to be added to the scene
     * @return an AClassWrapper representing that class
     */
    public AClassWrapper vivifyClass(@BinaryName String className) {
        if (classes.containsKey(className)) {
            return classes.get(className);
        } else {
            AClass aClass = theScene.classes.getVivify(className);
            AClassWrapper wrapper = new AClassWrapper(aClass);
            classes.put(className, wrapper);
            return wrapper;
        }
    }

    /**
     * Avoid using this if possible; use the other methods of this class unless you absolutely need
     * an AScene.
     *
     * @return the representation of this scene using only the AScene. Returns a clone.
     */
    public AScene getAScene() {
        return theScene.clone();
    }
}
