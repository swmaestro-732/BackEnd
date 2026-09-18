package com.example.backend.common.persistence.postgis

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.geo.Viewport
import org.jetbrains.exposed.v1.core.BooleanColumnType
import org.jetbrains.exposed.v1.core.Cast
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.doubleLiteral
import org.jetbrains.exposed.v1.core.intLiteral

fun makePoint(
    latitude: Double,
    longitude: Double,
): Expression<Coordinate> {
    val point =
        CustomFunction(
            "ST_MakePoint",
            GeometryColumnType,
            doubleLiteral(longitude),
            doubleLiteral(latitude),
        )
    val pointWithSrid = CustomFunction("ST_SetSRID", GeometryColumnType, point, intLiteral(WGS84_SRID))
    return Cast(pointWithSrid, GeographyPointColumnType())
}

fun Expression<Coordinate>.stX(): ExpressionWithColumnType<Double> =
    CustomFunction("ST_X", DoubleColumnType(), Cast(this, GeometryColumnType))

fun Expression<Coordinate>.stY(): ExpressionWithColumnType<Double> =
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

/** 지도 사각형은 평면 경계로 비교한다. geography 폴리곤의 곡선 경계로 변환하지 않는다. */
fun Expression<Coordinate>.intersectsViewport(viewport: Viewport): Op<Boolean> {
    val envelope =
        CustomFunction(
            "ST_MakeEnvelope",
            GeometryColumnType,
            doubleLiteral(viewport.southWest.longitude),
            doubleLiteral(viewport.southWest.latitude),
            doubleLiteral(viewport.northEast.longitude),
            doubleLiteral(viewport.northEast.latitude),
            intLiteral(WGS84_SRID),
        )
    return PostGisBooleanFunction("ST_Intersects", Cast(this, GeometryColumnType), envelope)
}

private object GeometryColumnType : ColumnType<Any>() {
    override fun sqlType(): String = "geometry"

    override fun valueFromDB(value: Any): Any = value
}

/** OpenSearch geohash_grid와 같은 WGS84 격자 키. */
fun Expression<Coordinate>.geoHash(precision: Int): Expression<String> =
    CustomFunction(
        "ST_GeoHash",
        org.jetbrains.exposed.v1.core
            .TextColumnType(),
        Cast(this, GeometryColumnType),
        intLiteral(precision),
    )

private class PostGisBooleanFunction(
    functionName: String,
    vararg expressions: Expression<*>,
) : Op<Boolean>() {
    private val function = CustomFunction(functionName, BooleanColumnType(), *expressions)

    override fun toQueryBuilder(queryBuilder: QueryBuilder) = function.toQueryBuilder(queryBuilder)
}

private const val WGS84_SRID = 4326
