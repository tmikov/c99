// Test nesting causing a resize of the init stack

struct S1 { int a; };
struct S2 { struct S1 s; };
struct S3 { struct S2 s; };
struct S4 { struct S3 s; };
struct S5 { struct S4 s; };
struct S6 { struct S5 s; };

struct S6 s6 = { 10 };
