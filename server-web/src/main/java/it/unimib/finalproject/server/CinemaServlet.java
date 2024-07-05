package it.unimib.finalproject.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("projections")
public class CinemaServlet {
    private final String IP_ADDRESS = "127.0.0.1";
    private final int DBPORT = 3030;
    private Socket DB;
    private static PrintWriter outputStream;
    private static BufferedReader inputStream;
    private static ObjectMapper mapper = new ObjectMapper();
    
    //viene richiesto l'utilizzo degli headers per analizzare il contenuto degli headers di alcune richieste
    @Context HttpHeaders headers;

    //GET /projections/
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProjections() {
        Projection[] projections = null;
        //returnMap è l'oggetto JSON ritornato
        Map<String, ArrayList<Object>> returnMap = new HashMap<String, ArrayList<Object>>();
        try{
            openDBConnection();

            //seguendo il protocollo, viene inviata una richiesta al DB per ottenere le projections disponibili
            outputStream.println("+GET projection:*");
            
            //viene letta la risposta (un array di projections)
            String JSONString = readStream();

            //viene controllato che il database non abbia ritornato un messaggio di errore
            if(checkErrorMessage(JSONString)){
                // Status code 500: errore del server
                return Response.serverError().build();
            }

            //viene registrato il modulo per gestire i tipi LocalTime e LocalDate
            mapper.registerModule(new JavaTimeModule());
            projections = mapper.readValue(JSONString, Projection[].class);
            
            //viene calcolata la mappa da ritornare
            returnMap = getProjectionsPerMovie(projections);
            
            closeDBConnection();
        } catch (Exception e) {
            e.printStackTrace();
            // Status code 500: errore del server
            return Response.serverError().build();
        }
        // Status code 200: tutto è andato a buon fine, restituisce la mappa
        return Response.ok(returnMap).build();
    }

    //GET /projections/{projectionId}
    @GET
    @Path("/{projectionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProjection(@PathParam("projectionId") String projectionId) {
        Projection projection = null;
        Object returnList[] = new Object[3];
        try {
            openDBConnection();

            // seguendo il protocollo, viene inviata una richiesta al DB per ottenere la projection richiesta
            outputStream.println("+GET projection:" + projectionId);

            // viene letta la risposta
            String JSONString = readStream();

            //viene controllato che il database non abbia ritornato un messaggio di errore
            if(checkErrorMessage(JSONString)){
                // Status code 500: errore del server
                return Response.serverError().build();
            }

            if(JSONString.equals("")){
                // Status code 404: risorsa non trovata
                return Response.status(Status.NOT_FOUND).build();
            }

            //viene registrato il modulo per gestire i tipi LocalTime e LocalDate
            mapper.registerModule(new JavaTimeModule());
            projection = mapper.readValue(JSONString, Projection.class);

            //vengono recuperati la room e il movie corrispondenti alla projection
            Response responseMovie = getMovie(projection.getmovieID());
            Movie movie = (Movie) responseMovie.getEntity();
            Response responseRoom = getRoom(projection.getRoomNumber());
            Room room = (Room) responseRoom.getEntity();
                
            //viene riempita la lista di ritorno
            returnList[0] = projection;
            returnList[1] = movie;
            returnList[2] = room;

            closeDBConnection();
        } catch (Exception e) {
            e.printStackTrace();
            //Status code 500: errore del server
            return Response.serverError().build();
        }
        //Status code 200: tutto è andato a buon fine, restituisce la lista
        return Response.ok(returnList).build();
    }
    
