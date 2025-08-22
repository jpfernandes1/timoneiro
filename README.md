# Timoneiro - Configuração de Perfis e Banco de Dados

Este projeto utiliza Spring Boot com perfis separados (dev, test e prod) para organizar as configurações de banco de dados de forma segura e prática. O objetivo é manter as credenciais fora do Git e facilitar o desenvolvimento local sem precisar setar variáveis de ambiente manualmente. Criamos o arquivo `application.yml` principal com configurações comuns a todos os perfis e placeholders para produção, e importamos opcionalmente um arquivo `application-local.yml` para desenvolvimento local. O arquivo `application-local.yml` contém as credenciais de desenvolvimento local e está listado no `.gitignore` para não vazar senhas. Os perfis disponíveis são: dev (PostgreSQL local para desenvolvimento), test (H2 in-memory para testes automatizados) e prod (PostgreSQL de produção usando variáveis de ambiente).

Para rodar localmente, clone o repositório com `git clone <URL_DO_REPOSITORIO>` e entre na pasta com `cd timoneiro`. Em seguida, crie o arquivo `application-local.yml` na raiz do projeto com suas credenciais locais (não comitar), por exemplo:  
`spring:`  
`  profiles: dev`  
`  datasource:`  
`    url: jdbc:postgresql://localhost:5432/timoneiro_dev`  
`    username: usuario_dev`  
`    password: senha_dev`.  
Depois, rode a aplicação no perfil dev (padrão) com `mvn spring-boot:run`. O Spring Boot carregará automaticamente o `application-local.yml` e usará o banco de desenvolvimento. Para rodar outro perfil, como produção, use `java -jar timoneiro.jar --spring.profiles.active=prod`, garantindo que as variáveis de ambiente do servidor estejam definidas (PROD_DB_HOST, PROD_DB_USER, PROD_DB_PASS etc.).

Esta configuração traz vários benefícios: segurança (senhas não entram no Git), flexibilidade (perfis separados para dev, test e prod) e praticidade (desenvolvimento local sem precisar setar variáveis de ambiente toda vez).
