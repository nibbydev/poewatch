package ovh.poe.Pricer;

import ovh.poe.Item;
import ovh.poe.Main;

import java.util.*;

/**
 * Price database entry object
 */
public class DataEntry {
    private class HourlyEntry {
        public double mean, median, mode;

        HourlyEntry (double mean, double median, double mode) {
            this.mean = mean;
            this.median = median;
            this.mode = mode;
        }
    }
    private class ItemEntry {
        public double price;
        public String accountName, id;

        ItemEntry (double price, String accountName, String id) {
            this.price = price;
            this.accountName = accountName;
            this.id = id;
        }
    }

    private int total_counter = 0;
    private int inc_counter = 0;
    private int dec_counter = 0;
    private double mean = 0.0;
    private double median, mode;
    private double threshold_multiplier = 0.0;
    private String key;
    private int iconIndex = -1;

    // Lists that hold price data
    private ArrayList<String> rawData = new ArrayList<>();
    private ArrayList<ItemEntry> database_items = new ArrayList<>(Main.CONFIG.baseDataSize);
    private ArrayList<HourlyEntry> database_hourly = new ArrayList<>(Main.CONFIG.hourlyDataSize);

    //////////////////
    // Main methods //
    //////////////////

    /**
     * Used to load data in on object initialization
     *
     * @param line Database entry from the CSV-format file
     */
    public DataEntry(String line) {
        parseLine(line);
    }

    /**
     * This is needed to create an instance without initial parameters
     */
    public DataEntry() {
    }

    /**
     * Adds entries to the rawData and database_itemIDs lists
     *
     * @param item Item object
     * @param accountName Account name of the seller
     */
    public void add(Item item, String accountName) {
        // Assign key if missing TODO: is this needed?
        if (key == null) key = item.getKey();

        // Add new value to raw data array
        rawData.add(item.getPrice() + "," + item.getPriceType() + "," + item.id + "," + accountName);

        // Get latest icon, if present
        if (item.icon != null) {
            // Get "?"'s index in url
            int tempIndex = (item.icon.contains("?") ? item.icon.indexOf("?") : item.icon.length());
            // Get everything before "?"
            String icon = item.icon.substring(0, tempIndex);
            // Save it in relationmanager and get index
            iconIndex = Main.RELATIONS.addIcon(icon);
        }
    }

    /**
     * Caller method. Calls other methods
     *
     * @param line Database entry from the CSV-format file
     */
    public void cycle(String line) {
        // Load data into lists
        parseLine(line);

        // Build statistics and databases
        parse();
        purge();
        build();

        // Limit list sizes
        cap();
    }

    /**
     * Caller method. Calls other methods
     */
    public void cycle() {
        // Build statistics and databases
        parse();
        purge();
        build();

        // Limit list sizes
        cap();
    }

    /////////////////////
    // Private methods //
    /////////////////////

    /**
     * Adds values from rawData array to prices database array
     */
    private void parse() {
        // Loop through entries
        for (String entry : rawData) {
            String[] splitEntry = entry.split(",");

            Double price = Double.parseDouble(splitEntry[0]);
            String priceType = splitEntry[1];
            String id = splitEntry[2];
            String account = splitEntry[3];

            // If a user already has listed the same item before, ignore it
            boolean discard = false;
            for (ItemEntry itemEntry : database_items) {
                if (itemEntry.accountName.equals(account) || itemEntry.id.equals(id)) discard = true;
            }
            if (discard) continue;

            // If the item was not listed for chaos orbs ("1" == Chaos Orb), then find the value in chaos
            if (!priceType.equals("1")) {
                // Get the database key of the currency the item was listed for
                String currencyKey = key.substring(0, key.indexOf("|")) + "|currency:orbs|" + Main.RELATIONS.indexToName.get(priceType) + "|5";

                // If there does not exist a relation between listed currency to Chaos Orbs, ignore the item
                if (!Main.PRICER_CONTROLLER.getCurrencyMap().containsKey(currencyKey)) continue;

                // Get the currency item entry the item was listed in
                DataEntry currencyEntry = Main.PRICER_CONTROLLER.getCurrencyMap().get(currencyKey);

                // If the currency the item was listed in has very few listings then ignore this item
                if (currencyEntry.getCount() < 20) continue;

                // Convert the item's price into Chaos Orbs
                price = price * currencyEntry.getMedian();
            }

            // Hard-cap item prices
            if (price > 500000.0 || price < 0.001) continue;

            // Add values to the front of the lists
            database_items.add(0, new ItemEntry(
                    Math.round(price * Main.CONFIG.pricePrecision) / Main.CONFIG.pricePrecision,
                    account,
                    id
            ));

            // Increment total added item counter
            inc_counter++;
        }

        // Clear raw data after extracting and converting values
        rawData.clear();
    }

