import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Renders every Pocketsum PNG asset from the same geometry as pocketsum-icon.svg /
 * ic_launcher_foreground.xml, so no external SVG tool is needed.
 *
 *   java docs/branding/logo/Render.java <repo root>
 */
public class Render {
    static final Color CHARCOAL = new Color(0x111614);
    static final Color EMERALD = new Color(0x10B981);
    static final Color EMERALD_DEEP = new Color(0x059669);
    static final Color MINT = new Color(0xA7F3D0);
    static final Color OFF_WHITE = new Color(0xF7F8F7);
    static final Color MUTED = new Color(0xB0C0BA);
    static final Color EMERALD_LIGHT = new Color(0x34D399);

    /** Draws the mark in 108-unit icon space. `withBackground` false = glyph only (transparent). */
    static void drawMark(Graphics2D g, boolean withBackground, Color bg) {
        if (withBackground) { g.setColor(bg); g.fill(new Rectangle2D.Double(-1, -1, 110, 110)); }
        g.setColor(MINT);
        g.fill(new Ellipse2D.Double(41, 33, 26, 26));
        Path2D plus = new Path2D.Double();
        plus.moveTo(52.5, 35.5); plus.lineTo(55.5, 35.5); plus.lineTo(55.5, 40); plus.lineTo(60, 40);
        plus.lineTo(60, 43); plus.lineTo(55.5, 43); plus.lineTo(55.5, 47.5); plus.lineTo(52.5, 47.5);
        plus.lineTo(52.5, 43); plus.lineTo(48, 43); plus.lineTo(48, 40); plus.lineTo(52.5, 40); plus.closePath();
        g.setColor(CHARCOAL);
        g.fill(plus);
        Path2D pocket = new Path2D.Double();
        pocket.moveTo(30, 50); pocket.lineTo(78, 50); pocket.lineTo(78, 68);
        pocket.append(new Arc2D.Double(54, 56, 24, 24, 0, -90, Arc2D.OPEN), true);
        pocket.lineTo(42, 80);
        pocket.append(new Arc2D.Double(30, 56, 24, 24, 270, -90, Arc2D.OPEN), true);
        pocket.closePath();
        g.setColor(EMERALD);
        g.fill(pocket);
        g.setColor(EMERALD_DEEP);
        g.fill(new Rectangle2D.Double(30, 50, 48, 4.5));
    }

    static Graphics2D gfx(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g;
    }

    /** Square icon = the 18..90 crop of the 108 grid. mask: null (square), "round" (corner radius 20%), "circle". */
    static BufferedImage icon(int size, String mask, boolean opaque) { return icon(size, mask, opaque, CHARCOAL); }

    static BufferedImage icon(int size, String mask, boolean opaque, Color bg) {
        BufferedImage img = new BufferedImage(size, size, opaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gfx(img);
        if (mask != null) {
            Shape clip = mask.equals("circle") ? new Ellipse2D.Double(0, 0, size, size)
                    : new RoundRectangle2D.Double(0, 0, size, size, size * 0.2, size * 0.2);
            g.setClip(clip);
        }
        double s = size / 72.0;
        g.transform(AffineTransform.getScaleInstance(s, s));
        g.translate(-18, -18);
        drawMark(g, true, bg);
        g.dispose();
        return img;
    }

    /** Transparent glyph (no background) centred in a square; used for the iOS launch screen. */
    static BufferedImage glyph(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gfx(img);
        double s = size / 72.0;
        g.transform(AffineTransform.getScaleInstance(s, s));
        g.translate(-18, -18);
        drawMark(g, false, null);
        g.dispose();
        return img;
    }

    static Font font(int style, float size) {
        for (String name : new String[]{"SF Pro Display", "Helvetica Neue", "Inter", "Roboto", "SansSerif"}) {
            Font f = new Font(name, style, Math.round(size));
            if (!f.getFamily().equals("Dialog") || name.equals("SansSerif")) return f.deriveFont(size);
        }
        return new Font("SansSerif", style, Math.round(size));
    }

    static BufferedImage featureGraphic() {
        int w = 1024, h = 500;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = gfx(img);
        g.setColor(CHARCOAL); g.fillRect(0, 0, w, h);
        // faint emerald glow behind the mark
        g.setPaint(new RadialGradientPaint(new Point2D.Double(250, 250), 320f,
                new float[]{0f, 1f}, new Color[]{new Color(0x10B981, false).darker().darker(), CHARCOAL}));
        g.fillRect(0, 0, w, h);
        BufferedImage ic = icon(280, "round", false, new Color(0x1B211E));
        g.drawImage(ic, 110, 110, null);
        g.setColor(new Color(0x2A302D));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(110, 110, 280, 280, 56, 56));
        g.setFont(font(Font.BOLD, 96f));
        g.setColor(OFF_WHITE);
        int x = 440, y = 265;
        String a = "Pocket", b = "sum";
        g.drawString(a, x, y);
        int aw = g.getFontMetrics().stringWidth(a);
        g.setColor(EMERALD_LIGHT);
        g.drawString(b, x + aw, y);
        g.setFont(font(Font.PLAIN, 34f));
        g.setColor(MUTED);
        g.drawString("Simple, offline money tracking.", x + 2, y + 62);
        g.dispose();
        return img;
    }

    static void save(BufferedImage img, File f) throws Exception {
        f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("wrote " + f + " " + img.getWidth() + "x" + img.getHeight());
    }

    public static void main(String[] args) throws Exception {
        File root = new File(args.length > 0 ? args[0] : ".");
        File res = new File(root, "composeApp/src/androidMain/res");
        String[] dens = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        int[] px = {48, 72, 96, 144, 192};
        for (int i = 0; i < dens.length; i++) {
            save(icon(px[i], "round", false), new File(res, "mipmap-" + dens[i] + "/ic_launcher.png"));
            save(icon(px[i], "circle", false), new File(res, "mipmap-" + dens[i] + "/ic_launcher_round.png"));
        }
        File ios = new File(root, "iosApp/iosApp/Assets.xcassets");
        save(icon(1024, null, true), new File(ios, "AppIcon.appiconset/AppIcon.png"));
        save(glyph(240), new File(ios, "LaunchIcon.imageset/LaunchIcon.png"));
        save(glyph(480), new File(ios, "LaunchIcon.imageset/LaunchIcon@2x.png"));
        save(glyph(720), new File(ios, "LaunchIcon.imageset/LaunchIcon@3x.png"));
        File store = new File(root, "docs/branding/store");
        save(icon(512, null, true), new File(store, "icon-512.png"));
        save(featureGraphic(), new File(store, "feature-graphic-1024x500.png"));
        File logo = new File(root, "docs/branding/logo");
        save(icon(512, "round", false), new File(logo, "pocketsum-icon-rounded-512.png"));
        save(glyph(512), new File(logo, "pocketsum-glyph-512.png"));
    }
}
