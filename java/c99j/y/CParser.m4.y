%language "Java"

%define package "c99.parser"
%code imports {
import c99.Constant;
import c99.IErrorReporter;
import c99.parser.ast.Ast;
}
%define public
%define parser_class_name {CParser}
%define extends {ParserActions}

%parse-param { IErrorReporter reporter_ }
%parse-param { SymTable symTab_ }
%code init { super.init( reporter_, symTab_ ); pushScope(); }

%define parse.error verbose

%locations

%destructor { popScope( (Scope)$$ ); } <Scope>

%token<Symbol> IDENT  "identifier"
%token<Decl> TYPENAME "typedef name"
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

%type<Ast> identifier
%type<Ast> any-identifier any-identifier_opt
%type<Ast> string-literal
%type<Ast> constant
%type<Ast> declaration-list declaration-list_opt
%type<Ast> declaration
%type<Ast> declaration-specifiers-nots
%type<Ast> declaration-specifiers-ts
%type<Ast> declaration-specifiers-ts-rest
%type<Ast> specifier-nots
%type<Ast> storage-class-specifier
%type<Ast> type-specifier
%type<Ast> type-specifier-notyp
%type<Ast> struct-or-union-specifier
%type<Code> struct-or-union
%type<Ast> struct-declaration-list
%type<Ast> struct-declaration
%type<Ast> specifier-qualifier-list
%type<Ast> specifier-or-qualifier
%type<Ast> enum-specifier
%type<Ast> enumerator-list
%type<Ast> enumerator
%type<Ast> enumeration-constant
%type<Ast> atomic-type-specifier
%type<Ast> type-qualifier
%type<Ast> function-specifier
%type<Ast> alignment-specifier
%type<Ast> declarator
%type<Ast> declarator_opt
%type<Ast> declarator-notyp declarator-notyp_opt
%type<Ast> direct-declarator
%type<Ast> direct-declarator-notyp
%type<Ast> direct-declarator-elem
%type<Ast> pointer pointer_opt
%type<Ast> type-qualifier-list type-qualifier-list_opt
%type<Ast> parameter-type-list
%type<Ast> parameter-list
%type<Ast> identifier-list identifier-list_opt
%type<Ast> type-name
%type<Ast> abstract-declarator abstract-declarator_opt
%type<Ast> direct-abstract-declarator-elem
%type<Ast> initializer
%type<Ast> initializer-list
%type<Ast> designation designation_opt
%type<Ast> designator-list
%type<Ast> designator
%type<Ast> static_assert-declaration

%type<Ast> statement
%type<Ast> labeled-statement
%type<Ast> compound-statement
%type<Ast> block-item-list block-item-list_opt
%type<Ast> block-item
%type<Ast> expression-statement
%type<Ast> selection-statement
%type<Ast> iteration-statement
%type<Ast> jump-statement

%type<Ast> primary-expression
%type<Ast> generic-selection
%type<Ast> generic-assoc-list
%type<Ast> generic-association
%type<Ast> postfix-expression
%type<Ast> argument-expression-list
%type<Ast> argument-expression-list_opt
%type<Ast> unary-expression
%type<String> unary-operator
%type<Ast> cast-expression
%type<Ast> multiplicative-expression
%type<Ast> additive-expression
%type<Ast> shift-expression
%type<Ast> relational-expression
%type<Ast> equality-expression
%type<Ast> AND-expression
%type<Ast> exclusive-OR-expression
%type<Ast> inclusive-OR-expression
%type<Ast> logical-AND-expression
%type<Ast> logical-OR-expression
%type<Ast> conditional-expression
%type<Ast> assignment-expression assignment-expression_opt
%type<String> assignment-operator
%type<Ast> expression expression_opt
%type<Ast> constant-expression

%expect 3
%start translation-unit

start_grammar
%%

identifier:
    IDENT       { $$ = ident( $IDENT, @IDENT ); }
  ;

