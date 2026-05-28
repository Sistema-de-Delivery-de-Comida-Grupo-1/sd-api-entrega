package br.ifg.urutai.sdapientrega.entity;

import br.ifg.urutai.sdapientrega.enums.PedidoStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidade que representa um pedido de entrega no sistema.
 * 
 * Responsável por armazenar informações do pedido como:
 * - Dados do cliente
 * - Endereço de entrega
 * - Número do pedido
 * - Método de pagamento
 * - Status da entrega
 * - Timestamps de criação e atualização
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

    @Column(name = "nome_cliente", nullable = false, length = 150)
    private String nomeCliente;

    @Column(name = "endereco", nullable = false, columnDefinition = "TEXT")
    private String endereco;

    @Column(name = "numero_pedido", nullable = false, unique = true, length = 50)
    private String numeroPedido;

    @Column(name = "metodo_pagamento", nullable = false, length = 50)
    private String metodoPagamento;

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
     * Construtor customizado para criação de pedido com dados iniciais
     */
    public PedidoEntrega(String nomeCliente, String endereco, String numeroPedido, 
                         String metodoPagamento) {
        this.nomeCliente = nomeCliente;
        this.endereco = endereco;
        this.numeroPedido = numeroPedido;
        this.metodoPagamento = metodoPagamento;
        this.status = PedidoStatus.RECEBIDO;
    }
}
