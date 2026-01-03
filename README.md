# Timoneiro 

## 🎯 **Stack Escolhida**
- **Java + Spring Boot** - API REST
- **PostgreSQL** - Dados relacionais e metadados
- **Cloudinary** - Armazenamento de imagens e vídeos
- **Northflank/Vercel/Neon** - Deploy (free tier)

## 🔄 **ANÁLISE DE TRADE-OFFS**

### **🗄️ PostgreSQL**
| **Prós** | **Contras** |
|----------|-------------|
| ACID guarantees | Overkill para volume baixo |
| Relationships e constraints | Complexidade de configuração |
| Migrations robustas | Performance em alta concorrência precisa tuning |
| JSONB para flexibilidade | Deployment gratuito tem limites |

Escala verticalmente bem, mas precisa de replicação para alta disponibilidade.

### **⚙️ Spring Boot**
| **Prós** | **Contras** |
|----------|-------------|
| Ecossistema maduro | Startup time mais lento |
| Segurança robusta | Memory footprint maior |
| Production-ready por padrão | Curva de aprendizado |
| Boa documentação e comunidade | Overengineering para CRUD simples |

É padrão enterprise, mas pode ser pesado para microsserviços leves;

## 🚨 **PONTOS DE ATENÇÃO**

### **Riscos Técnicos:**
- Consistência entre PostgreSQL e Cloudinary - Deletar embarcação deve deletar imagens
- Rate limiting necessário - Proteger contra uploads maliciosos
- Monitoramento distribuído

### **Riscos de Custo:**
- PostgreSQL gratuito tem limites (10GB)
- Cloudinary free tier (10GB storage)
- Deploy platform pode cobrar por build minutes

## 🛡️ **MITIGAÇÕES IMPLEMENTADAS**
- Validações de tamanho de arquivo no backend
- Limites por usuário no upload
- Monitoramento básico de storage
- Arquitetura preparada para migração


## 🎯 **JUSTIFICATIVA DA ESCOLHA**
**Para Portfolio:**
- Arquitetura production-ready
- Custo zero durante desenvolvimento
- Stack valorizada no mercado
- Base para evolução futura

**No Mundo Real:**
- Arquitetura comprovada em escala
- Separação adequada de concerns
- Facilidade de contratação de devs
- Ecossistema maduro de ferramentas

# ⚖️ Trade-offs: Sistema de pagamentos

O **PagSeguro** permite demonstrar a integração real com um gateway de pagamento brasileiro, usando seu ambiente de Sandbox sem custos e com total segurança.

✅ Vantagens:

- Sandbox para testes: Permite simular todo o fluxo de pagamento sem usar dados ou dinheiro real
- Foco no Brasil: Demonstra conhecimento do mercado local, um diferencial para oportunidades domésticas
- Documentação em Português: Facilita a implementação e entendimento

⚠️ Desvantagens/Riscos:
- Menor reconhecimento global: Stripe teria maior escalabilidade para mercado internacional

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

Há um arquivo .env.properties contendo usuarios e senhas. Esta configuração traz vários benefícios: segurança (senhas não entram no Git), flexibilidade (caso queira separar perfis em dev, test e prod) e praticidade (desenvolvimento local sem precisar setar variáveis de ambiente toda vez).

Os arquivos `application.yml` obtêm as senhas do .env.properties (há um arquivo .env.example para você preencher conforme suas senhas).

Para rodar localmente, clone o repositório com `git clone <URL_DO_REPOSITORIO>` e entre na pasta com `cd timoneiro`. 

Em seguida, siga preencha o .env.example na raiz do projeto com suas credenciais locais.

Depois, rode a aplicação no perfil dev (padrão) com `mvn spring-boot:run`. 

O Spring Boot carregará automaticamente o `application-local.yml` . 

## **Modelo de Banco de Dados**

### **Visão Geral**
O banco de dados do **timoneiro** foi projetado para suportar todas as funcionalidades principais do sistema, 
garantindo integridade dos dados, performance e escalabilidade. 

Utiliza princípios **relacionais**, com relacionamentos, índices e constraints adequados.

---

### **Entidades**

1. **Users (Usuário)**
    - Armazena todos os usuários do sistema (clientes e proprietários de barcos).
    - Campos: `name`, `email` (único), `password`, `phone`, `role`, `created_at`, `active`, `cpf`.
    - Justificativa: Necessário para autenticação (JWT) e para vincular barcos, reservas, avaliações, mensagens e pagamentos a usuários específicos.

