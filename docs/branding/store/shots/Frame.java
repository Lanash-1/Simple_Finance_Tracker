import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Frames raw 1080x2400 emulator screenshots as 1080x1920 (9:16) Play Store phone screenshots:
 * charcoal canvas, one-line caption, the screenshot scaled and clipped to a rounded phone shape.
 *
 *   java docs/branding/store/shots/Frame.java docs/branding/store/shots
 *
 * Edit the CAPTIONS table to change order or wording; the first column is the raw file name.
 */
public class Frame {
    static final String[][] CAPTIONS = {
        {"dark-home", "See where you stand, every month", "01-home-dark"},
        {"light-add", "Add a transaction in seconds", "02-add-transaction"},
        {"light-history-filters", "Search and filter your whole history", "03-history-filters"},
        {"light-insights", "Budgets and monthly cash flow", "04-insights"},
        {"dark-accounts", "Cash, bank and card, side by side", "05-accounts-dark"},
        {"light-home", "Offline. Private. No account needed.", "06-home-light"},
    };
    static final int W = 1080, H = 1920;
    static final Color CHARCOAL = new Color(0x11, 0x16, 0x14), MINT = new Color(0xA7, 0xF3, 0xD0), EMERALD = new Color(0x34, 0xD3, 0x99);

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : ".");
        for (String[] row : CAPTIONS) {
            BufferedImage shot = ImageIO.read(new File(dir, "raw/" + row[0] + ".png"));
            BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(CHARCOAL); g.fillRect(0, 0, W, H);

            // Caption block
            g.setColor(EMERALD); g.setFont(new Font("Helvetica Neue", Font.BOLD, 30));
            drawCentered(g, "POCKETSUM", 150);
            g.setColor(Color.WHITE);
            int size = 56;
            do { g.setFont(new Font("Helvetica Neue", Font.BOLD, size--)); } while (g.getFontMetrics().stringWidth(row[1]) > W - 120 && size > 30);
            drawCentered(g, row[1], 240);

            // Screenshot: fit below the caption, keep aspect, rounded corners, thin bezel.
            int top = 330, bottom = H + 60; // let the phone run off the bottom edge like real store shots
            int availH = bottom - top;
            double scale = Math.min((W - 180) / (double) shot.getWidth(), availH / (double) shot.getHeight());
            int sw = (int) Math.round(shot.getWidth() * scale), sh = (int) Math.round(shot.getHeight() * scale);
            int x = (W - sw) / 2, y = top;
            int r = 64;
            g.setColor(new Color(0x2A, 0x30, 0x2D));
            g.fill(new RoundRectangle2D.Double(x - 14, y - 14, sw + 28, sh + 28, r + 14, r + 14));
            Shape clip = new RoundRectangle2D.Double(x, y, sw, sh, r, r);
            g.setClip(clip);
            g.drawImage(shot, x, y, sw, sh, null);
            g.setClip(null);
            g.dispose();
            ImageIO.write(out, "png", new File(dir, row[2] + ".png"));
            System.out.println(row[2] + ".png");
        }
    }

    static void drawCentered(Graphics2D g, String text, int baseline) {
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (W - w) / 2, baseline);
    }
}
