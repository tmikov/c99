char x[] = { "hello" };

struct S { int index; char name[4]; int tag; };
struct S s1 = { 1, "na", 2};
struct S s2 = { 1, {"na"}, 2};
