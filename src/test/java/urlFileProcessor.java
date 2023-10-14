import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class urlFileProcessor {
    public static void main(String[] args) {
        // Set the file path
        String filePath = "LowerEnvLinks.txt";

        try {
            // Create readers and writers for the file
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            Path tempFilePath = Files.createTempFile(Paths.get("."), "temp", ".txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFilePath.toFile()));

            // Read each URL from the file, add ".html" to the end of the URL, and write to the temporary file
            String url;
            while ((url = reader.readLine()) != null) {
                url = url.trim() + ".html";
                writer.write(url);
                writer.newLine();
            }

            // Close the readers and writers
            reader.close();
            writer.close();

            // Replace the original file with the temporary file
            Files.delete(Paths.get(filePath));
            Files.move(tempFilePath, Paths.get(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
