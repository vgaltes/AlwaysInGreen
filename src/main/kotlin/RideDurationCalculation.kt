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
        var lastEventTime = rideEvents.first().occurredOn
        var lastBillableTime = lastEventTime.plusSeconds(billableDurationInSeconds)

        var freeTime = if (subscriptionName == "tester") 30.minutes else 0.minutes

        val (ridingDurations, pausingDurations) = getRideDurations(rideEvents, now)
        val (billableRidingDuration, billablePausingDuration) = getBillableDurations(
            rideEvents,
            lastBillableTime,
            freeTime
        )

        if (ridingDurations.duration + pausingDurations.duration < 2.minutes) {
            return RideDurations(ridingDurations.duration, pausingDurations.duration, Duration.ZERO, Duration.ZERO)
        }

        return RideDurations(ridingDurations.duration, pausingDurations.duration, billableRidingDuration, billablePausingDuration)
    }

    private fun getRideDurations(
        rideEvents: List<RideEvent>,
        now: Instant
    ): Pair<StateDuration, StateDuration> {
        var lastEventTime = rideEvents.first().occurredOn
        val ridingDurations = StateDuration()
        val pausingDurations = StateDuration()
        rideEvents.forEach { rideEvent ->
            val realDuration = durationBetween(lastEventTime, rideEvent.occurredOn)

            lastEventTime = rideEvent.occurredOn

            when (rideEvent.type) {
                RideEventType.PAUSED -> ridingDurations.increment(realDuration, Duration.ZERO)
                RideEventType.RESUMED -> pausingDurations.increment(realDuration, Duration.ZERO)
                RideEventType.STARTED -> {}
            }
        }

        val realDuration = durationBetween(lastEventTime, now)

        ridingDurations.increment(realDuration, Duration.ZERO)
        return Pair(ridingDurations, pausingDurations)
    }
    private fun getBillableDurations(
        rideEvents: List<RideEvent>,
        lastBillableTime: Instant,
        freeTime: Duration
    ): Pair<Duration, Duration> {
        var lastEventTime = rideEvents.first().occurredOn
        var freeTime1 = freeTime
        var ridingDuration: Duration = ZERO
        var pausingDuration: Duration = ZERO
        rideEvents.forEach { rideEvent ->
            val potentialBillableDuration =
                if (lastBillableTime < lastEventTime) Duration.ZERO
                else if (lastBillableTime < rideEvent.occurredOn) durationBetween(lastEventTime, lastBillableTime)
                else durationBetween(lastEventTime, minOf(rideEvent.occurredOn, lastBillableTime))

            lastEventTime = rideEvent.occurredOn

            val (spanBillableDuration, newFreeTime) = getBillableDuration(potentialBillableDuration, freeTime1)
            freeTime1 = newFreeTime

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

        val potentialBillableDuration =
            if (lastBillableTime < lastEventTime) Duration.ZERO
            else durationBetween(lastEventTime, lastBillableTime)
        val (spanBillableDuration, _) = getBillableDuration(potentialBillableDuration, freeTime1)

        ridingDuration += spanBillableDuration
        return Pair(ridingDuration, pausingDuration)
    }

    private fun getBillableDuration(duration: Duration, freeTime: Duration): Pair<Duration, Duration> =
        when {
            freeTime == Duration.ZERO -> duration to Duration.ZERO
            freeTime != Duration.ZERO && duration < freeTime -> Duration.ZERO to (freeTime - duration)
            else -> (duration - freeTime) to Duration.ZERO
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

data class StateDuration(var duration: Duration = Duration.ZERO, var billableDuration: Duration = Duration.ZERO) {
    fun increment(durationToIncrement: Duration, billableDurationToImcrement: Duration) {
        this.duration = this.duration + durationToIncrement
        this.billableDuration = this.billableDuration + billableDurationToImcrement
    }
}