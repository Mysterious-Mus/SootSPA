public class AvailableExp {
    // 1must cannot find the Optimizable point, but may1 is effective
    public void may1() {
        int S = 0, a = 6, b = 7, s;
        // S = a * b;
        while(S < 300) {
            s = a * b;
            S += s;
        }
    }

    // may1 cannot find the Optimizable point, but 'may1+enhanced_may_iter' is effective
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

    // may1 and 'may1+enhanced_may_iter' cannot find all 3 Optimizable points, but 'may2+enhanced_may_iter' is effective
    public void may1fail2() {
        int S = 0, a = 6, b = 7, s;
        // S = a * b;
        while (S < 300) {
            s = a - b;
            while (s < 30) {
                s += a + b;
            }
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

    // 'may2+enhanced_may_iter' find 2, and converge in 'may3+enhanced_may_iter' that find all 3 Optimizable points
    public void may3() {
        int S = 0, a = 6, b = 7, s;
        // S = a * b;
        while (S < 300) {
            s = 6;
            s = 7;
            s = 8;
            if (s % 2 == 1) {
                a++;
                while (s % 2 == 1) {
                    s++;
                }
            }
            s = 9;
            s = 10;
            s = a * b;
            S += s;
        }
    }
}
