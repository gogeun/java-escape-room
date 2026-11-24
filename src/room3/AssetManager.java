package room3;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;

public final class AssetManager {
    private final Map<String,BufferedImage> cache=new HashMap<>();
    private final Set<String> missing=new LinkedHashSet<>();
    public BufferedImage image(String name) {
        return cache.computeIfAbsent(name, key -> {
            try(var stream=openImage(key)) {
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
    private InputStream openImage(String name) throws IOException {
        InputStream resource=AssetManager.class.getResourceAsStream("/room3/images/"+name);
        if(resource!=null) return resource;
        // IDE runs often compile classes without copying the project's resources.
        if(!Path.of(name).getFileName().toString().equals(name)) return null;
        Path[] starts={Path.of("").toAbsolutePath(), classLocation()};
        for(Path start:starts) {
            for(Path dir=start;dir!=null;dir=dir.getParent()) {
                Path image=dir.resolve("resources").resolve("room3").resolve("images").resolve(name);
                if(Files.isRegularFile(image)) return Files.newInputStream(image);
            }
        }
        java.io.File found=common.Resources.file("resources/room3/images/"+name);   // [통합] 상위 폴더에서 실행해도 찾기
        if(found.isFile()) return Files.newInputStream(found.toPath());
        return null;
    }
    private Path classLocation() {
        try {
            Path location=Path.of(AssetManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isDirectory(location)?location:location.getParent();
        } catch(Exception e) {
            return null;
        }
    }
    public Set<String> missing() { return Set.copyOf(missing); }
}

