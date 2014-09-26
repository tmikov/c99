%token VOID INT CONST IDENT ASTERISK STATIC
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
    declaration-specifiers declarator declaration-list_opt compound-statement
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
    pointer_opt direct-declarator
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
direct-declarator:
    IDENT
  | "(" declarator ")"
  | direct-declarator direct-declarator-elem
  ;

direct-declarator-elem:
    "[" type-qualifier-list_opt assignment-expression_opt "]"
  | "[" STATIC type-qualifier-list_opt assignment-expression "]"
  | "[" type-qualifier-list STATIC assignment-expression "]"
  | "[" type-qualifier-list_opt ASTERISK "]"
  | "(" parameter-type-list ")"
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

