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
    BufferedImage image;
    Graphics2D canvas;

    private final ASTNode root;

    public Diagram(ASTNode root) {
        this.root = root;

        int width = 1000;
        int height = 600;

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        canvas = image.createGraphics();
        canvas.setBackground(Color.WHITE);
        canvas.clearRect(0, 0, width, height);

        Font font = new Font("TimesRoman", Font.BOLD, 20);
        canvas.setFont(font);
        drawBox("Hello World this is just a test to see if this is better or not", "10", 60, 100);
        try {
            ImageIO.write(image, "PNG", new File("z:\\image.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawBox(String title, String extra, int x, int y) {
        Font titleFont = new Font("Arial", Font.BOLD, 20);
        canvas.setFont(titleFont);
        FontMetrics fontMetrics = canvas.getFontMetrics();
        Rectangle2D r = fontMetrics.getStringBounds(title, canvas);
        int width = (int) r.getWidth() + 20;
        int height = fontMetrics.getAscent() + 20;
        canvas.setColor(Color.ORANGE);
        canvas.fillRect(x, y, width, height);
        canvas.setColor(Color.BLACK);
        canvas.drawRect(x, y, width, height);
        canvas.drawString(title, x + 10, y + fontMetrics.getAscent() + 10);

        Font extraFont = new Font("TimesRoman", Font.BOLD, 15);
        canvas.setFont(extraFont);
        r = fontMetrics.getStringBounds(extra, canvas);
        width = (int) r.getWidth() + 20;
        height += fontMetrics.getAscent();
        canvas.drawString(extra, x, y + height);
    }
}
