%language "Java"

%define package "c99.parser"
%code imports {
import c99.Constant;
import c99.CompilerOptions;
import c99.IErrorReporter;
import c99.parser.ast.Ast;
import static c99.Types.*;
import static c99.parser.Trees.*;
}
%define public
%define parser_class_name {CParser}
%define extends {ParserActions}

%parse-param { CompilerOptions opts_ }
%parse-param { IErrorReporter reporter_ }
%parse-param { SymTable symTab_ }
%code init {
  super.init( opts_, reporter_, symTab_ );
  pushScope(Scope.Kind.FILE);
}

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
%token<Code> _IMAGINARY   "_Imaginary"
%token<Code> _NORETURN   "_Noreturn"
%token<Code> _STATIC_ASSERT   "_Static_assert"
%token<Code> _THREAD_LOCAL   "_Thread_local"
%token<Code> GCC_ASM   "__asm__"
%token<Code> GCC_VOLATILE   "__volatile__"
%token<Code> GCC_TYPEOF   "__typeof__"
%token<Code> GCC_LABEL   "__label__"
%token<Code> GCC_ALIGNOF   "__alignof__"
%token<Code> GCC_ATTRIBUTE   "__attribute__"

// Set precedences to avoid IF-ELSE `shift'/reduce conflict
%precedence IF
%precedence ELSE

%type<Ast> constant
%type<Ast> enumerator-list
%type<Ast> enumerator
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

%expect 1
%start translation-unit

start_grammar
%%

rule(<Symbol>,identifier):
    IDENT
  ;

rule(<Symbol>,any-identifier,opt):
    TYPENAME    { $$ = $TYPENAME.symbol; }
  | IDENT
  ;

rule(<TStringLiteral>,string-literal):
    STRING_CONST[c]                     { $$ = stringLiteral(@$, $c); }
  | string-literal[lit] STRING_CONST[c] { $$ = stringLiteral(@$, $lit, $c); }
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
    function-definition
  | declaration
  | ";" { pedWarning( @1, "ANSI C disallows empty statement at file scope" ); }
// GCC extension
  | basic-asm ";"
  ;

// (6.9.1)
function-definition:
    specified-declarator-func compound-statement
  | specified-declarator-func PushParamScope declaration-list {} compound-statement
        { popScope($PushParamScope); FIXME(); }
  ;

rule(<Ast>,specified-declarator-func):
    declaration-specifiers-nots[ds] declarator-func-notyp[decl]  { $$ = declare($decl,$ds,true); }
  | declaration-specifiers-ts[ds]   declarator-func[decl]        { $$ = declare($decl,$ds,true); }
  ;

// (6.9.1)
declaration-list:
    declaration
  | declaration-list declaration
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
  | declaration-specifiers-nots init-declarator-list-notyp_opt ";"
  | declaration-specifiers-ts   init-declarator-list_opt ";"
  ;

rule(<DeclSpec>,declaration-specifiers-nots):
    _declaration-specifiers-nots { $$ = declSpec(@1,$1); }
  ;

rule(<DeclSpec>,declaration-specifiers-ts):
    _declaration-specifiers-ts   { $$ = declSpec(@1,$1); }
  ;

rule(<SpecNode>,_declaration-specifiers-nots):
    specifier-nots
  | specifier-nots _declaration-specifiers-nots  { $$ = append( $1, $2 ); }
  ;

rule(<SpecNode>,_declaration-specifiers-ts):
    type-specifier declaration-specifiers-ts-rest  { $$ = append( $1, $2 ); }
  | specifier-nots _declaration-specifiers-ts       { $$ = append( $1, $2 ); }
  ;

rule(<SpecNode>,declaration-specifiers-ts-rest):
    %empty                                               { $$ = null; }
  | type-specifier-notyp declaration-specifiers-ts-rest  { $$ = append( $1, $2 ); }
  | specifier-nots declaration-specifiers-ts-rest        { $$ = append( $1, $2 ); }
  ;

rule(<SpecNode>,specifier-nots):
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
    declarator[decl] asm-label_opt                 { $$ = FIXME(); declare($decl,$<DeclSpec>0,false); }
  | declarator[decl] asm-label_opt "=" initializer { $$ = FIXME(); declare($decl,$<DeclSpec>0,true); }
  ;

