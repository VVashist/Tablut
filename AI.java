package tablut;


import static tablut.Piece.*;

/** A Player that automatically generates moves.
 *  @author Vineet Vashist
 */
class AI extends Player {

    /** A position-score magnitude indicating a win (for white if positive,
     *  black if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;

    /** A position-score magnitude indicating a forced win in a subsequent
     *  move.  This differs from WINNING_VALUE to avoid putting off wins. */

    private static final int WILL_WIN_VALUE = Integer.MAX_VALUE - 40000;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI with no piece or controller (intended to produce
     *  a template). */
    AI() {
        this(null, null);
    }

    /** A new AI playing PIECE under control of CONTROLLER. */
    AI(Piece piece, Controller controller) {
        super(piece, controller);
    }

    @Override
    Player create(Piece piece, Controller controller) {
        return new AI(piece, controller);
    }

    @Override
    String myMove() {
        return findMove().toString();

    }

    @Override
    boolean isManual() {
        return false;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        foundMove = null;
        int sense = 0;

        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        Boolean saveMove = true;

        if (board().turn() == WHITE || board().turn() == KING) {
            sense = -1;

        } else {
            sense = 1;
        }

        findMove(b, 0, saveMove, sense, alpha, beta);

        return foundMove;

    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private static Move foundMove;

    /** Evaluate the maximizer function in the mimimax tree.
     * @param board  board
     * @param alpha value
     * @param beta value
     * @param depth of the board
     * @param saveMove boolean
     * @return final score*/
    private int findMax(Board board, int depth, boolean saveMove,
                        int alpha, int beta) {

        if (board.winner() != null) {
            return staticScore(board, board.turn());
        }
        Move amove = null;

        int tempVal = -WINNING_VALUE;
        for (Move m : board.legalMoves(WHITE)) {

            Board mvboard = new Board(board);
            mvboard.makeMove(m);

            int response;
            if (depth == maxDepth(mvboard)) {
                response = staticScore(mvboard, mvboard.turn());
            } else {
                response = findMin(mvboard, depth + 1, false, alpha, beta);
                if (response == WINNING_VALUE) {
                    response = (WILL_WIN_VALUE - (mvboard.moveCount() * 1000));
                }
            }

            mvboard.aIundo();

            if (response >= tempVal) {

                amove = m;
                tempVal = response;
                alpha = Math.max(alpha, response);
                if (beta <= alpha) {
                    break;
                }
            }
        }
        if (saveMove) {
            foundMove = amove;
        }
        return tempVal;
    }

    /** Evaluate the minimizer function in the mimimax tree.
     * @param board  board
     * @param alpha value
     * @param beta value
     * @param depth of the board
     * @param saveMove boolean
     * @return final score*/
    private int findMin(Board board, int depth, boolean saveMove,
                        int alpha, int beta) {
        if (board.winner() != null) {
            return staticScore(board, board.turn());
        }
        Move returnMove = null;
        int tempVal = WINNING_VALUE;
        for (Move m : board.legalMoves(BLACK)) {
            Board mvboard = new Board(board);
            mvboard.makeMove(m);

            int response;
            if (depth == maxDepth(board)) {
                response = staticScore(mvboard, mvboard.turn());
            } else {
                response = findMax(mvboard, depth + 1, false, alpha, beta);
                if (response == -WINNING_VALUE) {
                    response = (-WILL_WIN_VALUE + (mvboard.moveCount() * 1000));
                }
            }

            mvboard.aIundo();

            if (response <= tempVal) {
                returnMove = m;
                tempVal = response;
                beta = Math.min(beta, response);
                if (beta <= alpha) {
                    break;
                }
            }
        }
        if (saveMove) {
            foundMove = returnMove;
        }
        return tempVal;
    }
    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound. */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {

        int finalval = 0;

        if (sense == -1) {
            finalval = findMax(board, depth, saveMove, alpha, beta);
        } else if (sense == 1) {
            finalval = findMin(board, depth, saveMove, alpha, beta);
        }

        return finalval;
    }


    /** Return a heuristically determined maximum search depth
     *  based on characteristics of BOARD. */
    private static int maxDepth(Board board) {
        return 2;
    }


    /** Return a heuristic value for BOARD.
     * @param board : the complete board
     * @param turn the current turn.*/
    private int staticScore(Board board, Piece turn) {
        if (board.winner() == BLACK) {
            return -WINNING_VALUE;
        } else if (board.winner() == WHITE) {
            return WINNING_VALUE;
        }

        if (turn == WHITE || turn == KING) {
            return  evaluateWhite(board)
                    - (board.moveCount() * THIRTY) - evaluateBlack(board);
        } else {
            return evaluateBlack(board)
                    + (board.moveCount() * THIRTY) + evaluateWhite(board);
        }
    }

    /**
     * Evaluates the state of the board in the perspective of the
     * Swdede aka WHITE player.
     * @param b board
     * @return  int*/
    public int evaluateWhite(Board b) {
        int kingDistance = b.distanceToClosestCorner(b.kingPosition());
        int whiteSize = ((b.pieceLocations(WHITE).size()) * 4);
        int black = b.pieceLocations(BLACK).size();
        int diff = ((black * 2) - (whiteSize)) * 100;
        int inc = 0;

        if (b.unblockedcorner()) {
            inc = WILL_WIN_VALUE;
        }
        return ((9 - kingDistance) * 1000) + whiteSize  + diff + inc;
    }


    /**
     * Evaluates the state of the board in the perspective of the
     * Muscovite player.
     * Calculates the score of the board, by adding beneficial
     * elements for the Muscovite player.
     * The higher the score, the better the board is for the player.
     * @return a score reflecting the state of the board for
     * the Muscovite player.
     * @param b  : board .
     */
    public int evaluateBlack(Board b) {
        int sizeOfMuscovite = ((b.getPieces(BLACK)) * 2);
        int numKingNeighbourPieces =
                b.getNeighbourPieces(b.kingPosition(), Piece.BLACK).size();
        int diff = (sizeOfMuscovite - b.getPieces(WHITE) * 4) * 100;


        return -((100 * numKingNeighbourPieces) + diff);
    }
/** assigning weight to the movecount.*/
    private static final int THIRTY = 30;
}
