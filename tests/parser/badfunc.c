typedef int TYP;

// error: TYP is interpreted as a type declaring an anonymous parameter
// to an anynymous function. It is equivalent to
// "int p1 ( int fn (TYP p) )"
int bad1 ( int (TYP) )
{
    return TYP;
}

int good1 ( void )
{
    // Unlike the case with bad1, (TYP) is parsed simply as an
    // identifier surrounded by parentheses. It is equivalent to
    // "int TYP"
    int (TYP) = 0;
    return TYP;
}

// error: TYP is interpreted as a type, declaring an anonymous
// parameter to an anonymous function. It is equivalent to
// "void p ( int fn (TYP p) (void ) )";
void bad2 ( int (TYP) ( void ) );

void good2 ()
{
    // Unlike the case with bad2, (TYP) is parsed simply as an
    // identifier surrounded by parenthises. It is equivalent to
    // "int TYP ( void )"
    int  (TYP) ( void );
    TYP();
};
