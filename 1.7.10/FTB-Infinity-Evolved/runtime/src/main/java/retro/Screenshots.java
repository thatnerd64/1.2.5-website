package retro;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.teavm.jso.typedarrays.Int8Array;

/** Screenshots (F2) are saved in the virtual .minecraft and also offered as a browser download. */
public final class Screenshots {
    private Screenshots() {
    }

    public static void offerDownload(File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] data = in.readAllBytes();
            JS.download(file.getName(), Int8Array.fromJavaArray(data), "image/png");
        } catch (IOException e) {
            JS.error("Could not export screenshot: " + e);
        }
    }
}