/*identifier_opt:
    %empty | identifier
  ;*/

any-identifier:
    TYPENAME    { $$ = ident( $TYPENAME.symbol, @TYPENAME ); }
  | IDENT       { $$ = ident( $IDENT, @IDENT ); }
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
    INT_NUMBER                          { $$ = constant($1,@1); }
  | REAL_NUMBER                         { $$ = constant($1,@1); }
  | CHAR_CONST                          { $$ = constant($1,@1); }
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
rule(<Ast>,function-definition):
    specified-declarator declaration-list_opt {} compound-statement
        { $$ = ast("function-definition", $[specified-declarator], $[declaration-list_opt], $[compound-statement]); }
  ;

rule(<Ast>,specified-declarator):
    declaration-specifiers-nots declarator-notyp  { $$ = declare($[declarator-notyp], $[declaration-specifiers-nots]); }
  | declaration-specifiers-ts   declarator        { $$ = declare($[declarator],       $[declaration-specifiers-ts]); }
  ;

// (6.9.1)
declaration-list:
    declaration                         { $$ = ast("declaration-list", $1); }
  | declaration-list declaration        { $$ = astAppend($1, $2); }
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
  | declaration-specifiers-nots init-declarator-list-notyp_opt ";" { $$ = ast("declaration", $1, $2); }
  | declaration-specifiers-ts   init-declarator-list_opt ";"       { $$ = ast("declaration", $1, $2); }
  ;

declaration-specifiers-nots:
    specifier-nots                              { $$ = ast( "declaration-specifiers", $1 ); }
  | specifier-nots declaration-specifiers-nots  { $$ = leftAppend( $1, $2 ); }
  ;

declaration-specifiers-ts:
    type-specifier declaration-specifiers-ts-rest  { $$ = leftAppend( $1, $2 ); }
  | specifier-nots declaration-specifiers-ts       { $$ = leftAppend( $1, $2 ); }
  ;

declaration-specifiers-ts-rest:
    %empty                                               { $$ = ast("declaration-specifiers"); }
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
rule(<Ast>,init-declarator-list,opt,declaration-specifiers):
    init-declarator                                 { $$ = ast("init-declarator-list", $[init-declarator]); }
  | init-declarator-list "," _PUSH0 init-declarator  { $$ = astAppend($1, $[init-declarator]); }
  ;

rule(<Ast>,init-declarator-list-notyp,opt,declaration-specifiers):
    init-declarator-notyp                                { $$ = ast("init-declarator-list", $[init-declarator-notyp]); }
  | init-declarator-list-notyp "," _PUSH0 init-declarator { $$ = astAppend($1, $[init-declarator]); }
  ;

// (6.7)
rule(<Ast>,init-declarator,,declaration-specifiers):
    declarator                  { $$ = ast("init-declarator", declare($declarator,$<Ast>0), null); }
  | declarator "=" initializer  { $$ = ast("init-declarator", declare($declarator,$<Ast>0), $initializer); }
  ;

rule(<Ast>,init-declarator-notyp,,declaration-specifiers):
    declarator-notyp                  { $$ = ast( "init-declarator", $[declarator-notyp], null ); }
  | declarator-notyp "=" initializer  { $$ = ast( "init-declarator", $[declarator-notyp], $initializer ); }
  ;

// (6.7.1)
storage-class-specifier:
    TYPEDEF                    { $$ = ast($1,@1); }
  | EXTERN                     { $$ = ast($1,@1); }
  | STATIC                     { $$ = ast($1,@1); }
  | _THREAD_LOCAL              { $$ = ast($1,@1); }
  | AUTO                       { $$ = ast($1,@1); }
  | REGISTER                   { $$ = ast($1,@1); }
  ;

// (6.7.2)
type-specifier:
    type-specifier-notyp
  | TYPENAME                    { $$ = ident($TYPENAME.symbol,@TYPENAME); }
  ;

