package it.unimib.finalproject.server;

public class Room{
    
    private String roomNumber;
    private int rows;
    private int columns;

    public Room(){
        super();
    }

    public Room(String roomNumber, int rows, int columns) {
        this.roomNumber = roomNumber;
        this.rows = rows;
        this.columns = columns;
    }
    
    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public String getRoomNumber() {
        return roomNumber;
    }
    
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String toString(){
        return "roomNumber: " + roomNumber + ", rows: " +  rows + ", columns: " + columns;
    }
}