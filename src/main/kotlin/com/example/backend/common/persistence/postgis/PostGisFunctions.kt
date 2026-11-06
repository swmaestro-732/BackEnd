package com.example.backend.common.persistence.postgis

import com.example.backend.common.geo.Coordinate
import org.jetbrains.exposed.v1.core.BooleanColumnType
import org.jetbrains.exposed.v1.core.Cast
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.doubleLiteral
import org.jetbrains.exposed.v1.core.intLiteral

fun makePoint(
    latitude: Double,
    longitude: Double,
): Expression<Coordinate> = Cast(geometryPoint(latitude, longitude), GeographyPointColumnType())

/** SRID 4326 geometry 점. 폴리곤 비교(ST_Contains·GIST 인덱스)에 쓴다. */
fun geometryPoint(
    latitude: Double,
    longitude: Double,
): Expression<Any> {
    val point =
        CustomFunction(
            "ST_MakePoint",
            GeometryColumnType,
            doubleLiteral(longitude),
            doubleLiteral(latitude),
        )
    return CustomFunction("ST_SetSRID", GeometryColumnType, point, intLiteral(WGS84_SRID))
}

fun Expression<Coordinate>.stX(): Expression<Double> =
    CustomFunction("ST_X", DoubleColumnType(), Cast(this, GeometryColumnType))

fun Expression<Coordinate>.stY(): Expression<Double> =
    CustomFunction("ST_Y", DoubleColumnType(), Cast(this, GeometryColumnType))

fun Expression<Coordinate>.stDistance(other: Expression<Coordinate>): Expression<Double> =
    CustomFunction("ST_Distance", DoubleColumnType(), this, other)

fun Expression<Coordinate>.stDWithin(
    other: Expression<Coordinate>,
    meters: Double,
): Op<Boolean> =
    PostGisBooleanFunction(
        "ST_DWithin",
        this,
        other,
        doubleLiteral(meters),
    )

fun Expression<Any>.stContains(other: Expression<Any>): Op<Boolean> = PostGisBooleanFunction("ST_Contains", this, other)

/** 도(degree) 단위 거리 필터. GIST 인덱스로 후보를 먼저 거를 때 쓴다. */
fun Expression<Any>.stDWithinDegrees(
    other: Expression<Any>,
    degrees: Double,
): Op<Boolean> = PostGisBooleanFunction("ST_DWithin", this, other, doubleLiteral(degrees))

/** geography 로 바꿔 잰 미터 거리. */
fun Expression<Any>.stDistanceMeters(other: Expression<Any>): Expression<Double> =
    CustomFunction(
        "ST_Distance",
        DoubleColumnType(),
        Cast(this, GeographyColumnType),
        Cast(other, GeographyColumnType),
    )

object GeometryColumnType : ColumnType<Any>() {
    override fun sqlType(): String = "geometry"

    override fun valueFromDB(value: Any): Any = value
}

private object GeographyColumnType : ColumnType<Any>() {
    override fun sqlType(): String = "geography"

    override fun valueFromDB(value: Any): Any = value
}

private class PostGisBooleanFunction(
    functionName: String,
    vararg expressions: Expression<*>,
) : Op<Boolean>() {
    private val function = CustomFunction(functionName, BooleanColumnType(), *expressions)

    override fun toQueryBuilder(queryBuilder: QueryBuilder) = function.toQueryBuilder(queryBuilder)
}

private const val WGS84_SRID = 4326
