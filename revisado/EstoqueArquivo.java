import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Controle simples de estoque baseado em arquivos (versão revisada).
 *
 * <p>Arquivos usados:
 * <ul>
 *   <li><b>estoque.csv</b>: estoque inicial, uma linha por produto no formato
 *       {@code codigo;quantidade}.</li>
 *   <li><b>movimentacoes.txt</b>: registro das saídas, uma linha por saída no formato
 *       {@code codigo;quantidade}. O arquivo só recebe linhas novas (nunca é reescrito).</li>
 * </ul>
 *
 * <p>Saldo atual de um produto = quantidade em estoque.csv - soma das saídas registradas.
 *
 * <p>Tratamento de erros (diferente do original, que escondia tudo com catch vazio):
 * <ul>
 *   <li>entrada inválida: {@link IllegalArgumentException};</li>
 *   <li>falha de leitura ou gravação, arquivo ausente ou linha inválida no arquivo:
 *       {@link IOException}, que chega até quem chamou o método.</li>
 * </ul>
 */
public class EstoqueArquivo {

    private final Path arquivoEstoque;
    private final Path arquivoMovimentacoes;
    private String ultimaMovimentacao = "";

    /** Usa os mesmos nomes de arquivo do original, na pasta onde o programa roda. */
    public EstoqueArquivo() {
        this(Path.of("estoque.csv"), Path.of("movimentacoes.txt"));
    }

    /** CORREÇÃO: os caminhos agora são recebidos de fora, em vez de fixos dentro dos métodos. */
    public EstoqueArquivo(Path arquivoEstoque, Path arquivoMovimentacoes) {
        this.arquivoEstoque = Objects.requireNonNull(arquivoEstoque);
        this.arquivoMovimentacoes = Objects.requireNonNull(arquivoMovimentacoes);
    }

    /**
     * Consulta o saldo atual de um produto.
     *
     * @param codigo código do produto (não pode ser nulo, vazio nem ter ';' ou quebra de linha)
     * @return saldo atual; devolve 0 quando o código não consta no estoque.csv
     * @throws IllegalArgumentException se o código for inválido
     * @throws IOException se o estoque.csv não existir, não puder ser lido ou tiver linha inválida
     */
    public synchronized int consultarQuantidade(String codigo) throws IOException {
        validarCodigo(codigo);

        int inicial = somarQuantidade(arquivoEstoque, codigo);
        // Se ainda não houve nenhuma saída, o arquivo de movimentações pode não existir.
        int saidas = Files.exists(arquivoMovimentacoes)
                ? somarQuantidade(arquivoMovimentacoes, codigo)
                : 0;
        return inicial - saidas;
    }

    /**
     * Registra a saída de uma quantidade de um produto.
     *
     * @param codigo     código do produto
     * @param quantidade quantidade que sai (deve ser maior que zero)
     * @return {@code true} se a saída foi registrada; {@code false} se o saldo é insuficiente
     * @throws IllegalArgumentException se o código ou a quantidade forem inválidos
     * @throws IOException se não for possível ler o estoque ou gravar a movimentação
     */
    public synchronized boolean registrarSaida(String codigo, int quantidade) throws IOException {
        validarCodigo(codigo);
        // CORREÇÃO: o original aceitava quantidade 0 e negativa.
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade da saida deve ser maior que zero.");
        }

        int disponivel = consultarQuantidade(codigo);
        if (quantidade > disponivel) {
            return false; // regra de negócio: saldo insuficiente (não é erro de arquivo)
        }

        // CORREÇÃO: try-with-resources fecha o arquivo (e faz o flush) mesmo se der erro.
        // O return true só acontece depois que a linha foi realmente gravada.
        try (Writer escrita = abrirEscritor(arquivoMovimentacoes)) {
            escrita.write(codigo + ";" + quantidade + System.lineSeparator());
        }

        // CORREÇÃO: só atualiza depois da gravação ter dado certo.
        ultimaMovimentacao = codigo;
        return true;
    }

    /** Devolve o código da última saída registrada com sucesso (vazio se não houve nenhuma). */
    public synchronized String getUltimaMovimentacao() {
        return ultimaMovimentacao;
    }

    // ------------------------------------------------------------------
    // Métodos de apoio
    // ------------------------------------------------------------------

    /**
     * Soma as quantidades de todas as linhas do arquivo cujo código é igual ao informado.
     * Linhas em branco e linhas de outros produtos (por exemplo um cabeçalho) são ignoradas.
     */
    private int somarQuantidade(Path arquivo, String codigo) throws IOException {
        int total = 0;
        // CORREÇÃO: o leitor é fechado automaticamente (no original nunca era fechado).
        try (BufferedReader leitor = abrirLeitor(arquivo)) {
            String linha;
            int numeroLinha = 0;
            while ((linha = leitor.readLine()) != null) {
                numeroLinha++;
                if (linha.isBlank()) {
                    continue;
                }
                String[] colunas = linha.split(";", -1);
                if (!colunas[0].trim().equals(codigo)) {
                    continue;
                }
                // CORREÇÃO: o original lia colunas[1] sem checar e estourava exceção.
                if (colunas.length != 2) {
                    throw new IOException("Linha " + numeroLinha + " de " + arquivo.getFileName()
                            + " invalida: esperado codigo;quantidade");
                }
                try {
                    total += Integer.parseInt(colunas[1].trim());
                } catch (NumberFormatException e) {
                    throw new IOException("Linha " + numeroLinha + " de " + arquivo.getFileName()
                            + " tem quantidade que nao e numero", e);
                }
            }
        }
        return total;
    }

    /** Impede código nulo/vazio e caracteres que corromperiam o arquivo (';' e quebra de linha). */
    private static void validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("O codigo do produto nao pode ser vazio.");
        }
        if (codigo.indexOf(';') >= 0 || codigo.indexOf('\n') >= 0 || codigo.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("O codigo nao pode ter ';' nem quebra de linha.");
        }
    }

    /**
     * Abre o arquivo para leitura (UTF-8). Existe como método separado para que os testes
     * possam trocar por um leitor que falha (test double), simulando um erro de leitura.
     */
    protected BufferedReader abrirLeitor(Path arquivo) throws IOException {
        return Files.newBufferedReader(arquivo, StandardCharsets.UTF_8);
    }

    /**
     * Abre o arquivo para acrescentar linhas no final (UTF-8). Também pode ser trocado nos
     * testes para simular uma falha de gravação.
     */
    protected Writer abrirEscritor(Path arquivo) throws IOException {
        return Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}