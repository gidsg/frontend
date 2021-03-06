package com.gu.test;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FrontSteps {

    private final SharedDriver webDriver;

    public FrontSteps(SharedDriver webDriver) {
        this.webDriver = webDriver;
    }

    @Given("^I am on the '(.*)' section front$")
    public void I_am_on_a_section_front(String sectionFront) throws Throwable {
        webDriver.open("/" + sectionFront);
    }
	
    // xpath to the first hideable section
    protected String sectionXpath = "//div[contains(@class, 'front-container')]/section[2]";
    protected String trailblockXpath = sectionXpath + "/div[contains(@class, 'trailblock')]";
	
    @Given("^a section is hidden$")
    public void a_section_is_hidden() throws Throwable {
      I_toggle_a_section("hide");
    }

    @When("^I (hide|show) a section$")
    public void I_toggle_a_section(String sectionState) throws Throwable {
    		// wait for the toggle to become visible
    		WebElement trailblockToggle = webDriver.waitForVisible(
    		    By.xpath(sectionXpath + "//button[contains(@class, 'toggle-trailblock')]")
    		);
    		String expectedTrailblockHeight = (sectionState.equals("show")) ? "none" : "0";
    		// only click if not in correct state
    		String actualTrailblockHeight = webDriver.findElement(By.xpath(trailblockXpath)).getCssValue("max-height");
    		if (!actualTrailblockHeight.equals(expectedTrailblockHeight)) {
    		  webDriver.jsClick(trailblockToggle);
    		}
  	}

  	@Then("^the section will be (hidden|shown)$")
  	public void the_section_will_be_toggled(String sectionState) throws Throwable {
    		String expectedTrailblockHeight = (sectionState.equals("shown")) ? "none" : "0";
    		// sections are hidden with css max-height
    		assertTrue(webDriver.waitForCss(
    		    By.xpath(trailblockXpath), "max-height", expectedTrailblockHeight)
    		);
  	}
  	
    @Then("^the '([^']*)' section should have a '([^']*)' cta that loads in more top stories$")
    public void should_load_in_more_stories(String section, String ctaText) throws Throwable {
        // horrible xpath to find the sections with a certain title
        String trailblockXpath = "//section[.//h1/descendant-or-self::*[contains(text(), '" + section + "')]]/div[contains(@class, 'trailblock')]";
        webDriver.waitForElement(By.xpath(trailblockXpath));
        WebElement trailblock = webDriver.findElement(By.xpath(trailblockXpath));
        // Wait for javascript to inject button.cta
        webDriver.waitForElement(By.cssSelector("button.cta"));
        WebElement cta = trailblock.findElement(By.cssSelector("button.cta"));
        assertEquals(ctaText, cta.getText());
        // how many trails do we currently have
        int trailCount = trailblock.findElements(By.className("trail")).size();
        webDriver.jsClick(cta);
        // wait for second list of top stories to load in
        webDriver.waitForElement(By.xpath(trailblockXpath + "/ul/li[" + (trailCount + 5) + "]"));
    }
	
}