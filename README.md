# Atividade Individual – Teste de Caixa Branca e Revisão de Código-Fonte

**Aluno:** Matheus Bau Nardi
**Curso:** Análise e Desenvolvimento de Sistemas – UniFacens
**Disciplina:** UX/UI e Testes de Software

---

## 1. Introdução

Esta atividade consiste em analisar um código Java com problemas, estudar a estrutura interna dele, testar e depois corrigir.

O cenário que recebi foi o **Cenário 2: estoque por arquivo CSV**, com a classe `EstoqueArquivo`. Ela tem dois métodos principais:

- `consultarQuantidade(String codigo)`: lê o arquivo `estoque.csv` e devolve a quantidade do produto.
- `registrarSaida(String codigo, int quantidade)`: verifica se existe saldo e, se existir, registra a saída no arquivo `movimentacoes.txt`.

Também existe o método `getUltimaMovimentacao()`, que só devolve o código da última saída registrada (não precisa de grafo).

### Estrutura do repositório

```
atividade-caixa-branca/
├── original/            código-base sem alterações
├── revisado/            código revisado (Etapa 3)
├── testes/              testes (Etapa 3)
├── diagramas/           grafos de fluxo (Etapa 2)
├── evidencias/          prints e saídas de terminal
├── PLANO_DE_TESTE.xlsx  planilha (aba CAIXA BRANCA (ESTÁTICO))
└── README.md            este relatório
```

---

## 2. Instruções de execução

**Código original:** para compilar, abrir o terminal na pasta `original` e rodar:

```
javac EstoqueArquivo.java
```

A classe não tem método `main`, então ela não executa sozinha. Ela é feita para ser chamada por outra classe ou por testes.

**Versão revisada e testes:** *(será preenchido na Etapa 3)*

---

## 3. Análise estática

A análise completa está na planilha `PLANO_DE_TESTE.xlsx`, na aba **CAIXA BRANCA (ESTÁTICO)**. Foram verificados 19 itens. Destes, 14 estão com problema (2 críticos, 3 altos, 5 médios e 4 baixos).

Os principais problemas que encontrei no código original:

**1. O saldo nunca diminui (crítico) – `registrarSaida`, linhas 28 a 41**
A saída só é gravada em `movimentacoes.txt`, mas `consultarQuantidade` lê apenas `estoque.csv`. Por isso a mesma saída pode ser registrada várias vezes, mesmo acima do saldo.

**2. Gravação sem garantia de que foi salva (crítico) – linhas 32 a 35**

```java
FileWriter escrita = new FileWriter("movimentacoes.txt", true);
escrita.write(codigo + ";" + quantidade + "\n");
ultimaMovimentacao = codigo;
return true;
```

Não existe `close()` nem `flush()`, mas o método retorna `true`. A movimentação pode não ser gravada no arquivo e o sistema avisa que deu certo.

**3. Erros ignorados (alto) – linhas 22 a 24 e 36 a 38**

```java
} catch (IOException e) {
    // Erro de leitura ignorado.
}
```

Se o arquivo não existir, o método devolve 0, igual a um estoque zerado. Se a gravação falhar, devolve `false`, igual a saldo insuficiente. A informação passada não é a que realmente aconteceu.

**4. Arquivos nunca fechados (alto) – linhas 13 e 32**
O `BufferedReader` e o `FileWriter` não são fechados, nem quando acontece `break` ou um erro.

**5. Falta de validação da entrada (alto) – linha 30**
O `if (quantidade <= disponivel)` aceita quantidade 0 e valores negativos. Como um código que não existe retorna 0, a chamada `registrarSaida("XYZ", 0)` retorna `true`.

**6. Leitura do CSV sem cuidado (médio) – linhas 16 a 18**
O código usa `colunas[1]` sem checar se a linha tem duas colunas e converte com `Integer.parseInt` sem tratar valores que não são número. Uma linha mal formada gera erro sem tratamento.

**Outros pontos** (resumidos na planilha): possível problema de acesso ao mesmo tempo (concorrência), código do produto sem filtro (pode bagunçar o arquivo), nomes de arquivo fixos no código, o arquivo `estoque.csv` é lido inteiro a cada consulta, falta de documentação nos métodos e classe com várias responsabilidades.

---

## 4. Grafos de fluxo e convenções

**Convenções usadas:**
- Cada nó é um bloco de comandos em sequência ou uma decisão (`if`, `while`).
- O `return` é o nó de saída. Quando há mais de um `return`, as saídas se juntam em um nó único.
- O `break` foi contado como uma aresta que sai do laço.
- Foi modelado o desvio para o `catch` de `IOException`, porque é a única exceção capturada. Esses desvios aparecem tracejados em vermelho.
- `NumberFormatException` e `ArrayIndexOutOfBoundsException` (linha 18) não foram modeladas, porque o código não as captura.

**Como os dois métodos se relacionam:** o `registrarSaida` chama o `consultarQuantidade` logo no primeiro nó (N1). Quando a consulta falha por erro de leitura, ela devolve 0, e esse 0 entra na decisão do `if` do `registrarSaida`. Por isso um erro de leitura pode acabar virando "saldo insuficiente".