    /**
     * Removes improper entries from databases
     */
    private void purge() {
        // Precautions
        if (database_items.isEmpty()) return;
        // If too few items have been found then it probably doesn't have a median price
        if (total_counter + inc_counter < 10) return;
        // No median price found
        if (median <= 0) return;

        // Loop through database_prices, if the price is lower than the boundaries, remove the first instance of the
        // price and its related account name and ID
        int offset = 0;
        int oldSize = database_items.size();
        for (int i = 0; i < oldSize; i++) {
            double price = database_items.get(i - offset).price;
            if (price < median * (2.0 + threshold_multiplier) && price > median / (2.0 + threshold_multiplier)) continue;

            database_items.remove(i - offset);

            // Since we removed elements with index i we need to adjust for the rest of them that fell back one place
            offset++;
        }

        // Increment discard counter by how many were discarded
        if (offset > 0) dec_counter += offset;
    }

    /**
     * Calculates mean/median
     */
    private void build() {
        // Calculate mean and median values
        double tempMean = findMean(0);
        double tempMedian = findMedian(0);
        double tempMode = findMode(0);

        // add to hourly
        database_hourly.add(0, new HourlyEntry(tempMean, tempMedian, tempMode));

        // Calculate mean and median values
        mean = findMean(1);
        median = findMedian(1);
        mode = findMode(1);

        // If more items were removed than added and at least 6 were removed, update counter by 0.1
        if (inc_counter > 0 && dec_counter > 0 && (dec_counter / (double)inc_counter) * 100.0 > 80)
            threshold_multiplier += 0.1;
        else if (inc_counter > 0 && threshold_multiplier > -1)
            threshold_multiplier -= 0.1;

        // Don't let it grow infinitely
        if (threshold_multiplier > 5) threshold_multiplier -= 0.2;

        if (Main.PRICER_CONTROLLER.clearStats) {
            total_counter += inc_counter;
            inc_counter = dec_counter = 0;
        }
    }

    /**
     * Soft-caps database lists
     */
    private void cap() {
        // If an array has more elements than specified, remove everything from the possible last index up until
        // however many excess elements it has

        if (database_items.size() > Main.CONFIG.baseDataSize) {
            database_items.subList(Main.CONFIG.baseDataSize, database_items.size() - 1).clear();
        }

        if (database_hourly.size() > Main.CONFIG.hourlyDataSize) {
            database_hourly.subList(Main.CONFIG.hourlyDataSize, database_hourly.size() - 1).clear();
        }
    }

    /**
     * Finds the mean value of an array
     *
     * @param method 0 for database_items, 1 for database_hourly
     * @return The mean of the array
     */
    private double findMean(int method) {
        double mean = 0.0;

        if (method == 0) {
            if (database_items.isEmpty()) return mean;
            for (ItemEntry entry : database_items) mean += entry.price;
            return Math.round(mean / database_items.size() * Main.CONFIG.pricePrecision) / Main.CONFIG.pricePrecision;
        } else {
            if (database_hourly.isEmpty()) return mean;
            for (HourlyEntry entry : database_hourly) mean += entry.mean;
            return Math.round(mean / database_hourly.size() * Main.CONFIG.pricePrecision) / Main.CONFIG.pricePrecision;
        }
    }

    /**
     * Finds the median of the given array
     *
     * @param method 0 for database_items, 1 for database_hourly
     * @return Median value of the list, shifted by however much specified in the config
     */
    private double findMedian(int method) {
        ArrayList<Double> tempList = new ArrayList<>();

        if (method == 0) {
            if (database_items.isEmpty()) return 0;
            for (ItemEntry entry : database_items) tempList.add(entry.price);
        } else {
            if (database_hourly.isEmpty()) return 0;
            for (HourlyEntry entry : database_hourly) tempList.add(entry.median);
        }

        Collections.sort(tempList);
        return Math.round(tempList.get(tempList.size() / Main.CONFIG.medianLeftShift) * Main.CONFIG.pricePrecision)
                / Main.CONFIG.pricePrecision;
    }

    /**
     * Finds the mode of the given array
     *
     * @param method 0 for database_items, 1 for database_hourly
     * @return Most frequently occurring value in the list
     */
    private double findMode(int method) {
        double maxValue = 0, maxCount = 0;

        if (method == 0) {
            for (ItemEntry entry_1 : database_items) {
                int count = 0;

                for (ItemEntry entry_2 : database_items) {
                    if (entry_2.price == entry_1.price) ++count;
                }

                if (count > maxCount) {
                    maxCount = count;
                    maxValue = entry_1.price;
                }
            }
        } else {
            for (HourlyEntry entry_1 : database_hourly) {
                int count = 0;

                for (HourlyEntry entry_2 : database_hourly) {
                    if (entry_2.mode == entry_1.mode) ++count;
                }

                if (count > maxCount) {
                    maxCount = count;
                    maxValue = entry_1.mode;
                }
            }
        }

        return maxValue;
    }

    /////////////////
    // I/O helpers //
    /////////////////

