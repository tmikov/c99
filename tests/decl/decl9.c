struct S
{
    int b;
    struct S * p; // This must not be a forward declaration
};


void func1 ( void )
{
    int a = 1;
    {
        int a = 2, b = a;
        assert( b == 2 );
    }
    {
        int a = a; // undefined value
    }
}
