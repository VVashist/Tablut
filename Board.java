package tablut;


import java.util.HashSet;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Arrays;
import java.util.List;
import java.util.Formatter;

import static tablut.Move.ROOK_MOVES;
import static tablut.Piece.*;
import static tablut.Square.*;

/** The state of a Tablut Game.
 *  @author Vineet Vashist
 */
class Board {


    /** The number of squares on a side of the board. */
    static final int SIZE = 9;

    /** The throne (or castle) square and its four surrounding squares.. */
    static final Square THRONE = sq(4, 4),
        NTHRONE = sq(4, 5),
        STHRONE = sq(4, 3),
        WTHRONE = sq(3, 4),
        ETHRONE = sq(5, 4);

    /** Initial positions of attackers. */
    static final Square[] INITIAL_ATTACKERS = {
        sq(0, 3), sq(0, 4), sq(0, 5), sq(1, 4),
        sq(8, 3), sq(8, 4), sq(8, 5), sq(7, 4),
        sq(3, 0), sq(4, 0), sq(5, 0), sq(4, 1),
        sq(3, 8), sq(4, 8), sq(5, 8), sq(4, 7)
    };

    /** Initial positions of defenders of the king. */
    static final Square[] INITIAL_DEFENDERS = {
        NTHRONE, ETHRONE, STHRONE, WTHRONE,
        sq(4, 6), sq(4, 2), sq(2, 4), sq(6, 4)
    };
    /** List of all the hostile squares on the board.*/
    static final ArrayList<Square> HOSTILE_SQUARES =
            new ArrayList<Square>(
                    Arrays.asList(THRONE, NTHRONE, ETHRONE, STHRONE, WTHRONE));

    /** Initializes a game board with SIZE squares on a side in the
     *  initial position. */
    Board() {
        init();
    }

    /** Initializes a copy of MODEL. */
    Board(Board model) {
        copy(model);
    }