    /**
     * Converts this instance's values into CSV format
     *
     * @return CSV line
     */
    public String buildLine() {
        /* (Spliterator: "::")
            0 - key
            1 - stats (Spliterator: "," and ":")
                cnt: - total count during league
                inc: - added items per 24h
                dec: - discarded items 24h
                mea: - mean
                med: - median
                mod: - mode
                mtp: - threshold_multiplier
                ico: - icon index
            2 - database entries (Spliterator: "|" and ",")
                0 - price
                1 - account name
                2 - item id
            3 - hourly (Spliterator: "|" and ",")
                0 - mean
                1 - median
                1 - mode
         */

        StringBuilder stringBuilder = new StringBuilder();

        // Add key
        stringBuilder.append(key);
        stringBuilder.append("::");

        // Add statistics
        stringBuilder.append("cnt:");
        stringBuilder.append(total_counter);
        stringBuilder.append(",add:");
        stringBuilder.append(inc_counter);
        stringBuilder.append(",dec:");
        stringBuilder.append(dec_counter);
        stringBuilder.append(",mea:");
        stringBuilder.append(mean);
        stringBuilder.append(",med:");
        stringBuilder.append(median);
        stringBuilder.append(",mod:");
        stringBuilder.append(mode);
        stringBuilder.append(",mtp:");
        stringBuilder.append(Math.round(threshold_multiplier * 100.0) / 100.0);
        stringBuilder.append(",ico:");
        stringBuilder.append(iconIndex);

        // Add delimiter
        stringBuilder.append("::");

        // Add database entries
        if (database_items.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (ItemEntry entry : database_items) {
                stringBuilder.append(entry.price);
                stringBuilder.append(",");
                stringBuilder.append(entry.accountName);
                stringBuilder.append(",");
                stringBuilder.append(entry.id);
                stringBuilder.append("|");
            }

            // Remove the overflow "|"
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        // Add delimiter
        stringBuilder.append("::");

        // Add hourly entries
        if (database_hourly.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (HourlyEntry entry : database_hourly) {
                stringBuilder.append(entry.mean);
                stringBuilder.append(",");
                stringBuilder.append(entry.median);
                stringBuilder.append(",");
                stringBuilder.append(entry.mode);
                stringBuilder.append("|");
            }

            // Remove the overflow "|"
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        // Add newline and return string
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    /**
     * Reads values from CSV line and adds them to lists
     *
     * @param line CSV line
     */
    private void parseLine(String line) {
        /* (Spliterator: "::")
            0 - key
            1 - stats (Spliterator: "," and ":")
                cnt: - total count during league
                inc: - added items per 24h
                dec: - discarded items 24h
                mea: - mean
                med: - median
                mod: - mode
                mtp: - threshold_multiplier
                ico: - icon index
            2 - database entries (Spliterator: "|" and ",")
                0 - price
                1 - account name
                2 - item id
            3 - hourly (Spliterator: "|" and ",")
                0 - mean
                1 - median
                1 - mode
         */

        String[] splitLine = line.split("::");

        // Add key if missing
        if (key == null) key = splitLine[0];

        // Import statistical values
        if (!splitLine[1].equals("-")) {
            String[] values = splitLine[1].split(",");

            for (String dataItem : values) {
                String[] splitDataItem = dataItem.split(":");

                switch (splitDataItem[0]) {
                    case "cnt":
                        total_counter = Integer.parseInt(splitDataItem[1]);
                        break;
                    case "add":
                        inc_counter += Integer.parseInt(splitDataItem[1]);
                        break;
                    case "dec":
                        dec_counter += Integer.parseInt(splitDataItem[1]);
                        break;
                    case "mea":
                        mean = Double.parseDouble(splitDataItem[1]);
                        break;
                    case "med":
                        median = Double.parseDouble(splitDataItem[1]);
                        break;
                    case "mod":
                        mode = Double.parseDouble(splitDataItem[1]);
                        break;
                    case "mtp":
                        threshold_multiplier = Double.parseDouble(splitDataItem[1]);
                        break;
                    case "ico":
                        iconIndex = Integer.parseInt(splitDataItem[1]);
                        break;
                    default:
                        System.out.println("idk: " + splitDataItem[0]);
                        break;
                }
            }
        }

        // Import database_prices, account names and item IDs
        if (!splitLine[2].equals("-")) {
            for (String entry : splitLine[2].split("\\|")) {
                String[] entryList = entry.split(",");

                database_items.add(new ItemEntry(Double.parseDouble(entryList[0]), entryList[1], entryList[2]));
            }
        }

        // Import hourly mean and median values
        if (!splitLine[3].equals("-")) {
            for (String entry : splitLine[3].split("\\|")) {
                String[] entryList = entry.split(",");

                database_hourly.add(new HourlyEntry(Double.parseDouble(entryList[0]), Double.parseDouble(entryList[1]), Double.parseDouble(entryList[2])));
            }
        }
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public double getMean() {
        return mean;
    }

    public double getMedian() {
        return median;
    }

    public double getMode() {
        return mode;
    }

    public String getKey() {
        return key;
    }

    public int getCount() {
        return total_counter;
    }

    public int getInc_counter() {
        return inc_counter;
    }

    public int getDec_counter() {
        return dec_counter;
    }

    public int getIcon() {
        return iconIndex;
    }
}
