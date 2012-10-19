grammar Unordered;

options {
  language = Java;
  output = AST;
  ASTLabelType = CommonTree;
}

tokens {
  FILE;
  ASSIGNMENT;
}

@header {
  package org.rhq.plugins.antlrconfig.test.parsers;
  
  import org.rhq.plugins.antlrconfig.tokens.*;
}

@lexer::header {
  package org.rhq.plugins.antlrconfig.test.parsers;
}

fragment
LETTER : 'a'..'z' | 'A'..'Z' ;

fragment
DIGIT : '0'..'9' ;

WORD : LETTER (LETTER | DIGIT)* ;

EQUALS : '=' ;

VALUE : DIGIT+;

WS : (' ' | '\t')+ {$channel = HIDDEN;} ;

NL : '\r'? '\n' ;

file : (assignment | NL)* -> ^(FILE<Unordered> assignment*)
     ; 

assignment : WORD EQUALS VALUE (NL | EOF) -> ^(ASSIGNMENT ^(WORD<Id>) VALUE);