2. **Boats (Barcos)**
    - Representa os barcos disponíveis para aluguel.
    - Campos: `name`, `description`, `type`, `capacity`, `price_per_hour`, `location`, `photo_url`, `owner_id`, `address_id`,`lengh`,`speed`,`fabrication`.
    - Justificativa: Cada barco deve estar vinculado a um usuário cadastrado (proprietário). Inclui `photo_url` para imagens e campos que suportam busca e definição de preço.

3. **BoatAvailability (Disponibilidade do Barco)**
    - Armazena os períodos de disponibilidade de cada barco.
    - Campos: `boat_id`, `start_date`, `end_date`, `price_per_hour`.
    - Justificativa: Permite que o proprietário defina os dias e horários que o barco estara disponível, permitindo reservas somente em períodos válidos.

4. **Bookings (Reservas)**
    - Registra as reservas feitas pelos usuários dentro da disponibilidade estipulada pelos proprietários
    - Campos: `user_id`, `boat_id`, `start_date`, `end_date`, `status`, `total_price`.
    - Justificativa: Conecta usuários e barcos para períodos de aluguel. O status e preço total são armazenados. A regra de impedir sobreposição de reservas é aplicada na aplicação.

5. **Reviews (Avaliações)**
    - Armazena notas e comentários dos usuários sobre barcos.
    - Campos: `user_id`, `boat_id`, `rating`, `comment`, `created_at`, `updated_ad`.
    - Justificativa: Permite feedback dos usuários, ajuda a criar confiança e melhorar a qualidade do serviço. A nota é limitada entre 1 e 5.

6. **Payments (Pagamento)**
    - Controla os pagamentos relacionados às reservas.
    - Campos: `booking_id`, `amount`, `status`, `payment_date`, `paymen_method`, `transaction_id`, `gateway_messsage`, `processed_at`, `created_at`, `updated_at`, `gateway_response`, `version`.
    - Justificativa: Conecta transações financeiras às reservas para faturamento correto e acompanhamento do status.

7. **Messages (Mensagens)**
    - Armazena comunicação entre usuários sobre uma reserva.
    - Campos: `booking_id`, `sender_id`, `content`, `sent_at`.
    - Justificativa: Permite comunicação direta entre usuários e proprietários dentro da plataforma.

8. **Addresses (Endereços)**
    - Armazena os endereços onde os barcos estão aportados.
    - Campos: `cep`, `number`, `street`, `neighborhood`, `city`, `state`, `marina`.
    - Justificativa: Permite a busca do barco nos diferentes niveis de localização.

9. **Boat_amenities (Comodidades dos barcos)**
    - Armazena os confortos disponíveis em cada embarcação
    - Campos: `amenity`
    - Justificativa: Permite o filtro conforme as necessidades do usuário.

10. **Boat_photos (Fotos)**
    - Armazena as URLs das fotograficas das embarcações (hospedadas no cloudinary)
    - Campos: `boat_id`, `photo_url`, `ordem`, `public_id`, `file_name`, `created_at`
    - Justificativa: Permite o filtro conforme as necessidades do usuário.


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
6. **Proteção de dados:** Utilização de basicDTOs como intermediários para não retornar a entidade inteira para os ResponseDTOs.

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

<br>

# Utilização 

<br>

## EndPoints

Para fazer requisições verifique se o endpoint é publico ou requer autenticação conforme informações abaixo.
Para endpoints que requerem autenticação, você pode criar o seu no endpoint `/api/users/register` ou pode usar esse usuário já cadastrado:

 (role_user):

  "email": "user@test.com",
  "password": "asd12345"


Para fazer uma reserva, selecione um dos periodos de disponibilidade dos barcos:

Azimut 55 - De: 10/01/2026 a 28/02/2026
Sea Ray 510 Sundancer - De 01/03/2026 a 30/05/2026
Splash - De 20/03/2026 a 01/07/2026 
Vento Norte - 01/06/2026 a 30/10/2026

> obs.: Você conseguirá criar janelas de disponibilidade somente nos barcos que cadastrar.

No checkout coloque o meio de pagamento `CREDIT_CARD` e preencha com os dados ficticios abaixo (disponibilizados pelo pagbank):

Nome: Jose da Silva
Número: 4539620659922097
Cód. de Seg.: 123
Data Exp.:12/26



### 🔐 Auth

| Endpoint | Method | Access | Request Body | Response |
|----------|--------|--------|--------------|----------|
| `/api/auth/login` | POST | **Public** | `{ "email": "user@example.com", "password": "string" }` | `{ "token": "jwt-token", "tokenType": "Bearer", "userId": 1, "email": "user@example.com", "name": "João" }` |
| `/api/auth/validate` | GET | **Public** (token no header) | Header: `Authorization: Bearer <token>` | `{ "valid": true, "email": "user@example.com", "authenticated": true, "timestamp": "2024-01-15T10:00:00Z" }` |
| `/api/auth/me` | GET | **Authenticated** | Header: `Authorization: Bearer <token>` | `{ "id": 1, "name": "João", "email": "user@example.com" }` |

