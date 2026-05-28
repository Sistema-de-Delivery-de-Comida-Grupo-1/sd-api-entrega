package br.ifg.urutai.sdapientrega.repository;

import br.ifg.urutai.sdapientrega.entity.PedidoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório para a entidade PedidoEntrega.
 * Responsável por operações de persistência no banco de dados.
 */
@Repository
public interface PedidoEntregaRepository extends JpaRepository<PedidoEntrega, Long> {

    /**
     * Busca um pedido pelo número do pedido.
     * 
     * @param numeroPedido número único do pedido
     * @return Optional contendo o pedido se encontrado
     */
    Optional<PedidoEntrega> findByNumeroPedido(String numeroPedido);
}
