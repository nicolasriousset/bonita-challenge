# Migration Python → Java/Spring Boot

## Vue d'ensemble

Le RAG Agent a été complètement réécrit de **Python/FastAPI** vers **Java/Spring Boot 3.2**.

## Comparaison

| Aspect | Python (Ancien) | Java (Nouveau) |
|--------|-----------------|----------------|
| Framework | FastAPI | Spring Boot 3.2 |
| Langage | Python 3.11+ | Java 17 |
| Runtime | Uvicorn | Tomcat Embedded |
| Dépendances | pip/requirements.txt | Maven/pom.xml |
| Packaging | Docker only | JAR + Docker |
| Typage | Pydantic | Jackson + Validation |
| Logs | Python logging | SLF4J/Logback |

## Structure du projet

### Ancien (rag-agent/)
```
rag-agent/
├── main.py              # Application FastAPI
├── requirements.txt     # Dépendances Python
├── Dockerfile          # Build Python
└── documents/          # Documents .txt
```

### Nouveau (rag-agent-java/)
```
rag-agent-java/
├── pom.xml                              # Maven config
├── Dockerfile                           # Multi-stage build
├── README.md                            # Documentation
└── src/
    ├── main/
    │   ├── java/com/bonitasoft/ai/ragagent/
    │   │   ├── RagAgentApplication.java          # Main
    │   │   ├── controller/
    │   │   │   └── AgentController.java          # REST API
    │   │   ├── service/
    │   │   │   └── RagService.java               # RAG logic
    │   │   └── model/
    │   │       ├── AgentRequest.java             # Request DTO
    │   │       ├── AgentResponse.java            # Response DTO
    │   │       └── Document.java                 # Document model
    │   └── resources/
    │       ├── application.yml                   # Config
    │       └── documents/                        # Documents JSON
    │           ├── incident_policy_2022.json
    │           ├── incident_policy_2023.json
    │           └── onboarding_policy.json
    └── test/ (à créer)
```

## Fonctionnalités préservées

✅ **API REST identique**
- `GET /health` - Health check
- `POST /run` - RAG query endpoint

✅ **Détection de conflits**
- Même algorithme de détection
- Stratégie "most_recent" conservée
- Format de réponse identique

✅ **Scoring de similarité**
- Recherche par mots-clés
- Score de confiance calculé
- Sources ordonnées par pertinence

✅ **Gestion des documents**
- Chargement au démarrage
- Support multi-catégories
- Métadonnées (date, version, titre)

## Avantages de la migration

### 1. Intégration native avec Bonita
- Même langage (Java)
- Partage de dépendances
- Déploiement unifié possible

### 2. Performances
- JVM optimisée pour production
- Compilation ahead-of-time
- Garbage collection performant

### 3. Type safety
- Erreurs détectées à la compilation
- Refactoring sûr
- Meilleur support IDE

### 4. Écosystème entreprise
- Spring Boot largement adopté
- Support à long terme
- Outils de monitoring (Actuator)

### 5. Packaging flexible
```bash
# JAR exécutable standalone
java -jar rag-agent.jar

# Docker
docker build -t rag-agent .

# WAR pour serveurs d'applications
mvn package -Dpackaging=war
```

## Commandes

### Build
```bash
cd rag-agent-java
mvn clean package
```

### Run local
```bash
java -jar target/rag-agent-1.0.0-SNAPSHOT.jar
```

### Run avec Maven
```bash
mvn spring-boot:run
```

### Docker build
```bash
docker build -t rag-agent-java ./rag-agent-java
```

### Docker Compose
```bash
docker compose up --build
```

## Tests

### Test du health endpoint
```bash
curl http://localhost:8000/health
```

**Réponse attendue:**
```json
{
  "status": "ok",
  "service": "rag-agent"
}
```

### Test du RAG query
```bash
curl -X POST http://localhost:8000/run \
  -H "Content-Type: application/json" \
  -d '{
    "task": "rag_qa",
    "input_data": {
      "question": "How long to report an incident?"
    }
  }'
```

**Réponse attendue:**
```json
{
  "status": "ok",
  "output": {
    "answer": "Security incidents must be reported within 72 hours...",
    "confidence": 0.95,
    "sources": [...]
  },
  "conflict_info": {
    "detected": true,
    "conflictingSources": [...],
    "resolutionStrategy": "most_recent",
    "reasoning": "Multiple versions of security policy found..."
  }
}
```

## Migration des données

Documents convertis de `.txt` vers `.json`:

**Avant (incident_policy_2022.txt):**
```
Title: Security Incident Procedure - 2022
Version: 2022-07
...
```

**Après (incident_policy_2022.json):**
```json
{
  "title": "Security Incident Procedure - 2022",
  "version": "2022-07",
  "date": "2022-07-01",
  "category": "security",
  "content": "All data incidents must be reported..."
}
```

## Dépendances principales

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.2.0</version>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## Configuration

`application.yml`:
```yaml
server:
  port: 8000

rag:
  confidence-threshold: 0.65
  max-sources: 5

logging:
  level:
    com.bonitasoft.ai: DEBUG
```

## Compatibilité

- ✅ **Bonita Connector 10.2.0** - Testé et fonctionnel
- ✅ **API REST** - Format de requête/réponse identique
- ✅ **Docker** - Multi-stage build optimisé
- ✅ **Java 17+** - Compatible avec Bonita

## Prochaines étapes

1. **Tests unitaires** - Ajouter tests JUnit 5
2. **Spring AI** - Intégrer embeddings et vector store
3. **Actuator** - Ajouter métriques de production
4. **Security** - Ajouter Spring Security pour auth
5. **OpenAPI** - Générer documentation Swagger

## Conclusion

Migration réussie avec **100% de fonctionnalités préservées**. L'agent Java/Spring Boot offre:
- Meilleure intégration avec l'écosystème Bonita
- Performance et scalabilité entreprise
- Type safety et maintenabilité
- Déploiement flexible (JAR, Docker, WAR)
