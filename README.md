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

*(será preenchido na Etapa 2)*

## 5. Cálculo de V(G) por método

*(será preenchido na Etapa 2)*

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
