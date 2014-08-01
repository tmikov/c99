%language "Java"

%define package "c99.parser"
%code imports { import c99.Constant; }
%define public
%define parser_class_name {CParser}
%define extends {ParserActions}

%define parse.error verbose

%locations

%token<Symbol> IDENT  "identifier"
%token<Symbol> TYPENAME "typedef name"
%token<Constant.IntC> INT_NUMBER  "integer number"
%token<Constant.RealC> REAL_NUMBER "real number"
%token<Constant.IntC> CHAR_CONST   "character literal"
%token WIDE_CHAR_CONST "wide character literal"
%token<byte[]> STRING_CONST    "string literal"
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
%token<Code> AMPERSAND   "&"
%token<Code> ASTERISK   "*"
%token<Code> PLUS   "+"
%token<Code> MINUS   "-"
%token<Code> TILDE   "~"
%token<Code> BANG   "!"

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
%token<Code> ELLIPSIS   "..."

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


%token<Code> AUTO   "auto"
%token BREAK   "break"
%token CASE   "case"
%token<Code> CHAR   "char"
%token<Code> CONST   "const"
%token CONTINUE   "continue"
%token DEFAULT   "default"
%token DO   "do"
%token<Code> DOUBLE   "double"
%token ELSE   "else"
%token<Code> ENUM   "enum"
%token<Code> EXTERN   "extern"
%token<Code> FLOAT   "float"
%token FOR   "for"
%token GOTO   "goto"
%token IF   "if"
%token<Code> INLINE   "INLINE"
%token<Code> INT   "int"
%token<Code> LONG   "long"
%token<Code> REGISTER   "register"
%token<Code> RESTRICT   "restrict"
%token RETURN   "return"
%token<Code> SHORT   "short"
%token<Code> SIGNED   "signed"
%token SIZEOF   "sizeof"
%token<Code> STATIC   "static"
%token<Code> STRUCT   "struct"
%token SWITCH   "switch"
%token<Code> TYPEDEF   "typedef"
%token<Code> UNION   "union"
%token<Code> UNSIGNED   "unsigned"
%token<Code> VOID   "void"
%token<Code> VOLATILE   "volatile"
%token WHILE   "while"
%token<Code> _ALIGNAS   "_Alignas"
%token _ALIGNOF   "_Alignof"
%token<Code> _ATOMIC   "_Atomic"
%token<Code> _BOOL   "_Bool"
%token<Code> _COMPLEX   "_Complex"
%token _GENERIC   "_Generic"
%token _IMAGINARY   "_Imaginary"
%token<Code> _NORETURN   "_Noreturn"
%token<Code> _STATIC_ASSERT   "_Static_assert"
%token<Code> _THREAD_LOCAL   "_Thread_local"

// Set precedences to avoid IF-ELSE `shift'/reduce conflict
%precedence IF
%precedence ELSE

%type<Tree> identifier
%type<Tree> any-identifier any-identifier_opt
%type<Tree> string-literal
%type<Tree> constant
%type<Tree> function-definition
%type<Tree> declaration-list declaration-list_opt
%type<Tree> declaration
%type<Tree> declaration-specifiers-nots
%type<Tree> declaration-specifiers-ts
%type<Tree> declaration-specifiers-ts-rest
%type<Tree> specifier-nots
%type<Tree> init-declarator-list init-declarator-list_opt
%type<Tree> init-declarator-list-notyp init-declarator-list-notyp_opt
%type<Tree> init-declarator
%type<Tree> init-declarator-notyp
%type<Tree> storage-class-specifier
%type<Tree> type-specifier
%type<Tree> type-specifier-notyp
%type<Tree> struct-or-union-specifier
%type<Code> struct-or-union
%type<Tree> struct-declaration-list
%type<Tree> struct-declaration
%type<Tree> struct-declspecs-nots
%type<Tree> struct-declspecs-ts
%type<Tree> struct-declspecs-ts-rest
%type<Tree> specifier-qualifier-list
%type<Tree> specifier-or-qualifier
%type<Tree> struct-declarator-list struct-declarator-list_opt
%type<Tree> struct-declarator-list-notyp struct-declarator-list-notyp_opt
%type<Tree> struct-declarator struct-declarator-notyp
%type<Tree> enum-specifier
%type<Tree> enumerator-list
%type<Tree> enumerator
%type<Tree> enumeration-constant
%type<Tree> atomic-type-specifier
%type<Tree> type-qualifier
%type<Tree> function-specifier
%type<Tree> alignment-specifier
%type<Tree> declarator
%type<Tree> declarator_opt
%type<Tree> declarator-notyp declarator-notyp_opt
%type<Tree> direct-declarator
%type<Tree> direct-declarator-notyp
%type<Tree> direct-declarator-elem
%type<Tree> pointer pointer_opt
%type<Tree> type-qualifier-list type-qualifier-list_opt
%type<Tree> parameter-type-list parameter-type-list_opt
%type<Tree> parameter-list
%type<Tree> parameter-declaration
%type<Tree> identifier-list identifier-list_opt
%type<Tree> type-name
%type<Tree> abstract-declarator abstract-declarator_opt
%type<Tree> direct-abstract-declarator
%type<Tree> direct-abstract-declarator-elem
%type<Tree> initializer
%type<Tree> initializer-list
%type<Tree> designation designation_opt
%type<Tree> designator-list
%type<Tree> designator
%type<Tree> static_assert-declaration

