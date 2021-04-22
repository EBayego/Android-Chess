package com.example.chess;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Paint;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
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
    private Paint paint, paintPieces;
    private Board board;

    private int actualRow, actualColumn, finalRow, finalColumn;

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
        initBoard(canvas);
        initPieces(canvas);
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
        paintPieces = new Paint();
        loadBitmaps();
        board = new Board();
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void movePiece(int actualRow, int actualColumn, int finalRow, int finalColumn) {
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
        if (otherPiece == null) { //can move
            if (moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn)) {
                actualPiece.setColumn(finalColumn);
                actualPiece.setRow(finalRow);
            }
        } else if (otherPiece != null) {
            if (otherPiece.getPlayer().equals(actualPiece.getPlayer())) { //blocks with his own pieces
                return;
            } else if (!otherPiece.getPlayer().equals(actualPiece.getPlayer())) { //eats and move
                if (moveRules(actualPiece, actualRow, actualColumn, finalRow, finalColumn)) {
                    actualPiece.setColumn(finalColumn);
                    actualPiece.setRow(finalRow);
                    board.getPieceList().remove(otherPiece);
                }
            }
        }
        ChessView chessView = (ChessView) findViewById(R.id.chess_view);
        chessView.invalidate();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean moveRules(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn) {
        switch (piece.getModel()) {
            case PAWN:
                if (!checkPieceInFront(piece, actualRow, actualColumn, finalRow, finalColumn)) {
                    //Movement
                    if (actualColumn == finalColumn) { //if moving forward
                        int maxMove = 1;
                        if (checkPawnFirstMove(piece)) {
                            maxMove = 2;
                        }
                        if (piece.getPlayer().equals(Player.WHITE)) {
                            if (actualRow - finalRow <= maxMove && actualRow - finalRow > 0) { //if moves 1 or 2
                                return true;
                            }
                        } else if (piece.getPlayer().equals(Player.BLACK)) {
                            if (finalRow - actualRow <= maxMove && finalRow - actualRow > 0) { //if moves 1 or 2
                                return true;
                            }
                        } else {
                            return false; //if moves back or too much ahead
                        }
                    } else if (actualColumn + 1 == finalColumn || actualColumn - 1 == finalColumn) { //if trying to eat
                        if (piece.getPlayer().equals(Player.WHITE)) {
                            if (actualRow - finalRow <= 1 && actualRow - finalRow > 0) {
                                Piece deletePiece = null;
                                for (Piece p : board.getPieceList()) {
                                    if (!p.getPlayer().equals(piece.getPlayer()) && p.getColumn() == finalColumn && p.getRow() == finalRow) {
                                        deletePiece = p;
                                    }
                                }
                                if (deletePiece != null){
                                    piece.setColumn(finalColumn);
                                    piece.setRow(finalRow);
                                    board.getPieceList().remove(deletePiece);
                                    ChessView chessView = (ChessView) findViewById(R.id.chess_view);
                                    chessView.invalidate();
                                    return false;
                                }
                            }
                        } else if (piece.getPlayer().equals(Player.BLACK)) {
                            if (finalRow - actualRow <= 1 && finalRow - actualRow > 0) {
                                Piece deletePiece = null;
                                for (Piece p : board.getPieceList()) {
                                    if (!p.getPlayer().equals(piece.getPlayer()) && p.getColumn() == finalColumn && p.getRow() == finalRow) {
                                        deletePiece = p;
                                    }
                                }
                                if (deletePiece != null){
                                    piece.setColumn(finalColumn);
                                    piece.setRow(finalRow);
                                    board.getPieceList().remove(deletePiece);
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
                break;
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean checkPieceInFront(Piece piece, int actualRow, int actualColumn, int finalRow, int finalColumn) {
        if (finalColumn == actualColumn) {
            List<Integer> rows;
            if (actualRow >= finalRow)
                rows = IntStream.rangeClosed(finalRow, actualRow).boxed().collect(Collectors.toList());
            else
                rows = IntStream.rangeClosed(actualRow, finalRow).boxed().collect(Collectors.toList());
            for (Piece p : board.getPieceList()) {
                if (!piece.equals(p) && rows.contains(p.getRow()) && actualColumn == p.getColumn()) {
                    if (!piece.getPlayer().equals(p.getPlayer()) && finalColumn == p.getColumn() && finalRow == p.getRow() && !piece.getModel().equals(PieceModel.PAWN))
                        return false;
                    return true;
                }
            }
        }
        return false;
    }
}
