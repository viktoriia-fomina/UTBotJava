package org.utbot.examples.mixed

import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

internal class SimpleNoConditionTest : AbstractTestCaseGeneratorTest(testClass = SimpleNoCondition::class) {

    @Test
    fun testNoConditionAdd() {
        check(
            SimpleNoCondition::basicAdd,
            eq(1)
        )
    }

    @Test
    fun testNoConditionPow() {
        check(
            SimpleNoCondition::basicXorInt,
            eq(1)
        )
    }

    @Test
    fun testNoConditionPowBoolean() {
        check(
            SimpleNoCondition::basicXorBoolean,
            eq(1)
        )
    }
}