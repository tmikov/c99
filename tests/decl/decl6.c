struct X {int x;};

// FAIL: This should be a warning!
void func ( union X { int x; } y );

void func3 ( union X { int x; } y, struct X { int x; } z );

void func1 ( int p1[10],
  int p2[const 10],  // FAIL!
  int p3[static 10], // FAIL!
  int p4[*] )        // FAIL!
{
}

// This should compile, albeit with a warning
void func2 ( struct SS { int a; } x, struct SS y )
{
}

void func5 ( struct S5 * p );