%type<Tree> statement
%type<Tree> labeled-statement
%type<Tree> compound-statement
%type<Tree> block-item-list block-item-list_opt
%type<Tree> block-item
%type<Tree> expression-statement
%type<Tree> selection-statement
%type<Tree> iteration-statement
%type<Tree> jump-statement

%type<Tree> primary-expression
%type<Tree> generic-selection
%type<Tree> generic-assoc-list
%type<Tree> generic-association
%type<Tree> postfix-expression
%type<Tree> argument-expression-list
%type<Tree> argument-expression-list_opt
%type<Tree> unary-expression
%type<String> unary-operator
%type<Tree> cast-expression
%type<Tree> multiplicative-expression
%type<Tree> additive-expression
%type<Tree> shift-expression
%type<Tree> relational-expression
%type<Tree> equality-expression
%type<Tree> AND-expression
%type<Tree> exclusive-OR-expression
%type<Tree> inclusive-OR-expression
%type<Tree> logical-AND-expression
%type<Tree> logical-OR-expression
%type<Tree> conditional-expression
%type<Tree> assignment-expression assignment-expression_opt
%type<String> assignment-operator
%type<Tree> expression expression_opt
%type<Tree> constant-expression

%start translation-unit

%%

identifier:
    IDENT       { $$ = ident( $IDENT ); }
  ;

/*identifier_opt:
    %empty | identifier
  ;*/

any-identifier:
    TYPENAME    { $$ = ident( $TYPENAME ); }
  | IDENT       { $$ = ident( $IDENT ); }
  ;

any-identifier_opt:
    %empty              { $$ = null; }
  | any-identifier
  ;

string-literal:
    STRING_CONST                        { $$ = stringLiteral( $STRING_CONST ); }
  | string-literal STRING_CONST         { $$ = stringLiteral( $1, $STRING_CONST ); }
  ;

constant:
    INT_NUMBER                          { $$ = constant( $1 ); }
  | REAL_NUMBER                         { $$ = constant( $1 ); }
  | CHAR_CONST                          { $$ = constant( $1 ); }
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
    function-definition       { print( $[function-definition] ); }
  | declaration               { print( $[declaration] ); }
  ;

// (6.9.1)
function-definition:
    declaration-specifiers-nots declarator-notyp declaration-list_opt compound-statement
        { $$ = tree("function-definition", $1, $2, $3, $4); }
  | declaration-specifiers-ts   declarator       declaration-list_opt compound-statement
        { $$ = tree("function-definition", $1, $2, $3, $4); }
  ;

// (6.9.1)
declaration-list:
    declaration                         { $$ = tree("declaration-list", $1); }
  | declaration-list declaration        { $$ = treeAppend($1, $2); }
  ;

declaration-list_opt:
    %empty                              { $$ = null; }
  | declaration-list
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

declaration:
    static_assert-declaration
  | declaration-specifiers-nots init-declarator-list-notyp_opt ";" { $$ = tree("declaration", $1, $2); }
  | declaration-specifiers-ts   init-declarator-list_opt ";"       { $$ = tree("declaration", $1, $2); }
  ;

declaration-specifiers-nots:
    specifier-nots                              { $$ = tree( "declaration-specifiers", $1 ); }
  | specifier-nots declaration-specifiers-nots  { $$ = leftAppend( $1, $2 ); }
  ;

declaration-specifiers-ts:
    type-specifier declaration-specifiers-ts-rest  { $$ = leftAppend( $1, $2 ); }
  | specifier-nots declaration-specifiers-ts       { $$ = leftAppend( $1, $2 ); }
  ;