> **Observações:**
> - `/api/auth/validate` requer token no header `Authorization` mas não requer autenticação para acessar o endpoint
> - `/api/auth/me` requer usuário autenticado com qualquer role (USER ou ADMIN)
> - Resposta de login: campo `nome` → `name` e adicionado `tokenType: "Bearer"`
> - `/api/auth/me` retorna `UserBasicDTO` com informações básicas do usuário atual



### 🚤 Boats


| Endpoint | Method | Access | Request Body | Response |
|----------|--------|--------|--------------|----------|
| `/api/boats` | GET | **Public** | - | `[ { "id": 1, "name": "Sea Ray 510", "description": "Luxuoso iate motorizado...", "type": "Motor Yacht", "capacity": 10, "length": 15.5, "speed": 25.0, "fabrication": 2020, "amenities": ["Wi-Fi", "TV", "Ar Condicionado"], "photos": ["https://res.cloudinary.com/.../sea-ray.jpg"], "pricePerHour": 350.00, "city": "Rio de Janeiro", "state": "RJ", "marina": "Marina da Glória", "ownerName": "João", "ownerId": 5 } ]` |
| `/api/boats/{id}` | GET | **Public** | - | `{ "id": 1, "name": "Sea Ray 510", "description": "Luxuoso iate motorizado...", "type": "Motor Yacht", "capacity": 10, "length": 15.5, "speed": 25.0, "fabrication": 2020, "amenities": ["Wi-Fi", "TV", "Ar Condicionado"], "photos": ["https://res.cloudinary.com/.../sea-ray.jpg"], "pricePerHour": 350.00, "city": "Rio de Janeiro", "state": "RJ", "marina": "Marina da Glória", "ownerName": "João", "ownerId": 5 }` |
| `/api/boats` | POST | **Authenticated** (ROLE_USER ou ROLE_ADMIN) | **multipart/form-data**<br>1. `boat` (JSON string): `{ "name": "Novo Barco", "description": "Descrição...", "type": "Veleiro", "capacity": 6, "length": 12.0, "speed": 15.0, "fabrication": 2018, "amenities": ["Cozinha", "Banheiro"], "pricePerHour": 200.00, "cep": "20000-000", "number": "100", "street": "Rua Exemplo", "neighborhood": "Centro", "city": "Rio de Janeiro", "state": "RJ", "marina": "Marina Exemplo" }`<br>2. `images` (opcional): Lista de arquivos (max 10) | `{ "id": 11, "name": "Novo Barco", "description": "Descrição...", "type": "Veleiro", "capacity": 6, "length": 12.0, "speed": 15.0, "fabrication": 2018, "amenities": ["Cozinha", "Banheiro"], "photos": ["https://res.cloudinary.com/.../image1.jpg"], "pricePerHour": 200.00, "city": "Rio de Janeiro", "state": "RJ", "marina": "Marina Exemplo", "ownerName": "João", "ownerId": 5 }` |
| `/api/boats/{id}` | PUT | **Authenticated** (dono do barco) | `{ "name": "Barco Atualizado", "description": "...", "type": "Motor Yacht", "capacity": 10, "length": 15.5, "speed": 25.0, "fabrication": 2020, "amenities": ["Wi-Fi", "TV"], "pricePerHour": 380.00, "cep": "20000-000", "number": "100", "street": "Rua Exemplo", "neighborhood": "Centro", "city": "Rio de Janeiro", "state": "RJ", "marina": "Marina Exemplo" }` | `{ "id": 1, "name": "Barco Atualizado", "description": "...", "type": "Motor Yacht", "capacity": 10, "length": 15.5, "speed": 25.0, "fabrication": 2020, "amenities": ["Wi-Fi", "TV"], "photos": ["https://res.cloudinary.com/.../sea-ray.jpg"], "pricePerHour": 380.00, "city": "Rio de Janeiro", "state": "RJ", "marina": "Marina da Glória", "ownerName": "João", "ownerId": 5 }` |
| `/api/boats/my-boats` | GET | **Authenticated** (dono) | Header: `Authorization: Bearer <token>`<br>Query Params: `?page=0&size=10&sort=name` | `{ "content": [ { "id": 1, "name": "Sea Ray 510", ... } ], "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }` |
| `/api/boats/{boatId}/photos` | GET | **Public** | - | `[ { "id": 1, "photoUrl": "https://res.cloudinary.com/.../image1.jpg", "publicId": "boats/abc123", "fileName": "barco1.jpg", "ordem": 0 } ]` |
| `/api/boats/{boatId}/photos` | POST | **Authenticated** (dono do barco) | **multipart/form-data**<br>`images`: Lista de arquivos (max 10) | `[ { "id": 2, "photoUrl": "https://res.cloudinary.com/.../image2.jpg", "publicId": "boats/def456", "fileName": "barco2.jpg", "ordem": 1 } ]` |
| `/api/boats/{boatId}/photos/{photoId}` | DELETE | **Authenticated** (dono do barco) | - | `204 No Content` |
| `/api/boats/{boatId}/photos/order` | PUT | **Authenticated** (dono do barco) | `{ "photoIds": [3, 1, 2] }` | `200 OK` |

