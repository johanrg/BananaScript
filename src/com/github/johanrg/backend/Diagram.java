package com.github.johanrg.backend;

import com.github.johanrg.ast.ASTNode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author Johan Gustafsson
 * @since 7/14/2016.
 */
public class Diagram {
    private BufferedImage image;
    private Graphics2D canvas;
    private Font titleFont;
    private Font extraFont;
    private final ASTNode root;

    public Diagram(ASTNode root) {
        this.root = root;
        titleFont = new Font("Arial", Font.BOLD, 12);
        extraFont = new Font("TimesRoman", Font.BOLD, 10);

        int width = 1000;
        int height = 600;

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        canvas = image.createGraphics();
        canvas.setBackground(Color.WHITE);
        canvas.clearRect(0, 0, width, height);

        Rectangle2D box1 = getBoxBounds("ASTBinaryOperator", "+", 200, 100);
        Rectangle2D box2 = getBoxBounds("ASTLiteral", "10", 170, (int) (box1.getY() + box1.getHeight() + 20));
        drawLine(box1, box2);
        drawBox("ASTBinaryOperator", "+", 200, 100);
        drawBox("ASTLiteral", "10", (int) box2.getX(), (int) box2.getY());
        try {
            ImageIO.write(image, "PNG", new File("z:\\image.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Rectangle2D getBoxBounds(String title, String extra, int x, int y) {

        canvas.setFont(titleFont);
        Rectangle2D titleRect = canvas.getFontMetrics().getStringBounds(title, canvas);

        int width = (int) titleRect.getWidth() + 10;
        int height = (int) titleRect.getHeight() + 10;
        int titleY = y + canvas.getFontMetrics().getAscent() + 5;

        canvas.setFont(extraFont);
        Rectangle2D extraRect = canvas.getFontMetrics().getStringBounds(extra, canvas);
        if (width < extraRect.getWidth() + 10) {
            width = (int) extraRect.getWidth() + 10;
        }
        int extraY = titleY + (int) extraRect.getHeight() + 5;
        height += (int) extraRect.getHeight() + 5;
        return new Rectangle2D.Double(x, y, width, height);
    }

    private void drawBox(String title, String extra, int x, int y) {

        canvas.setFont(titleFont);
        Rectangle2D titleRect = canvas.getFontMetrics().getStringBounds(title, canvas);

        int width = (int) titleRect.getWidth() + 10;
        int height = (int) titleRect.getHeight() + 10;
        int titleX = x + 5;
        int titleY = y + canvas.getFontMetrics().getAscent() + 5;

        canvas.setFont(extraFont);
        Rectangle2D extraRect = canvas.getFontMetrics().getStringBounds(extra, canvas);
        if (width < extraRect.getWidth() + 10) {
            width = (int) extraRect.getWidth() + 10;
        }
        int extraX = x + (width / 2) - (int) (extraRect.getWidth() / 2);
        int extraY = titleY + (int) extraRect.getHeight() + 5;
        height += (int) extraRect.getHeight() + 5;

        canvas.setColor(Color.ORANGE);
        canvas.fillRect(x, y, width, height);
        canvas.setColor(Color.BLACK);
        canvas.drawRect(x, y, width, height);

        canvas.setFont(titleFont);
        canvas.drawString(title, titleX, titleY);

        canvas.setFont(extraFont);

        canvas.drawString(extra, extraX, extraY);
    }

    private void drawLine(Rectangle2D box1, Rectangle2D box2) {
        canvas.setColor(Color.BLACK);
        canvas.drawLine((int) box1.getCenterX(), (int) box1.getCenterY(), (int) box2.getCenterX(), (int) box2.getCenterY());
    }
}
