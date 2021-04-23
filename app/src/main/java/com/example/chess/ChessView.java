package com.example.chess;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Paint;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

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
    private float originX = 0f, originY = 0f, squareSize = 0f;
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
            R.drawable.pawnwhite);
    private HashMap<Integer, Bitmap> bitmaps;
    private Paint paint;
    private Board board;
    private int actualRow, actualColumn, finalRow, finalColumn;
    private boolean whiteTurn;
    private View.OnClickListener onClickListener;

    public ChessView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null && squareSize == 0f) {
            float boardSize = ((canvas.getWidth() <= canvas.getHeight()) ? canvas.getWidth() : canvas.getHeight()) * scale;
            squareSize = boardSize / 8f;
            originX = (canvas.getWidth() - boardSize) / 2f;
            originY = (canvas.getHeight() - boardSize) / 2f;
            initVariables();
        }
        paint.setColor(getResources().getColor(R.color.darkMode));
        canvas.drawPaint(paint);
        initBoard(canvas);
        initPieces(canvas);
        switchText(canvas);
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
        }
        return true;
    }

    private void initVariables() {
        bitmaps = new HashMap<>();
        paint = new Paint(Color.LTGRAY);
        loadBitmaps();
        board = new Board();
        whiteTurn = true;
    }

    private void loadBitmaps() {
        for (Integer id : imgIds) {
            bitmaps.put(id, BitmapFactory.decodeResource(getResources(), id));
        }
    }

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

    private void switchText(Canvas canvas) {
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void movePiece(int actualRow, int actualColumn, int finalRow, int finalColumn) {
        if (finalRow < 1 || finalRow > 8 || finalColumn < 1 || finalColumn > 8) //if goes out of the board
            return;

        Piece actualPiece = null, otherPiece = null;
        for (Piece p : board.getPieceList()) {
            if (p.getColumn() == finalColumn && p.getRow() == finalRow) {
                otherPiece = p;
            }
            if (p.getColumn() == actualColumn && p.getRow() == actualRow) {
                actualPiece = p;
            }
        }
        if (actualPiece == null) {
            return;
        }
        if ((actualPiece.getPlayer().equals(Player.WHITE) && whiteTurn) || (actualPiece.getPlayer().equals(Player.BLACK) && !whiteTurn)) {
            if (otherPiece == null) { //can move
                if (moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn, otherPiece)) {
                    actualPiece.setColumn(finalColumn);
                    actualPiece.setRow(finalRow);
                    whiteTurn = !whiteTurn;
                }
            } else if (otherPiece != null && !otherPiece.getModel().equals(PieceModel.KING)) {
                if (otherPiece.getPlayer().equals(actualPiece.getPlayer())) { //blocks with his own pieces
                    return;
                } else if (!otherPiece.getPlayer().equals(actualPiece.getPlayer())) { //eats and move
                    if (moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn, otherPiece)) {
                        actualPiece.setColumn(finalColumn);
                        actualPiece.setRow(finalRow);
                        board.getPieceList().remove(otherPiece);
                        whiteTurn = !whiteTurn;
                    }
                }
            }
            ChessView chessView = (ChessView) findViewById(R.id.chess_view);
            chessView.invalidate();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean moveRules(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn, Piece otherPiece) {
        switch (piece.getModel()) {
            case PAWN:
                Boolean p = pawnMovement(piece, actualRow, actualColumn, finalRow, finalColumn, otherPiece);
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
                Boolean k = kingMovement(piece, actualRow, actualColumn, finalRow, finalColumn, otherPiece);
                if (k != null) return k;
                break;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Boolean pawnMovement(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn, Piece otherPiece) {
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
                        return true;
                    }
                } else if (piece.getPlayer().equals(Player.BLACK)) {
                    if (finalRow - actualRow <= maxMove && finalRow - actualRow > 0) { //if moves 1 or 2
                        if (finalRow == 8) {
                            piece.setModel(PieceModel.QUEEN);
                            piece.setResID(R.drawable.queenblack);
                        }
                        return true;
                    }
                } else {
                    return false; //if moves back or too much ahead
                }
            } else if (actualColumn + 1 == finalColumn || actualColumn - 1 == finalColumn) { //if trying to eat
                if (piece.getPlayer().equals(Player.WHITE)) {
                    if (actualRow - finalRow <= 1 && actualRow - finalRow > 0) {
                        if (otherPiece.getModel().equals(PieceModel.KING)) {
                            return true;
                        } else if (otherPiece != null) {
                            if (finalRow == 1) {
                                piece.setModel(PieceModel.QUEEN);
                                piece.setResID(R.drawable.queenwhite);
                            }
                            piece.setColumn(finalColumn);
                            piece.setRow(finalRow);
                            board.getPieceList().remove(otherPiece);
                            whiteTurn = !whiteTurn;
                            ChessView chessView = (ChessView) findViewById(R.id.chess_view);
                            chessView.invalidate();
                            return false;
                        }
                    }
                } else if (piece.getPlayer().equals(Player.BLACK)) {
                    if (finalRow - actualRow <= 1 && finalRow - actualRow > 0) {
                        if (otherPiece.getModel().equals(PieceModel.KING)) {
                            return true;
                        } else if (otherPiece != null) {
                            if (finalRow == 1) {
                                piece.setModel(PieceModel.QUEEN);
                                piece.setResID(R.drawable.queenblack);
                            }
                            piece.setColumn(finalColumn);
                            piece.setRow(finalRow);
                            board.getPieceList().remove(otherPiece);
                            whiteTurn = !whiteTurn;
                            ChessView chessView = (ChessView) findViewById(R.id.chess_view);
                            chessView.invalidate();
                            return false;
                        }
                    }
                } else {
                    return false; //if moves back or too much ahead
                }
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Boolean rookMovement(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn) {
        if (!checkPieceInFront(piece, actualRow, actualColumn, finalRow, finalColumn) && !checkPieceInLateral(piece, actualRow, actualColumn, finalRow, finalColumn)) {
            if (actualColumn == finalColumn || actualRow == finalRow)
                return true;
            else
                return false;
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Boolean knightMovement(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn) {
        if (Math.abs(finalColumn - actualColumn) == 2 && Math.abs(finalRow - actualRow) == 1 || Math.abs(finalColumn - actualColumn) == 1 && Math.abs(finalRow - actualRow) == 2)
            return true;
        else
            return false;
    }

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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Boolean kingMovement(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn, Piece otherPiece) {
        if ((Math.abs(finalColumn - actualColumn) == 0 || Math.abs(finalColumn - actualColumn) == 1) && (Math.abs(finalRow - actualRow) == 0 || Math.abs(finalRow - actualRow) == 1)) {
            if (otherPiece != null && !otherPiece.getModel().equals(PieceModel.KING)) {
                for (Piece p : board.getPieceList()) {
                    if (p.getPlayer().equals(otherPiece.getPlayer()) && !p.equals(otherPiece)) {
                        if (moveRules(p, p.getRow(), p.getColumn(), otherPiece.getRow(), otherPiece.getColumn(), otherPiece)) {
                            return false;
                        }
                    }
                }
                piece.setColumn(finalColumn);
                piece.setRow(finalRow);
                board.getPieceList().remove(otherPiece);
                whiteTurn = !whiteTurn;
                ChessView chessView = (ChessView) findViewById(R.id.chess_view);
                chessView.invalidate();
                return false;
            }
            if (otherPiece == null) {
                for (Piece p : board.getPieceList()) {
                    if (!p.getPlayer().equals(piece.getPlayer())) {
                        if (moveRules(p, p.getRow(), p.getColumn(), finalRow, finalColumn, piece)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } else
            return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean checkPieceInFront(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn) {
        boolean eats = false, defending = false;
        if (finalColumn == actualColumn) {
            List<Integer> rows;
            if (actualRow >= finalRow) {
                rows = IntStream.rangeClosed(finalRow, actualRow).boxed().collect(Collectors.toList());
                Collections.reverse(rows);
            } else
                rows = IntStream.rangeClosed(actualRow, finalRow).boxed().collect(Collectors.toList());
            for (Piece p : board.getPieceList()) {
                if (!piece.getPlayer().equals(p.getPlayer()) && finalColumn == p.getColumn() && finalRow == p.getRow() && !piece.getModel().equals(PieceModel.PAWN))
                    eats = true;
                else if (piece.getPlayer().equals(p.getPlayer()) && finalColumn == p.getColumn() && finalRow == p.getRow() && !piece.getModel().equals(PieceModel.PAWN))
                    defending = true;
                else if (!piece.equals(p) && rows.contains(p.getRow()) && actualColumn == p.getColumn()) {
                    return true;
                }
            }
            if (eats) return false;
            if (defending) return false;
        }
        return false;
    }

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
            if(fbDialogue.isShowing())
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
}