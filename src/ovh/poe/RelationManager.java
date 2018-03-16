package ovh.poe;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;

/**
 * Maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    private Gson gson = Main.getGson();

    public Map<String, String> indexToName = new HashMap<>();
    public Map<String, String> nameToIndex = new HashMap<>();
    public Map<String, String> aliasToIndex = new HashMap<>();
    public Map<String, String> aliasToName = new HashMap<>();

    public Map<String, Integer> iconToIndex = new HashMap<>();
    public Map<Integer, String> indexToIcon = new TreeMap<>();

    public Map<String, List<String>> categories = new HashMap<>();

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
        File file = new File("./iconRelations.json");

        // Open up the reader
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            Type listType = new TypeToken<HashMap<Integer, String>>(){}.getType();
            HashMap<Integer, String> relations = gson.fromJson(reader, listType);

            // Lambda loop
            relations.forEach((key, value) -> {
                iconToIndex.put(value, key);
                indexToIcon.put(key, value);
            });

        } catch (IOException ex) {
            // Doesn't matter if the file exists or not. It will be written to later.
        }
    }

    /**
     * Saves data to file on program exit
     */
    public void saveData() {
        // Save icon relations to file
        File iconFile = new File("./iconRelations.json");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(iconFile), "UTF-8"))) {
            gson.toJson(indexToIcon, writer);
        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write to icoRelations.json");
            ex.printStackTrace();
        }

        // Save item categories to file
        File categoryFile = new File("./categories.json");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(categoryFile), "UTF-8"))) {
            gson.toJson(categories, writer);
        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write to categories.json");
            ex.printStackTrace();
        }
    }

    /**
     * Provides an interface for saving and retrieving icons and indexes
     *
     * @param icon Image url to add
     * @return Generated index of added url
     */
    public int addIcon(String icon) {
        // If icon is already present, return icon index
        if (iconToIndex.containsKey(icon)) return iconToIndex.get(icon);

        // Otherwise add to map and return icon index
        int index = iconToIndex.size();
        iconToIndex.put(icon, index);
        indexToIcon.put(index, icon);
        return index;
    }

    /**
     * Manages item category list
     *
     * @param parentCategory Parent category of item (e.g. "armour" or "flasks")
     * @param childCategory Child category of item (e.g. "gloves" or null)
     */
    public void addCategory(String parentCategory, String childCategory) {
        List<String> childCategories = categories.getOrDefault(parentCategory, new ArrayList<>());

        if (childCategory != null && !childCategories.contains(childCategory)) childCategories.add(childCategory);

        categories.putIfAbsent(parentCategory, childCategories);
    }
}
