package c99.parser;

import c99.Constant;
import c99.parser.tree.TExpr;

public class InitActions extends DeclActions
{

private final int validateIndex ( TExpr.ArithConstant index )
{
  Constant.IntC vindex = (Constant.IntC) index.getValue();
  long nindex;

  if (!vindex.fitsInLong() || (nindex = vindex.asLong()) > Integer.MAX_VALUE)
  {
    error( index, "index integer overflow" );
    return -1;
  }

  if (nindex < 0 )
  {
    error( index, "negative designator index" );
    return -1;
  }

  return (int)nindex;
}

public final parsedInit.Designator indexDesignator ( CParser.Location loc, TExpr.ArithConstant index )
{
  int nindex = validateIndex( index );
  return new parsedInit.IndexDesignator( loc, nindex < 0, nindex >= 0 ? nindex : 0 );
}

public final parsedInit.Designator fieldDesignator ( CParser.Location loc, Symbol ident )
{
  return new parsedInit.FieldDesignator( loc, false, ident );
}

public final parsedInit.Designator rangeDesignator ( CParser.Location loc, TExpr.ArithConstant first, TExpr.ArithConstant last )
{
  pedWarning( loc, "range designators are a GCC-extension" );

  int nfirst = validateIndex( first );
  int nlast = validateIndex( last );

  if (nlast < 0)
    nfirst = -1;

  if (nfirst >= 0 && nlast < nfirst)
  {
    error( loc, "array designator range [%d ... %d] is empty", nfirst, nlast );
    nfirst = -1;
  }

  return new parsedInit.RangeDesignator( loc, nfirst < 0, nfirst >= 0 ? nfirst : 0, nfirst >= 0 ? nlast : 0 );
}

public final parsedInit.Initializer initializer ( CParser.Location loc, TExpr.Expr expr )
{
  return new parsedInit.InitializerExpr( loc, expr );
}

public final parsedInit.InitializerList initializerList (
  CParser.Location loc, parsedInit.InitializerList list, parsedInit.Designator designation, parsedInit.Initializer elem
)
{
  if (list == null)
    list = new parsedInit.InitializerList(null);
  elem.setDesignation( designation );
  list.add( elem );
  list.setRange( loc );
  return list;
}

public final parsedInit.InitializerList emptyInitializerList ( CParser.Location loc )
{
  return new parsedInit.InitializerList(loc);
}

}
