import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchProduct {

    public static final String LISTINGS_FILENAME = "/listings.txt";
    public static final String PRODUCTS_FILENAME = "/products.txt";
    public static final String RESULTS_FILENAME = "/results.txt";

    // HashMap that contains the manufacturer to listings mapping
    public static HashMap<String, List<JSONObject>> manufacturerToListingMap = new HashMap<>();
    // HashMap that contains the product_name to listings mapping
    public static HashMap<String, List<JSONObject>> resultsMap = new HashMap<>();

    public static String currentDirectory = System.getProperty("user.dir");

    /**
     * Creates the manufacturer to listings mapping. Groups all the listings that has the same manufacturer
     *
     * @throws IOException
     */
    public static void createManufacturerToListingMap() throws IOException {

        FileReader reader = new FileReader(currentDirectory + LISTINGS_FILENAME);
        BufferedReader br = new BufferedReader(reader);
        String line = null;

        while ((line = br.readLine()) != null) {
            JSONObject listingJsonObj = new JSONObject(line);
            String manufacturerName = listingJsonObj.get("manufacturer").toString().toLowerCase();

            if (manufacturerToListingMap.get(manufacturerName) != null) {
                manufacturerToListingMap.get(manufacturerName).add(listingJsonObj);
            } else {
                List<JSONObject> listingList = new ArrayList<JSONObject>();
                listingList.add(listingJsonObj);
                manufacturerToListingMap.put(manufacturerName, listingList);
            }
        }

        br.close();
    }

    /**
     * exact string matching is done between the two params
     *
     * Search-by-one process is to match strings that are off by a dash. For example,
     * "Cybershot" and "Cyber-Shot" family name, "VG120" and "VG-120" model name
     * Assume - Using the convention of each word being separated by spaces in the listing title
     *
     * @param sourceString
     * @param targetString
     * @return whether the targetString exists in the sourceString or not
     */
    public static boolean isContains(String sourceString, String targetString) {

        char[] source = sourceString.toCharArray();
        char[] target = targetString.toCharArray();
        int sourceCount = sourceString.length();
        int targetCount = targetString.length();

        char first = target[0];
        int max = sourceCount - targetCount;

        for (int i = 0; i <= max; i++) {
            // Look for first character
            if (source[i] != first) {
                while (++i <= max && source[i] != first) ;
            }

            // Found first character, now look at the rest of source string
            if (i <= max) {
                if ((i != 0) && (source[i-1] != ' ')) {
                    continue;
                }
                int j = i + 1;
                int end = j + targetCount - 1;
                boolean overLookDashOnce = false;
                int k;
                for (k = 1; j < end; j++, k++) {
                    try {
                        if (j >= sourceCount) {
                            break;
                        }
                        else if ((source[j] != target[k]) && ((source[j] == '-') || (target[k] == '-')) &&
                                (!overLookDashOnce)) {
                            if (source[j] == '-') {
                                j++;
                                end++;
                                overLookDashOnce = true;
                            } else if (target[k] == '-') {
                                k++;
                                end--;
                                overLookDashOnce = true;
                            }
                        } else if ((source[j] != target[k])) {
                            break;
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.err.println("ERROR: index is out of bounds -> " + e.getMessage());
                    }
                }
                try {
                    if ((j == end) && (k == targetCount)) {
                        if ((j >= sourceCount) || (source[j] == ' ')) {
                            return true;
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.println("ERROR: index is out of bounds -> " + e.getMessage());
                }

            }
        }
        return false;
    }

    /**
     * compares a product's model and family (if exists) and matches it with the given listing JSON object
     *
     * @param productObj
     * @param listingObj
     * @return boolean whether the model and family name matches or not
     */
    public static boolean familyAndModelMatch(JSONObject productObj, JSONObject listingObj) {
        String familyName = "";
        String modelName = productObj.get("model").toString().toLowerCase();
        String listingName = listingObj.get("title").toString().toLowerCase();

        boolean modelMatch = false;
        boolean familyMatch = false;

        // First attempt of searching exact string or off by one string if family name exists
        if (productObj.has("family")) {
            familyName = productObj.get("family").toString().toLowerCase();
            if (isContains(listingName, familyName)) {
                familyMatch = true;
            } else {
                // second attempt of replacing the spaces with dashes and searching again
                Pattern pattern = Pattern.compile("\\s");
                Matcher matcher = pattern.matcher(modelName);
                if (matcher.find()) {
                    familyName = familyName.replace(" ", "-");
                    if (isContains(listingName, familyName)) {
                        familyMatch = true;
                    }
                }
            }
        } else {
            familyMatch = true;
        }


        // First attempt of searching exact string or off by one string
        if (isContains(listingName, modelName)) {
            modelMatch = true;
        } else {
            // second attempt of replacing the spaces with dashes and checking again
            Pattern pattern = Pattern.compile("\\s");
            Matcher matcher = pattern.matcher(modelName);
            if (matcher.find()) {
                modelName = modelName.replace(" ", "-");
                if (isContains(listingName, modelName)) {
                    modelMatch = true;
                }
            }
        }

        return (familyMatch && modelMatch);
    }

    /**
     * Parses the products file and matches the model and filename to all the listings that corresponds to the
     * manufacturer for a certain product
     *
     * @return nothing
     * @throws IOException
     */
    public static void parseProductFile() throws IOException {
        FileReader reader = new FileReader(currentDirectory + PRODUCTS_FILENAME);
        BufferedReader br = new BufferedReader(reader);
        String line = null;

        while ((line = br.readLine()) != null) {
            JSONObject productJsonObj = new JSONObject(line);
            String manufacturerName = productJsonObj.get("manufacturer").toString().toLowerCase();
            String productName = productJsonObj.get("product_name").toString();

            if (manufacturerToListingMap.get(manufacturerName) != null) {

                Iterator listingsIterator = manufacturerToListingMap.get(manufacturerName).iterator();

                // Go through each listing item for the specific manufacturer
                while (listingsIterator.hasNext()) {
                    JSONObject listingJsonObj = (JSONObject) listingsIterator.next();

                    if (familyAndModelMatch(productJsonObj, listingJsonObj)) {
                        if (resultsMap.get(productName) != null) {
                            resultsMap.get(productName).add(listingJsonObj);
                        } else {
                            List<JSONObject> newListingList = new ArrayList<JSONObject>();
                            newListingList.add(listingJsonObj);
                            resultsMap.put(productName, newListingList);
                        }
                    }
                }
                // Debugging: output the product names that are not present in results.txt
//                if (resultsMap.get(productName) == null) {
//                    System.out.print(", " + productName + ", ");
//                }
            }
        }
        System.out.println();

        br.close();
    }

    /**
     * prints a HashMap<String, List<JSONObject>> hashmap. For debugging purposes
     *
     * @param hm input hashmap that is printed out
     * @return nothing
     */
    public static void printHashMap(HashMap<String, List<JSONObject>> hm) {
        for (HashMap.Entry<String, List<JSONObject>> entry : hm.entrySet()) {
            System.out.println(entry.getKey() + ": ");
            List listingList = entry.getValue();
            for (int i = 0; i < listingList.size(); i++) {
                System.out.println("   " + listingList.get(i).toString());
            }
        }
    }

    /**
     * Parses a HashMap<String, List<JSONObject>> hashmap and writes it to the current directory
     *
     * @param hm       the hashmap to parse and write
     * @param fileName the name of the file to write to
     * @return nothing
     */
    public static void writeToFile(HashMap<String, List<JSONObject>> hm, String fileName) throws IOException {
        FileWriter fw = new FileWriter(currentDirectory + fileName);

        Iterator resultsIterator = hm.keySet().iterator();
        while (resultsIterator.hasNext()) {
            String productName = (String) resultsIterator.next();
            List<JSONObject> listings = hm.get(productName);
            fw.write("{\"product_name\":\"" + productName + "\",\"listings\":" + listings + "}\n");
        }

        fw.close();
    }

    /**
     * Main method that calls all the helper functions and shows the execution time
     *
     * @param args not used
     * @return nothing
     */
    public static void main(String[] args) throws IOException {
        final long startTime = System.currentTimeMillis();

        createManufacturerToListingMap();
        parseProductFile();
        writeToFile(resultsMap, RESULTS_FILENAME);

        // Test cases to test isContains.
        // TODO: Remove this in the next commit
//        System.out.println("Example 1 -> " + isContains("This is a test string", "test")); // true
//        System.out.println("Example 2 -> " + isContains("This is a test string", "tests")); // false
//        System.out.println("Example 3 -> " + isContains("This is a tests string", "test")); // false
//        System.out.println("Example 4 -> " + isContains("This is a test-123 string", "test123")); // true
//        System.out.println("Example 5 -> " + isContains("This is a test123 string", "test-123")); // true
//        System.out.println("Example 6 -> " + isContains("canon battery grip bg-1", "g12")); // false
//        System.out.println("Example 6 -> " + isContains("Olympus 228185 VG-120 Digital Camera (Red)\",\"manufacturer", "VG120"));
//        System.out.println("Example 6 -> " + isContains("Samsung TL240 - Digital camera - compact - 14.2 Mpix - optical zoom: 7 x - supported memory: microSD, microSDHC - gray\n", "TL240"));
//        System.out.println("Example 7 -> " + isContains("Fujifilm FinePix Z900EXR Digital Still Camera - Red (16MP, 5x Optical Zoom) 3.5 inch LCD Touch Screen", "FinePix"));

        final long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");
    }
}