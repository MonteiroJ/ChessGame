package main;

import piece.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class GamePanel extends JPanel implements Runnable {

    public static final int WIDTH = 1100;
    public static final int HEIGHT = 800;
    final int FPS = 60;
    Thread gameThread;
    Board board = new Board();
    Mouse mouse = new Mouse();

    // PIECES
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    ArrayList<Piece> promotionPieces = new ArrayList<>();
    Piece activePiece, checkingPiece;
    public static Piece castlingPiece;

    // COLOR
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    int currentColor = WHITE;

    // BOOLEANS
    boolean canMove;
    boolean validSquare;
    boolean promotion;
    boolean gameOver;
    boolean stalemate;

    public GamePanel () {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        addMouseMotionListener(mouse);
        addMouseListener(mouse);

        setPieces();
        //testPromotion();
        //testIllegal();


        copyPieces(pieces, simPieces);
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setPieces () {
        // White
        pieces.add(new Pawn(WHITE, 0, 6));
        pieces.add(new Pawn(WHITE, 1, 6));
        pieces.add(new Pawn(WHITE, 2, 6));
        pieces.add(new Pawn(WHITE, 3, 6));
        pieces.add(new Pawn(WHITE, 4, 6));
        pieces.add(new Pawn(WHITE, 5, 6));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Pawn(WHITE, 7, 6));
        pieces.add(new Rook(WHITE, 0, 7));
        pieces.add(new Rook(WHITE, 7, 7));
        pieces.add(new Knight(WHITE, 1, 7));
        pieces.add(new Knight(WHITE, 6, 7));
        pieces.add(new Bishop(WHITE, 2, 7));
        pieces.add(new Bishop(WHITE, 5, 7));
        pieces.add(new Queen(WHITE, 3, 7));
        pieces.add(new King(WHITE, 4, 7));

        // Black
        pieces.add(new Pawn(BLACK, 0, 1));
        pieces.add(new Pawn(BLACK, 1, 1));
        pieces.add(new Pawn(BLACK, 2, 1));
        pieces.add(new Pawn(BLACK, 3, 1));
        pieces.add(new Pawn(BLACK, 4, 1));
        pieces.add(new Pawn(BLACK, 5, 1));
        pieces.add(new Pawn(BLACK, 6, 1));
        pieces.add(new Pawn(BLACK, 7, 1));
        pieces.add(new Rook(BLACK, 0, 0));
        pieces.add(new Rook(BLACK, 7, 0));
        pieces.add(new Knight(BLACK, 1, 0));
        pieces.add(new Knight(BLACK, 6, 0));
        pieces.add(new Bishop(BLACK, 2, 0));
        pieces.add(new Bishop(BLACK, 5, 0));
        pieces.add(new Queen(BLACK, 3, 0));
        pieces.add(new King(BLACK, 4, 0));
    }
    // TODO : delete this method at the end
    public void testPromotion() {
        pieces.add(new Pawn(WHITE, 0, 3));
        pieces.add(new Pawn(BLACK, 5, 4));
    }
    // TODO : delete this method at the end
    public void testIllegal() {
        pieces.add(new Pawn(WHITE, 7, 6));
        pieces.add(new King(WHITE, 3, 7));
        pieces.add(new King(BLACK, 0, 3));
        pieces.add(new Bishop(BLACK, 1, 4));
        pieces.add(new Queen(BLACK, 4, 5));

    }

    private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        for (int i = 0; i < source.size(); i++) {
            target.add(source.get(i));
        }
    }

    @Override
    public void run() {
        // GAME LOOP
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while(gameThread != null) {
            currentTime = System.nanoTime();

            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
               update();
               repaint();
               delta--;
            }
        }
    }

    private void update() {

        if(promotion) {
            promoting();
        }
        else if (gameOver == false && stalemate == false){
            ///// MOUSE BUTTON PRESSED /////
            if(mouse.pressed) {
                if(activePiece == null) {
                    // If the activePiece is null, check if you can pick up the piece
                    for(Piece piece : simPieces) {
                        // If the mouse is on an ally piece, pick it up as the activePiece
                        if(
                                piece.color == currentColor &&
                                        piece.col == mouse.x / Board.SQUARE_SIZE &&
                                        piece.row == mouse.y / Board.SQUARE_SIZE) {
                            activePiece = piece;
                        }
                    }
                }
                else {
                    // If the player is holding a piece, simulate the move
                    simulate();
                }
            }

            ///// MOUSE BUTTON RELEASED /////
            if(mouse.pressed == false) {
                if(activePiece != null) {
                    if(validSquare) {
                        // MOVE CONFIRMED

                        // Update the piece list in case a piece has been captured and removed during the simulation
                        copyPieces(simPieces, pieces);
                        activePiece.updatePosition();
                        if(castlingPiece != null) {
                            castlingPiece.updatePosition();
                        }

                        if(isKingInCheck() && isCheckmate()) {
                            gameOver = true;
                        }
                        else if(isStalemate() && isKingInCheck() == false) {
                            stalemate = true;
                        }
                        // The game is still going on
                        else {
                            if(canPromote()) {
                                promotion = true;
                            }
                            else {
                                changePlayer();
                            }
                        }
                    }
                    else {
                        // The move is not valid, reset everything
                        copyPieces(pieces, simPieces);
                        activePiece.resetPosition();
                        activePiece = null;
                    }
                }
            }
        }
    }

    private void simulate() {
        canMove = false;
        validSquare = false;

        // Reset the piece list in every loop
        // This is basically for restoring the removed pieces during the simulation
        copyPieces(pieces, simPieces);

        // Reset the castling piece's position
        if(castlingPiece != null) {
            castlingPiece.col = castlingPiece.preCol;
            castlingPiece.x = castlingPiece.getX(castlingPiece.col);
            castlingPiece = null;
        }

        // If a piece is being held, update its position
        activePiece.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activePiece.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activePiece.col = activePiece.getCol(activePiece.x);
        activePiece.row = activePiece.getRow(activePiece.y);

        // Check if the piece is hovering over a reachable square
        if(activePiece.canMove(activePiece.col, activePiece.row)) {
            canMove = true;

            // If hitting a piece, remove it from the list
            if(activePiece.hittingPiece != null) {
                simPieces.remove(activePiece.hittingPiece.getIndex());
            }
            checkCastling();
            if(isIllegal(activePiece) == false && opponentCanCaptureKing() == false) {
                validSquare = true;
            }
        }
    }

    private boolean isIllegal(Piece king) {
        if(king.type == Type.KING) {
            for(Piece piece : simPieces) {
                if(piece != king && piece.color != king.color && piece.canMove(king.col, king.row)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean opponentCanCaptureKing() {
        Piece king = getKing(false);
        for(Piece piece : simPieces) {
            if(piece.color != king.color && piece.canMove(king.col, king.row)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKingInCheck() {
        Piece king = getKing(true);

        if(activePiece.canMove(king.col, king.row)) {
            checkingPiece = activePiece;
            return true;
        }
        else {
            checkingPiece = null;
        }
        return false;
    }

    private Piece getKing(boolean opponent) {
        Piece king = null;
        for(Piece piece : simPieces) {
            if(opponent) {
                if(piece.type == Type.KING && piece.color != currentColor) {
                    king = piece;
                }
            }
            else {
                if(piece.type == Type.KING && piece.color == currentColor) {
                    king = piece;
                }
            }
        }
        return king;
    }

    private boolean isCheckmate() {
        Piece king = getKing(true);

        if(kingCanMove(king)) {
            return false;
        }
        else {
            // But you still have a chance
            // Check if the player can block the attack with your piece

            // Check the position of the checking piece and the king in check
            int colDiff = Math.abs(checkingPiece.col - king.col);
            int rowDiff = Math.abs(checkingPiece.row - king.row);

            // The checking piece is attacking vertically
            if (colDiff == 0) {
                // The checking piece is above the king
                if(checkingPiece.row < king.row) {
                    for(int row = checkingPiece.row; row < king.row; row++) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(checkingPiece.col, row)) {
                                return false;
                            }
                        }
                    }
                }
                // The checking piece is below the king
                if(checkingPiece.row > king.row) {
                    for(int row = checkingPiece.row; row > king.row; row--) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(checkingPiece.col, row)) {
                                return false;
                            }
                        }
                    }
                }
            }
            // The attacking piece is attacking horizontally
            else if (rowDiff == 0) {
                // The checking piece is to the left
                if(checkingPiece.col < king.col) {
                    for(int col = checkingPiece.col; col < king.col; col++) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(col, checkingPiece.row)) {
                                return false;
                            }
                        }
                    }
                }
                // The checking piece is to the right
                if(checkingPiece.col > king.col) {
                    for(int col = checkingPiece.col; col > king.col; col--) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(col, checkingPiece.row)) {
                                return false;
                            }
                        }
                    }
                }

            }
            // The attacking piece is attacking diagonally
            else if (colDiff == rowDiff) {
                // The checking Piece is above the king
                if(checkingPiece.row < king.row) {
                    //The checking piece is in the upper left
                    if(checkingPiece.col < king.col) {
                        for(int col = checkingPiece.col, row = checkingPiece.row; col < king.col; col++, row++) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                    // The checking piece is in the upper right
                    if(checkingPiece.col > king.col) {
                        for(int col = checkingPiece.col, row = checkingPiece.row; col > king.col; col--, row++) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
                // The checking Piece is below the king
                if(checkingPiece.row > king.row) {
                    //The checking piece is in the lower left
                    if(checkingPiece.col < king.col) {
                        for(int col = checkingPiece.col, row = checkingPiece.row; col < king.col; col++, row--) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                    // The checking piece is in the lower right
                    if(checkingPiece.col > king.col) {
                        for(int col = checkingPiece.col, row = checkingPiece.row; col > king.col; col--, row--) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean kingCanMove(Piece king) {
        // Simulate if there is any square where the king can move
        if(isValidMove(king, -1, -1)) { return true; }
        if(isValidMove(king, 0, -1)) { return true; }
        if(isValidMove(king, 1, -1)) { return true; }
        if(isValidMove(king, -1, 0)) { return true; }
        if(isValidMove(king, 1, 0)) { return true; }
        if(isValidMove(king, -1, 1)) { return true; }
        if(isValidMove(king, 0, 1)) { return true; }
        if(isValidMove(king, 1, 1)) { return true; }

        return false;
    }

    private boolean isValidMove(Piece king, int colPlus, int rowPlus) {
        boolean isValidMove = false;

        // Update the king's position for a second
        king.col += colPlus;
        king.row += rowPlus;

        if(king.canMove(king.col, king.row)) {
            if(king.hittingPiece != null) {
                simPieces.remove(king.hittingPiece.getIndex());
            }
            if(isIllegal(king) == false) {
                isValidMove = true;
            }
        }
        // Reset the king's position and restore the removed piece
        king.resetPosition();
        copyPieces(pieces, simPieces);

        return isValidMove;
    }

    private boolean isStalemate() {
        int count = 0;
        // Count the number of pieces
        for(Piece piece : simPieces) {
            if(piece.color != currentColor) {
                count++;
            }
        }

        // If the only piece (the king) is left
        if(count == 1) {
            if(!kingCanMove(getKing(true))) {
                return true;
            }
        }

        // If there's only king on the board
        if(simPieces.size() == 2) {
            return true;
        }
        return false;
    }

    private void checkCastling() {
        if(castlingPiece != null) {
            if(castlingPiece.col == 0) {
                castlingPiece.col += 3;
            }
            else if (castlingPiece.col == 7) {
                castlingPiece.col -= 2;
            }
            castlingPiece.x = castlingPiece.getX(castlingPiece.col);
        }
    }

    private void changePlayer() {
        if(currentColor == WHITE) {
            currentColor = BLACK;
            // Reset black's two stepped status
            for(Piece piece : pieces) {
                if(piece.color == BLACK) {
                    piece.twoStepped = false;
                }
            }
        }
        else {
            currentColor = WHITE;
            // Reset black's two stepped status
            for(Piece piece : pieces) {
                if(piece.color == WHITE) {
                    piece.twoStepped = false;
                }
            }
        }
        activePiece = null;
    }

    private boolean canPromote() {
        if(activePiece.type == Type.PAWN) {
            if(currentColor == WHITE && activePiece.row == 0 || currentColor == BLACK && activePiece.row == 7) {
                promotionPieces.clear();
                promotionPieces.add(new Rook(currentColor, 9, 2));
                promotionPieces.add(new Knight(currentColor,9 , 3));
                promotionPieces.add(new Bishop(currentColor,9 , 4));
                promotionPieces.add(new Queen(currentColor,9 , 5));
                return true;
            }
        }
        return false;
    }

    private void promoting() {
        if(mouse.pressed) {
            for(Piece piece : promotionPieces) {
                if(piece.col == mouse.x/Board.SQUARE_SIZE && piece.row == mouse.y/Board.SQUARE_SIZE) {
                    switch(piece.type){
                        case ROOK -> simPieces.add(new Rook(currentColor, activePiece.col, activePiece.row));
                        case KNIGHT -> simPieces.add(new Knight(currentColor, activePiece.col, activePiece.row));
                        case BISHOP -> simPieces.add(new Bishop(currentColor, activePiece.col, activePiece.row));
                        case QUEEN -> simPieces.add(new Queen(currentColor, activePiece.col, activePiece.row));
                        default -> {
                            break;
                        }
                    }
                    simPieces.remove(activePiece.getIndex());
                    copyPieces(simPieces, pieces);
                    activePiece = null;
                    promotion = false;
                    changePlayer();
                }
            }
        }
    }

    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D graphics2D = (Graphics2D) graphics;

        // Board
        board.draw(graphics2D);

        // PIECES
        for (Piece piece : simPieces) {
            piece.draw(graphics2D);
        }

        if (activePiece != null) {
            if(canMove) {
                if(isIllegal(activePiece) || opponentCanCaptureKing()) {
                    graphics2D.setColor(Color.GRAY);
                    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    graphics2D.fillRect(activePiece.col * Board.SQUARE_SIZE, activePiece.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
                else {
                    graphics2D.setColor(Color.WHITE);
                    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    graphics2D.fillRect(activePiece.col * Board.SQUARE_SIZE, activePiece.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
            }

            // Draw the active piece in the end so it won't be hidden by the board or the colored square
            activePiece.draw(graphics2D);
        }

        // STATUS MESSASGE
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setFont(new Font("Book Antiqua", Font.PLAIN, 40));
        graphics2D.setColor(Color.WHITE);

        if(promotion) {
            graphics2D.drawString("Promote to:", 840, 150);
            for (Piece piece : promotionPieces) {
                graphics2D.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
            }
        }
        else {
            if(currentColor == WHITE) {
                graphics2D.drawString("White's trun", 840, 550);
                if(checkingPiece != null && checkingPiece.color == BLACK) {
                    graphics2D.setColor(Color.RED);
                    graphics2D.drawString("The King", 840, 650);
                    graphics2D.drawString("is in check !", 840, 700);
                }
            }
            else {
                graphics2D.drawString("Black's trun", 840, 250);
                if(checkingPiece != null && checkingPiece.color == WHITE) {
                    graphics2D.setColor(Color.RED);
                    graphics2D.drawString("The King", 840, 100);
                    graphics2D.drawString("is in check !", 840, 150);
                }
            }

            if(gameOver) {
                String str = "";
                if(currentColor == WHITE) {
                    str = "White Wins";
                }
                else {
                    str = "Black Wins";
                }
                graphics2D.setFont(new Font("Arial", Font.PLAIN, 90));
                graphics2D.setColor(Color.GREEN);
                graphics2D.drawString(str, 200, 420);
            }

            if(stalemate) {
                graphics2D.setFont(new Font("Arial", Font.PLAIN, 90));
                graphics2D.setColor(Color.LIGHT_GRAY);
                graphics2D.drawString("Stalemate", 200, 420);
            }
        }
    }
}
