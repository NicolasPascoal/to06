# AppRoomProduto: Cadastro de Produtos com Room SQLite

**Alunos:** Nicolas Gomes Pascoal | Wendel Miranda
**RA:** 242357 | 242740
**Disciplina:** Programação para Dispositivos Móveis
**Professor(a):** Alessandro

## Sobre o projeto
Aplicativo Android em Kotlin que cadastra, lista, busca, atualiza e exclui produtos usando persistência local com Room (SQLite). Os dados ficam salvos no próprio celular e continuam lá mesmo depois de fechar o app.

## Tecnologias
- Kotlin 1.9.0
- Room 2.6.1 (SQLite) com `kapt`
- ViewBinding
- Corrotinas (`lifecycleScope` + funções `suspend`)
- RecyclerView + `ListAdapter` / `DiffUtil`
- Material Components 3
- Android Gradle Plugin 8.5.1 · Gradle 8.7 · compileSdk 34 · minSdk 24
- Desenvolvido no Android Studio Koala (2024.1.1)

## Estrutura
```
app/src/main/
├── java/br/universidade/approomproduto/
│   ├── model/Product.kt        ← tabela (Entidade)
│   ├── dao/ProductDao.kt       ← comandos SQL (DAO)
│   ├── db/AppDatabase.kt       ← o banco de dados (Singleton)
│   └── view/
│       ├── MainActivity.kt     ← a tela
│       └── ProductAdapter.kt   ← adapter da RecyclerView
├── res/layout/activity_main.xml
├── res/layout/item_product.xml
└── AndroidManifest.xml
```

## Prints do app
| Tela inicial | Lista após fechar e reabrir o app (persistência) |
|---|---|
| <img src="docs/prints/tela-inicial.png" width="260"> | <img src="docs/prints/persistencia.png" width="260"> |

### Logcat com a listagem (filtro `tag:TAG`)
```
I TAG : Produto inserido: Teclado - R$149.9
I TAG : Produto inserido: Mouse - R$59.0
I TAG : Produto inserido: Monitor - R$899.9
I TAG : Produto = Teclado, Preço = R$149.9, Descrição = Teclado USB, Qtd = 10
I TAG : Produto = Mouse, Preço = R$59.0, Descrição = Mouse optico, Qtd = 25
I TAG : Produto = Monitor, Preço = R$899.9, Descrição = Monitor 24 polegadas, Qtd = 5
I TAG : Busca por "mo": 2 produto(s)
I TAG : Produto atualizado: #2 Mouse - R$59.0 -> R$49.9
I TAG : Produto excluído: #3 Monitor
I TAG : Todos os produtos foram excluídos.
I TAG : Nenhum produto encontrado.
```

---

## Parte A: Complemento prático

### A1. Limpar campos após inserir
**O que foi feito:** depois que o `insertAll` termina com sucesso (dentro da corrotina), a função `limparCampos()` apaga o conteúdo dos campos Nome, Preço, Descrição e Quantidade e chama `requestFocus()` no campo Nome, deixando o cursor pronto para o próximo cadastro.

```kotlin
lifecycleScope.launch {
    productDao.insertAll(product)
    Toast.makeText(this@MainActivity, "Produto inserido!", Toast.LENGTH_SHORT).show()
    limparCampos()
    listarProdutos()
}

private fun limparCampos() {
    binding.editProductName.text.clear()
    binding.editProductPrice.text.clear()
    binding.editProductDescription.text.clear()
    binding.editProductQuantity.text.clear()
    binding.editProductName.requestFocus()
}
```
Print (campos limpos, cursor no Nome e o Toast "Produto inserido!"):

<img src="docs/prints/a1.png" width="260">

### A2. Validação de preço
**O que foi feito:** o texto do preço é convertido com `toDoubleOrNull()`. Se o resultado for `null` (campo vazio ou inválido), zero ou negativo, o produto não é salvo e o erro aparece no próprio campo com `editProductPrice.error`. Também validei o nome (obrigatório) e a quantidade (não pode ser negativa).

```kotlin
val price = priceText.toDoubleOrNull()
if (price == null || price <= 0) {
    binding.editProductPrice.error = "Informe um preço válido"
    binding.editProductPrice.requestFocus()
    return
}
```
Print (tentando salvar com preço 0):

