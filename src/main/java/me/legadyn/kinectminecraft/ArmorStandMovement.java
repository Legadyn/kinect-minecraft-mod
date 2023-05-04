package me.legadyn.kinectminecraft;

public class ArmorStandMovement {

    public float headPitch,

    right_armX,right_armY,
            left_armX,left_armY,

    right_legX,right_legY,
            left_legX,left_legY,

    yaw,pitch;

    public double vecX,vecY,vecZ;

    public ArmorStandMovement( ) {

    }

    //yaw and pitch inversed
    public ArmorStandMovement(String[] decoded) {
        this.headPitch = Float.valueOf(decoded[0]);

        this.right_armY = Float.valueOf(decoded[1]);
        this.right_armX = Float.valueOf(decoded[2]);
        this.left_armY = Float.valueOf(decoded[3]);
        this.left_armX = Float.valueOf(decoded[4]);

        this.right_legY = Float.valueOf(decoded[5]);
        this.right_legX = Float.valueOf(decoded[6]);
        this.left_legY = Float.valueOf(decoded[7]);
        this.left_legX = Float.valueOf(decoded[8]);

        this.vecX = Double.valueOf(decoded[9]);
        this.vecY = Double.valueOf(decoded[10]);
        this.vecZ = Double.valueOf(decoded[11]);

        this.yaw = Float.valueOf(decoded[12]);
        this.pitch = Float.valueOf(decoded[13]);
    }

    public void convert(String[] decoded) {
        this.headPitch = Float.valueOf(decoded[0]);

        this.right_armX = Float.valueOf(decoded[1]);
        this.right_armY = Float.valueOf(decoded[2]);
        this.left_armX = Float.valueOf(decoded[3]);
        this.left_armY = Float.valueOf(decoded[4]);

        this.right_legX = Float.valueOf(decoded[5]);
        this.right_legY = Float.valueOf(decoded[6]);
        this.left_legX = Float.valueOf(decoded[7]);
        this.left_legY = Float.valueOf(decoded[8]);

        this.vecX = Double.valueOf(decoded[9]);
        this.vecY = Double.valueOf(decoded[10]);
        this.vecZ = Double.valueOf(decoded[11]);

        this.yaw = Float.valueOf(decoded[12]);
        this.pitch = Float.valueOf(decoded[13]);
    }
}
