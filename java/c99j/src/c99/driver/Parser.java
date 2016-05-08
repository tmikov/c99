package c99.driver;

import c99.*;
import c99.parser.*;
import c99.parser.pp.Prepr;
import c99.parser.pp.SearchPathFactory;
import c99.parser.tree.*;

import java.io.*;

public class Parser
{
private final CompilerOptions m_opts;
private final SearchPathFactory m_incSearch;
private final SimpleErrorReporter m_reporter;
private final PrintStream m_out;
private final int m_debugLevel;

public Parser (
  CompilerOptions opts, SearchPathFactory incSearch, SimpleErrorReporter reporter, PrintStream out,
  int debugLevel
)
{
  m_opts = opts;
  m_incSearch = incSearch;
  m_reporter = reporter;
  m_out = out;
  m_debugLevel = debugLevel;
}

public void run ( String fileName ) throws IOException
{
  SymTable symTable = new SymTable();
  // NOTE: use File.getAbsoluteFile() to make it possible to change the current directory
  // by setting the system property "user.dir". File.getAbsoluteFile() obeys that property.
  Prepr<Symbol> pp = new Prepr<Symbol>(m_opts, m_reporter, m_incSearch.finish(m_opts),
                        fileName, new FileInputStream( new File(fileName).getAbsoluteFile() ), symTable );
  BisonLexer lex = new BisonLexer(m_reporter, symTable, pp);
  final PrintStream saveOut = System.out;
  System.setOut(m_out);
  try
  {
    CParser parser = new CParser(
      lex,
      new CompEnv(m_opts, m_reporter, new OurVisitor(new PrintWriter(m_out))), symTable
    );
    parser.setDebugLevel(m_debugLevel);
    parser.parse();
  }
  finally
  {
    System.setOut(saveOut);
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
