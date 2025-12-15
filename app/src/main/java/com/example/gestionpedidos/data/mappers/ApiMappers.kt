// ==========================
// File: ApiMappers.kt
// Package: com.example.gestionpedidos.data.mappers
// ==========================

package com.example.gestionpedidos.data.mappers

import com.example.gestionpedidos.data.local.entities.*
import com.example.gestionpedidos.data.local.dao.*
import com.google.gson.annotations.SerializedName

// ============================================
// MODELOS DE API (Response) - IMPORTADOS DESDE EL PAQUETE RAÍZ
// ============================================

// NOTA: Si ya tienes ProductoResponse y PedidoResponse en tu models.kt,
// usa esos. Si no, descomenta estas definiciones:

/*
data class ProductoResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("sku") val sku: String?,
    @SerializedName("imagenUrl") val imagenUrl: String?,
    @SerializedName("categoriaId") val categoriaId: Int,
    @SerializedName("categoriaNombre") val categoriaNombre: String,
    @SerializedName("subcategoriaId") val subcategoriaId: Int,
    @SerializedName("subcategoriaNombre") val subcategoriaNombre: String,
    @SerializedName("estadoId") val estadoId: Int,
    @SerializedName("estadoNombre") val estadoNombre: String,
    @SerializedName("familiaId") val familiaId: Int?,
    @SerializedName("familiaNombre") val familiaNombre: String?,
    @SerializedName("marcaId") val marcaId: Int?,
    @SerializedName("marcaNombre") val marcaNombre: String?,
    @SerializedName("presentaciones") val presentaciones: List<PresentacionResponse>,
    @SerializedName("presentacionVentaDefaultId") val presentacionVentaDefaultId: Int?,
    @SerializedName("presentacionCompraDefaultId") val presentacionCompraDefaultId: Int?,
    @SerializedName("existenciasDisponibles") val existenciasDisponibles: Double,
    @SerializedName("tieneStockBajo") val tieneStockBajo: Boolean,
    @SerializedName("esServicio") val esServicio: Boolean,
    @SerializedName("manejaInventario") val manejaInventario: Boolean,
    @SerializedName("permiteVentaNegativa") val permiteVentaNegativa: Boolean
)

data class PresentacionResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("codigoBarras") val codigoBarras: String?,
    @SerializedName("factor") val factor: Double,
    @SerializedName("costo") val costo: Double,
    @SerializedName("precioVenta") val precioVenta: Double,
    @SerializedName("orden") val orden: Int,
    @SerializedName("esUnidadBase") val esUnidadBase: Boolean,
    @SerializedName("existenciasEnPresentacion") val existenciasEnPresentacion: Double,
    @SerializedName("existenciasDisponiblesEnPresentacion") val existenciasDisponiblesEnPresentacion: Double
)

data class PedidoResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("fechaPedido") val fechaPedido: String,
    @SerializedName("fechaEntregaEsperada") val fechaEntregaEsperada: String,
    @SerializedName("fechaEntregaReal") val fechaEntregaReal: String?,
    @SerializedName("proveedorId") val proveedorId: Int,
    @SerializedName("proveedorNombre") val proveedorNombre: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("observaciones") val observaciones: String?,
    @SerializedName("flete") val flete: Double,
    @SerializedName("detalles") val detalles: List<DetallePedidoResponse>?,
    @SerializedName("totalNeto") val totalNeto: Double
)

data class DetallePedidoResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("productoId") val productoId: Int,
    @SerializedName("productoNombre") val productoNombre: String,
    @SerializedName("presentacionId") val presentacionId: Int?,
    @SerializedName("presentacionNombre") val presentacionNombre: String?,
    @SerializedName("codigoBarras") val codigoBarras: String?,
    @SerializedName("cantidadPedida") val cantidadPedida: Double,
    @SerializedName("precioUnitario") val precioUnitario: Double,
    @SerializedName("iva") val iva: Double,
    @SerializedName("cantidadRecibida") val cantidadRecibida: Double?,
    @SerializedName("subtotalProducto") val subtotalProducto: Double,
    @SerializedName("costoUnitarioFinal") val costoUnitarioFinal: Double
)
*/

