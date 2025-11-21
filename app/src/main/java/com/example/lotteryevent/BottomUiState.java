package com.example.lotteryevent;

import com.example.lotteryevent.viewmodels.EventDetailsViewModel;

/**
 * A helper class to represent possible states of the screen, depending on how fetching data performs.
 */
public class BottomUiState {
    public enum StateType { LOADING, SHOW_INFO_TEXT, SHOW_ONE_BUTTON, SHOW_TWO_BUTTONS }
    public final BottomUiState.StateType type;
    public final String infoText;
    public final String positiveButtonText;
    public final String negativeButtonText;

    /**
     * Constructor allowing for instance creation via static methods
     * @param type state type
     * @param infoText text to display as info
     * @param positiveButtonText text for the positive button
     * @param negativeButtonText text for the negative button
     */
    public BottomUiState(BottomUiState.StateType type, String infoText, String positiveButtonText, String negativeButtonText) {
        this.type = type;
        this.infoText = infoText;
        this.positiveButtonText = positiveButtonText;
        this.negativeButtonText = negativeButtonText;
    }

    // Static factory methods for creating states
    /**
     * Static factory method for creating loading state
     * @return UI state
     */
    public static BottomUiState loading() { return new BottomUiState(BottomUiState.StateType.LOADING, null, null, null); }

    /**
     * Static factory method for creating infoText state
     * @return UI state
     */
    public static BottomUiState infoText(String text) { return new BottomUiState(BottomUiState.StateType.SHOW_INFO_TEXT, text, null, null); }

    /**
     * Static factory method for creating one button state
     * @return UI state
     */
    public static BottomUiState oneButton(String text) { return new BottomUiState(BottomUiState.StateType.SHOW_ONE_BUTTON, null, text, null); }

    /**
     * Static factory method for creating two buttons state
     * @return UI state
     */
    public static BottomUiState twoButtons(String pos, String neg) { return new BottomUiState(BottomUiState.StateType.SHOW_TWO_BUTTONS, null, pos, neg); }
}