type-specifier-notyp:
    VOID                        { $$ = ast($1,@1); }
  | CHAR                        { $$ = ast($1,@1); }
  | SHORT                       { $$ = ast($1,@1); }
  | INT                         { $$ = ast($1,@1); }
  | LONG                        { $$ = ast($1,@1); }
  | FLOAT                       { $$ = ast($1,@1); }
  | DOUBLE                      { $$ = ast($1,@1); }
  | SIGNED                      { $$ = ast($1,@1); }
  | UNSIGNED                    { $$ = ast($1,@1); }
  | _BOOL                       { $$ = ast($1,@1); }
  | _COMPLEX                    { $$ = ast($1,@1); }
  | atomic-type-specifier
  | struct-or-union-specifier
  | enum-specifier
  ;

// (6.7.2.1)
struct-or-union-specifier:
    struct-or-union any-identifier_opt "{" PushScope struct-declaration-list "}"
      { $$ = ast( $[struct-or-union], $[any-identifier_opt], $[struct-declaration-list] ); popScope($PushScope); }
  | struct-or-union any-identifier
      { $$ = ast( $[struct-or-union], $[any-identifier], null ); }
  ;

// (6.7.2.1)
struct-or-union:
    STRUCT
  | UNION
  ;

// (6.7.2.1)
struct-declaration-list:
    struct-declaration                          { $$ = ast( "struct-declaration-list", $1 ); }
  | struct-declaration-list struct-declaration  { $$ = astAppend( $1, $2 ); }
  ;

// (6.7.2.1)
struct-declaration:
    static_assert-declaration
  | declaration-specifiers-nots struct-declarator-list-notyp_opt ";" { $$ = ast( "struct-declaration", $1, $2 ); }
  | declaration-specifiers-ts   struct-declarator-list_opt ";"       { $$ = ast( "struct-declaration", $1, $2 ); }
  ;

// (6.7.2.1)
specifier-qualifier-list:
    specifier-or-qualifier                              { $$ = ast("specifier-qualifier-list", $1); }
  | specifier-qualifier-list specifier-or-qualifier     { $$ = astAppend($1,$2); }
  ;

specifier-or-qualifier:
    type-specifier
  | type-qualifier
  ;

// (6.7.2.1)
rule(<Ast>,struct-declarator-list,opt):
    struct-declarator                                   { $$ = ast("struct-declarator-list", $[struct-declarator]); }
  | struct-declarator-list "," _PUSH0 struct-declarator { $$ = astAppend($1,$[struct-declarator]); }
  ;

rule(<Ast>,struct-declarator-list-notyp,opt):
    struct-declarator-notyp                                    { $$ = ast("struct-declarator-list", $[struct-declarator-notyp]); }
  | struct-declarator-list-notyp "," _PUSH0 struct-declarator  { $$ = astAppend($1,$[struct-declarator]); }
  ;

// (6.7.2.1)
rule(<Ast>,struct-declarator):
    declarator                              { $$ = ast( "struct-declarator", declare($[declarator],$<Ast>0) ); }
  | declarator_opt ":" constant-expression  { $$ = ast( "bitfield-declarator", declare($[declarator_opt],$<Ast>0), $[constant-expression] ); }
  ;

rule(<Ast>,struct-declarator-notyp):
    declarator-notyp                               { $$ = ast( "struct-declarator", declare($[declarator-notyp],$<Ast>0) ); }
  | declarator-notyp_opt ":" constant-expression   { $$ = ast( "bitfield-declarator", declare($[declarator-notyp_opt],$<Ast>0), $[constant-expression] ); }
  ;

// (6.7.2.2)
enum-specifier:
    ENUM any-identifier_opt "{" enumerator-list "}"     { $$ = ast( $ENUM, $[any-identifier_opt], $[enumerator-list] ); }
  | ENUM any-identifier_opt "{" enumerator-list "," "}" { $$ = ast( $ENUM, $[any-identifier_opt], $[enumerator-list] ); }
  | ENUM any-identifier                                 { $$ = ast( $ENUM, $[any-identifier],     null ); }
  ;

