package com.example.backend.common.persistence.postgis

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.postgresql.util.PGobject
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GeographyPointColumnType : ColumnType<GeoPoint>() {
    override fun sqlType(): String = "geography(Point, 4326)"

    override fun notNullValueToDB(value: GeoPoint): Any =
        PGobject().apply {
            type = "geography"
            this.value = "SRID=4326;POINT(${value.longitude} ${value.latitude})"
        }

    override fun valueFromDB(value: Any): GeoPoint {
        // 같은 트랜잭션에서 insert 후 되읽을 때는 Exposed 캐시가 이미 변환된 GeoPoint 를 그대로 넘긴다. 그대로 반환한다.
        if (value is GeoPoint) return value

        val ewkbHex =
            when (value) {
                is String -> value
                is PGobject -> requireNotNull(value.value) { "PostGIS geography 값이 비어 있습니다." }
                else -> error("지원하지 않는 PostGIS geography 값 타입입니다: ${value::class.qualifiedName}")
            }

        return parseEwkbPoint(ewkbHex)
    }

    private fun parseEwkbPoint(ewkbHex: String): GeoPoint {
        val normalizedHex = ewkbHex.removePrefix("\\x").removePrefix("0x")
        require(normalizedHex.length % 2 == 0) { "EWKB hex 길이가 올바르지 않습니다." }

        val bytes =
            ByteArray(normalizedHex.length / 2) { index ->
                normalizedHex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        require(bytes.size >= EWKB_POINT_WITH_SRID_SIZE) { "EWKB Point 데이터가 너무 짧습니다." }

        val byteOrder =
            when (bytes[0].toInt() and 0xFF) {
                0 -> ByteOrder.BIG_ENDIAN
                1 -> ByteOrder.LITTLE_ENDIAN
                else -> error("EWKB byte order 값이 올바르지 않습니다.")
            }
        val buffer = ByteBuffer.wrap(bytes).order(byteOrder)
        buffer.get()

        val type = buffer.int
        require(type and EWKB_TYPE_MASK == EWKB_POINT_TYPE) { "EWKB geometry 타입이 Point가 아닙니다." }
        require(type and EWKB_SRID_FLAG != 0) { "EWKB Point에 SRID가 없습니다." }

        val srid = buffer.int
        require(srid == WGS84_SRID) { "EWKB Point의 SRID가 $WGS84_SRID 이 아닙니다: $srid" }

        val longitude = buffer.double
        val latitude = buffer.double
        return GeoPoint(latitude = latitude, longitude = longitude)
    }

    private companion object {
        const val WGS84_SRID = 4326
        const val EWKB_POINT_TYPE = 1
        const val EWKB_SRID_FLAG = 0x20000000
        const val EWKB_TYPE_MASK = 0x0FFFFFFF
        const val EWKB_POINT_WITH_SRID_SIZE = 1 + Int.SIZE_BYTES + Int.SIZE_BYTES + Double.SIZE_BYTES * 2
    }
}

fun Table.geographyPoint(name: String): Column<GeoPoint> = registerColumn(name, GeographyPointColumnType())
