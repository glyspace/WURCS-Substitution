package org.glygen.wurcsreplacement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;

public class WURCSSubstituentReplacer {

    public static void main(String[] args) {
        try {
            Map<String, Object> params = parseArgs(args);

            String inputFile = (String) params.get("-f");
            String substituent = (String) params.get("-s");
            String replacement = (String) params.get("-r");
            String outputFile = (String) params.get("-o");

            List<String> uList = (List<String>) params.getOrDefault("-u", new java.util.ArrayList<>());

            if (inputFile == null || substituent == null || replacement == null || outputFile == null) {
                System.err.println("Usage: -f <input> -s <substituent> -r <replacement> -o <output>");
                System.exit(1);
            }

            processFile(inputFile, substituent, replacement, uList, outputFile);

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<String, Object> parseArgs(String[] args) {
        Map<String, Object> params = new HashMap<>();
        java.util.List<String> uList = new java.util.ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String key = args[i];

            if (key.equals("-u")) {
                if (i + 1 < args.length) {
                    uList.add(args[++i]);
                }
            } else {
                if (i + 1 < args.length) {
                    params.put(key, args[++i]);
                }
            }
        }

        if (!uList.isEmpty()) {
            params.put("-u", uList);
        }

        return params;
    }

    private static void processFile(String inputFile,
                                    String substituent,
                                    String replacement,
                                    List<String> uList,
                                    String outputFile) throws IOException {

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                if (line.trim().isEmpty()) {
                    writer.newLine();
                    continue;
                }

                String modified = line.replace(substituent, replacement);
                
                try {
                	// handle uncertain terminal
                    if (!uList.isEmpty()) {
                    	modified = handleUncertainTerminal (modified, uList);
                    }
                    // Validate with WURCS framework
                    validateWURCS(modified);

                    // Write only valid sequences
                    writer.write(modified);
                    writer.newLine();

                } catch (Exception e) {
                    System.err.println(
                            "Line " + lineNumber +
                            " | WURCS: " + modified +
                            " | Error: " + e.getMessage()
                    );
                }
            }
        }
    }
    
    private static String handleUncertainTerminal(String wurcs, List<String> uList) throws Exception {
	    // Split by "/" but ignore "/" inside brackets []
	    java.util.List<String> parts = new java.util.ArrayList<>();
	    StringBuilder current = new StringBuilder();

	    int bracketLevel = 0;

	    for (char c : wurcs.toCharArray()) {
	        if (c == '[') bracketLevel++;
	        if (c == ']') bracketLevel--;

	        if (c == '/' && bracketLevel == 0) {
	            parts.add(current.toString());
	            current.setLength(0);
	        } else {
	            current.append(c);
	        }
	    }
	    parts.add(current.toString());

	    if (parts.size() != 5) {
	        throw new Exception("Invalid WURCS format: expected 5 parts but got " + parts.size());
	    }

	    String[] nums = parts.get(1).split(",");
	    if (nums.length != 3) {
	        throw new Exception("Invalid second part format: " + parts.get(1));
	    }

	    try {
	        int third = Integer.parseInt(nums[2]);
	        third += uList.size(); 
	        nums[2] = String.valueOf(third);
	    } catch (NumberFormatException e) {
	        throw new Exception("Invalid number in second part: " + parts.get(1));
	    }

	    String modifiedPart2 = String.join(",", nums);

	    String part5 = parts.get(4);
	    // Extract sequences like a, b, c ... Z, aa, ab ...
	    java.util.Set<String> sequences = new java.util.LinkedHashSet<>();
	    java.util.regex.Matcher m = java.util.regex.Pattern
	            .compile("[a-zA-Z]+")
	            .matcher(part5);
	    while (m.find()) {
	        sequences.add(m.group());
	    }
	    
	    StringBuilder appendBuilder = new StringBuilder();
	    for (String u : uList) {
	        appendBuilder.append("_");
	        int i = 0;
	        for (String seq : sequences) {
	            appendBuilder.append(seq).append("?");

	            if (i < sequences.size() - 1) {
	                appendBuilder.append("|");
	            }
	            i++;
	        }

	        appendBuilder.append("}");
	        appendBuilder.append(u);
	    }
	    parts.set(1, modifiedPart2);

	    String base = String.join("/", parts);
	    return base + appendBuilder.toString();
	}

	private static void validateWURCS(String wurcs) throws Exception {
        WURCSValidator validator = new WURCSValidator();
        validator.start(wurcs);
        if (validator.getReport().hasError()) {
            String errorMessage = "";
            for (String error: validator.getReport().getErrors()) {
                errorMessage += error + ", ";
            }
            errorMessage = errorMessage.substring(0, errorMessage.lastIndexOf(","));
            throw new IllegalArgumentException ("WURCS validation error. Reason " + errorMessage);
        } 
    }
}