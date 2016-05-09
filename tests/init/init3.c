struct S { int a, b; };
struct S s1 = { 1, 2 };
struct S s2 = { 1, 2, 3 }; // WARNING
struct S s3 = { 1 };
struct S s4 = { {1} };
struct S s5 = { {{1}} }; // WARNING

struct T { int x; struct S s; int y; };
struct T t1 = 1; // ERROR
struct T t2 = s1; // ERROR
struct T t3 = { 1, 2, 3, 4 };
struct T t4 = t3;
