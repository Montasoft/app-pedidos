package com.example.gestionpedidos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gestionpedidos.data.local.dao.*
import com.example.gestionpedidos.data.local.entities.*

@Database(
    entities = [
        PedidoEntity::class,
        DetallePedidoEntity::class,
        ProductoEntity::class,
        PresentacionEntity::class,
        ProveedorEntity::class,
        CompraEntity::class,
        DetalleCompraEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pedidoDao(): PedidoDao
    abstract fun detallePedidoDao(): DetallePedidoDao
    abstract fun productoDao(): ProductoDao
    abstract fun presentacionDao(): PresentacionDao
    abstract fun proveedorDao(): ProveedorDao
    abstract fun compraDao(): CompraDao
    abstract fun detalleCompraDao(): DetalleCompraDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "gestion_pedidos_db"

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // Solo para desarrollo
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Limpia la instancia de la base de datos (útil para testing)
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}