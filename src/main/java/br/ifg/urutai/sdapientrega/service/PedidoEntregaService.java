package br.ifg.urutai.sdapientrega.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ifg.urutai.sdapientrega.dto.NotificacaoDTO;
import br.ifg.urutai.sdapientrega.dto.PedidoEntregaRequestDTO;
import br.ifg.urutai.sdapientrega.dto.PedidoEntregaResponseDTO;
import br.ifg.urutai.sdapientrega.entity.PedidoEntrega;
import br.ifg.urutai.sdapientrega.enums.PedidoStatus;
import br.ifg.urutai.sdapientrega.exception.PedidoDuplicadoException;
import br.ifg.urutai.sdapientrega.exception.PedidoNaoEncontradoException;
import br.ifg.urutai.sdapientrega.producer.NotificacaoProducer;
import br.ifg.urutai.sdapientrega.repository.PedidoEntregaRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsável pela lógica de negócio do domínio de Pedidos de Entrega.
 *
 * Responsabilidades: - CRUD de pedidos - Atualização de status com regras de
 * negócio - Integração com RabbitMQ para notificações - Tratamento de exceções
 * de negócio
 */
@Slf4j
@Service
@Transactional
public class PedidoEntregaService {

    @Autowired
    private PedidoEntregaRepository pedidoRepository;

    @Autowired
    private NotificacaoProducer notificacaoProducer;

    @Autowired
    private PedidoEmEntregaCacheService cacheService;

    /**
     * Inicializa o cache ao carregar a aplicação. Sincroniza com o banco de
     * dados para recuperar pedidos em entrega anteriores.
     */
    @PostConstruct
    public void inicializarCache() {
        log.info("Inicializando cache de pedidos em entrega...");
        cacheService.sincronizarCacheComBanco();
    }

    /**
     * Cria um novo pedido de entrega a partir dos dados recebidos do serviço de
     * Pedidos (sd-api-pedido).
     *
     * @param requestDTO dados do pedido recebidos via RabbitMQ ou REST
     * @return DTO com dados do pedido criado
     * @throws PedidoDuplicadoException se o idPedido já existe
     */
    public PedidoEntregaResponseDTO criarPedido(PedidoEntregaRequestDTO requestDTO) {
        log.info("Criando novo pedido de entrega para idPedido: {}", requestDTO.getId());

        // Validar se o pedido já existe
        if (pedidoRepository.findByIdPedido(requestDTO.getId()).isPresent()) {
            log.warn("Tentativa de criar pedido duplicado. idPedido: {}", requestDTO.getId());
            throw new PedidoDuplicadoException(
                    "Pedido com idPedido " + requestDTO.getId() + " já existe no sistema"
            );
        }

        // Criar nova entidade com os dados recebidos
        PedidoEntrega pedido = new PedidoEntrega(
                requestDTO.getId(),
                requestDTO.getIdCliente(),
                requestDTO.getValorTotal()
        );

        // Salvar no banco
        PedidoEntrega pedidoSalvo = pedidoRepository.save(pedido);
        log.info("Pedido de entrega criado com sucesso. id: {} - idPedido: {} - idCliente: {}",
                pedidoSalvo.getId(), pedidoSalvo.getIdPedido(), pedidoSalvo.getIdCliente());

        return converterParaResponseDTO(pedidoSalvo);
    }

    /**
     * Busca um pedido pelo seu ID interno de entrega.
     *
     * @param id ID interno do registro de entrega
     * @return DTO com dados do pedido
     * @throws PedidoNaoEncontradoException se o pedido não existe
     */
    @Transactional(readOnly = true)
    public PedidoEntregaResponseDTO buscarPorId(Long id) {
        log.info("Buscando pedido com ID: {}", id);

        PedidoEntrega pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(
                "Pedido com ID " + id + " não encontrado"
        ));

