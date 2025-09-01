
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class test {

    // Convert character to digit value for different bases
    private static int digitOf(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'Z') return 10 + (c - 'A');
        if (c >= 'a' && c <= 'z') return 10 + (c - 'a');
        throw new IllegalArgumentException("Invalid digit: '" + c + "'");
    }

    // Parse string in given base to BigInteger
    private static BigInteger parseInBase(String s, int base) {
        BigInteger result = BigInteger.ZERO;
        BigInteger baseValue = BigInteger.valueOf(base);
        
        for (char c : s.toCharArray()) {
            int digit = digitOf(c);
            if (digit >= base) {
                throw new IllegalArgumentException("Digit '" + c + "' not valid for base " + base);
            }
            result = result.multiply(baseValue).add(BigInteger.valueOf(digit));
        }
        return result;
    }



    // Simple JSON parser for our specific format
    private static Map<String, Object> parseJson(String jsonStr) {
        Map<String, Object> result = new HashMap<>();
        
        // Remove outer braces and split by top-level commas
        jsonStr = jsonStr.trim();
        if (jsonStr.startsWith("{")) jsonStr = jsonStr.substring(1);
        if (jsonStr.endsWith("}")) jsonStr = jsonStr.substring(0, jsonStr.length() - 1);
        
        // Split carefully to handle nested objects
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceLevel = 0;
        boolean inQuotes = false;
        
        for (char c : jsonStr.toCharArray()) {
            if (c == '"' && (current.length() == 0 || current.charAt(current.length() - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == '{') braceLevel++;
                else if (c == '}') braceLevel--;
                else if (c == ',' && braceLevel == 0) {
                    parts.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }
        
        // Parse each part
        for (String part : parts) {
            int colonIndex = part.indexOf(':');
            if (colonIndex == -1) continue;
            
            String key = part.substring(0, colonIndex).trim().replaceAll("\"", "");
            String value = part.substring(colonIndex + 1).trim();
            
            if (value.startsWith("{") && value.endsWith("}")) {
                // Parse nested object
                Map<String, String> nested = new HashMap<>();
                String innerContent = value.substring(1, value.length() - 1);
                String[] pairs = innerContent.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        nested.put(kv[0].trim().replaceAll("\"", ""), 
                                  kv[1].trim().replaceAll("\"", ""));
                    }
                }
                result.put(key, nested);
            } else {
                value = value.replaceAll("\"", "");
                try {
                    result.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    // Lagrange interpolation to find polynomial value at given x
    private static BigInteger lagrangeInterpolation(List<Point> points, BigInteger x) {
        BigInteger result = BigInteger.ZERO;
        
        for (int i = 0; i < points.size(); i++) {
            BigInteger term = points.get(i).y;
            
            // Calculate Lagrange basis polynomial L_i(x)
            for (int j = 0; j < points.size(); j++) {
                if (i != j) {
                    BigInteger numerator = x.subtract(points.get(j).x);
                    BigInteger denominator = points.get(i).x.subtract(points.get(j).x);
                    term = term.multiply(numerator).divide(denominator);
                }
            }
            result = result.add(term);
        }
        return result;
    }
    
    // Point class to store (x, y) coordinates
    private static class Point {
        BigInteger x, y;
        
        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }
    
    private static void processJsonFile(String filePath) throws Exception {
        System.out.println("Processing: " + filePath);
        
        // Read and parse JSON file
        String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
        Map<String, Object> data = parseJson(jsonContent);
        
        // Get k value (minimum points needed)
        @SuppressWarnings("unchecked")
        Map<String, String> keys = (Map<String, String>) data.get("keys");
        int k = Integer.parseInt(keys.get("k"));
        
        // Extract coordinate points from JSON
        List<Point> points = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if ("keys".equals(entry.getKey())) continue;
            
            try {
                int x = Integer.parseInt(entry.getKey());
                @SuppressWarnings("unchecked")
                Map<String, String> valueData = (Map<String, String>) entry.getValue();
                
                int base = Integer.parseInt(valueData.get("base"));
                String value = valueData.get("value");
                BigInteger y = parseInBase(value, base);
                
                points.add(new Point(BigInteger.valueOf(x), y));
            } catch (NumberFormatException ignored) {}
        }
        
        // Use first k points for interpolation
        points.sort((p1, p2) -> p1.x.compareTo(p2.x));
        List<Point> selectedPoints = points.subList(0, k);
        
        // Find constant term by evaluating polynomial at x=0
        BigInteger constantTerm = lagrangeInterpolation(selectedPoints, BigInteger.ZERO);
        
        System.out.println(constantTerm);
    }
    
    public static void main(String[] args) throws Exception {
        // Define the JSON test case files
        String[] testFiles = {
            "test_case1.json",
            "test_case2.json"
        };
        
        // Process each test file
        for (String filePath : testFiles) {
            try {
                processJsonFile(filePath);
            } catch (Exception e) {
                System.err.println("Error processing " + filePath + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
