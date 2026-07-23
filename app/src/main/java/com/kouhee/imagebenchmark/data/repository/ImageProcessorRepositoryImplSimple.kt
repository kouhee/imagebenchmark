package com.kouhee.imagebenchmark.data.repository

import android.os.Trace
import android.util.Log
import com.kouhee.imagebenchmark.common.timing.ProcessingTimer
import com.kouhee.imagebenchmark.common.timing.TimingSource
import com.kouhee.imagebenchmark.data.processor.ImageProcessorFactory
import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ImageData
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine
import com.kouhee.imagebenchmark.domain.repository.ImageProcessorRepository
import java.util.concurrent.atomic.AtomicInteger

class ImageProcessorRepositoryImplSimple(
    private val factory: ImageProcessorFactory
) : ImageProcessorRepository {

    companion object {
        private const val TRACE_NAME = "ProcessRepository"
        private const val TAG = "Repository"
        private val traceCounter = AtomicInteger(0)
    }

    override suspend fun process(
        image: ImageData,
        filter: FilterType,
        engine: ProcessingEngine
    ): ImageData {
        val eventId = traceCounter.getAndIncrement()
        Trace.beginAsyncSection(TRACE_NAME, eventId)
        val processor = factory.create(filter, engine)

        val start = ProcessingTimer.mark()
        val result = processor.process(image)
        val timing = ProcessingTimer.durationUs(start, ProcessingTimer.mark())
        val processingTimeUs = timing.timeUs
        val logic = "filter=${filter.name}, engine=${engine.name}, processor=${processor::class.simpleName}"
        if (timing.source != TimingSource.ELAPSED_REALTIME) {
            Log.w(TAG, "Timing fallback used (#$eventId, $logic): ${timing.source}")
        }

        result.processingTimeUs = processingTimeUs
        result.processingTimeMs = processingTimeUs / 1000.0

        Trace.endAsyncSection(TRACE_NAME, eventId)
        return result
    }
}
