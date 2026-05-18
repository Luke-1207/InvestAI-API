````md id="5v2qai"
<div align="center">

# 💸 InvestAI API

### API REST Inteligente para Plataforma de Investimentos

<img src="./docs/assets/banner.png" alt="InvestAI API Banner" width="100%"/>

<br/>

<!-- BADGES -->

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-green?style=for-the-badge&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-AMQP-orange?style=for-the-badge&logo=rabbitmq)
![Docker](https://img.shields.io/badge/Docker-Containerized-blue?style=for-the-badge&logo=docker)
![JWT](https://img.shields.io/badge/Auth-JWT-black?style=for-the-badge)

<br/>

![Release](https://img.shields.io/badge/release-v0.0.1-blue?style=flat-square)
![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)

</div>

---

# 📖 Sobre a API

A **InvestAI API** é o backend principal da plataforma InvestAI, responsável por:

- 🔐 Autenticação e autorização JWT
- 👤 Gerenciamento de usuários e perfis de investidor
- 📈 Consulta e gerenciamento de ativos financeiros
- 💰 Módulo de renda fixa
- 📊 Dashboard personalizado
- 📑 Geração de relatórios PDF
- 🔔 Sistema de notificações
- ⭐ Favoritos
- 🧠 Integração com microsserviços de IA
- 📡 Integração com APIs externas de mercado financeiro

A aplicação foi desenvolvida utilizando arquitetura modular e princípios modernos de desenvolvimento backend com Spring Boot.

---

# ✨ Preview

> Espaço reservado para screenshots da documentação Swagger, arquitetura e dashboards futuros.

## 📚 Swagger UI

<img src="./docs/assets/swagger-preview.png" alt="Swagger Preview"/>

---

## 🏗️ Arquitetura

<img src="./docs/assets/architecture-preview.png" alt="Arquitetura"/>

---

# ⚙️ Stack Tecnológica

## 🚀 Backend
- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Spring Cache
- Spring AMQP
- Validation
- Lombok

---

## 🗄️ Banco de Dados
- PostgreSQL
- Flyway Migration

---

## 📨 Mensageria
- RabbitMQ

---

## 📄 Documentação
- Swagger / OpenAPI

---

## 🐳 Infraestrutura
- Docker
- Docker Compose

---

# 🏗️ Arquitetura do Projeto

```text id="wtjlwm"
com.investai.api
├── config/
│
├── module/
│   ├── auth/
│   ├── perfil/
│   ├── ativo/
│   ├── rendafixa/
│   ├── dashboard/
│   ├── relatorio/
│   ├── notificacao/
│   └── favorito/
│
├── infra/
│   ├── rabbitmq/
│   ├── hgbrasil/
│   └── exception/
│
└── shared/
    └── security/
```

---

# 📂 Estrutura Detalhada

## ⚙️ config/
Classes globais de configuração da aplicação.

```text id="lfhrj0"
config/
├── SecurityConfig.java
├── RabbitConfig.java
├── CacheConfig.java
├── SwaggerConfig.java
└── CorsConfig.java
```

---

## 📦 module/
Módulos de negócio da aplicação.

Cada módulo segue uma estrutura desacoplada e organizada por responsabilidade.

```text id="mw6m5r"
module/
└── auth/
    ├── controller/
    ├── service/
    ├── dto/
    ├── entity/
    ├── repository/
    ├── mapper/
    └── enums/
```

---

## 🔐 auth/
Responsável por:
- Cadastro
- Login
- JWT
- Refresh Token
- Controle de acesso

---

## 👤 perfil/
Gerenciamento do perfil de investidor.

- Perfil de risco
- Objetivos financeiros
- Horizonte de investimento
- Preferências de setores

---

## 📈 ativo/
Módulo de renda variável.

- Ações
- FIIs
- ETFs
- Filtros
- Rankings
- Integração com mercado

---

## 💰 rendafixa/
Módulo de renda fixa.

- Tesouro Direto
- CDB
- LCI/LCA
- Simulação líquida
- Compatibilidade por perfil

---

## 📊 dashboard/
Agregador principal de dados da aplicação.

- Indicadores econômicos
- Sugestões personalizadas
- Dados resumidos do usuário

---

## 📑 relatorio/
Geração de PDFs da plataforma.

```text id="upn57d"
relatorio/
├── controller/
├── service/
├── builder/
└── component/
```

---

## 🔔 notificacao/
Sistema de notificações da plataforma.

- Alertas
- Eventos financeiros
- Notificações in-app

---

## ⭐ favorito/
Gerenciamento de favoritos do usuário.

- Ativos favoritos
- Alertas automáticos
- Monitoramento rápido

---

## 📨 infra/rabbitmq/
Camada de comunicação assíncrona.

```text id="2j2gvx"
rabbitmq/
├── publisher/
├── consumer/
├── dto/
└── config/
```

Responsável por:
- Publicação de eventos
- Consumo de filas
- Integração com microsserviço IA

---

## 🌎 infra/hgbrasil/
Integração HTTP com APIs externas.

```text id="vk9gcf"
hgbrasil/
├── client/
├── dto/
├── mapper/
└── service/
```

---

## ❌ infra/exception/
Tratamento global de exceções.

```text id="kgsy8w"
exception/
├── GlobalExceptionHandler.java
├── BusinessException.java
├── NotFoundException.java
├── UnauthorizedException.java
└── ErrorResponse.java
```

---

## 🔒 shared/security/
Componentes compartilhados de segurança.

```text id="g2r96q"
security/
├── JwtUtil.java
├── JwtFilter.java
├── CustomUserDetailsService.java
└── SecurityConstants.java
```

---

# 🚀 Como Executar o Projeto

# 📋 Pré-requisitos

- Java 21
- Maven
- Docker
- Docker Compose
- PostgreSQL

---

# 🐳 Subindo Infraestrutura

## Execute:

```bash id="v7u6vx"
docker-compose up -d
```

---

# 🗄️ Serviços disponíveis

| Serviço | Porta |
|---|---|
| PostgreSQL | 5432 |
| RabbitMQ | 5672 |
| RabbitMQ Management | 15672 |

---

# ▶️ Executando a API

## Clone o projeto

```bash id="79db6o"
git clone https://github.com/SEU-USUARIO/investai-api.git
```

---

## Entre na pasta

```bash id="m3m5j9"
cd investai-api
```

---

## Execute a aplicação

```bash id="rk7n9r"
./mvnw spring-boot:run
```

---

# 🔑 Variáveis de Ambiente

```env id="zv4u8u"
JWT_SECRET=
POSTGRES_DB=
POSTGRES_USER=
POSTGRES_PASSWORD=
RABBITMQ_HOST=
RABBITMQ_PORT=
RABBITMQ_USER=
RABBITMQ_PASSWORD=
HG_BRASIL_API_KEY=
```

---

# 📚 Swagger/OpenAPI

Após iniciar a aplicação:

```txt id="vj5smp"
http://localhost:8080/swagger-ui.html
```

---

# 🔐 Segurança

A API utiliza:

- JWT Authentication
- Spring Security
- Stateless Session
- Refresh Tokens
- Role-based Authorization

---

# 📨 Comunicação Assíncrona

RabbitMQ é utilizado para:

- Integração com IA
- Processamento assíncrono
- Geração de resumos
- Ranqueamento de ativos

---

# 📄 Padrões Utilizados

## 🧱 Arquitetura
- Modular Monolith
- Layered Architecture
- DTO Pattern
- Builder Pattern

---

## 📚 Convenções
- RESTful APIs
- Versionamento `/api/v1`
- Soft Delete
- Exception Handling Global

---

## 🗃️ Banco de Dados
- UUID como chave primária
- Flyway Migration
- Auditoria de timestamps

---

# 🧪 Testes

> Espaço reservado para estratégia de testes automatizados.

## Futuramente:
- Unit Tests
- Integration Tests
- TestContainers
- MockMvc

---

# 📦 Roadmap

## ✅ MVP
- [ ] Autenticação JWT
- [ ] Perfil do investidor
- [ ] CRUD de ativos
- [ ] Dashboard
- [ ] RabbitMQ
- [ ] Relatórios PDF

---

## 🚧 Futuro
- [ ] Cache distribuído
- [ ] Rate limiting
- [ ] Observabilidade
- [ ] Métricas Prometheus
- [ ] OpenTelemetry
- [ ] CI/CD

---

# 📸 Releases

> Espaço reservado para screenshots futuras.

## v0.1.0

<img src="./docs/assets/releases/v0.1.0.png"/>

---

# 🤝 Contribuição

## Branch Pattern

```bash id="d1r23j"
feature/
bugfix/
hotfix/
release/
```

---

## Commit Pattern

```bash id="4h4c0k"
feat:
fix:
refactor:
docs:
test:
chore:
```

---

# 📄 Licença

Este projeto está sob a licença MIT.

---

# 👨‍💻 Autor

<div align="center">

## Lucas Fabiano

### Backend Developer • Java • Spring Boot • Software Architecture

<br/>

<a href="https://github.com/SEU-USUARIO">
  <img src="https://img.shields.io/badge/GitHub-Perfil-black?style=for-the-badge&logo=github"/>
</a>

<a href="https://linkedin.com/in/SEU-LINKEDIN">
  <img src="https://img.shields.io/badge/LinkedIn-Perfil-blue?style=for-the-badge&logo=linkedin"/>
</a>

</div>

---

<div align="center">

## ⭐ InvestAI API

Backend moderno para uma plataforma inteligente de investimentos.

</div>
````