### 4.1 Método `consultarQuantidade`

| Nó | Linhas | Descrição |
|---|---|---|
| N1 | 11 a 14 | `quantidade = 0`, abre o arquivo e declara `linha` |
| N2 | 15 | `while ((linha = leitor.readLine()) != null)` |
| N3 | 16 | `colunas = linha.split(";")` |
| N4 | 17 | `if (colunas[0].equals(codigo))` |
| N5 | 18 e 19 | `parseInt(...)` e `break` |
| N6 | 22 a 24 | `catch (IOException e)` |
| N7 | 25 | `return quantidade` |

**Arestas:** N1→N2, N2→N3, N3→N4, N4→N5, N4→N2, N5→N7 (`break`), N2→N7 (fim do arquivo).
**Arestas excepcionais:** N1→N6 (arquivo não encontrado), N2→N6 (falha em `readLine`), N6→N7.

![Grafo de consultarQuantidade](diagramas/grafo_consultarQuantidade.png)

### 4.2 Método `registrarSaida`

| Nó | Linhas | Descrição |
|---|---|---|
| N1 | 29 | `disponivel = consultarQuantidade(codigo)` |
| N2 | 30 | `if (quantidade <= disponivel)` |
| N3 | 32 e 33 | abre o `FileWriter` e escreve |
| N4 | 34 e 35 | `ultimaMovimentacao = codigo` e `return true` |
| N5 | 36 a 38 | `catch (IOException e)` |
| N6 | 40 | `return false` |
| N7 | - | saída única (une os dois `return`) |

**Arestas:** N1→N2, N2→N3 (saldo suficiente), N2→N6 (saldo insuficiente), N3→N4, N4→N7, N6→N7.
**Arestas excepcionais:** N3→N5 (IOException ao abrir ou escrever), N5→N6.

![Grafo de registrarSaida](diagramas/grafo_registrarSaida.png)

Os arquivos-fonte dos grafos (`.drawio`) estão na pasta `diagramas/`.

## 5. Cálculo de V(G) por método

Fórmula usada: **V(G) = E − N + 2P**, com P = 1, porque cada grafo tem um único componente conectado.

| Método | N | E | P | V(G) = E − N + 2P | Regiões | D + 1 |
|---|---|---|---|---|---|---|
| `consultarQuantidade` | 7 | 10 | 1 | 10 − 7 + 2 = **5** | 5 | 4 + 1 = 5 |
| `registrarSaida` | 7 | 8 | 1 | 8 − 7 + 2 = **3** | 3 | 2 + 1 = 3 |

### 5.1 `consultarQuantidade`

**Verificação por regiões (4 internas + a externa = 5):**
- R1: o laço formado por N2, N3 e N4 (aresta de volta N4→N2).
- R2: entre o caminho N2→N3→N4→N5→N7 e a aresta direta N2→N7 (fim do arquivo).
- R3: entre N2→N7, N2→N6 e N6→N7.
- R4: o triângulo formado por N1, N2 e N6.
- R5: a região externa, fora do grafo.

**Verificação por decisões (D + 1):** são 4 decisões binárias: o `if` (N4), o `while` (N2, que tem 3 saídas e conta como 2 decisões: continuar ou sair, e o desvio de `IOException`) e o desvio de `IOException` em N1. Logo D = 4 e V(G) = 4 + 1 = 5.

Se os desvios excepcionais não fossem modelados, V(G) seria 3 (só o `while` e o `if`).

### 5.2 `registrarSaida`

**Verificação por regiões (2 internas + a externa = 3):**
- R1: formada por N2, N3, N5 e N6.
- R2: formada por N3, N4, N7, N6 e N5.
- R3: a região externa.

**Verificação por decisões (D + 1):** são 2 decisões binárias: o `if` (N2) e o desvio de `IOException` em N3. Logo D = 2 e V(G) = 2 + 1 = 3.

Se o desvio excepcional não fosse modelado, V(G) seria 2.

### Convenções aplicadas
- O código não tem `switch`, então essa convenção não se aplica.
- Cada desvio para o `catch` foi contado como uma decisão adicional.
- O `break` e os `return` foram contados na topologia (N5→N7, N4→N7 e N6→N7).
- Os valores foram calculados sobre o código original, antes das correções.

## 6. Caminhos básicos

*(será preenchido na Etapa 2)*

## 7. Matriz de casos de teste e resultados

*(será preenchido nas Etapas 2 e 3)*

## 8. Correções implementadas e comparação antes/depois

*(será preenchido na Etapa 3)*

## 9. Limitações

*(será preenchido ao final)*

## 10. Conclusão e referências

*(a conclusão será preenchida ao final)*

**Referências**

- Slides da aula 10 – UX/UI e Testes de Software (UniFacens).
- PRESSMAN, Roger S.; MAXIM, Bruce R. *Engenharia de Software: Uma Abordagem Profissional*. 8. ed. Porto Alegre: AMGH Editora, 2016.
