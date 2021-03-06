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

/**
 * Tests conference state after entering and exiting audio only mode.
 *
 * @author Leonard Kim
 */
public class AudioOnlyTest
        extends TestCase
{
    /**
     * Constructs test
     * @param name the method name for the test.
     */
    public AudioOnlyTest(String name)
    {
        super(name);
    }

    private String AUDIO_ONLY_ENABLED_ICON_XPATH
        = "//div[@id='videoResolutionLabel']" +
        "//i[contains(@class, 'icon-visibility-off')]";
    private String LARGE_AVATAR_XPATH ="//div[@id='dominantSpeaker']";
    private String MUTE_BUTTON_ID = "toolbar_button_camera";
    private String VIDEO_QUALITY_BUTTON_ID
        = "toolbar_button_videoquality";
    private String VIDEO_QUALITY_SLIDER_CLASS = "video-quality-dialog-slider";

    /**
     * Orders the tests.
     * @return the suite with order tests.
     */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest(
            new AudioOnlyTest("enableAudioOnlyAndCheck"));
        suite.addTest(
            new AudioOnlyTest("videoUnmuteDisabledInAudioOnly"));
        suite.addTest(
            new AudioOnlyTest("avatarsDisplayForParticipants"));
        suite.addTest(
            new AudioOnlyTest("disableAudioOnlyAndCheck"));

        return suite;
    }

    /**
     * Enables audio only mode for the owner and verifies the other participant
     * sees the owner as video muted.
     */
    public void enableAudioOnlyAndCheck()
    {
        setAudioOnlyAndCheck(
            ConferenceFixture.getOwner(),
            "owner",
            ConferenceFixture.getSecondParticipant(),
            true);
    }

    /**
     * Verifies the owner cannot video unmute while in audio only mode.
     */
    public void videoUnmuteDisabledInAudioOnly() {
        MeetUIUtils.clickOnToolbarButton(ConferenceFixture.getOwner(),
            MUTE_BUTTON_ID);

        verifyVideoMute(
            ConferenceFixture.getOwner(),
            "owner",
            ConferenceFixture.getSecondParticipant(),
            true);
    }

    /**
     * Verifies the owner sees avatars for itself and other participants.
     */
    public void avatarsDisplayForParticipants() {
        WebDriver owner = ConferenceFixture.getOwner();

        TestUtils.waitForDisplayedElementByXPath(owner, LARGE_AVATAR_XPATH, 2);

        MeetUIUtils.assertLocalThumbnailShowsAvatar(owner);
    }

    /**
     * Disables audio only mode and verifies both the owner and the other
     * participant see the owner as not video muted.
     */
    public void disableAudioOnlyAndCheck() {
        setAudioOnlyAndCheck(
            ConferenceFixture.getOwner(),
            "owner",
            ConferenceFixture.getSecondParticipant(),
            false);
    }

    /**
     * Toggles the audio only state of a specific Meet conference participant
     * and verifies participant sees the audio only label and that a specific
     * other Meet conference participant sees a specific video mute state for
     * the former.
     *
     * @param testee the {@code WebDriver} which represents the Meet conference
     * participant whose audio only state is to be toggled
     * @param testeeName the name of {@code testee} to be displayed
     * should the test fail
     * @param observer the {@code WebDriver} which represents the Meet
     * conference participant to verify the mute state of {@code testee}
     * @param audioOnly the audio only state of {@code testee} expected to be
     * observed by {@code observer}
     */
    private void setAudioOnlyAndCheck(
        WebDriver testee,
        String testeeName,
        WebDriver observer,
        boolean audioOnly)
    {
        setAudioOnly(testee, audioOnly);

        verifyVideoMute(
            testee, testeeName, observer, audioOnly);

        TestUtils.waitForDisplayedOrNotByXPath(
            testee,
            AUDIO_ONLY_ENABLED_ICON_XPATH,
            5,
            audioOnly);
    }

    /**
     * Shows the video quality menu in the from the toolbar and sets audio only
     * mode to either on or off.
     *
     * @param participant the {@code WebDriver}.
     * @param audioOnly whether or not audio only mode should be enabled.
     */
    private void setAudioOnly(WebDriver participant, boolean audioOnly)
    {
        // Open tbe video quality dialog.
        setVideoQualityDialogVisible(participant, true);

        // Calculate how far to move the quality slider and in which direction.
        WebElement videoQualitySlider = participant.findElement(
            By.className(VIDEO_QUALITY_SLIDER_CLASS));

        int audioOnlySliderValue
            = Integer.parseInt(videoQualitySlider.getAttribute("min"));
        int maxDefinitionSliderValue
            = Integer.parseInt(videoQualitySlider.getAttribute("max"));

        int activeValue
            = Integer.parseInt(videoQualitySlider.getAttribute("value"));
        int targetValue = audioOnly
            ? audioOnlySliderValue : maxDefinitionSliderValue;

        int distanceToTargetValue = targetValue - activeValue;
        Keys keyDirection = distanceToTargetValue > 0 ? Keys.RIGHT : Keys.LEFT;

        // Move the slider to the target value.
        for (int i = 0; i < Math.abs(distanceToTargetValue); i++) {
            videoQualitySlider.sendKeys(keyDirection);
        }

        // Close the video quality dialog.
        setVideoQualityDialogVisible(participant, false);
    }

    /**
     * Sets whether or not the video quality dialog is displayed.
     *
     * @param participant the {@code WebDriver}.
     * @param visible whether or not the dialog should be displayed.
     */
    private void setVideoQualityDialogVisible(
        WebDriver participant, boolean visible) {
        boolean isDialogSliderVisible = !participant.findElements(
            By.className(VIDEO_QUALITY_SLIDER_CLASS)).isEmpty();

        if ((isDialogSliderVisible && !visible)
            || (!isDialogSliderVisible && visible)) {
            MeetUIUtils.clickOnToolbarButton(
                participant, VIDEO_QUALITY_BUTTON_ID);
        }
    }

    /**
     * Verifies that a specific other Meet conference participant sees a
     * specific mute state for the former.
     *
     * @param testee the {@code WebDriver} which represents the Meet conference
     * participant whose mute state is to be toggled
     * @param testeeName the name of {@code testee} to be displayed should the
     * test fail
     * @param observer the {@code WebDriver} which represents the Meet
     * conference participant to verify the mute state of {@code testee}
     * @param muted the mute state of {@code testee} expected to be observed by
     * {@code observer}
     */
    private void verifyVideoMute(
        WebDriver testee,
        String testeeName,
        WebDriver observer,
        boolean muted
    ) {
        // Verify the observer sees the testee in the desired muted state.
        MeetUIUtils.assertMuteIconIsDisplayed(
                observer,
                testee,
                muted,
                true, // checking video mute
                testeeName);

        // Verify the testee sees itself in the desired muted state.
        MeetUIUtils.assertMuteIconIsDisplayed(
                testee,
                testee,
                muted,
                true, // checking video mute
                testeeName);
    }
}
