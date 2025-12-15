package com.example.gestionpedidos.data.local.dao
// ==========================
// File: AppDao.kt
// Package: com.example.gestionpedidos.data.local.dao
// ==========================

import androidx.room.*
import com.example.gestionpedidos.data.local.entities.*
import kotlinx.coroutines.flow.Flow


// ============================================
// PEDIDOS DAO
// ============================================

@Dao
interface PedidoDao {

    @Transaction
    @Query("SELECT * FROM pedidos ORDER BY fechaPedido DESC")
    fun getAllPedidosConDetalles(): Flow<List<PedidoConDetalles>>

    @Transaction
    @Query("SELECT * FROM pedidos WHERE id = :pedidoId")
    suspend fun getPedidoConDetalles(pedidoId: Int): PedidoConDetalles?

    @Query("SELECT * FROM pedidos WHERE id = :pedidoId")
    suspend fun getPedido(pedidoId: Int): PedidoEntity?

    @Query("SELECT * FROM pedidos WHERE estado = :estado ORDER BY fechaPedido DESC")
    fun getPedidosPorEstado(estado: String): Flow<List<PedidoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPedido(pedido: PedidoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPedidos(pedidos: List<PedidoEntity>)

    @Update
    suspend fun updatePedido(pedido: PedidoEntity)

    @Delete
    suspend fun deletePedido(pedido: PedidoEntity)

    @Query("DELETE FROM pedidos WHERE id = :pedidoId")
    suspend fun deletePedidoById(pedidoId: Int)

    @Query("DELETE FROM pedidos")
    suspend fun deleteAllPedidos()
}

// ============================================
// DETALLE PEDIDOS DAO
// ============================================

@Dao
interface DetallePedidoDao {

    @Query("SELECT * FROM detalle_pedidos WHERE pedidoId = :pedidoId")
    suspend fun getDetallesPorPedido(pedidoId: Int): List<DetallePedidoEntity>

    @Query("SELECT * FROM detalle_pedidos WHERE id = :detalleId")
    suspend fun getDetalle(detalleId: Int): DetallePedidoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetalle(detalle: DetallePedidoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetalles(detalles: List<DetallePedidoEntity>)

    @Update
    suspend fun updateDetalle(detalle: DetallePedidoEntity)

    @Delete
    suspend fun deleteDetalle(detalle: DetallePedidoEntity)

    @Query("DELETE FROM detalle_pedidos WHERE pedidoId = :pedidoId")
    suspend fun deleteDetallesPorPedido(pedidoId: Int)

    @Query("UPDATE detalle_pedidos SET confirmado = :confirmado WHERE id = :detalleId")
    suspend fun updateConfirmado(detalleId: Int, confirmado: Boolean)
}

// ============================================
// PRODUCTOS DAO
// ============================================

@Dao
interface ProductoDao {

    @Transaction
    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    fun getAllProductosConPresentaciones(): Flow<List<ProductoConPresentaciones>>

    @Transaction
    @Query("SELECT * FROM productos WHERE id = :productoId")
    suspend fun getProductoConPresentaciones(productoId: Int): ProductoConPresentaciones?

    @Query("SELECT * FROM productos WHERE id = :productoId")
    suspend fun getProducto(productoId: Int): ProductoEntity?

    @Query("SELECT * FROM productos WHERE categoriaId = :categoriaId")
    fun getProductosPorCategoria(categoriaId: Int): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE subcategoriaId = :subcategoriaId")
    fun getProductosPorSubcategoria(subcategoriaId: Int): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE nombre LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%'")
    fun searchProductos(query: String): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE tieneStockBajo = 1")
    fun getProductosStockBajo(): Flow<List<ProductoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducto(producto: ProductoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductos(productos: List<ProductoEntity>)

    @Update
    suspend fun updateProducto(producto: ProductoEntity)

    @Delete
    suspend fun deleteProducto(producto: ProductoEntity)

    @Query("DELETE FROM productos")
    suspend fun deleteAllProductos()
}

// ============================================
// PRESENTACIONES DAO
// ============================================

@Dao
interface PresentacionDao {

    @Query("SELECT * FROM presentaciones WHERE productoId = :productoId ORDER BY orden ASC")
    suspend fun getPresentacionesPorProducto(productoId: Int): List<PresentacionEntity>

    @Query("SELECT * FROM presentaciones WHERE id = :presentacionId")
    suspend fun getPresentacion(presentacionId: Int): PresentacionEntity?

    @Query("SELECT * FROM presentaciones WHERE codigoBarras = :codigoBarras LIMIT 1")
    suspend fun getPresentacionPorCodigoBarras(codigoBarras: String): PresentacionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresentacion(presentacion: PresentacionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresentaciones(presentaciones: List<PresentacionEntity>)

    @Update
    suspend fun updatePresentacion(presentacion: PresentacionEntity)

    @Delete
    suspend fun deletePresentacion(presentacion: PresentacionEntity)

    @Query("DELETE FROM presentaciones WHERE productoId = :productoId")
    suspend fun deletePresentacionesPorProducto(productoId: Int)
}

// ============================================
// PROVEEDORES DAO
// ============================================

@Dao
interface ProveedorDao {

    @Query("SELECT * FROM proveedores ORDER BY nombre ASC")
    fun getAllProveedores(): Flow<List<ProveedorEntity>>

    @Query("SELECT * FROM proveedores WHERE id = :proveedorId")
    suspend fun getProveedor(proveedorId: Int): ProveedorEntity?

    @Query("SELECT * FROM proveedores WHERE nombre LIKE '%' || :query || '%'")
    fun searchProveedores(query: String): Flow<List<ProveedorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProveedor(proveedor: ProveedorEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProveedores(proveedores: List<ProveedorEntity>)

    @Update
    suspend fun updateProveedor(proveedor: ProveedorEntity)

    @Delete
    suspend fun deleteProveedor(proveedor: ProveedorEntity)

    @Query("DELETE FROM proveedores")
    suspend fun deleteAllProveedores()
}

// ============================================
// COMPRAS DAO
// ============================================

@Dao
interface CompraDao {

    @Transaction
    @Query("SELECT * FROM compras ORDER BY fechaCompra DESC")
    fun getAllComprasConDetalles(): Flow<List<CompraConDetalles>>

    @Transaction
    @Query("SELECT * FROM compras WHERE id = :compraId")
    suspend fun getCompraConDetalles(compraId: Int): CompraConDetalles?

    @Query("SELECT * FROM compras WHERE estado = :estado ORDER BY fechaCompra DESC")
    fun getComprasPorEstado(estado: String): Flow<List<CompraEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompra(compra: CompraEntity): Long

    @Update
    suspend fun updateCompra(compra: CompraEntity)

    @Delete
    suspend fun deleteCompra(compra: CompraEntity)

    @Query("DELETE FROM compras")
    suspend fun deleteAllCompras()
}

// ============================================
// DETALLE COMPRAS DAO
// ============================================

@Dao
interface DetalleCompraDao {

    @Query("SELECT * FROM detalle_compras WHERE compraId = :compraId")
    suspend fun getDetallesPorCompra(compraId: Int): List<DetalleCompraEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetalle(detalle: DetalleCompraEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetalles(detalles: List<DetalleCompraEntity>)

    @Update
    suspend fun updateDetalle(detalle: DetalleCompraEntity)

    @Delete
    suspend fun deleteDetalle(detalle: DetalleCompraEntity)

    @Query("DELETE FROM detalle_compras WHERE compraId = :compraId")
    suspend fun deleteDetallesPorCompra(compraId: Int)
}