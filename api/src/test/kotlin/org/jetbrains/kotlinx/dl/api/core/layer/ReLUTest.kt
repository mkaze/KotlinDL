/*
 * Copyright 2020 JetBrains s.r.o. and Kotlin Deep Learning project contributors. All Rights Reserved.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dl.api.core.layer

import org.jetbrains.kotlinx.dl.api.core.layer.activation.ReLU
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ReLUTest : ActivationLayerTest() {
    @Test
    fun defaultRelu() {
        val input = floatArrayOf(-3.0f, -1.0f, 0.0f, 2.0f)
        val expected = floatArrayOf(0.0f, 0.0f, 0.0f, 2.0f)

        assertActivationFunctionSameOutputShape(ReLU(), input, expected)
    }

    @Test
    fun reluWithMaxValue() {
        val input = floatArrayOf(-3.0f, -1.0f, 0.0f, 2.0f)
        val expected = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

        assertActivationFunctionSameOutputShape(ReLU(maxValue = 1.0f), input, expected)
    }

    @Test
    fun reluWithNegativeSlope() {
        val exception = Assertions.assertThrows(UnsupportedOperationException::class.java) {
            assertActivationFunctionIrrelevantInputOutput(ReLU(negativeSlope = 1.0f))
        }
        assertEquals(
            "Should be returned the LeakyRelu, but it's not supported in KotlinDL yet!",
            exception.message
        )
    }

    @Test
    fun reluWithThreshold() {
        val input = floatArrayOf(-3.0f, -1.0f, 0.0f, 2.0f)
        val expected = floatArrayOf(0.0f, 0.0f, 0.0f, 2.0f)

        assertActivationFunctionSameOutputShape(ReLU(threshold = 1.5f), input, expected)
    }
}
