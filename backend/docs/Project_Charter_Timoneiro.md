# 📋 Documento de Visão do Projeto - Plataforma de Aluguel de Barcos

## 🧑‍💻 História do Cliente (Problem Statement)

"Como entusiasta do mundo náutico e proprietário de uma lancha, sempre percebi o **desperdício** de embarcações paradas em marinas enquanto milhares de pessoas sonham em ter experiências no mar.

Meu barco fica ocioso 90% do tempo, mas o processo de aluguel é tão burocrático que desanima tanto proprietários quanto locatários. As poucas plataformas existentes são **ultrapassadas**, não oferecem **segurança jurídica** e têm **experiência do usuario pobre**.

Quero criar o 'Airbnb dos barcos' - uma plataforma que:
- Torne o aluguel **tão simples quanto reservar um Airbnb**
- Ofereça **segurança** para ambas as partes
- Democratize o acesso às experiências náuticas
- Gere **renda extra** para proprietários
- Promova o **turismo náutico** brasileiro"

## 📋 Visão Geral do Projeto
Plataforma digital que conecta proprietários de barcos com pessoas interessadas em alugar embarcações para passeios, pesca, eventos e experiências náuticas.

## 🎯 Objetivo Principal
Criar o "Airbnb dos barcos" - um marketplace seguro e intuitivo para aluguel de embarcações com sistema de reservas, pagamentos e avaliações.

---

### 🧑‍💼 Necessidades do Negócio
| Necessidade | Solução Proposta |
|------------|------------------|
| Reduzir ociosidade das embarcações | Sistema de calendário e gestão de disponibilidade |
| Garantir segurança nas transações | Sistema de verificação, seguros e contratos digitais |
| Simplificar processo de aluguel | Plataforma intuitiva mobile-first |
| Atrair diferentes perfis de usuários | Sistema de categorização e filtros inteligentes |
| Gerar confiança entre as partes | Sistema de avaliações bilateral e reputação |

## 👥 Exemplos de Personas e Jornadas

### Persona 1: Carlos, o Locador (45 anos)
**"Quero gerar renda extra com meu barco sem dor de cabeça"**
- Proprietário de lancha de 25 pés
- Usa o barco apenas em alguns finais de semana (subutilizado)
- Preocupado com segurança e danos à embarcação
- Não tem tempo para ficar negociando individualmente

**Jornada do Carlos:**
1. Cadastra barco com fotos e especificações ✅
2. Define calendário de disponibilidade e preços 📅
3. Recebe solicitações de reserva com perfil do locatário 👤
4. Aprova reserva com seguro automático 📝
5. Recebe pagamento seguro após o passeio 💰
6. Avalia locatário para comunidade ⭐

### Persona 2: Ana, a Locatária (32 anos)
**"Quero presentear meu marido com um passeio de barco surpresa"**
- Não entende de barcos mas quer experiência segura
- Preocupada com preços transparentes sem taxas escondidas
- Quer tudo resolvido pelo celular 📱
- Precisa de suporte em caso de problemas

**Jornada da Ana:**
1. Busca barcos disponíveis para data desejada 🔍
2. Filtra por preço, avaliações e equipamentos ⚙️
3. Reserva com pagamento seguro 💳
4. Recebe confirmação com todos os detalhes 📋
5. Tem experiência memorável no barco ⛵
6. Avalia o proprietário e compartilha nas redes 🌟

### Persona 3: Ricardo, locatário - o Líder do Grupo (28 anos)
**"Quero organizar uma festa inesquecível no mar para meus amigos"**
- Planeja eventos sociais com frequência
- Preocupado com custo-benefício para dividir entre o grupo
- Precisa de espaço suficiente para 10-15 pessoas
- Quer embarcação com som ambiente e geladeira
- Valoriza fotos bonitas para redes sociais

**Jornada do Ricardo:**
1. Busca barcos com boa avaliação para festas 🎉
2. Filtra por capacidade mínima de 12 pessoas 👥
3. Compara preços e negocia desconto para grupo 💰
4. Reserva com opção de pagamento dividido 🤝
5. Recebe checklist do que pode levar no barco 📋
6. Tem festa épica com todos os amigos ⛵
7. Comparte fotos e marca a plataforma 📸

### Persona 4: João, locatário - o Pescador Amador (45 anos)
**"Quero pescar em lugares que só se chega de barco"**
- Conhece as melhores épocas e locais para pesca
- Preocupado com equipamentos adequados (sonda, viveiro)
- Precisa de capitão que conheça os pontos pesqueiros
- Valoriza silêncio e horários específicos (madrugada)
- Quer preço justo por período prolongado (6-8 horas)

**Jornada do João:**
1. Busca barcos especializados em pesca 🎣
2. Filtra por equipamentos (sonda, vara, viveiro) ⚙️
3. Verifica se oferece serviço de capitão experiente 🧭
4. Reserva com horário personalizado (5h às 11h) ⏰
5. Leva sua própria equipagem preferida 🎒
6. Pega seus melhores peixes e registra o momento 📷
7. Avalia especialmente o conhecimento do capitão ⭐

## 🏗️ Escopo Funcional (MVP)

**Tempo estimado: 3 meses**

#### 🔐 Módulo de Autenticação
- [ ] Cadastro com roles: `USER`, `OWNER`, `ADMIN`
- [ ] Login social (Google, Facebook)
- [ ] Verificação em 2 etapas
- [ ] Login com JWT e refresh tokens
- [ ] Perfil completo com verificação (Upload de fotos e documentos - CNH, habilitação náutica)

#### 🚤 Módulo de Embarcações
- [ ] CRUD completo de barcos
- [ ] Upload múltiplo de fotos/vídeos pela web e celular
- [ ] Sistema de categorização: Lancha, Veleiro, Jet Ski, Catamarã, etc.
- [ ] Calendário de disponibilidade integrado
- [ ] Sistema de preços e taxas dinâmicos (hora, dia, temporada)

