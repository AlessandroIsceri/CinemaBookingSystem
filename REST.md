# Progetto Sistemi Distribuiti 2022-2023 - API REST

Documentazione delle API REST. Si assume che i dati vengano scambiati in formato JSON.

Di seguito vengono riportate le risorse utilizzate da questo protocollo.

## `projection`

* `id`: una stringa contenente un identificativo univoco della projection;
* `movieID`: una stringa contenente l'ID del *movie* associato alla projection;
* `date`: una stringa (nella Servlet gestito come tipo DateTime) che rappresenta la data della projection;
* `time`: una stringa (nella Servlet gestito come tipo LocalTime) che rappresenta l'orario della projection;
* `roomNumber` una stringa che rappresenta il numero della *room* in cui avverrà la projection; 
* `availableSeats`: una matrice di booleani che rappresenta lo stato dei posti (true = libero, false = occupato).

## `movie`

* `id`: una stringa contenente un identificativo univoco del movie;
* `title`: una stringa contenente il titolo del movie;
* `duration`: un intero che rappresenta la durata del movie in minuti;
* `description`: una stringa che rappresenta la descrizione del movie.

## `room`

* `roomNumber`: una stringa contenente un identificativo univoco della room;
* `rows`: un intero che rappresenta il numero di righe (di posti) della room;
* `columns`: un intero che rappresenta il numero di colonne (di posti) della room.

## `reservation`

