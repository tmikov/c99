%language "Java"

%define package "c99.parser"
%define public
%define parser_class_name {CParser}

%define parse.error verbose

%locations

%token IDENT
%token INT_NUMBER
%token REAL_NUMBER
%token CHAR_CONST
%token WIDE_CHAR_CONST // uUL
%token STRING_CONST
%token WIDE_STRING_CONST // u8 u U L

%token L_BRACKET R_BRACKET
%token L_PAREN R_PAREN
%token L_CURLY R_CURLY
%token FULLSTOP
%token MINUS_GREATER

%token PLUS_PLUS
%token MINUS_MINUS
%token AMPERSAND
%token ASTERISK
%token PLUS
%token MINUS
%token TILDE
%token BANG

%token SLASH
%token PERCENT
%token LESS_LESS
%token GREATER_GREATER
%token LESS
%token GREATER
%token LESS_EQUALS
%token GREATER_EQUALS
%token EQUALS_EQUALS
%token BANG_EQUALS
%token CARET
%token VERTICAL
%token AMPERSAND_AMPERSAND
%token VERTICAL_VERTICAL

%token QUESTION
%token COLON
%token SEMICOLON
%token ELLIPSIS

%token EQUALS
%token ASTERISK_EQUALS
%token SLASH_EQUALS
%token PERCENT_EQUALS
%token PLUS_EQUALS
%token MINUS_EQUALS
%token LESS_LESS_EQUALS
%token GREATER_GREATER_EQUALS
%token AMPERSAND_EQUALS
%token CARET_EQUALS
%token VERTICAL_EQUALS

%token COMMA

// Parser-only
%token AUTO
%token BREAK
%token CASE
%token CHAR
%token CONST
%token CONTINUE
%token DEFAULT
%token DO
%token DOUBLE
%token ELSE
%token ENUM
%token EXTERN
%token FLOAT
%token FOR
%token GOTO
%token IF
%token INLINE
%token INT
%token LONG
%token REGISTER
%token RESTRICT
%token RETURN
%token SHORT
%token SIGNED
%token SIZEOF
%token STATIC
%token STRUCT
%token SWITCH
%token TYPEDEF
%token UNION
%token UNSIGNED
%token VOID
%token VOLATILE
%token WHILE
%token _ALIGNAS
%token _ALIGNOF
%token _ATOMIC
%token _BOOL
%token _COMPLEX
%token _GENERIC
%token _IMAGINARY
%token _NORETURN
%token _STATIC_ASSERT
%token _THREAD_LOCAL

%%

program : INT_NUMBER INT_NUMBER IF ;