    /** Copies MODEL into me. */
    void copy(Board model) {
        if (model == this) {
            return;
        }

        squares = new Square[SIZE][SIZE];
        pieces = new Piece[SIZE][SIZE];

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                squares[i][j] = model.squares[i][j];
                pieces[i][j] = model.pieces[i][j];
            }
        }
        this._moveCount = model._moveCount;
        this._turn = model._turn;
        this._winner = model._winner;
        this._movelimit = model._movelimit;
        this.movelist = model.movelist;
        this._repeated = model._repeated;
        this.trackUndostr = model.trackUndostr;
        this.trackUndo = model.trackUndo;

    }

    /** Clears the board to the initial position. */
    void init() {
        squares = new Square[SIZE][SIZE];
        pieces = new Piece[SIZE][SIZE];
        _turn = BLACK;
        _winner = null;
        _moveCount = 0;
        _movelimit = -1;

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                squares[i][j] = sq(i, j);

                if (squares[i][j] == THRONE) {
                    pieces[i][j] = KING;
                } else {
                    for (Square s : INITIAL_ATTACKERS) {
                        if (squares[i][j] == s) {
                            pieces[i][j] = BLACK;
                        }
                    }
                    for (Square s : INITIAL_DEFENDERS) {
                        if (squares[i][j] == s) {
                            pieces[i][j] = WHITE;
                        }
                    }
                    if (pieces[i][j] == null) {
                        pieces[i][j] = EMPTY;
                    }
                }
            }

        }
    }

    /** Set the move limit to LIM.  It is an error if 2*LIM <= moveCount().
     * @param n is the limit we want to set the game to. */
    void setMoveLimit(int n) {
        if (2 * n <= moveCount()) {
            throw new IllegalArgumentException("Exceeded move limit!");
        }
        _movelimit = n;
    }

    /** Return a Piece representing whose move it is (WHITE or BLACK). */
    Piece turn() {
        return _turn;
    }

    /** Return the winner in the current position, or null if there is no winner
     *  yet. */
    Piece winner() {
        return _winner;
    }

    /** Returns true iff this is a win due to a repeated position. */
    boolean repeatedPosition() {
        checkRepeated();
        return _repeated;
    }

    /** Record current position and set winner() next mover if the current
     *  position is a repeat. */
    private void checkRepeated() {
        if (trackUndostr.contains(this.encodedBoard())) {
            _winner = turn();
            _repeated = true;
        }

    }

    /** Return the number of moves since the initial position that have not been
     *  undone. */
    int moveCount() {
        return _moveCount;
    }

    /** Return location of the king. */
    Square kingPosition() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (pieces[i][j] == KING) {
                    return squares[i][j];
                }
            }
        }
        return null;
    }

    /**Function to check of the game is over and find the winner.*/
    private void myWinner() {
        if (kingPosition() == null) {
            _winner = BLACK;
        } else if (legalMoves(turn()).size() == 0) {
            _winner = turn().opponent();
        } else {
            if (kingPosition().isEdge()) {
                _winner = WHITE;
            }
        }
    }
    /** Return the contents the square at S. */
    final Piece get(Square s) {
        return get(s.col(), s.row());
    }

    /** Return the contents of the square at (COL, ROW), where
     *  0 <= COL, ROW <= 9. */
    final Piece get(int col, int row) {
        if ((0 > col && col > 9) || (0 > row && row > 9)) {
            throw new IllegalArgumentException("Incorrect no. of row and col.");
        }
        return pieces[col][row];
    }

    /** Return the contents of the square at COL ROW. */
    final Piece get(char col, char row) {
        return get(row - '1', col - 'a');
    }

    /** Set square S to P and record for undoing. */
    final void put(Piece p, Square s) {

        pieces[s.col()][s.row()] = p;
    }


    /** Set square S to P. */
    final void revPut(Piece p, Square s) {
        pieces[s.col()][s.row()] = p;
    }

    /** Set square COL ROW to P. */
    final void put(Piece p, char col, char row) {
        put(p, sq(col - 'a', row - '1'));
    }


    /** Return true iff FROM - TO is an unblocked rook move on the current
     *  board.  For this to be true, FROM-TO must be a rook move and the
     *  squares along it, other than FROM, must be empty. */
    boolean isUnblockedMove(Square from, Square to) {
        int direction = from.direction(to);
        Square tempmove = from;
        int i = 1;

        while (tempmove.rookMove(direction, i) != to.rookMove(direction, i)) {
            Square atemp = tempmove.rookMove(direction, i);
            if (pieces[atemp.col()][atemp.row()] == EMPTY) {
                tempmove = atemp;
            } else {
                return false;
            }
        }

        if (tempmove != to) {
            return false;
        }
        return true;
    }

    /** Return true iff FROM is a valid starting square for a move. */
    boolean isLegal(Square from) {
        Boolean A = get(from) == _turn;
        Boolean B = (get(from) == KING && _turn == WHITE);
        return A || B;
    }

    /** Return true iff FROM-TO is a valid move. */
    boolean isLegal(Square from, Square to) {
        if (from.isRookMove(to)) {
            if (isUnblockedMove(from, to) && isLegal(from)
                    && (!(to == THRONE && get(from) != KING))) {
                return true;
            }
        }
        return false;
    }

    /** Return true iff MOVE is a legal move in the current
     *  position. */
    boolean isLegal(Move move) {
        return isLegal(move.from(), move.to());
    }

    /** Move FROM-TO, assuming this is a legal move. */
    void makeMove(Square from, Square to) {
        assert isLegal(from, to);

        if (trackUndo.empty()) {
            Board prevBoard = new Board();
            trackUndo.push(prevBoard);
            trackUndostr.add(prevBoard.encodedBoard());

        } else {
            Board prevBoard = new Board(this);
            trackUndo.push(prevBoard);
            trackUndostr.add(prevBoard.encodedBoard());
        }

        Piece temp = pieces[from.col()][from.row()];

        put(temp, to);
        put(EMPTY, from);

        findAllCaptures(to);

        _turn = this.turn().opponent();

        _moveCount++;

        repeatedPosition();
    }

    /** Move according to MOVE, assuming it is a legal move. */
    void makeMove(Move move) {
        if (checkMovelimit()) {
            makeMove(move.from(), move.to());
            myWinner();
        }
    }

    /** Check the move limit for the current.
     * @return the boolean.*/
    private Boolean checkMovelimit() {
        if (_movelimit == -1) {
            return true;
        } else if (_movelimit > 0 && moveCount() <= 2 * _movelimit) {
            return true;
        }
        return false;
    }

    /** Capture the piece between SQ0 and SQ2, assuming a piece just moved to
     *  SQ0 and the necessary conditions are satisfied. */
    private void capture(Square sq0, Square sq2) {
        Square die = sq0.between(sq2);
        pieces[die.col()][die.row()] = EMPTY;
    }

    /** Function to find all the captured moves.
     * @param to   : moved to the "to" square. */
    void findAllCaptures(Square to) {

        for (int dir = 0; dir < 4; dir++) {
            Square oneafter = to.rookMove(dir, 2);
            Square adjacent = to.rookMove(dir, 1);
            if (oneafter != null) {
                if (get(adjacent) == get(to).opponent()) {
                    if (get(to) == get(oneafter)
                            || (get(oneafter) == EMPTY && oneafter == THRONE)) {
                        capture(to, oneafter);
                    }
                }
            }
        }

        throneCornerCase(to);
        kingcapture(to);
    }

    /** Function to the check for the corner case for capturing
     * awhite when king is on the throne surrounded by 3 blacks.
     * @param to after moving to the "to" square.*/

    private void throneCornerCase(Square to) {
        int counter = 0;

        if (KING == get(THRONE)) {
            for (int dir = 0; dir < 4; dir++) {
                Square kingadj = THRONE.rookMove(dir, 1);
                if (get(kingadj) == BLACK) {
                    counter++;
                }
            }
            if (counter == 3) {
                if (THRONE.isRookMove(to)) {
                    int mydir = THRONE.direction(to);
                    Square orthagonal = THRONE.rookMove(mydir, 2);
                    Square adjacent = THRONE.rookMove(mydir, 1);

                    if (get(to) == get(orthagonal) && get(to) == BLACK
                            && get(adjacent) == WHITE) {
                        capture(to, THRONE);

                    }
                }
            }
        }
    }

    /** Function to check if the  king is being captured after a move.
     * @param to  : moved to the "to" square. */
    private void kingcapture(Square to) {
        int counter = 0;
        Boolean found = false;

        Square kingPos = kingPosition();

        for (int dir = 0; dir < 4; dir++) {

            Square kingadj = kingPos.rookMove(dir, 1);
            if (kingadj != null) {
                if (get(kingadj) == BLACK) {
                    counter++;
                }
            }
        }

        if (to.isRookMove(kingPos)) {
            int mydir = to.direction(kingPos);
            Square orthagonal = to.rookMove(mydir, 2);

            if (orthagonal != null) {
                if (HOSTILE_SQUARES.contains(kingPos)) {
                    found = true;
                    for (Square ts : HOSTILE_SQUARES) {
                        if (get(ts) == KING) {
                            if (ts == THRONE) {
                                if (counter == 4) {
                                    capture(to, orthagonal);
                                    break;
                                }
                            } else if (counter == 3) {
                                capture(to, orthagonal);

                                break;
                            }
                        }
                    }

                    if (get(to) == BLACK && !found && counter >= 2) {
                        Square adjacent = to.rookMove(mydir, 1);
                        if (get(adjacent) == KING
                                && get(to) == get(orthagonal)) {
                            capture(to, orthagonal);

                        }
                    }
                }
            }
        }
    }

    /** Undo one move.  Has no effect on the initial board. */
    void undo() {
        if (_moveCount > 0) {
            undoPosition();
        }
    }

    /** Remove record of current position in the set of positions encountered,
     *  unless it is a repeated position or we are at the first move. */
    private void undoPosition() {
        Board tempboard = trackUndo.pop();
        trackUndostr.remove(tempboard.encodedBoard());

        this._moveCount = tempboard._moveCount;
        this._turn = tempboard._turn;
        this._winner = tempboard._winner;
        this.pieces = tempboard.pieces;
        this._movelimit = tempboard._movelimit;
        this.movelist = tempboard.movelist;
        this.squares = tempboard.squares;

        _repeated = false;
    }

    /** Clear the undo stack and board-position counts. Does not modify the
     *  current position or win status. */
    void clearUndo() {

        trackUndostr.clear();
        trackUndo.removeAllElements();

    }

    /** Undo used for the AI while checking for moves.*/
    void aIundo() {
        Board tempboard = trackUndo.pop();
        trackUndostr.remove(tempboard.encodedBoard());
        _moveCount--;
    }

    /** Return a new mutable list of all legal moves on the current board for
     *  SIDE (ignoring whose turn it is at the moment). */
    List<Move> legalMoves(Piece side) {
        movelist = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                int dir = 0;
                if (pieces[i][j] == side
                        || (side == WHITE && pieces[i][j] == KING)) {
                    while (dir < 4) {
                        for (Move m : ROOK_MOVES[squares[i][j].index()][dir]) {
                            if (m.from().isRookMove(m.to())
                                    && isUnblockedMove(m.from(), m.to())) {
                                if (!(m.to() == THRONE)) {
                                    movelist.add(m);
                                }
                            }
                        }
                        dir++;
                    }
                }
            }
        }
        return movelist;
    }

    /** Return true iff SIDE has a legal move. */
    boolean hasMove(Piece side) {

        List<Move> mymovelist = legalMoves(side);
        if (mymovelist.size() <= 0) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /** Return a text representation of this Board.  If COORDINATES, then row
     *  and column designations are included along the left and bottom sides.
     */
    String toString(boolean coordinates) {
        Formatter out = new Formatter();
        for (int r = SIZE - 1; r >= 0; r -= 1) {
            if (coordinates) {
                out.format("%2d", r + 1);
            } else {
                out.format("  ");
            }
            for (int c = 0; c < SIZE; c += 1) {
                out.format(" %s", get(c, r));
            }
            out.format("%n");
        }
        if (coordinates) {
            out.format("  ");
            for (char c = 'a'; c <= 'i'; c += 1) {
                out.format(" %c", c);
            }
            out.format("%n");
        }
        return out.toString();
    }

    /** Return the locations of all pieces on SIDE. */
    public HashSet<Square> pieceLocations(Piece side) {
        assert side != EMPTY;
        HashSet<Square> piecesLocation = new HashSet<>();

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (pieces[i][j] == side
                        || (side == WHITE && pieces[i][j] == KING)) {
                    piecesLocation.add(squares[i][j]);
                }
            }
        }
        return piecesLocation;
    }

    /** Return the contents of _board in the order of SQUARE_LIST as a sequence
     *  of characters: the toString values of the current turn and Pieces. */
    String encodedBoard() {
        char[] result = new char[Square.SQUARE_LIST.size() + 1];
        result[0] = turn().toString().charAt(0);
        for (Square sq : SQUARE_LIST) {
            result[sq.index() + 1] = get(sq).toString().charAt(0);
        }
        return new String(result);

    }

    /** Given a coordinate, returns the distance between the SQAURE
     *  and the closest corner.
     *  @param kingPos position of the king.*/

    public int distanceToClosestCorner(Square kingPos) {
        return Math.min((SIZE - kingPos.col()), (SIZE - kingPos.row()));
    }

    /** Distance between two squares.
     * @param to : move to.
     * @param from  : move from
     * @return all the corners*/
    public int distance(Square from, Square to) {
        return Math.abs(from.col() - to.col())
                +  Math.abs(from.row() - to.row());
    }

    /** List of the all the corner squares.
     * @return : returns the corner positions/*/
    public List<Square> getCorners() {
        ArrayList<Square> corners = new ArrayList<Square>();
        for (int i = 0; i < SIZE; i++) {
            corners.add(squares[i][0]);
            corners.add(squares[i][8]);
            corners.add(squares[0][i]);
            corners.add(squares[8][i]);

        }
        return corners;
    }