> **Observações:**
> - **Acesso:** Todos os endpoints de criação/atualização exigem autenticação e que o usuário seja dono do barco
> - **Fotos:** Sistema integrado com Cloudinary, suporte a múltiplas fotos com ordenação
> - **Endereço:** enviado inline no JSON (cep, number, street, neighborhood, city, state, marina) em vez de `addressId`
> - **Paginação:** Endpoint `/api/boats/my-boats` suporta paginação

## 👥 Users

| Endpoint | Method | Access | Request Body | Response |
|----------|--------|--------|--------------|----------|
| `/api/users/register` | POST | **Public** | `{ "name": "João", "email": "joao@example.com", "password": "senha123", "cpf": "12345678901", "phone": "11999999999" }` | `{ "token": "jwt-token", "tokenType": "Bearer", "userId": 2, "email": "joao@example.com", "name": "João" }` |
| `/api/users` | POST | **ROLE_ADMIN** | `{ "name": "Maria", "email": "maria@example.com", "password": "senha123", "cpf": "98765432100", "phone": "11988888888" }` | `{ "token": "jwt-token", "tokenType": "Bearer", "userId": 3, "email": "maria@example.com", "name": "Maria" }` |
| `/api/users` | GET | **ROLE_ADMIN** | - | `[ { "id": 1, "name": "João", "phone": "11999999999", "email": "joao@example.com", "createdAt": "2025-08-27T12:00:00" }, { "id": 3, "name": "Maria", "phone": "11988888888", "email": "maria@example.com", "createdAt": "2025-08-27T12:05:00" } ]` |
| `/api/users/search?name={name}` | GET | **ROLE_ADMIN** | - | `[ { "id": 1, "name": "João", "phone": "11999999999", "email": "joao@example.com", "createdAt": "2025-08-27T12:00:00" } ]` |
| `/api/users/email/{email}` | GET | **ROLE_ADMIN** | - | `{ "id": 1, "name": "João", "phone": "11999999999", "email": "joao@example.com", "createdAt": "2025-08-27T12:00:00" }` |
| `/api/users/{id}` | PUT | **ROLE_ADMIN ou Self** | `{ "name": "João Atualizado", "email": "joao@example.com", "password": "novaSenha123", "cpf": "12345678901", "phone": "11999999999" }` | `{ "id": 1, "name": "João Atualizado", "phone": "11999999999", "email": "joao@example.com", "createdAt": "2025-08-27T12:00:00" }` |
| `/api/users/{id}` | DELETE | **ROLE_ADMIN** | - | `204 No Content` |
| `/api/users/profile` | GET | **Authenticated** (ROLE_USER ou ROLE_ADMIN) | - | `{ "id": 1, "name": "João", "phone": "11999999999", "email": "joao@example.com", "createdAt": "2025-08-27T12:00:00" }` |
| `/api/users/profile` | PUT | **Authenticated** (ROLE_USER ou ROLE_ADMIN) | `{ "name": "João Atualizado", "email": "joao@example.com", "password": "novaSenha123", "cpf": "12345678901", "phone": "11999999999" }` | `{ "id": 1, "name": "João Atualizado", "phone": "11999999999", "email": "joao@example.com", "createdAt": "2025-08-27T12:00:00" }` |

> **Observações:**
> - **Self:** Usuário pode acessar seu próprio recurso (`/api/users/{id}`) com o próprio ID
> - **Campos obrigatórios:** Todos os campos do `UserRequestDTO` são obrigatórios em POST/PUT (incluindo `cpf` e `password`)
> - **Respostas diferentes:** Register e Create retornam `AuthResponseDTO` com token, outros endpoints retornam `UserResponseDTO`
> - **Novo campo:** `cpf` adicionado ao request (validação CPF brasileiro, 11 dígitos)
> - **Campo removido:** Response não inclui mais o campo `telefone` (agora é `phone`)
> - **Validação:** `cpf` é validado quanto à formatação e dígitos verificadores
> - **Password:** Em atualizações, a senha deve ser enviada (mesmo se não for alterar)


