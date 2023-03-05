package model.web;

import java.util.Objects;
import java.util.Scanner;

public class PageLoader {
    public String getData (String filename) {
        return new Scanner(Objects.requireNonNull(getClass().getResourceAsStream("/pages/" + filename))).useDelimiter("\\Z").next();
    }
}
