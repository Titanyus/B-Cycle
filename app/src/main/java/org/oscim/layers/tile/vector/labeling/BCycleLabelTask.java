package org.oscim.layers.tile.vector.labeling;

import org.oscim.core.MapPosition;
import org.oscim.renderer.bucket.BCycleTextBucket;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.TextureBucket;

/** Same two-slot worker handoff as VTM LabelTask, with bounded per-vertex fade data. */
final class BCycleLabelTask {
    final MapPosition pos=new MapPosition();
    final SymbolBucket symbolLayer=new SymbolBucket();
    final BCycleTextBucket textLayer=new BCycleTextBucket();
    final TextureBucket layers=symbolLayer;
    long epoch,fadeUntil;
    BCycleLabelTask() { symbolLayer.next=textLayer; }
}
