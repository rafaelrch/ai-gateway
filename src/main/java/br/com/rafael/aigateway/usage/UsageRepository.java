package br.com.rafael.aigateway.usage;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Guarda o histórico de uso em memória (sem banco, por decisão do projeto).
 *
 * @Repository: estereótipo do Spring para a camada de dados. Funciona como
 * @Component e, com JPA, também traduziria exceções de banco.
 *
 * É um bean singleton acessado por várias requisições ao mesmo tempo, então
 * a lista precisa ser segura para threads. CopyOnWriteArrayList copia o
 * array a cada escrita: leitura nunca trava e nunca vê lista pela metade.
 * Serve bem para poucas escritas; com muito volume, seria um banco.
 */
@Repository
public class UsageRepository {

    // A lista em si. final: a referência nunca troca, só o conteúdo.
    private final List<UsoRegistradoEvent> registros = new CopyOnWriteArrayList<>();

    // Acrescenta um registro no fim.
    public void salvar(UsoRegistradoEvent evento) {
        registros.add(evento);
    }

    // Devolve uma cópia imutável, para ninguém de fora alterar o histórico.
    public List<UsoRegistradoEvent> listar() {
        return List.copyOf(registros);
    }
}