rule(<Ast>,init-declarator-notyp,,declaration-specifiers):
    declarator-notyp[decl] asm-label_opt                  { $$ = FIXME(); declare($decl,$<DeclSpec>0,false); }
  | declarator-notyp[decl] asm-label_opt "=" initializer  { $$ = FIXME(); declare($decl,$<DeclSpec>0,true); }
  ;

rule(,asm-label,optn):
    GCC_ASM "(" string-literal ")"
  ;

rule(<SpecNode>,gcc-attribute-specifier):
    GCC_ATTRIBUTE "(" "(" gcc-attribute-list[list] ")" ")"      { $$ = specExtAttr(@$,$list); }
  ;

rule(<TExtAttrList>,gcc-attribute-list):
    gcc-attribute_opt[attr]                               { $$ = extAttrList(@$,null,$attr); }
  | gcc-attribute-list[list] "," gcc-attribute_opt[attr]  { $$ = extAttrList(@$,$list,$attr); }
  ;

rule(<TExtAttr>,gcc-attribute,opt):
    gcc-attribute-name[name]                                          { $$ = extAttr(@$,@name,$name,null); }
  | gcc-attribute-name[name] "(" gcc-attribute-param-list[params] ")" { $$ = extAttr(@$,@name,$name,$params); }
  ;

rule(<String>,gcc-attribute-name):
    any-identifier     { $$ = $1.name; }
  | string-literal     { $$ = stringLiteralString(@1,$1); }
  ;

rule(<TreeList>,gcc-attribute-param-list):
    gcc-attribute-param[param]                                    { $$ = treeList(null,$param); }
  | gcc-attribute-param-list[list] "," gcc-attribute-param[param] { $$ = treeList($list,$param); }
  ;

rule(<Tree>,gcc-attribute-param):
    any-identifier    { $$ = symbolTree(@1,$1); }
  | INT_NUMBER        { $$ = intNumber(@1,$1); }
  | string-literal    { $$ = $1; }
  ;

// (6.7.1)
rule(<SpecNode>,storage-class-specifier):
    TYPEDEF                    { $$ = spec(@1,$1); }
  | EXTERN                     { $$ = spec(@1,$1); }
  | STATIC                     { $$ = spec(@1,$1); }
  | _THREAD_LOCAL              { $$ = spec(@1,$1); }
  | AUTO                       { $$ = spec(@1,$1); }
  | REGISTER                   { $$ = spec(@1,$1); }
  ;

// (6.7.2)
rule(<SpecNode>,type-specifier):
    type-specifier-notyp
  | TYPENAME                    { $$ = specTypename(@1,$1); }
  ;

rule(<SpecNode>,type-specifier-notyp):
    VOID                        { $$ = spec(@1,$1); }
  | CHAR                        { $$ = spec(@1,$1); }
  | SHORT                       { $$ = spec(@1,$1); }
  | INT                         { $$ = spec(@1,$1); }
  | LONG                        { $$ = spec(@1,$1); }
  | FLOAT                       { $$ = spec(@1,$1); }
  | DOUBLE                      { $$ = spec(@1,$1); }
  | SIGNED                      { $$ = spec(@1,$1); }
  | UNSIGNED                    { $$ = spec(@1,$1); }
  | _BOOL                       { $$ = spec(@1,$1); }
  | _COMPLEX                    { $$ = spec(@1,$1); }
  | _IMAGINARY                  { $$ = spec(@1,$1); }
  | atomic-type-specifier
  | struct-or-union-specifier
  | enum-specifier
// GNU C extension
  | GCC_TYPEOF "(" expression ")" { FIXME(); }
  | GCC_TYPEOF "(" type-name ")"  { FIXME(); }
  ;

// (6.7.2.1)
rule(<SpecNode>,struct-or-union-specifier):
    struct-or-union any-identifier_opt "{" PushAggScope struct-declaration-list "}"
      { $$ = declareAgg(@[struct-or-union], $[struct-or-union], @[any-identifier_opt], $[any-identifier_opt], popScope($PushAggScope)); }
  | struct-or-union any-identifier
      { $$ = specAgg(@[struct-or-union], $[struct-or-union], @[any-identifier], $[any-identifier]); }
  ;

