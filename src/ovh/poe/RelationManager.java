package ovh.poe;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    private Gson gson = Main.getGson();

    public Map<String, String> indexToName = new HashMap<>();
    public Map<String, String> nameToIndex = new HashMap<>();
    public Map<String, String> aliasToIndex = new HashMap<>();
    public Map<String, String> aliasToName = new HashMap<>();

    public Map<String, String> iconToIndex = new HashMap<>();
    public Map<String, String> indexToIcon = new HashMap<>();

    /**
     * Reads currency and icon relation data from file on object init
     */
    public RelationManager() {
        readCurrencyRelationsFromFile();
        readIconsFromFile();
    }

    /**
     * Reads currency relation data from file
     */
    private void readCurrencyRelationsFromFile() {
        File file = new File("./currencyRelations.json");

        // Open up the reader
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            Type listType = new TypeToken<ArrayList<Mappers.CurrencyRelation>>(){}.getType();
            List<Mappers.CurrencyRelation> relations = gson.fromJson(reader, listType);

            for (Mappers.CurrencyRelation relation : relations) {
                indexToName.put(relation.index, relation.name);
                nameToIndex.put(relation.name, relation.index);

                for (String alias : relation.aliases) {
                    aliasToIndex.put(alias, relation.index);
                    aliasToName.put(alias, relation.name);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Reads icon relation data from file
     */
    private void readIconsFromFile() {
        File file = new File("./icons.json");

        // Open up the reader
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            Type listType = new TypeToken<HashMap<String, String>>(){}.getType();
            HashMap<String, String> relations = gson.fromJson(reader, listType);

            // Lambda loop
            relations.forEach((key, value) -> {
                iconToIndex.put(value, key);
                indexToIcon.put(key, value);
            });

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Saves data to file on program exit
     */
    public void saveData() {
        File file = new File("./currencyRelations.json");

        // Open up the reader
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            gson.toJson(indexToIcon, writer);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
