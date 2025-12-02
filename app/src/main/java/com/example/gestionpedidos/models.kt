
// ==========================
// File: models.kt
// Package: com.example.gestionpedidos
// ==========================

package com.example.gestionpedidos

import java.util.*
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

import com.google.gson.annotations.SerializedName


@Parcelize
data class DetallePedido(
    val productoId: Int,
    val productoNombre: String,
    var cantidadPedida: Double,
    var precioEsperado: Double,
    val presentacionId: Int?,
    val codigoDeBarras: String? = null,
    var confirmado: Boolean = false
) : Parcelable

data class Pedido(
    val idPedido: Int,
    val fechaPedido: String,
    val proveedor: String,
    var estadoNombre: String,
    val detallesPedido: List<DetallePedido>
)

data class DetalleCompra(
    val productoId: Int,
    val productoNombre: String,
    val codigoDeBarras: String,
    var cantidadCompra: Double,
    var precioUnitario: Double
)

data class Compra(
    var idCompra: Int = 0,
    val fechaCompra: String,
    val proveedor: String,
    val estado: String = "abierto",
    val detallesCompra: MutableList<DetalleCompra> = mutableListOf()
)

data class Presentacion(
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
    @SerializedName("presentaciones") val presentaciones: List<Presentacion>,
    @SerializedName("presentacionVentaDefaultId") val presentacionVentaDefaultId: Int?,
    @SerializedName("presentacionCompraDefaultId") val presentacionCompraDefaultId: Int?,
    @SerializedName("existenciasDisponibles") val existenciasDisponibles: Double,
    @SerializedName("tieneStockBajo") val tieneStockBajo: Boolean,
    @SerializedName("esServicio") val esServicio: Boolean,
    @SerializedName("manejaInventario") val manejaInventario: Boolean,
    @SerializedName("permiteVentaNegativa") val permiteVentaNegativa: Boolean
)

data class Producto(
    val id: Int,
    val nombre: String,
    val codigoBarras: String?,
    val costo: Double,
    val precioVenta: Double,
    val existencias: Double,
    val categoriaId: Int,
    val subcategoriaId: Int,
    val estadoId: Int,
    val categoriaNombre: String,
    val presentacionId: Int?,
    val subcategoriaNombre: String,
    val estadoNombre: String
)

data class Proveedor(
    val id: Int,
    val nombre: String
)

// modelos tal como se recibel del backend
/**
 * Modelo para un Pedido tal como viene del endpoint de la lista de pedidos.
 * Usa @SerializedName para mapear los nombres del JSON a nombres de propiedad más claros.
 */
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

/**
 * Modelo para un Detalle de Pedido anidado dentro de PedidoResponse.
 */
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


// --- FUNCIONES DE MAPEO (Convierten modelos de API a modelos de UI) ---

/**
 * Convierte un ProductoResponse (de la API) a un modelo Producto (para la UI).
 * Esta función desacopla tu lógica de la UI de la estructura exacta de la API.
 */
fun ProductoResponse.aProductoDeUI(): Producto {
    // LÓGICA DE SELECCIÓN DE PRESENTACIÓN MEJORADA:
    // 1. Busca la presentación que coincida con 'presentacionCompraDefaultId'.
    // 2. Si no la encuentra, busca la que esté marcada como 'esUnidadBase'.
    // 3. Si tampoco la encuentra, toma la primera de la lista.
    // 4. Si la lista está vacía, el resultado será null.
    val presentacionPrincipal = this.presentaciones.find { it.id == this.presentacionCompraDefaultId }
        ?: this.presentaciones.find { it.esUnidadBase }
        ?: this.presentaciones.firstOrNull()

    return Producto(
        id = this.id,
        nombre = this.nombre,
        codigoBarras = presentacionPrincipal?.codigoBarras,
        costo = presentacionPrincipal?.costo ?: 0.0,
        existencias = this.existenciasDisponibles,
        categoriaNombre = this.categoriaNombre,
        subcategoriaNombre = this.subcategoriaNombre,
        estadoNombre = this.estadoNombre,
        categoriaId = this.categoriaId,
        subcategoriaId = this.subcategoriaId,
        estadoId = this.estadoId,
        presentacionId = presentacionPrincipal?.id,
        precioVenta = presentacionPrincipal?.precioVenta ?: 0.0,
    )
}


/**
 * Convierte un PedidoResponse (de la API) a un modelo Pedido (para la UI).
 */
fun PedidoResponse.aPedidoDeUI(): Pedido {
    return Pedido(
        idPedido = this.id,
        fechaPedido = this.fechaPedido,
        proveedor = this.proveedorNombre,
        estadoNombre = this.estado,
        detallesPedido = this.detalles?.map { detalleResponse ->
            DetallePedido(
                productoId = detalleResponse.productoId,
                productoNombre = detalleResponse.productoNombre,
                cantidadPedida = detalleResponse.cantidadPedida,
                precioEsperado = detalleResponse.precioUnitario,
                codigoDeBarras = detalleResponse.codigoBarras,
                presentacionId = detalleResponse.presentacionId,
                confirmado = false // O basado en alguna lógica si la tienes
            )
        } ?: emptyList() // Si no hay detalles, devuelve una lista vacía
    )
}


// --- MODELOS PARA LA PETICIÓN (REQUEST) DE GUARDAR PEDIDO ---

/**
 * Representa el cuerpo (body) de la petición POST para crear un nuevo pedido.
 */
data class PedidoRequest(
    @SerializedName("fechaPedido") val fechaPedido: String,
    @SerializedName("fechaEntregaEsperada") val fechaEntregaEsperada: String,
    @SerializedName("proveedorId") val proveedorId: Int,
    @SerializedName("estado") val estado: String = "Requerido",
    @SerializedName("observaciones") val observaciones: String? = null,
    @SerializedName("flete") val flete: Double = 0.0,
    @SerializedName("detalles") val detalles: List<DetallePedidoRequest>,
    // Los campos calculados como 'totalNeto', 'totalIva', etc.,
    // serán en un futuro calculado por el backend , pero de momento los estamos enviado aca
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

/**
 * Representa un detalle de pedido dentro de la petición POST.
 */
data class DetallePedidoRequest(
    @SerializedName("productoId") val productoId: Int,
    @SerializedName("presentacionId") val presentacionId: Int?,
    @SerializedName("cantidadPedida") val cantidadPedida: Double,
    @SerializedName("precioUnitario") val precioUnitario: Double,
    @SerializedName("descuentoPreIva") val descuentoPreIva: Double = 0.0,
    @SerializedName("iva") val iva: Double = 0.0, // Puedes ajustar estos valores por defecto
    @SerializedName ("otrosImpuestos") val otrosImpuestos: Double = 0.00,
    @SerializedName ("otrosImpuestosDetalle") val otrosImpuestosDetalle: String? = null,
    @SerializedName ("descuentoPosIva") val descuentoPosIva: Double = 0.00,
    @SerializedName ("neto") val neto: Double = 0.00,
    @SerializedName ("flete") val flete: Double = 0.00,
    @SerializedName("observacion") val observacion: String? = null,
)