declaration-specifiers-ts-rest:
    %empty                                               { $$ = tree("declaration-specifiers"); }
  | type-specifier-notyp declaration-specifiers-ts-rest  { $$ = leftAppend( $1, $2 ); }
  | specifier-nots declaration-specifiers-ts-rest        { $$ = leftAppend( $1, $2 ); }
  ;

specifier-nots:
    storage-class-specifier
  | type-qualifier
  | function-specifier
  | alignment-specifier
  ;

// (6.7)
init-declarator-list:
    init-declarator                             { $$ = tree("init-declarator-list", $1); }
  | init-declarator-list "," init-declarator    { $$ = treeAppend($1, $3); }
  ;

init-declarator-list_opt:
    %empty { $$ = null; }
  | init-declarator-list
  ;

init-declarator-list-notyp:
    init-declarator-notyp                          { $$ = tree("init-declarator-list", $1); }
  | init-declarator-list-notyp "," init-declarator { $$ = treeAppend($1, $3); }
  ;

init-declarator-list-notyp_opt:
    %empty { $$ = null; }
  | init-declarator-list-notyp
  ;

// (6.7)
init-declarator:
    declarator                  { $$ = tree( "init-declarator", $declarator, null ); }
  | declarator "=" initializer  { $$ = tree( "init-declarator", $declarator, $initializer ); }
  ;

init-declarator-notyp:
    declarator-notyp                  { $$ = tree( "init-declarator", $[declarator-notyp], null ); }
  | declarator-notyp "=" initializer  { $$ = tree( "init-declarator", $[declarator-notyp], $initializer ); }
  ;

// (6.7.1)
storage-class-specifier:
    TYPEDEF                    { $$ = tree($1); }
  | EXTERN                     { $$ = tree($1); }
  | STATIC                     { $$ = tree($1); }
  | _THREAD_LOCAL              { $$ = tree($1); }
  | AUTO                       { $$ = tree($1); }
  | REGISTER                   { $$ = tree($1); }
  ;

// (6.7.2)
type-specifier:
    type-specifier-notyp
  | TYPENAME                    { $$ = ident($1); }
  ;

type-specifier-notyp:
    VOID                        { $$ = tree($1); }
  | CHAR                        { $$ = tree($1); }
  | SHORT                       { $$ = tree($1); }
  | INT                         { $$ = tree($1); }
  | LONG                        { $$ = tree($1); }
  | FLOAT                       { $$ = tree($1); }
  | DOUBLE                      { $$ = tree($1); }
  | SIGNED                      { $$ = tree($1); }
  | UNSIGNED                    { $$ = tree($1); }
  | _BOOL                       { $$ = tree($1); }
  | _COMPLEX                    { $$ = tree($1); }
  | atomic-type-specifier
  | struct-or-union-specifier
  | enum-specifier
  ;

// (6.7.2.1)
struct-or-union-specifier:
    struct-or-union any-identifier_opt "{" struct-declaration-list "}"
      { $$ = tree( $[struct-or-union], $[any-identifier_opt], $[struct-declaration-list] ); }
  | struct-or-union any-identifier
      { $$ = tree( $[struct-or-union], $[any-identifier], null ); }
  ;

// (6.7.2.1)
struct-or-union:
    STRUCT
  | UNION
  ;

// (6.7.2.1)
struct-declaration-list:
    struct-declaration                          { $$ = tree( "struct-declaration-list", $1 ); }
  | struct-declaration-list struct-declaration  { $$ = treeAppend( $1, $2 ); }
  ;

// (6.7.2.1)
struct-declaration:
    static_assert-declaration
  | struct-declspecs-nots struct-declarator-list-notyp_opt ";" { $$ = tree( "struct-declaration", $1, $2 ); }
  | struct-declspecs-ts   struct-declarator-list_opt ";"       { $$ = tree( "struct-declaration", $1, $2 ); }
  ;

struct-declspecs-nots:
    type-qualifier                          { $$ = tree( "struct-declspec", $1 ); }
  | type-qualifier struct-declspecs-nots    { $$ = leftAppend( $1, $2 ); }
  ;

struct-declspecs-ts:
    type-specifier struct-declspecs-ts-rest { $$ = leftAppend( $1, $2 ); }
  | type-qualifier struct-declspecs-ts      { $$ = leftAppend( $1, $2 ); }
  ;

