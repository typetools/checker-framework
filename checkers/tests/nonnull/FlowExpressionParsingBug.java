import javax.swing.JMenuBar;

import checkers.nonnull.quals.Nullable;
import checkers.nonnull.quals.RequiresNonNull;


public abstract class FlowExpressionParsingBug {
   
    protected @Nullable JMenuBar menuBar = null;

    @RequiresNonNull("menuBar")
    public void addFavorite() {
    }
    
    @RequiresNonNull("this.menuBar")
    public void addFavorite1() {
    }
}