// (6.7.2.1)
rule(<Code>,struct-or-union):
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
  | declaration-specifiers-nots struct-declarator-list-notyp_opt ";"
  | declaration-specifiers-ts   struct-declarator-list_opt ";"
  ;

// (6.7.2.1)
rule(<SpecNode>,specifier-qualifier-list):
    specifier-or-qualifier
  | specifier-qualifier-list specifier-or-qualifier     { $$ = append($1,$2); }
  ;

rule(<SpecNode>,specifier-or-qualifier):
    type-specifier
  | type-qualifier
  ;

// (6.7.2.1)
rule(,struct-declarator-list,optn):
    struct-declarator
  | struct-declarator-list "," _PUSH0 struct-declarator
  ;

rule(,struct-declarator-list-notyp,optn):
    struct-declarator-notyp
  | struct-declarator-list-notyp "," _PUSH0 struct-declarator
  ;

// (6.7.2.1)
struct-declarator:
    declarator[decl]                              { declare($decl,$<DeclSpec>0); }
  | declarator_opt ":" constant-expression        { FIXME(); }
  ;

struct-declarator-notyp:
    declarator-notyp[decl]                         { declare($decl,$<DeclSpec>0); }
  | declarator-notyp_opt ":" constant-expression   { FIXME(); }
  ;

// (6.7.2.2)
rule(<SpecNode>,enum-specifier):
    ENUM any-identifier_opt "{" enumerator-list "}"             { FIXME(); }
  | ENUM any-identifier_opt "{" enumerator-list "," "}"         { FIXME(); }
  | ENUM any-identifier                                         { FIXME(); }  
  ;

// (6.7.2.2)
enumerator-list:
    enumerator                       { $$ = ast( "enumerator-list", $1 ); }
  | enumerator-list "," enumerator   { $$ = astAppend( $1, $3 ); }
  ;

// (6.7.2.2)
enumerator:
    enumeration-constant                          { FIXME(); }
  | enumeration-constant "=" constant-expression  { FIXME(); }
  ;

enumeration-constant:
    any-identifier
  ;

// (6.7.2.4)
rule(<SpecNode>,atomic-type-specifier):
    _ATOMIC "(" type-name ")"  { FIXME(); }
  ;

// (6.7.3)
rule(<SpecNode>,type-qualifier):
    CONST       { $$ = spec(@1,$1); }
  | RESTRICT    { $$ = spec(@1,$1); }
  | VOLATILE    { $$ = spec(@1,$1); }
  | _ATOMIC     { $$ = spec(@1,$1); }
  | gcc-attribute-specifier
  ;

// (6.7.4)
rule(<SpecNode>,function-specifier):
    INLINE      { $$ = spec(@1,$1); }
  | _NORETURN   { $$ = spec(@1,$1); }
  ;

// (6.7.5)
rule(<SpecNode>,alignment-specifier):
    _ALIGNAS "(" type-name ")"                  { FIXME(); }
  | _ALIGNAS "(" constant-expression ")"        { FIXME(); }
  ;

// (6.7.6)
rule(<Declarator>,declarator):
    declarator-func
  | declarator-nofunc
  ;
rule(<Declarator>,declarator_opt):
    declarator
  | %empty                              { $$ = abstractDeclarator(yyloc); }
  ;

rule(<Declarator>,declarator-notyp):
    declarator-func-notyp
  | declarator-nofunc-notyp
  ;
rule(<Declarator>,declarator-notyp_opt):
    declarator-notyp
  | %empty                              { $$ = abstractDeclarator(yyloc); }
  ;

rule(<Declarator>,declarator-func):
    pointer_opt[ptr] direct-declarator-func[decl]  { $$ = $decl.append($ptr); }
  ;
rule(<Declarator>,declarator-func-notyp):
    pointer[ptr] direct-declarator-func[decl]      { $$ = $decl.append($ptr); }
  |              direct-declarator-func-notyp
  ;

rule(<Declarator>,declarator-nofunc):
    pointer_opt[ptr] direct-declarator-nofunc[decl]  { $$ = $decl.append($ptr); }
  ;
rule(<Declarator>,declarator-nofunc-notyp):
    pointer[ptr] direct-declarator-nofunc[decl]      { $$ = $decl.append($ptr); }
  |              direct-declarator-nofunc-notyp
  ;

rule(<Declarator>,direct-declarator):
    direct-declarator-func
  | direct-declarator-nofunc
  ;
