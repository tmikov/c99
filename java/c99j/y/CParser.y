%language "Java"

%define package "c99.parser"
%define public
%define parser_class_name {CParser}

%define parse.error verbose

%locations

%token IDENT  "identifier"
%token TYPENAME "typedef name"
%token INT_NUMBER  "integer number"
%token REAL_NUMBER "real number"
%token CHAR_CONST   "character literal"
%token WIDE_CHAR_CONST "wide character literal"
%token STRING_CONST    "string literal"
%token WIDE_STRING_CONST  "wide string literal"

%token L_BRACKET   "["
%token R_BRACKET   "]"
%token L_PAREN   "("
%token R_PAREN   ")"
%token L_CURLY   "{"
%token R_CURLY   "}"
%token FULLSTOP   "."
%token MINUS_GREATER   "->"

%token PLUS_PLUS   "++"
%token MINUS_MINUS   "--"
%token AMPERSAND   "&"
%token ASTERISK   "*"
%token PLUS   "+"
%token MINUS   "-"
%token TILDE   "~"
%token BANG   "!"

%token SLASH   "/"
%token PERCENT   "%"
%token LESS_LESS   "<<"
%token GREATER_GREATER   ">>"
%token LESS   "<"
%token GREATER   ">"
%token LESS_EQUALS   "<="
%token GREATER_EQUALS   ">="
%token EQUALS_EQUALS   "=="
%token BANG_EQUALS   "!="
%token CARET   "^"
%token VERTICAL   "|"
%token AMPERSAND_AMPERSAND   "&&"
%token VERTICAL_VERTICAL   "||"

%token QUESTION   "?"
%token COLON   ":"
%token SEMICOLON   ";"
%token ELLIPSIS   "..."

%token EQUALS   "="
%token ASTERISK_EQUALS   "*="
%token SLASH_EQUALS   "/="
%token PERCENT_EQUALS   "%="
%token PLUS_EQUALS   "+="
%token MINUS_EQUALS   "-="
%token LESS_LESS_EQUALS   "<<="
%token GREATER_GREATER_EQUALS   ">>="
%token AMPERSAND_EQUALS   "&="
%token CARET_EQUALS   "^="
%token VERTICAL_EQUALS   "|="

%token COMMA   ","


%token AUTO   "auto"
%token BREAK   "break"
%token CASE   "case"
%token CHAR   "char"
%token CONST   "const"
%token CONTINUE   "continue"
%token DEFAULT   "default"
%token DO   "do"
%token DOUBLE   "double"
%token ELSE   "else"
%token ENUM   "enum"
%token EXTERN   "extern"
%token FLOAT   "float"
%token FOR   "for"
%token GOTO   "goto"
%token IF   "if"
%token INLINE   "INLINE"
%token INT   "int"
%token LONG   "long"
%token REGISTER   "register"
%token RESTRICT   "restrict"
%token RETURN   "return"
%token SHORT   "short"
%token SIGNED   "signed"
%token SIZEOF   "sizeof"
%token STATIC   "static"
%token STRUCT   "struct"
%token SWITCH   "switch"
%token TYPEDEF   "typedef"
%token UNION   "union"
%token UNSIGNED   "unsigned"
%token VOID   "void"
%token VOLATILE   "volatile"
%token WHILE   "while"
%token _ALIGNAS   "_Alignas"
%token _ALIGNOF   "_Alignof"
%token _ATOMIC   "_Atomic"
%token _BOOL   "_Bool"
%token _COMPLEX   "_Complex"
%token _GENERIC   "_Generic"
%token _IMAGINARY   "_Imaginary"
%token _NORETURN   "_Noreturn"
%token _STATIC_ASSERT   "_Static_assert"
%token _THREAD_LOCAL   "_Thread_local"

// Set precedences to avoid IF-ELSE `shift'/reduce conflict
%precedence IF
%precedence ELSE

