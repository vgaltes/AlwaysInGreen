package com.vgaltes

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

object RideDurationCalculation {
    fun getRideBillableDurations(
        rideEvents: List<RideEvent>,
        subscriptionName: String,
        billableDurationInSeconds: Long,
        now: Instant = Instant.now(),
    ): RideDurations {
        var freeTime = if (subscriptionName == "tester") 30.minutes else 0.minutes

        val (ridingDuration, pausingDuration) =
            (rideEvents + RideEvent(RideEventType.FINISHED, now)).getDurations()

        val (billableRidingDuration, billablePausingDuration) =
            adjustEventsToStartAndEndBillableTimes(rideEvents, billableDurationInSeconds, freeTime).getDurations()

        return RideDurations(ridingDuration, pausingDuration, billableRidingDuration, billablePausingDuration)
    }

    private fun adjustEventsToStartAndEndBillableTimes(
        rideEvents: List<RideEvent>,
        billableDurationInSeconds: Long,
        freeTime: Duration
    ): List<RideEvent> {
        var firstEventTime = rideEvents.first().occurredOn
        var lastBillableTime = firstEventTime.plusSeconds(billableDurationInSeconds)

        val rideEventsConsideringCustomEnding =
            rideEvents.filter { it.occurredOn < lastBillableTime } + RideEvent(RideEventType.FINISHED, lastBillableTime)

        return if (firstEventTime.plusSeconds(freeTime.inWholeSeconds) < lastBillableTime) {
            val firstBillableTime = firstEventTime.plusSeconds(freeTime.inWholeSeconds)
            buildList {
                add(RideEvent(RideEventType.STARTED, firstBillableTime))
                addAll(rideEventsConsideringCustomEnding.filter { it.occurredOn > firstBillableTime && it.type != RideEventType.STARTED })
            }
        } else {
            emptyList()
        }
    }

    private fun List<RideEvent>.getDurations(): Pair<Duration, Duration> {
        return this.windowed(2, 1).fold(ZERO to ZERO) { accum, (first, second) ->
            val realDuration = durationBetween(first.occurredOn, second.occurredOn)
            when (second.type){
                RideEventType.PAUSED -> accum.first + realDuration to accum.second
                RideEventType.RESUMED -> accum.first to accum.second + realDuration
                RideEventType.FINISHED -> accum.first + realDuration to accum.second
                RideEventType.STARTED -> accum
            }
        }


        /*var lastEventTime = rideEvents.first().occurredOn
        var ridingDuration = ZERO
        var pausingDuration = ZERO
        rideEvents.forEach { rideEvent ->
            val realDuration = durationBetween(lastEventTime, rideEvent.occurredOn)
            lastEventTime = rideEvent.occurredOn

            when (rideEvent.type) {
                RideEventType.PAUSED -> ridingDuration += realDuration
                RideEventType.RESUMED -> pausingDuration += realDuration
                RideEventType.STARTED -> {}
                RideEventType.FINISHED -> ridingDuration += realDuration
            }
        }

        val realDuration = durationBetween(lastEventTime, lastTimeToConsider)

        ridingDuration += realDuration
        return Pair(ridingDuration, pausingDuration)*/
    }

    private fun durationBetween(
        initial: Instant,
        final: Instant,
    ) = java.time.Duration.between(initial, final).toKotlinDuration()
}

data class RideDurations(
    val ridingDuration: Duration,
    val pausedDuration: Duration,
    val billableRidingDuration: Duration,
    val billablePausedDuration: Duration
) {
}

data class RideEvent(val type: RideEventType, val occurredOn: Instant)

enum class RideEventType {
    PAUSED,
    RESUMED,
    STARTED,
    FINISHED
}