## 📅 Boat Availability

| Endpoint | Method | Access | Request Body | Response |
|----------|--------|--------|--------------|----------|
| `/api/boats/{boatId}/availability` | POST | **Authenticated** (ROLE_USER ou ROLE_ADMIN) | `{ "startDate": "2024-12-30T14:30:00", "endDate": "2024-12-30T18:30:00", "pricePerHour": 350.00 }` | `{ "id": 1, "boatId": 5, "startDate": "2024-12-30T14:30:00", "endDate": "2024-12-30T18:30:00", "pricePerHour": 350.00 }` |
| `/api/boats/{boatId}/availability` | GET | **Public** | - | `[ { "id": 1, "boatId": 5, "startDate": "2024-12-30T14:30:00", "endDate": "2024-12-30T18:30:00", "pricePerHour": 350.00 }, { "id": 2, "boatId": 5, "startDate": "2024-12-31T10:00:00", "endDate": "2024-12-31T14:00:00", "pricePerHour": 400.00 } ]` |
| `/api/boats/{boatId}/availability/{id}` | GET | **Public** | - | `{ "id": 1, "boatId": 5, "startDate": "2024-12-30T14:30:00", "endDate": "2024-12-30T18:30:00", "pricePerHour": 350.00 }` |
| `/api/boats/{boatId}/availability/{id}` | PUT | **Authenticated** (ROLE_USER ou ROLE_ADMIN) | `{ "startDate": "2024-12-30T15:00:00", "endDate": "2024-12-30T19:00:00", "pricePerHour": 380.00 }` | `{ "id": 1, "boatId": 5, "startDate": "2024-12-30T15:00:00", "endDate": "2024-12-30T19:00:00", "pricePerHour": 380.00 }` |
| `/api/boats/{boatId}/availability/{id}` | DELETE | **Authenticated** (ROLE_USER ou ROLE_ADMIN) | - | `204 No Content` |
| `/api/boats/{boatId}/availability/check-availability` | GET | **Public** | Query Params: `?startDate=2024-12-30T14:30:00&endDate=2024-12-30T18:30:00` | `true` ou `false` |

> **Observações:**
> - **Acesso:** Endpoints GET são públicos; POST/PUT/DELETE requerem autenticação (dono do barco ou admin)
> - **Datas:** Formato ISO 8601: `YYYY-MM-DDTHH:mm:ss`
> - **Verificação:** `check-availability` retorna booleano indicando se o barco está disponível no intervalo
> - **Preço:** `pricePerHour` pode variar por janela de disponibilidade
> - **Validação:** Não podem haver conflitos de datas (janelas sobrepostas para o mesmo barco)


## 🎫 Bookings

| Endpoint | Method | Access | Request Body | Response |
|----------|--------|--------|--------------|----------|
| `/api/bookings` | POST | **Authenticated** (ROLE_USER ou ROLE_ADMIN) | `{ "boatId": 5, "startDate": "2024-12-30T14:30:00", "endDate": "2024-12-30T18:30:00", "paymentMethod": "CREDIT_CARD", "mockCardData": { "cardNumber": "4111111111111111", "expiryMonth": "12", "expiryYear": "2025", "cvv": "123" } }` | `{ "id": 1, "user": { "id": 1, "name": "João", "email": "joao@example.com" }, "boat": { "id": 5, "name": "Sea Ray 510", "type": "Motor Yacht", "capacity": 10, "pricePerHour": 350.00 }, "startDate": "2024-12-30T14:30:00", "endDate": "2024-12-30T18:30:00", "status": "PENDING", "totalPrice": 1400.00 }` |
| `/api/bookings/{bookingId}` | GET | **Authenticated** (dono da reserva ou dono do barco) | - | `{ "id": 1, "user": { "id": 1, "name": "João", "email": "joao@example.com" }, "boat": { "id": 5, "name": "Sea Ray 510", "type": "Motor Yacht", "capacity": 10, "pricePerHour": 350.00 }, "startDate": "2024-12-30T14:30:00", "endDate": "2024-12-30T18:30:00", "status": "CONFIRMED", "totalPrice": 1400.00 }` |
| `/api/bookings/my-bookings` | GET | **Authenticated** (ROLE_USER ou ROLE_ADMIN) | Query Params: `?page=0&size=20&status=CONFIRMED` (status opcional) | `{ "content": [ { "id": 1, "user": { "id": 1, "name": "João", "email": "joao@example.com" }, "boat": { "id": 5, "name": "Sea Ray 510", "type": "Motor Yacht", "capacity": 10, "pricePerHour": 350.00 }, "startDate": "2024-12-30T14:30:00", "endDate": "2024-12-30T18:30:00", "status": "CONFIRMED", "totalPrice": 1400.00 } ], "pageable": { "pageNumber": 0, "pageSize": 20 }, "totalPages": 1, "totalElements": 1 }` |
| `/api/bookings/{bookingId}/cancel` | POST | **Authenticated** (dono da reserva) | - | `200 OK` |

