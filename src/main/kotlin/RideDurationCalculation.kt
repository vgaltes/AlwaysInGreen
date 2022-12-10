package com.cooltra.zeus.pricing

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
        var firstEventTime = rideEvents.first().occurredOn
        var lastBillableTime = firstEventTime.plusSeconds(billableDurationInSeconds)
        var freeTime = if (subscriptionName == "tester") 30.minutes else 0.minutes
        val firstBillableTime =
            if (firstEventTime.plusSeconds(freeTime.inWholeSeconds) < lastBillableTime ) firstEventTime.plusSeconds(freeTime.inWholeSeconds)
            else lastBillableTime

        val (ridingDuration, pausingDuration) = getDurations(rideEvents, now)

        val rideEventsConsideringCustomEnding = rideEvents.map { if ( it.occurredOn > lastBillableTime ) it.copy(occurredOn = lastBillableTime) else it }
        val rideEventsConsideringFreeTime = rideEventsConsideringCustomEnding.map { if (it.occurredOn < firstBillableTime) it.copy(occurredOn = firstBillableTime) else it }

        val (billableRidingDuration, billablePausingDuration) = getDurations(rideEventsConsideringFreeTime, lastBillableTime)

        return RideDurations(ridingDuration, pausingDuration, billableRidingDuration, billablePausingDuration)
    }

    private fun getDurations(
        rideEvents: List<RideEvent>,
        lastTimeToConsider: Instant
    ): Pair<Duration, Duration> {
        var lastEventTime = rideEvents.first().occurredOn
        var ridingDuration = ZERO
        var pausingDuration = ZERO

        rideEvents.forEach { rideEvent ->
            val realDuration = durationBetween(lastEventTime, rideEvent.occurredOn)
            lastEventTime = rideEvent.occurredOn

            when (rideEvent.type) {
                RideEventType.PAUSED -> ridingDuration += realDuration
                RideEventType.RESUMED -> pausingDuration += realDuration
                RideEventType.STARTED -> {}
            }
        }

        val realDuration = durationBetween(lastEventTime, lastTimeToConsider)

        ridingDuration += realDuration
        return Pair(ridingDuration, pausingDuration)
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
    STARTED
}
