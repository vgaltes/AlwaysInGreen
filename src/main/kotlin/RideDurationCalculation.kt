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

        val (ridingDuration, pausingDuration) = getRideDurations(rideEvents, now)
        val (billableRidingDuration, billablePausingDuration) = getBillableDurations(
            rideEvents,
            billableDurationInSeconds,
            subscriptionName
        )

        if (ridingDuration + pausingDuration < 2.minutes) {
            return RideDurations(ridingDuration, pausingDuration, ZERO, ZERO)
        }

        return RideDurations(ridingDuration, pausingDuration, billableRidingDuration, billablePausingDuration)
    }


    private fun getRideDurations(rideEvents: List<RideEvent>,
                                 lastTimeToConsider: Instant) : Pair<Duration, Duration> {
        return getDurations(rideEvents, lastTimeToConsider, ZERO)
    }

    private fun getBillableDurations(
        rideEvents: List<RideEvent>,
        billableDurationInSeconds: Long,
        subscriptionName: String
    ): Pair<Duration, Duration> {
        var lastEventTime = rideEvents.first().occurredOn
        var lastBillableTime = lastEventTime.plusSeconds(billableDurationInSeconds)
        var freeTime = if (subscriptionName == "tester") 30.minutes else 0.minutes

        val adjustedRideEvents = adjustToLastBillableTime(rideEvents, lastBillableTime)
        return getDurations(adjustedRideEvents, lastBillableTime, freeTime)
    }

    private fun adjustToLastBillableTime(rideEvents: List<RideEvent>, lastBillableTime: Instant): List<RideEvent> {
        return rideEvents.map {
            if (it.occurredOn > lastBillableTime) it.copy(occurredOn = lastBillableTime)
            else it
        }
    }

    private fun getDurations(
        rideEvents: List<RideEvent>,
        lastBillableTime: Instant,
        initialFreeTime: Duration
    ): Pair<Duration, Duration> {
        var lastEventTime = rideEvents.first().occurredOn
        var ridingDuration: Duration = ZERO
        var pausingDuration: Duration = ZERO
        var freeTime =  initialFreeTime
        rideEvents.forEach { rideEvent ->
            val realDuration = durationBetween(lastEventTime, rideEvent.occurredOn)
            lastEventTime = rideEvent.occurredOn

            val (spanBillableDuration, newFreeTime) = getBillableDuration(realDuration, freeTime)
            freeTime = newFreeTime

            when (rideEvent.type) {
                RideEventType.PAUSED -> {
                    ridingDuration += spanBillableDuration
                }
                RideEventType.RESUMED -> {
                    pausingDuration += spanBillableDuration
                }
                RideEventType.STARTED -> {}
            }
        }

        val realDuration = durationBetween(lastEventTime, lastBillableTime)
        val (spanBillableDuration, _) = getBillableDuration(realDuration, freeTime)

        ridingDuration += spanBillableDuration

        return Pair(ridingDuration, pausingDuration)
    }

    private fun getBillableDuration(duration: Duration, freeTime: Duration): Pair<Duration, Duration> =
        when {
            freeTime == ZERO -> duration to ZERO
            freeTime != ZERO && duration < freeTime -> ZERO to (freeTime - duration)
            else -> (duration - freeTime) to ZERO
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