%start translation-unit

%%

identifier:
    IDENT
  ;

/*identifier_opt:
    %empty | identifier
  ;*/

any-identifier:
    TYPENAME
  | IDENT
  ;

any-identifier_opt:
    %empty | any-identifier
  ;

string-literal:
    STRING_CONST
  | string-literal STRING_CONST
  ;

constant:
    INT_NUMBER
  | REAL_NUMBER
  | CHAR_CONST
/*  | enumeration-constant*/
  ;

// A.2.4 External definitions
//

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

// (6.9.1)
declaration-list:
    declaration
  | declaration-list declaration
  ;

declaration-list_opt:
    %empty | declaration-list
  ;

// A.2.2. Declarations
//

// The conflict is as follows:
//    first-part TYPENAME
// Is TYPENAME part of the declaration specifiers, or is it the identifier that is being re-defined?
//
// The rule is that if we have encountered a <type-specifier> or another TYPENAME in "first-part", then this
// TYPENAME is just an identifier. If we haven't, then this TYPENAME is part of the type specifier.
//
// "first-part-without-TYPENAME-and-type-specifier" TYPENAME-as-type
// "first-part-with-TYPENAME-or-type-specifier" TYPENAME-as-ident

// (6.7)
//declaration:
//    declaration-specifiers init-declarator-list_opt ";"
//  | static_assert-declaration
//  ;

// (6.7)
//declaration-specifiers:
//    storage-class-specifier declaration-specifiers_opt
//  | type-specifier declaration-specifiers_opt
//  | type-qualifier declaration-specifiers_opt
//  | function-specifier declaration-specifiers_opt
//  | alignment-specifier declaration-specifiers_opt
//  ;

//declaration-specifiers_opt:
//    %empty | declaration-specifiers
//  ;
declaration-specifiers : "blaa" ;

declaration:
    static_assert-declaration
  | specifier-nots declaration-nots
  | type-specifier declaration-ts
  ;

declaration-nots:
    init-declarator-list-notyp_opt ";"
  | specifier-nots declaration-nots
  | type-specifier declaration-ts
  ;

declaration-ts:
    init-declarator-list_opt ";"
  | specifier-nots declaration-ts
  | type-specifier-notyp declaration-ts
  ;

specifier-nots:
    storage-class-specifier
  | type-qualifier
  | function-specifier
  | alignment-specifier
  ;

// (6.7)
init-declarator-list:
    init-declarator
  | init-declarator-list "," init-declarator
  ;

init-declarator-list_opt:
    %empty | init-declarator-list
  ;

init-declarator-list-notyp:
    init-declarator-notyp
  | init-declarator-list-notyp "," init-declarator
  ;

init-declarator-list-notyp_opt:
    %empty | init-declarator-list-notyp
  ;

// (6.7)
init-declarator:
    declarator
  | declarator "=" initializer
  ;

init-declarator-notyp:
    declarator-notyp
  | declarator-notyp "=" initializer
  ;

// (6.7.1)
storage-class-specifier:
    TYPEDEF
  | EXTERN
  | STATIC
  | _THREAD_LOCAL
  | AUTO
  | REGISTER
  ;

// (6.7.2)
type-specifier:
    type-specifier-notyp
  | typedef-name
  ;

type-specifier-notyp:
    VOID
  | CHAR
  | SHORT
  | INT
  | LONG
  | FLOAT
  | DOUBLE
  | SIGNED
  | UNSIGNED
  | _BOOL
  | _COMPLEX
  | atomic-type-specifier
  | struct-or-union-specifier
  | enum-specifier
  ;

// (6.7.2.1)
struct-or-union-specifier:
    struct-or-union any-identifier_opt "{" struct-declaration-list "}"
  | struct-or-union any-identifier
  ;

// (6.7.2.1)
struct-or-union:
    STRUCT
  | UNION
  ;

