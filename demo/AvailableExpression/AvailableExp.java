public class AvailableExp {
    public void may1() {
        int S = 0, a = 6, b = 7, s;
        // S = a * b;
        while(S < 300) {
            s = a * b;
            S += s;
        }
    }

    public void may1fail() {
        int S = 0, a = 6, b = 7, s;
        // S = a * b;
        while(S < 300) {
            s = 6;
            s = 7;
            s = 8;
            s = a * b;
            S += s;
        }
    }

    public void may2() {
        int S = 0, a = 6, b = 7, s;
        // S = a * b;
        while(S < 300) {
            s = 6;
            s = 7;
            s = 8;
            if (s % 2 == 1) {
                a ++;
            }
            s = 9;
            s = 10;
            s = a * b;
            S += s;
        }
    }

}