rule(<Declarator>,direct-declarator-prm):
    direct-declarator-func-prm
  | direct-declarator-nofunc-prm
  ;
rule(<Declarator>,direct-declarator-prmnotyp):
    direct-declarator-func-prmnotyp
  | direct-declarator-nofunc-prmnotyp
  ;

rule(<Symbol>,any-pident):
    any-identifier
  | "(" any-pident[id] ")"    { $$ = $id; }
  ;
rule(<Symbol>,pident):
    identifier
  | "(" any-pident[id] ")"    { $$ = $id; }
  ;
rule(<Symbol>,pident-prm):
    "(" identifier[id] ")"    { $$ = $id; }
  | "(" pident-prm[id] ")"    { $$ = $id; }
  ;

rule(<Declarator>,direct-declarator-func):
    any-pident[id] elem-func[el]                  { $$ = declarator(@id,$id).append($el); }
  | "(" direct-declarator-func[decl] ")"          { $$ = $decl; }
  | direct-declarator-func[decl] direct-declarator-elem[el] { $$ = $decl.append($el); }
  ;
rule(<Declarator>,direct-declarator-func-notyp):
    pident[id] elem-func[el]                           { $$ = declarator(@id,$id).append($el); }
  | "(" direct-declarator-func[decl] ")"               { $$ = $decl; }
  | direct-declarator-func-notyp[decl] direct-declarator-elem[el] { $$ = $decl.append($el); }
  ;
rule(<Declarator>,direct-declarator-func-prm):
    any-identifier[id] elem-func[el]                   { $$ = declarator(@id,$id).append($el); }
  | direct-declarator-func-prm2
  | direct-declarator-func-prm[decl] direct-declarator-elem[el] { $$ = $decl.append($el); }
  ;
rule(<Declarator>,direct-declarator-func-prm2):
    pident-prm[id] elem-func[el]                   { $$ = declarator(@id,$id).append($el); }
  | "(" direct-declarator-func-prm2[decl] ")"      { $$ = $decl; }
  ;
rule(<Declarator>,direct-declarator-func-prmnotyp):
    identifier[id] elem-func[el]                   { $$ = declarator(@id,$id).append($el); }
  | direct-declarator-func-prmnotyp2
  | direct-declarator-func-prmnotyp[decl] direct-declarator-elem[el] { $$ = $decl.append($el); }
  ;
rule(<Declarator>,direct-declarator-func-prmnotyp2):
    pident-prm[id] elem-func[el]                   { $$ = declarator(@id,$id).append($el); }
  | "(" direct-declarator-func-prmnotyp2[decl] ")" { $$ = $decl; }
  ;

rule(<Declarator>,direct-declarator-nofunc):
    any-pident[id]                               { $$ = declarator(@id,$id); }
  | d2
  ;
rule(<Declarator>,direct-declarator-nofunc-notyp):
    pident[id]                                   { $$ = declarator(@id,$id); }
  | d2-notyp
  ;
rule(<Declarator>,direct-declarator-nofunc-prm):
    any-identifier[id]                           { $$ = declarator(@id,$id); }
  | pident-prm[id]                               { $$ = declarator(@id,$id); }
  | d2-prm
  ;
rule(<Declarator>,direct-declarator-nofunc-prmnotyp):
    identifier[id]                               { $$ = declarator(@id,$id); }
  | pident-prm[id]                               { $$ = declarator(@id,$id); }
  | d2-prmnotyp
  ;

rule(<Declarator>,d2):
    any-pident[id] elem-nofunc[el]                { $$ = declarator(@id,$id).append($el); }
  | "(" pointer[ptr] direct-declarator[decl] ")"  { $$ = $decl.append($ptr); }
  | d2[decl] direct-declarator-elem[el]           { $$ = $decl.append($el); }
  ;
rule(<Declarator>,d2-notyp):
    pident[id] elem-nofunc[el]                    { $$ = declarator(@id,$id).append($el); }
  | "(" pointer[ptr] direct-declarator[decl] ")"  { $$ = $decl.append($ptr); }
  | d2-notyp[decl] direct-declarator-elem[el]     { $$ = $decl.append($el); }
  ;
