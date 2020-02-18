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
 * The Annotation File Utilities scene-lib library is a bit of a dumpster fire. Its classes are all
 * final, so they cannot be extended. However, it also doesn't provide enough information to
 * usefully print stub files: it lacks information about what is and is not an enum, about the base
 * types of variables, and more.
 *
 * <p>This class wraps AScene but provides access to that missing information. This allows us to
 * preserve the code that generates .jaif files, while allowing us to sanely and safely keep the
 * information we need to generate stubs.
 */
public class ASceneWrapper {

    /** The AScene being wrapped. */
    private AScene theScene;

    /** The classes in the scene. */
    private Map<@BinaryName String, AClassWrapper> classes;

    /** Constructor. Pass the AScene to wrap. */
    public ASceneWrapper(AScene theScene) {
        this.theScene = theScene;
        this.classes = new HashMap<>();
    }

    public Map<@BinaryName String, AClassWrapper> getClasses() {
        return ImmutableMap.copyOf(classes);
    }

    /** Removes all annotations that should be ignored from an AScene.. */
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

    /** Removes all annotations that should be ignored from an ATypeElement. */
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
     */
    public AScene getAScene() {
        return theScene.clone();
    }
}
