# Timoneiro 



## **Como reproduzir na sua máquina**

Siga os passos abaixo para configurar o banco de dados e rodar a aplicação localmente:

### 1. Pré-requisitos
- Java 17+ instalado
- Maven ou Gradle
- PostgreSQL rodando localmente
- Flyway (integrado à aplicação Spring Boot)
- IDE de sua preferência (IntelliJ, VS Code, Eclipse, etc.)

### 2. Configurar o banco
Crie o banco no PostgreSQL:

CREATE DATABASE boat_rental; (ou o nome que você preferir)

Atualize o arquivo `application.properties` ou `application.yml` do Spring Boot com as credenciais do seu banco:

spring.datasource.url=jdbc:postgresql://localhost:5432/boat_rental  
spring.datasource.username=SEU_USUARIO  
spring.datasource.password=SUA_SENHA  
spring.datasource.driver-class-name=org.postgresql.Driver

# Flyway
spring.flyway.enabled=true  
spring.flyway.locations=classpath:db/migration

### 3. Rodar as migrations
Coloque o arquivo `V1__initial_schema.sql` (com todas as tabelas, índices e constraints) em `src/main/resources/db/migration`.  
Ao iniciar a aplicação, o Flyway aplicará automaticamente a migration e criará todas as tabelas.

### 4. Inicializar a aplicação
Rode a aplicação Spring Boot via IDE ou terminal:

mvn spring-boot:run

A aplicação conectará ao banco e poderá ser testada localmente.

### 5. Testar
Use Postman ou qualquer cliente HTTP para testar os endpoints.  
Verifique se o banco está populado e funcionando conforme esperado.




## Configurações Gerais

Criamos o arquivo `application.yml` principal com configurações comuns e importamos opcionalmente um arquivo `application-local.yml` 
contendo usuario e senha. Esta configuração traz vários benefícios: segurança (senhas não entram no Git), flexibilidade
(caso queira separar perfis em dev, test e prod) e praticidade (desenvolvimento local sem precisar setar
variáveis de ambiente toda vez).

O arquivo `application-local.yml` contém as credenciais de desenvolvimento local e está listado 
no `.gitignore` para não vazar senhas. 

Para rodar localmente, clone o repositório com `git clone <URL_DO_REPOSITORIO>` e entre na pasta com 
`cd timoneiro`. 

Em seguida, crie o arquivo `application-local.yml` na raiz do projeto com suas credenciais locais 
(não comitar), por exemplo:  
`spring:`   
`  datasource:`  
`    url: jdbc:postgresql://localhost:5432/boat_rental`  
`    username: usuario_dev`  
`    password: senha_dev`.

Depois, rode a aplicação no perfil dev (padrão) com `mvn spring-boot:run`. 

O Spring Boot carregará automaticamente o `application-local.yml` . 

## **Modelo de Banco de Dados**

### **Visão Geral**
O banco de dados do **timoneiro** foi projetado para suportar todas as funcionalidades principais do sistema, 
garantindo integridade dos dados, performance e escalabilidade. 

Utiliza princípios **relacionais**, com relacionamentos, índices e constraints adequados.

---

### **Entidades**

1. **User (Usuário)**
    - Armazena todos os usuários do sistema (clientes e proprietários de barcos).
    - Campos: `name`, `email` (único), `password`, `phone`, `role`, `created_at`.
    - Justificativa: Necessário para autenticação (JWT) e para vincular barcos, reservas, avaliações, mensagens e pagamentos a usuários específicos.

2. **Boat (Barco)**
    - Representa os barcos disponíveis para aluguel.
    - Campos: `name`, `description`, `type`, `capacity`, `price_per_hour`, `location`, `photo_url`, `owner_id`.
    - Justificativa: Cada barco deve estar vinculado a um usuário cadastrado (proprietário). Inclui `photo_url` para imagens e campos que suportam busca e definição de preço.

3. **BoatAvailability (Disponibilidade do Barco)**
    - Armazena os períodos de disponibilidade de cada barco.
    - Campos: `start_date`, `end_date`.
    - Justificativa: Permite que o proprietário defina os dias e horários que o barco estara disponível, permitindo reservas somente em períodos válidos.

