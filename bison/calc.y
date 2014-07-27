
%token<Integer> NUM
%destructor {System.out.println($$);} NUM

%%
prog: NUM { System.out.println( $NUM +""+ @$ ); };


