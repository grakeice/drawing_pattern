package org.example.snowflake

import kotlinx.coroutines.*
import processing.core.PApplet
import processing.core.PFont
import kotlin.math.*

data class Vector(val x: Float, val y: Float) {
    operator fun plus(other: Vector) = Vector(x + other.x, y + other.y)
    operator fun plus(other: Polar) = this + other.toVector()
    operator fun minus(other: Vector) = Vector(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Vector(x / scalar, y / scalar)
    operator fun unaryMinus() = Vector(-x, -y)
    fun toPolar(): Polar {
        return Polar(sqrt(x.pow(2) + y.pow(2)), atan2(y, x))
    }
}

data class Polar(val radius: Float, val angle: Float) {
    fun toVector(): Vector {
        return Vector(cos(angle), sin(angle)) * radius
    }
}

class SnowFlake : PApplet() {

    class Engine(depth: Int = 1, splitN: Int = 3, baseN: Int = 3) {
        @Volatile
        private var sides: List<Vector> = emptyList()

        private var _radius = 500f
        var radius: Float
            get() = _radius
            set(value) {
                _radius = if (value >= 0f) value else 0f
                initializeSides()
            }

        private var _depth = depth
        var depth: Int
            get() = _depth
            set(value) {
                _depth = if (value < 0) 0 else value
            }
        private var _splitN = splitN
        var splitN: Int
            get() = _splitN
            set(value) {
                _splitN = if (value < 3) 3 else value
            }

        private var _baseN = baseN
        var baseN: Int
            get() = _baseN
            set(value) {
                _baseN = if (value < 1) 1 else value
                initializeSides()
            }

        init {
            initializeSides()
        }

        fun initializeSides() {
            val newSides = arrayListOf<Vector>()
            val baseAngle = PI * (2 - 1f / baseN)
            for (i in 0 until baseN) {
                newSides.add(Polar(radius, baseAngle + PI - i * 2 * PI / baseN).toVector())
            }
            sides = newSides
        }

        suspend fun getComputedSides(): List<Vector> {
            return computeSnowflakeSides(sides, depth, splitN)
        }

        private suspend fun computeSnowflakeSides(sides: List<Vector>, depth: Int, n: Int): List<Vector> {
            if (depth > 0) {
                val result = arrayListOf<Vector>()
                for (side in sides) {
                    yield()
                    val baseSidePolar = side.toPolar()
                    val processedRadius = baseSidePolar.radius / n
                    for (i in 0..n) {
                        when (i) {
                            0, n -> result.add(Polar(processedRadius, baseSidePolar.angle).toVector())
                            else -> result.add(
                                Polar(
                                    processedRadius, baseSidePolar.angle + PI - i * 2 * PI / n
                                ).toVector()
                            )
                        }
                    }
                }
                return computeSnowflakeSides(result, depth - 1, n)
            } else {
                return sides
            }
        }
    }

    override fun settings() {
        size(1000, 1000)
    }

    var isComputing = false

    lateinit var engine: Engine
    private var sides: List<Vector> = listOf()
    lateinit var root: Vector
    lateinit var font: PFont
    override fun setup() {
        background(255)
        font = createFont("Noto Sans", 15f)
        textFont(font)

        engine = Engine()
        updateSidesAsync()
        root = Vector(0f, 0f)
    }

    fun drawLine(from: Vector, to: Vector) {
        line(from.x, from.y, to.x, to.y)
    }

    private val parentJob = Job()
    private val scope = CoroutineScope(Dispatchers.Default + parentJob)

    private var computeJob: Job? = null

    fun updateSidesAsync() {
        computeJob?.cancel()

        isComputing = true
        computeJob = scope.launch(Dispatchers.Default) {
            try {
                val result = engine.getComputedSides()
                if (isActive) {
                    sides = result
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                if (isActive) {
                    isComputing = false
                }
            }
        }
    }

    fun drawUI() {
        fill(0)
        text("元の図形　 : 正 ${engine.baseN} 角形", 10f, 30f)
        text("辺の分割数 : ${engine.splitN}", 10f, 50f)
        text("深さ　　　 : ${engine.depth}", 10f, 70f)

        val explainOffset = 60f
        text("WASD", 10f, height - 110f)
        text(": 移動", explainOffset, height - 110f)
        text("–/+", 10f, height - 90f)
        text(": 縮小/拡大", explainOffset, height - 90f)
        text("j/l", 10f, height - 70f)
        text(": 元の図形の頂点数", explainOffset, height - 70f)
        text("↓ ↑", 10f, height - 50f)
        text(": 辺の分割数", explainOffset, height - 50f)
        text("← →", 10f, height - 30f)
        text(": 深さ", explainOffset, height - 30f)
    }

    override fun draw() {
        background(255)
        if (isComputing) {
            fill(255f, 0f, 0f)
            text("計算中...", width - 150f, 30f)
        }
        val currentSides = sides
        if (currentSides.isEmpty()) {
            drawUI()
            return
        }
        val points = arrayListOf<Vector>()
        var currentPos = Vector(0f, 0f)
        points.add(currentPos)
        for (side in currentSides) {
            currentPos += side
            points.add(currentPos)
        }

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }

        val figureCenterX = (minX + maxX) / 2f
        val figureCenterY = (minY + maxY) / 2f

        val offsetX = width / 2f - figureCenterX + root.x
        val offsetY = height / 2f - figureCenterY + root.y

        pushMatrix()
        translate(offsetX, offsetY)
        var before = points[0]
        for (i in 1 until points.size) {
            val current = points[i]
            drawLine(before, current)
            before = current
        }
        popMatrix()

        drawUI()
    }

    override fun keyPressed() {
        var shouldUpdate = false

        if (key.code == CODED) {
            when (keyCode) {
                RIGHT -> {
                    engine.depth += 1
                    shouldUpdate = true
                }

                LEFT -> {
                    engine.depth -= 1
                    shouldUpdate = true
                }

                UP -> {
                    engine.splitN += 1
                    shouldUpdate = true
                }

                DOWN -> {
                    engine.splitN -= 1
                    shouldUpdate = true
                }

                else -> return
            }
        } else {
            when (key) {
                '+', '=' -> {
                    engine.radius += 100f
                    shouldUpdate = true
                }

                '-' -> {
                    engine.radius -= 100f
                    shouldUpdate = true
                }

                'l' -> {
                    engine.baseN += 1
                    shouldUpdate = true
                }

                'j' -> {
                    engine.baseN -= 1
                    shouldUpdate = true
                }

                'w' -> root += Vector(0f, 10f)
                's' -> root += Vector(0f, -10f)
                'a' -> root += Vector(10f, 0f)
                'd' -> root += Vector(-10f, 0f)
                else -> return
            }
        }

        if (shouldUpdate) {
            updateSidesAsync()
        }
    }

    override fun exit() {
        parentJob.cancel()
        super.exit()
    }
}

fun main() {
    PApplet.main(SnowFlake::class.java.name)
}