<img src="docs/prints/a2.png" width="260">

### A3. Lista na tela (desafio extra: RecyclerView)
**O que foi feito:** em vez de mostrar só no Logcat, adicionei uma `RecyclerView` abaixo dos botões. Cada produto vira um card (`item_product.xml`) com id, nome, preço formatado em reais, quantidade e descrição. O `ProductAdapter` é um `ListAdapter` com `DiffUtil`, então basta chamar `submitList()` com a lista vinda do banco. O Logcat continua sendo usado também.

```kotlin
// MainActivity
adapter = ProductAdapter(
    onItemClick = { product -> preencherAtualizacao(product) },
    onDeleteClick = { product -> excluirProduto(product) }
)
binding.recyclerProducts.layoutManager = LinearLayoutManager(this)
binding.recyclerProducts.adapter = adapter

// ProductAdapter.onBindViewHolder
textName.text = "#${product.id}  ${product.name}"
textDetails.text = "${product.price.toReais()}  ·  Qtd: ${product.quantity}"
textDescription.text = product.description
```

### A4. Total do estoque
**O que foi feito:** junto da lista, um `TextView` mostra quantos produtos estão cadastrados (`products.size`) e a soma dos preços (`products.sumOf { it.price }`), formatada como moeda brasileira. O total é atualizado toda vez que a lista muda (listar, buscar, inserir, atualizar ou excluir).

```kotlin
private fun mostrarProdutos(products: List<Product>) {
    adapter.submitList(products)
    binding.textEmpty.isVisible = products.isEmpty()
    val total = products.sumOf { it.price }
    binding.textTotal.text = "Produtos: ${products.size}   |   Soma dos preços: ${total.toReais()}"
}
```
Print de A3 + A4 (3 produtos, soma R$ 149,90 + R$ 59,00 + R$ 899,90 = R$ 1.108,80):

<img src="docs/prints/a3-a4.png" width="260">

### A5. Buscar por nome
**O que foi feito:** criei a função `buscarPorNome` no DAO com `LIKE` e, na tela, um campo de busca com o botão Buscar. O texto digitado é envolvido por `%` (`"%$texto%"`) para encontrar qualquer nome que **contenha** o trecho. Se o campo estiver vazio, a lista completa é mostrada.

```kotlin
// ProductDao
@Query("SELECT * FROM product WHERE name LIKE :nome")
suspend fun buscarPorNome(nome: String): List<Product>

// MainActivity
lifecycleScope.launch {
    val products = productDao.buscarPorNome("%$texto%")
    mostrarProdutos(products)
}
```
Print (busca por "mo" encontrou Mouse e Monitor; o `LIKE` do SQLite não diferencia maiúsculas/minúsculas):

<img src="docs/prints/a5.png" width="260">

### A6. Excluir produtos (+ desafio extra com `@Delete`)
**O que foi feito:** o DAO ganhou `deleteAll()` com `DELETE FROM product`, ligado ao botão **Excluir Todos**, que antes pede confirmação num `AlertDialog` (para não apagar tudo sem querer). No desafio extra, cada card tem um ícone de lixeira que exclui só aquele produto usando `@Delete` (o Room usa a chave primária `id` do objeto para saber qual linha apagar).

```kotlin
// ProductDao
@Delete
suspend fun delete(product: Product)

@Query("DELETE FROM product")
suspend fun deleteAll()

// MainActivity
private fun excluirProduto(product: Product) {
    lifecycleScope.launch {
        productDao.delete(product)
        listarProdutos()
    }
}
```
| Excluindo só o Monitor (lixeira) | Confirmação | Depois de Excluir Todos |
|---|---|---|
| <img src="docs/prints/a6-item.png" width="220"> | <img src="docs/prints/a6-dialogo.png" width="220"> | <img src="docs/prints/a6.png" width="220"> |

### A7. Atualizar produto
**O que foi feito:** criei a função `update` com `@Update` no DAO. Na tela, o usuário informa o **id** e o **novo preço** (ou simplesmente toca em um produto da lista, que já preenche esses dois campos). O app busca o produto com `getById`, cria uma cópia com o novo preço usando `copy(price = ...)` e chama `update`. O Room encontra a linha pelo `id` e atualiza.

