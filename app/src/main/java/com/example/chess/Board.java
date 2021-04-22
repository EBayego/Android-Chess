package com.example.chess;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private List<Piece> pieceList;

    public Board() {
        pieceList = new ArrayList<>();
        init();
    }

    private void init() {
        pieceList.add(new Piece(1, 1, Player.BLACK, PieceModel.ROOK, R.drawable.rookblack));
        pieceList.add(new Piece(1, 2, Player.BLACK, PieceModel.KNIGHT, R.drawable.knightblack));
        pieceList.add(new Piece(1, 3, Player.BLACK, PieceModel.BISHOP, R.drawable.bishopblack));
        pieceList.add(new Piece(1, 4, Player.BLACK, PieceModel.QUEEN, R.drawable.queenblack));
        pieceList.add(new Piece(1, 5, Player.BLACK, PieceModel.KING, R.drawable.kingblack));
        pieceList.add(new Piece(1, 6, Player.BLACK, PieceModel.BISHOP, R.drawable.bishopblack));
        pieceList.add(new Piece(1, 7, Player.BLACK, PieceModel.KNIGHT, R.drawable.knightblack));
        pieceList.add(new Piece(1, 8, Player.BLACK, PieceModel.ROOK, R.drawable.rookblack));

        for (int i = 1; i <= 8; i++) {
            pieceList.add(new Piece(2, i, Player.BLACK, PieceModel.PAWN, R.drawable.pawnblack));
        }

        for (int i = 1; i <= 8; i++) {
            pieceList.add(new Piece(7, i, Player.WHITE, PieceModel.PAWN, R.drawable.pawnwhite));
        }

        pieceList.add(new Piece(8, 1, Player.WHITE, PieceModel.ROOK, R.drawable.rookwhite));
        pieceList.add(new Piece(8, 2, Player.WHITE, PieceModel.KNIGHT, R.drawable.knightwhite));
        pieceList.add(new Piece(8, 3, Player.WHITE, PieceModel.BISHOP, R.drawable.bishopwhite));
        pieceList.add(new Piece(8, 4, Player.WHITE, PieceModel.QUEEN, R.drawable.queenwhite));
        pieceList.add(new Piece(8, 5, Player.WHITE, PieceModel.KING, R.drawable.kingwhite));
        pieceList.add(new Piece(8, 6, Player.WHITE, PieceModel.BISHOP, R.drawable.bishopwhite));
        pieceList.add(new Piece(8, 7, Player.WHITE, PieceModel.KNIGHT, R.drawable.knightwhite));
        pieceList.add(new Piece(8, 8, Player.WHITE, PieceModel.ROOK, R.drawable.rookwhite));
    }

    public List<Piece> getPieceList() {
        return pieceList;
    }
}
