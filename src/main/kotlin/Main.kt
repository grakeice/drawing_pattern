package org.example

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

data class Coordinate<T : Number>(val x: T, val y: T)

class LinePattern : PApplet() {
	override fun settings() {
		size(1000, 1000)
	}

	override fun setup() {
		background(255)
		noLoop()
	}

	override fun draw() {
		val offset = 4
		if (!(width % offset == 0 && height % offset == 0)) throw Error(
			"間隔が割り切れません"
		)
		strokeWeight(1f)
		val coordinates = mutableListOf<Coordinate<Int>>()

		// 左辺
		for (y in 0..height step offset) coordinates.add(Coordinate(0, y))

		// 底辺
		for (x in 0..width step offset) coordinates.add(Coordinate(x, height))

		// 右辺
		for (y in height downTo offset step offset) coordinates.add(Coordinate(width, y))

		// 上辺
		for (x in width downTo offset step offset) coordinates.add(Coordinate(x, 0))

		// もっかい左辺
		for (y in 0..height step offset) coordinates.add(Coordinate(0, y))

		val countOfPointsInSide = width / offset
		colorMode(HSB, (coordinates.size - countOfPointsInSide).toFloat(), 1f, 1f)
		for (i in 0 until coordinates.size - countOfPointsInSide) {
			stroke(i.toFloat(), 1f, 1f)
			line(
				coordinates[i].x.toFloat(),
				coordinates[i].y.toFloat(),
				coordinates[i + countOfPointsInSide].x.toFloat(),
				coordinates[i + countOfPointsInSide].y.toFloat()
			)
		}
	}
}

class FacePattern : PApplet() {
	override fun settings() {
		size(700, 700)
	}

	override fun setup() {
		background(255)
		noFill()
		noLoop()
	}

	override fun draw() {
		strokeWeight(9f);  //line weight
		stroke(50f, 200f, 50f);  //line color RGB
		rect(0f, 0f, 300f, 250f);
		rect(50f, 100f, 30f, 30f);//left eye
		rect(220f, 100f, 30f, 30f);//right eye
		rect(80f, 180f, 150f, 20f);//mouth
	}
}

class LinePatternByAngle : PApplet() {
	override fun settings() {
		size(700, 700)
	}

	override fun setup() {
		background(255)
		noFill()
		noLoop()
	}

	fun drawTree(from: Vector, radius: Float, depth: Int) {
		val root = from + Vector(0f, -10f)
		if (depth != 0) {
			val to1 = root + Polar(radius, -(2f / 3f) * PI)
			val to2 = root + Polar(radius, -(1f / 3f) * PI)
			line(root.x, root.y, to1.x, to1.y)
			line(root.x, root.y, to2.x, to2.y)
			drawTree(to1, radius, depth - 1)
			drawTree(to2, radius, depth - 1)
		}
		line(from.x, from.y, root.x, root.y)
	}

	override fun draw() {
		val from = Vector(350f, 600f)
//        val to = getPointFromAngle(from, (1f/4f) * PI, 40f)
//        line(from.x, from.y, to.x, to.y)
		drawTree(from, 20f, 4)
	}
}

class DrawTree : PApplet() {
	override fun settings() {
		size(1000, 1000)
	}

	override fun setup() {
		background(255)
		noFill()
		noLoop()
	}

	fun drawLine(from: Vector, to: Vector) {
		line(from.x, from.y, to.x, to.y)
	}

	fun drawTree(from: Vector, angle: Float, radius: Float, depth: Int, count: Int = 0) {
		val shorteningRate = 4f / 6f
		strokeWeight(depth.toFloat() * 10f / (count + 1).toFloat())
		stroke(120f, (count + 1).toFloat(), (count + depth).toFloat())
		if (count == 0 && depth > 0) {
			val to = from + Polar(radius, angle)
			drawLine(from, to)
			drawTree(to, angle, shorteningRate * radius, depth - 1, 1)
		} else if (depth > 0) {
			val first = from + Polar(radius, angle + (-PI / 6f))
			val second = from + Polar(radius, angle + (PI / 6f))
			drawLine(from, first)
			drawLine(from, second)
			drawTree(first, angle + (-PI / 6f), shorteningRate * radius, depth - 1, count + 1)
			drawTree(second, angle + (PI / 6f), shorteningRate * radius, depth - 1, count + 1)
		}
	}

	override fun draw() {
		val root = Vector(width.toFloat() / 2f, height.toFloat())
		val depth = 20
		colorMode(HSB, 360f, depth.toFloat(), depth.toFloat())
		drawTree(root, (-PI / 2f), 200f, 10)
	}
}


fun main() {
//    PApplet.main(LinePattern::class.java.name)
//    PApplet.main(FacePattern::class.java.name)
//    PApplet.main(LinePatternByAngle::class.java.name)
	PApplet.main(DrawTree::class.java.name)
}