struct-declspecs-ts-rest:
    %empty                                        { $$ = tree("struct-declspecs"); }
  | type-specifier-notyp struct-declspecs-ts-rest { $$ = leftAppend( $1, $2 ); }
  | type-qualifier struct-declspecs-ts-rest       { $$ = leftAppend( $1, $2 ); }
  ;

// (6.7.2.1)
specifier-qualifier-list:
    specifier-or-qualifier                              { $$ = tree("specifier-qualifier-list", $1); }
  | specifier-qualifier-list specifier-or-qualifier     { $$ = treeAppend($1,$2); }
  ;

specifier-or-qualifier:
    type-specifier
  | type-qualifier
  ;

// (6.7.2.1)
struct-declarator-list:
    struct-declarator                                   { $$ = tree("struct-declarator-list", $1); }
  | struct-declarator-list "," struct-declarator        { $$ = treeAppend($1,$3); }
  ;

struct-declarator-list_opt:
    %empty  { $$ = null; }
  | struct-declarator-list
  ;

struct-declarator-list-notyp:
    struct-declarator-notyp                             { $$ = tree("struct-declarator-list", $1); }
  | struct-declarator-list-notyp "," struct-declarator  { $$ = treeAppend($1,$3); }
  ;

struct-declarator-list-notyp_opt:
    %empty { $$ = null; }
  | struct-declarator-list-notyp
  ;

// (6.7.2.1)
struct-declarator:
    declarator                              { $$ = tree( "struct-declarator", $[declarator] ); }
  | declarator_opt ":" constant-expression  { $$ = tree( "bitfield-declarator", $[declarator_opt], $[constant-expression] ); }
  ;

struct-declarator-notyp:
    declarator-notyp                               { $$ = tree( "struct-declarator", $[declarator-notyp] ); }
  | declarator-notyp_opt ":" constant-expression   { $$ = tree( "bitfield-declarator", $[declarator-notyp_opt], $[constant-expression] ); }
  ;

// (6.7.2.2)
enum-specifier:
    ENUM any-identifier_opt "{" enumerator-list "}"     { $$ = tree( $ENUM, $[any-identifier_opt], $[enumerator-list] ); }
  | ENUM any-identifier_opt "{" enumerator-list "," "}" { $$ = tree( $ENUM, $[any-identifier_opt], $[enumerator-list] ); }
  | ENUM any-identifier                                 { $$ = tree( $ENUM, $[any-identifier],     null ); }
  ;

// (6.7.2.2)
enumerator-list:
    enumerator                       { $$ = tree( "enumerator-list", $1 ); }
  | enumerator-list "," enumerator   { $$ = treeAppend( $1, $3 ); }
  ;

// (6.7.2.2)
enumerator:
    enumeration-constant                          { $$ = tree( "enumerator", $[enumeration-constant], null ); }
  | enumeration-constant "=" constant-expression  { $$ = tree( "enumerator", $[enumeration-constant], $[constant-expression] ); }
  ;

enumeration-constant:
    any-identifier    { $$ = tree( "enumeration-constant", $[any-identifier] ); }
  ;

// (6.7.2.4)
atomic-type-specifier:
    _ATOMIC "(" type-name ")"	{ $$ = tree( $_ATOMIC, $[type-name] ); }
  ;

// (6.7.3)
type-qualifier:
    CONST       { $$ = tree( $1 ); }
  | RESTRICT    { $$ = tree( $1 ); }
  | VOLATILE    { $$ = tree( $1 ); }
  | _ATOMIC     { $$ = tree( $1 ); }
  ;

// (6.7.4)
function-specifier:
    INLINE      { $$ = tree( $1 ); }
  | _NORETURN   { $$ = tree( $1 ); }
  ;

// (6.7.5)
alignment-specifier:
    _ALIGNAS "(" type-name ")"		 { $$ = tree( $_ALIGNAS, $[type-name] ); }
  | _ALIGNAS "(" constant-expression ")" { $$ = tree( $_ALIGNAS, $[constant-expression] ); }
  ;

// (6.7.6)
declarator:
    pointer_opt direct-declarator       { $$ = tree( "declarator", $pointer_opt, $[direct-declarator] ); }
  ;

declarator_opt:
    %empty      { $$ = null; }
  | declarator
  ;

declarator-notyp:
    pointer direct-declarator           { $$ = tree( "declarator", $pointer, $[direct-declarator] ); }
  | direct-declarator-notyp             { $$ = tree( "declarator", null, $[direct-declarator-notyp] ); }
  ;

declarator-notyp_opt:
    %empty      { $$ = null; }
  | declarator-notyp
  ;

