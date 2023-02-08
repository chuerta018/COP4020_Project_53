package edu.ufl.cise.plcsp23;

public class StringLitToken implements IStringLitToken {

    final Kind kind;
    final int pos;
    final int length;
    final char[] source;
    final int row;
    final int col;

    //===== Constructor =====//
    public StringLitToken(Kind _kind, int _row, int _col, int _pos, int _length, char[] _source) {
        super(); // idk what this does tbh // ima watch lecture
        kind = _kind;
        row = _row;
        col = _col;
        pos = _pos;
        length = _length;
        source = _source;

    }


    public SourceLocation getSourceLocation() {
        //wrong I need to figure out how to get line, pos is the colomun
        return new SourceLocation(row, col);

    }

    public Kind getKind() {
        return kind;
    }

    public String getTokenString() {
        StringBuilder tokenString = new StringBuilder();
        int j = pos;
        for (int i = 0; i < length; i++) {
            tokenString.append(source[j]);
            j++;
        }

        return tokenString.toString();
    }

    @Override
    public String getValue() {
        StringBuilder tokenString = new StringBuilder();
        int j = pos;
        for (int i = 0; i < length - 1; i++) {
            if (source[j] != '"') // includes all escape squences just not "
            {
                if (source[j] == 92) {
                    switch (source[j + 1]) {
                        case 'b':
                            tokenString.append('\b');
                            j++;
                            break;
                        case 't':
                            tokenString.append('\t');
                            j++;
                            break;
                        case '"':
                            tokenString.append(source[j + 1]);
                            j++;
                            break;
                        case 92:
                            tokenString.append(source[j + 1]);
                            j++;
                            break;
                        case 'n':
                            tokenString.append('\n');
                            j++;
                            break;
                        case 'r':
                            tokenString.append('\r');
                            j++;
                            break;
                        default:
                            break; // IT SOULD NEVER REACH HERE HOPEFULLY ......
                    }
                } else {
                    tokenString.append(source[j]);
                }
            }
            j++;
        }
        return tokenString.toString();
    }
}

