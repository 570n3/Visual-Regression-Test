import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.annotations.Test;

public class LinkManager {


        @Test
        public void Baitlinks() throws IOException {

            FirefoxOptions options = new FirefoxOptions();
            options.setHeadless(true);

            // Creating a new instance of FirefoxDriver
            WebDriverManager.firefoxdriver().clearDriverCache().setup();
            FirefoxDriver driver = new FirefoxDriver(options);

            driver.get("https://example.com/en/sitemap.xml");
            List<WebElement> elements = driver.findElements(By.xpath("//*[contains(text(),'https://example.com/en')]"));

            try (BufferedWriter bw = new BufferedWriter(new FileWriter("LowerEnvLinks.txt"))) {
                for (WebElement element : elements) {
                    String text = element.getText();
                    Pattern pattern = Pattern.compile("https://example.com/en");
                    Matcher matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        bw.write(text);
                        bw.newLine();
                    }
                }
            }

            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader("LowerEnvLinks.txt"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.isEmpty()) {
                        lines.add(line);
                    }
                }
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter("LowerEnvLinks.txt"))) {
                for (String line : lines) {
                    bw.write(line);
                    bw.newLine();
                }
            }

        }
    @Test
    public void Baselinks() throws IOException {

        FirefoxOptions options = new FirefoxOptions();
        options.setHeadless(true);

        // Creating a new instance of FirefoxDriver
        WebDriverManager.firefoxdriver().clearDriverCache().setup();
        FirefoxDriver driver = new FirefoxDriver(options);

        driver.get("https://example/en/sitemap.xml");
        List<WebElement> elements = driver.findElements(By.xpath("//*[contains(text(),'https://example.com/en')]"));

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("ProdLinks.txt"))) {
            for (WebElement element : elements) {
                String text = element.getText();
                Pattern pattern = Pattern.compile("https://example.com/en");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    bw.write(text);
                    bw.newLine();
                }
            }
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("ProdLinks.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("ProdLinks.txt"))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
        driver.quit();
    }
}