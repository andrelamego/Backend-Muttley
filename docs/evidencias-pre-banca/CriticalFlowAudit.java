import com.fatec.muttley.auth.AuthController;
import com.fatec.muttley.evento.*;
import com.fatec.muttley.evento.enums.StatusEventoEnum;
import com.fatec.muttley.participacao.*;
import com.fatec.muttley.pessoa.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.lang.reflect.*;
import java.time.LocalDate;
import java.util.*;

public class CriticalFlowAudit {
  static void inject(Object target, String field, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(field); f.setAccessible(true); f.set(target, value);
  }
  @SuppressWarnings("unchecked")
  static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
  }
  static Object fallback(Method method) {
    if (method.getReturnType() == boolean.class) return false;
    if (method.getReturnType() == int.class) return 0;
    if (method.getReturnType() == long.class) return 0L;
    if (method.getReturnType() == Optional.class) return Optional.empty();
    return null;
  }
  public static void main(String[] args) throws Exception {
    Pessoa person = new Pessoa(); person.setId(10L); person.setNome("Pessoa fictícia");
    person.setEmail("original@example.invalid"); person.setCpf("529.982.247-25"); person.setRole(Role.ADMIN);
    var encoder = new BCryptPasswordEncoder(); person.setSenha(encoder.encode("senha-original-teste"));
    PessoaRepository people = proxy(PessoaRepository.class, (p,m,a) -> switch(m.getName()) {
      case "findById", "findByCpf" -> Optional.of(person);
      case "findByEmail" -> person.getEmail().equals(a[0]) ? Optional.of(person) : Optional.empty();
      case "existsByEmail" -> person.getEmail().equals(a[0]);
      case "existsByRole" -> person.getRole() == a[0];
      case "save" -> a[0];
      default -> fallback(m);
    });
    PessoaService peopleService = new PessoaService();
    inject(peopleService, "pessoaRepository", people);
    inject(peopleService, "pessoaMapper", new PessoaMapperImpl());
    inject(peopleService, "passwordEncoder", encoder);
    AuthController auth = new AuthController(null); inject(auth, "pessoaService", peopleService);
    var response = auth.cadastrarUsuario(new AtualizacaoPessoa(10L, "Nome alterado", "novo@example.invalid", "(11) 90000-0000", "529.982.247-25", "senha-substituta-teste"));
    System.out.println("Cadastro com ID existente: status=" + response.getStatusCode().value() + ", pessoa existente alterada=" + person.getEmail().equals("novo@example.invalid") + ", senha substituida=" + encoder.matches("senha-substituta-teste", person.getSenha()));

    person.setRole(Role.USER); person.setSenha(null);
    response = auth.completarCadastro(new AtualizacaoPessoa(null, "Nome preenchido", person.getEmail(), "(11) 90000-0000", "529.982.247-25", "senha-nova-teste"));
    System.out.println("Completar cadastro sem token de verificacao: status=" + response.getStatusCode().value() + ", senha definida=" + encoder.matches("senha-nova-teste", person.getSenha()));

    Evento event = new Evento(); event.setId(20L); event.setData(LocalDate.now().plusDays(1)); event.setHorarioInicio("10:00"); event.setStatus(StatusEventoEnum.CRIADO);
    Participacao part = new Participacao(); part.setId(30L); part.setPessoa(person); part.setEvento(event);
    ParticipacaoRepository parts = proxy(ParticipacaoRepository.class, (p,m,a) -> switch(m.getName()) {
      case "existsByEventoIdAndPessoaId" -> event.getStatus() != StatusEventoEnum.CRIADO;
      case "findByEventoIdAndPessoaId", "findById" -> Optional.of(part);
      case "findMaiorNumeroInscricao" -> 5;
      case "save" -> a[0];
      default -> fallback(m);
    });
    EventoService events = new EventoService() { @Override public Optional<Evento> procurarPorId(Long id) { return Optional.of(event); } };
    ParticipacaoService service = new ParticipacaoService();
    inject(service, "pessoaService", peopleService); inject(service, "eventoService", events); inject(service, "participacaoRepository", parts);
    person.setRole(Role.ADMIN);
    service.registrarInscricaoPublica(20L, new InscricaoPublicaRequest(person.getNome(), person.getCpf(), person.getEmail()));
    System.out.println("Administrador apos inscricao publica: role=" + person.getRole());
    event.setStatus(StatusEventoEnum.CANCELADO);
    service.confirmarPresenca(20L, person.getCpf());
    System.out.println("Presenca em evento cancelado: presente=" + part.isPresente());
    System.out.println("Modo: metodos reais com repositories simulados em memoria; sem HTTP, banco ou emails.");
  }
}
