package com.bluberry.adclient.sp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GPIO {

    public String port;
    public int pin;

    //Constructor
    public GPIO(int pin) {
        this.port = "gpio" + pin;
        this.pin = pin;
    }

    //get direction of gpio
    public String getInOut() {
        String command = String.format("cat /sys/class/gpio/%s/direction", this.port);
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
            String retour = text.toString();
            return retour;
        } catch (IOException e) {
            return "";
        }
    }

    // get state of gpio for input and output
    //test if gpio is configurate
    public int getState() {
        String command = String.format("cat /sys/class/gpio/%s/value", this.port);
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
            try {
                String retour = text.toString();
                if (retour.equals("")) {
                    return -1;
                } else {
                    return Integer.parseInt(retour.substring(0, 1));
                }
            } catch (NumberFormatException nfe) {
                return -1;
            }
        } catch (IOException e) {
            return -1;
        }
    }

    //set value of the output
    public boolean setState(int value) {
        String command = String.format("echo %d > /sys/class/gpio/%s/value", value, this.port);
        try {
            String[] test = new String[]{"su", "-c", command};
            Runtime.getRuntime().exec(test);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // set direction
    public boolean setInOut(String direction) {
        String command = String.format("echo %s > /sys/class/gpio/%s/direction", direction, this.port);
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    //export gpio
    public boolean activationPin() {
        String command = String.format("echo %d > /sys/class/gpio/export", this.pin);
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // unexport gpio
    public boolean desactivationPin() {
        String command = String.format("echo %d > /sys/class/gpio/unexport", this.pin);
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    //init the pin
    public int initPin(String direction) {
        int retour = 0;
        boolean ret = true;

        // see if gpio is already set
        retour = getState();
        if (retour == -1) {
            // unexport the gpio
            ret = desactivationPin();
            if (ret == false) {
                retour = -1;
            }

            //export the gpio
            ret = activationPin();
            if (ret == false) {
                retour = -2;
            }
        }

        // get If gpio direction is define
        String ret2 = getInOut();
        if (!ret2.contains(direction)) {
            // set the direction (in or out)
            ret = setInOut(direction);
            if (ret == false) {
                retour = -3;
            }
        }

        return retour;
    }
}