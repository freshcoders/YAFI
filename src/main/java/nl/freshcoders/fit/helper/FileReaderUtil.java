package nl.freshcoders.fit.helper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FileReaderUtil {
    public static String readFile(String filePath) {
        try {
            // Create a FileReader object to read the file
            FileReader fileReader = new FileReader(filePath);

            // Create a BufferedReader object to read the file line by line
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            // Create a StringBuilder object to concatenate the lines into a single string
            StringBuilder stringBuilder = new StringBuilder();

            // Read the first line from the file
            String line = bufferedReader.readLine();

            // Keep reading the lines from the file until we reach the end
            while (line != null) {
                // Append the line to the string builder
                stringBuilder.append(line);

                // Read the next line from the file
                line = bufferedReader.readLine();
            }

            // Close the BufferedReader
            bufferedReader.close();

            // Convert the string builder to a string
            String fileContent = stringBuilder.toString();

            return fileContent;
        } catch (FileNotFoundException e) {
            // Handle the exception if the file doesn't exist
            System.out.println("File not found: " + filePath);
            return null;
        } catch (IOException e) {
            // Handle any other I/O exceptions
            e.printStackTrace();
            return null;
        }
    }
}