> **Observações:**
> - **Acesso:** 
>   - POST: usuário autenticado cria reserva para si mesmo (userId definido automaticamente do token)
>   - GET /my-bookings: retorna reservas do usuário atual (como locatário)
>   - GET por ID: apenas dono da reserva ou dono do barco pode visualizar
>   - POST cancel: apenas dono da reserva pode cancelar
> - **Status da reserva:** PENDING, CONFIRMED, CANCELLED, FINISHED
> - **Pagamento:** Inclui mockCardData para ambiente de demonstração
> - **Preço total:** Calculado dinamicamente com base nas janelas de disponibilidade
> - **Implementação:** 
>   - `GET /api/bookings/{bookingId}` retorna 501 (Not Implemented)
>   - `POST /api/bookings/{bookingId}/cancel` retorna 501 (Not Implemented)
> - **Paginação:** `/my-bookings` suporta paginação com ordenação por data decrescente

## 💬 Messages

| Endpoint | Method | Access | Request Body | Response |
|----------|--------|--------|--------------|----------|
| `/api/messages` | POST | **Authenticated** (any role) | `{ "bookingId": 1, "boatId": null, "content": "Olá, tenho interesse no barco" }`<br>ou<br>`{ "bookingId": null, "boatId": 5, "content": "Olá, tenho interesse no barco" }` | `{ "id": 1, "content": "Olá, tenho interesse no barco", "sentAt": "2024-01-15T10:00:00", "sender": { "id": 1, "name": "João", "email": "joao@example.com" }, "bookingId": 1, "boatId": null }` |
| `/api/messages/booking/{bookingId}` | GET | **Authenticated** (booking owner or boat owner) | - | `[ { "id": 1, "content": "Olá, tenho interesse no barco", "sentAt": "2024-01-15T10:00:00", "sender": { "id": 1, "name": "João", "email": "joao@example.com" }, "bookingId": 1, "boatId": null } ]` |
| `/api/messages/boat/{boatId}` | GET | **Authenticated** (any role) | - | `[ { "id": 1, "content": "Olá, tenho interesse no barco", "sentAt": "2024-01-15T10:00:00", "sender": { "id": 1, "name": "João", "email": "joao@example.com" }, "bookingId": null, "boatId": 5 } ]` |

> **Observações:**
> - **Contexto:** As mensagens podem ser associadas a uma reserva (booking) ou a um barco (pré-reserva). Os campos `bookingId` e `boatId` são mutuamente exclusivos.
> - **Acesso:**
>   - POST: qualquer usuário autenticado pode enviar mensagens (o sistema associa o remetente automaticamente)
>   - GET por booking: apenas o dono da reserva (sailor) ou o dono do barco (boat owner) podem visualizar
>   - GET por boat: no MVP, qualquer usuário autenticado pode visualizar as mensagens do barco (pré-reserva)
> - **Limite de conteúdo:** A mensagem não pode exceder 2000 caracteres.
> - **Resposta:** Inclui o remetente (UserBasicDTO) e o timestamp de envio.


## 💳 Payments

| Endpoint | Method | Access | Request Body | Response |
|----------|--------|--------|--------------|----------|
| `/api/payments/booking` | POST | **Authenticated** (any role) | `{ "amount": 1400.00, "paymentMethod": "CREDIT_CARD", "mockCardData": { "cardNumber": "4111111111111111", "expiryMonth": "12", "expiryYear": "2025", "cvv": "123" }, "description": "Pagamento reserva barco Sea Ray 510", "bookingId": 1, "userEmail": "joao@example.com", "installments": 1 }` | `{ "success": true, "transactionId": "tx_123456", "status": "CONFIRMED", "gatewayMessage": "Payment successful", "errorMessage": null, "processedAt": "2024-01-15T10:00:00", "paymentId": 1, "amount": 1400.00, "paymentMethod": "CREDIT_CARD", "bookingId": 1, "boatId": null, "pixQrCode": null, "boletoUrl": null, "paymentUrl": null, "expiresAt": null }` |
| `/api/payments/direct` | POST | **Authenticated** (any role) | `{ "amount": 500.00, "paymentMethod": "PIX", "description": "Depósito para reserva", "boatId": 5, "userEmail": "joao@example.com", "installments": 1 }` | `{ "success": true, "transactionId": "tx_789012", "status": "PENDING", "gatewayMessage": "PIX QR Code generated", "errorMessage": null, "processedAt": "2024-01-15T10:05:00", "paymentId": 2, "amount": 500.00, "paymentMethod": "PIX", "bookingId": null, "boatId": 5, "pixQrCode": "000201010212...", "boletoUrl": null, "paymentUrl": null, "expiresAt": "2024-01-15T11:05:00" }` |
| `/api/payments/transaction/{transactionId}` | GET | **Authenticated** (any role) | - | `404 Not Found` (Não implementado) |
| `/api/payments/history` | GET | **Authenticated** (any role) | Query Params: `?page=0&size=20` | `200 OK` (Retorno vazio - Não implementado) |
| `/api/payments/webhook/pagseguro` | POST | **Public** (chamado pelo gateway) | Payload do gateway (string) e header `X-Signature` | `200 OK` (Não implementado) |
| `/api/payments/health` | GET | **Public** | - | `"Payment service is healthy"` |

