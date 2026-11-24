package common;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.net.URI;

/**
 * 프로젝트 폴더(resources 폴더가 들어 있는 곳)를 자동으로 찾아주는 도우미.
 *
 * VS Code / IntelliJ 등에서 어느 폴더를 기준으로 실행하더라도
 * "resources/room2/..." 같은 경로가 올바른 위치를 가리키도록 한다.
 *  1) 현재 실행 폴더와 그 상위 폴더들
 *  2) 컴파일된 .class 파일 위치와 그 상위 폴더들
 *  3) 현재 실행 폴더의 하위 폴더 (2단계까지)
 * 순서로 "resources" 폴더를 찾는다.
 */
public final class Resources {
    private Resources() {}

    private static File baseDir;

    public static File baseDir() {
        if (baseDir == null) {
            baseDir = locate();
            System.out.println("[Resources] 프로젝트 폴더: " + baseDir.getAbsolutePath());
        }
        return baseDir;
    }

    /** 프로젝트 폴더 기준 상대경로 → 실제 파일 */
    public static File file(String relativePath) {
        return new File(baseDir(), relativePath);
    }

    public static String path(String relativePath) {
        return file(relativePath).getPath();
    }

    /** 이미지 읽기 (없으면 콘솔에 경로를 출력하고 null 반환) */
    public static Image image(String relativePath) {
        File f = file(relativePath);
        if (f.exists()) {
            try {
                return ImageIO.read(f);
            } catch (Exception e) {
                System.err.println("[Resources] 이미지 읽기 실패: " + f.getAbsolutePath());
                return null;
            }
        }
        System.err.println("[Resources] 이미지 없음: " + f.getAbsolutePath());
        return null;
    }

    private static boolean isProjectDir(File dir) {
        return dir != null && new File(dir, "resources").isDirectory() && new File(dir, "src").isDirectory()
            || dir != null && new File(dir, "resources/room2").isDirectory();
    }

    private static File locate() {
        File cwd = new File(System.getProperty("user.dir")).getAbsoluteFile();

        // 1) 실행 폴더와 상위 폴더
        for (File d = cwd; d != null; d = d.getParentFile()) {
            if (isProjectDir(d)) return d;
        }

        // 2) .class 파일 위치와 상위 폴더
        try {
            URI uri = Resources.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            for (File d = new File(uri).getAbsoluteFile(); d != null; d = d.getParentFile()) {
                if (isProjectDir(d)) return d;
            }
        } catch (Exception ignored) {
        }

        // 3) 실행 폴더의 하위 폴더 (2단계)
        File found = searchChildren(cwd, 2);
        if (found != null) return found;

        return cwd;
    }

    private static File searchChildren(File dir, int depth) {
        if (depth == 0) return null;
        File[] children = dir.listFiles(File::isDirectory);
        if (children == null) return null;
        for (File c : children) {
            if (isProjectDir(c)) return c;
        }
        for (File c : children) {
            File f = searchChildren(c, depth - 1);
            if (f != null) return f;
        }
        return null;
    }
}
