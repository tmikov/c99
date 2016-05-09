struct S { int a, b; };
struct T { int x; struct S s; int y; };


struct T t1 = { 1, 2, 3, 4 };
struct T t2 = { 1, {2}, 3 };
struct S s1 = { 10, 20 };
struct T t3 = { 1, s1, 2 };
struct T t4 = { 1, t3, 2 }; // ERROR
