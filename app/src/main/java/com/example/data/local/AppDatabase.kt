package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        OrderEntity::class,
        RiderEntity::class,
        CouponEntity::class,
        NotificationEntity::class,
        AddressEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gharkirana_grocery.db"
                ).fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed database in background coroutine
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).appDao()
                            dao.insertCategories(SeedData.categories)
                            dao.insertProducts(SeedData.products)
                            dao.insertRiders(SeedData.riders)
                            dao.insertCoupons(SeedData.coupons)
                            for (addr in SeedData.defaultAddresses) {
                                dao.insertAddress(addr)
                            }
                            for (order in SeedData.createInitialOrders()) {
                                dao.insertOrder(order)
                            }
                            for (notif in SeedData.initialNotifications) {
                                dao.insertNotification(notif)
                            }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
