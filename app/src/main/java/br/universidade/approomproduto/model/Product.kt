package br.universidade.approomproduto.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price") val price: Double,
    @ColumnInfo(name = "description") val description: String?,
    // A8: novo campo (quantidade em estoque) -> versão do banco subiu para 2
    @ColumnInfo(name = "quantity") val quantity: Int = 0
)
