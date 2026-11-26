import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Programa de testes do codigo ORIGINAL (EstoqueArquivo).
 *
 * Como funciona: cada caso de teste roda em um processo Java separado, dentro de uma
 * pasta temporaria nova. Assim um caso nao atrapalha o outro (o codigo original deixa
 * arquivos abertos) e os arquivos estoque.csv e movimentacoes.txt sao criados do zero.
 *
 * Executar a partir da raiz do projeto (veja o README, secao 2).
 * Resultado: tabela no terminal e arquivo evidencias/saida_testes_original.txt
 */
public class TestesOriginal {

    // Arquivo padrao usado nos testes (secao 7 do README)
    static final String ESTOQUE_PADRAO = "A001;10\nB002;0\nC003;25\n";

    // Lista dos casos: id, metodo, descricao curta
    static final String[][] CASOS = {
        {"CT01", "consultarQuantidade", "arquivo vazio"},
        {"CT02", "consultarQuantidade", "codigo na 1a linha"},
        {"CT03", "consultarQuantidade", "codigo na 3a linha (varias iteracoes)"},
        {"CT04", "consultarQuantidade", "codigo inexistente"},
        {"CT05", "consultarQuantidade", "estoque.csv nao existe"},
        {"CT06", "consultarQuantidade", "IOException no meio da leitura"},
        {"CT07", "consultarQuantidade", "linha sem ponto e virgula"},
        {"CT08", "consultarQuantidade", "quantidade nao numerica"},
        {"CT09", "consultarQuantidade", "codigo nulo"},
        {"CT10", "registrarSaida", "saida de 5 (saldo 10)"},
        {"CT11", "registrarSaida", "saida de 10 (igual ao saldo)"},
        {"CT12", "registrarSaida", "saida de 11 (maior que o saldo)"},
        {"CT13", "registrarSaida", "falha de gravacao (IOException)"},
        {"CT14", "registrarSaida", "quantidade 0"},
        {"CT15", "registrarSaida", "quantidade negativa"},
        {"CT16", "registrarSaida", "codigo inexistente com quantidade 0"},
        {"CT17", "registrarSaida", "duas saidas de 10 (saldo 10)"},
        {"CT18", "registrarSaida", "estoque.csv inexistente com quantidade 5"},
    };

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("--caso")) {
            executarCaso(args[1]);   // modo filho: roda um caso so
        } else {
            executarTodos();         // modo principal: roda todos
        }
    }

    // ------------------------------------------------------------------
    // MODO PRINCIPAL: abre um processo para cada caso e monta a tabela
    // ------------------------------------------------------------------
    static void executarTodos() throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = classpathAbsoluto();

        List<String> linhas = new ArrayList<>();
        linhas.add("TESTES DO CODIGO ORIGINAL - EstoqueArquivo");
        linhas.add("Executado em: " + LocalDateTime.now());
        linhas.add("Java: " + System.getProperty("java.version"));
        linhas.add("");
        linhas.add(String.format("%-5s %-20s %-14s %-42s %s", "ID", "METODO", "SITUACAO", "CASO", "OBSERVADO"));
        linhas.add("-".repeat(130));

        int passou = 0, falhou = 0, naoExecutado = 0;
        for (String[] caso : CASOS) {
            Path pasta = Files.createTempDirectory("caixabranca_" + caso[0] + "_");
            String observado = "(sem resultado)";
            String situacao = "ERRO";
            try {
                ProcessBuilder pb = new ProcessBuilder(java, "-cp", classpath, "TestesOriginal", "--caso", caso[0]);
                pb.directory(pasta.toFile());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = r.readLine()) != null) {
                        if (l.startsWith("OBSERVADO=")) observado = l.substring("OBSERVADO=".length());
                        if (l.startsWith("SITUACAO=")) situacao = l.substring("SITUACAO=".length());
                    }
                }
                p.waitFor();
            } finally {
                apagar(pasta);
            }
            if (situacao.equals("PASSOU")) passou++;
            else if (situacao.equals("FALHOU")) falhou++;
            else naoExecutado++;
            linhas.add(String.format("%-5s %-20s %-14s %-42s %s", caso[0], caso[1], situacao, caso[2], observado));
        }

        linhas.add("");
        linhas.add("RESUMO: " + passou + " passaram, " + falhou + " falharam, " + naoExecutado + " nao executados/erro (total " + CASOS.length + ")");

        for (String l : linhas) System.out.println(l);

        Path saida = Path.of("evidencias", "saida_testes_original.txt");
        Files.createDirectories(saida.getParent());
        Files.write(saida, linhas, StandardCharsets.UTF_8);
        System.out.println();
        System.out.println("Saida gravada em: " + saida.toAbsolutePath());
    }

    // ------------------------------------------------------------------
    // MODO FILHO: roda um caso e imprime OBSERVADO= e SITUACAO=
    // ------------------------------------------------------------------
    static void executarCaso(String id) throws Exception {
        EstoqueArquivo e = new EstoqueArquivo();

        switch (id) {
            case "CT01": {   // P1: arquivo vazio -> 0
                escrever("estoque.csv", "");
                Object r = tentar(() -> e.consultarQuantidade("A001"));
                resultado(r instanceof Integer && (Integer) r == 0, descrever(r));
                break;
            }
            case "CT02": {   // P2: codigo na 1a linha -> 10
                escrever("estoque.csv", ESTOQUE_PADRAO);
                Object r = tentar(() -> e.consultarQuantidade("A001"));
                resultado(r instanceof Integer && (Integer) r == 10, descrever(r));
                break;
            }
            case "CT03": {   // P2 com varias iteracoes: codigo na 3a linha -> 25
                escrever("estoque.csv", ESTOQUE_PADRAO);
                Object r = tentar(() -> e.consultarQuantidade("C003"));
                resultado(r instanceof Integer && (Integer) r == 25, descrever(r));
                break;
            }
            case "CT04": {   // P3: codigo inexistente -> 0
                escrever("estoque.csv", ESTOQUE_PADRAO);
                Object r = tentar(() -> e.consultarQuantidade("Z999"));
                resultado(r instanceof Integer && (Integer) r == 0, descrever(r));
                break;
            }
            case "CT05": {   // P4: arquivo inexistente -> deve informar o erro (excecao), nao devolver 0
                Object r = tentar(() -> e.consultarQuantidade("A001"));
                resultado(r instanceof Throwable, descrever(r));
                break;
            }
            case "CT06": {   // P5: nao da para simular no original (nome do arquivo fixo dentro do metodo)
                System.out.println("OBSERVADO=nao ha como injetar um leitor que falhe no original");
                System.out.println("SITUACAO=NAO EXECUTADO");
                break;
            }
            case "CT07": {   // linha sem ';' -> nao pode estourar ArrayIndexOutOfBoundsException
                escrever("estoque.csv", "A001\n");
                Object r = tentar(() -> e.consultarQuantidade("A001"));
                resultado(!(r instanceof ArrayIndexOutOfBoundsException), descrever(r));
                break;
            }
            case "CT08": {   // quantidade nao numerica -> nao pode estourar NumberFormatException
                escrever("estoque.csv", "A001;abc\n");
                Object r = tentar(() -> e.consultarQuantidade("A001"));
                resultado(!(r instanceof NumberFormatException), descrever(r));
                break;
            }
            case "CT09": {   // codigo nulo -> deve rejeitar com IllegalArgumentException
                escrever("estoque.csv", ESTOQUE_PADRAO);
                Object r = tentar(() -> e.consultarQuantidade(null));
                resultado(r instanceof IllegalArgumentException, descrever(r));
                break;
            }
            case "CT10": {   // saida de 5: true, grava "A001;5" e atualiza ultimaMovimentacao
                escrever("estoque.csv", ESTOQUE_PADRAO);
                Object r = tentar(() -> e.registrarSaida("A001", 5));
                String arq = lerArquivo("movimentacoes.txt");
                boolean ok = Boolean.TRUE.equals(r) && arq.equals("A001;5\\n") && "A001".equals(e.getUltimaMovimentacao());
                resultado(ok, descrever(r) + " movimentacoes.txt=" + arq + " ultima=" + e.getUltimaMovimentacao());
                break;
            }
            case "CT11": {   // saida igual ao saldo: true e grava a linha
                escrever("estoque.csv", ESTOQUE_PADRAO);
                Object r = tentar(() -> e.registrarSaida("A001", 10));
                String arq = lerArquivo("movimentacoes.txt");
                boolean ok = Boolean.TRUE.equals(r) && arq.equals("A001;10\\n") && "A001".equals(e.getUltimaMovimentacao());
                resultado(ok, descrever(r) + " movimentacoes.txt=" + arq + " ultima=" + e.getUltimaMovimentacao());
                break;
            }
            case "CT12": {   // saida maior que o saldo: false e nao grava nada
                escrever("estoque.csv", ESTOQUE_PADRAO);
                Object r = tentar(() -> e.registrarSaida("A001", 11));
                String arq = lerArquivo("movimentacoes.txt");
                boolean ok = Boolean.FALSE.equals(r) && (arq.equals("(nao existe)") || arq.isEmpty());
                resultado(ok, descrever(r) + " movimentacoes.txt=" + arq);
                break;
            }
            case "CT13": {   // movimentacoes.txt vira uma pasta -> gravar falha; deve informar a falha
                escrever("estoque.csv", ESTOQUE_PADRAO);
                Files.createDirectory(Path.of("movimentacoes.txt"));
                Object r = tentar(() -> e.registrarSaida("A001", 5));
                boolean ok = (r instanceof Throwable) && e.getUltimaMovimentacao().isEmpty();
                resultado(ok, descrever(r) + " ultima='" + e.getUltimaMovimentacao() + "'");
                break;
            }
            case "CT14": {   // quantidade 0 -> deve rejeitar
                escrever("estoque.csv", ESTOQUE_PADRAO);
                Object r = tentar(() -> e.registrarSaida("A001", 0));
                resultado(rejeitou(r), descrever(r) + " movimentacoes.txt=" + lerArquivo("movimentacoes.txt"));
                break;
            }
            case "CT15": {   // quantidade negativa -> deve rejeitar
                escrever("estoque.csv", ESTOQUE_PADRAO);
                Object r = tentar(() -> e.registrarSaida("A001", -5));
                resultado(rejeitou(r), descrever(r) + " movimentacoes.txt=" + lerArquivo("movimentacoes.txt"));
                break;
            }
            case "CT16": {   // codigo inexistente com quantidade 0 -> deve rejeitar
                escrever("estoque.csv", ESTOQUE_PADRAO);
                Object r = tentar(() -> e.registrarSaida("Z999", 0));
                resultado(rejeitou(r), descrever(r) + " movimentacoes.txt=" + lerArquivo("movimentacoes.txt"));
                break;
            }
            case "CT17": {   // duas saidas de 10 com saldo 10: 1a true, 2a false
                escrever("estoque.csv", ESTOQUE_PADRAO);
                Object r1 = tentar(() -> e.registrarSaida("A001", 10));
                Object r2 = tentar(() -> e.registrarSaida("A001", 10));
                boolean ok = Boolean.TRUE.equals(r1) && Boolean.FALSE.equals(r2);
                resultado(ok, "1a=" + r1 + " 2a=" + r2 + " estoque.csv=" + lerArquivo("estoque.csv"));
                break;
            }
            case "CT18": {   // estoque.csv inexistente: deve informar o erro de leitura, e nao devolver false como se fosse saldo insuficiente
                Object r = tentar(() -> e.registrarSaida("A001", 5));
                resultado(r instanceof Throwable, descrever(r));
                break;
            }
            default:
                System.out.println("OBSERVADO=caso desconhecido");
                System.out.println("SITUACAO=ERRO");
        }
    }

    // ------------------------------------------------------------------
    // Funcoes de apoio
    // ------------------------------------------------------------------

    // Executa o codigo e devolve o resultado ou a excecao que foi lancada
    static Object tentar(Callable<Object> c) {
        try {
            return c.call();
        } catch (Throwable t) {
            return t;
        }
    }

    // "Rejeitou" = devolveu false ou lancou IllegalArgumentException
    static boolean rejeitou(Object r) {
        return Boolean.FALSE.equals(r) || r instanceof IllegalArgumentException;
    }

    static String descrever(Object r) {
        if (r instanceof Throwable) return "excecao=" + r.getClass().getSimpleName();
        return "retorno=" + r;
    }

    static void resultado(boolean passou, String observado) {
        System.out.println("OBSERVADO=" + observado);
        System.out.println("SITUACAO=" + (passou ? "PASSOU" : "FALHOU"));
    }

    static void escrever(String nome, String texto) throws IOException {
        Files.writeString(Path.of(nome), texto);
    }

    static String lerArquivo(String nome) throws IOException {
        Path p = Path.of(nome);
        if (!Files.exists(p)) return "(nao existe)";
        if (Files.isDirectory(p)) return "(e uma pasta)";
        return Files.readString(p).replace("\n", "\\n");
    }

    static String classpathAbsoluto() {
        StringBuilder cp = new StringBuilder();
        for (String parte : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (cp.length() > 0) cp.append(File.pathSeparator);
            cp.append(Path.of(parte).toAbsolutePath());
        }
        return cp.toString();
    }

    static void apagar(Path pasta) {
        try (var arvore = Files.walk(pasta)) {
            arvore.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignorado) {
            // limpeza da pasta temporaria: se nao der, nao atrapalha o teste
        }
    }
}
