package br.universidade.approomproduto.view

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import br.universidade.approomproduto.dao.ProductDao
import br.universidade.approomproduto.databinding.ActivityMainBinding
import br.universidade.approomproduto.db.AppDatabase
import br.universidade.approomproduto.model.Product
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var productDao: ProductDao
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pega a instância única (Singleton) do banco de dados Room
        db = AppDatabase.getInstance(applicationContext)
        productDao = db.productDao()

        // A3: RecyclerView com a lista de produtos
        adapter = ProductAdapter(
            onItemClick = { product -> preencherAtualizacao(product) },
            onDeleteClick = { product -> excluirProduto(product) }
        )
        binding.recyclerProducts.layoutManager = LinearLayoutManager(this)
        binding.recyclerProducts.adapter = adapter

        binding.btnInserirProduto.setOnClickListener { inserirProduto() }
        binding.btnListarProdutos.setOnClickListener { listarProdutos() }
        binding.btnBuscar.setOnClickListener { buscarProdutos() }
        binding.btnAtualizarPreco.setOnClickListener { atualizarPreco() }
        binding.btnExcluirTodos.setOnClickListener { confirmarExcluirTodos() }
    }

    private fun inserirProduto() {
        val name = binding.editProductName.text.toString().trim()
        val priceText = binding.editProductPrice.text.toString()
        val description = binding.editProductDescription.text.toString().trim()
        val quantityText = binding.editProductQuantity.text.toString()

        if (name.isEmpty()) {
            binding.editProductName.error = "Informe o nome do produto"
            binding.editProductName.requestFocus()
            return
        }

        // A2: preço vazio, zero ou negativo não é aceito
        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            binding.editProductPrice.error = "Informe um preço válido"
            binding.editProductPrice.requestFocus()
            return
        }

        // A8: quantidade em estoque (vazio = 0)
        val quantity = if (quantityText.isEmpty()) 0 else quantityText.toIntOrNull()
        if (quantity == null || quantity < 0) {
            binding.editProductQuantity.error = "Informe uma quantidade válida"
            binding.editProductQuantity.requestFocus()
            return
        }

        val product = Product(name = name, price = price, description = description, quantity = quantity)

        // Insere o produto no banco de dados de forma assíncrona (usando corrotina)
        lifecycleScope.launch {
            productDao.insertAll(product)
            Log.i("TAG", "Produto inserido: $name - R$$price")
            Toast.makeText(this@MainActivity, "Produto inserido!", Toast.LENGTH_SHORT).show()
            limparCampos()
            listarProdutos()
        }
    }

    // A1: limpa os campos e volta o cursor para o Nome
    private fun limparCampos() {
        binding.editProductName.text.clear()
        binding.editProductPrice.text.clear()
        binding.editProductDescription.text.clear()
        binding.editProductQuantity.text.clear()
        binding.editProductName.requestFocus()
    }

    private fun listarProdutos() {
        lifecycleScope.launch {
            val products = productDao.getAll()
            if (products.isNotEmpty()) {
                products.forEach {
                    Log.i("TAG", "Produto = ${it.name}, Preço = R$${it.price}, Descrição = ${it.description}, Qtd = ${it.quantity}")
                }
            } else {
                Log.i("TAG", "Nenhum produto encontrado.")
            }
            mostrarProdutos(products)
        }
    }

    // A3 + A4: mostra a lista na tela junto com quantidade e soma dos preços
    private fun mostrarProdutos(products: List<Product>) {
        adapter.submitList(products)
        binding.textEmpty.isVisible = products.isEmpty()
        val total = products.sumOf { it.price }
        binding.textTotal.text = "Produtos: ${products.size}   |   Soma dos preços: ${total.toReais()}"
    }

    // A5: busca por nome usando LIKE com "%texto%"
    private fun buscarProdutos() {
        val texto = binding.editSearch.text.toString().trim()
        if (texto.isEmpty()) {
            listarProdutos()
            return
        }
        lifecycleScope.launch {
            val products = productDao.buscarPorNome("%$texto%")
            Log.i("TAG", "Busca por \"$texto\": ${products.size} produto(s)")
            mostrarProdutos(products)
        }
    }

    // A7: tocar em um item preenche o id e o preço atual
    private fun preencherAtualizacao(product: Product) {
        binding.editUpdateId.setText(product.id.toString())
        binding.editUpdatePrice.setText(product.price.toString())
        binding.editUpdatePrice.requestFocus()
        binding.editUpdatePrice.selectAll()
    }

    // A7: atualiza o preço de um produto já cadastrado usando @Update
    private fun atualizarPreco() {
        val id = binding.editUpdateId.text.toString().toIntOrNull()
        val newPrice = binding.editUpdatePrice.text.toString().toDoubleOrNull()

        if (id == null) {
            binding.editUpdateId.error = "Informe o id"
            return
        }
        if (newPrice == null || newPrice <= 0) {
            binding.editUpdatePrice.error = "Informe um preço válido"
            return
        }

        lifecycleScope.launch {
            val product = productDao.getById(id)
            if (product == null) {
                binding.editUpdateId.error = "Produto não encontrado"
                return@launch
            }
            productDao.update(product.copy(price = newPrice))
            Log.i("TAG", "Produto atualizado: #$id ${product.name} - R$${product.price} -> R$$newPrice")
            Toast.makeText(this@MainActivity, "Preço atualizado!", Toast.LENGTH_SHORT).show()
            binding.editUpdateId.text.clear()
            binding.editUpdatePrice.text.clear()
            listarProdutos()
        }
    }

    // A6 (desafio extra): exclui um produto específico com @Delete
    private fun excluirProduto(product: Product) {
        lifecycleScope.launch {
            productDao.delete(product)
            Log.i("TAG", "Produto excluído: #${product.id} ${product.name}")
            Toast.makeText(this@MainActivity, "${product.name} excluído", Toast.LENGTH_SHORT).show()
            listarProdutos()
        }
    }

    // A6: exclui todos os produtos (com confirmação)
    private fun confirmarExcluirTodos() {
        AlertDialog.Builder(this)
            .setTitle("Excluir todos")
            .setMessage("Deseja apagar todos os produtos cadastrados?")
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch {
                    productDao.deleteAll()
                    Log.i("TAG", "Todos os produtos foram excluídos.")
                    Toast.makeText(this@MainActivity, "Todos os produtos foram excluídos", Toast.LENGTH_SHORT).show()
                    listarProdutos()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
