package de.bcycle.app

import org.mapsforge.map.layer.cache.InMemoryTileCache
import org.mapsforge.map.layer.queue.Job

/** Keep the whole render working set resident, including overdraw and rotation.
 * Mapsforge draws from RAM only; a smaller cache repeatedly evicts visible disk tiles.
 * Size follows the viewport, not ride duration, and shrinks after substantial reductions.
 */
internal class ViewportTileCache : InMemoryTileCache(16) {
    @Synchronized
    override fun setWorkingSet(jobs: Set<Job>) {
        val needed = maxOf(16, jobs.size + 4)
        if (needed > capacity || needed <= capacity / 2) setCapacity(needed)
        super.setWorkingSet(jobs)
    }
}