// (6.7.6)
direct-declarator:
    any-identifier                              { $$ = tree( "direct-declarator", $[any-identifier] ); }
  | "(" declarator ")"                          { $$ = tree( "direct-declarator", $declarator ); }
  | direct-declarator direct-declarator-elem    { $$ = treeAppend( $1, $2 ); }
  ;

direct-declarator-notyp:
    identifier                                  { $$ = tree( "direct-declarator", $[identifier] ); }
  | "(" declarator ")"                          { $$ = tree( "direct-declarator", $declarator ); }
  | direct-declarator-notyp direct-declarator-elem  { $$ = treeAppend( $1, $2 ); }
  ;

direct-declarator-elem:
    "[" type-qualifier-list_opt assignment-expression_opt "]"    { $$ = tree( "array-decl", null, $[type-qualifier-list_opt], $[assignment-expression_opt] ); }
  | "[" STATIC type-qualifier-list_opt assignment-expression "]" { $$ = tree( "array-decl", tree($STATIC), $[type-qualifier-list_opt], $[assignment-expression] ); }
  | "[" type-qualifier-list STATIC assignment-expression "]"     { $$ = tree( "array-decl", tree($STATIC), $[type-qualifier-list], $[assignment-expression] ); }
  | "[" type-qualifier-list_opt ASTERISK "]"                     { $$ = tree( "array-decl", null, $[type-qualifier-list_opt], tree($ASTERISK) ); }
  | "(" parameter-type-list ")"                                  { $$ = tree( "func-declarator", $[parameter-type-list] ); }
  | "(" identifier-list_opt ")"                                  { $$ = tree( "old-func-declarator", $[identifier-list_opt] ); }
  ;

// (6.7.6)
pointer:
    "*" type-qualifier-list_opt         { $$ = tree( "pointer", $2, null ); }
  | "*" type-qualifier-list_opt pointer { $$ = tree( "pointer", $2, $3 ); }
  ;

pointer_opt:
    %empty      { $$ = null; }
  | pointer
  ;

// (6.7.6)
type-qualifier-list:
    type-qualifier                      { $$ = tree( "type-qualifier-list", $1 ); }
  | type-qualifier-list type-qualifier  { $$ = treeAppend( $1, $2 ); }
  ;

type-qualifier-list_opt:
    %empty              { $$ = null; }
  | type-qualifier-list
  ;

// (6.7.6)
parameter-type-list:
    parameter-list
  | parameter-list "," "..."            { $$ = treeAppend( $1, tree($3) ); }
  ;

parameter-type-list_opt:
    %empty { $$ = null; }
  | parameter-type-list
  ;

// (6.7.6)
parameter-list:
    parameter-declaration                       { $$ = tree("parameter-list", $1); }
  | parameter-list "," parameter-declaration    { $$ = treeAppend( $1, $3); }
  ;

// (6.7.6)
parameter-declaration:
    declaration-specifiers-nots                                     { $$ = tree("parameter-declaration",$1,null,null); }
  | declaration-specifiers-ts                                       { $$ = tree("parameter-declaration",$1,null,null); }
  | declaration-specifiers-nots pointer                             { $$ = tree("parameter-declaration",$1,$2,null); }
  | declaration-specifiers-ts   pointer                             { $$ = tree("parameter-declaration",$1,$2,null); }
  | declaration-specifiers-nots pointer direct-declarator           { $$ = tree("parameter-declaration",$1,$2,$3); }
  | declaration-specifiers-ts   pointer direct-declarator           { $$ = tree("parameter-declaration",$1,$2,$3); }
  | declaration-specifiers-nots         direct-declarator-notyp     { $$ = tree("parameter-declaration",$1,null,$2); }
  | declaration-specifiers-ts           direct-declarator           { $$ = tree("parameter-declaration",$1,null,$2); }
  | declaration-specifiers-nots pointer direct-abstract-declarator  { $$ = tree("parameter-declaration",$1,$2,$3); }
  | declaration-specifiers-ts   pointer direct-abstract-declarator  { $$ = tree("parameter-declaration",$1,$2,$3); }
  | declaration-specifiers-nots         direct-abstract-declarator  { $$ = tree("parameter-declaration",$1,null,$2); }
  | declaration-specifiers-ts           direct-abstract-declarator  { $$ = tree("parameter-declaration",$1,null,$2); }
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
    identifier                          { $$ = tree("identifier-list", $1 ); }
  | identifier-list "," any-identifier  { $$ = treeAppend( $1, $3 ); }
  ;