// ============================================
// MODELOS DE API (Request)
// ============================================

data class PedidoRequest(
    @SerializedName("fechaPedido") val fechaPedido: String,
    @SerializedName("fechaEntregaEsperada") val fechaEntregaEsperada: String,
    @SerializedName("proveedorId") val proveedorId: Int,
    @SerializedName("estado") val estado: String = "Requerido",
    @SerializedName("observaciones") val observaciones: String? = null,
    @SerializedName("flete") val flete: Double = 0.0,
    @SerializedName("detalles") val detalles: List<DetallePedidoRequest>,
    @SerializedName("subtotalProductos") val subtotalProductos: Double = 0.0,
    @SerializedName("totalDescuentosPreIva") val totalDescuentosPreIva: Double = 0.0,
    @SerializedName("baseGravableTotal") val baseGravableTotal: Double = 0.0,
    @SerializedName("totalIva") val totalIva: Double = 0.0,
    @SerializedName("totalOtrosImpuestos") val totalOtrosImpuestos: Double = 0.0,
    @SerializedName("subtotalConImpuestos") val subtotalConImpuestos: Double = 0.0,
    @SerializedName("totalDescuentosPosIva") val totalDescuentosPosIva: Double = 0.0,
    @SerializedName("subtotalSinFlete") val subtotalSinFlete: Double = 0.0,
    @SerializedName("neto") val neto: Double = 0.0,
)

data class DetallePedidoRequest(
    @SerializedName("productoId") val productoId: Int,
    @SerializedName("presentacionId") val presentacionId: Int?,
    @SerializedName("cantidadPedida") val cantidadPedida: Double,
    @SerializedName("precioUnitario") val precioUnitario: Double,
    @SerializedName("descuentoPreIva") val descuentoPreIva: Double = 0.0,
    @SerializedName("iva") val iva: Double = 0.0,
    @SerializedName("otrosImpuestos") val otrosImpuestos: Double = 0.00,
    @SerializedName("otrosImpuestosDetalle") val otrosImpuestosDetalle: String? = null,
    @SerializedName("descuentoPosIva") val descuentoPosIva: Double = 0.00,
    @SerializedName("neto") val neto: Double = 0.00,
    @SerializedName("flete") val flete: Double = 0.00,
    @SerializedName("observacion") val observacion: String? = null,
)

// ============================================
// MAPPERS: API RESPONSE -> ROOM ENTITY
// ============================================

// Extensiones para com.example.gestionpedidos.ProductoResponse (del paquete raíz)
fun com.example.gestionpedidos.ProductoResponse.toEntity(): ProductoEntity {
    return ProductoEntity(
        id = id,
        nombre = nombre,
        sku = sku,
        imagenUrl = imagenUrl,
        categoriaId = categoriaId,
        categoriaNombre = categoriaNombre,
        subcategoriaId = subcategoriaId,
        subcategoriaNombre = subcategoriaNombre,
        estadoId = estadoId,
        estadoNombre = estadoNombre,
        familiaId = familiaId,
        familiaNombre = familiaNombre,
        marcaId = marcaId,
        marcaNombre = marcaNombre,
        presentacionVentaDefaultId = presentacionVentaDefaultId,
        presentacionCompraDefaultId = presentacionCompraDefaultId,
        existenciasDisponibles = existenciasDisponibles,
        tieneStockBajo = tieneStockBajo,
        esServicio = esServicio,
        manejaInventario = manejaInventario,
        permiteVentaNegativa = permiteVentaNegativa
    )
}

fun com.example.gestionpedidos.Presentacion.toEntity(productoId: Int): PresentacionEntity {
    return PresentacionEntity(
        id = id,
        productoId = productoId,
        nombre = nombre,
        codigoBarras = codigoBarras,
        factor = factor,
        costo = costo,
        precioVenta = precioVenta,
        orden = orden,
        esUnidadBase = esUnidadBase,
        existenciasEnPresentacion = existenciasEnPresentacion,
        existenciasDisponiblesEnPresentacion = existenciasDisponiblesEnPresentacion
    )
}

