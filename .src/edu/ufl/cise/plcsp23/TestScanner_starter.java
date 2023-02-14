/*Copyright 2023 by Beverly A Sanders
 *
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the
 * University of Florida during the spring semester 2023 as part of the course project.
 *
 * No other use is authorized.
 *
 * This code may not be posted on a public web site either during or after the course.
 */

package edu.ufl.cise.plcsp23;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import edu.ufl.cise.plcsp23.IToken.Kind;
import edu.ufl.cise.plcsp23.IToken.SourceLocation;

class TestScanner_starter {

	// makes it easy to turn output on and off (and less typing than
	// System.out.println)
	static final boolean VERBOSE = true;

	void show(Object obj) {
		if (VERBOSE) {
			System.out.println(obj);
		}
	}

	// check that this token has the expected kind
	void checkToken(Kind expectedKind, IToken t) {
		assertEquals(expectedKind, t.getKind());
	}

	void checkToken(Kind expectedKind, String expectedChars, SourceLocation expectedLocation, IToken t) {
		assertEquals(expectedKind, t.getKind());
		assertEquals(expectedChars, t.getTokenString());
		assertEquals(expectedLocation, t.getSourceLocation());
		;
	}

	void checkIdent(String expectedChars, IToken t) {
		checkToken(Kind.IDENT, t);
		assertEquals(expectedChars.intern(), t.getTokenString().intern());
		;
	}

	void checkString(String expectedValue, IToken t) {
		assertTrue(t instanceof IStringLitToken);
		assertEquals(expectedValue, ((IStringLitToken) t).getValue());
	}

	void checkString(String expectedChars, String expectedValue, SourceLocation expectedLocation, IToken t) {
		assertTrue(t instanceof IStringLitToken);
		assertEquals(expectedValue, ((IStringLitToken) t).getValue());
		assertEquals(expectedChars, t.getTokenString());
		assertEquals(expectedLocation, t.getSourceLocation());
	}

	void checkNUM_LIT(int expectedValue, IToken t) {
		checkToken(Kind.NUM_LIT, t);
		int value = ((INumLitToken) t).getValue();
		assertEquals(expectedValue, value);
	}

	void checkNUM_LIT(int expectedValue, SourceLocation expectedLocation, IToken t) {
		checkToken(Kind.NUM_LIT, t);
		int value = ((INumLitToken) t).getValue();
		assertEquals(expectedValue, value);
		assertEquals(expectedLocation, t.getSourceLocation());
	}

	void checkTokens(IScanner s, IToken.Kind... kinds) throws LexicalException {
		for (IToken.Kind kind : kinds) {
			checkToken(kind, s.next());
		}
	}

	void checkTokens(String input, IToken.Kind... kinds) throws LexicalException {
		IScanner s = CompilerComponentFactory.makeScanner(input);
		for (IToken.Kind kind : kinds) {
			checkToken(kind, s.next());
		}
	}

	// check that this token is the EOF token
	void checkEOF(IToken t) {
		checkToken(Kind.EOF, t);
	}


	@Test
	void emptyProg() throws LexicalException {
		String input = "";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkEOF(scanner.next());
	}

	@Test
	void onlyWhiteSpace() throws LexicalException {
		String input = " \t \r\n \f \n";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkEOF(scanner.next());
		checkEOF(scanner.next());  //repeated invocations of next after end reached should return EOF token
	}

