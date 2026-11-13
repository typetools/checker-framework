package org.checkerframework.javacutil.trees;

import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A DetachedVarSymbol represents a variable that is not part of any AST Tree. DetachedVarSymbols
 * are created when desugaring source code constructs and they carry important type information, but
 * some methods such as TreeInfo.declarationFor do not work on them.
 */
public class DetachedVarSymbol extends Symbol.VarSymbol {

  /** The declaration tree for this variable, or null if it has not been set. */
  protected @Nullable VariableTree decl;

  /**
   * Constructs a detached variable symbol, given its flags, name, type, and owner.
   *
   * @param flags the modifier flags of the variable
   * @param name the name of the variable
   * @param type the type of the variable
   * @param owner the symbol that owns the variable, such as a method
   */
  public DetachedVarSymbol(long flags, Name name, Type type, Symbol owner) {
    super(flags, name, type, owner);
    this.decl = null;
  }

  /**
   * Sets the declaration tree for the variable.
   *
   * @param decl the declaration tree for the variable
   */
  public void setDeclaration(VariableTree decl) {
    this.decl = decl;
  }

  /**
   * Returns the declaration tree for the variable.
   *
   * @return the declaration tree for the variable, or null if it has not been set
   */
  public @Nullable VariableTree getDeclaration() {
    return decl;
  }
}
