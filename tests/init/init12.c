// Designator error cases

int sc1 = { .d = 30 };  // ERROR
int sc2 = { [3] = 31 }; // ERROR

char a[] = { [1] = "a" }; // ERROR
char b[] = { .a = "b" }; // ERROR
