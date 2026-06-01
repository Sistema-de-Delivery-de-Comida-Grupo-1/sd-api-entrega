package br.ifg.urutai.sdapientrega.repository;

import br.ifg.urutai.sdapientrega.entity.PedidoEntrega;
import br.ifg.urutai.sdapientrega.enums.PedidoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Busca todos os pedidos que estão em entrega.
     * Retorna pedidos com status SAIU_PARA_ENTREGA.
     *
     * @return lista de pedidos em entrega
     */
    List<PedidoEntrega> findByStatus(PedidoStatus status);

    /**
     * Busca pedidos nos status de entrega (SAIU_PARA_ENTREGA ou ENTREGUE).
     *
     * @return lista de pedidos em processo de entrega
     */
    @Query("SELECT p FROM PedidoEntrega p WHERE p.status IN (br.ifg.urutai.sdapientrega.enums.PedidoStatus.SAIU_PARA_ENTREGA, br.ifg.urutai.sdapientrega.enums.PedidoStatus.ENTREGUE)")
    List<PedidoEntrega> findPedidosEmEntrega();
}