4. **Booking (Reserva)**
    - Registra as reservas feitas pelos usuários dentro da disponibilidade estipulada pelos proprietários
    - Campos: `user_id`, `boat_id`, `start_date`, `end_date`, `status`, `total_price`.
    - Justificativa: Conecta usuários e barcos para períodos de aluguel. O status e preço total são armazenados. A regra de impedir sobreposição de reservas é aplicada na aplicação.

5. **Review (Avaliação)**
    - Armazena notas e comentários dos usuários sobre barcos.
    - Campos: `user_id`, `boat_id`, `rating`, `comment`, `date`.
    - Justificativa: Permite feedback dos usuários, ajuda a criar confiança e melhorar a qualidade do serviço. A nota é limitada entre 1 e 5.

6. **Payment (Pagamento)**
    - Controla os pagamentos relacionados às reservas.
    - Campos: `booking_id`, `amount`, `status`, `payment_date`.
    - Justificativa: Conecta transações financeiras às reservas para faturamento correto e acompanhamento do status.

7. **Message (Mensagem)**
    - Armazena comunicação entre usuários sobre uma reserva.
    - Campos: `booking_id`, `sender_id`, `content`, `sent_at`.
    - Justificativa: Permite comunicação direta entre usuários e proprietários dentro da plataforma.

---

### **Índices**
- **Boat (`location`, `type`)** → acelera buscas por localização e tipo de barco.
- **Booking (`boat_id`, `start_date`, `end_date`)** → agiliza checagem de disponibilidade.
- **Booking (`user_id`)** → busca rápida das reservas de um usuário.
- **Payment (`status`)** → consultas eficientes de pagamentos pendentes.
- **Review (`boat_id`)** → recuperação rápida de avaliações de um barco.

**Justificativa:** Índices melhoram o desempenho das consultas, especialmente para buscas frequentes e relatórios.

---

### **Decisões de Design**
1. **Modelo Relacional:** Garante integridade referencial usando chaves estrangeiras.
2. **Separação de Responsabilidades:** Cada entidade tem uma função única (ex.: BoatAvailability separado de Booking).
3. **Integridade de Dados:** Constraints em campos únicos (email), limites de rating e campos não nulos.
4. **Extensibilidade:** Novos recursos (promoções, papéis adicionais) podem ser adicionados sem mudanças significativas no esquema.
5. **Compatibilidade:** Script SQL para PostgreSQL, pronto para Flyway, permitindo controle de versão e deploy seguro.

## Padrão de Projeto

No backend do projeto, adotamos o padrão **MVC (Model-View-Controller)**.
#### Justificativa para usar MVC

- **Separação de responsabilidades:** Cada camada tem uma função clara, evitando misturar lógica de negócio, acesso a dados e tratamento de requisições.
- **Manutenção facilitada:** Mudanças na lógica de negócio ou na forma de persistência não afetam o Controller.
- **Testabilidade:** Serviços podem ser testados isoladamente sem depender do Controller ou do banco de dados.
- **Escalabilidade:** O projeto pode crescer sem comprometer a organização do código.

#### Componentes do MVC no nosso projeto

- **Model (Entidades e DTOs):**  
  Representa os dados do sistema.
   - `User`, `Boat`, `Address`, `Booking`, etc.
   - DTOs (`UserRequestDTO`, `UserResponseDTO`) para expor apenas os dados necessários ao front-end, evitando o vazamento de informações sensíveis.

- **Controller:**  
  Responsável por receber as requisições HTTP, delegar a lógica para os serviços e retornar respostas.  
  Ele não deve conter lógica de negócio, apenas tratar requisições, respostas e erros.

- **Service:**  
  Camada intermediária onde a **lógica de negócio** é implementada.  
  Recebe dados do controller, manipula entidades, chama repositórios e retorna resultados.  
  Exemplo: `UserService` que salva, busca e deleta usuários.