// (6.7.2.1)
struct-declaration-list:
    struct-declaration
  | struct-declaration-list struct-declaration
  ;

// (6.7.2.1)
struct-declaration:
    static_assert-declaration
  | type-qualifier struct-declaration-nots
  | type-specifier struct-declaration-ts
  ;

struct-declaration-nots:
    struct-declarator-list-notyp_opt ";"
  | type-qualifier struct-declaration-nots
  | type-specifier struct-declaration-ts
  ;

struct-declaration-ts:
    struct-declarator-list_opt ";"
  | type-qualifier struct-declaration-ts
  | type-specifier-notyp struct-declaration-ts
  ;

// (6.7.2.1)
specifier-qualifier-list:
    type-specifier specifier-qualifier-list_opt
  | type-qualifier specifier-qualifier-list_opt
  ;

specifier-qualifier-list_opt:
    %empty | specifier-qualifier-list
  ;

// (6.7.2.1)
struct-declarator-list:
    struct-declarator
  | struct-declarator-list "," struct-declarator
  ;

struct-declarator-list_opt:
    %empty | struct-declarator-list
  ;

struct-declarator-list-notyp:
    struct-declarator-notyp
  | struct-declarator-list-notyp "," struct-declarator
  ;

struct-declarator-list-notyp_opt:
    %empty | struct-declarator-list-notyp
  ;

// (6.7.2.1)
struct-declarator:
    declarator
  | declarator_opt ":" constant-expression
  ;

struct-declarator-notyp:
    declarator-notyp
  | declarator-notyp_opt ":" constant-expression
  ;

// (6.7.2.2)
enum-specifier:
    ENUM any-identifier_opt "{" enumerator-list "}"
  | ENUM any-identifier_opt "{" enumerator-list "," "}"
  | ENUM any-identifier
  ;

// (6.7.2.2)
enumerator-list:
    enumerator
  | enumerator-list "," enumerator
  ;

// (6.7.2.2)
enumerator:
    enumeration-constant
  | enumeration-constant "=" constant-expression
  ;

enumeration-constant:
    any-identifier
  ;

// (6.7.2.4)
atomic-type-specifier:
    _ATOMIC "(" type-name ")"
  ;

// (6.7.3)
type-qualifier:
    CONST
  | RESTRICT
  | VOLATILE
  | _ATOMIC
  ;

// (6.7.4)
function-specifier:
    INLINE
  | _NORETURN
  ;

// (6.7.5)
alignment-specifier:
    _ALIGNAS "(" type-name ")"
  | _ALIGNAS "(" constant-expression ")"
  ;

// (6.7.6)
declarator:
    pointer_opt direct-declarator
  ;

declarator_opt:
    %empty | declarator
  ;

declarator-notyp:
    pointer direct-declarator
  | direct-declarator-notyp
  ;

declarator-notyp_opt:
    %empty | declarator-notyp
  ;

// (6.7.6)
direct-declarator:
    any-identifier
  | "(" declarator ")"
  | direct-declarator direct-declarator-elem
  ;

direct-declarator-notyp:
    identifier
  | "(" declarator ")"
  | direct-declarator-notyp direct-declarator-elem
  ;

direct-declarator-elem:
    "[" type-qualifier-list_opt assignment-expression_opt "]"
  | "[" STATIC type-qualifier-list_opt assignment-expression "]"
  | "[" type-qualifier-list STATIC assignment-expression "]"
  | "[" type-qualifier-list_opt ASTERISK "]"
  | "(" parameter-type-list ")"
  | "(" identifier-list_opt ")"
  ;

// (6.7.6)
pointer:
    "*" type-qualifier-list_opt
  | "*" type-qualifier-list_opt pointer
  ;

pointer_opt:
    %empty | pointer
  ;

// (6.7.6)
type-qualifier-list:
    type-qualifier
  | type-qualifier-list type-qualifier
  ;

type-qualifier-list_opt:
    %empty | type-qualifier-list
  ;

