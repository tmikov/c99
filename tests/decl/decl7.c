struct S1 func1 ( void );

int (func2 ( void ))[10]; //BAD: function returning array

typedef void FUNC1 ( void );
FUNC1 funcs2[10]; //BAD: array of functions

FUNC1 func3 ( void ); //BAD: function returning a function

void (func4 ( void ))(void);

struct S1 func1 ( void ) //BAD: incomplete type in function definition
{}

struct S2
{
    void func1 ( void ); //BAD: field declared as function
};

void (funcs1[10])(void); //BAD: array of functions
