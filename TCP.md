# Progetto Sistemi Distribuiti 2022-2023 - TCP

Il protocollo TCP realizzato per la comunicazione tramite Socket è un protocollo testuale di tipo Richiesta-Risposta, quindi ad ogni richiesta viene generata la risposta corrispondente.

### Formato chiavi e valori

* La chiave è una stringa di tipo `keyType:Id`, dove keyType è una stringa arbitraria a scelta di chi utilizza il database e il protocollo, mentre Id è un identificatore numerico univoco per il keyType.
* Il valore è una stringa contenente un oggetto JSON. Il contenuto dell'oggetto JSON dipende da chi utilizza il database e il protocollo.

### Richieste:

Di seguito vengono elencati i comandi utilizzabili nella comunicazione ideata:

* **+GET**

  *   
    ```
    +GET key:*
    ```
    Se viene utilizzato questo comando, il database ritorna tutti gli oggetti JSON (in formato String) la cui chiave è uguale a key (indipendentemente dal loro id).

    #### Esempio:

    ###### Richiesta:

    > "+GET projection:*"

    ###### Ritorno:

    Il database ritorna tutte le projections, ovvero tutti gli oggetti JSON la cui chiave è uguale a projection.
    Si ricorda che le richieste e le risposte sono stringhe.

    ```
    "[
       {
          "id":"3",
          "movieID":"3",
          "date":"2035-12-25",
          "time":"23:00",
          "roomNumber":"2",
          "availableSeats":[
             [
                true,
                true,
                true
             ],
             [
                true,
                true,
                true
             ]
          ]
       },
       {
          "id":"2",
          "movieID":"2",
          "date":"2035-12-23",
          "time":"23:00",
          "roomNumber":"2",
          "availableSeats":[
             [
                true,
                true,
                true
             ],
             [
                false,
                false,
                true
             ]
          ]
       },
       {
          "id":"1",
          "movieID":"2",
          "date":"2035-12-23",
          "time":"21:10",
          "roomNumber":"1",
          "availableSeats":[
             [
                false,
                true,
                true
             ],
             [
                false,
                true,
                true
             ],
             [
                false,
                false,
                false
             ]
          ]
       }
    ]"
    ```

  * 
    ```
    +GET key:id
    ```
     
    Se viene utilizzato questo comando, il database ritorna l'oggetto JSON (in formato String) la cui chiave è uguale a key ed il cui identificatore è identico ad id.

    #### Esempio: 
     
    ###### Richiesta:

    > "+GET projection:2"
     
    ###### Ritorno:

    Il database ritorna la projection, ovvero l'oggetto JSON la cui chiave è uguale a projection ed il cui id è uguale a 2.

    ```
    "{
       "id":"2",
       "movieID":"2",
       "date":"2035-12-23",
       "time":"23:00",
       "roomNumber":"2",
       "availableSeats":[
          [
             true,
             true,
             true
          ],
          [
             false,
             false,
             true
          ]
       ]
    }"
    ```
    
* **+ADD**

    ```
    +ADD key:id
    payload
    +END
    ```

    Se viene utilizzato questo comando, il database attende un oggetto JSON (in formato String) e capisce che il payload è terminato tramite il comando +END inserito alla fine della comunicazione.
    Il database aggiunge l'oggetto JSON passato come payload, avente come chiave `key:id`, all'insieme degli oggetti in memoria.

    Il database ritorna `"true"` se l'aggiunta è andata a buon fine (ovvero se non esisteva un elemento con quella key), altrimenti `"false"`.
    
    #### Esempio: 
    
    ###### Richiesta:

    > "+ADD persona:1"
    > 
    > "{name: "Pippo", surname:"Baudo"}"
    > 
    > "+END"
    
    ###### Ritorno:
    
    Il database memorizza l'oggetto `{name: "Pippo", surname:"Baudo"}` con chiave `"persona:1"`.
   
    ```
    "true"
    ```


* **+PUT**

    ```
    +PUT key:id
    payload
    +END
    ```
    
    Se viene utilizzato questo comando, il database cerca un oggetto con chiave `"key:id"`, se lo trova, modifica il valore corrente con il nuovo payload (un oggetto JSON) e ritorna `"true"`, in caso contrario ritorna `"false"`.
    
    #### Esempio: 
    
    ###### Richiesta:

    > "+PUT persona:1"
    > 
    > "{name:"Bruno", surname:"Mars"}"
    > 
    > "+END"
    
    ###### Risposta:

    Il database cerca tra gli oggetti presenti in memoria l'oggetto con chiave `"persona:1"` se lo trova, modifica il valore corrente con `{name:"Bruno", surname:"Mars"}` e ritorna `"true"`, in caso contrario ritorna `"false"`.

    ```
    "true"
    ```

* **+DEL**

     ```
     +DEL key:id
     ```
    
    Se viene utilizzato questo comando, il database cerca un oggetto con chiave `"key:id"`, se lo trova, lo elimina e ritorna `"true"`, in caso contrario ritorna `"false"`.
    
    #### Esempio: 
    
    ###### Richiesta:

    > "+DEL persona:1"
    
    ###### Risposta:

    Il database cerca tra gli oggetti presenti in memoria l'oggetto con chiave `"persona:1"` se lo trova, lo elimina e ritorna `"true"`, in caso contrario ritorna `"false"`.

    ```
    "true"
    ```

* **+NID**
    
    ```
    +NID key
    ```
    
    Se viene utilizzato questo comando, il database ritorna un nuovo identificatore (id) utilizzabile (libero) per quella chiave (key).

    #### Esempio: 
    
    ###### Richiesta:

    > "+NID persona"
    
    ###### Risposta:

    Il database (vuoto) ritorna il valore 1.
    
    ```
    "1"
    ```

    #### Esempio:
    
    ###### Richiesta:

    > "+NID persona"

    ###### Risposta:
    
    Il database (non vuoto, contiene tre oggetti di chiave persona aventi come id rispettivamente: 2, 3, 5) ritorna il valore 6.

    ```
    "6"
    ```

* **+CLOSE**
    
    ```
    +CLOSE
    ```
    
    Se viene utilizzato questo comando, la connessione socket tra le due componenti viene chiusa.

* **+ERR**
    
    ```
    +ERR messaggio
    ```
    
    Se viene utilizzato questo comando, significa che il database ha riscontrato un errore durante l'elaborazione e quindi ritorna un messaggio di errore che descrive il problema.

    #### Esempio:

    Il database riceve un comando che non è presente nel protocollo (non è in questo elenco).
    Utilizza 

    > "+ERR Unknown command"

    per segnalarlo.

* **+END**

  Questo comando viene utilizzato quando si sta trasferendo un payload di dimensione variabile e si vuole segnalare la fine del payload.
