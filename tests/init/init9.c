struct S { int a, b; };
struct T { int x; struct S s; int y; };

struct S s1 = 10; // ERROR
struct S s2 = { 10 };
struct S s3 = s2; 

int i1 = 1;
int i2 = { 1 };
