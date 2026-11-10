package room3;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;

public final class AssetManager {
    private final Map<String,BufferedImage> cache=new HashMap<>();
    private final Set<String> missing=new LinkedHashSet<>();
    public BufferedImage image(String name) {
        return cache.computeIfAbsent(name, key -> {
            try(var stream=AssetManager.class.getResourceAsStream("/room3/images/"+key)) {
                if(stream==null) throw new IOException("Missing resource: "+key);
                BufferedImage result=ImageIO.read(stream);
                if(result==null) throw new IOException("Invalid image: "+key);
                return result;
            } catch(IOException e) {
                missing.add(key); System.err.println(e.getMessage());
                BufferedImage fallback=new BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=fallback.createGraphics(); g.setColor(new Color(40,20,25));g.fillRect(0,0,640,360);
                g.setColor(Color.WHITE);g.drawString(TextDisplay.text("Missing image: "+key),30,180);g.dispose();return fallback;
            }
        });
    }
    public Set<String> missing() { return Set.copyOf(missing); }
}
