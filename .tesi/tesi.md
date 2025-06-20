# Indice

## 1. Introduzione

### 1.1 Obiettivo del progetto
**ZenFlow** è una web app pensata per semplificare la gestione dei progetti di sviluppo, offrendo supporto a tech lead e software architects nella stesura delle user story.  
Include strumenti come:
- Calcolo delle stime (PERT, Planning Poker)
- Timeline delle attività
- Gestione del personale coinvolto

### 1.2 Motivazioni della scelta tecnologica
La scelta delle tecnologie per ZenFlow è stata guidata dalla necessità di creare un'applicazione robusta, manutenibile e scalabile:

- **Spring Boot**: Framework che semplifica lo sviluppo di applicazioni Java enterprise-ready, offrendo configurazione automatica e un ecosistema completo di strumenti
- **JPA/Hibernate**: Per la persistenza dei dati e l'ORM, permettendo di lavorare con oggetti Java anziché query SQL dirette
- **PostgreSQL**: Database relazionale robusto con supporto per transazioni complesse e integrità referenziale
- **Flyway**: Per la gestione delle migrazioni del database, garantendo consistenza tra ambienti diversi
- **Thymeleaf**: Template engine per le viste che permette di creare HTML dinamico mantenendo template validi anche come file statici
- **Spring Security**: Per implementare un sistema di autenticazione e autorizzazione robusto basato su ruoli (RBAC)
- **Bootstrap**: Framework CSS per un'interfaccia utente responsive e moderna
- **Lombok**: Per ridurre il codice boilerplate e migliorare la leggibilità
- **MapStruct**: Per la mappatura efficiente tra entità e DTO senza overhead a runtime

### 1.3 Breve overview della soluzione sviluppata
ZenFlow è una soluzione completa per la gestione di progetti Agile che include:

- **Gestione di progetti e team**: Creazione e configurazione di progetti, assegnazione di membri al team con ruoli specifici
- **Backlog di user story**: Creazione, prioritizzazione e gestione delle user story
- **Pianificazione e monitoraggio di sprint**: Definizione di sprint con date di inizio/fine e monitoraggio dell'avanzamento
- **Stima delle user story**: Supporto per Planning Poker collaborativo e metodologia PERT per stime più accurate
- **Sistema RBAC**: Controllo degli accessi basato su ruoli per garantire che ogni utente possa accedere solo alle funzionalità appropriate

## 2. Background teorico

### 2.1 ORM: cos'è e perché usarlo
L'Object-Relational Mapping (ORM) risolve il problema dell'impedance mismatch tra il modello a oggetti e il modello relazionale, permettendo di:

- **Lavorare con oggetti Java**: Manipolare dati attraverso oggetti e metodi Java anziché query SQL
- **Gestire automaticamente le relazioni**: Mappare relazioni complesse tra entità (one-to-many, many-to-many, ecc.)
- **Semplificare le operazioni CRUD**: Ridurre il codice necessario per operazioni di base sul database
- **Migliorare la manutenibilità**: Centralizzare la logica di accesso ai dati e ridurre la duplicazione del codice
- **Ottimizzare le performance**: Attraverso caching, lazy loading e altre strategie

### 2.2 Introduzione a JPA, Spring Framework, Spring Data
Il progetto utilizza un ecosistema di tecnologie Java complementari:

- **JPA (Java Persistence API)**: Standard Java per l'ORM che definisce come mappare oggetti Java a tabelle di database relazionali
- **Hibernate**: Implementazione di JPA utilizzata nel progetto, che fornisce funzionalità avanzate come caching, lazy loading e ottimizzazione delle query
- **Spring Framework**: Ecosistema completo per lo sviluppo di applicazioni enterprise, basato su dependency injection e inversion of control
- **Spring Data**: Semplifica l'accesso ai dati fornendo repository predefiniti e query method derivate dai nomi dei metodi
- **Spring Boot**: Semplifica la configurazione e il deployment di applicazioni Spring, con auto-configurazione e starter dependencies

### 2.3 Database relazionali e mapping oggetto-relazionale
Nel progetto, il mapping tra oggetti Java e database relazionale è realizzato attraverso:

- **Entità con annotazioni JPA**: Classi Java annotate con `@Entity` e `@Table` per mappare oggetti a tabelle
- **Relazioni tra entità**: Uso di annotazioni come `@OneToMany`, `@ManyToOne`, `@ManyToMany` per definire relazioni
- **Strategie di fetch**: Configurazione di caricamento LAZY (on-demand) vs EAGER (immediato) per ottimizzare le performance
- **Cascading delle operazioni**: Propagazione automatica di operazioni (persist, merge, remove) alle entità correlate
- **Gestione delle transazioni**: Definizione di confini transazionali chiari per garantire l'integrità dei dati

### 2.4 Pattern architetturali adottati
ZenFlow implementa un'architettura a layer ben definiti:

- **Presentation Layer**: Controller Spring MVC e viste Thymeleaf per l'interfaccia utente
- **Service Layer**: Componenti che implementano la logica di business, con transazioni ben definite
- **Repository Layer**: Interfacce Spring Data JPA per l'accesso ai dati
- **Domain Layer**: Entità JPA e modelli di dominio che rappresentano i concetti chiave dell'applicazione
- **Security Layer**: Configurazione Spring Security per autenticazione e autorizzazione basata su ruoli

Seguendo le best practices di Spring Boot, il progetto adotta:
- Constructor injection per le dipendenze
- Package-private visibility per componenti interni
- Configurazione tipizzata con `@ConfigurationProperties`
- Transazioni ben definite con `@Transactional`
- Disabilitazione di Open Session in View
- Separazione tra layer web e persistenza con DTO

## 3. Analisi requisiti

### 3.1 Requisiti funzionali
ZenFlow implementa i seguenti requisiti funzionali:

- **Gestione utenti e ruoli**:
  - Registrazione e autenticazione utenti
  - Gestione dei ruoli e permessi (RBAC)
  - Profili utente personalizzabili

- **Creazione e gestione progetti**:
  - Creazione di nuovi progetti con metadati
  - Assegnazione di team members con ruoli specifici
  - Dashboard di progetto con metriche e stato

- **Gestione del backlog di user story**:
  - Creazione e modifica di user story
  - Prioritizzazione del backlog
  - Categorizzazione con tag e label

- **Pianificazione e monitoraggio sprint**:
  - Definizione di sprint con date di inizio/fine
  - Assegnazione di user story agli sprint
  - Monitoraggio dell'avanzamento con burndown chart

- **Stima delle user story**:
  - Planning Poker collaborativo in tempo reale
  - Calcolo di stime PERT (Pessimistica, Ottimistica, Più probabile)
  - Storico delle stime per analisi

- **Assegnazione e tracciamento task**:
  - Scomposizione di user story in task
  - Assegnazione di task ai membri del team
  - Aggiornamento dello stato di avanzamento

- **Calcolo metriche di progetto**:
  - Velocità del team
  - Burndown e burnup chart
  - Previsioni di completamento

### 3.2 Requisiti non funzionali
L'applicazione soddisfa i seguenti requisiti non funzionali:

- **Sicurezza**:
  - Autenticazione robusta
  - Autorizzazione basata su ruoli
  - Protezione contro vulnerabilità comuni (CSRF, XSS)

- **Usabilità e interfaccia**:
  - Design responsive per desktop e mobile
  - Interfaccia intuitiva con feedback immediato
  - Accessibilità secondo standard WCAG

- **Persistenza e integrità dei dati**:
  - Transazioni ACID per garantire consistenza
  - Validazione dei dati in input
  - Backup e recovery

- **Audit trail**:
  - Tracciamento delle modifiche con Hibernate Envers
  - Log delle operazioni critiche
  - Storico delle versioni per entità importanti

- **Scalabilità e performance**:
  - Ottimizzazione delle query database
  - Caching appropriato
  - Paginazione per grandi set di dati

### 3.3 Diagrammi UML
Per la progettazione del sistema sono stati realizzati i seguenti diagrammi:

- **Diagramma delle classi**: Rappresentazione delle entità principali e delle loro relazioni
- **Diagramma ER del database**: Schema del database relazionale
- **Diagrammi di sequenza**: Per i principali casi d'uso (es. creazione progetto, planning poker)
- **Diagrammi di attività**: Per workflow complessi come il processo di stima delle user story

## 4. Progettazione del sistema

### 4.1 Architettura dell'applicazione
ZenFlow implementa un'architettura MVC (Model-View-Controller) basata su Spring Boot:

- **Model**: Entità JPA che rappresentano il dominio dell'applicazione
- **View**: Template Thymeleaf che generano l'interfaccia HTML
- **Controller**: Classi Spring MVC che gestiscono le richieste HTTP

L'architettura segue il principio di separazione delle responsabilità, con componenti specializzati per ogni aspetto dell'applicazione.

### 4.2 Database design
Lo schema del database, gestito attraverso migrazioni Flyway, include:

- **Tabelle principali**:
  - `users`, `roles`, `permissions`: Per il sistema RBAC
  - `projects`, `sprints`, `user_stories`, `tasks`: Per il core dell'applicazione
  - `planning_poker_sessions`, `planning_poker_votes`: Per le funzionalità di stima

- **Tabelle di relazione**:
  - `user_roles`, `role_permissions`: Per le relazioni many-to-many del sistema RBAC
  - `project_team_members`: Per associare utenti ai progetti con ruoli specifici

- **Tabelle di audit**:
  - Tabelle con suffisso `_aud` generate da Hibernate Envers per tracciare le modifiche

### 4.3 Spiegazione entity e relazioni
Le principali entità del sistema e le loro relazioni sono:

- **User, Role, Permission**:
  - Sistema RBAC con utenti che hanno ruoli, e ruoli che hanno permessi
  - Relazioni many-to-many gestite con tabelle di join

- **Project**:
  - Entità centrale con relazioni verso User (owner, team members)
  - Contiene metadati del progetto e riferimenti a sprint e user story

- **Sprint**:
  - Periodi di lavoro con date di inizio/fine
  - Relazione many-to-one con Project e one-to-many con UserStory

- **UserStory**:
  - Requisiti utente con stime e priorità
  - Relazioni con Project, Sprint e Task

- **Task**:
  - Attività concrete per implementare le user story
  - Relazione many-to-one con UserStory e User (assignee)

- **PlanningPokerSession**:
  - Sessioni di stima collaborativa
  - Relazioni con UserStory e User (partecipanti)

### 4.4 Layer logici
L'applicazione è organizzata in layer logici ben definiti:

- **Controller**: Gestione delle richieste HTTP, validazione input, rendering delle viste
  - Package `it.zenflow.controller`
  - Implementa pattern PRG (Post-Redirect-Get) per form submission

- **Service**: Logica di business con transazioni ben definite
  - Package `it.zenflow.service`
  - Metodi annotati con `@Transactional` o `@Transactional(readOnly = true)`

- **Repository**: Accesso ai dati tramite Spring Data JPA
  - Package `it.zenflow.repository`
  - Interfacce che estendono `JpaRepository` con query methods

- **DTO**: Oggetti per il trasferimento dati tra layer
  - Package `it.zenflow.dto`
  - Record Java con validazione Jakarta Bean Validation

- **Mapper**: Conversione tra entità e DTO
  - Implementati con MapStruct per generazione efficiente a compile-time

## 5. Implementazione

### 5.1 Tecnologie utilizzate
Il progetto utilizza le seguenti tecnologie e librerie:

- **Spring Boot 3.5.0**: Framework per applicazioni Java
- **Java 24**: Linguaggio di programmazione con features moderne (records, pattern matching)
- **PostgreSQL/H2**: Database relazionali (produzione/test)
- **Flyway**: Gestione delle migrazioni del database
- **Hibernate/JPA**: ORM per la persistenza
- **Hibernate Envers**: Audit trail delle modifiche
- **Spring Security**: Framework per autenticazione e autorizzazione
- **Thymeleaf**: Template engine per le viste
- **Bootstrap**: Framework CSS per UI responsive
- **Lombok**: Riduzione del boilerplate
- **MapStruct**: Mappatura tra oggetti
- **Spring Boot Docker Compose**: Integrazione con Docker per sviluppo e test

