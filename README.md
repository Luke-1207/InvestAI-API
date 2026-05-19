<div align="center">

# 💸 InvestAI API

### API REST Inteligente para Plataforma de Investimentos

[//]: # (TODO- Banner do projeto)
[//]: # (<img src="./docs/assets/banner.png" alt="InvestAI API Banner" width="100%"/>)

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

[//]: # (## 📚 Swagger UI)

[//]: # ()
[//]: # (<img src="./docs/assets/swagger-preview.png" alt="Swagger Preview"/>)

[//]: # ()
[//]: # (---)

[//]: # (## 🏗️ Arquitetura)

[//]: # ()
[//]: # (<img src="./docs/assets/architecture-preview.png" alt="Arquitetura"/>)

[//]: # ()

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

# 🚀 Como Executar o Projeto

# 📋 Pré-requisitos

- Java 21
- Maven
- Docker
- Docker Compose
- PostgreSQL

---

[//]: # (# 🗄️ Serviços disponíveis)

[//]: # ()
[//]: # (| Serviço | Porta |)

[//]: # (|---|---|)

[//]: # (| PostgreSQL | 5432 |)

[//]: # (| RabbitMQ | 5672 |)

[//]: # (| RabbitMQ Management | 15672 |)

[//]: # ()
[//]: # (---)

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

# 📸 Releases

> Espaço reservado para screenshots futuras.

## v0.1.0

<img src="./docs/assets/releases/v0.1.0.png"/>

---

# 👨‍💻 Autor

<div align="center">

## Lucas Fabiano

### Backend Developer • Java • Spring Boot • Software Architecture

<br/>

<a href="https://github.com/Luke-1207">
  <img src="https://img.shields.io/badge/GitHub-Perfil-black?style=for-the-badge&logo=github"/>
</a>

<a href="https://www.linkedin.com/in/lucas-fabiano-peres-silva-70390424a/">
  <img src="https://img.shields.io/badge/LinkedIn-Perfil-blue?style=for-the-badge&logo=linkedin"/>
</a>

</div>

---

<div align="center">

## ⭐ InvestAI API

Backend moderno para uma plataforma inteligente de investimentos.

</div>
