# Documentação do Projeto Leadflow Backend

## Visão Geral do Projeto
O Leadflow Backend é uma aplicação desenvolvida em Java utilizando o framework Spring Boot. Ele fornece funcionalidades para gerenciar leads, autenticação de usuários, multi-tenancy e outros serviços relacionados. O projeto segue uma arquitetura modular e utiliza o Maven para gerenciamento de dependências e execução de testes.

### Tecnologias Utilizadas
- **Java**: Linguagem principal do projeto.
- **Spring Boot**: Framework para desenvolvimento rápido de aplicações Java.
- **Maven**: Ferramenta de build e gerenciamento de dependências.
- **JUnit**: Framework para testes unitários e de integração.
- **Banco de Dados**: Configurado para diferentes ambientes (dev, test, prod).

### Estrutura do Projeto
A estrutura do projeto segue o padrão do Maven:

```
leadflow-backend/
├── src/
│   ├── main/
│   │   ├── java/com/leadflow/backend/  # Código principal
│   │   └── resources/                  # Arquivos de configuração
│   ├── test/
│   │   ├── java/com/leadflow/backend/  # Testes
│   │   └── resources/                  # Recursos para testes
├── pom.xml                             # Configuração do Maven
├── target/                             # Arquivos gerados
```

## Configuração do Ambiente

### Pré-requisitos
- **Java 17** ou superior.
- **Maven** instalado e configurado no PATH.
- Banco de dados configurado conforme os arquivos `application-*.yml`.

### Passos para Configuração
1. Clone o repositório:
   ```bash
   git clone https://github.com/gabrielcapiotti/leadflow-backend.git
   ```
2. Navegue até o diretório do projeto:
   ```bash
   cd leadflow-backend
   ```
3. Compile o projeto:
   ```bash
   mvn clean install
   ```
4. Execute a aplicação:
   ```bash
   mvn spring-boot:run
   ```

## Testes

### Testes Existentes

#### 1. LeadServiceTest
- **Localização**: `src/test/java/com/leadflow/backend/service/lead/LeadServiceTest.java`
- **Descrição**: Este teste cobre as principais funcionalidades do serviço de leads, garantindo que as operações relacionadas a leads sejam realizadas corretamente.
- **Funcionalidades testadas**:
  - **Criação de Leads**:
    - Verifica se um lead é criado corretamente com os dados fornecidos.
    - Garante que o histórico de status do lead seja registrado.
    - Valida que não é possível criar leads com e-mails duplicados.
  - **Listagem de Leads Ativos**:
    - Certifica que apenas leads ativos são retornados para um usuário específico.
  - **Atualização de Status de Leads**:
    - Garante que o status de um lead pode ser atualizado e que o histórico é salvo.
    - Verifica que o histórico não é salvo se o status não for alterado.
  - **Exceções**:
    - Lança exceções apropriadas quando um lead não é encontrado ou quando os dados fornecidos são inválidos.

#### 2. Outros Testes Identificados
- **Controladores**:
  - `LeadControllerTest`: Testa endpoints relacionados a leads.
  - `AuthControllerTest`: Valida endpoints de autenticação.
- **Serviços**:
  - Testes para serviços como `AuthService` e `UserService`.
- **Repositórios**:
  - Testes para repositórios como `LeadRepository`.

### Componentes que Precisam de Testes

#### 1. Serviços
Os serviços abaixo são responsáveis pela lógica de negócios e precisam de testes unitários para garantir sua funcionalidade:

- **AuthService**:
  - Gerenciamento de autenticação e tokens.
  - Validação de credenciais.
- **UserService**:
  - Gerenciamento de usuários.
  - Criação, atualização e exclusão de usuários.
- **RoleService**:
  - Gerenciamento de permissões e papéis de usuários.
- **LogService**:
  - Registro de logs de atividades.
- **SettingService**:
  - Gerenciamento de configurações do sistema.
- **LeadStatusHistoryService**:
  - Registro e recuperação do histórico de status de leads.
- **JwtService**:
  - Geração e validação de tokens JWT.
- **TenantService**:
  - Gerenciamento de multi-tenancy.

#### 2. Controladores
Os controladores abaixo expõem endpoints REST e precisam de testes para validar suas respostas e integração com os serviços:

- **UserController**:
  - Endpoints para gerenciamento de usuários.
- **SettingController**:
  - Endpoints para configurações do sistema.
- **RoleController**:
  - Endpoints para gerenciamento de papéis e permissões.
- **AuthController**:
  - Endpoints para autenticação e autorização.
- **LeadStatusHistoryController**:
  - Endpoints para histórico de status de leads.

#### 3. Repositórios
Os repositórios abaixo interagem diretamente com o banco de dados e precisam de testes para validar consultas e operações:

- **LeadRepository**:
  - Consultas relacionadas a leads.
- **Outros Repositórios**:
  - Garantir que todas as operações de CRUD funcionem corretamente.

### Estratégia de Testes

#### 1. Testes Unitários
- **Objetivo**: Garantir que cada unidade de código funcione isoladamente.
- **Ferramentas**: JUnit, Mockito.
- **Cobertura**:
  - Métodos de serviços.
  - Validação de dados.
  - Exceções lançadas.

#### 2. Testes de Integração
- **Objetivo**: Validar a interação entre diferentes componentes (controladores, serviços e repositórios).
- **Ferramentas**: Spring Boot Test, Testcontainers (se necessário).
- **Cobertura**:
  - Endpoints REST.
  - Integração com o banco de dados.

#### 3. Testes de Regressão
- **Objetivo**: Garantir que alterações no código não introduzam novos bugs.
- **Ferramentas**: JUnit, Maven Surefire.
- **Cobertura**:
  - Funcionalidades críticas do sistema.

### Próximos Passos
1. **Criar Testes Unitários**:
   - Priorizar serviços e controladores listados.
   - Garantir cobertura de exceções e validações.
2. **Desenvolver Testes de Integração**:
   - Validar endpoints REST e interações com o banco de dados.
3. **Automatizar Execução de Testes**:
   - Configurar pipelines de CI/CD para executar os testes automaticamente.
4. **Monitorar Cobertura de Testes**:
   - Utilizar ferramentas como JaCoCo para garantir alta cobertura de código.

---

Com esta abordagem, garantiremos a qualidade e a confiabilidade do sistema, minimizando riscos de falhas em produção.