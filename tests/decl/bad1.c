struct X {int x;};

// FAIL: This should be a warning!
void func ( union X { int x; } y );

void func1 ( int p1[10],
  int p2[const 10],  // FAIL!
  int p3[static 10], // FAIL!
  int p4[*] )        // FAIL!
{
}
