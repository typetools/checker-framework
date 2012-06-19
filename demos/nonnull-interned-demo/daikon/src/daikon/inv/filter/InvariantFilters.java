package daikon.inv.filter;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import daikon.inv.*;
import daikon.PptMap;
import daikon.VarInfo;
import daikon.PrintInvariants;
import daikon.Daikon;

//  This class contains a collection of invariant filters, and allows other
//  code to perform invariant filtering.  To filter invariants, do the
//  following:
//     o  Instantiate an InvariantFilters object.
//     o  At any time, adjust the filters as necessary using the public methods.
//     o  Call:  invariantFilters.shouldKeep( invariant );
//
//  There are two main kinds of filters: property filters and variable
//  filters.  Property filters attempt to eliminate irrelevant invariants,
//  and are all turned on by default.  Variable filters only keep
//  invariants which contain all or any of a set of variables (depending on
//  variableFilterType).  There are no variable filters by default.  See
//  the manual for more information on property and variable filters.

public class InvariantFilters {
  public static final int ANY_VARIABLE = 1;
  public static final int ALL_VARIABLES = 2;
  int variableFilterType = ANY_VARIABLE;

  // propertyFilters is a map from filter description to filter object.  We
  // need this mapping so that the GUI can easily tell InvariantFilters --
  // by passing in a filter description -- which filter was de/selected.
  // Use TreeMap to preserve order of filters (eg, so that
  // ControlledInvariantFilter will always be last).

  // annoyingly, this doesn't actually do what the original author
  // intended.  TreeMap orders things based on the keys, which in this
  // case is the description of the filter (a string).  What we'd
  // actually like is to order them in order of something like
  // [probability of eliminating an inv]/[expected running time]...in
  // other words, based on a benefit to cost measurement.  hence, this
  // will become a list (in particular a Vector).  This does increase
  // the running time of lookups based on the descriptions from O(log
  // n) to O(n), but that functionality isn't used a whole lot and
  // there are only ~10 filters anyway.

  List<InvariantFilter> propertyFilters = new Vector<InvariantFilter>();
  List<VariableFilter> variableFilters = new ArrayList<VariableFilter>();

  public InvariantFilters() {

    addPropertyFilter( new UnjustifiedFilter());
    addPropertyFilter( new ParentFilter());
    addPropertyFilter( new ObviousFilter());

    // This set of filters is invoked when preparing invariants for processing
    // by Simplify, but before Simplify actually runs this will just not
    // filter anything, so no need to fear recursiveness here.
    addPropertyFilter( new SimplifyFilter( this ));

    addPropertyFilter( new OnlyConstantVariablesFilter());
    addPropertyFilter( new DerivedParameterFilter());
    if (Daikon.output_format == OutputFormat.ESCJAVA) {
      addPropertyFilter( new UnmodifiedVariableEqualityFilter());
    }

    // Filter out invariants that contain certain types of derived variables
    // By default, all derived variales are accepted.
    addPropertyFilter (new DerivedVariableFilter());
  }


  private static InvariantFilters default_filters = null;

  public static InvariantFilters defaultFilters() {
    if (default_filters == null)
      default_filters = new InvariantFilters ();
    return default_filters;
  }


  void addPropertyFilter( InvariantFilter filter ) {
    propertyFilters.add( filter );
  }


  public InvariantFilter shouldKeepVarFilters( Invariant invariant ) {
    // Logger df = PrintInvariants.debugFiltering;
    if (variableFilters.size() != 0) {
      if (variableFilterType == InvariantFilters.ANY_VARIABLE) {
        boolean hasAnyVariable = false;
        for (VariableFilter filter : variableFilters) {
          if (! filter.shouldDiscard( invariant )) {
            hasAnyVariable = true;
          }
        }
        if (! hasAnyVariable) {
          if (invariant.logOn())
            invariant.log ("Failed ANY_VARIABLE filter");
          return variableFilters.get(0);
        }
      } else if (variableFilterType == InvariantFilters.ALL_VARIABLES) {
        for (VariableFilter filter : variableFilters) {
          if (filter.shouldDiscard( invariant )) {
            if (invariant.logOn())
              invariant.log ("Failed ALL_VARIABLES filter"
                             + filter.getClass().getName());
              return filter;
          }
        }
      }
    }
    return null;
  }

