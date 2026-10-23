package br.universidade.approomproduto.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.universidade.approomproduto.model.Product

@Dao
interface ProductDao {
    // suspend: o Room não permite consultas na main thread
    @Query("SELECT * FROM product")
    suspend fun getAll(): List<Product>

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun getById(id: Int): Product?

    // A5: busca por nome (o "%" é colocado na MainActivity)
    @Query("SELECT * FROM product WHERE name LIKE :nome")
    suspend fun buscarPorNome(nome: String): List<Product>

    @Insert
    suspend fun insertAll(vararg products: Product)

    // A7: atualiza o produto com o mesmo id (retorna quantas linhas mudaram)
    @Update
    suspend fun update(product: Product): Int

    // A6 (desafio extra): exclui um produto específico pelo id
    @Delete
    suspend fun delete(product: Product)

    // A6: exclui todos os produtos
    @Query("DELETE FROM product")
    suspend fun deleteAll()
}
