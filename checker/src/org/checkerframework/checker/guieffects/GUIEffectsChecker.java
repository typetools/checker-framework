package org.checkerframework.checker.guieffects;

import org.checkerframework.checker.guieffects.qual.AlwaysSafe;
import org.checkerframework.checker.guieffects.qual.PolyUI;
import org.checkerframework.checker.guieffects.qual.UI;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.source.SupportedLintOptions;

@SupportedLintOptions({"debugSpew"})
@TypeQualifiers({
    UI.class, PolyUI.class, AlwaysSafe.class
})
public class GUIEffectsChecker extends BaseTypeChecker {}