* `id`: una stringa contenente un identificativo univoco (per la projection) della reservation;
* `projectionId`: una stringa contenente l'ID della *projection* associata alla reservation;
* `reservedSeats`: Una lista di numeri che rappresenta il numero dei posti associati alla reservation (prenotati dall'utente).

> Il posto (identificato da riga e colonna in una matrice) è associato al numero nel seguente modo:
> * `numero % numeroColonne` è uguale al numero della colonna del posto
> * `numero / numeroColonne` è uguale al numero della riga del posto

# Chiamate implementate

## GET /projections

Ritorna un oggetto in cui ogni chiave (id del movie) ha come valore una lista contenente come primo elemento l'oggetto movie, seguito da tutte le projections relative a quel movie.

#### Esempio:

`GET /projections/`

###### Ritorno:

```json
{
    "2": [
        {
            "id": "2",
            "title": "RoboCop",
            "duration": 102,
            "description": "Ambientato in una Detroit dominata dal crimine..."
        },
        {
            "id": "2",
            "movieID": "2",
            "date": "2035-12-23",
            "time": "23:00",
            "roomNumber": "2",
            "availableSeats": [
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
            "id": "1",
            "movieID": "2",
            "date": "2035-12-23",
            "time": "21:10",
            "roomNumber": "1",
            "availableSeats": [
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
    ],
    "3": [
        {
            "id": "3",
            "title": "Inception",
            "duration": 148,
            "description": "Cobb e' il miglior ladro di informazioni del mondo: ..."
        },
        {
            "id": "3",
            "movieID": "3",
            "date": "2035-12-25",
            "time": "23:00",
            "roomNumber": "2",
            "availableSeats": [
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
        }
    ]
}
```

#### Codici di stato:

* `200 OK`: le risorse esistono e sono state restituite.

## GET /projections/{projectionId}

Ritorna una lista di tre elementi in cui il primo oggetto rappresenta la projection richiesta, il secondo oggetto rappresenta il movie relativo alla projection ed il terzo oggetto rappresenta la room in cui avverrà la projection.

#### Esempio:

`GET /projections/2`

###### Ritorno:

```json
[
    {
        "id": "2",
        "movieID": "2",
        "date": "2035-12-23",
        "time": "23:00",
        "roomNumber": "2",
        "availableSeats": [
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
        "id": "2",
        "title": "RoboCop",
        "duration": 102,
        "description": "Ambientato in una Detroit dominata dal crimine..."
    },
    {
        "roomNumber": "2",
        "rows": 2,
        "columns": 3
    }
]
```

#### Codici di stato:

* `200 OK`: la risorsa esiste ed è stata restituita.

* `404 NOT FOUND`: la risorsa non esiste.

* `500 INTERNAL SERVER ERROR`: il server non è riuscito ad elaborare la richiesta.

## PUT /projections/{projectionId}/

Aggiorna la proiezione il cui id è uguale a quello specificato (projectionId), con l'oggetto JSON passato nel body, l'id dell'oggetto JSON non può essere diverso dal projectionId inserito come parametro di path.
La richiesta deve avere l'header `Content-Type: application/json`.

#### Esempio:

`PUT /projections/2`

###### Body:

```json
{
    "id": "2",
    "movieID": "2",
    "date": "2035-12-23",
    "time": "23:00",
    "roomNumber": "2",
    "availableSeats": [
        [
            true, true, false
        ],
        [
            false, false, true
        ]
    ]
}
```

#### Codici di stato:

* `204 NO CONTENT`: la risorsa è stata aggiornata con successo.

* `400 BAD REQUEST`: la risorsa inviata nel body è sbagliata (l'oggetto JSON non è formattato correttamente, ovvero non segue lo schema di projection) oppure il parametro di path non corrisponde all'id dell'oggetto JSON passato nel body.

* `404 NOT FOUND`: la projection non esiste e dunque non è stata aggiornata.

* `415 UNSUPPORTED MEDIA TYPE`: Il content-type della richiesta non è application/json.

* `500 INTERNAL SERVER ERROR`: il server non è riuscito ad elaborare la richiesta.

## POST /projections/{projectionId}/reservations

Aggiunge una nuova reservation riguardante la proiezione il cui id è uguale al parametro di path projectionId.
La reservation viene passata nel body come oggetto JSON.
Viene ritornato un URI che contiene il path per visualizzare la reservation effettuata.

#### Esempio:

`POST /projections/2/reservations/`

###### Body:

```json
{
    "projectionID" : "2",
    "reservedSeats" : [
        0, 
        1
    ]
}
```

###### Ritorno:

Location: `/projections/2/reservations/6`.

#### Codici di stato:

* `201 CREATED`: la risorsa è stata creata, viene restituito un URI contenente la location per visualizzare la risorsa.

* `400 BAD REQUEST`: la risorsa inviata nel body è sbagliata (l'oggetto JSON non è formattato correttamente, ovvero non segue lo schema di reservation) oppure il parametro di path non corrisponde all'id della proiezione nell'oggetto JSON passato nel body oppure non è stato selezionato nessun posto.

* `404 NOT FOUND`: la projection con id uguale a projectionId non esiste.

* `409 CONFLICT`: esiste una già reservation a cui sono assegnati uno o più posti selezionati.

* `415 UNSUPPORTED MEDIA TYPE`: il content-type della richiesta non è application/json.

* `500 INTERNAL SERVER ERROR`: il server non è riuscito ad elaborare la richiesta.

## GET /projections/{projectionId}/reservations/{reservationId}

Ritorna un oggetto JSON di tipo reservation il cui id corrisponde a reservationId.

#### Esempio:

`GET /projections/2/reservations/6`

###### Ritorno:

```json
{
    "id": "6",
    "projectionID": "2",
    "reservedSeats": [
        0,
        1
    ]
}
```

#### Codici di stato:

* `200 OK`: la risorsa esiste ed è stata restituita.

* `404 NOT FOUND`: la risorsa (projection oppure reservation) non esiste.

* `500 INTERNAL SERVER ERROR`: il server non è riuscito ad elaborare la richiesta.

## PUT /projections/{projectionId}/reservations/{reservationId}

Aggiorna la prenotazione il cui id è uguale a quello specificato (reservationId), con l'oggetto JSON passato nel body, l'id dell'oggetto JSON riguardante la projection non può essere diverso dal projectionId inserito come parametro di path, così come l'id della reservation.
La richiesta deve avere l'header `Content-Type: application/json`.

#### Esempio:

`PUT /projections/2/reservations/6`

###### Body:

```json
{
    "id": "6",
    "projectionID": "2",
    "reservedSeats": [
        0
    ]
}
```

#### Codici di stato:

* `204 NO CONTENT`: la risorsa è stata aggiornata con successo.

* `400 BAD REQUEST`: la risorsa inviata nel body è sbagliata (l'oggetto JSON non è formattato correttamente, ovvero non segue lo schema di reservation) oppure uno dei parametri di path non corrisponde al corrispettivo dell'oggetto JSON passato nel body.

* `404 NOT FOUND`: la reservation o la projection non esiste e dunque la reservation non è stata aggiornata.

* `415 UNSUPPORTED MEDIA TYPE`: il content-type della richiesta non è application/json.

* `500 INTERNAL SERVER ERROR`: il server non è riuscito ad elaborare la richiesta.

## DELETE /projections/{projectionId}/reservations/{reservationId}

Rimuove la reservation il cui id è uguale a reservationId, se presente.

#### Esempio:

`DELETE /projections/2/reservations/6`

#### Codici di stato:

* `204 NO CONTENT`: la risorsa è stata eliminata con successo.

* `404 NOT FOUND`: la reservation non esiste oppure la projection non esiste e dunque la reservation non è stata eliminata.

* `500 INTERNAL SERVER ERROR`: il server non è riuscito ad elaborare la richiesta.
