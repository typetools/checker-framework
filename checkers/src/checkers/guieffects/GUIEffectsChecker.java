package checkers.guieffects;

import checkers.basetype.BaseTypeChecker;
import checkers.guieffects.quals.AlwaysSafe;
import checkers.guieffects.quals.PolyUI;
import checkers.guieffects.quals.UI;
import checkers.quals.TypeQualifiers;
import checkers.source.SupportedLintOptions;

@SupportedLintOptions({"debugSpew"})
@TypeQualifiers({
    UI.class, PolyUI.class, AlwaysSafe.class
})
public class GUIEffectsChecker extends BaseTypeChecker {}
