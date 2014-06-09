package c99;

public interface IErrorReporter
{
public void warning ( ISourceRange rng, String format, Object... args );
public void error ( ISourceRange rng, String format, Object... args );
public String formatRange ( ISourceRange rng );
}
