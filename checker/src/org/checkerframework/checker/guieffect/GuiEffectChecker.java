package org.checkerframework.checker.guieffect;

import org.checkerframework.checker.guieffect.qual.AlwaysSafe;
import org.checkerframework.checker.guieffect.qual.PolyUI;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.source.SupportedLintOptions;

@SupportedLintOptions({"debugSpew"})
@TypeQualifiers({
    UI.class, PolyUI.class, AlwaysSafe.class
})
public class GuiEffectChecker extends BaseTypeChecker {}
