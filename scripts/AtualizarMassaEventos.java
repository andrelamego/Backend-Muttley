import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/** Atualiza somente a agenda vencida e ainda sem presenças/certificados da massa local. */
public class AtualizarMassaEventos {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private record Agenda(long id, LocalDate data, String inicio, String fim, String status) {}
    private record Mudanca(Agenda antes, Agenda depois) {}

    public static void main(String[] args) throws Exception {
        boolean aplicar = args.length == 1 && "--aplicar".equals(args[0]);
        if (args.length > 0 && !aplicar) throw new IllegalArgumentException("Use sem argumentos para simular ou --aplicar para atualizar.");
        Properties props = new Properties();
        try (var input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            props.load(input);
        }
        String url = props.getProperty("spring.datasource.url");
        if (url == null || !url.matches("jdbc:(mysql|mariadb)://(localhost|127\\.0\\.0\\.1):3306/muttley(\\?.*)?")) {
            throw new IllegalStateException("A atualização é exclusiva do banco local muttley na porta 3306.");
        }
        // Não criar banco por engano; usar somente a base existente.
        url = url.split("\\?", 2)[0];
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        if (url.startsWith("jdbc:mariadb:")) Class.forName("org.mariadb.jdbc.Driver");
        else Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection(url, props.getProperty("spring.datasource.username"), props.getProperty("spring.datasource.password"))) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);
            try {
                List<Agenda> agendas = carregarElegiveis(c, agora, aplicar);
                List<Mudanca> mudancas = planejar(agendas, agora);
                System.out.println("Referência: " + agora.truncatedTo(ChronoUnit.SECONDS) + " America/Sao_Paulo");
                System.out.println("Eventos elegíveis: " + mudancas.size());
                for (Mudanca m : mudancas) {
                    System.out.printf("Evento %d: %s %s -> %s %s-%s %s%n", m.antes.id,
                            m.antes.data, m.antes.status, m.depois.data, m.depois.inicio, m.depois.fim, m.depois.status);
                }
                if (!aplicar || mudancas.isEmpty()) {
                    c.rollback();
                    System.out.println(aplicar ? "Nenhuma alteração necessária." : "Simulação concluída. Nenhum registro alterado.");
                    return;
                }
                Path backup = salvarBackup(mudancas, agora);
                String sql = "update evento set data=?,horario_inicio=?,horario_fim=?,status=? where id_evento=? and data=? and horario_inicio=? and horario_fim=? and status=?";
                try (PreparedStatement update = c.prepareStatement(sql)) {
                    for (Mudanca m : mudancas) {
                        update.setObject(1, m.depois.data);
                        update.setString(2, m.depois.inicio);
                        update.setString(3, m.depois.fim);
                        update.setString(4, m.depois.status);
                        update.setLong(5, m.antes.id);
                        update.setObject(6, m.antes.data);
                        update.setString(7, m.antes.inicio);
                        update.setString(8, m.antes.fim);
                        update.setString(9, m.antes.status);
                        if (update.executeUpdate() != 1) throw new IllegalStateException("Agenda mudou durante a atualização: " + m.antes.id);
                    }
                }
                conferir(c, mudancas);
                c.commit();
                System.out.println("Atualização confirmada: " + mudancas.size() + " eventos.");
                System.out.println("Backup e SQL de reversão: " + backup.toAbsolutePath());
                try {
                    Files.writeString(backup.resolve("aplicado.txt"), "Atualização confirmada: " + mudancas.size() + " eventos.\n", StandardCharsets.UTF_8);
                } catch (IOException erroMarcador) {
                    System.err.println("Banco atualizado e backup preservado; não foi possível gravar o marcador aplicado.txt.");
                }
            } catch (Exception erro) {
                c.rollback();
                throw erro;
            }
        }
    }

    private static List<Agenda> carregarElegiveis(Connection c, LocalDateTime agora, boolean bloquear) throws Exception {
        String sql = """
                select e.id_evento,e.data,e.horario_inicio,e.horario_fim,e.status
                from evento e
                where e.status in ('CRIADO','EM_ANDAMENTO')
                  and timestamp(e.data,e.horario_fim) < ?
                  and not exists (select 1 from participacao p where p.id_evento=e.id_evento and p.presente=1)
                  and not exists (select 1 from certificado c join participacao p on p.id_participacao=c.id_participacao where p.id_evento=e.id_evento)
                order by e.data,e.id_evento
                """ + (bloquear ? " for update" : "");
        List<Agenda> agendas = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setTimestamp(1, Timestamp.valueOf(agora));
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) agendas.add(new Agenda(rs.getLong(1), rs.getObject(2, LocalDate.class), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
        }
        return agendas;
    }

    private static List<Mudanca> planejar(List<Agenda> agendas, LocalDateTime agora) {
        int[] dias = {1, 2, 3, 5, 7, 10, 14, 18, 21, 28, 35};
        List<Mudanca> mudancas = new ArrayList<>();
        for (int i = 0; i < agendas.size(); i++) {
            Agenda antes = agendas.get(i);
            Agenda depois;
            if (agendas.size() >= 3 && i == 0) {
                LocalTime inicio = agora.toLocalTime().isBefore(LocalTime.of(0, 20)) ? LocalTime.MIN : agora.minusMinutes(20).toLocalTime();
                LocalDateTime limite = agora.toLocalDate().atTime(23, 59);
                LocalTime fim = agora.plusMinutes(100).isAfter(limite) ? limite.toLocalTime() : agora.plusMinutes(100).toLocalTime();
                depois = new Agenda(antes.id, agora.toLocalDate(), inicio.format(HORA), fim.format(HORA), "EM_ANDAMENTO");
            } else if (agendas.size() >= 3 && i == 1) {
                LocalDateTime inicio = agora.plusHours(3).truncatedTo(ChronoUnit.MINUTES);
                if (!inicio.plusHours(2).toLocalDate().equals(agora.toLocalDate())) inicio = agora.toLocalDate().plusDays(1).atTime(9, 0);
                depois = new Agenda(antes.id, inicio.toLocalDate(), inicio.toLocalTime().format(HORA), inicio.plusHours(2).toLocalTime().format(HORA), "CRIADO");
            } else {
                int indice = agendas.size() >= 3 ? i - 2 : i;
                int deslocamento = indice < dias.length ? dias[indice] : dias[dias.length - 1] + 7 * (indice - dias.length + 1);
                depois = new Agenda(antes.id, agora.toLocalDate().plusDays(deslocamento), antes.inicio, antes.fim, "CRIADO");
            }
            if (!LocalTime.parse(depois.fim).isAfter(LocalTime.parse(depois.inicio))) throw new IllegalStateException("Horários inválidos no evento " + antes.id);
            mudancas.add(new Mudanca(antes, depois));
        }
        return mudancas;
    }

    private static Path salvarBackup(List<Mudanca> mudancas, LocalDateTime agora) throws Exception {
        Path pasta = Path.of("target", "backups-massa", agora.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + UUID.randomUUID());
        Files.createDirectories(pasta);
        StringBuilder csv = new StringBuilder("id;data_anterior;inicio_anterior;fim_anterior;status_anterior;data_nova;inicio_novo;fim_novo;status_novo\n");
        StringBuilder sql = new StringBuilder("-- Conferir o estado atual antes de executar; reverte apenas agendas ainda iguais às aplicadas e sem novas presenças/certificados.\nUSE muttley;\nSTART TRANSACTION;\n");
        for (Mudanca m : mudancas) {
            csv.append(String.format("%d;%s;%s;%s;%s;%s;%s;%s;%s%n", m.antes.id, m.antes.data, m.antes.inicio, m.antes.fim, m.antes.status, m.depois.data, m.depois.inicio, m.depois.fim, m.depois.status));
            sql.append(String.format("UPDATE evento e SET data='%s',horario_inicio='%s',horario_fim='%s',status='%s' WHERE id_evento=%d AND data='%s' AND horario_inicio='%s' AND horario_fim='%s' AND status='%s' AND NOT EXISTS (SELECT 1 FROM participacao p WHERE p.id_evento=e.id_evento AND p.presente=1) AND NOT EXISTS (SELECT 1 FROM certificado c JOIN participacao p ON p.id_participacao=c.id_participacao WHERE p.id_evento=e.id_evento);%n", m.antes.data, m.antes.inicio, m.antes.fim, m.antes.status, m.antes.id, m.depois.data, m.depois.inicio, m.depois.fim, m.depois.status));
        }
        sql.append("COMMIT;\n");
        Files.writeString(pasta.resolve("agendas.csv"), csv, StandardCharsets.UTF_8);
        Files.writeString(pasta.resolve("restaurar.sql"), sql, StandardCharsets.UTF_8);
        return pasta;
    }

    private static void conferir(Connection c, List<Mudanca> mudancas) throws Exception {
        try (PreparedStatement s = c.prepareStatement("select data,horario_inicio,horario_fim,status from evento where id_evento=?")) {
            for (Mudanca m : mudancas) {
                s.setLong(1, m.depois.id);
                try (ResultSet rs = s.executeQuery()) {
                    if (!rs.next() || !m.depois.data.equals(rs.getObject(1, LocalDate.class)) || !m.depois.inicio.equals(rs.getString(2)) || !m.depois.fim.equals(rs.getString(3)) || !m.depois.status.equals(rs.getString(4))) {
                        throw new IllegalStateException("Falha ao conferir evento " + m.depois.id);
                    }
                }
            }
        }
    }
}