```kotlin
// ProductDao
@Query("SELECT * FROM product WHERE id = :id")
suspend fun getById(id: Int): Product?

@Update
suspend fun update(product: Product): Int

// MainActivity
lifecycleScope.launch {
    val product = productDao.getById(id)
    if (product == null) {
        binding.editUpdateId.error = "Produto não encontrado"
        return@launch
    }
    productDao.update(product.copy(price = newPrice))
    listarProdutos()
}
```
| Antes (Mouse a R$ 59,00, id 2 selecionado) | Depois (Mouse a R$ 49,90) |
|---|---|
| <img src="docs/prints/a7-antes.png" width="260"> | <img src="docs/prints/a7.png" width="260"> |

### A8. Novo campo na tabela (`quantity`) e versão 2 do banco
**O que foi feito:** adicionei `quantity: Int` na entidade `Product`, um campo "Quantidade em estoque" no layout e aumentei a versão do banco de `1` para `2` no `@Database`.

```kotlin
@Entity
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price") val price: Double,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "quantity") val quantity: Int = 0
)

@Database(entities = [Product::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() { ... }
```

**O que aconteceu com os produtos já cadastrados:** eles foram **apagados**. Testei assim: deixei no emulador um banco na versão 1 (tabela `product` sem a coluna `quantity`) com dois produtos e depois abri o app na versão 2:

```
-- banco antes (versão 1)
user_version = 1
id|name|price|description
1|Teclado|149.9|Teclado USB
2|Mouse|59.0|Mouse optico

-- banco depois de abrir o app versão 2
user_version = 2
colunas: id, name, price, description, quantity
total de produtos = 0
```

**Por quê:** quando o Room abre o banco, ele compara a versão gravada no arquivo (1) com a versão declarada no código (2). Como a estrutura mudou, ele precisa de uma *Migration* que diga como sair da versão 1 e chegar à 2 (por exemplo, `ALTER TABLE product ADD COLUMN quantity INTEGER NOT NULL DEFAULT 0`). Eu não escrevi essa migração, e o banco foi criado com `.fallbackToDestructiveMigration()`. Com essa opção, quando falta a migração, o Room **apaga todas as tabelas e cria tudo de novo** com a estrutura nova, então os dados antigos se perdem. Sem o `fallbackToDestructiveMigration()`, o app fecharia com um erro do tipo `IllegalStateException: A migration from 1 to 2 was required but not found`.

| Campo Quantidade no formulário | Lista vazia depois da migração destrutiva |
|---|---|
| <img src="docs/prints/a8-campo.png" width="260"> | <img src="docs/prints/a8-migracao.png" width="260"> |

---

## Parte B: Questões dissertativas

### B1. O que é persistência de dados? O que aconteceria se os produtos fossem guardados apenas em uma `MutableList` dentro da `MainActivity`?
**Resposta:** Persistência é guardar os dados de forma que eles continuem existindo depois que o app é fechado ou o celular é reiniciado. No nosso projeto o Room grava os produtos em um arquivo SQLite dentro do aparelho. Uma `MutableList` fica só na memória RAM, então os produtos sumiriam ao fechar o app, quando o Android encerra o processo ou até ao girar a tela. Testamos isso: fechamos o app à força, abrimos de novo e os produtos continuavam lá.

### B2. Qual a diferença entre usar o SQLite diretamente (`SQLiteOpenHelper` + SQL manual) e usar o Room? Cite pelo menos duas vantagens do Room.
**Resposta:** Com o `SQLiteOpenHelper` escrevemos todo o SQL à mão e convertemos o `Cursor` em objetos coluna por coluna. O Room usa o mesmo SQLite, mas gera esse código a partir de anotações. Vantagens: (1) o SQL é verificado na compilação, então um erro como `FROM produto` quebra o build e não o app; (2) as consultas já devolvem objetos, como a `List<Product>` do `getAll()`; (3) `@Insert`, `@Update` e `@Delete` dispensam SQL; (4) tem suporte a corrotinas e a migrações.

