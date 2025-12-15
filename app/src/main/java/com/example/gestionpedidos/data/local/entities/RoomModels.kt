package com.example.gestionpedidos.data.local.entities

import androidx.room.*
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.example.gestionpedidos.Producto
import com.example.gestionpedidos.DetallePedido
import com.example.gestionpedidos.Pedido
import com.example.gestionpedidos.Proveedor
import com.example.gestionpedidos.Presentacion
import com.example.gestionpedidos.Compra
import com.example.gestionpedidos.DetalleCompra


// ============================================
// ENTIDADES PRINCIPALES
// ============================================



@Entity(
    tableName = "pedidos",
    indices = [Index(value = ["proveedorId"])]
)
data class PedidoEntity(
    @PrimaryKey val id: Int,
    val fechaPedido: String,
    val fechaEntregaEsperada: String,
    val fechaEntregaReal: String?,
    val proveedorId: Int,
    val proveedorNombre: String,
    val estado: String,
    val observaciones: String?,
    val flete: Double,
    val totalNeto: Double,
    val subtotalProductos: Double = 0.0,
    val totalDescuentosPreIva: Double = 0.0,
    val baseGravableTotal: Double = 0.0,
    val totalIva: Double = 0.0,
    val totalOtrosImpuestos: Double = 0.0,
    val subtotalConImpuestos: Double = 0.0,
    val totalDescuentosPosIva: Double = 0.0,
    val subtotalSinFlete: Double = 0.0
)

@Entity(
    tableName = "detalle_pedidos",
    foreignKeys = [
        ForeignKey(
            entity = PedidoEntity::class,
            parentColumns = ["id"],
            childColumns = ["pedidoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["pedidoId"]),
        Index(value = ["productoId"])
    ]
)
data class DetallePedidoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pedidoId: Int,
    val productoId: Int,
    val productoNombre: String,
    val presentacionId: Int?,
    val presentacionNombre: String?,
    val codigoBarras: String?,
    val cantidadPedida: Double,
    val precioUnitario: Double,
    val iva: Double,
    val cantidadRecibida: Double?,
    val subtotalProducto: Double,
    val costoUnitarioFinal: Double,
    val confirmado: Boolean = false,
    val descuentoPreIva: Double = 0.0,
    val otrosImpuestos: Double = 0.0,
    val otrosImpuestosDetalle: String? = null,
    val descuentoPosIva: Double = 0.0,
    val neto: Double = 0.0,
    val flete: Double = 0.0,
    val observacion: String? = null
)

@Entity(tableName = "productos")
data class ProductoEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val sku: String?,
    val imagenUrl: String?,
    val categoriaId: Int,
    val categoriaNombre: String,
    val subcategoriaId: Int,
    val subcategoriaNombre: String,
    val estadoId: Int,
    val estadoNombre: String,
    val familiaId: Int?,
    val familiaNombre: String?,
    val marcaId: Int?,
    val marcaNombre: String?,
    val presentacionVentaDefaultId: Int?,
    val presentacionCompraDefaultId: Int?,
    val existenciasDisponibles: Double,
    val tieneStockBajo: Boolean,
    val esServicio: Boolean,
    val manejaInventario: Boolean,
    val permiteVentaNegativa: Boolean
)

@Entity(
    tableName = "presentaciones",
    foreignKeys = [
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productoId"])]
)
data class PresentacionEntity(
    @PrimaryKey val id: Int,
    val productoId: Int,
    val nombre: String,
    val codigoBarras: String?,
    val factor: Double,
    val costo: Double,
    val precioVenta: Double,
    val orden: Int,
    val esUnidadBase: Boolean,
    val existenciasEnPresentacion: Double,
    val existenciasDisponiblesEnPresentacion: Double
)

@Entity(tableName = "proveedores")
data class ProveedorEntity(
    @PrimaryKey val id: Int,
    val nombre: String
)

@Entity(
    tableName = "compras",
    indices = [Index(value = ["idCompra"], unique = true)]
)
data class CompraEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idCompra: Int = 0,
    val fechaCompra: String,
    val proveedor: String,
    val estado: String = "abierto"
)

@Entity(
    tableName = "detalle_compras",
    foreignKeys = [
        ForeignKey(
            entity = CompraEntity::class,
            parentColumns = ["id"],
            childColumns = ["compraId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["compraId"]),
        Index(value = ["productoId"])
    ]
)
data class DetalleCompraEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val compraId: Int,
    val productoId: Int,
    val productoNombre: String,
    val codigoDeBarras: String,
    val cantidadCompra: Double,
    val precioUnitario: Double
)

// ============================================
// DATA TRANSFER OBJECTS (DTOs)
// ============================================

/**
 * DTO para obtener un pedido con sus detalles
 */
data class PedidoConDetalles(
    @Embedded val pedido: PedidoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "pedidoId"
    )
    val detalles: List<DetallePedidoEntity>
)

