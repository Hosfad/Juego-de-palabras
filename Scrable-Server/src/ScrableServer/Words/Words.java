package ScrableServer.Words;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class Words {

    static File file = new File(Words.class.getResource("Cache.json").getFile());
    static Gson gson = new Gson();

    public static List<Word> all(){
        try {
            return gson.fromJson(new BufferedReader(new FileReader(file)),Cache.class).words;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
    public static Word getRandomWord(char c){

        return getRandomWord(w->w.name.toLowerCase().startsWith(String.valueOf(c).toLowerCase()));
    }
    public static Word getRandomWord(Predicate <Word> filter){
        List<Word> words = all(filter);
        return words.get((int) Math.floor( Math.random()*(words.size()  )));
    }

    public static List<Word> all(Predicate<Word> filter){
        List<Word> words = new ArrayList<>();
        for (Word word : all()) {
            if (filter.test(word)) {
                words.add(word);
            }
        }
        return words;
    }
    class Cache{
        public List<Word> words = new ArrayList<>();
    }

    public static void main(String[] args) {
        Map<String , Integer> dublicates = new java.util.HashMap<>();
        for (Word word : all()) {
            if (dublicates.containsKey(word.name)){
                dublicates.put(word.name,dublicates.get(word.name)+1);
            }else{
                dublicates.put(word.name,1);
            }
        }
        for (Map.Entry<String, Integer> entry : dublicates.entrySet()) {
            if (entry.getValue() > 1){
                System.out.println(entry.getKey() + " " + entry.getValue());
            }
        }


    }

}
