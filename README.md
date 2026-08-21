# ThallesNascimentoEmissor

Emissor (producer) de mensagens de mudança de permissão de usuário, construído com Spring Boot e RabbitMQ. Expõe um endpoint REST que publica mensagens em uma fila consumida pelo projeto irmão [ReceptorPermissionamento](../ReceptorPermissionamento).

## Sumário

- [Sobre o projeto](#sobre-o-projeto)
- [Arquitetura e fluxo de mensageria](#arquitetura-e-fluxo-de-mensageria)
- [Tecnologias utilizadas](#tecnologias-utilizadas)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Pré-requisitos](#pré-requisitos)
- [Configuração](#configuração)
- [Como executar](#como-executar)
  - [Localmente (Maven Wrapper)](#localmente-maven-wrapper)
  - [Com Docker Compose](#com-docker-compose)
- [Endpoint da API](#endpoint-da-api)
- [Documentação interativa (Swagger/OpenAPI)](#documentação-interativa-swaggeropenapi)
- [Topologia RabbitMQ declarada](#topologia-rabbitmq-declarada)
- [Projeto relacionado](#projeto-relacionado)
- [CI/CD (Jenkins)](#cicd-jenkins)
- [Testes](#testes)
- [Pontos de atenção / melhorias futuras](#pontos-de-atenção--melhorias-futuras)

## Sobre o projeto

Projeto de estudo (SENAC) com a descrição "Emissor Mudar Permissão de Usuário". É o lado **Producer** de um sistema de permissionamento baseado em mensageria: recebe uma requisição HTTP com os dados de um usuário e sua permissão, e publica essa informação em uma fila RabbitMQ para ser processada de forma assíncrona por outro serviço.

## Arquitetura e fluxo de mensageria

```
Cliente HTTP
    │  POST /permissionamento/mensagem
    ▼
ThallesNascimentoEmissor (porta 5454)
    │  RabbitTemplate.convertAndSend("banco", "permissoes", Producer)
    ▼
Exchange "banco" (direct)
    │  routing key "permissoes"
    ▼
Fila "permissoes"
    │
    ▼
ReceptorPermissionamento (consumidor externo, outro repositório)
    → persiste os dados em MySQL
```

Este serviço apenas **declara e publica** na fila — o consumo e a persistência ficam a cargo do projeto [ReceptorPermissionamento](../ReceptorPermissionamento).

## Tecnologias utilizadas

- Java 17
- Spring Boot 3.4.0
- Spring Web
- Spring AMQP (RabbitMQ)
- springdoc-openapi 2.7.0 (Swagger UI)
- Maven / Maven Wrapper
- Docker / Docker Compose
- Jenkins (pipeline de CI/CD)

## Estrutura do projeto

```
ThallesNascimentoEmissor/
├── docker-compose.yml
├── dockerfile
├── jenkinsfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/br/com/senac/ThallesNascimentoEmissor/
    │   │   ├── ThallesNascimentoEmissorApplication.java
    │   │   ├── configs/
    │   │   │   └── MQConfig.java
    │   │   ├── controllers/
    │   │   │   └── ProducerController.java
    │   │   └── entities/
    │   │       └── Producer.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/br/com/senac/ThallesNascimentoEmissor/
            └── ThallesNascimentoEmissorApplicationTests.java
```

## Pré-requisitos

- JDK 17
- Maven (ou usar o Maven Wrapper incluso, `./mvnw`)
- Uma instância RabbitMQ acessível
- Docker e Docker Compose (opcional, para subir a aplicação em container)

## Configuração

As configurações ficam em `src/main/resources/application.properties`:

```properties
spring.application.name=ThallesNascimentoEmissor

server.port=5454

spring.rabbitmq.host=10.136.75.59
spring.rabbitmq.port=5672
spring.rabbitmq.username=admin
spring.rabbitmq.password=admin123
```

> **Atenção:** o host do RabbitMQ é um IP fixo de ambiente de estudo e as credenciais estão fixas no arquivo (valores de exemplo, não seguros para produção). Para rodar localmente, ajuste `spring.rabbitmq.host` para `localhost` (ou o host da sua instância) ou externalize esses valores via variáveis de ambiente.

## Como executar

### Localmente (Maven Wrapper)

```bash
./mvnw spring-boot:run
```

A aplicação sobe na porta `5454` e precisa de um RabbitMQ acessível no host configurado.

### Com Docker Compose

```bash
docker-compose up -d --build
```

Isso sobe o container da aplicação (porta `5454`) e um RabbitMQ com plugin de management (`rabbitmq:3-management`), expondo:
- `5672` — protocolo AMQP
- `15672` — painel de administração web

## Endpoint da API

**`POST /permissionamento/mensagem`**

Publica uma mensagem na fila `permissoes`.

Corpo da requisição (JSON):

```json
{
  "nameUser": "joao.silva",
  "permissionUser": "ADMIN"
}
```

Exemplo com `curl`:

```bash
curl -X POST http://localhost:5454/permissionamento/mensagem \
  -H "Content-Type: application/json" \
  -d '{"nameUser": "joao.silva", "permissionUser": "ADMIN"}'
```

Resposta: `200 OK` sem corpo.

## Documentação interativa (Swagger/OpenAPI)

Com a aplicação rodando, a documentação da API fica disponível em:

```
http://localhost:5454/swagger-ui.html
```

## Topologia RabbitMQ declarada

Declarada programaticamente em `MQConfig` (via `AmqpAdmin`, na inicialização):

| Recurso | Nome | Tipo |
|---|---|---|
| Exchange | `banco` | Direct |
| Fila | `permissoes` | Durável |
| Routing key | `permissoes` | — |

## Projeto relacionado

- [ReceptorPermissionamento](../ReceptorPermissionamento) — consome a fila `permissoes` e persiste as permissões recebidas em um banco MySQL.

## CI/CD (Jenkins)

O `jenkinsfile` define um pipeline básico:
1. Checkout do repositório
2. `mvn clean install`
3. Build da imagem Docker da aplicação
4. Parada/remoção de containers antigos
5. `docker-compose up -d --build`

## Testes

O projeto contém apenas o teste de contexto padrão gerado pelo Spring Initializr (`contextLoads`), que verifica se a aplicação sobe sem erros. Não há testes de integração para o fluxo de publicação de mensagens.

## Pontos de atenção / melhorias futuras

- Credenciais do RabbitMQ hardcoded em `application.properties` — recomenda-se externalizar via variáveis de ambiente/secrets.
- Ausência de tratamento de erros no controller/publicação (falhas de conexão com o broker não são tratadas).
- Ausência de testes de integração, apesar da dependência `spring-rabbit-test` já estar disponível no classpath de testes.