identifier-list_opt:
    %empty              { $$ = null; }
  | identifier-list
  ;

// (6.7.7)
type-name:
    specifier-qualifier-list abstract-declarator_opt    { $$ = tree("type-name", $1, $2); }
  ;
  
// (6.7.7)
abstract-declarator:
    pointer                                     { $$ = tree( "abstract-declarator", $pointer, null ); }
  | pointer_opt direct-abstract-declarator      { $$ = tree( "abstract-declarator", $pointer_opt, $[direct-abstract-declarator] ); }
  ;

abstract-declarator_opt:
    %empty      { $$ = null; }
  | abstract-declarator
  ;

// (6.7.7)
direct-abstract-declarator:
    "(" abstract-declarator ")"                 { $$ = tree( "direct-abstract-declarator", $[abstract-declarator] ); }
  | direct-abstract-declarator-elem             { $$ = tree( "direct-abstract-declarator", $[direct-abstract-declarator-elem] ); }
  | direct-abstract-declarator direct-abstract-declarator-elem { $$ = treeAppend( $1, $2 ); }
  ;

direct-abstract-declarator-elem:
    "[" type-qualifier-list assignment-expression_opt "]"        { $$ = tree( "array-decl", null, $[type-qualifier-list], $[assignment-expression_opt] ); }
  | "[" assignment-expression_opt "]"                            { $$ = tree( "array-decl", null, null, $[assignment-expression_opt] ); }
  | "[" STATIC type-qualifier-list_opt assignment-expression "]" { $$ = tree( "array-decl", tree($STATIC), $[type-qualifier-list_opt], $[assignment-expression] ); }
  | "[" type-qualifier-list STATIC assignment-expression "]"     { $$ = tree( "array-decl", tree($STATIC), $[type-qualifier-list], $[assignment-expression] ); }
  | "[" ASTERISK "]"                                             { $$ = tree( "array-decl", null, null, tree($ASTERISK) ); }
  | "(" parameter-type-list_opt ")"                              { $$ = tree( "func-declarator", $[parameter-type-list_opt] ); }
  ;

// (6.7.9)
initializer:
    assignment-expression               { $$ = tree("initializer",$1); }
  | "{" initializer-list "}"            { $$ = tree("compound-initializer",$2); }
  | "{" initializer-list "," "}"        { $$ = tree("compound-initializer",$2); }
  ;

// (6.7.9)
initializer-list:
    designation_opt initializer         { $$ = tree("initializer-list", tree("initializer-elem",$designation_opt,$initializer)); }
  | initializer-list "," designation_opt initializer { $$ = treeAppend($1, tree("initializer-elem",$designation_opt,$initializer)); }
  ;

// (6.7.9)
designation:
    designator-list "="                 { $$ = tree("designation",$1); }
  ;

designation_opt:
    %empty { $$ = null; }
  | designation
  ;
  
// (6.7.9)
designator-list:
    designator                          { $$ = tree("designator-list",$1); }
  | designator-list designator          { $$ = treeAppend($1,$2); }
  ;

// (6.7.9)
designator:
    "[" constant-expression "]"         { $$ = tree("designator-index",$[constant-expression]); }
  | "." any-identifier                  { $$ = tree("designator-member",$[any-identifier]); }
// GNU C extension
  | "[" constant-expression[ce1] "..." constant-expression[ce2] "]" { $$ = tree("designator-range",$ce1,$ce2); }
  ;

// (6.7.10)
static_assert-declaration:
    _STATIC_ASSERT "(" constant-expression "," string-literal ")" ";"
        { $$ = tree( $_STATIC_ASSERT, $[constant-expression], $[string-literal] ); }
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
    any-identifier ":" statement                { $$ = tree("label", $statement); }
  | CASE constant-expression ":" statement      { $$ = tree("case", $[constant-expression], $statement); }
  | DEFAULT ":" statement                       { $$ = tree("default", null, $statement); }
// GNU C Extension
  | CASE constant-expression[ce1] "..." constant-expression[ce2] ":" statement { $$ = tree("case-range", $ce1, $ce2, $statement); }
  ;

// (6.8.2)
compound-statement:
    "{" block-item-list_opt "}" { $$ = tree("compound-statement",$[block-item-list_opt]); }
  ;

// (6.8.2)
block-item-list:
    block-item                  { $$ = tree("block-item-list",$1); }
  | block-item-list block-item  { $$ = treeAppend($1,$2); }
  ;

block-item-list_opt:
    %empty { $$ = null; }
  | block-item-list
  ;

