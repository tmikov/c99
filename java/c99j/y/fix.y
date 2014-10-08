%{
#include <stdio.h>

int yylex ( void );
void yyerror ( const char * msg );

#define YYERROR_VERBOSE 1
%}

%token VOID INT CONST IDENT STATIC
%token SEMI ";"
%token LPAR "("
%token RPAR ")"
%token ASTERISK "*"
%%

// (6.9)
translation-unit:
    %empty
  | translation-unit external-declaration
  ;

// (6.9)
external-declaration:
    function-definition 
  | declaration        
  ;

// (6.9.1)
function-definition:
    declaration-specifiers declarator-func declaration-list_opt compound-statement
  ;

compound-statement:
   "{" "}"
  ;

// (6.9.1)
declaration-list:
    declaration                   
  | declaration-list declaration 
  ;

declaration-list_opt:
    %empty                      
  | declaration-list
  ;


declaration:
    declaration-specifiers init-declarator-list_opt ";" 
  ;

// deliberate right recursion here
declaration-specifiers:
    specifier 
  | specifier declaration-specifiers
  ;

specifier:
    type-specifier
  | type-qualifier
  ; 

type-specifier:
    VOID 
  | INT  
  ;

init-declarator-list:
    init-declarator
  | init-declarator-list "," init-declarator
  ;

init-declarator-list_opt:
    %empty
  | init-declarator-list
  ;

// (6.7)
init-declarator:
    declarator 
  ;


// (6.7.6)
declarator:
    declarator-func
  | declarator-nofunc
  ;

declarator-func:
    pointer_opt direct-declarator-func
  ;

declarator-nofunc:
    pointer_opt direct-declarator-nofunc
  ;

pointer:
                  "*" type-qualifier-list_opt
  | pointer       "*" type-qualifier-list_opt
  ;

pointer_opt:
    %empty 
  | pointer
  ;

// (6.7.6)
type-qualifier-list:
    type-qualifier                     
  | type-qualifier-list type-qualifier
  ;

type-qualifier-list_opt:
    %empty              
  | type-qualifier-list
  ;


// (6.7.3)
type-qualifier:
    CONST    
 ;

// (6.7.6)

// Parenthesized idenitifier
pident:
    IDENT
  | "(" pident ")"
  ; 

direct-declarator:
    direct-declarator-func
  | direct-declarator-nofunc
  ;

direct-declarator-func:
    pident elem-func
  | "(" direct-declarator-func ")"
  | direct-declarator-func direct-declarator-elem
  ;

direct-declarator-nofunc:
    pident
  | d2
  ;

d2:
    pident elem-nofunc
  | "(" pointer direct-declarator ")"
  | d2 direct-declarator-elem
  ;

direct-declarator-elem:
    elem-nofunc
  | elem-func
  ;

elem-nofunc:
    "[" type-qualifier-list_opt assignment-expression_opt "]"
  | "[" STATIC type-qualifier-list_opt assignment-expression "]"
  | "[" type-qualifier-list STATIC assignment-expression "]"
  | "[" type-qualifier-list_opt ASTERISK "]"
  ;

elem-func:
    "(" parameter-type-list ")"
  | "(" identifier-list_opt ")"
  ;

abstract-declarator:
    pointer
  | pointer_opt direct-abstract-declarator
  ;

/*abstract-declarator_opt:
    %empty      { $$ = null; }
  | abstract-declarator
  ;*/

// (6.7.7)
direct-abstract-declarator:
    "(" abstract-declarator ")" 
  | direct-abstract-declarator-elem 
  | direct-abstract-declarator direct-abstract-declarator-elem 
  ;

direct-abstract-declarator-elem:
    "[" type-qualifier-list assignment-expression_opt "]"
  | "[" assignment-expression_opt "]"
  | "[" STATIC type-qualifier-list_opt assignment-expression "]"
  | "[" type-qualifier-list STATIC assignment-expression "]"
  | "[" ASTERISK "]"
  | "(" parameter-type-list ")"
  | "(" ")"
  ;

// (6.7.6)
parameter-type-list:
    parameter-list
  | parameter-list "," "..." 
  ;

// (6.7.6)
parameter-list:
    parameter-declaration                    
  | parameter-list "," parameter-declaration
  ;

// (6.7.6)
parameter-declaration:
    declaration-specifiers
  | declaration-specifiers pointer
  | declaration-specifiers pointer direct-declarator 
  | declaration-specifiers         direct-declarator
  | declaration-specifiers pointer direct-abstract-declarator
  | declaration-specifiers         direct-abstract-declarator
  ;

// (6.7.6)
identifier-list:
    IDENT
  | identifier-list "," IDENT
  ;

identifier-list_opt:
    %empty        
  | identifier-list
  ;

assignment-expression:
    IDENT
  ;

assignment-expression_opt:
    %empty
  | assignment-expression
  ;
%%

int yylex ( void )
{
  static int toks[] = {
    // int (*a)(void);
    INT, LPAR, ASTERISK, IDENT, RPAR, LPAR, VOID, RPAR, SEMI,
    // int (a)(int x);
    INT, LPAR, IDENT, RPAR, LPAR, INT, IDENT, RPAR, SEMI,
    // void (*signal(int sig))(int)
    VOID, LPAR, ASTERISK, IDENT, LPAR, INT, IDENT, RPAR, RPAR, LPAR, INT, RPAR, SEMI,
    // int (func) ( void ) (void)
    INT, LPAR, IDENT, RPAR, LPAR, VOID, RPAR, LPAR, VOID, RPAR, SEMI
  };
  static int i = 0;

  return i < sizeof(toks)/sizeof(toks[0]) ? toks[i++] : EOF;
}

void yyerror ( const char * err )
{
  fprintf( stderr, "error: %s\n", err );
}

int main ( void )
{
  return yyparse();
}