/** Boolena Function to find is the king has any unblocked corner space.
 * @return  Boolean */
    public boolean unblockedcorner() {
        Square kinpos =  kingPosition();
        if (kinpos != null) {
            for (Square corner : getCorners()) {
                if (isLegal(kingPosition(), corner)) {
                    return true;
                }
            }
        }

        return false;
    }
    /**.
     * Finds the coordinates of the neighbours of a piece.
     * @param position of the piece whose neighbours we want to find
     * @param colour the colour of the neighbours we are looking for
     * @return the list of neighbours.
     */
    public List<Square> getNeighbourPieces(Square position, Piece colour) {
        List<Square> neighbours = new ArrayList<>();

        for (int dir = 0; dir < 4; dir++) {
            Square adj = position.rookMove(dir, 1);
            if (get(adj) == colour && adj != null) {
                neighbours.add(adj);
            }
        }
        return neighbours;
    }

    /** Returns the current size of the stack. */
    public void stacksize() {
        System.out.println("Current undo stack size : " + trackUndo.size());
    }

    /** Function to return the no. of pieces of the SIDE on the board.
     * @param turn turn of the current player on board.*/
    public int getPieces(Piece turn) {
        int black = 0;
        int white = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (turn == BLACK) {
                    black += 1;
                } else if (turn == WHITE || turn == KING) {
                    white += 1;
                }
            }
        }
        if (turn == BLACK) {
            return black;
        } else if (turn == WHITE) {
            return white;
        }
        return 0;
    }


    /** Piece whose turn it is (WHITE or BLACK). */
    private Piece _turn;

    /** Cached value of winner on this board, or EMPTY if it has not been
     *  computed. */

    private Piece _winner;
    /** Number of (still undone) moves since initial position. */
    private int _moveCount;

    /** Sets the no. of moves you can make in the game. */
    private int _movelimit;

    /** True when current board is a repeated position (ending the game). */
    private boolean _repeated;

    /** 2D array containing each piece on the board. */
    private Piece[][] pieces;

    /** 2D array containing each square on the board. */
    private Square[][] squares;

    /** Stack containing the board state for undo and repeated checks. */
    private Stack<Board> trackUndo = new Stack<Board>();

    /** Hashset containing the encoded board state . */
    private  HashSet<String> trackUndostr = new HashSet<String>();

    /** List of moves. */
    private List<Move> movelist;

}
