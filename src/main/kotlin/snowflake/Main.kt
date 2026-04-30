package org.example.snowflake

import processing.core.PApplet
import kotlin.math.*

data class Vector(val x: Float, val y: Float) {
    operator fun plus(other: Vector) = Vector(x + other.x, y + other.y)
    operator fun plus(other: Polar) = this + other.toVector()
    operator fun minus(other: Vector) = Vector(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Vector(x / scalar, y / scalar)
    operator fun unaryMinus() = Vector(-x, -y)
    fun toPolar(): Polar {
        return Polar(sqrt(x.pow(2) + y.pow(2)).toFloat(), atan2(y, x))
    }
}

data class Polar(val radius: Float, val angle: Float) {
    fun toVector(): Vector {
        return Vector(cos(angle), sin(angle)) * radius
    }
}

class SnowFlake : PApplet() {

    class Engine(depth: Int = 1, splitN: Int = 3, baseN: Int = 3) {
        var sides = arrayListOf<Vector>()

        private var _radius = 500f
        var radius: Float
            get() = _radius
            set(value) {
                _radius = value
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
                _splitN = if (value < 2) 2 else value
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
            sides.clear()
            val baseAngle = PI * (2 - 1f / baseN)
            for (i in 0 until baseN) {
                sides.add(Polar(radius, baseAngle + PI - i * 2 * PI / baseN).toVector())
            }
        }

        fun getComputedSides(): List<Vector> {
            return computeSnowflakeSides(sides, depth, splitN)
        }

        private fun computeSnowflakeSides(sides: List<Vector>, depth: Int, n: Int = 3): List<Vector> {
            if (depth > 0 && n >= 3) {
                val result = arrayListOf<Vector>()
                for (side in sides) {
                    val baseSidePolar = side.toPolar()
                    val processedRadius = baseSidePolar.radius / n
                    for (i in 0..n) {
                        when (i) {
                            0, n -> result.add(Polar(processedRadius, baseSidePolar.angle).toVector())
                            else -> result.add(
                                Polar(
                                    processedRadius,
                                    baseSidePolar.angle + PI - i * 2 * PI / n
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

    lateinit var engine: Engine
    lateinit var sides: List<Vector>
    lateinit var root: Vector
    override fun setup() {
        noLoop()
        background(255)
        engine = Engine()
        sides = engine.getComputedSides()
        root = Vector(width.toFloat() / 2f, 100f)
    }

    fun drawLine(from: Vector, to: Vector) {
        line(from.x, from.y, to.x, to.y)
    }

    override fun draw() {
        background(255)
        var before = root
        for (side in sides) {
            val current = before + side
            drawLine(before, current)
            before = current
        }
        val font = createFont("Noto Sans", 15f)
        textFont(font)
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

    override fun keyPressed() {
        if (key.code == CODED) {
            when (keyCode) {
                RIGHT -> engine.depth += 1
                LEFT -> engine.depth -= 1
                UP -> engine.splitN += 1
                DOWN -> engine.splitN -= 1
                else -> return
            }
        } else {
            when (key) {
                '+', '=' -> engine.radius += 100f
                '-' -> engine.radius -= 100f
                'l' -> engine.baseN += 1
                'j' -> engine.baseN -= 1
                'w' -> root += Vector(0f, 10f)
                's' -> root += Vector(0f, -10f)
                'a' -> root += Vector(10f, 0f)
                'd' -> root += Vector(-10f, 0f)
                else -> return
            }
        }
        sides = engine.getComputedSides()
        redraw()
    }
}

fun main() {
    PApplet.main(SnowFlake::class.java.name)
}
