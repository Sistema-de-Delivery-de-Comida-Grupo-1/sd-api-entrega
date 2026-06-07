package br.ifg.urutai.sdapientrega.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import br.ifg.urutai.sdapientrega.consumer.PedidoEntregaConsumer;
import br.ifg.urutai.sdapientrega.entity.PedidoEntrega;
import br.ifg.urutai.sdapientrega.enums.PedidoStatus;
import br.ifg.urutai.sdapientrega.repository.PedidoEntregaRepository;
import br.ifg.urutai.sdapientrega.service.PedidoEmEntregaCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Componente responsável por realizar a carga inicial de dados no banco H2 ao
 * iniciar a aplicação.
 *
 * Insere pedidos de exemplo com diferentes status e, em seguida, sincroniza o
 * cache em memória com os dados persistidos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private PedidoEntregaRepository pedidoEntregaRepository;
    private PedidoEmEntregaCacheService pedidoEmEntregaCacheService;

    private Logger log = LoggerFactory.getLogger(PedidoEntregaConsumer.class);

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
                criarPedido(101L, 1L, 4500, PedidoStatus.RECEBIDO),
                criarPedido(102L, 2L, 3200, PedidoStatus.PREPARANDO_ENTREGA),
                criarPedido(103L, 3L, 7800, PedidoStatus.SAIU_PARA_ENTREGA),
                criarPedido(104L, 4L, 2100, PedidoStatus.ENTREGUE),
                criarPedido(105L, 5L, 5600, PedidoStatus.CONFIRMADO_PELO_CLIENTE)
        );

        pedidoEntregaRepository.saveAll(pedidos);
        log.info("{} pedidos inseridos com sucesso no banco H2.", pedidos.size());
    }

    /**
     * Cria um PedidoEntrega com os dados fornecidos.
     *
     * @param idPedido ID do pedido no serviço de origem (sd-api-pedido)
     * @param idCliente ID do cliente
     * @param valorTotal valor total do pedido em centavos
     * @param status status inicial do pedido
     * @return PedidoEntrega configurado
     */
    private PedidoEntrega criarPedido(Long idPedido, Long idCliente, int valorTotal, PedidoStatus status) {
        PedidoEntrega pedido = new PedidoEntrega(idPedido, idCliente, valorTotal);
        pedido.setStatus(status);
        return pedido;
    }
}
