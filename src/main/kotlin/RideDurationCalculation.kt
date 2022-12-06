package com.cooltra.zeus.pricing

import java.time.Instant
import kotlin.time.Duration
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

        val (ridingDurations, pausingDurations) = getDurations(rideEvents, lastEventTime, lastBillableTime, freeTime, now)
        val (billableRidingDurations, billablePausingDurations) = getBillableDurations(rideEvents, lastEventTime, lastBillableTime, freeTime, now)

        if (ridingDurations.duration + pausingDurations.duration < 2.minutes) {
            return RideDurations(ridingDurations.duration, pausingDurations.duration, Duration.ZERO, Duration.ZERO)
        }

        return RideDurations(ridingDurations.duration, pausingDurations.duration, ridingDurations.billableDuration, pausingDurations.billableDuration)
    }

    private fun getDurations(
        rideEvents: List<RideEvent>,
        lastEventTime: Instant,
        lastBillableTime: Instant,
        freeTime: Duration,
        now: Instant
    ): Pair<StateDuration, StateDuration> {
        var lastEventTime1 = lastEventTime
        var freeTime1 = freeTime
        val ridingDurations = StateDuration()
        val pausingDurations = StateDuration()
        rideEvents.forEach { rideEvent ->
            val realDuration = durationBetween(lastEventTime1, rideEvent.occurredOn)
            val potentialBillableDuration =
                if (lastBillableTime < lastEventTime1) Duration.ZERO
                else if (lastBillableTime < rideEvent.occurredOn) durationBetween(lastEventTime1, lastBillableTime)
                else durationBetween(lastEventTime1, minOf(rideEvent.occurredOn, lastBillableTime))

            lastEventTime1 = rideEvent.occurredOn

            val (spanBillableDuration, newFreeTime) = getBillableDuration(potentialBillableDuration, freeTime1)
            freeTime1 = newFreeTime

            when (rideEvent.type) {
                RideEventType.PAUSED -> ridingDurations.increment(realDuration, spanBillableDuration)
                RideEventType.RESUMED -> pausingDurations.increment(realDuration, spanBillableDuration)
                RideEventType.STARTED -> {}
            }
        }

        val realDuration = durationBetween(lastEventTime1, now)
        val potentialBillableDuration =
            if (lastBillableTime < lastEventTime1) Duration.ZERO
            else durationBetween(lastEventTime1, lastBillableTime)
        val (spanBillableDuration, _) = getBillableDuration(potentialBillableDuration, freeTime1)

        ridingDurations.increment(realDuration, spanBillableDuration)
        return Pair(ridingDurations, pausingDurations)
    }
    private fun getBillableDurations(
        rideEvents: List<RideEvent>,
        lastEventTime: Instant,
        lastBillableTime: Instant,
        freeTime: Duration,
        now: Instant
    ): Pair<StateDuration, StateDuration> {
        var lastEventTime1 = lastEventTime
        var freeTime1 = freeTime
        val ridingDurations = StateDuration()
        val pausingDurations = StateDuration()
        rideEvents.forEach { rideEvent ->
            val realDuration = durationBetween(lastEventTime1, rideEvent.occurredOn)
            val potentialBillableDuration =
                if (lastBillableTime < lastEventTime1) Duration.ZERO
                else if (lastBillableTime < rideEvent.occurredOn) durationBetween(lastEventTime1, lastBillableTime)
                else durationBetween(lastEventTime1, minOf(rideEvent.occurredOn, lastBillableTime))

            lastEventTime1 = rideEvent.occurredOn

            val (spanBillableDuration, newFreeTime) = getBillableDuration(potentialBillableDuration, freeTime1)
            freeTime1 = newFreeTime

            when (rideEvent.type) {
                RideEventType.PAUSED -> ridingDurations.increment(realDuration, spanBillableDuration)
                RideEventType.RESUMED -> pausingDurations.increment(realDuration, spanBillableDuration)
                RideEventType.STARTED -> {}
            }
        }

        val realDuration = durationBetween(lastEventTime1, now)
        val potentialBillableDuration =
            if (lastBillableTime < lastEventTime1) Duration.ZERO
            else durationBetween(lastEventTime1, lastBillableTime)
        val (spanBillableDuration, _) = getBillableDuration(potentialBillableDuration, freeTime1)

        ridingDurations.increment(realDuration, spanBillableDuration)
        return Pair(ridingDurations, pausingDurations)
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