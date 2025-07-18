# Resoconto del Problema di Eliminazione delle User Story con Sessioni di Planning Poker

## Il Problema

### Descrizione dell'Errore
Quando un utente tentava di eliminare una user story che aveva associata una sessione di planning poker, si verificava il seguente errore:

```
There was an unexpected error (type=Internal Server Error, status=500).
could not execute statement [ERROR: update or delete on table "user_stories" violates foreign key constraint "fk_estimation_votes_on_user_story" on table "estimation_votes" Dettaglio: Key (id)=(2) is still referenced from table "estimation_votes".] [delete from zenflow.user_stories where id=?]; SQL [delete from zenflow.user_stories where id=?]; constraint [fk_estimation_votes_on_user_story]
```

### Analisi del Problema
Questo errore si verificava a causa di un vincolo di integrità referenziale nel database. In particolare:

1. La tabella `estimation_votes` aveva un vincolo di chiave esterna (`fk_estimation_votes_on_user_story`) che faceva riferimento alla tabella `user_stories`
2. Quando si tentava di eliminare una user story, il database impediva l'operazione perché esistevano ancora record nella tabella `estimation_votes` che facevano riferimento a quella user story
3. Questi voti di stima erano parte di una sessione di planning poker associata alla user story

Le relazioni tra le entità coinvolte erano:
- `UserStory` → `PlanningPokerSession` (one-to-many)
- `PlanningPokerSession` → `EstimationVote` (one-to-many)
- `EstimationVote` → `UserStory` (many-to-one)

Il requisito era che quando una user story viene eliminata, anche le sessioni di planning poker associate e i relativi voti dovrebbero essere eliminati automaticamente.

## Approcci alla Soluzione

### Primo Approccio: Eliminazione Programmatica
Il primo approccio considerato prevedeva una soluzione programmatica nel service layer:

1. Aggiungere un metodo al repository `PlanningPokerSessionRepository` per trovare le sessioni per una determinata user story
2. Modificare il metodo `deleteById` in `UserStoryService` per:
   - Trovare tutte le sessioni di planning poker associate alla user story
   - Eliminare manualmente ciascuna sessione prima di eliminare la user story
   - Aggiornare i punti storia totali del progetto

Questo approccio avrebbe funzionato, ma richiedeva codice aggiuntivo e gestione manuale delle dipendenze.

### Approccio Scelto: Utilizzo di JPA Cascade Type
Il secondo approccio, che è stato implementato, utilizza le funzionalità di JPA per gestire automaticamente l'eliminazione a cascata:

1. Aggiungere una relazione bidirezionale tra `UserStory` e `PlanningPokerSession`
2. Configurare questa relazione con `cascade = CascadeType.ALL` e `orphanRemoval = true`
3. Lasciare che JPA gestisca automaticamente l'eliminazione delle entità correlate

## Implementazione della Soluzione

### Modifiche al Modello di Dominio
Abbiamo aggiunto una relazione bidirezionale in `UserStory.java`:

```java
@OneToMany(mappedBy = "userStory", cascade = CascadeType.ALL, orphanRemoval = true)
private final List<PlanningPokerSession> planningSessions = new ArrayList<>();
```

Questa annotazione indica a JPA che:
- Esiste una relazione one-to-many tra `UserStory` e `PlanningPokerSession`
- La relazione è mappata dal campo `userStory` nella classe `PlanningPokerSession`
- Tutte le operazioni (persist, merge, remove, refresh, detach) devono essere propagate alle entità figlie
- Le entità figlie orfane (non più referenziate) devono essere eliminate automaticamente

### Aggiunta di un Metodo al Repository
Abbiamo anche aggiunto un metodo al `PlanningPokerSessionRepository` per supportare la ricerca di sessioni per una user story:

```java
List<PlanningPokerSession> findByUserStory(UserStory userStory);
```

Questo metodo è utile per i test e per eventuali operazioni che richiedono di trovare le sessioni associate a una user story.

### Test dell'Implementazione
Abbiamo creato un test specifico (`testDeleteByIdWithCascadeDeletion`) per verificare che l'eliminazione a cascata funzioni correttamente:

1. Creazione di una user story con una sessione di planning poker associata
2. Chiamata al metodo `deleteById` del service
3. Verifica che la user story e, implicitamente, la sessione di planning poker siano state eliminate

## Perché Questa Soluzione è Migliore

### Vantaggi dell'Approccio Dichiarativo
L'utilizzo di annotazioni JPA per la gestione delle relazioni e delle operazioni a cascata offre diversi vantaggi:

1. **Approccio dichiarativo**: La relazione e il suo comportamento sono definiti in un unico punto
2. **Meno codice**: Non è necessaria una logica di eliminazione personalizzata nel service layer
3. **Coerenza**: JPA gestisce le operazioni a cascata in modo coerente e affidabile
4. **Sicurezza transazionale**: Tutte le operazioni avvengono all'interno della stessa transazione
5. **Manutenibilità**: Il codice è più pulito e più facile da mantenere

### Allineamento con le Best Practices di Spring Boot
Questa soluzione è in linea con le best practices di Spring Boot e JPA:

1. **Utilizzo delle funzionalità ORM**: Sfruttamento delle capacità di mapping oggetto-relazionale di JPA
2. **Incapsulamento della logica di persistenza**: La logica di eliminazione è gestita dal layer di persistenza
3. **Separazione delle responsabilità**: Ogni componente ha una responsabilità ben definita

## Conclusione

Il problema dell'eliminazione delle user story con sessioni di planning poker associate è stato risolto implementando una relazione bidirezionale con eliminazione a cascata utilizzando le annotazioni JPA. Questa soluzione è più elegante, richiede meno codice e segue le best practices di Spring Boot.

I test confermano che l'implementazione funziona correttamente: quando una user story viene eliminata, anche le sessioni di planning poker associate e i relativi voti vengono eliminati automaticamente, evitando violazioni dei vincoli di integrità referenziale del database.