> **Observações:**
> - **Acesso:** 
>   - Endpoints de processamento de pagamento exigem autenticação.
>   - O webhook é público, mas atualmente não implementado.
>   - Health check é público.
> - **Status de pagamento:** PENDING, PROCESSING, CONFIRMED, DECLINED, FAILED, CANCELLED, EXPIRED, REFUNDED, UNKNOWN.
> - **Métodos de pagamento:** CREDIT_CARD, PIX, BOLETO.
> - **Campos específicos por método:**
>   - CREDIT_CARD: requer `mockCardData` no ambiente de sandbox.
>   - PIX: retorna `pixQrCode` e `expiresAt`.
>   - BOLETO: retorna `boletoUrl` e `expiresAt`.
> - **Implementação:** 
>   - `GET /api/payments/transaction/{transactionId}` retorna 404 (Not Implemented)
>   - `GET /api/payments/history` retorna 200 com corpo vazio (Not Implemented)
>   - `POST /api/payments/webhook/pagseguro` retorna 200 (Not Implemented)
> - **Webhook:** O endpoint de webhook não valida assinatura no momento (não implementado).


## ⭐ Reviews

| Endpoint | Method | Access | Request Body | Response |
|----------|--------|--------|--------------|----------|
| `/api/v1/reviews` | POST | **Authenticated** (any role) | `{ "userID": 1, "boatId": 5, "rating": 5, "comment": "Excelente experiência, barco impecável!" }` | `{ "id": 1, "user": { "id": 1, "name": "João", "email": "joao@example.com" }, "boat": { "id": 5, "name": "Sea Ray 510", "type": "Motor Yacht", "capacity": 10, "pricePerHour": 350.00 }, "rating": 5, "comment": "Excelente experiência, barco impecável!", "createdAt": "2024-01-15T10:00:00", "updatedAt": null }` |
| `/api/v1/reviews/{reviewId}` | PUT | **Authenticated** (review owner) | `{ "userID": 1, "boatId": 5, "rating": 4, "comment": "Boa experiência, poderia melhorar alguns detalhes." }` | `{ "id": 1, "user": { "id": 1, "name": "João", "email": "joao@example.com" }, "boat": { "id": 5, "name": "Sea Ray 510", "type": "Motor Yacht", "capacity": 10, "pricePerHour": 350.00 }, "rating": 4, "comment": "Boa experiência, poderia melhorar alguns detalhes.", "createdAt": "2024-01-15T10:00:00", "updatedAt": "2024-01-15T11:00:00" }` |
| `/api/v1/reviews/{reviewId}` | GET | **Public** | - | `{ "id": 1, "user": { "id": 1, "name": "João", "email": "joao@example.com" }, "boat": { "id": 5, "name": "Sea Ray 510", "type": "Motor Yacht", "capacity": 10, "pricePerHour": 350.00 }, "rating": 5, "comment": "Excelente experiência, barco impecável!", "createdAt": "2024-01-15T10:00:00", "updatedAt": null }` |
| `/api/v1/reviews/boat/{boatId}` | GET | **Public** | - | `[ { "id": 1, "user": { "id": 1, "name": "João", "email": "joao@example.com" }, "boat": { "id": 5, "name": "Sea Ray 510", "type": "Motor Yacht", "capacity": 10, "pricePerHour": 350.00 }, "rating": 5, "comment": "Excelente experiência...", "createdAt": "2024-01-15T10:00:00" } ]` |
| `/api/v1/reviews/my-reviews` | GET | **Authenticated** (any role) | - | `[ { "id": 1, "user": { "id": 1, "name": "João", "email": "joao@example.com" }, "boat": { "id": 5, "name": "Sea Ray 510", "type": "Motor Yacht", "capacity": 10, "pricePerHour": 350.00 }, "rating": 5, "comment": "Excelente experiência...", "createdAt": "2024-01-15T10:00:00" } ]` |
| `/api/v1/reviews/{reviewId}` | DELETE | **Authenticated** (review owner or admin) | Query Param: `?isAdmin=false` (opcional) | `204 No Content` |
| `/api/v1/reviews/boat/{boatId}/stats` | GET | **Public** | - | `{ "averageRating": 4.5, "totalReviews": 10, "ratingDistribution": { "5": 6, "4": 3, "3": 1, "2": 0, "1": 0 } }` |

