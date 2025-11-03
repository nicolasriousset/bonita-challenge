# 🔌 Installation du Connecteur AI Agent dans Bonita Studio

## Méthode Simple (Recommandée)

### 1. Compiler le connecteur

```powershell
cd bonita-connector-ai-agent
mvn clean package -DskipTests
```

Le JAR est créé dans : `bonita-connector-ai-agent/target/bonita-connector-ai-agent-1.0.0-SNAPSHOT-jar-with-dependencies.jar`

### 2. Importer dans Bonita Studio

1. **Ouvrez Bonita Studio**
2. Allez dans **New > Extensions > Add a custom extension > connector**
3. Sélectionnez le fichier JAR :
   ```
   bonita-connector-ai-agent/target/bonita-connector-ai-agent-1.0.0-SNAPSHOT-jar-with-dependencies.jar
   ```
4. Cliquez sur **Import** ou **OK**

✅ Le connecteur apparaît maintenant dans la catégorie **"AI"** !

### 3. Démarrer le RAG Agent

```powershell
cd rag-agent-java
java -jar target/rag-agent-1.0.0-SNAPSHOT.jar
```

L'agent écoute sur `http://localhost:8000`

### 4. Tester le connecteur

```powershell
.\test-connector.ps1
```

## Configuration dans Bonita

Quand vous ajoutez le connecteur sur une tâche :

| Paramètre | Valeur | Description |
|-----------|--------|-------------|
| **Agent URL** | `http://localhost:8000/run` | URL de l'agent RAG |
| **Task** | `rag_qa` | Type de tâche (Q&A, summarize, classify) |
| **Input Data** | `{"question": "What is the onboarding deadline?"}` | Données d'entrée en JSON |
| **Params** | `{"top_k": 3, "min_confidence": 0.65}` | Paramètres optionnels |

### Variables de sortie

Le connecteur retourne :

- **status** : `ok`, `low_confidence`, ou `error`
- **output** : Réponse de l'agent (JSON) contenant :
  - `answer` : La réponse générée
  - `confidence` : Niveau de confiance
  - `sources` : Documents sources utilisés
- **usage** : Métriques de performance
- **error** : Message d'erreur (si status = error)

## Exemple de processus Bonita

1. **Créez une tâche** dans votre diagramme
2. **Ajoutez le connecteur** "AI Agent Connector"
3. **Configurez les paramètres** comme ci-dessus
4. **Mappez les sorties** vers des variables de processus
5. **Utilisez la réponse** dans les tâches suivantes

## Dépannage

### Le connecteur n'apparaît pas

1. Vérifiez que le JAR contient `.def` et `.impl` :
   ```powershell
   jar -tf target\bonita-connector-ai-agent-1.0.0-SNAPSHOT-jar-with-dependencies.jar | Select-String "\.def|\.impl"
   ```

2. Redémarrez Bonita Studio : **Help > Restart**

### Erreur de connexion à l'agent

Vérifiez que le RAG Agent est démarré :
```powershell
curl http://localhost:8000/health
```

Devrait retourner : `{"status":"ok"}`

## Architecture

```
Bonita Process
    ↓
[AI Agent Connector]  ← (HTTP)
    ↓
[RAG Agent Java]
    ↓
[Vector Store + Documents]
    ↓
[Conflict Detection & Resolution]
```

## Support

- Logs Bonita : `C:\BonitaStudioCommunity-2024.3-u0\workspace\.metadata\.log`
- Logs Agent : Console Java ou `rag-agent-java/logs/`
