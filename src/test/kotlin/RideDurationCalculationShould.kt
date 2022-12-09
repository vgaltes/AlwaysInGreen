package com.vgaltes

import com.vgaltes.RideEventType.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RideDurationCalculationShould : StringSpec({
    "calculate durations for a rental without pauses" {
        val now = Instant.now()
        val subscriptionName = "basic"
        val rentalEvents = listOf(
            RideEvent(STARTED, now.minusSeconds(120)),
        )

        val (ridingRentalDuration, pausedRentalDuration, billableRidingDuration, billablePausingDuration) =
            RideDurationCalculation.getRideBillableDurations(rentalEvents, subscriptionName, 120, now)

        ridingRentalDuration shouldBe 2.minutes
        pausedRentalDuration shouldBe ZERO
        billableRidingDuration shouldBe 2.minutes
        billablePausingDuration shouldBe ZERO
    }

    "return billable durations as zero if the rental is shorter than 30 minutes and the user is tester" {
        val now = Instant.now()
        val subscriptionName = "tester"
        val rentalEvents = listOf(
            RideEvent(STARTED, now.minusSeconds(1799)),
        )

        val (ridingRentalDuration, pausedRentalDuration, billableRidingDuration, billablePausingDuration) =
            RideDurationCalculation.getRideBillableDurations(rentalEvents, subscriptionName, 1799, now)

        ridingRentalDuration shouldBe 1799.seconds
        pausedRentalDuration shouldBe ZERO
        billableRidingDuration shouldBe ZERO
        billablePausingDuration shouldBe ZERO
    }

    "return billable durations if the rental is bigger than 30 minutes and the user is tester" {
        val now = Instant.now()
        val subscriptionName = "tester"
        val rentalEvents = listOf(
            RideEvent(STARTED, now.minusSeconds(1860)),
        )

        val (ridingRentalDuration, pausedRentalDuration, billableRidingDuration, billablePausingDuration) =
            RideDurationCalculation.getRideBillableDurations(rentalEvents, subscriptionName, 1860, now)

        ridingRentalDuration shouldBe 31.minutes
        pausedRentalDuration shouldBe ZERO
        billableRidingDuration shouldBe 1.minutes
        billablePausingDuration shouldBe ZERO
    }

    "return billable durations if the user is tester and the rental has pauses" {
        val now = Instant.now()
        val subscriptionName = "tester"
        val rentalEvents = listOf(
            RideEvent(STARTED, now.minusSeconds(1920)),
            RideEvent(PAUSED, now.minusSeconds(120)),
            RideEvent(RESUMED, now.minusSeconds(60)),
        )

        val (ridingRentalDuration, pausedRentalDuration, billableRidingDuration, billablePausingDuration) =
            RideDurationCalculation.getRideBillableDurations(rentalEvents, subscriptionName, 1920, now)

        ridingRentalDuration shouldBe 31.minutes
        pausedRentalDuration shouldBe 1.minutes
        billableRidingDuration shouldBe 1.minutes
        billablePausingDuration shouldBe 1.minutes
    }

    "return billable durations if the user is tester and the rental is in pause when the free time finishes" {
        val now = Instant.now()
        val subscriptionName = "tester"
        val rentalEvents = listOf(
            RideEvent(STARTED, now.minusSeconds(3600)),
            RideEvent(PAUSED, now.minusSeconds(2700)),
            RideEvent(RESUMED, now.minusSeconds(900)),
        )

        val (ridingRentalDuration, pausedRentalDuration, billableRidingDuration, billablePausingDuration) =
            RideDurationCalculation.getRideBillableDurations(rentalEvents, subscriptionName, 3600, now)

        ridingRentalDuration shouldBe 30.minutes
        pausedRentalDuration shouldBe 30.minutes
        billableRidingDuration shouldBe 15.minutes
        billablePausingDuration shouldBe 15.minutes
    }

    "calculate durations for a rental with a pause" {
        val now = Instant.now()
        val subscriptionName = "basic"
        val rentalEvents = listOf(
            RideEvent(STARTED, now.minusSeconds(300)),
            RideEvent(PAUSED, now.minusSeconds(120)),
            RideEvent(RESUMED, now.minusSeconds(60)),
        )

        val (ridingRentalDuration, pausedRentalDuration, billableRidingDuration, billablePausingDuration) =
            RideDurationCalculation.getRideBillableDurations(rentalEvents, subscriptionName, 300, now)

        ridingRentalDuration shouldBe 4.minutes
        pausedRentalDuration shouldBe 1.minutes
        billableRidingDuration shouldBe 4.minutes
        billablePausingDuration shouldBe 1.minutes
    }

    "calculate durations for a rental with two pauses" {
        val now = Instant.now()
        val subscriptionName = "basic"
        val rentalEvents = listOf(
            RideEvent(STARTED, now.minusSeconds(300)),
            RideEvent(PAUSED, now.minusSeconds(240)),
            RideEvent(RESUMED, now.minusSeconds(180)),
            RideEvent(PAUSED, now.minusSeconds(120)),
            RideEvent(RESUMED, now.minusSeconds(60)),
        )

        val (ridingRentalDuration, pausedRentalDuration, billableRidingDuration, billablePausingDuration) =
            RideDurationCalculation.getRideBillableDurations(rentalEvents, subscriptionName, 300, now)

        ridingRentalDuration shouldBe 3.minutes
        pausedRentalDuration shouldBe 2.minutes
        billableRidingDuration shouldBe 3.minutes
        billablePausingDuration shouldBe 2.minutes
    }

    "calculate durations for a rental without pauses but closed by CS" {
        val now = Instant.now()
        val subscriptionName = "basic"
        val rentalEvents = listOf(
            RideEvent(STARTED, now.minusSeconds(300)),
        )

        val (ridingRentalDuration, pausedRentalDuration, billableRidingDuration, billablePausingDuration) =
            RideDurationCalculation.getRideBillableDurations(rentalEvents, subscriptionName, 120, now)

        ridingRentalDuration shouldBe 5.minutes
        pausedRentalDuration shouldBe 0.minutes
        billableRidingDuration shouldBe 2.minutes
        billablePausingDuration shouldBe 0.minutes
    }

    "calculate durations for a rental with two pauses and closed by CS" {
        val now = Instant.now()
        val subscriptionName = "basic"
        val rentalEvents = listOf(
            RideEvent(STARTED, now.minusSeconds(300)),
            RideEvent(PAUSED, now.minusSeconds(240)),
            RideEvent(RESUMED, now.minusSeconds(180)),
            RideEvent(PAUSED, now.minusSeconds(120)),
            RideEvent(RESUMED, now.minusSeconds(60)),
        )

        val (ridingRentalDuration, pausedRentalDuration, billableRidingDuration, billablePausingDuration) =
            RideDurationCalculation.getRideBillableDurations(rentalEvents, subscriptionName, 150, now)

        ridingRentalDuration shouldBe 3.minutes
        pausedRentalDuration shouldBe 2.minutes
        billableRidingDuration shouldBe 1.5.minutes
        billablePausingDuration shouldBe 1.minutes
    }
})
