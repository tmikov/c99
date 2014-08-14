// Declarator failure cases

int a1[10];
int a2[const 10];
int a3[static 10];
int a4[*];

void func1 ( int p1[10],
  int p2[const 10],  // FAIL!
  int p3[static 10], // FAIL!
  int p4[*] )        // FAIL!
{
  int b1[10];
  int b2[const 10];
  int b3[static 10];
  int b4[*];
}

void func2 ( a, b, c );
void func3 ( a, b, a, c );