// (6.8.2)
block-item:
    declaration { $$ = tree("declaration-statement",$1); }
  | statement
  ;

// (6.8.3)
expression-statement:
    expression_opt ";" { $$ = tree("expression-statement",$1); }
  ;

// (6.8.4)
selection-statement:
    IF "(" expression ")" statement                         %prec IF   { $$ = tree("if",$expression,$statement,null); }
  | IF "(" expression ")" statement[s1] ELSE statement[s2]  %prec ELSE { $$ = tree("if",$expression,$s1,$s2); }
  | SWITCH "(" expression ")" statement                                { $$ = tree("switch",$expression,$statement); }
  ;

// (6.8.5)
iteration-statement:
    WHILE "(" expression ")" statement           { $$ = tree("while",$expression,$statement); }
  | DO statement WHILE "(" expression ")" ";"    { $$ = tree("do",$statement,$expression); }
  | FOR "(" expression_opt[e1] ";" expression_opt[e2] ";" expression_opt[e3] ")" statement { $$ = tree("for",$e1,$e2,$e3); }
  | FOR "(" declaration[dcl] expression_opt[e2] ";" expression_opt[e3] ")" statement       { $$ = tree("for",$dcl,$e2,$e3); }
  ;

// (6.8.6)
jump-statement:
    GOTO any-identifier ";"   { $$ = tree("goto",$[any-identifier]); }
  | CONTINUE ";"              { $$ = tree("continue"); }
  | BREAK ";"                 { $$ = tree("break"); }
  | RETURN expression_opt ";" { $$ = tree("return", $expression_opt); }
  ;

// A.2.1 Expressions

// (6.5.1)
primary-expression:
    identifier
  | constant
  | string-literal
  | "(" expression ")"  { $$ = $expression; }
  | generic-selection
  ;

// (6.5.1.1)
generic-selection:
    _GENERIC "(" assignment-expression "," generic-assoc-list ")" { $$ = tree("_Generic",$[assignment-expression],$[generic-assoc-list]); }
  ;

// (6.5.1.1)
generic-assoc-list:
    generic-association                         { $$ = tree("generic-assoc-list",$1); }
  | generic-assoc-list "," generic-association  { $$ = treeAppend($1,$3); }
  ;

// (6.5.1.1)
generic-association:
    type-name ":" assignment-expression         { $$ = tree("generic-type-name-assoc",$[type-name],$[assignment-expression]); }
  | DEFAULT ":" assignment-expression           { $$ = tree("generic-default-assoc",null,$[assignment-expression]); }
  ;

// (6.5.2)
postfix-expression:
    primary-expression
  | postfix-expression "[" expression "]"                    { $$ = tree("subscript",$1,$expression); }
  | postfix-expression "(" argument-expression-list_opt ")"  { $$ = tree("call",$1,$[argument-expression-list_opt]); }
  | postfix-expression "." any-identifier                    { $$ = tree("select",$1,$[any-identifier]); }
  | postfix-expression "->" any-identifier                   { $$ = tree("ptr-select",$1,$[any-identifier]); }
  | postfix-expression "++"                                  { $$ = tree("post-inc",$1); }
  | postfix-expression "--"                                  { $$ = tree("post-dec",$1); }
  | "(" type-name ")" "{" initializer-list "}"               { $$ = tree("compound-literal", $[type-name], $[initializer-list]); }
  | "(" type-name ")" "{" initializer-list "," "}"           { $$ = tree("compound-literal", $[type-name], $[initializer-list]); }
  ;

// (6.5.2)
argument-expression-list:
    assignment-expression                               { $$ = tree("argument-expression-list",$1); }
  | argument-expression-list "," assignment-expression  { $$ = treeAppend($1,$3); }
  ;

argument-expression-list_opt:
    %empty { $$ = null; }
  | argument-expression-list
  ;

// (6.5.3)
unary-expression:
    postfix-expression
  | "++" unary-expression               { $$ = tree("pre-inc", $2); }
  | "--" unary-expression               { $$ = tree("pre-dec", $2); }
  | unary-operator cast-expression      { $$ = tree($[unary-operator], $[cast-expression]); }
  | SIZEOF unary-expression             { $$ = tree("sizeof-expr",$2); }
  | SIZEOF "(" type-name ")"            { $$ = tree("sizeof-type",$[type-name]); }
  | _ALIGNOF "(" type-name ")"          { $$ = tree("_Alignof",$[type-name]); }
