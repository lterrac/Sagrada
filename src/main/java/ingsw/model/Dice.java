package ingsw.model;

import java.io.Serializable;
import java.util.Random;

public class Dice implements Serializable {
    private int faceUpValue;
    private final Color diceColor;

    public Dice(Color diceColor) {
        this.diceColor = diceColor;
    }

    public Dice(int faceUpValue, Color diceColor) {
        this.faceUpValue = faceUpValue;
        this.diceColor = diceColor;
    }

    /**
     * Draft the dice
     * get a random number between 1 and 6 and set the faceUpValue
     */
    void roll() {
        int value = (new Random()).nextInt(6) + 1;
        setFaceUpValue(value);
    }

    public int getFaceUpValue() {
        return faceUpValue;
    }

    public void setFaceUpValue(int faceUpValue) {
        this.faceUpValue = faceUpValue;
    }

    public void increasesByOneValue(){
        faceUpValue ++;
    }
    public void decreasesByOneValue(){
        faceUpValue--;
    }

    public Color getDiceColor() {
        return diceColor;
    }

    @Override
    public String toString() {
        if (faceUpValue != 0) {
            return diceColor.toString() + String.valueOf(faceUpValue);
        } else {
            return diceColor.toString();
        }
    }
}
