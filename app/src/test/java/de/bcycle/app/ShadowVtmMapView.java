package de.bcycle.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/** Give each Robolectric sandbox its own identical JNI image. All actual VTM camera code still runs. */
@Implements(value = org.oscim.android.MapView.class, isInAndroidSdk = false)
public class ShadowVtmMapView extends org.robolectric.shadows.ShadowGLSurfaceView {
    private static boolean loaded;
    @org.robolectric.annotation.RealObject private org.oscim.android.MapView realMap;

    @Override @Implementation protected void onPause() {
        org.robolectric.shadow.api.Shadow.directlyOn(realMap, org.oscim.android.MapView.class, "onPause");
    }
    @Override @Implementation protected void onResume() {
        org.robolectric.shadow.api.Shadow.directlyOn(realMap, org.oscim.android.MapView.class, "onResume");
    }

    @Implementation
    protected static synchronized void init() {
        if (loaded) return;
        try {
            Path original = java.util.Arrays.stream(System.getProperty("java.library.path").split(java.io.File.pathSeparator))
                    .map(directory -> Path.of(directory, "libvtm-jni.so")).filter(Files::isRegularFile)
                    .findFirst().orElseThrow(() -> new IOException("VTM test JNI image missing"));
            Path image = Files.createTempFile("bicycle-vtm-sandbox-", ".so");
            Files.copy(original, image, StandardCopyOption.REPLACE_EXISTING);
            image.toFile().deleteOnExit();
            System.load(image.toAbsolutePath().toString());
            loaded = true;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot load the VTM test JNI library", error);
        }
    }
}
