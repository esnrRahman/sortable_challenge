import org.json.JSONArray;
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
     * @param source
     * @param subItem
     * @return
     */
    public static boolean isContain(String source, String subItem){
        // TODO: Do a search-by-one (SBO) process and replace space with dash (RSD) process to improve matching of products.
        // SBO process is to match strings for example "Cybershot" and "Cyber-Shot" family name, "VG120" and "VG-120" model name
        // RSD process is to match strings for example "Mju 9010" and "Mju-9010"
        String pattern = "\\b" + subItem + "\\b";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(source);
        return m.find();
    }

    /**
     * compares a product's model and family (if exists) and matches it with the given listing JSON object
     * @param productObj
     * @param listingObj
     * @return boolean whether the model and family name matches or not
     */
    public static boolean familyAndModelMatch(JSONObject productObj, JSONObject listingObj) {
        String familyName = "";
        String modelName = productObj.get("model").toString().toLowerCase();
        String listingName = listingObj.get("title").toString().toLowerCase();

        boolean containsFamilyName = false;

        if (productObj.has("family")) {
            familyName = productObj.get("family").toString().toLowerCase();
            containsFamilyName = true;
        }

        if (isContain(listingName, modelName)) {
            // TODO: Need a more sophisticated string check
            if (containsFamilyName) {
                if (isContain(listingName, familyName)) {
                    return true;
                }
                return false;
            }
            return true;
        }
        return false;
    }


    /**
     * Parses the products file and matches the model and filename to all the listings that corresponds to the
     * manufacturer for a certain product
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
     * @param hm input hashmap that is printed out
     * @return nothing
     */
    public static void printHashMap(HashMap<String, List<JSONObject>> hm) {
        for (HashMap.Entry<String, List<JSONObject>> entry : hm.entrySet())
        {
            System.out.println(entry.getKey() + ": ");
            List listingList = entry.getValue();
            for (int i = 0; i < listingList.size(); i++) {
                System.out.println("   " + listingList.get(i).toString());
            }
        }
    }

    /**
     * Parses a HashMap<String, List<JSONObject>> hashmap and writes it to the current directory
     * @param hm the hashmap to parse and write
     * @param fileName the name of the file to write to
     * @return nothing
     */
    public static void writeToFile(HashMap<String, List<JSONObject>> hm, String fileName) throws IOException {
        FileWriter fw = new FileWriter(currentDirectory + fileName);

        Iterator resultsIterator = hm.keySet().iterator();
        while (resultsIterator.hasNext()) {
            String productName = (String) resultsIterator.next();
            List<JSONObject> listings = hm.get(productName);
            fw.write( "{\"product_name\":\"" + productName + "\",\"listings\":" + listings + "}\n");
        }

        fw.close();
    }

    /**
     * Main method that calls all the helper functions and shows the execution time
     * @param args not used
     * @return nothing
     */
    public static void main(String[] args) throws IOException {
        final long startTime = System.currentTimeMillis();

        createManufacturerToListingMap();
        parseProductFile();
        writeToFile(resultsMap, RESULTS_FILENAME);

        final long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime) + "ms" );
    }
}