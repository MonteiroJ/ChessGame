package piece;

import main.GamePanel;
import main.Type;

public class King extends Piece {
    public King(int color, int col, int row) {
        super(color, col, row);
        type = Type.KING;
        if (color == GamePanel.WHITE) {
            image = getImage("/pieces/white_king");
        }
        else {
            image = getImage("/pieces/black_king");
        }
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if(isWithinBoard(targetCol, targetRow)) {
            // MOVEMENT
            if(
                    Math.abs(targetCol - preCol) + Math.abs(targetRow - preRow) == 1 ||
                    Math.abs(targetCol - preCol) * Math.abs(targetRow - preRow) == 1
            ) {
                if(isValidSquare(targetCol, targetRow)) {
                    return true;
                }
            }

            // CASTLING
            if(moved == false) {
                // Right castling
                if(targetCol == preCol + 2 && targetRow == preRow && pieceIsOnStraightLine(targetCol, targetRow) == false) {
                    for (Piece piece : GamePanel.simPieces) {
                        if(piece.col == preCol + 3 && piece.row == preRow && piece.moved == false) {
                            GamePanel.castlingPiece = piece;
                            return true;
                        }
                    }
                }

                // Left castling
                if(targetCol == preCol-2 && targetRow == preRow && pieceIsOnStraightLine(targetCol, targetRow) == false) {
                    Piece pieces[] = new Piece[2];
                    for (Piece piece : GamePanel.simPieces) {
                        if(piece.col == preCol-3 && piece.row == targetRow) {
                            pieces[0] = piece;
                        }
                        if(piece.col == preCol-4 && piece.row == targetRow) {
                            pieces[1] = piece;
                        }

                        if(pieces[0] == null && pieces[1] != null && pieces[1].moved == false) {
                            GamePanel.castlingPiece = pieces[1];
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