// GNU C extension
  | "&&" any-identifier                 { $$ = tree("address-label",$[any-identifier]); }
  ;

// (6.5.3)
unary-operator:
    "&" { $$ = "address-of"; }
  | "*" { $$ = "deref"; }
  | "+" { $$ = "u-plus"; }
  | "-" { $$ = "u-minus"; }
  | "~" { $$ = "binary-not"; }
  | "!" { $$ = "logical-not"; }
  ;

// (6.5.4)
cast-expression:
    unary-expression
  | "(" type-name ")" cast-expression   { $$ = tree("cast",$[type-name],$4); }
  ;

// (6.5.5)
multiplicative-expression:
    cast-expression
  | multiplicative-expression "*" cast-expression  { $$ = tree("mul",$1,$3); }
  | multiplicative-expression "/" cast-expression  { $$ = tree("div",$1,$3); }
  | multiplicative-expression "%" cast-expression  { $$ = tree("rem",$1,$3); }
  ;

// (6.5.6)
additive-expression:
    multiplicative-expression
  | additive-expression "+" multiplicative-expression { $$ = tree("add",$1,$3); }
  | additive-expression "-" multiplicative-expression { $$ = tree("sub",$1,$3); }
  ;

// (6.5.7)
shift-expression:
    additive-expression
  | shift-expression "<<" additive-expression        { $$ = tree("shl",$1,$3); }
  | shift-expression ">>" additive-expression        { $$ = tree("shr",$1,$3); }
  ;

// (6.5.8)
relational-expression:
    shift-expression
  | relational-expression "<" shift-expression       { $$ = tree("lt",$1,$3); }
  | relational-expression ">" shift-expression       { $$ = tree("gt",$1,$3); }
  | relational-expression "<=" shift-expression      { $$ = tree("le",$1,$3); }
  | relational-expression ">=" shift-expression      { $$ = tree("ge",$1,$3); }
  ;

// (6.5.9)
equality-expression:
    relational-expression
  | equality-expression "==" relational-expression   { $$ = tree("eq",$1,$3); }
  | equality-expression "!=" relational-expression   { $$ = tree("ne",$1,$3); }
  ;

// (6.5.10)
AND-expression:
    equality-expression
  | AND-expression "&" equality-expression           { $$ = tree("binary-and",$1,$3); }
  ;

// (6.5.11)
exclusive-OR-expression:
    AND-expression
  | exclusive-OR-expression "^" AND-expression       { $$ = tree("binary-xor",$1,$3); }
  ;

// (6.5.12)
inclusive-OR-expression:
    exclusive-OR-expression
  | inclusive-OR-expression "|" exclusive-OR-expression { $$ = tree("binary-or",$1,$3); }
  ;

// (6.5.13)
logical-AND-expression:
    inclusive-OR-expression
  | logical-AND-expression "&&" inclusive-OR-expression  { $$ = tree("logical-and",$1,$3); }
  ;

// (6.5.14)
logical-OR-expression:
    logical-AND-expression
  | logical-OR-expression "||" logical-AND-expression    { $$ = tree("logical-or",$1,$3); }
  ;

// (6.5.15)
conditional-expression:
    logical-OR-expression
  | logical-OR-expression[e1] "?" expression[e2] ":" conditional-expression[e3] { $$ = tree("conditional",$e1,$e2,$e3); }
  ;

// (6.5.16)
assignment-expression:
    conditional-expression
  | unary-expression assignment-operator assignment-expression  { $$ = tree($[assignment-operator],$1,$3); }
  ;

assignment-expression_opt:
    %empty { $$ = null; }
  | assignment-expression
  ;

// (6.5.16)
assignment-operator:
    "="   { $$ = "assign"; }
  | "*="  { $$ = "assign-mul"; }
  | "/="  { $$ = "assign-div"; }
  | "%="  { $$ = "assign-rem"; }
  | "+="  { $$ = "assign-add"; }
  | "-="  { $$ = "assign-sub"; }
  | "<<=" { $$ = "assign-shl"; }
  | ">>=" { $$ = "assign-shr"; }
  | "&="  { $$ = "assign-binary-and"; }
  | "^="  { $$ = "assign-binary-xor"; }
  | "|="  { $$ = "assign-binary-or"; }
  ;

// (6.5.17)
expression:
    assignment-expression
  | expression "," assignment-expression        { $$ = tree("comma",$1,$3); }
  ;

expression_opt:
    %empty { $$ = null; }
  | expression
  ;

// (6.6)
constant-expression:
    conditional-expression
  ;
