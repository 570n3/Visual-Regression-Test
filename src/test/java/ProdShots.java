import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

public class ProdShots {
    private static final Logger LOGGER = Logger.getLogger(LowerShots.class.getName());
    private static final int THREAD_COUNT = 1;

    @Test
    public void imageShots() throws InterruptedException {
        String tableName = "links_" + System.currentTimeMillis();
        // File path of the text file containing the links
        String filePath = "ProdLinks.txt";
        // Folder path where the screenshots will be saved
        String folderPath = "ProdShots";

        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);
        FirefoxOptions options = new FirefoxOptions();
        options.setHeadless(true);

        WebDriverManager.firefoxdriver().clearDriverCache().setup();

        try {
            // Open the text file
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            List<String> links = new ArrayList<>();
            String link;

            // Read each line (which should be a link) from the text file
            while ((link = br.readLine()) != null) {
                links.add(link);
            }
            insertLinksToDatabase(links, tableName);

            // Retrieve links from the database table
            links = getLinksFromDatabase(tableName);
            System.out.println("Meaw" + links.size());

            // Calculate the chunk size for each thread
            int chunkSize = (int) Math.ceil((double) links.size() / THREAD_COUNT);

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

            for (int i = 0; i < THREAD_COUNT; i++) {
                final int chunkIndex = i;
                // Execute each chunk of links in a separate thread
                executor.execute(() -> {
                    try {
                        List<String> linksChunk = getLinksFromDatabase(chunkIndex, chunkSize, tableName);
                        FirefoxDriver driver = new FirefoxDriver(options);
                        processLinksChunk(driver, linksChunk, folderPath, tableName);
                        //driver.quit();
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            executor.shutdown();
            latch.await(); // Wait for all threads to finish

            // Check for missed links and process them
            List<String> missedLinks = getMissedLinksFromDatabase(tableName);
            if (!missedLinks.isEmpty()) {
                processMissedLinks(options, folderPath, tableName);
            }

            // Check for any remaining missed links and update the 'missed' column
            List<String> remainingMissedLinks = getMissedLinksFromDatabase(tableName);
            if (!remainingMissedLinks.isEmpty()) {
                updateMissedLinksInDatabase(remainingMissedLinks, tableName);
            }

        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> getLinksFromDatabase(String tableName) throws SQLException {
        List<String> links = new ArrayList<>();
        String selectQuery = "SELECT url FROM " + tableName;
        try (Connection conn = createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectQuery)) {
            while (rs.next()) {
                String link = rs.getString("url");
                links.add(link);
            }
        }
        return links;
    }

    // Process a chunk of links
    private void processLinksChunk(FirefoxDriver driver, List<String> linksChunk, String folderPath, String tableName) throws IOException, SQLException {
        try {
            for (String link : linksChunk) {
                LOGGER.info("Processing link: " + link);

                try {
                    // Navigate to the link
                    driver.get(link);

                    //waitForPageLoad(driver);

                    JavascriptExecutor jse = (JavascriptExecutor) driver;
                    try {
                        jse.executeScript("$(document).ready(function(){\n" +
                                "    $('#cookie-information-template-wrapper').html('');\n" +
                                "});");
                        jse.executeScript("$(document).ready(function() {\n" +
                                "    $('header').css('position','absolute');\n" +
                                "})");
                    } catch (WebDriverException e) {
                        System.out.println("An uncaught JavaScript exception has occurred: " + e.getMessage());
                        continue;
                    }

                    // Take a screenshot
                    takeScreenshot(driver, link, folderPath);
                } catch (Exception e) {
                    System.out.println("Error processing link: " + link);
                    e.printStackTrace();
                    // Store the missed link in the database
                    insertMissedLinkToDatabase(link, tableName);
                }
            }
        } finally {
            driver.quit();
        }
    }

    // Wait for the page to load completely
    private void waitForPageLoad(WebDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(webDriver -> ((JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState")
                        .equals("complete"));
    }

    // Take a screenshot of the current page
    private void takeScreenshot(WebDriver driver, String link, String folderPath) throws IOException {
        // Remove illegal characters from the link to use as the file name
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        File scrFile = ((FirefoxDriver) driver).getFullPageScreenshotAs(OutputType.FILE);
        String filename = getPathFromUrl(link);
        Path screenShotPath = Paths.get(folderPath + "/" + filename + ".png");
        File screenShot = screenShotPath.toFile();
        FileUtils.copyFile(scrFile, screenShot);
    }





    // Create a connection to the database
    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:prod.sqlite");
    }

    // Create the links table if it doesn't exist
    private void createLinksTableIfNotExists(String tableName) throws SQLException {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " +
                tableName +
                " (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, missed TEXT)";
        try (Connection conn = createConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableQuery);
        }
    }

    // Insert links into the database table
    private void insertLinksToDatabase(List<String> links, String tableName) throws SQLException {
        createLinksTableIfNotExists(tableName);
        String insertQuery = "INSERT INTO " +
                tableName +
                " (url) VALUES (?)";
        try (Connection conn = createConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            conn.setAutoCommit(false); // Disable auto-commit

            for (String link : links) {
                pstmt.setString(1, link);
                pstmt.addBatch(); // Add the prepared statement to the batch
            }

            pstmt.executeBatch(); // Execute the batch of insert statements
            conn.commit(); // Commit the changes
        }
    }

    // Insert a missed link into the database table
    private void insertMissedLinkToDatabase(String link, String tableName) throws SQLException {
        String updateQuery = "UPDATE "+ tableName +" SET missed = ? WHERE url = ?";
        try (Connection conn = createConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
            pstmt.setString(1, "true");
            pstmt.setString(2, link);
            pstmt.executeUpdate();
        }
    }

    // Retrieve links from the database table
    private List<String> getLinksFromDatabase(int chunkIndex, int chunkSize, String tableName) throws SQLException {
        List<String> links = new ArrayList<>();
        String selectQuery = "SELECT url FROM "+ tableName +" LIMIT ? OFFSET ?";
        try (Connection conn = createConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectQuery)) {
            pstmt.setInt(1, chunkSize);
            pstmt.setInt(2, chunkIndex * chunkSize);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String link = rs.getString("url");
                    links.add(link);
                }
            }
        }
        return links;
    }

    // Retrieve missed links from the database table
    private List<String> getMissedLinksFromDatabase(String tableName) throws SQLException {
        List<String> missedLinks = new ArrayList<>();
        String selectQuery = "SELECT url FROM " + tableName + " WHERE missed = 'true'";
        try (Connection conn = createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectQuery)) {
            while (rs.next()) {
                String link = rs.getString("url");
                missedLinks.add(link);
            }
        }
        return missedLinks;
    }
    private void processMissedLinks(FirefoxOptions options, String folderPath, String tableName) {
        List<String> missedLinks;
        try {
            missedLinks = getMissedLinksFromDatabase(tableName);
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving missed links from the database", e);
        }

        if (!missedLinks.isEmpty()) {
            FirefoxDriver driver = new FirefoxDriver(options);
            try {
                processLinksChunk(driver, missedLinks, folderPath, tableName);
            } catch (IOException | SQLException e) {
                throw new RuntimeException("Error processing missed links", e);
            }
        }
    }

    private void updateMissedLinksInDatabase(List<String> remainingMissedLinks, String tableName) {
        try (Connection conn = createConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE "+ tableName +" SET missed = 'false'");
            if (!remainingMissedLinks.isEmpty()) {
                String updateQuery = "UPDATE "+ tableName +" SET missed = 'true' WHERE url IN (";
                for (int i = 0; i < remainingMissedLinks.size(); i++) {
                    if (i > 0) {
                        updateQuery += ",";
                    }
                    updateQuery += "'" + remainingMissedLinks.get(i) + "'";
                }
                updateQuery += ")";
                stmt.execute(updateQuery);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating missed links in the database", e);
        }
    }


    private String getPathFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path.isEmpty() || path.equals("/")) {
                return "root";
            } else {
                String subPath = path.substring(path.indexOf("/en"));
                if (subPath.endsWith(".html")) {
                    subPath = subPath.substring(0, subPath.lastIndexOf(".html"));
                }
                return subPath.replaceAll("/", "_");
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return "";
    }
}
