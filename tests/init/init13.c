struct S1 { int a; } s1 = { 10, .a = 10 };

int a[] = { [1] = 10, 20, [1] = 11 };

struct S2 { int a, b; } s2;
struct S3 { int x; struct S2 s; int y; };

struct S3 s3 = { .s = s2, .x = 10, .s.b = 20 };