// (6.7.6)
parameter-type-list:
    parameter-list
  | parameter-list "," "..."
  ;

parameter-type-list_opt:
    %empty | parameter-type-list
  ;

// (6.7.6)
parameter-list:
    parameter-declaration
  | parameter-list "," parameter-declaration
  ;

// (6.7.6)
parameter-declaration:
    declaration-specifiers declarator
  | declaration-specifiers abstract-declarator_opt
  ;

/*
  In a identifier list (old-style parameter list)
  all but the first identifier cannot redefine a typedef.
  (6.9.1-6)
  (If the first one was a typedef then we would assume that this
  is a new style declaration).
*/
// (6.7.6)
identifier-list:
    identifier
  | identifier-list "," any-identifier
  ;

identifier-list_opt:
    %empty | identifier-list
  ;

// (6.7.7)
type-name:
    specifier-qualifier-list abstract-declarator_opt
  ;
  
// (6.7.7)
abstract-declarator:
    pointer
  | pointer_opt direct-abstract-declarator
  ;

abstract-declarator_opt:
    %empty | abstract-declarator
  ;

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
  | "[" "*" "]"
  | "(" parameter-type-list_opt ")"
  ;

// (6.7.8)
typedef-name:
    TYPENAME
  ;

// (6.7.9)
initializer:
    assignment-expression
  | "{" initializer-list "}"
  | "{" initializer-list "," "}"
  ;

// (6.7.9)
initializer-list:
    designation_opt initializer
  | initializer-list "," designation_opt initializer
  ;

// (6.7.9)
designation:
    designator-list "="
  ;

designation_opt:
    %empty | designation
  ;
  
// (6.7.9)
designator-list:
    designator
  | designator-list designator
  ;

// (6.7.9)
designator:
    "[" constant-expression "]"
  | "." any-identifier
// GNU C extension
  | "[" constant-expression "..." constant-expression "]"
  ;

// (6.7.10)
static_assert-declaration:
    _STATIC_ASSERT "(" constant-expression "," string-literal ")" ";"
  ;


// A.2.3 Statements


statement:
    labeled-statement
  | compound-statement
  | expression-statement
  | selection-statement
  | iteration-statement
  | jump-statement
  ;

// (6.8.1)
labeled-statement:
    any-identifier ":" statement
  | CASE constant-expression ":" statement
  | DEFAULT ":" statement
// GNU C Extension
  | CASE constant-expression "..." constant-expression ":" statement
  ;

// (6.8.2)
compound-statement:
    "{" block-item-list_opt "}"
  ;

// (6.8.2)
block-item-list:
    block-item
  | block-item-list block-item
  ;

block-item-list_opt:
    %empty | block-item-list
  ;

// (6.8.2)
block-item:
    declaration
  | statement
  ;

// (6.8.3)
expression-statement:
    expression_opt ";"
  ;

// (6.8.4)
selection-statement:
    IF "(" expression ")" statement  %prec IF
  | IF "(" expression ")" statement ELSE statement  %prec ELSE
  | SWITCH "(" expression ")" statement
  ;

// (6.8.5)
iteration-statement:
    WHILE "(" expression ")" statement
  | DO statement WHILE "(" expression ")" ";"
  | FOR "(" expression_opt ";" expression_opt ";" expression_opt ")" statement
  | FOR "(" declaration expression_opt ";" expression_opt ")" statement
  ;

// (6.8.6)
jump-statement:
    GOTO any-identifier ";"
  | CONTINUE ";"
  | BREAK ";"
  | RETURN expression_opt ";"
  ;

// A.2.1 Expressions

// (6.5.1)
primary-expression:
    identifier
  | constant
  | string-literal
  | "(" expression ")"
  | generic-selection
  ;

// (6.5.1.1)
generic-selection:
    _GENERIC "(" assignment-expression "," generic-assoc-list ")"
  ;

