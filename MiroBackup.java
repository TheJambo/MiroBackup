package mirobackup;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.valueOf;


public class MiroBackup {
    static Properties appProps = new Properties();


    public static void main(String[] args) throws Exception {
        loadProperties();

        String NewLastRunDate = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
                .format(LocalDateTime.now());
        System.out.println("NewLastRunDate Date: " + NewLastRunDate);
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", "C:\\MiroBackup\\5Min");
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--start-maximized");
            WebDriver driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
        
            try {
                try {
                    doLogin(driver);
                    doBoardBackup(driver);

                } catch (ElementNotInteractableException e) {
                    System.out.println(e);
                }

            } catch (ElementNotInteractableException e) {
                System.out.println(e);
            } finally {
                driver.quit();

            }
    }

    public static void doLogin(WebDriver driver) throws Exception {
        driver.get("https://miro.com/sso/login/");
        try {
            while (!driver.getTitle().contains("Online Whiteboard for Visual Collaboration")) {
                Thread.sleep(5 * 1000); //Check for successful login every 5 seconds
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread.sleep(30000);
    }

    public static boolean downloadBoard(WebDriver driver, String boardID) throws Exception {
        driver.get("https://miro.com/api/v1/boards/" + boardID + "?archive=true");
        Thread.sleep(1000);
        String source = driver.getPageSource();
        if (source.contains("\"reason\" : \"insufficientPermission\",")) {
            System.out.println("Insufficient Permission - Not backed up.");
            System.out.println("Source: " + source);
            return false;
        }
        else if (source.contains("\"reason\" : \"boardNotFound\",")) {
            System.out.println("Could not find board " + boardID);
            driver.get("https://miro.com/app/dashboard/"); //Go back to dashboard, otherwise errors remain on the page and continuously trigger errors.
            return false;
        }
        else {
            System.out.println("Backing Up board \"" + boardID + "\"");
            return true;
        }
    }

    public static void doBoardBackup(WebDriver driver) throws Exception {

        try {
            while (true) { //try every 5 minutes to download all the boards
                try {
                    int x = 1;
                    while (true) { //Keep downloading boards in increasing numbers until it errors.
                        if (!downloadBoard(driver, appProps.getProperty("Board" + x + "ID")))
                        {
                            throw new InterruptedException("Could not Download");
                        }
                        x++;
                    }
                } catch (InterruptedException e) { //Go here when running out of boards to download.
                    System.out.println("[" + new Date() + "] Will try again in 5 minutes.");
                }
                loadProperties(); //Refresh properties to look for new boards
                Thread.sleep((long) Integer.parseInt(appProps.getProperty("TimeBetweenChecks")) * 60 * 1000); //Wait 5 minutes
            }
        } catch (InterruptedException e) { //Real errors.
            e.printStackTrace();
        }
    }

    private static void loadProperties() {
        try {
            Path PropertyFile = Paths.get("Miro.properties");

            Reader PropReader = Files.newBufferedReader(PropertyFile);
            appProps.load(PropReader);

            PropReader.close();
            System.setProperty("webdriver.chrome.driver", appProps.getProperty("ChromeDriverLocation"));

        } catch (IOException Ex) {
            System.out.println(
                    "Unable to find configuration file \"Miro.properties\". The file has been created for you.");
            createProperties();
        }

    }

    private static void createProperties() {
        Properties AppProps = new Properties();
        AppProps.setProperty("ChromeDriverLocation", "C:\\chromedriver.exe");
        AppProps.setProperty("Board1Name", "Team Board");
        AppProps.setProperty("Board2Name", "Retrospective Board");
        AppProps.setProperty("Board3Name", "Program Board");
        AppProps.setProperty("Board1ID", "AAA_1234=");
        AppProps.setProperty("Board2ID", "BBB_1234=");
        AppProps.setProperty("Board3ID", "CCC_1234=");
        AppProps.setProperty("SlackHookURL",
                "https://hooks.slack.com/services/xxxxxxxxx/xxxxxxxxx/xxxxxxxxxxxxxxxxxxxxxxxx");

        Path PropertyFile = Paths.get("Miro.properties");

        try {
            Writer PropWriter = Files.newBufferedWriter(PropertyFile);
            AppProps.store(PropWriter, "Application Properties");
            PropWriter.close();
        } catch (IOException Ex) { // No Properties file - Create one!
            System.out.println("Could not create file.");
        }
    }
}