### B3. Explique o papel de cada uma das três peças do Room: Entity, DAO e Database. Como elas se relacionam no seu projeto?
**Resposta:** A **Entity** (`Product.kt`) define a tabela: cada atributo vira uma coluna e o `id` é a chave primária. O **DAO** (`ProductDao.kt`) é a interface com os comandos do banco (`getAll`, `insertAll`, `update`, `delete`, `buscarPorNome`...), e o Room gera a implementação. O **Database** (`AppDatabase.kt`) junta as entidades, define a versão e fornece o DAO. Na prática, a `MainActivity` pega o `AppDatabase`, obtém o `ProductDao` e chama seus métodos passando objetos `Product`, sem escrever SQL na tela.

### B4. Por que o Android não permite acessar o banco na thread principal? O que é um ANR? Como `suspend` e `lifecycleScope.launch` resolvem isso?
**Resposta:** A thread principal desenha a tela e responde aos toques. Acessar o banco envolve leitura em disco e pode demorar, então a tela travaria. O **ANR** (Application Not Responding) é o aviso que o Android mostra quando o app fica cerca de 5 segundos sem responder. No nosso projeto, as funções do DAO são `suspend`, e o Room as executa em segundo plano. Chamamos essas funções dentro de `lifecycleScope.launch`, uma corrotina que espera o banco sem bloquear a tela e é cancelada sozinha se a Activity for fechada.

### B5. O que faz o `fallbackToDestructiveMigration()`? Por que ele é perigoso em um app publicado na Play Store? O que seria uma migração (Migration)?
**Resposta:** Ele faz o Room apagar e recriar o banco quando a versão muda e não existe uma migração definida. Foi o que aconteceu no item A8: ao passar para a versão 2, os produtos sumiram. Em um app publicado, os usuários perderiam todos os dados ao atualizar o app. Uma **Migration** é o código que transforma o banco de uma versão para outra mantendo os dados. No nosso caso seria `ALTER TABLE product ADD COLUMN quantity INTEGER NOT NULL DEFAULT 0`, registrado com `addMigrations(MIGRATION_1_2)`.

### B6. Qual é a vantagem do ViewBinding em relação ao `findViewById`? Que tipo de erro ele ajuda a evitar?
**Resposta:** O ViewBinding gera uma classe para cada layout (`ActivityMainBinding`) com acesso direto e já tipado a cada componente, como `binding.editProductPrice`. Com `findViewById`, é preciso buscar cada view pelo id e informar o tipo manualmente, o que dá mais código. Ele evita o `NullPointerException`, que acontece quando se usa um id que não existe no layout, e o `ClassCastException`, que acontece quando se converte a view para o tipo errado. Esses erros passam a aparecer na compilação, e não com o app rodando.

### B7. Por que em apps maiores é recomendado criar o banco como Singleton? O que poderia acontecer se várias telas criassem cada uma o seu banco?
**Resposta:** Criar o banco é uma operação pesada, então o ideal é existir uma única instância para o app todo. No nosso projeto fizemos isso com `AppDatabase.getInstance()`, usando `synchronized` para garantir que só uma instância seja criada. Se cada tela criasse seu próprio banco, haveria várias conexões abertas no mesmo arquivo, mais uso de memória, risco de conflitos de escrita (`database is locked`) e uma tela poderia não ver as alterações feitas por outra.

### B8. Reflexão pessoal: dificuldades e um app real onde o Room seria útil
**Resposta:** A maior dificuldade foi a compatibilidade de versões: com o Android Studio Koala tivemos que usar AGP 8.5.1, Gradle 8.7, Kotlin 1.9.0 e o JDK 17 do próprio Android Studio. Também corrigimos um erro de tema do Material 3 e entendemos na prática por que os produtos sumiram ao mudar a versão do banco. Um app real em que o Room seria útil é o de controle de estoque de uma loja ou farmácia: o funcionário cadastraria produtos, quantidade e validade mesmo sem internet. Os dados ficariam no celular e seriam sincronizados com um servidor quando houvesse conexão.

---

## Como executar
1. Clone o repositório
2. Abra no Android Studio (testado no Koala 2024.1.1; o Gradle usa o JDK 17 embutido)
3. Execute em um emulador ou celular com Android 7.0 (API 24) ou superior
