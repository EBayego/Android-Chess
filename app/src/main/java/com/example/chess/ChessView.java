package com.example.chess;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Paint;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChessView extends View {
    private final float scale = .95f;
    private float originX = 0f, originY = 0f, squareSize = 0f, canvasWidth, canvasHeight;
    private final List<Integer> imgIds = Arrays.asList(
            R.drawable.rookblack,
            R.drawable.rookwhite,
            R.drawable.knightblack,
            R.drawable.knightwhite,
            R.drawable.bishopblack,
            R.drawable.bishopwhite,
            R.drawable.kingblack,
            R.drawable.kingwhite,
            R.drawable.queenblack,
            R.drawable.queenwhite,
            R.drawable.pawnblack,
            R.drawable.pawnwhite,
            R.drawable.kingwhitecheck,
            R.drawable.kingblackcheck);
    private HashMap<Integer, Bitmap> bitmaps;
    private Paint paint;
    private Board board;
    private int actualRow, actualColumn, finalRow, finalColumn;
    private boolean whiteTurn;
    private View.OnClickListener onClickListener;
    private int enPassantRow, enPassantColumn;
    private boolean enPassantMove;
    private MediaPlayer moveMP, eatMP, checkMP, checkMateMP, startGameMP, castleMP;
    private boolean kingChecked, checkMate, restartGame;
    private List<Integer> rowsCheckList, columnsCheckList;
    private List<Piece> piecesChecking;
    private List<Integer> rowsXRayList, columnsXRayList;
    private boolean blackKingFirstMove, whiteKingFirstMove;
    private boolean blackSortRookFirstMove, blackLongRookFirstMove, whiteSortRookFirstMove, whiteLongRookFirstMove;
    private CountDownTimer timerBlack, timerWhite;
    private String timeBlackStr, timeWhiteStr;
    private long timeBlack, timeWhite;
    private final long matchTime = 300999; //5 minutes
    private boolean saveWhiteTime, saveBlackTime;

    public ChessView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if ((canvas != null && squareSize == 0f) || restartGame) { // init game
            float boardSize = ((canvas.getWidth() <= canvas.getHeight()) ? canvas.getWidth() : canvas.getHeight()) * scale;
            squareSize = boardSize / 8f;
            originX = (canvas.getWidth() - boardSize) / 2f;
            originY = (canvas.getHeight() - boardSize) / 2f;
            canvasHeight = canvas.getHeight();
            canvasWidth = canvas.getWidth();
            initVariables();
        }
        paint.setColor(getResources().getColor(R.color.darkMode)); // on invalidate reDraw everything
        canvas.drawPaint(paint);
        initBoard(canvas);
        initPieces(canvas);
        //switchText(canvas);
        printTime(canvas);
        if (checkMate) {
            timerBlack.cancel();
            timerWhite.cancel();
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.new_game_button),
                    null,
                    new RectF(
                            originX + squareSize * 2.25f,
                            originY * 3.8f,
                            originX + squareSize * 2.25f + squareSize * 3.5f,
                            originY * 3.8f + squareSize * .75f
                    ),
                    paint);
        }
    }

    /**
     * Prints the time left.
     */
    private void printTime(Canvas canvas) {
        if (!"".equals(timeBlackStr)) {
            paint.setTextSize(90);
            paint.setUnderlineText(false);
            paint.setColor(getResources().getColor(R.color.white));
            canvas.drawText(timeBlackStr, canvasWidth - (2.5f * squareSize), originY - squareSize * 2.25f, paint);
            timeBlackStr = "";
        }
        if (!"".equals(timeWhiteStr)) {
            paint.setTextSize(90);
            paint.setUnderlineText(false);
            paint.setColor(getResources().getColor(R.color.white));
            canvas.drawText(timeWhiteStr, originX, canvasHeight - squareSize / 2, paint);
            timeWhiteStr = "";
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (MotionEvent.ACTION_DOWN == event.getAction()) {
            actualRow = (int) (((event.getY() - originY) / squareSize) + 1);
            actualColumn = (int) (((event.getX() - originX) / squareSize) + 1);
        }
        if (MotionEvent.ACTION_UP == event.getAction()) {
            finalRow = (int) (((event.getY() - originY) / squareSize) + 1);
            finalColumn = (int) (((event.getX() - originX) / squareSize) + 1);
            if (actualRow != finalRow || actualColumn != finalColumn)
                movePiece(actualRow, actualColumn, finalRow, finalColumn);
            if (checkMate) {
                if (event.getX() >= (canvasWidth * 311f / 1080f) && event.getX() <= (canvasWidth * 760f / 1080f) && event.getY() >= (canvasHeight * 1602f / 1868f) && event.getY() <= (canvasHeight * 1694f / 1868f)) {
                    restartGame = true;
                    ChessView chessView = (ChessView) findViewById(R.id.chess_view);
                    chessView.invalidate();
                }
            }
        }
        return true;
    }

    /**
     * Initialize the variables to the default values to start the game.
     */
    private void initVariables() {
        bitmaps = new HashMap<>();
        paint = new Paint();
        loadBitmaps();
        board = new Board();
        whiteTurn = true;
        piecesChecking = new ArrayList<>();
        moveMP = MediaPlayer.create(this.getContext(), R.raw.move_sound);
        eatMP = MediaPlayer.create(this.getContext(), R.raw.eat_sound);
        checkMP = MediaPlayer.create(this.getContext(), R.raw.check_sound);
        checkMateMP = MediaPlayer.create(this.getContext(), R.raw.check_mate_sound);
        startGameMP = MediaPlayer.create(this.getContext(), R.raw.start_game);
        castleMP = MediaPlayer.create(this.getContext(), R.raw.castle_sound);
        blackKingFirstMove = true;
        whiteKingFirstMove = true;
        whiteSortRookFirstMove = true;
        whiteLongRookFirstMove = true;
        blackSortRookFirstMove = true;
        blackLongRookFirstMove = true;
        kingChecked = false;
        checkMate = false;
        restartGame = false;
        timeWhite = matchTime;
        timeBlack = matchTime;
        timeBlackStr = "";
        timeWhiteStr = "";
        initWhiteTimer(matchTime);
        initBlackTimer(matchTime);
        startGameMP.start();
    }

    /**
     * Initialize black's timer.
     */
    private void initBlackTimer(long time) {
        timeBlackStr = "";
        if (timerBlack != null) { // to prevent ending game if timer ends
            timerBlack.cancel();
            timerBlack = null;
        }
        timerBlack = new CountDownTimer(time, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (saveBlackTime) {
                    timeBlack = millisUntilFinished + 500;
                    saveBlackTime = false;
                }
                long actualTime;
                if (!whiteTurn) // if this turn, prints countdown, if not, prints time he moved on
                    actualTime = millisUntilFinished;
                else
                    actualTime = timeBlack;
                int minutes = (int) Math.floor(actualTime / 60000);
                int seconds = (int) (actualTime / 1000) % 60;
                timeBlackStr = "[ " + minutes + ":";
                timeBlackStr += ((seconds < 10) ? ("0" + seconds) : seconds) + " ]";
            }

            @Override
            public void onFinish() {
                if (!whiteTurn) {
                    checkMate = true;
                    ChessView chessView = (ChessView) findViewById(R.id.chess_view);
                    chessView.invalidate();
                }
            }
        };
        timerBlack.start();
    }

    /**
     * Initialize white's timer.
     */
    private void initWhiteTimer(long time) {
        timeWhiteStr = "";
        if (timerWhite != null) {
            timerWhite.cancel();
            timerWhite = null;
        }
        timerWhite = new CountDownTimer(time, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (saveWhiteTime) {
                    timeWhite = millisUntilFinished + 500;
                    saveWhiteTime = false;
                }
                long actualTime;
                if (whiteTurn)
                    actualTime = millisUntilFinished;
                else
                    actualTime = timeWhite;
                int minutes = (int) Math.floor(actualTime / 60000);
                int seconds = (int) (actualTime / 1000) % 60;
                timeWhiteStr = "[ " + minutes + ":";
                timeWhiteStr += ((seconds < 10) ? ("0" + seconds) : seconds) + " ]";
                ChessView chessView = (ChessView) findViewById(R.id.chess_view);
                chessView.invalidate();
            }

            @Override
            public void onFinish() {
                if (whiteTurn) {
                    checkMate = true;
                    ChessView chessView = (ChessView) findViewById(R.id.chess_view);
                    chessView.invalidate();
                }
            }
        };
        timerWhite.start();
    }

    /**
     * Load the HashMap of bitmaps to draw.
     */
    private void loadBitmaps() {
        for (Integer id : imgIds) {
            bitmaps.put(id, BitmapFactory.decodeResource(getResources(), id));
        }
    }

    /**
     * Print the squares of the board.
     */
    private void initBoard(Canvas canvas) {
        for (int i = 0; i < 8; i++) {
            for (int k = i, j = 0; k < i + 8; k++, j++) {
                paint.setColor((k % 2 == 0) ? getResources().getColor(R.color.brown) : getResources().getColor(R.color.light));

                canvas.drawRect(
                        originX + squareSize * j,
                        originY + i * squareSize,
                        originX + squareSize * (j + 1),
                        originY + (i + 1) * squareSize,
                        paint);
            }
        }
    }

    /**
     * Print all the pieces in his square on the board.
     */
    private void initPieces(Canvas canvas) {
        for (Piece p : board.getPieceList()) {
            if (p != null)
                canvas.drawBitmap(bitmaps.get(p.getResID()),
                        null,
                        new RectF(
                                originX + squareSize * (p.getColumn() - 1),
                                originY + squareSize * (p.getRow() - 1),
                                originX + squareSize * (p.getColumn()),
                                originY + squareSize * (p.getRow())
                        ),
                        paint);
        }
    }

    /**
     * Switch the text between turns.
     */
    private void switchText(Canvas canvas) {
        if (!checkMate) {
            paint.setTextSize(60);
            paint.setTypeface(Typeface.SERIF);
            paint.setUnderlineText(false);
            if (whiteTurn) {
                paint.setColor(getResources().getColor(R.color.white));
                canvas.drawText("White's Turn", originX * 13f, originY * 4f, paint);
            } else {
                paint.setColor(getResources().getColor(R.color.black));
                canvas.drawText("Black's Turn", originX * 13f, originY / 1.4f, paint);
            }
        }
    }

    /**
     * Action of moving the piece.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void movePiece(int actualRow, int actualColumn, int finalRow, int finalColumn) {
        if (checkMate)
            return;

        if (finalRow < 1 || finalRow > 8 || finalColumn < 1 || finalColumn > 8) //if goes out of the board
            return;

        Piece actualPiece = null, otherPiece = null;
        for (Piece p : board.getPieceList()) {
            if (p.getColumn() == finalColumn && p.getRow() == finalRow) { // search piece when first touch
                otherPiece = p;
            }
            if (p.getColumn() == actualColumn && p.getRow() == actualRow) { // search piece when raiseº
                actualPiece = p;
            }
        }
        if (actualPiece == null) { //if didn't touch a piece
            return;
        }

        if (!kingChecked) { // if trying to move a piece on xRay of its king, can`t move
            Piece king = null;
            for (Piece p : board.getPieceList()) {
                if (p.getPlayer().equals(actualPiece.getPlayer()) && p.getModel().equals(PieceModel.KING)) {
                    king = p;
                    break;
                }
            }
            if (king != null) {
                for (Piece p : board.getPieceList()) {
                    if (!p.getPlayer().equals(king.getPlayer())) {
                        if (xRay(p, king)) {
                            if (actualPieceAloneOnXRay(p, king, actualPiece)) {
                                if (moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn, otherPiece, true))
                                    moveOnXRay(p, actualPiece, finalColumn, finalRow);
                                return;
                            }
                        }
                    }
                }
            }
        }

        if (kingChecked) { // king check rules
            if ((actualPiece.getPlayer().equals(Player.WHITE) && whiteTurn) || (actualPiece.getPlayer().equals(Player.BLACK) && !whiteTurn)) {
                Piece pieceChecking = null, king = null;
                for (Piece p : board.getPieceList()) {
                    if (p.getModel().equals(PieceModel.KING) && p.getPlayer().equals(whiteTurn ? Player.WHITE : Player.BLACK)) {
                        king = p;
                    }
                }
                for (Piece p : board.getPieceList()) {
                    if (!p.getPlayer().equals(king.getPlayer()) && (moveRules(p, p.getRow(), p.getColumn(), king.getRow(), king.getColumn(), king, false))) {
                        pieceChecking = p;
                    }
                }
                if (otherPiece == null && piecesChecking.size() == 1) {
                    if (!actualPiece.getModel().equals(PieceModel.KING)) {
                        if (rowsCheckList != null && rowsCheckList.size() > 0 && columnsCheckList != null && columnsCheckList.size() > 0) { //if putting a piece in diagonal of the check
                            for (int i = 0; i < rowsCheckList.size(); i++) {
                                if (rowsCheckList.get(i) == finalRow && columnsCheckList.get(i) == finalColumn) {
                                    if (moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn, otherPiece, true)) {
                                        endTurn(actualPiece, finalColumn, finalRow, false);
                                        return;
                                    }
                                }
                            }
                        } else if (rowsCheckList != null && rowsCheckList.size() > 0) { //if putting a piece in front of the check
                            for (int i = 0; i < rowsCheckList.size(); i++) {
                                if (rowsCheckList.get(i) == finalRow && finalColumn == pieceChecking.getColumn()) {
                                    if (moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn, otherPiece, true)) {
                                        endTurn(actualPiece, finalColumn, finalRow, false);
                                        return;
                                    }
                                }
                            }
                        } else if (columnsCheckList != null && columnsCheckList.size() > 0) { //if putting a piece in lateral of the check
                            for (int i = 0; i < columnsCheckList.size(); i++) {
                                if (columnsCheckList.get(i) == finalColumn && finalRow == pieceChecking.getRow()) {
                                    if (moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn, otherPiece, true)) {
                                        endTurn(actualPiece, finalColumn, finalRow, false);
                                        return;
                                    }
                                }
                            }
                        }
                    } else { //if moving the king from the check
                        if (moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn, otherPiece, true)) {
                            endTurn(actualPiece, finalColumn, finalRow, false);
                            return;
                        }
                    }
                } else if (otherPiece != null && !otherPiece.getModel().equals(PieceModel.KING)) {
                    if (otherPiece.getPlayer().equals(actualPiece.getPlayer())) { //blocks with his own pieces
                        return;
                    } else if (!otherPiece.getPlayer().equals(actualPiece.getPlayer())) { //eats the checkingPiece
                        if (piecesChecking.size() == 1) {
                            if (pieceChecking.equals(otherPiece) && moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn, otherPiece, true)) {
                                board.getPieceList().remove(otherPiece);
                                endTurn(actualPiece, finalColumn, finalRow, true);
                                return;
                            }
                        } else if (piecesChecking.size() == 2) { // if double check, only the king can move or move and eat
                            if (actualPiece.getModel().equals(PieceModel.KING)) {
                                if (moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn, otherPiece, true)) {
                                    if (otherPiece != null) {
                                        board.getPieceList().remove(otherPiece);
                                        endTurn(actualPiece, finalColumn, finalRow, true);
                                    } else {
                                        endTurn(actualPiece, finalColumn, finalRow, false);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } else if ((actualPiece.getPlayer().equals(Player.WHITE) && whiteTurn) || (actualPiece.getPlayer().equals(Player.BLACK) && !whiteTurn)) { // normal rules
            if (otherPiece == null) { //can move
                if (moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn, otherPiece, true)) {
                    endTurn(actualPiece, finalColumn, finalRow, false);
                    return;
                }
            } else if (otherPiece != null && !otherPiece.getModel().equals(PieceModel.KING)) {
                if (otherPiece.getPlayer().equals(actualPiece.getPlayer())) { //blocks with his own pieces
                    return;
                } else if (!otherPiece.getPlayer().equals(actualPiece.getPlayer())) { //eats and move
                    if (moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn, otherPiece, true)) {
                        board.getPieceList().remove(otherPiece);
                        endTurn(actualPiece, finalColumn, finalRow, true);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Returns if the piece can be in finalRow and finalColumn or in the otherPiece square.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean moveRules(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn, Piece otherPiece, boolean move) {
        switch (piece.getModel()) {
            case PAWN:
                Boolean p = pawnMovement(piece, actualRow, actualColumn, finalRow, finalColumn, otherPiece, move);
                if (p != null) return p;
                break;
            case ROOK:
                Boolean r = rookMovement(piece, actualRow, actualColumn, finalRow, finalColumn);
                if (r != null) return r;
                break;
            case KNIGHT:
                Boolean kn = knightMovement(piece, actualRow, actualColumn, finalRow, finalColumn);
                if (kn != null) return kn;
                break;
            case BISHOP:
                Boolean b = bishopMovement(piece, actualRow, actualColumn, finalRow, finalColumn);
                if (b != null) return b;
                break;
            case QUEEN:
                Boolean q = queenMovement(piece, actualRow, actualColumn, finalRow, finalColumn);
                if (q != null) return q;
                break;
            case KING:
                Boolean k = kingMovement(piece, actualRow, actualColumn, finalRow, finalColumn, otherPiece, move);
                if (k != null) return k;
                break;
        }
        return false;
    }

    /**
     * Special rules for pawns.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private Boolean pawnMovement(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn, Piece otherPiece, boolean move) {
        if (!checkPieceInFront(piece, actualRow, actualColumn, finalRow, finalColumn)) {
            if (actualColumn == finalColumn) {//if moving forward
                if (otherPiece != null && otherPiece.getModel().equals(PieceModel.KING))
                    return false;
                int maxMove = 1;
                if (checkPawnFirstMove(piece)) {
                    maxMove = 2;
                }
                if (piece.getPlayer().equals(Player.WHITE)) {
                    if (actualRow - finalRow <= maxMove && actualRow - finalRow > 0) { //if moves 1 or 2
                        if (finalRow == 1) {
                            coronationMenu(piece);
                        }
                        if (maxMove == 2) { // next turn, can be captured by enPassant rule
                            enPassantRow = finalRow + 1;
                            enPassantColumn = finalColumn;
                            enPassantMove = true;
                        }
                        return true;
                    }
                } else if (piece.getPlayer().equals(Player.BLACK)) {
                    if (finalRow - actualRow <= maxMove && finalRow - actualRow > 0) { //if moves 1 or 2
                        if (finalRow == 8) {
                            coronationMenu(piece);
                        }
                        if (maxMove == 2) { // next turn, can be captured by enPassant rule
                            enPassantRow = finalRow - 1;
                            enPassantColumn = finalColumn;
                            enPassantMove = true;
                        }
                        return true;
                    }
                }
            } else if (actualColumn + 1 == finalColumn || actualColumn - 1 == finalColumn) { //if trying to eat
                if (piece.getPlayer().equals(Player.WHITE)) {
                    if (actualRow - finalRow == 1) {
                        if (otherPiece != null && otherPiece.getModel().equals(PieceModel.KING)) {
                            return false; //TODO return true; if doesnt work
                        } else if (otherPiece != null) { // if there is a piece to eat
                            if (finalRow == 1) {
                                coronationMenu(piece);
                            }
                            if (move) {
                                board.getPieceList().remove(otherPiece);
                                endTurn(piece, finalColumn, finalRow, true);
                            }
                            return false;
                        } else if (finalColumn == enPassantColumn && finalRow == enPassantRow) { // if last turn a pawn went enPassant
                            if (move) {
                                Piece delete = null;
                                for (Piece p : board.getPieceList()) {
                                    if (!p.getPlayer().equals(piece.getPlayer()))
                                        if (p.getRow() == enPassantRow + 1 && p.getColumn() == enPassantColumn)
                                            delete = p;
                                }
                                if (delete != null)
                                    board.getPieceList().remove(delete);
                                endTurn(piece, finalColumn, finalRow, delete != null);
                            }
                            return false;
                        }
                    }
                } else if (piece.getPlayer().equals(Player.BLACK)) {
                    if (finalRow - actualRow == 1) {
                        if (otherPiece != null && otherPiece.getModel().equals(PieceModel.KING)) {
                            return true;
                        } else if (otherPiece != null) {
                            if (finalRow == 1) {
                                coronationMenu(piece);
                            }
                            if (move) {
                                board.getPieceList().remove(otherPiece);
                                endTurn(piece, finalColumn, finalRow, true);
                            }
                            return false;
                        } else if (finalColumn == enPassantColumn && finalRow == enPassantRow) {
                            if (move) {
                                Piece delete = null;
                                for (Piece p : board.getPieceList()) {
                                    if (!p.getPlayer().equals(piece.getPlayer()))
                                        if (p.getRow() == enPassantRow - 1 && p.getColumn() == enPassantColumn)
                                            delete = p;
                                }
                                if (delete != null)
                                    board.getPieceList().remove(delete);
                                endTurn(piece, finalColumn, finalRow, delete != null);
                            }
                            return false;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Rules for rook.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private Boolean rookMovement(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn) {
        if (!checkPieceInFront(piece, actualRow, actualColumn, finalRow, finalColumn) && !checkPieceInLateral(piece, actualRow, actualColumn, finalRow, finalColumn)) {
            if (actualColumn == finalColumn || actualRow == finalRow) {
                if (piece.getPlayer().equals(Player.WHITE)) { // rules for castle
                    if (piece.getColumn() == 1)
                        whiteLongRookFirstMove = false;
                    else if (piece.getColumn() == 8)
                        whiteSortRookFirstMove = false;
                } else {
                    if (piece.getColumn() == 1)
                        blackLongRookFirstMove = false;
                    else if (piece.getColumn() == 8)
                        blackSortRookFirstMove = false;
                }
                return true;
            } else
                return false;
        }
        return null;
    }

    /**
     * Rules for knight.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private Boolean knightMovement(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn) {
        if (Math.abs(finalColumn - actualColumn) == 2 && Math.abs(finalRow - actualRow) == 1 || Math.abs(finalColumn - actualColumn) == 1 && Math.abs(finalRow - actualRow) == 2)
            return true;
        else
            return false;
    }

    /**
     * Rules for bishop.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private Boolean bishopMovement(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn) {
        if (!checkPieceInDiagonal(piece, actualRow, actualColumn, finalRow, finalColumn)) {
            if (Math.abs(finalColumn - actualColumn) == Math.abs(finalRow - actualRow))
                return true;
            else
                return false;
        }
        return null;
    }

    /**
     * Rules for queen.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private Boolean queenMovement(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn) {
        if (!checkPieceInFront(piece, actualRow, actualColumn, finalRow, finalColumn) && !checkPieceInLateral(piece, actualRow, actualColumn, finalRow, finalColumn) && !checkPieceInDiagonal(piece, actualRow, actualColumn, finalRow, finalColumn)) {
            if (actualColumn == finalColumn || actualRow == finalRow)
                return true;
            else if (Math.abs(finalColumn - actualColumn) == Math.abs(finalRow - actualRow))
                return true;
            else
                return false;
        }
        return null;
    }

    /**
     * Special rules for king.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private Boolean kingMovement(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn, Piece otherPiece, boolean move) {
        if (move) {
            kingCastle(piece, finalRow, finalColumn);
        }
        if ((Math.abs(finalColumn - actualColumn) == 0 || Math.abs(finalColumn - actualColumn) == 1) && (Math.abs(finalRow - actualRow) == 0 || Math.abs(finalRow - actualRow) == 1)) {
            if (otherPiece != null && !otherPiece.getModel().equals(PieceModel.KING)) {
                if (!piece.getPlayer().equals(otherPiece.getPlayer())) { //if trying to eat a defended piece, don`t
                    for (Piece p : board.getPieceList()) {
                        if (p.getPlayer().equals(otherPiece.getPlayer()) && !p.equals(otherPiece)) {
                            if (moveRules(p, p.getRow(), p.getColumn(), otherPiece.getRow(), otherPiece.getColumn(), otherPiece, false)) {
                                return false;
                            }
                        }
                    }
                }
                if (move) { // if not looking for checks
                    if (piece.getPlayer().equals(Player.WHITE)) // castle rules
                        whiteKingFirstMove = false;
                    else
                        blackKingFirstMove = false;
                    board.getPieceList().remove(otherPiece);
                    endTurn(piece, finalColumn, finalRow, true);
                    return false;
                } else
                    return true;
            }
            if (otherPiece == null) { //if it's a free square but is defended, can`t move
                for (Piece p : board.getPieceList()) {
                    if (!p.getPlayer().equals(piece.getPlayer())) {
                        if (moveRules(p, p.getRow(), p.getColumn(), finalRow, finalColumn, piece, false)) {
                            return false;
                        }
                    }
                }
                if (move) {
                    endTurn(piece, finalColumn, finalRow, false);
                    if (piece.getPlayer().equals(Player.WHITE)) // castle rules
                        whiteKingFirstMove = false;
                    else
                        blackKingFirstMove = false;
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there are any piece in front.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean checkPieceInFront(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn) {
        boolean eats = false, defending = false;
        if (finalColumn == actualColumn) { // if moving in front
            List<Integer> rows;
            if (actualRow >= finalRow) { //if moving backwards the order should be reversed
                rows = IntStream.rangeClosed(finalRow, actualRow).boxed().collect(Collectors.toList());
                Collections.reverse(rows);
            } else
                rows = IntStream.rangeClosed(actualRow, finalRow).boxed().collect(Collectors.toList()); //if moving forward list order is OK
            for (Piece p : board.getPieceList()) {
                if (!piece.getPlayer().equals(p.getPlayer()) && finalColumn == p.getColumn() && finalRow == p.getRow() && !piece.getModel().equals(PieceModel.PAWN))
                    eats = true;
                else if (piece.getPlayer().equals(p.getPlayer()) && finalColumn == p.getColumn() && finalRow == p.getRow() && !piece.getModel().equals(PieceModel.PAWN))
                    defending = true;
                else if (!piece.equals(p) && rows.contains(p.getRow()) && actualColumn == p.getColumn()) { //if the piece is in front
                    return true;
                }
            }
            if (eats) return false;
            if (defending) return false;
        }
        return false;
    }

    /**
     * Checks if there are any piece in lateral.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean checkPieceInLateral(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn) {
        boolean eats = false, defending = false;
        if (finalRow == actualRow) {
            List<Integer> columns;
            if (actualColumn >= finalColumn) {
                columns = IntStream.rangeClosed(finalColumn, actualColumn).boxed().collect(Collectors.toList());
                Collections.reverse(columns);
            } else
                columns = IntStream.rangeClosed(actualColumn, finalColumn).boxed().collect(Collectors.toList());
            for (Piece p : board.getPieceList()) {
                if (!piece.getPlayer().equals(p.getPlayer()) && finalRow == p.getRow() && finalColumn == p.getColumn() && !piece.getModel().equals(PieceModel.PAWN))
                    eats = true;
                else if (piece.getPlayer().equals(p.getPlayer()) && finalRow == p.getRow() && finalColumn == p.getColumn() && !piece.getModel().equals(PieceModel.PAWN))
                    defending = true;
                else if (!piece.equals(p) && columns.contains(p.getColumn()) && actualRow == p.getRow()) {
                    return true;
                }
            }
            if (eats) return false;
            if (defending) return false;
        }
        return false;
    }

    /**
     * Checks if there are any piece in diagonal.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean checkPieceInDiagonal(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn) {
        boolean eats = false, defending = false;
        if (Math.abs(finalColumn - actualColumn) == Math.abs(finalRow - actualRow)) {
            List<Integer> rows;
            List<Integer> columns;
            if (actualRow >= finalRow) {
                rows = IntStream.rangeClosed(finalRow, actualRow).boxed().collect(Collectors.toList());
                Collections.reverse(rows);
            } else
                rows = IntStream.rangeClosed(actualRow, finalRow).boxed().collect(Collectors.toList());

            if (actualColumn >= finalColumn) {
                columns = IntStream.rangeClosed(finalColumn, actualColumn).boxed().collect(Collectors.toList());
                Collections.reverse(columns);
            } else
                columns = IntStream.rangeClosed(actualColumn, finalColumn).boxed().collect(Collectors.toList());

            for (Piece p : board.getPieceList()) {
                for (int i = 0; i < rows.size(); i++) {
                    if (!piece.getPlayer().equals(p.getPlayer()) && finalColumn == p.getColumn() && finalRow == p.getRow() && !piece.getModel().equals(PieceModel.PAWN))
                        eats = true;
                    else if (piece.getPlayer().equals(p.getPlayer()) && finalColumn == p.getColumn() && finalRow == p.getRow() && !piece.getModel().equals(PieceModel.PAWN))
                        defending = true;
                    else if (!piece.equals(p) && rows.get(i) == p.getRow() && columns.get(i) == p.getColumn()) {
                        return true;
                    }
                }
            }
            if (eats) return false;
            if (defending) return false;
        }
        return false;
    }

    /**
     * Check if the pawn is moving for the first time.
     */
    private boolean checkPawnFirstMove(Piece piece) {
        if (piece.getPlayer().equals(Player.BLACK)) {
            if (piece.getRow() == 2) {
                return true;
            }
        } else if (piece.getPlayer().equals(Player.WHITE)) {
            if (piece.getRow() == 7) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void kingCastle(Piece king, int finalRow, int finalColumn) {
        Piece rook = null;
        if (kingChecked)
            return;
        if (whiteKingFirstMove && king.getPlayer().equals(Player.WHITE)) {
            if (whiteLongRookFirstMove && finalRow == 8 && finalColumn == 3) { // long white castle
                for (Piece p : board.getPieceList()) {
                    if (!p.getModel().equals(PieceModel.KING)) {
                        if (p.getRow() == 8 && (p.getColumn() == 3 || p.getColumn() == 4)) { // if piece in the way
                            return;
                        }
                        if (!p.getPlayer().equals(king.getPlayer())) {
                            if (moveRules(p, p.getRow(), p.getColumn(), 8, 3, null, false)
                                    || moveRules(p, p.getRow(), p.getColumn(), 8, 4, null, false)) { // if a enemy piece is looking at the castling
                                return;
                            }
                        }
                        if (p.getRow() == 8 && p.getColumn() == 1 && p.getModel().equals(PieceModel.ROOK))
                            rook = p;
                    }
                }
                if (rook != null) {
                    rook.setColumn(4);
                }
                endTurn(king, finalColumn, finalRow, null);
                whiteKingFirstMove = false;
                whiteLongRookFirstMove = false;
            } else if (whiteSortRookFirstMove && finalRow == 8 && finalColumn == 7) { // short white castle
                for (Piece p : board.getPieceList()) {
                    if (!p.getModel().equals(PieceModel.KING)) {
                        if (p.getRow() == 8 && (p.getColumn() == 6 || p.getColumn() == 7)) {
                            return;
                        }
                        if (!p.getPlayer().equals(king.getPlayer())) {
                            if (moveRules(p, p.getRow(), p.getColumn(), 8, 6, null, false)
                                    || moveRules(p, p.getRow(), p.getColumn(), 8, 7, null, false)) {
                                return;
                            }
                        }
                        if (p.getRow() == 8 && p.getColumn() == 8 && p.getModel().equals(PieceModel.ROOK))
                            rook = p;
                    }
                }
                if (rook != null) {
                    rook.setColumn(6);
                }
                endTurn(king, finalColumn, finalRow, false);
                whiteKingFirstMove = false;
                whiteSortRookFirstMove = false;
            }

        } else if (blackKingFirstMove && king.getPlayer().equals(Player.BLACK)) {
            if (blackLongRookFirstMove && finalRow == 1 && finalColumn == 3) { // long black castle
                for (Piece p : board.getPieceList()) {
                    if (!p.getModel().equals(PieceModel.KING)) {
                        if (p.getRow() == 1 && (p.getColumn() == 3 || p.getColumn() == 4)) {
                            return;
                        }
                        if (!p.getPlayer().equals(king.getPlayer())) {
                            if (moveRules(p, p.getRow(), p.getColumn(), 1, 3, null, false)
                                    || moveRules(p, p.getRow(), p.getColumn(), 1, 4, null, false)) {
                                return;
                            }
                        }
                        if (p.getRow() == 1 && p.getColumn() == 1 && p.getModel().equals(PieceModel.ROOK))
                            rook = p;
                    }
                }
                if (rook != null) {
                    rook.setColumn(4);
                }
                endTurn(king, finalColumn, finalRow, false);
                blackKingFirstMove = false;
                blackLongRookFirstMove = false;
            } else if (blackSortRookFirstMove && finalRow == 1 && finalColumn == 7) { // short black castle
                for (Piece p : board.getPieceList()) {
                    if (!p.getModel().equals(PieceModel.KING)) {
                        if (p.getRow() == 1 && (p.getColumn() == 6 || p.getColumn() == 7)) {
                            return;
                        }
                        if (!p.getPlayer().equals(king.getPlayer())) {
                            if (moveRules(p, p.getRow(), p.getColumn(), 1, 6, new Piece(1, 6, Player.BLACK, PieceModel.KING, R.drawable.kingblack), false)
                                    || moveRules(p, p.getRow(), p.getColumn(), 1, 7, new Piece(1, 6, Player.BLACK, PieceModel.KING, R.drawable.kingblack), false)) {
                                return;
                            }
                        }
                        if (p.getRow() == 1 && p.getColumn() == 8 && p.getModel().equals(PieceModel.ROOK))
                            rook = p;
                    }
                }
                if (rook != null) {
                    rook.setColumn(6);
                }
                endTurn(king, finalColumn, finalRow, false);
                blackKingFirstMove = false;
                blackSortRookFirstMove = false;
            }
        }
    }

    private void coronationMenu(Piece piece) {
        final Dialog fbDialogue = new Dialog(ChessView.this.getContext(), android.R.style.Theme_Black_NoTitleBar);
        fbDialogue.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
        fbDialogue.setContentView(R.layout.coronation_menu);
        fbDialogue.setCancelable(true);
        fbDialogue.show();

        onClickListener = v -> {
            if (v.getResources().getResourceEntryName(v.getId()).contains("queen")) {
                piece.setModel(PieceModel.QUEEN);
                if (piece.getPlayer().equals(Player.WHITE))
                    piece.setResID(R.drawable.queenwhite);
                else
                    piece.setResID(R.drawable.queenblack);
            } else if (v.getResources().getResourceEntryName(v.getId()).contains("knight")) {
                piece.setModel(PieceModel.KNIGHT);
                if (piece.getPlayer().equals(Player.WHITE))
                    piece.setResID(R.drawable.knightwhite);
                else
                    piece.setResID(R.drawable.knightblack);
            } else if (v.getResources().getResourceEntryName(v.getId()).contains("rook")) {
                piece.setModel(PieceModel.ROOK);
                if (piece.getPlayer().equals(Player.WHITE))
                    piece.setResID(R.drawable.rookwhite);
                else
                    piece.setResID(R.drawable.rookblack);
            } else if (v.getResources().getResourceEntryName(v.getId()).contains("bishop")) {
                piece.setModel(PieceModel.BISHOP);
                if (piece.getPlayer().equals(Player.WHITE))
                    piece.setResID(R.drawable.bishopwhite);
                else
                    piece.setResID(R.drawable.bishopblack);
            }
            if (fbDialogue.isShowing())
                fbDialogue.dismiss();
            ChessView chessView = (ChessView) findViewById(R.id.chess_view);
            chessView.invalidate();
        };


        ImageView coroQueen = fbDialogue.findViewById(R.id.queenmenu);
        coroQueen.setOnClickListener(onClickListener);
        ImageView coroKnight = fbDialogue.findViewById(R.id.knightmenu);
        coroKnight.setOnClickListener(onClickListener);
        ImageView coroRook = fbDialogue.findViewById(R.id.rookmenu);
        coroRook.setOnClickListener(onClickListener);
        ImageView coroBishop = fbDialogue.findViewById(R.id.bishopmenu);
        coroBishop.setOnClickListener(onClickListener);
        if (piece.getPlayer().equals(Player.WHITE)) {
            coroQueen.setImageResource(R.drawable.queenwhite);
            coroKnight.setImageResource(R.drawable.knightwhite);
            coroRook.setImageResource(R.drawable.rookwhite);
            coroBishop.setImageResource(R.drawable.bishopwhite);
        } else {
            coroQueen.setImageResource(R.drawable.queenblack);
            coroKnight.setImageResource(R.drawable.knightblack);
            coroRook.setImageResource(R.drawable.rookblack);
            coroBishop.setImageResource(R.drawable.bishopblack);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void endTurn(Piece piece, int finalColumn, int finalRow, Boolean eats) {
        if (!enPassantMove) {
            enPassantColumn = 0;
            enPassantRow = 0;
        } else
            enPassantMove = false;
        piece.setColumn(finalColumn);
        piece.setRow(finalRow);
        whiteTurn = !whiteTurn;
        ChessView chessView = (ChessView) findViewById(R.id.chess_view);
        chessView.invalidate();
        if (whiteTurn) {
            saveBlackTime = true;
            initWhiteTimer(timeWhite);
        } else {
            saveWhiteTime = true;
            initBlackTimer(timeBlack);
        }
        kingCheck();
        if (kingChecked) {
            checkMP.start();
        } else if (eats == null) {
            castleMP.start();
        } else if (eats) {
            eatMP.start();
        } else if (!eats) {
            moveMP.start();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void kingCheck() {
        if (piecesChecking.size() > 0)
            piecesChecking = new ArrayList<>();
        kingChecked = false;
        rowsCheckList = null;
        columnsCheckList = null;
        Piece king = null;
        for (Piece p : board.getPieceList())
            if (p.getModel().equals(PieceModel.KING))
                if ((whiteTurn && p.getPlayer().equals(Player.WHITE)) || (!whiteTurn && p.getPlayer().equals(Player.BLACK))) {
                    king = p;
                    break;
                }

        if (king != null) {
            Piece checking;
            for (Piece p : board.getPieceList()) {
                if (!p.getPlayer().equals(king.getPlayer())) {
                    if (moveRules(p, p.getRow(), p.getColumn(), king.getRow(), king.getColumn(), king, false)) {
                        kingChecked = true;
                        checking = p;
                        piecesChecking.add(checking);
                        getCheckSquareList(p, king);
                        kingCheckMated(p, king);
                    }
                }
            }
            if (piecesChecking.size() >= 1) {
                if (king.getPlayer().equals(Player.WHITE)) { //draw kingcheck model
                    board.getPieceList().get(board.getPieceList().indexOf(king)).setResID(R.drawable.kingwhitecheck);
                } else
                    board.getPieceList().get(board.getPieceList().indexOf(king)).setResID(R.drawable.kingblackcheck);
            } else { //draw original model
                for (Piece p : board.getPieceList()) {
                    if (p.getModel().equals(PieceModel.KING)) {
                        if (p.getPlayer().equals(Player.WHITE))
                            p.setResID(R.drawable.kingwhite);
                        else
                            p.setResID(R.drawable.kingblack);
                    }
                }
            }
        }
        ChessView chessView = (ChessView) findViewById(R.id.chess_view);
        chessView.invalidate();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void getCheckSquareList(Piece p, Piece king) {
        switch (p.getModel()) {
            case ROOK:
                if (king.getRow() == p.getRow()) {
                    if (p.getColumn() >= king.getColumn()) {
                        columnsCheckList = IntStream.rangeClosed(king.getColumn() + 1, p.getColumn() - 1).boxed().collect(Collectors.toList());
                        Collections.reverse(columnsCheckList);
                    } else
                        columnsCheckList = IntStream.rangeClosed(p.getColumn() + 1, king.getColumn() - 1).boxed().collect(Collectors.toList());
                } else if (king.getColumn() == p.getColumn()) {
                    if (p.getRow() >= king.getRow()) {
                        rowsCheckList = IntStream.rangeClosed(king.getRow() + 1, p.getRow() - 1).boxed().collect(Collectors.toList());
                        Collections.reverse(rowsCheckList);
                    } else
                        rowsCheckList = IntStream.rangeClosed(p.getRow() + 1, king.getRow() - 1).boxed().collect(Collectors.toList());
                }
                break;
            case BISHOP:
                if (p.getRow() >= king.getRow()) {
                    rowsCheckList = IntStream.rangeClosed(king.getRow() + 1, p.getRow() - 1).boxed().collect(Collectors.toList());
                    Collections.reverse(rowsCheckList);
                } else
                    rowsCheckList = IntStream.rangeClosed(p.getRow() + 1, king.getRow() - 1).boxed().collect(Collectors.toList());

                if (p.getColumn() >= king.getColumn()) {
                    columnsCheckList = IntStream.rangeClosed(king.getColumn() + 1, p.getColumn() - 1).boxed().collect(Collectors.toList());
                    Collections.reverse(columnsCheckList);
                } else
                    columnsCheckList = IntStream.rangeClosed(p.getColumn() + 1, king.getColumn() - 1).boxed().collect(Collectors.toList());
                break;
            case QUEEN:
                if (king.getRow() == p.getRow()) {
                    if (p.getColumn() >= king.getColumn()) {
                        columnsCheckList = IntStream.rangeClosed(king.getColumn() + 1, p.getColumn() - 1).boxed().collect(Collectors.toList());
                        Collections.reverse(columnsCheckList);
                    } else
                        columnsCheckList = IntStream.rangeClosed(p.getColumn() + 1, king.getColumn() - 1).boxed().collect(Collectors.toList());

                } else if (king.getColumn() == p.getColumn()) {
                    if (p.getRow() >= king.getRow()) {
                        rowsCheckList = IntStream.rangeClosed(king.getRow() + 1, p.getRow() - 1).boxed().collect(Collectors.toList());
                        Collections.reverse(rowsCheckList);
                    } else
                        rowsCheckList = IntStream.rangeClosed(p.getRow() + 1, king.getRow() - 1).boxed().collect(Collectors.toList());

                } else {
                    if (p.getRow() >= king.getRow()) {
                        rowsCheckList = IntStream.rangeClosed(king.getRow() + 1, p.getRow() - 1).boxed().collect(Collectors.toList());
                        Collections.reverse(rowsCheckList);
                    } else
                        rowsCheckList = IntStream.rangeClosed(p.getRow() + 1, king.getRow() - 1).boxed().collect(Collectors.toList());

                    if (p.getColumn() >= king.getColumn()) {
                        columnsCheckList = IntStream.rangeClosed(king.getColumn() + 1, p.getColumn() - 1).boxed().collect(Collectors.toList());
                        Collections.reverse(columnsCheckList);
                    } else
                        columnsCheckList = IntStream.rangeClosed(p.getColumn() + 1, king.getColumn() - 1).boxed().collect(Collectors.toList());
                }
                break;
        }
    }

    private boolean xRay(Piece piece, Piece king) {
        switch (piece.getModel()) {
            case ROOK:
                if (piece.getColumn() == king.getColumn() || piece.getRow() == king.getRow()) {
                    return true;
                }
                break;
            case BISHOP:
                if (Math.abs(piece.getColumn() - king.getColumn()) == Math.abs(piece.getRow() - king.getRow())) {
                    return true;
                }
                break;
            case QUEEN:
                if (piece.getColumn() == king.getColumn() || piece.getRow() == king.getRow())
                    return true;
                else if (Math.abs(piece.getColumn() - king.getColumn()) == Math.abs(piece.getRow() - king.getRow()))
                    return true;
                break;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean actualPieceAloneOnXRay(Piece piece, Piece king, Piece actualPiece) {
        getCheckSquareList(piece, king);
        rowsXRayList = rowsCheckList;
        columnsXRayList = columnsCheckList;
        rowsCheckList = null;
        columnsCheckList = null;
        List<Piece> piecesXRay = new ArrayList<>();
        if (rowsXRayList != null && columnsXRayList != null) { //if putting a piece in diagonal of the check
            for (Piece p : board.getPieceList()) {
                for (int i = 0; i < rowsXRayList.size(); i++) {
                    if (rowsXRayList.get(i) == p.getRow() && columnsXRayList.get(i) == p.getColumn()) {
                        piecesXRay.add(p);
                    }
                }
            }
        } else if (rowsXRayList != null) { //if putting a piece in front of the check
            for (Piece p : board.getPieceList()) {
                for (int i = 0; i < rowsXRayList.size(); i++) {
                    if (p.getColumn() == king.getColumn() && rowsXRayList.get(i) == p.getRow()) {
                        piecesXRay.add(p);
                    }
                }
            }
        } else if (columnsXRayList != null) { //if putting a piece in lateral of the check
            for (Piece p : board.getPieceList()) {
                for (int i = 0; i < columnsXRayList.size(); i++) {
                    if (p.getRow() == king.getRow() && columnsXRayList.get(i) == p.getColumn()) {
                        piecesXRay.add(p);
                    }
                }
            }
        }
        if (piecesXRay != null && piecesXRay.contains(actualPiece)) {
            if (piecesXRay.size() == 1) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void moveOnXRay(Piece p, Piece piece, int finalColumn, int finalRow) {
        boolean eats = false;
        switch (p.getModel()) {
            case ROOK:
                if (rowsXRayList != null) {
                    if ((rowsXRayList.contains(finalRow) || p.getRow() == finalRow) && p.getColumn() == finalColumn) {
                        if (p.getRow() == finalRow) {
                            board.getPieceList().remove(p);
                            eats = true;
                        }
                        endTurn(piece, finalColumn, finalRow, eats);
                    }
                } else if (columnsXRayList != null) {
                    if ((columnsXRayList.contains(finalColumn) || p.getColumn() == finalColumn) && p.getRow() == finalRow) {
                        if (p.getColumn() == finalColumn) {
                            board.getPieceList().remove(p);
                            eats = true;
                        }
                        endTurn(piece, finalColumn, finalRow, eats);
                    }
                }
                break;
            case BISHOP:
                if (rowsXRayList != null && columnsXRayList != null) {
                    for (int i = 0; i < rowsXRayList.size(); i++) {
                        if ((rowsXRayList.get(i) == finalRow && columnsXRayList.get(i) == finalColumn) || (p.getRow() == finalRow && p.getColumn() == finalColumn)) {
                            if (p.getRow() == finalRow && p.getColumn() == finalColumn) {
                                board.getPieceList().remove(p);
                                eats = true;
                            }
                            endTurn(piece, finalColumn, finalRow, eats);
                        }
                    }
                }
                break;
            case QUEEN:
                if (rowsXRayList != null && columnsXRayList != null) {
                    for (int i = 0; i < rowsXRayList.size(); i++) {
                        if ((rowsXRayList.get(i) == finalRow && columnsXRayList.get(i) == finalColumn) || (p.getRow() == finalRow && p.getColumn() == finalColumn)) {
                            if (p.getRow() == finalRow && p.getColumn() == finalColumn) {
                                board.getPieceList().remove(p);
                                eats = true;
                            }
                            endTurn(piece, finalColumn, finalRow, eats);
                        }
                    }
                } else if (rowsXRayList != null) {
                    if ((rowsXRayList.contains(finalRow) || p.getRow() == finalRow) && p.getColumn() == finalColumn) {
                        if (p.getRow() == finalRow) {
                            board.getPieceList().remove(p);
                            eats = true;
                        }
                        endTurn(piece, finalColumn, finalRow, eats);
                    }
                } else if (columnsXRayList != null) {
                    if ((columnsXRayList.contains(finalColumn) || p.getColumn() == finalColumn) && p.getRow() == finalRow) {
                        if (p.getColumn() == finalColumn) {
                            board.getPieceList().remove(p);
                            eats = true;
                        }
                        endTurn(piece, finalColumn, finalRow, eats);
                    }
                }
                break;
        }
        rowsXRayList = null;
        columnsXRayList = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void kingCheckMated(Piece checkingPiece, Piece king) {
        if (piecesChecking.size() == 1) {
            if (rowsCheckList != null && rowsCheckList.size() > 0 && columnsCheckList != null && columnsCheckList.size() > 0) { //if putting a piece in diagonal of the check
                for (int i = 0; i < rowsCheckList.size(); i++) {
                    for (Piece p : board.getPieceList()) {
                        if (p.getPlayer().equals(king.getPlayer()))
                            if (moveRules(p, p.getRow(), p.getColumn(), rowsCheckList.get(i), columnsCheckList.get(i), null, false)) {
                                return;
                            }
                    }
                }
            } else if (rowsCheckList != null && rowsCheckList.size() > 0) { //if putting a piece in front of the check
                for (int i = 0; i < rowsCheckList.size(); i++) {
                    for (Piece p : board.getPieceList()) {
                        if (p.getPlayer().equals(king.getPlayer()))
                            if (moveRules(p, p.getRow(), p.getColumn(), rowsCheckList.get(i), checkingPiece.getColumn(), null, false)) {
                                return;
                            }
                    }
                }
            } else if (columnsCheckList != null && columnsCheckList.size() > 0) { //if putting a piece in lateral of the check
                for (int i = 0; i < columnsCheckList.size(); i++) {
                    for (Piece p : board.getPieceList()) {
                        if (p.getPlayer().equals(king.getPlayer())) {
                            if (moveRules(p, p.getRow(), p.getColumn(), checkingPiece.getRow(), columnsCheckList.get(i), null, false)) {
                                return;
                            }
                        }
                    }
                }
            }
            // if can eat the checkingPiece
            for (Piece p : board.getPieceList()) {
                if (p.getPlayer().equals(king.getPlayer()))
                    if (moveRules(p, p.getRow(), p.getColumn(), checkingPiece.getRow(), checkingPiece.getColumn(), checkingPiece, false)) {
                        return;
                    }
            }
            // if the king can move
            for (int row = -1; row <= 1; row++) {
                for (int column = -1; column <= 1; column++) {
                    if (king.getRow() + row >= 1 && king.getRow() + row <= 8 && king.getColumn() + column >= 1 && king.getColumn() + column <= 8) {
                        Piece otherPieceAux = null;
                        for (Piece p : board.getPieceList()) {
                            if (!p.equals(king) && p.getRow() == king.getRow() + row && p.getColumn() == king.getColumn() + column) {
                                otherPieceAux = p;
                                break;
                            }
                        }
                        if (moveRules(king, king.getRow(), king.getColumn(), king.getRow() + row, king.getColumn() + column, (otherPieceAux != null) ? otherPieceAux : null, false)) {
                            return;
                        }
                    }
                }
            }
        } else if (piecesChecking.size() == 2) {
            for (int row = -1; row <= 1; row++) {
                for (int column = -1; column <= 1; column++) {
                    Piece otherPiece = null;
                    for (Piece p : board.getPieceList()) {
                        if (!p.equals(king) && p.getRow() == king.getRow() + row && p.getColumn() == king.getColumn() + column)
                            otherPiece = p;
                    }
                    if (moveRules(king, king.getRow(), king.getColumn(), king.getRow() + row, king.getColumn() + column, (otherPiece != null) ? otherPiece : null, false)) {
                        return;
                    }
                }
            }
        }
        // if reach here, win
        checkMate = true;
        checkMateMP.start();
    }
}