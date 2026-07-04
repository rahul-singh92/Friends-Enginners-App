package com.jitendersingh.friendsengineer;

public class NumberToWords {

    private static final String[] oneToNineteen = {
            "", "One", "Two", "Three", "Four", "Five", "Six",
            "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve",
            "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] tens = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty",
            "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String convert(double amount) {

        long rupees = (long) amount;

        if (rupees == 0)
            return "Zero Rupees Only";

        return convertNumber(rupees).trim() + " Rupees Only";
    }

    private static String convertNumber(long n) {

        StringBuilder result = new StringBuilder();

        if (n >= 10000000) {
            result.append(convertNumber(n / 10000000))
                    .append(" Crore ");
            n %= 10000000;
        }

        if (n >= 100000) {
            result.append(convertNumber(n / 100000))
                    .append(" Lakh ");
            n %= 100000;
        }

        if (n >= 1000) {
            result.append(convertNumber(n / 1000))
                    .append(" Thousand ");
            n %= 1000;
        }

        if (n >= 100) {
            result.append(oneToNineteen[(int) (n / 100)])
                    .append(" Hundred ");
            n %= 100;
        }

        if (n >= 20) {
            result.append(tens[(int) (n / 10)])
                    .append(" ");
            n %= 10;
        }

        if (n > 0) {
            result.append(oneToNineteen[(int) n])
                    .append(" ");
        }

        return result.toString();
    }
}