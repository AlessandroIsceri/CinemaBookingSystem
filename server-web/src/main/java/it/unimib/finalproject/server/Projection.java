package it.unimib.finalproject.server;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonFormat;


public class Projection{
    private String id;
    private String movieID;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    
    @JsonFormat(pattern = "HH:mm", locale = "it")
    private LocalTime time;
   
    private String roomNumber;
    private boolean availableSeats[][];

    public Projection(String id, String movieID, LocalDate date, LocalTime time, String roomNumber, boolean availableSeats[][]){
        this.id = id;
        this.movieID = movieID;
        this.roomNumber = roomNumber;
        this.date = date;
        this.time = time;
        this.availableSeats = availableSeats;
    }

    public Projection(){
        super();
    }

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getmovieID() {
        return movieID;
    }

    public void setmovieID(String movieID) {
        this.movieID = movieID;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }
    
    public boolean[][] getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(boolean[][] availableSeats) {
        this.availableSeats = availableSeats;
    }

    public String toString(){
        String s = "";
        s = "movieID: " + movieID + ", ";
        s += "date: " + date.toString() + ", ";
        s += "time: " + time.toString() + ", ";
        s += "room: " + roomNumber + ", ";
        s += "availableSeats: " + Arrays.toString(availableSeats);
        return s;
    }
    
    /*
        Per i posti viene utilizzata una matrice:
        
        Il posto (identificato da riga e colonna) è associato al numero del posto nel seguente modo:
        numeroColonnaPosto = numeroPosto % numeroColonne,
        numeroRigaPosto = numeroPosto / numeroColonne.
    
        Viceversa, a partire dal numero di riga e di colonna del posto si può risalire al numero del posto nel seguente modo:
        numeroPosto = numeroRigaPosto * colonneDellaSala + numeroColonnaPosto;
        
        Esempio Numerico: data una sala 2x7 identificata da una matrice m[2][7],
        dove m(i,j) = numero del posto di riga i e colonna j:
       
        0, 1, 2, 3,  4,  5,  6
        7, 8, 9, 10, 11, 12, 13

        (N.B: le matrici sono 0-based)
        ipotizzando di dover calcolare la riga e la colonna che identificano il posto 9,
        si procede come segue:
        
        colonna = 9 % 7 = 2
        riga = 9 / 7 = 1
    
        Viceversa, partendo da riga = 1, colonna = 2,
        se si vuole calcolare il numero del posto si procede come segue:
        numPosto = 1 * 7 + 2 = 9

    */
    
    //metodo per rendere un posto non disponibile (quindi uguale a false)
    public void setUnavailable(int numPosto){
        int numColonne = availableSeats[0].length;
        int riga = numPosto / numColonne;
        int colonna = numPosto % numColonne;
        availableSeats[riga][colonna] = false;
    }

    //metodo per rendere un posto disponibile (quindi uguale a true)
    public void setAvailable(int numPosto){
        int numColonne = availableSeats[0].length;
        int riga = numPosto / numColonne;
        int colonna = numPosto % numColonne;
        availableSeats[riga][colonna] = true;
    }

    //metodo per controllare se un posto è disponibile
    public boolean isAvailable(int numPosto){
        int numColonne = availableSeats[0].length;
        int riga = numPosto / numColonne;
        int colonna = numPosto % numColonne; 
        return availableSeats[riga][colonna];
    }

}
