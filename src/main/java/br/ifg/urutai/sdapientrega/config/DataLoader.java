package br.ifg.urutai.sdapientrega.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import br.ifg.urutai.sdapientrega.entity.PedidoEntrega;
import br.ifg.urutai.sdapientrega.enums.PedidoStatus;
import br.ifg.urutai.sdapientrega.repository.PedidoEntregaRepository;
import br.ifg.urutai.sdapientrega.service.PedidoEmEntregaCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Componente responsável por realizar a carga inicial de dados no banco H2
 * ao iniciar a aplicação.
 *
 * Insere pedidos de exemplo com diferentes status e, em seguida,
 * sincroniza o cache em memória com os dados persistidos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final PedidoEntregaRepository pedidoEntregaRepository;
    private final PedidoEmEntregaCacheService pedidoEmEntregaCacheService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========================================");
        log.info("Iniciando carga de dados no banco H2...");
        log.info("========================================");

        carregarPedidos();
        pedidoEmEntregaCacheService.sincronizarCacheComBanco();

        log.info("========================================");
        log.info("Carga de dados finalizada com sucesso.");
        log.info("========================================");
    }

    private void carregarPedidos() {
        List<PedidoEntrega> pedidos = List.of(
                criarPedido(
                        "João Silva",
                        "Rua das Flores, 123 - Centro, Urutaí-GO",
                        "PED-001",
                        "Cartão de Crédito",
                        PedidoStatus.RECEBIDO
                ),
                criarPedido(
                        "Maria Oliveira",
                        "Av. Brasil, 456 - Jardim América, Urutaí-GO",
                        "PED-002",
                        "Pix",
                        PedidoStatus.PREPARANDO_ENTREGA
                ),
                criarPedido(
                        "Carlos Santos",
                        "Rua XV de Novembro, 789 - Vila Nova, Urutaí-GO",
                        "PED-003",
                        "Dinheiro",
                        PedidoStatus.SAIU_PARA_ENTREGA
                ),
                criarPedido(
                        "Ana Costa",
                        "Rua Goiás, 321 - Setor Norte, Urutaí-GO",
                        "PED-004",
                        "Cartão de Débito",
                        PedidoStatus.ENTREGUE
                ),
                criarPedido(
                        "Pedro Souza",
                        "Av. Universitária, 654 - Campus IFG, Urutaí-GO",
                        "PED-005",
                        "Pix",
                        PedidoStatus.CONFIRMADO_PELO_CLIENTE
                )
        );

        pedidoEntregaRepository.saveAll(pedidos);
        log.info("{} pedidos inseridos com sucesso no banco H2.", pedidos.size());
    }

    private PedidoEntrega criarPedido(String nomeCliente, String endereco,
                                      String numeroPedido, String metodoPagamento,
                                      PedidoStatus status) {
        PedidoEntrega pedido = new PedidoEntrega(nomeCliente, endereco, numeroPedido, metodoPagamento);
        pedido.setStatus(status);
        return pedido;
    }
}