        return converterParaResponseDTO(pedido);
    }

    /**
     * Lista todos os pedidos de entrega.
     *
     * @return lista de DTOs com dados dos pedidos
     */
    @Transactional(readOnly = true)
    public List<PedidoEntregaResponseDTO> listarTodos() {
        log.info("Listando todos os pedidos");

        return pedidoRepository.findAll()
                .stream()
                .map(this::converterParaResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Atualiza o status de um pedido.
     *
     * Regras de negócio: - SAIU_PARA_ENTREGA: envia notificação e adiciona ao
     * cache - ENTREGUE: envia notificação e atualiza cache - Outros status:
     * remove do cache se aplicável
     *
     * @param id ID interno do registro de entrega
     * @param novoStatus novo status
     * @return DTO com dados do pedido atualizado
     * @throws PedidoNaoEncontradoException se o pedido não existe
     */
    public PedidoEntregaResponseDTO atualizarStatus(Long id, PedidoStatus novoStatus) {
        log.info("Atualizando status do pedido. ID: {} - Novo Status: {}", id, novoStatus);

        PedidoEntrega pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(
                "Pedido com ID " + id + " não encontrado"
        ));

        PedidoStatus statusAnterior = pedido.getStatus();
        pedido.setStatus(novoStatus);
        PedidoEntrega pedidoAtualizado = pedidoRepository.save(pedido);

        log.info("Status do pedido atualizado. ID: {} - Status Anterior: {} - Novo Status: {}",
                id, statusAnterior, novoStatus);

        // Gerenciar cache de pedidos em entrega
        gerenciarCachePedidosEmEntrega(pedidoAtualizado, statusAnterior);

        // Enviar notificação conforme o novo status
        enviarNotificacoesDoStatus(pedidoAtualizado);

        return converterParaResponseDTO(pedidoAtualizado);
    }

    /**
     * Confirma o recebimento do pedido pelo cliente. Atualiza o status para
     * CONFIRMADO_PELO_CLIENTE.
     *
     * @param id ID interno do registro de entrega
     * @return DTO com dados do pedido atualizado
     * @throws PedidoNaoEncontradoException se o pedido não existe
     */
    public PedidoEntregaResponseDTO confirmarRecebimento(Long id) {
        log.info("Confirmando recebimento do pedido. ID: {}", id);

        PedidoEntrega pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(
                "Pedido com ID " + id + " não encontrado"
        ));

        pedido.setStatus(PedidoStatus.CONFIRMADO_PELO_CLIENTE);
        PedidoEntrega pedidoAtualizado = pedidoRepository.save(pedido);

        log.info("Recebimento confirmado. id: {} - idPedido: {}",
                pedidoAtualizado.getId(), pedidoAtualizado.getIdPedido());

        // Remove do cache quando a entrega é confirmada pelo cliente
        cacheService.removerPedidoDosEntregas(pedidoAtualizado.getId());

        return converterParaResponseDTO(pedidoAtualizado);
    }

    /**
     * Gerencia o cache de pedidos em entrega conforme mudanças de status.
     *
     * @param pedido pedido atualizado
     * @param statusAnterior status anterior do pedido
     */
    private void gerenciarCachePedidosEmEntrega(PedidoEntrega pedido, PedidoStatus statusAnterior) {
        // Se o novo status é SAIU_PARA_ENTREGA, adiciona ao cache
        if (pedido.getStatus() == PedidoStatus.SAIU_PARA_ENTREGA) {
            cacheService.adicionarPedidoEmEntrega(pedido);
            log.info("Pedido adicionado ao cache de entregas em andamento. ID: {}", pedido.getId());
        } // Se estava em entrega e mudou para outro status diferente de ENTREGUE, remove do cache
        else if ((statusAnterior == PedidoStatus.SAIU_PARA_ENTREGA
                || statusAnterior == PedidoStatus.ENTREGUE)
                && pedido.getStatus() != PedidoStatus.ENTREGUE) {
            cacheService.removerPedidoDosEntregas(pedido.getId());
            log.info("Pedido removido do cache de entregas. ID: {}", pedido.getId());
        } // Se o novo status é ENTREGUE, mantém/atualiza no cache até confirmação do cliente
        else if (pedido.getStatus() == PedidoStatus.ENTREGUE) {
            cacheService.adicionarPedidoEmEntrega(pedido);
            log.info("Pedido atualizado no cache com status ENTREGUE. ID: {}", pedido.getId());
        }
    }

    /**
     * Envia notificações conforme o status do pedido.
     *
     * @param pedido entidade do pedido
     */
    private void enviarNotificacoesDoStatus(PedidoEntrega pedido) {
        try {
            switch (pedido.getStatus()) {
                case SAIU_PARA_ENTREGA:
                    NotificacaoDTO notificacaoSaida = NotificacaoDTO.builder()
                            .idPedido(pedido.getIdPedido())
                            .idCliente(pedido.getIdCliente())
                            .mensagem("Seu pedido saiu para entrega.")
                            .tipoEvento("SAIU_PARA_ENTREGA")
                            .timestamp(System.currentTimeMillis())
                            .build();

                    notificacaoProducer.enviarNotificacaoSaidaEntrega(notificacaoSaida);
                    break;

                case ENTREGUE:
                    NotificacaoDTO notificacaoEntregue = NotificacaoDTO.builder()
                            .idPedido(pedido.getIdPedido())
                            .idCliente(pedido.getIdCliente())
                            .mensagem("Seu pedido foi entregue.")
                            .tipoEvento("ENTREGUE")
                            .timestamp(System.currentTimeMillis())
                            .build();

                    notificacaoProducer.enviarNotificacaoEntregue(notificacaoEntregue);
                    break;

                default:
                    NotificacaoDTO notificacaoGenerica = NotificacaoDTO.builder()
                            .idPedido(pedido.getIdPedido())
                            .idCliente(pedido.getIdCliente())
                            .tipoEvento(pedido.getStatus().name())
                            .timestamp(System.currentTimeMillis())
                            .build();

                    notificacaoProducer.enviarAtualizacaoStatus(notificacaoGenerica);
                    break;
            }
        } catch (Exception e) {
            log.error("Erro ao enviar notificação para o pedido idPedido={}. Erro: {}",
                    pedido.getIdPedido(), e.getMessage(), e);
            // Não propagar a exceção para não impedir a atualização do status
        }
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