rule(<Declarator>,d2-prm):
    any-identifier[id] elem-nofunc[el]            { $$ = declarator(@id,$id).append($el); }
  | pident-prm[id] elem-nofunc[el]                { $$ = declarator(@id,$id).append($el); }
  | "(" pointer[ptr] direct-declarator-prm[decl] ")" { $$ = $decl.append($ptr); }
  | d2-prm[decl] direct-declarator-elem[el]       { $$ = $decl.append($el); }
  ;
rule(<Declarator>,d2-prmnotyp):
    identifier[id] elem-nofunc[el]                { $$ = declarator(@id,$id).append($el); }
  | pident-prm[id] elem-nofunc[el]                { $$ = declarator(@id,$id).append($el); }
  | "(" pointer[ptr] direct-declarator-prm[decl] ")" { $$ = $decl.append($ptr); }
  | d2-prmnotyp[decl] direct-declarator-elem[el]  { $$ = $decl.append($el); }
  ;

// (6.7.6)
rule(<DeclElem>,direct-declarator-elem):
    elem-nofunc
  | elem-func
  ;

rule(<DeclElem>,elem-nofunc):
    "[" type-qualifier-list_opt assignment-expression_opt "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list_opt],null,null,$[assignment-expression_opt]); }
  | "[" STATIC type-qualifier-list_opt assignment-expression "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list_opt],@STATIC,null,$[assignment-expression]); }
  | "[" type-qualifier-list STATIC assignment-expression "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list],@STATIC,null,$[assignment-expression]); }
  | "[" type-qualifier-list_opt ASTERISK "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list_opt],null,@ASTERISK,null); }
  ;

rule(<DeclElem>,elem-func):
    newfunc-decl
  | oldfunc-decl
  ;

rule(<DeclElem>,oldfunc-decl):
    "(" identifier-list_opt ")" { $$ = oldFuncDecl(@$, $[identifier-list_opt]); }
  ;
rule(<DeclElem>,newfunc-decl):
    "(" parameter-type-list ")" { $$ = funcDecl(@$, $[parameter-type-list]); }
  ;

// (6.7.6)
rule(<DeclElem>,pointer,opt):
                  "*"[p] type-qualifier-list_opt  { $$ = pointerDecl(@p,$[type-qualifier-list_opt], null); }
  | pointer[left] "*"[p] type-qualifier-list_opt  { $$ = pointerDecl(@p,$[type-qualifier-list_opt], $left); }
  ;

// (6.7.6)
rule(<SpecNode>,type-qualifier-list,opt):
    type-qualifier
  | type-qualifier-list[left] type-qualifier  { $$ = append($left, $[type-qualifier]); }
  ;

// (6.7.6)
rule(<DeclList>,parameter-type-list):
    parameter-list
  | parameter-list "," "..."            { $$ = $[parameter-list].setEllipsis(); }
  ;

// (6.7.6)
rule(<DeclList>,parameter-list):
    parameter-declaration                           { $$ = declList(null,$[parameter-declaration]); }
  | parameter-list[left] "," parameter-declaration  { $$ = declList($left,$[parameter-declaration]); }
  ;

// (6.7.6)
rule(<DeclInfo>,parameter-declaration):
    declaration-specifiers-nots pointer direct-declarator-prm
        { $$ = declInfo($[direct-declarator-prm].append($pointer), $[declaration-specifiers-nots]); }
  | declaration-specifiers-ts   pointer direct-declarator-prm
        { $$ = declInfo($[direct-declarator-prm].append($pointer), $[declaration-specifiers-ts]); }
  | declaration-specifiers-nots         direct-declarator-prmnotyp
        { $$ = declInfo($[direct-declarator-prmnotyp], $[declaration-specifiers-nots]); }
  | declaration-specifiers-ts           direct-declarator-prm
        { $$ = declInfo($[direct-declarator-prm], $[declaration-specifiers-ts]); }
  | declaration-specifiers-nots pointer direct-abstract-declarator_opt
        { $$ = declInfo($[direct-abstract-declarator_opt].append($pointer), $[declaration-specifiers-nots]); }
  | declaration-specifiers-ts   pointer direct-abstract-declarator_opt
        { $$ = declInfo($[direct-abstract-declarator_opt].append($pointer), $[declaration-specifiers-ts]); }
  | declaration-specifiers-nots         direct-abstract-declarator_opt
        { $$ = declInfo($[direct-abstract-declarator_opt], $[declaration-specifiers-nots]); }
  | declaration-specifiers-ts           direct-abstract-declarator_opt
        { $$ = declInfo($[direct-abstract-declarator_opt], $[declaration-specifiers-ts]); }
  ;

