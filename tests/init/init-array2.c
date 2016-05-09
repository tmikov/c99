int a1[5] = 1; // ERROR
int a2[5] = { 1, 2, 3 };
int a3[5] = { 1, 2, 3, 4, 5, 6 }; // WARNING: excess elements
int a4[5] = {{1, 2}}; // WARNING
int a5[5] = { [3] = 3, 4, [1] = 1 };
int a6[5] = { [-1] = -1 }; // ERROR
int a7[5] = { [8] = 8 }; // ERROR
int a8[5] = { [5] = 5 }; // ERROR
