package br.ifg.urutai.sdapientrega.controller;


import br.ifg.urutai.sdapientrega.dto.AtualizarStatusDTO;
import br.ifg.urutai.sdapientrega.dto.PedidoEntregaRequestDTO;
import br.ifg.urutai.sdapientrega.dto.PedidoEntregaResponseDTO;
import br.ifg.urutai.sdapientrega.service.PedidoEntregaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller REST para operações com Pedidos de Entrega.
 * 
 * Endpoints:
 * - POST /entregas - Criar novo pedido
 * - GET /entregas/{id} - Buscar pedido por ID
 * - GET /entregas - Listar todos os pedidos
 * - PUT /entregas/{id}/status - Atualizar status
 * - PUT /entregas/{id}/confirmar - Confirmar recebimento
 */
@Slf4j
@RestController
@RequestMapping("/entregas")
@Tag(name = "Pedidos de Entrega", description = "Operações relacionadas a pedidos de entrega")
public class PedidoEntregaController {

    @Autowired
    private PedidoEntregaService pedidoService;

    /**
     * Cria um novo pedido de entrega.
     * 
     * @param requestDTO dados do pedido
     * @return resposta com dados do pedido criado
     */
    @PostMapping
    @Operation(
            summary = "Criar novo pedido de entrega",
            description = "Cria um novo pedido de entrega no sistema"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoEntregaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou incompletos"),
            @ApiResponse(responseCode = "409", description = "Pedido duplicado - número já existe")
    })
    public ResponseEntity<PedidoEntregaResponseDTO> criarPedido(
            @Valid @RequestBody PedidoEntregaRequestDTO requestDTO) {
        
        log.info("Requisição para criar pedido: {}", requestDTO.getNumeroPedido());
        
        PedidoEntregaResponseDTO pedido = pedidoService.criarPedido(requestDTO);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    /**
     * Busca um pedido pelo seu ID.
     * 
     * @param id ID do pedido
     * @return resposta com dados do pedido
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar pedido por ID",
            description = "Retorna os dados de um pedido específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoEntregaResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<PedidoEntregaResponseDTO> buscarPorId(
            @Parameter(description = "ID do pedido", required = true, example = "1")
            @PathVariable Long id) {
        
        log.info("Requisição para buscar pedido com ID: {}", id);
        
        PedidoEntregaResponseDTO pedido = pedidoService.buscarPorId(id);
        
        return ResponseEntity.ok(pedido);
    }

    /**
     * Lista todos os pedidos de entrega.
     * 
     * @return lista de pedidos
     */
    @GetMapping
    @Operation(
            summary = "Listar todos os pedidos",
            description = "Retorna uma lista de todos os pedidos de entrega cadastrados"
    )
    @ApiResponse(responseCode = "200", description = "Lista de pedidos retornada com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoEntregaResponseDTO.class)))
    public ResponseEntity<List<PedidoEntregaResponseDTO>> listarTodos() {
        
        log.info("Requisição para listar todos os pedidos");
        
        List<PedidoEntregaResponseDTO> pedidos = pedidoService.listarTodos();
        
        return ResponseEntity.ok(pedidos);
    }

    /**
     * Atualiza o status de um pedido.
     * 
     * @param id ID do pedido
     * @param statusDTO novo status
     * @return resposta com dados do pedido atualizado
     */
    @PutMapping("/{id}/status")
    @Operation(
            summary = "Atualizar status do pedido",
            description = "Atualiza o status de um pedido existente. Ao mudar para 'SAIU_PARA_ENTREGA' ou 'ENTREGUE', " +
                    "envia notificação via RabbitMQ"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoEntregaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<PedidoEntregaResponseDTO> atualizarStatus(
            @Parameter(description = "ID do pedido", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody AtualizarStatusDTO statusDTO) {
        
        log.info("Requisição para atualizar status do pedido ID: {} - Novo Status: {}", id, statusDTO.getStatus());
        
        PedidoEntregaResponseDTO pedido = pedidoService.atualizarStatus(id, statusDTO.getStatus());
        
        return ResponseEntity.ok(pedido);
    }

    /**
     * Confirma o recebimento do pedido pelo cliente.
     * Altera o status para CONFIRMADO_PELO_CLIENTE.
     * 
     * @param id ID do pedido
     * @return resposta com mensagem de sucesso
     */
    @PutMapping("/{id}/confirmar")
    @Operation(
            summary = "Confirmar recebimento do pedido",
            description = "Confirma o recebimento do pedido pelo cliente, alterando o status para CONFIRMADO_PELO_CLIENTE"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recebimento confirmado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<Map<String, Object>> confirmarRecebimento(
            @Parameter(description = "ID do pedido", required = true, example = "1")
            @PathVariable Long id) {
        
        log.info("Requisição para confirmar recebimento do pedido ID: {}", id);
        
        PedidoEntregaResponseDTO pedido = pedidoService.confirmarRecebimento(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("mensagem", "Recebimento do pedido confirmado com sucesso");
        response.put("pedido", pedido);
        
        return ResponseEntity.ok(response);
    }
}
