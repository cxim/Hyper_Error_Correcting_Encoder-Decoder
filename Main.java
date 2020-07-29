package correcter;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Main {

    public static boolean[] getHammingParity(String byteString) {
        boolean[] answer = new boolean[8];
        for (int j = 1; j < 8; j++) {
            if ((j & (j - 1)) == 0) { // i.e. j is a power of two
                int sum = 0;
                int nowBit = j; // starting just after the actual parity bit
                int leftInARow = j - 1; // one fewer left in-a-row than usual
                while (nowBit < 8) {
                    while (leftInARow > 0 && nowBit < 8) {
                        if (byteString.charAt(nowBit) == '1') {
                            sum++;
                        }
                        nowBit++;
                        leftInARow--;
                    }
                    leftInARow = j;
                    nowBit += j;
                }
                answer[j - 1] = sum % 2 == 1;
            }
        }
        answer[7] = false; // the last power of two (pos 8) doesn't need to be checked, always zero
        return answer; // contains values for parity vars at indices 1 - 1, 2 - 1, 4 - 1 8 - 1
    }

    @SuppressWarnings({"DuplicatedCode", "unused"})
    public static void encode() {

        try (Scanner inFile = new Scanner(new File("send.txt"));
             FileOutputStream outFile = new FileOutputStream("encoded.txt")
        ) {

            StringBuilder input = new StringBuilder();
            while (inFile.hasNextLine()) {
                input.append(inFile.nextLine());
            }

            System.out.println("send.txt:");
            System.out.println("text view: " + input);
            System.out.println("hex view: " + formatWithRadix(input.toString(), 16));
            System.out.println("bin view: " + formatWithRadix(input.toString(), 2));
            System.out.println();

            System.out.println("encoded.txt:");
            StringBuilder expanded = new StringBuilder();

            byte seenCount = 0;
            for (int i = 0; i < input.length(); i++) {
                byte now = (byte) input.charAt(i);
                for (byte shift = 7; shift >= 0; shift--) {
                    byte maskedValue = (byte) ((now >> shift) & 1);
                    if (maskedValue == 0) {
                        expanded.append('0').append('0');
                    } else {
                        expanded.append('1').append('1');
                    }
                    seenCount++;
                    if (seenCount == 3) {
                        expanded.append(".. ");
                        seenCount = 0;
                    }
                }
            }
            if (seenCount == 0) {
                expanded.deleteCharAt(expanded.length() - 1); // remove trailing space
            } else {
                for (; seenCount > 0; seenCount--) {
                    expanded.append("..");
                }
            }

            System.out.println("expand: " + expanded);

            String[] bytes = expanded.toString().split(" ");
            StringBuilder parity = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                byte sum = 0;
                for (int j = 0; j < 6; j++) {
                    char nowBit = bytes[i].charAt(j);
                    char toAdd = nowBit != '.' ? nowBit : '0';
                    if (j % 2 == 1 && toAdd == '1') {
                        sum++;
                    }
                    parity.append(toAdd);
                }
                parity.append(sum % 2).append(sum % 2);
                if (i != bytes.length - 1) {
                    parity.append(' ');
                }
            }

            System.out.println("parity: " + parity);
            displayAndWriteWithStream(parity.toString(), outFile);

        } catch (IOException e) {
            System.out.println("Something went wrong with encode().");
            e.printStackTrace();
        }

    }

    @SuppressWarnings("DuplicatedCode")
    public static void encodeHamming() {

        try (Scanner inFile = new Scanner(new File("send.txt"));
             FileOutputStream outFile = new FileOutputStream("encoded.txt")
        ) {

            StringBuilder input = new StringBuilder();
            while (inFile.hasNextLine()) {
                input.append(inFile.nextLine());
            }

            System.out.println("send.txt:");
            System.out.println("text view: " + input);
            System.out.println("hex view: " + formatWithRadix(input.toString(), 16));
            System.out.println("bin view: " + formatWithRadix(input.toString(), 2));
            System.out.println();

            System.out.println("encoded.txt:");
            StringBuilder expanded = new StringBuilder();

            for (int i = 0; i < input.length(); i++) {
                byte now = (byte) input.charAt(i);
                byte posInExpandedByte = 1; // not by index b/c of Hamming code spec!
                for (byte shift = 7; shift >= 0; shift--) {
                    // if position (left-to-right, starting from 1) is a power of two...
                    while ((posInExpandedByte & (posInExpandedByte - 1)) == 0) {
                        // ... that position is a parity bit
                        expanded.append(".");
                        posInExpandedByte++;
                        if (posInExpandedByte > 8) { // (will always be 9; occurs when finished with first half of now)
                            expanded.append(" ");
                            posInExpandedByte = 1;
                        }
                    }
                    byte maskedValue = (byte) ((now >> shift) & 1);
                    if (maskedValue == 0) {
                        expanded.append('0');
                    } else {
                        expanded.append('1');
                    }
                    posInExpandedByte++;
                }
                expanded.append(". "); // last bit is a parity bit
            }
            expanded.deleteCharAt(expanded.length() - 1); // remove trailing space

            System.out.println("expand: " + expanded);

            String[] bytes = expanded.toString().split(" ");
            StringBuilder parity = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                boolean[] parityKey = getHammingParity(bytes[i]);
                for (int j = 1; j <= 8; j++) {
                    if ((j & (j - 1)) == 0) { // i.e. j is a power of two
                        parity.append(parityKey[j - 1] ? '1' : '0');
                    } else {
                        parity.append(bytes[i].charAt(j - 1));
                    }
                }
                if (i != bytes.length - 1) {
                    parity.append(' ');
                }
            }

            System.out.println("parity: " + parity);
            displayAndWriteWithStream(parity.toString(), outFile);

        } catch (IOException e) {
            System.out.println("Something went wrong with encodeHamming().");
            e.printStackTrace();
        }

    }

    public static void send() {

        try (FileInputStream inFile = new FileInputStream(new File("encoded.txt"));
             FileOutputStream outFile = new FileOutputStream("received.txt")
        ) {

            ArrayList<Character> chars = getInputFromStream(inFile);

            System.out.println("encoded.txt:");
            StringBuilder inputHex = new StringBuilder();
            StringBuilder inputBinary = new StringBuilder();

            Random random = new Random(System.currentTimeMillis());
            StringBuilder garbledBinary = new StringBuilder();

            for (int i = 0; i < chars.size(); i++) {

                char now = chars.get(i);
                inputHex.append(formatWithRadix(now, 16));
                inputBinary.append(formatWithRadix(now, 2));
                if (i != chars.size() - 1) {
                    inputHex.append(' ');
                    inputBinary.append(' ');
                }

                int maskValue = random.nextInt(7);
                char nowGarbled = (char) (now ^ (char) (1 << maskValue));
                garbledBinary.append(formatWithRadix(nowGarbled, 2));
                if (i != chars.size() - 1) {
                    garbledBinary.append(' ');
                }

            }
            System.out.println("hex view: " + inputHex);
            System.out.println("bin view: " + inputBinary);
            System.out.println();

            System.out.println("received.txt:");
            System.out.println("bin view: " + garbledBinary);

            displayAndWriteWithStream(garbledBinary.toString(), outFile);

        } catch (IOException e) {
            System.out.println("Something went wrong with send().");
        }

    }

    @SuppressWarnings({"DuplicatedCode", "unused"})
    public static void decode() {

        try (FileInputStream inFile = new FileInputStream(new File("received.txt"));
             FileWriter outFile = new FileWriter("decoded.txt")
        ) {

            ArrayList<Character> chars = getInputFromStream(inFile);

            System.out.println("received.txt:");
            StringBuilder inputHex = new StringBuilder();
            StringBuilder inputBinary = new StringBuilder();

            StringBuilder correct = new StringBuilder();

            for (int i = 0; i < chars.size(); i++) {

                char now = chars.get(i);
                inputHex.append(formatWithRadix(now, 16));
                String nowBinary = formatWithRadix(now, 2);
                inputBinary.append(nowBinary);

                int faultyPairIndex = 0;
                while (nowBinary.charAt(faultyPairIndex) == nowBinary.charAt(faultyPairIndex + 1)) {
                    faultyPairIndex += 2;
                }
                int sum = 0;
                for (int k = 0; k < 6; k += 2) {
                    if (k != faultyPairIndex && nowBinary.charAt(k) == '1') {
                        sum++;
                    }
                }
                String fix;
                if (faultyPairIndex != 6) { // if error is not in parity, fix error
                    if (sum % 2 == Integer.parseInt(String.valueOf(nowBinary.charAt(6)))) {
                        fix = "00";
                    } else {
                        fix = "11";
                    }
                } else { // if error is in parity, redo parity
                    fix = String.valueOf(sum % 2).repeat(2);
                }
                String correctNowBinary = nowBinary.substring(0, faultyPairIndex) + fix +
                        nowBinary.substring(faultyPairIndex + 2);
                correct.append(correctNowBinary);

                if (i != chars.size() - 1) {
                    inputHex.append(' ');
                    inputBinary.append(' ');
                    correct.append(' ');
                }

            }

            System.out.println("hex view: " + inputHex);
            System.out.println("bin view: " + inputBinary);
            System.out.println();

            System.out.println("decoded.txt:");
            System.out.println("correct: " + correct);

            StringBuilder decode = new StringBuilder();
            StringBuilder nowByte = new StringBuilder();
            for (String expandedBinary : correct.toString().split(" ")) {
                for (int i = 0; i < 6; i += 2) {
                    nowByte.append(expandedBinary.charAt(i));
                    if (nowByte.length() == 8) {
                        decode.append(nowByte.toString()).append(' ');
                        nowByte = new StringBuilder();
                    }
                }
            }

            System.out.println("decode: " + decode + nowByte);
            decode.deleteCharAt(decode.length() - 1); // removes trailing space
            System.out.println("remove: " + decode);

            StringBuilder output = new StringBuilder();
            String[] charStrings = decode.toString().split(" ");
            for (String charString : charStrings) {
                char nowChar = (char) Integer.parseInt(charString, 2);
                output.append(nowChar);
                outFile.write(nowChar);
            }

            System.out.println("hex view: " + formatWithRadix(output.toString(), 16));
            System.out.println("text view: " + output);

        } catch (IOException e) {
            System.out.println("Something went wrong with decode().");
        }

    }

    @SuppressWarnings("DuplicatedCode")
    public static void decodeHamming() {

        try (FileInputStream inFile = new FileInputStream(new File("received.txt"));
             FileWriter outFile = new FileWriter("decoded.txt")
        ) {

            ArrayList<Character> chars = getInputFromStream(inFile);

            System.out.println("received.txt:");
            StringBuilder inputHex = new StringBuilder();
            StringBuilder inputBinary = new StringBuilder();

            StringBuilder correct = new StringBuilder();

            for (int i = 0; i < chars.size(); i++) {

                char now = chars.get(i);
                inputHex.append(formatWithRadix(now, 16));
                String nowBinary = formatWithRadix(now, 2);
                inputBinary.append(nowBinary);

                /* will add to this to get value;
                bias is b/c algorithm starts indices at one instead of zero */
                int badIndex = -1;
                boolean[] actualParityKey = getHammingParity(nowBinary);
                for (int j = 1; j <= 8; j *= 2) {
                    boolean apparentParityKey = nowBinary.charAt(j - 1) == '1'; // (else, == '0')
                    if (apparentParityKey != actualParityKey[j - 1]) {
                        badIndex += j;
                    }
                }
                char replacement = nowBinary.charAt(badIndex) == '1' ? '0' : '1';
                correct.append(nowBinary, 0, badIndex).append(replacement)
                        .append(nowBinary.substring(badIndex + 1));

                if (i != chars.size() - 1) {
                    inputHex.append(' ');
                    inputBinary.append(' ');
                    correct.append(' ');
                }

            }

            System.out.println("hex view: " + inputHex);
            System.out.println("bin view: " + inputBinary);
            System.out.println();

            System.out.println("decoded.txt:");
            System.out.println("correct: " + correct);

            StringBuilder decode = new StringBuilder();
            StringBuilder nowByte = new StringBuilder();
            for (String expandedBinary : correct.toString().split(" ")) {
                for (int i = 1; i <= 8; i ++) {
                    if ((i & (i - 1)) != 0) { // if not a parity bit
                        nowByte.append(expandedBinary.charAt(i - 1));
                        if (nowByte.length() == 8) {
                            decode.append(nowByte.toString()).append(' ');
                            nowByte = new StringBuilder();
                        }
                    }
                }
            }

            System.out.println("decode: " + decode);

            StringBuilder output = new StringBuilder();
            String[] charStrings = decode.toString().split(" ");
            for (String charString : charStrings) {
                char nowChar = (char) Integer.parseInt(charString, 2);
                output.append(nowChar);
                outFile.write(nowChar);
            }

            System.out.println("hex view: " + formatWithRadix(output.toString(), 16));
            System.out.println("text view: " + output);

        } catch (IOException e) {
            System.out.println("Something went wrong with decodeHamming().");
        }

    }

    public static String formatWithRadix(char value, int radix) {

        StringBuilder nowString = new StringBuilder(Integer.toString(value, radix));
        if (radix == 2) {
            for (int j = nowString.length(); j < 8; j++) {
                nowString.insert(0, '0');
            }
        } else if (radix == 16) {
            if (nowString.length() < 2) {
                nowString.insert(0, '0');
            } else if (nowString.length() > 2) {
                nowString = new StringBuilder(nowString.substring(nowString.length() - 2));
            }
            nowString = new StringBuilder(nowString.toString().toUpperCase());
        }
        return nowString.toString();
    }

    public static String formatWithRadix(String text, int radix) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            output.append(formatWithRadix(text.charAt(i), radix));
            if (i != text.length() - 1) {
                output.append(' ');
            }
        }
        return output.toString();
    }

    private static ArrayList<Character> getInputFromStream(FileInputStream inFile) throws IOException {
        ArrayList<Character> chars = new ArrayList<>();
        int inByte = inFile.read();
        while (inByte != -1) {
            chars.add((char) inByte);
            inByte = inFile.read();
        }
        return chars;
    }

    private static void displayAndWriteWithStream(String binary, FileOutputStream destination) throws IOException {

        StringBuilder output = new StringBuilder();
        String[] charStrings = binary.split(" ");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(charStrings.length);
        for (String charString : charStrings) {
            char nowChar = (char) Integer.parseInt(charString, 2);
            output.append(nowChar);
            byteStream.write(nowChar);
        }

        System.out.println("hex view: " + formatWithRadix(output.toString(), 16));

        byteStream.writeTo(destination);

    }

    public static void main(String[] args) {

        System.out.print("Write a mode: ");
        Scanner keyboard = new Scanner(System.in);
        String mode = keyboard.next();
        System.out.println();

        switch (mode) {
            case "encode":
                encodeHamming();
                break;
            case "send":
                send();
                break;
            case "decode":
                decodeHamming();
                break;
            default:
                throw new IllegalArgumentException("Unknown mode.");
        }

    }

}

