package br.ifg.urutai.sdapientrega.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.ifg.urutai.sdapientrega.entity.PedidoEntrega;
import br.ifg.urutai.sdapientrega.enums.PedidoStatus;

/**
 * Repositório para a entidade PedidoEntrega. Responsável por operações de
 * persistência no banco de dados.
 */
@Repository
public interface PedidoEntregaRepository extends JpaRepository<PedidoEntrega, Long> {

    /**
     * Busca um pedido pelo ID do pedido de origem (sd-api-pedido).
     *
     * @param idPedido ID do pedido no serviço de origem
     * @return Optional contendo o pedido se encontrado
     */
    Optional<PedidoEntrega> findByIdPedido(Long idPedido);

    /**
     * Busca todos os pedidos com um determinado status.
     *
     * @param status status desejado
     * @return lista de pedidos com o status informado
     */
    List<PedidoEntrega> findByStatus(PedidoStatus status);

    /**
     * Busca pedidos nos status de entrega (SAIU_PARA_ENTREGA ou ENTREGUE).
     *
     * @return lista de pedidos em processo de entrega
     */
    @Query("SELECT p FROM PedidoEntrega p WHERE p.status IN ("
            + "br.ifg.urutai.sdapientrega.enums.PedidoStatus.SAIU_PARA_ENTREGA, "
            + "br.ifg.urutai.sdapientrega.enums.PedidoStatus.ENTREGUE)")
    List<PedidoEntrega> findPedidosEmEntrega();
}
