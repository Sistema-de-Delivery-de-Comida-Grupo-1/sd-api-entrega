package br.ifg.urutai.sdapientrega.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.ifg.urutai.sdapientrega.consumer.PedidoEntregaConsumer;
import br.ifg.urutai.sdapientrega.dto.PedidoEntregaResponseDTO;
import br.ifg.urutai.sdapientrega.entity.PedidoEntrega;
import br.ifg.urutai.sdapientrega.repository.PedidoEntregaRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço responsável por gerenciar o cache de pedidos em entrega.
 *
 * Mantém um armazenamento em memória dos pedidos que estão sendo entregues,
 * permitindo rápido acesso e consultas sem necessidade de consultar o banco de
 * dados para cada requisição.
 *
 * Thread-safe: utiliza ConcurrentHashMap para permitir acesso de múltiplas
 * threads.
 */
@Slf4j
@Service
public class PedidoEmEntregaCacheService {

    @Autowired
    private PedidoEntregaRepository pedidoRepository;

    private Logger log = LoggerFactory.getLogger(PedidoEntregaConsumer.class);

    // Cache thread-safe de pedidos em entrega: chave = ID do pedido
    private final Map<Long, PedidoEntrega> pedidosEmEntregaCache = new ConcurrentHashMap<>();

    /**
     * Adiciona um pedido ao cache de entrega quando ele sai para entrega.
     *
     * @param pedido entidade do pedido
     */
    public void adicionarPedidoEmEntrega(PedidoEntrega pedido) {
        if (pedido != null && pedido.getId() != null) {
            pedidosEmEntregaCache.put(pedido.getId(), pedido);
            log.info("Pedido adicionado ao cache de entrega. ID: {} - idPedido: {}",
                    pedido.getId(), pedido.getIdPedido());
        }
    }

    /**
     * Remove um pedido do cache quando ele é entregue ou cancelado.
     *
     * @param pedidoId ID do pedido
     */
    public void removerPedidoDosEntregas(Long pedidoId) {
        if (pedidosEmEntregaCache.containsKey(pedidoId)) {
            PedidoEntrega pedido = pedidosEmEntregaCache.remove(pedidoId);
            log.info("Pedido removido do cache de entrega. ID: {} - idPedido: {}",
                    pedidoId, pedido.getIdPedido());
        }
    }

    /**
     * Atualiza um pedido no cache com informações mais recentes.
     *
     * @param pedido entidade atualizada do pedido
     */
    public void atualizarPedidoEmEntrega(PedidoEntrega pedido) {
        if (pedidosEmEntregaCache.containsKey(pedido.getId())) {
            pedidosEmEntregaCache.put(pedido.getId(), pedido);
            log.info("Pedido atualizado no cache de entrega. ID: {} - idPedido: {}",
                    pedido.getId(), pedido.getIdPedido());
        }
    }

    /**
     * Retorna todos os pedidos que estão em entrega (do cache).
     *
     * @return lista de pedidos em entrega
     */
    public List<PedidoEntrega> obterTodosPedidosEmEntrega() {
        return Collections.unmodifiableList(
                pedidosEmEntregaCache.values().stream().collect(Collectors.toList())
        );
    }

    /**
     * Retorna todos os pedidos em entrega como DTOs de resposta.
     *
     * @return lista de DTOs de pedidos em entrega
     */
    public List<PedidoEntregaResponseDTO> obterTodosPedidosEmEntregaDTO() {
        return obterTodosPedidosEmEntrega().stream()
                .map(this::converterParaResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca um pedido específico no cache de entrega.
     *
     * @param pedidoId ID do pedido
     * @return PedidoEntrega se encontrado no cache, null caso contrário
     */
    public PedidoEntrega obterPedidoEmEntrega(Long pedidoId) {
        return pedidosEmEntregaCache.get(pedidoId);
    }

    /**
     * Verifica se um pedido está no cache de entrega.
     *
     * @param pedidoId ID do pedido
     * @return true se o pedido está em entrega, false caso contrário
     */
    public boolean estaPedidoEmEntrega(Long pedidoId) {
        return pedidosEmEntregaCache.containsKey(pedidoId);
    }

    /**
     * Retorna a quantidade de pedidos em entrega.
     *
     * @return número de pedidos no cache de entrega
     */
    public int obterQuantidadePedidosEmEntrega() {
        return pedidosEmEntregaCache.size();
    }

    /**
     * Sincroniza o cache com o banco de dados. Busca todos os pedidos que estão
     * em status de entrega e adiciona ao cache.
     */
    public void sincronizarCacheComBanco() {
        log.info("Sincronizando cache de pedidos em entrega com banco de dados...");

        // Limpar cache existente
        pedidosEmEntregaCache.clear();

        // Buscar do banco todos os pedidos que estão em entrega
        List<PedidoEntrega> pedidosEmEntrega = pedidoRepository.findPedidosEmEntrega();

        // Adicionar ao cache
        pedidosEmEntrega.forEach(pedido
                -> pedidosEmEntregaCache.put(pedido.getId(), pedido)
        );

        log.info("Cache sincronizado com sucesso. Total de pedidos em entrega: {}",
                pedidosEmEntregaCache.size());
    }

    /**
     * Limpa todo o cache de pedidos em entrega.
     */
    public void limparCache() {
        pedidosEmEntregaCache.clear();
        log.info("Cache de pedidos em entrega foi limpo");
    }

    /**
     * Converte uma entidade PedidoEntrega para um DTO de resposta.
     *
     * @param pedido entidade a ser convertida
     * @return DTO com dados do pedido
     */
    private PedidoEntregaResponseDTO converterParaResponseDTO(PedidoEntrega pedido) {
        return new PedidoEntregaResponseDTO(
                pedido.getId(),
                pedido.getIdPedido(),
                pedido.getIdCliente(),
                pedido.getValorTotal(),
                pedido.getStatus(),
                pedido.getDataCriacao(),
                pedido.getDataAtualizacao()
        );
    }
}
