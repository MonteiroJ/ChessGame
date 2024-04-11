package piece;

import main.GamePanel;
import main.Type;

public class Pawn extends Piece {

    public Pawn(int color, int col, int row) {
        super(color, col, row);
        type = Type.PAWN;
        if (color == GamePanel.WHITE) {
            image = getImage("/pieces/white_pawn");
        }
        else {
            image = getImage("/pieces/black_pawn");
        }
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        // Difine the move value based on its color
        if(isWithinBoard(targetCol, targetRow) && isSameSquare(targetCol, targetRow) == false) {
            int moveValue = 0;
            if(color == GamePanel.WHITE) {
                moveValue = -1;
            }
            else {
                moveValue = 1;
            }

            // Check the hitting piece
            hittingPiece = getHittingPiece(targetCol, targetRow);

            // 1 square movement
            if(targetCol == preCol && targetRow == preRow + moveValue && hittingPiece == null) {
                return true;
            }

            // 2 square movement
            if(targetCol == preCol && targetRow == preRow + moveValue * 2 && hittingPiece == null && moved == false && pieceIsOnStraightLine(targetCol, targetRow) == false) {
                return true;
            }

            // Diagonal movement & capture (if a piece is on a square diagonally in front of it
            if(Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue && hittingPiece != null && hittingPiece.color != color) {
                return true;
            }

            // En passant
            if(Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue) {
                for (Piece piece : GamePanel.simPieces) {
                    if(piece.col == targetCol && piece.row == preRow && piece.twoStepped == true) {
                        hittingPiece = piece;
                        return true;
                    }
                }
            }

            // If pawn has reach
        }
        return false;
    }
}
