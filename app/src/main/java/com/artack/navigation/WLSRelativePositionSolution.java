package com.artack.navigation;

import com.google.location.lbs.gnss.gps.pseudorange.Ecef2EnuConverter;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

public class WLSRelativePositionSolution {
    //RD, SatPos,ref
    // RD - Pseudoragne differenses
    private final int MaxIterationsCount = 12;
    private final double Epsilon = 1e-8;

    //Create Matrix
    private RealMatrix Xk = new Array2DRowRealMatrix(3,1);
    private RealMatrix X0 = new Array2DRowRealMatrix(3,1);
    private RealMatrix MatrixF = new Array2DRowRealMatrix(3,1);
    private RealMatrix Xref = new Array2DRowRealMatrix(3,1);
    private RealMatrix SatPosENU = new Array2DRowRealMatrix(3,1);
    /** Iterative WLS method for relative navigation solution*/
    public RealMatrix ComputeRangeDifferences (RealMatrix PseudoRangeBase,RealMatrix PseudoRangeTarget)
    {
        int rowCount = PseudoRangeTarget.getRowDimension();
        RealMatrix RD = new Array2DRowRealMatrix(rowCount,1);
        for(int i=0;i<rowCount;i++)
        {
            RD.setEntry(i,1,PseudoRangeBase.getEntry(i,1)-PseudoRangeTarget.getEntry(i,1));
        }
        return RD;
    }

    public RealMatrix WLSSolution(RealMatrix RD, RealMatrix SatPosECEF,RealMatrix ref)
    {
        Xk.setColumn(1,new double[]{0,0,0});//Solution
        X0.setColumn(1,new double[]{0,0,0});//Initial/Current Solution
        Xref.setColumn(1,new double[]{0,0,0});//ECEF Reference Coordinates
        MatrixF.setColumn(1,new double[]{0,0,0});//FunctionalMatrix
        for( int i=0;i<SatPosECEF.getRowDimension();i++)
        {
            Ecef2EnuConverter.EnuValues enuValues = Ecef2EnuConverter.convertEcefToEnu(SatPosECEF.getEntry(i,1),
                    SatPosECEF.getEntry(i,2),
                    SatPosECEF.getEntry(i,3),
                    ref.getEntry(1,1),
                    ref.getEntry(2,1)
                    );
            SatPosENU.setColumn(i,new double[]{enuValues.enuEast,enuValues.enuNorth,enuValues.enuUP});
        }

        int k = 1;
        while (((Math.abs(Xk.getEntry(1,1) - X0.getEntry(1,1))<Epsilon) &&
                (Math.abs(Xk.getEntry(2,1) - X0.getEntry(2,1))<Epsilon)) ||
                (k<MaxIterationsCount))
        {
            k++;
            RealMatrix H = GradientMatrix(SatPosENU,Xref);
            MatrixF = CalculateFMatrix(SatPosENU,X0,Xref,RD);
            Xk = X0.add((H.transpose().multiply(H)).power(-1)).multiply(H.transpose()).multiply(RD.subtract(MatrixF));
        }
        return Xk;
    }

    private RealMatrix CalculateFMatrix(RealMatrix X,RealMatrix X0,RealMatrix Xref,RealMatrix RD) {
        RealMatrix F = new Array2DRowRealMatrix(3,1);
        F.setColumn(1,new double[]{0,0,0});//Solution
        for(int i=0;i<X.getRowDimension();i++)
        {
            F.setEntry(i,1,
                    X0.getEntry(1,1) + (X.getEntry(1,i)-Xref.getEntry(1,1))/
                            CalcNorm(X.getColumnMatrix(i).subtract(Xref))+
                       X0.getEntry(2,1) + (X.getEntry(2,i)-Xref.getEntry(2,1))/
                            CalcNorm(X.getColumnMatrix(i).subtract(Xref))+
                       X0.getEntry(3,1) + (X.getEntry(3,i)-Xref.getEntry(3,1))/
                            CalcNorm(X.getColumnMatrix(i).subtract(Xref)));
        }
        return F;
    }

    public RealMatrix GradientMatrix(RealMatrix X,RealMatrix Xref)
    {
        RealMatrix H = new Array2DRowRealMatrix(X.getRowDimension(),3);
        for(int i=1;i<X.getRowDimension();i++)
        {
            for(int j=1;j<3;j++)
            {
                H.setEntry(i,j,(X.getEntry(i,j)-Xref.getEntry(1,j))/CalcNorm(X.getColumnMatrix(i).subtract(Xref)));
            }
        }
        return H;
    }

    public double CalcNorm(RealMatrix SubtMatr)
    {
         return Math.sqrt(Math.pow(SubtMatr.getEntry(1,1),2)+
                 Math.pow(SubtMatr.getEntry(2,1),2)+
                 Math.pow(SubtMatr.getEntry(3,1),2));
    }
}
