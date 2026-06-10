# SD-API-ENTREGA

## 1. Visão Geral

O **sd-api-entrega** é um microserviço responsável pelo gerenciamento do ciclo de vida das entregas dos pedidos realizados no sistema distribuído.

Este serviço recebe eventos provenientes do serviço de pedidos, registra novas entregas, atualiza o status dos pedidos em entrega, mantém um cache de entregas em andamento e publica eventos para outros serviços através do RabbitMQ.

### Principais Responsabilidades

* Registrar pedidos prontos para entrega.
* Gerenciar o fluxo de status da entrega.
* Disponibilizar consultas REST sobre entregas.
* Publicar notificações e eventos.
* Manter cache de pedidos em entrega.
* Integrar-se ao Service Discovery Eureka.

---

# 2. Arquitetura do Sistema

O microserviço participa de uma arquitetura baseada em microsserviços composta pelos seguintes componentes:

* sd-api-pedido
* sd-api-entrega
* sd-api-notificacao
* RabbitMQ
* Eureka Server

Fluxo simplificado:

1. O serviço de pedidos publica um evento quando um pedido fica pronto para entrega.
2. O sd-api-entrega consome o evento.
3. O pedido é registrado no banco H2.
4. O status da entrega é atualizado conforme o andamento da entrega.
5. Eventos são publicados para o serviço de notificação.
6. O cliente consulta o estado da entrega através dos endpoints REST.

---

# 3. Modelo de Dados

## Entidade PedidoEntrega

Representa uma entrega registrada no sistema.

| Campo           | Tipo          |
| --------------- | ------------- |
| id              | Long          |
| idPedido        | Long          |
| idCliente       | Long          |
| valorTotal      | double        |
| status          | PedidoStatus  |
| dataCriacao     | LocalDateTime |
| dataAtualizacao | LocalDateTime |

---

# 4. DTOs

## AtualizarStatusDTO

Utilizado para atualizar o status de uma entrega.

```json
{
  "status": "SAIU_PARA_ENTREGA"
}
```

---

## ItemPedidoDTO

Representa um item recebido do serviço de pedidos.

```json
{
  "id": 1,
  "quantidade": 2,
  "preco": 19.90
}
```

---

## PedidoEntregaRequestDTO

DTO utilizado para criação de entregas.

```json
{
  "id": 101,
  "idCliente": 1,
  "valorTotal": 45.00,
  "status": "PRONTO_PARA_ENTREGA"
}
```

---

## PedidoEntregaResponseDTO

DTO retornado pelas operações REST.

```json
{
  "id": 1,
  "idPedido": 101,
  "idCliente": 1,
  "valorTotal": 45.00,
  "status": "RECEBIDO"
}
```

---

## NotificacaoDTO

Mensagem enviada via RabbitMQ.

```json
{
  "idPedido": 101,
  "idCliente": 1,
  "mensagem": "Seu pedido saiu para entrega.",
  "tipoEvento": "SAIU_PARA_ENTREGA"
}
```

---

## PedidoEventDTO

Evento publicado para outros microsserviços.

```json
{
  "id": 101,
  "idCliente": 1,
  "status": "ENTREGUE",
  "valorTotal": 45.00
}
```

---

# 5. Camada de Serviço

## PedidoEntregaService

Responsável pelas regras de negócio:

* Criar pedidos de entrega.
* Atualizar status.
* Confirmar recebimento.
* Publicar eventos RabbitMQ.
* Gerenciar cache de entregas.

### Regras de Negócio

#### SAIU_PARA_ENTREGA

* Adiciona ao cache.
* Publica evento para notificação.
* Envia mensagem RabbitMQ.

#### ENTREGUE

* Atualiza cache.
* Publica evento para notificação.

#### CONFIRMADO_PELO_CLIENTE

* Remove do cache.
* Finaliza o fluxo da entrega.

---

## PedidoEmEntregaCacheService

Responsável por manter um cache em memória utilizando ConcurrentHashMap.

Funcionalidades:

* Adicionar pedidos em entrega.
* Remover pedidos entregues.
* Consultar pedidos em andamento.
* Sincronizar cache com banco de dados.

---

# 6. Persistência

## Banco de Dados

Banco utilizado:

* H2 Database

Configuração:

```properties
spring.datasource.url=jdbc:h2:mem:deliverydb
```

A entidade PedidoEntrega é persistida através do repositório:

```java
PedidoEntregaRepository
```

Operações disponíveis:

* findByIdPedido()
* findByStatus()
* findPedidosEmEntrega()

---

# 7. Endpoints REST

Base URL:

```http
http://localhost:8087/api
```

## Criar Pedido

```http
POST /entregas
```

---

## Buscar Pedido

```http
GET /entregas/{id}
```

---

## Listar Todos

```http
GET /entregas
```

---

## Atualizar Status

```http
PUT /entregas/{id}/status
```

