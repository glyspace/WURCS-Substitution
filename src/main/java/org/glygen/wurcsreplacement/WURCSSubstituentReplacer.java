package org.glygen.wurcsreplacement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;

public class WURCSSubstituentReplacer {

    public static void main(String[] args) {
        try {
            Map<String, String> params = parseArgs(args);

            String inputFile = params.get("-f");
            String substitutent = params.get("-s");
            String replacement = params.get("-r");
            String outputFile = params.get("-o");

            if (inputFile == null || substitutent == null || replacement == null || outputFile == null) {
                System.err.println("Usage: -f <input> -s <substituent> -r <replacement> -o <output>");
                System.exit(1);
            }

            processFile(inputFile, substitutent, replacement, outputFile);

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < args.length - 1; i += 2) {
            map.put(args[i], args[i + 1]);
        }
        return map;
    }

    private static void processFile(String inputFile,
                                    String substitutent,
                                    String replacement,
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

                String modified = line.replace(substitutent, replacement);

                try {
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
    
    private static void validateWURCS(String wurcs) throws Exception {
        WURCSValidator validator = new WURCSValidator();
        validator.start(wurcs);
        if (validator.getReport().hasError()) {
            String errorMessage = "";
            for (String error: validator.getReport().getErrors()) {
                errorMessage += error + ", ";
            }
            errorMessage = errorMessage.substring(0, errorMessage.lastIndexOf(","));
            throw new IllegalArgumentException ("WURCS conversion error. Reason " + errorMessage);
        } 
    }
}