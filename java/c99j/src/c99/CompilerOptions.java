package c99;

public class CompilerOptions
{
public boolean noStdInc = false;
public boolean gccExtensions = true;

public boolean warnUndef = false;

public boolean signedChar = false;

public int maxAlign = 8;
public int defCodePointers = 0; // 0:near, 1:far, 2:huge
public int defDataPointers = 0; // 0:near, 1:far, 2:huge
} // class

