package com.example.chess;

public class Piece {
    private int row;
    private int column;
    private Player player;
    private PieceModel model;
    private int resID;

    public Piece(int row, int column, Player player, PieceModel model, int resID) {
        this.row = row;
        this.column = column;
        this.player = player;
        this.model = model;
        this.resID = resID;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public Player getPlayer() {
        return player;
    }

    public PieceModel getModel() {
        return model;
    }

    public void setModel(PieceModel model) {
        this.model = model;
    }

    public int getResID() {
        return resID;
    }

    public void setResID(int resID) {
        this.resID = resID;
    }
}