/*
  In a identifier list (old-style parameter list)
  all but the first identifier cannot redefine a typedef.
  (6.9.1-6)
  (If the first one was a typedef then we would assume that this
  is a new style declaration).
*/
// (6.7.6)
rule(<IdentList>,identifier-list,opt):
    identifier                                { $$ = identListAdd(@identifier, identList(), $identifier); }
  | identifier-list[list] "," any-identifier  { $$ = identListAdd(@[any-identifier], $list, $[any-identifier]); }
  ;

// (6.7.7)
rule(<Qual>,type-name):
    specifier-qualifier-list[slist] abstract-declarator_opt
        { $$ = mkTypeName($[abstract-declarator_opt], declSpec(@slist,$slist)); }
  ;
  
// (6.7.7)
rule(<Declarator>,abstract-declarator):
    pointer                                     { $$ = abstractDeclarator(@pointer).append($pointer); }
  |             direct-abstract-declarator
  | pointer     direct-abstract-declarator      { $$ = $[direct-abstract-declarator].append($pointer); }
  ;

rule(<Declarator>,abstract-declarator_opt):
    abstract-declarator
  | %empty                                      { $$ = abstractDeclarator(yyloc); }
  ;

// (6.7.7)
rule(<Declarator>,direct-abstract-declarator):
    "(" abstract-declarator ")"                                      { $$ = $[abstract-declarator]; }
  | direct-abstract-declarator-elem[elem]                            { $$ = abstractDeclarator(@elem).append($elem); }
  | direct-abstract-declarator direct-abstract-declarator-elem[elem] { $$ = $1.append($elem); }
  ;

rule(<Declarator>,direct-abstract-declarator_opt):
    direct-abstract-declarator
  | %empty                                     { $$ = abstractDeclarator(yyloc); }
  ;

rule(<DeclElem>,direct-abstract-declarator-elem):
    "[" type-qualifier-list assignment-expression_opt "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list],null,null,$[assignment-expression_opt]); }
  | "[" assignment-expression_opt "]"
        { $$ = arrayDecl(@$,null,null,null,$[assignment-expression_opt]); }
  | "[" STATIC type-qualifier-list_opt assignment-expression "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list_opt],@STATIC,null,$[assignment-expression]); }
  | "[" type-qualifier-list STATIC assignment-expression "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list],@STATIC,null,$[assignment-expression]); }
  | "[" ASTERISK "]"
        { $$ = arrayDecl(@$,null,null,@ASTERISK,null); }
  | newfunc-decl
  | "(" ")"
        { $$ = oldFuncDecl(@$,null); }
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
  | "." any-identifier                  { FIXME(); }
// GNU C extension
  | "[" constant-expression[ce1] "..." constant-expression[ce2] "]" { $$ = ast("designator-range",$ce1,$ce2); }
  ;

// (6.7.10)
static_assert-declaration:
    _STATIC_ASSERT "(" constant-expression "," string-literal ")" ";"
        { FIXME(); }
  ;


// A.2.3 Statements


statement:
    labeled-statement
  | compound-statement
  | expression-statement
  | selection-statement
  | iteration-statement
  | jump-statement
// GCC extension
  | asm-statement       { FIXME(); }
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
    "{" PushBlockScope declare-labels-list_opt block-item-list_opt "}"
        { $$ = FIXME(); popScope($PushBlockScope); }
  ;

rule(<Scope>,PushBlockScope):
    %empty { $$ = pushScope(Scope.Kind.BLOCK); }

rule(<Scope>,PushParamScope):
    %empty { $$ = pushScope(Scope.Kind.PARAM); }

rule(<Scope>,PushAggScope):
    %empty { $$ = pushScope(Scope.Kind.AGGREGATE); }

// EXT: This is a GNU C extension
//
rule(,declare-labels-list,optn):
    declare-labels ";"
  | declare-labels-list declare-labels ";"
  ;

declare-labels:
    GCC_LABEL any-identifier
  | declare-labels "," any-identifier
  ;