#### 📅 Módulo de Reservas
- [ ] Busca inteligente com filtros (localização, data, tipo, preço)
- [ ] Calendário em tempo real com disponibilidade
- [ ] Confirmações automáticas por email/SMS com termos e condições
- [ ] Dashboard de gestão

#### 💰 Módulo de Pagamentos
- [ ] Integração com gateway de pagamento (Stripe, PagSeguro)
- [ ] Pagamentos seguros com proteção ao consumidor
- [ ] Sistema de caução/depósito seguros
- [ ] Sistema de reembolsos
- [ ] Extrato financeiro, Recibos e comprovantes fiscais

#### ⭐ Sistema de Avaliações
- [ ] Avaliação bilateral (locatário ←→ proprietário)
- [ ] Ratings por categorias: embarcação, limpeza, comunicação
- [ ] Comentários moderados e verificados
- [ ] Sistema de confiança e reputação

## 🎨 Experiência do Usuário

### 📍 Onboarding
- Tour guiado pela plataforma
- Verificação em etapas (email, telefone, documentos)
- Personalização baseada no tipo de usuário

### 🔍 Sistema de Buscas
- Mapas interativos com barcos disponíveis
- Filtros avançados: preço, tipo, equipamentos, avaliações
- Busca por localização "perto de mim"
- Recomendações personalizadas

### 📱 Dashboard
- Painel do locatário: reservas, favoritos, histórico
- Painel do Locador: 
  - gestão de frota:
    - Occupancy rate por embarcação
  - finanças:
    - Seasonal trends e preços dinâmicos
    - Reports
  - calendário
  - Customer satisfaction ratings
- Painel admin: moderação, analytics, suporte

---

## ⚠️ Requisitos Legais e de Segurança

### 📝 Conformidade Legal
- Termos de uso e política de privacidade
- Contratos digitais assináveis
- Conformidade com leis marítimas locais
- Seguro obrigatório para todas as reservas

### 🔒 Segurança
- Verificação de identidade dos usuários
- Sistema de trust & safety
- Moderação de conteúdo e suporte 24/7
- Protocolos de emergência e suporte


## 🎯 Métricas de Sucesso (MVP)

### 📊 KPIs do MVP
| Métrica | Meta | Status |
|---------|------|--------|
| Tempo para cadastrar barco | < 10 minutos | ⏳ |
| Taxa de conversão busca→reserva | > 5% | ⏳ |
| Tempo médio de resposta | < 2 horas | ⏳ |
| Satisfação do usuário (NPS) | > 50 | ⏳ |

### 🚀 Go-to-Market Strategy
- Lançamento em 3 cidades litorâneas piloto
- Parceria com marinas e clubes náuticos
- Marketing digital segmentado

## 💰 Modelo de Negócio

### 📈 Revenue Streams
1. **Comissão por reserva** (15-20% do valor total)
2. **Serviços premium** (fotografia profissional, limpeza)
3. **Publicidade segmentada** (empresas náuticas)
4. **Assinaturas** para proprietários (recursos avançados)


## ⚠️ Riscos e Mitigações

### 🔴 Riscos Técnicos
- **Problemas de integração com pagamentos**
    - Mitigação: Multi-gateway (2 provedores mínimo)

### 🟡 Riscos Operacionais
- **Danos às embarcações**
    - Mitigação: Seguro obrigatório + caução digital

### 🟢 Riscos de Mercado
- **Sazonalidade do turismo náutico**
    - Mitigação: Expansão para diferentes regiões

---


### 🚦 Fluxo de Reserva
1. Busca → Filtros → Seleção → Datas → Confirmação → Pagamento → Confirmação → Check-in → Experiência → Check-out → Avaliação


## 💡 Diferenciais Competitivos

### 🎯 Versus Competidores Existentes
- ✅ Foco em experiência
- ✅ Sistema de verificação e segurança robusto
- ✅ Integração com provedores de serviços náuticos
- ✅ Modelo de preços transparente sem taxas ocultas
- ✅ Comunidade ativa com programas de fidelidade

### 🌊 Paralelo com Airbnb de Carros (Turo)
- 🚗 Carros: estacionamento fixo, vias públicas
- 🚤 Barcos: atracadouros específicos, regulamentação marítima
- 🚗 Seguro: automotivo convencional
- 🚤 Seguro: náutico especializado com cobertura de salvamento
- 🚗 Manutenção: oficinas terrestres
- 🚤 Manutenção: estaleiros náuticos especializados

---

## 📞 Suporte e Operações

### 🎪 Customer Success
- Suporte via chat, email, telefone
- Concierge para experiências premium
- Mediação de conflitos e resolução de problemas
- Programa de embaixadores e capitães parceiros

### ⚓ Operações Náuticas
- Parcerias com marinas e clubes náuticos
- Rede de provedores de serviços (limpeza, manutenção)
- Programas de treinamento para proprietários
- Certificação de embarcações e capitães

---

### 🔄 Metodologia Ágil
- Sprints de 2 semanas com demo ao final
- Reuniões diárias de 15min (daily standup)
- Review e planning a cada sprint
- Testes automatizados desde o início


### 🔧 Stack Tecnológica
- **Backend**: Spring Boot 3, Java 17, JPA/Hibernate
- **Frontend**: ReactJS
- **Database**: PostgreSQL com PostGIS para geolocalização
- **Cache**: Redis para sessões e performance
- **Search**: Elasticsearch para buscas avançadas
- **Storage**: AWS S3 para mídia e documentos
- **Mobile**: React Native (proximos passos)

*Documento criado em [15/08/2025] - Versão 1.0*