// (6.7.2.2)
enumerator-list:
    enumerator                       { $$ = ast( "enumerator-list", $1 ); }
  | enumerator-list "," enumerator   { $$ = astAppend( $1, $3 ); }
  ;

// (6.7.2.2)
enumerator:
    enumeration-constant                          { $$ = ast( "enumerator", $[enumeration-constant], null ); }
  | enumeration-constant "=" constant-expression  { $$ = ast( "enumerator", $[enumeration-constant], $[constant-expression] ); }
  ;

enumeration-constant:
    any-identifier    { $$ = ast( "enumeration-constant", $[any-identifier] ); }
  ;

// (6.7.2.4)
atomic-type-specifier:
    _ATOMIC "(" type-name ")"	{ $$ = ast( $_ATOMIC, $[type-name] ); }
  ;

// (6.7.3)
type-qualifier:
    CONST       { $$ = ast( $1 ); }
  | RESTRICT    { $$ = ast( $1 ); }
  | VOLATILE    { $$ = ast( $1 ); }
  | _ATOMIC     { $$ = ast( $1 ); }
  ;

// (6.7.4)
function-specifier:
    INLINE      { $$ = ast( $1 ); }
  | _NORETURN   { $$ = ast( $1 ); }
  ;

// (6.7.5)
alignment-specifier:
    _ALIGNAS "(" type-name ")"		 { $$ = ast( $_ALIGNAS, $[type-name] ); }
  | _ALIGNAS "(" constant-expression ")" { $$ = ast( $_ALIGNAS, $[constant-expression] ); }
  ;

// (6.7.6)
declarator:
    pointer_opt direct-declarator       { $$ = seqAppend($[direct-declarator],$pointer_opt); }
  ;

declarator_opt:
    %empty      { $$ = null; }
  | declarator
  ;

declarator-notyp:
    pointer direct-declarator           { $$ = seqAppend($[direct-declarator],$pointer); }
  | direct-declarator-notyp             { $$ = $[direct-declarator-notyp]; }
  ;

declarator-notyp_opt:
    %empty      { $$ = null; }
  | declarator-notyp
  ;

// (6.7.6)
direct-declarator:
    any-identifier                              { $$ = ast( "direct-declarator", $[any-identifier], null ); }
  | "(" declarator ")"                          { $$ = $declarator; }
  | direct-declarator direct-declarator-elem    { $$ = seqAppend($1,$[direct-declarator-elem]); }
  ;

direct-declarator-notyp:
    identifier                                      { $$ = ast( "direct-declarator", $[identifier], null ); }
  | "(" declarator ")"                              { $$ = $declarator; }
  | direct-declarator-notyp direct-declarator-elem  { $$ = seqAppend($1,$[direct-declarator-elem]); }
  ;

direct-declarator-elem:
    "[" type-qualifier-list_opt assignment-expression_opt "]"
        { $$ = ast("qual",$[type-qualifier-list_opt],ast("array",null,$[assignment-expression_opt],null)); }
  | "[" STATIC type-qualifier-list_opt assignment-expression "]"
        { $$ = ast("qual",$[type-qualifier-list_opt],ast("array",ast($STATIC),$[assignment-expression],null)); }
  | "[" type-qualifier-list STATIC assignment-expression "]"
        { $$ = ast("qual",$[type-qualifier-list],ast("array",ast($STATIC),$[assignment-expression],null)); }
  | "[" type-qualifier-list_opt ASTERISK "]"
        { $$ = ast("qual",$[type-qualifier-list_opt],ast("array",null,ast($ASTERISK),null)); }
  | "(" PushScope parameter-type-list ")"
        { $$ = ast("qual",null,ast("func",$[parameter-type-list],null)); popScope($PushScope); }
  | "(" identifier-list_opt ")"
        { $$ = ast("qual",null,ast("old-func",$[identifier-list_opt],null)); }
  ;

