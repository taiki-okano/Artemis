package de.tum.in.www1.artemis.e2eTests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

public class SignInDemo {

    private static WebDriver driver;

    private static String USERNAME = "ab12cde";

    private static String USER_PASSWORD = "test_user_pwd";

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.manage().window().setSize(new Dimension(1920, 1080));
        WebDriverWait wait = new WebDriverWait(driver, 30);

        // Open start webpage for all tests of this class
        driver.get("http://localhost:9000");
    }

    @AfterEach
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void correctWebPageTest() {
        WebElement selector = driver.findElement(By.cssSelector("jhi-home"));
        assertThat(selector.isDisplayed()).isEqualTo(true);
    }

    @Test
    public void correctHomeTitleTest() {
        WebElement h1 = driver.findElement(By.cssSelector("h1"));
        assertThat(h1.getText()).isEqualTo("Welcome to Artemis!");
    }

    @Test
    public void loginWithCorrectCredentialsTest() throws Exception {
        // Get the needed Elements
        WebElement usernameInput = driver.findElement(By.id("username"));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement signIn = driver.findElement(By.cssSelector("button[type=submit"));

        // Input credentials and submit
        usernameInput.sendKeys(USERNAME);
        passwordInput.sendKeys(USER_PASSWORD);
        signIn.click();

        // TODO: Replace implementation with something independent of website loading time
        Thread.sleep(2000);

        // Check that the signIn was successful i.e. that we are forwarded the correct webpage
        assertThat(driver.getCurrentUrl()).isEqualTo("http://localhost:9000/courses");
        // Check that this webpage contains the necessary content
        WebElement selector = driver.findElement(By.cssSelector("jhi-overview"));
        assertThat(selector.isDisplayed()).isEqualTo(true);
    }
}
