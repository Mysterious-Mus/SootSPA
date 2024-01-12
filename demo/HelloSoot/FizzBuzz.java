public class FizzBuzz {

    public void printFizzBuzz(int k){
        if (k%15==0)
            System.out.println("FizzBuzz");
        else if (k%5==0)
            System.out.println("Buzz");
        else if (k%3==0)
            System.out.println("Fizz");
        else
            System.out.println(k);
    }

    public void moreAvaliable() {
        int S = 0, a = 6, b = 7, s;
        while(S < 300) {
            s = a * b;
            if(s % 2 == 0) {
                a++;
            }
            S += s;
        }
    }

    public void fizzBuzz(int n){
        for (int i=1; i<=n; i++)
            printFizzBuzz(i);
    }
}