// (6.7.6)
pointer:
                  "*" type-qualifier-list_opt  { $$ = ast("qual", $[type-qualifier-list_opt], ast("pointer",null)); }
  | pointer[left] "*" type-qualifier-list_opt  { $$ = ast("qual", $[type-qualifier-list_opt], ast("pointer",$left)); }
  ;

pointer_opt:
    %empty      { $$ = null; }
  | pointer
  ;

// (6.7.6)
type-qualifier-list:
    type-qualifier                      { $$ = ast( "qual-list", $1 ); }
  | type-qualifier-list type-qualifier  { $$ = astAppend( $1, $2 ); }
  ;

type-qualifier-list_opt:
    %empty              { $$ = null; }
  | type-qualifier-list
  ;

// (6.7.6)
parameter-type-list:
    parameter-list
  | parameter-list "," "..."            { $$ = astAppend( $1, ast($3) ); }
  ;

// (6.7.6)
parameter-list:
    parameter-declaration                       { $$ = ast("parameter-list", $1); }
  | parameter-list "," parameter-declaration    { $$ = astAppend( $1, $3); }
  ;

// (6.7.6)
rule(<Ast>,parameter-declaration):
    declaration-specifiers-nots pointer direct-declarator
        { $$ = ast("param-decl",declare(seqAppend($[direct-declarator],$pointer),$[declaration-specifiers-nots])); }
  | declaration-specifiers-ts   pointer direct-declarator
        { $$ = ast("param-decl",declare(seqAppend($[direct-declarator],$pointer),$[declaration-specifiers-ts])); }
  | declaration-specifiers-nots         direct-declarator-notyp
        { $$ = ast("param-decl",declare($[direct-declarator-notyp],$[declaration-specifiers-nots])); }
  | declaration-specifiers-ts           direct-declarator                
        { $$ = ast("param-decl",declare($[direct-declarator],$[declaration-specifiers-ts])); }
  | declaration-specifiers-nots pointer direct-abstract-declarator_opt   
        { $$ = ast("param-decl",declare(seqAppend($[direct-abstract-declarator_opt],$pointer),$[declaration-specifiers-nots])); }
  | declaration-specifiers-ts   pointer direct-abstract-declarator_opt   
        { $$ = ast("param-decl",declare(seqAppend($[direct-abstract-declarator_opt],$pointer),$[declaration-specifiers-ts])); }
  | declaration-specifiers-nots         direct-abstract-declarator_opt   
        { $$ = ast("param-decl",declare($[direct-abstract-declarator_opt],$[declaration-specifiers-nots])); }
  | declaration-specifiers-ts           direct-abstract-declarator_opt   
        { $$ = ast("param-decl",declare($[direct-abstract-declarator_opt],$[declaration-specifiers-ts])); }
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
    identifier                          { $$ = ast("identifier-list", $1 ); }
  | identifier-list "," any-identifier  { $$ = astAppend( $1, $3 ); }
  ;

identifier-list_opt:
    %empty              { $$ = null; }
  | identifier-list
  ;

// (6.7.7)
type-name:
    specifier-qualifier-list abstract-declarator_opt    { $$ = ast("type-name", $1, $2); }
  ;
  
// (6.7.7)
abstract-declarator:
    pointer                                     { $$ = seqAppend(ast("direct-abstract-declarator",null), $pointer); }
  | pointer_opt direct-abstract-declarator      { $$ = seqAppend($[direct-abstract-declarator], $pointer_opt); }
  ;

abstract-declarator_opt:
    %empty      { $$ = null; }
  | abstract-declarator
  ;