### 5.2 Esempi di codice
Il progetto include esempi significativi di:

- **Definizione di entità con JPA**: Classi con annotazioni JPA per mapping ORM

```java
@Entity
@Table(name = "user_stories")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoryStatus status = StoryStatus.BACKLOG;

    // Relazioni
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "userStory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();
}
```

- **Implementazione di repository**: Interfacce Spring Data con query methods

```java
@Repository
public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

    List<UserStory> findByProject(Project project);

    List<UserStory> findByProjectAndStatus(Project project, StoryStatus status);

    @Query("SELECT us FROM UserStory us WHERE us.project.id = :projectId")
    List<UserStory> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT us FROM UserStory us LEFT JOIN FETCH us.tasks WHERE us.id = :id")
    Optional<UserStory> findByIdWithTasks(@Param("id") Long id);
}
```

- **Servizi con logica di business**: Componenti `@Service` con transazioni ben definite

```java
@Service
@RequiredArgsConstructor
public class UserStoryService {

    private final UserStoryRepository userStoryRepository;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public Optional<UserStory> findById(Long id) {
        return userStoryRepository.findById(id);
    }

    @Transactional
    public UserStory save(UserStory userStory) {
        // Calcolo della stima PERT se necessario
        if (userStory.getEstimationType() == EstimationType.PERT) {
            if (userStory.getOptimisticEstimate() != null && 
                userStory.getPessimisticEstimate() != null && 
                userStory.getMostLikelyEstimate() != null) {

                double pertEstimate = (userStory.getOptimisticEstimate() + 
                                      (4 * userStory.getMostLikelyEstimate()) + 
                                      userStory.getPessimisticEstimate()) / 6;

                userStory.setPertEstimate(pertEstimate);
                userStory.setStoryPoints((int) Math.round(pertEstimate));
            }
        }

        return userStoryRepository.save(userStory);
    }
}
```

- **Controller MVC**: Gestione delle richieste HTTP e rendering delle viste

```java
@Controller
@RequestMapping("/projects/{projectId}/user-stories")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class UserStoryController {

    private final UserStoryService userStoryService;
    private final ProjectService projectService;
    private final MessageSource messageSource;

    @GetMapping("")
    @PreAuthorize("hasAuthority('READ_USER_STORY')")
    public String listUserStories(@PathVariable Long projectId, Model model) {
        return projectService.findById(projectId)
                .map(project -> {
                    List<UserStory> userStories = userStoryService.findByProject(project);
                    model.addAttribute("project", project);
                    model.addAttribute("userStories", userStories);
                    return "projects/user-stories/list";
                })
                .orElse("redirect:/projects");
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_USER_STORY')")
    public String createUserStory(
            @PathVariable Long projectId,
            @Valid @ModelAttribute UserStoryDTO userStoryDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "projects/user-stories/form";
        }

        // Conversione da DTO a entità e salvataggio
        // ...

        String message = messageSource.getMessage("userstory.created", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);

        return "redirect:/projects/" + projectId + "/user-stories";
    }
}
```

- **DTO per separazione dei layer**: Oggetti per il trasferimento dati con validazione

```java
public class UserStoryDTO {

    private Long id;

    @NotBlank(message = "{validation.userstory.title.required}")
    @Size(min = 3, max = 200, message = "{validation.userstory.title.size}")
    private String title;

    @Size(max = 2000, message = "{validation.userstory.description.size}")
    private String description;

    @NotNull(message = "{validation.userstory.status.required}")
    private StoryStatus status = StoryStatus.BACKLOG;

    private Long projectId;
    private Long assignedToId;

    // PERT Estimation fields
    private Double optimisticEstimate;
    private Double pessimisticEstimate;
    private Double mostLikelyEstimate;
}
```

- **Configurazione di sicurezza**: Setup di Spring Security con RBAC

### 5.3 Come viene gestita la persistenza
La persistenza dei dati è gestita attraverso:

- **Spring Data JPA**: Repository predefiniti con metodi CRUD e query methods
- **Configurazione di Hibernate**: Ottimizzazioni per performance e consistenza
- **Gestione delle transazioni**: Boundaries transazionali ben definiti nei service
- **Migrazioni del database**: Script Flyway per evoluzione controllata dello schema
- **Audit trail**: Hibernate Envers per tracciare modifiche alle entità

### 5.4 Test unitari e di integrazione
L'approccio ai test include:

- **Test unitari**: JUnit 5 con Mockito per testare componenti isolati
- **Test di integrazione**: TestContainers per test con database reali in container Docker
- **Test di sicurezza**: Verifica delle regole di autorizzazione e autenticazione
- **Test end-to-end**: Simulazione di scenari utente completi

## 6. Discussione e considerazioni

### 6.1 Problemi incontrati
Durante lo sviluppo sono state affrontate diverse sfide:

- **Gestione delle relazioni JPA**: Configurazione corretta di fetch type e cascading
- **Configurazione della sicurezza**: Implementazione di un sistema RBAC flessibile
- **Implementazione dell'audit trail**: Setup di Hibernate Envers per tracciare le modifiche
- **Ottimizzazione delle query**: Risoluzione di problemi N+1 e performance
- **Eliminazione a cascata delle entità correlate**: Risoluzione di vincoli di integrità referenziale
- **Aggiornamento dei punti storia del progetto**: Risoluzione di un problema con il calcolo dei punti totali

#### 6.1.1 Problema di eliminazione delle User Story con sessioni di Planning Poker

Un problema significativo è emerso quando si tentava di eliminare una user story che aveva associata una sessione di planning poker. Il sistema generava il seguente errore:

```
There was an unexpected error (type=Internal Server Error, status=500).
could not execute statement [ERROR: update or delete on table "user_stories" violates foreign key constraint "fk_estimation_votes_on_user_story" on table "estimation_votes"]
```

Questo errore si verificava a causa di un vincolo di integrità referenziale nel database. In particolare:

1. La tabella `estimation_votes` aveva un vincolo di chiave esterna (`fk_estimation_votes_on_user_story`) che faceva riferimento alla tabella `user_stories`
2. Quando si tentava di eliminare una user story, il database impediva l'operazione perché esistevano ancora record nella tabella `estimation_votes` che facevano riferimento a quella user story
3. Questi voti di stima erano parte di una sessione di planning poker associata alla user story

Le relazioni tra le entità coinvolte erano:
- `UserStory` → `PlanningPokerSession` (one-to-many)
- `PlanningPokerSession` → `EstimationVote` (one-to-many)
- `EstimationVote` → `UserStory` (many-to-one)

Il requisito era che quando una user story viene eliminata, anche le sessioni di planning poker associate e i relativi voti dovrebbero essere eliminati automaticamente.

##### Approcci alla Soluzione

###### Primo Approccio: Eliminazione Programmatica
Il primo approccio considerato prevedeva una soluzione programmatica nel service layer:

1. Aggiungere un metodo al repository `PlanningPokerSessionRepository` per trovare le sessioni per una determinata user story
2. Modificare il metodo `deleteById` in `UserStoryService` per:
   - Trovare tutte le sessioni di planning poker associate alla user story
   - Eliminare manualmente ciascuna sessione prima di eliminare la user story
   - Aggiornare i punti storia totali del progetto

Questo approccio avrebbe funzionato, ma richiedeva codice aggiuntivo e gestione manuale delle dipendenze.

###### Approccio Scelto: Utilizzo di JPA Cascade Type
Il secondo approccio, che è stato implementato, utilizza le funzionalità di JPA per gestire automaticamente l'eliminazione a cascata:

1. Aggiungere una relazione bidirezionale tra `UserStory` e `PlanningPokerSession`
2. Configurare questa relazione con `cascade = CascadeType.ALL` e `orphanRemoval = true`
3. Lasciare che JPA gestisca automaticamente l'eliminazione delle entità correlate

##### Implementazione della Soluzione

Abbiamo aggiunto una relazione bidirezionale in `UserStory.java`:

```java
@OneToMany(mappedBy = "userStory", cascade = CascadeType.ALL, orphanRemoval = true)
private List<PlanningPokerSession> planningSessions = new ArrayList<>();
```

Questa annotazione indica a JPA che:
- Esiste una relazione one-to-many tra `UserStory` e `PlanningPokerSession`
- La relazione è mappata dal campo `userStory` nella classe `PlanningPokerSession`
- Tutte le operazioni (persist, merge, remove, refresh, detach) devono essere propagate alle entità figlie
- Le entità figlie orfane (non più referenziate) devono essere eliminate automaticamente

Abbiamo anche aggiunto un metodo al `PlanningPokerSessionRepository` per supportare la ricerca di sessioni per una user story:

```java
List<PlanningPokerSession> findByUserStory(UserStory userStory);
```

##### Vantaggi dell'Approccio Dichiarativo
L'utilizzo di annotazioni JPA per la gestione delle relazioni e delle operazioni a cascata offre diversi vantaggi:

1. **Approccio dichiarativo**: La relazione e il suo comportamento sono definiti in un unico punto
2. **Meno codice**: Non è necessaria una logica di eliminazione personalizzata nel service layer
3. **Coerenza**: JPA gestisce le operazioni a cascata in modo coerente e affidabile
4. **Sicurezza transazionale**: Tutte le operazioni avvengono all'interno della stessa transazione
5. **Manutenibilità**: Il codice è più pulito e più facile da mantenere

Questa soluzione è in linea con le best practices di Spring Boot e JPA, sfruttando le capacità di mapping oggetto-relazionale di JPA e incapsulando la logica di eliminazione nel layer di persistenza.

#### 6.1.2 Problema di aggiornamento dei punti storia totali del progetto

Un altro problema significativo è emerso durante l'utilizzo delle sessioni di Planning Poker. Dopo aver completato una sessione di Planning Poker e assegnato i punti storia a una user story, il totale dei punti storia del progetto non veniva aggiornato correttamente. Questo causava una discrepanza tra la somma effettiva dei punti storia di tutte le user story e il valore visualizzato nella dashboard del progetto.

##### Analisi del Problema

Per comprendere il problema, abbiamo eseguito i seguenti test:

1. Creazione di una user story con 3 punti storia
2. Verifica che il progetto mostrasse correttamente un totale di 3 punti storia
3. Creazione di una nuova user story e avvio di una sessione di Planning Poker
4. Completamento della sessione con 13 punti storia
5. Verifica che il progetto continuasse a mostrare solo 3 punti storia invece di 16

Analizzando il codice, abbiamo identificato la causa principale:

```java
// In PlanningPokerSessionService.java
private void processVotesAndUpdateUserStories(PlanningPokerSession session) {
    // ... codice per calcolare i punti storia ...

    if (userStory.getEstimationType() == EstimationType.STORY_POINTS) {
        int finalEstimate = calculateFinalStoryPoints(votes);
        userStory.setStoryPoints(finalEstimate);
    } else if (userStory.getEstimationType() == EstimationType.PERT) {
        calculatePERTEstimates(votes, userStory);
    }

    userStoryRepository.save(userStory);  // Chiamata diretta al repository
}
```

Il problema era che il metodo `processVotesAndUpdateUserStories()` nel `PlanningPokerSessionService` chiamava direttamente `userStoryRepository.save(userStory)` invece di utilizzare `userStoryService.save(userStory)`.

Nel `UserStoryService`, il metodo `save()` conteneva la logica per aggiornare il totale dei punti storia del progetto:

```java
@Transactional
public UserStory save(UserStory userStory) {
    // ... altro codice ...

    // Salva la user story
    UserStory savedUserStory = userStoryRepository.save(userStory);

    // Aggiorna il totale dei punti storia del progetto
    Project project = userStory.getProject();
    if (project != null) {
        List<UserStory> allStories = userStoryRepository.findByProject(project);
        int totalPoints = allStories.stream()
            .filter(story -> story.getStoryPoints() != null)
            .mapToInt(UserStory::getStoryPoints)
            .sum();

        project.setTotalStoryPoints(totalPoints);
        projectService.save(project);
    }

    return savedUserStory;
}
```