/**
 * DTO para obtener un producto con sus presentaciones
 */
data class ProductoConPresentaciones(
    @Embedded val producto: ProductoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "productoId"
    )
    val presentaciones: List<PresentacionEntity>
)

/**
 * DTO para obtener una compra con sus detalles
 */
data class CompraConDetalles(
    @Embedded val compra: CompraEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "compraId"
    )
    val detalles: List<DetalleCompraEntity>
)

// ============================================
// MODELOS PARCELABLES PARA UI
// ============================================
/*
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

data class Presentacion(
    val id: Int,
    val nombre: String,
    val codigoBarras: String?,
    val factor: Double,
    val costo: Double,
    val precioVenta: Double,
    val orden: Int,
    val esUnidadBase: Boolean,
    val existenciasEnPresentacion: Double,
    val existenciasDisponiblesEnPresentacion: Double
)
*/
// ============================================
// FUNCIONES DE MAPEO: ENTITY -> UI MODEL
// ============================================

fun PedidoConDetalles.aPedidoDeUI(): Pedido {
    return Pedido(
        idPedido = pedido.id,
        fechaPedido = pedido.fechaPedido,
        proveedor = pedido.proveedorNombre,
        estadoNombre = pedido.estado,
        detallesPedido = detalles.map { it.aDetallePedidoDeUI() }
    )
}

fun DetallePedidoEntity.aDetallePedidoDeUI(): DetallePedido {
    return DetallePedido(
        productoId = productoId,
        productoNombre = productoNombre,
        cantidadPedida = cantidadPedida,
        precioEsperado = precioUnitario,
        codigoDeBarras = codigoBarras,
        presentacionId = presentacionId,
        confirmado = confirmado
    )
}

fun ProductoConPresentaciones.aProductoDeUI(): Producto {
    val presentacionPrincipal = presentaciones.find { it.id == producto.presentacionCompraDefaultId }
        ?: presentaciones.find { it.esUnidadBase }
        ?: presentaciones.firstOrNull()

    return Producto(
        id = producto.id,
        nombre = producto.nombre,
        codigoBarras = presentacionPrincipal?.codigoBarras,
        costo = presentacionPrincipal?.costo ?: 0.0,
        existencias = producto.existenciasDisponibles,
        categoriaNombre = producto.categoriaNombre,
        subcategoriaNombre = producto.subcategoriaNombre,
        estadoNombre = producto.estadoNombre,
        categoriaId = producto.categoriaId,
        subcategoriaId = producto.subcategoriaId,
        estadoId = producto.estadoId,
        presentacionId = presentacionPrincipal?.id,
        precioVenta = presentacionPrincipal?.precioVenta ?: 0.0
    )
}

fun CompraConDetalles.aCompraDeUI(): Compra {
    return Compra(
        idCompra = compra.idCompra,
        fechaCompra = compra.fechaCompra,
        proveedor = compra.proveedor,
        estado = compra.estado,
        detallesCompra = detalles.map { it.aDetalleCompraDeUI() }.toMutableList()
    )
}

fun DetalleCompraEntity.aDetalleCompraDeUI(): DetalleCompra {
    return DetalleCompra(
        productoId = productoId,
        productoNombre = productoNombre,
        codigoDeBarras = codigoDeBarras,
        cantidadCompra = cantidadCompra,
        precioUnitario = precioUnitario
    )
}

fun ProveedorEntity.aProveedorDeUI(): Proveedor {
    return Proveedor(
        id = id,
        nombre = nombre
    )
}

fun PresentacionEntity.aPresentacionDeUI(): Presentacion {
    return Presentacion(
        id = id,
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

// ============================================
// FUNCIONES DE MAPEO: UI MODEL -> ENTITY
// ============================================

fun Compra.aCompraEntity(id: Int = 0): CompraEntity {
    return CompraEntity(
        id = id,
        idCompra = idCompra,
        fechaCompra = fechaCompra,
        proveedor = proveedor,
        estado = estado
    )
}

fun DetalleCompra.aDetalleCompraEntity(compraId: Int, id: Int = 0): DetalleCompraEntity {
    return DetalleCompraEntity(
        id = id,
        compraId = compraId,
        productoId = productoId,
        productoNombre = productoNombre,
        codigoDeBarras = codigoDeBarras,
        cantidadCompra = cantidadCompra,
        precioUnitario = precioUnitario
    )
}

fun Proveedor.aProveedorEntity(): ProveedorEntity {
    return ProveedorEntity(
        id = id,
        nombre = nombre
    )
}