// (6.7.7)
rule(<Ast>,direct-abstract-declarator):
    "(" abstract-declarator ")"                                { $$ = $[abstract-declarator]; }
  | direct-abstract-declarator-elem                            { $$ = ast("direct-abstract-declarator", $[direct-abstract-declarator-elem]); }
  | direct-abstract-declarator direct-abstract-declarator-elem { $$ = seqAppend($1, $[direct-abstract-declarator-elem]); }
  ;

rule(<Ast>,direct-abstract-declarator_opt):
    %empty { $$ = ast("direct-abstract-declarator",null); }
  | direct-abstract-declarator
  ;

direct-abstract-declarator-elem:
    "[" type-qualifier-list assignment-expression_opt "]"
        { $$ = ast("qual",$[type-qualifier-list],ast("array",null,$[assignment-expression_opt],null)); }
  | "[" assignment-expression_opt "]"
        { $$ = ast("qual",null,ast("array",null,$[assignment-expression_opt],null)); }
  | "[" STATIC type-qualifier-list_opt assignment-expression "]"
        { $$ = ast("qual",$[type-qualifier-list_opt],ast("array",ast($STATIC),$[assignment-expression],null)); }
  | "[" type-qualifier-list STATIC assignment-expression "]"
        { $$ = ast("qual",$[type-qualifier-list],ast("array",ast($STATIC),$[assignment-expression],null)); }
  | "[" ASTERISK "]"
        { $$ = ast("qual",null,ast("array",null,ast($ASTERISK),null)); }
  | "(" parameter-type-list ")"
        { $$ = ast("qual",null,ast("func",$[parameter-type-list],null)); }
  | "(" ")"
        { $$ = ast("qual",null,ast("old-func",null)); }
  ;

// (6.7.9)
initializer:
    assignment-expression               { $$ = ast("initializer",$1); }
  | "{" initializer-list "}"            { $$ = ast("compound-initializer",$2); }
  | "{" initializer-list "," "}"        { $$ = ast("compound-initializer",$2); }
  ;

// (6.7.9)
initializer-list:
    designation_opt initializer         { $$ = ast("initializer-list", ast("initializer-elem",$designation_opt,$initializer)); }
  | initializer-list "," designation_opt initializer { $$ = astAppend($1, ast("initializer-elem",$designation_opt,$initializer)); }
  ;

// (6.7.9)
designation:
    designator-list "="                 { $$ = ast("designation",$1); }
  ;

designation_opt:
    %empty { $$ = null; }
  | designation
  ;
  
// (6.7.9)
designator-list:
    designator                          { $$ = ast("designator-list",$1); }
  | designator-list designator          { $$ = astAppend($1,$2); }
  ;

// (6.7.9)
designator:
    "[" constant-expression "]"         { $$ = ast("designator-index",$[constant-expression]); }
  | "." any-identifier                  { $$ = ast("designator-member",$[any-identifier]); }
// GNU C extension
  | "[" constant-expression[ce1] "..." constant-expression[ce2] "]" { $$ = ast("designator-range",$ce1,$ce2); }
  ;

// (6.7.10)
static_assert-declaration:
    _STATIC_ASSERT "(" constant-expression "," string-literal ")" ";"
        { $$ = ast( $_STATIC_ASSERT, $[constant-expression], $[string-literal] ); }
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
    any-identifier ":" statement                { $$ = ast("label", $statement); }
  | CASE constant-expression ":" statement      { $$ = ast("case", $[constant-expression], $statement); }
  | DEFAULT ":" statement                       { $$ = ast("default", null, $statement); }
// GNU C Extension
  | CASE constant-expression[ce1] "..." constant-expression[ce2] ":" statement { $$ = ast("case-range", $ce1, $ce2, $statement); }
  ;

// (6.8.2)
compound-statement:
    "{" PushScope block-item-list_opt "}"
        { $$ = ast("compound-statement",$[block-item-list_opt]); popScope($PushScope); }
  ;

rule(<Scope>,PushScope):
    %empty { $$ = pushScope(); }

