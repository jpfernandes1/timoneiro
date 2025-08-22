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

### Perfis e Banco de Dados

Este projeto utiliza Spring Boot com perfis separados (dev, test e prod) 
para organizar as configurações de banco de dados de forma segura e prática. 

O objetivo é manter as credenciais fora do Git e facilitar o desenvolvimento local sem precisar 
setar variáveis de ambiente manualmente. 

Criamos o arquivo `application.yml` principal com configurações comuns a todos os perfis e
placeholders para produção, e importamos opcionalmente um arquivo `application-local.yml` 
para desenvolvimento local. 

O arquivo `application-local.yml` contém as credenciais de desenvolvimento local e está listado 
no `.gitignore` para não vazar senhas. 

Os perfis disponíveis são: dev (PostgreSQL local para desenvolvimento), 
test (H2 in-memory para testes automatizados) e prod (PostgreSQL de produção usando variáveis de ambiente).

Para rodar localmente, clone o repositório com `git clone <URL_DO_REPOSITORIO>` e entre na pasta com 
`cd timoneiro`. 

Em seguida, crie o arquivo `application-local.yml` na raiz do projeto com suas credenciais locais 
(não comitar), por exemplo:  
`spring:`  
`  profiles: dev`  
`  datasource:`  
`    url: jdbc:postgresql://localhost:5432/timoneiro_dev`  
`    username: usuario_dev`  
`    password: senha_dev`.  

Depois, rode a aplicação no perfil dev (padrão) com `mvn spring-boot:run`. 

O Spring Boot carregará automaticamente o `application-local.yml` e usará o banco de desenvolvimento. 

Para rodar outro perfil, como produção, use `java -jar timoneiro.jar --spring.profiles.active=prod`, 
garantindo que as variáveis de ambiente do servidor estejam definidas (PROD_DB_HOST, PROD_DB_USER, PROD_DB_PASS etc.).

Esta configuração traz vários benefícios: segurança (senhas não entram no Git), flexibilidade 
(perfis separados para dev, test e prod) e praticidade (desenvolvimento local sem precisar setar 
variáveis de ambiente toda vez).

## **Modelo de Banco de Dados**

### **Visão Geral**
O banco de dados do **timoneiro** foi projetado para suportar todas as funcionalidades principais do sistema, 
garantindo integridade dos dados, performance e escalabilidade. 

Utiliza princípios **relacionais**, com relacionamentos, índices e constraints adequados.

---

### **Entidades e Propósito**

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
    - Justificativa: Permite que o proprietário defina quando o barco está disponível, permitindo reservas somente em períodos válidos.

4. **Booking (Reserva)**
    - Registra as reservas feitas pelos usuários para barcos específicos.
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
