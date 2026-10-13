package de.bcycle.app

import org.robolectric.nativeruntime.DefaultNativeRuntimeLoader

/** SDK sandboxes share the JVM ZIP provider. Serialize font extraction across their classloaders. */
class SerialNativeRuntimeLoader : DefaultNativeRuntimeLoader() {
    override fun ensureLoaded() {
        synchronized(System.getProperties()) { super.ensureLoaded() }
    }
}