- **Repository:**  
  Responsável pela comunicação com o banco de dados usando JPA/Hibernate.  
  Interfaces que estendem `JpaRepository` permitem realizar operações CRUD sem precisar escrever SQL manualmente.



#### Integração com MapStruct

O uso do **MapStruct** se encaixa perfeitamente nesse padrão:
- Os **mappers** atuam como uma ponte entre Model e DTOs, mantendo o Controller e o Service livres da lógica de conversão de dados.
- Isso reforça a separação de responsabilidades e torna o código mais limpo e seguro.

### EndPoints

## Authentication & User Endpoints

Esta seção documenta todos os endpoints relacionados à autenticação e gerenciamento de usuários do sistema.

| Endpoint | Method | Access | Request Body | Response |
|----------|--------|--------|--------------|----------|
| `/api/auth/login` | POST | Public | `{ "email": "user@example.com", "password": "string" }` | `{ "userId": 1, "nome": "João", "email": "user@example.com", "token": "jwt-token-aqui" }` |
| `/api/users/register` | POST | Public | `{ "name": "João", "email": "joao@example.com", "password": "senha123", "telefone": "11999999999" }` | `{ "userId": 2, "nome": "João", "email": "joao@example.com", "telefone": "11999999999", "criadoEm": "2025-08-27T12:00:00", "token": "jwt-token-aqui" }` |
| `/api/users` | POST | Admin | `{ "name": "Maria", "email": "maria@example.com", "password": "senha123", "telefone": "11988888888" }` | `{ "userId": 3, "nome": "Maria", "email": "maria@example.com", "telefone": "11988888888", "criadoEm": "2025-08-27T12:05:00", "token": "jwt-token-aqui" }` |
| `/api/users` | GET | Admin | - | `[ { "id": 1, "nome": "João", "telefone": "11999999999", "email": "joao@example.com", "criadoEm": "2025-08-27T12:00:00" }, { "id": 3, "nome": "Maria", "telefone": "11988888888", "email": "maria@example.com", "criadoEm": "2025-08-27T12:05:00" } ]` |
| `/api/users/search?name={name}` | GET | Admin | - | `[ { "id": 1, "nome": "João", "telefone": "11999999999", "email": "joao@example.com", "criadoEm": "2025-08-27T12:00:00" } ]` |
| `/api/users/email/{email}` | GET | Admin | - | `{ "id": 1, "nome": "João", "telefone": "11999999999", "email": "joao@example.com", "criadoEm": "2025-08-27T12:00:00" }` |
| `/api/users/{id}` | PUT | Admin ou Self | `{ "name": "João Atualizado", "email": "joao@example.com", "telefone": "11999999999" }` | `{ "id": 1, "nome": "João Atualizado", "telefone": "11999999999", "email": "joao@example.com", "criadoEm": "2025-08-27T12:00:00" }` |
| `/api/users/{id}` | DELETE | Admin | - | `204 No Content` |
| `/api/users/profile` | GET | Authenticated | - | `{ "id": 1, "nome": "João", "telefone": "11999999999", "email": "joao@example.com", "criadoEm": "2025-08-27T12:00:00" }` |
| `/api/users/profile` | PUT | Authenticated | `{ "name": "João Atualizado", "email": "joao@example.com", "telefone": "11999999999" }` | `{ "id": 1, "nome": "João Atualizado", "telefone": "11999999999", "email": "joao@example.com", "criadoEm": "2025-08-27T12:00:00" }` |

> **Observações:**
> - "Admin" significa que apenas usuários com `ROLE_ADMIN` podem acessar.
> - "Self" significa que o usuário pode acessar seu próprio recurso (perfil ou atualização) mesmo sem ser admin.
> - `AuthResponseDTO` contém `userId`, `nome`, `email`, e `token`.
> - `UserResponseDTO` contém `id`, `nome`, `telefone`, `email` e `criadoEm`.


## Notas do Desenvolvedor

Pontos de evolução pessoal nesse projeto:

* Entendimento maior sobre autenticação com JWT

   Pontos relevantes: 
        - cors;
        - Problemas com versões do Spring e JDK incompatíveis com algumas dependencias;