// (6.8.2)
block-item-list:
    block-item                  { $$ = ast("block-item-list",$1); }
  | block-item-list block-item  { $$ = astAppend($1,$2); }
  ;

block-item-list_opt:
    %empty { $$ = null; }
  | block-item-list
  ;

// (6.8.2)
block-item:
    declaration { $$ = ast("declaration-statement",$1); }
  | statement
  ;

// (6.8.3)
expression-statement:
    expression_opt ";" { $$ = ast("expression-statement",$1); }
  ;

// (6.8.4)
selection-statement:
    IF "(" expression ")" statement                         %prec IF   { $$ = ast("if",$expression,$statement,null); }
  | IF "(" expression ")" statement[s1] ELSE statement[s2]  %prec ELSE { $$ = ast("if",$expression,$s1,$s2); }
  | SWITCH "(" expression ")" statement                                { $$ = ast("switch",$expression,$statement); }
  ;

// (6.8.5)
iteration-statement:
    WHILE "(" expression ")" statement           { $$ = ast("while",$expression,$statement); }
  | DO statement WHILE "(" expression ")" ";"    { $$ = ast("do",$statement,$expression); }
  | FOR "(" expression_opt[e1] ";" expression_opt[e2] ";" expression_opt[e3] ")" statement
      { $$ = ast("for",$e1,$e2,$e3); }
  | FOR "(" PushScope declaration[dcl] expression_opt[e2] ";" expression_opt[e3] ")" statement
      { $$ = ast("for",$dcl,$e2,$e3); popScope($PushScope); }
  ;

// (6.8.6)
jump-statement:
    GOTO any-identifier ";"   { $$ = ast("goto",$[any-identifier]); }
  | CONTINUE ";"              { $$ = ast("continue"); }
  | BREAK ";"                 { $$ = ast("break"); }
  | RETURN expression_opt ";" { $$ = ast("return", $expression_opt); }
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
    _GENERIC "(" assignment-expression "," generic-assoc-list ")" { $$ = ast("_Generic",$[assignment-expression],$[generic-assoc-list]); }
  ;

// (6.5.1.1)
generic-assoc-list:
    generic-association                         { $$ = ast("generic-assoc-list",$1); }
  | generic-assoc-list "," generic-association  { $$ = astAppend($1,$3); }
  ;

// (6.5.1.1)
generic-association:
    type-name ":" assignment-expression         { $$ = ast("generic-type-name-assoc",$[type-name],$[assignment-expression]); }
  | DEFAULT ":" assignment-expression           { $$ = ast("generic-default-assoc",null,$[assignment-expression]); }
  ;

// (6.5.2)
postfix-expression:
    primary-expression
  | postfix-expression "[" expression "]"                    { $$ = ast("subscript",$1,$expression); }
  | postfix-expression "(" argument-expression-list_opt ")"  { $$ = ast("call",$1,$[argument-expression-list_opt]); }
  | postfix-expression "." any-identifier                    { $$ = ast("select",$1,$[any-identifier]); }
  | postfix-expression "->" any-identifier                   { $$ = ast("ptr-select",$1,$[any-identifier]); }
  | postfix-expression "++"                                  { $$ = ast("post-inc",$1); }
  | postfix-expression "--"                                  { $$ = ast("post-dec",$1); }
  | "(" type-name ")" "{" initializer-list "}"               { $$ = ast("compound-literal", $[type-name], $[initializer-list]); }
  | "(" type-name ")" "{" initializer-list "," "}"           { $$ = ast("compound-literal", $[type-name], $[initializer-list]); }
  ;

// (6.5.2)
argument-expression-list:
    assignment-expression                               { $$ = ast("argument-expression-list",$1); }
  | argument-expression-list "," assignment-expression  { $$ = astAppend($1,$3); }
  ;

argument-expression-list_opt:
    %empty { $$ = null; }
  | argument-expression-list
  ;

