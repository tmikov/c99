// Designator error cases

struct S1 { int a; } s1 = { .b = 10 };

int a1[] = { .c = 20 };

struct S1 s2 = { [1] = 20 };

struct S1 s3 = { .a.b = 20 };