	@Test
	void operators() throws LexicalException {
		String input = "ident ==plu+s ** _-_test";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.IDENT,"ident", new SourceLocation(1,1), scanner.next());
		checkToken(Kind.EQ, scanner.next());
		checkToken(Kind.IDENT,"plu", new SourceLocation(1,9), scanner.next());
		checkToken(Kind.PLUS, scanner.next());
		checkToken(Kind.IDENT,"s", new SourceLocation(1,13), scanner.next());
		checkToken(Kind.EXP, scanner.next());
		checkToken(Kind.IDENT,"_", new SourceLocation(1,18), scanner.next());
		checkToken(Kind.MINUS, scanner.next());
		checkToken(Kind.IDENT,"_test", new SourceLocation(1,20), scanner.next());
		checkEOF(scanner.next());
	}

	@Test
	void operators1() throws LexicalException {
		String input = "<+";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.LT, scanner.next());
		checkToken(Kind.PLUS, scanner.next());
		checkEOF(scanner.next());
	}

	@Test
	void numLits1() throws LexicalException {
		String input = """
				123
				05 240
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkNUM_LIT(123, scanner.next());
		checkNUM_LIT(0, scanner.next());
		checkNUM_LIT(5, scanner.next());
		checkNUM_LIT(240, scanner.next());
		checkEOF(scanner.next());
	}

	@Test
		//Too large should still throw LexicalException
	void numLitTooBig() throws LexicalException {
		String input = "999999999999999999999";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void numLit1() throws LexicalException {
		String input = """
				123456
					xy123 56
				
				  3-3=0
				1
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkNUM_LIT(123456, new SourceLocation(1, 1), scanner.next());
		checkIdent("xy123", scanner.next());
		checkNUM_LIT(56, new SourceLocation(2, 8), scanner.next());
		checkNUM_LIT(3, new SourceLocation(4, 3), scanner.next());
		checkToken(Kind.MINUS, "-", new SourceLocation(4, 4), scanner.next());
		checkNUM_LIT(3, new SourceLocation(4, 5), scanner.next());
		checkToken(Kind.ASSIGN, "=", new SourceLocation(4,6), scanner.next());
		checkNUM_LIT(0, new SourceLocation(4, 7), scanner.next());
		checkNUM_LIT(1, new SourceLocation(5, 1), scanner.next());
		checkEOF(scanner.next());
		checkEOF(scanner.next());
	}

	@Test
	void identsAndReserved() throws LexicalException {
		String input = """
				i0
				  i1  x ~~~2 spaces at beginning and after il
				y Y
				""";

		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.IDENT,"i0", new SourceLocation(1,1), scanner.next());
		checkToken(Kind.IDENT, "i1",new SourceLocation(2,3), scanner.next());
		checkToken(Kind.RES_x, "x", new SourceLocation(2,7), scanner.next());
		checkToken(Kind.RES_y, "y", new SourceLocation(3,1), scanner.next());
		checkToken(Kind.RES_Y, "Y", new SourceLocation(3,3), scanner.next());
		checkEOF(scanner.next());
	}


	@Test
	void operators0() throws LexicalException {
		String input = """
				==
				+
				/
				====
				=
				===
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.EQ, scanner.next());
		checkToken(Kind.PLUS, scanner.next());
		checkToken(Kind.DIV, scanner.next());
		checkToken(Kind.EQ, scanner.next());
		checkToken(Kind.EQ, scanner.next());
		checkToken(Kind.ASSIGN, scanner.next());
		checkToken(Kind.EQ, scanner.next());
		checkToken(Kind.ASSIGN, scanner.next());
		checkEOF(scanner.next());
	}


	@Test
	void stringLiterals1() throws LexicalException {
		String input = """
				"hello"
				"\t"
				"\\""
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString(input.substring(0, 7),"hello", new SourceLocation(1,1), scanner.next());
		checkString(input.substring(8, 11), "\t", new SourceLocation(2,1), scanner.next());
		checkString(input.substring(12, 16), "\"",  new SourceLocation(3,1), scanner.next());
		checkEOF(scanner.next());
	}


	@Test
	void illegalEscape() throws LexicalException {
		String input = """
				"\\t"
				"\\k"
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString("\"\\t\"","\t", new SourceLocation(1,1), scanner.next());
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void illegalLineTermInStringLiteral() throws LexicalException {
		String input = """
				"\\n"  ~ this one passes the escape sequence--it is OK
				"\n"   ~ this on passes the LF, it is illegal.
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString("\"\\n\"","\n", new SourceLocation(1,1), scanner.next());
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void lessThanGreaterThanExchange() throws LexicalException {
		String input = """
				<->>>>=
				<<=<
				""";
		checkTokens(input, Kind.EXCHANGE, Kind.GT, Kind.GT, Kind.GE, Kind.LT, Kind.LE, Kind.LT, Kind.EOF);
	}

	/** The Scanner should not backtrack so this input should throw an exception */
	@Test
	void incompleteExchangeThrowsException() throws LexicalException {
		String input = " <- ";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void illegalChar() throws LexicalException {
		String input = """
				abc
				@
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkIdent("abc", scanner.next());
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken t = scanner.next();
		});
	}

	//ADDITONAL TESTS HERE
	@Test
	public void testStringErrorEOF() throws LexicalException {
		String input = """
           "good"
           "test
   
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.STRING_LIT,scanner.next());
		Exception e = assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void testErrorOnSingleDoubleQuote() throws LexicalException {
		String input = "\"";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken token = scanner.next();
		});
	}

	@Test
	public void testStringErrorEscape() throws LexicalException {
		String input = """
                "good"
                "test \\n nesting \\h"
   
                """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.STRING_LIT, scanner.next());
		Exception e = assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	public void testIllegalEscape2() throws LexicalException
	{
		// Octal escapes are not processed in this language
		String input = """
        "\\123"
        """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		Exception e = assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void testIllegalUnicodeInIdent1() throws LexicalException
	{
		String input = "abc\uD83D\uDE02def"; // "Face with Tears of Joy" emoji
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.IDENT, scanner.next());
		Exception e = assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void andNothingButComments() throws LexicalException {
		String input = """
            ~jerry
            ~can
            ~move
            ~if
            ~he's
            ~not
            ~@#$%&#^%&@
            ~tired
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkEOF(scanner.next());
	}

	@Test
	void andNumLitsZeroes() throws LexicalException {
		String input = """
            000
            00
            001
            10 0
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkNUM_LIT(0, scanner.next());
		checkNUM_LIT(0, scanner.next());
		checkNUM_LIT(0, scanner.next());
		checkNUM_LIT(0, scanner.next());
		checkNUM_LIT(0, scanner.next());
		checkNUM_LIT(0, scanner.next());
		checkNUM_LIT(0, scanner.next());
		checkNUM_LIT(1, scanner.next());
		checkNUM_LIT(10, scanner.next());
		checkNUM_LIT(0, scanner.next());
		checkEOF(scanner.next());
	}

	@Test
	void andIdentsWithNumLits() throws LexicalException {
		String input = """
            0f0f0
            12if21
            12if 21
            00 if 12
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkNUM_LIT(0, scanner.next());
		checkToken(Kind.IDENT, "f0f0", new SourceLocation(1, 2), scanner.next());
		checkNUM_LIT(12, scanner.next());
		checkToken(Kind.IDENT, "if21", new SourceLocation(2, 3), scanner.next());
		checkNUM_LIT(12, scanner.next());
		checkToken(Kind.RES_if, "if", new SourceLocation(3, 3), scanner.next());
		checkNUM_LIT(21, scanner.next());
		checkNUM_LIT(0, scanner.next());
		checkNUM_LIT(0, scanner.next());
		checkToken(Kind.RES_if, "if", new SourceLocation(4, 4), scanner.next());
		checkNUM_LIT(12, scanner.next());
		checkEOF(scanner.next());
	}

	@Test
	void andOperators1() throws LexicalException {
		String input = """
            =&&
            *****
            ~====
            ||?:,|
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.ASSIGN, scanner.next());
		checkToken(Kind.AND, scanner.next());
		checkToken(Kind.EXP, scanner.next());
		checkToken(Kind.EXP, scanner.next());
		checkToken(Kind.TIMES, scanner.next());
		checkToken(Kind.OR, scanner.next());
		checkToken(Kind.QUESTION, scanner.next());
		checkToken(Kind.COLON, scanner.next());
		checkToken(Kind.COMMA, scanner.next());
		checkToken(Kind.BITOR, scanner.next());
		checkEOF(scanner.next());
	}

	@Test
	void andEmptyStrings() throws LexicalException {
		String input = """
            \"\"\"\"\"\"\"
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString("", scanner.next());
		checkString("", scanner.next());
		checkString("", scanner.next());
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void andOperators() throws LexicalException {
		String input = """
            =&&
            *****
            ~====
            ||?:,|
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.ASSIGN, scanner.next());
		checkToken(Kind.AND, scanner.next());
		checkToken(Kind.EXP, scanner.next());
		checkToken(Kind.EXP, scanner.next());
		checkToken(Kind.TIMES, scanner.next());
		checkToken(Kind.OR, scanner.next());
		checkToken(Kind.QUESTION, scanner.next());
		checkToken(Kind.COLON, scanner.next());
		checkToken(Kind.COMMA, scanner.next());
		checkToken(Kind.BITOR, scanner.next());
		checkEOF(scanner.next());
	}

	@Test
	void andMoreIllegalChars() throws LexicalException {
		String input1 = "hey! `";
		IScanner scanner1 = CompilerComponentFactory.makeScanner(input1);
		checkIdent("hey", scanner1.next());
		checkToken(Kind.BANG, scanner1.next());
		assertThrows(LexicalException.class, () -> {
			scanner1.next();
		});

		String input2 = "stop \\";
		IScanner scanner2 = CompilerComponentFactory.makeScanner(input2);
		checkIdent("stop", scanner2.next());
		assertThrows(LexicalException.class, () -> {
			scanner2.next();
		});

		String input3 = "noo '";
		IScanner scanner3 = CompilerComponentFactory.makeScanner(input3);
		checkIdent("noo", scanner3.next());
		assertThrows(LexicalException.class, () -> {
			scanner3.next();
		});


	}

	@Test
	void tripleQuote() throws LexicalException {
		String input = """
            "\""
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString("", scanner.next());
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void escapeToEmptyString() throws LexicalException {
		String input = """
            "\"
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString("\"\"", "", new SourceLocation(1, 1), scanner.next());
		checkEOF(scanner.next());
	}

	@Test
	void illegalEscape1() throws LexicalException {
		String input = """
            "\\"
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void stringTest() throws LexicalException {
		String input = """
				"\\""
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString(input.substring(0, 4), "\"", new SourceLocation(1, 1), scanner.next());
	}

	@Test
	void stringContainBSlash() throws LexicalException {
		String input = """
            "\\ abc"
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void stringContainBSlash2() throws LexicalException {
		String input = """
            "abc \\"
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void stringContainBSlash3() throws LexicalException {
		String input = """
            "abc \\""
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString("abc \"", scanner.next());
	}
	@Test
	void stringContainBSlash4() throws LexicalException {
		String input = """
            "abc \\""abc"
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString("abc \"", scanner.next());
		checkIdent("abc", scanner.next());
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
		checkEOF(scanner.next());
	}

	@Test
	void andIllegalCarriageReturn() throws LexicalException {
		String input = """
            "\\r" ~ legal
            "\r" ~ illegal
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString("\"\\r\"", "\r", new SourceLocation(1, 1), scanner.next());
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void stringWithQuotes() throws LexicalException {
		String input = """
                \"\\"Hello World\\"\"
                """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString("\"Hello World\"", scanner.next());
	}

	@Test
	void singleQuote() throws LexicalException {
		String input = """
            "   
            """;
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void allOperatorsAndSeparators() throws LexicalException {
		/*  Operators and Separators . | , | ? | : | ( | ) | < | > | [ | ] | { | } | = | == | <-> | <= |  >= | ! | & | && | | | || |
      + | - | * | ** | / | %   */
		String input = """
				. , ? : ( ) < > [ ] { } = == <-> <= >= ! & && | || + - * ** / %
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.DOT, ".", new SourceLocation(1, 1),scanner.next());
		checkToken(Kind.COMMA, scanner.next());
		checkToken(Kind.QUESTION, scanner.next());
		checkToken(Kind.COLON, scanner.next());
		checkToken(Kind.LPAREN, scanner.next());
		checkToken(Kind.RPAREN, scanner.next());
		checkToken(Kind.LT, scanner.next());
		checkToken(Kind.GT, scanner.next());
		checkToken(Kind.LSQUARE, scanner.next());
		checkToken(Kind.RSQUARE, scanner.next());
		checkToken(Kind.LCURLY, scanner.next());
		checkToken(Kind.RCURLY, scanner.next());
		checkToken(Kind.ASSIGN, scanner.next());
		checkToken(Kind.EQ, scanner.next());
		checkToken(Kind.EXCHANGE, scanner.next());
		checkToken(Kind.LE, scanner.next());
		checkToken(Kind.GE, scanner.next());
		checkToken(Kind.BANG, scanner.next());
		checkToken(Kind.BITAND, scanner.next());
		checkToken(Kind.AND, scanner.next());
		checkToken(Kind.BITOR, scanner.next());
		checkToken(Kind.OR, scanner.next());
		checkToken(Kind.PLUS, scanner.next());
		checkToken(Kind.MINUS, scanner.next());
		checkToken(Kind.TIMES, scanner.next());
		checkToken(Kind.EXP, scanner.next());
		checkToken(Kind.DIV, scanner.next());
		checkToken(Kind.MOD, "%", new SourceLocation(1, 63),scanner.next());
	}

	@Test
	void allReservedWords() throws LexicalException {
		/* reserved words: image | pixel | int | string | void | nil | load | display | write | x | y | a | r | X  | Y | Z |
          x_cart | y_cart | a_polar | r_polar | rand | sin | cos | atan  | if | while  */
		String input = """
				image pixel int string void nil load display write x y a r X Y Z x_cart y_cart a_polar r_polar rand sin cos atan if while
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.RES_image, "image", new SourceLocation(1, 1), scanner.next());
		checkToken(Kind.RES_pixel, scanner.next());
		checkToken(Kind.RES_int, scanner.next());
		checkToken(Kind.RES_string, scanner.next());
		checkToken(Kind.RES_void, scanner.next());
		checkToken(Kind.RES_nil, scanner.next());
		checkToken(Kind.RES_load, scanner.next());
		checkToken(Kind.RES_display, scanner.next());
		checkToken(Kind.RES_write, scanner.next());
		checkToken(Kind.RES_x, scanner.next());
		checkToken(Kind.RES_y, scanner.next());
		checkToken(Kind.RES_a, scanner.next());
		checkToken(Kind.RES_r, scanner.next());
		checkToken(Kind.RES_X, scanner.next());
		checkToken(Kind.RES_Y, scanner.next());
		checkToken(Kind.RES_Z, scanner.next());
		checkToken(Kind.RES_x_cart, scanner.next());
		checkToken(Kind.RES_y_cart, scanner.next());
		checkToken(Kind.RES_a_polar, scanner.next());
		checkToken(Kind.RES_r_polar, scanner.next());
		checkToken(Kind.RES_rand, scanner.next());
		checkToken(Kind.RES_sin, scanner.next());
		checkToken(Kind.RES_cos, scanner.next());
		checkToken(Kind.RES_atan, scanner.next());
		checkToken(Kind.RES_if, scanner.next());
		checkToken(Kind.RES_while, scanner.next());
	}

	@Test
	void doOperatorsSeparateTokens() throws LexicalException {
		String input = """
				doesthis+work.for-you?
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.IDENT,"doesthis", new SourceLocation(1,1), scanner.next());
		checkToken(Kind.PLUS, scanner.next());
		checkToken(Kind.IDENT,"work", new SourceLocation(1,10), scanner.next());
		checkToken(Kind.DOT, scanner.next());
		// for is NOT a reserved word, oddly enough
		checkToken(Kind.IDENT,"for", new SourceLocation(1,15), scanner.next());
		checkToken(Kind.MINUS, scanner.next());
		checkToken(Kind.IDENT,"you", new SourceLocation(1,19), scanner.next());
		checkToken(Kind.QUESTION, scanner.next());
	}

	@Test
	void reservedWordsWithAddedText() throws LexicalException {
		String input = """
				image imagee limage pixelx inty int stringz astring voida nill loadd load displayy ewrite write
				xx yy aa rr XX YY ZZ x_cartt y_cartt xa_polar r_polar randd sinn cosss atann iff whilee
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);

		checkToken(Kind.RES_image, scanner.next());
		checkToken(Kind.IDENT,"imagee", new SourceLocation(1,7), scanner.next());
		checkToken(Kind.IDENT,"limage", new SourceLocation(1,14), scanner.next());
		checkToken(Kind.IDENT,"pixelx", new SourceLocation(1,21), scanner.next());
		checkToken(Kind.IDENT,"inty", new SourceLocation(1,28), scanner.next());
		checkToken(Kind.RES_int, scanner.next());
		checkToken(Kind.IDENT,"stringz", new SourceLocation(1,37), scanner.next());
		checkToken(Kind.IDENT,"astring", new SourceLocation(1,45), scanner.next());
		checkToken(Kind.IDENT,"voida", new SourceLocation(1,53), scanner.next());
		checkToken(Kind.IDENT,"nill", new SourceLocation(1,59), scanner.next());
		checkToken(Kind.IDENT,"loadd", new SourceLocation(1,64), scanner.next());
		checkToken(Kind.RES_load, scanner.next());
		checkToken(Kind.IDENT,"displayy", new SourceLocation(1,75), scanner.next());
		checkToken(Kind.IDENT,"ewrite", new SourceLocation(1,84), scanner.next());
		checkToken(Kind.RES_write, scanner.next());
		checkToken(Kind.IDENT,"xx", new SourceLocation(2,1), scanner.next());
		checkToken(Kind.IDENT,"yy", new SourceLocation(2,4), scanner.next());
		checkToken(Kind.IDENT,"aa", new SourceLocation(2,7), scanner.next());
		checkToken(Kind.IDENT,"rr", new SourceLocation(2,10), scanner.next());
		checkToken(Kind.IDENT,"XX", new SourceLocation(2,13), scanner.next());
		checkToken(Kind.IDENT,"YY", new SourceLocation(2,16), scanner.next());
		checkToken(Kind.IDENT,"ZZ", new SourceLocation(2,19), scanner.next());
		checkToken(Kind.IDENT,"x_cartt", new SourceLocation(2,22), scanner.next());
		checkToken(Kind.IDENT,"y_cartt", new SourceLocation(2,30), scanner.next());
		checkToken(Kind.IDENT,"xa_polar", new SourceLocation(2,38), scanner.next());
		checkToken(Kind.RES_r_polar, scanner.next());
		checkToken(Kind.IDENT,"randd", new SourceLocation(2,55), scanner.next());
		checkToken(Kind.IDENT,"sinn", new SourceLocation(2,61), scanner.next());
		checkToken(Kind.IDENT,"cosss", new SourceLocation(2,66), scanner.next());
		checkToken(Kind.IDENT,"atann", new SourceLocation(2,72), scanner.next());
		checkToken(Kind.IDENT,"iff", new SourceLocation(2,78), scanner.next());
		checkToken(Kind.IDENT,"whilee", new SourceLocation(2,82), scanner.next());
	}

	@Test
	void reserved() throws LexicalException {
		String input = """
				if0 _while 
				display write
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkIdent("if0", scanner.next());
		checkIdent("_while", scanner.next());
		checkToken(Kind.RES_display, "display", new SourceLocation(2, 1), scanner.next());
		checkToken(Kind.RES_write, "write", new SourceLocation(2, 9), scanner.next());
		checkEOF(scanner.next());
	}

	@Test
	void operatorsPart2() throws LexicalException {
		// note: there is a whitespace at the end of rows 1 & 2
		String input = """
				&||&&&| 
				|*|||| 
				**%::?!
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkTokens(input, Kind.BITAND, Kind.OR, Kind.AND, Kind.BITAND, Kind.BITOR,
				Kind.BITOR, Kind.TIMES, Kind.OR, Kind.OR, Kind.EXP, Kind.MOD,
				Kind.COLON, Kind.COLON, Kind.QUESTION, Kind.BANG);
	}

	@Test
	void chaching() throws LexicalException {
		// this should throw an exception
		String input = "$$$";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken t = scanner.next();
		});
	}

	@Test
	void comments() throws LexicalException {
		String input = """
				~Look at me! I'm a comment!
				~ ~ ~
				~I said look at ME!
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkEOF(scanner.next());
	}

	@Test
	void num_lits_2() throws LexicalException {
		String input = "001122334455";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkNUM_LIT(0, scanner.next());
		checkNUM_LIT(0, scanner.next());
		checkNUM_LIT(1122334455, scanner.next());
	}

	@Test
	void the_ultimate_test() throws LexicalException {
		String input = """
				*&THE++
				"ULT_TIM0TE"
				->TEST.<-
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.TIMES, scanner.next());
		checkToken(Kind.BITAND, scanner.next());
		checkIdent("THE", scanner.next());
		checkToken(Kind.PLUS, scanner.next());
		checkToken(Kind.PLUS, scanner.next());
		checkString(input.substring(8, 20), "ULT_TIM0TE", new SourceLocation(2, 1), scanner.next());
		checkToken(Kind.MINUS, scanner.next());
		checkToken(Kind.GT, scanner.next());
		checkIdent("TEST", scanner.next());
		checkToken(Kind.DOT, scanner.next());
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void stringLiteralTest() throws LexicalException {
		String input = """
				\"\\"Hello World\\"\"
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString("\"Hello World\"", scanner.next());
	}

	@Test
	void operatorTest() throws LexicalException {
		String input = """
				<=<->===&&||
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkTokens(input, Kind.LE, Kind.EXCHANGE, Kind.EQ, Kind.ASSIGN, Kind.AND, Kind.OR);
	}

	@Test
	void codeTest() throws LexicalException {
		String input = """
				int foo = 10
				if(foo == 10)
				{
				     foo = foo + 20
				}
				
				while(foo & 0)
				{
				     foo = foo - 20
				}
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.RES_int,"int", new SourceLocation(1,1), scanner.next());
		checkToken(Kind.IDENT,"foo", new SourceLocation(1,5), scanner.next());
		checkToken(Kind.ASSIGN,"=", new SourceLocation(1,9), scanner.next());
		checkToken(Kind.NUM_LIT,"10", new SourceLocation(1,11), scanner.next());
		checkToken(Kind.RES_if,"if", new SourceLocation(2,1), scanner.next());
		checkToken(Kind.LPAREN,"(", new SourceLocation(2,3), scanner.next());
		checkToken(Kind.IDENT,"foo", new SourceLocation(2,4), scanner.next());
		checkToken(Kind.EQ,"==", new SourceLocation(2,8), scanner.next());
		checkToken(Kind.NUM_LIT,"10", new SourceLocation(2,11), scanner.next());
		checkToken(Kind.RPAREN,")", new SourceLocation(2,13), scanner.next());
		checkToken(Kind.LCURLY,"{", new SourceLocation(3,1), scanner.next());
		checkToken(Kind.IDENT,"foo", new SourceLocation(4,6), scanner.next());
		checkToken(Kind.ASSIGN,"=", new SourceLocation(4,10), scanner.next());
		checkToken(Kind.IDENT,"foo", new SourceLocation(4,12), scanner.next());
		checkToken(Kind.PLUS,"+", new SourceLocation(4,16), scanner.next());
		checkToken(Kind.NUM_LIT,"20", new SourceLocation(4,18), scanner.next());
		checkToken(Kind.RCURLY,"}", new SourceLocation(5,1), scanner.next());
		checkToken(Kind.RES_while,"while", new SourceLocation(7,1), scanner.next());
		checkToken(Kind.LPAREN,"(", new SourceLocation(7,6), scanner.next());
		checkToken(Kind.IDENT,"foo", new SourceLocation(7,7), scanner.next());
		checkToken(Kind.BITAND,"&", new SourceLocation(7,11), scanner.next());
		checkToken(Kind.NUM_LIT,"0", new SourceLocation(7,13), scanner.next()); //ERROR Here
		checkToken(Kind.RPAREN,")", new SourceLocation(7,14), scanner.next());
		checkToken(Kind.LCURLY,"{", new SourceLocation(8,1), scanner.next());
		checkToken(Kind.IDENT,"foo", new SourceLocation(9,6), scanner.next());
		checkToken(Kind.ASSIGN,"=", new SourceLocation(9,10), scanner.next());
		checkToken(Kind.IDENT,"foo", new SourceLocation(9,12), scanner.next());
		checkToken(Kind.MINUS,"-", new SourceLocation(9,16), scanner.next());
		checkToken(Kind.NUM_LIT,"20", new SourceLocation(9,18), scanner.next());
		checkToken(Kind.RCURLY,"}", new SourceLocation(10,1), scanner.next());
	}
}
