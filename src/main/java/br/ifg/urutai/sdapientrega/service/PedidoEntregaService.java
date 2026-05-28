package br.ifg.urutai.sdapientrega.service;


import br.ifg.urutai.sdapientrega.dto.NotificacaoDTO;
import br.ifg.urutai.sdapientrega.dto.PedidoEntregaRequestDTO;
import br.ifg.urutai.sdapientrega.dto.PedidoEntregaResponseDTO;
import br.ifg.urutai.sdapientrega.entity.PedidoEntrega;
import br.ifg.urutai.sdapientrega.enums.PedidoStatus;
import br.ifg.urutai.sdapientrega.exception.PedidoDuplicadoException;
import br.ifg.urutai.sdapientrega.exception.PedidoNaoEncontradoException;
import br.ifg.urutai.sdapientrega.producer.NotificacaoProducer;
import br.ifg.urutai.sdapientrega.repository.PedidoEntregaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Service responsável pela lógica de negócio do domínio de Pedidos de Entrega.
 * 
 * Responsabilidades:
 * - CRUD de pedidos
 * - Atualização de status com regras de negócio
 * - Integração com RabbitMQ para notificações
 * - Tratamento de exceções de negócio
 */
@Slf4j
@Service
@Transactional
public class PedidoEntregaService {

    @Autowired
    private PedidoEntregaRepository pedidoRepository;

    @Autowired
    private NotificacaoProducer notificacaoProducer;

    /**
     * Cria um novo pedido de entrega.
     * 
     * @param requestDTO dados do pedido a ser criado
     * @return DTO com dados do pedido criado
     * @throws PedidoDuplicadoException se o número do pedido já existe
     */
    public PedidoEntregaResponseDTO criarPedido(PedidoEntregaRequestDTO requestDTO) {
        log.info("Criando novo pedido: {}", requestDTO.getNumeroPedido());

        // Validar se o pedido já existe
        if (pedidoRepository.findByNumeroPedido(requestDTO.getNumeroPedido()).isPresent()) {
            log.warn("Tentativa de criar pedido duplicado: {}", requestDTO.getNumeroPedido());
            throw new PedidoDuplicadoException(
                    "Pedido com número " + requestDTO.getNumeroPedido() + " já existe no sistema"
            );
        }

        // Criar nova entidade
        PedidoEntrega pedido = new PedidoEntrega(
                requestDTO.getNomeCliente(),
                requestDTO.getEndereco(),
                requestDTO.getNumeroPedido(),
                requestDTO.getMetodoPagamento()
        );

        // Salvar no banco
        PedidoEntrega pedidoSalvo = pedidoRepository.save(pedido);
        log.info("Pedido criado com sucesso. ID: {} - Número: {}", pedidoSalvo.getId(), pedidoSalvo.getNumeroPedido());

        return converterParaResponseDTO(pedidoSalvo);
    }

    /**
     * Busca um pedido pelo seu ID.
     * 
     * @param id ID do pedido
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
     * Regras de negócio:
     * - SAIU_PARA_ENTREGA: envia notificação
     * - ENTREGUE: envia notificação
     * 
     * @param id ID do pedido
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

        // Enviar notificação conforme o novo status
        enviarNotificacoesDoStatus(pedidoAtualizado);

        return converterParaResponseDTO(pedidoAtualizado);
    }

    /**
     * Confirma o recebimento do pedido pelo cliente.
     * Atualiza o status para CONFIRMADO_PELO_CLIENTE.
     * 
     * @param id ID do pedido
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

        log.info("Recebimento do pedido confirmado. ID: {} - Número: {}", 
                pedidoAtualizado.getId(), pedidoAtualizado.getNumeroPedido());

        return converterParaResponseDTO(pedidoAtualizado);
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
                            .numeroPedido(pedido.getNumeroPedido())
                            .nomeCliente(pedido.getNomeCliente())
                            .mensagem("Seu pedido saiu para entrega.")
                            .tipoEvento("SAIU_PARA_ENTREGA")
                            .build();
                    
                    notificacaoProducer.enviarNotificacaoSaidaEntrega(notificacaoSaida);
                    break;

                case ENTREGUE:
                    NotificacaoDTO notificacaoEntregue = NotificacaoDTO.builder()
                            .numeroPedido(pedido.getNumeroPedido())
                            .nomeCliente(pedido.getNomeCliente())
                            .mensagem("Seu pedido foi entregue.")
                            .tipoEvento("ENTREGUE")
                            .build();
                    
                    notificacaoProducer.enviarNotificacaoEntregue(notificacaoEntregue);
                    break;

                default:
                    NotificacaoDTO notificacaoGenerica = NotificacaoDTO.builder()
                            .numeroPedido(pedido.getNumeroPedido())
                            .nomeCliente(pedido.getNomeCliente())
                            .tipoEvento(pedido.getStatus().name())
                            .build();
                    
                    notificacaoProducer.enviarAtualizacaoStatus(notificacaoGenerica);
                    break;
            }
        } catch (Exception e) {
            log.error("Erro ao enviar notificação para o pedido {}. Erro: {}", 
                    pedido.getNumeroPedido(), e.getMessage(), e);
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
                pedido.getNomeCliente(),
                pedido.getEndereco(),
                pedido.getNumeroPedido(),
                pedido.getMetodoPagamento(),
                pedido.getStatus(),
                pedido.getDataCriacao(),
                pedido.getDataAtualizacao()
        );
    }
}
