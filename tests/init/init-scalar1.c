// 6.7.8#11
int a = 10;
int b = { 10 };
// WARNING
int c = {{ 10 }};
// WARNING
int d = {{{ 20 }}};
// this is actually OK, should only produce a warning
int e = { 10, 20 };
// this is actually OK, should only produce two warnings
int e1 = {{10},20};
// this is actually OK, should only produce warnings
int e1_1 = {{10,20},30};
// ERROR
int e2 = {};

struct S1 { int x; int y; };
struct S1 s1_1 = { 1, {2}};
struct S1 s1_2 = { 1, {{2}}};
