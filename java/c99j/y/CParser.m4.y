%language "Java"

%define package "c99.parser"
%code imports {
import c99.Constant;
import c99.CompEnv;
import c99.parser.ast.Ast;
import static c99.Types.*;
import static c99.parser.Trees.*;
import c99.parser.tree.*;
}
%define public
%define parser_class_name {CParser}
%define extends {ParserActions}

%parse-param { CompEnv cenv_ }
%parse-param { SymTable symTab_ }
%code init {
  super.init( cenv_, symTab_ );
  pushFileScope();
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

rule(<TExpr.Expr>,constant):
    INT_NUMBER                          { $$ = exprConstant(@1,$1); }
  | REAL_NUMBER                         { $$ = exprConstant(@1,$1); }
  | CHAR_CONST                          { $$ = exprConstant(@1,$1); }
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

specified-declarator-func:
    declaration-specifiers-nots[ds] declarator-func-notyp[decl]
        { finishDeclarator($ds,$decl,true); }
  | declaration-specifiers-ts[ds]   declarator-func[decl]
        { finishDeclarator($ds,$decl,true); }
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

rule(<TSpecNode>,declaration-specifiers-nots):
    specifier-nots
  | specifier-nots declaration-specifiers-nots  { $$ = appendSpecNode( $1, $2 ); }
  ;

rule(<TSpecNode>,declaration-specifiers-ts):
    type-specifier declaration-specifiers-ts-rest  { $$ = appendSpecNode( $1, $2 ); }
  | specifier-nots declaration-specifiers-ts       { $$ = appendSpecNode( $1, $2 ); }
  ;

rule(<TSpecNode>,declaration-specifiers-ts-rest):
    %empty                                               { $$ = null; }
  | type-specifier-notyp declaration-specifiers-ts-rest  { $$ = appendSpecNode( $1, $2 ); }
  | specifier-nots declaration-specifiers-ts-rest        { $$ = appendSpecNode( $1, $2 ); }
  ;

rule(<TSpecNode>,specifier-nots):
    storage-class-specifier
  | type-qualifier
  | function-specifier
  | alignment-specifier
  ;

// (6.7)
init-declarator-list_opt:
    %empty                  { emptyDeclaration($<TSpecNode>0); }
  | init-declarator-list
  ;
init-declarator-list:
    init-declarator
  | init-declarator-list "," _PUSH0 init-declarator
  ;

init-declarator-list-notyp_opt:
    %empty                  { emptyDeclaration($<TSpecNode>0); }
  | init-declarator-list-notyp
  ;
init-declarator-list-notyp:
    init-declarator-notyp
  | init-declarator-list-notyp "," _PUSH0 init-declarator
  ;

// (6.7)
init-declarator:
    declarator[decl] asm-label_opt
        { finishDeclarator( $<TSpecNode>0, $decl, false ); }
  | declarator[decl] asm-label_opt "="
        { finishDeclarator( $<TSpecNode>0, $decl, true ); }
        initializer
  ;

init-declarator-notyp:
    declarator-notyp[decl] asm-label_opt
        { finishDeclarator( $<TSpecNode>0, $decl, false ); }
  | declarator-notyp[decl] asm-label_opt "="
        { finishDeclarator( $<TSpecNode>0, $decl, true ); }
        initializer
  ;

rule(,asm-label,optn):
    GCC_ASM "(" string-literal ")"
  ;

rule(<TSpecNode>,gcc-attribute-specifier):
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
rule(<TSpecNode>,storage-class-specifier):
    TYPEDEF                    { $$ = spec(@1,$1); }
  | EXTERN                     { $$ = spec(@1,$1); }
  | STATIC                     { $$ = spec(@1,$1); }
  | _THREAD_LOCAL              { $$ = spec(@1,$1); }
  | AUTO                       { $$ = spec(@1,$1); }
  | REGISTER                   { $$ = spec(@1,$1); }
  ;

// (6.7.2)
rule(<TSpecNode>,type-specifier):
    type-specifier-notyp
  | TYPENAME                    { $$ = specTypename(@1,$1); }
  ;

rule(<TSpecNode>,type-specifier-notyp):
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
  | _IMAGINARY                  { pedWarning(@1, "%s is an extension", $1.str); $$ = spec(@1,$1); }
  | atomic-type-specifier
  | struct-or-union-specifier
  | enum-specifier
// GNU C extension
  | GCC_TYPEOF "(" expression ")" { FIXME(); }
  | GCC_TYPEOF "(" type-name ")"  { FIXME(); }
  ;

// (6.7.2.1)
rule(<TSpecNode>,struct-or-union-specifier):
    struct-or-union[code] any-identifier_opt[ident] "{"
            { $<Decl>$ = beginDeclareAgg(@code, $code, @ident, $ident); }[decl]
        PushAggScope[scope] struct-declaration-list "}"
            { $$ = declareAgg($code, $<Decl>decl, popScope($scope)); }
  | struct-or-union[code] any-identifier[ident]
            { $$ = referenceAgg(@code, $code, @ident, $ident); }
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
rule(<TSpecNode>,specifier-qualifier-list):
    specifier-or-qualifier
  | specifier-qualifier-list specifier-or-qualifier     { $$ = appendSpecNode($1,$2); }
  ;

rule(<TSpecNode>,specifier-or-qualifier):
    type-specifier
  | type-qualifier
  ;

// (6.7.2.1)
struct-declarator-list_opt:
    %empty { emptyDeclaration( $<TSpecNode>0 ); }
  | struct-declarator-list
  ;
struct-declarator-list:
    struct-declarator
  | struct-declarator-list "," _PUSH0 struct-declarator
  ;

struct-declarator-list-notyp_opt:
    %empty { emptyDeclaration( $<TSpecNode>0 ); }
  | struct-declarator-list-notyp
  ;
struct-declarator-list-notyp:
    struct-declarator-notyp
  | struct-declarator-list-notyp "," _PUSH0 struct-declarator
  ;

// (6.7.2.1)
struct-declarator:
    declarator[decl]                        { finishDeclarator($<TSpecNode>0,$decl,false); }
  | declarator_opt[decl] ":" constant-expression[w]  { finishBitfield($<TSpecNode>0,$decl,@w,$w); }
  ;

struct-declarator-notyp:
    declarator-notyp[decl]                  { finishDeclarator($<TSpecNode>0,$decl,false); }
  | declarator-notyp_opt[decl] ":" constant-expression[w]  { finishBitfield($<TSpecNode>0,$decl,@w,$w); }
  ;

// (6.7.2.2)
rule(<TSpecNode>,enum-specifier):
    ENUM[code] any-identifier_opt[ident] "{"
            { $<Decl>$ = beginDeclareAgg(@code, $code, @ident, $ident ); }[decl]
        PushEnumScope[scope] enumerator-list comma_opt "}"
            { $$ = declareAgg($code, $<Decl>decl, popScope($scope)); }
  | ENUM[code] any-identifier[ident]
            { $$ = referenceAgg(@code, $code, @ident, $ident); }
  ;

comma_opt:
    %empty
  | ","
  ;

// (6.7.2.2)
enumerator-list:
    enumerator
  | enumerator-list "," enumerator
  ;

// (6.7.2.2)
enumerator:
    enumeration-constant[ident]
        { declareEnumConstant(@ident,$ident,null,null); }
  | enumeration-constant[ident] "=" constant-expression[val]
        { declareEnumConstant(@ident,$ident,@val,$val); }
  ;

rule(<Symbol>,enumeration-constant):
    any-identifier
  ;

// (6.7.2.4)
rule(<TSpecNode>,atomic-type-specifier):
    _ATOMIC "(" type-name ")"  { FIXME(); }
  ;

// (6.7.3)
rule(<TSpecNode>,type-qualifier):
    CONST       { $$ = spec(@1,$1); }
  | RESTRICT    { $$ = spec(@1,$1); }
  | VOLATILE    { $$ = spec(@1,$1); }
  | _ATOMIC     { $$ = spec(@1,$1); }
  | gcc-attribute-specifier
  ;

// (6.7.4)
rule(<TSpecNode>,function-specifier):
    INLINE      { $$ = spec(@1,$1); }
  | _NORETURN   { $$ = spec(@1,$1); }
  ;

// (6.7.5)
rule(<TSpecNode>,alignment-specifier):
    _ALIGNAS "(" type-name ")"                  { FIXME(); }
  | _ALIGNAS "(" constant-expression ")"        { FIXME(); }
  ;

// (6.7.6)
rule(<TDeclarator>,declarator):
    declarator-func
  | declarator-nofunc
  ;
rule(<TDeclarator>,declarator_opt):
    declarator
  | %empty                              { $$ = abstractDeclarator(yyloc); }
  ;

rule(<TDeclarator>,declarator-notyp):
    declarator-func-notyp
  | declarator-nofunc-notyp
  ;
rule(<TDeclarator>,declarator-notyp_opt):
    declarator-notyp
  | %empty                              { $$ = abstractDeclarator(yyloc); }
  ;

rule(<TDeclarator>,declarator-func):
    pointer_opt[ptr] direct-declarator-func[decl]  { $$ = $decl.append($ptr); }
  ;
rule(<TDeclarator>,declarator-func-notyp):
    pointer[ptr] direct-declarator-func[decl]      { $$ = $decl.append($ptr); }
  |              direct-declarator-func-notyp
  ;

rule(<TDeclarator>,declarator-nofunc):
    pointer_opt[ptr] direct-declarator-nofunc[decl]  { $$ = $decl.append($ptr); }
  ;
rule(<TDeclarator>,declarator-nofunc-notyp):
    pointer[ptr] direct-declarator-nofunc[decl]      { $$ = $decl.append($ptr); }
  |              direct-declarator-nofunc-notyp
  ;

/*rule_(<TDeclarator>,direct-declarator):
    direct-declarator-func
  | direct-declarator-nofunc
  ;*/
rule(<TDeclarator>,direct-declarator-prm):
    direct-declarator-func-prm
  | direct-declarator-nofunc-prm
  ;
rule(<TDeclarator>,direct-declarator-prmnotyp):
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

rule(<TDeclarator>,direct-declarator-func):
    any-pident[id] elem-func[el]                  { $$ = declarator(@id,$id).append($el); }
  | "(" direct-declarator-func[decl] ")"          { $$ = $decl; }
  | "(" pointer[ptr] direct-declarator-func[decl] ")"       { $$ = $decl.append($ptr); }
  | direct-declarator-func[decl] direct-declarator-elem[el] { $$ = $decl.append($el); }
  ;
rule(<TDeclarator>,direct-declarator-func-notyp):
    pident[id] elem-func[el]                           { $$ = declarator(@id,$id).append($el); }
  | "(" direct-declarator-func[decl] ")"               { $$ = $decl; }
  | "(" pointer[ptr] direct-declarator-func[decl] ")"             { $$ = $decl.append($ptr); }
  | direct-declarator-func-notyp[decl] direct-declarator-elem[el] { $$ = $decl.append($el); }
  ;
rule(<TDeclarator>,direct-declarator-func-prm):
    any-identifier[id] elem-func[el]                   { $$ = declarator(@id,$id).append($el); }
  | direct-declarator-func-prm2
  | direct-declarator-func-prm[decl] direct-declarator-elem[el] { $$ = $decl.append($el); }
  ;
rule(<TDeclarator>,direct-declarator-func-prm2):
    pident-prm[id] elem-func[el]                   { $$ = declarator(@id,$id).append($el); }
  | "(" direct-declarator-func-prm2[decl] ")"      { $$ = $decl; }
  | "(" pointer[ptr] direct-declarator-func-prm2[decl] ")"      { $$ = $decl.append($ptr); }
  ;
rule(<TDeclarator>,direct-declarator-func-prmnotyp):
    identifier[id] elem-func[el]                   { $$ = declarator(@id,$id).append($el); }
  | direct-declarator-func-prmnotyp2
  | direct-declarator-func-prmnotyp[decl] direct-declarator-elem[el] { $$ = $decl.append($el); }
  ;
rule(<TDeclarator>,direct-declarator-func-prmnotyp2):
    pident-prm[id] elem-func[el]                   { $$ = declarator(@id,$id).append($el); }
  | "(" direct-declarator-func-prmnotyp2[decl] ")" { $$ = $decl; }
  | "(" pointer[ptr] direct-declarator-func-prmnotyp2[decl] ")" { $$ = $decl.append($ptr); }
  ;

rule(<TDeclarator>,direct-declarator-nofunc):
    any-pident[id]                               { $$ = declarator(@id,$id); }
  | d2
  ;
rule(<TDeclarator>,direct-declarator-nofunc-notyp):
    pident[id]                                   { $$ = declarator(@id,$id); }
  | d2-notyp
  ;
rule(<TDeclarator>,direct-declarator-nofunc-prm):
    any-identifier[id]                           { $$ = declarator(@id,$id); }
  | pident-prm[id]                               { $$ = declarator(@id,$id); }
  | d2-prm
  ;
rule(<TDeclarator>,direct-declarator-nofunc-prmnotyp):
    identifier[id]                               { $$ = declarator(@id,$id); }
  | pident-prm[id]                               { $$ = declarator(@id,$id); }
  | d2-prmnotyp
  ;

rule(<TDeclarator>,d2):
    any-pident[id] elem-nofunc[el]                { $$ = declarator(@id,$id).append($el); }
  | "(" pointer[ptr] direct-declarator-nofunc[decl] ")"  { $$ = $decl.append($ptr); }
  | d2[decl] direct-declarator-elem[el]           { $$ = $decl.append($el); }
  ;
rule(<TDeclarator>,d2-notyp):
    pident[id] elem-nofunc[el]                    { $$ = declarator(@id,$id).append($el); }
  | "(" pointer[ptr] direct-declarator-nofunc[decl] ")"  { $$ = $decl.append($ptr); }
  | d2-notyp[decl] direct-declarator-elem[el]     { $$ = $decl.append($el); }
  ;
rule(<TDeclarator>,d2-prm):
    any-identifier[id] elem-nofunc[el]            { $$ = declarator(@id,$id).append($el); }
  | pident-prm[id] elem-nofunc[el]                { $$ = declarator(@id,$id).append($el); }
  | "(" pointer[ptr] direct-declarator-nofunc-prm[decl] ")" { $$ = $decl.append($ptr); }
  | d2-prm[decl] direct-declarator-elem[el]       { $$ = $decl.append($el); }
  ;
rule(<TDeclarator>,d2-prmnotyp):
    identifier[id] elem-nofunc[el]                { $$ = declarator(@id,$id).append($el); }
  | pident-prm[id] elem-nofunc[el]                { $$ = declarator(@id,$id).append($el); }
  | "(" pointer[ptr] direct-declarator-nofunc-prm[decl] ")" { $$ = $decl.append($ptr); }
  | d2-prmnotyp[decl] direct-declarator-elem[el]  { $$ = $decl.append($el); }
  ;

// (6.7.6)
rule(<TDeclarator.Elem>,direct-declarator-elem):
    elem-nofunc
  | elem-func
  ;

rule(<TDeclarator.Elem>,elem-nofunc):
    "[" type-qualifier-list_opt assignment-expression_opt[sz] "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list_opt],null,null,@sz,$sz); }
  | "[" STATIC type-qualifier-list_opt assignment-expression[sz] "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list_opt],@STATIC,null,@sz,$sz); }
  | "[" type-qualifier-list STATIC assignment-expression[sz] "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list],@STATIC,null,@sz,$sz); }
  | "[" type-qualifier-list_opt ASTERISK "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list_opt],null,@ASTERISK,null,null); }
  ;

rule(<TDeclarator.Elem>,elem-func):
    newfunc-decl
  | oldfunc-decl
  ;

rule(<TDeclarator.Elem>,oldfunc-decl):
    "(" PushParamScope identifier-list_opt ")"
        { popScope($PushParamScope); $$ = oldFuncDecl(@$, $[identifier-list_opt]); }
  ;
rule(<TDeclarator.Elem>,newfunc-decl):
    "(" PushParamScope parameter-type-list ")" { $$ = funcDecl(@$, popScope($PushParamScope)); }
  ;

// (6.7.6)
rule(<TDeclarator.Elem>,pointer,opt):
                  "*"[p] type-qualifier-list_opt  { $$ = pointerDecl(@p,$[type-qualifier-list_opt], null); }
  | pointer[left] "*"[p] type-qualifier-list_opt  { $$ = pointerDecl(@p,$[type-qualifier-list_opt], $left); }
  ;

// (6.7.6)
rule(<TSpecNode>,type-qualifier-list,opt):
    type-qualifier
  | type-qualifier-list[left] type-qualifier  { $$ = appendSpecNode($left, $[type-qualifier]); }
  ;

// (6.7.6)
parameter-type-list:
    parameter-list
  | parameter-list "," "..."            { ((ParamScope)topScope()).setEllipsis(); }
  ;

// (6.7.6)
parameter-list:
    parameter-declaration
  | parameter-list "," parameter-declaration
  ;

// (6.7.6)
parameter-declaration:
    declaration-specifiers-nots[spec] pointer direct-declarator-prm[decl]          { finishDeclarator($spec,$decl.append($pointer)); }
  | declaration-specifiers-ts[spec]   pointer direct-declarator-prm[decl]          { finishDeclarator($spec,$decl.append($pointer)); }
  | declaration-specifiers-nots[spec]         direct-declarator-prmnotyp[decl]     { finishDeclarator($spec,$decl); }
  | declaration-specifiers-ts[spec]           direct-declarator-prm[decl]          { finishDeclarator($spec,$decl); }
  | declaration-specifiers-nots[spec] pointer direct-abstract-declarator_opt[decl] { finishDeclarator($spec,$decl.append($pointer)); }
  | declaration-specifiers-ts[spec]   pointer direct-abstract-declarator_opt[decl] { finishDeclarator($spec,$decl.append($pointer)); }
  | declaration-specifiers-nots[spec]         direct-abstract-declarator_opt[decl] { finishDeclarator($spec,$decl); }
  | declaration-specifiers-ts[spec]           direct-abstract-declarator_opt[decl] { finishDeclarator($spec,$decl); }
  ;

/*
  In a identifier list (old-style parameter list)
  all but the first identifier cannot redefine a typedef.
  (6.9.1-6)
  (If the first one was a typedef then we would assume that this
  is a new style declaration).
*/
// (6.7.6)
rule(<TIdentList>,identifier-list,opt):
    identifier                                { $$ = identListAdd(@identifier, identList(), $identifier); }
  | identifier-list[list] "," any-identifier  { $$ = identListAdd(@[any-identifier], $list, $[any-identifier]); }
  ;

// (6.7.7)
rule(<TDeclaration>,type-name):
    specifier-qualifier-list[slist] abstract-declarator_opt
        { $$ = mkTypeName($[abstract-declarator_opt], $slist); }
  ;
  
// (6.7.7)
rule(<TDeclarator>,abstract-declarator):
    pointer                                     { $$ = abstractDeclarator(@pointer).append($pointer); }
  |             direct-abstract-declarator
  | pointer     direct-abstract-declarator      { $$ = $[direct-abstract-declarator].append($pointer); }
  ;

rule(<TDeclarator>,abstract-declarator_opt):
    abstract-declarator
  | %empty                                      { $$ = abstractDeclarator(yyloc); }
  ;

// (6.7.7)
rule(<TDeclarator>,direct-abstract-declarator):
    "(" abstract-declarator ")"                                      { $$ = $[abstract-declarator]; }
  | direct-abstract-declarator-elem[elem]                            { $$ = abstractDeclarator(@elem).append($elem); }
  | direct-abstract-declarator direct-abstract-declarator-elem[elem] { $$ = $1.append($elem); }
  ;

rule(<TDeclarator>,direct-abstract-declarator_opt):
    direct-abstract-declarator
  | %empty                                     { $$ = abstractDeclarator(yyloc); }
  ;

rule(<TDeclarator.Elem>,direct-abstract-declarator-elem):
    "[" type-qualifier-list assignment-expression_opt[sz] "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list],null,null,@sz,$sz); }
  | "[" assignment-expression_opt[sz] "]"
        { $$ = arrayDecl(@$,null,null,null,@sz,$sz); }
  | "[" STATIC type-qualifier-list_opt assignment-expression[sz] "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list_opt],@STATIC,null,@sz,$sz); }
  | "[" type-qualifier-list STATIC assignment-expression[sz] "]"
        { $$ = arrayDecl(@$,$[type-qualifier-list],@STATIC,null,@sz,$sz); }
  | "[" ASTERISK "]"
        { $$ = arrayDecl(@$,null,null,@ASTERISK,null,null); }
  | newfunc-decl
  | "(" ")"
        { $$ = oldFuncDecl(@$,null); }
  ;

// (6.7.9)
initializer:
    assignment-expression               { $$ = FIXME(); }
  | "{" initializer-list "}"            { $$ = FIXME(); }
  | "{" initializer-list "," "}"        { $$ = FIXME(); }
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
    "[" constant-expression "]"         { $$ = FIXME(); }
  | "." any-identifier                  { FIXME(); }
// GNU C extension
  | "[" constant-expression[ce1] "..." constant-expression[ce2] "]" { $$ = FIXME(); }
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
  | CASE constant-expression ":" statement      { $$ = FIXME(); }
  | DEFAULT ":" statement                       { $$ = ast("default", null, $statement); }
// GNU C Extension
  | CASE constant-expression[ce1] "..." constant-expression[ce2] ":" statement { $$ = FIXME(); }
  ;

// (6.8.2)
compound-statement:
    "{" PushBlockScope declare-labels-list_opt block-item-list_opt "}"
        { $$ = FIXME(); popScope($PushBlockScope); }
  ;

rule(<Scope>,PushBlockScope):
    %empty { $$ = pushBlockScope(); }

rule(<ParamScope>,PushParamScope):
    %empty { $$ = pushParamScope(); }

rule(<Scope>,PushAggScope):
    %empty { $$ = pushAggScope(); }

rule(<EnumScope>,PushEnumScope):
    %empty { $$ = pushEnumScope(); }

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
    expression_opt ";" { $$ = FIXME(); }
  ;

// (6.8.4)
selection-statement:
    IF "(" expression ")" statement                         %prec IF   { $$ = ast("if",null,$statement,null); }
  | IF "(" expression ")" statement[s1] ELSE statement[s2]  %prec ELSE { $$ = ast("if",null,$s1,$s2); }
  | SWITCH "(" expression ")" statement                                { $$ = ast("switch",null,$statement); }
  ;

// (6.8.5)
iteration-statement:
    WHILE "(" expression ")" statement           { $$ = ast("while",null,$statement); }
  | DO statement WHILE "(" expression ")" ";"    { $$ = ast("do",$statement,null); }
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
  | RETURN expression_opt ";" { $$ = ast("return", null ); }
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
rule(<TExpr.Expr>,primary-expression):
    identifier          { $$ = exprIdent(@1,$1); }
  | constant
  | string-literal      { $$ = exprStringLiteral($1); }
  | "(" expression[e] ")"  { $$ = $e; }
// GCC extension
  | "(" compound-statement ")" { FIXME(); }
  | generic-selection   { FIXME(); }
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
rule(<TExpr.Expr>,postfix-expression):
    primary-expression
  | postfix-expression "[" expression "]"
        { @2.end = @4.end; $$ = m_subscript.expr(@2,$1,$3); }
  | postfix-expression "(" argument-expression-list_opt ")"  { $$ = FIXME(); }
  | postfix-expression "." any-identifier                    { $$ = m_dotMember.expr(@2,$1,$3); }
  | postfix-expression "->" any-identifier                   { $$ = m_ptrMember.expr(@2,$1,$3); }
  | postfix-expression "++"                                  { $$ = m_postInc.expr(@2,$1); }
  | postfix-expression "--"                                  { $$ = m_postDec.expr(@2,$1); }
  | "(" type-name ")" "{" initializer-list "}"               { FIXME(); }
  | "(" type-name ")" "{" initializer-list "," "}"           { FIXME(); }
  ;

// (6.5.2)
argument-expression-list:
    assignment-expression
  | argument-expression-list "," assignment-expression
  ;

argument-expression-list_opt:
    %empty
  | argument-expression-list
  ;

// (6.5.3)
rule(<TExpr.Expr>,unary-expression):
    postfix-expression
  | "++" unary-expression               { $$ = m_preInc.expr(@1,$2); }
  | "--" unary-expression               { $$ = m_preDec.expr(@1,$2); }
  | "&" cast-expression                 { $$ = m_addr.expr(@1,$2); }
  | "*" cast-expression                 { $$ = m_indirect.expr(@1,$2); }
  | "+" cast-expression                 { $$ = m_uplus.expr(@1,$2); }
  | "-" cast-expression                 { $$ = m_uminus.expr(@1,$2); }
  | "~" cast-expression                 { $$ = m_bitwiseNot.expr(@1,$2); }
  | "!" cast-expression                 { $$ = m_logNeg.expr(@1,$2); }
  | SIZEOF unary-expression             { $$ = m_sizeOfExpr.expr(@1,$2); }
  | SIZEOF "(" type-name ")"            { $$ = m_sizeOfType.expr(@1,$3); }
  | _ALIGNOF "(" type-name ")"          { $$ = m_alignOfType.expr(@1,$3); }
// GNU C extension
  | GCC_ALIGNOF unary-expression
        {
          extWarning( @1, "'%s' is a GCC extension", $1.str );
          $$ = m_alignOfExpr.expr(@1,$2);
        }
  | GCC_ALIGNOF "(" type-name ")"
        {
          extWarning( @1, "'%s' is a GCC extension", $1.str );
          $$ = m_alignOfType.expr(@1,$3);
        }
  | "&&" any-identifier                 { FIXME(); }
  ;

// (6.5.4)
rule(<TExpr.Expr>,cast-expression):
    unary-expression
  | "(" type-name ")" cast-expression   { @1.end = @3.end; $$ = m_typecast.expr(@1,$2,$4); }
  ;

// (6.5.5)
rule(<TExpr.Expr>,multiplicative-expression):
    cast-expression
  | multiplicative-expression "*" cast-expression  { $$ = m_mul.expr(@2,$1,$3); }
  | multiplicative-expression "/" cast-expression  { $$ = m_div.expr(@2,$1,$3); }
  | multiplicative-expression "%" cast-expression  { $$ = m_remainder.expr(@2,$1,$3); }
  ;

// (6.5.6)
rule(<TExpr.Expr>,additive-expression):
    multiplicative-expression
  | additive-expression "+" multiplicative-expression { $$ = m_add.expr(@2,$1,$3); }
  | additive-expression "-" multiplicative-expression { $$ = m_sub.expr(@2,$1,$3); }
  ;

// (6.5.7)
rule(<TExpr.Expr>,shift-expression):
    additive-expression
  | shift-expression "<<" additive-expression        { $$ = m_lshift.expr(@2,$1,$3); }
  | shift-expression ">>" additive-expression        { $$ = m_rshift.expr(@2,$1,$3); }
  ;

// (6.5.8)
rule(<TExpr.Expr>,relational-expression):
    shift-expression
  | relational-expression "<" shift-expression       { $$ = m_lt.expr(@2,$1,$3); }
  | relational-expression ">" shift-expression       { $$ = m_gt.expr(@2,$1,$3); }
  | relational-expression "<=" shift-expression      { $$ = m_le.expr(@2,$1,$3); }
  | relational-expression ">=" shift-expression      { $$ = m_ge.expr(@2,$1,$3); }
  ;

// (6.5.9)
rule(<TExpr.Expr>,equality-expression):
    relational-expression
  | equality-expression "==" relational-expression   { $$ = m_eq.expr(@2,$1,$3); }
  | equality-expression "!=" relational-expression   { $$ = m_ne.expr(@2,$1,$3); }
  ;

// (6.5.10)
rule(<TExpr.Expr>,AND-expression):
    equality-expression
  | AND-expression "&" equality-expression           { $$ = m_bitwiseAnd.expr(@2,$1,$3); }
  ;

// (6.5.11)
rule(<TExpr.Expr>,exclusive-OR-expression):
    AND-expression
  | exclusive-OR-expression "^" AND-expression       { $$ = m_bitwiseXor.expr(@2,$1,$3); }
  ;

// (6.5.12)
rule(<TExpr.Expr>,inclusive-OR-expression):
    exclusive-OR-expression
  | inclusive-OR-expression "|" exclusive-OR-expression { $$ = m_bitwiseOr.expr(@2,$1,$3); }
  ;

// (6.5.13)
rule(<TExpr.Expr>,logical-AND-expression):
    inclusive-OR-expression
  | logical-AND-expression "&&" inclusive-OR-expression  { $$ = m_logAnd.expr(@2,$1,$3); }
  ;

// (6.5.14)
rule(<TExpr.Expr>,logical-OR-expression):
    logical-AND-expression
  | logical-OR-expression "||" logical-AND-expression    { $$ = m_logOr.expr(@2,$1,$3); }
  ;

// (6.5.15)
rule(<TExpr.Expr>,conditional-expression):
    logical-OR-expression
  | logical-OR-expression[e1] "?" expression[e2] ":" conditional-expression[e3] { $$ = FIXME(); }
  ;

// (6.5.16)
rule(<TExpr.Expr>,assignment-expression,opt):
    conditional-expression
  | unary-expression assignment-operator assignment-expression
  ;

// (6.5.16)
rule(<TreeCode>,assignment-operator):
    "="   { $$ = TreeCode.ASSIGN; }
  | "*="  { $$ = TreeCode.ASSIGN_MUL; }
  | "/="  { $$ = TreeCode.ASSIGN_DIV; }
  | "%="  { $$ = TreeCode.ASSIGN_REM; }
  | "+="  { $$ = TreeCode.ASSIGN_ADD; }
  | "-="  { $$ = TreeCode.ASSIGN_SUB; }
  | "<<=" { $$ = TreeCode.ASSIGN_LSHIFT; }
  | ">>=" { $$ = TreeCode.ASSIGN_RSHIFT; }
  | "&="  { $$ = TreeCode.ASSIGN_BITWISE_AND; }
  | "^="  { $$ = TreeCode.ASSIGN_BITWISE_XOR; }
  | "|="  { $$ = TreeCode.ASSIGN_BITWISE_OR; }
  ;

// (6.5.17)
rule(<TExpr.Expr>,expression,opt):
    assignment-expression
  | expression "," assignment-expression        { $$ = FIXME(); }
  ;

// (6.6)
rule(<TExpr.ArithConstant>,constant-expression):
    conditional-expression[e] { $$ = constantExpression(@e,$e); }
  ;

end_grammar

