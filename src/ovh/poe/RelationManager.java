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

    /**
     * Reads currency and icon relation data from file on object init
     */
    public RelationManager() {
        readCurrencyRelationsFromFile();
    }

    /**
     * Reads currency relation data from file and adds it to maps
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
}
