package c99.parser.tree;

import c99.Types;
import c99.parser.Symbol;

import java.util.LinkedHashMap;

/** This class needed only as a workaround for a Bison BUG - generics in %type */
public final class TIdentList extends LinkedHashMap<Symbol,Types.Param>
{
}