// (6.5.1.1)
generic-assoc-list:
    generic-association
  | generic-assoc-list "," generic-association
  ;

// (6.5.1.1)
generic-association:
    type-name ":" assignment-expression
  | DEFAULT ":" assignment-expression
  ;

// (6.5.2)
postfix-expression:
    primary-expression
  | postfix-expression "[" expression "]"
  | postfix-expression "(" argument-expression-list_opt ")"
  | postfix-expression "." any-identifier
  | postfix-expression "->" any-identifier
  | postfix-expression "++"
  | postfix-expression "--"
  | "(" type-name ")" "{" initializer-list "}"
  | "(" type-name ")" "{" initializer-list "," "}"
  ;

// (6.5.2)
argument-expression-list:
    assignment-expression
  | argument-expression-list "," assignment-expression
  ;

argument-expression-list_opt:
    %empty | argument-expression-list
  ;

// (6.5.3)
unary-expression:
    postfix-expression
  | "++" unary-expression
  | "--" unary-expression
  | unary-operator cast-expression
  | SIZEOF unary-expression
  | SIZEOF "(" type-name ")"
  | _ALIGNOF "(" type-name ")"
// GNU C extension
  | "&&" any-identifier
  ;

// (6.5.3)
unary-operator:
    "&"
  | "*"
  | "+"
  | "-"
  | "~"
  | "!"
  ;

// (6.5.4)
cast-expression:
    unary-expression
  | "(" type-name ")" cast-expression
  ;

// (6.5.5)
multiplicative-expression:
    cast-expression
  | multiplicative-expression "*" cast-expression
  | multiplicative-expression "/" cast-expression
  | multiplicative-expression "%" cast-expression
  ;

// (6.5.6)
additive-expression:
    multiplicative-expression
  | additive-expression "+" multiplicative-expression
  | additive-expression "-" multiplicative-expression
  ;

// (6.5.7)
shift-expression:
    additive-expression
  | shift-expression "<<" additive-expression
  | shift-expression ">>" additive-expression
  ;

// (6.5.8)
relational-expression:
    shift-expression
  | relational-expression "<" shift-expression
  | relational-expression ">" shift-expression
  | relational-expression "<=" shift-expression
  | relational-expression ">=" shift-expression
  ;

// (6.5.9)
equality-expression:
    relational-expression
  | equality-expression "==" relational-expression
  | equality-expression "!=" relational-expression
  ;

// (6.5.10)
AND-expression:
    equality-expression
  | AND-expression "&" equality-expression
  ;

// (6.5.11)
exclusive-OR-expression:
    AND-expression
  | exclusive-OR-expression "^" AND-expression
  ;

// (6.5.12)
inclusive-OR-expression:
    exclusive-OR-expression
  | inclusive-OR-expression "|" exclusive-OR-expression
  ;

// (6.5.13)
logical-AND-expression:
    inclusive-OR-expression
  | logical-AND-expression "&&" inclusive-OR-expression
  ;

// (6.5.14)
logical-OR-expression:
    logical-AND-expression
  | logical-OR-expression "||" logical-AND-expression
  ;

// (6.5.15)
conditional-expression:
    logical-OR-expression
  | logical-OR-expression "?" expression ":" conditional-expression
  ;

// (6.5.16)
assignment-expression:
    conditional-expression
  | unary-expression assignment-operator assignment-expression
  ;

assignment-expression_opt:
    %empty | assignment-expression
  ;

// (6.5.16)
assignment-operator:
    "="
  | "*="
  | "/="
  | "%="
  | "+="
  | "-="
  | "<<="
  | ">>="
  | "&="
  | "^="
  | "|="
  ;

// (6.5.17)
expression:
    assignment-expression
  | expression "," assignment-expression
  ;

expression_opt:
    %empty | expression
  ;

// (6.6)
constant-expression:
    conditional-expression
  ;