  public InvariantFilter shouldKeepPropFilters( Invariant invariant ) {
    Logger df = PrintInvariants.debugFiltering;
    for (InvariantFilter filter : propertyFilters) {
      if (invariant.logDetail() || df.isLoggable(Level.FINE)) {
        invariant.log (df, "applying " + filter.getClass().getName());
      }
      if (filter.shouldDiscard( invariant )) {
        if (invariant.logOn() || df.isLoggable(Level.FINE))
          invariant.log (df, "failed " + filter.getClass().getName()
                         // + " num_values = "
                         // + ",num_unmod_missing_samples==" + invariant.ppt.num_mod_samples()
                         + ": " + invariant.format()
                         );
        return filter;
      }
     }
     return null;
  }

  public InvariantFilter shouldKeep( Invariant invariant ) {
    Logger df = PrintInvariants.debugFiltering;

    if (invariant.logOn() || df.isLoggable(Level.FINE)) {
      invariant.log (df, "filtering");
    }

    if (invariant instanceof GuardingImplication) {
      invariant = ((Implication) invariant).right;
    }

    // Do variable filters first since they eliminate more invariants.
    InvariantFilter result = shouldKeepVarFilters(invariant);
    if (result != null) {
      return result;
    }

    //  Property filters.
    invariant.log ("Processing " + propertyFilters.size() + " Prop filters");
    return (shouldKeepPropFilters(invariant));

  }

  public Iterator<InvariantFilter> getPropertyFiltersIterator() {
    return propertyFilters.iterator();
  }

  private InvariantFilter find(String description) {
    InvariantFilter answer = null;
    for (InvariantFilter filter : propertyFilters) {
      if (filter.getDescription().equals(description)) {
        answer = filter;
      }
    }
    return answer;
  }

  public boolean getFilterSetting( String description ) {
    return find(description).getSetting();
  }

  public void changeFilterSetting( String description, boolean turnOn ) {
    InvariantFilter filter = find(description);
    if (turnOn)
      filter.turnOn();
    else
      filter.turnOff();
  }

  public void turnFiltersOn() {
    for (InvariantFilter filter : propertyFilters) {
      filter.turnOn();
    }
  }

  public void turnFiltersOff() {
    for (InvariantFilter filter : propertyFilters) {
      filter.turnOff();
    }
  }

  public void addVariableFilter( String variable ) {
    variableFilters.add( new VariableFilter( variable ));
  }

  public boolean containsVariableFilter( String variable ) {
    for (VariableFilter vf : variableFilters) {
      if (vf.getVariable().equals( variable )) {
        return true;
      }
    }
    return false;
  }

  public void removeVariableFilter( String variable ) {
    boolean foundOnce = false;
    for (Iterator<VariableFilter> iter = variableFilters.iterator(); iter.hasNext(); ) {
      VariableFilter vf = iter.next();
      if (vf.getVariable().equals( variable )) {
        iter.remove();
        foundOnce = true;
      }
    }
    if (foundOnce) return;

    throw new Error( "InvariantFilters.removeVariableFilter():  filter for variable '" + variable + "' not found" );
  }

  // variableFilterType is either InvariantFilters.ANY_VARIABLE or InvariantFilters.ALL_VARIABLES
  public void setVariableFilterType( int variableFilterType ) {
    this.variableFilterType = variableFilterType;
  }

  //  I wasn't sure where to put this method, but this class seems like the
  //  best place.  Equality invariants only exist to make invariant output
  //  more readable, so this shouldn't be in the main Daikon engine code.
  //  Equality invariants aren't *directly* related to filtering, but their
  //  existence allows us to filter out certain invariants containing
  //  non-canonical variables ("x=y", "x=z", etc).  Also, I am hesitant to
  //  put code dealing with the internal workings of invariants/daikon in
  //  the GUI package.  Therefore, I put the method here rather than in
  //  InvariantsGUI.java.

  /**
   * This function takes a list of invariants, finds the equality
   * Comparison invariants (x==y, y==z), and deletes and replaces them
   * with Equality invariants (x==y==z).  The first variable in an
   * Equality invariant is always the canonical variable of the group.
   * The Equality invariants are inserted into the beginning.  Equality
   * invariants are useful when it comes to displaying invariants.
   **/
  public static List<Invariant> addEqualityInvariants( List<Invariant> invariants ) {

    return invariants;

  }

  // For debugging
  static String reprVarInfoList(List<VarInfo> vis) {
    String result = "";
    for (int i=0; i<vis.size(); i++) {
      if (i!=0) result += " ";
      VarInfo vi = vis.get(i);
      result += vi.name();
    }
    return "[ " + result + " ]";
  }

}
