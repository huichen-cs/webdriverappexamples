package edu.vsu.cs.proglang.webdriveapp;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.base.Function;

public class GoogleSearch {
	private static final Logger log = Logger.getLogger(GoogleSearch.class
			.getName());

	private final String REMOTE_WEBDRIVER_URL = "http://localhost:4444/wd/hub";
	private final int TIME_OUT_SECONDS = 60;
	private final int POLLING_INTERVAL_SECONDS = 5;
		
	/**
	 * Google specific constants
	 */
	private final String GOOGLE_SEARCH_BOX_ID = "lst-ib";
	private final String GOOGLE_URL = "http://www.google.com";
	private final String GOOGLE_RESULT_PAGE_HEADING_XPATH = 
			"//h2[text()=\"Search Results\"]";
	
	/* the xpath can be written in different ways, such as,
	 * 
	 * //div[@id="ires"]//div[@class="rc" and number(@data-hveid)]/h3[@class="r"]/a
	 * 
	 * //div[@id="ires"]/div[@id="rso"]/div[@class="srg"]/div[@class="g"]/div[@class="rc" and number(@data-hveid)]/h3[@class="r"]/a
	 *
	 * It can be very precise, but less likely to tolerate variation.
	 * It can also be more ambiguous, but may result in unwanted elements 
	 * being retrieved while it tolerate variation. 
	 *  
	 */
	private final String GOOGLE_RESULT_HEADER_XPATH = 
			"//div[@id=\"ires\"]//div[@class=\"rc\" and number(@data-hveid)]/h3[@class=\"r\"]/a";

	private WebDriver driver;

	public static void main(String[] args) {
		GoogleSearch gs = new GoogleSearch();

		gs.doSearch("Regular Expression");

		if (!gs.isResultPageReady()) {
			gs.close();
			return;
		}
		
		List<WebElement> weList = gs.getResultList();
		if (weList == null) {
			gs.close();
			return;
		}
		
		gs.showLast(weList);
		
		gs.showPageInfo();
		
		gs.goback();
		
		if (!gs.isResultPageReady()) {
			gs.close();
			return;
		}
	}

	public GoogleSearch() {
		driver = null;

		try {
			URL url;

			url = new URL(REMOTE_WEBDRIVER_URL);

			// you may launch a different web browser
			driver = new RemoteWebDriver(url, DesiredCapabilities.chrome());

			log.log(Level.INFO, "The designated web browser is launched.");

			url = new URL(GOOGLE_URL);
			driver.navigate().to(url);

			log.log(Level.INFO, "openned URL " + GOOGLE_URL + ".");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e); // stop execution
		} catch (UnreachableBrowserException e) {
			throw new RuntimeException(e); // stop execution
		} catch (WebDriverException e) {
			throw new RuntimeException(e); // stop execution
		}
	}

	public void close() {
		log.log(Level.INFO, "Closing the designated web browser.");
		if (driver != null) {
			driver.close();
		}
	}

	public void doSearch(String words) {

		try {
			WebElement we = driver.findElement(By.id(GOOGLE_SEARCH_BOX_ID));

			log.log(Level.INFO, "located Google search input box");

			we.sendKeys(words);

			log.log(Level.INFO, "entered in the Google search input box: "
					+ words + ".");

			we.sendKeys(Keys.RETURN);

			log.log(Level.INFO,
					"hit ENTER key and search should be progress ...");
		} catch (NoSuchElementException e) {
			throw new RuntimeException(e); // stop execution
		}
	}

	public boolean isResultPageReady() {
		/**
		 * WebDriver is non-blocking. The result page may not appear immediately
		 * when the ENTER key is pressed. In the following, we demonstrate here
		 * that we can wait for a certain element to appear. 
		 * 
		 * The element is expressed in an XPATH. You may examine an XPATH
		 * using FirePath Firefox Addon, an extension to Firebug
		 * 
		 * https://addons.mozilla.org/en-US/firefox/addon/firepath/
		 */
		log.log(Level.INFO, "Locate elements using xpath: "
				+ GOOGLE_RESULT_PAGE_HEADING_XPATH);
		WebElement we = (new WebDriverWait(driver, TIME_OUT_SECONDS))
				.until(ExpectedConditions.presenceOfElementLocated(By
						.xpath(GOOGLE_RESULT_PAGE_HEADING_XPATH)));

		if (we == null) {
			log.log(Level.SEVERE, "Google search result is not returned.");
			return false;
		} else {
			log.log(Level.INFO, "Google search result returned");
			return true;
		}
	}
	
	public List<WebElement> getResultList() {

		/**
		 * Again, WebDriver is non-blocking and Google may build the results on
		 * the fly gradually. Here we demonstrate that we can wait for one of
		 * a list of elements to appear.
		 * 
		 * Waiting for TIME_OUT_SECONDS for a list of elements to appear on the
		 * page, checking for the presence once every POLLING_INTERVAL_SECONDS
		 */

		log.log(Level.INFO, "Locate elements using xpath: "
				+ GOOGLE_RESULT_HEADER_XPATH);
		Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
				.withTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.pollingEvery(POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS)
				.ignoring(NoSuchElementException.class);

		List<WebElement> weList = wait
				.until(new Function<WebDriver, List<WebElement>>() {
					public List<WebElement> apply(WebDriver driver) {
						return driver.findElements(By
								.xpath(GOOGLE_RESULT_HEADER_XPATH));
					}
				});

		if (weList.isEmpty()) {
			log.log(Level.WARNING, "Google has not returned any results, yet.");
			return null;
		}

		log.log(Level.INFO, "Google returned " + weList.size() + " results.");
		return weList;
	}
	
	public void showLast(List<WebElement> weList) {
		WebElement we = weList.get(weList.size() - 1);

		we.click();

		/**
		 * Certain poorly written web page may be loading forever or for a long
		 * time when content useful to a user is already loaded.
		 * 
		 * Below we simulate a wait, if too long, stop the page load. There may
		 * be better solutions. The solution may not always work. This is just
		 * one approach.
		 */
		try {
			driver.manage().timeouts()
					.pageLoadTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);

			log.log(Level.INFO, "Showed the last entry in the search result.");
		} catch (TimeoutException e) {
			log.log(Level.INFO,
					"Web page is taking too long to load. Stop it ...");
			driver.findElement(By.tagName("body")).sendKeys(
					Keys.CONTROL + "Escape");
		}
	}
	
	public void showPageInfo() {
		String title = driver.getTitle();
		String url = driver.getCurrentUrl();

		log.log(Level.INFO, "Title: " + title + "\n" + "URL: " + url + "\n");

		System.out.println("Title: " + title + "\n" + "URL: " + url + "\n");
		
	}
	
	public void goback() {
		/**
		 * this method is really simple, it is not necessary to make it a method
		 */
		log.log(Level.INFO,  "Go back to the search result.");
		driver.navigate().back();
	}
}
