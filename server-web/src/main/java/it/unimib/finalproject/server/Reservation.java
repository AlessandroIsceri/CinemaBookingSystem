package it.unimib.finalproject.server;

import java.util.List;
import java.util.ArrayList;

public class Reservation{
    private String id;
    private String projectionID;
    private List<Integer> reservedSeats;

    public Reservation(String id, String projectionID, int[] reservedSeats){
        this.reservedSeats = new ArrayList<Integer>();
        this.id = id;
        this.projectionID = projectionID;
        initReservedSeats(reservedSeats);
    }

    public Reservation(){
        super();
    }

    //metodo utilizzato per trasferire gli oggetti di un array nell'arrayList
    private void initReservedSeats(int[] reservedSeats) {
        for(int i = 0; i < reservedSeats.length; i++)
            this.reservedSeats.add(reservedSeats[i]);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectionID() {
        return projectionID;
    }

    public void setProjectionID(String projectionID) {
        this.projectionID = projectionID;
    }
    
    public void setReservedSeats(List<Integer> reservedSeats) {
        this.reservedSeats = reservedSeats;
    }
    
    public List<Integer> getReservedSeats() {
        return reservedSeats;
    }
    
    //metodo che rimuove il posto di numero indicato dalla prenotazione
    public void removeSeat(int seat){
        reservedSeats.remove(Integer.valueOf(seat));
    }

    public String toString(){
        String s = "";
        s = "id: " + id + ", ";
        s += "projection: " + projectionID +", ";
        s += "reservedSeats: " +  reservedSeats.toString();
        return s;
    }
}