    //PUT /projections/{projectionId}
    @PUT
    @Path("/{projectionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProjection(@PathParam("projectionId") String projectionId, String body) {
        
        boolean checkHeaders = headers.getRequestHeader("Content-Type").contains("application/json");

        //si controlla che gli header della richiesta contengano come content-type application/json
        if(!checkHeaders){
            //Status code 415: il content-type non è application/json
            return Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build();
        }
        try {
            openDBConnection();
            
            //viene registrato il modulo per gestire i tipi LocalTime e LocalDate
            mapper.registerModule(new JavaTimeModule());
            Projection projection = mapper.readValue(body, Projection.class);
            
            //viene controllato il possibile errore 400
            if(!projection.getId().equals(projectionId)){
                //Status code 400: l'id della richiesta non corrisponde all'id dell'oggetto passato nel body
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            // seguendo il protocollo, viene inviata una richiesta al DB per aggiornare la projection con id uguale ad projectionId
            outputStream.println("+PUT projection:" + projectionId);
            outputStream.println(body);
            outputStream.println("+END");

            // viene letta la risposta
            String JSONString = readStream();

            //viene controllato che il database non abbia ritornato un messaggio di errore
            if(checkErrorMessage(JSONString)){
                // Status code 500: errore del server
                return Response.serverError().build();
            }

            if(JSONString.equals("false")){
                // Status code 400: Non è stata trovata la projection nel database
                return Response.status(Status.NOT_FOUND).build();
            }
            
            closeDBConnection();
        }catch (JsonProcessingException e) {
            //Status code 400: errore nel parsing dell'oggetto JSON (passato nel body) in una projection
            e.printStackTrace();
		    return Response.status(Response.Status.BAD_REQUEST).build();
        } 
        catch (Exception e) {
            e.printStackTrace();
            // Status code 500: errore del server
            return Response.serverError().build();
        }
        //Status code 204: la richiesta è andata a buon fine e la projection è stata aggiornata
        return Response.noContent().build();
    }

    //POST /projections/{projectionId}/reservations/
    @POST
    @Path("/{projectionId}/reservations/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReservation(@PathParam("projectionId") String projectionId, String body){
        URI uri = null;
        try{
            //si controlla che gli header della richiesta contengano come content-type application/json
            boolean checkHeaders = headers.getRequestHeader("Content-Type").contains("application/json");
            if(!checkHeaders){
                //Status code 415: il content-type non è application/json
                return Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build();
            }

            //viene registrato il modulo per gestire i tipi LocalTime e LocalDate
            mapper.registerModule(new JavaTimeModule());
            Reservation reservation = mapper.readValue(body, Reservation.class);
            
            if(!reservation.getProjectionID().equals(projectionId)){
                //Status code 400: l'id della richiesta e quello dell'oggetto passato nel body non corrispondono
                return Response.status(Response.Status.BAD_REQUEST).build();
            }          

            if(reservation.getReservedSeats().isEmpty()){
                //Status code 400: nessun posto prenotato
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            //viene recuperata la projection corrispondente
            Response responseProjection = getProjection(projectionId);
            if(responseProjection.getStatus() == 404){
                // Status code 404: la projection non esiste
                return Response.status(Status.NOT_FOUND).build();
            }
            Object[] tempResponse = (Object[]) responseProjection.getEntity();
            Projection projection = (Projection) tempResponse[0];

            //viene controllato se i posti sono riservabili o sono già occupati
            boolean reservationOK = reserveSeats(projection, reservation.getReservedSeats());
            if(!reservationOK){
                //Status code 409: il posto è già stato prenotato
                return Response.status(Status.CONFLICT).build();
            }

            //viene ricavato un nuovo id
            String newID = (String) getNewId("projection" + projectionId + "-reservation").getEntity();
            
            //viene settato il nuovo id alla reservation
            reservation.setId(newID);

            //la reservation viene convertita in un oggetto JSON
            String newResJSON = mapper.writeValueAsString(reservation);

            openDBConnection();
            
            //seguendo il protocollo, viene inviata una richiesta al DB per inserire la nuova reservation
            outputStream.println("+ADD projection" + projectionId + "-reservation:" + newID);
            outputStream.println(newResJSON);
            outputStream.println("+END");
            
            //viene letta la risposta
            String JSONString = readStream();

            //viene controllato che il database non abbia ritornato un messaggio di errore
            if(checkErrorMessage(JSONString)){
                // Status code 500: errore del server
                return Response.serverError().build();
            }

            if(JSONString.equals("false")){
                // Status code 500: errore del server
                return Response.serverError().build();
            }

            //vengono aggiornati i posti disponibili per quella proiezione
            String JSONProjection = mapper.writeValueAsString(projection);
            updateProjection(projectionId, JSONProjection);

            closeDBConnection();

            //viene creato un URI da ritornare come da protocollo
            uri = new URI("/projections/" + projectionId + "/reservations/" + reservation.getId());

        }catch (JsonProcessingException e) {
            e.printStackTrace();
            //Status code 400: bad request (JSON malformattato)
		    return Response.status(Response.Status.BAD_REQUEST).build();
        }catch(Exception e){
            e.printStackTrace();
            // Status code 500: errore del server
            return Response.serverError().build();
        }
        // Status code 201: la richiesta è andata a buon fine, viene restituito l'uri creato sopra
        return Response.created(uri).build();
    }
    
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{projectionId}/reservations/{reservationId}")
    public Response getReservation(@PathParam("projectionId") String projectionId, @PathParam("reservationId") String reservationId) {
        Reservation returnReservation = null;
        try {

            Response responseProjection = getProjection(projectionId);
            if(responseProjection.getStatus() == 404){
                //Status code 404: la projection selezionata non esiste
                return Response.status(Status.NOT_FOUND).build();
            }

            openDBConnection();
            
            // seguendo il protocollo, viene inviata una richiesta al DB per ottenere la reservation richiesta
            outputStream.println("+GET projection" + projectionId + "-reservation:" + reservationId);

            String JSONString = readStream();

            //viene controllato che il database non abbia ritornato un messaggio di errore
            if(checkErrorMessage(JSONString)){
                // Status code 500: errore del server
                return Response.serverError().build();
            }
            
            if(JSONString.equals("")){
                //Status code 404: la reservation selezionata non esiste
                return Response.status(Status.NOT_FOUND).build();
            }

            mapper.registerModule(new JavaTimeModule());            
            returnReservation = mapper.readValue(JSONString, Reservation.class);
            
            closeDBConnection();
        } catch(Exception e){
            e.printStackTrace();
            // Status code 500: errore del server
            return Response.serverError().build();
        }
        return Response.ok(returnReservation).build();
    }

    @PUT
    @Path("/{projectionId}/reservations/{reservationId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateReservation(@PathParam("projectionId") String projectionId, @PathParam("reservationId") String reservationId, String body) {
        
        try {
            //vengono controllati gli headers
            boolean checkHeaders = headers.getRequestHeader("Content-Type").contains("application/json");
            if(!checkHeaders){
                //Status code 415: il content-type della richiesta non è corretto
                return Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build();
            }

            //viene convertito il body in un oggetto di tipo Reservation 
            mapper.registerModule(new JavaTimeModule());
            Reservation newReservation = mapper.readValue(body, Reservation.class);

            //Controllo che l'ID della proiezione corrisponda
            if(!newReservation.getProjectionID().equals(projectionId)){
                //Status code 400: bad request (JSON errato)
                return Response.status(Status.BAD_REQUEST).build();
            }
            
            //Controllo che l'ID della reservation corrisponda
            if(!newReservation.getId().equals(reservationId)){
                //Status code 400: bad request (JSON errato)
                return Response.status(Status.BAD_REQUEST).build();
            }
            
            //se la lista dei nuovi posti prenotati è vuota, allora la reservation viene cancellata
            if(newReservation.getReservedSeats().isEmpty()){
                return deleteReservation(projectionId, reservationId);
            }

            //viene ricavata la projection corrispondente
            Response responseProjection = getProjection(projectionId);
            
            if(responseProjection.getStatus() == 404){
                // Status code 404: la projection selezionata non esiste
                return Response.status(Status.NOT_FOUND).build();
            }
            
            Object[] tempResponse = (Object[]) responseProjection.getEntity();
            Projection projection = (Projection) tempResponse[0];

            //viene ricavata la vecchia reservation (da aggiornare coi nuovi posti prenotati)
            Response responseReservation = getReservation(projectionId, reservationId);
            
            if(responseReservation.getStatus() == 404){
                // Status code 404: la reservation selezionata non esiste
                return Response.status(Status.NOT_FOUND).build();
            }
            
            Reservation oldReservation = (Reservation) responseReservation.getEntity();
            
            // vengono calcolati i posti della reservation che vanno rimossi dalla reservation
            List <Integer> seatsToRemove = getSeatsToRemove(oldReservation.getReservedSeats(), newReservation.getReservedSeats());
            
            freeSeats(projection, seatsToRemove);
            
            openDBConnection();

            //viene settato l'id alla new reservation nel caso non venga passato in input
            newReservation.setId(oldReservation.getId());
            
            //la nuova reservation viene inviata al database per essere memorizzata al posto della precedente
            String JSONRes = mapper.writeValueAsString(newReservation);
            outputStream.println("+PUT projection" + projectionId + "-reservation:" + reservationId);
            outputStream.println(JSONRes);
            outputStream.println("+END");

            String JSONString = readStream();

            //viene controllato che il database non abbia ritornato un messaggio di errore
            if(checkErrorMessage(JSONString)){
                // Status code 500: errore del server
                return Response.serverError().build();
            }

            if(JSONString.equals("false")){
                // Status code 500: errore del server
                return Response.serverError().build();
            }
            
            //vengono aggiornati i posti disponibili nella proiezione interessata
            String JSONProj = mapper.writeValueAsString(projection);
            updateProjection(projectionId, JSONProj);
        
            closeDBConnection();
        }catch (JsonProcessingException e) {
            e.printStackTrace();
            //Status code 400: bad request (JSON malformattato)
		    return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            e.printStackTrace();
            // Status code 500: errore del server
            return Response.serverError().build();
        }
        // Status code 204: l'aggiornamento è avvenuto con successo
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{projectionId}/reservations/{reservationId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteReservation(@PathParam("projectionId") String projectionId, @PathParam("reservationId") String reservationId) {
        
        try {
            //viene chiamato il metodo getProjection per aggiornare i posti disponibili
            Response responseProjection = getProjection(projectionId);
            
            if(responseProjection.getStatus() == 404){
                // Status code 404: la projection selezionata non esiste
                return Response.status(Status.NOT_FOUND).build();
            
            }
            Object[] temp = (Object[]) responseProjection.getEntity();
            Projection projection = (Projection) temp[0];

            //viene chiamato il metodo getReservation per eliminare i posti prenotati
            Response responseReservation = getReservation(projectionId, reservationId);

            if(responseReservation.getStatus() == 404){
                // Status code 404: la reservation selezionata non esiste
                return Response.status(Status.NOT_FOUND).build();
            }

            Reservation reservation = (Reservation) responseReservation.getEntity();

            freeSeats(projection, reservation.getReservedSeats());

            openDBConnection();

            //come da protocollo, viene inviato un messaggio di tipo DEL
            outputStream.println("+DEL projection" + projectionId + "-reservation:" + reservationId);
            
            String JSONString = readStream();

            //viene controllato che il database non abbia ritornato un messaggio di errore
            if(checkErrorMessage(JSONString)){
                // Status code 500: errore del server
                return Response.serverError().build();
            }

            if(JSONString.equals("false")){
                // Status code 500: errore nel server
                return Response.serverError().build();
            }

            closeDBConnection();

            //viene aggiornata la proiezione con i nuovi posti disponibili
            mapper.registerModule(new JavaTimeModule());
            String JSONProj = mapper.writeValueAsString(projection);

            headers.getRequestHeaders().add("Content-Type", "application/json");
            updateProjection(projectionId, JSONProj);
            headers.getRequestHeaders().remove("Content-Type", "application/json");
            
        } catch (Exception e) {
            e.printStackTrace();
            // Status code 500: errore del server
            return Response.serverError().build();   
        }
        //Status code: 204: l'operazione è andata a buon fine
        return Response.noContent().build();
    }

    public Response getMovie(String movieID){
        Movie movie = null;
        try {
            openDBConnection();

            // seguendo il protocollo, viene inviata una richiesta al DB per ottenere uno specifico movie
            outputStream.println("+GET movie:" + movieID);

            // viene letta la risposta
            String JSONString = readStream();

            mapper.registerModule(new JavaTimeModule());
            movie = mapper.readValue(JSONString, Movie.class);
            
            closeDBConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.ok(movie).build();
    }
    
    
    public Response getRoom(String roomNumber) {
        Room room = null;
        try {
            openDBConnection();

            // seguendo il protocollo, viene inviata una richiesta al DB per ottenere la room richiesta
            outputStream.println("+GET room:" + roomNumber);

            // viene letta la risposta
            String JSONString = readStream();

            room = mapper.readValue(JSONString, Room.class);
            
            closeDBConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.ok(room).build();
    }

    public Response getNewId(String key){
        
        String newId = "";
        try {
            
            openDBConnection();

            //come da protocollo, viene inviato un comando NID per ottenere un id disponibile
            outputStream.println("+NID " + key);
            
            newId = readStream();

            closeDBConnection();
        } catch (Exception e){
            e.printStackTrace();
        }
        return Response.ok(newId).build();
    }

    //metodo per liberare dei posti in precedenza prenotati da una proiezione
    public void freeSeats(Projection projection, List<Integer> freedSeats){
        for (int i = 0; i < freedSeats.size(); i++) {
            // vengono liberati i posti
            projection.setAvailable(freedSeats.get(i));
        }
    }
    
    //metodo per riservare i posti per una proiezione
    public boolean reserveSeats(Projection projection, List<Integer> reservedSeats){
        //viene controllato che i posti siano effetivamente liberi
        for(int i = 0; i < reservedSeats.size(); i++){
            if(!projection.isAvailable(reservedSeats.get(i))){
                //uno dei posti che si voleva prenotare e' occupato
                return false;
            }
        }

        //una volta passato il controllo, i posti vengono effettivamente riservati
        for(int i = 0; i < reservedSeats.size(); i++){
            projection.setUnavailable(reservedSeats.get(i));
        }

        return true;
    }

    //metodo che ritorna il numero di un posto, dato un numero di riga, colonna e stanza.
    public int getSeatNumber(int rowNumber, int colNumber, Room room){
        return rowNumber * room.getColumns() + colNumber;
    }

    //metodo per ottenere tutte le projections raggruppate in base al movie
    public Map<String, ArrayList<Object>> getProjectionsPerMovie(Projection[] projections) {

        Map<String, ArrayList<Object>> returnMap = new HashMap<String, ArrayList<Object>>();

        // per ogni projection, viene controllato il movieID, in modo tale da poter
        // raggruppare tutte le projections in base al movie
        for (int i = 0; i < projections.length; i++) {

            // viene trovato il movie associato alla projection corrente
            Response responseMovie = getMovie(projections[i].getmovieID());
            Movie movie = (Movie) responseMovie.getEntity();

            ArrayList<Object> ProjectionsPerMovie = returnMap.get(movie.getId());

            // se nella returnMap, per il movie considerato, non c'è alcuna proiezione
            if (ProjectionsPerMovie == null) {
                // viene inizializzata la lista e inserito come primo elemento il movie
                ProjectionsPerMovie = new ArrayList<Object>();
                ProjectionsPerMovie.add(movie);
            }

            // viene aggiunta alla lista la proiezione corrente
            ProjectionsPerMovie.add(projections[i]);
            // viene aggiornata la lista nella mappa per quel movie
            returnMap.put(movie.getId(), ProjectionsPerMovie);
        }

        return returnMap;
    }

    //metodo per calcolare i posti da rimuovere da una prenotazione
    public List<Integer> getSeatsToRemove(List<Integer> oldReservedSeats, List<Integer> newReservedSeats){
        List <Integer> seatsToRemove = new ArrayList<Integer>();
        
        //Per ogni posto in oldReservation viene controllato se è presemte in newReservation, 
        //se manca viene aggiunto alla lista di posti da rimuovere.
        for(int i = 0; i < oldReservedSeats.size(); i++){
            boolean found = false; 
            for(int j = 0; j < newReservedSeats.size(); j++){
                if(oldReservedSeats.get(i) == newReservedSeats.get(j)){
                    found = true;
                    break;
                }
            }      
            if(!found){
                seatsToRemove.add(oldReservedSeats.get(i));
            }
        }

        return seatsToRemove;
    }

    //funzione che controlla se il database ha ritornato un messaggio di errore
    public boolean checkErrorMessage(String JSONString){
        return JSONString.substring(0, 4).equals("+ERR");
    }

    public void openDBConnection() throws UnknownHostException, IOException{
        // Si apre una socket verso il database, si ottengono i dati e si
        // costruisce la risposta.
        DB = new Socket(IP_ADDRESS, DBPORT);
        // si inizializzano gli stream di input e output
        outputStream = new PrintWriter(DB.getOutputStream(), true);
        inputStream = new BufferedReader(new InputStreamReader(DB.getInputStream()));
    }

    public void closeDBConnection() throws IOException {
        outputStream.println("+CLOSE");
        inputStream.close();
        outputStream.close();
        DB.close();
    }

    public String readStream() throws IOException{
        String inputLine = "";
        String JSONString = "";
        while (!(inputLine = inputStream.readLine()).equals("+END")) {
            JSONString += inputLine;
        }
        return JSONString;
    }
}