fun com.example.gestionpedidos.PedidoResponse.toEntity(): PedidoEntity {
    return PedidoEntity(
        id = id,
        fechaPedido = fechaPedido,
        fechaEntregaEsperada = fechaEntregaEsperada,
        fechaEntregaReal = fechaEntregaReal,
        proveedorId = proveedorId,
        proveedorNombre = proveedorNombre,
        estado = estado,
        observaciones = observaciones,
        flete = flete,
        totalNeto = totalNeto
    )
}

fun com.example.gestionpedidos.DetallePedidoResponse.toEntity(pedidoId: Int): DetallePedidoEntity {
    return DetallePedidoEntity(
        id = id,
        pedidoId = pedidoId,
        productoId = productoId,
        productoNombre = productoNombre,
        presentacionId = presentacionId,
        presentacionNombre = presentacionNombre,
        codigoBarras = codigoBarras,
        cantidadPedida = cantidadPedida,
        precioUnitario = precioUnitario,
        iva = iva,
        cantidadRecibida = cantidadRecibida,
        subtotalProducto = subtotalProducto,
        costoUnitarioFinal = costoUnitarioFinal
    )
}

// ============================================
// MAPPERS: ROOM ENTITY -> API REQUEST
// ============================================

fun PedidoEntity.toRequest(detalles: List<DetallePedidoEntity>): PedidoRequest {
    return PedidoRequest(
        fechaPedido = fechaPedido,
        fechaEntregaEsperada = fechaEntregaEsperada,
        proveedorId = proveedorId,
        estado = estado,
        observaciones = observaciones,
        flete = flete,
        detalles = detalles.map { it.toRequest() },
        subtotalProductos = subtotalProductos,
        totalDescuentosPreIva = totalDescuentosPreIva,
        baseGravableTotal = baseGravableTotal,
        totalIva = totalIva,
        totalOtrosImpuestos = totalOtrosImpuestos,
        subtotalConImpuestos = subtotalConImpuestos,
        totalDescuentosPosIva = totalDescuentosPosIva,
        subtotalSinFlete = subtotalSinFlete,
        neto = totalNeto
    )
}

fun DetallePedidoEntity.toRequest(): DetallePedidoRequest {
    return DetallePedidoRequest(
        productoId = productoId,
        presentacionId = presentacionId,
        cantidadPedida = cantidadPedida,
        precioUnitario = precioUnitario,
        descuentoPreIva = descuentoPreIva,
        iva = iva,
        otrosImpuestos = otrosImpuestos,
        otrosImpuestosDetalle = otrosImpuestosDetalle,
        descuentoPosIva = descuentoPosIva,
        neto = neto,
        flete = flete,
        observacion = observacion
    )
}

// ============================================
// FUNCIONES DE EXTENSIÓN PARA GUARDAR
// ============================================

/**
 * Guarda un ProductoResponse completo (producto + presentaciones) en Room
 */
suspend fun com.example.gestionpedidos.ProductoResponse.saveToDatabase(
    productoDao: ProductoDao,
    presentacionDao: PresentacionDao
) {
    // Guardar el producto
    productoDao.insertProducto(this.toEntity())

    // Guardar las presentaciones
    val presentacionesEntity = this.presentaciones.map { it.toEntity(this.id) }
    presentacionDao.insertPresentaciones(presentacionesEntity)
}

/**
 * Guarda un PedidoResponse completo (pedido + detalles) en Room
 */
suspend fun com.example.gestionpedidos.PedidoResponse.saveToDatabase(
    pedidoDao: PedidoDao,
    detallePedidoDao: DetallePedidoDao
) {
    // Guardar el pedido
    pedidoDao.insertPedido(this.toEntity())

    // Guardar los detalles
    val detallesEntity = this.detalles?.map { it.toEntity(this.id) } ?: emptyList()
    detallePedidoDao.insertDetalles(detallesEntity)
}