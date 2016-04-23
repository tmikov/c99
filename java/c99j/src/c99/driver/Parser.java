package c99.driver;

import c99.*;
import c99.parser.*;
import c99.parser.pp.Prepr;
import c99.parser.pp.SearchPathFactory;
import c99.parser.tree.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Arrays;

public class Parser
{
public static void main ( String[] args )
{
  if ("--cpp".equals( args[0] ))
  {
    PreprMain.main(Arrays.copyOfRange(args, 1, args.length));
    return;
  }

  try
  {
    String fileName = null;

    CompilerOptions opts = new CompilerOptions();
    SearchPathFactory incSearch = new SearchPathFactory();
    int debugLevel = 0;

    for ( int i = 0; i < args.length; ++i )
    {
      final String arg = args[i];

      if ("--nostdinc".equals( arg ))
        opts.getPreprOptions().setNoStdInc( true );
      if ("--debug".equals( arg ))
        debugLevel = 1;
      else if (arg.startsWith("-I") || arg.startsWith("-i"))
      {
        String tmp = arg.substring( 2 );
        if (tmp.length() == 0)
        {
          System.err.println( "**fatal: missing argument for " + arg );
          System.exit(1);
        }
        if (arg.startsWith("-I"))
          incSearch.addInclude( tmp );
        else
          incSearch.addQuotedInclude(tmp);
      }
      else if (arg.startsWith("-"))
      {
        System.err.println( "**fatal: unknown command line option '"+arg +"'" );
        System.exit(1);
      }
      else
      {
        if (fileName != null)
        {
          System.err.println( "**fatal: More than one input filename specified" );
          System.exit(1);
        }
        fileName = arg;
      }
    }

    if (fileName == null)
    {
      System.err.println( "**fatal: No input filename specified" );
      System.exit(1);
    }

    DummyErrorReporter reporter = new DummyErrorReporter();
    SymTable symTable = new SymTable();
    // NOTE: use File.getAbsoluteFile() to make it possible to change the current directory
    // by setting the system property "user.dir". File.getAbsoluteFile() obeys that property.
    Prepr<Symbol> pp = new Prepr<Symbol>( opts, reporter, incSearch.finish( opts ),
                          fileName, new FileInputStream( new File(fileName).getAbsoluteFile() ), symTable );
    BisonLexer lex = new BisonLexer(reporter, symTable, pp);
    CParser parser = new CParser(
      lex,
      new CompEnv( opts, reporter, new OurVisitor(new PrintWriter(System.out))), symTable
    );
    parser.setDebugLevel( debugLevel );
    parser.parse();
  }
  catch (Exception e)
  {
    e.printStackTrace();
  }
}

private static final class OurVisitor implements Visitor.TranslationUnit
{
  private final PrintWriter m_out;
  private int m_indent;

  private OurVisitor ( PrintWriter out )
  {
    m_out = out;
  }

  @Override
  public void visitRecordDecl ( TSpecTagNode specNode, Decl decl, boolean definition )
  {
    m_out.format( "RecordDecl <%s> %s%s\n", SourceRange.formatRange(decl), decl.type.readableType(),
            definition ? " definition" : " forward ref");
    if (definition)
    {
      m_indent += 2;
      Types.StructUnionSpec agg = (Types.StructUnionSpec) decl.type.spec;
      for (Types.Member m : agg.getFields())
        visitField( agg, m );
      m_indent -= 2;
    }
    m_out.flush();
  }

  private void visitField ( Types.StructUnionSpec agg, Types.Member m )
  {
    MiscUtils.printIndent( m_indent, m_out );
    m_out.format( "FieldDecl <%s> %s: %s\n", SourceRange.formatRange(m), m.name.name, m.type.readableType() );
  }

  @Override
  public Visitor.DeclaratorList visitDeclaratorList ( TSpecNode specNode )
  {
    return new Visitor.DeclaratorList()
    {
      @Override
      public void visitDeclaration ( TDeclaration tDecl, Decl decl )
      {
        m_out.format( "Decl <%s> %s %s %s: %s\n", SourceRange.formatRange(decl),
                decl.sclass, decl.linkage, decl.symbol.name,
                decl.type.readableType() );
      }

      @Override
      public void visitEmptyDeclaration ( TDeclaration tDecl )
      {
        if (false)
        {
          m_out.format( "EmptyDecl <%s> %s %s: %s\n", SourceRange.formatRange(tDecl),
                  tDecl.sclass, tDecl.linkage, tDecl.type.readableType() );
        }
      }

      @Override public void end ()
      {
        m_out.flush();
      }
    };
  }
}


}
