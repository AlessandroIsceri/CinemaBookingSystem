package it.unimib.finalproject.database;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Database {
    private Map<String, String> DB;
    //per gestione concorrenza
    private int writingThreads;
    private int readingThreads;
    private int waitingThreads;


    public Database(String fileName){
        this.writingThreads = 0;
        this.readingThreads = 0;
        this.waitingThreads = 0;
        DB = new HashMap<String, String>();
        //initHashMap inizializza la mappa DB coi dati del file
        initHashMap(fileName);
    }

    //metodo invocata nel caso in cui venisse inviata una richiesta di tipo +GET
    public String getRequest(String key){
        String returnValue = "";
        int colonIndex = key.indexOf(":");
        //keyType è la parte della chiave che non comprende l'identificatore, esempio: projection:4 -> projection
        String keyType = key.substring(0, colonIndex);
        String id = key.substring(colonIndex + 1);
        
        //gestione concorrenza
        waitToRead();

        //Se l'id corrisponde a "*", allora vengono ritornati tutti gli oggetti con keyType identico a quello passato in input
        //(in keyType:id), sotto forma di array
        if(id.equals("*")){
            returnValue += "[\n";

            for (String currentKey : DB.keySet()) {
                //viene trovato il currentKeyType per vedere se corrisponde a quello passato in input
                String currentKeyType = currentKey.substring(1, currentKey.indexOf(":"));
                //se il keyType corrisponde a quello passato in input e non si riferisce al MaxID, il valore corrente viene
                //aggiunto al messaggio di ritorno
                if(currentKeyType.equals(keyType) && !currentKey.equals(addQuotes(keyType + ":maxID"))){
                    returnValue += DB.get(currentKey) + ",\n";
                }
            }
            //viene rimossa l'ultima virgola del messaggio di ritorno
            returnValue = returnValue.substring(0, returnValue.length() - 2);
            returnValue += "\n]\n";
        }
        //altrimenti viene cercato l'oggetto con keyType:id corrispondente alla key passata in ingresso
        else{
            for (String currentKey : DB.keySet()) {
                //se la chiave corrente è uguale a quella di input, viene ritornato il valore corrente
                if(currentKey.equals(addQuotes(key))){
                    returnValue += DB.get(currentKey) + "\n";
                    break;
                }
            }
        }
        //gestione concorrenza
        endReading();

        return returnValue;
    }
        
    //metodo invocata nel caso in cui venisse inviata una richiesta di tipo +PUT
    public String putRequest(String key, String param){

        //gestione concorrenza
        waitToWrite();

        //viene cercato l'elemento che is vuole aggiornare (con currentKey = key)
        for (String currentKey : DB.keySet()) {
            if(currentKey.equals(addQuotes(key))){
                //se l'elemento esiste, il DB viene aggiornato col nuovo valore e viene ritornato "true"
                DB.put(currentKey, param);
                //gestione concorrenza
                endWriting();
                return "true";
            }
        }
        //gestione concorrenza
        endWriting();
        //altrimenti ritorno "false"
        return "false";
    }

    // metodo invocata nel caso in cui venisse inviata una richiesta di tipo +ADD
    public String addRequest(String key, String param){
        //gestione concorrenza
        waitToWrite();
        if(DB.get(key) == null){
            DB.put(addQuotes(key), param);
            //gestione concorrenza
            endWriting();
            return "true";
        }
        //gestione concorrenza
        endWriting();
        return "false";
    }

    //metodo invocata nel caso in cui venisse inviata una richiesta di tipo +DEL
    //viene cercato l'elemento che is vuole eliminare (con currentKey = key)
    public String delRequest(String key){
        
        //gestione concorrenza
        waitToWrite();
        for (String currentKey : DB.keySet()) {
            if(currentKey.equals(addQuotes(key))){
                //se l'elemento esiste, viene eliminato dal DB e viene ritornato "true"
                DB.remove(currentKey);
                //gestione concorrenza
                endWriting();
                return "true";
            }
        }
        //gestione concorrenza
        endWriting();
        //altrimenti ritorno "false"
        return "false";
    }
        
    //metodo invocata nel caso in cui venisse inviata una richiesta di tipo +NID
    //il metodo ritorna un nuovo id (utilizzabile) per il keyType segnalato in input

    public String getNewId(String keyType){
        
        //gestione concorrenza
        waitToRead();

        //viene trovato il maxID corrisponente alla currentKeyType
        String maxID = DB.get(addQuotes(keyType + ":maxID"));
        
        //gestione concorrenza
        endReading();

        //se maxID è null significa che non è stata inserita nessun'istanza con quel keyType
        if(maxID == null){
            //gestione concorrenza
            waitToWrite();
            //quindi viene inizializzato a 0 e viene ritornato
            DB.put(addQuotes(keyType + ":maxID"), addQuotes(""+0));
            //gestione concorrenza
            endWriting();
            return "0";
        }
        //altrimenti, se max è gia definito nel DB, ritorno il suo successore come maxID
        int newMaxID = Integer.parseInt(maxID.substring(1, maxID.length() - 1)) + 1;
        //e viene aggiornato il DB col nuovo valore massimo
        //gestione concorrenza
        waitToWrite();
        DB.put(addQuotes(keyType + ":maxID"), addQuotes("" + newMaxID));
        //gestione concorrenza
        endWriting();
        return "" + newMaxID;
    }

    // metodo di supporto che aggiunge doppi apici dove necessari
    public String addQuotes(String s){
        return "\"" + s + "\"";
    }

    //4 metodi per gestire la concorrenza
    //numero massimo di readers = 3, numero massimo di writers = 1
    private synchronized void waitToRead(){
        while(readingThreads >= 3 || writingThreads > 0 || waitingThreads > 0){
            try{
                wait();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        readingThreads++;
    }

    private synchronized void endReading(){
        readingThreads--;
        if(readingThreads == 0) 
            notifyAll();
    }

    private synchronized void waitToWrite(){
        waitingThreads++;
        while(readingThreads > 0 || writingThreads > 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        writingThreads++;
        waitingThreads--;
    }

    private synchronized void endWriting(){
        writingThreads--;
        notifyAll();
    }

    //metodo che legge il file contenente un backup del database e aggiorna il database in memory
    private void initHashMap(String fileName){
        try {
            //viene aperto il file
            File file = new File(fileName);
            ObjectMapper mapper = new ObjectMapper();        
            Scanner scanner = new Scanner(file);
            String fileContent = "";
    
            // viene letto il file
            while(scanner.hasNextLine()){
                fileContent += scanner.nextLine();
            }      
            scanner.close();

            // dopo aver letto il file, si riempie la mappa che rappresenta il DB in memory col contenuto
            JsonNode object = mapper.readTree(fileContent);

            //itero per riempire la mappa
            Iterator<Entry<String, JsonNode>> iterator = object.fields();
            while (iterator.hasNext()) {
                Entry<String, JsonNode> temp = iterator.next();
                //addquotes viene utilizzato per far si che la notazione JSON non vada persa (le stringhe non verrebbero rappresentate con le ")
                DB.put(addQuotes(temp.getKey()), temp.getValue().toString());
                iterator.remove();
            }

            //per ogni keyType inserita nel file, dobbiamo calcolare l'id massimo
            int colonIndex = -1;
            int maxID;
            Map<String, String> maxIDMap = new HashMap<String, String>();
            for (String currentKey : DB.keySet()) {
                colonIndex = currentKey.indexOf(":"); // trova l'indice di ":"
                
                // calcolo il massimo ID per quella chiave
                String keyType = currentKey.substring(0, colonIndex);
                
                //se il maxID è già stato calcolato, continuo a scorrere la mappa
                if(maxIDMap.get(keyType + ":maxID") != null){
                    continue;
                }
                //altrimenti calcolo il maxID
                maxID = initMaxId(keyType);
                //e lo inserisco nella mappa
                maxIDMap.put(keyType + ":maxID" + '"', addQuotes("" + maxID));
            }
            //aggiungo la mappa dei maxID nella mappa che rappresenta il DB
            DB.putAll(maxIDMap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }    
    }

    // metodo che, data una chiave in input, calcola e ritorna il maxID per quella chiave
    public int initMaxId(String keyType){
        int maxID = -1;
        int colonIndex = -1;
        int currentId = -1;
        String currentIdStr = "";
        
        for (String currentKey : DB.keySet()) {
            colonIndex = currentKey.indexOf(":");
            //se il keyType della chiave corrente corrisponde con quello passato in input
            if (currentKey.substring(0, colonIndex).equals(keyType)){
                // prendiamo l'id corrente (ex: projection{projectionId}-reservation:4 -> 4)
                currentIdStr = currentKey.substring(colonIndex + 1, currentKey.length() - 1);
                currentId = Integer.parseInt(currentIdStr);
                //ed eventualmente aggiorniamo il max
                if (maxID < currentId)
                    maxID = currentId;
            }
        }
        return maxID;
    }

    /*
    STAMPA DB, utile per visualizzare il contenuto del database in fase di testing
    public void printDB(){
        System.out.println("**********************\nDB:\n");
        for (Map.Entry<String, String> entry : DB.entrySet()) {
            System.out.println(entry.getKey() + "=" + entry.getValue());
        }
        System.out.println("**********************");
    }
    */

}
