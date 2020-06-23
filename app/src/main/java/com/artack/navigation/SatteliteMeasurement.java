package com.artack.navigation;

public class SatteliteMeasurement {
    private int SvID;
    private  double Pseudorange;

    public SatteliteMeasurement(int svID, double pseudorange) {
        SvID = svID;
        Pseudorange = pseudorange;
    }

    public int getSvID() {
        return SvID;
    }

    public double getPseudorange() {
        return Pseudorange;
    }
}