> **Observações:**
> - **Versão da API:** Este módulo usa a versão v1 (`/api/v1/reviews`) enquanto outros módulos não usam versionamento.
> - **Acesso:**
>   - POST: usuário autenticado cria avaliação (deve ser dono da reserva/compras do barco)
>   - PUT: apenas o dono da avaliação pode atualizar
>   - DELETE: dono da avaliação ou admin (parâmetro `isAdmin` pode ser usado)
>   - GET endpoints são públicos (exceto `my-reviews` que requer autenticação)
> - **Validação:**
>   - `rating` deve ser entre 1 e 5
>   - `comment` máximo 1000 caracteres
>   - `userID` e `boatId` são obrigatórios no request
> - **Estatísticas:** O endpoint `/stats` retorna média de avaliações, total e distribuição por estrelas
> - **Inconsistência:** O `ReviewRequestDTO` exige `userID`, mas o controlador usa o userId do token. Provavelmente o serviço valida que correspondem

## 🏠 Address

| Endpoint | Method | Access | Request Body | Response |
|----------|--------|--------|--------------|----------|
| `/api/address` | POST | **Authenticated** (any role) | `{ "cep": "20000-000", "number": "100", "street": "Rua Exemplo", "neighborhood": "Centro", "city": "Rio de Janeiro", "state": "RJ" }` | `{ "id": 1, "city": "Rio de Janeiro", "state": "RJ" }` |
| `/api/address` | GET | **Authenticated** (any role) | - | `[ { "id": 1, "city": "Rio de Janeiro", "state": "RJ" }, { "id": 2, "city": "São Paulo", "state": "SP" } ]` |
| `/api/address/{id}` | GET | **Authenticated** (any role) | - | `{ "id": 1, "city": "Rio de Janeiro", "state": "RJ" }` |
| `/api/address/private/{id}` | GET | **Authenticated** (any role) | - | `{ "id": 1, "cep": "20000-000", "number": "100", "street": "Rua Exemplo", "neighborhood": "Centro", "city": "Rio de Janeiro", "state": "RJ" }` |
| `/api/address/{id}` | PUT | **Authenticated** (any role) | `{ "cep": "20000-000", "number": "101", "street": "Rua Atualizada", "neighborhood": "Centro", "city": "Rio de Janeiro", "state": "RJ" }` | `{ "id": 1, "city": "Rio de Janeiro", "state": "RJ" }` |
| `/api/address/{id}` | DELETE | **Authenticated** (any role) | - | `204 No Content` |

> **Observações:**
> - **Acesso:** Todos os endpoints exigem autenticação (usuário logado com qualquer role)
> - **Duas respostas diferentes:** 
>   - `/api/address/{id}` retorna apenas `city` e `state` (público)
>   - `/api/address/private/{id}` retorna todos os campos do endereço (privado)
> - **Segurança:** Não há distinção de roles para endpoints de endereço
> - **Integração:** Endereços são normalmente criados/atualizados através dos endpoints de Boat

## 🩺 Health

| Endpoint | Method | Access | Request Body | Response |
|----------|--------|--------|--------------|----------|
| `/health` | GET | **Public** | - | `"UP"` (text/plain) |

> **Observações:**
> - **Acesso:** Público, sem autenticação necessária.
> - **Propósito:** Verificação de disponibilidade da aplicação (liveness probe).
> - **Uso:** Utilizado por balanceadores de carga, orquestradores de containers e sistemas de monitoramento.

## Notas do Desenvolvedor

Pontos de evolução pessoal nesse projeto:

* Entendimento maior sobre autenticação com JWT

   Pontos relevantes: 
        - cors;
        - Problemas com versões do Spring e JDK incompatíveis com algumas dependencias;

* Utilização de Basic DTOs para ocultar informações sensíveis
   
* Reduzir tempo de start:

   Remoção de dependencias redundantes;
   Trocar jdk por jre no Dockerfile
   Health check rodando antes da aplicação estar funcional (deploy)

* Configuração de variáveis de ambiente no inicio do projeto

* Compressão de imagens devido demora no upload poder quebrar o request (preferível aguardar o retorno que usar assincrono e não saber que ocorreu um erro)