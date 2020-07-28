package api.keras.layers.twodim

import api.KGraph
import api.depthwiseConv2dBiasVarName
import api.depthwiseConv2dKernelVarName
import api.keras.activations.Activations
import api.keras.initializers.Initializer
import api.keras.layers.LayerWithoutGradients
import api.keras.shape.numElementsInShape
import api.keras.shape.shapeFromDims
import api.keras.shape.shapeToLongArray
import api.tensor.convertTensorToMultiDimArray
import org.tensorflow.Operand
import org.tensorflow.Shape
import org.tensorflow.op.Ops
import org.tensorflow.op.core.Variable
import kotlin.math.roundToInt

class DepthwiseConv2D<T : Number>(
    val filters: Long,
    val kernelSize: LongArray,
    val strides: LongArray,
    val activation: Activations = Activations.Relu,
    val kernelInitializer: Initializer<T>,
    val biasInitializer: Initializer<T>,
    val padding: ConvPadding,
    name: String = ""
) : LayerWithoutGradients<T>(name) {
    // weight tensors
    private lateinit var kernel: Variable<T>
    private lateinit var bias: Variable<T>

    // weight tensor shapes
    private lateinit var biasShape: Shape
    private lateinit var kernelShape: Shape

    private val KERNEL = "depthwise_conv2d_kernel"
    private val BIAS = "depthwise_conv2d_bias"

    override fun defineVariables(tf: Ops, kGraph: KGraph<T>, inputShape: Shape) {
        // Amount of channels should be the last value in the inputShape (make warning here)
        val lastElement = inputShape.size(inputShape.numDimensions() - 1)

        // Compute shapes of kernel and bias matrices
        kernelShape = shapeFromDims(*kernelSize, lastElement, filters)
        biasShape = Shape.make(filters)

        // should be calculated before addWeight because it's used in calculation, need to rewrite addWEight to avoid strange behaviour
        // calculate fanIn, fanOut
        val inputDepth = lastElement // amount of channels
        val outputDepth = filters // amount of channels for the next layer

        fanIn = (inputDepth * kernelSize[0] * kernelSize[1]).toInt()
        fanOut = ((outputDepth * kernelSize[0] * kernelSize[1] / (strides[0].toDouble() * strides[1])).roundToInt())

        if (name.isNotEmpty()) {
            val kernelVariableName = depthwiseConv2dKernelVarName(name)
            val biasVariableName = depthwiseConv2dBiasVarName(name)

            kernel = tf.withName(kernelVariableName).variable(kernelShape, getDType())
            bias = tf.withName(biasVariableName).variable(biasShape, getDType())

            kernel = addWeight(tf, kGraph, kernelVariableName, kernel, kernelInitializer)
            bias = addWeight(tf, kGraph, biasVariableName, bias, biasInitializer)
        } else {
            kernel = tf.variable(kernelShape, getDType())
            bias = tf.variable(biasShape, getDType())
            kernel = addWeight(tf, kGraph, KERNEL, kernel, kernelInitializer)
            bias = addWeight(tf, kGraph, BIAS, bias, biasInitializer)
        }
    }

    override fun computeOutputShape(inputShape: Shape): Shape {
        //TODO: outputShape calculation depending on padding type https://github.com/keras-team/keras/blob/master/keras/utils/conv_utils.py

        return Shape.make(inputShape.size(0), inputShape.size(1), inputShape.size(2), filters)
    }

    override fun transformInput(tf: Ops, input: Operand<T>): Operand<T> {
        val tfPadding = when (padding) {
            ConvPadding.SAME -> "SAME"
            ConvPadding.VALID -> "VALID"
            ConvPadding.FULL -> TODO()
        }

        val signal = tf.nn.biasAdd(tf.nn.depthwiseConv2dNative(input, kernel, strides.toMutableList(), tfPadding), bias)
        return Activations.convert<T>(activation).apply(tf, signal, name)
    }

    override fun getWeights(): List<Array<*>> {
        val result = mutableListOf<Array<*>>()

        val session = parentModel.session

        val runner = session.runner()
            .fetch(depthwiseConv2dKernelVarName(name))
            .fetch(depthwiseConv2dBiasVarName(name))

        val tensorList = runner.run()
        val filtersTensor = tensorList[0]
        val biasTensor = tensorList[1]

        val dstData = filtersTensor.convertTensorToMultiDimArray()
        result.add(dstData)

        val dstData2 = biasTensor.convertTensorToMultiDimArray()
        result.add(dstData2)

        return result.toList()
    }

    override fun hasActivation(): Boolean {
        return true
    }

    override fun getParams(): Int {
        return (numElementsInShape(shapeToLongArray(kernelShape)) + numElementsInShape(shapeToLongArray(biasShape))).toInt()
    }
}