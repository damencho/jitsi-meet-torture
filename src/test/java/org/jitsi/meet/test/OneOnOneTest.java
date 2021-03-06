/*
 * Copyright @ 2017 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.meet.test;

import junit.framework.*;
import org.jitsi.meet.test.util.*;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;

/**
 * Tests 1-on-1 remote video thumbnail display in the filmstrip.
 *
 * @author Leonard Kim
 */
public class OneOnOneTest
    extends TestCase
{
    /**
     * The duration to wait, in seconds, remote videos in filmstrip to display
     * and complete animations.
     */
    private final int filmstripVisibilityWait = 5;

    /**
     * Parameters to attach to the meeting url to enable One-On-One behavior
     * and have toolbars dismiss faster, as remote video visibility is also
     * tied to toolbar visibility.
     */
    private final String oneOnOneConfigOverrides
        = "config.disable1On1Mode=false"
        + "&interfaceConfig.TOOLBAR_TIMEOUT=250"
        + "&interfaceConfig.INITIAL_TOOLBAR_TIMEOUT=250"
        + "&config.alwaysVisibleToolbar=false";

    /**
     * Constructs test
     * @param name the method name for the test.
     */
    public OneOnOneTest(String name)
    {
        super(name);
    }

    /**
     * Orders the tests.
     * @return the suite with order tests.
     */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest(new OneOnOneTest("testFilmstripHiddenInOneOnOne"));
        suite.addTest(new OneOnOneTest("testFilmstripVisibleWithMoreThanTwo"));
        suite.addTest(
            new OneOnOneTest("testFilmstripDisplayWhenReturningToOneOnOne"));
        suite.addTest(new OneOnOneTest("testFilmstripVisibleOnSelfViewFocus"));
        suite.addTest(new OneOnOneTest("testFilmstripHoverShowsVideos"));
        suite.addTest(new OneOnOneTest("testStopOneOnOneTest"));

        return suite;
    }

    /**
     * Tests remote videos in filmstrip do not display in a 1-on-1 call.
     */
    public void testFilmstripHiddenInOneOnOne()
    {
        WebDriver owner =  ConferenceFixture.getOwner();
        WebDriver secondParticipant = ConferenceFixture.getSecondParticipant();

        // Close the browsers first and then load the meeting so hash changes to
        // the config are detected by the browser.
        ConferenceFixture.close(owner);
        ConferenceFixture.close(secondParticipant);

        ConferenceFixture.startOwner(oneOnOneConfigOverrides);
        ConferenceFixture.startSecondParticipant(oneOnOneConfigOverrides);

        // Prevent toolbar from being always displayed as filmstrip visibility
        // is tied to toolbar visibility.
        stopDockingToolbar(owner);
        stopDockingToolbar(secondParticipant);

        verifyRemoteVideosDisplay(owner, false);
        verifyRemoteVideosDisplay(secondParticipant, false);
    }

    /**
     * Tests remote videos in filmstrip do display when in a call with more than
     * two total participants.
     */
    public void testFilmstripVisibleWithMoreThanTwo() {
        // Close the third participant's browser and reopen so hash changes to
        // the config are detected by the browser.
        WebDriver thirdParticipant = ConferenceFixture.getThirdParticipant();

        ConferenceFixture.waitForThirdParticipantToConnect();
        ConferenceFixture.close(thirdParticipant);
        ConferenceFixture.startThirdParticipant(oneOnOneConfigOverrides);
        stopDockingToolbar(thirdParticipant);

        verifyRemoteVideosDisplay(ConferenceFixture.getOwner(), true);
        verifyRemoteVideosDisplay(
            ConferenceFixture.getSecondParticipant(), true);
        verifyRemoteVideosDisplay(thirdParticipant, true);
    }

    /**
     * Tests remote videos in filmstrip do not display after transitioning to
     * 1-on-1 mode. Also tests remote videos in filmstrip do display when
     * focused on self and transitioning back to 1-on-1 mode.
     */
    public void testFilmstripDisplayWhenReturningToOneOnOne() {
        MeetUIUtils.clickOnLocalVideo(ConferenceFixture.getSecondParticipant());

        ConferenceFixture.closeThirdParticipant();

        verifyRemoteVideosDisplay(ConferenceFixture.getOwner(), false);
        verifyRemoteVideosDisplay(
            ConferenceFixture.getSecondParticipant(), true);
    }

    /**
     * Tests remote videos in filmstrip become visible when focused on self view
     * while in a 1-on-1 call.
     */
    public void testFilmstripVisibleOnSelfViewFocus() {
        MeetUIUtils.clickOnLocalVideo(ConferenceFixture.getOwner());
        verifyRemoteVideosDisplay(ConferenceFixture.getOwner(), true);

        MeetUIUtils.clickOnLocalVideo(ConferenceFixture.getOwner());
        verifyRemoteVideosDisplay(ConferenceFixture.getOwner(), false);
    }

    /**
     * Tests remote videos in filmstrip stay visible when hovering over when the
     * filmstrip is hovered over.
     */
    public void testFilmstripHoverShowsVideos() {
        WebDriver owner = ConferenceFixture.getOwner();

        WebElement toolbar = owner.findElement(By.id("localVideoContainer"));
        Actions hoverOnToolbar = new Actions(owner);
        hoverOnToolbar.moveToElement(toolbar);
        hoverOnToolbar.perform();

        verifyRemoteVideosDisplay(owner, true);
    }

    /**
     * Ensures participants reopen their browsers without 1-on-1 mode enabled.
     */
    public void testStopOneOnOneTest() {
        ConferenceFixture.restartParticipants();
    }

    /**
     * Check if remote videos in filmstrip are visible.
     *
     * @param testee the <tt>WebDriver</tt> of the participant for whom we're
     *               checking the status of filmstrip remote video visibility.
     * @param isDisplayed whether or not filmstrip remote videos should be
     *                    visible
     */
    private void verifyRemoteVideosDisplay(
        WebDriver testee, boolean isDisplayed)
    {
        waitForToolbarsHidden(testee);

        String filmstripRemoteVideosXpath
            = "//div[@id='filmstripRemoteVideosContainer']";

        TestUtils.waitForDisplayedOrNotByXPath(
            testee,
            filmstripRemoteVideosXpath,
            filmstripVisibilityWait,
            isDisplayed);
    }

    /**
     * Disables permanent display (docking) of the toolbars.
     *
     * @param testee the <tt>WebDriver</tt> of the participant for whom we're
     *               no longer want to dock toolbars.

     */
    private void stopDockingToolbar(WebDriver testee) {
        ((JavascriptExecutor) testee)
            .executeScript("APP.UI.dockToolbar(false);");
    }

    /**
     * Waits until the toolbars are no longer displayed.
     *
     * @param testee the <tt>WebDriver</tt> of the participant for whom we're
     *               waiting to no longer see toolbars.
     */
    private void waitForToolbarsHidden(WebDriver testee) {
        // Wait for the visible filmstrip to no longer be displayed.
        String visibleToolbarXpath
            = "//*[contains(@class, 'toolbar_secondary')"
            + "and contains(@class ,'slideInExtX')]";

        TestUtils.waitForElementNotPresentByXPath(
            testee,
            visibleToolbarXpath,
            filmstripVisibilityWait);
    }
}
