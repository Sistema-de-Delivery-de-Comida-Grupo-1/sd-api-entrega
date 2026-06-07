package br.ifg.urutai.sdapientrega.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import br.ifg.urutai.sdapientrega.enums.PedidoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade que representa um pedido de entrega no sistema.
 *
 * Armazena os dados essenciais recebidos do serviço de Pedidos (sd-api-pedido)
 * necessários para gerenciar o ciclo de vida da entrega: - ID do pedido de
 * origem - ID do cliente - Valor total - Status da entrega (gerenciado por este
 * microserviço) - Timestamps de criação e atualização
 */
@Entity
@Table(name = "pedidos_entrega")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEntrega implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "id_pedido", nullable = false, unique = true)
    private Long idPedido;

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    @Column(name = "valor_total", nullable = false)
    private double valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PedidoStatus status;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    /**
     * Construtor customizado para criação de pedido com dados iniciais. O
     * status é definido como RECEBIDO por padrão.
     *
     * @param idPedido ID do pedido no serviço de origem
     * @param idCliente ID do cliente
     * @param valorTotal valor total do pedido em centavos
     */
    public PedidoEntrega(Long idPedido, Long idCliente, double valorTotal) {
        this.idPedido = idPedido;
        this.idCliente = idCliente;
        this.valorTotal = valorTotal;
        this.status = PedidoStatus.RECEBIDO;
    }
}