// (6.8.2)
block-item-list:
    block-item                  { FIXME(); }
  | block-item-list block-item  { FIXME(); }
  ;

block-item-list_opt:
    %empty { $$ = null; }
  | block-item-list
  ;

// (6.8.2)
rule(<Ast>,block-item):
    declaration { FIXME(); }
  | statement   { FIXME(); }
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
      { FIXME(); }
  | FOR "(" PushBlockScope declaration[dcl] expression_opt[e2] ";" expression_opt[e3] ")" statement
      { FIXME(); popScope($PushBlockScope); }
  ;

// (6.8.6)
jump-statement:
    GOTO any-identifier ";"   { FIXME(); }
  | CONTINUE ";"              { $$ = ast("continue"); }
  | BREAK ";"                 { $$ = ast("break"); }
  | RETURN expression_opt ";" { $$ = ast("return", $expression_opt); }
  ;

asm-statement:
    basic-asm ";"
  | extended-asm ";"
  ;

basic-asm:
    GCC_ASM asm-volatile_opt "(" string-literal[templ] ")"
  ;

extended-asm:
    GCC_ASM asm-volatile_opt "(" string-literal[templ] ":" asm-operands_opt[out] ")"
  | GCC_ASM asm-volatile_opt "(" string-literal[templ] ":" asm-operands_opt[out] ":" asm-operands_opt[in] ")"
  | GCC_ASM asm-volatile_opt
      "(" string-literal[templ] ":" asm-operands_opt[out] ":" asm-operands_opt[in] ":" asm-clobbers[clob] ")"
  | GCC_ASM asm-volatile_opt GOTO
      "(" string-literal[templ] ":" asm-operands_opt[out] ":" asm-operands_opt[in] ":" asm-clobbers[clob] ":"
          asm-labels_opt[labs] ")"
  ;

rule(,asm-volatile,optn):
    GCC_VOLATILE
  ;

rule(,asm-operands,optn):
    asm-operand
  | asm-operands "," asm-operand
  ;

asm-operand:
    string-literal "(" expression ")"
  | "[" any-identifier "]" string-literal "(" expression ")"
  ;

asm-clobbers:
    string-literal
  | asm-clobbers "," string-literal
  ;

rule(,asm-labels,optn):
    any-identifier
  | asm-labels "," any-identifier
  ;

// A.2.1 Expressions

// (6.5.1)
primary-expression:
    identifier          { $$ = ident($1,@1); }
  | constant
  | string-literal      { FIXME(); }
  | "(" expression ")"  { $$ = $expression; }
// GCC extension
  | "(" compound-statement ")" { FIXME(); }
  | generic-selection
  ;

// (6.5.1.1)
generic-selection:
    _GENERIC "(" assignment-expression "," generic-assoc-list ")" { FIXME(); }
  ;

// (6.5.1.1)
generic-assoc-list:
    generic-association                         { FIXME(); }
  | generic-assoc-list "," generic-association  { FIXME(); }
  ;

// (6.5.1.1)
generic-association:
    type-name ":" assignment-expression         { FIXME(); }
  | DEFAULT ":" assignment-expression           { FIXME(); }
  ;

// (6.5.2)
postfix-expression:
    primary-expression
  | postfix-expression "[" expression "]"                    { $$ = ast("subscript",$1,$expression); }
  | postfix-expression "(" argument-expression-list_opt ")"  { $$ = ast("call",$1,$[argument-expression-list_opt]); }
  | postfix-expression "." any-identifier                    { FIXME(); }
  | postfix-expression "->" any-identifier                   { FIXME(); }
  | postfix-expression "++"                                  { $$ = ast("post-inc",$1); }
  | postfix-expression "--"                                  { $$ = ast("post-dec",$1); }
  | "(" type-name ")" "{" initializer-list "}"               { FIXME(); }
  | "(" type-name ")" "{" initializer-list "," "}"           { FIXME(); }
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
  | SIZEOF "(" type-name ")"            { FIXME(); }
  | _ALIGNOF "(" type-name ")"          { FIXME(); }
// GNU C extension
  | GCC_ALIGNOF unary-expression        { FIXME(); }
  | GCC_ALIGNOF "(" type-name ")"       { FIXME(); }
  | "&&" any-identifier                 { FIXME(); }
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
  | "(" type-name ")" cast-expression   { FIXME(); }
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

