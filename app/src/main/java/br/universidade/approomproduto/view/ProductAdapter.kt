package br.universidade.approomproduto.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.universidade.approomproduto.databinding.ItemProductBinding
import br.universidade.approomproduto.model.Product
import java.text.NumberFormat
import java.util.Locale

// Formata um valor como moeda brasileira (ex.: R$ 149,90)
fun Double.toReais(): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(this)

// A3 (desafio extra): adapter da RecyclerView que mostra a lista de produtos
class ProductAdapter(
    private val onItemClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(DIFF) {

    class ProductViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        with(holder.binding) {
            textName.text = "#${product.id}  ${product.name}"
            textDetails.text = "${product.price.toReais()}  ·  Qtd: ${product.quantity}"
            textDescription.text = product.description
            textDescription.isVisible = !product.description.isNullOrBlank()
            root.setOnClickListener { onItemClick(product) }
            btnDelete.setOnClickListener { onDeleteClick(product) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Product, newItem: Product) = oldItem == newItem
        }
    }
}
