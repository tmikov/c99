package c99.driver;

import c99.SimpleErrorReporter;
import c99.parser.pp.PreprOptions;
import c99.parser.pp.SearchPathFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

@RunWith(Parameterized.class)
public class PreprocessorTest
{
private final File m_input;
private final File m_expectedOutput;
private final File m_expectedMessages;

private static final File TEST_DIR = new File("tests/pp/").getAbsoluteFile();

public PreprocessorTest ( String name, File input, File expectedOutput, File expectedMessages ) throws ParseException
{
  m_input = input;
  m_expectedOutput = expectedOutput;
  m_expectedMessages = expectedMessages;
}

private static String m_saveCurDir;

@BeforeClass
public static void prepare ()
{
  m_saveCurDir = System.setProperty("user.dir", TEST_DIR.getAbsolutePath());
}

@AfterClass
public static void cleanup ()
{
  System.setProperty("user.dir", m_saveCurDir);
}

@Test
public void testPrepr () throws Exception
{
  ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
  StringWriter err = new StringWriter(1024);

  PreprOptions opts = new PreprOptions();
  opts.setForcedDate(new SimpleDateFormat("MMM ddd yyyy HH:mm:ss").parse("Jul 13 2014 10:00:00"));

  SearchPathFactory incSearch = new SearchPathFactory();
  SimpleErrorReporter reporter = new SimpleErrorReporter(new PrintWriter(err, true));

  new Preprocessor(opts, incSearch, reporter, new PrintStream(out)).run(m_input.getPath(), true, false);

  compare(m_expectedOutput, new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
  compare(m_expectedMessages, new StringReader(err.toString()));
}

private static void compare (File expected, Reader actual) throws IOException
{
  try (LineNumberReader exp = new LineNumberReader(new FileReader(expected));
       LineNumberReader act = new LineNumberReader(actual))
  {
    String expLine, actLine;

    while ( (expLine = exp.readLine()) != null | (actLine = act.readLine()) != null)
      assertEquals(expected + ": line "+ exp.getLineNumber() +" is different", expLine, actLine);
  }
}

@Parameters(name = "{0}")
public static Collection<Object[]> enumerateTests () throws IOException
{
  final String[] testFiles = TEST_DIR.list(( dir, name ) -> name.matches(".+\\.c"));

  ArrayList<Object[]> p = new ArrayList<Object[]>(testFiles.length);
  for ( String fn : testFiles )
    p.add(new Object[]{ fn, new File(fn), new File(TEST_DIR, fn + ".i").getAbsoluteFile(),
      new File(TEST_DIR, fn + ".msg").getAbsoluteFile() });

  return p;
}
}