// (6.5.3)
unary-expression:
    postfix-expression
  | "++" unary-expression               { $$ = ast("pre-inc", $2); }
  | "--" unary-expression               { $$ = ast("pre-dec", $2); }
  | unary-operator cast-expression      { $$ = ast($[unary-operator], $[cast-expression]); }
  | SIZEOF unary-expression             { $$ = ast("sizeof-expr",$2); }
  | SIZEOF "(" type-name ")"            { $$ = ast("sizeof-type",$[type-name]); }
  | _ALIGNOF "(" type-name ")"          { $$ = ast("_Alignof",$[type-name]); }
// GNU C extension
  | "&&" any-identifier                 { $$ = ast("address-label",$[any-identifier]); }
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
  | "(" type-name ")" cast-expression   { $$ = ast("cast",$[type-name],$4); }
  ;

// (6.5.5)
multiplicative-expression:
    cast-expression
  | multiplicative-expression "*" cast-expression  { $$ = ast("mul",$1,$3); }
  | multiplicative-expression "/" cast-expression  { $$ = ast("div",$1,$3); }
  | multiplicative-expression "%" cast-expression  { $$ = ast("rem",$1,$3); }
  ;

// (6.5.6)
additive-expression:
    multiplicative-expression
  | additive-expression "+" multiplicative-expression { $$ = ast("add",$1,$3); }
  | additive-expression "-" multiplicative-expression { $$ = ast("sub",$1,$3); }
  ;

// (6.5.7)
shift-expression:
    additive-expression
  | shift-expression "<<" additive-expression        { $$ = ast("shl",$1,$3); }
  | shift-expression ">>" additive-expression        { $$ = ast("shr",$1,$3); }
  ;

// (6.5.8)
relational-expression:
    shift-expression
  | relational-expression "<" shift-expression       { $$ = ast("lt",$1,$3); }
  | relational-expression ">" shift-expression       { $$ = ast("gt",$1,$3); }
  | relational-expression "<=" shift-expression      { $$ = ast("le",$1,$3); }
  | relational-expression ">=" shift-expression      { $$ = ast("ge",$1,$3); }
  ;

// (6.5.9)
equality-expression:
    relational-expression
  | equality-expression "==" relational-expression   { $$ = ast("eq",$1,$3); }
  | equality-expression "!=" relational-expression   { $$ = ast("ne",$1,$3); }
  ;

// (6.5.10)
AND-expression:
    equality-expression
  | AND-expression "&" equality-expression           { $$ = ast("binary-and",$1,$3); }
  ;

// (6.5.11)
exclusive-OR-expression:
    AND-expression
  | exclusive-OR-expression "^" AND-expression       { $$ = ast("binary-xor",$1,$3); }
  ;

// (6.5.12)
inclusive-OR-expression:
    exclusive-OR-expression
  | inclusive-OR-expression "|" exclusive-OR-expression { $$ = ast("binary-or",$1,$3); }
  ;

// (6.5.13)
logical-AND-expression:
    inclusive-OR-expression
  | logical-AND-expression "&&" inclusive-OR-expression  { $$ = ast("logical-and",$1,$3); }
  ;

// (6.5.14)
logical-OR-expression:
    logical-AND-expression
  | logical-OR-expression "||" logical-AND-expression    { $$ = ast("logical-or",$1,$3); }
  ;

// (6.5.15)
conditional-expression:
    logical-OR-expression
  | logical-OR-expression[e1] "?" expression[e2] ":" conditional-expression[e3] { $$ = ast("conditional",$e1,$e2,$e3); }
  ;

// (6.5.16)
assignment-expression:
    conditional-expression
  | unary-expression assignment-operator assignment-expression  { $$ = ast($[assignment-operator],$1,$3); }
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
  | expression "," assignment-expression        { $$ = ast("comma",$1,$3); }
  ;

expression_opt:
    %empty { $$ = null; }
  | expression
  ;

// (6.6)
constant-expression:
    conditional-expression
  ;

end_grammar