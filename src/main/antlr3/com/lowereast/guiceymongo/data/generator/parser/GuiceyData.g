grammar GuiceyData;

options {
	language=Java;
	output=AST;
	backtrack=true;
}

tokens {
	DATA='data';
	TYPE_MAP='map';
	TYPE_SET='set';
	TYPE_LIST='list';
	PARAMETERIZED_TYPE='parameterized-type';
	// primitive can be a user-defined Data type
	TYPE_PRIMITIVE='primitive';
	PROPERTY='property';
	OPTION='option';
	COMMENT='comment';
	PAIR='pair';
	ENUM='enum';
}

@header {
package com.lowereast.guiceymongo.data.generator.parser;
}
@lexer::header {
package com.lowereast.guiceymongo.data.generator.parser;
}

start
	:	entry+ EOF
	;

entry
	:	data
	|	enumeration
	;

data
	:	javadoc? DATA ID '{' data_entry* '}' -> ^(DATA ID javadoc? data_entry*)
	|	javadoc? DATA ID '{}' -> ^(DATA ID javadoc?)
	;

enumeration
	:	javadoc? ENUM ID '{}' -> ^(ENUM ID javadoc?)
	|	javadoc? ENUM ID '{' (ID ',')* ID '}' -> ^(ENUM ID+ javadoc?)
	;

data_entry
	:	entry
	|	javadoc? option* property -> ^(property javadoc? option*)
	;

javadoc
	:	'/*' comment '*/' -> ^(COMMENT comment)
	;

comment
	:	~( '*/' )*
	;

option
	:	'[' ID ']' -> ^(OPTION ID)
	|	'[' ID '(' value ')]' -> ^(OPTION ID value)
//	|	'[' ID '(' pair ')]' -> ^(OPTION ID pair)
	|	'[' ID '(' (pair ',')* pair ')]' -> ^(OPTION ID pair+)
	;
	
pair
	:	key '=' value -> ^(PAIR key value)
	;
	
key
	:	ID
	;
value
	:	INT
	|	FLOAT
	|	STRING
	;

property 
/*
	:	'map<' key_type ',' value_type '>' ID ';' -> ^(PROPERTY ID TYPE_MAP key_type value_type)
	|	'set<' type '>' ID ';' -> ^(PROPERTY ID TYPE_SET type)
	|	'list<' type '>' ID ';' -> ^( PROPERTY ID TYPE_LIST type)
	|	type 'data;' -> ^(PROPERTY 'data' TYPE_PRIMITIVE type)
	|	type ID ';' -> ^(PROPERTY ID TYPE_PRIMITIVE type)
*/
	:	type 'data;' -> ^(PROPERTY 'data' type)
	|	type ID ';' -> ^(PROPERTY ID type)
	;

key_type
	: type
	;

value_type
	: type
	;

type
	:	'map<' type ',' type '>' -> TYPE_MAP type+
	|	'set<' type '>' -> TYPE_SET type
	|	'list<' type '>' -> TYPE_LIST type
	|	TYPE -> TYPE_PRIMITIVE TYPE
	|	ID -> TYPE_PRIMITIVE ID
	|	ID '<' type (',' type)* '>' -> PARAMETERIZED_TYPE ID type+
	;

TYPE	:	ID ('.' ID)+
	;

ID 	:	('a'..'z'|'A'..'Z') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
	;

WS 	:   ( ' '
	| '\t'
        | '\r'
        | '\n'
        ) {$channel=HIDDEN;}
    ;

INT :	'0'..'9'+
    ;

FLOAT
    :   ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    |   '.' ('0'..'9')+ EXPONENT?
    |   ('0'..'9')+ EXPONENT
    ;

//COMMENT
//    :   '//' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
//    ;

STRING
    :  '\'' ( ESC_SEQ | ~('\\'|'\'') )* '\''
    ;

fragment
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UNICODE_ESC
    |   OCTAL_ESC
    ;

fragment
OCTAL_ESC
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;