Exemplo:

```json
{
  "status": "ENTREGUE"
}
```

---

## Confirmar Recebimento

```http
PUT /entregas/{id}/confirmar
```

---

## Pedidos em Entrega

```http
GET /entregas/em-entrega/lista
```

---

## Estatísticas de Entrega

```http
GET /entregas/em-entrega/estatisticas
```

---

## Verificar Pedido em Entrega

```http
GET /entregas/{id}/em-entrega
```

---

## Sincronizar Cache

```http
POST /entregas/em-entrega/sincronizar
```

---

# 8. Comunicação entre Microsserviços

## Comunicação Assíncrona

O sd-api-entrega utiliza RabbitMQ para receber e publicar eventos.

### Consumidor

Classe:

```java
PedidoEntregaConsumer
```

Fila consumida:

```text
entrega.pedido.novo.queue
```

Exchange:

```text
event-notificacao
```

Evento processado:

```text
PRONTO_PARA_ENTREGA
```

Ao receber esse evento, o sistema cria automaticamente uma nova entrega.

---

## Produtor

Classe:

```java
NotificacaoProducer
```

Responsável por publicar eventos para:

* sd-api-notificacao
* outros consumidores do ecossistema

---

# 9. Mensageria e Eventos (RabbitMQ)

## Exchange Compartilhado

```text
event-notificacao
```

Utilizado para integração entre:

* sd-api-pedido
* sd-api-entrega
* sd-api-notificacao

---

## Exchange Interno

```text
entrega.topic.exchange
```

Utilizado para eventos internos do domínio de entrega.

---

## Filas

### entrega.notificacao.queue

Recebe:

```text
entrega.notificacao.*
```

Eventos:

* SAIU_PARA_ENTREGA
* ENTREGUE

---

### entrega.status.queue

Recebe:

```text
entrega.status.*
```

Eventos genéricos de alteração de status.

---

### entrega.pedido.novo.queue

Recebe eventos provenientes do:

```text
event-notificacao
```

---

## Publish / Subscribe

O projeto utiliza o padrão Publish/Subscribe através do RabbitMQ.

Exemplo:

1. sd-api-pedido publica um evento.
2. RabbitMQ distribui o evento.
3. sd-api-entrega recebe a mensagem.
4. sd-api-notificacao recebe a mesma mensagem.

Dessa forma existe desacoplamento entre os serviços.

---

# 10. Serviço de Nomes (Service Discovery)

O projeto utiliza o Eureka Server como mecanismo de descoberta de serviços.

Dependência:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

Configuração:

```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

Registro:

```properties
spring.application.name=sd-api-entrega
```

Benefícios:

* Descoberta automática de serviços.
* Balanceamento de carga.
* Eliminação de endereços fixos.
* Maior escalabilidade.

---

# 11. Invocação Remota (RPC)

O sistema distribuído utiliza comunicação remota entre microsserviços.

### Comunicação REST

Os serviços podem realizar chamadas HTTP para consultar ou atualizar informações de outros serviços.

Exemplo:

```http
GET /entregas/{id}
```

---

### Comunicação Orientada a Eventos

Além das chamadas REST, o sistema utiliza RabbitMQ para troca assíncrona de mensagens.

Benefícios:

* Menor acoplamento.
* Maior tolerância a falhas.
* Processamento assíncrono.

---

# 12. Uso de gRPC

O projeto foi estruturado para comunicação entre microsserviços utilizando RPC.

Foi utilizado o protocolo gRPC para comunicação síncrona entre serviços, permitindo:

* Alta performance.
* Serialização eficiente com Protocol Buffers.
* Menor consumo de banda.
* Comunicação fortemente tipada.

O gRPC é utilizado quando existe necessidade de resposta imediata entre serviços, enquanto o RabbitMQ é utilizado para comunicação assíncrona baseada em eventos.

---

# 13. Tecnologias Utilizadas

* Java 17
* Spring Boot 3.4.6
* Spring Data JPA
* RabbitMQ
* Eureka Client
* H2 Database
* Swagger/OpenAPI
* Lombok
* Maven

---

# 14. Execução

## RabbitMQ

```bash
docker run -d --hostname rabbitmq \
-p 5672:5672 \
-p 15672:15672 \
rabbitmq:3-management
```

Painel:

http://localhost:15672

Usuário:

```text
guest
```

Senha:

```text
guest
```

---

## Eureka Server

```text
http://localhost:8761
```

---

## Swagger

```text
http://localhost:8087/api/swagger-ui.html
```

---

# 15. Conclusão

O microserviço sd-api-entrega é responsável pelo gerenciamento completo das entregas dentro do sistema distribuído. Ele utiliza persistência em banco H2, cache em memória, comunicação assíncrona com RabbitMQ e descoberta de serviços através do Eureka, garantindo escalabilidade, desacoplamento e alta disponibilidade.
