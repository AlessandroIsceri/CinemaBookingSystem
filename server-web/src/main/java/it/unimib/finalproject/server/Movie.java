package it.unimib.finalproject.server;

public class Movie{
    private String id;
    private String title;
    private int duration;  
    private String description;

    public Movie(String id, String title, int duration, String description) {
        this.id = id;
        this.title = title;
        this.duration = duration;
        this.description = description;
    }
    
    public Movie(){
        super();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
	
    public String toString(){
        return "id: " + id + ", title: " + title + ", duration: " + duration + ", description: " + description;
    }
}