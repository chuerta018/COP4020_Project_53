package edu.ufl.cise.plcsp23;

public class Token {
	// Record to represent the location in the source code
	public record SourceLocation(int line, int column) {}
	public static enum Kind {
	  IDENT,
	  NUM_LIT,
	  PLUS,
	  TIMES,
	  EQ,
	  KW_IF,
	  KW_ELSE,
	  EOF,
	  ERROR //may be useful
	}
	final Kind kind;
	final int pos;
	final int length;
	final char[] source;
	
	//constructor initializes final fields
	public Token(Kind kind, int pos, int length, char[] source) {
	  super();
	  this.kind = kind;
	  this.pos = pos;
	  this.length = length;
	  this.source = source;
	}
	public SourceLocation getSourceLocation() {...}
	public Kind getKind() {return kind;}
	//returns the characters from the source belonging to the token
	public String getTokenString() {...}  
	//prints token, used during development
	@Override  public String toString() {...}
	
}
