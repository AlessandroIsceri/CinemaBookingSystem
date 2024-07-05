# Progetto Sistemi Distribuiti 2022-2023

## Componenti del gruppo

* Alessandro Isceri 879309 <a.isceri@campus.unimib.it>
* Mattia Ingrassia 879204 <m.ingrassia3@campus.unimib.it>
* Riccardo Ghilotti 879259 <r.ghilotti@campus.unimib.it>

## Compilazione ed esecuzione

Il server Web e il database sono dei progetti Java che utilizano Maven per gestire le dipendenze, la compilazione e l'esecuzione. È necessario eseguire i seguenti obiettivi per compilare ed eseguire: `clean`, che rimuove la cartella `target`, `compile` per compilare e `exec:java` per avviare il componente.

I tre obiettivi possono essere eseguiti insieme in una sola riga di comando da terminale tramite `./mvnw clean compile exec:java` per Linux/Mac e `mvnw.cmd clean compile exec:java` per Windows. L'unico requisito è un'installazione di Java (almeno la versione 11), verificando che la variabile `JAVA_PATH` sia correttamente configurata.

Si può anche utilizzare un IDE come Eclipse o IntelliJ IDEA, in tal caso va configurato (per Eclipse si possono seguire le istruzioni mostrate nelle slide del laboratorio 5 sulle Servlet).

## Porte e indirizzi

Il server Web si pone in ascolto all'indirizzo `localhost` alla porta `8080`. Il database si pone in ascolto allo stesso indirizzo del server Web ma alla porta `3030`.

Per utilizzare il client correttamente è importante modificare le impostazioni di CORS, come mostrato nel laboratorio 8 su JavaScript (AJAX).

### Maggiori informazioni sul progetto

#### Database

Il database è generico, si basa su una struttura chiave valore rappresentata come hashmap, in cui:
* La chiave è una stringa di tipo `keyType:Id`.
* Il valore è una stringa contenente un oggetto JSON.

Il database, per gestire le operazioni di inserimento, contiene anche delle coppie chiave-valore del tipo `<"keyType:MaXID", "ID">`, in modo che dopo ogni inserimento l'ID viene incrementato preservando l'unicità dei vari oggetti.

Per quanto riguarda la concorrenza, sono stati creati dei metodi per garantire il corretto funzionamento del sistema anche in caso di accessi concorrenti, utilizzando i metodi risolutivi del problema dei lettori/scrittori, bilanciando il carico tra lettori e scrittori (massimo tre lettori concorrenti oppure uno scrittore).

Inoltre, è presente un metodo `addQuotes(String str)`, necessario per preservare il formato delle stringhe nel file JSON nel database.

#### Servlet

La componente che funziona da punto intermedio tra il database generico e le pagine dell'applicazione HTML. Gestisce sia le chiamate tramite protocollo REST API (e i relativi status code), sia la comunicazione col database tramite il protocollo TCP.

##### Struttura chiavi utilizzata dalla Servlet

Per modellare la struttura del cinema, vengono utilizzate le seguenti tipologie di chiavi:
* Movie &rarr;       `movie:Id`
* Room &rarr;        `room:Id`
* Projection &rarr;  `projection:Id`
* Reservation &rarr; `projection{projectionId}-reservation:Id`, dove `{projectionId}` è un numero.

#### Client

Il client Web è formato da tre file HTML:
* `index.html`: rappresenta la home page del sito, contiene una lista dei film e delle rispettive proiezioni in programma, e una sezione per cercare una prenotazione esistente.
* `projection.html`: è la pagina che mostra le informazioni riguardanti una proiezione, che permette di vedere i posti prenotabili e di effettuare una prenotazione.
* `reservation.html`: è la pagina che mostra le informazioni riguardanti una prenotazione, che permette di vedere quali posti sono prenotati ed, eventualmente, di rimuoverli dalla prenotazione o cancellare la prenotazione stessa.

##### Stile del client

Per lo stile è stato utilizzato il framework css di [Bulma](https://www.bulma.io/), che non interferisce con javascript. Inoltre sono stati creati altri file css (uno per pagina, presenti nella cartella styles) per modellare lo stile.
