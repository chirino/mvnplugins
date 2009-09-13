package org.component;

public class DefaultPizzaComponent implements PizzaComponent {
    public String[] getToppings() {
        return new String[]{ "cheeze", "sardines"};
    }
}