Chiamando direttamente il repository, questa logica di aggiornamento veniva bypassata, causando la discrepanza nei punti storia totali.

##### Soluzione Implementata

La soluzione è stata semplice ma efficace:

1. Aggiungere `UserStoryService` come dipendenza in `PlanningPokerSessionService`
2. Modificare il metodo `processVotesAndUpdateUserStories()` per utilizzare `userStoryService.save(userStory)` invece di `userStoryRepository.save(userStory)`

```java
// Modifica in PlanningPokerSessionService.java
private void processVotesAndUpdateUserStories(PlanningPokerSession session) {
    // ... codice esistente ...

    if (userStory.getEstimationType() == EstimationType.STORY_POINTS) {
        int finalEstimate = calculateFinalStoryPoints(votes);
        userStory.setStoryPoints(finalEstimate);
    } else if (userStory.getEstimationType() == EstimationType.PERT) {
        calculatePERTEstimates(votes, userStory);
    }

    userStoryService.save(userStory);  // Chiamata al service invece che al repository
}
```

##### Lezioni Apprese

Questo problema ha evidenziato l'importanza di:

1. **Rispettare i confini dei layer architetturali**: I controller dovrebbero chiamare i service, e i service dovrebbero chiamare altri service o repository, mai bypassare un layer
2. **Centralizzare la logica di business**: La logica per aggiornare il totale dei punti storia era correttamente implementata nel service, ma non veniva utilizzata
3. **Utilizzare l'injection delle dipendenze**: Assicurarsi che tutte le dipendenze necessarie siano iniettate correttamente
4. **Testare scenari end-to-end**: Il problema è stato scoperto solo testando l'intero flusso di lavoro dall'inizio alla fine

Questa esperienza ha rafforzato l'importanza di seguire il principio di responsabilità unica e di mantenere una chiara separazione delle responsabilità tra i diversi layer dell'applicazione.

### 6.2 Soluzioni adottate
I problemi sono stati risolti seguendo le best practices di Spring Boot:

- **Constructor injection**: Per dipendenze chiare e testabilità
- **Transazioni ben definite**: Con `@Transactional` e `readOnly = true` dove appropriato
- **Disabilitazione di Open Session in View**: Per evitare problemi N+1
- **Separazione tra layer web e persistenza**: Uso di DTO per disaccoppiare API e database
- **Validazione centralizzata**: Con Jakarta Bean Validation sui DTO

### 6.3 Possibili miglioramenti futuri
Il progetto potrebbe essere migliorato con:

- **Implementazione di API REST complete**: Per supportare client JavaScript moderni
- **Aggiunta di un frontend SPA**: Con React, Angular o Vue per UX migliorata
- **Integrazione con sistemi CI/CD**: Per deployment automatizzato
- **Implementazione di microservizi**: Per scalabilità e resilienza
- **Aggiunta di funzionalità di reportistica avanzata**: Dashboard e analytics

## 7. Conclusioni

### 7.1 Obiettivi raggiunti
ZenFlow ha raggiunto i seguenti obiettivi:

- **Creazione di una piattaforma completa per la gestione Agile**: Con supporto per l'intero ciclo di vita del progetto
- **Implementazione di funzionalità di stima collaborativa**: Planning Poker e PERT
- **Sistema di sicurezza robusto**: Basato su ruoli e permessi granulari

### 7.2 Spunti di riflessione
Il progetto ha evidenziato:

- **L'importanza delle metodologie Agile**: Per gestire progetti software complessi
- **Il valore degli strumenti di supporto al project management**: Per migliorare efficienza e trasparenza
- **L'evoluzione delle tecnologie Java e Spring**: Verso paradigmi più moderni e produttivi

### 7.3 Valore del progetto
ZenFlow rappresenta:

- **Strumento pratico per team Agile**: Che facilita la collaborazione e la pianificazione
- **Esempio di applicazione enterprise moderna**: Con architettura robusta e scalabile
- **Dimostrazione di competenze tecniche avanzate**: Nell'ecosistema Java/Spring
